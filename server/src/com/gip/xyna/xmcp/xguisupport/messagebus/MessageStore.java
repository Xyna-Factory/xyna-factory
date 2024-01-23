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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.LeafFilter;
import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.Pathable;
import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.Trap;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageOutputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;


public class MessageStore {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(MessageStore.class);
  
  private TrappedPathTree<Message> pathTree = new TrappedPathTree<Message>();
  private ReadWriteLock pathTreeLock = new ReentrantReadWriteLock(true);
  private long lastRecievedId = -1;
  private final Set<Long> messageIdsToIgnore = new HashSet<Long>();
  
  
  private final MessageTopic topic;
  
  public MessageStore(MessageTopic topic) {
    this.topic = topic;
    Thread thread = new Thread(new Runnable() {
      
      public void run() {
        while (MessageStore.this.topic.isConnected()) {
          try {
            refreshFromTopic();
          } catch (Throwable t) {
            Department.handleThrowable(t);
            logger.debug("Error while refreshing from topic", t);
            try {
              Thread.sleep(3000);
            } catch (InterruptedException e) {
              // ntbd
            }
          }
        }
      }
    });
    thread.setName("MessageStore");
    thread.setDaemon(true);
    thread.start();
  }
  
  
  public void refreshFromTopic() {
    List<Message> unreceivedMessages = topic.receive();
    pathTreeLock.writeLock().lock();
    try {
      for (Message message : unreceivedMessages) {
        if (!messageIdsToIgnore.contains(message.getId())) {
          pathTree.insert(message);
          // lastReceivedId is only important for reading persistent messages
          if (message.isPersistent() && lastRecievedId < message.getId()) {
            lastRecievedId = message.getId();
          }
        }
      }
      messageIdsToIgnore.clear();
    } finally {
      pathTreeLock.writeLock().unlock();
    }
  }
  
  
  public void addSubscription(MessageBusSubscriptionSession session, MessageBusSubscription subscription) {
    pathTreeLock.writeLock().lock();
    try {
      if (session.getMessageTrigger() == null) {
        session.setMessageTrigger(pathTreeLock.writeLock().newCondition());
      }
      try {
        Trap<Message> trap = generateTrap(session, subscription);
        subscription.setTrap(trap);
        pathTree.trap(subscription, trap);
        for (MessageOutputParameter messageOutputParameter : session.getTransientMessage()) {
          if (subscription.check(messageOutputParameter)) {
            messageOutputParameter.addCorrelatedSubscription(subscription.getId());
          }
        }
      } finally {
        session.getMessageTrigger().signalAll();
      }
    } finally {
      pathTreeLock.writeLock().unlock();
    }
  }
  
  
  public void cancelSubscription(MessageBusSubscription subscription) {
    pathTreeLock.writeLock().lock();
    try {
      pathTree.disarm(subscription, subscription.getTrap());
    } finally {
      pathTreeLock.writeLock().unlock();
    }
  }
  
 
  public MessageRetrievalResult fetchMessages(MessageBusSubscriptionSession session, Long lastReceivedId) {
    pathTreeLock.readLock().lock();
    try {
      long timeout = System.currentTimeMillis() + XynaProperty.MESSAGE_BUS_FETCH_TIMEOUT.getMillis();
      Set<MessageOutputParameter> result = new HashSet<MessageOutputParameter>();
      fetching: while (result.size() == 0 && timeout > System.currentTimeMillis()) {
        result.addAll(session.getTransientMessage());
        Map<Long, MessageOutputParameter> persistentMessages = new HashMap<Long, MessageOutputParameter>();
        for (MessageBusSubscription subscription : session.getSubscriptions()) {
          if (subscription.hasUnreceivedMessages()) {
            mergeMessagesIntoMap(persistentMessages, subscription.getId(), pathTree.getLeafs(subscription), lastReceivedId, subscription.forceReadPersistent());
            subscription.setUnreceivedMessages(false);
            subscription.setForceReadPersistent(false);
          }
        }
        result.addAll(persistentMessages.values());
        if (result.size() == 0) {
          try {
            if (session.getMessageTrigger() == null) {
              session.setMessageTrigger(pathTreeLock.writeLock().newCondition());
            }
            pathTreeLock.readLock().unlock(); // upgrade lock for await
            try {
              pathTreeLock.writeLock().lock();
              try {
                for (MessageBusSubscription subscription : session.getSubscriptions()) { // check conditions as we might have missed messages while upgrading
                  if (subscription.hasUnreceivedMessages() || session.getTransientMessage().size() > 0) {
                    continue fetching;
                  }
                }
                session.getMessageTrigger().await(timeout - System.currentTimeMillis() , TimeUnit.MILLISECONDS);
              } finally {
                pathTreeLock.writeLock().unlock();
              }
            } finally {
              pathTreeLock.readLock().lock();
            }
          } catch (InterruptedException e) {
            // aye, await shorter
          }
        }
      }
      session.clearTransientMessages();
      return new MessageRetrievalResult(result, lastRecievedId);
    } finally {
      pathTreeLock.readLock().unlock();
    }
  }
  
  
  public Set<Message> fetchPersistentMessages(final Pathable path) {
    pathTreeLock.readLock().lock();
    try {
      return pathTree.getLeafs(path);
    } finally {
      pathTreeLock.readLock().unlock();
    }
  }
  
  
  public void removePersistentMessage(final String product, final String context, final String correlation, final Long messageId) {
    pathTreeLock.writeLock().lock();
    try {
      pathTree.removeLeaf(new Pathable() {
        public String[] getPath() {
          return new String[] {product, context, correlation};
        }
      }, new LeafFilter<Message>() {

        public boolean accept(Message leaf) {
          boolean result = leaf.getId().equals(messageId);
          return result;
        }
        
      });
      messageIdsToIgnore.add(messageId);
      topic.removeMessage(messageId);
    } finally {
      pathTreeLock.writeLock().unlock();
    }
  }
  
  
  private Trap<Message> generateTrap(final MessageBusSubscriptionSession session, final MessageBusSubscription subscription) {
    return new Trap<Message>() {
      public void trigger(Message message) {
        if (subscription.check(message)) {
          if (message.isPersistent()) {
            subscription.setUnreceivedMessages(true);
          } else {
            session.addTransientMessage(subscription.getId(), message);
          }
          Condition condi = session.getMessageTrigger();
          if (condi != null) {
            condi.signalAll();
          }
        }
      }
    };
  }
  
  
  void signal(MessageBusSubscriptionSession session) {
    pathTreeLock.writeLock().lock();
    try {
      Condition condi = session.getMessageTrigger();
      if (condi != null) {
        condi.signalAll();
      }
    } finally {
      pathTreeLock.writeLock().unlock();
    }
  }
  
  
  static void mergeMessagesIntoMap(Map<Long, MessageOutputParameter> emergingMap, Long currentSubscriptionId,
                                   Collection<Message> currentSubscriptionResult, Long lastReceivedId, boolean forceReadPersistent) {
    for (Message message : currentSubscriptionResult) {
      if (forceReadPersistent || lastReceivedId < message.getId()) {
        mergeMessageIntoMap(emergingMap, currentSubscriptionId, message);
      }
    }
  }
  
  static void mergeMessageIntoMap(Map<Long, MessageOutputParameter> emergingMap, Long currentSubscriptionId, Message message) {
    MessageOutputParameter entry = emergingMap.get(message.getId());
    if (entry == null) {
      entry = new MessageOutputParameter(message);
    }
    entry.addCorrelatedSubscription(currentSubscriptionId);
    emergingMap.put(message.getId(), entry);
  }
  
  
  
}
