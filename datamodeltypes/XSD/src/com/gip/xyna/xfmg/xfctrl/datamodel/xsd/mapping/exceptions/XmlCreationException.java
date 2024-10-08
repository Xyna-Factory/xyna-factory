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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions;


/**
 *
 */
public class XmlCreationException extends Exception {

  private static final long serialVersionUID = 1L;

  private XmlCreationFailure failure;
  private String type;
  
  public static enum XmlCreationFailure {
    Configuration, Writing, Creation, InvalidData;
    
  }
  
  public XmlCreationException(XmlCreationFailure failure, String value) {
    super(failure +": "+value);
    this.failure = failure;
  }
  
  public XmlCreationException(XmlCreationFailure failure, String value, Throwable cause) {
    super(failure +": "+value, cause);
    this.failure = failure;
  }
  
  public XmlCreationException(XmlCreationFailure failure, Throwable cause) {
    super(failure +": "+cause.getMessage(), cause);
    this.failure = failure;
  }

  public XmlCreationFailure getFailure() {
    return failure;
  }

  public String getType() {
    return type;
  }
  
  public void setType(String type) {
    this.type = type;
  }
  
}
