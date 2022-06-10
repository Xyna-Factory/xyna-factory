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

package xact.XScrpt.services;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandler;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.events.AbortServiceStepEvent;



public class Script implements ServiceStepEventHandler<AbortServiceStepEvent> {

  public static final int MIXED_MODE = 0; // stdout u. stderr zusammen
  public static final int SEPARATED_MODE = 1; // stdout u. stderr getrennt

  private static final Logger logger = CentralFactoryLogging.getLogger(Script.class);


  // Das kill Programm liegt bei SunOS z.B. unter /usr/bin/kill (der Default),
  // bei Linux aber unter /bin/kill. Per System.property kann das killCommand
  // gesetzt werden.
  public static final String DEFAULT_KILL_CMD_SUN_OS = "/usr/bin/kill";
  public static final String DEFAULT_KILL_CMD_LINUX = "/bin/kill";

  private static final String[] KILL_COMMAND_POSSIBILITIES = new String[] {DEFAULT_KILL_CMD_LINUX,
      DEFAULT_KILL_CMD_SUN_OS};
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
      sb.append("}, killing script executions wont be possible.");
      logger.warn(sb);
      killCommand = "kill";
    }
  }


  private static final String PATH_TO_SHELL = System.getProperty("SHELL");


  protected String command;
  protected Process process;
  protected long pid = -1;
  protected int exitCode = 0;
  protected boolean timedOut = false;

  protected InputStream stdout;
  protected InputStream stderr;

  protected StringBuffer scriptOutput;
  protected StringBuffer scriptError;

  private int mode;
  private String[] envp;

  protected Timer timer = null;
  protected TimerTask timerTask = null;
  protected long timeout = -1; // Default: no timeout


  /**
   * Constructor: Im Array envp koennen Umgebungsvariablen der Form name=value uebergeben werden.
   */
  public Script(String[] envp) {
    this.envp = envp;
    this.mode = MIXED_MODE; // Default

    this.scriptOutput = new StringBuffer();
    this.scriptError = new StringBuffer();

    logger.debug("Current working directory for script execution: " + getCwd());
  }


  /**
   * Es wird keine spezielle Umgebung (sondern die des Eltern-Processes) verwendet.
   */
  public Script() {
    this(null);
  }


  /**
   * Constructor with timeout
   */
  public Script(long timeout) {
    this(null);
    this.timeout = timeout;
  }


  /**
   * gets the current timeout
   */
  public long getTimeout() {
    return timeout;
  }


  /**
   * sets the timeout for the script
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }


  /**
   * does the script timed out
   */
  public boolean isTimedOut() {
    return timedOut;
  }


  /**
   * Liefert das aktuelle Arbeitsverzeichnis
   */
  public static String getCwd() {
    return new File(new File(".").getAbsolutePath()).getParent();
  }


  /**
   * Methode zum Aendern des Modus: MIXED oder SEPARATED. Im MIXED Mode werden die Ausgaben von stdout und stderr
   * zusammen in scriptOutput geliefert. Im SEPARATED Mode werden die Ausgaben von stderr nach scriptError geschrieben.
   */
  public void setMode(int mode) {
    if (mode == SEPARATED_MODE) {
      this.mode = mode;
    } else {
      this.mode = MIXED_MODE;
    }
  }


  /**
   * Externes Kommando ausfuehren. Gibt die Ausgabe des Kommandos (stdout) zurueck
   */
  public String exec(String command) throws Exception {
    pid = execGetPid(command);
    return getOutput();
  }


  /**
   * start an external command returns the pid of started (UINX) process
   * @throws IOException 
   */
  public long execGetPid(String command) throws IOException {

    //String[] _cmd = (shell != null && shell.length()>0 ? new String[]{shell, command} : new String[]{command});
    String _cmd[];

    // Sonderbehandlung, wenn Kommando mit einem '-' beginnt:
    // Keine Ausfuehrung mit $SHELL -c sondern direkt.
    // Dabei wird der String an whitespace gesplittet und in Array-Items
    // gepackt. Enthaelt das Kommando ein ',', so wird ab diesem der Rest
    // des Kommandos als ein Token betrachtet und wird komplett in das letzte
    // Array-Item gesteckt
    if (command.trim().charAt(0) == '-') {
      _cmd = StringHelper.splitCmd(command.substring(1)); // '-' vorher abschneiden

      if (logger.isDebugEnabled()) {
        StringBuilder dbg = new StringBuilder("Executing script \"");
        for (int i = 0; i < _cmd.length; i++) {
          dbg.append("(").append(i + 1).append(")").append(_cmd[i]);
          if (i < _cmd.length - 1)
            dbg.append(" ");
        }
        dbg.append(" ...");
        //OBLog.log.finer(dbg.toString());
        logger.debug(dbg.toString());
      }

      process = Runtime.getRuntime().exec(_cmd);
    } else if (PATH_TO_SHELL != null && PATH_TO_SHELL.length() > 0) {
      _cmd = new String[] {PATH_TO_SHELL, "-c", command.trim()};
      if (logger.isDebugEnabled()) {
        logger.debug("Executing script \"" + _cmd[0] + " " + _cmd[1] + " " + _cmd[2] + "\" ...");
      }
      process = Runtime.getRuntime().exec(_cmd);
    } else {
      // Start external process
      if (logger.isDebugEnabled()) {
        logger.debug("Executing script \"" + command + "\" ...");
      }
      process = Runtime.getRuntime().exec(command);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("process " + process.getClass().getName() + " started");
    }

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
    } else {
      logger.warn("Cannot get processId on non UNIX platform");
    }

    // setup timer
    if (timeout > 0) {
      logger.info("winding timer for script: " + timeout);
      final Script _script = this;
      timerTask = new TimerTask() {

        public void run() {
          _script.cancelScriptExecution();
        }
      };
      timer = new Timer();
      timer.schedule(timerTask, timeout);
    }

    return pid;
  }


  /**
   * This method must be called by the client first to get the script output and second to ensure that the process will
   * not block on filled streams
   */
  public String getOutput() throws Exception {
    stdout = process.getInputStream(); // stdout of external process
    stderr = process.getErrorStream(); // stderr of external process

    // Thread must *not* wait for ScriptJob, instead it reads its output
    // (Waiting for ScriptJob to end may cause deadlock because the
    // buffers (stdin, stdout) will be filled (by ScriptJob) but not read
    // (by this thread). So buffers will be filles to max and ScriptJob
    // will block on writing.
    return readProcessStreams(stdout, stderr); // Lesen bis Prozess fertig
  }


  /**
   * Liest die uebergebenen InputStreams aus. Wenn es nichts zu Lesen gibt (stdout.available() < 1 && stderr.available()
   * < 1) wird ermittelt, ob der schreibende Thread (externe Process) noch laeuft. Ist das der Fall, dann wartet der
   * Thread eine kurze Zeit (0.5 sec) bevor er den naechsten Leseversuch staret. Die Methode endet, entweder mit
   * Exception oder wenn der externe Process fertig ist. Liefert den exitValue des exterene Prozesses
   */
  protected String readProcessStreams(InputStream stdout, InputStream stderr) throws Exception {
    int avail_s, avail_e, count_s, count_e;
    String line;
    byte[] buf_s = new byte[1024]; // Lesepuffer fuer stdout
    byte[] buf_e = new byte[1024]; // Lesepuffer fuer stderr

    while (true) {
      while (true) {
        try {
          // Gibt es was von den Streams zu Lesen?
          avail_s = stdout.available();
          avail_e = stderr.available();

          if (avail_s < 1 && avail_e < 1) {
            // Es gibt nichts zu lesen, raus aus der innern Schleife,
            // Testen, ob der externe Prozess noch laeuft.
            break;
          }

          if (avail_s > 0) {
            // Es gibt was von stdout zu Lesen
            count_s = stdout.read(buf_s);
            line = new String(buf_s, 0, count_s);
            scriptOutput.append(line);
          }
          if (avail_e > 0) {
            // Es gibt was von stderr zu Lesen
            count_e = stderr.read(buf_e);
            line = new String(buf_e, 0, count_e);
            // Im MIXED_MODE wird auch stderr in den sbOutput Buffer
            // geschrieben
            if (mode == MIXED_MODE) {
              scriptOutput.append(line);
            } else {
              scriptError.append(line);
            }
          }
        } catch (IOException iox) {
          logger.debug("Got IOException while reading script output: " + iox.getMessage());
          break;
        }
      } // Ende innere (Lese-) Schleife 

      // Nachsehen, ob der externe Prozess noch laeuft
      // Die Methode process.exitValue() erzeugt eine Exception wenn
      // der Prozess noch laueft. Wenn sie keine Exception erzeugt,
      // ist der Prozess fertig und wir koennen auch die auessere
      // Schleife beenden.
      try {
        exitCode = process.exitValue();
        if (logger.isDebugEnabled()) {
          logger.debug("Script execution finished with exitCode " + exitCode + ", output: " + scriptOutput.toString());
        }
        break; // Ok, Prozess ist fertig, es gibt nix mehr zu lesen
      } catch (IllegalThreadStateException e) {
        // Der Prozess laeuft noch, also kurz warten, dann naechsten
        // Leseversuch starten
        try {
          Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
      }
    } // Ende auessere Schleife

    if (timer != null) {
      timer.cancel();
    }

    return scriptOutput.toString();
  }


  /**
   * Liefert den exitCode eines ausgefuehrten Scripts
   */
  public int getExitCode() {
    return exitCode;
  }


  /**
   * Liefert den scriptOutput Buffer
   */
  public StringBuffer getScriptOutput() {
    return scriptOutput;
  }


  /**
   * Liefert den scriptError Buffer
   */
  public StringBuffer getScriptError() {
    return scriptError;
  }


  public void cancelScriptExecution() {

    logger.debug("TIMEOUT or CANCEL, destroying process ...");

    // Kein process.destroy() mehr probieren. Es tut irgendwie nicht das, was wir
    // brauchen. Unter Linux tut es zwar ueberhaupt etwas, aber es scheint die
    // Streams zu schliessen, so dass wir den output des beendeten Prozesses nicht
    // mehr lesen koennen (IOException). Unter Solaris scheint es ueberhaupt nicht
    // oder nicht immer zu funktionieren.
    //process.destroy();
    if ( timer != null ) {
      timer.cancel();
      timedOut = true; //FIXME nur auf true setzen, wenn es auch timeout ist - nicht bei cancel
    }

    if (pid > 0) {
      String killCmd = killCommand + " -TERM " + pid; // FIXME: kill und Signal als Properties
      logger.info("killing process: " + killCmd);
      try {
        Runtime.getRuntime().exec(killCmd);
      } catch (IOException e) {
        logger.warn("could not execute kill command", e);
      }
      try {
        Thread.sleep(2000);
      } catch (InterruptedException ex) {
      }
      // Wenn der Process jetzt noch laeuft hat er vielleicht das QUIT Signal abgefangen
      // Dann noch mal mit KILL probieren
      try {
        logger.info("process exited with exit code: " + process.exitValue());
      } catch (IllegalThreadStateException x1) {
        logger.info("process still running. Trying forcefully (kill -KILL) ...");
        try {
          killCmd = killCommand + " -KILL " + pid;
          Runtime.getRuntime().exec(killCmd);
        } catch (IOException e) {
          logger.warn("Could not kill process <" + pid + ">", e);
        }
        try {
          Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
        // Wenn der process jetzt noch laeuft ist nichts mehr zu machen
        try {
          logger.info("process exited with exit code: " + process.exitValue());
        } catch (IllegalThreadStateException x2) {
          logger.error("process still running. giving up");
        }
      }
    } else {
      logger.error("cannot kill process since pid is not set!");
    }

  }


  public void handleServiceStepEvent(AbortServiceStepEvent event) {
    logger.info("Received CancelEvent, Reason: " + event.getAbortionReason());
    this.cancelScriptExecution();
  }


  public void safelyClose() {
    if (process != null) {
      ScriptExecutorServiceImpl.safelyCloseStream(process.getOutputStream());
      ScriptExecutorServiceImpl.safelyCloseStream(process.getErrorStream());
      ScriptExecutorServiceImpl.safelyCloseStream(process.getInputStream());
    }
  }

}
