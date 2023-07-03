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

import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStore;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;

import xact.mail.Receiver;
import xact.mail.account.MailAccountData;
import xact.mail.account.MailAccountStorage;
import xact.mail.exceptions.CreateMailException;
import xact.mail.exceptions.InvalidMailAddressException;
import xact.mail.exceptions.SendMailException;
import xact.mail.internal.Mail;
import xact.mail.internal.MailServer;
import xact.mail.internal.SMTPImpl;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;

import java.util.List;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;


public class MailTrigger extends EventListener<MailTriggerConnection, MailStartParameter> {

  private final static Logger logger = CentralFactoryLogging.getLogger(MailTrigger.class);
  
  private MailStartParameter startParameter;

  private LocalMailStore mailStore = new LocalMailStore();
  private Receiver receiver;
  private SMTPImpl sender;
  private boolean firstReceive;
  private RepeatedExceptionCheck recReceive;

  
  public MailTrigger() {
  }

  
  public void start(MailStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {
    this.startParameter = sp;
    
    MailAccountData  mailAccountData = MailAccountStorage.getInstance().getMailAccount(startParameter.getMailAccount());
    if (mailAccountData == null) {
      throw new IllegalStateException("No MailAccount "+startParameter.getMailAccount()+" registered");
    }
    KeyManagement km = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
    if (mailAccountData.getKeyStore() != null &&
        !mailAccountData.getKeyStore().isEmpty()) {
      try {
        km.getKeyStore(mailAccountData.getKeyStore());
      } catch (XFMG_UnknownKeyStore e) {
        throw new IllegalStateException("Configured keyStore "+mailAccountData.getKeyStore()+" is not registered", e);
      } catch (XFMG_UnknownKeyStoreType e) {
        throw new IllegalStateException("Configured keyStore "+mailAccountData.getKeyStore()+" is not registered", e);
      }
    }
    if (mailAccountData.getTrustStore() != null &&
        !mailAccountData.getTrustStore().isEmpty()) {
      try {
        km.getKeyStore(mailAccountData.getTrustStore());
      } catch (XFMG_UnknownKeyStore e) {
        throw new IllegalStateException("Configured trustStore "+mailAccountData.getTrustStore()+" is not registered", e);
      } catch (XFMG_UnknownKeyStoreType e) {
        throw new IllegalStateException("Configured trustStore "+mailAccountData.getTrustStore()+" is not registered", e);
      }
    }
    
    MailServer mailServer = new MailServer(mailAccountData, 
                                           new MailServer.SocketParameter((int) sp.getPollingTime().getDurationInMillis() / 2,
                                                                          (int) sp.getPollingTime().getDurationInMillis() / 2));
    receiver = createReceiver(mailServer);
    sender = mailServer.createSender();
    this.firstReceive = true;
  }

  
  public MailTriggerConnection receive() {
    MailTriggerConnection tc = receiveInternal();
    if( tc != null ) {
      return tc;
    }
    
    //Keine Mail erhalten oder Fehler, daher warten
    try {
      Thread.sleep(startParameter.getPollingTime().getDurationInMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return null;
  }

  
  private MailTriggerConnection receiveInternal() {
    List<Mail> mails = null;
    try {
      LocalMailReceiveHandler rh = new LocalMailReceiveHandler(mailStore, startParameter.getReadHeader() );
      mails = receiver.receive( rh );
    } catch (MessagingException e) {
      handleReceiveException(e, firstReceive);
    }
    firstReceive = false;
    if( mails != null && ! mails.isEmpty() ) {
      Mail mail = mails.get(0);
      return new MailTriggerConnection(mailStore, mail);
    }
    return null; //macht Retry
  }
  
  private void handleReceiveException(Throwable t, boolean firstReceive) throws TriggerFailedException {
    if(firstReceive) {
      logger.warn("Failed to receive mail", t);
      throw new TriggerFailedException("Failed to receive mail", t);
    } else {
      // da dieser Fehler nicht immer auftritt, wird er als temporÃ¤res Problem angesehen.
      //Daher werden weiter Retries probiert.
      if( recReceive == null ) {
        recReceive = new RepeatedExceptionCheck();
      }
      int cnt = recReceive.checkRepeationCount(t);
      if( cnt == 0 ) {
        logger.warn("Failed to receive mail", t);
      } else {
        logger.warn("Failed to receive mail again ("+cnt+") "+t.getMessage());
      }
    }
  }
  
  
  /**
   * Called by Xyna Processing if there are not enough system capacities to process the request.
   */
  protected void onProcessingRejected(String cause, MailTriggerConnection con) {
    mailStore.notProcessed(con.getMessageId());
  }

  
  /**
   * called by Xyna Processing to stop the Trigger.
   * should make sure, that start() may be called again directly afterwards. connection instances
   * returned by the method receive() should not be expected to work after stop() has been called.
   */
  public void stop() throws XACT_TriggerCouldNotBeStoppedException {
    //mailStore bleibt erhalten
    //ansonsten muss nichts geschlossen werden
    //TriggerConnections funktionieren weiter
    recReceive = null;
  }

  
  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter
   * registered to this trigger
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(MailTriggerConnection con) {
    //Mail sollte nicht gelöscht werden. Oder doch? Dann sollte das ein weiterer StartParameter werden!
    mailStore.notProcessed(con.getMessageId());
  }

  
  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    return "MailTrigger receives mails";
  }
  

  public void delete(String messageId) throws MessagingException {
    receiver.delete(messageId);
    mailStore.delete(messageId);
  }

  public void sendReply(Mail mail) {
    try {
      sender.send(mail);
    } catch (InvalidMailAddressException | CreateMailException | SendMailException e) {
      logger.warn("Failed to send reply ",e);
    }
  }
  
  
  private static Receiver createReceiver(MailServer mailServer) {
    return mailServer.getMailAccount().getAccountProtocol().getReceiver(mailServer);
    // throw new IllegalArgumentException("Invalid protocol '" + mailServer.getMailAccount().getType() + "', failed to create Receiver.");
  }
  
  
  public static class TriggerFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TriggerFailedException(String message, Throwable cause) {
      super(message,cause);
    }
    
  }

}
