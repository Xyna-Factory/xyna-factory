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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.Collection;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.extendedstatus.ExtendedStatusInformation;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


/**
 *
 */
public class ExtendedStatus implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) {
    if (XynaFactory.getInstance().getFactoryManagement() == null || XynaFactory.getInstance().getFactoryManagement()
        .getXynaExtendedStatusManagement() == null) {
      return;
    }
    Collection<ExtendedStatusInformation> startupInfos = XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement().listExtendedStatusInformation();
    if(!startupInfos.isEmpty()) {
      for(ExtendedStatusInformation info : startupInfos) {
        StringBuilder tmpBuffer = new StringBuilder();
        tmpBuffer.append(info.getStep()).append(": ").append(info.getComponentName());
        if(info.getAdditionalInformation() != null) {
          tmpBuffer.append(" - ").append(info.getAdditionalInformation());
        }
        clw.writeLineToCommandLine(tmpBuffer.toString());
      }
    }

  }

}
