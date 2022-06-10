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

package com.gip.juno.ws.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


public class MessageBuilder {
 
  private String domain = "U";
  private String errorNumber = "00001";
  private String severity = "3";
  private List<String> parameters = new ArrayList<String>();
  private String description = "No description supplied.";
  private String stackTraceStr = "No stacktrace supplied.";

  public MessageBuilder setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public MessageBuilder setErrorNumber(String number) {
    this.errorNumber = number;
    return this;
  } 

  public MessageBuilder setSeverity(String severity) {
    this.severity = severity;
    return this;
  } 

  public MessageBuilder addParameter(String param) {
    this.parameters.add(param);
    return this;
  }

  public MessageBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public MessageBuilder setCause(Throwable cause) {
    this.stackTraceStr = stackTraceToString(cause);
    return this; 
  }

  public MessageBuilder setCause(String cause) {
    this.stackTraceStr = cause;
    return this; 
  }
    
  public static String build(MessageBuilder data) {
    return buildMessage(data);
  }
  
  public String build() {
    return buildMessage(this);
  }
  
  public static String buildMessage(MessageBuilder data) {
    StringBuilder ret = new StringBuilder(""); 
    ret.append(data.domain);
    ret.append("-");
    ret.append(data.errorNumber);
    ret.append("-");
    ret.append(data.severity);
    ret.append("|");    
    for (String param : data.parameters) {      
      ret.append(param);
      ret.append("|");
    }
    ret.append("#|");
    ret.append(data.description);
    ret.append("|#|");
    ret.append(data.stackTraceStr);
    return ret.toString();
  }
  
  
  /** 
   * gibt den Stacktrace einer Exception als String zurueck 
   * @param t beliebiges Throwable
   * @return printStackTrace als String
   */
  public static String stackTraceToString(Throwable t) {
    Writer sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    String s = sw.toString();
    return s;
  }
}
