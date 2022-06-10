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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement.SessionFinalizationHandler;
import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.Pathable;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;


public class MessageBusImpl implements MessageBus, SessionFinalizationHandler {
  
  private final MessageStore store;
  private ConcurrentMap<String, MessageBusSubscriptionSession> activeSessions = new ConcurrentHashMap<String, MessageBusSubscriptionSession>();
  
  
  public MessageBusImpl(MessageTopic topic) {
    store = new MessageStore(topic);
  }
  

  public boolean addSubscription(String subscriptionSessionId, MessageSubscriptionParameter subscription) {
    MessageBusSubscription sub = new MessageBusSubscription(subscription.getId(), subscription.getProduct(), subscription.getContext(), subscription.getFilter());
    MessageBusSubscriptionSession session = getSessionLazyCreate(subscriptionSessionId);
    if (!session.addSubscription(sub)) {
      return false;
    } else {
      store.addSubscription(session, sub);
      return true;
    }
  }

  
  public boolean cancelSubscription(String subscriptionSessionId, Long subscriptionId) {
    MessageBusSubscriptionSession session = getSessionLazyCreate(subscriptionSessionId);
    MessageBusSubscription sub = session.getSubscription(subscriptionId);
    if (sub == null) {
      return false;
    } else {
      store.cancelSubscription(sub);
      return true;
    }
  }
  
  
  public MessageRetrievalResult fetchMessages(String subscriptionSessionId, Long lastReceivedId) {
    MessageBusSubscriptionSession session = getSessionLazyCreate(subscriptionSessionId);
    return store.fetchMessages(session, lastReceivedId);
  }
  
  
  public Set<Message> fetchPersistentMessages(final PredefinedMessagePath path) {
    if (path.isPersistent()) {
      return store.fetchPersistentMessages(new Pathable() {
        public String[] getPath() {
          return path.getPath();
        }
      });
    } else {
      return Collections.emptySet();
    }
  }

  
  private MessageBusSubscriptionSession getSessionLazyCreate(String subscriptionSessionId) {
    MessageBusSubscriptionSession session = getSession(subscriptionSessionId);
    if (session == null) {
      session = new MessageBusSubscriptionSession();
      if (activeSessions.putIfAbsent(subscriptionSessionId, session) == null) {
        session = activeSessions.get(subscriptionSessionId);
        if (XynaFactory.isFactoryServer()) { // to ease testing
          XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement().addSessionTerminationHandler(subscriptionSessionId, this);
        }
      }
    }
    return session;
  }
  
  
  public void abortFetchers() {
    for (MessageBusSubscriptionSession session : activeSessions.values()) {
      long uid;
      try {
        uid = IDGenerator.getInstance().getUniqueId();
      } catch (XynaException e) {
        throw new RuntimeException(e);
      }
      session.addTransientMessage(uid, new Message(uid, "Xyna", "Internal", "Abort", "XYNAADMIN", new ArrayList<SerializablePair<String, String>>(), false));
      store.signal(session);
    }
  }
  
  
  public MessageBusSubscriptionSession getSession(String subscriptionSessionId) {
    return activeSessions.get(subscriptionSessionId);
  }


  public void handleSessionFinalization(String sessionId) {
    MessageBusSubscriptionSession session = activeSessions.remove(sessionId);
    for (MessageBusSubscription subscription : session.getSubscriptions()) {
      store.cancelSubscription(subscription);
    }
  }


  public void removePersistentMessage(String product, String context, String correlation, Long messageId) {
    store.removePersistentMessage(product, context, correlation, messageId);
  }
  
  
  public boolean signal(String subscriptionSessionId) {
    MessageBusSubscriptionSession session = getSession(subscriptionSessionId);
    if (session == null) {
      return false;
    } else {
      store.signal(session);
      return true;
    }
  }


}
