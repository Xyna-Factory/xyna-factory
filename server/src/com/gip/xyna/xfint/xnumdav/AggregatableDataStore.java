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

package com.gip.xyna.xfint.xnumdav;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;



public class AggregatableDataStore implements Serializable {


  private static final long serialVersionUID = -8971595667148555032L;

  private int maxEntriesPerDepth = 10; //max cnt buckets. falls mehr buckets ben�tigt werden, wird bucket width erh�ht
  private Number intervalLength; //bucket width

  private int lastIndex = 0;
  private int firstIndex = 0;

  private Map<Integer, StorableAggregatableDataEntry> mapIndexToEntry = new TreeMap<Integer, StorableAggregatableDataEntry>();


  public AggregatableDataStore(int maximumEntriesPerDepthLevel, Number intervalLength) {
    this.maxEntriesPerDepth = maximumEntriesPerDepthLevel;
    this.intervalLength = intervalLength;
  }


  public synchronized void addEntry(StorableAggregatableDataEntry newEntry) {
    addEntryInternally(newEntry);
    while (lastIndex - firstIndex >= maxEntriesPerDepth) {
      increaseIntervalLength();
    }
  }


  private void addEntryInternally(StorableAggregatableDataEntry newEntry) {
    if (mapIndexToEntry.size() == 0) {
      // first entry
      firstIndex = 0;
      lastIndex = 0;
      mapIndexToEntry.put(lastIndex, newEntry);
    } else {
      Number smallestValue = mapIndexToEntry.get(firstIndex).getValueX();

      Number newEntryValueX = newEntry.getValueX();
      Number difference = NumberHelper.subtractNumbers(newEntryValueX, smallestValue);
      int indexOffset = NumberHelper.divideNumberByNumber(difference, intervalLength);
      if (NumberHelper.isFirstArgumentLargerOrEqualToSecond(newEntryValueX, smallestValue)) {
        // entry xvalue is larger than the smallest existing entry xvalue
        int index = firstIndex + indexOffset;
        StorableAggregatableDataEntry existingEntry = mapIndexToEntry.get(index);
        if (existingEntry != null) {
          mapIndexToEntry.put(index, mergeEntryIntoExistingEntry(newEntry, existingEntry));
          return; // done
        } else {
          if (index <= lastIndex) {
            mapIndexToEntry.put(index, newEntry);
          } else {
            lastIndex = index;
            // eventually merge
            mapIndexToEntry.put(index, newEntry);
          }
        }
      } else {
        int index = firstIndex - 1 + indexOffset;
        StorableAggregatableDataEntry existingEntry = mapIndexToEntry.get(index);
        if (existingEntry != null) {
          mapIndexToEntry.put(index, mergeEntryIntoExistingEntry(newEntry, existingEntry));
          return; // done
        } else {
          // eventually merge
          mapIndexToEntry.put(index, newEntry);
          firstIndex = index;
        }
      }
    }
  }


  public int getMaximumEntriesPerDepthLevel() {
    return maxEntriesPerDepth;
  }


  private StorableAggregatableDataEntry mergeEntryIntoExistingEntry(StorableAggregatableDataEntry a,
                                                                            StorableAggregatableDataEntry b) {
    a.mergeEntryIntoThis(b);
    return a;
  }


  private void increaseIntervalLength() {
    this.intervalLength = NumberHelper.multiply(this.intervalLength, 2);
    this.lastIndex = 0;
    this.firstIndex = 0;
    Map<Integer, StorableAggregatableDataEntry> oldMapIndexToEntry = mapIndexToEntry;
    mapIndexToEntry = new TreeMap<Integer, StorableAggregatableDataEntry>();

    Iterator<StorableAggregatableDataEntry> iter = oldMapIndexToEntry.values().iterator();
    while (iter.hasNext()) {
      addEntryInternally(iter.next());
    }
  }


  public synchronized Collection<StorableAggregatableDataEntry> getEntries() {
    return new ArrayList<StorableAggregatableDataEntry>(mapIndexToEntry.values());
  }


  public synchronized Collection<StorableAggregatableDataEntry> getDerivatives() {

    Collection<StorableAggregatableDataEntry> result = new ArrayList<StorableAggregatableDataEntry>();

    StorableAggregatableDataEntry currentEntry = null, nextEntry = null;
    Iterator<StorableAggregatableDataEntry> iter = mapIndexToEntry.values().iterator();
    while (iter.hasNext()) {
      nextEntry = iter.next();
      if (currentEntry != null) {

        Number xIntermediate = NumberHelper.addNumbers(nextEntry.getValueX(), currentEntry.getValueX());
        xIntermediate = NumberHelper.divideNumberByInt(xIntermediate, 2);

        Number xDiff = NumberHelper.subtractNumbers(nextEntry.getValueX(), currentEntry.getValueX());

        Number yDiff = NumberHelper.subtractNumbers(nextEntry.getValue(), currentEntry.getValue());
        yDiff = NumberHelper.multiply(yDiff, 1000); // kHz -> Hz
        double yDiffD = NumberHelper.divideNumberByNumberUnRounded(yDiff, xDiff);

        StorableAggregatableDataEntry derivativePoint = new StorableAggregatableDataEntry(xIntermediate, yDiffD);
        result.add(derivativePoint);
      }
      currentEntry = nextEntry;
    }

    return result;

  }


  public synchronized Number getCurrentIntervalLength() {
    return intervalLength;
  }

}
