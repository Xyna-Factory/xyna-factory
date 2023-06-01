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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.StringUtils;

/**
 * Scans if a given String satisfies the format and returns true if it does 
 */
public class TimeControlRuleParser<T extends TimeControlUnit> {

  final static String MILLISECOND_IDENTIFIER = "Millis";
  final static String SECOND_IDENTIFIER = "Second";
  final static String MINUTE_IDENTIFIER = "Minute";
  final static String HOUR_IDENTIFIER = "Hour";
  final static String DAY_IDENTIFIER = "Day";
  final static String MONTH_IDENTIFIER = "Month";
  final static String DAY_OF_WEEK_IDENTIFIER = "DayOfWeek";
  final static String YEAR_IDENTIFIER = "Year";
  
  private final static String RESTRICTION_IDENTIFIER = "Restriction";
  private final static String DURATION_IDENTIFIER = "Duration";
  
  private final static Pattern rulePattern = Pattern.compile("(("+RESTRICTION_IDENTIFIER+"|"+DURATION_IDENTIFIER+")=)?\\[([^\\]]+)\\]");
  private final static Pattern onlyNumbers = Pattern.compile("^[0-9]+$");
  
  
  private final T aInstance;
  protected long duration;
  
  public TimeControlRuleParser(Class<T> backingEnum) {
    T[] enums = backingEnum.getEnumConstants();
    if (enums == null || enums.length == 0) {
      throw new RuntimeException("Backing enum has no values!");
    } else {
      aInstance = enums[0];
    }
  }
 
  protected boolean acceptRule(String rule) {
    Matcher ruleMatcher = rulePattern.matcher(rule);
    boolean acceptedRestriction = false;
    boolean acceptedDuration = false;
    if (ruleMatcher.find()) {
      String identifier = ruleMatcher.group(2);
      if (identifier == null) {
        acceptedRestriction = acceptRestrictions(ruleMatcher.group(3));
      } else if (identifier.equals(RESTRICTION_IDENTIFIER)) {
        acceptedRestriction = acceptRestrictions(ruleMatcher.group(3));
      } else if (identifier.equals(DURATION_IDENTIFIER)) {
        acceptedDuration = acceptDuration(ruleMatcher.group(3));
      } else {
        return false;
      }
    }
    if (ruleMatcher.find()) {
      String identifier = ruleMatcher.group(2);
      if (identifier == null) {
        acceptedDuration = acceptDuration(ruleMatcher.group(3));
      } else if (identifier.equals(RESTRICTION_IDENTIFIER)) {
        acceptedRestriction = acceptRestrictions(ruleMatcher.group(3));
      } else if (identifier.equals(DURATION_IDENTIFIER)) {
        acceptedDuration = acceptDuration(ruleMatcher.group(3));
      } else {
        return false;
      }
    }
    return acceptedRestriction && acceptedDuration;
  }
  
  
  protected boolean acceptRestrictions(String restrictions) {
    String[] restrictionSplit = split(restrictions, ';');
    for (String restriction : restrictionSplit) {
      String[] keyValueSplit = split(restriction, '=');
      if (keyValueSplit.length == 2) {
        TimeControlUnit unit = aInstance.getTimeControlUnitByStringIdentifier(keyValueSplit[0]);
        if (unit == null) {
          return false;
        }
        if (!acceptTimeControlUnit(unit, keyValueSplit[1])) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }
  
  
  protected boolean acceptDuration(String durations) {
    long durationValue = 0;
    String[] durationSplit = split(durations, ';');
    for (String duration : durationSplit) {
      String[] keyValueSplit = split(duration, '=');
      if (keyValueSplit.length == 2) {
        if (keyValueSplit[0].equals(MILLISECOND_IDENTIFIER)) {
          durationValue += Long.parseLong(keyValueSplit[1]);
        } else if (keyValueSplit[0].equals(SECOND_IDENTIFIER)) {
          durationValue += Long.parseLong(keyValueSplit[1]) * 1000;
        } else if (keyValueSplit[0].equals(MINUTE_IDENTIFIER)) {
          durationValue += Long.parseLong(keyValueSplit[1]) * 1000 * 60;
        } else if (keyValueSplit[0].equals(HOUR_IDENTIFIER)) {
          durationValue += Long.parseLong(keyValueSplit[1]) * 1000 * 60 * 60;
        } else if (keyValueSplit[0].equals(DAY_IDENTIFIER)) {
          durationValue += Long.parseLong(keyValueSplit[1]) * 1000 * 60 * 60 * 24;
        }
      } else {
        return false;
      }
    }
    this.duration = durationValue;
    return true;
  }
  
  
  protected boolean acceptTimeControlUnit(TimeControlUnit unit, String condition) {
    if (acceptSimpleValue(unit, condition) || 
        acceptValueRange(unit, condition) ||
        acceptValueList(unit, condition)) {
      return true;
    } else if (unit.getStringIdentifier().equals(DAY_IDENTIFIER)) {
      return acceptXthWeekDayInMonth(unit, condition) || 
             acceptWeekDayValue(unit, condition) ||
             acceptValueWithIncrement(unit, condition);
    } else if (!unit.getStringIdentifier().equals(DAY_OF_WEEK_IDENTIFIER)) {
      return acceptValueWithIncrement(unit, condition);
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptSimpleValue(TimeControlUnit unit, String condition) {
    if (unit.getStringIdentifier().equals(DAY_IDENTIFIER)) {
        return acceptNumberOrStringRepresentation(unit, condition) ||
               acceptNegativValue(unit, condition);
    } else {
      return acceptNumberOrStringRepresentation(unit, condition);
    }
  }
  
  
  protected boolean acceptWeekDayValue(TimeControlUnit unit, String condition) {
    if (condition.endsWith("W")) {
      return acceptSimpleValue(unit, condition.substring(0, condition.length() - 1));
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptValueRange(TimeControlUnit unit, String condition) {
    if (condition.contains("-")) {
      int index = condition.indexOf('-');
      if (index == 0) {
        index = condition.indexOf('-', 1);
      }
      //String[] rangeParts = condition.split("-");
      if (//rangeParts.length == 2 &&
                      index > 0 &&
          acceptSimpleValue(unit, condition.substring(0, index).trim()) &&
          acceptSimpleValue(unit, condition.substring(index + 1).trim())) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptValueList(TimeControlUnit unit, String condition) {
    if (condition.contains(",")) {
      String[] listParts = split(condition, ',');
      if (listParts.length > 1) {
        for (String listPart : listParts) {
          if (!(acceptSimpleValue(unit, listPart.trim()) ||
                acceptValueRange(unit, listPart.trim()))) {
            return false;
          }
        }
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptValueWithIncrement(TimeControlUnit unit, String condition) {
    if (condition.contains(":")) {
      String[] rangeParts = split(condition, ':');
      if (rangeParts.length == 2 && rangeParts[0].length() > 0 &&
          acceptSimpleValue(unit, rangeParts[0].trim()) &&
          acceptSimpleNumber(rangeParts[1].trim(), 1, Integer.MAX_VALUE)) {
        return true;
      } else if (rangeParts.length == 2 && rangeParts[0].length() == 0 ) {
        return acceptSimpleNumber(rangeParts[1], 1, Integer.MAX_VALUE);
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptXthWeekDayInMonth(TimeControlUnit unit, String condition) {
    return acceptPositiveXthWeekDayInMonth(unit, condition) ||
           acceptNegativeXthWeekDayInMonth(unit, condition);
  }
  
  protected boolean acceptNegativeXthWeekDayInMonth(TimeControlUnit unit, String condition) {
    if (condition.length() > 0 && condition.contains("L")) {
      int lIndex = condition.indexOf('L');
      String positiveCondition = condition.substring(0, lIndex) + condition.substring(lIndex + 1, condition.length());
      return acceptPositiveXthWeekDayInMonth(unit, positiveCondition);
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptPositiveXthWeekDayInMonth(TimeControlUnit unit, String condition) {
    if (condition.contains("#")) {
      String[] conditionSplit = split(condition, '#');
      if (conditionSplit.length == 2) {
        return acceptSimpleNumber(conditionSplit[0].trim(), 0, 5) &&
               acceptSimpleValue(unit.getTimeControlUnitByStringIdentifier(DAY_OF_WEEK_IDENTIFIER), conditionSplit[1].trim());
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptNegativValue(TimeControlUnit unit, String condition) {
    if (condition.length() > 0 && condition.endsWith("L")) {
      return acceptNumberOrStringRepresentation(unit, condition.substring(0, condition.length() - 1));
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptNumberOrStringRepresentation(TimeControlUnit unit, String condition) {
    return acceptSimpleNumber(condition, unit.getLowerBound(), unit.getUpperBound()) ||
           acceptStringRepresentation(unit, condition);
  }
  
  
  protected boolean acceptStringRepresentation(TimeControlUnit unit, String condition) {
    // this assumes numbers have already been tried, so if there is no backingEnum we can't accept it
    if (unit.hasBackingEnum()) {
      return getTimeControlEnumByStringRepresentation(condition, unit.getBackingEnumClass()) != null;
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptSimpleNumber(String expression, int min, int max) {
    Matcher numbersMatcher = onlyNumbers.matcher(expression);
    if (numbersMatcher.matches()) {
      int value = Integer.parseInt(expression);
      return value >= min && value <= max;
    } else {
      return false;
    }
  }
  
  
  protected static TimeControlEnum getTimeControlEnumByStringRepresentation(String expression, Class<TimeControlEnum> timeControlEnumClass) {
    TimeControlEnum[] timeControlEnums = timeControlEnumClass.getEnumConstants();
    for (TimeControlEnum timeControlEnum : timeControlEnums) {
      if (timeControlEnum.getStringRepresentation().equals(expression)) {
        return timeControlEnum;
      }
    }
    return null;
  }
  
  
  private final static String[] split(String toSplit, char delimiter) {
    return StringUtils.fastSplit(toSplit, delimiter, -1);
  }
  

  public static interface TimeControlEnum /* name is more of marker and does not represent functionality, implementers do not need to control time itself */ {
    
    String getStringRepresentation();
    
    int getNumericRepresentation();
    
  }
  
  
  protected static enum Month implements TimeControlEnum {
    JAN("JAN",1),FEB("FEB",2),MAR("MAR",3),APR("APR",4),MAY("MAY",5),JUN("JUN",6),JUL("JUL",7),AUG("AUG",8),SEP("SEP",9),OKT("OKT",10),NOV("NOV",11),DEZ("DEZ",12);

    private final String stringRepresentation;
    private final int numericRepresentation;
    
    private Month(String stringRepresentation, int numericRepresentation) {
      this.stringRepresentation = stringRepresentation;
      this.numericRepresentation = numericRepresentation;
    }
    
    public String getStringRepresentation() {
      return stringRepresentation;
    }

    public int getNumericRepresentation() {
      return numericRepresentation;
    }
    
  }
  
  
  protected static enum DayOfWeek implements TimeControlEnum {
    MON("MON",1),TUE("TUE",2),WED("WED",3),THU("THU",4),FRI("FRI",5),SAT("SAT",6),SUN("SUN",7);

    private final String stringRepresentation;
    private final int numericRepresentation;
    
    private DayOfWeek(String stringRepresentation, int numericRepresentation) {
      this.stringRepresentation = stringRepresentation;
      this.numericRepresentation = numericRepresentation;
    }
    
    public String getStringRepresentation() {
      return stringRepresentation;
    }
    
    public int getNumericRepresentation() {
      return numericRepresentation;
    }
    
  }
  
}
