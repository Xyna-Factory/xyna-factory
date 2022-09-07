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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Addinheritancerule;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.Builder;



public class AddinheritanceruleImpl extends XynaCommandImplementation<Addinheritancerule> {

  public void execute(OutputStream statusOutputStream, Addinheritancerule payload) throws XynaException {
    ParameterInheritanceManagement parameterInheritanceMgmt = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();
    
    ParameterType parameterType = null;
    try {
      parameterType = ParameterType.valueOf(payload.getParameterType());
    } catch (IllegalArgumentException e) {
      writeLineToCommandLine(statusOutputStream, "Invalid parameter type '" + payload.getParameterType() + "'. Allowed values are "
          + Arrays.toString(ParameterType.values()) + ".");
      return;
    }
    
    Builder builder;
    try {
      builder = parameterType.createInheritanceRuleBuilder(payload.getValue());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, "Cannot parse value '", payload.getValue(), "'");
      return;
    }
    
    if (payload.getChildFilter() != null) {
      builder.childFilter(payload.getChildFilter());
    }
    
    if (payload.getPrecedence() != null) {
      try {
        builder.precedence(Integer.valueOf(payload.getPrecedence()));
      } catch (NumberFormatException e) {
        writeLineToCommandLine(statusOutputStream,
                               "Could not parse precedence '" + payload.getPrecedence() + "'");
        return;
      }
    }

    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.getRuntimeContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    Long revision = revisionManagement.getRevision(runtimeContext);

    DestinationKey dk = new DestinationKey(payload.getOrderType(), runtimeContext);
    
    CommandControl.tryLock(CommandControl.Operation.ORDERTYPE_MODIFY, revision);
    try {
      parameterInheritanceMgmt.addInheritanceRule(parameterType, dk, builder.build());
    } finally {
      CommandControl.unlock(CommandControl.Operation.ORDERTYPE_MODIFY, revision);
    }
  }

}
