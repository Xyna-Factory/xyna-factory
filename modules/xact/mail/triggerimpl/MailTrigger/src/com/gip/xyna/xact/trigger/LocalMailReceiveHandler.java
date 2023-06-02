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
package com.gip.xyna.xact.trigger;

import javax.mail.Message;
import javax.mail.MessagingException;

import xact.mail.ReceiveHandler;
import xact.mail.internal.Mail;
import xact.mail.internal.ReceiveHandlerMail;

public class LocalMailReceiveHandler implements ReceiveHandler<Mail> {

  private LocalMailStore mailStore;
  private String messageId;
  private boolean readHeader;
  
  
  public LocalMailReceiveHandler(LocalMailStore mailStore, boolean readHeader) {
    this.mailStore = mailStore;
    this.readHeader = readHeader;
  }

  public Mail handle(String messageId, Message message) throws MessagingException {
    if( mailStore.contains(messageId) ) {
      return null; //Mail bereits erhalten
    } else {
      mailStore.add(messageId);
      this.messageId = messageId;
      //nur eine Mail erhalten!
      
      ReceiveHandlerMail rhm = new ReceiveHandlerMail(readHeader);
      return rhm.handle(messageId, message);
    }
  }

  public boolean receiveNext() {
    return messageId == null; //solange keine Message gefunden wurde
  }

  public String getMessageId() {
    return messageId;
  }

}
