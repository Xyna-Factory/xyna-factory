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
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelTypeException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.RemoveDataModelParameters;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Removedatamodel;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class RemovedatamodelImpl extends XynaCommandImplementation<Removedatamodel> {

  public void execute(OutputStream statusOutputStream, Removedatamodel payload) throws XynaException {
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    
    List<String> parameters = toList(payload.getParameters());
    ReturnCode returnCode;
    if (parameters.contains("help")) {
      String help = PluginDescriptionUtils.help(dmm.getDataModelTypeDescription(payload.getDatamodeltype()), parameters,
                                                ParameterUsage.Delete, "remove");
      writeToCommandLine(statusOutputStream, help);
      returnCode = ReturnCode.SUCCESS;
    } else {
      returnCode = removeDataModel(dmm, statusOutputStream, parameters, payload);
    }
    writeEndToCommandLine(statusOutputStream, returnCode);
  }
  
  private ReturnCode removeDataModel(DataModelManagement dmm, OutputStream statusOutputStream, List<String> parameters, Removedatamodel payload) throws XFMG_NoSuchDataModelTypeException, PersistenceLayerException, XFMG_NoSuchDataModelException {
    DataModelResult result = new DataModelResult();
    RemoveDataModelParameters rdmp = new RemoveDataModelParameters(payload.getDatamodeltype(),  payload.getDatamodelname(), payload.getDatamodelversion());
    rdmp.setParameters(StringParameter.listToMap(parameters) );
    dmm.removeDataModel( result, rdmp );

    if( result.hasSingleMessages() ) {
      writeLineToCommandLine(statusOutputStream, result.singleMessagesToString("\n"));
    }
    
    ReturnCode returnCode = result.isSucceeded() ? ReturnCode.SUCCESS : ReturnCode.GENERAL_ERROR;
    return returnCode;
  }
  
  private List<String> toList(String[] strings) {
    if (strings == null || strings.length == 0) {
      return Collections.emptyList();
    }
    return Arrays.asList(strings);
  }

}
