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

package com.gip.xyna.xact.filter.session.messagebus;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.SessionBasedData;
import com.gip.xyna.xact.filter.session.messagebus.events.XmomTypeEvent;
import com.gip.xyna.xact.filter.session.messagebus.events.XmomTypeEvent.XmomType;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringManagement;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagementPortal;
import com.gip.xyna.xmcp.xguisupport.messagebus.PredefinedMessagePath;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageOutputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;

import xmcp.yggdrasil.DocumentChange;
import xmcp.yggdrasil.DocumentLock;
import xmcp.yggdrasil.DocumentUnlock;
import xmcp.yggdrasil.Event;
import xmcp.yggdrasil.ProjectEvent;
import xmcp.yggdrasil.XMOMChangedRTCDependencies;
import xmcp.yggdrasil.XMOMCreateRTC;
import xmcp.yggdrasil.XMOMDelete;
import xmcp.yggdrasil.XMOMDeleteRTC;
import xmcp.yggdrasil.XMOMSave;
import xprc.xpce.RuntimeContext;



public class PollEventFetcher {

  private static final long FETCH_DELAY = 100L; // millis
  private static final String ALL_FILTER = ".*";
  private static final String PRODUCT_XYNA = "Xyna";

  private static final AtomicLong pollingThreadIdGenerator = new AtomicLong();
  private final String sessionId;
  private final Map<String, List<Event>> pollRequestUUIDToEventsMap;
  private final Map<String, Boolean> pollRequestUUIDToIsProject;
  private final ConcurrentMap<XmomType, XmomTypeEvent> locks;
  private final SessionBasedData sessionBasedData;
  private final Map<String, Set<Long>> correlationToSubscriptionIds = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> correlationToRequestUUIDs = new ConcurrentHashMap<>();
  private static final AtomicLong subscriptionIdGenerator = new AtomicLong();
  private long lastReceivedId = -1;
  private volatile boolean terminate = false;

  private static final MessageBusManagementPortal messageBusManagementPortal = XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement();

  private enum SearchField { FIRST, SECOND }


  public PollEventFetcher(String sessionId, Map<String, List<Event>> pollRequestUUIDToEventsMap, Map<String, Boolean> pollRequestUUIDToIsProject, ConcurrentMap<XmomType, XmomTypeEvent> locks, SessionBasedData responsibleSession) {
    this.sessionId = sessionId;
    this.pollRequestUUIDToEventsMap = pollRequestUUIDToEventsMap;
    this.pollRequestUUIDToIsProject = pollRequestUUIDToIsProject;
    this.locks = locks;
    this.sessionBasedData = responsibleSession;

    addSubscription(ALL_FILTER, PredefinedMessagePath.XYNA_MODELLER_SAVE);
    addSubscription(ALL_FILTER, PredefinedMessagePath.XYNA_MODELLER_DELETE);
    addSubscription(ALL_FILTER, PredefinedMessagePath.XYNA_MODELLER_UPDATE);
    addSubscription(ALL_FILTER, PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_CREATE);
    addSubscription(ALL_FILTER, PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_DELETE);
    addSubscription(ALL_FILTER, PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_UPDATE);

    Thread messageFetcher = new Thread("Message bus fetcher thread " + pollingThreadIdGenerator.getAndIncrement()) {
      @Override
      public void run() {
        while (!terminate) {
          try {
            Map<String, Set<Long>> curCorrelationToSubscriptionIds;
            synchronized (pollRequestUUIDToEventsMap) {
              curCorrelationToSubscriptionIds = new HashMap<>(correlationToSubscriptionIds);
            }

            MessageRetrievalResult result = messageBusManagementPortal.fetchMessages(sessionId, lastReceivedId);
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
              synchronized (pollRequestUUIDToEventsMap) {
                for (Entry<String, Set<Long>> correlationToSubscriptionId : correlationToSubscriptionIds.entrySet()) {
                  curCorrelationToSubscriptionIds.put(correlationToSubscriptionId.getKey(), correlationToSubscriptionId.getValue()); // TODO: Warum? Das wurde doch gerade erst kopiert.
                }
              }

              result.getMessages().forEach(x -> PollEventFetcher.this.messageReceived(x, curCorrelationToSubscriptionIds));
              lastReceivedId = result.getLastCheckedId();
            }
          } catch (Exception e) {
            Utils.logError("Multiuser: Failed to poll messages for session " + sessionId, e);
          }

          try {
            Thread.sleep(FETCH_DELAY);
          } catch (Exception e) {
            Utils.logError("Multiuser: Message polling thread for session " + sessionId + " died", e);
            break;
          }
        }

        terminate = false;
      }
    };

    messageFetcher.setDaemon(true);
    messageFetcher.start();
  }


  private void messageReceived(MessageOutputParameter message, Map<String, Set<Long>> curCorrelationToSubscriptionIds) {
    if (Objects.equals(message.getProduct(), PRODUCT_XYNA)) {
      xynaMessageReceived(message, curCorrelationToSubscriptionIds);
    } else {
      projectMessageReceived(message, curCorrelationToSubscriptionIds);
    }
  }

  private void xynaMessageReceived(MessageOutputParameter message, Map<String, Set<Long>> curCorrelationToSubscriptionIds) {
    if (curCorrelationToSubscriptionIds.keySet().contains(message.getCorrelation())) {
      for (Long subscriptionId : curCorrelationToSubscriptionIds.get(message.getCorrelation())) {
        if (message.getCorrelatedSubscriptions().contains(subscriptionId)) {
          documentMessageReceived(message, PredefinedMessagePath.byContext(message.getContext()));
          return;
        }
      }
    }

    if (curCorrelationToSubscriptionIds.keySet().contains(ALL_FILTER)) {
      for (Long subscriptionId : curCorrelationToSubscriptionIds.get(ALL_FILTER)) {
        if (message.getCorrelatedSubscriptions().contains(subscriptionId)) {
          globalMessageReceived(message, PredefinedMessagePath.byContext(message.getContext()));
          return;
        }
      }
    }
  }
  

  private void documentMessageReceived(MessageOutputParameter message, PredefinedMessagePath messagePath) {
    XmomTypeEvent event = new XmomTypeEvent(message);
    Event newPollEvent = null;

    try {
      switch (messagePath) {
        case XYNA_MODELLER_LOCKS:
          locks.put(event.getXmomType(), event);
          newPollEvent = new DocumentLock(message.getCreator(), event.getXmomType().getName(), Utils.getXpceRtc(event.getXmomType().getRtc()));
          break;
        case XYNA_MODELLER_UNLOCKS: 
          locks.remove(event.getXmomType());
          newPollEvent = new DocumentUnlock(message.getCreator(), event.getXmomType().getName(), Utils.getXpceRtc(event.getXmomType().getRtc()));
          break;
        case XYNA_MODELLER_AUTOSAVES:
          newPollEvent = new DocumentChange(message.getCreator(), event.getXmomType().getName(), Utils.getXpceRtc(event.getXmomType().getRtc()));
          FQName fqName = new FQName(event.getXmomType().getRtc(), event.getXmomType().getName());

          if (!Objects.equals(sessionBasedData.getSession().getUser(), message.getCreator())) {
            synchronized (sessionBasedData) {
              List<String> documents = findInPayload(message, LockManagement.MESSAGE_PAYLOAD_KEY_DOCUMENT, SearchField.FIRST);
              if (!documents.isEmpty()) {
                if (documents.size() > 1) {
                  Utils.logError("Multiuser: Unexpected number of documents for update received: " + documents, null);
                }

                // store new version of document in internal cache
                sessionBasedData.reloadGbo(fqName, documents.get(0));
              } else {
                List<String> autosaveCounters = findInPayload(message, LockManagement.MESSAGE_PAYLOAD_KEY_PUBLICATION_ID, SearchField.FIRST);
                if (!autosaveCounters.isEmpty()) {
                  if (autosaveCounters.size() > 1) {
                    Utils.logError("Multiuser: Unexpected number of autosave counters for update received: " + autosaveCounters, null);
                  }

                  // store new version of document in internal cache
                  sessionBasedData.reloadGbo(fqName);
                }
              }
            }
          }
          break;
        default:
          Utils.logError("Multiuser: Unhandled document message received: " + messagePath, null);
          break;
      }

      if (newPollEvent != null) {
        addToPollEvents(newPollEvent, false);
      }
    } catch (Exception e) {
      Utils.logError("Multiuser: Could not create poll event for " + event.getXmomType().getName(), e);
    }
  }


  private void projectMessageReceived(MessageOutputParameter message, Map<String, Set<Long>> curCorrelationToSubscriptionIds) {
    String json = message.getPayload().get(0).getSecond();
    ProjectEvent projectEvent = (ProjectEvent)Utils.convertJsonToGeneralXynaObject(json);
    addToPollEvents(projectEvent, true);
  }


  private void addToPollEvents(Event newPollEvent, boolean projectPoll) {
    synchronized (pollRequestUUIDToEventsMap) {
      for (Entry<String, List<Event>> pollRequestMapEntry : pollRequestUUIDToEventsMap.entrySet()) {
        if (pollRequestUUIDToIsProject.get(pollRequestMapEntry.getKey()) == projectPoll) {
          pollRequestMapEntry.getValue().add(newPollEvent);
        }
      }
    }
  }

  private void globalMessageReceived(MessageOutputParameter message, PredefinedMessagePath messagePath) {
    XmomTypeEvent event = new XmomTypeEvent(message);
    List<Event> newPollEvents = new ArrayList<>();

    try {
      switch (messagePath) {
        case XYNA_MODELLER_SAVE:
          newPollEvents.add(new XMOMSave(message.getCreator(), event.getXmomType().getName(), Utils.getXpceRtc(event.getXmomType().getRtc())));
          break;
        case XYNA_MODELLER_DELETE:
          newPollEvents.add(new XMOMDelete(message.getCreator(), event.getXmomType().getName(), Utils.getXpceRtc(event.getXmomType().getRtc())));
          break;
        case XYNA_MODELLER_UPDATE:
          RuntimeContext rtc = Utils.getXpceRtc(event.getXmomType().getRtc());
          for (String oldFqn : findInPayload(message, RefactoringManagement.DELETED, SearchField.SECOND)) {
            newPollEvents.add(new XMOMDelete(message.getCreator(), oldFqn, rtc));
          }

          for (String newFqn : findInPayload(message, RefactoringManagement.SAVED, SearchField.SECOND)) {
            newPollEvents.add(new XMOMSave(message.getCreator(), newFqn, rtc));
          }
          break;
        case XYNA_RUNTIME_CONTEXT_CREATE:
          if (event.getXmomType().getRtc() != null) {
            newPollEvents.add(new XMOMCreateRTC(Utils.getXpceRtc(event.getXmomType().getRtc())));
          }
          break;
        case XYNA_RUNTIME_CONTEXT_DELETE:
          if (event.getXmomType().getRtc() != null) {
            newPollEvents.add(new XMOMDeleteRTC(Utils.getXpceRtc(event.getXmomType().getRtc())));
          }
          break;
        case XYNA_RUNTIME_CONTEXT_UPDATE:
          if (event.getXmomType().getRtc() != null) {
            newPollEvents.add(new XMOMChangedRTCDependencies(Utils.getXpceRtc(event.getXmomType().getRtc())));
          }
          break;
        default:
          Utils.logError("Multiuser: Unhandled global message received: " + messagePath, null);
          break;
      }

      for (Event newPollEvent : newPollEvents) {
        addToPollEvents(newPollEvent, false);
      }
    } catch (Exception e) {
      Utils.logError("Multiuser: Could not create poll event for " + event.getXmomType().getName(), e);
    }
  }


  private List<String> findInPayload(MessageOutputParameter message, String searchString, SearchField searchField) {
    List<String> results = new ArrayList<>();
    if (message == null || message.getPayload() == null) {
      return results;
    }

    for (SerializablePair<String, String> payloadElement : message.getPayload()) {
      if ( (searchField == SearchField.FIRST && Objects.equals(payloadElement.getFirst(), searchString)) ||
           (searchField == SearchField.SECOND && Objects.equals(payloadElement.getSecond(), searchString)) ) {
        results.add(searchField == SearchField.FIRST ? payloadElement.getSecond() : payloadElement.getFirst());
      }
    }

    return results;
  }


  public void documentOpened(GenerationBaseObject gbo) {
    try {
      if (gbo.getRuntimeContext() instanceof Application) {
        // documents in applications can't be changed -> no subscriptions necessary
        return;
      }

      Path path = new Path(gbo.getFQName().getFqName(), gbo.getGenerationBase().getRevision());
      String correlationId = LockManagement.createCorrelation(path, gbo.getType().getNiceName());
      if (!gbo.getSaveState() || correlationToSubscriptionIds.keySet().contains(correlationId)) {
        return;
      }

      addSubscription(correlationId, PredefinedMessagePath.XYNA_MODELLER_LOCKS);
      addSubscription(correlationId, PredefinedMessagePath.XYNA_MODELLER_UNLOCKS);
      addSubscription(correlationId, PredefinedMessagePath.XYNA_MODELLER_AUTOSAVES);
    } catch (Exception e) {
      Utils.logError("Multiuser: Could not add message bus subscriptions for " + gbo.getFQName().getFqName(), e);
    }
  }


  public void documentClosed(GenerationBaseObject gbo) {
    if (gbo.getRuntimeContext() instanceof Application) {
      // documents in applications can't be changed -> no subscriptions necessary
      return;
    }

    Path path = new Path(gbo.getFQName().getFqName(), gbo.getGenerationBase().getRevision());
    String correlationId;
    try {
      synchronized(pollRequestUUIDToEventsMap) {
        correlationId = LockManagement.createCorrelation(path, gbo.getType().getNiceName());
        if (!correlationToSubscriptionIds.keySet().contains(correlationId)) {
          return;
        }
  
        removeSubscription(correlationId);
      }
    } catch (Exception e) {
      Utils.logError("Multiuser: Could not cancel message bus subscriptions for " + gbo.getFQName().getFqName(), e);
    }
  }


  private void addSubscription(String correlationId, PredefinedMessagePath messagePath) {
    addSubscription(correlationId, messagePath.getProduct(), messagePath.getContext());
  }


  private void addSubscription(String correlationId, String product, String context) {
    synchronized(pollRequestUUIDToEventsMap) {
      Set<Long> subscriptionIds;
      if (!correlationToSubscriptionIds.containsKey(correlationId)) {
        subscriptionIds = ConcurrentHashMap.newKeySet();
        correlationToSubscriptionIds.put(correlationId, subscriptionIds);
      } else {
        subscriptionIds = correlationToSubscriptionIds.get(correlationId);
      }

      MessageSubscriptionParameter subscriptionParameter = new MessageSubscriptionParameter(subscriptionIdGenerator.getAndIncrement(), product, context, correlationId);
      subscriptionIds.add(subscriptionParameter.getId());
      messageBusManagementPortal.addSubscription(sessionId, subscriptionParameter);
    }
  }


  private void removeSubscription(String correlationId) {
    if (!correlationToSubscriptionIds.containsKey(correlationId)) {
      return;
    }

    for (Long subscriptionId : correlationToSubscriptionIds.get(correlationId)) {
      messageBusManagementPortal.cancelSubscription(sessionId, subscriptionId);
    }

    correlationToSubscriptionIds.remove(correlationId);
  }


  public void addProjectPolling(String requestUuid, String correlationId, String product, String context) {
    synchronized(pollRequestUUIDToEventsMap) {
      Set<String> requestUuids;
      if (!correlationToRequestUUIDs.containsKey(correlationId)) {
        requestUuids = new HashSet<String>();
        correlationToRequestUUIDs.put(correlationId, requestUuids);
      } else {
        requestUuids = correlationToRequestUUIDs.get(correlationId);
      }

      if (requestUuids.isEmpty()) {
        // for this correlation-id, no project subscription exists for a browser tab, yet -> subscribe
        addSubscription(correlationId, product, context);
      }

      // add this browser tab to the list of tabs that need this subscription
      requestUuids.add(requestUuid);
    }
  }


  public void cancelProjectPolling(String pollRequestUuid, String correlation) {
    synchronized(pollRequestUUIDToEventsMap) {
      if (correlationToRequestUUIDs.containsKey(correlation)) {
        // remove this browser tab from the list of tabs that need this subscription
        correlationToRequestUUIDs.get(correlation).remove(pollRequestUuid);
      }

      // if no tabs for the user need updates for the subscription, anymore, unsubscribe it from the message bus
      Set<String> remainingUUIDs = correlationToRequestUUIDs.get(correlation);
      if (remainingUUIDs == null || remainingUUIDs.size() == 0) {
        removeSubscription(correlation);
        correlationToRequestUUIDs.remove(correlation);
      }
    }
  }


  public void stop() {
    terminate = true;
  }

}
