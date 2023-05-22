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
package com.gip.xyna.xfmg.xods.orderinputsource.workflow;




import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputCreationInstance;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class WorkflowInputCreationInstance implements OrderInputCreationInstance {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(WorkflowInputCreationInstance.class);

  private final WorkflowInputSource inputGen;


  public WorkflowInputCreationInstance(WorkflowInputSource inputGen) {
    this.inputGen = inputGen;
  }


  public XynaOrderCreationParameter generate(long generationContextId,
                                             OptionalOISGenerateMetaInformation parameters) throws XynaException {

    XynaOrderCreationParameter orderToCreateInput = inputGen.getOrderCreationParameterForWorkflowToCreateInputWith();
    orderToCreateInput.setTransientCreationRole(parameters.getTransientCreationRole());
    GeneralXynaObject output = XynaFactory.getInstance().getProcessing().startOrderSynchronously(orderToCreateInput);
    //output element 1 kann allgemeine orderinput-parameter spezifizieren
    //alle folgenden objekte werden 1:1 an den anderen wf weitergereicht

    XynaObject creationParas = null;
    GeneralXynaObject input = new Container();
    if (output == null) {
    } else if (output instanceof Container) {
      Container c = (Container) output;
      if (c.size() == 0) {
      } else {
        for (int i = 0; i < c.size(); i++) {
          if (c.get(i) == null) {
            ((Container) input).add(null);
          } else if (c.get(i).getClass().getName().equals("xprc.xpce.OrderCreationParameter")) {
            if (c.get(i) instanceof XynaObject) {
              creationParas = (XynaObject) c.get(i);
            } else {
              logger.warn("OrderCreationParameter is expected to be a XynaObject.");
            }
          } else {
            ((Container) input).add(c.get(i));
          }
        }
      }
    } else if (output.getClass().getName().equals("xprc.xpce.OrderCreationParameter") && output instanceof XynaObject) {
      creationParas = (XynaObject) output;
    } else {
      input = output;
    }

    DestinationKey dest = inputGen.getWorkflowDestinationToGenerateFor();
    //immer neuen destinationkey setzen, weil die instanzen nicht immutable sind.
    dest = new DestinationKey(dest.getOrderType(), dest.getRuntimeContext());

    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dest, input);

    if (creationParas != null) {
      Integer monitoringLevel = (Integer) creationParas.get("monitoringLevel");
      Integer priority = (Integer) creationParas.get("priority");
      String custom0 = (String) creationParas.get("custom0");
      String custom1 = (String) creationParas.get("custom1");
      String custom2 = (String) creationParas.get("custom2");
      String custom3 = (String) creationParas.get("custom3");
      xocp.setMonitoringLevel(monitoringLevel);
      if (priority != null) {
        xocp.setPriority(priority);
      }
      xocp.setCustom0(custom0);
      xocp.setCustom1(custom1);
      xocp.setCustom2(custom2);
      xocp.setCustom3(custom3);
    }

    return xocp;
  }


  public void notifyOnOrderStart() throws XynaException {
    //ntbd
  }


}
