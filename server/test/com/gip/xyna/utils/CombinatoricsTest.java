/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.utils;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.utils.Combinatorics.CombinationHandler;

import junit.framework.TestCase;



public class CombinatoricsTest extends TestCase {

  private static class Combi {

    private final int[] combi;


    private Combi(int[] combi) {
      this.combi = combi;
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(combi);
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Combi other = (Combi) obj;
      if (!Arrays.equals(combi, other.combi))
        return false;
      return true;
    }


    public String toString() {
      return Arrays.toString(combi);
    }

  }

  private static class CollectionHandler implements CombinationHandler {

    private final List<Combi> combis = new ArrayList<>();


    @Override
    public boolean accept(int[] properties) {
      combis.add(new Combi(Arrays.copyOf(properties, properties.length)));
      return true;
    }

  }

  private static class CountHandler implements CombinationHandler {

    private int cnt = 0;


    @Override
    public boolean accept(int[] properties) {
      cnt++;
      return true;
    }

  }


  public void test1() {
    CollectionHandler c = new CollectionHandler();
    Combinatorics.iterateOverCombinations(c, new int[] {2, 3});
    List<Combi> expected = new ArrayList<>();
    expected.add(new Combi(new int[] {0, 0}));
    expected.add(new Combi(new int[] {0, 1}));
    expected.add(new Combi(new int[] {0, 2}));
    expected.add(new Combi(new int[] {1, 0}));
    expected.add(new Combi(new int[] {1, 1}));
    expected.add(new Combi(new int[] {1, 2}));
    assertEquals(expected, c.combis);
  }


  public void test2() {
    CountHandler c = new CountHandler();
    Combinatorics.iterateOverCombinations(c, new int[] {4, 7, 2, 11, 1, 1, 3});
    assertEquals(4 * 7 * 2 * 11 * 3, c.cnt);
  }


  public void test3() {
    CountHandler c = new CountHandler();
    try {
      Combinatorics.iterateOverCombinations(c, new int[] {4, 7, 2, 0, 1, 1, 3});
      fail();
    } catch (RuntimeException e) {
      //ok
    }
  }
  
  private static class FindCombinationIndex implements CombinationHandler {

    private int cnt = 0;
    private final int[] target;

    public FindCombinationIndex(int... target) {
      this.target = target;
    }


    @Override
    public boolean accept(int[] properties) {
      if (Arrays.equals(properties, target)) {
        return false;
      }
      cnt++;
      return true;
    }

  }
  
  public void test4() {
    FindCombinationIndex c = new FindCombinationIndex(2,3,4);
    Combinatorics.iterateOverCombinations(c, new int[]{7,8,9});
    assertEquals(4+3*9+2*8*9, c.cnt);
  }

}
