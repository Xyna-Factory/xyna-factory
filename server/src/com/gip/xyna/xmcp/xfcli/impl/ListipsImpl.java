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
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.generated.Listips;



public class ListipsImpl extends XynaCommandImplementation<Listips> {

  public void execute(OutputStream statusOutputStream, Listips payload) throws XynaException {
    InternetAddressBean[] internetAddresses =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
            .listInternetAddresses();
    writeLineToCommandLine(statusOutputStream, internetAddresses.length + " IP" + (internetAddresses.length != 1 ? "s are" : " is") + " defined"
        + (internetAddresses.length > 0 ? ":" : "."));
    for (InternetAddressBean iab : internetAddresses) {
      writeLineToCommandLine(statusOutputStream, "  - " + iab.getId() + " = " + iab.getInetAddress().getHostAddress());
      if (payload.getVerbose() && iab.getDocumentation() != null) {
        writeLineToCommandLine(statusOutputStream, "      " + iab.getDocumentation());
      }
    }
  }

}
