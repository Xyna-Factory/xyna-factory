/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;

import xact.mail.account.MailAccountData;
import xact.mail.exceptions.CreateMailException;
import xact.mail.exceptions.InvalidMailAddressException;
import xact.mail.exceptions.SendMailException;
import xact.mail.internal.Mail.Content;
import xact.mail.internal.MailServer.SessionTransportExecutor;

public class MailSender implements SessionTransportExecutor {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(MailSender.class);

  private Mail mail;
  private MailAccountData mailAccount;

  public MailSender(Mail mail, MailAccountData mailAccount) {
    this.mail = mail;
    this.mailAccount = mailAccount;
  }

  @Override
  public void execute(Session session, Transport transport) throws Exception {
    MimeMessage message = null;
    try {
      message = createMessage(session, mail);
    } catch( MessagingException e ) {
      throw new CreateMailException(e);
    }
    try {
      transport.sendMessage(message, message.getAllRecipients());
      logger.info("Sent message with id "+ message.getMessageID());
    } catch (MessagingException e) {
      throw new SendMailException(e);
    }
  }

  /**
   * @param mail
   * @return
   * @throws MessagingException 
   * @throws InvalidMailAddressException 
   */
  private MimeMessage createMessage(Session session, Mail mail) throws MessagingException, InvalidMailAddressException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom( createFrom(mail.getSender()) );
    addRecipients(message, Message.RecipientType.TO, mail.getRecipientsTo() );
    addRecipients(message, Message.RecipientType.CC, mail.getRecipientsCc() );
    addRecipients(message, Message.RecipientType.BCC, mail.getRecipientsBcc() );
    message.setSubject(mail.getSubject());
    message.setReplyTo(message.getFrom());
    message.setSentDate(new Date());

    if( mail.getAttachments() == null || mail.getAttachments().isEmpty() ) {
      fillBody( message, mail.getBody() );
    } else {
      Multipart multipart = new MimeMultipart();

      BodyPart part1 = new MimeBodyPart();
      fillBody( part1, mail.getBody() );
      multipart.addBodyPart(part1);

      for (Content attachment : mail.getAttachments() ) {
        BodyPart bp = new MimeBodyPart();
        bp.setFileName(attachment.getName());
        bp.setText(attachment.getContentString());
        bp.setHeader("Content-Type", attachment.getMediaType() );
        multipart.addBodyPart(bp);
      }
      message.setContent(multipart);
    }

    return message;
  }

  private Address createFrom(String sender) throws InvalidMailAddressException {
    if( sender == null || sender.isEmpty() ) {
      sender = mailAccount.getAddress();
    }
    return toInternetAddress(sender);
  }

  private void addRecipients(MimeMessage message, RecipientType recipientType, List<String> recipients) throws InvalidMailAddressException, MessagingException {
    if( recipients == null ) {
      return;
    }
    for( String to : recipients ) {
      message.addRecipient(recipientType, toInternetAddress(to) );
    }
  }

  private void fillBody(Part part, Content body) throws MessagingException {
    part.setContent(body.getContentString(), body.getMediaType() ); //TODO charset=UTF-8 ergänzen?
  }

  private InternetAddress toInternetAddress(String mailAddress) throws InvalidMailAddressException {
    if( mailAddress == null ) {
      throw new InvalidMailAddressException("mailAddress is null");
    }
    try {
      return new InternetAddress(mailAddress);
    } catch (AddressException e) {
      throw new InvalidMailAddressException(mailAddress, e);
    }
  }

}
