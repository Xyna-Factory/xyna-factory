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

package com.gip.xyna.xfint.xnumdav;

import java.sql.Date;
import java.util.Random;

import junit.framework.TestCase;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;



public class AggregatableDataStoreTest extends TestCase {

  public void testNumberHelper() {
    assertEquals(-1,(NumberHelper.divideNumberByNumber(-3L, 3L)));
    assertEquals(0,(NumberHelper.divideNumberByNumber(0L, 3L)));
    assertEquals(0,(NumberHelper.divideNumberByNumber(1L, 3L)));
    assertEquals(1,(NumberHelper.divideNumberByNumber(3L, 3L)));
    assertEquals(1,(NumberHelper.divideNumberByNumber(4L, 3L)));
    assertEquals(1,(NumberHelper.divideNumberByNumber(5L, 3L)));
    assertEquals(2,(NumberHelper.divideNumberByNumber(6L, 3L)));
    assertEquals(2,(NumberHelper.divideNumberByNumber(7L, 3L)));
    assertEquals(2,(NumberHelper.divideNumberByNumber(8L, 3L)));
    assertEquals(3,(NumberHelper.divideNumberByNumber(9L, 3L)));
  }

  public void testAddSomeEntries() throws InterruptedException {

    AggregatableDataStore datastore = new AggregatableDataStore(5, 1000L);

    for (int i = 0; i < 1250; i++) {
      double value = new Random().nextDouble();
      StorableAggregatableDataEntry nextEntry = new StorableAggregatableDataEntry(System.currentTimeMillis(), value);
      datastore.addEntry(nextEntry);
      Thread.sleep(5);
    }

    for (StorableAggregatableDataEntry entry : datastore.getEntries()) {
      String s = Constants.defaultUTCSimpleDateFormat().format(new Date((Long) entry.getValueX())) + " -> "
                      + entry.getValue() + ", min=" + entry.getMinimumValue() + ", max=" + entry.getMaximumValue()
                      + ", count=" + entry.getNumberOfDatapoints();
      CentralFactoryLogging.getLogger(getClass()).debug("Result: " + s);

      assertTrue(NumberHelper.isFirstArgumentLargerOrEqualToSecond((double) 0.1, entry.getMinimumValue()));

      Number maxMinusOne = NumberHelper.addNumbers(entry.getMaximumValue(), (double) -1);
      Number maxMinusOneTimeMinusOne = NumberHelper.multiply(maxMinusOne, -1);
      assertTrue(NumberHelper.isFirstArgumentLargerOrEqualToSecond(0.1, maxMinusOneTimeMinusOne));

      Number averageMinus0point5 = NumberHelper.addNumbers(entry.getValue(), (double) -0.5);
      boolean averageValueIsNearTo0point5 = NumberHelper.isFirstArgumentLargerOrEqualToSecond(averageMinus0point5,
                                                                                              (double) -0.1)
                      && NumberHelper.isFirstArgumentLargerOrEqualToSecond((double) 0.1, averageMinus0point5);
      assertTrue(averageValueIsNearTo0point5);
    }

  }

}
