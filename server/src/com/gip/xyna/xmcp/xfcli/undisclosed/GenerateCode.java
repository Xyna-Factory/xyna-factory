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

import javax.tools.JavaFileObject;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;


public class GenerateCode implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
    
    String fqName = allArgs.getArg(0);
    Long revision = -1L;
    if (allArgs.getArgCount() > 1) {
      revision = Long.parseLong(allArgs.getArg(1));
    }
    
    GenerationBase gb;
    XMOMType type = XMOMType.getXMOMTypeByRootTag(GenerationBase.retrieveRootTag(fqName, revision, true, false));
    switch (type) {
      case DATATYPE :
        gb = DOM.getOrCreateInstance(fqName, new GenerationBaseCache(), revision);
        break;
      case WORKFLOW:
        gb = WF.getOrCreateInstance(fqName, new GenerationBaseCache(), revision);
        break;
      case EXCEPTION:
        gb = ExceptionGeneration.getOrCreateInstance(fqName, new GenerationBaseCache(), revision);
        break;
      default :
        throw new UnsupportedOperationException(type.toString());
    }
    JavaFileObject jfo = gb.generateCode(false);

    if (jfo == null) {
      clw.writeLineToCommandLine("Did not generate any code.");
    } else {
      clw.writeToCommandLine(jfo.getCharContent(true));
    }
  }

  
}
