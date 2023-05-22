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

package com.gip.xyna.xprc.xfractwfe.generation;


public class ErrorInfo {
  // Exception
  
  private AVariable exceptionVariable = null;
  
  public void setExceptionVariable(AVariable exception) {
    this.exceptionVariable = exception;
  }
  
  public AVariable getExceptionVariable() {
    return exceptionVariable;
  }
  
  // Message
  
  private String message;
  
  public String getMessage() {
    return message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  // Stacktrace
  
  private String stacktrace;
  
  public String getStacktrace() {
    return stacktrace;
  }
  
  public void setStacktrace(String stacktrace) {
    this.stacktrace = stacktrace;
  }
}
