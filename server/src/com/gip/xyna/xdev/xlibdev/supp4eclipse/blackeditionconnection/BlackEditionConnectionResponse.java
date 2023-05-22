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

package com.gip.xyna.xdev.xlibdev.supp4eclipse.blackeditionconnection;

import com.gip.xyna.utils.exceptions.XynaException;


public class BlackEditionConnectionResponse {


  private final Object response;

  public BlackEditionConnectionResponse(Object response) {
    this.response = response;
  }

  public Object getResponse() {
    return response;
  }


  public Object[] getResponseAsComplexStringArray() throws XynaException {
    if (response instanceof Object[])
      return getResponseAsComplexStringArray((Object[]) response);
    else {
      return new Object[] {parseObjectAsString(response)};
    }
  }


  private Object[] getResponseAsComplexStringArray(Object[] x) throws XynaException {

    try {
      Object[] oArray = x;
      Object[] result = new Object[oArray.length];
      for (int i = 0; i < oArray.length; i++) {
        if (oArray[i] instanceof Byte[]) {
          result[i] = parseObjectAsString(oArray[i]);
        } else if (oArray[i] instanceof Object[]) {
          result[i] = getResponseAsComplexStringArray((Object[]) oArray[i]);
        } else {
          result[i] = parseObjectAsString(oArray[i]);
        }
      }
      return result;
    } catch (ClassCastException e) {
      throw new XynaException("Response object is not an array of objects");
    }

  }

  public String getResponseAsString() throws XynaException {
    return parseObjectAsString(response);
  }


  public String[] getResponseAsListOfStrings() throws XynaException {
    try {
      Object[] oArray = (Object[]) response;
      String[] result = new String[oArray.length];
      for (int i = 0; i < oArray.length; i++) {
        result[i] = parseObjectAsString(oArray[i]);
      }
      return result;
    } catch (ClassCastException e) {
      throw new XynaException("Response object is not an array of objects");
    }
  }


  private String parseObjectAsString(Object o) throws XynaException {
    try {
      byte[] ba;
      if (o instanceof Byte[]) {
        Byte[] byteArray = (Byte[]) o;
        ba = new byte[byteArray.length];
        for (int i = 0; i < ba.length; i++) {
          ba[i] = byteArray[i];
        }
      }
      else {
        ba = (byte[]) o;
      }
      return new String(ba);
    } catch (ClassCastException e) {
      throw new XynaException("Response object is not a single String", e);
    }
  }

}
