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
package com.gip.xyna.xmcp.xguisupport.messagebus;

public enum PredefinedMessagePath {
  XYNA_MODELLER_LOCKS(new String[] {"Xyna", "Process Modeller Locks"}, true),
  XYNA_MODELLER_UNLOCKS(new String[] {"Xyna", "Process Modeller Unlocks"}, false),
  XYNA_MODELLER_AUTOSAVES(new String[] {"Xyna", "Process Modeller Autosaves"}, true),
  XYNA_MODELLER_CHAT(new String[] {"Xyna", "Process Modeller Chat"}, false),
  XYNA_MODELLER_DELETE(new String[] {"Xyna", "Process Modeller Delete XMOM"}, false),
  XYNA_MODELLER_SAVE(new String[] {"Xyna", "Process Modeller Save XMOM"}, false),
  XYNA_MONITOR_UPDATES(new String[] {"Xyna", "Process Monitor Audit Updates"}, false),
  XYNA_MODELLER_UPDATE(new String[] {"Xyna", "Process Modeller Update XMOM"}, false),
  XYNA_RUNTIME_CONTEXT_CREATE(new String[] {"Xyna", "RuntimeContext Create"}, false),
  XYNA_RUNTIME_CONTEXT_UPDATE(new String[] {"Xyna", "RuntimeContext Update"}, false),
  XYNA_RUNTIME_CONTEXT_DELETE(new String[] {"Xyna", "RuntimeContext Delete"}, false);
  
  private final String[] path;
  private final boolean persistent;
  
  private PredefinedMessagePath(String[] path, boolean persistent) {
    this.path = path;
    this.persistent = persistent;
  }
  
  public String[] getPath() {
    return path;
  }
  
  public String getProduct() {
    return path[0];
  }
  
  public String getContext() {
    return path[1];
  }
  
  public boolean isPersistent() {
    return persistent;
  }

  public static PredefinedMessagePath byContext(String context) {
    for (PredefinedMessagePath messagePath : PredefinedMessagePath.values()) {
      if (messagePath.getContext().equals(context)) {
        return messagePath;
      }
    }

    return null;
  }

}
