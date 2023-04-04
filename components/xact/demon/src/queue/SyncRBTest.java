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


public class SyncRBTest {

  private static final int NUMBER = 1000000; //0000;

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    SynchronizedRingBuffer<Integer> ring = new SynchronizedRingBuffer<Integer>(5);
    
    for( int i=0; i< 1000; ++i ) {
      ring.offer( Integer.valueOf(i) );
      ring.poll();
    }
    
    test(ring);
    //test(ring);
    //test(ring);
    
    /*
    new Thread( new Poller(ring) ).start();
    
    new Thread( new Offerer(ring) ).start();
    */
  }
  
  
  private static void test(SynchronizedRingBuffer<Integer> ring) {
    long start = System.currentTimeMillis();
    for( int i=0; i< NUMBER; ++i ) {
      ring.offer( Integer.valueOf(i) );
      ring.poll();
    }
    long end = System.currentTimeMillis();
    System.out.println( "Single Thread " + (end-start)+" ms" );
        
    for( int t=1; t<260; t*=2 ) {
      test(ring,t);
    }
    
  }

  private static void test(SynchronizedRingBuffer<Integer> ring, int threads ) {
    long start = System.currentTimeMillis();
    int sum = 0;
    for( int i=0; i<threads; ++i ) {
      int num = NUMBER/threads;
      sum += num;
      new Thread( new WaitingOfferer(ring, NUMBER/threads) ).start();
    }
    new Poller(ring, sum ).run();
    long end = System.currentTimeMillis();
    System.out.println( threads+"+1 Threads " + (end-start)+" ms" );
  }
  

  public static class Poller implements Runnable {
    private SynchronizedRingBuffer<Integer> ring;
    private int number;

    public Poller(SynchronizedRingBuffer<Integer> ring, int number) {
      this.ring=ring;
      this.number=number;
    }

    public void run() {
      int received = 0;
      while( true ) {
        if( ring.poll() != null ) {
          //System.out.println( "Received "+received);
          ++received;
          if( received == number ) break;
        } else {
          Thread.yield();
        }
      }
    }
    
  }

  public static class Offerer implements Runnable {
    private SynchronizedRingBuffer<Integer> ring;
    private int number;
    
    public Offerer(SynchronizedRingBuffer<Integer> ring, int number) {
      this.ring=ring;
      this.number=number;
    }

    public void run() {
      int sent = 0;
      while( true ) {
        if( ring.offer( Integer.valueOf(sent) ) ) {
          //System.out.println( "Sent "+sent);
          ++sent;
          if( sent == number ) break;
          
        } else {
          Thread.yield();
        }
      }
    }
    
  }
  
  public static class WaitingOfferer implements Runnable {
    private SynchronizedRingBuffer<Integer> ring;
    private int number;
    
    public WaitingOfferer(SynchronizedRingBuffer<Integer> ring, int number) {
      this.ring=ring;
      this.number=number;
    }

    public void run() {
      for( int i=0; i<number; ++i ) {
        Integer s = Integer.valueOf(i);
        while( !ring.offer( s ) ) {
          Thread.yield();
        }
      }
    }
    
  }

}
