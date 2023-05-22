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
package com.gip.xyna.xnwh.selection.parsing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.StringSplitter;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel.DataModelSelectParser;
import com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem.DeploymentItemSelectParser;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource.OrderInputSourceSelectParser;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseBoolean;
import com.gip.xyna.xnwh.selection.WhereClauseEnum;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClauseStringSerializable;
import com.gip.xyna.xnwh.selection.WhereClauseStringTransformation;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;


public abstract class SelectionParser<P extends WhereClausesContainerBase<P>> {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(SelectionParser.class);

  private final static char CHARACTER_ESCAPE = '\\'; //einzelne Zeichen sind durch \ escaped
  private final static char CHARACTER_ESCAPE_SEQUENCE = '\"'; //Bereiche sind durch umgebende " escaped
  private final static String CHARACTER_ESCAPE_SEQUENCE_STRING = String.valueOf(CHARACTER_ESCAPE_SEQUENCE);
  public final static String CHARACTER_WILDCARD = "%";
  private final static String CHARACTER_NEGATION = "!";
  private final static String OR_IDENTIFIER = " OR ";
  private final static String AND_IDENTIFIER = " AND ";
  private final static String CHARACTER_GREATER = ">";
  private final static String CHARACTER_SMALLER = "<";
  private final static String CHARACTER_BRACE_OPEN = "(";
  private final static String CHARACTER_BRACE_CLOSE = ")";
  private final static String CHARACTER_NULL = "NULL";
  
  private final static Pattern PATTERN_SIMPLE_SIGNS = Pattern.compile("[^!<>()%\"]+");
  
  //Splittet einen String an %
  private final static StringSplitter SPLITTER_WILDCARD = new StringSplitter(CHARACTER_WILDCARD);
  
  public static enum Copula {
    AND, OR;
  }
  
  /*
   * {SimpleSigns: no " ( % }
   * {EscapedSign: Everything expect " }
   * {EscapedSigns: {EscapedSign}* }
   * {Escaped: "{EscapedSigns}"|\{EscapedSign} }
   * {EscapedEscaped: \" }
   * {Operator: > or < }
   * {SmallerStatement: '{Operator(<)} {SimpleSigns}'}
   * {GreaterStatement: '{Operator(>)} {SimpleSigns}'}
   * {LikeSign: %}
   * {LikeStatement: {EqualStatement}{LikeSign}{EqualStatement}}
   * {EqualStatement: ({Escaped}|{SimpleSigns})*}
   * {Copula: ' OR ' ' AND '} 
   * {SingleStatement: ({SmallerStatement}|{GreaterStatement}|{EqualStatement}|{LikeStatement})}
   * {LinkedStatement: {SingleStatement}({Copula}{SingleStatement})* }
   * {Negation: !{SimpleString}|!{LinkedStatement}}
   * {ComplexStatement: ({LinkedStatement}|{Negation})({Copula}({LinkedStatement}|{Negation})*}
   */
  
  
  protected abstract WhereClause<P> retrieveColumnWhereClause(String column, P whereClause);
  
  protected abstract P newWhereClauseContainer();
  
  protected abstract void parseSelectInternally(String select);

  protected abstract WhereClausesConnection<P> parseFilterInternally(Map<String, String> filters) throws XNWH_WhereClauseBuildException;
  
  
  public List<String> convertIntoTokens(String s) {
    Stack<String> tokens = new Stack<String>();
    try {
      s = replaceUnescapedCharacters(s, '*', '%');
      if (!acceptFilter(s, tokens)) {
        throw new IllegalArgumentException(s);
      }
    } catch (IllegalArgumentException e) {
      String escaped = escape(s);
      logger.debug("Could not parse " + s + ". Search for literal " + escaped);
      tokens.clear();
      tokens.push(Operator.EQUALS.toString());
      tokens.push(escaped);
    }
    return tokens;
  }
  
  /**
   * Ersetzt alle nicht escapten oldChar durch newChar
   * @param s
   * @param oldChar
   * @param newChar
   * @return
   */
  private static String replaceUnescapedCharacters(String s, char oldChar, char newChar) {
    List<EscapePart> escapeParts = split(s);
    
    StringBuilder sb = new StringBuilder();
    for (EscapePart ep : escapeParts) {
      if (ep.getState() == EscapeState.UNESCAPED) {
        sb.append(ep.getValue().replace(oldChar, newChar));
      } else {
        sb.append(ep.getValue());
      }
    }
    
    return sb.toString();
  }

  /**
   * Escaped den String wie folgt:
   * - Backslash wird durch doppelt Backslash ersetzt
   * - Anf�hrungszeichen wird durch Backslash+Anf�hrungszeichen ersetzt
   * - String wird mit Anf�hrungszeichen umgeben
   * @param s
   * @return
   */
  public static String escape(String s) {
    if (s == null || s.length() == 0) {
      return s;
    }
    s = s.replace("" + CHARACTER_ESCAPE, "" + CHARACTER_ESCAPE + CHARACTER_ESCAPE); 
    s = s.replace("" + CHARACTER_ESCAPE_SEQUENCE, "" + CHARACTER_ESCAPE + CHARACTER_ESCAPE_SEQUENCE);
    return CHARACTER_ESCAPE_SEQUENCE + s + CHARACTER_ESCAPE_SEQUENCE;
  }

  /**
   * Teilt den String in escapte und unescapte Anteile und Escape-Zeichen auf.
   * @param s
   * @return
   */
  private static List<EscapePart> split(String s) {
    List<EscapePart> ret = new ArrayList<SelectionParser.EscapePart>();
    StringBuilder part = new StringBuilder();
    int index = 0;
    boolean literal = false;
    boolean escaped = false;
    while (index < s.length()) {
      char c = s.charAt(index);
      if (!escaped) {
        switch (c) {
          case CHARACTER_ESCAPE :
            if (part.length() > 0) {
              ret.add(new EscapePart(part.toString(), literal ? EscapeState.ESCAPED : EscapeState.UNESCAPED));
              part.setLength(0);
            }
            ret.add(new EscapePart(String.valueOf(c), EscapeState.ESCAPE_CHARACTER));
            escaped = true;
            break;
          case CHARACTER_ESCAPE_SEQUENCE :
            if (part.length() > 0) {
              ret.add(new EscapePart(part.toString(), literal ? EscapeState.ESCAPED : EscapeState.UNESCAPED));
              part.setLength(0);
            }
            ret.add(new EscapePart(String.valueOf(c), EscapeState.ESCAPE_CHARACTER));
            literal = !literal;
            break;
          default:
            part.append(c);
        }
      } else {
        if (part.length() > 0) {
          ret.add(new EscapePart(part.toString(), EscapeState.ESCAPED));
          part.setLength(0);
        }
        ret.add(new EscapePart(String.valueOf(c), EscapeState.ESCAPED));
        escaped = false;
      }
      index++;
    }
    
    if (literal) {
      //Ende-" fehlt
      throw new IllegalArgumentException(s);
    }
    
    if (part.length() > 0) {
      ret.add(new EscapePart(part.toString(), escaped ? EscapeState.ESCAPED : EscapeState.UNESCAPED));
    }
    
    return ret;
  }


  private static Pair<Integer, Integer> getIndexOfEscapeSequenceCharacter(String s) {
    List<EscapePart> escapeParts = split(s);
    
    int startIndex = 0;
    int endIndex = 0;
    boolean start = true;
    for (EscapePart ep : escapeParts) {
      if (ep.getState() == EscapeState.ESCAPE_CHARACTER
                      && ep.getValue().equals(String.valueOf(CHARACTER_ESCAPE_SEQUENCE))) {
        if (start) {
          startIndex = startIndex + ep.getValue().length() - 1;
          start = false;
        } else {
          endIndex = endIndex + ep.getValue().length() - 1;
          break;
        }
      } else {
        if (start) {
          startIndex += ep.getValue().length();
        } else {
          endIndex += ep.getValue().length();
        }
      }
    }
    
    return Pair.of(startIndex, endIndex);
  }

  
  private boolean acceptFilter(String s, Stack<String> tokens) {    
    if (acceptLinkedStatements(s, tokens) ||
        acceptSingleStatement(s, tokens) ||
        acceptBracedStatement(s, tokens) ||
        acceptNegation(s, tokens)) {
      return true;
    } else {
      return false;
    }
  }
  
  
  private boolean acceptLinkedStatements(String s, Stack<String> tokens) {
    // split after first unescaped (and unbraced copulas)
    String[] parts = splitStringAroundFirstUnescapedAndUnbracedCopula(s);
    if (parts != null) {
      //firstPart
      if (acceptSingleStatement(parts[0], tokens) ||
          acceptNegation(parts[0], tokens) || 
          acceptBracedStatement(parts[0], tokens)) {
        tokens.push(parts[1].trim());
        //remains
        if (acceptLinkedStatements(parts[2], tokens) ||
            acceptSingleStatement(parts[2], tokens) ||
            acceptNegation(parts[2], tokens) ||
            acceptBracedStatement(parts[2], tokens)) {
          return true;
        } else {
          tokens.pop();
          return false;
        }
      } else {
        return false;
      }   
    } else {
      return false;
    }
  }
  
  
  private boolean acceptSingleStatement(String s, Stack<String> tokens) {
    if (acceptEqualStatement(s, tokens) || 
        acceptLikeStatement(s, tokens) ||
        acceptGreaterStatement(s, tokens) ||
        acceptSmallerStatement(s, tokens) ||
        acceptIsNULLStatement(s, tokens)) {
      return true;
    } else {
      return false;
    }
  }
  
  
  private boolean acceptIsNULLStatement(String s, Stack<String> tokens) {
    //{EqualStatement: ({Escaped}|{SimpleSigns})*}
    if (s.equals(CHARACTER_NULL)) {
      tokens.push(Operator.ISNULL.toString());
      tokens.push(s);
      return true;
    } else {
      return false;
    }
  }
  
  private boolean acceptEqualStatement(String s, Stack<String> tokens) {
    //{EqualStatement: ({Escaped}|{SimpleSigns})*}
    if (acceptSimpleSignsAndEscapeSequences(s)) {
      tokens.push(Operator.EQUALS.toString());
      tokens.push(s);
      return true;
    } else {
      return false;
    }
  }
  
  
  private boolean acceptSimpleSignsAndEscapeSequences(String s) {
    //split into escaped and unescaped groups
    //match comments unto escapedStuff
    //match the rest onto simpleSigns
    if(s.equals(CHARACTER_NULL)) {
      return false;
    }
    
    List<EscapePart> escapeParts = split(s);
    
    for (EscapePart ep : escapeParts) {
      if (ep.getState() == EscapeState.UNESCAPED) {
        if (!acceptSimpleSigns(ep.getValue()) && !ep.getValue().equals("")) {
          return false;
        }
      } else {
        if (!acceptEscapedSigns(ep.getValue())) {
          return false;
        }
      }
    }
    
    return true;
  }
  
  
  private boolean acceptLikeStatement(String s, Stack<String> tokens) {
    //{LikeStatement: {EqualStatement}{LikeSign}{EqualStatement}}
    //we could really try to ensure the existence of an unescaped like, split there and try to parse the other two strings (as Equal- and/or Like-Statement)
    String escapeLikeInsideEscapeSequences = getCloneWithEscapedStringInsideEscapeSequence(s, "%");
    if (escapeLikeInsideEscapeSequences.contains("%")) {
      String[] unlikeParts = escapeLikeInsideEscapeSequences.split("%");
      for (String unlikePart : unlikeParts) {
        if (!unlikePart.equals("") && !acceptSimpleSignsAndEscapeSequences(unlikePart)) {
          return false;
        }
      }
      tokens.push(Operator.LIKE.toString());
      tokens.push(s);
      return true;
    } else {
      return false;
    }
  }
  
  
  private boolean acceptGreaterStatement(String s, Stack<String> tokens) {
    //'> {SimpleSigns}'}
    if (s.startsWith(CHARACTER_GREATER)) {
      String remains = s.substring(1).trim();
      if (acceptSimpleSigns(remains)) {
        tokens.push(CHARACTER_GREATER);
        tokens.push(remains);
        return true;
      }
    }
    return false;
  }
  
  
  private boolean acceptSmallerStatement(String s, Stack<String> tokens) {
    //'< {SimpleSigns}'}
    if (s.startsWith(CHARACTER_SMALLER)) {
      String remains = s.substring(1).trim();
      if (acceptSimpleSigns(remains)) {
        tokens.push(CHARACTER_SMALLER);
        tokens.push(remains);
        return true;
      }
    }
    return false;
  }
  
  
  private boolean acceptSimpleSigns(String s) {
    if (s.contains(AND_IDENTIFIER) || s.contains(OR_IDENTIFIER)) {
      return false;
    }
    Matcher m = PATTERN_SIMPLE_SIGNS.matcher(s);
    return m.matches();
  }
  
  
  private boolean acceptEscapedSigns(String s) {
    return true; //escapte Zeichen immer erlauben
  }
  
  
  private boolean acceptNegation(String s, Stack<String> tokens) {
    //!
    //accept: !acceptEqualStatement(s) && !acceptLikeStatement(s) && !acceptGreaterOrSmallerStatement(s)
    if (s.startsWith(CHARACTER_NEGATION)) {
      tokens.push("!");
      String remains = s.substring(CHARACTER_NEGATION.length());
      if (acceptSingleStatement(remains, tokens) || acceptBracedStatement(remains, tokens)) {
        return true;
      } else {
        tokens.pop();
        return false;
      }
    } else {
      return false;
    }
    
  }
  
  
  private boolean acceptBracedStatement(String s, Stack<String> tokens) {
    // ( ... )
    // remove braces and try to accept the rest as negation, single, linked or complex
    if (s.startsWith(CHARACTER_BRACE_OPEN) && s.endsWith(CHARACTER_BRACE_CLOSE)) {
      tokens.push("(");
      String remains = s.substring(CHARACTER_BRACE_OPEN.length(), s.length()-CHARACTER_BRACE_CLOSE.length());
      if (acceptLinkedStatements(remains, tokens) || 
          acceptSingleStatement(remains, tokens) ||
          acceptBracedStatement(remains, tokens) ||
          acceptNegation(remains, tokens)) {
        tokens.push(")");
        return true;
      } else {
        tokens.pop();
        return false;
      }
    } else {
      return false;
    }
  }
  
  
 
  
  /*public static void main(String... args) {
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.orderarchive);
    srb.setSelection("status");
    srb.setMaxRows(100);
    Map<String,String> filters = new HashMap<String, String>();
    filters.put("custom1", "!(bla OR blaub OR baum OR blib OR blib2 OR blib3 OR blib4)");
    filters.put("orderType", "!(com.gip.xyna.Cancel OR ManualInteractionRedirection OR com.gip.xyna.ResumeMultipleOrders OR com.gip.xyna.ResumeOrder OR com.gip.xyna.SuspendAllOrders OR com.gip.xyna.SuspendOrder OR xmcp.manualinteraction.ManualInteraction OR com.gip.xyna.SuspendOrdertype)");
    filters.put("custom2", "\"ba%um\" OR \"hasfjkhf(dnsag\"gahsdg%fjkah\"df%sd\"");
    srb.setFilterEntries(filters);
    
    try {
      OrderInstanceSelect ois = (OrderInstanceSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      System.out.println(ois.getSelectString());
      for (int i = 0; i< ois.getParameter().size(); i++) {
        System.out.println(ois.getParameter().get(i));
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }*/
    
  
  //With return[0] = the first statement
  //     return[1] = the copula
  //     return[2] = the remaining statement
  // (baum OR arg) AND argel
  //               ^^^
  // "baum OR arg" AND argel
  //               ^^^
  private static String[] splitStringAroundFirstUnescapedAndUnbracedCopula(String linkedStatement) {
    int indexOfCopula = getIndexOfCopula(linkedStatement, 0);
    // fail fast if there is no copula at all
    if (indexOfCopula < 0) {
      return null;
    }
    
    int indexOfOpeningBrace = getCloneWithEscapedStringInsideEscapeSequence(linkedStatement, "(").indexOf(CHARACTER_BRACE_OPEN);
    int indexOfClosingBrace = getCloneWithEscapedStringInsideEscapeSequence(linkedStatement, ")").indexOf(CHARACTER_BRACE_CLOSE);
    Pair<Integer, Integer> indexOfEscapeSequence = getIndexOfEscapeSequenceCharacter(linkedStatement);
    int indexOfEscapeSequenceStart = indexOfEscapeSequence.getFirst();
    int indexOfEscapeSequenceEnd;
    if (indexOfEscapeSequenceStart < 0) {
      indexOfEscapeSequenceEnd = -1;
    } else {
      indexOfEscapeSequenceEnd = indexOfEscapeSequence.getSecond();
    }
    while (indexOfCopula > -1 &&
           ((indexOfCopula > indexOfOpeningBrace && indexOfCopula < indexOfClosingBrace) ||
           (indexOfCopula > indexOfEscapeSequenceStart && indexOfCopula < indexOfEscapeSequenceEnd))) {
      // get the next one                                             while 3 might not be the whole copula it is enough to not recognize it anymore
      indexOfCopula = getIndexOfCopula(linkedStatement, indexOfCopula+3);
      if (indexOfCopula > indexOfClosingBrace && indexOfClosingBrace > -1) {
        indexOfOpeningBrace = getCloneWithEscapedStringInsideEscapeSequence(linkedStatement, "(").indexOf(CHARACTER_BRACE_OPEN, indexOfOpeningBrace + CHARACTER_BRACE_OPEN.length());
        indexOfClosingBrace = getCloneWithEscapedStringInsideEscapeSequence(linkedStatement, ")").indexOf(CHARACTER_BRACE_CLOSE, indexOfClosingBrace + CHARACTER_BRACE_OPEN.length());
      }
      if (indexOfCopula > indexOfEscapeSequenceEnd && indexOfEscapeSequenceEnd > -1) {
        indexOfEscapeSequenceStart = indexOfEscapeSequence.getFirst();
        if (indexOfEscapeSequenceStart < 0) {
          indexOfEscapeSequenceEnd = -1;
        } else {
          indexOfEscapeSequenceEnd = indexOfEscapeSequence.getSecond();
        }
      }
    }
    if (indexOfCopula < 0) {
      return null;
    }
    String[] result = new String[3];
    result[0] = linkedStatement.substring(0, indexOfCopula);
    int indexAfterCopula;
    if (linkedStatement.substring(indexOfCopula).startsWith(OR_IDENTIFIER)) {
      result[1] = OR_IDENTIFIER;
      indexAfterCopula = indexOfCopula + OR_IDENTIFIER.length();
    } else {
      result[1] = AND_IDENTIFIER;
      indexAfterCopula = indexOfCopula + AND_IDENTIFIER.length();
    }
    result[2] = linkedStatement.substring(indexAfterCopula);
    return result;
  }
  
  
  private static int getIndexOfCopula(String linkedStatement, int offset) {
    int indexOfAnd = linkedStatement.indexOf(AND_IDENTIFIER, offset);
    int indexOfOr = linkedStatement.indexOf(OR_IDENTIFIER, offset);
    if (indexOfAnd < 0) {
      return indexOfOr;
    }
    if (indexOfOr < 0) {
      return indexOfAnd;
    }
    return Math.min(indexOfAnd, indexOfOr);
  }
  
  
  private static String getCloneWithEscapedStringInsideEscapeSequence(String statement, String stringToEscape) {
    StringBuilder escapeSequence = new StringBuilder();
    for (int i = 0; i<stringToEscape.length(); i++) {
      escapeSequence.append("a");
    }    
    
    List<EscapePart> escapeParts = split(statement);
    
    StringBuilder sb = new StringBuilder();
    for (EscapePart ep : escapeParts) {
      if (ep.getState() == EscapeState.ESCAPED) {
        sb.append(ep.getValue().replace(stringToEscape, escapeSequence.toString()));
      } else {
        sb.append(ep.getValue());
      }
    }
    
    return sb.toString();
  }

  
  
  public static enum Operator {

    EQUALS("EQUALS"), LIKE("LIKE"), GREATER(">"), SMALLER("<"), ISNULL("ISNULL");

    private String operatorString;


    private Operator(String operatorString) {
      this.operatorString = operatorString;
    }

    public static Operator fromValue(String operatorString) {
      for (Operator o: values()) {
        if (o.toString().equals(operatorString)) {
          return o;
        }
      }
      throw new IllegalArgumentException("Unknown operator: '" + operatorString + "'");
    }

    @Override
    public String toString() {
      return operatorString;
    }
  }
  

  private int findIndexOfConclusiveBrace(int j, List<String> tokens) {
    int openBraceCounter = 0;
    while (!tokens.get(j).equals(")") || openBraceCounter != 0) {
      if(tokens.get(j).equals("(")) {
        openBraceCounter++;
      } else if(tokens.get(j).equals(")")) {
        openBraceCounter--;
      }
      j++;
    }
    return j;
  }
  
  
  protected WhereClausesConnection<P> parseTokens(List<String> tokens, String column, P whereClause) throws XNWH_WhereClauseBuildException {
    int eleCount = tokens.size();    
    if (eleCount == 0) {
      return null;
    }

    String actEle = tokens.get(0);
    if (actEle.equals("!")) {
      if (tokens.get(1).equals("(")) { //it's a negative brace
        // jump to end of brace & parse next connection
        int j = 2;
        j = findIndexOfConclusiveBrace(j, tokens);
        j++;
        // we now have isolated the negative brace
        WhereClausesConnection<P> connection = whereClause.whereNot(parseTokens(tokens.subList(2, j-1), column, newWhereClauseContainer()));
        // is anything left?
        if (j+1 >= eleCount) {
          return connection;
        }
        P newWhereClause = parseCopula(tokens.get(j), connection);
        return parseTokens(tokens.subList(j+1, eleCount), column, newWhereClause);        
      } else { //it's a single negative statement       
        WhereClause<P> where = retrieveColumnWhereClause(column, newWhereClauseContainer());
        WhereClausesConnection<P> connection =  whereClause.whereNot(parseComparison(tokens.get(1), tokens.get(2), where));
        if (eleCount > 3) {
          actEle = tokens.get(3);
          P newWhereClause = parseCopula(tokens.get(3), connection);
          //continue if anything is left
          return parseTokens(tokens.subList(4, eleCount), column, newWhereClause);
          
        } else { //we are done
          return connection;
        }
      }
    } else if (actEle.equals("(")) {     // it's not negative, it's either a brace or a single Statement
      int j = 1;
      j = findIndexOfConclusiveBrace(j, tokens);
      j++;
      WhereClausesConnection<P> connection = whereClause.where(parseTokens(tokens.subList(1, j-1), column, newWhereClauseContainer()));
      //parse the rest after the brace if anything is left
      if (j+1 >= eleCount) {
        return connection;
      }
      P newWhereClause = parseCopula(tokens.get(j), connection);
      return parseTokens(tokens.subList(j+1, eleCount), column, newWhereClause);
    } else {
      WhereClause<P> where = retrieveColumnWhereClause(column, whereClause);
      WhereClausesConnection<P> connection = parseComparison(tokens.get(0), tokens.get(1), where);
      if (eleCount > 2) {
        P newWhereClause = parseCopula(tokens.get(2), connection);
        return parseTokens(tokens.subList(3, eleCount), column, newWhereClause);
      } else { //we are done
        return connection;
      }
    }
  }
  
  private P parseCopula(String copula, WhereClausesConnection<P> connection) {
    if (copula.equals(Copula.AND.toString())) {
      return connection.and();
    } else {
      return connection.or();
    }
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  private WhereClausesConnection<P> parseComparison(String operatorString, String value, WhereClause<P> where)
                  throws XNWH_WhereClauseBuildException {

    Operator operator = Operator.fromValue(operatorString);
    if (where == null ) {
      throw new IllegalArgumentException("WhereClause must not be null");
    } else if (where instanceof WhereClauseNumber) {
      if (value.startsWith("\"") && value.endsWith("\"")) {
        value = value.substring(1, value.length() - 1);
      }
      switch (operator) {
        case EQUALS :
          return ((WhereClauseNumber<P>) where).isEqual(Long.parseLong(value));
        case LIKE :
          return ((WhereClauseNumber<P>) where).isLike(value);
        case GREATER :
          return ((WhereClauseNumber<P>) where).isBiggerThan(Long.parseLong(value));
        case SMALLER :
          return ((WhereClauseNumber<P>) where).isSmallerThan(Long.parseLong(value));
        default :
          throw new RuntimeException("Unsupported operator: " + operator.toString());
      }
    } else if (where instanceof WhereClauseString) { //can only be a WhereClauseString then
      switch (operator) {
        case EQUALS :
          return ((WhereClauseString<P>) where).isEqual(value);
        case LIKE :
          return ((WhereClauseString<P>) where).isLike(value);
        case GREATER :
          return ((WhereClauseString<P>) where).isBiggerThan(value);
        case SMALLER :
          return ((WhereClauseString<P>) where).isSmallerThan(value);
        case ISNULL:
          return ((WhereClauseString<P>) where).isNull();
        default :
          throw new RuntimeException("Unsupported operator: " + operator.toString());
      }
    } else if (where instanceof WhereClauseStringTransformation) {
      switch (operator) {
        case EQUALS :
          return ((WhereClauseStringTransformation<P>) where).isEqual(value);
        case ISNULL:
          return ((WhereClauseStringTransformation<P>) where).isNull();
        default :
          throw new RuntimeException("Unsupported operator: " + operator.toString());
      }
    } else if (where instanceof WhereClauseBoolean) {
      switch (operator) {
        case EQUALS :
          return ((WhereClauseBoolean<P>) where).isEqual(Boolean.valueOf(value));
        default :
          throw new RuntimeException("Unsupported operator for column of type <boolean>");
      }
    } else if (where instanceof WhereClauseStringSerializable) {
      switch (operator) {
        case EQUALS :
          return ((WhereClauseStringSerializable) where).isEqual(value);
        default :
          throw new RuntimeException("Unsupported operator for column of type <StringSerializable>");
      }
    } else if (where instanceof WhereClauseEnum) {
      switch (operator) {
        case EQUALS :
          return ((WhereClauseEnum) where).isEqual(value);
        default :
          throw new RuntimeException("Unsupported operator for column of type <Enum>");
      }
    } else {
      throw new RuntimeException("Unsupported WhereClause of type " + where.getClass() );
    }
  }
  
  
  public WhereClausesConnection<P> parseFilter(Map<String, String> filters) throws XNWH_WhereClauseBuildException {
    return parseFilterInternally(filters);
  }
  
  
  public void parseSelection(String selection) throws XNWH_NoSelectGivenException {
    if( selection == null || selection.length() == 0 ) {
      throw new XNWH_NoSelectGivenException();
    }
    String[] selections = selection.split(",");
    for (String select : selections) {
      parseSelectInternally(select.trim());
    }
  }
  
  
  private final static EnumMap<ArchiveIdentifier,Class<? extends SelectionParser<?>>> SELECT_PARSER;
  static {
    SELECT_PARSER = new EnumMap<ArchiveIdentifier,Class<? extends SelectionParser<?>>>(ArchiveIdentifier.class);
    put( SELECT_PARSER, ArchiveIdentifier.orderarchive, OrderInstanceSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.userarchive, UserSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.miarchive, ManualInteractionSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.fqctrltaskinformation, FrequencyControlledTaskSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.xmomcache, XMOMCacheSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.vetos, VetoSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.cronlikeorders, CronLikeOrderSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.batchprocess, BatchProcessSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.datamodel, DataModelSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.deploymentitem, DeploymentItemSelectParser.class );
    put( SELECT_PARSER, ArchiveIdentifier.orderInputSource, OrderInputSourceSelectParser.class );
  }
  private static <T extends SelectionParser<?>> void put( EnumMap<ArchiveIdentifier, Class<? extends SelectionParser<?>>> selectParser,
                                                          ArchiveIdentifier archiveIdentifier, Class<T> clazz) {
    selectParser.put(archiveIdentifier, clazz);
  }

  public final static Selection generateSelectObjectFromSearchRequestBean(SearchRequestBean srb) throws XNWH_SelectParserException, XNWH_NoSelectGivenException, XNWH_WhereClauseBuildException {
    
    Class<? extends SelectionParser<?>> cl = SELECT_PARSER.get(srb.getArchiveIdentifier());
  
    if( cl == null ) {
      throw new IllegalArgumentException("Invalid archive specified");
    }
    try {
      SelectionParser<?> selectParser = cl.getConstructor().newInstance();
      selectParser.parseSelection(srb.getSelection());
      selectParser.parseFilter(srb.getFilterEntries());
      updateWhereClauses(srb, selectParser);
      return selectParser.getSelectImpl();
    } catch (XNWH_NoSelectGivenException e) {
      throw e;
    } catch (XNWH_WhereClauseBuildException e) {
      throw e;
    } catch( Exception e ) {
      throw new XNWH_SelectParserException(srb.getArchiveIdentifier().toString(), e); 
    } 
  }


  //sets where clauses,
  //if where clause is equals, then remove escape characters (unless the are escaped)
  private static <T extends WhereClausesContainerBase<T>> void updateWhereClauses(SearchRequestBean srb, SelectionParser<T> selectParser)
      throws XNWH_WhereClauseBuildException {

    Set<WhereClausesConnection<T>> wcc = selectParser.getWhereClausesConnections();

    if (wcc == null) {
      return; //nothing to do
    }

    for (WhereClausesConnection<T> swcc : wcc) {
      if (swcc == null || swcc.getConnectedObject() == null) {
        continue;
      }
      WhereClause<?> w = swcc.getConnectedObject();
      if (w instanceof WhereClauseString) {
        WhereClauseString<?> wcs = (WhereClauseString<?>) w;
        if (wcs.getOperator() == WhereClauseString.Operator.EQUALS) {
          wcs.setParameterValue(removeRegionEscapeFromString(wcs.getParameterValue()));
        }
      }
    }
  }


  public static void updateWhereClause(WhereClausesConnection<?> wcc) {
    if (wcc == null) {
      return;
    }

    WhereClause<?> w = wcc.getConnectedObject();
    if (w instanceof WhereClauseString) {
      WhereClauseString<?> wcs = (WhereClauseString<?>) w;
      if (wcs.getOperator() == WhereClauseString.Operator.EQUALS) {
        wcs.setParameterValue(removeRegionEscapeFromString(wcs.getParameterValue()));
      }
    }
  }


  private static String removeRegionEscapeFromString(String s) {
    List<EscapePart> escapeParts = new ArrayList<EscapePart>();
    try {
      escapeParts = split(s);
    } catch (IllegalArgumentException ex) {
      return s;
    }

    Stream<EscapePart> stream = escapeParts.stream();
    stream = stream.filter(x -> x.getState() != EscapeState.ESCAPE_CHARACTER); //remove not escaped escape characters
    String result = String.join("", stream.map(x -> x.getValue()).collect(Collectors.toList()));
    return result;
  }


  public abstract Selection getSelectImpl();
  

  public static interface EscapeParams {

    /**
     * Escaped den String f�r Like-Anfragen.
     * @param s
     * @return
     */
    public String escapeForLike(String s);
    
    /**
     * Liefert die spezifische Wildcard.
     * @return
     */
    public String getWildcard();
  
  }
  
  private enum EscapeState {
    ESCAPED, UNESCAPED, ESCAPE_CHARACTER;
  }
  
  private static class EscapePart {
    private final String value;
    private final EscapeState state;
    
    public EscapePart(String value, EscapeState state) {
      this.value = value;
      this.state = state;
    }
    
    public EscapeState getState() {
      return state;
    }
    
    public String getValue() {
      return value;
    }
  }
  
  /**
   * Wird von den PersistenceLayern verwendet. F�r Like-Anfragen werden (unescapte) % durch eigene
   * Wildcards ersetzt. Alle anderen Zeichen werden PersistenceLayer-spezifisch escaped. 
   * Equals-Anfragen werden nicht angepasst.
   * 
   * Verwandelt den param-String (angegeben in persistencelayer-neutralem Format) in das spezifische Format, was 
   * der PersistenceLayer ben�tigt.
   * 
   * @param param
   * @param isLike
   * @param e
   * @return
   */
  public static String escapeParams(String param, boolean isLike, EscapeParams e) {
    //return input for equals
    if (!isLike) {
      return param;
    }

    StringBuilder sb = new StringBuilder();
    StringBuilder part = new StringBuilder();
    
    List<EscapePart> escapeParts = new ArrayList<EscapePart>();
    try {
      escapeParts = split(param);
    } catch (IllegalArgumentException ex) {
      //falls der Parameter nicht aufgeteilt werden kann, dann ohne Anpassungen �bernehmen
      escapeParts.add(new EscapePart(param, EscapeState.ESCAPED));
    }
    
    for (EscapePart ep : escapeParts) {
      if (ep.getState() == EscapeState.ESCAPED) {
        part.append(ep.getValue()); //escapte Anteile einfach �bernehmen
      }
      
      if (ep.getState() == EscapeState.UNESCAPED) {
        //in den unescapten Anteilen bei Like die % durch PersistenceLayer-spezifische Wildcard ersetzen
        if (isLike) {
          List<String> parts = SPLITTER_WILDCARD.split(ep.getValue(), true);
          for (String p : parts) {
            if (SPLITTER_WILDCARD.isSeparator(p)) {
              sb.append(e.escapeForLike(part.toString()));
              sb.append(e.getWildcard());
              part.setLength(0);
            } else {
              part.append(p);
            }
          }
        } else {
          part.append(ep.getValue());
        }
      }
    }
    
    if (isLike) {
      sb.append(e.escapeForLike(part.toString()));
    } else {
      sb.append(part);
    }
    
    return sb.toString();
  }


  /**
   * falls string null, wird null zur�ckgegeben
   * falls string als literal escaped, wird inhalt zur�ckgegeben
   * ansonsten wird der string direkt wieder zur�ckgegeben
   */
  public static String getLiteral(String val) {
    if (val == null) {
      return null;
    }
    if (val.startsWith(CHARACTER_ESCAPE_SEQUENCE_STRING) && val.endsWith(CHARACTER_ESCAPE_SEQUENCE_STRING)) {
      val =
          val.substring(1, val.length() - 1).replaceAll(Pattern.quote(CHARACTER_ESCAPE + CHARACTER_ESCAPE_SEQUENCE_STRING),
                                                        Matcher.quoteReplacement(CHARACTER_ESCAPE_SEQUENCE_STRING));
    }
    return val;
  }

  /**
   * set after parseFilter() call
   */
  protected abstract Set<WhereClausesConnection<P>> getWhereClausesConnections();
  
  /*public static void main(String... args) {
    OrderInstanceSelectParser oisp = new OrderInstanceSelectParser();
    //System.out.println(oisp.convertIntoTokens("jdshak\\fhs%das OR ���ggfhds"));
    System.out.println(oisp.convertIntoTokens("a OR ass\"%b%\"sasa OR c AND !(aa OR bb)"));
  }*/
  
}
