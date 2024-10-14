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
package com.gip.xyna.utils.collections;

import java.util.Arrays;
import java.util.List;

/*
 * RingBuffer, der mitzählt, wie oft ein Element hintereinander hinzugefügt wird.
 * Damit werden für mehrfache hinzugefügte Element keine zusätzlichen Plätze im 
 * RingBuffer belegt und Wiederholungen lassen sich besser erkennen.
 * 
 * 
 * Achtung: nicht synchronisiert (da interne Verwendung von ArrayRingBuffer und add(..) !)
 */
public class RepeationCountingRingBuffer<E> {
  
  public static class Entry<E> {
    private int count;
    private E value;
    
    public Entry(E value) {
      this.value = value;
      this.count = 1;
    }

    private boolean repeated(E next) {
      if( value == null ) {
        if( next == null ) {
          ++count;
          return true;
        }
      } else {
        if( value.equals(next) ) {
          ++count;
          return true;
        }
      }
      return false;
    }
    
    @Override
    public String toString() {
      return "Entry("+count+":"+value+")";
    }

    public E getValue() {
      return value;
    }
    
    public int getCount() {
      return count;
    }
    
  }
  
  private Entry<E> last;
  private ArrayRingBuffer<Entry<E>> buffer;

  public RepeationCountingRingBuffer(int size) {
    this.buffer = new ArrayRingBuffer<>(size);
  }

  public int size() {
    return buffer.size();
  }

  public void add(E next) {
    if( last == null ) {
      last = new Entry<>(next);
      buffer.add(last);
    } else {
      if( ! last.repeated(next) ) {
        last = new Entry<>(next);
        buffer.add(last);
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  public List<Entry<E>> getEntries() {
    return Arrays.asList(buffer.getOrdered((Entry<E>[])new Entry[buffer.size()] ) );
  }
  
  @Override
  public String toString() {
    return "RepeationCountingRingBuffer("+getEntries()+")";
  }
  
}
