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

package com.gip.xyna.xfmg.xods.orderinputsource.xtf;



import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputCreationInstance;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xnwh.persistence.xmom.IFormula;
import com.gip.xyna.xnwh.persistence.xmom.QueryParameter;
import com.gip.xyna.xnwh.persistence.xmom.SelectionMask;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;



public class XTFInputCreationInstance implements OrderInputCreationInstance {

  private final XTFInputSource inputGen;
  private long generationContextId;
  private static final String NOTIFICATION_ORDER_TYPE = "xdev.xtestfactory.infrastructure.util.NotifyTestCaseExecution";

  private static XynaPropertyInt GENERATOR_MON_LEVEL =
      new XynaPropertyInt("xdev.xtestfactory.infrastructure.generator_monitoring_level",
                          MonitoringCodes.STEP_MONITORING_ON_ERROR_NO_ARCHIVE);
  static {
    GENERATOR_MON_LEVEL.setDefaultDocumentation(DocumentationLanguage.EN, "Defines the monitoring level that "
        + "is used for input generating workflows. -1 for no special configuration.");
    GENERATOR_MON_LEVEL.setDefaultDocumentation(DocumentationLanguage.DE, "Definiert das Monitoring Level,"
        + " welches für die Input-generierenden Workflows verwendet wird. (-1 zur Verwendung des dem Ordertype zugeordneten Monitoring Levels)");
  }
  
  private OptionalOISGenerateMetaInformation params;


  public XTFInputCreationInstance(XTFInputSource inputGen) {
    this.inputGen = inputGen;
  }


  public XynaOrderCreationParameter generate(long generationContextId, OptionalOISGenerateMetaInformation params)
      throws XynaException {

    this.generationContextId = generationContextId;
    this.params = params;
    
    GeneralXynaObject output;
    Optional<XynaOrderCreationParameter> orderToCreateInput = inputGen.getOrderCreationParameterForWorkflowToCreateInputWith();
    XynaOrderCreationParameter xocp = orderToCreateInput.get();
    XynaOrderServerExtension xose; 
    OrderContextServerExtension ctx;
        
    if (orderToCreateInput.isPresent()) {
      
      xocp.setCustom3("" + generationContextId);
  
      // set mon level including all subworkflows
      int targetMonLvl = GENERATOR_MON_LEVEL.get();
      if (targetMonLvl > -1) {
        xocp.setMonitoringLevel(targetMonLvl);
        xocp.addParameterInheritanceRule(ParameterType.MonitoringLevel,
                                                       InheritanceRule.createMonitoringLevelRule(targetMonLvl + "").precedence(0)
                                                           .childFilter("*").build());
      }
      if (startedFromGUI(params)) {
        xocp.setCustom2(params.getValue(XTFInputSourceType.KEY_EXECUTING_USER));
        // removed, currently not supported in 6.1.2.x
        //orderToCreateInput.setTransientCreationRole(params.getTransientCreationRole());
        xose = new XynaOrderServerExtension(xocp);
        ctx = new OrderContextServerExtension(xose);
        ctx.set("startedFromGUI", true);
      } else {
        xose = new XynaOrderServerExtension(xocp);
        ctx = new OrderContextServerExtension(xose);
        ctx.set("startedFromGUI", false);
      }
      if (xocp.getAcknowledgableObject() != null) {
        ctx.set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY, xocp.getAcknowledgableObject());
      }
      if (xocp.getTransientCreationRole() != null) {
        ctx.set(OrderContextServerExtension.CREATION_ROLE_KEY, xocp.getTransientCreationRole());
      }
      
      xose.setOrderContext(ctx);
      xose = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrderSynchronous(xose);
      output = xose.getOutputPayload();
    } else {
      output = new Container();
    }
    

    // ein output element darf allgemeine orderinput-parameter spezifizieren
    // alle anderen objekte werden 1:1 an den anderen wf weitergereicht

    XynaObject creationParas = null;
    GeneralXynaObject input = new Container();
    if (output == null) {
      throw new RuntimeException("Order returns null!");
    } else if (output instanceof Container) {
      Container c = (Container) output;
      if (c.size() == 0) {
        
      } else {
        for (int i = 0; i < c.size(); i++) {
          if (c.get(i) == null) {
            ((Container) input).add(null);
          } else if (c.get(i).getClass().getName().equals("xprc.xpce.OrderCreationParameter")) {
            creationParas = (XynaObject) c.get(i);
          } else {
            ((Container) input).add(c.get(i));
          }
        }
      }
    } else if (output instanceof XynaObject) {
      if (output.getClass().getName().equals("xprc.xpce.OrderCreationParameter")) {
        creationParas = (XynaObject) output;
      } else {
        input = (XynaObject) output;
      }
    } else {
      input = output;
    }

    DestinationKey dest = inputGen.getWorkflowDestinationToGenerateFor();
    xocp = new XynaOrderCreationParameter(dest, input);

    String testcaseid = inputGen.getTestcaseid();
    XynaObject currentCase =  getStorableFromFactory("xdev.xtestfactory.infrastructure.storables.TestCase", testcaseid);
    Integer priority = (Integer)currentCase.get("priority");

    if (priority != null) {
      xocp.setPriority(priority);
    }

    Integer monitoringLevel = null;
    if (creationParas != null) {
      Integer overriddenMonitoringLevel = (Integer) creationParas.get("monitoringLevel");
      if (overriddenMonitoringLevel != null) {
        monitoringLevel = overriddenMonitoringLevel;
      }
      String custom3 = (String) creationParas.get("custom3");
      xocp.setCustom3(custom3);
    }

    if (monitoringLevel != null) {
      xocp.setMonitoringLevel(monitoringLevel);
    }
    xocp.setCustom0(inputGen.testcaseid);
    xocp.setCustom1(inputGen.testcasename);
    if (params != null) {
      xocp.setCustom2(params.getValue(XTFInputSourceType.KEY_EXECUTING_USER));
    }

    return xocp;

  }


  public void notifyOnOrderStart() throws XynaException {
    if (startedFromGUI(params)) {
      
      RuntimeContext targetRuntimeContext = inputGen.getWorkflowDestinationToGenerateFor().getRuntimeContext();
      XynaOrderCreationParameter xocp =
          new XynaOrderCreationParameter(new DestinationKey(NOTIFICATION_ORDER_TYPE, targetRuntimeContext),
                                         new Container());
      xocp.setCustom3("" + generationContextId);
      xocp.setMonitoringLevel(MonitoringCodes.STEP_MONITORING_ON_ERROR_NO_ARCHIVE);
      XynaFactory.getInstance().getProcessing().startOrderSynchronously(xocp);
    }
  }


  private XynaObject getStorableFromFactory(String storableName, String inputId) {
    try {

      XMOMPersistenceManagement persistenceMgmt =
          XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();

      // select * from <storableName>
      SelectionMask selectionMask = new SelectionMask(storableName);
      QueryParameter queryParameter = new QueryParameter(1, false, null);

      // where <idColumn> = <id>
      IFormula formula = new GetInputById(inputId);

      long revision;
      revision =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(inputGen.getWorkflowDestinationToGenerateFor().getRuntimeContext());

      List<? extends XynaObject> result = persistenceMgmt.query(null, selectionMask, formula, queryParameter, revision);

      return result.get(0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  public class GetInputById implements IFormula {

    private String id;


    public GetInputById(String id) {
      super();
      this.id = id;
    }


    public List<Accessor> getValues() {
      return Collections.emptyList();
    }


    public String getFormula() {
      return "%0%.iD == \"" + id + "\"";
    }
  }
  
  private boolean startedFromGUI (OptionalOISGenerateMetaInformation params) {
    if (params.getValue(XTFInputSourceType.KEY_EXECUTING_USER) != null) {
      return true;
    } else {
      return false;
    }
  }


}
