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
package com.gip.xyna.xact.trigger;

public class SSHCustomizationParameter {

  public enum NewLine {
    CRLF("\r\n") {
      public String mapNewLines(String msg) {
        return msg.replaceAll("\n", "\r\n");
      }
    }, 
    LF("\n") {
      public String mapNewLines(String msg) {
        return msg;
      }
    }, 
    None("\r\n") {
      public String mapNewLines(String msg) {
        return msg;
      }
    };

    private String nl;

    private NewLine(String nl) {
      this.nl = nl;
    }
    
    public abstract String mapNewLines(String msg);

    public String getNewLine() {
      return nl;
    }
  }
  
  private String identifier;
  private String errorPrefix;
  private NewLine newLine;
  
  
  public String getIdentifier() {
    return identifier;
  }
  
  public String getErrorPrefix() {
    return errorPrefix;
  }
  
  public NewLine getNewLine() {
    return newLine;
  }
  
  public static SSHCustomizationParameter build(String identifier, 
      String errorPrefix,
      NewLine newLine) {
    SSHCustomizationParameter scp = new SSHCustomizationParameter();
    scp.identifier = identifier;
    scp.errorPrefix = errorPrefix;
    scp.newLine = newLine;
    
    return scp;
  }

  public static SSHCustomizationParameter buildDefault() {
    SSHCustomizationParameter scp = new SSHCustomizationParameter();
    scp.identifier = "";
    scp.errorPrefix = "ERROR:";
    scp.newLine = NewLine.CRLF; //TODO ok?
    return scp;
  }
  
  
  
}
