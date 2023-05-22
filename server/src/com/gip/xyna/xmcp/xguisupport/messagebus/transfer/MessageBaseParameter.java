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
package com.gip.xyna.xmcp.xguisupport.messagebus.transfer;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xmcp.xguisupport.messagebus.Message;


public class MessageBaseParameter implements Serializable {
  

  private static final long serialVersionUID = -7471034590865640291L;
  
  private String product;
  private String context;
  private String correlation;
  private String creator;
  private List<SerializablePair<String, String>> payload;
  
  
  public MessageBaseParameter(String product, String context, String correlation, String creator, List<SerializablePair<String, String>> payload) {
    this.product = product;
    this.context = context;
    this.correlation = correlation;
    this.creator = creator;
    this.payload = payload;
  }
  
  
  public MessageBaseParameter(Message message) {
    this(message.getProduct(), message.getContext(), message.getCorrelation(), message.getCreator(), message.getPayload());
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
  
  
}
