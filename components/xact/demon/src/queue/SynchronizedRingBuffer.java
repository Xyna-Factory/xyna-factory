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
package queue;

import java.util.Collection;
import java.util.Iterator;

public class SynchronizedRingBuffer<T> extends RingBuffer<T> {
  
  public SynchronizedRingBuffer( int capacity ) {
    super(capacity);
  }
  
  @Override
  public synchronized Iterator<T> iterator() {
    throw new UnsupportedOperationException("Schwierige Implementation... besonders bei Synchronem Zugriff");
  }

  @Override
  public synchronized int size() {
    return super.size();
  }

  @Override
  public synchronized boolean offer(T o) {
    return super.offer(o);
  }

  @Override
  public synchronized T peek() {
    return super.peek();
  }

  @Override
  public synchronized T poll() {
    return super.poll();
  }
  
  @Override
  public synchronized boolean poll(T data) {
    return super.poll(data);
  }
 
  @Override
  public synchronized void copyTo( Collection<T> collection ) {
    super.copyTo(collection);
  }

}
