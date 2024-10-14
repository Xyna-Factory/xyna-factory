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

import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


/**
 *
 */
public class ListFurtherInformationFromStartup implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) {
    if (XynaFactory.getInstance().getFactoryManagement() == null || XynaFactory.getInstance().getFactoryManagement()
        .getXynaExtendedStatusManagement() == null) {
      return;
    }
    Map<String,List<String>> startupInfos = XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement().getFurtherInformationFromStartup();
    if( startupInfos == null || startupInfos.size() == 0 ) {
      return; //keine Meldungen
    }
    
    String toShow = allArgs.getFirstArgOrDefault("ungrouped");
    
    if( "list".equalsIgnoreCase(toShow) ) {
      for( Map.Entry<String,List<String>> entry : startupInfos.entrySet() ) {
        int size = entry.getValue() == null ? 0 : entry.getValue().size();
        clw.writeLineToCommandLine("\""+entry.getKey()+"\" has "+size+ (size == 1 ? " entry" : " entries") );
      }
      return;
    }
    if( "grouped".equalsIgnoreCase(toShow) ) {
      for( Map.Entry<String,List<String>> entry : startupInfos.entrySet() ) {
        if( ! entry.getKey().startsWith("FutureExecution_" ) ) {
          writeEntries( clw, entry.getKey(), entry.getValue(), true );
        }
      }
      return;
    }
    if( "ungrouped".equalsIgnoreCase(toShow) ) { //default, altes Format
      int size = 0;
      for( Map.Entry<String,List<String>> entry : startupInfos.entrySet() ) {
        if( ! entry.getKey().startsWith("FutureExecution_" ) ) {
          size += entry.getValue().size();
        }
      }
      if( size > 0 ) { 
        clw.writeLineToCommandLine("Found " + size + " warning" + (startupInfos.size() == 1 ? "" : "s") +  " at startup ...");
        for( Map.Entry<String,List<String>> entry : startupInfos.entrySet() ) {
          if( ! entry.getKey().startsWith("FutureExecution_" ) ) {
            writeEntries( clw, entry.getKey(), entry.getValue(), false );
          }
        }
      }
      return;
    }
    List<String> entries = startupInfos.get(toShow);
    writeEntries( clw, toShow, entries, false );
    
  }

  private void writeEntries(CommandLineWriter clw, String key, List<String> values, boolean header) {
    if (header) {
      int size = values == null ? 0 : values.size();
      clw.writeLineToCommandLine("\"" + key + "\" has " + size + (size == 1 ? " entry:" : " entries:"));
      if (values != null) {
        synchronized (values) {
          for (String info : values) {
            clw.writeLineToCommandLine("\t" + info);
          }
        }
      }
    } else {
      if (values != null) {
        synchronized (values) {
          for (String info : values) {
            clw.writeLineToCommandLine(info);
          }
        }
      }
    }
  }

}
