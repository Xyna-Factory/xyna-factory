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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CLIRegistry;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


/**
 *
 */
public class BashCompletion implements CommandExecution {

  static Logger logger = CentralFactoryLogging.getLogger(BashCompletion.class);
 
  public void execute(AllArgs allArgs, CommandLineWriter clw) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String bashCompletion = createBashCompletion();
    clw.writeString( bashCompletion );
    clw.writeEndToCommandLine(ReturnCode.SILENT); //verhindert Ausgabe von "ok"
  }

  private String createBashCompletion() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    
    Map<String, Class<? extends AXynaCommand>> commands = CLIRegistry.getInstance().getAllDistinctCommandClasses();
    /*
    Zu Testzwecken kleinere Ausgabe
    Map<String, Class<? extends AXynaCommand>> commands = new HashMap<String, Class<? extends AXynaCommand>>();
    for( Map.Entry<String, Class<? extends AXynaCommand>> entry : CLIRegistry.getInstance().getAllCommandClasses().entrySet() ) {
      if( entry.getKey().startsWith("addc") ) {
        commands.put( entry.getKey(), entry.getValue() );
      }
    }
    */
    
    StringBuilder sb = new StringBuilder();
    sb.append("# This configuration file is auto-generated.\n");
    sb.append("# WARNING: Do not edit this file, your changes will be lost.\n");
    sb.append("\n");
    sb.append("_xynafactory() {\n");
    sb.append("  local cur prev opts\n");
    sb.append("  COMPREPLY=()\n");
    sb.append("  cur=\"${COMP_WORDS[COMP_CWORD]}\"\n");
    sb.append("  prev=\"${COMP_WORDS[COMP_CWORD-1]}\"\n");
    sb.append("  first=\"${COMP_WORDS[1]}\"\n");
    sb.append("  \n");
    sb.append("  opts=\"help ").append(commandList(sb, commands.keySet() )).append("\"\n");
    sb.append("  \n");
    sb.append("  case \"${first}\" in\n");
    sb.append("    help)\n");
    sb.append("      COMPREPLY=( $(compgen -W \"$opts\" -- ${cur}) )\n");
    sb.append("      return 0\n");
    sb.append("      ;;\n");
    sb.append(     commandParamList(sb,commands));
    sb.append("    *)\n");
    sb.append("    ;;\n");
    sb.append("  esac\n");
    sb.append("  \n");
    sb.append("  COMPREPLY=($(compgen -W \"${opts}\" -- ${cur})) \n"); 
    sb.append("  return 0\n");
    sb.append("}\n");
    sb.append("\n");
    sb.append("complete -F _xynafactory xynafactory.sh\n");
    return sb.toString();
  }

  private String commandParamList(StringBuilder sb, Map<String, Class<? extends AXynaCommand>> commands) throws InstantiationException, IllegalAccessException {
    for( Map.Entry<String, Class<? extends AXynaCommand>> entry : commands.entrySet() ) {
      sb.append("    ").append(entry.getKey()).append(")\n");
      sb.append("      COMPREPLY=( $(compgen -W \"").append(appendOptions(sb, entry.getValue()) ).append("\" -- ${cur}) )\n");
      sb.append("      return 0\n");
      sb.append("      ;;\n");
    }
    return "";//dummy
  }

  private String appendOptions(StringBuilder sb, Class<? extends AXynaCommand> aXynaCommandClass) throws InstantiationException, IllegalAccessException {
    Collection<Option> options = XynaFactoryCLIConnection.getOptions( aXynaCommandClass );
    String sep = "-";
    for( Option option : options ) {
      sb.append(sep).append( option.getOpt() );
      sep = " -";
      if( option.getLongOpt() != null ) {
        sb.append(sep).append("-").append( option.getLongOpt() );
      }
      
    }
    return "";//dummy
  }

  private String commandList(StringBuilder sb, Set<String> set) {
    String sep = "";
    for( String cmd : set ) {
      sb.append( sep ).append(cmd);
      sep = " ";
    }
    return "";//dummy
  }
}
