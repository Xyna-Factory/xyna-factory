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

import java.util.Iterator;
import java.util.NoSuchElementException;

// suppress unchecked warnings in Java 1.5.0_6 and later
@SuppressWarnings("unchecked")

public class RingBufferTestInternet<Item> implements Iterable<Item> {
    private Item[] a;            // queue elements
    private int N = 0;           // number of elements on queue
    private int first = 0;       // index of first element of queue
    private int last  = 0;       // index of next available slot

    // cast needed since no generic array creation in Java
    public RingBufferTestInternet(int capacity) {
        a = (Item[]) new Object[capacity];
    }

    public boolean isEmpty() { return N == 0; }
    public int size()        { return N;      }

    public void enqueue(Item item) {
        if (N == a.length) { throw new RuntimeException("Ring buffer overflow"); }
        a[last] = item;
        last = (last + 1) % a.length;     // wrap-around
        N++;
    }

    // remove the least recently added item - doesn't check for underflow
    public Item dequeue() {
        if (isEmpty()) { throw new RuntimeException("Ring buffer underflow"); }
        Item item = a[first];
        a[first] = null;                  // to help with garbage collection
        N--;
        first = (first + 1) % a.length;   // wrap-around
        return item;
    }

    public Iterator<Item> iterator() { return new RingBufferIterator(); }

    // an iterator, doesn't implement remove() since it's optional
    private class RingBufferIterator implements Iterator<Item> {
        private int i = 0;
        public boolean hasNext()  { return i < N;                               }
        public void remove()      { throw new UnsupportedOperationException();  }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            return a[i++];
        }
    }



    // a test client
    public static void main(String[] args) {
        RingBufferTestInternet<String> ring = new RingBufferTestInternet<String>(20);
        ring.enqueue("This");
        ring.enqueue("is");
        ring.enqueue("a");
        ring.enqueue("test.");

        for (String s : ring) {
            System.out.print(s+"#");
        }
        System.out.println();

        while (!ring.isEmpty())  {
            System.out.println(ring.dequeue());
            
            for (String s : ring) {
              System.out.print(s+"#");
          }
          System.out.println();
        }
    }


}