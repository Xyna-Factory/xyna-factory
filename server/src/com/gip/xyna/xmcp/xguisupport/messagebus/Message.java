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
package com.gip.xyna.xmcp.xguisupport.messagebus;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.Pathable;
import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.TrappedPathLeaf;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;


public class Message implements Pathable, TrappedPathLeaf {
  

  private static final long serialVersionUID = -7471034590865640291L;
  
  private Long id;
  private boolean persistent = false;
  private String product;
  private String context;
  private String correlation;
  private String creator;
  private List<SerializablePair<String, String>> payload;
  private transient volatile Map<String, String> payloadAsMap;
  
  public Message(Long id, String product, String context, String correlation, String creator, List<SerializablePair<String, String>> payload, boolean persistent) {
    this.id = id;
    this.product = product;
    this.context = context;
    this.correlation = correlation;
    this.creator = creator;
    this.payload = payload;
    this.persistent = persistent;
  }
  
  
  public Message(Long id, MessageInputParameter input) throws XynaException {
    this(id,
         input.getProduct(),
         input.getContext(),
         input.getCorrelation(),
         input.getCreator(),
         input.getPayload(),
         input.isPersistent());
  }
  
  
  public Long getId() {
    return id;
  }
  
  public boolean isPersistent() {
    return persistent;
  }
  
  public String getProduct() {
    return product;
  }
  
  public String getContext() {
    return context;
  }
  
  public String getCorrelation() {
    return correlation;
  }
  
  public String getCreator() {
    return creator;
  }
  
  public List<SerializablePair<String, String>> getPayload() {
    return payload;
  }

  public Map<String, String> getPayloadAsMap() {
    if (payloadAsMap == null) {
      Map<String, String> payloadMap = new HashMap<String, String>();
      for (SerializablePair<String, String> pair : payload) {
        payloadMap.put(pair.getFirst(), pair.getSecond());
      }
      payloadAsMap = payloadMap;
    }
    return payloadAsMap;
  }
  
  public String[] getPath() {
    return new String[] {product, context, correlation};
  }

  public boolean isTrapBait() {
    return !persistent;
  }

  
}
