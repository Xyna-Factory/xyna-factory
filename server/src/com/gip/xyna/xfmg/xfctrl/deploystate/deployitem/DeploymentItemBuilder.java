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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ProblemType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ResolutionFailure;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.AccessPart;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.InstanceMethodAccessPart;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.MemberVarAccessPart;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface.ImplementationType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface.OperationType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface.AvariableNotResolvableException;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSource;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.Expression2Args;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.CastExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.DynamicResultTypExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionParameterTypeDefinition;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Operator;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.EmptyVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.ServiceIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.InvalidInvocationException;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCallInService;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;

public class DeploymentItemBuilder {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(DeploymentItemBuilder.class);
  
  public interface InterfaceWithPotentiallyUnknownProvider {

    public boolean providerIsKnown();

    public ResolutionFailure getResolutionFailure();
    
    public TypeInterface getProvider();
    
  }

  /*
   * bei verwendung einer inputsource in einem workflow soll der workflow genau dann invalide werden, wenn durch die reparatur der inputsource der workflow
   * nicht valide werden kann.
   * damit verbleibt das problem der outputs, die mit dem output des von der inputsource referenzierten workflows übereinstimmen müssen. 
   */
  public static class UsageOfOutputsOfWFReferencedByInputSource implements DeploymentItemInterface, InterfaceWithPotentiallyUnknownProvider {

    private final String inputSourceName;
    private final long revision;
    private final String fqWFName;
    private final OperationInterface expectedOperationOutputsOfCalledWorkflow; //nur output ist von interesse 


    private UsageOfOutputsOfWFReferencedByInputSource(long revision, String inputSourceName, String fqWFName,
                               OperationInterface expectedOperationOutputsOfCalledWorkflow) {
      this.revision = revision;
      this.inputSourceName = inputSourceName;
      this.fqWFName = fqWFName;
      this.expectedOperationOutputsOfCalledWorkflow =
          OperationInterface.of(expectedOperationOutputsOfCalledWorkflow.getName(), null,
                                expectedOperationOutputsOfCalledWorkflow.getOutput());
    }


    public boolean resolve() {
      OrderInputSourceStorable ois;
      try {
        ois = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement()
            .getInputSourceByName(revision, inputSourceName);
      } catch (PersistenceLayerException e1) {
        return false;
      }
      if (ois == null) {
        return false;
      }
      try {
        DestinationValue executionDestination =
            XynaFactory
                .getInstance()
                .getProcessing()
                .getXynaProcessCtrlExecution()
                .getXynaExecution()
                .getExecutionDestination(new DestinationKey(ois.getOrderType(), XynaFactory.getInstance().getFactoryManagement()
                                             .getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision)));
        if (executionDestination == null || !executionDestination.getFQName().equals(fqWFName)) {
          //derzeit nicht unterstützt, dass man einen ordertype verwendet, der einen anderen kompatiblen workflow aufruft
          return false;
        }
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return false;
      }

      return InterfaceEmployment.of(TypeInterface.of(fqWFName), expectedOperationOutputsOfCalledWorkflow).resolve();
    }


    public String getDescription() {
      return "OrderInputSource " + inputSourceName;
    }


    public static DeploymentItemInterface of(StepFunction stepFunction) {
      try {
        ServiceIdentification sid = stepFunction.getParentScope().identifyService(stepFunction.getServiceId());
        String fqWFName = sid.service.getOriginalFqName();
        DeploymentItemInterface oi = OperationInterface.of(stepFunction, sid);
        if (oi instanceof OperationInterface) {
          return new UsageOfOutputsOfWFReferencedByInputSource(stepFunction.getParentWFObject().getRevision(), stepFunction.getOrderInputSourceRef(), fqWFName,
                                        (OperationInterface) oi);
        } else if (oi instanceof UnresolvableInterface) {
          return oi;
        } else {
          throw new RuntimeException("unexpected type of deploymentiteminterface: " + oi.getClass().getSimpleName());
        }
      } catch (XPRC_InvalidServiceIdException e) {
        return UnresolvableInterface.get(TypeOfUsage.SERVICE_REFERENCE, stepFunction.getServiceId(), stepFunction.getXmlId());
      }
    }


    public boolean providerIsKnown() {
      return getProvider() != null;
    }
    

    public ResolutionFailure getResolutionFailure() {
      return ResolutionFailure.of(TypeOfUsage.SERVICE_REFERENCE, fqWFName); //TODO TypeOfUsage.OrderInputSource reference erstellen?
    }


    public TypeInterface getProvider() {
      return TypeInterface.of(fqWFName);
    }


    public String getWFName() {
      return fqWFName;
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fqWFName == null) ? 0 : fqWFName.hashCode());
      result = prime * result + ((inputSourceName == null) ? 0 : inputSourceName.hashCode());
      result = prime * result + (int) (revision ^ (revision >>> 32));
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      UsageOfOutputsOfWFReferencedByInputSource other = (UsageOfOutputsOfWFReferencedByInputSource) obj;
      if (fqWFName == null) {
        if (other.fqWFName != null)
          return false;
      } else if (!fqWFName.equals(other.fqWFName))
        return false;
      if (inputSourceName == null) {
        if (other.inputSourceName != null)
          return false;
      } else if (!inputSourceName.equals(other.inputSourceName))
        return false;
      if (revision != other.revision)
        return false;
      return true;
    }
    
    

  }
  
  private static final String ORDERTYPE_PREFIX = "xprc.xpce.ordertype";

  //verwendung vom ordertype ohne genaueres wissen über die eingangsschnittstelle des workflows
  public static class OrderTypeEmployment implements DeploymentItemInterface, InterfaceWithPotentiallyUnknownProvider {

    private final DestinationKey destinationKey;


    public OrderTypeEmployment(DestinationKey destinationKey) {
      this.destinationKey = destinationKey;
    }


    public boolean resolve() {
      try {
        XynaFactory.getInstance().getProcessing().getDestination(DispatcherIdentification.Execution, destinationKey);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      }
      return true;
    }


    public String getDescription() {
      return "Ordertype " + destinationKey;
    }


    public boolean providerIsKnown() {
      return true;
    }


    public ResolutionFailure getResolutionFailure() {
      return ResolutionFailure.of(TypeOfUsage.ORDERTYPE, destinationKey.getOrderType());
    }


    public TypeInterface getProvider() {
      TypeInterface ti = new TypeInterface(ORDERTYPE_PREFIX + "." + destinationKey.getOrderType()); //TODO XMOMType ersatz enum auf ORDERTYPE setzen
      return ti;
    }


    public String getOrderType() {
      return destinationKey.getOrderType();
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((destinationKey == null) ? 0 : destinationKey.hashCode());
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      OrderTypeEmployment other = (OrderTypeEmployment) obj;
      if (destinationKey == null) {
        if (other.destinationKey != null)
          return false;
      } else if (!destinationKey.equals(other.destinationKey))
        return false;
      return true;
    }

  }
  
  //verwendung von ordertype und eingangsschnittstelle von workflow. name des workflows ist irrelevant (weil über ordertype gestartet), und ausgangsschnittstelle auch
  public static class DetachedOrderTypeEmployment implements DeploymentItemInterface, InterfaceWithPotentiallyUnknownProvider {

    private final DestinationKey destinationKey;
    private final OperationInterface signatureOfCalledWF; //operationInterface hat namen mit drin - braucht man hier nicht. es geht hier nur darum zu beschreiben, welche inputdaten man von der destination verwendet


    public DetachedOrderTypeEmployment(DestinationKey destinationKey, OperationInterface signatureOfCalledWF) {
      this.destinationKey = destinationKey;
      this.signatureOfCalledWF = signatureOfCalledWF;
    }


    public boolean resolve() {
      try {
        DispatcherEntry destination =
            XynaFactory.getInstance().getProcessing().getDestination(DispatcherIdentification.Execution, destinationKey);
        //check, dass destination die entsprechende signatur hat
        if (ExecutionType.getByTypeString(destination.getValue().getDestinationType()) == ExecutionType.XYNA_FRACTAL_WORKFLOW) {
          String wfName = destination.getValue().getFqName();
          signatureOfCalledWF.setName(GenerationBase.getSimpleNameFromFQName(wfName));
          InterfaceEmployment employment = InterfaceEmployment.of(TypeInterface.of(wfName), signatureOfCalledWF);
          return employment.resolve();
        } else {
          return false; //nicht unterstützt
        }
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      }
    }


    public TypeInterface getProvider() {
      DispatcherEntry destination;
      try {
        destination = XynaFactory.getInstance().getProcessing().getDestination(DispatcherIdentification.Execution, destinationKey);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return null;
      }
      if (destination.getValue().getDestinationType().equals(ExecutionType.XYNA_FRACTAL_WORKFLOW.name())) {
        return TypeInterface.of(destination.getValue().getFqName());
      } else {
        return null;
      }
    }

    public String getOrderType() {
      return destinationKey.getOrderType();
    }

    public boolean providerIsKnown() {
      return getProvider() != null;
    }


    public String getDescription() {
      return "Ordertype " + destinationKey + " -> " + signatureOfCalledWF;
    }


    public ResolutionFailure getResolutionFailure() {
      return ResolutionFailure.of(TypeOfUsage.ORDERTYPE, destinationKey.getOrderType());
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((destinationKey == null) ? 0 : destinationKey.hashCode());
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      DetachedOrderTypeEmployment other = (DetachedOrderTypeEmployment) obj;
      if (destinationKey == null) {
        if (other.destinationKey != null)
          return false;
      } else if (!destinationKey.equals(other.destinationKey))
        return false;
      return true;
    }


  }


  public static Optional<DeploymentItem> buildInputSource(OrderInputSourceStorable oigs, OrderInputSource inputSource) {
    String inputSourceFQName = OrderInputSourceManagement.convertNameToUniqueDeploymentItemStateName(oigs.getName());
    DeploymentItem di = new DeploymentItem(inputSourceFQName, "InputSource");
    di.setLabel(oigs.getName());
    di.addInterfaceEmployment(DeploymentLocation.SAVED, new OrderTypeEmployment(oigs.getDestinationKey()));
    for (DeploymentItemInterface dii : inputSource.getDeployedInterfaces()) {
      di.addInterfaceEmployment(DeploymentLocation.SAVED, dii);
    }
    di.addPublishedInterface(DeploymentLocation.SAVED, TypeInterface.of(inputSourceFQName));
    di.setLastModified(System.currentTimeMillis());
    di.setType(XMOMType.ORDERINPUTSOURCE);
    return Optional.of(di);
  }

  
  public static Optional<DeploymentItem> build(String fqName, Optional<XMOMType> optionalType, long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    return build(fqName, optionalType, false, revision);
  }
  
  public static Optional<DeploymentItem> build(String fqName, Optional<XMOMType> optionalType, boolean fromSavedOnly, long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    Set<DeploymentLocation> sources = new HashSet<DeploymentLocation>();
    sources.add(DeploymentLocation.SAVED);
    if (!fromSavedOnly) {
      sources.add(DeploymentLocation.DEPLOYED);  
    }
    return build(fqName, optionalType, sources, revision, false, new EnumMap<DeploymentLocation, GenerationBaseCache>(DeploymentLocation.class));
  }


  public static Optional<DeploymentItem> build(String fqName, Optional<XMOMType> optionalType, Set<DeploymentLocation> sources, long revision, boolean isApplicationItem, Map<DeploymentLocation, GenerationBaseCache> cache) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    boolean parseSaved = false;
    File savedFile = null;
    if (!isApplicationItem) {
      savedFile = new File(GenerationBase.getFileLocationForSavingStaticHelper(fqName, revision) + ".xml");
      if (sources.contains(DeploymentLocation.SAVED) && savedFile.exists()) {
        parseSaved = true;
      }
    }
    boolean parseDeployed = false;
    File deployedFile = new File(GenerationBase.getFileLocationForDeploymentStaticHelper(fqName, revision) + ".xml");
    if (sources.contains(DeploymentLocation.DEPLOYED) && deployedFile.exists()) {
      parseDeployed = true;
    }
    if (!parseSaved && !parseDeployed) {
      return Optional.empty();
    }
    XMOMType type;
    if (optionalType.isPresent()) {
      type = optionalType.get();
    } else {
      type = XMOMType.getXMOMTypeByRootTag(GenerationBase.retrieveRootTag(fqName, revision, true, false));
    }
    if (type == XMOMType.FORM) {
      return Optional.empty();
    }
    DeploymentItem di = new DeploymentItem(fqName, type);
    di.setApplicationItem(isApplicationItem);
    if (parseDeployed) {
      di.setDeployed(true);
      di.setLastDeployed(deployedFile.lastModified());
      DeploymentState ds = deriveDeploymentState(fqName, type, revision, deployedFile);
      switch (ds) {
        case INCOMPLETE :
          di.setIncomplete(true);
          // fall through
        case DEPLOYED :
          break;
        case WEIRD :
          // TODO log
          // just ignore that object for now? or register as saved?
          return Optional.empty();
        case NOT_DEPLOYED :
          di.setLocationContentChanges(false);
          break;
        default :
          break;
      }
      GenerationBase deployed = generateInstance(fqName, type, revision, DeploymentLocation.DEPLOYED, cache);
      Pair<Set<DeploymentItemInterface>, Set<DeploymentItemInterface>> deployedInterfaces = buildInterfaces(deployed);
      for (DeploymentItemInterface diiDeployed : deployedInterfaces.getFirst()) {
        di.addPublishedInterface(DeploymentLocation.DEPLOYED, diiDeployed);
      }
      for (DeploymentItemInterface diiiDeployed : deployedInterfaces.getSecond()) {
        di.addInterfaceEmployment(DeploymentLocation.DEPLOYED, diiiDeployed);
      }
      di.setLabel(deployed.getLabel());
    }
    if (parseSaved) {
      di.setLastModified(savedFile.lastModified());
      
      GenerationBase saved = generateInstance(fqName, type, revision, DeploymentLocation.SAVED, cache);
      Pair<Set<DeploymentItemInterface>, Set<DeploymentItemInterface>> deployedInterfaces = buildInterfaces(saved);
      boolean isReservedObject = GenerationBase.isReservedServerObjectByFqOriginalName(fqName);
      for (DeploymentItemInterface diiSaved : deployedInterfaces.getFirst()) {
        di.addPublishedInterface(DeploymentLocation.SAVED, diiSaved);
        if (isReservedObject) {
          di.addPublishedInterface(DeploymentLocation.DEPLOYED, diiSaved);
        }
      }
      if (!isReservedObject) {
        for (DeploymentItemInterface diiSaved : deployedInterfaces.getSecond()) {
          di.addInterfaceEmployment(DeploymentLocation.SAVED, diiSaved);
        }
      }
      
      if (saved instanceof DOM) {
        DOM dom = (DOM) saved;
        String fqClassName = GenerationBase.transformNameForJava(fqName);
        String libName = GenerationBase.getSimpleNameFromFQName(fqClassName) + "Impl.jar";
        for (Operation op : dom.getOperations()) {
          if (op.implementedInJavaLib()) {
            //Jar wird verwendet
            File jarFile = new File(GenerationBase.getFileLocationOfServiceLibsForSaving(fqClassName, revision) + Constants.fileSeparator + libName);
            di.setSavedImplJar(jarFile);
            break;
          }
        }
      }
      
      di.setLabel(saved.getLabel());
    }
    // TODO compare xml-strings for location content changes?
    return Optional.of(di);
  }

  private static Pair<Set<DeploymentItemInterface>, Set<DeploymentItemInterface>> buildInterfaces(GenerationBase gb) {
    if (gb instanceof WF) {
      return buildWFInterfaces((WF) gb);
    } else if (gb instanceof DomOrExceptionGenerationBase) {
      return buildDomOrExceptionInterfaces((DomOrExceptionGenerationBase) gb);
    } else {
      throw new IllegalArgumentException("fsgfaks");
    }
  }
  
  
  private static Pair<Set<DeploymentItemInterface>, Set<DeploymentItemInterface>> buildWFInterfaces(WF wf) {
    Set<DeploymentItemInterface> invocations = new HashSet<DeploymentItemInterface>();
    Set<Step> steps = new HashSet<Step>();
    // input, output, exceptions
    for (AVariable in : wf.getInputVars()) {
      invocations.add(extractTypeInterface(in, TypeOfUsage.INPUT, wf.getWfAsStep()));
    }
    for (AVariable out : wf.getOutputVars()) {
      invocations.add(extractTypeInterface(out, TypeOfUsage.OUTPUT, wf.getWfAsStep()));
    }
    for (AVariable thrown : wf.getAllThrownExceptions()) {
      if (thrown.isPrototype()) {
        //FIXME TypeOfUsage ist nicht korrekt, müsste EXCEPTION sein, gibts aber nicht
        invocations.add(extractTypeInterface(thrown, TypeOfUsage.OUTPUT, null));
      } else {
        invocations.add(TypeInterface.of(thrown, XMOMType.EXCEPTION));
      }
    }
    WF.addChildStepsRecursively(steps, wf.getWfAsStep());
    for (Step step : steps) {
      // StepCatch: exceptions
      if (step instanceof StepCatch) {
        invocations.addAll(getInvocations((StepCatch) step));
      } else /* StepChoice: might contain doms or formulas */ if (step instanceof StepChoice) {
        invocations.addAll(checkMappingExpressions((StepChoice) step));
        invocations.addAll(getChoiceClasses((StepChoice) step));
      } else /* StepMapping: mappings */ if (step instanceof StepMapping) {
        for (String id : step.getInputVarIds()) {
          invocations.addAll(generateVarAccess(step, id, TypeOfUsage.INPUT));
        }
        for (String id : step.getOutputVarIds()) {
          invocations.addAll(generateVarAccess(step, id, TypeOfUsage.OUTPUT));
        }
        invocations.addAll(checkMappingExpressions((StepMapping)step));
        invocations.addAll(extractModelledExpressionInvocations(((StepMapping) step).getParsedExpressions(), step.getXmlId(), true));
      } else /* Step throw ? */ if (step instanceof StepThrow) {
        String id = ((StepThrow)step).getAllUsedVariableIds().iterator().next();
        try {
          VariableIdentification varId =  step.getParentScope().identifyVariable(id);
          invocations.add(TypeInterface.of(varId.getVariable(), XMOMType.EXCEPTION));
        } catch (XPRC_InvalidVariableIdException e) {
          invocations.add(UnresolvableInterface.get(TypeOfUsage.EMPLOYMENT, id, step.getXmlId()));
        }
      } else /* Step assign ? */ if (step instanceof StepAssign) {
        invocations.addAll(extractAssignementAccess((StepAssign) step));
      } else /* Step Function: subWfs, serviceOps */ if (step instanceof StepFunction) {
        StepFunction stepFunction = (StepFunction) step;
        if (stepFunction.getOrderInputSourceRef() == null) {
          Collection<? extends DeploymentItemInterface> oDiis = extractFunctionInvocation(stepFunction);
          if (oDiis.size() > 0) {
            for (DeploymentItemInterface oDii: oDiis) {
              invocations.add(oDii);
            }
          }
          for (String id : stepFunction.getInputVarIds()) {
            invocations.addAll(generateVarAccess(step, id, TypeOfUsage.INPUT));
          }
        } else {
          /*
           * überprüfung von:
           * - existenz von inputsource
           * - zeigt ordertype von inputsource auf den workflow, der referenziert wird
           * - passen die erwarteten outputs zu den outputs vom workflow
           */
          invocations.add(TypeInterface.of(OrderInputSourceManagement.convertNameToUniqueDeploymentItemStateName(stepFunction
              .getOrderInputSourceRef()), "InputSource"));
          invocations.add(UsageOfOutputsOfWFReferencedByInputSource.of(stepFunction));
        }

        for (String id : stepFunction.getOutputVarIds()) {
          invocations.addAll(generateVarAccess(step, id, TypeOfUsage.OUTPUT));
        }
      }
    }
    Set<DeploymentItemInterface> published = new HashSet<DeploymentItemInterface>();
    DeploymentItemInterface dii;
    try {
      dii = OperationInterface.of(wf);
      published.add(dii);
    } catch (AvariableNotResolvableException e) {
      //nicht auflösbare variable wurden oben bereits ermittelt.
    }
    published.add(TypeInterface.of(wf));
    return Pair.of(published, invocations);
  }
  
  private static DeploymentItemInterface extractTypeInterface(AVariable var, TypeOfUsage type, Step container) {
    if (var.isPrototype()) {
      Integer xmlId = -1;
      if (container != null) {
        xmlId = container.getXmlId();
      }
      return new UnresolvableInterface.PrototypeElement(type, xmlId);
    } else {
      try {
        return TypeInterface.of(var, type);
      } catch (AvariableNotResolvableException e) {
        return e.getProblem();
      }
    }
  }
  
  private static Set<? extends DeploymentItemInterface> checkMappingExpressions(StepMapping step) {
    return validateModelledExpressions(step.getParsedExpressions(), step.getRawExpressions(), step);
  }
  
  private static Set<? extends DeploymentItemInterface> checkMappingExpressions(StepChoice step) {
    if (step.getParsedFormulas() != null) {
      return validateModelledExpressions(step.getParsedFormulas(), step.getComplexCaseNames(), step);
    } else {
      return Collections.emptySet();
    }
  }
  
  
  private static Set<? extends DeploymentItemInterface> validateModelledExpressions(List<ModelledExpression> exprs, List<String> raw, Step step) {
    List<Integer> expressionsToValidate = new ArrayList<Integer>();
    for (int i=0; i<exprs.size(); i++) {
      if (exprs.get(i) == null) {
        expressionsToValidate.add(i);
      }
    }
    if (expressionsToValidate.size() > 0) {
      Set<DeploymentItemInterface> validationFailures = new HashSet<DeploymentItemInterface>();
      for (Integer index : expressionsToValidate) {
        try {
          ModelledExpression.parse(step, raw.get(index));
        } catch (XPRC_ParsingModelledExpressionException e) {
          validationFailures.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, e.getMessage(), step.getXmlId()));
        }
      }
      return validationFailures;
    } else {
      return Collections.emptySet();
    }
  }

  private static Pair<Set<DeploymentItemInterface>, Set<DeploymentItemInterface>> buildDomOrExceptionInterfaces(DomOrExceptionGenerationBase domOrException) {
    Set<DeploymentItemInterface> published = new HashSet<DeploymentItemInterface>();
    Map<DeploymentItemInterface, DeploymentItemInterface> invocations = new HashMap<DeploymentItemInterface, DeploymentItemInterface>();
    TypeInterface myType = TypeInterface.of(domOrException);
    published.add(myType);
    if (domOrException.hasSuperClassGenerationObject()) {
      published.add(SupertypeInterface.of(domOrException));
      addToInvocation(invocations, TypeInterface.of(domOrException.getSuperClassGenerationObject(), TypeOfUsage.SUPERTYPE));
    }
    List<AVariable> memVars = domOrException.getMemberVars();
    for (AVariable memVar : memVars) {
      try {
        published.add(MemberVariableInterface.of(memVar));
      } catch (AvariableNotResolvableException e) {
        addToInvocation(invocations, e.getProblem());
      }
      // if complex type use that type
      if (!memVar.isJavaBaseType()) {
        try {
          addToInvocation(invocations, TypeInterface.of(memVar));
        } catch (AvariableNotResolvableException e) {
          addToInvocation(invocations, e.getProblem());
        }
      }
    }

    // if dom, check for services
    if (domOrException instanceof DOM) {
      DOM dom = (DOM) domOrException;
      Map<String, List<Operation>> serviceOperationMap = dom.getServiceNameToOperationMap();
      for (List<Operation> operations : serviceOperationMap.values()) {
        if (operations.size() > 0) {
          addToInvocation(invocations, NoInactiveMethodsInHierarchy.get());
          for (Operation operation : operations) {
            OperationInterface opIf;
            try {
              opIf = OperationInterface.of(myType, operation);
              published.add(opIf);
              if (operation instanceof WorkflowCallInService) {
                WorkflowCallInService wfCall = (WorkflowCallInService) operation;
                OperationInterface wfOp =
                    OperationInterface.of(wfCall.getWf().getOriginalSimpleName(), OperationType.WORKFLOW,
                                          ImplementationType.CONCRETE, opIf.getInput(), opIf.getOutput(),
                                          opIf.getExceptions());
                addToInvocation(invocations, InterfaceEmployment.of(TypeInterface.of(wfCall.getWf()), wfOp));
              }
            } catch (AvariableNotResolvableException e) {
              //ok, nun findet man die nicht auflösbaren variablen unten.
            }
            for (AVariable variable : operation.getInputVars()) {
              if (!variable.isJavaBaseType()) {
                addToInvocation(invocations, extractTypeInterface(variable, TypeOfUsage.INPUT, null));
              }
            }
            for (AVariable variable : operation.getOutputVars()) {
              if (!variable.isJavaBaseType()) {
                addToInvocation(invocations, extractTypeInterface(variable, TypeOfUsage.OUTPUT, null));
              }
            }
            for (AVariable variable : operation.getThrownExceptions()) {
              if (variable.isPrototype()) {
                //FIXME TypeOfUsage ist nicht korrekt, müsste EXCEPTION sein, gibts aber nicht
                addToInvocation(invocations, extractTypeInterface(variable, TypeOfUsage.OUTPUT, null));
              } else {
                addToInvocation(invocations, TypeInterface.of(variable, XMOMType.EXCEPTION));
              }
            }
          }
        }
      }
      if (dom.hasSuperClassGenerationObject() && !dom.isAbstract()) {
        addToInvocation(invocations, NoAbstractMethodsInHierarchy.get());
        addToInvocation(invocations, TypeInterface.of(dom.getSuperClassGenerationObject().getOriginalFqName(), domOrException instanceof DOM ? XMOMType.DATATYPE : XMOMType.EXCEPTION));
      }
    }
    
    //values nehmen, nicht keys, weil die keys evtl nicht die richtigen typesOfUsage haben, siehe addToInvocation
    return Pair.of(published, (Set<DeploymentItemInterface>) new HashSet<DeploymentItemInterface>(invocations.values()));
  }

  private static void addToInvocation(Map<DeploymentItemInterface, DeploymentItemInterface> invocations, DeploymentItemInterface t) {
    if (t instanceof TypeInterface) {
      DeploymentItemInterface old = invocations.put(t, t); //ersetzt evtl nur value!
      if (old != null) {
        Set<TypeOfUsage> oldSet = ((TypeInterface) old).typesOfUsage;
        if (oldSet != null) {
          TypeInterface ti = (TypeInterface) t;
          if (ti.typesOfUsage == null) {
            ti.typesOfUsage = new HashSet<TypeOfUsage>(oldSet);
          } else {
            ti.typesOfUsage.addAll(((TypeInterface) old).typesOfUsage);
          }
        }
      }
    } else {
      invocations.put(t, t);
    }
  }  

  private static Collection<? extends DeploymentItemInterface> extractFunctionInvocation(StepFunction step) {
    List<DeploymentItemInterface> result = new ArrayList<DeploymentItemInterface>();
    ServiceIdentification sid;
    try {
      sid = step.getParentScope().identifyService(step.getServiceId());
    } catch (XPRC_InvalidServiceIdException e) {
      result.add(UnresolvableInterface.get(TypeOfUsage.SERVICE_REFERENCE, step.getServiceId(), step.getXmlId()));
      return result;
    }
    if (sid.service.isPrototype()) {
      result.add(new UnresolvableInterface.PrototypeElement(TypeOfUsage.SERVICE_REFERENCE, step.getXmlId()));
    } else {
      //dass das ein workflow ist, wird in deploymentitemstateimpl benutzt, um alle sub-wf aufrufe zu finden
      //TODO nachteil ist hier, dass eigtl das xml auch gültig bleiben würde, wenn man den wf löscht und eine servicegroup anlegt, die in einem DT lebt, der genauso heisst.
      //deshalb wäre "UNKNOWN" als type dann resistenter.
      DeploymentItemInterface operation = OperationInterface.of(step, sid);
      if (operation instanceof UnresolvableInterface) {
        result.add(operation);
      } else {
        result
            .add(InterfaceEmployment.of(TypeInterface
                                            .of(sid.service.getOriginalFqName(),
                                                sid.service.isDOMRef() ? XMOMType.DATATYPE : XMOMType.WORKFLOW),
                                        operation));
        // The following lines can be activated to implement a check on used objects that go beyond the simple interface matching check above
        // see also ActiveFlagCheckedOperationInterface.java
//        result
//            .add(InterfaceEmployment.of(TypeInterface
//                                            .of(sid.service.getOriginalFqName(),
//                                                sid.service.isDOMRef() ? XMOMType.DATATYPE : XMOMType.WORKFLOW),
//                                        ActiveFlagCheckedOperationInterface.of(step, sid)));
      }
    }
    return result;
  }


  private static Set<DeploymentItemInterface> extractModelledExpressionInvocations(List<ModelledExpression> expressions, Integer xmlId, boolean mustHaveSourceExpression) {
    if (expressions != null && expressions.size() > 0) {
      Set<DeploymentItemInterface> invocations = new HashSet<DeploymentItemInterface>();
      for (ModelledExpression me : expressions) {
        if (me != null && 
            !me.hasPathMapTarget() &&
            !me.hasPathMapSource()) {
          invocations.addAll(getInvocations(me, mustHaveSourceExpression, xmlId));
        }
      }
      return invocations;
    } else {
      return Collections.emptySet();
    }
  }

  private static Set<DeploymentItemInterface> getChoiceClasses(StepChoice step) {
    Set<DeploymentItemInterface> invocations = new HashSet<DeploymentItemInterface>();
    if (step.isBaseSubclassChoice()) {
      SupertypeInterface supertype = SupertypeInterface.of(step.getBaseDomForSubClassChoice().getOriginalFqName());
      for (String fqName : step.getComplexCaseNames()) {
        if (fqName.equals(supertype.getName())) {
          invocations.add(TypeInterface.of(fqName));
        } else {
          invocations.add(InterfaceEmployment.of(TypeInterface.of(fqName), supertype));
        }
      }
    } else {
      invocations.addAll(extractModelledExpressionInvocations(step.getParsedFormulas(), step.getXmlId(), false));
    }
    return invocations;
  }


  public static Set<DeploymentItemInterface> getInvocations(ModelledExpression me, boolean mustHaveSourceExpression, Integer xmlId) {
    Set<DeploymentItemInterface> invocations = new HashSet<DeploymentItemInterface>();
    AccessChainBuildingVisitor acbv_target = new AccessChainBuildingVisitor(xmlId);
    try {
      me.visitTargetExpression(acbv_target);
    } catch (UnresolvableExpression e) {
      if (e instanceof VariableIdentificationException) {
        switch (((VariableIdentificationException)e).type) {
          case MISSING_VAR_ID :
            invocations.add(new UnresolvableInterface.MissingVarId(TypeOfUsage.MODELLED_EXPRESSION, xmlId));
            break;
          case PROTOTYPE_ELEMENT :
            invocations.add(new UnresolvableInterface.PrototypeElement(TypeOfUsage.MODELLED_EXPRESSION, xmlId));
            break;
          default :
            invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "", xmlId));
            break;
        }
      } else if (e.getCause() instanceof XPRC_InvalidVariableIdException) {
        invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "", xmlId));
      } else if (e.getCause() instanceof XPRC_InvalidVariableMemberNameException) {
        invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, ((XPRC_InvalidVariableMemberNameException)e.getCause()).getMemberName(), xmlId));
      } else {
        invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "UNKNOWN", xmlId));
      }
    }
    invocations.addAll(acbv_target.dynamicTypeAccess);
    invocations.addAll(acbv_target.accessChainDependencies);
    AccessChain targetChain = acbv_target.getRootChain();
    if (mustHaveSourceExpression) {
      if (me.getFoundAssign() != null) {
        AccessChainBuildingVisitor acbv_source = new AccessChainBuildingVisitor(xmlId);
        try {
          me.visitSourceExpression(acbv_source);
        } catch (UnresolvableExpression e) {
          if (e instanceof VariableIdentificationException) {
            switch (((VariableIdentificationException) e).type) {
              case MISSING_VAR_ID :
                invocations.add(new UnresolvableInterface.MissingVarId(TypeOfUsage.MODELLED_EXPRESSION, xmlId));
                break;
              case PROTOTYPE_ELEMENT :
                invocations.add(new UnresolvableInterface.PrototypeElement(TypeOfUsage.MODELLED_EXPRESSION, xmlId));
              default :
                invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "", xmlId));
                break;
            }
          } else if (e.getCause() instanceof XPRC_InvalidVariableIdException) {
            invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "", xmlId));
          } else if (e.getCause() instanceof XPRC_InvalidVariableMemberNameException) {
            invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION,
                                                      ((XPRC_InvalidVariableMemberNameException) e.getCause()).getMemberName(), xmlId));
          } else {
            invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "UNKNOWN", xmlId));
          }
        }
        invocations.addAll(acbv_source.dynamicTypeAccess);
        invocations.addAll(acbv_source.accessChainDependencies);
        AccessChain sourceChain = acbv_source.getRootChain();
        if (sourceChain == null) {
          if (targetChain != null) {
            invocations.add(AccessChain.of(TypeInterface.anyType(), targetChain, xmlId, false));
          } else {
            invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "Mapping is missing target expression", xmlId));
          }
        } else if (targetChain == null) {
          invocations.add(AccessChain.of(TypeInterface.anyType(), sourceChain, xmlId, false));
        } else {
          invocations.add(AccessChain.of(sourceChain, targetChain, xmlId));
        }
      } else {
        //fehlende zuweisung
        invocations.add(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "Mapping is missing Assignment Operator", xmlId));
      }
    } else {
      if (targetChain != null) {
        invocations.add(AccessChain.of(TypeInterface.anyType(), targetChain, xmlId, false));
      }
    }
    return invocations;
  }

  private static Set<DeploymentItemInterface> getInvocations(StepCatch stepCatch) {
    Set<String> exceptionIds = stepCatch.getAllUsedVariableIds();
    Set<DeploymentItemInterface> diis = new HashSet<DeploymentItemInterface>(); 
    for (String exceptionId : exceptionIds) {
      try {
        VariableIdentification varId = stepCatch.getParentScope().identifyVariable(exceptionId);
        diis.add(TypeInterface.of(varId.getVariable(), XMOMType.EXCEPTION));
      } catch (XPRC_InvalidVariableIdException e) {
        diis.add(UnresolvableInterface.get(TypeOfUsage.EMPLOYMENT, exceptionId, stepCatch.getXmlId()));
      }
    }
    return diis;
  }
  
  
  private static Set<DeploymentItemInterface> extractAssignementAccess(StepAssign step) {
    Set<DeploymentItemInterface> diis = new HashSet<DeploymentItemInterface>();
    Set<String> allIds = step.getAllUsedVariableIds();
    for (String id : allIds) {
      diis.addAll(generateVarAccess(step, id, TypeOfUsage.EMPLOYMENT));
    }
    return diis;
  }
  
  
  private static Set<DeploymentItemInterface> generateVarAccess(Step containingStep, String varId, TypeOfUsage typeOfUsage) {
    Set<DeploymentItemInterface> diis = new HashSet<DeploymentItemInterface>();
    try {
      if (varId == null || varId.trim().isEmpty()) {
        diis.add(new UnresolvableInterface.MissingVarId(typeOfUsage, containingStep.getXmlId()));
        return diis;
      }
      VariableIdentification indVar = containingStep.getParentScope().identifyVariable(varId);
      DeploymentItemInterface providingType =  extractTypeInterface(indVar.getVariable(), typeOfUsage, containingStep);
      diis.add(providingType);
      if (providingType instanceof TypeInterface) {
        List<AVariable> childVars = indVar.getVariable().getChildren();
        for (AVariable childVar : childVars) {
          extractConstants((TypeInterface)providingType, containingStep.getXmlId(), childVar, indVar.getVariable().isList(), diis);
        }
      }
    } catch (XPRC_InvalidVariableIdException e) {
      return Collections.singleton(UnresolvableInterface.get(typeOfUsage, varId, containingStep.getXmlId()));
    }
    return diis;
  }
  
  public static void extractConstants(TypeInterface providingType, AVariable variable, boolean parentWasList, Set<DeploymentItemInterface> diis) {
    extractConstants(providingType, null, variable, parentWasList, diis);
  }
  
  public static void extractConstants(TypeInterface providingType, Integer stepId, AVariable variable, boolean parentWasList, Set<DeploymentItemInterface> diis) {
    if (variable.hasValue() && variable.isJavaBaseType() && variable.getJavaTypeEnum() == null) {
      //das ist der fall, dass die membervariable unbekannt ist. beim validate würde es einen fehler geben
      diis.add(UnresolvableInterface.get(TypeOfUsage.EMPLOYMENT, String.valueOf(variable.getVarName()), stepId));
      return;
    }
    if (variable.hasValueOrIsDOM() && variable.getVarName().length() > 0 && !parentWasList) {
      try {
        diis.add(InterfaceEmployment.of(providingType, MemberVariableInterface.of(variable)));
      } catch (AvariableNotResolvableException e) {
        diis.add(e.getProblem());
      }
    }
    
    try {
      TypeInterface subProvider = TypeInterface.of(variable);
      if (variable.getChildren().size() > 0) {
        for (AVariable childVar : variable.getChildren()) {
          extractConstants(subProvider, stepId, childVar, variable.isList(), diis);
        }
      } else {
        // das ist der Fall bei einer konstant vorbelegte Liste von "enum"-Typen
        TypeInterface type = TypeInterface.of(variable);
        if (!type.isJavaBaseType()) {
          diis.add(type);
        }
      }
    } catch (AvariableNotResolvableException e) {
      diis.add(e.getProblem());
    }
  }

  
  static void appendOriginalFqName(StringBuilder appender, AVariable variable)  {
    if (variable.isJavaBaseType()) {
      appender.append(variable.getJavaTypeEnum().getFqName());
    } else {
      appender.append(variable.getOriginalPath()).append('.').append(variable.getOriginalName());
    }
  }
  
  
  static GenerationBase generateInstance(String fqName, XMOMType type, long revision, DeploymentLocation location, Map<DeploymentLocation, GenerationBaseCache> cache) throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH {
    boolean fromDeployed = location == DeploymentLocation.DEPLOYED;
    GenerationBaseCache locationCache = cache.get(location);
    if (locationCache == null) {
      locationCache = new GenerationBaseCache();
      cache.put(location, locationCache);
    }
    switch (type) {
      case WORKFLOW :
        WF wf = WF.getOrCreateInstance(fqName, locationCache, revision);
        wf.parseGeneration(fromDeployed, false, false);
        return wf;
      case DATATYPE :
        DOM dom = DOM.getOrCreateInstance(fqName, locationCache, revision);
        dom.parseGeneration(fromDeployed, false, false);
        return dom;
      case EXCEPTION :
        ExceptionGeneration exGen = ExceptionGeneration.getOrCreateInstance(fqName, locationCache, revision);
        exGen.parseGeneration(fromDeployed, false, false);
        return exGen;
      default :
        throw new IllegalArgumentException("Invalid xmomType for GenerationBase creation " + type);
    }
  }
  
  
  private static enum DeploymentState {
    DEPLOYED, INCOMPLETE, NOT_DEPLOYED, WEIRD;
  }
  
  
  /**
   * xml ~ deployedXMLExists
   * wb ~ workflowDatabaseExists
   * cl ~ classloaderExists
   */
  private static enum ObservedDeploymentStates {
    xml_wb_cl(true, true, true, DeploymentState.DEPLOYED), 
    xml_wb__(true, true, false, DeploymentState.INCOMPLETE),
    xml___cl(true, false, true, DeploymentState.NOT_DEPLOYED), // TODO mark as invalid?
    xml____(true, false, false, DeploymentState.NOT_DEPLOYED), // TODO mark as invalid?
    __wb_cl(false, true, true, DeploymentState.WEIRD), 
    __wb__(false, true, false, DeploymentState.INCOMPLETE),
    ____cl(false, false, true, DeploymentState.WEIRD),
    _____(false, false, false, DeploymentState.NOT_DEPLOYED)
    ;
    
    private final boolean deployedXMLExists;
    private final boolean workflowDatabaseEntryExists;
    private final boolean classloaderExists;
    private final DeploymentState deploymentState;
    
    private ObservedDeploymentStates(boolean deployedXMLExists,
                                     boolean workflowDatabaseEntryExists,
                                     boolean classloaderExists,
                                     DeploymentState deploymentState) {
      this.deployedXMLExists = deployedXMLExists;
      this.workflowDatabaseEntryExists = workflowDatabaseEntryExists;
      this.classloaderExists = classloaderExists;
      this.deploymentState = deploymentState;
    }
    
    
    public DeploymentState getDeploymentState() {
      return deploymentState;
    }
    
    
    public static ObservedDeploymentStates getByStates(boolean deployedXMLExists,
                                                       boolean workflowDatabaseEntryExists,
                                                       boolean classloaderExists) {
      for (ObservedDeploymentStates ods : values()) {
        if (ods.deployedXMLExists == deployedXMLExists &&
            ods.workflowDatabaseEntryExists == workflowDatabaseEntryExists &&
            ods.classloaderExists == classloaderExists) {
          if (ods.getDeploymentState() == DeploymentState.WEIRD) {
            logger.debug("ObservedDeploymentStates: " + ods);
          }
          return ods;
        }
      }
      throw new IllegalArgumentException();
    }
    
  }
 
  
  public static DeploymentState deriveDeploymentState(String fqName, XMOMType type, long revision, File deployedFile) {
    try {
      ClassLoaderType classLoaderType;
      if (type == XMOMType.WORKFLOW) {
        classLoaderType = ClassLoaderType.WF;
      } else if (type == XMOMType.EXCEPTION) {
        classLoaderType = ClassLoaderType.Exception;
      } else {
        classLoaderType = ClassLoaderType.MDM;
      }
      return ObservedDeploymentStates
          .getByStates(deployedFile.exists(),
                       XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase()
                           .isRegisteredByFQ(type, fqName, revision),
                       GenerationBase.isReservedServerObjectByFqOriginalName(fqName)
                           || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                               .getClassLoaderByType(classLoaderType, GenerationBase.transformNameForJava(fqName), revision) != null)
          .getDeploymentState();
    } catch (XPRC_InvalidPackageNameException e) {
      return DeploymentState.WEIRD;
    }
  }
  
  
  static Optional<Operation> resolveMethod(DOM serviceGroup, String methodName) {
    for (List<Operation> serviceEntries : serviceGroup.getServiceNameToOperationMap().values()) {
      for (Operation operation : serviceEntries) {
        if (operation.getName().equals(methodName)) {
          return Optional.of(operation);
        }
      }
    }
    return Optional.empty();
  }
  
  private static class AccessChainBuildingVisitor extends EmptyVisitor {
    
    private static class LocalChainContext {
      /*
       * lebenszyklus:
       * initial: rootType=null, chain=null
       * kontext startet: rootType=typ von kontext-root, chain=null
       * chain-element startet: rootType=null, chain=chain von ehemaligem rootType + part
       * ende: rootType=null, chain!=null
       */
      private TypeInterface rootType;
      private AccessChain chain;
      public boolean currentChainElementHasIndexDef = false;
    }
    
    private Stack<Object> contextKey = new Stack<Object>();
    private Set<TypeInterface> dynamicTypeAccess = new HashSet<TypeInterface>();
    
    
    //falls innerhalb der funktion ein roottype oder eine chain verwendet wurde, muss diese nun beseitigt werden, damit sie bei der parent-funktion (o.ä,) nicht interferiert
    //beispiel: cast(%0%.a[length(%1%.b)], type) -> %1% chain muss abgespalten werden, sobald der kontext von length() endet.
    //wenn funktionsparameter-kontext endet muss das genauso abgespalten werden
    //beispiel: cast(%0%.a[sum(%1%.b, %2%.c)], type)
    //operators(expr2args genauso: cast(%0%.a[length(%1%.b) + length(%2%.c)], type)
    private Set<DeploymentItemInterface> accessChainDependencies = new HashSet<DeploymentItemInterface>();
    private Map<Object, LocalChainContext> context = new HashMap<Object, LocalChainContext>();
    private final Integer xmlId;
    private Stack<List<TypeInterface>> instanceFunctionInputs = new Stack<List<TypeInterface>>();
    private boolean activeChainInIndexDef = false;
    
    public AccessChainBuildingVisitor(Integer xmlId) {
      context.put(null, new LocalChainContext());
      this.xmlId = xmlId;
    }
    
    public AccessChain getRootChain() {
      return createOrGetChain(context.get(null));
    }

    public void functionEnds(FunctionExpression fe) {
      contextKey.pop();
      LocalChainContext localContext = context.remove(fe);
      if (fe instanceof DynamicResultTypExpression) {
        FunctionExpression.DynamicResultTypExpression drte = (DynamicResultTypExpression) fe;
        TypeInterface dynamicType = null;
        try {
          dynamicType = TypeInterface.of(drte.getDynamicTypeName(), drte.getDynamicTypeType());
        } catch (Exception e) {
          //ignore
          dynamicType = TypeInterface.of("invalid dynamic type", XMOMType.DATATYPE);
        }
        dynamicTypeAccess.add(dynamicType);

        //solltype auf parent context vererben (parent ist bereits current)
        getCurrentChainContext().rootType = dynamicType;

        if (fe instanceof CastExpression) {
          saveTypedChain(localContext, dynamicType, true);
        }
      } else {
        
        LocalChainContext parentContext = getCurrentChainContext();
        if (parentContext.currentChainElementHasIndexDef) {
          parentContext.currentChainElementHasIndexDef = false;
          //roottype so lassen
          //TODO überprüfen, dass funktion int-rückgabe typ kompatibel ist, sonst wird z.b. %0%[appendlist(%1%, "bla")] nicht richtig validiert
        } else {
          //parentcontext kann kein chain/oder root haben
          parentContext.chain = null;
          parentContext.rootType = TypeInterface.of(fe.getResultType(), false);
          FunctionParameterTypeDefinition parameterTypeDef = fe.getFunction().getParameterTypeDef();
          if (parameterTypeDef.numberOfParas() > 0 || parameterTypeDef.numberOfParas() < 0
              || fe.getFunction().getParameterTypeDef().numberOfOptionalParas() > 0) {
            saveTypedChain(localContext, TypeInterface.of(fe.getParameterTypeDef(fe.getSubExpressions().size() - 1), false));
          }
        }
      }
    }



    @Override
    public void expression2ArgsStarts(Expression2Args expression) {
      contextKey.push(expression);
      context.put(expression, new LocalChainContext());
    }

    @Override
    public void operator(Operator operator) {
      //mit allem primitiven kompatibel (~String)
      saveTypedChain(getCurrentChainContext(), TypeInterface.of(AVariable.PrimitiveType.STRING, false));
    }

    private void saveTypedChain(LocalChainContext context, TypeInterface targetTypeOfChain, boolean isCausedByCast) {
      AccessChain chain = createOrGetChain(context);
      if (chain != null) {
        accessChainDependencies.add(AccessChain.of(targetTypeOfChain, chain, xmlId, isCausedByCast));
        context.chain = null;
        context.rootType = null;
      }
    }
    
    private void saveTypedChain(LocalChainContext context, TypeInterface targetTypeOfChain) {
      saveTypedChain(context, targetTypeOfChain, false);
    }

    @Override
    public void expression2ArgsEnds(Expression2Args expression) {
      //mit allem primitiven kompatibel (~String)
      saveTypedChain(getCurrentChainContext(), TypeInterface.of(AVariable.PrimitiveType.STRING, false));
      contextKey.pop();
    }

    @Override
    public void functionSubExpressionEnds(FunctionExpression fe, int parameterIndex) {
      if (fe.getFunction().getName().equals(Functions.APPEND_TO_LIST_FUNCTION_NAME) && parameterIndex == 0) {
        //typ des ersten parameters vererbt sich auf rückgabe typ von funktion - deshalb diesen typ in parent-chain setzen
        LocalChainContext parentContext = getParentChainContext();
        LocalChainContext localContext = getCurrentChainContext();
        AccessChain chain = createOrGetChain(localContext);
        parentContext.chain = chain; //hier kann bisher kein roottyp/chain existieren
        localContext.chain = null; //nullen für den nächsten parameter
      } else if (parameterIndex < fe.getSubExpressions().size() - 1) { //letzten parameter hier nicht bearbeiten, sondern in functionEnds()        
        saveTypedChain(context.get(fe), TypeInterface.of(fe.getParameterTypeDef(parameterIndex), false));
      }
    }
    
    public void functionStarts(FunctionExpression fe) {
      contextKey.push(fe);
      context.put(fe, new LocalChainContext());
    }

    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      contextKey.push(vifi);
      context.put(vifi, new LocalChainContext());
      instanceFunctionInputs.push(new ArrayList<TypeInterface>());
    }

    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      List<TypeInterface> inputs = instanceFunctionInputs.pop();
      contextKey.pop();
      OperationInterface operation = OperationInterface.of(vifi.getName(), OperationType.INSTANCE_SERVICE, null, inputs, (List<TypeInterface>)null, (List<TypeInterface>)null);
      createOrAppendChain(InstanceMethodAccessPart.of(operation));
    }

    public void instanceFunctionSubExpressionEnds(Expression fe, int parameterCnt) {
      if (!(fe instanceof LiteralExpression)) {
        LocalChainContext context = getCurrentChainContext();
        AccessChain chain = createOrGetChain(context);
        if (chain == null) {
          instanceFunctionInputs.peek().add(TypeInterface.anyType());
        } else if (chain.getRootPart() == null) { // Variable without access part
          instanceFunctionInputs.peek().add(chain.getRootType());
        } else {
          instanceFunctionInputs.peek().add(chain);
        }
        context.chain = null;
        context.rootType = null;
      } else {
        instanceFunctionInputs.peek().add(TypeInterface.anyType());
      }
    }

    private LocalChainContext getParentChainContext() {
      Object currentExpr;
      if (contextKey.empty()) {
        throw new RuntimeException();
      } else if (contextKey.size() == 1) {
        currentExpr = null;
      } else {
        currentExpr = contextKey.get(contextKey.size() - 2);
      }
      return context.get(currentExpr);
    }
    
    private LocalChainContext getCurrentChainContext() {
      Object currentExpr;
      if (contextKey.empty()) {
        currentExpr = null;
      } else {
        currentExpr = contextKey.peek();
      }
      return context.get(currentExpr);
    }

    public void variableStarts(Variable variable) {
      LocalChainContext parentContext = getCurrentChainContext();
      LocalChainContext contextToUse = parentContext;
      if (parentContext.currentChainElementHasIndexDef) {
        parentContext.currentChainElementHasIndexDef = false;
        contextKey.push(variable);
        contextToUse = new LocalChainContext();
        context.put(variable, contextToUse);
        activeChainInIndexDef = true;
      }
      try {
        variable.validate();
        contextToUse.rootType = TypeInterface.of(variable);
      } catch (XPRC_InvalidVariableIdException e) {
        try {
          if (variable.isPrototype()) {
            throw new VariableIdentificationException(ProblemType.PROTOTYPE_ELEMENT, e);
          } else {
            throw new VariableIdentificationException(ProblemType.MISSING_VAR_ID, e);
          }
        } catch (XPRC_InvalidVariableIdException ee) {
          throw new UnresolvableExpression(ee);
        }
      } catch (XPRC_InvalidVariableMemberNameException|InvalidInvocationException e) {
        try {
          contextToUse.rootType = TypeInterface.of(variable.getRootInfo().getTypeInfo(false), variable.getRootInfo().getTypeInfo(false).isList());
        } catch (XPRC_InvalidVariableIdException e1) {
          throw new RuntimeException(e);
        }
      } catch (RuntimeException e) {
        try {
          if (variable.isPrototype()) {
            throw new VariableIdentificationException(ProblemType.PROTOTYPE_ELEMENT, e);
          } else {
            throw new UnresolvableExpression(e);
          }
        } catch (XPRC_InvalidVariableIdException ee) {
          throw new UnresolvableExpression(ee);
        }
      }
      contextToUse.currentChainElementHasIndexDef = variable.getIndexDef() != null;
      if (variable.getParts() == null || variable.getParts().size() <= 0) {
        //kontext abschliessen
        TypeInterface rootType = contextToUse.rootType;
        contextToUse.rootType = null;
        if (rootType.isList() && variable.getIndexDef() != null) {
          //listenindex zugriff beim root
          rootType = TypeInterface.of(rootType, false);
        }
        contextToUse.chain = AccessChain.of(rootType, (AccessPart)null);
      }
    }

    @Override
    public void allPartsOfVariableFinished(Variable variable) {
      //TODO es fehlt eine analoge behandlung noch für das enden der chain hinter einem funktionsaufruf (Beispiel: f(x,y).a.b["0"].c)
      if (!contextKey.isEmpty() &&
          contextKey.peek() == variable) {
        if (activeChainInIndexDef) {
          saveTypedChain(getCurrentChainContext(), TypeInterface.of(AVariable.PrimitiveType.LONG, false));
        }
        contextKey.pop();
      }
    }

    @Override
    public void indexDefEnds() {
      activeChainInIndexDef = false;
    }

    public void variablePartStarts(VariableAccessPart part) {
      if (part.isMemberVariableAccess()) {
        if (part.getName() == null) {
          throw new IllegalArgumentException("part.getName() == null in " + part);
        }
        MemberVarAccessPart accessPart = MemberVarAccessPart.of(MemberVariableInterface.of(part.getName()), part.getIndexDef() != null);
        createOrAppendChain(accessPart);
        getCurrentChainContext().currentChainElementHasIndexDef = part.getIndexDef() != null;
      } // else list access?
    }

    private void createOrAppendChain(AccessPart part) {
      LocalChainContext localContext = getCurrentChainContext();
      if (localContext.rootType == null) {
        if (localContext.chain == null) {
          throw new RuntimeException(); //sollte vorher bereits einen root gesetzt haben
        }
        AccessChain chain = localContext.chain;
        chain.append(part);
      } else {
        localContext.chain = AccessChain.of(localContext.rootType, part);
        localContext.rootType = null;
      }
    }

    private AccessChain createOrGetChain(LocalChainContext localContext) {
      if (localContext.rootType == null) {
        if (localContext.chain == null) {
          return null;
        } else {
          return localContext.chain;
        }
      } else {
        localContext.chain = AccessChain.of(localContext.rootType, (AccessPart) null);
        localContext.rootType = null;
        return localContext.chain;
      }
    }
    
  }
  
  
  private static class UnresolvableExpression extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public UnresolvableExpression(Throwable cause) {
      super(cause);
    }
  }
  
  
  private static class VariableIdentificationException extends UnresolvableExpression {
    
    private static final long serialVersionUID = 1L;

    ProblemType type;
    
    public VariableIdentificationException(ProblemType type, Throwable cause) {
      super(cause);
      this.type = type;
    }
  }

}
