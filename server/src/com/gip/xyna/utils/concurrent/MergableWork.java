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
package com.gip.xyna.utils.concurrent;

import java.util.Iterator;


/**
 * K - Key: identifier for the task, hashCode and equals of the identifying object should be implemented appropriately
 * S - Shared processing data: data that is shared between all work units executing during the same time window
 * R - Result: the return value of the processed work
 */
public interface MergableWork<K,S,R> {
  
  public K getKey();
  
  /**
   * If several work units are executed in the same time window one of them is selected to perform the shared data initialization,
   * if there is only one unit it will perform the initialization.
   * It is given an iterator over all participating work units in case it is needed for initialization.
   */
  public S initSharedProcessingData(Iterator<MergableWork<K,S,R>> workloadIterator);
  
  public R process(S sharedProcessingData) throws Exception;
  
  /**
   * At the end of the processing a final operation on the shared data can be performed
   */
  public void finalizeSharedProcessingData(S sharedData, Iterator<MergableWork<K,S,R>> workloadIterator)  throws Exception;
  
  /**
   * return true if the other work unit could be successfully merged, false otherwise
   */
  public boolean merge(MergableWork<K,S,R> other);
  
  /**
   * if a merge with this work unit should not be tried
   */
  public boolean isMergeable();
  
  
  /**
   * Helper class for work units that do not require shared processing data
   */
  public static abstract class UnsharedMergableWork<K,R> implements MergableWork<K, Void, R>{
    
    public Void initSharedProcessingData(Iterator<MergableWork<K, Void, R>> workloadIter) {
      return null;
    }
    
    public R process(Void arg0) throws Exception {
      return process();
    }
    
    public abstract R process() throws Exception;
    
    public void finalizeSharedProcessingData(Void arg0, Iterator<MergableWork<K,Void,R>> workloadIterator) throws Exception {
    }
    
  }
  
}
