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
package com.gip.xyna.xfmg.xods.orderinputsource.workflow;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ResolutionFailure;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.DetachedOrderTypeEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.InterfaceWithPotentiallyUnknownProvider;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputCreationInstance;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSource;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class WorkflowInputSource implements OrderInputSource {

  private static class OutputMatchesInput implements DeploymentItemInterface, InterfaceWithPotentiallyUnknownProvider {

    private final DestinationKey outputProvider;
    private final DestinationKey inputConsumer;


    public OutputMatchesInput(DestinationKey outputProvider, DestinationKey inputConsumer) {
      this.outputProvider = outputProvider;
      this.inputConsumer = inputConsumer;
    }


    public boolean providerIsKnown() {
      return false;
    }


    public ResolutionFailure getResolutionFailure() {
      return ResolutionFailure.of(TypeOfUsage.ORDERTYPE, outputProvider.getOrderType() + "->" + inputConsumer.getOrderType());
    }


    public boolean resolve() {
      DispatcherEntry outputDestination;
      RuntimeContext outputContext;
      DispatcherEntry inputDestination;
      RuntimeContext inputContext;
      try {
        outputDestination = XynaFactory.getInstance().getProcessing().getDestination(DispatcherIdentification.Execution, outputProvider);
        outputContext = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getRuntimeContextDefiningOrderType(outputDestination.getKey());
        inputDestination = XynaFactory.getInstance().getProcessing().getDestination(DispatcherIdentification.Execution, inputConsumer);
        inputContext = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getRuntimeContextDefiningOrderType(inputDestination.getKey());
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      }
      if (ExecutionType.getByTypeString(outputDestination.getValue().getDestinationType()) == ExecutionType.XYNA_FRACTAL_WORKFLOW) {
        long outputRevision;
        long inputRevision;
        try {
          outputRevision =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(outputContext);
          inputRevision =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(inputContext);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          return false;
        }
        String outputProviderWfName = outputDestination.getValue().getFqName();

        if (ExecutionType.getByTypeString(inputDestination.getValue().getDestinationType()) == ExecutionType.XYNA_FRACTAL_WORKFLOW) {
          String inputProviderWfName = inputDestination.getValue().getFqName();

          DeploymentItemStateManagement dim =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
          DeploymentItemState deploymentItemStateOutput = dim.get(outputProviderWfName, outputRevision);
          if (deploymentItemStateOutput != null) {
            Set<OperationInterface> publishedInterfacesOutput =
                deploymentItemStateOutput.getPublishedInterfaces(OperationInterface.class, DeploymentLocation.DEPLOYED);
            if (publishedInterfacesOutput.size() == 1) {
              DeploymentItemState deploymentItemStateInput = dim.get(inputProviderWfName, inputRevision);
              if (deploymentItemStateInput != null) {
                Set<OperationInterface> publishedInterfacesInput =
                    deploymentItemStateInput.getPublishedInterfaces(OperationInterface.class, DeploymentLocation.DEPLOYED);
                if (publishedInterfacesInput.size() == 1) {
                  //vergleichen, dass output auf input passt
                  OperationInterface wfWithOutputInterface = publishedInterfacesOutput.iterator().next();
                  OperationInterface wfWithInputInterface = publishedInterfacesInput.iterator().next();

                  return mayCall(wfWithOutputInterface, wfWithInputInterface);
                }
              }
            }
          }
        }
      }
      return false;
    }


    private boolean mayCall(OperationInterface caller, OperationInterface interfaceToCall) {
      //nun kann der caller noch den zusätzlichen xprc.xpce.OrderCreationParameter parameter haben, den man beim vergleich ignorieren muss

      List<TypeInterface> input = new ArrayList<TypeInterface>();
      for (TypeInterface ti : caller.getOutput()) {
        if (!ti.getName().equals("xprc.xpce.OrderCreationParameter")) {
          input.add(ti);
        }
      }
      OperationInterface tmp = OperationInterface.of(null, input, null);
      return interfaceToCall.matches(tmp);
    }


    public String getDescription() {
      return "<" + inputConsumer.getOrderType() + ">'s output used as input for <" + outputProvider.getOrderType() + ">";
    }


    public TypeInterface getProvider() {
      return null;
    }

  }


  private DestinationKey workflowDestinationToGenerateFor;
  private XynaOrderCreationParameter xocpForWorkflowToCreateInputWith;


  public WorkflowInputSource(DestinationKey workflowDestinationToGenerateFor, XynaOrderCreationParameter xocpForWorkflowToCreateInputWith) {
    this.workflowDestinationToGenerateFor = workflowDestinationToGenerateFor;
    this.xocpForWorkflowToCreateInputWith = xocpForWorkflowToCreateInputWith;
  }


  public DestinationKey getWorkflowDestinationToGenerateFor() {
    return workflowDestinationToGenerateFor;
  }


  public OrderInputCreationInstance createInstance() {
    return new WorkflowInputCreationInstance(this);
  }


  //kann überschrieben werden um mit zusätzlichem input anzureichern
  public XynaOrderCreationParameter getOrderCreationParameterForWorkflowToCreateInputWith() {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(xocpForWorkflowToCreateInputWith);
    //immer neuen destinationkey setzen, weil die instanzen nicht immutable sind.
    xocp.setDestinationKey(new DestinationKey(xocp.getDestinationKey().getOrderType(), xocp.getDestinationKey().getRuntimeContext()));

    return xocp;
  }


  public Set<DeploymentItemInterface> getDeployedInterfaces() {
    Set<DeploymentItemInterface> set = new HashSet<DeploymentItemInterface>();

    //inputschnittstelle von generierungs-workflow = leer!
    OperationInterface operation = OperationInterface.of(null, new ArrayList<TypeInterface>(), null);
    set.add(new DetachedOrderTypeEmployment(xocpForWorkflowToCreateInputWith.getDestinationKey(), operation));

    //outputschnittstelle von generierungs-workflow = aufgerufener workflow. validiert auch die existenz des ordertypes mit
    set.add(new OutputMatchesInput(xocpForWorkflowToCreateInputWith.getDestinationKey(), workflowDestinationToGenerateFor));
    return set;
  }


}
