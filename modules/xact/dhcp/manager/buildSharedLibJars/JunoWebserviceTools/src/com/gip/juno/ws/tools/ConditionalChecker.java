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

package com.gip.juno.ws.tools;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.exceptions.DPPWebserviceIncorrectConditionalSyntaxException;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;
import com.gip.juno.ws.exceptions.MessageBuilder;

enum ElementTag { condition, dualOperation, parenthesisOpen, parenthesisClosed, notOperation, unknown, end }

class ConditionElement {
  private final ElementTag _tag = ElementTag.unknown;
  
  /**
   * Set of tags of ConditionElements before which this instance is allowed in a condition 
   */
  protected HashSet<ElementTag> _allowedBefore; 
  public ConditionElement() {
    _allowedBefore = init();
  }
  private HashSet<ElementTag> init() {
    HashSet<ElementTag> ret = new HashSet<ElementTag>();
    return ret;
  }  
  /**
   * returns true if parameter tag is contained in the Set _allowedBefore,
   * that means if an ConditionElement with the parameter tag is allowed to be placed in a condition
   * AFTER this instance
   */
  public boolean isAllowedBefore(ElementTag tag) {
    if (_allowedBefore.contains(tag)) {
      return true;
    }
    return false;
  }
  public ElementTag getTag() {
    return _tag;
  }
  public String printAllowedBefore() {
    StringBuilder ret = new StringBuilder(" (allowed before elements: ");
    for (ElementTag tag : _allowedBefore) {
      ret.append(tag);
      ret.append("  ");
    }
    ret.append(")");
    return ret.toString();
  }
  public String print() { return " ConditionElement "; }
  public String toString() {return " ConditionElement "; }
}


class ConditionRefNum extends ConditionElement {
  private final ElementTag _tag = ElementTag.condition;  
  //private ConditionRefNum() {}
  public ElementTag getTag() {
    return _tag;
  }
  public int number;
  public ConditionRefNum(int num) {
    _allowedBefore = init();
    number = num;
  }
  private HashSet<ElementTag> init() {
    HashSet<ElementTag> ret = new HashSet<ElementTag>();
    ret.add(ElementTag.end);
    ret.add(ElementTag.parenthesisClosed);
    ret.add(ElementTag.dualOperation);
    return ret;
  }
  public String print() {
    return "<" + number + ">";
  }
  public String toString() {
    return "ConditionRefNum (" + number + ")" + printAllowedBefore();
  }
}


class DualOperation extends ConditionElement {
  private final ElementTag _tag = ElementTag.dualOperation;
  public DualOperation() {
    _allowedBefore = init();
  }
  public ElementTag getTag() {
    return _tag;
  }
  protected HashSet<ElementTag> init() {
    HashSet<ElementTag> ret = new HashSet<ElementTag>();
    ret.add(ElementTag.parenthesisOpen);
    ret.add(ElementTag.notOperation);
    ret.add(ElementTag.condition);
    return ret;
  }
  public String print() { return " DualOperation "; }
  public String toString() {
    return "DualOperation" + printAllowedBefore();
  }
}


class AndOperation extends DualOperation {
  public AndOperation() {
    super();
  }
  public String print() { return " AND "; }
  public String toString() {
    return "AndOperation" + printAllowedBefore();
  }
}


class OrOperation extends DualOperation {
  public OrOperation() {
    super();
  }
  public String print() { return " OR "; }
  public String toString() {
    return "OrOperation" + printAllowedBefore();
  }
}


class NotOperation extends ConditionElement {  
  private final ElementTag _tag = ElementTag.notOperation;
  public NotOperation() {
    _allowedBefore = init();
  }
  private HashSet<ElementTag> init() {
    HashSet<ElementTag> ret = new HashSet<ElementTag>();
    ret.add(ElementTag.parenthesisOpen);
    return ret;
  }
  public ElementTag getTag() {
    return _tag;
  }
  public String print() { return "NOT "; }
  public String toString() {
    return "NotOperation" + printAllowedBefore();
  }
} 


class ParenthesisOpen extends ConditionElement {
  private final ElementTag _tag = ElementTag.parenthesisOpen;
  public ParenthesisOpen() {
    _allowedBefore = init();
  }
  private HashSet<ElementTag> init() {
    HashSet<ElementTag> ret = new HashSet<ElementTag>();
    ret.add(ElementTag.parenthesisOpen);
    ret.add(ElementTag.notOperation);
    ret.add(ElementTag.condition);
    return ret;
  }
  public ElementTag getTag() {
    return _tag;
  }
  public String print() { return "( "; }
  public String toString() {
    return "ParenthesisOpen" + printAllowedBefore();
  }
}


class ParenthesisClosed extends ConditionElement {
  private final ElementTag _tag = ElementTag.parenthesisClosed;
  public ParenthesisClosed() {
    _allowedBefore = init();
  }
  private HashSet<ElementTag> init() {
    HashSet<ElementTag> ret = new HashSet<ElementTag>();
    ret.add(ElementTag.end);
    ret.add(ElementTag.parenthesisClosed);
    ret.add(ElementTag.dualOperation);
    return ret;
  }
  public ElementTag getTag() {
    return _tag;
  }
  public String print() { return " )"; }
  public String toString() {
    return "ParenthesisClosed" + printAllowedBefore();
  }
}


/**
 * represents condition in database column conditional
 */
class Condition {
  public List<ConditionElement> _elements;
  
  public Condition(String conditionStr, Logger logger, String schemaForConditionLookup) throws RemoteException {
    _elements = parse(conditionStr, logger, schemaForConditionLookup);
  }
  
  public static List<ConditionElement> parse(String conditionStr, Logger logger, String schemaForConditionLookup) throws RemoteException {
    List<ConditionElement> ret = new ArrayList<ConditionElement>();
    StringBuilder builder = new StringBuilder("");
    boolean inNumber = false;
    for (int i=0; i< conditionStr.length(); i++) {
      String nextChar = conditionStr.substring(i, i+1);
      if (inNumber) {
        inNumber = parseInNumber(builder, nextChar, ret, logger, schemaForConditionLookup);
      } else {
        inNumber = parseOutsideNumber(builder, nextChar, ret, logger);
      }
    }
    return ret;
  }
  
  private static boolean parseInNumber(StringBuilder builder, String nextChar, List<ConditionElement> elements,
          Logger logger, String schemaForConditionLookup) throws RemoteException {
    if (nextChar.equals("<")) {
      throw new DPPWebserviceIncorrectConditionalSyntaxException(
          "ConditionalChecker: Expression has incorrect syntax.");          
    } else if (nextChar.equals(">")) {
      if (builder.toString().trim().equals("")) {
        throw new DPPWebserviceIncorrectConditionalSyntaxException(
            "ConditionalChecker: Expression has incorrect syntax.");
      }
      String numStr = builder.toString().trim();
      int num = Integer.parseInt(numStr);
      
      if (!QueryTools.queryConditionIDExists(numStr, logger, schemaForConditionLookup)) {
        logger.info("ConditionalChecker: Expression contains ConditionID which does not exist.");
        throw new DPPWebserviceIncorrectConditionalSyntaxException(
            "ConditionalChecker: Expression contains ConditionID which does not exist.");
      }
      
      ConditionRefNum element = new ConditionRefNum(num);
      elements.add(element);
      builder.setLength(0);
      return false;
    } 
    builder.append(nextChar);
    return true;
  }
  
  private static boolean parseOutsideNumber(StringBuilder builder, String nextChar, 
          List<ConditionElement> elements, Logger logger) throws DPPWebserviceIncorrectConditionalSyntaxException {
    if (nextChar.equals("<")) {
      if (!builder.toString().trim().equals("")) {
        throw new DPPWebserviceIncorrectConditionalSyntaxException("Conditional has incorrect syntax.");
      }
      builder.setLength(0);
      return true;
    } else if (nextChar.equals(">")) {
      throw new DPPWebserviceIncorrectConditionalSyntaxException("Conditional has incorrect syntax.");
    } 
    builder.append(nextChar);
    boolean elementComplete = false;
    String val = builder.toString().trim();
    if (val.equals("NOT")) {
      elementComplete = true;
      elements.add(new NotOperation());
    } else if (val.equals("AND")) {
      elementComplete = true;
      elements.add(new AndOperation());
    } else if (val.equals("OR")) {
      elementComplete = true;
      elements.add(new OrOperation());
    } else if (val.equals("(")) {
      elementComplete = true;
      elements.add(new ParenthesisOpen());
    } else if (val.equals(")")) {
      elementComplete = true;
      elements.add(new ParenthesisClosed());
    } else {
      if (val.length() > 3) {        
        throw new DPPWebserviceIncorrectConditionalSyntaxException(
            "Conditional has incorrect syntax. Current parsed element = " + val);
      }
    }
    if (elementComplete) {
      builder.setLength(0); 
    }
    return false;        
  }
  
  
  /**
   * Checks the syntax of the condition
   * 
   */
  public static boolean checkSyntax(List<ConditionElement> elements, Logger logger) {
    if (elements == null) {
      return false;
    }
    if (elements.size() == 0) {
      return false;
    }
    boolean ret = true;
    int numOpen = 0;
    int numClosed = 0;
    boolean startok = false;
    if (elements.get(0).getTag() == ElementTag.parenthesisOpen) {
      startok = true;
    } else if (elements.get(0).getTag() == ElementTag.notOperation) {
      startok = true;
    } else if (elements.get(0).getTag() == ElementTag.condition) {
      startok = true;
    }
    if (!startok) {
      logger.info("Conditional: Check Syntax not correct: Incorrect first element");
      return false;
    }
    boolean hadCondition = false;
    for (int i=0; i < elements.size(); i++) {
      ConditionElement elem = elements.get(i);
      if (elem instanceof ConditionRefNum) {
        hadCondition = true;
      } else if (elem instanceof ParenthesisOpen) {
        numOpen++;
      } else if (elem instanceof ParenthesisClosed) {
        numClosed++;
      }
      ElementTag nextTag = getTagNextElement(elements, i);
      if (!elem.isAllowedBefore(nextTag)) {
        logger.info("Conditional: Check Syntax not correct: Incorrect element with tag " + nextTag 
            + " after element " + elem.getTag());
        return false;
      }
    }
    if (numOpen != numClosed) {
      logger.info("Conditional: Check Syntax not correct: number of open and closed parentheses not equal.");
      return false;
    }
    if (!hadCondition) {
      logger.info("Conditional: Check Syntax not correct: No condition reference number in expression.");
      return false;
    }
    return ret;
  }
  
  private static ElementTag getTagNextElement(List<ConditionElement> elements, int posBeforeNext) {
    if (posBeforeNext < 0) {
      return ElementTag.unknown;
    }
    if (posBeforeNext == elements.size() -1) {
      return ElementTag.end;
    }
    return elements.get(posBeforeNext + 1).getTag();
  }
  
  public boolean isValid(Logger logger) {
    return checkSyntax(_elements, logger);
  }
  
  public String toString() {
    StringBuilder ret = new StringBuilder(""); 
    for (ConditionElement elem : _elements) {
      ret.append(elem.toString());
      ret.append("  ");
    }
    return ret.toString();
  }
  
  public String print() {
    StringBuilder ret = new StringBuilder("");  
    for (ConditionElement elem : _elements) {
      ret.append(elem.print());
    }    
    return ret.toString();
  }
}


/**
 * performs syntax check and adjustment for condition in database column conditional
 */
public class ConditionalChecker {
  
  public static String DEFAULT_SCHEMA_FOR_CONDITION_LOOKUP = "dhcp";
  
  public static String adjust(String conditionStr, Logger logger, String schemaForConditionLookup) throws RemoteException {
    try {
      logger.info("ConditionalChecker: Checking syntax of expression: " + conditionStr);
      Condition condition = new Condition(conditionStr, logger, schemaForConditionLookup);
      logger.info("ConditionalChecker: Parsed conditional: " + condition.toString());
      if (!condition.isValid(logger)) {
        logger.error("ConditionalChecker: incorrect syntax in expression: " + conditionStr);
        throw new DPPWebserviceIncorrectConditionalSyntaxException(new MessageBuilder().setDescription(
            "Conditional has incorrect syntax.").addParameter(conditionStr).setErrorNumber("00202"));
      }
      String ret = condition.print();
      logger.info("ConditionalChecker: Syntax check OK. Returned expression = " + ret);
      return ret;
    } catch (RemoteException e) {
      throw e;
    } catch (Exception e) {
      throw new DPPWebserviceUnexpectedException(e);
    }
  }
  
  public static String adjust(String conditionStr, Logger logger) throws RemoteException {
    return adjust(conditionStr, logger, DEFAULT_SCHEMA_FOR_CONDITION_LOOKUP);
  }
    
}
