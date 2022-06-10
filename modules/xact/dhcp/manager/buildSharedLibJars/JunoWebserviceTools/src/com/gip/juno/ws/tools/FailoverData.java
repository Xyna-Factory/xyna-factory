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

package com.gip.juno.ws.tools;

import com.gip.juno.ws.enums.FailoverFlag;

/**
 * Class that wraps two ConnectionInfo instances for primary and secondary instance of a failover pair
 */
public class FailoverData {
  private ConnectionInfo _data1;
  private ConnectionInfo _data2;
  
  public void setPrimary(ConnectionInfo data) {
    _data1 = data;
  }
  public void set(ConnectionInfo data, FailoverFlag flag) {
    if (flag == FailoverFlag.primary) {
      _data1 = data;
    } else if (flag == FailoverFlag.secondary) {
      _data2 = data;
    } 
  }
  public void setSecondary(ConnectionInfo data) {
    _data2 = data;
  }
  public ConnectionInfo getPrimary() {
    return _data1;
  }
  public ConnectionInfo getSecondary() {
    return _data2;
  }
  public ConnectionInfo get(FailoverFlag flag) {
    if (flag == FailoverFlag.primary) {
      return _data1;
    } else if (flag == FailoverFlag.secondary) {
      return _data2; 
    }
    return null;
  }
  
  public String toString() {
    String ret = "FailoverData { Primary Connection: \n ";
    if (_data1 != null) {
      ret += _data1.toString();
    }
    else {
      ret += " null, "; 
    }
    ret += " Secondary Connection: \n ";
    if (_data2 != null) {  
      ret += _data2.toString() + "} \n";
    } else {
      ret += " null. \n ";
    }
    return ret;
  }

}