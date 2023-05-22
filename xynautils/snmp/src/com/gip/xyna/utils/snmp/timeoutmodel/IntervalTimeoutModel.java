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
package com.gip.xyna.utils.snmp.timeoutmodel;


/**
 * IntervalTimeoutModel reads the retry-intervals from the int-array.
 * If the array has length n, n-1 retries are done. (1 extra timeout after
 * the last retry). 
 *
 */
public class IntervalTimeoutModel implements TimeoutModel2 {
  
  private int[] retryIntervals;
  private long requestTimeout;
  
  /**
   * default constructor
   */
  public IntervalTimeoutModel() { /*nothing to do*/ }

  /**
   * @param retryIntervals
   */
  public IntervalTimeoutModel(int[] retryIntervals) {
    String check = check(retryIntervals);
    if( check != null ) {
      throw new IllegalArgumentException(check);
    }
    this.retryIntervals=retryIntervals.clone();
    for( int i=0; i< this.retryIntervals.length; ++i ) {
      requestTimeout += this.retryIntervals[i];
    }
  }

  public int getRetries() {
    return retryIntervals.length -1;
  }

  public long getRetryTimeout(int retryCount) {
    return retryIntervals[retryCount];
  }

  public long getRequestTimeout() {
    return requestTimeout;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel#newInstance(int, int)
   */
  public TimeoutModel newInstance(int retries, int timeout) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel2#newInstance(int[])
   */
  public TimeoutModel newInstance(int[] data) {
    return new IntervalTimeoutModel(data);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel2#check(int, int)
   */
  public String check(int retries, int timeout) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel2#check(int[])
   */
  public String check(int[] timeoutModelData) {
    if( timeoutModelData == null || timeoutModelData.length == 0 ) {
      return "retryIntervals must have length >= 1";
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
