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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.vars.ConstPermission;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType.DispatchingParameter;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction.RemoteDespatchingParameter;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xmcp.processmodeller.datatypes.Area;
import xmcp.processmodeller.datatypes.ContentArea;
import xmcp.processmodeller.datatypes.Data;
import xmcp.processmodeller.datatypes.Formula;
import xmcp.processmodeller.datatypes.FormulaArea;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.LabelArea;
import xmcp.processmodeller.datatypes.RemoteDestination;
import xmcp.processmodeller.datatypes.RemoteDestinationArea;
import xmcp.processmodeller.datatypes.TextArea;
import xmcp.processmodeller.datatypes.TypeLabelArea;
import xmcp.processmodeller.datatypes.Variable;
import xmcp.processmodeller.datatypes.VariableArea;
import xmcp.processmodeller.datatypes.exception.ExceptionHandlingArea;

public class ServiceUtils {
  
  private static final RemoteDestinationManagement rdMgmt =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
  private static final RevisionManagement rm =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private static final ApplicationManagement appMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
  
  public static TextArea createDocumentationArea(ObjectId id, String documentation) {
    TextArea area = new TextArea();
    area.setName(Tags.SERVICE_DOCUMENTATION);
    area.setId(ObjectId.createDocumentationAreaId(id.getBaseId()));
    area.setText(documentation);
    return area;
  }

  public static Area createExceptionHandlingArea(DistinctionJson distinctionJson) {
    return createExceptionHandlingArea(distinctionJson, null);
  }
  
  public static ExceptionHandlingArea createExceptionHandlingArea(DistinctionJson distinctionJson, CompensationJson compensationJson) {
    ExceptionHandlingArea area = new ExceptionHandlingArea();
    area.setName(Tags.ERROR_HANDLING);
    area.setItemTypes(Arrays.asList(MetaXmomContainers.EXCEPTION_HANDLING_FQN, MetaXmomContainers.COMPENSATION_FQN));
    if(distinctionJson != null) {
      area.addToItems((Item) distinctionJson.getXoRepresentation());
    }
    if(compensationJson != null) {
      area.addToItems((Item) compensationJson.getXoRepresentation());
    }
    return area;
  }
  
  public static TypeLabelArea createLabelArea(ObjectId id, String label, String fqn, boolean readOnly, boolean isPrototype) {
    TypeLabelArea labelArea = new TypeLabelArea();
    labelArea.setName(Tags.SERVICE_LABEL);
    labelArea.setId(ObjectId.createLabelAreaId(id.getBaseId()));
    labelArea.setText(label);
    labelArea.setReadonly(readOnly);
    labelArea.setFqn(fqn);
    labelArea.setIsAbstract(isPrototype);
    return labelArea;
  }
  
  public static LabelArea createLabelArea(String label) {
    LabelArea labelArea = new LabelArea();
    labelArea.setName(Tags.LABEL);
    labelArea.setText(label);
    return labelArea;
  }
  
  public static VariableArea createVariableArea(GenerationBaseObject gbo, String id, VarUsageType usage, IdentifiedVariables identifiedVariables, String areaName, String[] itemTypes, boolean readonly) {
    VariableArea area = new VariableArea();
    area.setId(id);
    List<VariableJson> variables = VariableJson.toList(identifiedVariables, usage, gbo, null);
    if(variables != null) {
      for (VariableJson variableJson : variables) {
        area.addToItems((Variable) variableJson.getXoRepresentation());
      }
    }          
    for (String type : itemTypes) {
      area.addToItemTypes(type);
    }
    area.setName(areaName);
    area.setReadonly(readonly);
    return area;
  }
  
  public static VariableArea createVariableArea(GenerationBaseObject gbo, ObjectId id, VarUsageType usage, IdentifiedVariables identifiedVariables, String areaName, String[] itemTypes, boolean readonly) {
    return createVariableArea(gbo, ObjectId.createId(id, usage), usage, identifiedVariables, areaName, itemTypes, readonly);
  }
  
  public static ContentArea createContentArea(List<? extends GeneralXynaObject> items, String id, String name, boolean readonly, String... additionalItemTypes ) {
    ContentArea a = new ContentArea();
    a.setItemTypes(ServiceUtils.getItemTypesContentArea(additionalItemTypes));
    if(id != null) {
      a.setId(id);
    }
    if(name != null) {
      a.setName(name);
    }
    a.setReadonly(readonly);
    for (GeneralXynaObject gxo : items) {
      if(gxo instanceof Item) {
        a.addToItems((Item) gxo);
      }
    }
    if(a.getItems() == null) {
      a.setItems(Collections.emptyList());
    }
    return a;
  }
  
  public static List<String> getItemTypesContentArea(String... additionalItemTypes) {
    List<String> result = new ArrayList<String>(20);
    result.add(MetaXmomContainers.MAPPING_FQN);
    result.add(MetaXmomContainers.PARALLELISM_FQN);
    result.add(MetaXmomContainers.CONDITIONAL_BRANCHING_FQN);
    result.add(MetaXmomContainers.CONDITIONAL_CHOICE_FQN);
    result.add(MetaXmomContainers.DYNAMIC_METHODE_INVOCATION_FQN);
    result.add(MetaXmomContainers.EXCEPTION_FQN);
    result.add(MetaXmomContainers.FOREACH_FQN);
    result.add(MetaXmomContainers.INVOCATION_FQN);
    result.add(MetaXmomContainers.QUERY_FQN);
    result.add(MetaXmomContainers.STATIC_METHODE_INVOCATION_FQN);
    result.add(MetaXmomContainers.THROW_FQN);
    result.add(MetaXmomContainers.TYPE_CHOICE_FQN);
    result.add(MetaXmomContainers.WORKFLOW_INVOCATION_FQN);
    result.add(MetaXmomContainers.TEMPLATE_FQN);
    for (String additionalItemType : additionalItemTypes) {
      result.add(additionalItemType);
    }
    return result;
  }
  
  public static FormulaArea createFormulaArea(GenerationBaseObject gbo, ObjectId objectId, List<String> expressions, String areaName, IdentifiedVariables variables, boolean isVariablesReadonly, boolean isFormulasReadonly, List<VarUsageType> varUsageTypes, String... additionalItemTypes) {
    FormulaArea fa = new FormulaArea();
    fa.addToItemTypes(MetaXmomContainers.FORMULA_FQN);
    for (String itemType : additionalItemTypes) {
      fa.addToItemTypes(itemType);
    }
    fa.setName(areaName);
    fa.setId(ObjectId.createId(ObjectType.formulaArea, objectId.getBaseId()));
    for (int expressionNr = 0; expressionNr < expressions.size(); expressionNr++) {
      String expression = expressions.get(expressionNr) != null ? expressions.get(expressionNr) : "";
      Formula f = new Formula();
      f.setExpression(expression);
      f.setIsVariablesReadonly(isVariablesReadonly);
      f.setReadonly(isFormulasReadonly);
      f.setId(ObjectId.createFormulaId(objectId.getBaseId(), VarUsageType.input, expressionNr));
      for (VarUsageType varUsage : varUsageTypes) {
        List<VariableJson> variableJsons = VariableJson.toList(variables, varUsage, gbo, false);
        if(variableJsons != null) {
          for (VariableJson variableJson : variableJsons) {
            switch(varUsage) {
              case input:
                f.addToInput((Variable) variableJson.getXoRepresentation());
                break;
              case output :
                f.addToOutput((Variable) variableJson.getXoRepresentation());
                break;
              case thrown :
                f.addToThrown((Variable) variableJson.getXoRepresentation());
                break;
            }
          }
        }
      }
      fa.addToItems(f);
    }
    if(fa.getItems() == null) {
      fa.setItems(Collections.emptyList());
    }
    return fa;
  }
 
  public static String getServiceTag(VarUsageType usage) {
    switch( usage ) {
      case input:
        return Tags.SERVICE_INPUT;
      case output:
        return Tags.SERVICE_OUTPUT;
      case thrown:
        return Tags.SERVICE_THROWS;
      default:
        throw new IllegalStateException("Unexpected usage "+usage);
    }
  }
  

  public static RemoteDestinationArea createRemoteDestinationArea(StepFunction step) {

    if(!couldBeRemoteCall(step)) {
      return null;
    }
    
    RemoteDestinationArea area = new RemoteDestinationArea();
    area.setId(ObjectId.createRemoteDestinationAreaId(step).getObjectId());
    area.setName(Tags.REMOTE_DESTINATION);
    RemoteDespatchingParameter rdp = step.getRemoteDispatchingParameter();
    
    //there could be a remoteDestination, but none is set
    if(rdp == null) {
      return area;
    }
    String remoteDestinationName = rdp.getRemoteDestination();
    
    //remoteDestination is set
    if(remoteDestinationName != null && remoteDestinationName.length() > 0) {
      Collection<RemoteDestinationInstanceInformation> allRemoteDestinationInstances = rdMgmt.listRemoteDestinationInstances();
      Optional<RemoteDestinationInstanceInformation> remoteDest = allRemoteDestinationInstances.stream().filter(x -> x.getName() != null && x.getName().equals(remoteDestinationName)).findAny();
      if(remoteDest.isPresent()) {
        RemoteDestinationInstanceInformation info = remoteDest.get();
        RemoteDestination rd = new RemoteDestination();
        area.addToItems(rd);

        rd.setId(ObjectId.createRemoteDestinationId(step).getObjectId());
        rd.setName(info.getName());
        rd.setDescription(info.getDescription());
        
        List<DispatchingParameter> dps = info.getDispatchingParams().getDispatchingParameters();
        if(dps != null && dps.size() > 0) {
          VariableArea va = new VariableArea();
          rd.addToAreas(va);
          int idx = 0;
          for(DispatchingParameter dp : dps) {
            //add something to variableArea
            Variable variable = new Data();
            variable.setAllowCast(false);
            variable.setAllowConst(ConstPermission.ALWAYS.name());
            variable.setDeletable(false);
            variable.setFqn(dp.getTypepath() + "." + dp.getTypename());
            variable.setId(ObjectId.createRemoteDestinationParameterId(step, idx).getObjectId());
            variable.setIsAbstract(false);
            variable.setReadonly(true);
            variable.setLabel(dp.getLabel());
            va.addToItems(variable);
            idx++;
          }
        }
      }
    }
    
    
    
    return area;
  }
  
  
  private static boolean couldBeRemoteCall(StepFunction step) {

    if (step.getService().isPrototype()) {
      return false;
    }

    if (step.getService().getWF() == null) {
      return false;
    }

    if (rdMgmt.listRemoteDestinationInstances().isEmpty()) {
      return false;
    }

    WF wf = step.getService().getWF();
    Long revision = wf.getRevision();
    RuntimeContext rtc = null;
    try {
      rtc = rm.getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return false;
    }

    return (rtc.getType() == RuntimeContextType.Application);
  }


  public static void setRemoteDestination(StepFunction step, String remoteDestination) {


    if (!couldBeRemoteCall(step)) {
      throw new IllegalStateException("This step cannot be called be used for remote calls");
    }


    if (remoteDestination == null || remoteDestination.length() == 0) {
      step.removeRemoteDispatchingParameter();
      return;
    }

    if (step.getRemoteDispatchingParameter() == null) {
      //set RemoteDispatching parameter to empty - prepare to set below
      step.createEmptyDispatchingParameter();
    }

    if (remoteDestination == step.getRemoteDispatchingParameter().getRemoteDestination()) {
      return; //nothing to be done
    }

    step.getRemoteDispatchingParameter().setRemoteDestination(remoteDestination);
    List<DispatchingParameter> params =
        rdMgmt.getRemoteDestinationTypeInstance(remoteDestination).getDispatchingParameterDescription().getDispatchingParameters();
    if (params == null) {
      step.getRemoteDispatchingParameter().setInvokeVarIds(new String[0]);
      step.getRemoteDispatchingParameter().setConstantConnected(new boolean[0]);
      step.getRemoteDispatchingParameter().setUserConnected(new boolean[0]);
    } else {
      step.getRemoteDispatchingParameter().setInvokeVarIds(new String[params.size()]);
      step.getRemoteDispatchingParameter().setConstantConnected(new boolean[params.size()]);
      step.getRemoteDispatchingParameter().setUserConnected(new boolean[params.size()]);
    }
  }

}