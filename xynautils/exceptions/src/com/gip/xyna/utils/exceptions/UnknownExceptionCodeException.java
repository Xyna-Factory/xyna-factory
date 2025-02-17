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
package com.gip.xyna.utils.exceptions;


//DO NOT CHANGE
//GENERATED BY com.gip.xyna.utils.exceptions.utils.codegen.JavaClass 2010-06-02T10:09:58Z;
public class UnknownExceptionCodeException extends XynaException {


  private String unknownCode;
  private String providedParameters;

  public UnknownExceptionCodeException(String unknownCode, String providedParameters) {
    super(new String[]{"XYNA-00003", unknownCode + "", providedParameters + ""});
    setUnknownCode(unknownCode);
    setProvidedParameters(providedParameters);
  }

  public UnknownExceptionCodeException(String unknownCode, String providedParameters, Throwable cause) {
    super(new String[]{"XYNA-00003", unknownCode + "", providedParameters + ""}, cause);
    setUnknownCode(unknownCode);
    setProvidedParameters(providedParameters);
  }

  protected UnknownExceptionCodeException(String[] args) {
    super(args);
  }

  protected UnknownExceptionCodeException(String[] args, Throwable cause) {
    super(args, cause);
  }

  public void setUnknownCode(String unknownCode) {
    this.unknownCode = unknownCode;
  }

  public String getUnknownCode() {
    return unknownCode;
  }

  public void setProvidedParameters(String providedParameters) {
    this.providedParameters = providedParameters;
  }

  public String getProvidedParameters() {
    return providedParameters;
  }

  public UnknownExceptionCodeException initCause(Throwable t) {
    return (UnknownExceptionCodeException) super.initCause(t);
  }


}
