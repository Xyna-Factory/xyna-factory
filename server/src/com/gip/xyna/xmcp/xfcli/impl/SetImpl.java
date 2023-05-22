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



import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Set;



public class SetImpl extends XynaCommandImplementation<Set> {

  public void execute(OutputStream statusOutputStream, Set payload) throws XynaException {
    if (XynaFactory.getInstance().getFactoryManagement() == null
        || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS() == null
        || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration() == null) {
      throw new RuntimeException("Command may not be executed right now.");
    }
    
    Configuration cfg = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
    
    if( payload.getNotSet() ) {
      //nur setzen, wenn Value noch nicht gesetzt ist
      XynaPropertyWithDefaultValue existing = cfg.getPropertyWithDefaultValue(payload.getKey());
      boolean hasNoValue = existing == null || existing.getValue() == null;
      if( ! hasNoValue ) {
        writeToCommandLine(statusOutputStream, "XynaProperty "+payload.getKey()+" already has a value: "+ existing.getValue() );
        writeEndToCommandLine(statusOutputStream, ReturnCode.SUCCESS_BUT_NO_CHANGE );
        return;
      }
    }
    //Value setzen
    cfg.setProperty(payload.getKey(), payload.getValue(), payload.getGlobal());
  }

}
