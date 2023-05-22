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
package com.gip.xyna.xprc.xfractwfe.base.parallel;

import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;


/**
 * usecase: die auftr�ge haben kapazit�ten, man will aber auf eine geringere anzahl als die kardinalit�t der kapazit�t beschr�nken.
 * dann ben�tigt man hier trotzdem, dass die auftr�ge die die kapazit�ten beim suspend nicht freigeben, sp�ter wieder
 * gestartet werden, damit es nicht zu deadlocks kommt
 * deadlocksituation:
 *  - alle (limitierten) lanes der parallelit�t sind durch auftr�ge verstopft, die im scheduler auf kapazit�t warten
 * - alle auftr�ge, die die kapazit�t haben, kommen nicht durch die parallelit�t durch
 */
public class ConstantBlockingParallelismLimitation extends AbstractBlockingParallelismLimitation {

  private int threadLimit;

  public ConstantBlockingParallelismLimitation(int threadLimit) {
    this.threadLimit = threadLimit;
    if( threadLimit <= 0 ) {
      throw new IllegalArgumentException("threadLimit must be > 0");
    }
  }
  
  @Override
  public int getMaxThreadLimit() {
    return threadLimit;
  }

  @Override
  public boolean shouldLaneBeBlocked(ProcessSuspendedException suspendedException) {
    //FIXME wenn die auftr�ge keine capacities brauchen, sperrt man unn�tzerweise lanes!
    return ! suspendedException.getSuspensionCause().needToFreeCapacities();
  }

}
