/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

package com.gip.xyna.xact.filter.xmom.datatypes.json;

import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesService;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.PluginPaths;
import com.gip.xyna.xact.filter.xmom.datatypes.json.Utils.ExtendedContextBuilder;
import com.gip.xyna.xact.filter.xmom.workflows.enums.GuiLabels;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCall;

import xmcp.processmodeller.datatypes.TextArea;
import xmcp.processmodeller.datatypes.datatypemodeller.DynamicMethod;
import xmcp.processmodeller.datatypes.datatypemodeller.Method;
import xmcp.processmodeller.datatypes.datatypemodeller.StaticMethod;
import xmcp.yggdrasil.plugin.Context;


public class DatatypeMethodXo implements HasXoRepresentation {
  
  private final GenerationBaseObject gbo;
  private ObjectId methodId;
  private IdentifiedVariables identifiedVariables;
  private final Operation operation;

  private DOM inheritedFrom;
  private String implementationType;
  private String implementation;
  private String reference;
  private Boolean overrides = Boolean.FALSE;
  protected final GuiHttpPluginManagement pluginMgmt;
  protected final ExtendedContextBuilder contextBuilder;

  public DatatypeMethodXo(Operation operation, GenerationBaseObject gbo, String id, ExtendedContextBuilder contextBuilder) {
    this.gbo = gbo;
    try {
      this.methodId = ObjectId.parse(id);
    } catch (UnknownObjectIdException e) {
      throw new RuntimeException("Could generate id for member function", e);
    }
    this.identifiedVariables = new IdentifiedVariablesService(methodId, operation, gbo.getDOM());
    this.identifiedVariables.showLinkState(false);
    pluginMgmt = GuiHttpPluginManagement.getInstance();

    this.operation = operation;
    if(operation.isAbstract()) {
      implementationType = GuiLabels.DT_LABEL_IMPL_TYPE_ABSTRACT;
    } else {
      if(operation instanceof JavaOperation) {
        implementationType = GuiLabels.DT_LABEL_IMPL_TYPE_CODED_SERVICE;
        JavaOperation javaOperation = (JavaOperation)operation;
        implementation = javaOperation.getImpl();
      } else if(operation instanceof WorkflowCall) {
        implementationType = GuiLabels.DT_LABEL_IMPL_TYPE_REFERENCE;
        WorkflowCall workflowCall = (WorkflowCall)operation;
        reference = workflowCall.getWfFQClassName();
      }
    }
    this.contextBuilder = contextBuilder;
  }

  @Override
  public GeneralXynaObject getXoRepresentation() {
    Method method;
    if(operation.isStatic()) {
      StaticMethod staticMethod = new StaticMethod();
      
      method = staticMethod;
    } else {
      DynamicMethod dynamicMethod = new DynamicMethod();
      if(reference != null) {
        dynamicMethod.setReference(reference);
      }
      method = dynamicMethod;
    }
    method.setLabel(operation.getLabel());
    method.setName(operation.getName());
    method.setIsAbortable(operation.isStepEventListener());
    method.setImplementationType(implementationType);
    method.setId(methodId.getObjectId());
    method.setIsLabelReadonly(operation.hasBeenPersisted());

    try {
      method.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(gbo.getFQName().getRevision()));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // nothing
    }
    if(implementation != null) {
      method.setImplementation(implementation);
    }
    Integer methodNumber = ObjectId.parseMemberMethodNumber(methodId);
    method.addToAreas(createDocumentationArea());
    method.addToAreas(ServiceUtils.createVariableArea(gbo, ObjectId.createId(ObjectType.methodVarArea, String.valueOf(methodNumber), ObjectPart.input), VarUsageType.input,
                                                      identifiedVariables, ServiceUtils.getServiceTag(VarUsageType.input), 
                                                      new String[] {MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN}, 
                                                      identifiedVariables.isReadOnly()));
    method.addToAreas(ServiceUtils.createVariableArea(gbo, ObjectId.createId(ObjectType.methodVarArea, String.valueOf(methodNumber), ObjectPart.output), VarUsageType.output,
                                                      identifiedVariables, ServiceUtils.getServiceTag(VarUsageType.output), 
                                                      new String[] {MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN}, 
                                                      identifiedVariables.isReadOnly()));
    method.addToAreas(ServiceUtils.createVariableArea(gbo, ObjectId.createId(ObjectType.methodVarArea, String.valueOf(methodNumber), ObjectPart.thrown), VarUsageType.thrown,
                                                      identifiedVariables, ServiceUtils.getServiceTag(VarUsageType.thrown), 
                                                      new String[] {MetaXmomContainers.EXCEPTION_FQN}, 
                                                      identifiedVariables.isReadOnly()));

    return method;
  }
  
  private TextArea createDocumentationArea() {
    TextArea area = new TextArea();
    area.setName(Tags.DATA_TYPE_DOCUMENTATION_AREA);
    area.setId(ObjectId.createOperationDocumentationAreaId(String.valueOf(ObjectId.parseMemberMethodNumber(methodId))));
    area.setText(operation.getDocumentation());
    area.setReadonly(inheritedFrom != null);
    Context context = contextBuilder.instantiateContext(PluginPaths.location_datatype_method_documentation, methodId.getObjectId());
    area.unversionedSetPlugin(pluginMgmt.createPlugin(context));
    return area;
  }

  public boolean isStatic() {
    return operation.isStatic();
  }
  
  public boolean isInherited() {
    return inheritedFrom != null;
  }
  
  public boolean isMemberMethod() {
    return !isInherited() && !overrides();
  }
  
  public boolean overrides() {
    return overrides;
  }
  
  public Boolean getOverrides() {
    return overrides;
  }

  
  public void setOverrides(Boolean overrides) {
    this.overrides = overrides;
  }

  
  public DOM getInheritedFrom() {
    return inheritedFrom;
  }

  
  public void setInheritedFrom(DOM inheritedFrom) {
    this.inheritedFrom = inheritedFrom;
  }
  
  public Operation getOperation() {
    return operation;
  }

}
