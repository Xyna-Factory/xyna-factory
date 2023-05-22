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

package com.gip.xyna.xprc.xprcods.orderarchive.selectorder;



public interface OISelect {

  // selects

  public OrderInstanceSelect selectId();

  public OrderInstanceSelect selectParentId();
  public OrderInstanceSelect selectExecutionType();
    
  public OrderInstanceSelect selectPriority();
  public OrderInstanceSelect selectStatus();
  public OrderInstanceSelect selectStartTime();
  public OrderInstanceSelect selectStopTime();
  public OrderInstanceSelect selectLastUpdate();
  public OrderInstanceSelect selectOrderType();
  public OrderInstanceSelect selectMonitoringLevel();
  public OrderInstanceSelect selectCustom0();
  public OrderInstanceSelect selectCustom1();
  public OrderInstanceSelect selectCustom2();
  public OrderInstanceSelect selectCustom3();
  public OrderInstanceSelect selectExceptions();
  
}
