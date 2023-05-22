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
package com.gip.xyna.xact.filter.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.session.gb.VariableMap;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.workflowwarnings.WorkflowWarningsHandler;
import com.gip.xyna.xact.filter.xmom.datatypes.json.DatatypeXo;
import com.gip.xyna.xact.filter.xmom.datatypes.json.ExceptiontypeXo;
import com.gip.xyna.xact.filter.xmom.datatypes.json.Utils;
import com.gip.xyna.xact.filter.xmom.servicegroup.ServiceGroupXO;
import com.gip.xyna.xact.filter.xmom.workflows.json.Workflow;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xmcp.processmodeller.datatypes.Item;

public class GenerationBaseObject {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(GenerationBaseObject.class);
  
  private static final DeploymentItemStateManagement statemgmt =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();

  private final FQName fqName;
  private final GenerationBase gb;
  private final View view;
  private int revision;
  private boolean hasBeenModified;
  private boolean savedAlready;
  private Dataflow df;
  private StepMap stepMap;
  private VariableMap variableMap;
  private XMOMType type;
  private ObjectIdentifierJson objectIdentifier;
  private Set<String> sgLibsToUpload; // file-ids of libraries to upload during deployment of service group
  private Set<String> sgLibsToDelete;  // file-names of libraries to delete during deployment of service group
  private XMOMLoader xmomLoader;
  private Type viewType;
  private Item lastXoRepresentation;
  private String focusId = null;


  public enum DeploymentState {
    notDeployed, modified, deployed, invalid;
  }


  public GenerationBaseObject(FQName fqName, GenerationBase gb, XMOMLoader xmomLoader) {
    this.xmomLoader = xmomLoader;
    this.fqName = fqName;
    this.gb = gb;
    this.view = new View(this);
    this.revision = 0;
    this.savedAlready = true;
    
    if (gb == null) {
      type = XMOMType.DATATYPE;
    } else if (gb instanceof WF) {
      type = XMOMType.WORKFLOW;
    } else if (gb instanceof DOM) {
      type = XMOMType.DATATYPE;
    } else if (gb instanceof ExceptionGeneration) {
      type = XMOMType.EXCEPTION;
    }
  }

  public GenerationBaseObject(FQName fqName, GenerationBase gb, XMOMLoader xmomLoader, boolean newCreated) {
    this(fqName, gb, xmomLoader);
    if( newCreated ) {
      this.savedAlready = false;
    }
  }
  
  public Item createXoRepresentation() {
    HasXoRepresentation payload;
    switch (getType()) {
      case DATATYPE:
        if (Type.serviceGroup == getViewType()) {
          payload = new ServiceGroupXO(this);
        } else {
          payload = new DatatypeXo(this);
        }
        break;
      case EXCEPTION:
        payload = new ExceptiontypeXo(this);
        break;
      case WORKFLOW:
        refreshDataflow(); // FIXME nicht immer refresh, nur wenn ben�tigt
        payload = new Workflow(this);
        break;
      default:
        return null;
    }
    
    return (Item)payload.getXoRepresentation();
  }
  
  public void setLastXoRepresentation(Item xoRepresentation) {
    lastXoRepresentation = xoRepresentation;
  }
  
  public Item getLastXoRepresentation() {
    return lastXoRepresentation;
  }
  
  public GenerationBase getGenerationBase() {
    return gb;
  }

  public XMOMType getType() {
    return type;
  }
  
  public WF getWorkflow() {
    return (WF)gb;
  }
  
  public DOM getDOM() {
    return (DOM)gb;
  }

  public ExceptionGeneration getExceptionGeneration() {
    return (ExceptionGeneration)gb;
  }

  public Dataflow getDataflow() {
    return df;
  }
  
  public FQName getFQName() {
    return fqName;
  }


  public ObjectIdentifierJson getObjectIdentifierJson() {
    if( objectIdentifier != null ) {
      return objectIdentifier;
    }
    ObjectIdentifierJson object = new ObjectIdentifierJson();
    if(gb == null) {
      //anyType
      object = Utils.createAnyTypeIdentifier(fqName.getRevision());
    } else {
      object.setLabel(gb.getLabel());
      object.setType(Type.of(type));
      if (object.getType() == Type.dataType) {

      }
      object.setFQName(new FQNameJson(gb.getOriginalPath(), gb.getOriginalSimpleName()));
      object.setRuntimeContext(new RuntimeContextJson(fqName.getRuntimeContext()));
      if (gb.getRevision() != fqName.getRevision()) {
        object.setOriginRuntimeContext(new RuntimeContextJson(gb.getRuntimeContext()));
      }
    }
    objectIdentifier = object;
    return objectIdentifier;
  }

  public void setRevision(int revision) {
    this.revision = revision;
  }

  public int getRevision() {
    return revision;
  }
  
  public boolean hasBeenModified() {
    return hasBeenModified;
  }

  public boolean getSaveState() {
    return savedAlready;
  }
  
  public void setSaveState(boolean savedAlready) {
    this.savedAlready = savedAlready;
  }
  
  public String getDeploymentState() {
    String fqName = getFQName().getFqName();
    long revision = getFQName().getRevision();
    
    try {
      DeploymentItemState state = statemgmt.get(fqName, revision);
      return state.getStateReport().getState().name();
    }
    catch(Exception e) {
      return null; //before save
    }
  }

  public void incrementRevision() {
    ++revision;
  }
  
  public GBSubObject getObject(String objectId) throws UnknownObjectIdException, MissingObjectException, XynaException {
    return (objectId == null) ? null : GBSubObject.of( this, objectId );
  }

  private StepMap getOrCreateStepMap() {
    if( stepMap == null ) {
      stepMap = new StepMap(getWorkflow());
    }
    return stepMap;
  }

  public Step getStep(String stepId) {
    if ( (stepId == null) || (stepId.isEmpty()) ) {
      // step is WFStep, which doesn't have an id
      return getOrCreateStepMap().get(null);
    } else {
      return getOrCreateStepMap().get(stepId);
    }
  }

  public WFStep getWFStep() {
    return (WFStep)getStep(null);
  }

  public View getView() {
    return view;
  }

  public StepMap getStepMap() {
    return getOrCreateStepMap();
  }
  
  public void createDataflow() {
    if( df == null ) {
      df = new Dataflow(this);
    } else {
      df.analyzeDataflow(getWorkflow());
    }
  }
  
  public void createDataflow(WorkflowWarningsHandler workflowWarningsHandler) {
    if( df == null ) {
      df = new Dataflow(this);
      df.setWorkflowWarningsHandler(workflowWarningsHandler);
    }
  }


  public void refreshDataflow() {
    if (df == null) {
      df = new Dataflow(this);
    } else {
      df.analyzeDataflow(getWorkflow());
    }

    df.applyDataflowToGB();
  }

  public GenerationBaseObject getParent() {
    switch( type ) {
    case DATATYPE:
      return createParent( getDOM().getSuperClassGenerationObject() );
    case EXCEPTION:
      return createParent( getExceptionGeneration().getSuperClassGenerationObject() );
    case FORM:
      break;
    case ORDERINPUTSOURCE:
      break;
    case WORKFLOW:
      break;
    default:
      break;
     //FIXME
    }
    return null;
  }

  private GenerationBaseObject createParent(GenerationBase parent) {
    if( parent == null ) {
      return null;
    } else {
      FQName fq = createFQName(parent.getOriginalPath(), parent.getOriginalSimpleName());
      GenerationBaseObject gbo = new GenerationBaseObject(fq, parent, xmomLoader);
      gbo.setViewType(getViewType());
      return gbo;
    }
  }

  public String getOriginalFqName() {
    return gb.getOriginalFqName();
  }

  public RuntimeContext getRuntimeContext() {
    return fqName.getRuntimeContext();
  }

  public FQName createFQName(String typePath, String typeName) {
    return new FQName(fqName.getRevision(), fqName.getRuntimeContext(), typePath, typeName);
  }

  public IdentifiedVariables identifyVariables(ObjectId objectId) {
    return getVariableMap().identifyVariables(objectId);
  }

  public VariableMap getVariableMap() {
    if( variableMap == null ) {
      variableMap = new VariableMap(this);
    }
    return variableMap;
  }

  public void resetVariableMap() {
    variableMap = null;
  }

  public ExceptionGeneration getExceptiontype() {
    return (ExceptionGeneration)gb;
  }

  public void markAsModified() {
    hasBeenModified = true;
  }

  public void markAsSaved() {
    savedAlready = true;
    hasBeenModified = false;
  }

  public Set<String> getSgLibsToUpload() {
    return sgLibsToUpload;
  }

  public void addSgLibToUpload(String fileId) {
    if (sgLibsToUpload == null) {
      sgLibsToUpload = new HashSet<String>();
    }

    sgLibsToUpload.add(fileId);
  }

  public void setSgLibsToUpload(Set<String> sgLibsToUpload) {
    this.sgLibsToUpload = sgLibsToUpload;
  }

  public Set<String> getSgLibsToDelete() {
    return sgLibsToDelete;
  }
  
  public void addSgLibToDelete(String libName) {
    if (sgLibsToDelete == null) {
      sgLibsToDelete = new HashSet<String>();
    }

    sgLibsToDelete.add(libName);

    if (sgLibsToUpload != null) {
      // in case an upload for the lib to delete was pending, it is to be discarded
      for (String fileId : sgLibsToUpload) {
        FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
        TransientFile tFile = fm.retrieve(fileId);
        if (tFile.getOriginalFilename().equals(libName)) {
          sgLibsToUpload.remove(fileId);
          break;
        }
      }
    }
  }

  public void setSgLibsToDelete(Set<String> sgLibToDelete) {
    this.sgLibsToDelete = sgLibToDelete;
  }
  
  public XMOMLoader getXmomLoader() {
    return xmomLoader;
  }

  
  public Type getViewType() {
    return viewType;
  }

  
  public void setViewType(Type viewType) {
    this.viewType = viewType;
  }


  public boolean focusOperation(String operationName) throws OperationNotSupportedException {
    if (getType() != XMOMType.DATATYPE) {
      throw new OperationNotSupportedException("Operations can only be focused in Data Types or Service Groups.");
    }

    List<OperationInformation> operationInformationList = Arrays.asList(getDOM().collectOperationsOfDOMHierarchy(true));
    int operationIdx = -1;
    for (OperationInformation operationInformation : operationInformationList) {
      if (Objects.equals(operationInformation.getOperation().getName(), operationName)) {
        operationIdx = operationInformationList.indexOf(operationInformation);
        break;
      }
    }

    if (operationIdx < 0) {
      focusId = null;
      return false;
    }

    focusId = ObjectId.createMemberMethodId(operationIdx);
    return true;
  }


  public String getFocusId() {
    return focusId;
  }


  public void setWarningsHandler(WorkflowWarningsHandler workflowWarningsHandler) {
    if (type != XMOMType.WORKFLOW) {
      return;
    }
    if (df == null) {
      df = new Dataflow(this);
    }
    df.setWorkflowWarningsHandler(workflowWarningsHandler);
  }

}
