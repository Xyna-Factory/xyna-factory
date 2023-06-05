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
package com.gip.xyna.utils.snmp.exception;

/**
 * Exception is thrown when a requested parameter is missing 
 *
 */
public class ParamNotSetException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private String paramName;

  /**
   * @param paramName
   */
  public ParamNotSetException(String paramName) {
    super( "Parameter "+paramName+" is missing" );
    this.paramName = paramName;
  }

  /**
   * @return
   */
  public String getParamName() {
    return paramName;
  }

}
