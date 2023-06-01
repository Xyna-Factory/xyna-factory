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
package xact.XScrpt.services;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import xact.XScrpt.datatypes.ScriptArgument;
import xact.XScrpt.datatypes.ScriptCallString;
import xact.XScrpt.datatypes.ScriptExecutionException;
import xact.XScrpt.datatypes.ScriptExecutionParams;
import xact.XScrpt.datatypes.ScriptExecutionResult;
import xact.XScrpt.datatypes.ScriptExpectedReturnValue;
import xact.XScrpt.datatypes.ScriptImplementation;
import xact.XScrpt.datatypes.ScriptInput;
import xact.XScrpt.datatypes.ScriptOutputMode;
import xact.XScrpt.datatypes.ScriptTimeout;

import com.gip.xyna.Department;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xprc.exceptions.XPRC_TTLExpirationBeforeHandlerRegistration;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;


public class ScriptExecutorServiceImpl implements DeploymentTask {

  static Logger logger = Logger.getLogger(ScriptExecutorServiceImpl.class);


  protected ScriptExecutorServiceImpl() {
  }


  public void onDeployment() {
    // do something on deployment, if required
    // this is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() {
    // do something on undeployment, if required
    // this is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public static ScriptExecutionResult scriptExecutorServiceWithMode(ScriptInput scriptInput,
                                                                    List<? extends ScriptArgument> args,
                                                                    ScriptExpectedReturnValue expectedReturnValueI,
                                                                    ScriptTimeout timeoutInteger,
                                                                    ScriptOutputMode mixedMode)
      throws ScriptExecutionException {
    List<String> newArgs = new ArrayList<String>();
    for (ScriptArgument arg : args) {
      newArgs.add(arg.getArgument());
    }
    return scriptExecutorServiceWithMode(scriptInput, newArgs, expectedReturnValueI.getValue(),
                                         timeoutInteger.getTimeout(), mixedMode.getMixedMode());
  }


  private static ScriptExecutionResult scriptExecutorServiceWithMode(ScriptInput scriptInput, List<String> args,
                                                                     Integer expectedReturnValueI,
                                                                     Integer timeoutInteger, Boolean mixedMode)
      throws ScriptExecutionException {

    ScriptExecutionResult scriptExecutionResult = new ScriptExecutionResult();

    // If param is a script implementation instead of a callstring 
    File tmpFile = null;

    String scriptCallString = "";

    if (scriptInput instanceof ScriptImplementation) {
      ScriptImplementation impl = (ScriptImplementation) scriptInput;
      try {
        tmpFile = saveImplementationAsTmpFile(impl.getContent());
        scriptCallString = tmpFile.getCanonicalPath();
      } catch (IOException e) {
        ScriptExecutionResult res = new ScriptExecutionResult();
        res.setScriptError("Cannot save script implementation as tmp file in hdd.");
        throw new ScriptExecutionException(res, e);
      }
    } else if (scriptInput instanceof ScriptCallString) {
      ScriptCallString callstr = (ScriptCallString) scriptInput;
      scriptCallString = callstr.getCallString();
    } else {
      if (scriptInput != null
          && (scriptInput.getClass().getName().equals(ScriptCallString.class.getName()) || scriptInput.getClass()
              .getName().equals(ScriptImplementation.class.getName()))) {
        throw new RuntimeException("Classloader of input <" + scriptInput + "> was <"
            + scriptInput.getClass().getClassLoader() + ">. Expecting <" + ScriptCallString.class.getClassLoader()
            + "> or <" + ScriptImplementation.class.getClassLoader() + ">.");
      }
      ScriptExecutionResult res = new ScriptExecutionResult();
      res.setScriptError("Type of input value is not supported. Type was: "
          + scriptInput.getClass().getName());
      throw new ScriptExecutionException(res);
    }

    // Concatenate script arguments to callstring
    if (args != null) {
      for (String s : args) {
        scriptCallString += " ";
        scriptCallString += s;
      }
    }

    long processid = 0;
    logger.debug("ExecutionType: Extern: " + scriptCallString);

    int exitCode = -1;

    Script script = new Script();
    
    try {

      if ( mixedMode.booleanValue() ) {
        script.setMode(Script.MIXED_MODE);
      } else {
        script.setMode(Script.SEPARATED_MODE);
      }
      
      //        ServiceStepEventSource eventSource = ServiceStepEventHandling.getEventSource();
      //        eventSource.listenOnCancelEvents(script);

      long startTime = System.currentTimeMillis();
      if (timeoutInteger != null) {
        script.timeout = timeoutInteger * 1000;
      } else {
        script.timeout = 0;
      }

      // Cancel ermoeglichen!
      ServiceStepEventSource eventSource = ServiceStepEventHandling.getEventSource();
      if (eventSource == null) {
        throw new RuntimeException("Failed to obtain event source. "
            + "Please make sure that the XMOM XML file has not been modified.");
      }
      try {
        eventSource.listenOnAbortEvents(script);
      } catch (XPRC_TTLExpirationBeforeHandlerRegistration e) {
        scriptExecutionResult.setScriptError("Execution timeout while executing script " + scriptCallString + ": " + e.getMessage());
        scriptExecutionResult.setReturnValue(127); // FIXME what is 127??
        throw new ScriptExecutionException(scriptExecutionResult, e);
      }

      // Starten der Ausfuehrung!
      try {
        processid = script.execGetPid(scriptCallString);
      } catch (IOException e1) {
        scriptExecutionResult.setScriptError("Error while executing script " + scriptCallString + ": " + e1.getMessage());
        scriptExecutionResult.setReturnValue(127); // FIXME what is 127??
        throw new ScriptExecutionException(scriptExecutionResult, e1);
      }

      logger.info("Script was started with processId: " + processid);

      Object lock = new Object();
      OutputGetter outputGetter = new OutputGetter(script, lock);
      outputGetter.start();

      // Output-lesende Methode lockt per synchronized
      // => HIER warten, bis "lock" freigegeben wird (= Stream geschlossen = Ausfuehrung beendet)
      synchronized (lock) {
        while (!outputGetter.executionCompleted) {
          // Pruefung, falls DIESER Thread zuerst das Lock bekommen haben sollte 
          // => Zur Sicherheit Run-Status ueberpruefen!
          try {
            lock.wait(1000);
          } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected interruption of thread during execution of abortable ScriptExecutorService", e);
          }
        }

        logger.debug("Script execution finished");
      }

      long endTime = System.currentTimeMillis();
      int duration = -1;
      try {
        duration = Integer.parseInt(Long.toString((endTime - startTime) / 1000));
      } catch (Exception e) {
        logger.error("Cannot calculate duration", e);
      }

      String stdErr = script.getScriptError().toString();
      String stdOut = script.getScriptOutput().toString();

      stdOut = (stdOut == null) ? "" : stdOut;
      stdErr = (stdErr == null) ? "" : stdErr;

      logger.debug("ScriptOutput: " + stdOut);

      exitCode = script.getExitCode();

      if (exitCode != 0) {
        logger.debug("exitCode: " + exitCode + ", script failed, timed out or canceld");

        if (script.isTimedOut()) {
          logger.debug("script timed out");
          stdOut += "\n\nThe execution timed out!";
        }

        //if ((exitCode == 9)) {
        if ((exitCode == 15)) { // SIGTERM
          logger.debug("script was killed");
          stdOut += "\n\nThe execution was aborted!";
        }

      }

      scriptExecutionResult.setScriptOutput(stdOut);
      scriptExecutionResult.setScriptError(stdErr);
      scriptExecutionResult.setReturnValue(exitCode);
      scriptExecutionResult.setDuration(duration);

      if (expectedReturnValueI != null) {
        int expRetVal = expectedReturnValueI;
        if (expRetVal != exitCode) {
          throw new ScriptExecutionException(scriptExecutionResult);
        }
      }

      return scriptExecutionResult;
    } finally {
      try {
        script.safelyClose();
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.info("Failed to close script", t);
      }
      // Delete tmpFile
      if (tmpFile != null) {
        try {
          tmpFile.delete();
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.debug( "Could not remove temporary file: " + tmpFile.getAbsolutePath(), t);
        }
      }
    }
  }
  

  public static ScriptExecutionResult ScriptExecutorService(ScriptExecutionParams scriptExecutorInputType)
      throws ScriptExecutionException {

    ScriptInput scriptInput = scriptExecutorInputType.getScriptInput();
    List<String> args = scriptExecutorInputType.getArguments();
    Integer expectedReturnValueI = scriptExecutorInputType.getExpectedReturnValue();
    Integer timeoutInteger = scriptExecutorInputType.getTimeout();
    return scriptExecutorServiceWithMode(scriptInput, args, expectedReturnValueI, timeoutInteger, true);
  }


  private static File saveImplementationAsTmpFile(String impl) throws IOException {

    File file = File.createTempFile("xyna", "xact.XScrpt");
    try {
      FileOutputStream fos = new FileOutputStream(file);
      try {
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        try {
          BufferedWriter writer = new BufferedWriter(osw);
          try {
            writer.write(impl);
            writer.flush();
          } finally {
            try {
              writer.close();
            } catch (IOException e) {
              logger.error("Cannot close file writer", e);
            }
          }
        } finally {
          osw.close();
        }
      } finally {
        fos.close();
      }

      String cmd[] = {"chmod", "777", file.getAbsolutePath()};
      Process p = Runtime.getRuntime().exec(cmd);
      try {
        int exitCode = p.waitFor();
        if (exitCode != 0) {
          logger.error("Cannot change file permissions for tmp file");
        }
      } catch (InterruptedException e) {
        logger.error("Cannot change file permissions for tmp file", e);
      } finally {
        safelyCloseStream(p.getErrorStream());
        safelyCloseStream(p.getInputStream());
        safelyCloseStream(p.getOutputStream());
      }

      return file;
    } catch (IOException e) {
      file.delete();
      throw e;
    } catch (RuntimeException e) {
      file.delete();
      throw e;
    } catch (Error e) {
      file.delete();
      throw e;
    }

  }


  static void safelyCloseStream(Closeable toBeClosed) {
    try {
      toBeClosed.close();
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.info("Failed to close stream", t);
    }
  }

}
