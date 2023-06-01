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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.util.HashMap;
import java.util.Map;

public class Conditional {

  private String conditional;//original conditional aus DeviceClass
  private String parsedConditional;
  private Map<String, String> parsedSubconditionals = new HashMap<String, String>();
  
  public Conditional(String conditional, String parsedConditional, Map<String, String> parsedSubconditionals){
    this.conditional = conditional;
    this.parsedConditional = parsedConditional;
    this.parsedSubconditionals = parsedSubconditionals;
  }
  
  public String getConditional(){
    return conditional;
  }
  
  public void setConditional(String conditional){
    this.conditional = conditional;
  }
  
  public String getParsedConditional(){
    return parsedConditional;
  }
  
  public void setParsedConditional(String parsedConditional){
    this.parsedConditional = parsedConditional;
  }
  
  public Map<String, String> getParsedSubconditionals(){
    return parsedSubconditionals;
  }
  
  public void setParsedSubconditionals(Map<String, String> parsedSubconditionals){
    this.parsedSubconditionals = parsedSubconditionals;
  }
  
}
