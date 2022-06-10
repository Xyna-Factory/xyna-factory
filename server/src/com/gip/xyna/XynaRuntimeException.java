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

package com.gip.xyna;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;


public class XynaRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private List<Throwable> runtimeExceptions;
  private List<XynaException> xynaExceptions;

  private String message;


  public XynaRuntimeException(String message, List<Throwable> runtimeExceptions,
                              List<XynaException> xynaExceptions) {
    super(message);
    this.message = message;
    this.setRuntimeExceptions(runtimeExceptions);
    this.setXynaExceptions(xynaExceptions);
  }


  public XynaRuntimeException(List<Throwable> runtimeExceptions, List<XynaException> xynaExceptions) {
    this("Several errors occurred", runtimeExceptions, xynaExceptions);
  }


  public void setXynaExceptions(List<XynaException> xynaExceptions) {
    this.xynaExceptions = xynaExceptions;
  }


  public List<XynaException> getXynaExceptions() {
    return xynaExceptions;
  }


  public void setRuntimeExceptions(List<Throwable> runtimeExceptions) {
    this.runtimeExceptions = runtimeExceptions;
  }


  public List<Throwable> getRuntimeExceptions() {
    return runtimeExceptions;
  }

  
  public String toString() {
    StringBuilder result = new StringBuilder().append(message).append(": ");
    if (runtimeExceptions != null) {
      for (Throwable t : runtimeExceptions) {
        result.append(t.getMessage()).append(" (").append(t.getClass().getSimpleName()).append("), ");
      }
    }
    if (xynaExceptions != null) {
      for (XynaException e : xynaExceptions) {
        result.append(e.getMessage()).append(", ");
      }
    }
    String resultAsString = result.toString();
    return resultAsString.substring(0, resultAsString.length() - 2);
  }

}
