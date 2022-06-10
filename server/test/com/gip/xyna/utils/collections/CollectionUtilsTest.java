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
package com.gip.xyna.utils.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.gip.xyna.utils.collections.CollectionUtils.Join;
import com.gip.xyna.utils.collections.CollectionUtils.JoinType;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;


public class CollectionUtilsTest extends TestCase {

  
  private static final List<String> WORTE = Arrays.asList("Apfel","Banane","Clementine","Ahorn","Buche","Axt");
  
  
  private static class StringLength implements Transformation<String, Integer> {
    public Integer transform(String from) {
      if( from == null ) {
        return null;
      }
      return Integer.valueOf(from.length());
    }
  }
  
  private static class FirstChar implements Transformation<String, String> {

    public String transform(String from) {
      if( from == null ) {
        return null;
      }
      return from.substring(0,1);
    }
    
  }
  
  private static class StartsWith implements Transformation<String, String> {

    private String prefix;

    public StartsWith(String prefix) {
      this.prefix = prefix;
    }

    public String transform(String from) {
      if( from == null ) {
        return null;
      }
      if( from.startsWith(prefix) ) {
        return from;
      }
      return null;
    }
    
  }
  
  private static class LengthJoin implements Join<String, Integer, Integer, String> {

    public Integer leftKey(String left) {
      if( left == null ) return -1;
      return left.length();
    }

    public Integer rightKey(Integer right) {
      return right;
    }

    public String join(Integer key, List<String> lefts, List<Integer> rights) {
      return key+"-"+lefts+"-"+rights;
    }
    
  }

  public void testTransform1() {
    List<String> from = new ArrayList<String>(WORTE);
    List<Integer> length = CollectionUtils.transform(from, new StringLength() );
    assertEquals("[5, 6, 10, 5, 5, 3]", length.toString() );
  }
  
  public void testTransform2() {
    List<String> from = new ArrayList<String>(WORTE);
    List<String> first = CollectionUtils.transform(from, new FirstChar() );
    assertEquals("[A, B, C, A, B, A]", first.toString() );
  }
  
  public void testTransform3() {
    List<String> from = new ArrayList<String>(WORTE);
    List<String> allAs = CollectionUtils.transform(from, new StartsWith("A") );
    assertEquals("[Apfel, null, null, Ahorn, null, Axt]", allAs.toString() );
  }
  
  public void testTransform3AndSkipNull() {
    List<String> from = new ArrayList<String>(WORTE);
    List<String> allAs = CollectionUtils.transformAndSkipNull(from, new StartsWith("A") );
    assertEquals("[Apfel, Ahorn, Axt]", allAs.toString() );
  }
  
  
  public void testGroup1() {
    List<String> from = new ArrayList<String>(WORTE);
    Map<Integer, ArrayList<String>> length = CollectionUtils.group(from, new StringLength() );
    assertEquals("{3=[Axt], 5=[Apfel, Ahorn, Buche], 6=[Banane], 10=[Clementine]}", length.toString() );
  }
 
  public void testGroup2() {
    List<String> from = new ArrayList<String>(WORTE);
    Map<String, ArrayList<String>> first = CollectionUtils.group(from, new FirstChar() );
    assertEquals("{A=[Apfel, Ahorn, Axt], B=[Banane, Buche], C=[Clementine]}", first.toString() );
  }

  public void testJoin_Inner() {
    List<String> left = new ArrayList<String>(WORTE);
    List<Integer> right = Arrays.asList( 1, 2, 3, 4, 5, 6 );
    
    List<String> join =  CollectionUtils.join(left, right, new LengthJoin(), JoinType.Inner);
    Collections.sort(join);
    assertEquals("[3-[Axt]-[3], 5-[Apfel, Ahorn, Buche]-[5], 6-[Banane]-[6]]" , join.toString() );
  }
  
  public void testJoin_LeftOuter() {
    List<String> left = new ArrayList<String>(WORTE);
    List<Integer> right = Arrays.asList( 1, 2, 3, 4, 5, 6 );
    
    List<String> join =  CollectionUtils.join(left, right, new LengthJoin(), JoinType.LeftOuter);
    Collections.sort(join);
    assertEquals("[10-[Clementine]-null, 3-[Axt]-[3], 5-[Apfel, Ahorn, Buche]-[5], 6-[Banane]-[6]]" , join.toString() );
  }
 
  public void testJoin_RightOuter() {
    List<String> left = new ArrayList<String>(WORTE);
    List<Integer> right = Arrays.asList( 1, 2, 3, 4, 5, 6 );
    
    List<String> join =  CollectionUtils.join(left, right, new LengthJoin(), JoinType.RightOuter);
    Collections.sort(join);
    assertEquals("[1-null-[1], 2-null-[2], 3-[Axt]-[3], 4-null-[4], 5-[Apfel, Ahorn, Buche]-[5], 6-[Banane]-[6]]" , join.toString() );
  }
  
  public void testJoin_FullOuter() {
    List<String> left = new ArrayList<String>(WORTE);
    List<Integer> right = Arrays.asList( 1, 2, 3, 4, 5, 6 );
    
    List<String> join =  CollectionUtils.join(left, right, new LengthJoin(), JoinType.FullOuter);
    Collections.sort(join);
    assertEquals("[1-null-[1], 10-[Clementine]-null, 2-null-[2], 3-[Axt]-[3], 4-null-[4], 5-[Apfel, Ahorn, Buche]-[5], 6-[Banane]-[6]]" , join.toString() );
  }
 
  public void testJoin_Anti() {
    List<String> left = new ArrayList<String>(WORTE);
    List<Integer> right = Arrays.asList( 1, 2, 3, 4, 5, 6 );
    
    List<String> join =  CollectionUtils.join(left, right, new LengthJoin(), JoinType.Anti);
    Collections.sort(join);
    assertEquals("[1-null-[1], 10-[Clementine]-null, 2-null-[2], 4-null-[4]]" , join.toString() );
  }
 

}
