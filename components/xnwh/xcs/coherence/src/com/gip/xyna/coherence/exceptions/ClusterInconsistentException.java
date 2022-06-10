/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.coherence.exceptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.utils.debugging.Debugger;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;


public class ClusterInconsistentException extends RuntimeException {

  private static final Logger logger = LoggerFactory.getLogger(ClusterInconsistentException.class);
  
  private static final long serialVersionUID = 4390579541932073515L;


  public ClusterInconsistentException(String msg) {
    super("Cluster is inconsistent: " + msg + ", thread=" + Thread.currentThread());
    printDebugInfos(defaultUTCSimpleDateFormat().format(new Date()) + " Cluster is inconsistent: " + msg + "\n");
  }


  public ClusterInconsistentException(String msg, Throwable cause) {
    super("Cluster is inconsistent: " + msg + ", thread=" + Thread.currentThread(), cause);
    StringWriter sw = new StringWriter();
    cause.printStackTrace(new PrintWriter(sw));
    printDebugInfos(defaultUTCSimpleDateFormat().format(new Date()) + " Cluster is inconsistent: " + msg + "\n"
        + sw.toString() + "\n");
  }


  private static SimpleDateFormat defaultUTCSimpleDateFormat() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    sdf.setLenient(false);
    return sdf;
  }


  private void printDebugInfos(String msg) {
    File f = new File("xllci.debug");
    int cnt = 0;
    while (true) {
      while (f.exists()) {
        f = new File("xllci.debug." + cnt);
        cnt++;
      }
      try {
        if (f.createNewFile()) {
          break;
        }
        //else: anderer thread war schneller
      } catch (IOException e) {
        logger.warn("file " + f.getAbsolutePath() + " could not be created to write debug-info to", e);
        return;
      }
    }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
      fos.write(msg.getBytes("UTF8"));
      Debugger.getDebugger().writeCSVToStream(fos);
    } catch (FileNotFoundException e) {
      logger.warn("file not found " + f.getAbsolutePath(), e);
    } catch (IOException e) {
      logger.warn("could not write debug info to file " + f.getAbsolutePath(), e);
    } finally {
      try {
        fos.close();
      } catch (IOException e) {
        logger.warn("could not close file " + f.getAbsolutePath(), e);
      }
    }
  }

}
