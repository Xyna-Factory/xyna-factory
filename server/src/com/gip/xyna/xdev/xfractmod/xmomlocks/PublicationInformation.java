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

import java.util.HashMap;
import java.util.Map;


public class PublicationInformation {
  
  private Map<Long, PublicationEntry> entries;
  private Long currentMessageId;
  private Long currentPublicationId;
  
  public boolean hasPuplications() {
    return (entries != null && entries.size() > 0);
  }
  
  public void addPublication(Long publicationId, String payload, Long messageId) {
    if (entries == null) {
      entries = new HashMap<Long, PublicationInformation.PublicationEntry>();
    }
    entries.put(publicationId, new PublicationEntry(publicationId, payload));
    currentMessageId = messageId;
    currentPublicationId = publicationId;
  }
  
  
  public boolean isRevert(Long publicationId) {
    return (currentPublicationId != null && publicationId < currentPublicationId);
  }
  
  
  public boolean isRedo(Long publicationId) {
    return (entries != null && entries.containsKey(publicationId));
  }
  
  
  public Long getCurrentMessageId() {
    return currentMessageId;
  }
  
  
  public Long getCurrentPublicationId() {
    return currentPublicationId;
  }
  
  
  public PublicationEntry getPublicationEntry(Long publicationId) {
    return entries.get(publicationId);
  }
  
  
  void clearPuplications() {
    entries.clear();
    currentMessageId = null;
    currentPublicationId = null;
  }

  
  public static class PublicationEntry {
    
    private final Long publicationId;
    private final String payload;
    
    PublicationEntry(Long publicationId, String payload) {
      this.publicationId = publicationId;
      this.payload = payload;
    }
    
    
    public Long getPublicationId() {
      return publicationId;
    }
    
    
    public String getPayload() {
      return payload;
    }

  }
}
