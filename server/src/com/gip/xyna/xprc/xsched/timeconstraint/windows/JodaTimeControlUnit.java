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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.DateTime.Property;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.ReadableInstant;
import org.joda.time.Seconds;
import org.joda.time.Years;

import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeControlRuleParser.DayOfWeek;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeControlRuleParser.Month;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeControlRuleParser.TimeControlEnum;



public enum JodaTimeControlUnit implements TimeControlUnit {

  MILLISECOND(TimeControlRuleParser.MILLISECOND_IDENTIFIER, DateTimeFieldType.millisOfSecond(), 999) {
    @Override
    public Property getProperty(DateTime from) {
      return from.millisOfSecond();
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      return SECOND;
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return another.getMillis() - one.getMillis();
    }
  },
  SECOND(TimeControlRuleParser.SECOND_IDENTIFIER, DateTimeFieldType.secondOfMinute(), 59) {
    @Override
    public Property getProperty(DateTime from) {
      return from.secondOfMinute();
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      return MINUTE;
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return Seconds.secondsBetween(one, another).getValue(0);
    }
  },
  MINUTE(TimeControlRuleParser.MINUTE_IDENTIFIER, DateTimeFieldType.minuteOfHour(), 59) {
    @Override
    public Property getProperty(DateTime from) {
      return from.minuteOfHour();
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      return HOUR;
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return Minutes.minutesBetween(one, another).getValue(0);
    }
  },
  HOUR(TimeControlRuleParser.HOUR_IDENTIFIER, DateTimeFieldType.hourOfDay(), 23) {
    @Override
    public Property getProperty(DateTime from) {
      return from.hourOfDay();
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      return DAY;
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return Hours.hoursBetween(one, another).getValue(0);
    }
  },
  DAY(TimeControlRuleParser.DAY_IDENTIFIER, DateTimeFieldType.dayOfMonth(), 1, 31) {
    @Override
    public Property getProperty(DateTime from) {
      return from.dayOfMonth();
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      return MONTH;
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return Days.daysBetween(one, another).getValue(0);
    }
  },
  DAY_OF_WEEK(TimeControlRuleParser.DAY_OF_WEEK_IDENTIFIER, DateTimeFieldType.dayOfWeek(), 1, 7, DayOfWeek.class) {
    @Override
    public Property getProperty(DateTime from) {
      throw new UnsupportedOperationException("MILLISECONDS.between");
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      throw new UnsupportedOperationException("getNextHigherUnit for DayOfWeek");
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return Months.monthsBetween(one, another).getValue(0);
    }
  },
  MONTH(TimeControlRuleParser.MONTH_IDENTIFIER, DateTimeFieldType.monthOfYear(), 1, 12, Month.class) {
    @Override
    public Property getProperty(DateTime from) {
      return from.monthOfYear();
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      return YEAR;
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return Months.monthsBetween(one, another).getValue(0);
    }
  },
  YEAR(TimeControlRuleParser.YEAR_IDENTIFIER, DateTimeFieldType.year(), 1, Integer.MAX_VALUE) {
    @Override
    public Property getProperty(DateTime from) {
      return from.yearOfEra();
    }

    @Override
    public JodaTimeControlUnit getNextHigherUnit() {
      return null;
    }

    @Override
    public long between(ReadableInstant one, ReadableInstant another) {
      return Years.yearsBetween(one, another).getValue(0);
    }
  };
  
  
  private final DateTimeFieldType dateTimeFieldType;
  private final int lowerBound;
  private final int upperBound;
  private final Class<TimeControlEnum> backingEnum;
  private final String stringRepresentation;
  
  private JodaTimeControlUnit(String stringRepresentation, DateTimeFieldType dateTimeFieldType, int upperBound) {
    this(stringRepresentation, dateTimeFieldType, 0, upperBound);
  }
  
  private <E extends TimeControlEnum> JodaTimeControlUnit(String stringRepresentation, DateTimeFieldType dateTimeFieldType, int lowerBound, int upperBound) {
    this(stringRepresentation, dateTimeFieldType, lowerBound, upperBound, (Class<E>)null);
  }
  
  @SuppressWarnings("unchecked")
  private <E extends TimeControlEnum> JodaTimeControlUnit(String stringRepresentation, DateTimeFieldType dateTimeFieldType, int lowerBound, int upperBound, Class<E> backingEnum) {
    this.stringRepresentation = stringRepresentation;
    this.dateTimeFieldType = dateTimeFieldType;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.backingEnum = (Class<TimeControlEnum>) backingEnum;
  }
  
  public DateTimeFieldType getDateTimeFieldType() {
    return dateTimeFieldType;
  }
  
  public int getUpperBound() {
    return upperBound;
  }
  
  public int getLowerBound() {
    return lowerBound;
  }
  
  public Class<TimeControlEnum> getBackingEnumClass() {
    return backingEnum;
  }
  
  public boolean hasBackingEnum() {
    return backingEnum != null;
  }
  
  public String getStringIdentifier() {
    return stringRepresentation;
  }
  
  public boolean checkValue(Object value) {
    if (checkValueAgainstBounds(lowerBound, upperBound, value)) {
      return true;
    } else if (backingEnum != null && value instanceof String) {
      return TimeControlRuleParser.getTimeControlEnumByStringRepresentation((String)value, backingEnum) != null;
    } else {
      return false;
    }
  }
  
  // both ends are inclusive
  private static boolean checkValueAgainstBounds(int from, int to, Object value) {
    int intValue;
    if (value instanceof Number) {
      intValue = ((Number) value).intValue();
    } else if (value instanceof String) {
      try {
        intValue = Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        return false;
      }
    } else {
      return false;
    }
    return intValue >= from && intValue <= to;
  }

  public TimeControlUnit getDayOfWeek() {
    return DAY_OF_WEEK;
  }

  public TimeControlUnit getTimeControlUnitByStringIdentifier(String identifier) {
    return getByStringIdentifier(identifier);
  }
  
  public static JodaTimeControlUnit getByStringIdentifier(String identifier) {
    for (JodaTimeControlUnit unit : values()) {
      if (unit.stringRepresentation.equals(identifier)) {
        return unit;
      }
    }
    return null;
  }
  
  
  public static JodaTimeControlUnit[] getSimpleTimeControlUnitsInOrder(boolean ascending, JodaTimeControlUnit upTo) {
    JodaTimeControlUnit[] unitsInOrder = getSimpleTimeControlUnitsInOrder(ascending);
    int index = Arrays.binarySearch(unitsInOrder, upTo);
    if (index < 0) {
      return unitsInOrder;
    } else {
      JodaTimeControlUnit[] upToUnit = new JodaTimeControlUnit[index +1];
      System.arraycopy(unitsInOrder, 0, upToUnit, 0, index+1);
      return upToUnit;
    }
  }
  
  public static JodaTimeControlUnit[] getSimpleTimeControlUnitsInOrder(boolean ascending) {
    if (ascending) {
      return new JodaTimeControlUnit[] {MILLISECOND, SECOND, MINUTE, HOUR, DAY, MONTH, YEAR};
    } else {
      return new JodaTimeControlUnit[] {YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISECOND};
    }
  }
  
  
  public abstract JodaTimeControlUnit getNextHigherUnit();

  
  public abstract DateTime.Property getProperty(DateTime from);
  
  
  public abstract long between(ReadableInstant one, ReadableInstant another);
  
}
