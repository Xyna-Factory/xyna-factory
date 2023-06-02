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
package com.gip.xyna.xact;



import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.log4j.Logger;



//FIXME duplicate code, ist auch im scriptexecutorservice so ähnlich
public class ProcessUtil {

  // Das kill Programm liegt bei SunOS z.B. unter /usr/bin/kill (der Default),
  // bei Linux aber unter /bin/kill. Per System.property kann das killCommand
  // gesetzt werden.
  private static final String DEFAULT_KILL_CMD_SUN_OS = "/usr/bin/kill";
  private static final String DEFAULT_KILL_CMD_LINUX = "/bin/kill";

  private static final Logger logger = Logger.getLogger(ProcessUtil.class);

  private static final String[] KILL_COMMAND_POSSIBILITIES = new String[] {DEFAULT_KILL_CMD_LINUX, DEFAULT_KILL_CMD_SUN_OS};
  protected static String killCommand;
  static {
    boolean found = false;
    for (String nextKillCommand : KILL_COMMAND_POSSIBILITIES) {
      if (new File(nextKillCommand).exists()) {
        killCommand = nextKillCommand;
        found = true;
      }
      break;
    }
    if (!found) {
      StringBuilder sb = new StringBuilder();
      sb.append("Could not find <kill> command in one of the following: {");
      for (int i = 0; i < KILL_COMMAND_POSSIBILITIES.length; i++) {
        sb.append("'").append(KILL_COMMAND_POSSIBILITIES[i]).append("'");
        if (i != KILL_COMMAND_POSSIBILITIES.length - 1) {
          sb.append(", ");
        }
      }
      sb.append("}, killing script executions may not be possible.");
      logger.error(sb);
      killCommand = "kill";
    }
  }


  public void killProcess(Process p) {
    try {
      p.exitValue();
      return; //bereits tot
    } catch (IllegalThreadStateException e) {
      //ok noch am leben
    }
    int pid = findPid(p);
    if (pid == Integer.MIN_VALUE) {
      //mehr kann man nun auch nicht probieren...
      logger.warn("Could not get processId. Process may not be possible to kill.");
      p.destroy();
    } else {
      cancelScriptExecution(pid, p);
    }
  }


  private int findPid(Process process) {
    int pid;
    // return the process id (pid) to the caller
    // HACK to access a private field of java.lang.UNIXProcess
    if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
      Field field;
      try {
        field = process.getClass().getDeclaredField("pid");
      } catch (SecurityException e) {
        throw new RuntimeException("Failed to access field 'pid' within process object", e);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("Failed to access field 'pid' within process object", e);
      }
      field.setAccessible(true);
      try {
        pid = field.getInt(process);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException("Failed to access field 'pid' within process object", e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to access field 'pid' within process object", e);
      }
      logger.debug("process id " + pid);
      return pid;
    } else {
      return Integer.MIN_VALUE;
    }
  }


  public void cancelScriptExecution(long pid, Process process) {

    // Kein process.destroy() mehr probieren. Es tut irgendwie nicht das, was wir
    // brauchen. Unter Linux tut es zwar ueberhaupt etwas, aber es scheint die
    // Streams zu schliessen, so dass wir den output des beendeten Prozesses nicht
    // mehr lesen koennen (IOException). Unter Solaris scheint es ueberhaupt nicht
    // oder nicht immer zu funktionieren.

    String killCmd = killCommand + " -TERM " + pid; // FIXME: kill und Signal als Properties
    logger.debug("killing process: " + killCmd);
    boolean killCommandWorks = true;
    try {
      Runtime.getRuntime().exec(killCmd);
    } catch (IOException e) {
      killCommandWorks = false;
      logger.warn("could not execute kill command '" + killCmd + "'. trying process.destroy() ...", e);
      //dann funktioniert ein kill -KILL wohl auch nicht... also zumindest process.destroy probieren.
      process.destroy();
    }

    if (waitForProcessToDie(process, 20)) {
      return;
    }

    if (killCommandWorks) {
      logger.debug("process still running. Trying forcefully (kill -KILL) ...");
      try {
        killCmd = killCommand + " -KILL " + pid;
        Runtime.getRuntime().exec(killCmd);
      } catch (IOException e) {
        logger.warn("could not execute kill command '" + killCmd + "'", e);
      }

      if (waitForProcessToDie(process, 5)) {
        return;
      }
    }
    logger.warn("Could not kill process <" + pid + ">");
  }


  private boolean waitForProcessToDie(Process process, int maxCnt) {
    boolean processRunning = true;
    int cnt = 0;
    while (processRunning && cnt++ < maxCnt) {
      // Wenn der Process jetzt noch laeuft hat er vielleicht das QUIT Signal abgefangen
      // Dann noch mal mit KILL probieren
      try {
        logger.debug("process exited with exit code: " + process.exitValue());
        return true;
      } catch (IllegalThreadStateException x1) {
        //ok retry.
      }

      try {
        Thread.sleep(200 / maxCnt * cnt); //durchschnittlich 100ms warten, am anfang weniger, später mehr
      } catch (InterruptedException ex) {
      }
    }
    return false;
  }
}
