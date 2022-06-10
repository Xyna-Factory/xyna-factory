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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.PluginDescription;


public interface RemoteDestinationType {
  
  public void init(Map<String, Object> parameter);
  
  public PluginDescription getInitialisationParameterDescription();
  
  public DispatchingParameterDescription getDispatchingParameterDescription();
  
  public DispatchingTarget dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter);
  
  public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location, DispatchingTarget failedTarget, Throwable error, int retryCount, GeneralXynaObject remoteDispatchingParameter);
  
  // how do we react to ErrorHandlingTypes during Phase2
  // Throw: we no longer fetch for that id (invalidate result on target asap as connection returned)
  // Dispatch: new target equals old target equals queue...just throw again?
  // Queue: insert again
  
  public static class DispatchingTarget {
    public String factoryNodeName;
    public List<String> factoryNodeNames;
    public RuntimeContext context;
    public long executionTimeout;
  }
  
  public static enum ErrorHandlingType {
    THROW, TRY_NEXT, QUEUE;
  }
  
  public static enum ErrorHandlingLocation {
    PHASE1, PHASE2;
  }
  
  public static class ErrorHandling {
    public ErrorHandlingType type;
    public Throwable error; // for THROW
    public DispatchingTarget target; // for DISPATCH
    public long timeout; // for QUEUE
    
    
    public static ErrorHandling nextDispatchingTarget(DispatchingTarget target) {
      ErrorHandling eh = new ErrorHandling();
      eh.target = target;
      eh.type = ErrorHandlingType.TRY_NEXT;
      return eh;
    }


    public static ErrorHandling fail(Throwable error) {
      ErrorHandling eh = new ErrorHandling();
      eh.type = ErrorHandlingType.THROW;
      eh.error = error;
      return eh;
    }
  }
  
  public static class DispatchingParameterDescription implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private List<DispatchingParameter> dispatchingParameters;

    
    public List<DispatchingParameter> getDispatchingParameters() {
      return dispatchingParameters;
    }

    public void setDispatchingParameters(List<DispatchingParameter> dispatchingParameters) {
      this.dispatchingParameters = dispatchingParameters;
    }
    
  }
  
  public static class DispatchingParameter implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String typename;
    private String typepath;
    private String label;
    private boolean isList;
    
    public DispatchingParameter() {
    }
    
    public DispatchingParameter(String typename, String typepath, String label, boolean isList) {
      this.typename = typename;
      this.typepath = typepath;
      this.label = label;
      this.isList = isList;
    }
    
    public String getLabel() {
      return label;
    }
    
    public void setLabel(String label) {
      this.label = label;
    }
    
    public String getTypename() {
      return typename;
    }
    
    public void setTypename(String typename) {
      this.typename = typename;
    }
    
    public String getTypepath() {
      return typepath;
    }
    
    public void setTypepath(String typepath) {
      this.typepath = typepath;
    }
    
    public boolean isList() {
      return isList;
    }
    
    public void setList(boolean isList) {
      this.isList = isList;
    }
    
    
  }

}
