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
package com.gip.xyna.xfmg.xopctrl.managedsessions;

import java.io.Serializable;
import java.util.List;

/**
 * ContainerClass for detailed session information
 * contains role, associated rights for that role, sessionId, startTime, lastInteraction
 *
 */
public class SessionDetails implements Serializable {

  private static final long serialVersionUID = 5256450627887692674L;
  
  private String sessionId;
  private String role;
  private List<String> rights;
  private long startTime;
  private long lastInteraction;
  private boolean multipleSessionsAllowed;
  private boolean forced;
  
  public SessionDetails(String sessionId, String role, List<String> rights, long startTime, long lastInteraction, boolean multipleSessionsAllowed, boolean forced) {
    super();
    this.sessionId = sessionId;
    this.role = role;
    this.rights = rights;
    this.startTime = startTime;
    this.lastInteraction = lastInteraction;
    this.multipleSessionsAllowed = multipleSessionsAllowed;
    this.forced = forced;
  }
  
  public String getSessionId() {
    return sessionId;
  }
  
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
  
  public List<String> getRights() {
    return rights;
  }
  
  public void setRights(List<String> rights) {
    this.rights = rights;
  }
  
  public long getStartTime() {
    return startTime;
  }
  
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }
  
  public long getLastInteraction() {
    return lastInteraction;
  }
  
  public void setLastInteraction(long lastInteraction) {
    this.lastInteraction = lastInteraction;
  }

  public boolean areMultipleSessionsAllowed() {
    return multipleSessionsAllowed;
  }

  public void setMultipleSessionsAllowed(boolean multipleSessionsAllowed) {
    this.multipleSessionsAllowed = multipleSessionsAllowed;
  }

  public boolean isForced() {
    return forced;
  }

  public void setForced(boolean forced) {
    this.forced = forced;
  }

}
