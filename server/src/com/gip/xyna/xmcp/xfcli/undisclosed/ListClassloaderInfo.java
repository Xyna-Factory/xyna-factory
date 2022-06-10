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

import java.util.HashMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


/**
 *
 */
public class ListClassloaderInfo implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    
    if (allArgs.getArgCount() == 0 ) {
      clw.writeToCommandLine(cld.getClassLoaderTrace().toString());
    } else {
      HashMap<String,ClassLoaderType> clts = new HashMap<String,ClassLoaderType>();
      for( ClassLoaderType clt : ClassLoaderType.values() ) {
        clts.put( clt.name().toLowerCase(), clt );
      }
      for( String arg : allArgs.getArgs() ) {
        ClassLoaderType clt = clts.get(arg.toLowerCase());
        if( clt == null ) {
          clw.writeToCommandLine("Unknown ClassLoaderType \""+arg+"\"\n");
          continue;
        }
        clw.writeToCommandLine(cld.getClassLoaderTrace(clt).toString());
      }
    }
  }

}
