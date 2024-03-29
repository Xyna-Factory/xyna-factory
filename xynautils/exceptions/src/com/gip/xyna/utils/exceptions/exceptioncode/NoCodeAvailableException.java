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
package com.gip.xyna.utils.exceptions.exceptioncode;

import com.gip.xyna.utils.exceptions.XynaException;

//DO NOT CHANGE
//GENERATED BY com.gip.xyna.utils.exceptions.utils.codegen.JavaClass 2010-06-02T10:09:58Z;
public class NoCodeAvailableException extends XynaException {


  private String codeGroupName;

  public NoCodeAvailableException(String codeGroupName) {
    super(new String[]{"XYNA-00011", codeGroupName + ""});
    setCodeGroupName(codeGroupName);
  }

  public NoCodeAvailableException(String codeGroupName, Throwable cause) {
    super(new String[]{"XYNA-00011", codeGroupName + ""}, cause);
    setCodeGroupName(codeGroupName);
  }

  protected NoCodeAvailableException(String[] args) {
    super(args);
  }

  protected NoCodeAvailableException(String[] args, Throwable cause) {
    super(args, cause);
  }

  public void setCodeGroupName(String codeGroupName) {
    this.codeGroupName = codeGroupName;
  }

  public String getCodeGroupName() {
    return codeGroupName;
  }

  public NoCodeAvailableException initCause(Throwable t) {
    return (NoCodeAvailableException) super.initCause(t);
  }


}
