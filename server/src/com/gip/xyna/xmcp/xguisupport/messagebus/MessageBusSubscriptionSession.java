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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;

import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageOutputParameter;

public class MessageBusSubscriptionSession {

  private String sessionId;
  private ConcurrentMap<Long, MessageBusSubscription> subscriptions = new ConcurrentHashMap<Long, MessageBusSubscription>();
  private Condition messageTrigger;
  private Map<Long, MessageOutputParameter> transientMessages = new HashMap<Long, MessageOutputParameter>();
  
  public MessageBusSubscriptionSession() {
  }
  
  boolean addSubscription(MessageBusSubscription subscription) {
    return subscriptions.putIfAbsent(subscription.getId(), subscription) == null;
  }
  
  boolean removeSubscription(Long subscriptionId) {
    return subscriptions.remove(subscriptionId) != null;
  }
  
  Collection<MessageBusSubscription> getSubscriptions() {
    return subscriptions.values();
  }
  
  MessageBusSubscription getSubscription(Long subscriptionId) {
    return subscriptions.get(subscriptionId);
  }

  void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
  
  String getSessionId() {
    return this.sessionId;
  }
  
  void setMessageTrigger(Condition messageTrigger) {
    this.messageTrigger = messageTrigger;
  }
  
  Condition getMessageTrigger() {
    return messageTrigger;
  }
  
  synchronized void addTransientMessage(Long subscriptionId, Message message) {
    MessageStore.mergeMessageIntoMap(transientMessages, subscriptionId, message);
  }
  
  synchronized Collection<MessageOutputParameter> getTransientMessage() {
    return transientMessages.values();
  }
  
  synchronized void clearTransientMessages() {
    transientMessages.clear();
  }
  
  
  public synchronized boolean directReply(Message message) {
    for (MessageBusSubscription subscription : subscriptions.values()) {
      if (subscription.check(message)) {
        addTransientMessage(subscription.getId(), message);
        return true;
      }
    }
    return false;
  }

}
