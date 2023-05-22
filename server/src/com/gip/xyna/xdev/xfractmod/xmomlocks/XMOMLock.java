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
package com.gip.xyna.xdev.xfractmod.xmomlocks;

import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;

class XMOMLock {
  
  private final Path path;
  private final String type;
  private final String user;
  private String sessionId;
  private long associatedLockMessage;
  private PublicationInformation publicationInformation;
  
  XMOMLock(Path path, String type, String user, String sessionId) {
    this.path = path;
    this.type = type;
    this.user = user;
    this.sessionId = sessionId;
    publicationInformation = new PublicationInformation();
  }

  public String getSessionId() {
    return sessionId;
  }
  
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
  
  public long getAssociatedLockMessage() {
    return associatedLockMessage;
  }
  
  public void setAssociatedLockMessage(long associatedLockMessage) {
    this.associatedLockMessage = associatedLockMessage;
  }
  
  public PublicationInformation getPublicationInformation() {
    return publicationInformation;
  }
  
  public Path getPath() {
    return path;
  }

  public String getType() {
    return type;
  }
  
  public String getUser() {
    return user;
  }
  
  
  
}
