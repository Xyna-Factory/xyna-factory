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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Addconnectionpool;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.pools.TypedConnectionPoolParameter;



public class AddconnectionpoolImpl extends XynaCommandImplementation<Addconnectionpool> {

  public void execute(OutputStream statusOutputStream, Addconnectionpool payload) throws XynaException {
    TypedConnectionPoolParameter tcpp = new TypedConnectionPoolParameter(payload.getType());
    tcpp.user(payload.getUser())
        .password(payload.getPassword())
        .size(Integer.parseInt(payload.getSize()))
        .connectString(payload.getConnectstring())
        .name(payload.getName());
    if (payload.getRetries() != null) { // TODO nicer parse with error message
      tcpp.maxRetries(Integer.parseInt(payload.getRetries()));
    }
    if (payload.getPooltypespecifics() != null && payload.getPooltypespecifics().length > 0) {
      boolean success = parsePooltypespecifics(statusOutputStream, tcpp, payload.getPooltypespecifics());
      if( ! success ) {
        return;
      }
    }
    
    try {
      ConnectionPoolManagement cpm = XynaFactory.getInstance().getXynaNetworkWarehouse().getConnectionPoolManagement();
      cpm.testConnectionPoolParameter(tcpp);
      cpm.addConnectionPool(tcpp);
      writeLineToCommandLine(statusOutputStream, "ConnectionPool " + tcpp.getName() + " successfully created.");
    } catch (NoConnectionAvailableException e) {
      writeLineToCommandLine(statusOutputStream, "ConnectionPool " + tcpp.getName() + " could not be created.");
      //TODO schöner ausgeben?s
      throw new RuntimeException(e);
    }
  }

  private boolean parsePooltypespecifics(OutputStream statusOutputStream,  TypedConnectionPoolParameter tcpp, String[] pooltypespecifics) {
    List<String> typespecifics = Arrays.asList( pooltypespecifics );
    if( typespecifics.contains("help") ) {
      String help = PluginDescriptionUtils.help(tcpp.getAdditionalDescription(), 
                                                typespecifics,
                                                ParameterUsage.Create,
                                                "add connection pool" );
      writeToCommandLine(statusOutputStream, help);
      writeEndToCommandLine(statusOutputStream, ReturnCode.SUCCESS);
      return false;
    }
    List<StringParameter<?>> sps = tcpp.getAdditionalDescription().getParameters(ParameterUsage.Create);
    try {
      Map<String, Object> parsed = StringParameter.parse(typespecifics).with(sps);
      tcpp.additionalParams(parsed);
      return true;
    } catch (StringParameterParsingException e) {
      
      writeToCommandLine(statusOutputStream, e.getMessage());
      writeEndToCommandLine(statusOutputStream, ReturnCode.XYNA_EXCEPTION);
      return false;
    }
  }

}
