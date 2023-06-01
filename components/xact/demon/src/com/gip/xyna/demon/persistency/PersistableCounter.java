/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.demon.persistency;

import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.utils.snmp.agent.utils.IntSource;

/**
 * Persistent Counter
 *
 */
public class PersistableCounter implements Persistable, IntSource {

  private AtomicInteger counter = new AtomicInteger();
  private String uniqueName;
  public PersistableCounter( String uniqueName ) {
    this.uniqueName = uniqueName;
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.demon.persistency.Persistable#getUniqueName()
   */
  public String getUniqueName() {
    return uniqueName;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.persistency.Persistable#setPersistentValue(java.lang.String)
   */
  public void setPersistentValue(String value) {
    if( value == null ) {
      counter.set(0);
    } else {
      counter.set(Integer.parseInt(value));
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.persistency.Persistable#getPersistentValue()
   */
  public String getPersistentValue() {
    return String.valueOf(counter.get());
  }
  
  /**
   * Increment the counter
   */
  public void increment() {
    counter.incrementAndGet();
  }
  
  /**
   * Get the current counter value
   * @return
   */
  public int getCounter() {
    return counter.get();
  }
    
  @Override
  public String toString() {
    return uniqueName+": "+getCounter();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.IntSource#getInt()
   */
  public int getInt() {
     return counter.get();
  }
  
}
