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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 *
 */
public class DataModelResult implements Serializable {

  private static final long serialVersionUID = 1L;

  public static enum Result {
    Succeeded(true),
    SucceededWithWarnings(true),
    NoImport(false),
    Failed(false);
    
    private boolean succeeded;

    private Result(boolean succeeded) {
      this.succeeded = succeeded;
    }
    
    public boolean isSucceeded() {
      return succeeded;
    }
    
  }
  
  public static enum Level {
    Info, Warning, Error;
  }
  
  public static class MessageGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private Level level;
    private String header;
    private List<String> messages;
    public MessageGroup(Level level, String header, Collection<String> messages) {
      this.level = level;
      this.header = header;
      this.messages = new ArrayList<String>(messages);
    }
    public String toSingleString(String headSep, String msgSep) {
      StringBuilder sb = new StringBuilder();
      sb.append(header);
      String sep = headSep;
      for( String s : messages ) {
        sb.append(sep).append(s);
        sep = msgSep;
      }
      return sb.toString();
    }
    public Level getLevel() {
      return level;
    }
    public String getHeader() {
      return header;
    }
    public int size() {
      return messages.size();
    }
  }
  
  public static class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private Level level;
    private String message;
    public Message(Level level, String message) {
      this.level = level;
      this.message = message;
    }
    public String getMessage() {
      return message;
    }
    public Level getLevel() {
      return level;
    }
  }
  
  private Result result = Result.Succeeded;
  
  private List<Exception> exceptions;
  private List<Message> singleMessages;
  private List<MessageGroup> groupedMessages;
  
  public boolean isSucceeded() {
    return result.isSucceeded();
  }

  public Result getResult() {
    return result;
  }

  public void addMessage(Level level, String message) {
    if( singleMessages == null ) {
      singleMessages = new ArrayList<Message>();
    }
    singleMessages.add(new Message(level,message));
  }

  public void addMessageGroup(Level level, String header, Collection<String> messages) {
    if( groupedMessages == null ) {
      groupedMessages = new ArrayList<MessageGroup>();
    }
    groupedMessages.add(new MessageGroup(level,header,messages));
  }
  
  public void addMessageGroup(String header, Collection<String> messages) {
    if( groupedMessages == null ) {
      groupedMessages = new ArrayList<MessageGroup>();
    }
    groupedMessages.add(new MessageGroup(Level.Info,header,messages));
  }

  
  public DataModelResult fail(String message) {
    result = Result.Failed;
    addMessage(Level.Error, message);
    return this;
  }
  
  public DataModelResult fail(Exception e) {
    result = Result.Failed;
    addMessage(Level.Error, e.getMessage());
    if( exceptions == null ) {
      exceptions = new ArrayList<Exception>();
    }
    exceptions.add(e);
    return this;
  }

  public DataModelResult fail(String message, Exception e) {
    result = Result.Failed;
    addMessage(Level.Error, message+" "+e.getMessage());
    if( exceptions == null ) {
      exceptions = new ArrayList<Exception>();
    }
    exceptions.add(e);
    return this;
  }

  public void info(String message) {
    addMessage(Level.Info, message);
  }

  public void warn(String message) {
    addMessage(Level.Warning, message);
  }

  public List<Exception> getExceptions() {
    return exceptions;
  }

  public boolean hasSingleMessages() {
    return singleMessages != null && ! singleMessages.isEmpty();
  }

  public List<Message> getSingleMessages() {
    return singleMessages;
  }

  public boolean hasMessageGroups() {
    return groupedMessages != null && ! groupedMessages.isEmpty();
  }

  public List<MessageGroup> getMessageGroups() {
    return groupedMessages;
  }

  public String singleMessagesToString(String separator) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for (Message message : singleMessages) {
      sb.append(sep).append(message.getMessage());
      sep = separator;
    }
    return sb.toString();
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  public void failAlreadyExistingNoOverwrite(List<String> alreadyExistingTypes) {
    fail("Some data models already exists and overwrite-flag is not set");
    addMessageGroup(Level.Warning, "Already existing data types", alreadyExistingTypes);
  }

  public void infoAlreadyExisting(List<String> alreadyExistingTypes) {
    info("Some data types already exist and will be overwritten");
  }

  public void savedDataModel(String modelName, Integer xmomTypeCount) {
    info("Saved data model "+modelName+" with "+ xmomTypeCount+" data types." );
  }

}
