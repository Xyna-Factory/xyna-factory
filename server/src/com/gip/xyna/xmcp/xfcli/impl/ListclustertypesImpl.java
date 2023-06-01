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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface.ClusterParameterInformation;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listclustertypes;



public class ListclustertypesImpl extends XynaCommandImplementation<Listclustertypes> {

  public void execute(OutputStream statusOutputStream, Listclustertypes payload) throws XynaException {
    ClusterParameterInformation[] information =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
            .getInformationForSupportedClusterTypes();
    StringBuilder sb = new StringBuilder();
    sb.append("The following types of cluster instances are available:\n");
    for (ClusterParameterInformation informationPerClusterInstanceType : information) {
      sb.append("\to Name: ").append(informationPerClusterInstanceType.name).append("\n");
      sb.append("\t\to Initialization parameters:\n");
      for (String s : informationPerClusterInstanceType.initializationParameterInformation.split("\\n")) {
        sb.append("\t\t\t\t").append(s).append("\n");
      }
      sb.append("\t\to Connection parameters:\n");
      for (String s : informationPerClusterInstanceType.connectionParameterInformation.split("\\n")) {
        sb.append("\t\t\t\t").append(s).append("\n");
      }
    }
    writeLineToCommandLine(statusOutputStream, sb.toString());
  }

}
