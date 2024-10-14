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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import java.util.Set;

public enum InconsistencyState {
  
  /*
   * pos => bedeutung
   * [0] => SAVED->SAVED
   * [1] => SAVED->DEPLOYED
   * [2] => DEPLOYED->SAVED
   * [3] => DEPLOYED->DEPLOYED
   */
  INVALID_0101(false),
  INVALID_10xy(false),
  INVALID_11xx(false),
  INVALID_0011(false),
  INVALID_0110(true),
  INVALID_0100(true),
  INVALID_0010(true),
  INVALID_1000(false),
  INVALID_0001(false), 
  ;
  

  private final boolean impendingOnly;
  
  private InconsistencyState(boolean impendingOnly) {
    this.impendingOnly = impendingOnly;
  }
  
  public boolean isImpendingOnly() {
    return impendingOnly;
  }
  
  
  public static InconsistencyState get(DeploymentItemInterface diii,
                                       Set<DeploymentItemInterface> ss,
                                       Set<DeploymentItemInterface> sd,
                                       Set<DeploymentItemInterface> ds,
                                       Set<DeploymentItemInterface> dd) {
    final boolean inc_ss = ss != null && ss.contains(diii);
    final boolean inc_sd = sd != null && sd.contains(diii);
    final boolean inc_ds = ds != null && ds.contains(diii);
    final boolean inc_dd = dd != null && dd.contains(diii);
    
    if (!inc_ss &&
        inc_sd &&
        !inc_ds &&
        inc_dd) {
      return INVALID_0101;
    } else if (inc_ss &&
               !inc_sd &&
               (inc_ds || inc_dd)) {
      return INVALID_10xy;
    } else if (inc_ss &&
               inc_sd &&
               inc_ds == inc_dd) {
      return INVALID_11xx;
    } else if (!inc_ss &&
               !inc_sd &&
               inc_ds &&
               inc_dd) {
      return INVALID_0011;
    } else if (!inc_ss &&
               inc_sd &&
               inc_ds &&
               !inc_dd) {
      return INVALID_0110;
    } else if (!inc_ss &&
               inc_sd &&
               !inc_ds &&
               !inc_dd) {
      return INVALID_0100;
    } else if (!inc_ss &&
               !inc_sd &&
               inc_ds &&
               !inc_dd) {
      return INVALID_0010;
    } else if (inc_ss &&
               !inc_sd &&
               !inc_ds &&
               !inc_dd) {
      return INVALID_1000;
    } else if (!inc_ss &&
               !inc_sd &&
               !inc_ds &&
               inc_dd) {
     return INVALID_0001;
    } else {
      String state = (inc_ss ? "1" : "0") + (inc_sd ? "1" : "0") + (inc_ds ? "1" : "0") + (inc_dd ? "1" : "0");
      throw new IllegalArgumentException("Unmatched inconsitency state " + state);
    }
  }
  
}
