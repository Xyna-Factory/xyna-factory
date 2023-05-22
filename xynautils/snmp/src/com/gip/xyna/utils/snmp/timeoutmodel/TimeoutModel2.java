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
 * Interface for all TimeoutModels, which can not be parameterized 
 * by the two parameters "int retries, int timeout", these 
 * TimeoutModels are contructed with an int[].
 * 
 * New defined TimeoutModels should be registered:
 * TimeoutModels.registerTimeoutModel( "defined", new DefinedTimeoutModel() );
 * Then they can be used in SnmpAccessData and SnmpContext.
 *
 */
public interface TimeoutModel2 extends TimeoutModel {

  /**
   * Returns a new Instance (constructor in interface)
   * @param data
   * @return
   */
  public TimeoutModel newInstance(int[] data); 

  /**
   * Checks whether parameters are valid
   * @param retries
   * @param timeout
   * @return null if parameters are valid, else error description
   */
  public String check(int retries, int timeout);

  /**
   * @param timeoutModelData
   * @return
   */
  public String check(int[] timeoutModelData);

}
