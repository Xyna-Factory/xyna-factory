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
package com.gip.xyna.utils;



public class Combinatorics {

  public interface CombinationHandler {

    public boolean accept(int[] properties);

  }


  /**
   * Ruft den CombinationHandler für jede Kombination von n Zahlen zwischen 0 und propertyCounts[i] auf.
   * <br>
   * Beispiel: propertyCounts={2,3}
   * <br>
   * =&gt;properties:[<p>
   * {0,0}<p>
   * {0,1}<p>
   * {0,2}<p>
   * {1,0}<p>
   * {1,1}<p>
   * {1,2}<p>
   * ]
   * <br>
   * Bricht ab, wenn der CombinationHandler false zurückgibt.
   */
  public static void iterateOverCombinations(CombinationHandler c, int[] propertyCounts) {
    int[] properties = new int[propertyCounts.length];
    iterate(c, properties, propertyCounts, 0);
  }


  private static boolean iterate(CombinationHandler c, int[] properties, int[] propertyCounts, int idx) {
    if (idx == propertyCounts.length) {
      return c.accept(properties);
    } else {
      if (propertyCounts[idx] <= 0) {
        throw new RuntimeException("Property count must be positive. Property count " + idx + " is " + propertyCounts[idx] + ".");
      }
      for (int i = 0; i < propertyCounts[idx]; i++) {
        properties[idx] = i;
        if (!iterate(c, properties, propertyCounts, idx + 1)) {
          return false;
        }
      }
    }
    return true;
  }

}
