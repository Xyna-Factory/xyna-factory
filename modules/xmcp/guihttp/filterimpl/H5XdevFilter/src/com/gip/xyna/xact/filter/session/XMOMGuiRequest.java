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
package com.gip.xyna.xact.filter.session;

import java.util.HashMap;

import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.FQName.XmomVersion;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;

public class XMOMGuiRequest {


  public static enum Operation {
    
    ViewSaved, 
    ViewDeployed, 
    Save,
    Deploy,
    Create, 
    Delete(true), 
    DeleteDocument(true), 
    DataflowSaved,
    DataflowDeployed,
    Relations,
    Insert(true), 
    TemplateCall(true), 
    Copy(true), 
    CopyToClipboard(true), 
    PasteFromClipboard(true), 
    Change(true),
    ConstantChange(true),
    ConstantDelete(true),
    Convert(true),
    Complete(true),
    Decouple(true),
    Sort(true),
    Session,
    Move(true),
    Refactor(true),
    Replace(true),
    Type(true),
    Upload,
    Close,
    Unlock,
    OrderInputSources,
    Undo,
    Redo, 
    ReferenceCandidates,
    ViewXml,
    RemoteDestination(true),
    Issues,
    ShowClipboard,
    ClearClipboard,
    GetPollEvents,
    GetProjectPollEvents,
    SubscribeProjectPollEvents,
    UnsubscribeProjectPollEvents,
    CopyXml,
    Warnings,
    ModelledExpressions
    ;
    
    private boolean isModification;
    private Operation() {
      this.isModification = false;
    }
    private Operation( boolean isModification) {
      this.isModification = isModification;
    }
    public boolean isModification() {
      return isModification;
    }
    
  }
  
  private final Operation operation;
  private final RuntimeContext runtimeContext;
  private final Long revision;
  private final Type type;
  private String typePath;
  private String typeName;
  private String objectId;
  private String json;
  private FQName fqName;
  private HashMap<String,String> parameter = new HashMap<String,String>();
  
  public XMOMGuiRequest(RuntimeContext runtimeContext, Long revision, Operation operation, Type type) {
    this.runtimeContext = runtimeContext;
    this.revision = revision;
    this.operation = operation;
    this.type = type;
  }

  public void setTypePath(String typePath) {
    this.typePath = typePath;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }


  public void setJson(String json) {
    this.json = json;
  }

  public boolean hasFQName() {
    return fqName != null || (typePath != null && typeName != null);
  }

  public FQName getFQName() {
    if (fqName == null && typePath != null && typeName != null) {
      fqName = new FQName(revision, runtimeContext, typePath, typeName, getXmomVersion());
    }

    return fqName;
  }

  public XmomVersion getXmomVersion() {
    return (operation == Operation.ViewDeployed || operation == Operation.DataflowDeployed) ? XmomVersion.DEPLOYED : XmomVersion.SAVED;
  }

  public void setFQName(FQName fqName) {
    this.fqName = fqName;
  }

  public Operation getOperation() {
    return operation;
  }
  
  public String getJson() {
    return json;
  }

  public Long getRevision() {
    return revision;
  }

  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }

  public String getObjectId() {
    return objectId;
  }

  public String getTypeName() {
    return typeName;
  }
  
  public Type getType() {
    return type;
  }

  public void addParameter(String key, String value) {
    parameter.put(key, value);
  }

  public boolean getBooleanParamter(String key, boolean def) {
    String v = parameter.get(key);
    if( v == null ) {
      return def;
    }
    if( "true".equals(v) ) {
      return true;
    }
    if( "false".equals(v) ) {
      return false;
    }
    return def;
  }
  
}
