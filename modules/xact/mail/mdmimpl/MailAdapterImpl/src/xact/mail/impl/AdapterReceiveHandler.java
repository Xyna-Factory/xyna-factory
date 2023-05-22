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
package xact.mail.impl;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import xact.mail.ReceiveOptions;
import xact.mail.ReceivedMail;
import xact.mail.Receiver;
import xact.mail.internal.Mail;
import xact.mail.internal.ReceiveHandlerMail;

public class AdapterReceiveHandler implements xact.mail.ReceiveHandler<ReceivedMail> {

  private Receiver receiver;
  private boolean readHeader;
  private List<String> messageIds = new ArrayList<>();
  
  public AdapterReceiveHandler(Receiver receiver, ReceiveOptions receiveOptions) {
    this.receiver = receiver;
    readHeader = false;//TODO konfigurierbar
  }

  @Override
  public ReceivedMail handle(String messageId, Message message) throws MessagingException {
    ReceiveHandlerMail rhm = new ReceiveHandlerMail(readHeader); 
    Mail mail = rhm.handle(messageId, message);
    messageIds.add(messageId);
    return MailConverter.convertToXmom(mail);
  }

  @Override
  public boolean receiveNext() {
    return true;
  }

  public void deleteMails() throws MessagingException {
    receiver.delete(messageIds);
  }

}
