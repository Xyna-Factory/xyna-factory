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

package com.gip.xyna.xact.filter.monitor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;

public class MonitorSession {
  
  public static class MonitorSessionInstance {
    
    private final ConcurrentHashMap<Long, String> orderIdToFileIdMap = new ConcurrentHashMap<>();
    
    private MonitorSessionInstance() {
      
    }
    
    public void clean() {
      orderIdToFileIdMap.clear();
    }
    
    public ConcurrentMap<Long, String> getOrderIdToFileIdMap() {
      return orderIdToFileIdMap;
    }
  }

  private MonitorSession() {
    
  }
  
  private static final SessionManagement sessionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
  
  private static final ConcurrentHashMap<String, MonitorSessionInstance> instanceMap = new ConcurrentHashMap<>();
  
  private static MonitorSession instance = null;
  
  private static MonitorSession getInstance() {
    if(instance == null) {
      instance = new MonitorSession();
    }
    return instance;
  }
  
  public static  MonitorSessionInstance getSessionInstance(HTTPTriggerConnection tc) {
    XynaPlainSessionCredentials sessionCredentials = AuthUtils.readCredentialsFromCookies(tc);
    return getSessionInstance(sessionCredentials.getSessionId());
  }
  
  public static MonitorSessionInstance getSessionInstance(String sessionId) {
    if(!instanceMap.containsKey(sessionId)) {
      return MonitorSession.getInstance().createNewSessionInstance(sessionId);
    }
    return instanceMap.get(sessionId);
  }
  
  public void killSession(String sessionId) {
    MonitorSessionInstance monitorSessionInstance = getSessionInstance(sessionId);
    if(monitorSessionInstance != null) {
      monitorSessionInstance.clean();
    }
    instanceMap.remove(sessionId);
  }
  
  private MonitorSessionInstance createNewSessionInstance(String sessionId) {
    MonitorSessionInstance monitorSessionInstance = new MonitorSessionInstance();
    sessionManagement.addSessionTerminationHandler(sessionId, this::killSession);
    instanceMap.put(sessionId, monitorSessionInstance);
    return monitorSessionInstance;
  }
  
}
