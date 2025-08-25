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

import xmcp.zeta.storage.generic.filter.elems.ContainerElem;
import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.lexer.FilterInputLexer;
import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.parser.phase2.TokenAdapter;
import xmcp.zeta.storage.generic.filter.shared.JsonWriter;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class TestFilterInputParser {

  // test replacer (mit strings); pos anfang, ende, mitte
  
  // test replace quotes; verschachtelte, kombin. '", leere quotes, pos anfang ende
  
  
  @Test
  public void testOps1() {
    try {
      String input = "!>1 | <20 & =10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      
      JsonWriter json = new JsonWriter();
      ContainerElem root = new ContainerElem(elems);
      root.writeJson(json);
      /*
      for (int i = 0; i < elems.size(); i++) {
        elems.get(i).writeJson(json);
      }
      */
      log(json.toString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testOps2() {
    try {
      String input = "!>1 | <20 & =10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      
      JsonWriter json = new JsonWriter();
      ContainerElem root = new ContainerElem(elems);
      root.writeJson(json);
      /*
      for (int i = 0; i < elems.size(); i++) {
        elems.get(i).writeJson(json);
      }
      */
      log(json.toString());
      log("##########");
      root.parse(new FilterInputParser());
      json = new JsonWriter();
      root.writeJson(json);
      log(json.toString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testOps3() {
    try {
      String input = "30 | <20 & !10";
      List<Token> tokens = new FilterInputLexer().execute(input);
      
      for (int i = 0; i < tokens.size(); i++) {
        log("" + i + ": " + tokens.get(i).getOriginalInput() + "  " + tokens.get(i).getClass().getName());
      }
      
      List<FilterElement> elems = new TokenAdapter().execute(tokens);
      
      ContainerElem root = new ContainerElem(elems);
      root.parse(new FilterInputParser());
      JsonWriter json = new JsonWriter();
      root.writeJson(json);
      log(json.toString());
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
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new TestFilterInputParser().testOps3();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  
}
