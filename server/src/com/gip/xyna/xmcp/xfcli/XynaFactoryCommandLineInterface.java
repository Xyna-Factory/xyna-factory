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

package com.gip.xyna.xmcp.xfcli;



import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.PIDLocator;
import com.gip.xyna.ShutdownHookManagement;
import com.gip.xyna.ShutdownHookManagement.ShutdownHookType;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XynaFactoryLogLevel;
import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.exceptions.Ex_InaccessableSocketException;
import com.gip.xyna.utils.concurrent.AtomicEnum;
import com.gip.xyna.utils.exceptions.ExceptionHandler;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.classloading.XynaClassLoader;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaRunnable;



public class XynaFactoryCommandLineInterface extends Thread {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaFactoryCommandLineInterface.class);

  public static final String QUIET_SUFFIX = "-q";
  public static final String PORT_SUFFIX = "-p";
  public static final String PRINTGOT_SUFFIX = "-printGot";

  public static final String COMMAND_START = "start";
  public static final String COMMAND_STOP = "stop";
  public static final String COMMAND_HELP = "help";

  public static final String XYNA_FACTORY = "Xyna Factory";
  
  public static final String MAIN_THREAD_NAME = XynaFactory.class.getSimpleName() + " MAIN";
  
  private static final String PID_FOLDER_PROPERTY = "pid.folder";

  private final ServerSocket serverSocket;

  public static final int DEFAULT_SERVER_PORT = 4242;

  private static int serverPort = DEFAULT_SERVER_PORT;
  private static boolean quiet = false;
  private static boolean printGot = false;

  private static AtomicEnum<Status> status = AtomicEnum.of(Status.Initializing);
  public enum Status {
    Initializing, Running, ShuttingDown, Stopped;
  }
  
  private ReentrantLock lock = new ReentrantLock();


  public XynaFactoryCommandLineInterface() throws XynaException {
    
    try {
      // TODO: als trigger/filter realisieren?
      serverSocket = new ServerSocket(serverPort, 0, InetAddress.getByName(null));
      
      //Task zum Schließen des Sockets beim SchutdownHookManagement registrieren
      ShutdownHookManagement.getInstance().addTask(new Runnable() {
        public void run() {
          closeServerSocketSafely(serverSocket);
        }
      }, ShutdownHookType.CLOSE_SOCKET);
      
    } catch (IOException e) {
      shutdownWhenError();
      throw new Ex_InaccessableSocketException(serverPort, e);
    }
    init();
  }

  private static void closeSocketSafely(Socket socket) {
    if (socket != null) {
      try {
        if (!socket.isClosed()) {
          logger.info("Closing open CLI socket.");
          try {
            socket.shutdownOutput();
          } finally {
            socket.close();
          }
        }
      } catch (Throwable t2) {
        Department.handleThrowable(t2);
        logger.error(null, t2);
      }
    }
  }
  
  //kann leider nicht mit obiger methode verschmolzen werden
  private static void closeServerSocketSafely(ServerSocket socket) {
    if (socket != null) {
      try {
        if (!socket.isClosed()) {
          logger.info("Closing open CLI listener socket.");
          socket.close();
        }
      } catch (Throwable t2) {
        Department.handleThrowable(t2);
        logger.error(null, t2);
      }
    }
  }

  
  private class CommandLineRunnable extends XynaRunnable {

    public void run() {
      Socket socket = null;
      try {
        if (logger.isDebugEnabled()) {
          logger.debug(Thread.currentThread().getName() + " running...");
        }
        
        while ( status.isNot(Status.Stopped) ) {
          try {
            socket = openSocket();
            if (status.isNot(Status.Stopped)) {
              executeCommand(socket);
              //das Socket wird in executeCommand geschlossen!
            } else {
              try {
                CommandLineWriter clw = new CommandLineWriter(socket.getOutputStream());
                clw.writeString("Server shutting down.");
                clw.writeEndToCommandLine(ReturnCode.COMMUNICATION_FAILED);
              } catch (Throwable t) {
                //pech
              } finally {
                closeSocketSafely(socket);
              }
            }
          } catch(OutOfMemoryError t) {
            Department.handleThrowable(t);
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug(Thread.currentThread().getName() + " stopped.");
        }
      } finally {
   
      }
    }
    
    private Socket openSocket() {
      Socket socket = null;
      try {
        logger.trace("Waiting for server connection");
        socket = serverSocket.accept();
        if (logger.isTraceEnabled()) {
          logger.trace("Got connection to " + XYNA_FACTORY);
        }
        
      } catch (IOException e) {
        if (status.isNot(Status.Stopped) ) {
          logger.error(null, e);
          //socket wird im finally in run()  zugemacht
        }
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error(null, t);
        //socket wird im finally in run() zugemacht
      }
      return socket;
    }
    

    private void executeCommand(Socket socket) {
      lock.lock();
      try {
        try {
          int n = getNumberOfCLICommandsToBeExecutedNonPooled();
          if (n > 0) {
            decrementNumberOfCLICommandsToBeExecutedNonPooled();
            new Thread(new XynaFactoryCLIConnection(socket), "CLI Thread").start();
          } else {
            XynaExecutor.getInstance(false)
            .executeRunnableWithUnprioritizedPlanningThreadpool(new XynaFactoryCLIConnection(socket));
          }
        } catch (RejectedExecutionException e) {
          logger.error("server busy.", e);
          CommandLineWriter clw = new CommandLineWriter( socket.getOutputStream() );
          clw.writeString("server busy.");
          clw.writeEndToCommandLine(ReturnCode.REJECTED);
          closeSocketSafely(socket);
        }
      } catch (Throwable t) {
        //loggen, und ignorieren.
        Department.handleThrowable(t);
        logger.error("Could not start thread to execute command.", t);
        //socket schliessen
        try {
          CommandLineWriter clw = new CommandLineWriter( socket.getOutputStream() );
          clw.writeString("Could not start thread to execute command.");
          clw.writeEndToCommandLine(ReturnCode.COMMUNICATION_FAILED);
          closeSocketSafely(socket);
        } catch (Throwable t2) {
          //ignore. ist dan wahrscheinlich nichts neues
        }
      } finally {
        lock.unlock();
      }

    }

    private void decrementNumberOfCLICommandsToBeExecutedNonPooled() {
      if (status.is(Status.Running)) {
        try {
          XynaProperty.NUMBER_OF_CLI_COMMANDS_TO_BE_EXECUTED_NON_POOLED.set(XynaProperty.NUMBER_OF_CLI_COMMANDS_TO_BE_EXECUTED_NON_POOLED.get() - 1);
        } catch (PersistenceLayerException e) {
          logger.warn(null, e);
        }
      }
    }

    private int getNumberOfCLICommandsToBeExecutedNonPooled() {
      if (status.is(Status.Running)) {
        return XynaProperty.NUMBER_OF_CLI_COMMANDS_TO_BE_EXECUTED_NON_POOLED.get();
      }
      return 0;
    }
    
  }


  private void init() throws XynaException {
    Thread commandLineThread = new Thread(new CommandLineRunnable());
    commandLineThread.setName(XynaFactoryCLIConnection.class.getSimpleName() + " Spawning Thread");
    commandLineThread.setPriority(MAX_PRIORITY);
    commandLineThread.start();

    try {
      Thread.currentThread().setName(MAIN_THREAD_NAME);
      XynaFactoryBase xfi = XynaFactory.createServerInstance();
      xfi.init();
      logger.log(XynaFactoryLogLevel.Factory, "Xyna Factory is up and running");
      status.compareAndSet(Status.Initializing, Status.Running);

      // falls dabei etwas schief geht, muss shutdown durchgeführt werden
    } catch (XynaException xe) {
      shutdownWhenError();
      throw xe;
    } catch (RuntimeException re) {
      shutdownWhenError();
      throw re;
    } catch (Error e) {
      shutdownWhenError();
      throw e;
    } catch (Throwable t) {
      try {
        shutdownWhenError(); // einzige fehlermöglichkeit ist outofmemory oder sowas, alles andere wird in shutdown
                             // gefangen und geloggt
      } finally {
        Department.handleThrowable(t);
      }
      throw new RuntimeException(t);
    }
  }

  private void shutdownWhenError() {
    try {
      status.compareAndSet(Status.Initializing, Status.ShuttingDown);
      
      XynaClusteringServicesManagement.getInstance().setGlobalReadyForChange(true);

      XynaFactory.getInstance().shutdownComponents();
      logger.log(XynaFactoryLogLevel.Factory, XYNA_FACTORY + " has been shut down successfully after error.");

    } catch (Throwable t) {
      try {
        //falls im fehlerfall der shutdown nicht korrekt funktioniert, bleibt nix anderes übrig, als das ordentlich zu loggen 
        logger.fatal("Error shutting down " + XYNA_FACTORY + ".", t);
      } finally {
        Department.handleThrowable(t);
      }
    } finally {
      status.set( Status.Stopped );
    }

  }


  public void run() {

    //FIXME dieser thread sollte erst erstellt werden, wenn der server runtergefahren wird.
    status.compareAndSet(Status.Initializing, Status.Running );
    Thread.currentThread().setName(MAIN_THREAD_NAME);

    try {
      while (status.is(Status.Running) ) {
        Thread.sleep(100);
      }
    } catch (Throwable t) {
      try {
        logger.error(t);
      } finally {
        Department.handleThrowable(t);
      }
    } finally {

      try {

        logger.log(XynaFactoryLogLevel.Factory, "Initializing shutdown of " + XYNA_FACTORY + "...");

        status.compareAndSet(Status.Running, Status.ShuttingDown );

        logger.log(XynaFactoryLogLevel.Factory, XynaFactoryCommandLineInterface.XYNA_FACTORY + " shutting down.");
        XynaFactory.getInstance().shutdownComponents();

        boolean needToCallSystemExit = false;
        if (terminateXynaExecutor()) {
          logger.debug("Successfully shut down thread pools");
        } else {
          logger.info("Timeout while waiting for thread pools to shut down, shutting down JVM using System.exit()");
          needToCallSystemExit = true;
        }

        //threads, die am ende ruhig offen sein dürfen. alle anderen sollten von xyna vernichtet worden sein!
        HashSet<String> threadNamesNormal = new HashSet<String>();
        threadNamesNormal.add(MAIN_THREAD_NAME);
        threadNamesNormal.add("Reference Handler"); //java.lang.ref.Reference
        threadNamesNormal.add("Finalizer"); //java.lang.ref.Finalizer
        threadNamesNormal.add("DestroyJavaVM");
        threadNamesNormal.add("Signal Dispatcher");
        threadNamesNormal.add(XynaFactoryCLIConnection.class.getSimpleName() + " Spawning Thread"); //wird erst durch shutdownhook geschlossen


        Map<Thread, StackTraceElement[]> stacktraces = Thread.getAllStackTraces();
        if (stacktraces != null && stacktraces.size() > threadNamesNormal.size()) {
          needToCallSystemExit = true;
          if (logger.isTraceEnabled()) {
            Iterator<Entry<Thread, StackTraceElement[]>> iter = stacktraces.entrySet().iterator();
            while (iter.hasNext()) {
              Entry<Thread, StackTraceElement[]> e = iter.next();
              if (threadNamesNormal.contains(e.getKey().getName())) {
                continue;
              }
              logger.trace("Thread '" + e.getKey().getName()
                              + "' is active and will be killed, listing current stacktrace:");
              if (e.getValue() != null) {
                for (StackTraceElement stacktraceElement : e.getValue()) {
                  if (stacktraceElement != null)
                    logger.trace("\t" + stacktraceElement.toString());
                }
              }
            }
          }
        }

        status.compareAndSet(Status.ShuttingDown, Status.Stopped );
        //jetzt kann der socket thread zu gehen und das socket schliessen
        //das serverSocket wird in einem Shutdown-Hook geschlossen
        
        logger.log(XynaFactoryLogLevel.Factory, XYNA_FACTORY + " shut down.");
        if (needToCallSystemExit) {
          System.exit(0);
        }

      } catch (XynaException e) {
        XynaFault_ctype xf = ExceptionHandler.toXynaFault(e);
        logger.error("Error while shutting down " + XYNA_FACTORY + " (" + xf.getCode() + ")", e);
        System.exit(1);
      } catch (Throwable t) {
        logger.error("Unexpected error while shutting down " + XYNA_FACTORY + ".", t);
        System.exit(1);
      } finally {
        if (serverSocket != null) {
          try {
            serverSocket.close();
          } catch (IOException e) {
            logger.error("Could not close server socket", e);
          }
          logger.info("Console interface closed.");
        } else {
          logger.debug("Socket had not been started correctly");
        }
      }
    }

  }


  public static void main(String[] args) {

    try {

      if (args.length == 0) {
        System.out.println("Missing command. Try \"help\".");
        return;
      }

      int beginIndex = 0;
      while (args[beginIndex].trim().length() == 0) {
        beginIndex++;
        if (beginIndex >= args.length) {
          System.out.println("Missing command. Try \"help\".");
          return;
        }
      }

      for (int i = 0; i < 3; i++) {
        if (args[beginIndex].equals(QUIET_SUFFIX)) {
          if (quiet) {
            System.out.println("Quiet parameter '" + QUIET_SUFFIX + "' specified twice, aborting.");
            return;
          }
          quiet = true;
          beginIndex++;
        } else if( args[beginIndex].equals(PRINTGOT_SUFFIX) ) {
          printGot = true;
          beginIndex++;
        } else if (args[beginIndex].equals(PORT_SUFFIX)) {
          if (args.length < beginIndex + 2) {
            System.out.println("Missing port. Try \"help\".");
          }
          try {
            serverPort = Integer.valueOf(args[beginIndex + 1]);
          } catch (NumberFormatException e) {
            System.out.println("Could not parse port '" + args[beginIndex + 1] + "'.");
            return;
          }
          beginIndex += 2;
        }
        if (args.length < beginIndex + 1) {
          System.out.println("Missing command. Try \"help\".");
          return;
        }
        if (beginIndex == 0) {
          // no condition matched, it wont match if we just try again
          break;
        }
      }

      args[beginIndex] = args[beginIndex].trim().toLowerCase();
      String cmd = args[beginIndex];
      ReturnCode rc = null;
      // TODO it might be possible to automatically generate the help information so that it is also available when no factory is running.
      //      if (COMMAND_HELP.equals(cmd)) {
      //        System.out.println(XynaFactoryCLIConnection.HELP);
      //        rc = ReturnCode.SUCCESS;
      //      } else 
      if ( COMMAND_START.equals(cmd) ) {
        rc = startFactory(args); //ist erst fertig, wenn Factory wieder beendet ist!
      } else if ( com.gip.xyna.xmcp.xfcli.generated.Status.COMMAND_Status.equals(cmd) ) {
        rc = status(args, beginIndex);
      } else {
        try {
          rc = executeServerConnectionCommand(args, null, beginIndex);
        } catch (ConnectException ce) {
          System.out.println("Unable to execute command \"" + args[beginIndex] + "\": " + XYNA_FACTORY
                          + " is not running on port '" + serverPort + "'");
          rc = ReturnCode.COMMUNICATION_FAILED;
        }
      }
      System.exit(rc.getCode());
      
    } catch (Error e) {
      logger.error(null, e);
    } catch (RuntimeException e) {
      logger.error(null, e);
    }
    
  }
  
  /**
   * @param args 
   * @return 
   * 
   */
  private static ReturnCode startFactory(String[] args) {
    ReturnCode rc = getFactoryState(new String[]{com.gip.xyna.xmcp.xfcli.generated.Status.COMMAND_Status});
    if (rc != ReturnCode.STATUS_SERVICE_NOT_RUNNING) {
      String message = null;
      int code = rc.getCode();
      switch( rc ) {
        case STATUS_UP_AND_RUNNING:
          message = " already running";
          code = ReturnCode.STATUS_ALREADY_RUNNING.getCode(); //hier ist STATUS_UP_AND_RUNNING ein Fehler, deswegen Code umsetzen!
          break;
        case STATUS_STARTING:
          message = " is already starting";
          break;
        case STATUS_STOPPING:
          message = " is shutting down now";
          break;
        default:
          message = " is in unexpected state:";
      }
      System.out.println(XYNA_FACTORY + message + " (Status '"+rc.getMessage()+"')");
      System.exit(code);
    }
    if (!quiet) {
      System.out.println(XYNA_FACTORY + " is starting...");
    }
    savePidToFile();
            
    if (containsArg(args, "-Xthreadtime")) {
      //TODO clibefehl zum ein-/ausschalten zur laufzeit. hier trotzdem lassen um serverstart zeiten messen zu können 
      ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
      if (tbean.isThreadCpuTimeSupported()) {
        logger.debug("ThreadCPUTime supported and enabled");
        tbean.setThreadCpuTimeEnabled(true);
      } else {
        logger.debug("ThreadCPUTime not supported");
      }
      if (tbean.isThreadContentionMonitoringSupported()) {
        logger.debug("ThreadContentionTime supported and enabled");
        tbean.setThreadContentionMonitoringEnabled(true);
      } else {
        logger.debug("ThreadContentionTime not supported");
      }
    }
    if (containsArg(args, "-Xnosecman")) {
      //securitymanager gegen einen austauschen, der nichts tut
      Constants.NORMAL_SECURITY_MANAGER = false;
    }
    
    if (containsArg(args, "-Xpausescheduler")) {
      //Scheduler soll pausiert werden
      Constants.PAUSE_SCHEDULER_AT_STARTUP = true;
    }
    
    try {
      Class xs = XynaClassLoader.getInstance().loadClass(XynaFactoryCommandLineInterface.class.getName());
      // aufruf von start-methode. ohne reflection gibt es eine classcastexception, weil die klasse zu der man castet
      // hier in der klasse mit dem zugehörigen classloader geladen wurde und deshalb anders ist als
      // die von dem classloaderdispatcher geladene klasse.
      Object o = xs.getConstructor().newInstance();
      Method[] methods = o.getClass().getMethods();

      boolean foundStartMethod = false;
      for (Method m : methods) {
        // find the start method inherited from java.lang.Thread
        if (m.getName().equals("start")) {
          foundStartMethod = true;
          m.invoke(o, new Object[0]);
          break;
        }
      }

      if (!foundStartMethod) {
        // if this happens something really got messed up
        throw new RuntimeException("Could not locate start method in "
                        + XynaFactoryCommandLineInterface.class.getName());
      }

      // TODO testen, dann ausgeben:
      if (!quiet) {
        System.out.println(XYNA_FACTORY + " starting on port " + XynaFactoryCommandLineInterface.serverPort);
      }
      //FIXME kann so nicht funktionieren, wenn Klasse oben wirklich mit anderem ClassLoader geladen wird 
      ((XynaFactoryCommandLineInterface)o).join(); //TODO ermitteln, ob regulär beendet und ReturnCode korrekt setzen!
      return ReturnCode.SUCCESS;
          
    } catch (Throwable e) {
      e.printStackTrace();
      logger.error("Could not start " + XYNA_FACTORY + ".", e);
      return ReturnCode.GENERAL_ERROR;
    }

  }
  
  /**
   * @param beginIndex 
   * @param args 
   * @return
   */
  private static ReturnCode status(String[] args, int beginIndex) {
    ReturnCode rc = getFactoryState(deleteFirstElementsFromArray(args, beginIndex));
    
    if (!quiet) {
      System.out.println("Status: '" + rc.getMessage() + "'");
    }
    
    if(args.length > beginIndex + 1 && ("-v".equals(args[beginIndex + 1]) || "--verbose".equals(args[beginIndex + 1]))) {
      try {
        rc = executeServerConnectionCommand(new String[] {XynaFactoryCLIConnection.COMMAND_EXTENDED_STATUS}, null, 0);
      } catch (ConnectException e) {
        // nothing to do
        rc = ReturnCode.COMMUNICATION_FAILED; 
      }
    }
    return rc;
  }
  
  

  private static void savePidToFile() {
    String pidFileFolder = System.getProperty(PID_FOLDER_PROPERTY);
    final File f = pidFileFolder == null ? new File("xynafactory.pid") : new File(pidFileFolder, "xynafactory.pid");
    if (f.exists()) {
      //server wurde nicht korrekt runtergefahren => backup anlegen, evtl lebt prozess ja noch.
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
      sdf.setTimeZone(TimeZone.getTimeZone(Constants.DEFAULT_TIMEZONE));
      sdf.setLenient(false);
      File fBackup = new File(pidFileFolder, "xynafactory.pid." + sdf.format(new Date()));
      if (logger.isInfoEnabled()) {
        logger.info("file xynafactory.pid found. server was propably not shut down successfully. moving old file to "
            + fBackup.getAbsolutePath() + ".");
      }
      try {
        FileUtils.moveFile(f, fBackup);
      } catch (Ex_FileAccessException e) {
        logger.warn("could not move old pid file " + f.getAbsolutePath() + " to " + fBackup.getAbsolutePath() + ".", e);
      }
    }
    int pid = -1;
    try {
      pid = PIDLocator.getPid();
    } catch (NoClassDefFoundError e) {
      logger.warn("some class was missing while trying to locate pid. propably jdk is not installed correctly or xyna can not find tools.jar.",
                e);
    }
    String pidContent;
    if (pid == -1) {
      pidContent = "unknown";
      logger.warn("could not locate pid for running factory");
    } else {
      pidContent = String.valueOf(pid);
    }
    try {
      logger.info("writing pid <" + pidContent + "> to file <" + f.getAbsolutePath() + ">");
      FileUtils.writeStringToFile(pidContent, f);
    } catch (Ex_FileWriteException e) {
      logger.warn("could not create pid file " + f.getAbsolutePath() + ". pid=" + pid, e);
    }
    
    //Task zum Löschen des pid files beim ShutdownHookManagement registrieren
    ShutdownHookManagement.getInstance().addTask(new Runnable() {

      public void run() {
        if (!FileUtils.deleteFileWithRetries(f)) {
          logger.warn("could not delete pid file " + f.getAbsolutePath() + " on shutdown.");
        }
        if (status.isNot(Status.Stopped)) {
          logger.warn("Server was shut down forcefully. (status="+status.get()+")");
        }
        if (logger.isTraceEnabled()) {
          logger.trace("pid file deleted successfully.");
        }
      }
    }, ShutdownHookType.DELETE_PID);
  }


  private static boolean containsArg(String[] args, String parameter) {
    for (String arg : args) {
      if (arg.equalsIgnoreCase(parameter)) {
        return true;
      }
    }
    return false;
  }
  
  private static String[] deleteFirstElementsFromArray(String[] args, int numberOfElementsToRemove) {
    String[] tmp = new String[args.length - numberOfElementsToRemove];
    System.arraycopy(args, numberOfElementsToRemove, tmp, 0, tmp.length);
    args = tmp;
    return args;
  }

  /**
   * @throws ConnectException falls server nicht running
   */
  private static ReturnCode executeServerConnectionCommand(String[] args, List<String> resultLineContainer, int argsBeginIdx)
      throws ConnectException {
    // mit xyna instanz verbinden
    ReturnCode returnCode = ReturnCode.COMMUNICATION_FAILED;
    Socket s = new Socket();
    try {

      s.connect(new InetSocketAddress("127.0.0.1", serverPort));

      OutputStream os = s.getOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(os);
      if (argsBeginIdx > 0) {
        args = deleteFirstElementsFromArray(args, argsBeginIdx);
        argsBeginIdx = 0;
      }
      oos.writeObject(args);
      oos.flush();
      s.getOutputStream().flush();

      BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), Constants.DEFAULT_ENCODING));
      String inputLine;
      while ((inputLine = br.readLine()) != null) {
        if( inputLine.startsWith(XynaFactoryCLIConnection.END_OF_STREAM) ) {
          String rc = inputLine.substring(XynaFactoryCLIConnection.END_OF_STREAM.length());
          try {
            returnCode = ReturnCode.valueOf(rc);
          } catch( IllegalArgumentException e ) {
            returnCode = ReturnCode.COMMUNICATION_FAILED;
          }
          break;
        }
        if (resultLineContainer == null) {
          if (!quiet) {
            if( printGot ) {
              System.out.println("got: " + inputLine);
            } else {
              System.out.println(inputLine);
            }
          }
        } else {
          resultLineContainer.add(inputLine);
        }
      }
      if (!s.isClosed()) {
        s.shutdownInput();
        s.shutdownOutput();
        s.close();
      }

    } catch (ConnectException e) {
      throw e;
    } catch (SocketException se) {
      //wahrscheinlich konnte man sich noch connecten und während der kommunikation wurde die verbindung geschlossen
      if (args[argsBeginIdx].equals(XynaFactoryCLIConnection.COMMAND_STOP)) {
        // fehler ignorieren. server wurde runtergefahren, stop soll nicht fehlschlagen
      } else {
        //auch wenn command status ist, connectexception werfen
        throw (ConnectException) new ConnectException().initCause(se);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if( returnCode == ReturnCode.SUCCESS ) {
      if (printGot && !quiet) {
        System.out.println("got: ok");
      }
    }
    return returnCode;
  }

  public static void shutdown() {
    status.compareAndSet(Status.Initializing, Status.ShuttingDown); //TODO nötig?
    status.compareAndSet(Status.Running, Status.ShuttingDown);
  }

  private boolean terminateXynaExecutor() {

    XynaExecutor.getInstance().shutdown();

    int timeoutCounter = 0;
    while (!XynaExecutor.getInstance().isTerminated()) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        logger.debug("Got interrupted while waiting for thread pools to shutdown, ignoring...");
      }
      if (timeoutCounter++ > 200) {
        return false;
      }
    }

    return true;
  }
  
  private static ReturnCode getFactoryState(String[] args) {
    ReturnCode state = null;
    try {
      ArrayList<String> resultLines = new ArrayList<String>();
      state = executeServerConnectionCommand(args, resultLines, 0);
    } catch (ConnectException e) {
      state = ReturnCode.STATUS_SERVICE_NOT_RUNNING;
    }
    return state;
  }

}
