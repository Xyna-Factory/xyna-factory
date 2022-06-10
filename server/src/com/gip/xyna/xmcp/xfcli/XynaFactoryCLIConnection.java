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

package com.gip.xyna.xmcp.xfcli;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;
import com.gip.xyna.update.UpdateGeneratedClasses;
import com.gip.xyna.utils.exceptions.ExceptionHandler;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.PeekInputStream;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.exceptions.XMCP_INVALID_PARAMETERNUMBER;
import com.gip.xyna.xmcp.xfcli.CLIRegistry.CommandIdentification;
import com.gip.xyna.xmcp.xfcli.CLIRegistry.ResolveResult;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter.CommandLineWriterIOException;
import com.gip.xyna.xmcp.xfcli.generated.Forceexit;
import com.gip.xyna.xmcp.xfcli.generated.Listsysteminfo;
import com.gip.xyna.xmcp.xfcli.generated.Listthreadpoolinfo;
import com.gip.xyna.xmcp.xfcli.generated.Loadlogproperties;
import com.gip.xyna.xmcp.xfcli.generated.Status;
import com.gip.xyna.xmcp.xfcli.generated.Version;
import com.gip.xyna.xmcp.xfcli.generation.CommandLineArgumentJavaGenerator;
import com.gip.xyna.xmcp.xfcli.undisclosed.BashCompletion;
import com.gip.xyna.xmcp.xfcli.undisclosed.CheckClassloader;
import com.gip.xyna.xmcp.xfcli.undisclosed.CleanODSMappings;
import com.gip.xyna.xmcp.xfcli.undisclosed.DiffApplications;
import com.gip.xyna.xmcp.xfcli.undisclosed.ExplainClassMapFilter;
import com.gip.xyna.xmcp.xfcli.undisclosed.ExtendedStatus;
import com.gip.xyna.xmcp.xfcli.undisclosed.GenerateCode;
import com.gip.xyna.xmcp.xfcli.undisclosed.Help;
import com.gip.xyna.xmcp.xfcli.undisclosed.KillThread;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListBatchProcessManagementInfo;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListClassloaderInfo;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListExtendedSchedulerInfo;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListFurtherInformationFromStartup;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListRmiInfo;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListSuspendResumeInfo;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListSynchronizationEntries;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListThreadInfo;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListTimeConstraintManagementInfo;
import com.gip.xyna.xmcp.xfcli.undisclosed.ListVetoCache;
import com.gip.xyna.xmcp.xfcli.undisclosed.PrintOrderTimings;
import com.gip.xyna.xmcp.xfcli.undisclosed.PrintStorableCode;
import com.gip.xyna.xmcp.xfcli.undisclosed.RemoveVetoCacheEntry;
import com.gip.xyna.xmcp.xfcli.undisclosed.ResolveAndListPhantoms;
import com.gip.xyna.xmcp.xfcli.undisclosed.ResumeOrder;
import com.gip.xyna.xmcp.xfcli.undisclosed.ShowPersistenceLayerDetails;
import com.gip.xyna.xmcp.xfcli.undisclosed.Stop;
import com.gip.xyna.xmcp.xfcli.undisclosed.SuspendOrder;
import com.gip.xyna.xmcp.xfcli.undisclosed.ValidateClassloaders;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusImpl;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagement;
import com.gip.xyna.xprc.XynaRunnable;



/**
 * client verbindung zum server
 */
public class XynaFactoryCLIConnection extends XynaRunnable {

  public static final String END_OF_STREAM = "ENDOFSTREAM_";
  
  private static NullOutputStream nullOutputStream = new NullOutputStream();

  private static Logger logger = CentralFactoryLogging.getLogger(XynaFactoryCLIConnection.class);

  @Deprecated // use createuser with -e (encrypted) flag instead
  public static final String COMMAND_CREATE_USER_WITH_HASHPASS = "createuserhashpass"; //intentional not included in help

  public static final String COMMAND_STOP = "stop";
  public static final String COMMAND_START = "start";
  public static final String COMMAND_RESTART = "restart ";
  
  public static final String COMMAND_EXTENDED_STATUS = "extendedstatus";
  public static final String COMMAND_LIST_FURTHER_INFORMATION_FROM_STARTUP = "listfurtherinformationfromstartup";

  public static final String COMMAND_LIST_CLASSLOADER_STATISTICS = "listclassloaderinfo";
  public static final String COMMAND_THREAD_DUMP = "listthreadinfo";
  public static final String COMMAND_KILL_THREAD = "killthread";
  public static final String COMMAND_PRINTORDERTIMINGS = "printordertimings";
  
  public static final String COMMAND_LIST_SYNCHRONIZATION_ENTRIES = "listsynchronizationentries";
  public static final String COMMAND_EXPORT_CODEGROUPS = "exportcodegroups";
  public static final String COMMAND_IMPORT_CODEGROUPS = "importcodegroups";
  public static final String COMMAND_LIST_RMI_INFO = "listrmiinfo";

  public static final String COMMAND_ENCRYPT_FOR_SECURE_STORAGE = "encrypt";
  
  private static final String COMMAND_PERSISTENCELAYER_DETAILS = "showpldetails";
  private static final String COMMAND_CODEACCESS_DETAILS = "showcodeaccessdetails";
  
  public static final String COMMAND_CALL_GC = "gc";
  public static final String COMMAND_LIST_PHANTOMS = "phantoms";
  
  public static final String COMMAND_XMOM_DISCOVERY = "xmomdiscovery";
  
  public static final String COMMAND_ABORT_FETCHERS = "abortfetch";
  
  public static final String COMMAND_XMOM_REGENERATE = "xmomregenerate";

  public static final String COMMAND_HELP = "help";
  public static final String COMMAND_BASH_COMPLETION = "bashcompletion";

  private static final String COMMAND_LIST_EXTENDED_SCHEDULER_INFORMATION = "listextendedschedulerinfo";
  
  private static final String COMMAND_EXPLAIN_CLASSMAPFILTERS = "explainclassmapfilters";
  
  private static final String COMMAND_GENERATE_CODE = "generatecode";


  private static final List<String> COMMANDS_ALLOWED_ON_STARTUP_OR_SHUTDOWN = 
      Arrays.asList( Forceexit.COMMAND_Forceexit, COMMAND_EXTENDED_STATUS, Status.COMMAND_Status, COMMAND_HELP,
                     COMMAND_THREAD_DUMP, COMMAND_LIST_RMI_INFO, COMMAND_KILL_THREAD, Loadlogproperties.COMMAND_Loadlogproperties,
                     Listthreadpoolinfo.COMMAND_Listthreadpoolinfo, Listsysteminfo.COMMAND_Listsysteminfo, COMMAND_KILL_THREAD,
                     COMMAND_LIST_CLASSLOADER_STATISTICS, Version.COMMAND_Version
          );
  
  public interface CommandExecution {
    void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception;
  }

  
  private static final Map<String,CommandExecution> executionMap = new HashMap<String,CommandExecution>();
  static {
    executionMap.put("listbatchprocessmanagementinfo", new ListBatchProcessManagementInfo() );
    executionMap.put("listtimeconstraintmanagementinfo", new ListTimeConstraintManagementInfo() );
    executionMap.put("listsuspendresumeinfo", new ListSuspendResumeInfo() );
    executionMap.put("listvetocache", new ListVetoCache() );
    executionMap.put("removevetocacheentry", new RemoveVetoCacheEntry() );
    executionMap.put("resumeorder", new ResumeOrder() );
    executionMap.put("suspendorder", new SuspendOrder() );
    executionMap.put(COMMAND_HELP, new Help() );
    executionMap.put(COMMAND_BASH_COMPLETION, new BashCompletion() );
    executionMap.put(COMMAND_THREAD_DUMP, new ListThreadInfo() );
    executionMap.put(COMMAND_KILL_THREAD, new KillThread());
    executionMap.put(COMMAND_PRINTORDERTIMINGS, new PrintOrderTimings());
    executionMap.put(COMMAND_PERSISTENCELAYER_DETAILS, new ShowPersistenceLayerDetails() );
    executionMap.put(COMMAND_LIST_RMI_INFO, new ListRmiInfo() );
    executionMap.put(COMMAND_LIST_SYNCHRONIZATION_ENTRIES, new ListSynchronizationEntries() );
    executionMap.put(COMMAND_EXTENDED_STATUS, new ExtendedStatus() );
    executionMap.put(COMMAND_LIST_FURTHER_INFORMATION_FROM_STARTUP, new ListFurtherInformationFromStartup() );
    executionMap.put(COMMAND_LIST_CLASSLOADER_STATISTICS, new ListClassloaderInfo() );
    executionMap.put(COMMAND_LIST_PHANTOMS, new ResolveAndListPhantoms() );
    executionMap.put(COMMAND_STOP, new Stop() );
    executionMap.put(COMMAND_CREATE_USER_WITH_HASHPASS, new CreateUserWithHashpass() );
    executionMap.put(COMMAND_CALL_GC, new GarbageCollection() );
    executionMap.put(COMMAND_EXPORT_CODEGROUPS, new ExportExceptionCodeGroups() );
    executionMap.put(COMMAND_IMPORT_CODEGROUPS, new ImportExceptionCodeGroups() );
    executionMap.put(COMMAND_XMOM_DISCOVERY, new XmomDiscovery() );
    executionMap.put(COMMAND_ABORT_FETCHERS, new AbortFetchers() );
    executionMap.put(COMMAND_XMOM_REGENERATE, new XmomRegenerate() );
    executionMap.put(COMMAND_LIST_EXTENDED_SCHEDULER_INFORMATION, new ListExtendedSchedulerInfo() );
    executionMap.put(COMMAND_EXPLAIN_CLASSMAPFILTERS, new ExplainClassMapFilter() );
    executionMap.put(COMMAND_GENERATE_CODE, new GenerateCode() );
    executionMap.put("diffapplications", new DiffApplications() );
    executionMap.put("checkclassloader", new CheckClassloader() );
    executionMap.put("validateclassloaders", new ValidateClassloaders() );
    executionMap.put("printstorablecode", new PrintStorableCode() );
    executionMap.put("cleanodsmappings", new CleanODSMappings());
  }
  
  public static Map<String, CommandExecution> getUndisclodedCommandExecutionMap() {
    return executionMap;
  }
  
  private Socket socket;
  
  private volatile AllArgs allArgs;


  public XynaFactoryCLIConnection(Socket socket) {
    super("XynaFactoryCommandLineInterface");
    this.socket = socket;
  }


  @Override
  public String toString() {
    if( allArgs == null && logger.isTraceEnabled() ) {
      fillAllArgsFromSocket(socket);
    }
    if( allArgs != null ) {
      return "XynaFactoryCommandLineInterface("+Arrays.asList(allArgs)+")";
    } else {
      return "XynaFactoryCommandLineInterface(unknown)";
    }
  }
  
  private void fillAllArgsFromSocket(Socket socket) {
    synchronized (socket) {
      if( allArgs != null ) {
        return;
      }
      try {
        //Achtung: es darf nicht bis zum Ende gelesen werden, da dann Socket-InputStream blockiert!
        //ObjectInputStream macht das bereits, ByteArrayOutputStream darf nicht mit StreamUtils
        //kopiert werden, es muss bei "End of Transmission" angehalten werden. 
        PeekInputStream pis = new PeekInputStream(socket.getInputStream(), 2);
        int[] peeked = pis.peek();
        String[] args = null;
        if( peeked[0] == 0xAC && peeked[1]==0xED ) {
          //Serialisierte Daten!
          ObjectInputStream ois = new ObjectInputStream(pis);
          Object argsAsObject;
          try {
            argsAsObject = ois.readObject();
          } finally {
            ois.close();
          }
          args = (String[]) argsAsObject;
          allArgs = new AllArgs();
          allArgs.setCommand(args[0]);
          allArgs.setArgs( Arrays.asList(args).subList(1,args.length) );
        } else {
          ArgsParser ap = new ArgsParser();
          allArgs = ap.parse(pis);
        }
      } catch (Exception e) { //IOException, ClassNotFoundException
        logger.error("Could not read socket", e);
      }
    }
  }
  
  private static class ArgsParser {
    private static final int EOT = 4; //4=EOT End of Transmission
    private static final int GS = 29; //\035=\x1D=GS Group Separator
    private static final int RS = 30; //'\036'; //\036=\x1E=RS Record Separator
    
    private enum Mode {
      command, args, additional, error;
      public Mode next() {
        switch( this ) {
          case command: return args;
          case args: return additional;
          case additional : 
          default:
            throw new IllegalStateException("no next mode defined after "+this);
        }
      }
    };
    
    public AllArgs parse(PeekInputStream pis) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      AllArgs allArgs = new AllArgs();
      Mode mode = Mode.command;
      int r = 0;
      do {
        r = pis.read();
        if( r == EOT ) {
          break; //nicht bis zum Blockieren lesen!
        }
        if( r == RS ) {
          //Parameter eintragen
          addToAllArgs(allArgs, mode, baos );
          if( mode == Mode.command ) {
            //Abwärtskompatibilität für alte Übergabe: Weiterschalten zum nächsten Mode
            mode = Mode.args;
          }
          continue; //r nicht in baos eintragen
        }
        if( r == GS ) {
          if( mode == Mode.command ) {
            //nur Command eintragen
            addToAllArgs(allArgs, mode, baos );
          }
          mode = mode.next(); //weiterschalten zum nächsten Mode
          continue; //r nicht in baos eintragen
        }
        baos.write(r);
      } while( r != -1 ); //Notausstieg
      //addToAllArgs(allArgs, mode, baos );
      return allArgs;
    }

    private void addToAllArgs(AllArgs allArgs, Mode mode, ByteArrayOutputStream baos) {
      String value = baos.toString();
      baos.reset();
      switch( mode ) {
        case command:
          allArgs.setCommand(value);
          break;
        case args:
          allArgs.addArg(value);
          break;
        case additional:
          allArgs.addAdditional(value);
          break;
      }
    }

  }

  
  public void run() {
    try {
      execute();
    } catch( Throwable t ) {
      logger.error(null, t);
      Department.handleThrowable(t);
    } finally {
      try {
        if (!socket.isClosed()) {
          try {
            socket.shutdownOutput();
          } finally {
            socket.close();
          }
        }
      } catch (Exception e) {
        logger.error("Could not close socket", e);
      }
    }
  }
   
  /**
   * 
   */
  private void execute() {
    CommandLineWriter clw = null;
    try {
      clw = new CommandLineWriter(socket.getOutputStream());
    } catch( IOException e ) {
      logger.error("No communication with caller possible", e);
      return; //TODO statt CLW zum versenden von nachrichten zu verwenden, CommandLineWriter umkonfigurieren auf Logging - trotzdem befehl ausführen!
    }
    if( allArgs == null ) {
      fillAllArgsFromSocket(socket);
    }
    if( allArgs == null ) {
      clw.close("reading allArgs failed", ReturnCode.COMMUNICATION_FAILED);
      return;
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Got "+allArgs );
    }
    
    try {
      execute(clw);
      clw.close(ReturnCode.SUCCESS);
    } catch ( CommandLineWriterIOException e ) {
      if (logger.isDebugEnabled()) {
        logger.debug("could not signal error and end communication", e);
      }
      return;
    } catch (XynaException xe) {
      //Alle weiteren Fehler sollten an Caller kommuniziert werden
      String message = getErrorMessageForThrowable(allArgs.getLowerCaseCommand(), xe);
      clw.close(message, ReturnCode.XYNA_EXCEPTION);
    } catch (Throwable t) {
      //Alle weiteren Fehler sollten an Caller kommuniziert werden
      String message = getErrorMessageForThrowable(allArgs.getLowerCaseCommand(), t);
      clw.close(message, ReturnCode.GENERAL_ERROR);
      Department.handleThrowable(t);
    }

  }


  private void execute(CommandLineWriter clw) throws Throwable {

    final ReturnCode rc = XynaFactory.getStatusCodeSLESLike();
    if (rc != ReturnCode.STATUS_UP_AND_RUNNING) {
      boolean allowedCall =
          COMMANDS_ALLOWED_ON_STARTUP_OR_SHUTDOWN.contains(allArgs.getLowerCaseCommand())
          || isGeneratedCommandAllowedOnStartupOrShutdown(allArgs.getLowerCaseCommand());
    
      if (!allowedCall) {
        clw.writeLineToCommandLine("Commands may not be executed if the factory is starting or shutting down");
        clw.writeEndToCommandLine(rc);
        return;
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("got command: " + allArgs.getLowerCaseCommand() );
    }
    if (COMMAND_HELP.equals(allArgs.getLowerCaseCommand()) ) {
      CommandExecution execCommand = executionMap.get( COMMAND_HELP );
      execCommand.execute( allArgs, clw );
      return;
    } else {
      if( tryGeneratedCommands(allArgs.getLowerCaseCommand(), clw) ) {
        return;
      } else {
        //weitere undisclosed commands ausprobieren
        CommandExecution execCommand = executionMap.get( allArgs.getLowerCaseCommand() );
        if( execCommand != null ) {
          execCommand.execute( allArgs, clw );
          return;
        }
      }
    }
    clw.writeString("unknown command \""+allArgs.getLowerCaseCommand()+"\". try \"help\"\n");
    clw.writeEndToCommandLine(ReturnCode.UNKNOWN_COMMAND);     
  }

  /**
   * @param t
   * @return
   */
  private String getErrorMessageForThrowable(String commandName, Throwable t) {
    if( t instanceof XynaException ) {
      XynaException f = (XynaException)t;
      Long rev = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      if (f instanceof XynaExceptionBase) {
        if (f.getClass().getClassLoader() instanceof ClassLoaderBase) {
          rev = ((ClassLoaderBase) f.getClass().getClassLoader()).getRevision();
        }
      }
      XynaFault_ctype xf = ExceptionHandler.toXynaFault(f, rev);
      if (f instanceof XMCP_INVALID_PARAMETERNUMBER) {
        logger.info("Invalid cli parameters: " + xf.getMessage());
        return "Error [" + xf.getCode() + "]: " + xf.getMessage();
      } else {
        logger.error("Command " + commandName + " could not be executed [" + xf.getCode() + "]", t);
        return "Error [" + xf.getCode() + "]: " + xf.getDetails();
      }
    } else {
      logger.error("Command " + commandName + " could not be executed", t);
      return "Error: " + t.getClass().getSimpleName() + " " + t.getMessage();
    }
  }


 

  /**
   * @param lowerCase
   * @return
   * @throws ClassNotFoundException 
   * @throws InvocationTargetException 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  private boolean isGeneratedCommandAllowedOnStartupOrShutdown(String lowerCase) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Class<? extends AXynaCommand> generatedCommand =
        CLIRegistry.getInstance().getAllDistinctCommandClasses().get(lowerCase);

    if (generatedCommand == null) {
      return false;
    }
    try {
      Method methodGetDependencies =
          generatedCommand.getMethod(CommandLineArgumentJavaGenerator.METHODNAME_GETDEPENDENCIES,
                                     new Class[0]);
      String[] dependencies = (String[]) methodGetDependencies.invoke(null, new Object[0]);
      for (String dependency : dependencies) {
        // TODO: besseren Dependency-Check für entsprechende Komponenten einbauen ...
        if (AXynaCommand.DEPENDENCIES_OPTION_NONE.equalsIgnoreCase(dependency)) {
          return true;
        } 
        //TODO else dependency überprüfen
      }
    } catch (NoSuchMethodException e) {
      logger.warn("Could not call <" + generatedCommand.getCanonicalName() + "."
          + CommandLineArgumentJavaGenerator.METHODNAME_GETDEPENDENCIES + ">", e);
    }
    return false;
  }


  /**
   * @param projectedCommandName
   * @param clw
   * @return
   * @throws Throwable
   */
  private boolean tryGeneratedCommands(String projectedCommandName, CommandLineWriter clw) throws Throwable {
    Collection<CommandIdentification> identifications = buildCommandIdentification(allArgs);
    ResolveResult result = CLIRegistry.getInstance().resolveCommand(projectedCommandName, identifications, clw);
    if (result.hasResolved()) {
      Class<? extends AXynaCommand> generatedCommand = result.getCommand();
      
      
      AXynaCommand newCommand = generatedCommand.getConstructor().newInstance();
      newCommand.parse(allArgs);
      if (newCommand.isQuietSet()) {
        newCommand.execute(nullOutputStream);
      } else {
        try {
          newCommand.execute(clw);
          try {
            clw.writeEndToCommandLine(ReturnCode.SUCCESS);
          } catch( CommandLineWriterIOException e ) {
            if( "java.net.SocketException: Socket closed".equals(e.getMessage()) ) {
              //Command newCommand hat den OutputStream eigenhändig geschlossen!
              clw.setEndWritten(true);
            } else {
              throw e; //andere weiterwerfen
            }
          }
        } catch (Throwable e) {
          Department.handleThrowable(e);
          if (XynaFactory.getStatusCodeSLESLike() != ReturnCode.STATUS_UP_AND_RUNNING) {
            logger
                .warn("Exception occurs while initializing or shutting down the server - probably the exception is no 'real' exception.");
          }
          throw e;
        }
      }
      return true;
    }
    if (result.isAmbiguous()) {
      return true;
    } else {
      return false;
    }
  }
  

  private boolean getInitiatlizingOrShuttingDown() {
    return XynaFactory.getStatusCodeSLESLike() == ReturnCode.STATUS_UP_AND_RUNNING;
  }


  public static Collection<CommandIdentification> buildCommandIdentification(AllArgs aa) {
    String cliapplication = null;
    String cliversion = null;
    
    Collection<CommandIdentification> identifications = new ArrayList<CommandIdentification>();
    String[] allArgs = aa.getArgsAsArray(0);
    for (int i = 0; i < allArgs.length; i++) {
      if (allArgs[i].equals("-"+AXynaCommand.CLI_APPLICATION_OPTION_STRING)) {
        cliapplication = allArgs[i+1];
      } else if (allArgs[i].equals("-"+AXynaCommand.CLI_CLASSLOADER_ID_OPTION_STRING)) {
        identifications.add(new CLIRegistry.ClassLoaderIdentityCommandIdentification(allArgs[i+1]));
      } else if (allArgs[i].equals("-"+AXynaCommand.CLI_CLASSLOADER_NAME_OPTION_STRING)) {
        identifications.add(new CLIRegistry.ClassLoaderNameCommandIdentification(allArgs[i+1]));
      } else if (allArgs[i].equals("-"+AXynaCommand.CLI_VERSION_OPTION_STRING)) {
        cliversion = allArgs[i+1];
      } else if (allArgs[i].equals("-"+AXynaCommand.CLI_WORKSPACE_OPTION_STRING)) {
        identifications.add(new CLIRegistry.WorkspaceCommandIdentification(allArgs[i+1]));
      }
    }
    
    if (cliapplication != null || cliversion != null) {
      identifications.add(new CLIRegistry.ApplicationVersionCommandIdentification(cliapplication, cliversion));
    }
    
    return identifications;
  }

  public static String[] parseSharedLibs(String sharedLibsString) {
    String[] parts = sharedLibsString.split(":");
    if (logger.isDebugEnabled()) {
      logger.debug("got shared libs: ");
      for (String s : parts) {
        logger.debug(" - " + s);
      }
    }
    return parts;
  }
  
  @Deprecated // use createuser with -e (encrypted) flag instead
  private static class CreateUserWithHashpass implements CommandExecution {
    public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
      if (allArgs.getArgCount() < 3) {
        throw new XMCP_INVALID_PARAMETERNUMBER("userIdentifier, roleName, passwordHash");
      } 
      if (XynaFactory.getInstance().getFactoryManagementPortal().createUser(allArgs.getArg(0), allArgs.getArg(1), allArgs.getArg(2), true)) {
        clw.writeToCommandLine("User '" + allArgs.getArg(0) + "' was succesfully created\n");
      } else {
        clw.writeToCommandLine("User '" + allArgs.getArg(0) + "' could not be created\n");
      }
    }
  }


  private static class GarbageCollection implements CommandExecution {
    public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
      System.gc();
      if ("rf".equals(allArgs.getFirstArgOrDefault(null))) {
        System.runFinalization();
      }
    }
  }
  
  private static class ExportExceptionCodeGroups implements CommandExecution {
    public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getExceptionManagement().exportToXml();
    }
  }
  
  private static class ImportExceptionCodeGroups implements CommandExecution {
    public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getExceptionManagement().importFromXml();
    }
  }

  private static class XmomDiscovery implements CommandExecution {
    public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
      boolean all = false;
      if( "all".equals(allArgs.getFirstArgOrDefault(null) ) ) {
        all = true;
      }
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase().discovery(all);
    }
  }
  
  private static class AbortFetchers implements CommandExecution {
    public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
            
      ((MessageBusImpl) ((MessageBusManagement) ((XynaFactory)XynaFactory.getInstance()).getXynaMultiChannelPortal().getMessageBusManagement()).getMessageBus()).abortFetchers();
    }
  }
  
  private static class XmomRegenerate implements CommandExecution {
    public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
      UpdateGeneratedClasses.regenerateWorkflowDatabase();
    }
  }


  /**
   * @param instance
   * @return
   */
  public static String[] getGroups(AXynaCommand instance) {
    return instance.getGroups();
  }

  /**
   * @param aXynaCommandClass
   * @return 
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  @SuppressWarnings("unchecked")
  public static Collection<Option> getOptions(Class<? extends AXynaCommand> aXynaCommandClass) throws InstantiationException, IllegalAccessException {
    AXynaCommand instance = aXynaCommandClass.newInstance();
    Options options = instance.getAllOptions();
    return options.getOptions();
  }

}
