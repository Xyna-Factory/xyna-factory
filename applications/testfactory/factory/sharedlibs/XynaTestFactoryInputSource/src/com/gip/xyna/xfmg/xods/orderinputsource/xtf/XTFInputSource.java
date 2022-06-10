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

package com.gip.xyna.xfmg.xods.orderinputsource.xtf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
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


public class XTFInputSource implements OrderInputSource{
  
  
  private DestinationKey workflowDestinationToGenerateFor;
  private Optional<XynaOrderCreationParameter> xocpForWorkflowToCreateInputWith;
  String testcaseid;
  String testcasename;


  public XTFInputSource(DestinationKey workflowDestinationToGenerateFor,
                        Optional<XynaOrderCreationParameter> xocpForWorkflowToCreateInputWith, String testcaseid,
                        String testcasename) {
    this.workflowDestinationToGenerateFor = workflowDestinationToGenerateFor;
    this.xocpForWorkflowToCreateInputWith = xocpForWorkflowToCreateInputWith;
    this.testcaseid = testcaseid;
    this.testcasename = testcasename;
  }


  public OrderInputCreationInstance createInstance() {
    return new XTFInputCreationInstance(this);
  }


  //kann berschrieben werden um mit zusätzlichem input anzureichern
  public Optional<XynaOrderCreationParameter> getOrderCreationParameterForWorkflowToCreateInputWith() {
    if (xocpForWorkflowToCreateInputWith.isPresent()) {
      XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(xocpForWorkflowToCreateInputWith.get());
      //immer neuen destinationkey setzen, weil die instanzen nicht immutable sind.
      xocp.setDestinationKey(new DestinationKey(xocp.getDestinationKey().getOrderType(), xocp.getDestinationKey().getRuntimeContext()));
      xocp.setCustom0(testcaseid);
      xocp.setCustom1(testcasename + " (Input Generation)");
      return Optional.of(xocp);
    } else {
      return Optional.empty();
    }
  }

  public DestinationKey getWorkflowDestinationToGenerateFor() {
    return new DestinationKey(workflowDestinationToGenerateFor.getOrderType(),
                              workflowDestinationToGenerateFor.getRuntimeContext());
  }

  public String getTestcaseid() {
    return testcaseid;
  }


  public Set<DeploymentItemInterface> getDeployedInterfaces() {
    Set<DeploymentItemInterface> set = new HashSet<DeploymentItemInterface>();

    //inputschnittstelle von generierungs-workflow = leer!
    if (xocpForWorkflowToCreateInputWith.isPresent()) {
      OperationInterface operation = OperationInterface.of(null, new ArrayList<TypeInterface>(), null);
      set.add(new DetachedOrderTypeEmployment(xocpForWorkflowToCreateInputWith.get().getDestinationKey(), operation));
      
      //outputschnittstelle von generierungs-workflow = aufgerufener workflow. validiert auch die existenz des ordertypes mit
      set.add(new OutputMatchesInput(xocpForWorkflowToCreateInputWith.get().getDestinationKey(),
                                     workflowDestinationToGenerateFor));
    }

    return set;
  }


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
      RuntimeContext outputProviderContext;
      DispatcherEntry inputDestination;
      RuntimeContext inputConsumerContext;
      try {
        outputDestination = XynaFactory.getInstance().getProcessing().getDestination(DispatcherIdentification.Execution, outputProvider);
        outputProviderContext = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getRuntimeContextDefiningOrderType(outputProvider);
        inputDestination = XynaFactory.getInstance().getProcessing().getDestination(DispatcherIdentification.Execution, inputConsumer);
        inputConsumerContext = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getRuntimeContextDefiningOrderType(inputConsumer);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      }
      
      Long outputProviderRevision;
      if (ExecutionType.getByTypeString(outputDestination.getValue().getDestinationType()) == ExecutionType.XYNA_FRACTAL_WORKFLOW) {
        long revision;
        try {
          revision =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(inputConsumerContext);
          outputProviderRevision = 
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(outputProviderContext);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          return false;
        }
        String outputProviderWfName = outputDestination.getValue().getFqName();
        

        if (ExecutionType.getByTypeString(inputDestination.getValue().getDestinationType()) == ExecutionType.XYNA_FRACTAL_WORKFLOW) {
          String inputProviderWfName = inputDestination.getValue().getFqName();

          DeploymentItemStateManagement deployItemStateMgmt =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
          DeploymentItemState deploymentItemStateOutput = deployItemStateMgmt.get(outputProviderWfName, outputProviderRevision);
          Set<OperationInterface> publishedInterfacesOutput =
              deploymentItemStateOutput.getPublishedInterfaces(OperationInterface.class, DeploymentLocation.DEPLOYED);
          if (publishedInterfacesOutput.size() == 1) {
            DeploymentItemState deploymentItemStateInput = deployItemStateMgmt.get(inputProviderWfName, revision);
            if (deploymentItemStateInput != null) {
              // can be null, presumably if the input generator has not been deployed yet
              Set<OperationInterface> publishedInterfacesInput =
                  deploymentItemStateInput
                      .getPublishedInterfaces(OperationInterface.class, DeploymentLocation.DEPLOYED);
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

}
