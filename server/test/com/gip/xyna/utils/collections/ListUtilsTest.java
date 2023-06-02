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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class ListUtilsTest extends TestCase {
  
  private List<String> createArrayList(String ... strings) {
    return new ArrayList<String>(Arrays.asList(strings));
  }
  
  
  public void testInsertIntoAt1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("2").into(list).at(2);
    assertEquals( "[A, B, 2, C, D]", list.toString() );
  }
  
  public void testInsertIntoAt2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("0").into(list).at(0);
    assertEquals( "[0, A, B, C, D]", list.toString() );
  }
  
  public void testInsertIntoAt3() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("-1").into(list).at(-1);
    assertEquals( "[A, B, C, D, -1]", list.toString() );
  }
  
  public void testInsertIntoAtend() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("end").into(list).atEnd();
    assertEquals( "[A, B, C, D, end]", list.toString() );
  }
  
  
  

  public void testInsertIntoAfter1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("c").into(list).after("C");
    assertEquals( "[A, B, C, c, D]", list.toString() );
  }
  public void testInsertIntoAfter2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("d").into(list).after("D");
    assertEquals( "[A, B, C, D, d]", list.toString() );
  }
  
  public void testInsertIntoBefore1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("a").into(list).before("B");
    assertEquals( "[A, a, B, C, D]", list.toString() );
  }
  
  public void testInsertIntoBefore2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.insert("z").into(list).before("A");
    assertEquals( "[z, A, B, C, D]", list.toString() );
  }
  
  
  
  public void testMoveNotFound() {
    List<String> list = createArrayList("A", "B", "C", "D");
    try {
      ListUtils.move("x").in(list).at(1);
      fail("Exception expected");
    } catch( Exception e ) {
      assertEquals( "element is no member of list", e.getMessage() );
    }
  }
  
  public void testMoveInAt1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("D").in(list).at(1);
    assertEquals( "[A, D, B, C]", list.toString() );
  }
  
  public void testMoveInAt2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("A").in(list).at(2);
    assertEquals( "[B, C, A, D]", list.toString() );
  }
  
  public void testMoveInAt3() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("B").in(list).at(-1);
    assertEquals( "[A, C, D, B]", list.toString() );
  }
 
  public void testMoveInAt4() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("B").in(list).at(1);
    assertEquals( "[A, B, C, D]", list.toString() );
  }
  
  public void testMoveInAtend() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("B").in(list).atEnd();
    assertEquals( "[A, C, D, B]", list.toString() );
  }
  
  
  
  public void testMoveInIibmAt1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("D").in(list).indexIsBeforeMove().at(1);
    assertEquals( "[A, D, B, C]", list.toString() );
  }
  
  public void testMoveInIibmAt2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("A").in(list).indexIsBeforeMove().at(3);
    assertEquals( "[B, C, A, D]", list.toString() );
  }
  
  public void testMoveInIibmAt3() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("B").in(list).indexIsBeforeMove().at(-1);
    assertEquals( "[A, C, D, B]", list.toString() );
  }
 
  public void testMoveInToIibmAt4() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("B").in(list).indexIsBeforeMove().at(1);
    assertEquals( "[A, B, C, D]", list.toString() );
  }
  
  
 
  
  
  
  

  public void testMoveInAfter1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("D").in(list).after("A");
    assertEquals( "[A, D, B, C]", list.toString() );
  }
  
  public void testMoveInAfter2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("A").in(list).after("C");
    assertEquals( "[B, C, A, D]", list.toString() );
  }
  
  public void testMoveInAfter3() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("B").in(list).after("D");
    assertEquals( "[A, C, D, B]", list.toString() );
  }
  
  public void testMoveInAfter4() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("C").in(list).after("B");
    assertEquals( "[A, B, C, D]", list.toString() );
    ListUtils.move("C").in(list).after("C");
    assertEquals( "[A, B, C, D]", list.toString() );
  }
  
  public void testMoveInBefore1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("D").in(list).before("B");
    assertEquals( "[A, D, B, C]", list.toString() );
  }
  
  public void testMoveInBefore2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("A").in(list).before("D");
    assertEquals( "[B, C, A, D]", list.toString() );
  }
  
  public void testMoveInBefore3() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("C").in(list).before("A");
    assertEquals( "[C, A, B, D]", list.toString() );
  }
  
  public void testMoveInBefore4() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.move("B").in(list).before("C");
    assertEquals( "[A, B, C, D]", list.toString() );
    ListUtils.move("B").in(list).before("B");
    assertEquals( "[A, B, C, D]", list.toString() );
  }


  public void testMoveinFromAt1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.moveIn(list).from(3).at(1);
    assertEquals( "[A, D, B, C]", list.toString() );
  }
  
  public void testMoveinFromAt2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.moveIn(list).from(0).at(2);
    assertEquals( "[B, C, A, D]", list.toString() );
  }
  
  public void testMoveinFromAtend() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.moveIn(list).from(0).atEnd();
    assertEquals( "[B, C, D, A]", list.toString() );
  }
  
  public void testMoveinFromAfter1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.moveIn(list).from(3).after("A");
    assertEquals( "[A, D, B, C]", list.toString() );
  }
  
  public void testMoveinFromAfter2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.moveIn(list).from(0).after("C");
    assertEquals( "[B, C, A, D]", list.toString() );
  }
  
  public void testMoveinFromBefore1() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.moveIn(list).from(3).before("B");
    assertEquals( "[A, D, B, C]", list.toString() );
  }
  
  public void testMoveinFromBefore2() {
    List<String> list = createArrayList("A", "B", "C", "D");
    ListUtils.moveIn(list).from(0).before("D");
    assertEquals( "[B, C, A, D]", list.toString() );
  }
  
  
  
  
  public void testTransferFromToAt1() {
    List<String> list1 = createArrayList("A", "B", "C", "D");
    List<String> list2 = createArrayList("a", "b", "c", "d");
    ListUtils.transfer("B").from(list1).to(list2).at(1);
    assertEquals( "[A, C, D]", list1.toString() );
    assertEquals( "[a, B, b, c, d]", list2.toString() );
  }

  public void testTransferfromAtindexToAt1() {
    List<String> list1 = createArrayList("A", "B", "C", "D");
    List<String> list2 = createArrayList("a", "b", "c", "d");
    ListUtils.transferFrom(list1).atIndex(1).to(list2).at(1);
    assertEquals( "[A, C, D]", list1.toString() );
    assertEquals( "[a, B, b, c, d]", list2.toString() );
  }

  
  
  public void testTransferFromToAtend() {
    List<String> list1 = createArrayList("A", "B", "C", "D");
    List<String> list2 = createArrayList("a", "b", "c", "d");
    ListUtils.transfer("C").from(list1).to(list2).atEnd();
    assertEquals( "[A, B, D]", list1.toString() );
    assertEquals( "[a, b, c, d, C]", list2.toString() );
  }
  
  public void testTransferFromToAfter() {
    List<String> list1 = createArrayList("A", "B", "C", "D");
    List<String> list2 = createArrayList("a", "b", "c", "d");
    ListUtils.transfer("C").from(list1).to(list2).after("a");
    assertEquals( "[A, B, D]", list1.toString() );
    assertEquals( "[a, C, b, c, d]", list2.toString() );
  }
  
  public void testTransferFromToBefore() {
    List<String> list1 = createArrayList("A", "B", "C", "D");
    List<String> list2 = createArrayList("a", "b", "c", "d");
    ListUtils.transfer("C").from(list1).to(list2).before("b");
    assertEquals( "[A, B, D]", list1.toString() );
    assertEquals( "[a, C, b, c, d]", list2.toString() );
  }
 

  
  
  
  
  
}
