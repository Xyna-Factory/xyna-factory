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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.trigger.DeployFilterParameter;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.StringParameterFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Deployfilter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class DeployfilterImpl extends XynaCommandImplementation<Deployfilter> {

  public void execute(OutputStream statusOutputStream, Deployfilter payload) throws XynaException {
    
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();

    DeployFilterParameter dfp = new DeployFilterParameter.Builder().
         filterName(payload.getFilterName()).instanceName(payload.getFilterInstanceName()).
         triggerInstanceName(payload.getTriggerInstanceName()).description(payload.getDocumentation()).
         optional(payload.getOptional()).configuration(payload.getConfigurationParameter()).
         revision(revision).build();
    
    if( dfp.getConfiguration() != null &&  ! dfp.getConfiguration().isEmpty() && "help".equals(dfp.getConfiguration().get(0)) ) {
      writeLineToCommandLine(statusOutputStream, printHelp(xat, dfp) );
      return;
    }
    
    
    CommandControl.tryLock(CommandControl.Operation.FILTER_DEPLOY, revision);
    try {
      xat.deployFilter(dfp);
      
      Filter filter = xat.getFilter(revision, payload.getFilterName(), true);
      StringBuilder sb = new StringBuilder();
      xat.appendTriggerState(sb, filter.getTriggerName(), revision, payload.getVerbose());
      writeLineToCommandLine(statusOutputStream, sb);
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_DEPLOY, revision);
    }

  }

  private String printHelp(XynaActivationTrigger xat, DeployFilterParameter dfp) throws PersistenceLayerException, XACT_FilterNotFound {
    FilterInformation fi = xat.getFilterInformation(dfp.getFilterName(), dfp.getRevision(), true);
    List<StringParameter<?>> cps = fi.getConfigurationParameter();
    if( cps == null || cps.isEmpty() ) {
      return "no configuration possible";
    }
    StringBuilder output = new StringBuilder();
    output.append("possible configuration parameter as key/value pairs (key=value):\n");
    for( StringParameter<?> sp : cps ) {
      output.append("  - ");
      StringParameterFormatter.appendStringParameter( output, sp );
      output.append("\n");
    }
    
    return output.toString();
  }

}
