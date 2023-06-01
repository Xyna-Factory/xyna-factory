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

package com.gip.xyna.xfmg.xclusteringservices;


public interface ClusterStateChangeHandler {

  /**
   * Ein Wechsel des ClusterState nach newState ist fest geplannt.
   * Mit dieser Methode wird dem ClusterStateChangeHandler folgendes erm�glicht:
   * a) den Wechsel zu verz�gern (durch R�ckgabe von false) 
   * b) einen fr�heren, noch nicht fertigen ClusterState-Wechsel abzubrechen. 
   * @param newState
   * @return true, wenn der ClusterState-Wechsel m�glich ist
   */
  public boolean isReadyForChange(ClusterState newState);
  
  /**
   * Ausf�hrung des ClusterState-Wechsels nach newState
   * @param newState
   */
  public void onChange(ClusterState newState);
  
}
