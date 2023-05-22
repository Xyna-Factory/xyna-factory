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

package com.gip.xyna.xprc.xsched.cronlikescheduling;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares cron like orders depending of their next execution time. Orders with earlier execution times come first.
 */
public class CronLikeOrderComparator implements Comparator<CronLikeOrder>, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Compares two cron like orders.
   * 
   * @param o1 first cron like order
   * @param o2 second cron like order
   * @return 0 if both orders are equal
   */
  public int compare(CronLikeOrder o1, CronLikeOrder o2) {
    if (o1.getNextExecution() == null) {
      if (o2.getNextExecution() == null) {
        return o1.getId().compareTo(o2.getId());
      }
      return -1;
    }
    if (o2.getNextExecution() == null) {
      return 1;
    }
    if (o1.getId() != null && o1.getId().equals(o2.getId())) {
      return 0;
    }
    if (o1.getNextExecution().compareTo(o2.getNextExecution()) > 0) {
      return 1;
    } else if (o1.getNextExecution().compareTo(o2.getNextExecution()) < 0) {
      return -1;
    } else {
      // compare according to the IDs since the remove method relies on the compareTo method
      return o1.getId().compareTo(o2.getId());
    }
  }

}
