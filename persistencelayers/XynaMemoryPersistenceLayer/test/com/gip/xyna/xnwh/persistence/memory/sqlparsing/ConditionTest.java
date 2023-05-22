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
package com.gip.xyna.xnwh.persistence.memory.sqlparsing;

import junit.framework.TestCase;


public class ConditionTest extends TestCase {


  public void test_1_01_SimpleWhereClauseParsing01() throws Exception {

    String toBeTested = "id = ?";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertEquals("?", empty.getExpression2());
    assertEquals("=", empty.getExpressionOperator());

  }


  public void test_1_02_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id =         ?";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertEquals("?", empty.getExpression2());
    assertEquals("=", empty.getExpressionOperator());

  }


  public void test_1_03_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id >         ?";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertEquals("?", empty.getExpression2());
    assertEquals(">", empty.getExpressionOperator());

  }


  public void test_1_04_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id <         ?";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertEquals("?", empty.getExpression2());
    assertEquals("<", empty.getExpressionOperator());

  }


  public void test_1_05_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id   !=    ?";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertEquals("?", empty.getExpression2());
    assertTrue(ConditionOperator.UNEQUAL2.equals(empty.getConditionOperator())
        || (ConditionOperator.EQUALS.equals(empty.getConditionOperator()) && empty.isNegated()));

  }


  public void test_1_06_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id   <>    ?";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertEquals("?", empty.getExpression2());
    assertTrue(ConditionOperator.UNEQUAL1.equals(empty.getConditionOperator())
               || (ConditionOperator.EQUALS.equals(empty.getConditionOperator()) && empty.isNegated()));

  }


  public void test_1_07_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id   liKe    ?";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertEquals("?", empty.getExpression2());
    assertEquals(ConditionOperator.LIKE, empty.getConditionOperator());

  }


  public void test_1_08_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id   liiiKe    ?";

    try {
    Condition empty = Condition.create(toBeTested);
    } catch (PreparedQueryParsingException e) {
      return;
    }

    fail ("Parser accepted the condition '" + toBeTested + "' as valid");

  }


  public void test_1_09_SimpleWhereClauseParsing() throws Exception {

    String toBeTested = "id     is  null";

    Condition empty = Condition.create(toBeTested);

    assertEquals(0, empty.getConditions().size());

    assertEquals("id", empty.getExpression1());
    assertNull("?", empty.getExpression2());
    assertEquals("is null", empty.getExpressionOperator());

  }


  public void test_1_10_SimpleWhereClauseParsing() throws Exception {
    String toBeTested = "id     is  not  null";
    Condition empty = Condition.create(toBeTested);

    assertEquals("id is not null null", createNormalForm(empty));
  }


  public void test_2_01_WhereClauseParsing() throws Exception {
    Condition x1 = Condition.create("id=? and not(ordertype=?) and not(blacondition=?)");
    assertEquals("id = ? && !(ordertype = ?) && !(blacondition = ?)", createNormalForm(x1));
  }


  private String createNormalForm(Condition c) {
    StringBuilder sb = new StringBuilder();
    createString(c, sb);
    return sb.toString();
  }


  public void test_2_01a_WhereClauseParsing() throws Exception {

    Condition x1 = Condition.create("not(id=?)");

    assertEquals(1, x1.getConditions().size());
    assertEquals(0, x1.getOperators().size());
    assertTrue(x1.isNegated());

    Condition c1_1 = x1.getConditions().get(0);
    assertEquals(0, c1_1.getConditions().size());

    assertEquals("id", c1_1.getExpression1());
    assertEquals("?", c1_1.getExpression2());
    assertEquals("=", c1_1.getExpressionOperator());

  }

  public void test_2_02_WhereClauseParsing() throws Exception {

    Condition x2 =
        Condition.create("id LIKE ? and not ( orderType = ? or orderType = ? or orderType = ? or orderType = ?"
            + " or orderType = ? or orderType = ?) and not ( id = ?)");

    assertEquals(3, x2.getConditions().size());
    assertEquals(2, x2.getOperators().size());

    Condition c2_1 = x2.getConditions().get(0);
    assertEquals(0, c2_1.getConditions().size());
    assertEquals(0, c2_1.getOperators().size());

    Condition c2_2 = x2.getConditions().get(1);
    assertEquals(6, c2_2.getConditions().size());
    assertTrue(c2_2.isNegated());
    assertEquals(5, c2_2.getOperators().size());

    Condition c2_3 = x2.getConditions().get(2);
    assertEquals(1, c2_3.getConditions().size());
    assertTrue(c2_3.isNegated());
    assertEquals(0, c2_3.getOperators().size());

//Condition x = new PreparedQueryCreator().Condition.create(
//"",
//new PreparedQueryCreator().new IntegerHelper(0), 0);
//Condition x = new PreparedQueryCreator().Condition.create("id4 LIKE ? and not ( id1 LIKE ? or id2 LIKE ?) or id5=?",
//new PreparedQueryCreator().new IntegerHelper(0), false);
//Condition x = new PreparedQueryCreator().Condition.create("(id1 LIKE ? and id2 LIKE ?) and id4 LIKE ?",
//new PreparedQueryCreator().new IntegerHelper(0), false);
//Condition x = new PreparedQueryCreator().Condition.create("not (id1 LIKE ? and id1a=?) and (id2=? or id2a LIKE ?) or not ( id4 = ? )",
//new PreparedQueryCreator().new IntegerHelper(0), false);

  }


  public void test_2_03_WhereClauseParsing() throws Exception {

    Condition x1 =
        Condition.create("not ( id1 LIKE ? and not (id2 LIKE ? and id2b=?) or id3 LIKE ?)"
            + " and id4 LIKE ? and ( status = ? or status2 = ? or status3 = ?)" + " or not ( id10 = ?)");

    assertEquals(4, x1.getConditions().size());
    assertEquals(3, x1.getOperators().size());

    assertEquals(x1.getOperators().get(0).toJava(), "&&");
    assertEquals(x1.getOperators().get(1).toJava(), "&&");
    assertEquals(x1.getOperators().get(2).toJava(), "||");


    Condition c1_1 = x1.getConditions().get(0);
    assertEquals(3, c1_1.getConditions().size());
    assertTrue(c1_1.isNegated());
    assertEquals(2, c1_1.getOperators().size());

    assertEquals(c1_1.getOperators().get(0).toJava(), "&&");
    assertEquals(c1_1.getOperators().get(1).toJava(), "||");


    Condition c1_1_2 = c1_1.getConditions().get(1);
    assertEquals(2, c1_1_2.getConditions().size());
    assertTrue(c1_1_2.isNegated());
    assertEquals(1, c1_1_2.getOperators().size());
    assertEquals(c1_1_2.getOperators().get(0).toJava(), "&&");

    assertEquals(0, c1_1_2.getConditions().get(0).getConditions().size());
    assertEquals(0, c1_1_2.getConditions().get(1).getConditions().size());
    assertEquals(0, c1_1_2.getConditions().get(0).getOperators().size());
    assertEquals(0, c1_1_2.getConditions().get(1).getOperators().size());

    Condition c1_2 = x1.getConditions().get(1);
    assertEquals(0, c1_2.getConditions().size());
    assertEquals(0, c1_2.getOperators().size());

    Condition c1_3 = x1.getConditions().get(2);
    assertEquals(3, c1_3.getConditions().size());
    assertEquals(2, c1_3.getOperators().size());
    assertEquals(c1_3.getOperators().get(0).toJava(), "||");
    assertEquals(c1_3.getOperators().get(1).toJava(), "||");

    assertEquals(0, c1_3.getConditions().get(0).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(1).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(2).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(0).getOperators().size());
    assertEquals(0, c1_3.getConditions().get(1).getOperators().size());
    assertEquals(0, c1_3.getConditions().get(2).getOperators().size());

    Condition c1_4 = x1.getConditions().get(3);
    assertEquals(1, c1_4.getConditions().size());
    assertTrue(c1_4.isNegated());
    assertEquals(0, c1_4.getOperators().size());

  }


  public void test_2_04_WhereClauseParsing() throws Exception {

    Condition x1 =
        Condition.create("not ((id2 LIKE ? and id2b=?) or id3 LIKE ?) and id4 LIKE ?"
            + " and ( status = ? or status2 = ? or status3 = ?) or not ( id10 = ?)");

    assertEquals(4, x1.getConditions().size());
    assertEquals(3, x1.getOperators().size());

    assertEquals(x1.getOperators().get(0).toJava(), "&&");
    assertEquals(x1.getOperators().get(1).toJava(), "&&");
    assertEquals(x1.getOperators().get(2).toJava(), "||");


    Condition c1_1 = x1.getConditions().get(0);
    assertEquals(2, c1_1.getConditions().size());
    assertTrue(c1_1.isNegated());
    assertEquals(1, c1_1.getOperators().size());

    assertEquals(c1_1.getOperators().get(0).toJava(), "||");


    Condition c1_1_1 = c1_1.getConditions().get(0);
    assertEquals(2, c1_1_1.getConditions().size());
    assertFalse(c1_1_1.isNegated());
    assertEquals(1, c1_1_1.getOperators().size());
    assertEquals(c1_1_1.getOperators().get(0).toJava(), "&&");

    assertEquals(0, c1_1_1.getConditions().get(0).getConditions().size());
    assertEquals(0, c1_1_1.getConditions().get(1).getConditions().size());
    assertEquals(0, c1_1_1.getConditions().get(0).getOperators().size());
    assertEquals(0, c1_1_1.getConditions().get(1).getOperators().size());

    Condition c1_2 = x1.getConditions().get(1);
    assertEquals(0, c1_2.getConditions().size());
    assertEquals(0, c1_2.getOperators().size());

    Condition c1_3 = x1.getConditions().get(2);
    assertEquals(3, c1_3.getConditions().size());
    assertEquals(2, c1_3.getOperators().size());
    assertEquals(c1_3.getOperators().get(0).toJava(), "||");
    assertEquals(c1_3.getOperators().get(1).toJava(), "||");

    assertEquals(0, c1_3.getConditions().get(0).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(1).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(2).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(0).getOperators().size());
    assertEquals(0, c1_3.getConditions().get(1).getOperators().size());
    assertEquals(0, c1_3.getConditions().get(2).getOperators().size());

    Condition c1_4 = x1.getConditions().get(3);
    assertEquals(1, c1_4.getConditions().size());
    assertTrue(c1_4.isNegated());
    assertEquals(0, c1_4.getOperators().size());

  }


  public void testTrailingSpaces() throws Exception {
    Condition x1 =
        Condition.create("  not         (             id1           =              ?          )                 ");
    assertEquals("!(id1 = ?)", createNormalForm(x1));
  }


  public void test_2_05_WhereClauseParsing() throws Exception {

    Condition x1 =
        Condition.create("not  (id1=? and not   ( id2 LIKE ? and id2b=?))"
            + " and id4 LIKE ? and ( status = ? or status2 = ? or status3 = ?)" + " or not ( id10 = ?)");

    assertEquals(4, x1.getConditions().size());
    assertEquals(3, x1.getOperators().size());

    assertEquals(x1.getOperators().get(0).toJava(), "&&");
    assertEquals(x1.getOperators().get(1).toJava(), "&&");
    assertEquals(x1.getOperators().get(2).toJava(), "||");


    Condition c1_1 = x1.getConditions().get(0);
    assertEquals(2, c1_1.getConditions().size());
    assertTrue(c1_1.isNegated());
    assertEquals(1, c1_1.getOperators().size());

    assertEquals(c1_1.getOperators().get(0).toJava(), "&&");


    Condition c1_1_2 = c1_1.getConditions().get(1);
    assertEquals(2, c1_1_2.getConditions().size());
    assertTrue(c1_1_2.isNegated());
    assertEquals(1, c1_1_2.getOperators().size());
    assertEquals(c1_1_2.getOperators().get(0).toJava(), "&&");

    assertEquals(0, c1_1_2.getConditions().get(0).getConditions().size());
    assertEquals(0, c1_1_2.getConditions().get(1).getConditions().size());
    assertEquals(0, c1_1_2.getConditions().get(0).getOperators().size());
    assertEquals(0, c1_1_2.getConditions().get(1).getOperators().size());

    Condition c1_2 = x1.getConditions().get(1);
    assertEquals(0, c1_2.getConditions().size());
    assertEquals(0, c1_2.getOperators().size());

    Condition c1_3 = x1.getConditions().get(2);
    assertEquals(3, c1_3.getConditions().size());
    assertEquals(2, c1_3.getOperators().size());
    assertEquals(c1_3.getOperators().get(0).toJava(), "||");
    assertEquals(c1_3.getOperators().get(1).toJava(), "||");

    assertEquals(0, c1_3.getConditions().get(0).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(1).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(2).getConditions().size());
    assertEquals(0, c1_3.getConditions().get(0).getOperators().size());
    assertEquals(0, c1_3.getConditions().get(1).getOperators().size());
    assertEquals(0, c1_3.getConditions().get(2).getOperators().size());

    Condition c1_4 = x1.getConditions().get(3);
    assertEquals(1, c1_4.getConditions().size());
    assertTrue(c1_4.isNegated());
    assertEquals(0, c1_4.getOperators().size());

  }
  
  public void test_2_06_WhereClauseParsing() throws PreparedQueryParsingException {

    Condition x1 =
        Condition.create("(slaveOrdertype LIKE 'xdev%') and   (( (application IS NULL ) and (slaveOrdertype " + 
            "LIKE '%') and ( version IS NULL ))or  (   ( application  LIKE '%' ) and (slaveOrdertype " + 
            "LIKE   '%') and (version LIKE '2%')))  ");

    StringBuilder sb = new StringBuilder();
    createString(x1, sb);
  //  System.out.println(sb);
    assertEquals("slaveOrdertype like 'xdev%' && ((application is null null && slaveOrdertype like '%' && version is null null) || (application like '%' && slaveOrdertype like '%' && version like '2%'))",
                 sb.toString());

  }


  private void createString(Condition c, StringBuilder sb) {
    if (c.getExpr1() != null) {
      if (c.isNegated()) {
        sb.append("!(");
      }
      sb.append(c.getExpr1()).append(" ").append(c.getConditionOperator().getSql()).append(" ").append(c.getExpr2());
      if (c.isNegated()) {
        sb.append(")");
      }
    } else {
      //c ist eine sammlung von sub-conditions
      for (int i = 0; i< c.getConditions().size(); i++) {
        boolean braceRequired = c.getConditions().get(i).getConditions().size() > 1 || c.isNegated();
        if (c.isNegated()) {
          sb.append("!");
        }
        if (braceRequired) {
          sb.append("(");
        }
        createString(c.getConditions().get(i), sb);
        if (braceRequired) {
          sb.append(")");
        }
        if (i < c.getConditions().size()-1) {          
          sb.append(" ").append(c.getOperators().get(i).toJava()).append(" ");
        }
      }
    }
  }
  
  public void testStackoverflow() throws PreparedQueryParsingException {
    StringBuilder s = new StringBuilder("ordertoresume = ? ");
    for (int i = 0; i<100000; i++) {
      s.append("or ordertoresume = ? ");
    }
    Condition c = Condition.create(s.toString());
  }
  
  public void testIn() throws PreparedQueryParsingException {
    Condition c = Condition.create("a in (?, ?, ?, 'asd') or not b < ?");
    String s = createNormalForm(c);
    assertEquals("a in (?, ?, ?, 'asd') || !(b < ?)", s);
    
    c = Condition.create("a in ('asd') or not b < ?");
    s = createNormalForm(c);
    assertEquals("a in ('asd') || !(b < ?)", s);

    c = Condition.create("a in (?) or not b < ?");
    s = createNormalForm(c);
    assertEquals("a in (?) || !(b < ?)", s);

    c = Condition.create("a in () or not b < ?");
    s = createNormalForm(c);
    assertEquals("a in () || !(b < ?)", s);

    c = Condition.create("a in ('asd', ?, ?, 31, 'asd') or not b < ?");
    s = createNormalForm(c);
    assertEquals("a in ('asd', ?, ?, 31, 'asd') || !(b < ?)", s);
  }
  
  public void testErroneousConditions() {
    checkErroneous("abc");
    checkErroneous("abc <");
    checkErroneous("abc !");
    checkErroneous("abc =");
    checkErroneous("abc < <");
    checkErroneous("abc is not");
    checkErroneous("abc is 17");
    checkErroneous("abc is not abc");
    checkErroneous("abc like or");
    checkErroneous("abc or abc");
    checkErroneous("abc < not abc");
    checkErroneous("abc not < abc");
    checkErroneous("(abc < ?");
    checkErroneous("(abc < ?) or (abc > ?");
    checkErroneous("(abc < ? or (abc > ?) or x = 5");
    checkErroneous("((abc < ? or (abc > ?)) or x = 5");
    checkErroneous("(abc < ?) or (abc > ? or (abc > ?)");
    checkErroneous("(abc < ?) or (abc > ? or (abc) > ?)");
    checkErroneous("abc < ? or (> 3)");
    checkErroneous("abc < ? or > 3 or > 4");
    checkErroneous("abc < ? or a in (3, 5");
    checkErroneous("abc < ? or a in (? 5)");
    checkErroneous("abc < ? or a in (? OR ?)");
    checkErroneous("abc < ? or a in (?, OR, ?)");
    checkErroneous("abc < ? or a in (?, !, ?)");
    checkErroneous("abc < ? or a in not (?)");
    checkErroneous("abc < ? or a in ?, 'asd', ?)");
    checkErroneous("abc < ? or a in (?, (?), ?)");
  }


  private void checkErroneous(String string) {
    try {
      Condition.create(string);
      fail(string);
    } catch (PreparedQueryParsingException e) {
      System.out.println("sql \"" + string + "\" invalid: " + e.getMessage());
      //ok
    }
  }
  
  public void testEscapedSpecialChars() throws PreparedQueryParsingException {
    checkSQL("a = '\\'as' and b = 3");
    checkSQL("a = '\\\\' and b = ''");
    checkSQL("a = '\\g\\f\\' or\\\'' and b = ''");
  }


  private void checkSQL(String string) throws PreparedQueryParsingException {
    Condition c = Condition.create(string);
    assertEquals(string.replace(" and ", " && ").replace(" or ", " || "), createNormalForm(c));
  }


  public void testPerformance() throws PreparedQueryParsingException {
    for (int r = 0; r < 2; r++) {
      long l = System.currentTimeMillis();
      for (int i = 0; i < 100000; i++) {
        Condition.create("abc < 51333");
      }
      for (int i = 0; i < 100000; i++) {
        Condition.create("(slaveOrdertype LIKE 'xdev%') and   (( (application IS NULL ) and (slaveOrdertype"
            + "            LIKE '%') and ( version IS NULL ))or  (   ( application  LIKE '%' ) and (slaveOrdertype"
            + "           LIKE   '%') and (version LIKE '2%'))) ");
      }
      if (r == 1) {
        assertTrue(System.currentTimeMillis() - l < 5000);
      }
    }
  }
  
  public void testNotEqualIsNot_Equal_() throws PreparedQueryParsingException {
    Condition c = Condition.create("a != '2'");
    assertEquals(ConditionOperator.EQUALS, c.getConditionOperator());
    assertTrue(c.isNegated());
  }
  
 }
