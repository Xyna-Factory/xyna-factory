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


public class RBTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    RingBuffer<Integer> ring = new RingBuffer<Integer>(6);
    for( int i=0; i< 10; ++i ) {
      ring.offer(i);
    }
    printRing( ring );
        
    System.out.println( ring.peek() );
    printRing( ring );
    
    System.out.println( ring.poll() );
    printRing( ring );
    
    for( int i=10; i< 13; ++i ) {
      ring.poll();
      ring.offer(i);
    }
    printRing( ring );
    
  }

  private static void printRing(RingBuffer<Integer> ring) {
    for (Integer i : ring) {
      System.out.print(i+"#");
    }
    System.out.println();
  }

  
}
