/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import java.util.List;

import com.gip.xyna.xmcp.xfcli.generated.Listorderinputsourcetypes;



public class ListorderinputsourcetypesImpl extends XynaCommandImplementation<Listorderinputsourcetypes> {

  public void execute(OutputStream statusOutputStream, Listorderinputsourcetypes payload) throws XynaException {
    OrderInputSourceManagement oigm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    List<PluginDescription> list = oigm.listOrderInputSourceTypes();
    
    if( list.size() == 0 ) {
      writeLineToCommandLine(statusOutputStream, "No order input generator type is registered.");
      return;
    } else if( list.size() == 1 ) {
      writeLineToCommandLine(statusOutputStream, "Registered order input generator type:");
    } else {
      writeLineToCommandLine(statusOutputStream,  list.size()+" registered order input generator types:");
    }
    
    DocumentationLanguage lang;
    if (payload.getLang() == null || payload.getLang().length() == 0) {
      lang = DocumentationLanguage.EN;
    } else {
      lang = DocumentationLanguage.valueOf(payload.getLang());
    }

    StringBuilder output = new StringBuilder();
    for( PluginDescription pd : list ) {
      output.append(pd.getName()).append("\n  ");
      PluginDescriptionUtils.append(output, pd, lang, ParameterUsage.Create, "new order input generator" );
      output.append("\n");
    }
    
    writeToCommandLine(statusOutputStream, output.toString());
  }

}
