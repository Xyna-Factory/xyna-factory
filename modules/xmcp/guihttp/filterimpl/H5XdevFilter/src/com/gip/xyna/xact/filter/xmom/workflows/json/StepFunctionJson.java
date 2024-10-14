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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.NotImplementedException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownInvocationTypeException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.ViewException;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.ServiceIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils.EscapableXMLEntity;

import xmcp.processmodeller.datatypes.Data;
import xmcp.processmodeller.datatypes.FilterCriterion;
import xmcp.processmodeller.datatypes.FormulaArea;
import xmcp.processmodeller.datatypes.OrderInputSource;
import xmcp.processmodeller.datatypes.OrderInputSourceArea;
import xmcp.processmodeller.datatypes.Query;
import xmcp.processmodeller.datatypes.RemoteDestinationArea;
import xmcp.processmodeller.datatypes.SelectionMaskItem;
import xmcp.processmodeller.datatypes.SortingCriterion;
import xmcp.processmodeller.datatypes.Variable;
import xmcp.processmodeller.datatypes.VariableArea;
import xmcp.processmodeller.datatypes.invocation.DynamicMethodInvocation;
import xmcp.processmodeller.datatypes.invocation.Invocation;
import xmcp.processmodeller.datatypes.invocation.StaticMethodInvocation;
import xmcp.processmodeller.datatypes.invocation.WorkflowInvocation;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.SortCriterion;
import xnwh.persistence.Storable;

public class StepFunctionJson extends XMOMGuiJson implements HasXoRepresentation {
  
  private static final String QUERY_PARAMETER_MAX_OBJECTS = "maxObjects";
  private static final String QUERY_PARAMETER_QUERY_HISTORY = "queryHistory";
  private static final String QUERY_PARAMETER_QUERY_SORT_CRITERION = "sortCriterion";
  private static final String QUERY_PARAMETER_QUERY_SORT_CRITERION_CRITERION = "criterion";
  private static final String QUERY_PARAMETER_QUERY_SORT_CRITERION_REVERSE = "reverse";
  
  private static final String QUERY_SELECTION_MASK_ROOT_TYPE = "rootType";
  private static final String QUERY_SELECTION_MASK_COLUMNS = "columns";

  private final View view;
  private final StepFunction step;
  private final ObjectId stepId;
  private final ObjectPart part;
  
  private String documentation;
  private IdentifiedVariables identifiedVariables;
  private DistinctionJson exceptionHandlingJson = null;
  private CompensationJson compensateJson = null;
  private ServiceJson serviceJson;

  private static final Logger logger = CentralFactoryLogging.getLogger(StepFunctionJson.class);

  private Type type;
  private enum Type { prototype, service, workflow };
  
  public StepFunctionJson(View view, StepFunction step, DistinctionJson exceptionHandlingJson, CompensationJson compensateJson) {
    this(view, step);
    this.exceptionHandlingJson = exceptionHandlingJson;
    this.compensateJson = compensateJson;
  }
  
  public StepFunctionJson(View view, StepFunction step) {
    this(view, step, ObjectPart.all);
  }
  
  public StepFunctionJson(View view, StepFunction step, ObjectPart part) {
    this.view = view;
    this.step = step;
    this.stepId = ObjectId.createStepId(step);
    this.part = part;
  }
  
  private void extractStep() throws XynaException {
    //FIXME warum nicht Service service = step.getServcie();
    ServiceIdentification serviceIdentification = step.getParentScope().identifyService(step.getServiceId());
    Service service = serviceIdentification.service;
    this.type = inferType(service);
    this.serviceJson = new ServiceJson(step.getLabel());
    this.identifiedVariables = view.getGenerationBaseObject().identifyVariables(stepId);
    documentation = step.getDocumentation();
    
    FQNameJson fqName;
    switch( type ) {
      case service: //Instanzmethode oder ServiceGroup-Operation
        DOM dom = service.getDom();
        Operation operation = dom.getOperationByName(step.getOperationName());
        //TODO operation.getSpecialPurposeIdentifier()
        fqName = new FQNameJson(dom.getOriginalPath(), dom.getOriginalSimpleName());
        fqName.setOperation(operation.getName());
        serviceJson.setFQName( fqName );
        //TODO service.getServiceName();
        break;
      case workflow: //Subworkflow
        WF wf = service.getWF();
        fqName = new FQNameJson(wf.getOriginalPath(), wf.getOriginalSimpleName());
        serviceJson.setFQName( fqName );
        break;
      case prototype:
        serviceJson.setPrototype(true);
        break;
    }
  }
  
  private Type inferType(Service service) {
    if (service.isPrototype()) {
      return Type.prototype;
    }
    if( service.getDom() != null ) {
      return Type.service;
    }
    if( service.getWF() != null ) {
      return Type.workflow;
    }
    return Type.prototype;
  }
 
  private Service getService() {
    try {
      return step.getParentScope().identifyService(step.getServiceId()).service;
    } catch (XPRC_InvalidServiceIdException e) {
      e.printStackTrace(); // TODO: log
      return null; // TODO: error handling
    }
  }
  
  private enum InvocationType {
    workflowInvocation, staticMethodeInvocation, dynamicMethodeInvocation, prototypeInvocation;
  }
  
  private InvocationType getInvocationType() {
    Service service = getService();
    if (service == null) {
      return null;
    }
    
    if (service.isPrototype()) {
      return InvocationType.prototypeInvocation;
    }

    if (service.getWF() != null) {
      return InvocationType.workflowInvocation;
    }

    try {
      if (service.getDom().getOperationByName(step.getOperationName()).isStatic()) {
        return InvocationType.staticMethodeInvocation;
      } else {
        return InvocationType.dynamicMethodeInvocation;
      }
    } catch (XPRC_OperationUnknownException e) {
      Utils.logError("Could not determine whether operation " + step.getOperationName() + " is instance method.", e);
      return null;
    }
  }
  
  private String getServiceName() {
    Service service = getService();
    return (service != null) ? service.getServiceName() : null;
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    try {
      extractStep();
    } catch( XynaException e ) {
      throw new ViewException(e);
    }
    InvocationType invocationType = getInvocationType();
    if (invocationType == null) {
      throw new UnknownInvocationTypeException("null");
    }
    
    if(part == ObjectPart.all) {      
      if(isQueryWithHelperMapping()) {
        return createQuery();
      } else {
        Invocation invocation;
        switch(invocationType) {
          case workflowInvocation:
            invocation = new WorkflowInvocation();
            break;
          case dynamicMethodeInvocation :
            invocation = new DynamicMethodInvocation();
            ((DynamicMethodInvocation)invocation).setService(getServiceName());
            break;
          case staticMethodeInvocation :
            invocation = new StaticMethodInvocation();
            ((StaticMethodInvocation)invocation).setService(getServiceName());
            break;
          case prototypeInvocation :
            invocation = new Invocation();
            break;
          default:
            throw new UnknownInvocationTypeException(invocationType.name());
        }
        if(serviceJson.getFQName() != null) {
          invocation.setFqn(serviceJson.getFQName().toString());
        }
        
        try {
          invocation.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(step.getParentWFObject().getRevision()));
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          // nothing
        }
        
        invocation.setOperation(step.getOperationName());
        invocation.setId(stepId.getObjectId());
        invocation.setIsAbstract(serviceJson.isPrototype());
        invocation.setDetached(step.isExecutionDetached());
        invocation.setFreeCapacities(step.freesCapacities());
        invocation.setFreeCapacitiesTaggable(step.isFreeCapacitiesTaggable());
        invocation.setDetachedTaggable(step.isDetachedTaggable());
        invocation.setDeletable(true);
        
        if(!serviceJson.isPrototype()) {
                    
          OrderInputSourceArea oisa = new OrderInputSourceArea();
          oisa.setName(Tags.ORDER_INPUT_SOURCES);
          oisa.addToItemTypes(MetaXmomContainers.ORDER_INPUT_SOURCE_FQN);
          
          String usedSource = step.getOrderInputSourceRef();
          OrderInputSource ois = new OrderInputSource();
          ois.setName(usedSource != null ? usedSource : "");
          ois.setId(ObjectId.createId(ObjectType.orderInputSource, stepId.getBaseId()));
          oisa.setUsedInputSource(ois);
          invocation.addToAreas(oisa);
          
          RemoteDestinationArea rda = ServiceUtils.createRemoteDestinationArea(step);
          if (rda != null) {
            invocation.addToAreas(rda);
          }
        }
        invocation.addToAreas(ServiceUtils.createVariableArea(
                       view.getGenerationBaseObject(), stepId, VarUsageType.input, 
                       identifiedVariables, ServiceUtils.getServiceTag(VarUsageType.input), 
                       new String[] {MetaXmomContainers.DATA_FQN,  MetaXmomContainers.EXCEPTION_FQN}, 
                       identifiedVariables.isReadOnly()));
        invocation.addToAreas(ServiceUtils.createLabelArea(
                       stepId, serviceJson.getLabel(), 
                       serviceJson.getFQName() != null ? serviceJson.getFQName().toString() : null, 
                       serviceJson.readOnly, serviceJson.isPrototype()));
        invocation.addToAreas(ServiceUtils.createDocumentationArea(stepId, documentation));
        invocation.addToAreas(ServiceUtils.createVariableArea(
                                                              view.getGenerationBaseObject(), stepId, VarUsageType.output, 
                                                              identifiedVariables, ServiceUtils.getServiceTag(VarUsageType.output), 
                                                              new String[] {MetaXmomContainers.DATA_FQN,  MetaXmomContainers.EXCEPTION_FQN}, 
                                                              identifiedVariables.isReadOnly()));
        if ( (exceptionHandlingJson != null) || (compensateJson != null) ) {
          invocation.addToAreas(ServiceUtils.createExceptionHandlingArea(exceptionHandlingJson, compensateJson));
        }
        
        return invocation;
      }
    } else {
      throw new NotImplementedException();
    }
  }
  
  private Query createQuery() {
    Query query = new Query();
    try {
      query.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(step.getParentWFObject().getRevision()));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // nothing
    }
    query.setLabel(step.getLabel());
    query.setId(ObjectId.createStepId(step).getObjectId());
    query.setDeletable(true);
    
    QueryParameter queryParameter = getQueryParameter();
    SelectionMask selectionMask = getSelectionMask();
    
    if(queryParameter != null) {
      query.setLimit(queryParameter.getMaxObjects());
      query.setQueryHistory(queryParameter.getQueryHistory());
    }
    query.addToAreas(ServiceUtils.createLabelArea(
                                                  stepId, step.getLabel(), 
                                                  serviceJson.getFQName() != null ? serviceJson.getFQName().toString() : null, 
                                                  false, false));
    query.addToAreas(ServiceUtils.createDocumentationArea(stepId, documentation));
    query.addToAreas(createQueryFilterCriteria(selectionMask));
    query.addToAreas(createQuerySortingArea(queryParameter, selectionMask));
    query.addToAreas(createSelectionMasksArea(selectionMask));    
    query.addToAreas(createQueryOutputArea(selectionMask));
    if ( (exceptionHandlingJson != null) || (compensateJson != null) ) {
      query.addToAreas(ServiceUtils.createExceptionHandlingArea(exceptionHandlingJson, compensateJson));
    }
    return query;
  }
  
  private QueryParameter getQueryParameter() {
    List<AVariableIdentification> variableIdentifications = identifiedVariables.getVariables(VarUsageType.input);
    
    for (AVariableIdentification vi : variableIdentifications) {
      if(vi.getIdentifiedVariable().getVarName().equals(Tags.QUERY_CONST_QUERY_PARAMETER) && vi.connectedness.isConstantConnected()) {
        AVariable variable = Utils.getGlobalConstVar(vi.connectedness.getConnectedVariableId(), view.getGenerationBaseObject().getWFStep());
        QueryParameter parameter = new QueryParameter();
        for (AVariable member : variable.getChildren()) {
          if(QUERY_PARAMETER_MAX_OBJECTS.equals(member.getVarName())) {
            parameter.setMaxObjects(Integer.valueOf(member.getValue()));
          } else if (QUERY_PARAMETER_QUERY_HISTORY.equals(member.getVarName())) {
            parameter.setQueryHistory(Boolean.valueOf(member.getValue()));
          } else if(QUERY_PARAMETER_QUERY_SORT_CRITERION.equals(member.getVarName())) {
            parameter.setSortCriterion(member.getChildren().stream().map(a -> {
              SortCriterion c = new SortCriterion();
              for (AVariable ac : a.getChildren()) {
                if(QUERY_PARAMETER_QUERY_SORT_CRITERION_CRITERION.equals(ac.getVarName())) {
                  c.setCriterion(ac.getValue());
                } else if (QUERY_PARAMETER_QUERY_SORT_CRITERION_REVERSE.equals(ac.getVarName())) {
                  c.setReverse(Boolean.valueOf(ac.getValue()));
                }
              }
              return c;
            }).collect(Collectors.toList()));
          }
        }
        return parameter;
      }
    }
     return null;     
  }
  
  private SelectionMask getSelectionMask() {
    List<AVariableIdentification> variableIdentifications = identifiedVariables.getVariables(VarUsageType.input);
    for (AVariableIdentification vi : variableIdentifications) {
      if(vi.getIdentifiedVariable().getVarName().equals(Tags.QUERY_CONST_SELECTION_MASK) && vi.connectedness.isConstantConnected()) {
        AVariable variable = Utils.getGlobalConstVar(vi.connectedness.getConnectedVariableId(), view.getGenerationBaseObject().getWFStep());
        SelectionMask mask = new SelectionMask();
        for (AVariable member : variable.getChildren()) {
          if(QUERY_SELECTION_MASK_ROOT_TYPE.equals(member.getVarName())) {
            mask.setRootType(member.getValue());
          } else if (QUERY_SELECTION_MASK_COLUMNS.equals(member.getVarName())) {
            for (String value : member.getValues()) {
              mask.addToColumns(value);
            }
          }
        }
        
        return mask;
      }
    }
    
    return null;
  }
  
  private VariableArea createQueryOutputArea(SelectionMask selectionMask) {
    
    List<VariableJson> variableJsons = VariableJson.toList(identifiedVariables, VarUsageType.output, view.getGenerationBaseObject(), null);
    Data outputVar = (Data) variableJsons.get(0).getXoRepresentation();
    
    VariableArea outputVariableArea = new VariableArea();
    outputVariableArea.setName(Tags.QUERY_OUTPUT);
    outputVariableArea.setItemTypes(Arrays.asList(MetaXmomContainers.DATA_FQN));
    outputVariableArea.setReadonly(true);
    
    if (outputVar.getCastToFqn() != null && Storable.class.getName().equals(outputVar.getCastToFqn())) {
      outputVar.setCastToFqn(null);
    }
    Data storable = createFakeStorableFromSelectionMask(selectionMask);
    if(storable != null) {
      outputVar.setLabel(storable.getLabel());
    }
    outputVariableArea.addToItems(outputVar);
    
    return outputVariableArea;
  }
  
  /**
   * Erzeugt eine Variable anhand der SelectionMask.
   * Diese wird von der GUI benötigt, um in den Formeln mit dem Datentyp arbeiten zu können.
   * @return
   */
  private Data createFakeStorableFromSelectionMask(SelectionMask selectionMask) {
    
    if(selectionMask == null || selectionMask.getRootType() == null) {
      return null;
    }
    
    String label = getSimpleName(selectionMask.getRootType());
    Data data = new Data();
    data.setId(ObjectId.createVariableId(stepId.getBaseId(), VarUsageType.input, Integer.MAX_VALUE));
    data.setDeletable(false);
    data.setFqn(selectionMask.getRootType());
    data.setReadonly(true);
    try {
      GenerationBaseObject storableGbo = view.getGenerationBaseObject().getXmomLoader()
          .load(new FQName(view.getGenerationBaseObject().getRuntimeContext(), selectionMask.getRootType()), true);
      label = storableGbo.getDOM().getLabel();
    } catch (XynaException e) {
      Utils.logError(e);
    }
    data.setLabel(label);
    return data;
  }
  
  private String getSimpleName(String fqn) {
    if(fqn == null) {
      return null;
    }
    int lastIndex = fqn.lastIndexOf('.');
    return fqn.substring(lastIndex);
  }
  
  private FormulaArea createSelectionMasksArea(SelectionMask selectionMask) {
    FormulaArea area = new FormulaArea();
    area.setName(Tags.QUERY_SELECTION_MASKS);
    area.setId(ObjectId.createId(ObjectType.querySelectionMasksArea, stepId.getBaseId()));
    if(selectionMask != null && selectionMask.getColumns() != null) {
      Data storable = createFakeStorableFromSelectionMask(selectionMask);
      int i = 0;
      for (String column : selectionMask.getColumns()) {
        SelectionMaskItem item = new SelectionMaskItem();
        if(storable != null) {
          item.addToInput(storable);
        }
        item.setId(ObjectId.createSelectionMaskId(stepId.getBaseId(), VarUsageType.input, i));
        item.setExpression(column);
        area.addToItems(item);
        
        i++;
      }
    }
    return area;
  }
  
  private FormulaArea createQuerySortingArea(QueryParameter queryParameter, SelectionMask selectionMask) {
    
    FormulaArea sortingCriterionArea = new FormulaArea();
    sortingCriterionArea.setName(Tags.QUERY_SORTINGS);
    sortingCriterionArea.setId(ObjectId.createId(ObjectType.querySortingArea, stepId.getBaseId()));
    
    if(queryParameter != null) {
      Data storable = createFakeStorableFromSelectionMask(selectionMask);
      List<? extends SortCriterion> criterions = queryParameter.getSortCriterion();
      int i = 0;
      for (SortCriterion sc : criterions) {
        SortingCriterion sortingCriterion = new SortingCriterion();
        if(storable != null) {
          sortingCriterion.addToInput(storable);
        }
        sortingCriterion.setId(ObjectId.createSortCriterionId(stepId.getBaseId(), VarUsageType.input, i));
        sortingCriterion.setExpression(sc.getCriterion());
        sortingCriterion.setAscending(!sc.getReverse());
        
        sortingCriterionArea.addToItems(sortingCriterion);
        i++;
      }
    }
    return sortingCriterionArea;
  }
  
  private FormulaArea createQueryFilterCriteria(SelectionMask selectionMask) {
    GBSubObject functionGBSubObject;
    try {
      functionGBSubObject = view.getGenerationBaseObject().getObject(stepId.getObjectId());
    } catch (UnknownObjectIdException | MissingObjectException | XynaException e) {
      // kann eigentlich nicht vorkommen, da die Prüfung isQueryWithHelperMapping() dann vorher schon nicht erfolgreich gewesen war
      Utils.logError(e);
      return null;
    }
    StepMapping mapping = QueryUtils.findQueryHelperMapping(functionGBSubObject);
    IdentifiedVariables identifiedMappingVariables = view.getGenerationBaseObject().identifyVariables(ObjectId.createStepId(mapping));
    
    Data storable = createFakeStorableFromSelectionMask(selectionMask);
    
    FormulaArea filterCriteriaArea = new FormulaArea();
    filterCriteriaArea.setName(Tags.QUERY_FILTER_CRITERIA);
    filterCriteriaArea.addToItemTypes(MetaXmomContainers.FORMULA_FQN);
    filterCriteriaArea.setId(ObjectId.createId(ObjectType.queryFilterArea, stepId.getBaseId()));

    List<String> expressions = Collections.emptyList();
    if(mapping.getRawExpressions() != null && !mapping.getRawExpressions().isEmpty()) {
      expressions = QueryUtils.extractQueryFormulas(mapping.getRawExpressions().get(0), mapping.getInputVars().size()); // FilterConditions aus dem Mapping parsen
    }
    /*
     * Es ist besser die FilterConditions aus dem Mapping zu nehmen, da der Flash-Modeller nicht immer die Conditions in die Meta-Informationen geschrieben hat.
     * Siehe PMOD-509 Filterkriterien in parallelen Querys werden nicht angezeigt
     */

    if(expressions != null) {
      for (int expressionNr = 0; expressionNr < expressions.size(); expressionNr++) {
        String expression = expressions.get(expressionNr) != null ? expressions.get(expressionNr) : "";
        FilterCriterion f = new FilterCriterion();
        f.setId(ObjectId.createFilterCriterionId(stepId.getBaseId(), VarUsageType.input, expressionNr));
        f.setIsVariablesReadonly(false);
        f.setExpression(unescapeXMLExpression(expression));
        if(storable != null) {
          f.addToInput(storable);
        }
        List<VariableJson> inputVars = VariableJson.toList(identifiedMappingVariables, VarUsageType.input, view.getGenerationBaseObject(), null);
        if(inputVars != null) {
          inputVars.forEach(v -> f.addToInput((Variable) v.getXoRepresentation()));
        }
        filterCriteriaArea.addToItems(f);
      }
    }
    return filterCriteriaArea;
  }
  
  private String unescapeXMLExpression(String expression) {
    if(expression == null) {
      return null;
    }
    for (EscapableXMLEntity entity : EscapableXMLEntity.values()) {
      expression = expression.replace(entity.getFullEscapedRepresentation(), entity.getUnescapedRepresentation());
    }
    expression = expression.replaceAll("(?<!\\\\)\\\\\"", "\"");        // \" -> "
    expression = expression.replaceAll("\\\\\\\\\\\\", "\\\\");         // \\\ -> \
    return expression;
  }
  
  private boolean isQueryWithHelperMapping() {
    try {
      return QueryUtils.findQueryHelperMapping(view.getGenerationBaseObject().getObject(stepId.getObjectId())) != null;
    } catch (UnknownObjectIdException | MissingObjectException | XynaException e) {
      // nothing
    }
    return false;
  }
}
