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

package com.gip.xyna.xact.filter.session.repair;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.ExpressionUtils;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationTypeInstance;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction.RemoteDespatchingParameter;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.ServiceIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;

import xmcp.processmodeller.datatypes.RepairEntry;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.Storable;



/*package*/ class StepFunctionRepair {

  private static final RemoteDestinationManagement rdMgmt =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();


  //GenerationBaseObject is only needed to check for Query
  //since query Dynamic TypeCast behaves differently
  public void registerFunctions(Consumer<BiFunction<StepFunction, Boolean, List<RepairEntry>>> addFunc, GenerationBaseObject obj) {
    addFunc.accept(this::nullInvalidInputIds);
    addFunc.accept(this::convertStepFunctionToPrototype);
    addFunc.accept(this::updateStepFunctionSignature);
    addFunc.accept(wrapGBOForConvertFunctionInputToPrototype(obj));
    addFunc.accept(wrapGBOForConvertFunctionOutputToPrototype(obj));
    addFunc.accept(wrapGBOForRemoveTypeCastForRemovedBaseType(obj));
    addFunc.accept(wrapGBOForQueryRepair(obj));
    addFunc.accept(this::removeTypeCast);
    addFunc.accept(this::removeConstantFromFunctionInput);
    addFunc.accept(this::repairInvalidRemoteDestinations);
    addFunc.accept(wrapGBOForrepairIdsForPrototypeOutput(obj));
    addFunc.accept(this::removeInvalidInputConnections);
    addFunc.accept(this::removeUnusedQueryInputs);
  }



  private BiFunction<StepFunction, Boolean, List<RepairEntry>> wrapGBOForConvertFunctionInputToPrototype(GenerationBaseObject gbo) {
    return (sf, apply) -> convertFunctionInputToPrototype(sf, gbo, apply);
  }


  private BiFunction<StepFunction, Boolean, List<RepairEntry>> wrapGBOForConvertFunctionOutputToPrototype(GenerationBaseObject gbo) {
    return (sf, apply) -> convertFunctionOutputToPrototype(sf, gbo, apply);
  }


  private BiFunction<StepFunction, Boolean, List<RepairEntry>> wrapGBOForRemoveTypeCastForRemovedBaseType(GenerationBaseObject gbo) {
    return (sf, apply) -> removeTypeCastForRemovedBaseType(sf, gbo, apply);
  }


  private BiFunction<StepFunction, Boolean, List<RepairEntry>> wrapGBOForQueryRepair(GenerationBaseObject gbo) {
    return (sf, apply) -> repairQuery(sf, gbo, apply);
  }


  private BiFunction<StepFunction, Boolean, List<RepairEntry>> wrapGBOForrepairIdsForPrototypeOutput(GenerationBaseObject gbo) {
    return (sf, apply) -> repairIdsForPrototypeOutput(sf, gbo, apply);
  }


  private List<RepairEntry> convertStepFunctionToPrototype(StepFunction step, boolean apply) {
    if (step.isPrototype()) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();
    boolean repairRequired = functionNeedsConversionToPrototype(step);


    if (repairRequired) {
      RepairEntry entry = new RepairEntry();
      entry.setDescription("Service Operation not found.");
      entry.setId(ObjectId.createStepId(step).getObjectId());
      entry.setType("Step conversion to Prototype");
      entry.setLocation(WorkflowRepair.createLocation(step));
      result.add(entry);

      if (apply) {
        applyStepFunctionConversionToPrototype(step);
      }
    }

    return result;
  }
  
  
  private List<RepairEntry> nullInvalidInputIds(StepFunction step, boolean apply){
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    String[] ids = step.getInputVarIds();
    List<Integer> indicesToNull = new ArrayList<Integer>();
    for(int i=0; i<ids.length; i++) {
      String id = ids[i];
      if(id == null || id.length() == 0) {
        continue;
      }
      if(!canIdentifyId(step, id)) {
        indicesToNull.add(i);
      }
    }

    if (indicesToNull.size() > 0) {
      RepairEntry entry = new RepairEntry();
      entry.setDescription("invalid input variable id");
      entry.setId(step.getStepId());
      entry.setType("remove invalid id");
      result.add(entry);
    }
    
    if(apply) {
      for(int i : indicesToNull) {
        step.getInputVarIds()[i] = null;
      }
    }
    
    return result;
  }


  private boolean canIdentifyId(StepFunction step, String id) {
    try {
      step.getParentScope().identifyVariable(id);
    } catch (XPRC_InvalidVariableIdException e) {
      return false;
    }
    return true;
  }


  private List<RepairEntry> updateStepFunctionSignature(StepFunction step, boolean apply) {

    if (!functionNeedsSignatureUpdate(step)) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();
    RepairEntry entry = new RepairEntry();
    String location = WorkflowRepair.createLocation(step);
    entry.setDescription("Signature of Step does not match called operation.");
    entry.setId(ObjectId.createStepId(step).getObjectId());
    entry.setType("Step signature update");
    entry.setLocation(location);
    result.add(entry);

    if (apply) {
      applyUpdateStepFunctionSignature(step);
    }

    return result;
  }


  //this repair is done after signature updates. => but changes are only applied, if apply  is true
  //unlike other steps, we have to check inputs of operation
  //only creates a RepairEntry, if a Connection is removed
  private List<RepairEntry> convertFunctionInputToPrototype(StepFunction step, GenerationBaseObject gbo, boolean apply) {
    List<AVariable> avars = Utils.getServiceInputVars(step);
    Function<Integer, String> idGenerator = WorkflowRepair.createVariableIDGenerator(step, VarUsageType.input);
    ConvertStepFunctionVarsToPrototypeData data = new ConvertStepFunctionVarsToPrototypeData();
    boolean hasOrderInputSource = step.getOrderInputSourceRef() != null && step.getOrderInputSourceRef().length() > 0;

    data.setIdGenerator(idGenerator);
    data.setStep(step);
    data.setVariables(avars);
    data.setIsInput(true);

    if (hasOrderInputSource) {
      data.setVariableIds(new String[avars.size()]);
      data.setCasts(new String[avars.size()]);
    } else {
      data.setVariableIds(step.getInputVarIds());
      data.setCasts(step.getInputVarCastToType());
    }

    return convertFunctionVarsToPrototype(data, gbo, apply);
  }


  //this repair is done after signature updates. => but changes are only applied, if apply  is true
  //converts StepFunction Output into Prototype, if there is no cast
  private List<RepairEntry> convertFunctionOutputToPrototype(StepFunction step, GenerationBaseObject gbo, boolean apply) {
    List<AVariable> avars = Utils.getServiceOutputVariables(step);
    Function<Integer, String> idGenerator = WorkflowRepair.createVariableIDGenerator(step, VarUsageType.output);
    ConvertStepFunctionVarsToPrototypeData data = new ConvertStepFunctionVarsToPrototypeData();
    data.setCasts(step.getReceiveVarCastToType());
    data.setIdGenerator(idGenerator);
    data.setStep(step);
    data.setVariables(avars);
    data.setVariableIds(step.getOutputVarIds());
    data.setIsInput(false);
    
    return convertFunctionVarsToPrototype(data, gbo, apply);
  }


  private List<RepairEntry> convertFunctionVarsToPrototype(ConvertStepFunctionVarsToPrototypeData data, GenerationBaseObject gbo,
                                                           boolean apply) {
    StepFunction step = data.getStep();

    //if apply is false, we might need to update Signature
    if (functionNeedsSignatureUpdate(step)) {
      return Collections.emptyList();
    }

    String location = WorkflowRepair.createLocation(step);
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<AVariable> avars = data.getVariables();
    Function<Integer, String> idGenerator = data.getIdGenerator();
    String[] casts = data.getCasts();
    String[] varIds = data.getVariableIds();

    for (int i = 0; i < avars.size(); i++) {
      AVariable avar = avars.get(i);
      boolean conversionRequired = XMOMRepair.variableHasToBeConverted(avar);
      boolean cast = casts[i] != null && casts[i].length() != 0;
      boolean variableSet = varIds[i] != null && varIds[i].length() > 0;

      if (cast) {
        continue;
      }

      if (conversionRequired && variableSet) {
        RepairEntry entry = XMOMRepair.createVarConvertedRepairEntry(location, avar, idGenerator.apply(i));
        result.add(entry);

        if (apply) {
          varIds[i] = null;
          if (data.getIsInput()) { //remove constant from input
            step.getInputConnections().getConstantConnected()[i] = false; 
          }
        }
      }
    }

    return result;
  }


  //this repair is done after signature updates. => but changes are only applied, if apply  is true
  private List<RepairEntry> removeTypeCastForRemovedBaseType(StepFunction step, GenerationBaseObject gbo, boolean apply) {
    BiFunction<RemoveTypeCastData, Boolean, List<RepairEntry>> f = (RemoveTypeCastData rtcd, Boolean a) -> {
      return removeTypeCastForRemovedBaseType(rtcd, gbo, apply);
    };
    return executeCastRemoval(step, apply, f);
  }


  private boolean variableDoesNotInheritFromStorable(AVariable avar) {
    if (!(avar.getDomOrExceptionObject() instanceof DOM)) {
      return true; //not a datatype
    }

    DOM dom = (DOM) avar.getDomOrExceptionObject();
    if (dom == null) {
      return true;
    }

    if (dom.isStorableEquivalent()) {
      return false;
    }

    return !dom.isInheritedFromStorable();
  }


  private void removeCastFromQuery(StepFunction step, GenerationBaseObject gbo, AVariable avar, AVariable serviceVar) {
    DomOrExceptionGenerationBase storableDoe = serviceVar.getDomOrExceptionObject();
    avar.replaceDomOrException(storableDoe, storableDoe.getLabel());
    try {
      GBSubObject sub = gbo.getObject(ObjectId.createStepId(step).getObjectId());
      SelectionMask mask = QueryUtils.getSelectionMask(sub);
      mask.setRootType(Storable.class.getName());

      AVariable constDataVar = AVariable.createFromXo(mask, gbo.getWFStep().getCreator(), false);
      constDataVar.setId(step.getCreator().getNextXmlId().toString());

      if (constDataVar.getDomOrExceptionObject() != null) {
        constDataVar.setLabel(constDataVar.getDomOrExceptionObject().getLabel());
      }

      constDataVar.setVarName("const_" + constDataVar.getOriginalName() + constDataVar.getId());
      step.getParentWFObject().getWfAsStep().getChildStep().addVar(constDataVar);


      int index = 0;
      for (int j = 0; j < step.getInputVarIds().length; j++) {
        String id = step.getInputVarIds()[j];
        AVariable a = step.getParentScope().identifyVariable(id).getVariable();
        if (SelectionMask.class.getName().equals(a.getFQClassName())) {
          index = j;
          break;
        }
      }

      step.getInputVarIds()[index] = constDataVar.getId();


    } catch (UnknownObjectIdException | MissingObjectException | XynaException e) {
      //could not remove cast
    }
  }


  //Query needs special treatment, because even though it has a type cast, that is not reflected in casts.
  private List<RepairEntry> repairQuery(StepFunction step, GenerationBaseObject gbo, boolean apply) {

    if (!step.isQueryStorable()) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<AVariable> vars = step.getOutputVars();
    for (int i = 0; i < vars.size(); i++) {
      AVariable avar = vars.get(i);
      if (XMOMRepair.variableHasToBeConverted(avar) || variableDoesNotInheritFromStorable(avar)) {
        RepairEntry entry = new RepairEntry();
        entry.setDescription("Query has invalid variable: " + avar.getLabel());
        entry.setId(WorkflowRepair.createVariableIDGenerator(step, VarUsageType.output).apply(i));
        entry.setLocation(WorkflowRepair.createLocation(step));
        entry.setType("Variable replacement by storable");
        result.add(entry);

        if (apply) {
          List<AVariable> serviceOutputVars = Utils.getServiceOutputVariables(step);
          AVariable serviceOutputVar = serviceOutputVars.get(i);
          removeCastFromQuery(step, gbo, avar, serviceOutputVar);
        }
      }
    }

    return result;
  }


  private List<RepairEntry> removeTypeCast(StepFunction step, boolean apply) {
    return executeCastRemoval(step, apply, this::removeTypeCast);
  }


  private List<RepairEntry> removeConstantFromFunctionInput(StepFunction step, boolean apply) {
    String[] ids = step.getInputVarIds();
    InputConnections connections = step.getInputConnections();
    
    List<RepairEntry> result = removeInvalidTypedConstants(step, ids, connections, apply);
    result.addAll(WorkflowRepair.removeConstant(step, ids, connections, apply));
    return result;
  }

  
  //nulls entry in ids, if there is an invalidly typed constant
  private List<RepairEntry> removeInvalidTypedConstants(StepFunction step, String[] ids, InputConnections connections, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    Boolean[] constants = connections.getConstantConnected();
    List<AVariable> serviceInputVars = Utils.getServiceInputVars(step);

    for (int i = 0; i < ids.length; i++) {
      if (constants[i] == null || constants[i] == false) {
        continue;
      }

      AVariable var = null;
      try {
        var = step.getParentScope().identifyVariable(ids[i]).getVariable();
      } catch (Exception e) {
        continue; //should not happen
      }

      if (var == null) {
        continue; //should not happen
      }

      boolean validType = true;
      
      if(serviceInputVars.size() <= i) {
        validType = false; //this input will be removed -> signature does not match
      } else {
        DomOrExceptionGenerationBase serviceDoE = serviceInputVars.get(i).getDomOrExceptionObject();
        DomOrExceptionGenerationBase varDoE = var.getDomOrExceptionObject();
        //serviceDoE == null for base.AnyType
        validType = serviceDoE == null || DomOrExceptionGenerationBase.isSuperClass(serviceDoE, varDoE);       
      }
      


      if (!validType) {
        ids[i] = null; //mark this entry so we do not try to validate the (invalid) constant later

        RepairEntry entry = new RepairEntry();
        entry.setDescription("Constant variable has invalid type.");
        entry.setId(ObjectId.createStepId(step).getObjectId());
        entry.setLocation(WorkflowRepair.createLocation(step));
        entry.setType("Remove constant");

        result.add(entry);

        if (apply) {
          constants[i] = false;
        }
      }
    }
    
    
    return result;
  }


  //happens when remote Destinations become invalid
  // - remote destination was removed
  // - remote destination requires different parameters
  private List<RepairEntry> repairInvalidRemoteDestinations(StepFunction step, boolean apply) {
    if (step.getRemoteDispatchingParameter() == null) {
      return Collections.emptyList();
    }
    List<RepairEntry> result = new ArrayList<RepairEntry>();

    RemoteDespatchingParameter parameter = step.getRemoteDispatchingParameter();
    String remoteDestination = parameter.getRemoteDestination();

    //if the remoteDestination does not exist
    List<RepairEntry> removeMissingDestination = repairMissingRemoteDestination(step, remoteDestination, apply);
    if (removeMissingDestination.size() > 0) {
      return removeMissingDestination;
    }

    return result;
  }
  
  
  private List<RepairEntry> removeUnusedQueryInputs(StepFunction step, boolean apply){
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    
    if(!step.isQueryStorable()) {
      return result;
    }
    
    StepMapping mapping = QueryUtils.findQueryHelperMapping(step);
    List<String> filterConditions = step.getQueryFilterConditions(); //TODO: if null check Mapping.. or always check mapping?

    if (ExpressionUtils.calculateVariableIndicesToRemove(filterConditions, mapping).size() > 0) {
      RepairEntry entry = new RepairEntry();
      entry.setDescription("Unused Input variable");
      entry.setId(ObjectId.createStepId(step).getObjectId());
      entry.setLocation("Query: \"" + step.getLabel() + "\"");
      entry.setType("Input variable removal");
      result.add(entry);

      if (apply) {
        QueryUtils.refreshQueryHelperMappingExpression(step, mapping);
      }
    }

    return result;
  }
  
  
  private List<RepairEntry> removeInvalidInputConnections(StepFunction step, boolean apply){
    
    if(functionNeedsSignatureUpdate(step)) {
      return Collections.emptyList(); //we are changing signature anyway
    }
    
    List<AVariable> serviceVars = Utils.getServiceInputVars(step);
    String[] stepInputVarIds = step.getInputVarIds();
    
    if(serviceVars.size() != stepInputVarIds.length) {
      return Collections.emptyList();
    }
    
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    
    for(int i=0; i<serviceVars.size(); i++) {
      AVariable serviceVar = serviceVars.get(i);
      AVariable stepInputVar = null;
      boolean invalidId = false;
      
      if (stepInputVarIds[i] != null && stepInputVarIds[i].length() > 0) {
        try {
          stepInputVar = step.getParentScope().identifyVariable(stepInputVarIds[i]).getVariable();
        } catch (XPRC_InvalidVariableIdException e) {
          invalidId = true;
        }
      }
      
      if(invalidId || (stepInputVar != null && serviceVar.isList() != stepInputVar.isList())) {
        
        RepairEntry entry = new RepairEntry();
        entry.setDescription("Invalid input Connection for step at input " + i + ((invalidId) ? ". Missing Id: " +  stepInputVarIds[i]: "."));
        entry.setId(step.getStepId());
        entry.setLocation(WorkflowRepair.createLocation(step));
        entry.setType("Remove invalid connection");
        result.add(entry);
        
        
        if(apply) {
          step.getInputVarIds()[i] = null;
        }
      }
      
    }
    
    
    return result;
  }
  
  
  private List<RepairEntry> repairIdsForPrototypeOutput(StepFunction step, GenerationBaseObject gbo, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();

    if (functionNeedsSignatureUpdate(step)) {
      return Collections.emptyList();
    }


    String[] ids = step.getOutputVarIds();
    List<AVariable> vars = step.getOutputVars();

    if (ids == null || vars == null || ids.length != vars.size()) {
      return Collections.emptyList();
    }

    for (int i = 0; i < ids.length; i++) {
      if (ids[i] != null && ids[i].length() > 0 && vars.get(i) == null) {
        RepairEntry entry = new RepairEntry();
        entry.setDescription("Output has Id, but variable does not exist");
        entry.setId(step.getStepId());
        entry.setLocation(WorkflowRepair.createLocation(step));
        entry.setType("Id removal");
        result.add(entry);

        if (apply) {
          ids[i] = null;
        }
      }
    }


    return result;
  }


  private List<RepairEntry> repairMissingRemoteDestination(StepFunction step, String remoteDestination, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    //remoteDestination does not exist
    RemoteDestinationTypeInstance rdti = null;
    rdti = rdMgmt.getRemoteDestinationTypeInstance(remoteDestination);
    if (rdti == null) {
      RepairEntry entry = new RepairEntry();
      entry.setDescription("Remote Destination " + remoteDestination + " does not exist.");
      entry.setId(step.getStepId());
      entry.setLocation(WorkflowRepair.createLocation(step));
      entry.setType("Remote Destination removal");
      result.add(entry);

      if (apply) {
        step.removeRemoteDispatchingParameter();
      }

      return result;
    }
    return Collections.emptyList();
  }


  private List<RepairEntry> executeCastRemoval(StepFunction s, boolean a, BiFunction<RemoveTypeCastData, Boolean, List<RepairEntry>> f) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    RemoveTypeCastData data = new RemoveTypeCastData();
    //data.step and data.location do change between input and output
    data.setStep(s);
    data.setLocation(WorkflowRepair.createLocation(s));

    //input
    data.setCastSupplier(s::getInputVarCastToType);
    data.setIdGenerator(WorkflowRepair.createVariableIDGenerator(s, VarUsageType.input));
    data.setVariables(Utils.getServiceInputVars(s));
    result.addAll(f.apply(data, a));

    //output
    data.setCastSupplier(s::getReceiveVarCastToType);
    data.setIdGenerator(WorkflowRepair.createVariableIDGenerator(s, VarUsageType.output));
    data.setVariables(Utils.getServiceOutputVariables(s));
    result.addAll(f.apply(data, a));

    return result;
  }


  private List<RepairEntry> removeTypeCastForRemovedBaseType(RemoveTypeCastData data, GenerationBaseObject gbo, boolean apply) {
    StepFunction step = data.getStep();
    String location = data.getLocation();
    String[] casts = data.getCastSupplier().get();
    List<AVariable> serviceVars = data.getVariables();
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    AVariable serviceVar;


    //if apply is false, we might need to update Signature
    if (functionNeedsSignatureUpdate(step)) {
      return Collections.emptyList();
    }

    for (int i = 0; i < casts.length; i++) {
      if (casts[i] == null || casts[i].length() == 0) {
        continue;
      }

      //insufficient number of service variables to cast
      if (serviceVars.size() <= i) {
        continue;
      }

      serviceVar = serviceVars.get(i); //TODO: check if we have enough serviceVars!
      if (XMOMRepair.variableHasToBeConverted(serviceVar)) {
        RepairEntry entry = new RepairEntry();
        entry.setDescription("TypeCast invalid and Basetype does not exist.");
        entry.setLocation(location);
        entry.setId(data.getIdGenerator().apply(i));
        entry.setType("Cast removal, variable conversion");
        result.add(entry);


        if (apply) {
          casts[i] = null;
          XMOMRepair.convertVariableToPrototype(serviceVar);
        }
      }
    }
    return result;

  }


  //removes cast if
  //- cast type does no longer exist
  //- cast is not valid (not a subType)
  private List<RepairEntry> removeTypeCast(RemoveTypeCastData data, boolean apply) {
    StepFunction step = data.getStep();
    String location = data.getLocation();
    String[] casts = data.getCastSupplier().get();
    List<AVariable> serviceVars = data.getVariables();
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    AVariable serviceVar;

    for (int i = 0; i < casts.length; i++) {
      if (casts[i] == null || casts[i].length() == 0) {
        continue;
      }

      //insufficient number of service variables to cast
      if (serviceVars.size() <= i) {
        continue;
      }

      //if base type does not exist, we do not remove the cast here. See removeTypeCastForRemovedBaseType.
      serviceVar = serviceVars.get(i);
      if (XMOMRepair.variableHasToBeConverted(serviceVar)) {
        continue;
      }

      Long revision = step.getParentWFObject().getRevision();
      boolean typeExists = WorkflowRepair.typeExists(casts[i], revision);
      boolean validCast = isValidCast(casts[i], revision, serviceVar);
      
      if (!typeExists || !validCast) {
        RepairEntry entry = new RepairEntry();
        entry.setDescription("Variable cast invalid");
        entry.setId(ObjectId.createStepId(step).getObjectId());
        entry.setLocation(location);
        entry.setType("Variable cast");
        result.add(entry);


        if (apply) {
          casts[i] = null;
        }
      }
    }

    return result;
  }


  private boolean isValidCast(String fqn, Long revision, AVariable serviceVar) {
    DomOrExceptionGenerationBase doe = serviceVar.getDomOrExceptionObject();
    if(doe == null) {
      return true; //AnyType
    }
    DomOrExceptionGenerationBase castDoe = null;
    try {
      castDoe = (DomOrExceptionGenerationBase) DomOrExceptionGenerationBase.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      castDoe.parse(false);
    } catch (Exception e) {
      return false;
    }
    return DomOrExceptionGenerationBase.isSuperClass(doe, castDoe);
  }


  private void applyUpdateStepFunctionSignature(StepFunction step) {
    //inputs
    int inputVarsOfService = Utils.getServiceInputVars(step).size();
    int inputVarsOfStep = step.getInputVarIds().length;
    if (inputVarsOfService > inputVarsOfStep) {
      for (int i = inputVarsOfStep; i < inputVarsOfService; i++) {
        step.getInputConnections().addInputConnection(i);
      }
    } else if (inputVarsOfService < inputVarsOfStep) {
      for (int i = inputVarsOfStep - 1; i >= inputVarsOfService; i--) {
        step.getInputConnections().removeInputConnection(i);
      }
    }

    //outputs
    List<AVariable> serviceOutputs = Utils.getServiceOutputVariables(step);
    for (int i = 0; i < serviceOutputs.size(); i++) {
      AVariable av = serviceOutputs.get(i);
      if (step.getOutputVars().size() <= i) {
        //add variable
        AVariable newVar = createClone(av, step);
        step.addOutputVarId(i, newVar.getId());
        step.getParentScope().getChildStep().addVar(newVar);
        continue;
      }

      //compare variable type
      AVariable stepVar = step.getOutputVars().get(i);
      boolean cast = step.getReceiveVarCastToType()[i] != null;
      if (domOrExceptionNeedsReplacement(stepVar.getDomOrExceptionObject(), av.getDomOrExceptionObject(), cast, false)) {
        if (cast) {
          step.getReceiveVarCastToType()[i] = null;
        } else {
          stepVar.replaceDomOrException(av.getDomOrExceptionObject(), stepVar.getLabel());
        }
      }
      
      //lists
      if (stepVar.isList() != av.isList()) {
        stepVar.setIsList(av.isList());
      }

    }

    //remove variable
    for (int i = step.getOutputVars().size() - 1; i >= serviceOutputs.size(); i--) {
      step.removeOutputVarId(i);
    }
  }


  private AVariable createClone(AVariable original, Step step) {
    AVariable result = null;

    if (original instanceof ServiceVariable) {
      result = new ServiceVariable((ServiceVariable) original);
    } else if (original instanceof ExceptionVariable) {
      result = new ExceptionVariable((ExceptionVariable) original);
    } else {
      throw new RuntimeException("unexpected AVariable type: " + original.getClass());
    }

    result.setId(String.valueOf(step.getCreator().getNextXmlId()));

    return result;
  }


  private boolean functionNeedsSignatureUpdate(StepFunction step) {
    //don't touch a prototype
    if (step.isPrototype() || functionNeedsConversionToPrototype(step)) {
      return false;
    }
  
    if(functionInputNeedsSignatureUpdate(step)) {
      return true;
    }
    
    if(functionOutputNeedsSignatureUpdate(step)) {
      return true;
    }

    return false;
  }
  
  
  private boolean functionInputNeedsSignatureUpdate(StepFunction step) {
    boolean hasOrderInputSource = step.getOrderInputSourceRef() != null && step.getOrderInputSourceRef().length() > 0;
    
    List<AVariable> serviceInputs = Utils.getServiceInputVars(step);
    String[] inputIds = step.getInputVarIds();
    
    //input length has to match, unless an orderInputSource is set
    if (!hasOrderInputSource && serviceInputs.size() != inputIds.length) {
      return true;
    }
    
    //check for connections that should not be
    //can't figure out if a auto-connections becomes ambiguous.
    for(int i=0; i< inputIds.length; i++) {
      String id = inputIds[i];
      if(id == null || id.length() == 0) {
        continue;
      }
      
      AVariable connectedVar = null;
      try {
        VariableIdentification ident = step.getParentScope().identifyVariable(id);
        connectedVar = ident.getVariable();
      } catch (XPRC_InvalidVariableIdException e) {
        return true; //variable was removed?
      }
      DomOrExceptionGenerationBase connectedDom = connectedVar.getDomOrExceptionObject();
      DomOrExceptionGenerationBase serviceDom = serviceInputs.get(i).getDomOrExceptionObject();
      boolean cast = step.getInputVarCastToType()[i] != null;
      
      //happens if the dataType was removed
      if(connectedDom == null) {
        continue;
      }
      
      if(domOrExceptionNeedsReplacement(connectedDom, serviceDom, cast, true)) {
        return true;
      }
    }
    
    
    
    return false;
  }
  
  private boolean functionOutputNeedsSignatureUpdate(StepFunction step) {
    List<AVariable> serviceOutputs = Utils.getServiceOutputVariables(step);
    List<AVariable> stepOutputs = step.getOutputVars();
    if (serviceOutputs.size() != stepOutputs.size()) {
      return true;
    }

    //without this check, types are updated quietly
    for (int i = 0; i < step.getOutputVars().size(); i++) {
      AVariable serviceVar = serviceOutputs.get(i);
      AVariable stepVar = stepOutputs.get(i);

      //if output is prototype, ignore it! 
      if (stepVar == null) {
        continue;
      }
      
      if (stepVar.isList() != serviceVar.isList()) {
        return true;
      }

      boolean cast = step.getReceiveVarCastToType()[i] != null && step.getReceiveVarCastToType()[i].length() != 0;

      //if this is a query, pretend like there is a cast
      if (step.isQueryStorable()) {
        cast = true;
      }

      if (domOrExceptionNeedsReplacement(stepVar.getDomOrExceptionObject(), serviceVar.getDomOrExceptionObject(), cast, false)) {
        return true;
      }
    }
    return false;
  }


  private boolean functionNeedsConversionToPrototype(StepFunction step) {

    //no need to convert, if this is a prototype already
    if (step.isPrototype()) {
      return false;
    }

    WF wf = step.getService().getWF();
    if (wf != null) {
      try {
        wf.parse(false);
        return !wf.exists();
      } catch (Exception e) {
        return true;
      }
    }

    DOM dom = step.getService().getDom();
    if (dom != null) {
      try {
        dom.parse(false);
        if (!dom.exists()) {
          return true;
        }

        String operationName = step.getOperationName();
        dom.getOperationByName(operationName);
      } catch (Exception e) {
        return true;
      }
    }
    
    //not a prototype, but no wf/dom set
    //happens if multiple service calls share the same serviceID
    //the first occurrence gets repaired and the second ends up
    //with null.AbstractService - but does not know it is a prototype
    if(wf == null && dom == null) {
      return true;
    }

    return false;
  }


  private void applyStepFunctionConversionToPrototype(StepFunction step) {

    String serviceId = step.getServiceId();
    ServiceIdentification si = null;
    try {
      si = step.getParentScope().identifyService(serviceId);
    } catch (XPRC_InvalidServiceIdException e) {
      e.printStackTrace();
    }
    si.service.createEmpty("-1"); //remove old service -otherwise we end up with multiple services with same id

    step.convertToPrototype();
    Step parentStepCatch = step.getParentStep();
    Step parentOfStepCatch = parentStepCatch.getParentStep();
    List<AVariable> varList;
    parentOfStepCatch.replaceChild(parentStepCatch, step);

    //when converting to Prototype, all in- and outputs are lost
    varList = new ArrayList<AVariable>(step.getInputVars());
    for (int i = varList.size() - 1; i >= 0; i--) {
      AVariable avar = varList.get(i);
      if (avar != null) {
        continue;
      }

      //remove from signature
      step.getInputConnections().removeInputConnection(i);
      varList.remove(i);
    }


    step.getService().getInputVars().addAll(varList);
    List<AVariable> outputVars = step.getOutputVars();
    List<AVariable> serviceOutputVars = step.getService().getOutputVars();
    for(AVariable outVar : outputVars)
      if (outVar != null) {
        serviceOutputVars.add(outVar);
      } else {
        //prototype - we do not know if it was a list, of what label it had
        AVariable prototype =  AVariable.createAnyType(step.getCreator(), false);
        prototype.createPrototype("AnyType");
        serviceOutputVars.add(prototype);
      }
  }


  private boolean domOrExceptionNeedsReplacement(DomOrExceptionGenerationBase current, DomOrExceptionGenerationBase should, boolean cast, boolean input) {

    //anyType
    if (should == null) {
      return false;
    }


    //if cast, this is fine
    if (cast) {
      return false;
    }

    //if should is not a superClass of current, we need to update
    if (!DomOrExceptionGenerationBase.isSuperClass(should, current)) {
      return true;
    }

    //if we are not casting current to a subclass of should then should and current have to be the same!
    //but only if this is output. Input does not require a cast.
    if (!cast && !input) {
      return !DomOrExceptionGenerationBase.isSuperClass(current, should);
    }

    //if non hit, there is no need to replace
    return false;
  }




}

