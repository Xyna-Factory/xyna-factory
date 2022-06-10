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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Trigger und SharedLib müssen bereits ein Mail-Objekt befüllen, können aber keinen 
 * XMOM-Datentyp verwenden.
 * Dieser Datentyp hier ist ein Nachbau des XMOM-Datentyps xact.mail.Mail. 
 * Damit ist dann eine Umwandlung in den XMOM-Datentyp einfach.
 *
 */
public class Mail implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private String subject;
  private Content body;
  private String sender;
  private List<String> recipientsTo;
  private List<String> recipientsCc;
  private List<String> recipientsBcc;
  private List<Content> attachments;
  private String messageId;
  private long sentDate;
  private long receivedDate;
  private List<HeaderField> header;
  private String encoding;
  
  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public Content getBody() {
    return body;
  }

  public void setBody(Content body) {
    this.body = body;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public List<String> getRecipientsTo() {
    return recipientsTo;
  }

  public void setRecipientsTo(List<String> recipientsTo) {
    this.recipientsTo = recipientsTo;
  }

  public void addRecipientTo(String to) {
    if( recipientsTo == null ) {
      recipientsTo = new ArrayList<>();
    }
    recipientsTo.add(to);
  }

  public List<String> getRecipientsCc() {
    return recipientsCc;
  }

  public void setRecipientsCc(List<String> recipientsCc) {
    this.recipientsCc = recipientsCc;
  }

  public void addRecipientCc(String cc) {
    if( recipientsCc == null ) {
      recipientsCc = new ArrayList<>();
    }
    recipientsCc.add(cc);
  }

  public List<String> getRecipientsBcc() {
    return recipientsBcc;
  }

  public void setRecipientsBcc(List<String> recipientsBcc) {
    this.recipientsBcc = recipientsBcc;
  }

  public void addRecipientBcc(String bcc) {
    if( recipientsBcc == null ) {
      recipientsBcc = new ArrayList<>();
    }
    recipientsBcc.add(bcc);
  }

  public List<Content> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Content> attachments) {
    this.attachments = attachments;
  }
  
  public void addToAttachments(Content attachment) {
    if( attachments == null ) {
      attachments = new ArrayList<>();
    }
    attachments.add(attachment);
  }
  
  public String getMessageId() {
    return messageId;
  }
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }
  public long getSentDate() {
    return sentDate;
  }
  public void setSentDate(long sentDate) {
    this.sentDate = sentDate;
  }
  public long getReceivedDate() {
    return receivedDate;
  }
  public void setReceivedDate(long receivedDate) {
    this.receivedDate = receivedDate;
  }

  public List<HeaderField> getHeader() {
    return header;
  }
  public void setHeader(List<HeaderField> header) {
    this.header = header;
  }
  public void addHeaderField(String key, String value) {
    if( header == null ) {
      header = new ArrayList<>();
    }
    header.add( new HeaderField(key,value) );
  }


  public String getEncoding() {
    return encoding;
  }


  public void setEncoding(String value) {
    encoding = value;
  }


  public static class Content implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final String name;
    private final String mediaType;
    private final String contentString;
    private final byte[] contentBytes;
    private final List<HeaderField> header;
   
    public Content(String name, String mediaType, String contentString, List<HeaderField> header) {
      this.name = name;
      this.mediaType = mediaType;
      this.contentString = contentString;
      this.contentBytes = null;
      this.header = header;
    }

    public Content(String name, String mediaType, byte[] contentBytes, List<HeaderField> header) {
      this.name = name;
      this.mediaType = mediaType;
      this.contentString = null;
      this.contentBytes = contentBytes;
      this.header = header;
    }

    public String getMediaType() {
      return mediaType;
    }

    public String getContentString() {
      return contentString;
    }

    public byte[] getContentBytes() {
      return contentBytes;
    }

    public String getName() {
      return name;
    }
    
    public List<HeaderField> getHeader() {
      return header;
    }
  }
  
  public static class HeaderField implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String value;

    public HeaderField(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Mail(").append(messageId).append(": \"").append(subject).append("\"\n");
    
    sb.append("  from ").append(sender);
    if( ! recipientsTo.isEmpty() ) {
      sb.append(", to=").append(recipientsTo);
    }
    if( ! recipientsCc.isEmpty() ) {
      sb.append(", cc=").append(recipientsCc);
    }
    if( ! recipientsBcc.isEmpty() ) {
      sb.append(", bcc=").append(recipientsBcc);
    }
    sb.append(",\n  sent ").append(sentDate).append(", received ").append(receivedDate);
    appendHeader(sb, header);
    sb.append("  body=");
    appendContent(sb, body, false);
    if( attachments != null ) {
      int a = 0;
      for( Content c : attachments ) {
        sb.append("\nattachment_").append( a).append("=");
        appendContent(sb, c, true);
        ++a;
      }
    }
    sb.append(")");
    return sb.toString();
  }
  public void appendHeader(StringBuilder sb, List<HeaderField> header) {
    if( header == null ) {
      return;
    }
    sb.append("\n  header[");
    String sep= "";
    for( HeaderField hf : header ) {
      sb.append(sep).append(hf.key).append("->\"").append(hf.value).append("\"");
      sep = ",\n         ";
    }
    sb.append("],\n");
  }
  
  private void appendContent(StringBuilder sb, Content content, boolean appendHeader) {
    sb.append("(").append(content.getName()).append(" (").append(content.getMediaType()).append("): ");
    if( appendHeader ) {
      appendHeader(sb, content.getHeader());
    }
    if( content.getContentString() != null ) {
      int length = content.getContentString().length();
      if( length < 50 ) {
        sb.append("  \"").append(content.getContentString()).append("\"");
      } else {
        sb.append("  \"").append(content.getContentString().substring(0,  40)).append("\"...");
      }
    } else {
      if( content.getContentBytes() != null ) {
        sb.append("  ").append(content.getContentBytes().length).append(" bytes");
      } 
    }
    sb.append(")");
  }
  
  
}
