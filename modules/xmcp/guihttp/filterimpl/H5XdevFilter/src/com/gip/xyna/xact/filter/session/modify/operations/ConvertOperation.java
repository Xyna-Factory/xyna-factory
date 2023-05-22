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

package com.gip.xyna.xact.filter.session.modify.operations;

import java.util.Arrays;
import java.util.List;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.PersistJson;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.modify.Insertion;
import com.gip.xyna.xact.filter.session.modify.Insertion.PossibleContent;
import com.gip.xyna.xact.filter.session.save.Persistence;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ConvertJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.InsertJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MappingJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Utils;

public class ConvertOperation extends ModifyOperationBase<ConvertJson>{
  
  enum ConvertStepTargetType {
    workflow, mapping
  }

  private ConvertJson convertJson; 
  
  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    convertJson = jp.parse(jsonRequest, ConvertJson.getJsonVisitor());
    
    return convertJson.getRevision();
  }

  @Override
  protected void modifyStep(Step step) throws Exception {
    ConvertStepTargetType targetType = ConvertStepTargetType.valueOf(convertJson.getTargetType());
    switch(targetType) {
      case workflow:
        convertPrototypeStepToWorkflow();
        break;
      case mapping:
        convertPrototypeStepToMapping();
        break;
      default:
        throw new java.lang.UnsupportedOperationException();
    }
  }
  
  private void convertPrototypeStepToMapping() throws Exception {
    
    GBSubObject relativeToObject = object.getParent();
    List<Step> steps = relativeToObject.getStepListAdapter();
    int index = -1;
    for(int i = 0; i < steps.size(); i++) {
      if(steps.get(i).equals(object.getStep())) {
        index = i;
        break;
      }
    }
    InsertJson insertJson = new InsertJson(convertJson.getRevision(), ObjectId.createStepId(relativeToObject.getStep()).getObjectId(), index);
    
    Insertion insertion = new Insertion(relativeToObject, insertJson);
    insertion.wrapWhenNeeded(modification.getObject());
    insertion.inferWhere(object);
    insertion.inferPossibleContent();
    insertion.checkContent(object);
    
    String label = "Mapping";
    if(object.getStep().getLabel() != null && !object.getStep().getLabel().equals("Service")) {
      label = object.getStep().getLabel();
    }
    
    MappingJson mappingJson = new MappingJson(label);
    
    Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.mapping, mappingJson);
    GBBaseObject newObject = createNewObject(insertion.getParent(), content, insertJson);
    insertion.insert(newObject);
    
    for (VarUsageType varUsageType : Arrays.asList(VarUsageType.input, VarUsageType.output)) {
      for (AVariableIdentification av : object.getIdentifiedVariables().getVariables(varUsageType)) {        
        copyVar(
                modification.getObject().getObject(ObjectId.createStepId(newObject.getStep(), ObjectId.ObjectPart.forUsage(varUsageType)).getObjectId()), 
                av, 
                ObjectId.createStepId(newObject.getStep(), ObjectId.ObjectPart.forUsage(varUsageType)));
      }

      modification.getObject().getDataflow().replaceVariables(object.getIdentifiedVariables(), newObject.getStep(), varUsageType);
    }
    
    
    DeleteOperation deleteOperation = new DeleteOperation();
    deleteOperation.object = object;
    deleteOperation.modification = modification;
    deleteOperation.modifyStep(object.getStep());
  }
  
  private void convertPrototypeStepToWorkflow() throws Exception {
    FQNameJson fqNameJsonNewObject = new FQNameJson(convertJson.getPath(), Utils.labelToJavaName(convertJson.getLabel(), true));
    
    try {
      modification.load(fqNameJsonNewObject);
      throw new UnsupportedOperationException("convert prototype", UnsupportedOperationException.PROTOTYPE_CONVERTION_NOT_ALLOWED_TARGET_TYPE_EXISTS);
    } catch (Ex_FileAccessException ex) {
      // Only unknown types are allowed as target type.
    }
    
    
    ObjectIdentifierJson objectIdentifierJson = new ObjectIdentifierJson();
    objectIdentifierJson.setRuntimeContext(new RuntimeContextJson(modification.getObject().getRuntimeContext()));
    objectIdentifierJson.setLabel(convertJson.getLabel());
    objectIdentifierJson.setType( ObjectIdentifierJson.Type.valueOf(convertJson.getTargetType()));
    objectIdentifierJson.setFQName(fqNameJsonNewObject);
      
    GenerationBaseObject gboNewObject = modification.getSession().createNewObject(objectIdentifierJson);
    
    try {
      if(gboNewObject.getType() == XMOMType.WORKFLOW && object.getType() == ObjectType.step) {
        List<AVariableIdentification> inputVariables =  object.getIdentifiedVariables().getVariables(VarUsageType.input);
        List<AVariableIdentification> outputVariables =  object.getIdentifiedVariables().getVariables(VarUsageType.output);
                
        for (AVariableIdentification av : inputVariables) {
          copyVar(gboNewObject.getObject("wf_input"), av, new ObjectId(ObjectType.workflow, null, ObjectPart.input));
        }
        
        for (AVariableIdentification av : outputVariables) {
          copyVar(gboNewObject.getObject("wf_output"), av, new ObjectId(ObjectType.workflow, null, ObjectPart.output));
        }
      }
            
      PersistJson persistJson = new PersistJson(0, false);
      persistJson.setLabel(convertJson.getLabel());
      persistJson.setPath(convertJson.getPath());
      
      Persistence persistence = new Persistence(gboNewObject, modification.getObject().getFQName().getRevision(), persistJson, modification.getSession().getSession());
      persistence.save();
      
      GBSubObject relativeToObject = object.getParent();
      List<Step> steps = relativeToObject.getStepListAdapter();
      int index = -1;
      for(int i = 0; i < steps.size(); i++) {
        if(steps.get(i).equals(object.getStep())) {
          index = i;
          break;
        }
      }
      InsertJson insertJson = new InsertJson(convertJson.getRevision(), ObjectId.createStepId(relativeToObject.getStep()).getObjectId(), index);
      
      Insertion insertion = new Insertion(relativeToObject, insertJson);
      insertion.wrapWhenNeeded(modification.getObject());
      insertion.inferWhere(object);
      insertion.inferPossibleContent();
      insertion.checkContent(object);
      
      ServiceJson serviceJson = new ServiceJson(convertJson.getLabel());
      serviceJson.setFQName(FQNameJson.ofPathAndName(gboNewObject.getOriginalFqName()));
      
      Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.service, serviceJson);
      GBBaseObject newObject = createNewObject(insertion.getParent(), content, insertJson);
      insertion.insert(newObject);
      
      Dataflow dataflow = modification.getObject().getDataflow();
      Step service = ((StepCatch)newObject.getStep()).getStepInTryBlock();
      IdentifiedVariables idVars = object.getIdentifiedVariables();
      
      dataflow.replaceVariables(idVars, service, VarUsageType.input);
      dataflow.replaceVariables(idVars, service, VarUsageType.output);

      
      DeleteOperation deleteOperation = new DeleteOperation();
      deleteOperation.object = object;
      deleteOperation.modification = modification;
      deleteOperation.modifyStep(object.getStep());
    } finally {
      modification.getSession().removeFromGboMap(gboNewObject.getFQName());
    }
  }
  
  private void copyVar(GBSubObject newObject, AVariableIdentification origVar, ObjectId destination) throws UnknownObjectIdException, MissingObjectException, XynaException {
    GBSubObject varObject = object.getRoot().getObject(origVar.internalGuiId.createId());
    VariableJson varJson = new VariableJson(varObject);
    GBBaseObject newVarObject = createParameter(newObject, varJson);

    InsertJson varInsertJson = new InsertJson(convertJson.getRevision(), destination.getObjectId(), -1);
    Insertion varInsertion = new Insertion(newObject, varInsertJson);
    varInsertion.inferWhere(varObject);
    varInsertion.inferPossibleContent();
    varInsertion.insert(newVarObject);
  }

  @Override
  protected void modifyVariable(Variable variable) throws Exception {
    
    GBSubObject parent = object.getParent();
    FQNameJson fqNameJson = new FQNameJson(convertJson.getPath(), Utils.labelToJavaName(convertJson.getLabel(), true));
    
    try {
      modification.load(fqNameJson);
      throw new UnsupportedOperationException("convert prototype", UnsupportedOperationException.PROTOTYPE_CONVERTION_NOT_ALLOWED_TARGET_TYPE_EXISTS);
    } catch (Ex_FileAccessException ex) {
      // Only unknown types are allowed as target type.
    }
    
    ObjectIdentifierJson objectIdentifierJson = new ObjectIdentifierJson();
    objectIdentifierJson.setRuntimeContext(new RuntimeContextJson(modification.getObject().getRuntimeContext()));
    objectIdentifierJson.setLabel(convertJson.getLabel());
    objectIdentifierJson.setType( ObjectIdentifierJson.Type.dataType);
    objectIdentifierJson.setFQName(fqNameJson);
    
    GenerationBaseObject gbo = modification.getSession().createNewObject(objectIdentifierJson);
    
    PersistJson deployRequest = new PersistJson(convertJson.getRevision(), false);
    deployRequest.setPath(convertJson.getPath());
    deployRequest.setLabel(convertJson.getLabel());
    
    Persistence persistence = new Persistence(
                      gbo, 
                      com.gip.xyna.xact.filter.util.Utils.getRtcRevision(modification.getObject().getRuntimeContext()), 
                      deployRequest, 
                      modification.getSession().getSession());
    persistence.save();
    persistence.deploy();
    
    parent.getVariableListAdapter().remove(variable);
    
    VariableJson variableJson = new VariableJson(convertJson.getTargetType(), convertJson.getLabel(), fqNameJson);
    InsertJson insertJson = new InsertJson(convertJson.getRevision(), object.getParent().getObjectId(), variable.getIndex());
    
    Insertion insertion = new Insertion(parent, insertJson);
    insertion.wrapWhenNeeded(modification.getObject());
    insertion.inferWhere(parent);
    insertion.inferPossibleContent();
    GBBaseObject newObject = createNewObject(parent, new Pair<Insertion.PossibleContent, XMOMGuiJson>(PossibleContent.variable, variableJson), insertJson);
    insertion.insert(newObject);
    insertion.updateParentsWhenNeeded(object, modification.getObject());
  }

}
