/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.utils.shell;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.shell.ShellCommand.BehaviourOnTimeout;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.Constants;


public class ShellExecution implements Callable<ShellExecutionResponse> {
  
  protected final Logger logger = CentralFactoryLogging.getLogger(ShellExecution.class);

  private final static Runtime runtime = Runtime.getRuntime();
  
  private final static String SHELL = "sh";
  private final static String SHELLPARAM = "-c";
  
  private final static SleepCounter SLEEP_COUNTER_TEMPLATE = new SleepCounter(5, 100, 3);
  
  private final ShellCommand command;
  private StringBuilder error;
  private StringBuilder output;
  
  
  public ShellExecution(ShellCommand command) {
    this.command = command;
    error = new StringBuilder();
    output = new StringBuilder();
  }
  

  public ShellExecutionResponse call() throws Exception {
    if (logger.isTraceEnabled()) {
      logger.trace("Executing command: '" + command.getCommand() + "'");
    }
    long starttime = System.currentTimeMillis();
    ShellExecutionResponse result;
    Process p = null;
    try {
      p = runtime.exec(new String[] {SHELL, SHELLPARAM, command.getCommand()}, command.getEnviromentalProperties(), command.getExecutionDirectory());
    } catch (IOException e) {
      appendExceptionToError(e);
      return new ShellExecutionResponse(error.toString(), false, null);
    }
    InputStream out = new BufferedInputStream(p.getInputStream());
    InputStream err = new BufferedInputStream(p.getErrorStream());
    SleepCounter pillow = SLEEP_COUNTER_TEMPLATE.clone();
    Integer exitValue = null;
    try {
      while (true) {
        collectData(out, err, pillow);
        try {
          exitValue = p.exitValue();
          break;
        } catch (IllegalThreadStateException e) {
          if (command.getTimeoutMillis() > 0 && System.currentTimeMillis() > starttime + command.getTimeoutMillis()) {
            if (command.getBehaviourOnTimeout() == BehaviourOnTimeout.ERROR) {
              throw new TimeoutException("Timeout while waiting " + command.getTimeoutMillis());
            } else {
              break;
            }
          }
          pillow.sleep();
        }
      }
      collectData(out, err, pillow);
      String errString = error.toString();
      if (errString.length() > 0) {
        if (logger.isTraceEnabled()) {
          logger.trace("Execution failed: '" + errString + "'");
        }
        result = new ShellExecutionResponse(errString, false, exitValue);
      } else {
        if (logger.isTraceEnabled()) {
          logger.trace("Execution returned: '" + output.toString() + "'");
        }
        result = new ShellExecutionResponse(output.toString(), true, exitValue);
      }
      
      if (logger.isTraceEnabled()) {
        logger.trace("Execution time: " + (System.currentTimeMillis() - starttime));
      }
      
      return result;

    } catch (Throwable t) {
      Department.handleThrowable(t);
      appendExceptionToError(t);
      if (logger.isTraceEnabled()) {
        logger.trace("Execution failed: '" + error.toString() + "'");
      }
      return new ShellExecutionResponse(error.toString(), false, exitValue);
    }
  }
  
  
  private void appendExceptionToError(Throwable t) {
    error.append(t.getMessage()).append(Constants.LINE_SEPARATOR);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    t.printStackTrace(new PrintStream(baos));
    try {
      error.append(baos.toString(Constants.DEFAULT_ENCODING));
    } catch (UnsupportedEncodingException e) {
      // highly unlikely
      error.append(baos.toString());
    }
  }


  // TODO configurable reset pillow if data was received?
  private void collectData(InputStream out, InputStream err, SleepCounter pillow) throws IOException {
    byte[] buf_out = new byte[1024];
    byte[] buf_err = new byte[1024];
    int output_available;
    int error_available;
    output_available = out.available();
    error_available = err.available();
    while (output_available > 0 || error_available > 0) {
      int read = 0;
      if (output_available > 0) {
        read = out.read(buf_out);
        output.append(new String(buf_out, 0, read, Constants.DEFAULT_ENCODING));
      }
      if (error_available > 0) {
        read = err.read(buf_err);
        error.append(new String(buf_err, 0, read, Constants.DEFAULT_ENCODING));
      }
      if (read > 0 && command.doResetTimeoutOnReceive()) {
        pillow.reset();
      }
      output_available = out.available();
      error_available = err.available();
    }
  }
  
  
  @Override
  public String toString() {
    return command.toString();
  }
    
}
