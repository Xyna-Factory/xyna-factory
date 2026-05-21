/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package pkg;

//import org.junit.jupiter.api.Test;  // if Junit 5 is used?
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator;

import xmcp.zeta.storage.generic.filter.elems.ContainerElem;
import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.elems.TokenOpElem;
import xmcp.zeta.storage.generic.filter.lexer.FilterInputLexer;
import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.parser.phase1.DoubleOperatorAdapter;
import xmcp.zeta.storage.generic.filter.parser.phase1.LiteralOperatorAdapter;
import xmcp.zeta.storage.generic.filter.parser.phase1.QuoteHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.ParenthesesHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.TokenAdapter;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Replacer;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public class TestFilterInputParser {

  @Test
  public void testQuotes1() {
    try {
      String input = "=abc 'o=p*q\"123\"<r'xyz | =\"aa'uv&st'bb\"";
      List<Token> tokens = new FilterInputLexer().execute(input);
      tokens = new QuoteHandler().execute(tokens);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      assertEquals("o=p*q\"123\"<r", tokens.get(3).getOriginalInput());
      assertEquals("aa'uv&st'bb", tokens.get(9).getOriginalInput());
      
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      
      ContainerElem root = new ContainerElem(elems);
      log(root.writeTreeInfo());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testQuotes2() {
    try {
      String input = "'12342'";
      
      FilterElement root = new FilterInputParser().parse(input);
      
      log(root.writeTreeInfo());
      assertEquals("Equals", root.getInfoString());
      FilterElement elem = root.getChild(0).get();
      assertEquals("12342", elem.getInfoString());
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  
  @Test
  public void testParentheses1() {
    try {
      String input = "!(>1 | <20) & (=10 | !(<5 & >3))";
      List<Token> tokens = new FilterInputLexer().execute(input);
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      elems = new ParenthesesHandler().execute(elems);
      ContainerElem root = new ContainerElem(elems);
      
      FilterElement elem = root.getChild(0).get();
      assertTrue(elem instanceof TokenOpElem);
      assertEquals(Enums.LexedOperatorCategory.NOT, ((TokenOpElem) elem).getCategory());
      elem = root.getChild(1).get().getChild(0).get();
      assertTrue(elem instanceof TokenOpElem);
      assertEquals(Enums.LexedOperatorCategory.GREATER_THAN, ((TokenOpElem) elem).getCategory());
      
      log(root.writeTreeInfo());
      
      elem = root.getChild(1).get().getChild(1).get();
      assertEquals("1", elem.getInfoString());
      elem = root.getChild(1).get().getChild(2).get();
      assertEquals("TOKEN-OR", elem.getInfoString());
      elem = root.getChild(2).get();
      assertEquals("TOKEN-AND", elem.getInfoString());
      elem = root.getChild(3).get().getChild(0).get();
      assertEquals("TOKEN-EQUALS", elem.getInfoString());
      elem = root.getChild(3).get().getChild(1).get();
      assertEquals("10", elem.getInfoString());
      elem = root.getChild(3).get().getChild(3).get();
      assertEquals("TOKEN-NOT", elem.getInfoString());
      elem = root.getChild(3).get().getChild(4).get().getChild(2).get();
      assertEquals("TOKEN-AND", elem.getInfoString());
      elem = root.getChild(3).get().getChild(4).get().getChild(4).get();
      assertEquals("3", elem.getInfoString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testParseContainers1() {
    try {
      String input = "!(>1 | <20) & (=10 | !(<5 & >3))";
      
      List<Token> tokens = new FilterInputLexer().execute(input);
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      elems = new ParenthesesHandler().execute(elems);
      ContainerElem root = new ContainerElem(elems);
      root.parse(new FilterInputParser());
      log(root.writeTreeInfo());
      
      FilterElement elem = root.getChild(0).get();
      assertEquals("AND", elem.getInfoString());
      elem = root.getChild(0).get().getChild(0).get();
      assertEquals("NOT", elem.getInfoString());
      elem = root.getChild(0).get().getChild(0).get().getChild(0).get();
      assertEquals("OR", elem.getInfoString());
      
      elem = root.getChild(0).get().getChild(0).get().getChild(0).get().getChild(1).get();
      assertEquals("LessThan", elem.getInfoString());
      elem = root.getChild(0).get().getChild(0).get().getChild(0).get().getChild(1).get().getChild(0).get();
      assertEquals("20", elem.getInfoString());
      
      elem = root.getChild(0).get().getChild(1).get();
      assertEquals("OR", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get().getChild(1).get();
      assertEquals("NOT", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get().getChild(1).get().getChild(0).get();
      assertEquals("AND", elem.getInfoString());
      
      elem = root.getChild(0).get().getChild(1).get().getChild(1).get().getChild(0).get().getChild(1).get();
      assertEquals("GreaterThan", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get().getChild(1).get().getChild(0).get().getChild(1).get().getChild(0).get();
      assertEquals("3", elem.getInfoString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testSql1() {
    try {
      String input = "!(>1 | <20) & (=30 | !(<5 & >3))";
      FilterElement root = new FilterInputParser().parse(input);
      
      SqlWhereClauseData str = new SqlWhereClauseData(new QueryGenerator());
      root.writeSql("col-1", str);
      String sql = str.toString();
      log(sql);
      logParameters(str);
      assertEquals("(NOT ((col-1 > ?) OR (col-1 < ?))) AND ((col-1 LIKE ?) OR (NOT ((col-1 < ?) AND (col-1 > ?))))", sql);
      assertEquals(5, str.getParameters().size());
      assertEquals("1", str.getParameters().get(0));
      assertEquals("20", str.getParameters().get(1));
      assertEquals("%30%", str.getParameters().get(2));
      assertEquals("5", str.getParameters().get(3));
      assertEquals("3", str.getParameters().get(4));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testSql2() {
    try {
      String input = "(!111 | 20*1) & (30* | !(=553))";
      
      List<Token> tokens = new FilterInputLexer().execute(input);
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      
      FilterElement root = new FilterInputParser().parse(input);
      
      SqlWhereClauseData str = new SqlWhereClauseData(new QueryGenerator());
      root.writeSql("col-1", str);
      String sql = str.toString();
      log(sql);
      logParameters(str);
      assertEquals("((NOT (col-1 LIKE ?)) OR (col-1 LIKE ?)) AND ((col-1 LIKE ?) OR (NOT (col-1 LIKE ?)))", sql);
      assertEquals(4, str.getParameters().size());
      assertEquals("%111%", str.getParameters().get(0));
      assertEquals("20%1", str.getParameters().get(1));
      assertEquals("30%", str.getParameters().get(2));
      assertEquals("%553%", str.getParameters().get(3));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testWildcards() {
    try {
      String input = "(!20*1 & 210*) |(*55*3 & ! 6*6*799)";
      FilterElement root = new FilterInputParser().parse(input);
      
      log(root.writeTreeInfo());
      
      FilterElement elem = root.getChild(0).get().getChild(0).get().getChild(0).get().getChild(0).get();
      assertEquals("20%1", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get().getChild(0).get();
      assertEquals("210%", elem.getInfoString());
      elem = root.getChild(1).get().getChild(0).get().getChild(0).get();
      assertEquals("%55%3", elem.getInfoString());
      elem = root.getChild(1).get().getChild(1).get().getChild(0).get().getChild(0).get();
      assertEquals("6%6%799", elem.getInfoString());
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testWildcardSql() {
    try {
      String input = "'20*1' | 210* | '2111' | '12_4%5\\' | 12_4%5\\6 | 34555 ";
      FilterElement root = new FilterInputParser().parse(input);
      
      log(root.writeTreeInfo());
      
      SqlWhereClauseData str = new SqlWhereClauseData(new QueryGenerator());
      root.writeSql("col-1", str);
      String sql = str.toString();
      log(sql);
      logParameters(str);
      
      assertEquals("(((((col-1 = ?) OR (col-1 LIKE ?)) OR (col-1 = ?)) OR (col-1 = ?))" +
                   " OR (col-1 LIKE ?)) OR (col-1 LIKE ?)", sql);
      assertEquals(6, str.getParameters().size());
      assertEquals("20*1", str.getParameters().get(0));
      assertEquals("210%", str.getParameters().get(1));
      assertEquals("2111", str.getParameters().get(2));
      assertEquals("12_4%5\\\\", str.getParameters().get(3));
      assertEquals("%12\\_4\\%5\\\\\\\\6%", str.getParameters().get(4));
      assertEquals("%34555%", str.getParameters().get(5));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testOps1() {
    try {
      String input = "!>1 | <20 & =10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      ContainerElem root = new ContainerElem(elems);
      log(root.writeTreeInfo());
      
      FilterElement elem;
      elem = root.getChild(0).get();
      assertEquals("TOKEN-NOT", elem.getInfoString());
      elem = root.getChild(1).get();
      assertEquals("TOKEN-GREATER_THAN", elem.getInfoString());
      elem = root.getChild(2).get();
      assertEquals("1", elem.getInfoString());
      elem = root.getChild(6).get();
      assertEquals("TOKEN-AND", elem.getInfoString());
      elem = root.getChild(7).get();
      assertEquals("TOKEN-EQUALS", elem.getInfoString());
      elem = root.getChild(8).get();
      assertEquals("10", elem.getInfoString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testSpelledOps() {
    try {
      String input = "not>1 AND <20 or 10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      tokens = new LiteralOperatorAdapter().execute(tokens);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      FilterElement root = new FilterInputParser().parse(input);
      log(root.writeTreeInfo());
      
      FilterElement elem = root;
      assertEquals("OR", elem.getInfoString());
      elem = root.getChild(0).get();
      assertEquals("AND", elem.getInfoString());
      elem = root.getChild(0).get().getChild(0).get();
      assertEquals("NOT", elem.getInfoString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  
  @Test
  public void testDoubleOps() {
    try {
      String input = "!>1 && <20 || 10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      tokens = new DoubleOperatorAdapter().execute(tokens);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      ContainerElem root = new ContainerElem(elems);
      log(root.writeTreeInfo());
      
      FilterElement elem = root;
      elem = root.getChild(2).get();
      assertEquals("1", elem.getInfoString());
      elem = root.getChild(3).get();
      assertEquals("TOKEN-AND", elem.getInfoString());
      elem = root.getChild(4).get();
      assertEquals("TOKEN-LESS_THAN", elem.getInfoString());
      elem = root.getChild(5).get();
      assertEquals("20", elem.getInfoString());
      elem = root.getChild(6).get();
      assertEquals("TOKEN-OR", elem.getInfoString());
      elem = root.getChild(7).get();
      assertEquals("10", elem.getInfoString());
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testOpsOrder1() {
    try {
      String input = "!>1 | <20 & =10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      
      ContainerElem root = new ContainerElem(elems);
      root.parse(new FilterInputParser());
      log(root.writeTreeInfo());
      
      FilterElement elem = root;
      elem = root.getChild(0).get();
      assertEquals("OR", elem.getInfoString());
      elem = root.getChild(0).get().getChild(0).get();
      assertEquals("NOT", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get();
      assertEquals("AND", elem.getInfoString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testOpsOrder2() {
    try {
      String input = "!>1 & <20 | =10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      
      ContainerElem root = new ContainerElem(elems);
      root.parse(new FilterInputParser());
      log(root.writeTreeInfo());
      
      FilterElement elem = root;
      elem = root.getChild(0).get();
      assertEquals("OR", elem.getInfoString());
      elem = root.getChild(0).get().getChild(0).get();
      assertEquals("AND", elem.getInfoString());
      elem = root.getChild(0).get().getChild(0).get().getChild(0).get();
      assertEquals("NOT", elem.getInfoString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testOpsInsertEquals() {
    try {
      String input = "30 | <20 & !10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      
      ContainerElem root = new ContainerElem(elems);
      root.parse(new FilterInputParser());
      log(root.writeTreeInfo());
      
      FilterElement elem = root;
      elem = root.getChild(0).get().getChild(1).get();
      assertEquals("AND", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get().getChild(1).get();
      assertEquals("NOT", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get().getChild(1).get().getChild(0).get();
      assertEquals("Equals", elem.getInfoString());
      elem = root.getChild(0).get().getChild(1).get().getChild(1).get().getChild(0).get().getChild(0).get();
      assertEquals("10", elem.getInfoString());
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testTokenize() {
    try {
      String input = "(>1) & (<20) & (!= 10)";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      assertEquals("20", tokens.get(9).getOriginalInput());
      assertEquals(")", tokens.get(10).getOriginalInput());
      assertEquals("!", tokens.get(15).getOriginalInput());
      assertEquals("10", tokens.get(18).getOriginalInput());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testReplacer1() throws Exception {
    try {
      List<String> list = List.of("A1", "B2", "C3", "D4", "E5");
      List<String> adapted = new Replacer<String>().replaceInList(list, 2, 4, "new1");
      for (int i = 0; i <adapted.size(); i++) {
        log("" + i + ": " + adapted.get(i));
      }
      assertEquals(4, adapted.size());
      assertEquals("B2", adapted.get(1));
      assertEquals("new1", adapted.get(2));
      assertEquals("E5", adapted.get(3));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testReplacer2() throws Exception {
    try {
      List<String> list = List.of("A1", "B2", "C3", "D4", "E5");
      List<String> adapted = new Replacer<String>().replaceInList(list, 2, 5, "new2");
      for (int i = 0; i <adapted.size(); i++) {
        log("" + i + ": " + adapted.get(i));
      }
      assertEquals(3, adapted.size());
      assertEquals("B2", adapted.get(1));
      assertEquals("new2", adapted.get(2));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testReplacer3() throws Exception {
    try {
      List<String> list = List.of("A1", "B2", "C3", "D4", "E5");
      List<String> adapted = new Replacer<String>().replaceInList(list, 0, 1, "new3");
      for (int i = 0; i <adapted.size(); i++) {
        log("" + i + ": " + adapted.get(i));
      }
      assertEquals(5, adapted.size());
      assertEquals("new3", adapted.get(0));
      assertEquals("B2", adapted.get(1));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  private void logParameters(SqlWhereClauseData sql) {
    for (String str : sql.getParameters()) {
      log("SQL Parameter: " + str);
    }
  }
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new TestFilterInputParser().testSql1();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
