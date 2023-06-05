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
package com.gip.xyna.xmcp.xguisupport;

import java.util.Set;

import com.gip.xyna.Section;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.xguisupport.messagebus.Message;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagement;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagementPortal;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;


public class XGUISupport extends Section implements XGUISupportPortal {

  public final static String DEFAULT_NAME = "Xyna GUI Support";
  
  public XGUISupport() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    deployFunctionGroup(new MessageBusManagement());
    XynaProperty.MESSAGE_BUS_FETCH_TIMEOUT.registerDependency(DEFAULT_NAME);
  }


  public MessageBusManagementPortal getMessageBusManagement() {
    return (MessageBusManagement) getFunctionGroup(MessageBusManagement.DEFAULT_NAME);
  }


  public Long publish(MessageInputParameter message) throws XynaException {
    return getMessageBusManagement().publish(message);
  }


  public boolean addSubscription(String subscriptionSessionId, MessageSubscriptionParameter subscription) {
    return getMessageBusManagement().addSubscription(subscriptionSessionId, subscription);
  }


  public boolean cancelSubscription(String subscriptionSessionId, Long subscriptionId) {
    return getMessageBusManagement().cancelSubscription(subscriptionSessionId, subscriptionId);
  }


  public MessageRetrievalResult fetchMessages(String subscriptionSessionId, Long lastReceivedId) {
    return getMessageBusManagement().fetchMessages(subscriptionSessionId, lastReceivedId);
  }


  public void removePersistentMessage(String product, String context, String correlation, Long messageId) {
    getMessageBusManagement().removePersistentMessage(product, context, correlation, messageId);
  }


}
