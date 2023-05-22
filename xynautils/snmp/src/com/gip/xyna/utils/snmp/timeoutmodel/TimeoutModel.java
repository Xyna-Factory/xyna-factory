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
 * Interface for all TimeoutModels, which can be parameterized 
 * by the two parameters "int retries, int timeout"
 * 
 * New defined TimeoutModels should be registered:
 * TimeoutModels.registerTimeoutModel( "defined", new DefinedTimeoutModel() );
 * Then they can be used in SnmpAccessData and SnmpContext.
 *
 */
public interface TimeoutModel {
  
  /**
   * Gets the timeout for the specified retry (a zero value for
   * <code>retryCount</code> specifies the first request).
   * @param retryCount
   *    the number of retries already performed for the target.
   */
  public long getRetryTimeout(int retryCount);

  /**
   * Gets the timeout for all retries, which is defined as the sum of
   * {@link #getRetryTimeout(int retryCount)}
   * for all <code>retryCount</code> in
   * <code>0 <= retryCount < totalNumberOfRetries</code>.
   * @return
   *    the time in milliseconds when the request will be timed out finally.
   */
  public long getRequestTimeout();

  /**
   * Gets the number of retries to be performed before a request is timed out.
   * @param retries
   *    the number of retries. <em>Note: If the number of retries is set to
   *    0, then the request will be sent out exactly once.</em>
   */
  public int getRetries();
  
  /**
   * Returns a new Instance (constructor in interface), with all data cloned
   * from the current instance (or for immutable TimeoutModels "return this" is allowed)
   * @return
   */
  public TimeoutModel newInstance();

  /**
   * Returns a new Instance (constructor in interface)
   * @param retries
   * @param timeout
   * @return
   */
  public TimeoutModel newInstance(int retries, int timeout);

  /**
   * Checks whether parameters are valid
   * @param retries
   * @param timeout
   * @return null if parameters are valid, else error description
   */
  public String check(int retries, int timeout);

}
