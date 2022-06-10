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


package com.gip.juno.ws.tools.snmp;

import com.gip.xyna.utils.snmp.exception.SnmpResponseException;


/**
 * information about response of snmp set command
 */
public class SnmpSetResponseStatus {

  protected boolean _isOk = true;

  protected int _errorStatus = 0;

  protected Exception _exception = null;


  public SnmpSetResponseStatus() {}

  public SnmpSetResponseStatus(boolean isOk) {
    _isOk = isOk;
  }

  public SnmpSetResponseStatus(SnmpResponseException ex) {
    _errorStatus = ex.getErrorStatus();
    _exception = ex;
    _isOk = false;
  }

  public boolean isStatusOK() {
    return _isOk;
  }

  public int getErrorStatus() {
    return _errorStatus;
  }


  public Exception getException() {
    return _exception;
  }

}
