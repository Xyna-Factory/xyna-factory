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

package com.gip.xyna.xprc.xpce.statustracking;

import java.util.ArrayList;

import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;


public interface IStatusChangeListener {

  /**
   * This is executed once an order corresponding to one of the watched DestinationsKeys performs a state transition.
   * 
   * @param orderId the relevant order id
   * @param newState its new state
   * @param sourceId the ID of the underlying ManualInteraction workflow or null if there is no MI
   */
  public void statusChanged(Long orderId, String newState, Long sourceId);


  /**
   * Is asked what DestinationKeys are watched upon adding or removing the listener. It is not relevant, however, when
   * it is checked whether a given order is watched by this listener. This circumstance should be improved in later
   * versions.
   */
  public ArrayList<DestinationKey> getWatchedDestinationKeys();

}
