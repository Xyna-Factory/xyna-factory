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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.util.Stack;


/**
 * Extends the Parser by extending several methods with token generation and their management on a stack  
 */
public class TimeControlRuleTokenizer<T extends TimeControlUnit> extends TimeControlRuleParser<T> {

  protected final Stack<Token> tokenStack;
  
  public TimeControlRuleTokenizer(Class<T> backingEnum) {
    super(backingEnum);
    tokenStack = new Stack<Token>();
  }
  
  
  @SuppressWarnings("unchecked")
  protected boolean acceptTimeControlUnit(TimeControlUnit unit, String condition) {
    tokenStack.clear();
    tokenStack.push(new TimeUnitIdentifierToken((T) unit));
    boolean superResult = super.acceptTimeControlUnit(unit, condition);
    return superResult;
  }
  
  
  @Override
  protected boolean acceptWeekDayValue(TimeControlUnit unit, String condition) {
    tokenStack.push(TokenType.DAY_IN_WEEK);
    boolean superResult = super.acceptWeekDayValue(unit, condition);
    if (!superResult) {
      while (tokenStack.pop() != TokenType.DAY_IN_WEEK) { }
    }
    return superResult;
  }
  
  
  protected boolean acceptValueRange(TimeControlUnit unit, String condition) {
    tokenStack.push(TokenType.VALUE_RANGE);
    boolean superResult = super.acceptValueRange(unit, condition);
    if (!superResult) {
      while (tokenStack.pop() != TokenType.VALUE_RANGE) { }
    }
    // expected stack if true <VALUE_RANGE, SimpleValue, SimpleValue>
    return superResult;
  }
  
  
  protected boolean acceptValueList(TimeControlUnit unit, String condition) {
    tokenStack.push(TokenType.LIST_START);
    boolean superResult = super.acceptValueList(unit, condition);
    if (!superResult) {
      while (tokenStack.pop() != TokenType.LIST_START) { }
    } else {
      tokenStack.push(TokenType.LIST_END);
    }
    // expected stack if true <LIST_START, (SimpleValue | VALUE_RANGE, SimpleValue, SimpleValue), LIST_END>
    return superResult;
  }
  
  
  protected boolean acceptValueWithIncrement(TimeControlUnit unit, String condition) {
    tokenStack.push(TokenType.INCREMENT_START);
    boolean superResult = super.acceptValueWithIncrement(unit, condition);
    if (!superResult) {
      while (tokenStack.pop() != TokenType.INCREMENT_START) { }
    } else {
      tokenStack.push(TokenType.INCREMENT_END);
    }
    // expected stack if true <INCREMENT_START, (SimpleValue,)? SimpleNumber, INCREMENT_END>
    return superResult;
  }
  
  
  protected boolean acceptXthWeekDayInMonth(TimeControlUnit unit, String condition) {
    boolean superResult = super.acceptXthWeekDayInMonth(unit, condition);
    // expected stack if true <(NEGATION,)? Xth_DAY_OF_WEEK_IN_MONTH, SimpleNumber, SimpleValue>
    return superResult;
  }
  
  protected boolean acceptNegativeXthWeekDayInMonth(TimeControlUnit unit, String condition) {
    tokenStack.push(TokenType.NEGATION);
    boolean superResult = super.acceptNegativeXthWeekDayInMonth(unit, condition);
    if (!superResult) {
      while (tokenStack.pop() != TokenType.NEGATION) { }
    }
    // expected stack if true <NEGATION, Xth_DAY_OF_WEEK_IN_MONTH, SimpleNumber, SimpleValue>
    return superResult;
  }
  
  
  protected boolean acceptPositiveXthWeekDayInMonth(TimeControlUnit unit, String condition) {
    tokenStack.push(TokenType.Xth_DAY_OF_WEEK_IN_MONTH);
    boolean superResult = super.acceptPositiveXthWeekDayInMonth(unit, condition);
    if (!superResult) {
      while (tokenStack.pop() != TokenType.Xth_DAY_OF_WEEK_IN_MONTH) { }
    }
    // expected stack if true <Xth_DAY_OF_WEEK_IN_MONTH, SimpleNumber, SimpleValue>
    return superResult;
  }
  
  
  protected boolean acceptNegativValue(TimeControlUnit unit, String condition) {
    tokenStack.push(TokenType.NEGATION);
    boolean superResult = super.acceptNegativValue(unit, condition);
    if (!superResult) {
      while (tokenStack.pop() != TokenType.NEGATION) { }
    }
    return superResult;
  }
  
  
  
  protected boolean acceptStringRepresentation(TimeControlUnit unit, String condition) {
    boolean superResult = super.acceptStringRepresentation(unit, condition);
    if (superResult) {
      tokenStack.push(new StringValueToken(unit, condition));
    }
    return superResult;
  }
  
  
  protected boolean acceptSimpleNumber(String expression, int min, int max) {
    boolean superResult = super.acceptSimpleNumber(expression, min, max);
    if (superResult) {
      tokenStack.push(new NumericValueToken(expression));
    }
    return superResult;
  }
  
  
  
  public Stack<Token> getTokenStack() {
    return tokenStack;
  }
  
  
  
  public static interface Token {
    
    TokenType getType();
    
  }
  
  
  public static enum TokenType implements Token {
    TIME_UNIT_IDENTIFIER, VALUE, VALUE_RANGE,
    INCREMENT_START, INCREMENT_END,
    LIST_START, LIST_END,
    NEGATION, Xth_DAY_OF_WEEK_IN_MONTH, DAY_IN_WEEK;

    public TokenType getType() {
      return this;
    }
    
  }
  
  
  public static abstract class ValueToken implements Token {
    
    private final int value;
    
    protected ValueToken(int value) {
      this.value = value;
    }
    
    public TokenType getType() {
      return TokenType.VALUE;
    }
    
    public int getValue() {
      return value;
    }
    
    @Override
    public String toString() {
      return getType().toString() + "(" + String.valueOf(value) + ")";
    }
    
  }
  
  
  public static class StringValueToken extends ValueToken {

    protected StringValueToken(TimeControlUnit unit, String value) {
      super(getTimeControlEnumByStringRepresentation(value, unit.getBackingEnumClass()).getNumericRepresentation());
    }
    
  }
  
  
  public static class NumericValueToken extends ValueToken {

    protected NumericValueToken(String value) {
      super(Integer.parseInt(value));
    }
    
    protected NumericValueToken(int value) {
      super(value);
    }
    
  }
  
  
  public class TimeUnitIdentifierToken implements Token {
    
    T timeControlUnit;
    
    private TimeUnitIdentifierToken(T timeControlUnit) {
      this.timeControlUnit = timeControlUnit;
    }

    public TokenType getType() {
      return TokenType.TIME_UNIT_IDENTIFIER;
    }
    
    public T getTimeControlUnit() {
      return timeControlUnit;
    }
    
    @Override
    public String toString() {
      return getType().toString() + "(" + timeControlUnit.toString() + ")";
    }
    
  }
  

}
