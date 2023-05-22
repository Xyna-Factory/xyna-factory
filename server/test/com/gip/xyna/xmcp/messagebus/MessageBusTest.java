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
package com.gip.xyna.xmcp.messagebus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBase;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBus;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusImpl;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageTopic;
import com.gip.xyna.xmcp.xguisupport.messagebus.QuickAndDirtyLocalMessageTopic;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;



public class MessageBusTest extends TestCase {

  private MessageBus messageBus;
  private MessageTopic topic;
  private AtomicLong subscriptionIdGenerator; 
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    topic = new QuickAndDirtyLocalMessageTopic() {
      @Override
      public long getUniqueMessageId() throws com.gip.xyna.utils.exceptions.XynaException {
        return subscriptionIdGenerator.incrementAndGet();
      };
    };
    messageBus = new MessageBusImpl(topic);
    subscriptionIdGenerator = new AtomicLong();
    Field defaultValueField = XynaPropertyBase.class.getDeclaredField("defValue");
    defaultValueField.setAccessible(true);
    defaultValueField.set(XynaProperty.MESSAGE_BUS_FETCH_TIMEOUT, 1000L);
    XynaPropertyUtils.exchangeXynaPropertySource(new AbstractXynaPropertySource() {
      public String getProperty(String name) {
        return null;
      }
    });
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    topic.disconnect();
    topic = null;
    messageBus = null;
    subscriptionIdGenerator = null;
  }
  
  
  private final static String DEFAULT_PRODUCT = "Xyna";
  private final static String DEFAULT_CONTEXT = "Chat";
  private final static String DEFAULT_PAYLOAD_KEY = "Text";
  private final static String DEFAULT_PAYLOAD_VALUE = "Jemand musste Josef K. verleumdet haben, denn ohne dass er etwas B�ses getan h�tte, wurde er eines Morgens verhaftet.";
  private final static List<SerializablePair<String, String>> DEFAULT_PAYLOAD = getDefaultPayload();
  
  private static List<SerializablePair<String, String>> getDefaultPayload() {
    List<SerializablePair<String, String>> payload = new ArrayList<SerializablePair<String,String>>();
    payload.add(new SerializablePair<String, String>(DEFAULT_PAYLOAD_KEY, DEFAULT_PAYLOAD_VALUE));
    return payload;
  }
  
  private static List<SerializablePair<String, String>> generatePayload(String value) {
    List<SerializablePair<String, String>> payload = new ArrayList<SerializablePair<String,String>>();
    payload.add(new SerializablePair<String, String>(DEFAULT_PAYLOAD_KEY, value));
    return payload;
  }
  
  private final AtomicLong SUBSCRIPTION_ID_GENERATOR = new AtomicLong();
  
  private MessageInputParameter getDefaultMessageParameter(boolean persistent) {
    return new MessageInputParameter(DEFAULT_PRODUCT, DEFAULT_CONTEXT, "test", "testMessagePuplication", DEFAULT_PAYLOAD, persistent);
  }
  
  private MessageSubscriptionParameter getDefaultMessageSubscriptionParameter() {
    return new MessageSubscriptionParameter(SUBSCRIPTION_ID_GENERATOR.incrementAndGet(), DEFAULT_PRODUCT, DEFAULT_CONTEXT, ".*");
  }
  
  
  @Test
  public void testSessionManipulation() {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    assertFalse("Same subscription should not have been added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    assertTrue("Subscription should have been succesfully cancelled", messageBus.cancelSubscription(uniqueSessionId, uniqueSubscription.getId()));
    assertTrue("Subscription should not have been succesfully cancelled as it should have already been removed",
               messageBus.cancelSubscription(uniqueSessionId, uniqueSubscription.getId()));
  }
  
  
  @Test
  public void testFetchAfterTransientMessagePuplication() throws XynaException, InterruptedException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
    
    MessageInputParameter message = getDefaultMessageParameter(false);
    topic.publish(message);
    
    retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
  }
  
  
  @Test
  public void testFetchAfterPersistentMessagePuplication() throws XynaException, InterruptedException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
    
    MessageInputParameter message = getDefaultMessageParameter(true);
    topic.publish(message);
    
    retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
  }
  
  
  @Test
  public void testPersientReceiveWithLateSubscription() throws XynaException, InterruptedException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    
    MessageInputParameter message = getDefaultMessageParameter(true);
    topic.publish(message);
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    
    topic.publish(message);
    
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    
    MessageSubscriptionParameter laterSubscription = getDefaultMessageSubscriptionParameter();
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription("mySession2", laterSubscription));
    
    retrieval = messageBus.fetchMessages("mySession2", -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been two messages", 2, retrieval.getMessages().size());
  }
  
  
  @Test
  public void testTransientReceiveWithLateSubscription() throws XynaException, InterruptedException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    
    MessageInputParameter message = getDefaultMessageParameter(false);
    topic.publish(message);
    
    Thread.sleep(500);
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
    
    topic.publish(message);
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
  }
  
  
  @Test
  public void testTransientFetchAndSubscriptionCancel() throws XynaException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    
    MessageInputParameter message = getDefaultMessageParameter(false);
    topic.publish(message);
    
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    
    assertTrue("Subscription should have been succesfully canceled", messageBus.cancelSubscription(uniqueSessionId, uniqueSubscription.getId()));
    topic.publish(message);
    
    retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
    
  }
  
  @Test
  public void testPersistentFetchAndSubscriptionCancel() throws XynaException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    
    topic.publish(getDefaultMessageParameter(true));
    
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    
    assertTrue("Subscription should have been succesfully canceled", messageBus.cancelSubscription(uniqueSessionId, uniqueSubscription.getId()));
    topic.publish(getDefaultMessageParameter(true));
    
    retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
  }
  
  
  @Test
  public void testReceiveFromDuplicateSubscriptions() throws XynaException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter firstSubscription = getDefaultMessageSubscriptionParameter();
    final MessageSubscriptionParameter secondSubscription = getDefaultMessageSubscriptionParameter();
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, firstSubscription));
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, secondSubscription));
    
    topic.publish(getDefaultMessageParameter(true));
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    assertTrue("Message should have been received from first subscription", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(firstSubscription.getId()));
    assertTrue("Message should have been received from second subscription", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(secondSubscription.getId()));
    
    topic.publish(getDefaultMessageParameter(false));
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    assertTrue("Message should have been received from first subscription", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(firstSubscription.getId()));
    assertTrue("Message should have been received from second subscription", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(secondSubscription.getId()));
  }
  
  @Test
  public void testStaggeredReceiveFromDuplicateSubscriptions() throws XynaException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter firstSubscription = getDefaultMessageSubscriptionParameter();
    final MessageSubscriptionParameter secondSubscription = getDefaultMessageSubscriptionParameter();
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, firstSubscription));
    //
    
    topic.publish(getDefaultMessageParameter(true));
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    assertTrue("Message should have been received from first subscription", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(firstSubscription.getId()));
    //assertTrue("Message should have been received from second subscription", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(secondSubscription.getId()));
    
    System.out.println(retrieval.getMessages().iterator().next().getId());
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, secondSubscription));
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getMessages().iterator().next().getId() +1);
    
    List<?> baum = retrieval.getMessages();
    System.out.println(baum.size());
  }
  
  
  @Test
  public void testFuzzyCorrelationFilters() throws XynaException {
    final String uniqueSessionId = "mySession";
    MessageSubscriptionParameter startingWithBSub = new MessageSubscriptionParameter(subscriptionIdGenerator.incrementAndGet(), DEFAULT_PRODUCT, DEFAULT_CONTEXT, "^[bB].*");
    MessageSubscriptionParameter endingWithMSub = new MessageSubscriptionParameter(subscriptionIdGenerator.incrementAndGet(), DEFAULT_PRODUCT, DEFAULT_CONTEXT, ".*[mM]$");
    MessageSubscriptionParameter containingAuSub = new MessageSubscriptionParameter(subscriptionIdGenerator.incrementAndGet(), DEFAULT_PRODUCT, DEFAULT_CONTEXT, ".*[aA][uU].*");
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, startingWithBSub));
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, endingWithMSub));
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, containingAuSub));
    
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
    
    MessageInputParameter unfittingMessage = new MessageInputParameter(DEFAULT_PRODUCT, DEFAULT_CONTEXT, "Wald", "", generatePayload(""), false);
    topic.publish(unfittingMessage);
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
    
    MessageInputParameter fittingStarting = new MessageInputParameter(DEFAULT_PRODUCT, DEFAULT_CONTEXT, "Ball", "", generatePayload(""), false);
    topic.publish(fittingStarting);
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertTrue("Message should have been received from startingWithBSub", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(startingWithBSub.getId()));
    assertEquals("Message should have only been received from startingWithBSub", 1, retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().size());
    
    MessageInputParameter fittingEnding = new MessageInputParameter(DEFAULT_PRODUCT, DEFAULT_CONTEXT, "Kamm", "", generatePayload(""), false);
    topic.publish(fittingEnding);
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertTrue("Message should have been received from endingWithMSub", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(endingWithMSub.getId()));
    assertEquals("Message should have only been received from endingWithMSub", 1, retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().size());
    
    MessageInputParameter fittingContains = new MessageInputParameter(DEFAULT_PRODUCT, DEFAULT_CONTEXT, "Daune", "", generatePayload(""), false);
    topic.publish(fittingContains);
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertTrue("Message should have been received from containingAuSub", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(containingAuSub.getId()));
    assertEquals("Message should have only been received from containingAuSub", 1, retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().size());
    
    MessageInputParameter fittingAll = new MessageInputParameter(DEFAULT_PRODUCT, DEFAULT_CONTEXT, "Baum", "", generatePayload(""), false);
    topic.publish(fittingAll);
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertTrue("Message should have been received from startingWithBSub", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(startingWithBSub.getId()));
    assertTrue("Message should have been received from endingWithMSub", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(endingWithMSub.getId()));
    assertTrue("Message should have been received from containingAuSub", retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().contains(containingAuSub.getId()));
    assertEquals("Message should have been received from all subs", 3, retrieval.getMessages().iterator().next().getCorrelatedSubscriptions().size());
  }
  
  
  @Test
  public void testPersistentMessageDelete() throws XynaException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    
    topic.publish(getDefaultMessageParameter(true));
    
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1L);
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    
    final String correlation = "Corr";
    Long messageIdToRemove = topic.publish(new MessageInputParameter(DEFAULT_PRODUCT, DEFAULT_CONTEXT, correlation, "", generatePayload(""), true));

    messageBus.removePersistentMessage(DEFAULT_PRODUCT, DEFAULT_CONTEXT, correlation, messageIdToRemove);
    
    retrieval = messageBus.fetchMessages(uniqueSessionId, retrieval.getLastCheckedId());
    assertEquals("There should have been no messages", 0, retrieval.getMessages().size());
  }
  
  
  @Test
  public void testPersientReceiveWithEmptyExpandingSubscription() throws XynaException, InterruptedException, ExecutionException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    
    MessageInputParameter message = getDefaultMessageParameter(true);
    topic.publish(message);
    
    Future<MessageRetrievalResult> futureRetrieval = asyncFetch(messageBus, uniqueSessionId, -1L);
    
    Thread.sleep(150);
    
    assertFalse("No subscription does exist.", futureRetrieval.isDone());
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    
    Thread.sleep(150);
    
    assertTrue("A subscription has been created.", futureRetrieval.isDone());
    
    MessageRetrievalResult retrieval = futureRetrieval.get();
    
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    
  }
  
  
  @Test
  public void testAddedSubscription() throws XynaException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter firstSubscription = new MessageSubscriptionParameter(1l, "Xyna", "context1", ".*");
    final MessageSubscriptionParameter secondSubscription =  new MessageSubscriptionParameter(2l, "Xyna", "context2", ".*");
    
    messageBus.addSubscription(uniqueSessionId, firstSubscription);

    MessageInputParameter message = new MessageInputParameter("Xyna", "context1", "some", "test", null, false);
    topic.publish(message);
    
    messageBus.addSubscription(uniqueSessionId, secondSubscription);
    
    
    MessageRetrievalResult retrieval = messageBus.fetchMessages(uniqueSessionId, -1l);
    assertFalse("Subscription2 sould not be correlated.", retrieval.getMessages().get(0).getCorrelatedSubscriptions().contains(2l));
  }
  
  @Test
  public void testPersientReceiveWithEmptyExpandingSubscription2() throws XynaException, InterruptedException, ExecutionException {
    final String uniqueSessionId = "mySession";
    final MessageSubscriptionParameter uniqueSubscription = getDefaultMessageSubscriptionParameter();
    
    MessageInputParameter message = getDefaultMessageParameter(true);
    topic.publish(message);
    
    Future<MessageRetrievalResult> futureRetrieval = asyncFetch(messageBus, uniqueSessionId, -1L);
    
    Thread.sleep(150);
    
    assertFalse("No subscription does exist.", futureRetrieval.isDone());
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, uniqueSubscription));
    
    Thread.sleep(150);
    
    assertTrue("A subscription has been created.", futureRetrieval.isDone());
    
    MessageRetrievalResult retrieval = futureRetrieval.get();
    
    System.out.println(retrieval.getMessages().iterator().next().getCorrelatedSubscriptions());
    
    final MessageSubscriptionParameter secondSubscription = getDefaultMessageSubscriptionParameter();
    
    futureRetrieval = asyncFetch(messageBus, uniqueSessionId, -1L);
    
    Thread.sleep(150);
    
    assertFalse("No subscription does exist.", futureRetrieval.isDone());
    
    assertTrue("Subscription should have been succesfully added", messageBus.addSubscription(uniqueSessionId, secondSubscription));

    Thread.sleep(150);
    
    retrieval = futureRetrieval.get();
    
    
    assertNotNull(retrieval);
    assertNotNull(retrieval.getMessages());
    assertEquals("There should have been one messages", 1, retrieval.getMessages().size());
    assertEquals("It should have had the given payload", DEFAULT_PAYLOAD, retrieval.getMessages().iterator().next().getPayload());
    
  }

  
  private final static ExecutorService executor = Executors.newFixedThreadPool(10);
  
  
  private final static Future<MessageRetrievalResult> asyncFetch(MessageBus messageBus, String uniqueSessionId, Long lastReceivedId) {
    return executor.submit(new AsyncMessageFetcher(messageBus, uniqueSessionId, lastReceivedId));
  }
  
  
  private static class AsyncMessageFetcher implements Callable<MessageRetrievalResult> {
    
    private final MessageBus messageBus;
    private final Long lastReceivedId;
    private final String uniqueSessionId;
    
    AsyncMessageFetcher(MessageBus messageBus, String uniqueSessionId, Long lastReceivedId) {
      this.messageBus = messageBus;
      this.lastReceivedId = lastReceivedId;
      this.uniqueSessionId = uniqueSessionId;
    }

    public MessageRetrievalResult call() throws Exception {
      return messageBus.fetchMessages(uniqueSessionId, lastReceivedId);
    }
    
  }
  
  
  
  
}
