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

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.session.exceptions.ViewException;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.xo.Util;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.datatypes.json.DatatypeXo;
import com.gip.xyna.xact.filter.xmom.datatypes.json.ExceptiontypeXo;
import com.gip.xyna.xact.filter.xmom.servicegroup.ServiceGroupXO;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.filter.xmom.workflows.json.Workflow;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import xmcp.processmodeller.datatypes.DataType;
import xmcp.processmodeller.datatypes.ExceptionType;
import xmcp.processmodeller.datatypes.ServiceGroup;
import xmcp.processmodeller.datatypes.response.GetDataTypeResponse;
import xmcp.processmodeller.datatypes.response.GetExceptionTypeResponse;
import xmcp.processmodeller.datatypes.response.GetServiceGroupResponse;
import xmcp.processmodeller.datatypes.response.GetWorkflowResponse;
import xmcp.processmodeller.datatypes.response.XMOMItemResponse;

public class View {
  
  private GenerationBaseObject gbo;
  private boolean readonly = false;

  public View(GenerationBaseObject gbo) {
    this.gbo = gbo;
  }

  public GeneralXynaObject viewAll(XMOMGuiRequest request) {
    ObjectIdentifierJson object = gbo.getObjectIdentifierJson();
    try {
      switch( gbo.getType() ) {
      case DATATYPE:
        com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type viewType = request.getType();
        if(viewType == null) {
          if(gbo.getViewType() != null) {
            viewType = gbo.getViewType();
          } else {
            return datatypeToJson(object);
          }
        }
        switch(viewType) {
          case serviceGroup:
            return serviceGroupToJson(object);
          case dataType:
            return datatypeToJson(object);
          default:
            return datatypeToJson(object);
        }
      case WORKFLOW:
        return workflowToJson(object);
      case EXCEPTION:
        return exceptionTypeToJson(object);
      
      default:
        return Utils.error("Unimplemented View for type " + gbo.getType());
      }
    } catch( ViewException e) {
      if(e.getCause() != null) {
        return Utils.error(e);
      }
      return Utils.error(e);
    }
  }

  private void setStateOfReponse(XMOMItemResponse response, GenerationBaseObject obj) {
    response.setSaveState(gbo.getSaveState());
    response.setDeploymentState(gbo.getDeploymentState());
    response.setModified(gbo.hasBeenModified());
  }
  
  private GeneralXynaObject serviceGroupToJson(ObjectIdentifierJson object) {
    GetServiceGroupResponse response = new GetServiceGroupResponse();
    setStateOfReponse(response, gbo);
    response.setFocusId(gbo.getFocusId());

    ServiceGroupXO serviceGroupXO = new ServiceGroupXO(gbo);
    serviceGroupXO.setReadonly(readonly);
    response.setXmomItem((ServiceGroup)serviceGroupXO.getXoRepresentation());
    response.setRevision(gbo.getRevision());

    return response;
  }
  
  private GeneralXynaObject datatypeToJson(ObjectIdentifierJson object) {
    GetDataTypeResponse response = new GetDataTypeResponse();
    setStateOfReponse(response, gbo);
    response.setRevision(gbo.getRevision());
    response.setFocusId(gbo.getFocusId());

    if(gbo.getGenerationBase() != null) {
      DatatypeXo datatypeXo = new DatatypeXo(gbo);
      datatypeXo.setReadonly(readonly);
      response.setXmomItem((DataType) datatypeXo.getXoRepresentation());
    } else {
      DataType anyTypeXo = (DataType) DatatypeXo.getAnyTypeXoRepresentation(object.getRuntimeContext());
      anyTypeXo.setReadonly(readonly);
      response.setXmomItem(anyTypeXo);
    }

    return response;
  }
  
  private GeneralXynaObject exceptionTypeToJson(ObjectIdentifierJson object) {
    GetExceptionTypeResponse response = new GetExceptionTypeResponse();
    setStateOfReponse(response, gbo);
    
    ExceptiontypeXo exceptionType = new ExceptiontypeXo(gbo);
    exceptionType.setReadonly(readonly);
    response.setXmomItem((ExceptionType) exceptionType.getXoRepresentation());
    response.setRevision(gbo.getRevision());
    return response;
  }
  

  private GeneralXynaObject workflowToJson(ObjectIdentifierJson object) {
    gbo.createDataflow();

    GetWorkflowResponse response = new GetWorkflowResponse();
    setStateOfReponse(response, gbo);

    Workflow workflow = new Workflow(gbo);
    workflow.setReadonly(readonly);
    xmcp.processmodeller.datatypes.Workflow xoRepresentation = (xmcp.processmodeller.datatypes.Workflow) workflow.getXoRepresentation();
    response.setXmomItem(xoRepresentation);
    gbo.setLastXoRepresentation(xoRepresentation);
    response.setRevision(gbo.getRevision());

    return response;
  }

  public GenerationBaseObject getGenerationBaseObject() {
    return gbo;
  }
  
  public static class MetaJson {

    private int revision;
    private boolean saveState;
    private String deploymentState;

    public MetaJson() {
    }
    
    public MetaJson(GenerationBaseObject gbo) {
      this.revision = gbo.getRevision();
      this.saveState = gbo.getSaveState();
      this.deploymentState = gbo.getDeploymentState();
    }
    
    public int getRevision() {
      return revision;
    }
    
    public boolean getSaveState() {
      return saveState;
    }
    
    public String getDeploymentState() {
      return deploymentState;
    }
    
    public static JsonVisitor<MetaJson> getJsonVisitor() {
      return new MetaJsonVisitor();
    }
    
    public static class MetaJsonVisitor extends EmptyJsonVisitor<MetaJson> {
      private MetaJson mj = new MetaJson();
      
      @Override
      public MetaJson get() {
        return mj;
      }

      @Override
      public MetaJson getAndReset() {
        MetaJson ret = mj;
        mj = new MetaJson();
        return ret;
      }
      
      @Override
      public void attribute(String label, String value, com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type type)
          throws UnexpectedJSONContentException {
        if( label.equals(Tags.REVISION) ) {
          mj.revision = Integer.parseInt(value);
          return;
        }
        if( label.equals(Tags.SAVE_STATE) ) {
          mj.saveState = Boolean.parseBoolean(value);
          return;
        }
        if( label.equals(Tags.DEPLOYMENT_STATE) ) {
          mj.deploymentState = value;
          return;
        }
        super.attribute(label, value, type);
      }
      
    }
  }
  
  public static class ViewWrapperJson implements JsonSerializable {

    private MetaJson meta;
    private ObjectIdentifierJson identifier;
    //private JsonSerializable object;
    private JsonSerializable content;
    private String contentString;
    
    public ViewWrapperJson() {
    }
   
    public void setMeta(MetaJson meta) {
      this.meta = meta;
    }
    public void setIdentifier(ObjectIdentifierJson identifier) {
      this.identifier = identifier;
    }
    //public void setObject(JsonSerializable object) {
    //  this.object = object;
    //}
    public void setContent(JsonSerializable content) {
      this.content = content;
    }
    public JsonSerializable getContent() {
      return content;
    }
    
    public ObjectIdentifierJson getIdentifier() {
      return identifier;
    }
    
    public MetaJson getMeta() {
      return meta;
    }
    
    public String getContentAsString() {
      return contentString;
    }
    
    @Override
    public void toJson(JsonBuilder jb) {
      jb.startObject(); {
        Util.writeMetaData(jb, MetaXmomContainers.GET_WORKFLOW_RESPONSE_FQN, true); // add meta data of container class that holds information about response
        jb.addNumberAttribute(Tags.REVISION, meta.getRevision());
        jb.addBooleanAttribute(Tags.SAVE_STATE, meta.getSaveState());
        jb.addStringAttribute(Tags.DEPLOYMENT_STATE, meta.getDeploymentState());
        jb.addObjectAttribute(ObjectIdentifierJson.Type.workflow.name()); { // TODO: nicht nur Workflow unterstuetzen
          content.toJson(jb);
        } jb.endObject();
      } jb.endObject();
      
    }
    
    public String toJson() {
      JsonBuilder jb = new JsonBuilder();
      toJson(jb);
      return jb.toString();
    }
    
    public static <T> ViewWrapperJson parse(String json, String string, JsonVisitor<T> contentVisitor) throws InvalidJSONException, UnexpectedJSONContentException {
      JsonParser jp = new JsonParser();
      ViewWrapperJsonVisitor<T> vwjv =  new ViewWrapperJsonVisitor<T>("data", contentVisitor);
      jp.parse(json, vwjv );
      return vwjv.get();
    }
    
    public static class ViewWrapperJsonVisitor<T> extends EmptyJsonVisitor<ViewWrapperJson> {
      
      private String wrapLabel;
      private JsonVisitor<T> contentVisitor;
      private ViewWrapperJson wrapped;
      private JsonVisitor<ObjectIdentifierJson> identifierVisitor;
     
      public ViewWrapperJsonVisitor(String wrapLabel, JsonVisitor<T> contentVisitor) {
        this.wrapLabel = wrapLabel;
        this.contentVisitor = contentVisitor;
        this.wrapped = new ViewWrapperJson();
        this.identifierVisitor = ObjectIdentifierJson.getJsonVisitor();
      }
      @Override
      public ViewWrapperJson get() {
        return wrapped;
      }
      @Override
      public ViewWrapperJson getAndReset() {
        ViewWrapperJson ret = wrapped;
        wrapped = new ViewWrapperJson();
        return ret;
      }
      
      @Override
      public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
        if( label.equals(wrapLabel) ) {
          return null;
        }
        if( label.equals("content") ) {
          return contentVisitor;
        }
        if( label.equals("meta") ) {
          return MetaJson.getJsonVisitor();
        }
        return identifierVisitor.objectStarts(label);
        
      }
      
      @Override
      public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
        identifierVisitor.attribute(label, value, type);
      }
      
      @Override
      public void object(String label, Object value) throws UnexpectedJSONContentException {
        if( label.equals(wrapLabel) ) {
          return;
        }
        if( label.equals("content") ) {
          if( value instanceof String ) {
            wrapped.contentString = (String) value;
          } else {
            wrapped.setContent( (JsonSerializable) value);
          }
          return;
        }
        if( label.equals("meta") ) {
          wrapped.setMeta( (MetaJson) value);
          return;
        }
        identifierVisitor.object(label, value);
        wrapped.setIdentifier(identifierVisitor.get());
      }

    }

  }

  
  public boolean isReadonly() {
    return readonly;
  }

  
  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

}
