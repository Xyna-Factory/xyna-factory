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
package xact.mail.internal;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.search.MessageIDTerm;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;

import xact.mail.ReceiveHandler;
import xact.mail.Receiver;
import xact.mail.internal.MailServer.FolderExecutor;

public class IMAPImpl implements Receiver {
  
  private static Logger logger = CentralFactoryLogging.getLogger(IMAPImpl.class);
  private static final String FOLDER_INBOX = "INBOX"; 
  
  private MailServer mailServer;
  private String inbox;

  public IMAPImpl(MailServer mailServer) {
    this.mailServer = mailServer;
    this.inbox = FOLDER_INBOX; //TODO konfigurierbar
  }

  @Override
  public <M> List<M> receive(ReceiveHandler<M> receiveHandler) throws MessagingException {
    Receiver<M> receiver = new Receiver<>(receiveHandler);
    mailServer.openFolderAndExecute(inbox, false, receiver );
    return receiver.getMails();
  }
  
  @Override
  public void delete(String messageId) throws MessagingException {
    mailServer.openFolderAndExecute(inbox, true, new Deleter(messageId) );
  }
  
  @Override
  public void delete(List<String> messageIds) throws MessagingException {
    mailServer.openFolderAndExecute(inbox, true, new Deleter(messageIds) );
  }
 
  private static class Receiver<M> implements FolderExecutor {
    
    private ReceiveHandler<M> receiveHandler;
    private List<M> mails = new ArrayList<>();
    
    public Receiver(ReceiveHandler<M> receiveHandler) {
      this.receiveHandler = receiveHandler;
    }

    @Override
    public void execute(Folder folder) throws MessagingException {
      int msgCount = folder.getMessageCount();
      int idx = 0;
      while( receiveHandler.receiveNext() && idx <msgCount ) {
        ++idx;
        Message m = folder.getMessage(idx);
        String messageId = ((MimeMessage)m).getMessageID();
        if (messageId != null) {
          M mail = receiveHandler.handle(messageId, m);
          if( mail != null ) {
            logger.debug("Received mail "+messageId);
            mails.add(mail);
          }
        }
      }
    }
    
    public List<M> getMails() {
      return mails;
   }

  }
  
  private static class Deleter implements FolderExecutor {

    private String messageId;
    private List<String> messageIds;
    
    public Deleter(String messageId) {
      this.messageId = messageId;
    }
    public Deleter(List<String> messageIds) {
      if( messageIds.size() == 1 ) {
        this.messageId = messageIds.get(0);
      } else {
        this.messageIds = messageIds;
      }
    }

    @Override
    public void execute(Folder folder) throws MessagingException {
      if( messageId != null ) {
        deleteSingleMail(folder);
      } else {
        deleteMultipleMails(folder);
      }
    }
    
    
    private void deleteSingleMail(Folder folder) throws MessagingException {
      Message[] messages = folder.search( new MessageIDTerm(messageId) );
      for( Message m : messages ) {
        m.setFlag(Flags.Flag.DELETED, true);
        logger.debug("Deleted mail "+messageId);
      }
    }
    
    private void deleteMultipleMails(Folder folder) throws MessagingException {
      Message[] messages = folder.getMessages();
      for( Message m : messages ) {
        String msgId = ((MimeMessage)m).getMessageID();
        if( messageIds.contains(msgId) ) {
          m.setFlag(Flags.Flag.DELETED, true);
          logger.debug("Deleted mail "+msgId);
        }
      }
    }

    
  }

 
}
