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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.Serializable;
import java.util.EnumSet;

import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;



public class StartApplicationParameters implements Serializable{

  private static final long serialVersionUID = 1L;


  private boolean forceStartInInconsistentCluster = false; //auch starten, obwohl application im cluster auf anderem knoten nicht existiert
  private boolean global = false;
  private EnumSet<OrderEntranceType> onlyEnableOrderEntrance = null; //falls angegeben, nur diese arten von orderentrance aktivieren 
  private boolean enableCrons = false; //alle Crons enablen


  public boolean isForceStartInInconsistentCluster() {
    return forceStartInInconsistentCluster;
  }


  public void setForceStartInInconsistentCluster(boolean forceStartInInconsistentCluster) {
    this.forceStartInInconsistentCluster = forceStartInInconsistentCluster;
  }


  public boolean isGlobal() {
    return global;
  }


  public void setGlobal(boolean global) {
    this.global = global;
  }


  public EnumSet<OrderEntranceType> getOnlyEnableOrderEntrance() {
    return onlyEnableOrderEntrance;
  }


  public void setOnlyEnableOrderEntrance(EnumSet<OrderEntranceType> onlyEnableOrderEntrance) {
    this.onlyEnableOrderEntrance = onlyEnableOrderEntrance;
  }


  public boolean isEnableCrons() {
    return enableCrons;
  }


  public void setEnableCrons(boolean enableCrons) {
    this.enableCrons = enableCrons;
  }

}
