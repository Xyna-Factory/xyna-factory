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



import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.generated.OverallInformationProvider;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;




public class CLIRegistry {

  private static final Logger logger = CentralFactoryLogging.getLogger(CLIRegistry.class);
  
  private Map<String, Class<? extends AXynaCommand>> factoryCommands =
      new ConcurrentHashMap<String, Class<? extends AXynaCommand>>();
  private ConcurrentMap<String, Map<ClassLoader, Class<? extends AXynaCommand>>> customCommands =
      new ConcurrentHashMap<String, Map<ClassLoader,Class<? extends AXynaCommand>>>();
  private boolean loadedFactoryClasses = false;
  private int changeCounter =0;

  public List<Class<? extends AXynaCommand>> getAllCommandClasses() throws ClassNotFoundException {
    lazyInitFactoryCommands();
    List<Class<? extends AXynaCommand>> commands = new ArrayList<Class<? extends AXynaCommand>>();
    commands.addAll(factoryCommands.values());
    for (Map<ClassLoader, Class<? extends AXynaCommand>> customCommand : customCommands.values()) {
      commands.addAll(customCommand.values());
    }
    return commands;
  }
  
  
  public Map<String, Class<? extends AXynaCommand>> getAllDistinctCommandClasses() throws ClassNotFoundException {
    lazyInitFactoryCommands();
    ConcurrentMap<String, Class<? extends AXynaCommand>> commands = new ConcurrentHashMap<String, Class<? extends AXynaCommand>>();
    commands.putAll(factoryCommands);
    for (Entry<String, Map<ClassLoader, Class<? extends AXynaCommand>>> customCommand : customCommands.entrySet()) {
      for (Class<? extends AXynaCommand> aCommand : customCommand.getValue().values()) {
        commands.putIfAbsent(customCommand.getKey(), aCommand);
      }
    }
    return commands;
  }
  
  
  private void lazyInitFactoryCommands() throws ClassNotFoundException {
    if (!loadedFactoryClasses) {
      List<Class<? extends AXynaCommand>> commands = OverallInformationProvider.getCommands();
      for (Class<? extends AXynaCommand> command : commands) {
        String commandName;
        try {
          commandName = command.getConstructor().newInstance().getCommandName().toLowerCase();
          factoryCommands.put(commandName, command);
        } catch (Exception e) {
          throw new RuntimeException(e);
        } 
      }
      loadedFactoryClasses = true;
    }
  }


  public void registerCLICommand(Class<? extends AXynaCommand> commandClass) {
    ++changeCounter;
    String commandName = commandClass.getSimpleName().toLowerCase();
    Map<ClassLoader, Class<? extends AXynaCommand>> commands = customCommands.get(commandName);
    if (commands == null) {
      commands = new WeakHashMap<ClassLoader, Class<? extends AXynaCommand>>();
      customCommands.putIfAbsent(commandName, commands);
      commands = customCommands.get(commandName);
    }
    commands.put(commandClass.getClassLoader(), commandClass);
    if( logger.isDebugEnabled() ) {
      logger.debug( "Registered CLI-command "+commandClass.getSimpleName() );
    }
  }
  
  
  public void unregisterCLICommand(Class<? extends AXynaCommand> commandClass) {
    ++changeCounter;
    String commandName = commandClass.getSimpleName().toLowerCase();
    Map<ClassLoader, Class<? extends AXynaCommand>> commands = customCommands.get(commandName);
    if (commands != null) {
      @SuppressWarnings("unused")
      Class<? extends AXynaCommand> removedCommand = commands.remove(commandClass.getClassLoader());
      // if we failed to remove we might try a more fuzzy identification via CommandIdentification
    }
    if( logger.isDebugEnabled() ) {
      logger.debug( "Unregistered CLI-command "+commandClass.getSimpleName() );
    }
  }
  
  
  public ResolveResult resolveCommand(String commandName, Collection<CommandIdentification> identifications, CommandLineWriter clw) throws ClassNotFoundException {
    lazyInitFactoryCommands();
    Class<? extends AXynaCommand> factoryCommand = factoryCommands.get(commandName);
    lazyPruneCustomCommands();
    Map<ClassLoader, Class<? extends AXynaCommand>> customCommand = customCommands.get(commandName);
    if (customCommand == null || customCommand.size() <= 0) {
      if (factoryCommand == null) {
        return EMPTY_RESULT;
      } else {
        return new ResolveResult(factoryCommand);
      }
    }
    if (identifications.size() > 0) {
      List<Entry<ClassLoader, Class<? extends AXynaCommand>>> commands = new ArrayList<Entry<ClassLoader, Class<? extends AXynaCommand>>>();
      commandLoop: for (Entry<ClassLoader, Class<? extends AXynaCommand>> entry : customCommand.entrySet()) {
        for (CommandIdentification identification : identifications) {
          if (!identification.identifies(entry.getKey())) {
            continue commandLoop;
          }
        }
        commands.add(entry);
      }
      if (commands.size() > 1) {
        Collection<ClassLoader> ambiguities = new ArrayList<ClassLoader>();
        for (Entry<ClassLoader, Class<? extends AXynaCommand>> entry : commands) {
          ambiguities.add(entry.getKey());
        }
        writeAmbigiuousHeader(commandName, clw);
        writeAmbiguityToCLI(commandName, ambiguities, clw);
        return AMBIGUOUS_RESULT;
      } else if (commands.size() <= 0) {
        clw.writeLineToCommandLine("No command matches the given identification!");
        return AMBIGUOUS_RESULT;
      } else {
        return new ResolveResult(commands.get(0).getValue());
      }
    } else {
      if (customCommand.size() > 1) {
        if (factoryCommand == null) {
          writeAmbigiuousHeader(commandName, clw);
          writeAmbiguityToCLI(commandName, customCommand.keySet(), clw);
          return AMBIGUOUS_RESULT;
        } else {
          writeFactoryOverwriteHeader(commandName, clw);
          writeAmbiguityToCLI(commandName, customCommand.keySet(), clw);
          return new ResolveResult(factoryCommand);
        }
      } else {
        return new ResolveResult(customCommand.values().iterator().next());
      }
    }
  }
  
  
  private void lazyPruneCustomCommands() {
    for (String commandName : customCommands.keySet()) {
      Map<ClassLoader, Class<? extends AXynaCommand>> customCommandMap = customCommands.get(commandName);
      Iterator<ClassLoader> keyIterator = customCommandMap.keySet().iterator();
      while (keyIterator.hasNext()) {
        ClassLoader cl = keyIterator.next();
        if (cl instanceof ClassLoaderBase && ((ClassLoaderBase) cl).isClosed()) {
          keyIterator.remove();
        }
      }
    }
  }

  
  public static void writeAmbigiuousHeader(String commandName, CommandLineWriter clw) {
    clw.writeLineToCommandLine("Command " + commandName + " has several registered versions.");
    clw.writeLineToCommandLine("Please qualify your invocation with a set of the following parameters that defines the call site uniquely:");
  }
  
  
  public static void writeFactoryOverwriteHeader(String commandName, CommandLineWriter clw) {
    clw.writeLineToCommandLine("Command " + commandName + " is ambigiuously defined, using factory version.");
    clw.writeLineToCommandLine("Please qualify your invocation with a set of the following parameters that defines the call site uniquely:");
  }
  

  private static void writeAmbiguityToCLI(String commandName, Collection<ClassLoader> ambiguties, CommandLineWriter clw) {
    String formatLine = "%-12s  %-60s  %-20s  %-15s  %-20s";
    clw.writeLineToCommandLine(String.format(formatLine, "-"+AXynaCommand.CLI_CLASSLOADER_ID_OPTION_STRING,
                                                         "-"+AXynaCommand.CLI_CLASSLOADER_NAME_OPTION_STRING,
                                                         "-"+AXynaCommand.CLI_APPLICATION_OPTION_STRING,
                                                         "-"+AXynaCommand.CLI_VERSION_OPTION_STRING,
                                                         "-"+AXynaCommand.CLI_WORKSPACE_OPTION_STRING));
    for (ClassLoader cl : ambiguties) {
      String clName = "-";
      String clApp = "-";
      String clVer = "-";
      String clWork = "-";
      if (cl instanceof ClassLoaderBase) {
        ClassLoaderBase clb = (ClassLoaderBase) cl;
        clName = clb.getClassLoaderID();
        Long revision = clb.getRevision();
        if (revision != null) {
          try {
            Application app = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getApplication(revision);
            if (app != null) {
              clApp = app.getName();
              clVer = app.getVersionName();
            }
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            // ntbd
          }
          
          try {
            Workspace ws = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getWorkspace(revision);
            if (ws != null) {
              clWork = ws.getName();
            }
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            // ntbd
          }
        }
      }
      clw.writeLineToCommandLine(String.format(formatLine, System.identityHashCode(cl), clName, clApp, clVer, clWork));
    }
  }


  private static CLIRegistry instance = new CLIRegistry();


  public static CLIRegistry getInstance() {
    return instance;
  }
  
  
  public static interface CommandIdentification {
    
    public boolean identifies(ClassLoader cl);
    
  }
  
  
  public static class ClassLoaderIdentityCommandIdentification implements CommandIdentification {
    
    private final String identitiy;
    
    public ClassLoaderIdentityCommandIdentification(String identitiy) {
      this.identitiy = identitiy;
    }

    public boolean identifies(ClassLoader cl) {
      String identitiy = String.valueOf(System.identityHashCode(cl));
      return this.identitiy.equals(identitiy);
    }
    
  }
  
  
  public static abstract class RevisionBasedCommandIdentification implements CommandIdentification {
    
    public boolean identifies(ClassLoader cl) {
      if (cl instanceof ClassLoaderBase) {
        ClassLoaderBase clb = (ClassLoaderBase)cl;
        Long revision = clb.getRevision();
        if (revision != null) {
          return identifies(revision);
        } else {
          return false;
        }
      } else {
        return false;
      }
    }
    
    public abstract boolean identifies(Long revision);
    
  }
  
  
  public static class ClassLoaderNameCommandIdentification implements CommandIdentification {
    
    private final String classLoaderId;
    
    public ClassLoaderNameCommandIdentification(String classLoaderId) {
      this.classLoaderId = classLoaderId;
    }
    
    public boolean identifies(ClassLoader cl) {
      if (cl instanceof ClassLoaderBase) {
        ClassLoaderBase clb = (ClassLoaderBase)cl;
        return clb.getClassLoaderID().equals(classLoaderId);
      } else {
        return false;
      }
    }
    
  }
  
  public static class ApplicationVersionCommandIdentification extends  RevisionBasedCommandIdentification {

    private final String application;
    private final String version;
    
    public ApplicationVersionCommandIdentification(String application, String version) {
      this.application = application;
      this.version = version;
    }


    @Override
    public boolean identifies(Long revision) {
      try {
        Application app = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getApplication(revision);
        if (application == null) {
          return app.getVersionName().equals(version);
        } else if (version == null) {
          return app.getName().equals(application);
        } else {
          return (app.getName().equals(application) &&
                  app.getVersionName().equals(version));
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return false;
      }
    }
    
  }
  
  
  public static class WorkspaceCommandIdentification extends  RevisionBasedCommandIdentification {

    private final String workspace;
    
    public WorkspaceCommandIdentification(String workspace) {
      this.workspace = workspace;
    }


    @Override
    public boolean identifies(Long revision) {
      try {
        Workspace space = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getWorkspace(revision);
        return space.getName().equals(workspace);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return false;
      }
    }
    
  }
  
  
  
  private final static ResolveResult EMPTY_RESULT = new ResolveResult(false);
  private final static ResolveResult AMBIGUOUS_RESULT = new ResolveResult(true);
  
  public static class ResolveResult {
    private final boolean ambiguous;
    private final Class<? extends AXynaCommand> command;
    
    private ResolveResult(Class<? extends AXynaCommand> command) {
      this.ambiguous = false;
      this.command = command;
    }
    
    private ResolveResult(boolean ambiguous) {
      this.ambiguous = ambiguous;
      this.command = null;
    }
    
    public boolean hasResolved() {
      return command != null;
    }
    
    public boolean isAmbiguous() {
      return ambiguous;
    }
    
    public Class<? extends AXynaCommand> getCommand() {
      return command;
    }
  }
  
  public int getChangeCounter() {
    return changeCounter;
  }
  


}
