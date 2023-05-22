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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xmcp.xguisupport.messagebus.Message;



public class MessageOutputParameter extends MessageBaseParameter {
  

  private static final long serialVersionUID = -7471034590865640291L;
  
  private Long id;
  private Set<Long> corraletedSubscriptions = new HashSet<Long>();
  
  
  public MessageOutputParameter(Long id, String product, String context, String correlation, String creator, List<SerializablePair<String, String>> payload) {
    super(product, context, correlation, creator, payload);
    this.id = id;
  }
  
  public MessageOutputParameter(Message message) {
    super(message);
    this.id = message.getId();
  }
  
  
  public void addCorrelatedSubscription(Long subscriptionId) {
    corraletedSubscriptions.add(subscriptionId);
  }
  
  public Set<Long> getCorrelatedSubscriptions() {
    return corraletedSubscriptions;
  }
  
  
  public Long getId() {
    return id;
  }

  
}
