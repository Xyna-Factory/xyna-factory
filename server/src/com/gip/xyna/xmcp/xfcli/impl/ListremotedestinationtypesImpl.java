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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listremotedestinationtypes;



public class ListremotedestinationtypesImpl extends XynaCommandImplementation<Listremotedestinationtypes> {

  public void execute(OutputStream statusOutputStream, Listremotedestinationtypes payload) throws XynaException {
    RemoteDestinationManagement rdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
   
    List<PluginDescription> list = new ArrayList<PluginDescription>();
    for( RemoteDestinationType rdt : rdMgmt.listRemoteDestinationTypes() ) {
      list.add( rdt.getInitialisationParameterDescription() );
    }
    Collections.sort(list);
    
    if( list.size() == 0 ) {
      writeLineToCommandLine(statusOutputStream, "No remote destination type is registered.");
      return;
    } else if( list.size() == 1 ) {
      writeLineToCommandLine(statusOutputStream, "Registered remote destination type:");
    } else {
      writeLineToCommandLine(statusOutputStream,  list.size()+" registered remote destination types:");
    }
    
    DocumentationLanguage lang = getLang(payload.getLang());

    StringBuilder output = new StringBuilder();
    for( PluginDescription pd : list ) {
      output.append(pd.getName()).append("\n  ");
      PluginDescriptionUtils.append(output, pd, lang, ParameterUsage.Create, "import" );
      output.append("\n");
    }
    
    writeToCommandLine( statusOutputStream, output.toString() );
  }

  private DocumentationLanguage getLang(String lang) {
    if( "EN".equalsIgnoreCase(lang) ) {
      return DocumentationLanguage.EN;
    } else if( "DE".equalsIgnoreCase(lang) ) {
      return DocumentationLanguage.DE;
    } else {
      return DocumentationLanguage.EN;
    }
  }
  
}
