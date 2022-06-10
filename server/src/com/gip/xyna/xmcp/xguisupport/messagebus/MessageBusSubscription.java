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
package com.gip.xyna.xmcp.xguisupport.messagebus;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.Pathable;
import com.gip.xyna.xmcp.xguisupport.messagebus.TrappedPathTree.Trap;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageOutputParameter;


public class MessageBusSubscription implements Serializable, Pathable {

  private static final long serialVersionUID = 4209231871153821458L;
  private static Pattern fuzzyCorrelationPatter = Pattern.compile(".*[|.+*].*",Pattern.MULTILINE);
  
  private Long id;
  private String product;
  private String context;
  private String filter;
  private Pattern filterPattern;
  private boolean unreceivedMessages = true;
  private boolean forceReadPersistent = true;
  private Trap<Message> trap;
  
  
  public MessageBusSubscription(Long id, String product, String context, String filter) {
    this.id = id;
    this.product = product;
    this.context = context;
    this.filter = filter;
  }
  
  
  public Long getId() {
    return id;
  }
  
  public String getProduct() {
    return product;
  }
  
  
  public String getContext() {
    return context;
  }
  
  
  public String getFilter() {
    return filter;
  }
  
  
  public boolean hasUnreceivedMessages() {
    return unreceivedMessages;
  }
  
  
  void setUnreceivedMessages(boolean unreceivedMessages) {
    this.unreceivedMessages = unreceivedMessages;
  }
  
  
  public boolean forceReadPersistent() {
    return forceReadPersistent;
  }
  
  
  void setForceReadPersistent(boolean forceReadPersistent) {
    this.forceReadPersistent = forceReadPersistent;
  }
  
  
  boolean check(Message message) {
    return this.check(message.getProduct(), message.getContext(), message.getCorrelation());
  }
  
  boolean check(MessageOutputParameter message) {
    return this.check(message.getProduct(), message.getContext(), message.getCorrelation());
  }
  
  boolean check(String product, String context, String correlation) {
    if (this.product.equals(product)) {
      if (this.context.equals(context)) {
        return check(correlation);
      }
    }
    return false;
  }
  
  
  boolean check(String correlation) {
    if (filter == null) {
      return true;
    }
    if (filterPattern == null) {
      filterPattern = Pattern.compile(filter);
    }
    Matcher filterMatcher = filterPattern.matcher(correlation);
    return filterMatcher.matches();
  }


  public String[] getPath() { // cache path
    if (filter == null) {
      return new String[] {product, context};
    }
    Matcher fuzzyCorrelationMatcher = fuzzyCorrelationPatter.matcher(filter);
    if (fuzzyCorrelationMatcher.matches()) {
      return new String[] {product, context};
    } else {
      return new String[] {product, context, filter}; 
    }
  }


  void setTrap(Trap<Message> trap) {
    this.trap = trap;
  }


  Trap<Message> getTrap() {
    return trap;
  }
  
}
