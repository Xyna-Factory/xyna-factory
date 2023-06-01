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
package com.gip.xyna.xprc.xfractwfe.generation.serviceimpl;

import com.gip.xyna.utils.exceptions.XynaException;


public class XynaExceptionResultingFromWorkflowCall extends RuntimeException {

  private static final long serialVersionUID = 1L;
  
  
  public XynaExceptionResultingFromWorkflowCall(XynaException xe) {
    super(xe);
  }
  
  public XynaException getCause() {
    return (XynaException) super.getCause();
  }

}
