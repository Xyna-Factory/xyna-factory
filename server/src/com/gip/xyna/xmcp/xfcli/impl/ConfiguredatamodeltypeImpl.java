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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Configuredatamodeltype;



public class ConfiguredatamodeltypeImpl extends XynaCommandImplementation<Configuredatamodeltype> {

  public void execute(OutputStream statusOutputStream, Configuredatamodeltype payload) throws XynaException {
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();

    List<String> parameters = toList(payload.getParameter());
    ReturnCode returnCode;
    if (parameters.contains("help")) {
      String help = PluginDescriptionUtils.help(dmm.getDataModelTypeDescription(payload.getDatamodeltype()), parameters,
                                                ParameterUsage.Configure, "configuration");
      writeToCommandLine(statusOutputStream, help);
    } else {
      try {
        dmm.configureDataModelType(payload.getDatamodeltype(), parameters);
      } catch (StringParameterParsingException e) {
        throw new RuntimeException(e);
      }
    }
    returnCode = ReturnCode.SUCCESS;
    writeEndToCommandLine(statusOutputStream, returnCode);
  }


  private List<String> toList(String[] strings) {
    if (strings == null || strings.length == 0) {
      return Collections.emptyList();
    }
    return Arrays.asList(strings);
  }

}
