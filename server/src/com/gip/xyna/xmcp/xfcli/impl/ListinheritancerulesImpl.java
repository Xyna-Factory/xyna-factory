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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listinheritancerules;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;



public class ListinheritancerulesImpl extends XynaCommandImplementation<Listinheritancerules> {

  private static final Comparator<? super DestinationKey> COMPARATOR = new Comparator<DestinationKey>() {

    @Override
    public int compare(DestinationKey o1, DestinationKey o2) {
      return o1.getOrderType().compareTo(o2.getOrderType());
    }
    
  };


  public void execute(OutputStream statusOutputStream, Listinheritancerules payload) throws XynaException {
    ParameterInheritanceManagement parameterInheritanceMgmt =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();

    ParameterType parameterType = null;
    try {
      parameterType = ParameterType.valueOf(payload.getParameterType());
    } catch (IllegalArgumentException e) {
      writeLineToCommandLine(statusOutputStream, "Invalid parameter type '" + payload.getParameterType() + "'. Valid are: " + Arrays.toString(ParameterType.values()));
      return;
    }

    RuntimeContext runtimeContext =
        RevisionManagement.getRuntimeContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    if (payload.getOrderType() == null) {
      List<DestinationKey> destinationKeysWithInheritanceRules = new ArrayList<>(parameterInheritanceMgmt.discoverInheritanceRuleOrderTypes());
      Collections.sort(destinationKeysWithInheritanceRules, COMPARATOR);
      for (DestinationKey dk : destinationKeysWithInheritanceRules) {
        if (dk.getRuntimeContext().equals(runtimeContext)) {
          List<InheritanceRule> rules = parameterInheritanceMgmt.listInheritanceRules(parameterType, dk);
          if (rules.size() > 0) {
            printRules(statusOutputStream, dk, parameterType, rules);
          }
        }
      }
    } else {
      DestinationKey dk = new DestinationKey(payload.getOrderType(), runtimeContext);

      List<InheritanceRule> rules = parameterInheritanceMgmt.listInheritanceRules(parameterType, dk);

      if (rules.size() == 0) {
        writeLineToCommandLine(statusOutputStream, "No inheritance rules of type '" + payload.getParameterType() + "' found for ordertype '"
            + payload.getOrderType() + "'.");
      } else {
        printRules(statusOutputStream, dk, parameterType, rules);
      }
    }
  }


  private void printRules(OutputStream statusOutputStream, DestinationKey dk, ParameterType type, List<InheritanceRule> rules) {
    writeLineToCommandLine(statusOutputStream, "Found " + rules.size() + " inheritance rules of type '" + type
        + "' for ordertype '" + dk.getOrderType() + "':");
    for (InheritanceRule rule : rules) {
      if (rule.getChildFilter() == null || rule.getChildFilter().length() == 0) {
        writeLineToCommandLine(statusOutputStream,
                               "own order type, value: '" + rule.getUnevaluatedValue() + "', precedence: '" + rule.getPrecedence() + "'");
      } else {
        writeLineToCommandLine(statusOutputStream, "child filter: '" + rule.getChildFilter() + "', value: '" + rule.getUnevaluatedValue()
            + "', precedence: '" + rule.getPrecedence() + "'");
      }
    }
  }

}
