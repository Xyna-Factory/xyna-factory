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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;


public class QuickAndDirtyLocalMessageTopic implements MessageTopic {

  List<Message> unreceivedMessages = new ArrayList<Message>();
  AtomicBoolean connectionState = new AtomicBoolean(true);
  
  public synchronized List<Message> receive() {
    while (unreceivedMessages.size() == 0 && connectionState.get()) {
      try {
        wait();
      } catch (InterruptedException e) {
        // aye, shorter then
      }
    }
    List<Message> result = new ArrayList<Message>(unreceivedMessages);
    unreceivedMessages.clear();
    return result;
  }

  public synchronized Long publish(MessageInputParameter message) throws XynaException {
    Long messageId = getUniqueMessageId();
    unreceivedMessages.add(new Message(messageId, message));
    notifyAll();
    return messageId;
  }

  public long getUniqueMessageId() throws XynaException {
    return IDGenerator.getInstance().getUniqueId();
  }

  public synchronized void disconnect() {
    connectionState.set(false);
    notifyAll();
  }

  public boolean isConnected() {
    return connectionState.get();
  }

  public synchronized void removeMessage(Long messageId) {
    Iterator<Message> iter = unreceivedMessages.iterator();
    while (iter.hasNext()) {
      Message current = iter.next();
      if (current.getId().equals(messageId)) {
        iter.remove();
        break;
      }
    }
  }

}
