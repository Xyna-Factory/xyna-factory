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

import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;

import xact.mail.internal.Mail;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;

import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

public class MailTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = 1L;
  private static Logger logger = CentralFactoryLogging.getLogger(MailTriggerConnection.class);

  private final LocalMailStore mailStore;
  private final Mail mail;
  private final String messageId;


  public MailTriggerConnection(LocalMailStore mailStore, Mail mail) {
    this.mailStore = mailStore;
    this.mail = mail;
    this.messageId = mail.getMessageId();
  }

  /**
   * Versucht einen Retry
   * Falls die Anzahl der Retries maxRetries überschreitet, wird false zurückgegeben.
   * In diesem Fall muss der Filter die Mail abschließend bearbeiten. Es bleibt wohl 
   * nur Loggen übrig, danach sollte ein Delete durchgeführt werden!
   * @param maxRetries
   * @return
   */
  public boolean retry(int maxRetries) {
    return mailStore.retry(messageId, maxRetries);
  }
  
  /**
   * L�scht die Mail auf dem Server
   * @throws MessagingException
   */
  public void delete() throws MessagingException {
    ((MailTrigger)getTrigger()).delete(messageId);
  }

  public String getMessageId() {
    return messageId;
  }
  
  public Mail getMail() {
    return mail;
  }

  public void sendReply(Mail reply) {
    //in der Reply k�nnen nun Sender, Recipients und Subject fehlen, diese werden nun ergänzt.
    if( isEmpty( reply.getSubject() ) ) {
      reply.setSubject( mail.getSubject() ); //TODO "Re: " ergänzen?
    }
    if( isEmpty( reply.getSender() ) ) {
      reply.setSender( mail.getRecipientsTo().get(0) );
    }
    if( isEmpty( reply.getRecipientsTo() ) ) {
      reply.addRecipientTo( mail.getSender() );
    }
    reply.addHeaderField("In-Reply-To", mail.getMessageId());
    
    ((MailTrigger)getTrigger()).sendReply(reply);
  }

  private static boolean isEmpty(List<String> list) {
    return list == null || list.isEmpty();
  }

  private static boolean isEmpty(String string) {
    return string == null || string.isEmpty();
  }

}
