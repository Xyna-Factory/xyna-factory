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
import java.io.PrintWriter;
import java.io.StringWriter;
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
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult.MessageGroup;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult.Result;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Modifydatamodel;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ModifydatamodelImpl extends XynaCommandImplementation<Modifydatamodel> {
  
  public void execute(OutputStream statusOutputStream, Modifydatamodel payload) throws XynaException {
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();

    List<String> parameters = toList(payload.getParameters());
    ReturnCode returnCode;
    if (parameters.contains("help")) {
      String help = PluginDescriptionUtils.help(dmm.getDataModelTypeDescription(payload.getDatamodeltype()), parameters,
                                                ParameterUsage.Modify, "modify");
      writeToCommandLine(statusOutputStream, help);
      returnCode = ReturnCode.SUCCESS;
    } else {
      returnCode = modifyDataModel(dmm, statusOutputStream, parameters, payload);
    }

    writeEndToCommandLine(statusOutputStream, returnCode);
  }

  private ReturnCode modifyDataModel(DataModelManagement dmm, OutputStream statusOutputStream, List<String> parameters, Modifydatamodel payload) throws XFMG_NoSuchDataModelTypeException, PersistenceLayerException, XFMG_NoSuchDataModelException {
    StringBuilder sb = new StringBuilder();
    DataModelResult result = new DataModelResult();
    
    boolean finallyWriteResult = true;
    try {
      ModifyDataModelParameters mdmp = new ModifyDataModelParameters(payload.getDatamodeltype(),  payload.getDatamodelname(), payload.getDatamodelversion());
      mdmp.setParameters(StringParameter.listToMap(parameters) );
      dmm.modifyDataModel(result, mdmp);
    } catch (XFMG_NoSuchDataModelTypeException e) {
      finallyWriteResult = false;
      throw e;
    } catch (XFMG_NoSuchDataModelException e) {
      finallyWriteResult = false;
      throw e;
    } finally {
      if( finallyWriteResult ) {
        if( result.hasSingleMessages() ) {
          writeLineToCommandLine(statusOutputStream, result.singleMessagesToString("\n"));
        }
        if( result.hasMessageGroups() ) {
          for( MessageGroup mg :  result.getMessageGroups() ) {
            writeLineToCommandLine(statusOutputStream, mg.toSingleString(":\n", "\n"));
          }
        }
      }
    }

    ReturnCode returnCode = ReturnCode.SUCCESS;
    if (result.getResult() != Result.Succeeded) {
      returnCode = ReturnCode.GENERAL_ERROR;
      sb = new StringBuilder();
      if( result.getExceptions() != null && ! result.getExceptions().isEmpty() ) {
         for( Exception e : result.getExceptions() ) {
           StringWriter sw = new StringWriter();
           PrintWriter pw = new PrintWriter(sw);
           e.printStackTrace(pw);
           sb.append("\n  ").append(sw.toString()).append("\n");
         }
      }
      writeLineToCommandLine(statusOutputStream, sb.toString());
    }
    return returnCode;
  }

  private List<String> toList(String[] strings) {
    if (strings == null || strings.length == 0) {
      return Collections.emptyList();
    }
    return Arrays.asList(strings);
  }

}

