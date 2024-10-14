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
package com.gip.xyna.utils.snmp.timeoutmodel;


/**
 * SimpleTimeoutModel uses always the same timeout for all retries.
 *
 */
public class SimpleTimeoutModel implements TimeoutModel {
  
  private int numberRetries;
  private long timeout;
  
  /**
   * default constructor
   */
  public SimpleTimeoutModel() { /*nothing to do*/ }
  
  /**
   * @param retries
   * @param timeout
   */
  public SimpleTimeoutModel(int retries, int timeout) {
    String check = check(retries,timeout);
    if( check != null ) {
      throw new IllegalArgumentException(check);
    }
    numberRetries = retries;
    this.timeout = timeout;
  }

  public int getRetries() {
    return numberRetries;
  }

  public long getRetryTimeout(int retryCount) {
    return timeout;
  }

  public long getRequestTimeout() {
    return (numberRetries+1)*timeout;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel#newInstance(int, int)
   */
  public TimeoutModel newInstance(int retries, @SuppressWarnings("hiding") int timeout) {
    return new SimpleTimeoutModel(retries,timeout);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel#check(int, int)
   */
  public String check(int retries, @SuppressWarnings("hiding") int timeout) {
    if( retries < 0 ) {
      return "Retries < 0";
    }
    if( timeout < 0 ) {
      return "Timeout < 0";
    }
    return null;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel#newInstance()
   */
  public TimeoutModel newInstance() {
    return this;
  }

}
