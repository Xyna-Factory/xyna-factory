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
package com.gip.xyna.xdev.map.typegen.exceptions;

import java.util.ArrayList;
import java.util.List;


public class XSDParsingException extends Exception {

  private static final long serialVersionUID = 1L;

  private List<String> errors;
  
  public XSDParsingException(List<String> errors) {
    super( errors.toString() );
    this.errors = errors;
  }

  public XSDParsingException(String message) {
    this.errors = new ArrayList<String>();
    this.errors.add(message);
  }

  public  List<String> getErrors() {
    return errors;
  }

}
