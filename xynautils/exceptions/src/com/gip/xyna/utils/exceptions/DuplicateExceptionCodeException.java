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
public class DuplicateExceptionCodeException extends XynaException {


  private String duplicateCode;
  private String language;

  public DuplicateExceptionCodeException(String duplicateCode, String language) {
    super(new String[]{"XYNA-00001", duplicateCode + "", language + ""});
    setDuplicateCode(duplicateCode);
    setLanguage(language);
  }

  public DuplicateExceptionCodeException(String duplicateCode, String language, Throwable cause) {
    super(new String[]{"XYNA-00001", duplicateCode + "", language + ""}, cause);
    setDuplicateCode(duplicateCode);
    setLanguage(language);
  }

  protected DuplicateExceptionCodeException(String[] args) {
    super(args);
  }

  protected DuplicateExceptionCodeException(String[] args, Throwable cause) {
    super(args, cause);
  }

  public void setDuplicateCode(String duplicateCode) {
    this.duplicateCode = duplicateCode;
  }

  public String getDuplicateCode() {
    return duplicateCode;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getLanguage() {
    return language;
  }

  public DuplicateExceptionCodeException initCause(Throwable t) {
    return (DuplicateExceptionCodeException) super.initCause(t);
  }


}
