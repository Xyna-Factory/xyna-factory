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
package com.gip.xyna.xact.trigger;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleDispatcher;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.XynaOrder;



public class SNMPStatisticsFilter extends ConnectionFilter<SNMPTriggerConnection> {


  private static final long serialVersionUID = 7444678023994422304L;

  private static Logger logger = CentralFactoryLogging.getLogger(SNMPStatisticsFilter.class);

  private static OidSingleDispatcher oidSingleDispatcher;
  private static volatile RequestHandler requestHandler;

  SNMPFactoryRuntimeStatisticsHandler runtimeStats;

  @Override
  public void onDeployment(EventListener trigger) {
    super.onDeployment(trigger);
    oidSingleDispatcher = new OidSingleDispatcher();
    runtimeStats = new SNMPFactoryRuntimeStatisticsHandler((SNMPTrigger) trigger);
    AbstractSNMPStatisticsHandler generic = new SNMPStatisticsGenericHandler(runtimeStats);
    //AbstractSNMPStatisticsHandler xynaStats = new SNMPXynaStatisticsHandler(null);
    //AbstractSNMPStatisticsHandler generic = new SNMPStatisticsGenericHandler(xynaStats);
    AbstractSNMPStatisticsHandler orders = new SNMPStatisticsOrdersHandler(generic);
    AbstractSNMPStatisticsHandler jvm = new SNMPStatisticsJVMHandler(orders);
    SNMPStatisticsFactoryNodeHandler nodes = new SNMPStatisticsFactoryNodeHandler(null);
    oidSingleDispatcher.add(nodes); // TODO schöner wäre, wenn die handler beim trigger registriert werden. der hat dann die übersicht, was es so alles gibt
    oidSingleDispatcher.add(jvm);
    oidSingleDispatcher.add(orders);
    oidSingleDispatcher.add(generic);
    //oidSingleDispatcher.add(xynaStats);
    oidSingleDispatcher.add(runtimeStats);
    requestHandler = new DefaultRequestHandler(oidSingleDispatcher);
  }


  @Override
  public void onUndeployment(EventListener trigger) {
    runtimeStats.shutdown();
  }


  /**
   * analyzes TriggerConnection and creates XynaOrder if it accepts the connection. if this filter does not return a
   * XynaOrder, Xyna Processing will call generateXynaOrder() of the next Filter registered for the Trigger
   * @param tc
   * @return XynaOrder which will be started by Xyna Processing. null if this Filter doesn't accept the connection
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error. results in
   *           onError() being called by Xyna Processing.
   * @throws InterruptedException if onError() should not be called. (e.g. if for a http trigger connection this filter
   *           decides, it wants to return a 500 servererror, and not call any workflow)
   */
  public XynaOrder generateXynaOrder(SNMPTriggerConnection tc) throws XynaException, InterruptedException {
    // wirft interruptedexception, falls oid bearbeitet wurde (filter greift), ansonsten passiert einfach nichts
    // (nächster filter...)
    tc.handleEvent(requestHandler);
    return null;
  }


  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, SNMPTriggerConnection tc) {
    // nicht zu implementieren, da keine xynaorder gestartet wird
  }


  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, SNMPTriggerConnection tc) {
    if (e != null && e.length > 0) {
      try {
        tc.sendError(RequestHandler.GENERAL_ERROR, e);
      } catch (XynaException e1) {
        logger.error("could not send Error", e1);
      }
    }
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "Filter for SNMP requests. Handles requests for all statistics.";
  }

}
