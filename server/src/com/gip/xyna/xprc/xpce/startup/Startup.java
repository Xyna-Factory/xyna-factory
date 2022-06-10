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

package com.gip.xyna.xprc.xpce.startup;



import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;



public class Startup extends FunctionGroup {

  public static final String DEFAULT_NAME = "Startup";


  public Startup() throws XynaException {
    super();
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
  }


  public void shutdown() throws XynaException {
  }


  public void executeStartupWorkflow() {

    String startupOrdertype =
        XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.XYNA_XPRC_XPCE_STARTUP_ORDERTYPE);
    if (startupOrdertype == null) {
      return;
    }

    // das folgende ist nur sinnvoll, weil vorher die ganze Initialisierung schon abgeschlossen ist
    DestinationKey startupDestinationKey = new DestinationKey(startupOrdertype);
    DestinationValue startupDestinationValue;
    try {
      startupDestinationValue =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
              .getExecutionEngineDispatcher().getDestination(startupDestinationKey);
    } catch (XPRC_DESTINATION_NOT_FOUND e1) {
      logger.warn("Startup ordertype not configured, skipping.");
      return;
    }

    if (!XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase()
        .isRegisteredByFQ(startupDestinationValue.getFQName(), RevisionManagement.REVISION_DEFAULT_WORKSPACE)) {
      // startup workflow noch nicht registriert, erster startup?
      logger.debug("Workflow for configured startup ordertype could not " + "be found in the "
          + WorkflowDatabase.DEFAULT_NAME + ", trying to redeploy.");
      try {
        // FIXME dependency in die fractal workflow engine!
        WF wf = WF.getInstance(startupOrdertype);
        // TODO welcher DeploymentMode?
        wf.setDeploymentComment("Startup WF");
        wf.deploy(DeploymentMode.codeChanged, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES);
      } catch (XynaException e) {
        logger.warn("Could not deploy configured startup workflow (" + startupOrdertype + "), skipping.");
        return;
      }
    } else {
      logger.debug("Workflow configured for startup ordertype <" + startupOrdertype + ">: <"
          + startupDestinationValue.getFQName() + ">");
    }

    // Startup-Auftrag einstellen
    GeneralXynaObject payload = null;
    XynaOrderServerExtension xo = new XynaOrderServerExtension(startupDestinationKey, payload);

    OrderContext ctx = new OrderContextServerExtension(xo);
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrder(xo, new ResponseListener() {

      private static final long serialVersionUID = 7698057800930797901L;


      @Override
      public void onError(XynaException[] e, OrderContext ctx) {
        logger.info("error while executing startup workflow (" + e[0].toString() + ")", e[0]);
      }


      @Override
      public void onResponse(GeneralXynaObject response, OrderContext ctx) {
        logger.debug("startup workflow finished execution successfully");
        // TODO: Fabrikstatus/Processingstatus auf "initialized" o.ä. setzen?
      }
    }, ctx);

  }

}
