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
package com.gip.xyna.xsor.indices.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.xsor.common.IntegrityAssertion;

//immutable
public class MultiIntValueWrapper implements IntValueWrapper {

  private static AtomicInteger disabled=new AtomicInteger(0);// ==0:enbaled, 
                                                             //>0: = disabled
  private static ArrayList<MultiIntValueWrapper> toConvert=new ArrayList<MultiIntValueWrapper>();
  // this should be tested with List<Integer>
  // not only performance wise but concerning memoryFootprint as well
  private int[] values;
  private ArrayList<Integer> val;
  
  MultiIntValueWrapper(int... values) {
    if (disabled.intValue()==0){
      this.values = values;
    } else {
      this.val=new ArrayList<Integer>();
      for(int i=0;i<values.length;i++){
        val.add(values[i]);
      }
      synchronized (toConvert) {
        toConvert.add(this);
      }
    }
  }

  public int[] getValues() {
    return values;
  }

  public IntValueWrapper addValueInternal(int value) {
    int searchIndex = Arrays.binarySearch(values, value);
    if (searchIndex >= 0) {
      return new MultiIntValueWrapper(values);
    } else {
      int insertionIndex = -searchIndex-1;
      int newValues[] = new int[values.length + 1];
      System.arraycopy(values, 0, newValues, 0, insertionIndex);
      newValues[insertionIndex] = value;
      System.arraycopy(values, insertionIndex, newValues, insertionIndex+1, newValues.length-(insertionIndex+1));
      return new MultiIntValueWrapper(newValues);
    }
  }
  
   public IntValueWrapper addValue(int value) {
     if (val!=null && disabled.intValue()>0){
       val.add(value);
       return this;
     } 
     return addValueInternal(value);     
  }
  
  public IntValueWrapper removeValue(int value) {
    @IntegrityAssertion
    int searchIndex = Arrays.binarySearch(values, value);
    assert (searchIndex > -1) : "value to remove not found: " + value + " in " + Arrays.toString(values);
    assert (searchIndex < values.length) : "searchIndex > values.length for: " + value + " in " + Arrays.toString(values);
    int newValues[] = new int[values.length - 1];
    System.arraycopy(values, 0, newValues, 0, searchIndex);
    System.arraycopy(values, searchIndex+1, newValues, searchIndex, newValues.length - searchIndex);
    if (newValues.length == 1) {
      return new SingleIntValueWrapper(newValues[0]);
    } else {
      return new MultiIntValueWrapper(newValues);
    }
  }
     
  public IntValueWrapper replaceValue(@IntegrityAssertion int oldValue, @IntegrityAssertion int newValue) {
    assert Arrays.binarySearch(values, oldValue) > -1 : "value to update not found: " + oldValue + " in " + Arrays.toString(values) + " newValue: " + newValue;
    assert (Arrays.binarySearch(values, newValue) <= -1) : "newValue to update already contained: " + newValue + " in " + Arrays.toString(values) + " oldValue: " + oldValue;
    return this.removeValue(oldValue).addValue(newValue);
  }
  
  private void convert() {
    if (val==null){
      return;
    }
    int[] valuesnew=new int[val.size()];
    for(int i=0;i<val.size();i++){
      valuesnew[i]=val.get(i);
    }
    Arrays.sort(valuesnew);
    values=valuesnew;
    val=null;   
  }

  public static void disableIndices() {
    synchronized(toConvert){
      disabled.incrementAndGet();
    }
  }

  public static void enableIndices() {
    synchronized(toConvert){
      int disabledInt=disabled.decrementAndGet();
      if (disabledInt==0){
        for(MultiIntValueWrapper multiIntWrapper: toConvert){
          multiIntWrapper.convert();
        }    
        toConvert.clear();
      }
    }
  } 
  
}
