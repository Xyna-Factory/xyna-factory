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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Exportdatamodels;



public class ExportdatamodelsImpl extends XynaCommandImplementation<Exportdatamodels> {

  public void execute(OutputStream statusOutputStream, Exportdatamodels payload) throws XynaException {
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    DataModelResult result = new DataModelResult();
    
    List<String> fqNames = payload.getDatamodelfqname() != null ? Arrays.asList(payload.getDatamodelfqname()) : null;
    dmm.exportDataModels(result, payload.getFilename(), fqNames);
    
    if( result.hasSingleMessages() ) {
      writeLineToCommandLine(statusOutputStream, result.singleMessagesToString("\n"));
    }
    
    ReturnCode returnCode = result.isSucceeded() ? ReturnCode.SUCCESS : ReturnCode.GENERAL_ERROR;
    
    writeEndToCommandLine(statusOutputStream, returnCode);
  }

}
