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
package com.gip.xyna.xprc.xfractwfe.base.parallel;

import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xsched.OrderTypeMaxParallelism;
import com.gip.xyna.xprc.xsched.OrderTypeMaxParallelism.MaxParallelismChangeListener;

/**
 * BlockingParallelismLimitation-Implemetierung, die als Limitierung die Anzahl an 
 * Aufträgen nimmt, die maximal wegen erfoderlicher Capacities laufen könnten.
 */
public class CapacityBlockingParallelismLimitation extends AbstractBlockingParallelismLimitation 
    implements MaxParallelismChangeListener {

  private OrderTypeMaxParallelism orderTypeMaxParallelism;
 

  public CapacityBlockingParallelismLimitation(OrderTypeMaxParallelism orderTypeMaxParallelism) {
    this.orderTypeMaxParallelism = orderTypeMaxParallelism;
    orderTypeMaxParallelism.addMaxParallelismChangeListener(this);
  }


  public void maxParallelismChanged() {
    setMaxThreadLimit(orderTypeMaxParallelism.maxParallelism());
  }


  @Override
  public int getMaxThreadLimit() {
    return orderTypeMaxParallelism.maxParallelism();
  }

  @Override
  public boolean shouldLaneBeBlocked(ProcessSuspendedException suspendedException) {
    return ! suspendedException.getSuspensionCause().needToFreeCapacities();
  }

  @Override
  public String toString() {
    return "CapacityBlockingParallelismLimitation("+orderTypeMaxParallelism.getLimitingCapName()+")";
  }
}
