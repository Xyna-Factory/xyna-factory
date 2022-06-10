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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CLIRegistry;
import com.gip.xyna.xmcp.xfcli.CLIRegistry.CommandIdentification;
import com.gip.xyna.xmcp.xfcli.CLIRegistry.ResolveResult;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


/**
 *
 */
public class Help implements CommandExecution {
  
  static Logger logger = CentralFactoryLogging.getLogger(Help.class);
  byte[] cachedHelpMessage;
  int cachedHelpMessageChangeCounter = -1;
  
  
  public void execute(AllArgs allArgs, CommandLineWriter clw ) throws Exception {
    if (allArgs.getArgCount() == 0) {
      clw.write( getCachedHelpMessage() );
    } else if("-s".equals(allArgs.getArg(0))) {
      clw.writeToCommandLine(createHelpMessageForUndisclosedCommands());
    }
    else {
      String command = allArgs.getArg(0);
      if( "help".equals(command) ) {
        clw.writeLineToCommandLine("usage: help <command>");
        clw.writeLineToCommandLine("Prints a help message for a single command.");
        return;
      }
      Collection<CommandIdentification> identifications = XynaFactoryCLIConnection.buildCommandIdentification(allArgs);
      ResolveResult result =  CLIRegistry.getInstance().resolveCommand(command, identifications, clw);
      if (result.hasResolved()) {
        Class<? extends AXynaCommand> klazz = result.getCommand();
        AXynaCommand commandInstance = klazz.getConstructor().newInstance();
        clw.writeToCommandLine(commandInstance.getExtendedDescription());
      } else if (!result.isAmbiguous()) {
        clw.writeLineToCommandLine("Unknown command: " + allArgs.getArg(0));
        clw.writeEndToCommandLine(ReturnCode.UNKNOWN_COMMAND);
      }
    }
  }


/**
   * @return
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws ClassNotFoundException 
   */
  private byte[] getCachedHelpMessage() throws Exception {
    int cc = CLIRegistry.getInstance().getChangeCounter();
    if( cc != cachedHelpMessageChangeCounter || cachedHelpMessage == null ) {
      synchronized ( Help.class ) {
        cc = CLIRegistry.getInstance().getChangeCounter();
        if( cc != cachedHelpMessageChangeCounter || cachedHelpMessage == null ) {
          String help = createHelpMessage();
          cachedHelpMessageChangeCounter = cc;
          try {
            cachedHelpMessage = help.getBytes(Constants.DEFAULT_ENCODING);
          } catch( UnsupportedEncodingException e ) {
            logger.warn( "UnsupportedEncodingException", e);
            cachedHelpMessage = help.getBytes();
          }
        }
      }
    }
    return cachedHelpMessage;
  }



  /**
   * @return
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws ClassNotFoundException 
   */
  private String createHelpMessage() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append(Constants.FACTORY_NAME + " help. For help on a single command, use 'help <command>'.\n");
    sb.append("Use 'help -s' for a list of undisclosed commands\n\n");
    sb.append("All commands can be preceded by '-p <port>' to address a specified CLI port, e.g. './"
            + Constants.SERVER_SHELLNAME
            + " -p 12345 start'.\n\n"
            + "[-d [port]] "
            + XynaFactoryCLIConnection.COMMAND_START
            + ": Starts the Xyna Factory Server. The factory can be started in debug mode using the -d flag. If no debug port is specified, the default port 4000 is used.\n"
            + XynaFactoryCLIConnection.COMMAND_STOP + ": Stops the Xyna Factory Server.\n" + "[-d [port]]" 
            + XynaFactoryCLIConnection.COMMAND_RESTART
            + ": Restarts the Xyna Factory Server.\n\n");

    Map<String, Set<AXynaCommand>> groupedCommands = new TreeMap<String, Set<AXynaCommand>>();
    for (Class<? extends AXynaCommand> clazz : CLIRegistry.getInstance().getAllDistinctCommandClasses().values()) {
      AXynaCommand instance = clazz.getConstructor().newInstance();
      String group;
      if (!instance.isDeprecated()) {
        String[] groups = XynaFactoryCLIConnection.getGroups(instance);
        group = groups != null && groups.length > 0 ? groups[0] : "";
      } else {
        group = "Deprecated";
      }
      Set<AXynaCommand> relevantCommands = groupedCommands.get(group);
      if (relevantCommands == null) {
        relevantCommands = new TreeSet<AXynaCommand>();
        groupedCommands.put(group, relevantCommands);
      }
      relevantCommands.add(instance);
    }
    for (Entry<String, Set<AXynaCommand>> groupOfCommands : groupedCommands.entrySet()) {
      String groupName = groupOfCommands.getKey();
      if (!"".equals(groupName)) {
        sb.append("---- ").append(groupName).append(" ----").append("\n");
      }
      for (AXynaCommand command : groupOfCommands.getValue()) {
        sb.append("\t### ").append(command.getDescription()).append("\n");
      }
      sb.append("\n");
    }
    return sb.toString();   
  }
  
  
  private String createHelpMessageForUndisclosedCommands() {
    StringBuilder sb = new StringBuilder();

    sb.append(Constants.FACTORY_NAME + " help for undisclosed commands. \n\n");
    sb.append("---- Undisclosed commands\n");
    Set<String> commandSet = XynaFactoryCLIConnection.getUndisclodedCommandExecutionMap().keySet();
    String[] commandList = commandSet.toArray(new String[commandSet.size()]);
    Arrays.sort(commandList, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        return s1.compareTo(s2);
      }
    });

    for (String cmdName : commandList) {
      sb.append("\t### ").append(cmdName).append("\n");
    }
    sb.append("\n");
    return sb.toString();
  }
}
