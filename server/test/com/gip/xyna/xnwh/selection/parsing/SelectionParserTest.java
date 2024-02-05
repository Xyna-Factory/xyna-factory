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
package com.gip.xyna.xnwh.selection.parsing;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParameters;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;


public class SelectionParserTest extends TestCase {

  private static class EscapeForMemory implements EscapeParameters {

    public String escapeForLike(String toEscape) {
      if (toEscape == null || toEscape.length() == 0) {
        return toEscape;
      }
      return Pattern.quote(toEscape);
    }

    @Override
    public String getMultiCharacterWildcard() {
      return ".*";
    }

    @Override
    public String getSingleCharacterWildcard() {
      return ".";
    }
    
  }
  
  private static class EscapeForOracle implements EscapeParameters {

    public String escapeForLike(String toEscape) {
      toEscape = toEscape.replaceAll("%", "\\\\%");
      toEscape = toEscape.replaceAll("_", "\\\\_");
      return toEscape;
    }

    @Override
    public String getMultiCharacterWildcard() {
      return "%";
    }

    @Override
    public String getSingleCharacterWildcard() {
      return "_";
    }
    
  }
  
  
  /**
   * Tested method SelectionParser#generateSelectObjectFromSearchRequestBean expects input from GUI.
   * Not escaped escape characters in EQUALS queries are removed.
   */
  public void testGenerateSelectObject() throws XNWH_InvalidSelectStatementException, XNWH_SelectParserException {
    //Suche nach einem normalen Anführungszeichen
    VetoSelectImpl selection = generateVetoSelectObject("\\\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("\"", selection.getParameter().get(0));
    
    //Filter enthält ein '
    selection = generateVetoSelectObject("x'y");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("x'y", selection.getParameter().get(0));

    //Suche nach Literal
    selection = generateVetoSelectObject("x");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));

    //Suche nach escaptem Literal
    selection = generateVetoSelectObject("\"x\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));
    
    //Negation
    selection = generateVetoSelectObject("!x");
    assertEquals("select vetoName from vetos where not ( documentation = ?)", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));

    //Negation mit Klammern
    selection = generateVetoSelectObject("!(x)");
    assertEquals("select vetoName from vetos where not ( documentation = ?)", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));

    //Negation mit Klammern und Verknüpfung
    selection = generateVetoSelectObject("!(x OR y)");
    assertEquals("select vetoName from vetos where not ( documentation = ? or documentation = ?)", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));
    assertEquals("y", selection.getParameter().get(1));
    
    //Wildcard *
    selection = generateVetoSelectObject("*x");
    assertEquals("select vetoName from vetos where documentation LIKE ?", selection.getSelectString());
    assertEquals("%x", selection.getParameter().get(0));
    
    //Suche nach dem Wert *x (durch " escaped)
    selection = generateVetoSelectObject("\"*x\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("*x", selection.getParameter().get(0));

    //Suche nach dem Wert *x (durch \ escaped)
    selection = generateVetoSelectObject("\\*x");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("*x", selection.getParameter().get(0));

    //Suche nach Werten, die mit \" beginnen und mit x aufhören
    selection = generateVetoSelectObject("\\\"*x");
    assertEquals("select vetoName from vetos where documentation LIKE ?", selection.getSelectString());
    assertEquals("\\\"%x", selection.getParameter().get(0));

    //Suche nach dem Wert \*x
    selection = generateVetoSelectObject("\\\\\"*x\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("\\*x", selection.getParameter().get(0));

    //Suche nach dem Wert %x (durch " escaped)
    selection = generateVetoSelectObject("\"%x\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("%x", selection.getParameter().get(0));

    //Suche nach dem Wert %x (durch \ escaped)
    selection = generateVetoSelectObject("\\%x");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("%x", selection.getParameter().get(0));
    
    //* escaped und als Wildcard
    selection = generateVetoSelectObject("\"*x\"*y\\\"z\"*\"\"\"*");
    assertEquals("select vetoName from vetos where documentation LIKE ?", selection.getSelectString());
    assertEquals("\"*x\"%y\\\"z\"*\"\"\"%", selection.getParameter().get(0));

    //Negation mit Wildcard
    selection = generateVetoSelectObject("!x*y");
    assertEquals("select vetoName from vetos where not ( documentation LIKE ?)", selection.getSelectString());
    assertEquals("x%y", selection.getParameter().get(0));
    
    //Suche nach Werten größer als x
    selection = generateVetoSelectObject(">x");
    assertEquals("select vetoName from vetos where documentation > ?", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));

    //Suche nach Werten kleiner als x
    selection = generateVetoSelectObject("<x");
    assertEquals("select vetoName from vetos where documentation < ?", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));
    
    //Suche nach dem Wert >x (durch " escaped)
    selection = generateVetoSelectObject("\">x\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals(">x", selection.getParameter().get(0));

    //Suche nach dem Wert >x (durch \ escaped)
    selection = generateVetoSelectObject("\\>x");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals(">x", selection.getParameter().get(0));
    
    //Oder-Verknüfung
    selection = generateVetoSelectObject("x OR y");
    assertEquals("select vetoName from vetos where documentation = ? or documentation = ?", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));
    assertEquals("y", selection.getParameter().get(1));

    //escapetes "OR"
    selection = generateVetoSelectObject("x \"OR\" y");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("x OR y", selection.getParameter().get(0));
    
    //Verknüfung mit Wildcard, Negation und Klammern
    selection = generateVetoSelectObject("x OR (y* AND !z)");
    assertEquals("select vetoName from vetos where documentation = ? or ( documentation LIKE ? and not ( documentation = ?))", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));
    assertEquals("y%", selection.getParameter().get(1));
    assertEquals("z", selection.getParameter().get(2));

    //Verknüfung mit Wildcard, Negation und Klammern
    selection = generateVetoSelectObject("(x* OR y*) AND !*z");
    assertEquals("select vetoName from vetos where ( documentation LIKE ? or documentation LIKE ?) and not ( documentation LIKE ?)", selection.getSelectString());
    assertEquals("x%", selection.getParameter().get(0));
    assertEquals("y%", selection.getParameter().get(1));
    assertEquals("%z", selection.getParameter().get(2));
    
    //Verknüfung mit Wildcard, Negation und (escapten) Klammern
    selection = generateVetoSelectObject("(x OR y) AND \\(z");
    assertEquals("select vetoName from vetos where ( documentation = ? or documentation = ?) and documentation = ?", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));
    assertEquals("y", selection.getParameter().get(1));
    assertEquals("(z", selection.getParameter().get(2));

    //Verknüfung mit Wildcard, Negation und (escapten) Klammern
    selection = generateVetoSelectObject("(x OR y) AND \"(z\"");
    assertEquals("select vetoName from vetos where ( documentation = ? or documentation = ?) and documentation = ?", selection.getSelectString());
    assertEquals("x", selection.getParameter().get(0));
    assertEquals("y", selection.getParameter().get(1));
    assertEquals("(z", selection.getParameter().get(2));
    
    //ungültiger Filter -> suche Literal 
    selection = generateVetoSelectObject("(x OR y) AND (z");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("(x OR y) AND (z", selection.getParameter().get(0));

    //ungültiger Filter -> suche Literal 
    selection = generateVetoSelectObject("\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("\"", selection.getParameter().get(0));

    //ungültiger Filter -> suche Literal 
    selection = generateVetoSelectObject("\"x");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("\"x", selection.getParameter().get(0));

    //ungültiger Filter -> suche Literal 
    selection = generateVetoSelectObject("\\\"x\"");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals("\\\"x\"", selection.getParameter().get(0));

    //ungültiger Filter -> suche Literal 
    selection = generateVetoSelectObject(">!3");
    assertEquals("select vetoName from vetos where documentation = ?", selection.getSelectString());
    assertEquals(">!3", selection.getParameter().get(0));
  }
  
  
  /**
   * escapeParams does not change EQUALS queries
   */
  public void testEscapeEqualParamsForMemory() {
    EscapeParameters escape = new EscapeForMemory();
    boolean like = false;
    assertEquals("x", SelectionParser.escapeParams("x", like, escape));
    assertEquals("\"x\"", SelectionParser.escapeParams("\"x\"", like, escape));
    assertEquals("x\"y\"\\\"", SelectionParser.escapeParams("x\"y\"\\\"", like, escape));
    assertEquals("x\\\\\\\"y", SelectionParser.escapeParams("x\\\\\\\"y", like, escape));
    assertEquals("%x", SelectionParser.escapeParams("%x", like, escape));
    assertEquals("\"%x\"", SelectionParser.escapeParams("\"%x\"", like, escape));
    assertEquals("\"%x\"%y\"\"z\"%\"\"\"%", SelectionParser.escapeParams("\"%x\"%y\"\"z\"%\"\"\"%", like, escape));
    assertEquals("x_y", SelectionParser.escapeParams("x_y", like, escape));
    assertEquals("x\"_\"y", SelectionParser.escapeParams("x\"_\"y", like, escape));
    assertEquals("x\\\\y", SelectionParser.escapeParams("x\\\\y", like, escape));
    assertEquals("\"x_y%\"%z\\%", SelectionParser.escapeParams("\"x_y%\"%z\\%", like, escape));
  }

  public void testEscapeEqualParamsForOracle() {
    EscapeParameters escape = new EscapeForOracle();
    boolean like = false;
    assertEquals("x", SelectionParser.escapeParams("x", like, escape));
    assertEquals("\"x\"", SelectionParser.escapeParams("\"x\"", like, escape));
    assertEquals("x\"y\"\\\"", SelectionParser.escapeParams("x\"y\"\\\"", like, escape));
    assertEquals("x\\\\\\\"y", SelectionParser.escapeParams("x\\\\\\\"y", like, escape));
    assertEquals("%x", SelectionParser.escapeParams("%x", like, escape));
    assertEquals("\"%x\"", SelectionParser.escapeParams("\"%x\"", like, escape));
    assertEquals("\"%x\"%y\"\"z\"%\"\"\"%", SelectionParser.escapeParams("\"%x\"%y\"\"z\"%\"\"\"%", like, escape));
    assertEquals("x_y", SelectionParser.escapeParams("x_y", like, escape));
    assertEquals("x\"_\"y", SelectionParser.escapeParams("x\"_\"y", like, escape));
    assertEquals("x\\\\y", SelectionParser.escapeParams("x\\\\y", like, escape));
    assertEquals("\"x_y%\"%z\\%", SelectionParser.escapeParams("\"x_y%\"%z\\%", like, escape));
  }
  
  public void testEscapeLikeParamsForMemory() {
    EscapeParameters escape = new EscapeForMemory();
    boolean like = true;
    assertEquals("\\Qx\\E", SelectionParser.escapeParams("x", like, escape));
    assertEquals("\\Qx\\E", SelectionParser.escapeParams("\"x\"", like, escape));
    assertEquals("\\Qxy\"\\E", SelectionParser.escapeParams("x\"y\"\\\"", like, escape));
    assertEquals("\\Qx\\\"y\\E", SelectionParser.escapeParams("x\\\\\\\"y", like, escape));
    assertEquals(".*\\Qx\\E", SelectionParser.escapeParams("%x", like, escape));
    assertEquals("\\Q%x\\E", SelectionParser.escapeParams("\"%x\"", like, escape));
    assertEquals("\\Q%x\\E.*\\Qyz%\\E.*", SelectionParser.escapeParams("\"%x\"%y\"\"z\"%\"\"\"%", like, escape));
    assertEquals("\\Qx\\E.\\Qy\\E", SelectionParser.escapeParams("x_y", like, escape));
    assertEquals("\\Qx_y\\E", SelectionParser.escapeParams("x\\_y", like, escape));
    assertEquals("\\Qx_y\\E", SelectionParser.escapeParams("x\"_\"y", like, escape));
    assertEquals("\\Qx\\y\\E", SelectionParser.escapeParams("x\\\\y", like, escape));
    assertEquals("\\Qx\\y\"z\\E", SelectionParser.escapeParams("x\\\\y\\\"z", like, escape));
    assertEquals("\\Qx\\y\"z\\E", SelectionParser.escapeParams("x\\\\\\y\\\"z", like, escape));
    assertEquals("\\Qx_y%\\E.*\\Qz%\\E", SelectionParser.escapeParams("\"x_y%\"%z\\%", like, escape));
  }

  public void testEscapeLikeParamsForOracle() {
    EscapeParameters escape = new EscapeForOracle();
    boolean like = true;
    assertEquals("x", SelectionParser.escapeParams("x", like, escape));
    assertEquals("x", SelectionParser.escapeParams("\"x\"", like, escape));
    assertEquals("xy\"", SelectionParser.escapeParams("x\"y\"\\\"", like, escape));
    assertEquals("x\\\"y", SelectionParser.escapeParams("x\\\\\\\"y", like, escape));
    assertEquals("%x", SelectionParser.escapeParams("%x", like, escape));
    assertEquals("\\%x", SelectionParser.escapeParams("\"%x\"", like, escape));
    assertEquals("\\%x%yz\\%%", SelectionParser.escapeParams("\"%x\"%y\"\"z\"%\"\"\"%", like, escape));
    assertEquals("x\\_y", SelectionParser.escapeParams("x\\_y", like, escape));
    assertEquals("x_y", SelectionParser.escapeParams("x_y", like, escape));
    assertEquals("x\\_y", SelectionParser.escapeParams("x\"\\_\"y", like, escape));
    assertEquals("x\\y", SelectionParser.escapeParams("x\\\\y", like, escape));
    assertEquals("x\\_y\\%%z\\%", SelectionParser.escapeParams("\"x_y%\"%z\\%", like, escape));

  }


  private VetoSelectImpl generateVetoSelectObject(String filter) throws XNWH_NoSelectGivenException, XNWH_WhereClauseBuildException, XNWH_SelectParserException {
    SearchRequestBean srb = new SearchRequestBean();
    srb.setSelection("vetoName");
    srb.setArchiveIdentifier(ArchiveIdentifier.vetos);
    
    Map<String, String> filterEntries = new HashMap<String, String>();
    filterEntries.put("documentation", filter);
    srb.setFilterEntries(filterEntries);
    
    return (VetoSelectImpl) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
  }
}
