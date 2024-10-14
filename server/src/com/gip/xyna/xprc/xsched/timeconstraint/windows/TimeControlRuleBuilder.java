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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.gip.xyna.xprc.xsched.timeconstraint.windows.RestrictionBasedTimeWindow.RestrictionBasedTimeWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeControlPart.PeriodicTimeControlPart;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeControlPart.StaticTimeControlPart;


/**
 * Extends the Tokenizer by catching token stacks and converting them to their TimeControlParts  
 */
public class TimeControlRuleBuilder extends TimeControlRuleTokenizer<JodaTimeControlUnit> {

  Map<JodaTimeControlUnit, TimeControlPart> build;
  
  public static boolean CHECK_DST = true; //für Tests kann hiermit die Überprüfung, ob DST erlaubt ist, ausgeschaltet werden
  
  public TimeControlRuleBuilder() {
    super(JodaTimeControlUnit.class);
    build = new EnumMap<JodaTimeControlUnit, TimeControlPart>(JodaTimeControlUnit.class);
  }
  
  
  public Map<JodaTimeControlUnit, TimeControlPart> getBuild() {
    return build;
  }
  
  
  @Override
  protected boolean acceptRestrictions(String restrictions) {
    boolean superResult = super.acceptRestrictions(restrictions);
    TimeControlPart.DayOfWeekControlPart dayOfWeekPart = (TimeControlPart.DayOfWeekControlPart) build.remove(JodaTimeControlUnit.DAY_OF_WEEK);
    if (dayOfWeekPart != null) {
      dayOfWeekPart.setInnerDayPart(build.get(JodaTimeControlUnit.DAY));
      build.put(JodaTimeControlUnit.DAY, dayOfWeekPart);
    }
    return superResult;
  }
  
  
  protected boolean acceptTimeControlUnit(TimeControlUnit unit, String condition) {
    boolean superResult = super.acceptTimeControlUnit(unit, condition);
    if (superResult) {
      TimeControlPart part = buildTimeControlPart(tokenStack);
      build.put(part.getUnit(), part);
    }
    return superResult;
  }


  private TimeControlPart buildTimeControlPart(Stack<Token> tokenStack) {
    if (tokenStack != null && tokenStack.size() > 1) {
      Token token = tokenStack.get(0);
      if (token.getType() == TokenType.TIME_UNIT_IDENTIFIER) {
        JodaTimeControlUnit unit = ((TimeUnitIdentifierToken)token).timeControlUnit;
        if (unit == JodaTimeControlUnit.DAY_OF_WEEK) {
          return buildDayOfWeekDecorator(tokenStack.subList(1, tokenStack.size()));
        } else {
          token = tokenStack.get(1);
          boolean fromList = false;
          switch (token.getType()) {
            case INCREMENT_START :
              return buildIncrement(unit, tokenStack.subList(2, tokenStack.size() - 1));
            case LIST_START :
              fromList = true;
            case VALUE_RANGE :
              return buildWhiteList(unit, tokenStack.subList(2, tokenStack.size()), fromList);
            case NEGATION :
              Token nextToken = tokenStack.get(2);
              switch (nextToken.getType()) {
                case Xth_DAY_OF_WEEK_IN_MONTH :
                  return buildXthDayOfWeek(unit, tokenStack.subList(3, tokenStack.size()), true);
                case VALUE :
                  return buildStatic(unit, nextToken, true);
                case LIST_START :
                  fromList = true;
                case VALUE_RANGE :
                  return buildWhiteList(unit, tokenStack.subList(2, tokenStack.size()), fromList);          
                case DAY_IN_WEEK :
                  return buildDayInWeek(unit, tokenStack.get(3), true);
                default :
                  throw new RuntimeException("unexpected tokentype " + nextToken.getType());
              }
            case VALUE :
              return buildStatic(unit, token, false);
            case Xth_DAY_OF_WEEK_IN_MONTH :
              return buildXthDayOfWeek(unit, tokenStack.subList(2, tokenStack.size()), false);
            case DAY_IN_WEEK :
              return buildDayInWeek(unit, tokenStack.get(2), false);
            default :
              throw new RuntimeException("unexpected tokentype " + token.getType());
          }
        }
      } else {
        throw new RuntimeException("unexpected tokentype " + token.getType());
      }
    } else {
      throw new RuntimeException("time control part missing");
    }
  }
  
  
  private TimeControlPart buildIncrement(JodaTimeControlUnit unit, List<Token> tokens) {
    ValueToken start;
    ValueToken increment;
    if (tokens.size() == 1) {
      start = new NumericValueToken(-1); 
      increment = (TimeControlRuleTokenizer.ValueToken) tokens.get(0);
    } else {
      start = (TimeControlRuleTokenizer.ValueToken) tokens.get(0);
      increment = (TimeControlRuleTokenizer.ValueToken) tokens.get(1);
    }
    return new TimeControlPart.PeriodicTimeControlPart(unit, start.getValue(), increment.getValue());
  }

  
  private TimeControlPart buildWhiteList(JodaTimeControlUnit unit, List<Token> tokens, boolean fromList) {
    Set<Integer> whiteValues = new HashSet<Integer>();
    Set<Integer> negWhiteValues = new HashSet<Integer>();
    Map<Integer, Integer> dynamicRanges = new HashMap<Integer, Integer>();
    Iterator<Token> tokenIter = tokens.iterator();
    if (fromList) {
      while (tokenIter.hasNext()) {
        Token token = tokenIter.next();
        if (token.getType() == TokenType.VALUE) {
          whiteValues.add(((ValueToken)token).getValue());
        } else if (token.getType() == TokenType.VALUE_RANGE) {
          ValueToken from = (ValueToken)tokenIter.next();
          Token nextToken = tokenIter.next();
          if (nextToken.getType() == TokenType.NEGATION) {
            dynamicRanges.put(from.getValue(), ((ValueToken)tokenIter.next()).getValue());
          } else {
            whiteValues.addAll(getValuesFromRange(unit, from, (ValueToken)tokenIter.next(), false));
          }
        } else if (token.getType() == TokenType.NEGATION) {
          Token nextToken = tokenIter.next();
          if (nextToken.getType() == TokenType.VALUE) {
            negWhiteValues.add(((ValueToken)nextToken).getValue());
          } else if (token.getType() == TokenType.VALUE_RANGE) {
            ValueToken from = (ValueToken)tokenIter.next();
            Token expectedNegation = tokenIter.next();
            if (expectedNegation.getType() == TokenType.NEGATION) {
              negWhiteValues.addAll(getValuesFromRange(unit, from, (ValueToken)tokenIter.next(), true));
            } else {
              // first is neg
              negWhiteValues.addAll(getValuesFromRange(unit, from, new NumericValueToken(1), true));
              // second is pos
              whiteValues.addAll(getValuesFromRange(unit, new NumericValueToken(unit.getLowerBound()), (ValueToken) expectedNegation, false));
            }
          }
        }
      }
    } else {
      Token token = tokenIter.next();
      if (token.getType() == TokenType.NEGATION) {
        ValueToken from = (ValueToken)tokenIter.next();
        token = tokenIter.next();
        if (token.getType() == TokenType.NEGATION) {
          negWhiteValues.addAll(getValuesFromRange(unit, from, (ValueToken)tokenIter.next(), true));
        } else {
          // first is neg
          negWhiteValues.addAll(getValuesFromRange(unit, from, new NumericValueToken(1), true));
          // second is pos
          whiteValues.addAll(getValuesFromRange(unit, new NumericValueToken(unit.getLowerBound()), (ValueToken) token, false));
        }
      } else {
        ValueToken from = (ValueToken)token;
        token = tokenIter.next();
        if (token.getType() == TokenType.NEGATION) {
          dynamicRanges.put(from.getValue(), ((ValueToken)tokenIter.next()).getValue());
        } else {
          whiteValues.addAll(getValuesFromRange(unit, from, (ValueToken)token, false));
        }
      }
    }
    Integer[] whiteInteger = whiteValues.toArray(new Integer[whiteValues.size()]);
    int[] whiteArray = new int[whiteValues.size()];
    for (int i = 0; i < whiteValues.size(); i++) {
      whiteArray[i] = whiteInteger[i].intValue();
    }
    Integer[] negWhiteInteger = negWhiteValues.toArray(new Integer[negWhiteValues.size()]);
    int[] negWhiteArray = new int[negWhiteValues.size()];
    for (int i = 0; i < negWhiteValues.size(); i++) {
      negWhiteArray[i] = negWhiteInteger[i].intValue();
    }
    if (dynamicRanges.size() > 0) {
      return new TimeControlPart.DynamicWhiteRange(unit, whiteArray, negWhiteArray, dynamicRanges);
    } else {
      return new TimeControlPart.WhiteListTimeControlPart(unit, whiteArray, negWhiteArray);
    }
    
  }
  
  
  private List<Integer> getValuesFromRange(JodaTimeControlUnit unit, ValueToken from, ValueToken to, boolean negated) {
    if (negated) {
      return getValuesFromRange(unit, to.getValue() - 1, from.getValue() - 1);
    } else {
      return getValuesFromRange(unit, from.getValue(), to.getValue());
    }
  }
  
  
  private List<Integer> getValuesFromRange(JodaTimeControlUnit unit, int from, int to) {
    List<Integer> whiteValues  = new ArrayList<Integer>();
    if (from <= to) {
      int current = from;
      while (current <= to) {
        whiteValues.add(current);
        current++;
      }
    } else {
      whiteValues.addAll(getValuesFromRange(unit, from, unit.getUpperBound()));
      whiteValues.addAll(getValuesFromRange(unit, unit.getLowerBound(), to));
    }
    return whiteValues;
  }
  
  
  
  private TimeControlPart buildStatic(JodaTimeControlUnit unit, Token token, boolean negative) {
    return new TimeControlPart.StaticTimeControlPart(unit, ((TimeControlRuleTokenizer.ValueToken) token).getValue(), negative);
  }
  
  
  private TimeControlPart buildDayInWeek(JodaTimeControlUnit unit, Token token, boolean negative) {
    return new TimeControlPart.DayInWeekControlPart(((ValueToken)token).getValue(), negative);
  }
  
  
  private TimeControlPart buildXthDayOfWeek(JodaTimeControlUnit unit, List<Token> tokens, boolean negated) {
    int dayOfWeekValue;
    int value;
    if (tokens.size() < 2) {
      value = 1;
      dayOfWeekValue = ((ValueToken)tokens.get(0)).getValue();
    } else {
      value = ((ValueToken)tokens.get(0)).getValue();
      dayOfWeekValue = ((ValueToken)tokens.get(1)).getValue();
    }
    if (negated) {
      value = -value;
    }
    return new TimeControlPart.WeekDayInMonth(dayOfWeekValue, value);
  }
  
  
  private TimeControlPart buildDayOfWeekDecorator(List<Token> tokens) {
    Iterator<Token> tokenIterator = tokens.iterator();
    int[] whiteValuesArray;
    Token token = tokenIterator.next();
    switch (token.getType()) {
      case VALUE :
        whiteValuesArray = new int[1];
        whiteValuesArray[0] = ((ValueToken) token).getValue();
        break;
      case VALUE_RANGE :
        int from = ((ValueToken) tokenIterator.next()).getValue();
        int to = ((ValueToken) tokenIterator.next()).getValue();
        whiteValuesArray = getAllIntsBetween(from, to);
        break;
      case LIST_START :
        Set<Integer> ints = new HashSet<Integer>();
        while (token.getType() != TokenType.LIST_END) {
          token = tokenIterator.next();
          if (token.getType() == TokenType.VALUE) {
            ints.add(((ValueToken) token).getValue());
          } else if (token.getType() != TokenType.LIST_END) {
            from = ((ValueToken) tokenIterator.next()).getValue();
            to = ((ValueToken) tokenIterator.next()).getValue();
            whiteValuesArray = getAllIntsBetween(from, to);
            for (Integer integer : ints) {
              ints.add(integer);
            }
          }
        }
        Integer[] integers = ints.toArray(new Integer[ints.size()]);
        whiteValuesArray = new int[integers.length];
        for (int i = 0; i < integers.length; i++) {
          whiteValuesArray[i] = integers[i].intValue();
        }
        break;
      default :
        throw new RuntimeException("");
    }
    return new TimeControlPart.DayOfWeekControlPart(whiteValuesArray);
  }
  
  
  private static int[] getAllIntsBetween(int from, int to) {
    int[] values = new int[to-from+1];
    for (int i = 0; i < values.length; i++) {
      values[i] = i + from;
    }
    return values;
  }
  
  
  public RestrictionBasedTimeWindow buildTimeWindow(RestrictionBasedTimeWindowDefinition definition) {
    
    if (!acceptRule(definition.getRuleAndDuration())) {
      throw new IllegalArgumentException("Invalid calendar definition: " + definition.getRuleAndDuration() + ".");
    }
    
    if (CHECK_DST && definition.isConsiderDaylightSaving()) {
      if (!acceptDaylightSavingTime()) {
        throw new IllegalArgumentException("Daylight saving time isn't supported for " + definition.getRuleAndDuration() + ".");
      }
    }
    
    TimeControlPart root = null;
    TimeControlPart current = null;
    boolean userDefinedField = false;
    for (JodaTimeControlUnit unit : JodaTimeControlUnit.getSimpleTimeControlUnitsInOrder(true)) {
      TimeControlPart part = build.get(unit);
      if (part == null && !userDefinedField) { 
          // insert static parts for everything below the first user defined field
          part = new TimeControlPart.StaticTimeControlPart(unit, -1, false);
      } else {
        userDefinedField = true;
      }
      if (part != null) {
        if (root == null) {
          root = part;
        }
        if (current == null) {
          current = part;
        } else {
          current.setNext(part);
          current = part;
        }
      }
    }
    return new RestrictionBasedTimeWindow(root, duration, definition);
  }
  

  private boolean acceptDaylightSavingTime() {
    boolean periodicAllowed = true;
    JodaTimeControlUnit[] unitsInOrder = JodaTimeControlUnit.getSimpleTimeControlUnitsInOrder(true, JodaTimeControlUnit.DAY);
    for (JodaTimeControlUnit unit : unitsInOrder) {
      TimeControlPart part = build.get(unit);
      if (part == null) {
        continue;
      }
      if (part instanceof StaticTimeControlPart) {
        //statische TimeControlParts sind immer erlaubt
        continue;
      } else if (part instanceof PeriodicTimeControlPart) {
        //periodische TimeControlParts sind erlaubt, wenn sie Vielfache eines Tages sind
        int increment = ((PeriodicTimeControlPart)part).getIncrement();
        switch (unit) { //absichtlich keine breaks
          case MILLISECOND:
            if (increment % 1000 != 0) return false;
            increment = increment / 1000;
          case SECOND:
            if (increment % 60 != 0) return false;
            increment = increment / 60;
          case MINUTE:
            if (increment % 60 != 0) return false;
            increment = increment / 60;
          case HOUR:
            if (increment % 24 != 0) return false;
          case DAY:
            //es darf nur einen periodischen TimeControlParts (kleiner oder gleich Day) geben
            if (!periodicAllowed) return false;
            periodicAllowed = false;
        }
      } else {
        //andere TimeControlParts sind ausser im Tages-Feld nicht erlaubt
        return unit == JodaTimeControlUnit.DAY;
      }
    }
    return true;
  }
  
}
