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
package xact.http.impl;

import java.util.ArrayList;
import java.util.List;

import xact.http.enums.statuscode.HTTPStatusCode;

import com.gip.xyna.xact.StatusCode;


public class StatusCodeSet {
  
  private static StatusCodeSet Set_OK = new StatusCodeSet(StatusCode.OK); //häufigster Fall
  
  public static StatusCodeSet parseExpectedList(List<? extends HTTPStatusCode> expectedHTTPStatusCode) {
   
    if( expectedHTTPStatusCode == null || expectedHTTPStatusCode.isEmpty() ) {
      return Set_OK;
    }
    if( expectedHTTPStatusCode.size() == 1 ) {
      StatusCode sc = StatusCode.parse( expectedHTTPStatusCode.get(0));
      if( sc != null ) {
        return new StatusCodeSet(sc);
      } else {
        return new StatusCodeSet(expectedHTTPStatusCode.get(0).getCode());
      }
    } else {
      StatusCodeSet scs = new StatusCodeSet();
      for( HTTPStatusCode hsc : expectedHTTPStatusCode ) {
        StatusCode sc = StatusCode.parse(hsc);
        if( sc != null ) {
          scs.add(sc);
        } else {
          scs.add(hsc.getCode());
        }
      }
      return scs;
    }
  }
  
  private int code;
  private ArrayList<Integer> list;

  private StatusCodeSet(StatusCode statusCode) {
    this.code = statusCode.getCode();
    this.list = null;
  }
  private StatusCodeSet(int statusCode) {
    this.code = statusCode;
    this.list = null;
  }

  private StatusCodeSet() {
    this.code = -1;
    this.list = new ArrayList<Integer>();
  }

  private void add(StatusCode statusCode) {
    list.add( statusCode.getCode() );
  }
  private void add(int statusCode) {
    list.add( statusCode );
  }

  public boolean check(HTTPStatusCode status) {
    int statusCode = status.getCode();
    if( code != -1 ) {
      return code == statusCode;
    } else {
      for( Integer c : list ) {
        if( c.intValue() == statusCode ) {
          return true;
        }
      }
      return false;
    }
  }

}
