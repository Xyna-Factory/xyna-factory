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

package com.gip.xyna.xprc.xsched.scheduling;

import com.gip.xyna.xprc.xsched.SchedulingData;


/**
 * 
 */
public class UrgencyCalculators {

  private final static long MILLISECONDS_PER_YEAR = 365L*24*60*60*1000;
  
  public static interface UrgencyCalculator {
    public long calculate( SchedulingData schedulingData );
  }
  
  public static UrgencyCalculator createNormalUrgencyCalculator( long p, long t ) {
    return new NormalUrgencyCalculator(p,t);
  }
  
  public static UrgencyCalculator createPriorityUrgencyCalculator() {
    return new NormalUrgencyCalculator(MILLISECONDS_PER_YEAR,1L);
  }
  
  
  
  
  
  /**
   * U = p*P + t*T, mit zwei festen, aber konfigurierbaren Parametern p und t.
   * Beispiel:
   * p     t       Interpretation
   * 0     1       Prioritäten werden nicht berücksichtigt
   * 1     0       Wartezeiten werden nicht berücksichtigt
   * 60000 1       Eine Wartezeit von 60 Sekunden bedeutet Hochstufung um eine Prioritätsstufe
   *
   */
  public static class NormalUrgencyCalculator implements UrgencyCalculator {

    private long p;
    private long t;

    public NormalUrgencyCalculator(long p, long t) {
      this.p = p;
      this.t = t;
    }

    public long calculate(SchedulingData schedulingData) {
      return p*schedulingData.getPriority() - t*schedulingData.getTimeConstraintData().getStartTimestamp();
    }
    
  }
  
  
  
  
  
}
