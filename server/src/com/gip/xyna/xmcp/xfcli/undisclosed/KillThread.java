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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


public class KillThread implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) {

    Parameters p = Parameters.parse(allArgs, clw);
    if (p == null) {
      return;
    }

    //TODO parametrisierung über parameter die bei kill -3 sichtbar sind. 
    //"VM Thread" prio=10 tid=0x000000005f7cf800 nid=0x7d96 runnable  -> weder tid (adresse des threads) noch nid hängen eindeutig mit der thread id zusammen.
    
    
    Entry<Thread, StackTraceElement[]> targetThread = getThreadByID(p.id);
    if (targetThread == null) {
      clw.writeLineToCommandLine("No thread found for id <" + p.id + ">");
      return;
    }

    StackTraceElement[] stacktrace = targetThread.getValue();
    Thread t = targetThread.getKey();
    clw.writeLineToCommandLine("Found thread for id <" + p.id + ">, current stacktrace:");
    clw.writeLineToCommandLine("\t" + t.getName() + " id=" + t.getId() + " " + t.getState());
    for (StackTraceElement ste: stacktrace) {
      clw.writeLineToCommandLine("\t\t" + ste.getClassName() + "." + ste.getMethodName()
          + (ste.getLineNumber() > 0 ? ":" + ste.getLineNumber() : ""));
    }

    for (int i = 0; i < p.numberOfRepetitions; i++) {
      if (p.forceStop) {
        clw.writeLineToCommandLine("Sending <STOP>");
        targetThread.getKey().stop();
      } else {
        clw.writeLineToCommandLine("Sending <INTERRUPT>");
        targetThread.getKey().interrupt();
      }
      if (!t.isAlive()) {
        clw.writeLineToCommandLine("Thread is no longer alive.");
        return;
      }
      if (i+1<p.numberOfRepetitions) {
        try {
          Thread.sleep(p.sleep);
        } catch (InterruptedException e) {
          clw.writeLineToCommandLine("Got interrupted while waiting for next signal");
          return;
        }
      }
    }

    if (t.isAlive()) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
      if (t.isAlive()) {
        clw.writeLineToCommandLine("Thread is still alive.");
      }
    }
    if (!t.isAlive()) {
      clw.writeLineToCommandLine("Thread is no longer alive.");
    }

  }


  public static Entry<Thread, StackTraceElement[]> getThreadByID(long id) {
    Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
    for (Entry<Thread, StackTraceElement[]> e: allThreads.entrySet()) {
      if (e.getKey().getId() == id) {
        return e;
      }
    }
    return null;
  }


  private static class Parameters {

    long id = -1;
    boolean forceStop = false;
    int numberOfRepetitions = 1;
    int sleep = 0;

    private Parameters() {
    }

    public static Parameters parse(AllArgs allArgs, CommandLineWriter clw) {
      Parameters p = new Parameters();
      try {
        switch (allArgs.getArgCount()) {
          case 4:
            p.sleep = Integer.parseInt(allArgs.getArg(3));
            if (p.sleep < 0) {
              return null;
            }
          case 3 :
            p.numberOfRepetitions = Integer.parseInt(allArgs.getArg(2));
            if (p.numberOfRepetitions < 1) {
              p.numberOfRepetitions = 1;
            }
          case 2 :
            p.forceStop = Boolean.parseBoolean(allArgs.getArg(1));
          case 1 :
            p.id = Long.parseLong(allArgs.getArg(0));
            break;
          default :
            clw.writeLineToCommandLine("parameters are <thread id: long> [<force stop: boolean> [<repetitions: int> [<sleep ms between signals: int>]]]\n");
            return null;
        }
        return p;
      } catch (Exception e) {
        CentralFactoryLogging.getLogger(KillThread.class).debug("Failed to parse parameters", e);
        clw.writeLineToCommandLine("parameters are <thread id: long> [<force stop: booelan> [<repetitions: int> [<sleep ms between signals: int>]]]\n");
        return null;
      }
    }
  }
  
}
