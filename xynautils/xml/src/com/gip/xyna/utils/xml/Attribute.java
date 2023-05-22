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
package com.gip.xyna.utils.xml;

public class Attribute {

  private String name;
  private String value;
  
  /**
   * Creates a new empty Attribute
   */
  public Attribute() {}
  
  /**
   * Creates a new Attribute with name and value
   * @param name
   * @param value
   */
  public Attribute( String name, Object value ) {
    this.name = name;
    if( value != null ) {
      this.value = value.toString();
    } else {
      this.value = null;
    }
  }
  
  public String toString() {
    return name + "=\"" + XMLUtils.encodeString( value ) + "\"";
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
  
}
