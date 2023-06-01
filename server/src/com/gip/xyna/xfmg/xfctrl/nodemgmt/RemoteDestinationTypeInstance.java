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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.PluginDescription;


public class RemoteDestinationTypeInstance implements RemoteDestinationType {
  
  private RemoteDestinationTypeInstanceStorable storable;
  private RemoteDestinationType initializedType;
  
  public RemoteDestinationTypeInstance(RemoteDestinationTypeInstanceStorable storable, Class<? extends RemoteDestinationType> clazz) {
    try {
      this.storable = storable;
      RemoteDestinationType type = clazz.getConstructor().newInstance();
      type.init(storable.getParameterMap());
      this.initializedType = type;
    } catch (Exception e) { 
      //InstantiationException, IllegalAccessException, StringParameterParsingException
      throw new RuntimeException(e);
    }
  }
  
  RemoteDestinationTypeInstanceStorable getStorable() {
    return storable;
  }
  
  public void init(Map<String, Object> parameter) {
    // ntbd, already initialised
  }
  
  public PluginDescription getInitialisationParameterDescription() {
    return initializedType.getInitialisationParameterDescription();
  }
  
  public DispatchingParameterDescription getDispatchingParameterDescription() {
    return initializedType.getDispatchingParameterDescription();
  }
  
  public DispatchingTarget dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) {
    DispatchingTarget target = initializedType.dispatch(ownContext, stubContext, remoteDispatchingParameter);
    if (storable != null &&
        storable.getExecutionTimeout() != null) {
      target.executionTimeout = storable.getExecutionTimeout().getDurationInMillis();
    }
    return target;
  }
  
  public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location,
                                             DispatchingTarget failedTarget, Throwable error, int retryCount,
                                             GeneralXynaObject remoteDispatchingParameter) {
    return initializedType.handleConnectionError(stubContext, location, failedTarget, error, retryCount, remoteDispatchingParameter);
  }
  
  RemoteDestinationInstanceInformation asInformation() {
    RemoteDestinationInstanceInformation info = new RemoteDestinationInstanceInformation();
    info.setName(storable.getName());
    info.setTypename(storable.getTypename());
    info.setDescription(storable.getDescription());
    info.setExecutionTimeout(storable.getExecutionTimeout());
    Map<String, String> stringMap = new HashMap<String, String>();
    try {
      for (Entry<String, Object> entry : storable.getParameterMap().entrySet()) {
        stringMap.put(entry.getKey(), String.valueOf(entry.getValue()));
      }
    } catch (StringParameterParsingException e) {
      throw new RuntimeException(e);
    }
    info.setStartparameter(stringMap);
    info.setDispatchingParams(initializedType.getDispatchingParameterDescription());
    return info;
  }

}
