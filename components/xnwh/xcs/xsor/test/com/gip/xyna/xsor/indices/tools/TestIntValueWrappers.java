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
package com.gip.xyna.xsor.indices.tools;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.gip.xyna.xsor.indices.tools.IntValueWrapper;
import com.gip.xyna.xsor.indices.tools.MultiIntValueWrapper;
import com.gip.xyna.xsor.indices.tools.SingleIntValueWrapper;


public class TestIntValueWrappers {

  
  @Test
  public void testAdditions() {
    IntValueWrapper oneWrapper = new SingleIntValueWrapper(1);
    assertIntValueWrapperValues(oneWrapper, 1);
    IntValueWrapper one2twoWrapper =  oneWrapper.addValue(2);
    assertTrue(one2twoWrapper instanceof MultiIntValueWrapper);
    assertIntValueWrapperValues(one2twoWrapper, 1, 2);
    IntValueWrapper one2threeWrapper =  one2twoWrapper.addValue(3);
    assertIntValueWrapperValues(one2threeWrapper, 1, 2, 3);
    IntValueWrapper unchangedWrapper =  one2threeWrapper.addValue(3);
    assertIntValueWrapperValues(unchangedWrapper, 1, 2, 3);
    
    IntValueWrapper otherOneWrapper = oneWrapper.addValue(1);
    assertIntValueWrapperValues(otherOneWrapper, 1);
    
    IntValueWrapper otherOne2threeWrapper =  one2threeWrapper.addValue(3);
    assertIntValueWrapperValues(otherOne2threeWrapper, 1, 2, 3);
    
    final int ADDITIONS = 10000;
    IntValueWrapper bigWrapper = new SingleIntValueWrapper(0);
    int i = 1;
    int[] comparissonArray = new int[ADDITIONS];
    comparissonArray[0] = 0;
    while (i < ADDITIONS) {
      bigWrapper = bigWrapper.addValue(i);
      comparissonArray[i] = i;
      i++;
    }
    assertTrue(Arrays.equals(comparissonArray, bigWrapper.getValues()));
  }
  
  
  @Test
  public void testSubstraction() {
    IntValueWrapper one2fiveWrapper = new MultiIntValueWrapper(1,2,3,4,5);
    assertIntValueWrapperValues(one2fiveWrapper, 1,2,3,4,5);
    IntValueWrapper firstSubstraction = one2fiveWrapper.removeValue(3);
    assertIntValueWrapperValues(firstSubstraction, 1,2,4,5);
    try {
      IntValueWrapper illegalWrapper = firstSubstraction.removeValue(3);
      fail("Expected RuntimeException");
    } catch (Throwable t) {
      // expected
    }
    IntValueWrapper fiveWrapper = firstSubstraction.removeValue(1).removeValue(2).removeValue(4);
    assertTrue(fiveWrapper instanceof SingleIntValueWrapper);
    assertIntValueWrapperValues(fiveWrapper, 5);
    IntValueWrapper nullWrapper = fiveWrapper.removeValue(5);
    assertTrue(nullWrapper == null);
        
    IntValueWrapper two2fiveWrapper = one2fiveWrapper.removeValue(1);
    assertIntValueWrapperValues(two2fiveWrapper, 2,3,4,5);
    
    IntValueWrapper one2fourWrapper = one2fiveWrapper.removeValue(5);
    assertIntValueWrapperValues(one2fourWrapper, 1,2,3,4);
  }
  
  
  @Test
  public void testReplace() {
    IntValueWrapper one2fiveWrapper = new MultiIntValueWrapper(1,2,3,4,5);
    assertIntValueWrapperValues(one2fiveWrapper, 1,2,3,4,5);
    
    IntValueWrapper endReplacement = one2fiveWrapper.replaceValue(5, 6);
    assertIntValueWrapperValues(endReplacement, 1,2,3,4,6);
    
    IntValueWrapper startReplacement = endReplacement.replaceValue(1, 0);
    assertIntValueWrapperValues(startReplacement, 0,2,3,4,6);
    
    IntValueWrapper midHighReplacement = startReplacement.replaceValue(3, 100);
    assertIntValueWrapperValues(midHighReplacement, 0,2,4,6,100);
    
    IntValueWrapper midLowReplacement = midHighReplacement.replaceValue(4, -100);
    assertIntValueWrapperValues(midLowReplacement, -100,0,2,6,100);
    
    try {
      IntValueWrapper illegalReplacement = midLowReplacement.replaceValue(666, 200);
      fail();
    } catch (Throwable t) { /* expected */}
    
    try {
      IntValueWrapper illegalReplacement = midLowReplacement.replaceValue(-100, 100);
      fail();
    } catch (Throwable t) { /* expected */}
    
    try {
      IntValueWrapper illegalReplacement = midLowReplacement.replaceValue(-100, -100);
      fail();
    } catch (Throwable t) { /* expected */}
    
  }
  
  
  @Test
  public void testImmutability() {
    IntValueWrapper oneWrapper = new SingleIntValueWrapper(1);
    IntValueWrapper otherOneWrapper = oneWrapper.addValue(1);
    assertImmutability(oneWrapper, otherOneWrapper);
    
    IntValueWrapper one2threeWrapper = new MultiIntValueWrapper(1,2,3);
    IntValueWrapper otherOne2threeWrapper =  one2threeWrapper.addValue(3);
    assertImmutability(one2threeWrapper, otherOne2threeWrapper);
  }
  
  @Test
  public void testFillAndEmpty() {
    IntValueWrapper fillingWrapper = new SingleIntValueWrapper(1);
    fillingWrapper = fillingWrapper.addValue(2);
    assertIntValueWrapperValues(fillingWrapper, 1,2);
    fillingWrapper = fillingWrapper.addValue(3);
    assertIntValueWrapperValues(fillingWrapper, 1,2,3);
    fillingWrapper = fillingWrapper.addValue(4);
    assertIntValueWrapperValues(fillingWrapper, 1,2,3,4);
    fillingWrapper = fillingWrapper.addValue(5);
    assertIntValueWrapperValues(fillingWrapper, 1,2,3,4,5);
    
    fillingWrapper = fillingWrapper.removeValue(1);
    assertIntValueWrapperValues(fillingWrapper, 2,3,4,5);
    fillingWrapper = fillingWrapper.removeValue(2);
    assertIntValueWrapperValues(fillingWrapper, 3,4,5);
    fillingWrapper = fillingWrapper.removeValue(3);
    assertIntValueWrapperValues(fillingWrapper, 4,5);
    fillingWrapper = fillingWrapper.removeValue(4);
    assertIntValueWrapperValues(fillingWrapper, 5);
    fillingWrapper = fillingWrapper.removeValue(5);
    assertTrue(fillingWrapper == null);
    
    fillingWrapper = new SingleIntValueWrapper(5);
    fillingWrapper = fillingWrapper.addValue(4);
    assertIntValueWrapperValues(fillingWrapper, 4,5);
    fillingWrapper = fillingWrapper.addValue(3);
    assertIntValueWrapperValues(fillingWrapper, 3,4,5);
    fillingWrapper = fillingWrapper.addValue(2);
    assertIntValueWrapperValues(fillingWrapper, 2,3,4,5);
    fillingWrapper = fillingWrapper.addValue(1);
    assertIntValueWrapperValues(fillingWrapper, 1,2,3,4,5);
    
    fillingWrapper = fillingWrapper.removeValue(5);
    assertIntValueWrapperValues(fillingWrapper, 1,2,3,4);
    fillingWrapper = fillingWrapper.removeValue(4);
    assertIntValueWrapperValues(fillingWrapper, 1,2,3);
    fillingWrapper = fillingWrapper.removeValue(3);
    assertIntValueWrapperValues(fillingWrapper, 1,2);
    fillingWrapper = fillingWrapper.removeValue(2);
    assertIntValueWrapperValues(fillingWrapper, 1);
    fillingWrapper = fillingWrapper.removeValue(1);
    assertTrue(fillingWrapper == null);
  }
  
  static AtomicReference<IntValueWrapper> staticIntValueWrapper;
  static Random r = new Random();
  
  @Test
  public void testRandomActions() throws InterruptedException, ExecutionException {
    int ITERATIONS_PER_ACTOR = 250000;
    int ACTORS = 5;
    staticIntValueWrapper = new AtomicReference<IntValueWrapper>(new SingleIntValueWrapper(1));
    ExecutorService threadpool = Executors.newFixedThreadPool(ACTORS);
    Collection<Future<Void>> futures = new ArrayList<Future<Void>>();
    for (int i = 0; i < ACTORS; i++) {
      futures.add(threadpool.submit(new RandomIntValueWrapperActionActor(ITERATIONS_PER_ACTOR)));
    }
    for (Future<Void> future : futures) {
      future.get();
    }
  }
  
  
  private static class RandomIntValueWrapperActionActor implements Callable<Void> {

    private int iterations;


    RandomIntValueWrapperActionActor(int iterations) {
      this.iterations = iterations;
    }


    public Void call() throws Exception {
      for (int j = 0; j < iterations; j++) {
        boolean success = false;
        while (!success) {
          int actionCode = r.nextInt(6);
          int randomId = r.nextInt();
          IntValueWrapper wrapper = staticIntValueWrapper.get();
          if (wrapper == null) {
            if (staticIntValueWrapper.compareAndSet(null, new SingleIntValueWrapper(randomId))) {
              success = true;
            }
          } else {
            switch (actionCode) {
              case 0 : // add
              case 4 :
                boolean addContainedValue = r.nextBoolean();
                if (addContainedValue) {
                  int[] values = wrapper.getValues();
                  int randomIndex = r.nextInt(values.length);
                  if (staticIntValueWrapper.compareAndSet(wrapper, wrapper.addValue(values[randomIndex]))) {
                    success = true;
                  }
                } else {
                  if (staticIntValueWrapper.compareAndSet(wrapper, wrapper.addValue(randomId))) {
                    success = true;
                  }
                }
                break;
              case 1 : // getValues and evaluate order
              case 5 :
                int[] values = wrapper.getValues();
                int previousValue = values[0];
                for (int i = 1; i < values.length; i++) {
                  assertTrue(values[i] > previousValue);
                  previousValue = values[i];
                }
                System.out.println("evaluted a size of: " + values.length);
                success = true;
                break;
              case 2 :
                values = wrapper.getValues();
                int randomIndex = r.nextInt(values.length);
                if (staticIntValueWrapper.compareAndSet(wrapper, wrapper.removeValue(values[randomIndex]))) {
                  success = true;
                }
                break;
              case 3 :
                values = wrapper.getValues();
                randomIndex = r.nextInt(values.length);
                addContainedValue = r.nextBoolean();
                if (addContainedValue) {
                  boolean replaceWithSame = r.nextBoolean();
                  if (replaceWithSame) {
                    try {
                      wrapper.replaceValue(values[randomIndex],  values[randomIndex]);
                      fail();
                    } catch (AssertionError e) { 
                      success = true;
                    }
                  } else {
                    if (Arrays.binarySearch(values, randomId) < 0) {
                      if (staticIntValueWrapper.compareAndSet(wrapper, wrapper.replaceValue(values[randomIndex], randomId))) {
                        success = true;
                      }
                    }
                  }
                } else {
                  if (values[randomIndex] != randomId) {
                    if (staticIntValueWrapper.compareAndSet(wrapper, wrapper.replaceValue(values[randomIndex], randomId))) {
                      success = true;
                    }
                  }
                }
                break;
            }
          }
        }
      }
      return null;
    }
    
  }
  
  /*static int[] array = new int[50];
  
  @Test
  public void testArraySort() {
    for (int i = 0; i < array.length; i++) {
      array[i] = i;
    }
    
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          Arrays.sort(array);
        }
      }
    }).start();
    
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          if (Arrays.binarySearch(array, array.length / 2) < 0) {
            System.out.println("couldn't find " +  array.length / 2 + " inside " + Arrays.toString(array));
          }
        }
      }
    }).start();
  }*/
  
  
  
  private void assertIntValueWrapperValues(IntValueWrapper wrapper, int... values) {
    assertTrue(wrapper.getValues().length == values.length);
    for (int i = 0; i < values.length; i++) {
      assertEquals(values[i], wrapper.getValues()[i]);
    }
  }
  
  
  private void assertImmutability(IntValueWrapper wrapper, IntValueWrapper otherWrapper) {
    assertFalse(wrapper == otherWrapper);
    //assertFalse(wrapper.getValues() == otherWrapper.getValues()); // performance fixes got rid of that ;)
  }
  
}
