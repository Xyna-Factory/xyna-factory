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
package com.gip.xyna.xact.filter;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate;

import base.date.format.YyyyMMDdTHHMmSs;
import xact.mail.Attachment;
import xact.mail.Bcc;
import xact.mail.Cc;
import xact.mail.Mail;
import xact.mail.MailAddress;
import xact.mail.MailRecipient;
import xact.mail.ReceivedMail;
import xact.mail.To;
import xact.mail.internal.Mail.Content;
import xact.templates.Document;
import xact.templates.DocumentType;
import xact.templates.HTML;
import xact.templates.JSON;
import xact.templates.PlainText;
import xact.templates.XML;

/**
 * Konversion xact.mail.impl.Mail in XMOM.Objekt xact.mail.Mail und umgekehrt
 * FIXME Verdopplung: Auch in MailAdapter
 */
public class MailConverter {

  private MailConverter() {}
  
  public static ReceivedMail convertToXmom(xact.mail.internal.Mail mail) {
    return new ReceivedMail().buildReceivedMail().
        subject( mail.getSubject() ).
        sender( new MailAddress(mail.getSender()) ).
        recipients( fillRecipients(mail) ).
        body( contentToDocument(mail.getBody()) ).
        attachments( fillAttachments(mail.getAttachments()) ).
        messageId(mail.getMessageId()).
        sent( absoluteDateFor( mail.getSentDate() ) ).
        received( absoluteDateFor( mail.getReceivedDate() ) ).
        instance();
  }

  private static AbsoluteDate absoluteDateFor(long millis) {
    if( millis <= 0 ) {
      return null;
    }
    AbsoluteDate d = new AbsoluteDate("", new YyyyMMDdTHHMmSs() );
    d.fromMillis(millis);
    return d;
  }

  public static xact.mail.internal.Mail convertFromXmom(Mail mail) {
    xact.mail.internal.Mail ret = new xact.mail.internal.Mail();
    ret.setSubject(mail.getSubject());
    ret.setSender(mail.getSender() != null ? mail.getSender().getMailAddress() : null);
    fillRecipients( ret, mail.getRecipients());
    ret.setBody( createContent( "body", null, mail.getBody() ) );
    if( mail.getAttachments() != null ) {
      for( Attachment a : mail.getAttachments() ) {
        ret.addToAttachments( createContent( a.getName(), a.getMediaType(), a.getDocument() ) );
      }
    }
    return ret;
  }


  private static List<MailRecipient> fillRecipients(xact.mail.internal.Mail mail) {
    List<MailRecipient> recipients = new ArrayList<>();
    for( String to : mail.getRecipientsTo() ) {
      recipients.add( new To(to) );
    }
    for( String cc : mail.getRecipientsCc() ) {
      recipients.add( new Cc(cc) );
    }
    for( String bcc : mail.getRecipientsBcc() ) {
      recipients.add( new Bcc(bcc) );
    }
    return recipients;
  }

  private static Document contentToDocument(Content content) {
    String mediaType = content.getMediaType();
    String contentString = content.getContentString();
    if( contentString != null ) {
      if( mediaType.startsWith("text/plain") ) {
        return new Document( new PlainText(), contentString );
      } else if( mediaType.startsWith("text/html") ) {
        return new Document( new HTML(), contentString );
      } else if( mediaType.startsWith("application/json") || mediaType.startsWith("text/json") ) {
        //text/json ist eigentlich nicht richtig: application/json ist korrekt
        return new Document( new JSON(), contentString );
      } else if( mediaType.startsWith("text/xml") || mediaType.startsWith("application/xml")) {
        return new Document( new XML(), contentString );
      } else if( mediaType.startsWith("text/") ) {
        return new Document( new PlainText(), contentString );
      } else {
        //TODO weitere MediaType unterscheiden...
        return new Document( new PlainText(), contentString );
      }
    } else {
      return new Document( new PlainText(), contentString );
    }
    
  }
  
  private static List<Attachment> fillAttachments(List<Content> attachments) {
    if( attachments == null ) {
      return null; //TODO leere Liste?
    }
    List<Attachment> list = new ArrayList<>(attachments.size());
    for( Content c : attachments ) {
      list.add( new Attachment().buildAttachment().
          name(c.getName()).
          mediaType(c.getMediaType()).
          document(contentToDocument(c)).
          instance() );
    }
    return list;
  }
  

  private static void fillRecipients(xact.mail.internal.Mail ret, List<? extends MailRecipient> recipients) {
    if( recipients != null ) {
      for( MailRecipient mr : recipients ) {
        String address = mr.getMailAddress();
        if( mr instanceof To ) {
          ret.addRecipientTo(address);
        } else if( mr instanceof Cc ) {
          ret.addRecipientCc(address);
        } else if( mr instanceof Bcc ) {
          ret.addRecipientBcc(address);
        } else {
          //TODO warnung
          ret.addRecipientTo(address);
        }
      }
    }
  }

  private static Content createContent(String name, String mediaType, Document document) {
    return new Content(name, inferMediaType( mediaType, document ), document.getText(), null );
  }

  private static String inferMediaType(String mediaType, Document document) {
    if( mediaType != null ) {
      return mediaType;
    }
    DocumentType dt = document.getDocumentType();
    if( dt instanceof PlainText ) {
      return "text/plain";
    } else if( dt instanceof HTML ) {
      return "text/html";
    } else if( dt instanceof XML ) {
      return "text/xml"; 
    } else if( dt instanceof JSON ) {
      return "application/json";
    } else {
      //TODO weitere...
      return "text/plain";
    }
  }
  
  
}
