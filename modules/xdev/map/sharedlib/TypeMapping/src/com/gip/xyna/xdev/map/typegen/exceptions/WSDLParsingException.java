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
package com.gip.xyna.xdev.map.typegen.exceptions;


/**
 *
 */
public class WSDLParsingException extends Exception {

  private static final long serialVersionUID = 1L;

  private WSDLParsingFailure failure;

  private String wsdlName;
  
  public static enum WSDLParsingFailure {
    Parse, WriteXsd
  }
  
  public WSDLParsingException(WSDLParsingFailure failure, String wsdlName, Throwable cause) {
    super(failure +": "+wsdlName, cause);
    this.wsdlName = wsdlName;
    this.failure = failure;
  }

  
  public WSDLParsingFailure getFailure() {
    return failure;
  }
  
  public String getName() {
    return wsdlName;
  }

}
