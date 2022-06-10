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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

import com.gip.xyna.utils.streams.StreamUtils;

import xact.mail.ReceiveHandler;
import xact.mail.internal.Mail.Content;
import xact.mail.internal.Mail.HeaderField;

public class ReceiveHandlerMail implements ReceiveHandler<Mail> {

  private boolean readHeader;
  
  public ReceiveHandlerMail(boolean readHeader) {
    this.readHeader = readHeader;
  }
  
  @Override
  public Mail handle(String messageId, Message message) throws MessagingException {
    try {
    Mail mail = new Mail();
    mail.setMessageId(messageId);
    mail.setSubject(message.getSubject());
    mail.setSender( extractSender(message.getFrom()) );
    mail.setRecipientsTo( extractRecipients( message.getRecipients(RecipientType.TO) ) );
    mail.setRecipientsCc( extractRecipients( message.getRecipients(RecipientType.CC) ) );
    mail.setRecipientsBcc( extractRecipients( message.getRecipients(RecipientType.BCC) ) );
    
    mail.setSentDate( getTime(message.getSentDate() ) );
    mail.setReceivedDate( getTime(message.getReceivedDate()) );
    
    if( readHeader ) {
      mail.setHeader( readHeader( message ) );
    }
    readBody( mail, message );
    return mail;
    } catch( IOException e ) {
      throw new MessagingException("Unexpected IOException: "+e.getMessage(), e);
    }
  }

  private long getTime(Date date) {
    if( date == null ) {
      return 0;
    } else {
      return date.getTime();
    }
  }

  private List<HeaderField> readHeader(Part part) throws MessagingException {
    List<HeaderField> header = new ArrayList<>();
    @SuppressWarnings("unchecked")
    Enumeration<javax.mail.Header> allHeaders = part.getAllHeaders();
    while( allHeaders.hasMoreElements() ) {
      javax.mail.Header h = allHeaders.nextElement();
      header.add( new HeaderField(h.getName(), h.getValue()));
    }
    return header;
  }

  @Override
  public boolean receiveNext() {
    return true;
  }

  
  private String extractSender(Address[] from) {
    if( from == null || from.length == 0 ) {
      return null;
    }
    //Falls es unerwarteterweise mehrere Sender geben sollte:
    //Im HeaderField stehen diese drin
    return extractAddress(from[0]);
  }

  private List<String> extractRecipients(Address[] recipients) {
    if( recipients == null || recipients.length == 0 ) {
      return Collections.emptyList();
    }
    List<String> rs = new ArrayList<>();
    for( Address a : recipients ) {
      rs.add( extractAddress(a) );
    }
    return rs;
  }
  
  private String extractAddress(Address address) {
    if( address instanceof InternetAddress ) {
      return ((InternetAddress)address).getAddress();
    } else {
      return address.toString();
    }
  }

  private void readBody(Mail mail, Part part) throws MessagingException, IOException{
    if (part.isMimeType("multipart/*")) {
      Multipart mp = (Multipart)part.getContent();
      readBody(mail, mp.getBodyPart(0));
      for (int i = 1; i < mp.getCount(); i++) {
        mail.addToAttachments(readContent(mp.getBodyPart(i)));
      }
    } else if (part.isMimeType("multipart/alternative")) {
      Multipart mp = (Multipart)part.getContent();
      for (int i = 0; i < mp.getCount(); i++) {
        Part bp = mp.getBodyPart(i);
        if (bp.isMimeType("text/plain")) {
          mail.setBody(readContent(bp));
        } else {
          mail.addToAttachments(readContent(bp));
        }
      }
    } else {
      mail.setBody(readContent(part));
    }
  }
  
  
  private Content readContent(Part part) throws MessagingException, IOException {
    List<HeaderField> header = null;
    if( readHeader ) {
      header = readHeader( part );
    }
    String type = part.getContentType();
    Object content = part.getContent();
    if( content instanceof String ) {
      return new Content(part.getFileName(), type, (String)content, header);
    }
    if( content instanceof InputStream ) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StreamUtils.copy((InputStream)content, baos);
      return new Content(part.getFileName(), type, baos.toByteArray(), header );
    }
    //unerwartet!
    StringBuilder sb = new StringBuilder("Unexpected Content ");
    if( content != null ) {
      sb.append("of type ").append(content.getClass()).append(": ");
    }
    sb.append(content);
    return new Content(part.getFileName(), type, sb.toString(), header);
  }

}
