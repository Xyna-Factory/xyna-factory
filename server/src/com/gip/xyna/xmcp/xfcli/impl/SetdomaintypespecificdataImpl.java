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

import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xmcp.xfcli.generated.Setdomaintypespecificdata;



public class SetdomaintypespecificdataImpl extends XynaCommandImplementation<Setdomaintypespecificdata> {

  public void execute(OutputStream statusOutputStream, Setdomaintypespecificdata payload) throws XynaException {
    
    Domain domain = factory.getFactoryManagementPortal().getDomain(payload.getDomainName());
    String[] domainTypeSpecificData = payload.getDomainTypeSpecificData();
    if (domainTypeSpecificData == null || domainTypeSpecificData.length <= 0) {
      throw new IllegalArgumentException("No domainTypeSpecificData received!");
    }
    if (domain == null || domain.getDomainTypeAsEnum() == null) {
      throw new IllegalArgumentException("Domain "+ payload.getDomainName() + " could not be found!");
    }
    DomainTypeSpecificData data = domain.getDomainTypeAsEnum().generateDomainTypeSpecificData(parserSpecificsIntoMap(domainTypeSpecificData));

    if (factory.getFactoryManagementPortal().setDomainSpecificDataOfDomain(payload.getDomainName(), data)) {
      writeLineToCommandLine(statusOutputStream,
                             new StringBuilder().append("Domain specific data fo domain ")
                                 .append(payload.getDomainName()).append(" succesfully set").toString());
    } else {
      writeLineToCommandLine(statusOutputStream,
                             new StringBuilder().append("Domain specific data fo domain ")
                                 .append(payload.getDomainName()).append(" could not be set").toString());
    }
  }

  
  private Map<String, List<String>> parserSpecificsIntoMap(String[] specifics) {
    Map<String, List<String>> dataMap = new HashMap<String, List<String>>();
    for (String specific : specifics) {
      int splitPoint = specific.indexOf('=');
      if (splitPoint > 0) {
        String key = specific.substring(0, splitPoint);
        String value = specific.substring(splitPoint + 1);
        if (dataMap.containsKey(key)) {
          dataMap.get(key).add(value);
        } else {
          dataMap.put(key, new ArrayList<String>(Collections.singletonList(value)));
        }
      }
    }
    return dataMap;
  }
  
}
