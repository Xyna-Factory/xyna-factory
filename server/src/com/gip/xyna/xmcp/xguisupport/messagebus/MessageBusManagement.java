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
package com.gip.xyna.xmcp.xguisupport.messagebus;


import java.util.Set;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;

/*
 * man stelle sich den messagebus als key-value store vor.
 * key = <product>.<context>.<correlation>
 * value = stringserializable<String, String>-liste
 * 
 * eine subscription gibt einen filter auf die correlation an, und den teilpfad des keys vor der correlation fest (<product>.<context>
 */
public class MessageBusManagement extends FunctionGroup implements MessageBusManagementPortal {
  
  public final static String DEFAULT_NAME = "Message Bus Management";
  
  private MessageTopic topic;
  private MessageBus bus;
  
  public MessageBusManagement() throws XynaException {
    super();
  }
  
  @Override
  protected void init() throws XynaException {
    this.topic = new QuickAndDirtyLocalMessageTopic();
    this.bus = new MessageBusImpl(topic);
  }
  

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  
  @Override
  protected void shutdown() throws XynaException {
    topic.disconnect();
  }
  
  public Long publish(MessageInputParameter message) throws XynaException {
    return topic.publish(message);
  }
  
  public boolean addSubscription(String subscriptionSessionId, MessageSubscriptionParameter subscription) {
    return bus.addSubscription(subscriptionSessionId, subscription);
  }
  
  public boolean cancelSubscription(String subscriptionSessionId, Long subscriptionId) {
    return bus.cancelSubscription(subscriptionSessionId, subscriptionId);
  }

  public MessageRetrievalResult fetchMessages(String subscriptionSessionId, Long lastReceivedId) {
    return bus.fetchMessages(subscriptionSessionId, lastReceivedId);
  }
  
  public Set<Message> fetchPersistentMessages(PredefinedMessagePath path) {
    return bus.fetchPersistentMessages(path);
  }
  
  public void removePersistentMessage(String product, String context, String correlation, Long messageId) {
    bus.removePersistentMessage(product, context, correlation, messageId);
  }

  
  // TODO currently not part of the interface, should it be?
  public MessageBus getMessageBus() {
    return bus;
  }
  
}
