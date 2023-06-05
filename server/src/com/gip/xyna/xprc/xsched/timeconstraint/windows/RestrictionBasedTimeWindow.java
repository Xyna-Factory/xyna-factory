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

import java.util.TimeZone;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeControlPart.Stepping;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeControlPart.TimeViolationException;

/*
 * FORMAT:
 * We dont' want to use standard cron notation but a rather similar syntax
 * We support -value as END_OF_??? minus value
 * That removes the need to support a special identifier for something like L
 * 
 * Seconds:
 * 0-59 as value | <value>:1-59 as valueIncrement| <value>-<value> as valueRange | <valueRange>,<valueRange> as valueList
 * Minutes:
 * same as Seconds
 * Hours:
 * 0-23 as value | rest same as Seconds
 * Day of month:
 * 1-31 as value | -<value> as negativeValue | <value> or <negativeValue> as simpleValue | <simpleValue>W as weekDayValue | 
 *  <simpleValue>:1-31 as valueIncrement| <simpleValue>-<simpleValue> as valueRange | <simpleValue>,<simpleValue> as valueList
 * Month:
 * 1-12 or JAN-DEC as value | rest same as Seconds
 * Day of week:
 * 0-6 as value | -<value> as xthWeekDayInMonth | rest same as Seconds (only using <values>, <xthWeekDayInMonth> has to stand alone)
 * Year:
 * 1970-2099 as value | rest same as Seconds
 * 
 * xthWeekDayInMonth should not be in DayOfWeek and rather in DayOfMonth?
 * although its primitives are DayOfWeeks ith rather effects the DayOfMonth setting
 * Revised:
 * 
 * Seconds:
 * 0-59 as value | <value>:1-59 as valueIncrement | <value>-<value> as valueRange | <valueRange>,<valueRange> as valueList
 * Minutes:
 * same as Seconds
 * Hours:
 * 0-23 as value | <value>:1-23 as valueIncrement | rest same as Seconds
 * Day of month:
 * 1-31 as value | -<value> as negativeValue | <value> or <negativeValue> as simpleValue | <simpleValue>W as weekDayValue | 
 *  <simpleValue>:1-31 as valueIncrement| <simpleValue>-<simpleValue> as valueRange | <valueRange>,<valueRange> as valueList |
 *  0-5#<Day of week value> as xthWeekDayOfMonth | -<xthWeekDayOfMonth> as xthLastWeekDayOfMonth
 * Month:
 * 1-12 or JAN-DEC as value | rest same as Seconds
 * Day of week:
 * 0-6 or MON-SUN as value | <value>-<value> as valueRange | <valueRange>,<valueRange> as valueList
 * Year:
 * 1970-2099 as value | rest same as Seconds
 * 
 * 
 * Rule StringRepresentation
 * as XML
 * <Restriction>
 *    <TimeUnit>???</TimeUnit>
 *    <Condition>???</Condition>
 * </Restriction>
 * <Duration>
 *    <TimeUnit>???<TimeUnit>
 *    <Value>???</Value>
 * </Duration>
 * short rep for cli
 * Restriction=[Hour=???;Second=???]Duration=[Seconds=???]
 * or shorter
 * [Hour=???;Second=???][Seconds=???]
 * TODO allow empty duration
 * 
 * DURATION:
 * why is duration fixed, couldn't duration be implicit by unrestricted areas?
 * Intent: every 1. and 15. of a Month, open for half an hour
 * With duration: duration = 30 min,  Restriction[DayOfMonth=1,15]  <-- exact hour would be unspecified
 *                duration = 30 min,  Restriction[DayOfMonth=1,15;Hour=14;Minutes=15] <-- open every 1. and 15. from 14:15 - 14:45
 * Without duration: Restriction[DayOfMonth=1,15;Minutes=0-30]
 *                   Restriction[DayOfMonth=1,15;Hour=14;Minutes=15-45]
 * Intent: every 1. and 15. of a Month, opens at 14:15 closes at 16:45
 * With duration: duration = 2 hours 30 min,  Restriction[DayOfMonth=1,15;Hour=14;Minutes=15]
 * Without duration: not expressible!!! 
 * It would be expressible if:
 * Intent: every 1. and 15. of a Month, opens at 14:15 closes at 16:15
 * With duration: duration = 2 hours 30 min,  Restriction[DayOfMonth=1,15;Hour=14-16;Minutes=15] but that could as well mean we want several minute long windows
 * ...duration has it's merits
 * What would be with an open & closing restriction?^^
 * More complex and more easily to missconfigure, durations pretty fine after all ;)
 * 
 */
public class RestrictionBasedTimeWindow extends TimeWindow {

  private final TimeControlPart root;
  private final long duration;
  private DateTime lastExecution;
  private RestrictionBasedTimeWindowDefinition definition;
  private transient boolean initialized = false;
  
  public RestrictionBasedTimeWindow(TimeControlPart root, long duration, RestrictionBasedTimeWindowDefinition definition) {
    this.duration = duration;
    this.root = root;
    this.definition = definition;
    if (definition.getAValidTime() != null) {
      lastExecution = createNow(definition.getAValidTime());
    }
  }
  
  
  @Override
  protected TimeWindowData recalculateInternal(long now) {
    DateTime nowDT = createNow(now);
    
    if (lastExecution == null) {
      lastExecution = root.initialize(nowDT);
    } else if (!initialized) {
      lastExecution = root.initialize(lastExecution);
    }
    initialized = true;
    
    definition.setAValidTime(getUTCStamp(lastExecution));
     
    DateTime afterOrEqual;
    try {
      afterOrEqual = calculate(nowDT, Stepping.AFTER_OR_EQUAL);
    } catch (TimeViolationException e) {
      afterOrEqual = createNow(Long.MAX_VALUE);
    }
    DateTime before;
    try {
      before = calculate(nowDT, Stepping.BEFORE_OR_EQUAL);
      if (afterOrEqual.isEqual(before)) {
        before = calculate(nowDT, Stepping.BEFORE);
      }
    } catch (TimeViolationException e) {
      before = createNow(Long.MIN_VALUE);
    }
       
    // Cases:
    //     b--duration-->        a--duration-->
    // 1)         n
    // 2)                   n
    // 3)                        n
    if (before.isBefore(nowDT) && before.isAfter(nowDT.minus(duration))) { // 1)
      this.lastExecution = before;
      return new TimeWindowData(true, getUTCStamp(afterOrEqual), getUTCStamp(before) + duration, getUTCStamp(before));
    } else if (before.isBefore(nowDT.plus(duration)) && afterOrEqual.isAfter(nowDT)) { // 2)
      this.lastExecution = afterOrEqual;
      long stampForAfterOrEqual = getUTCStamp(afterOrEqual);
      return new TimeWindowData(false, stampForAfterOrEqual,
                                stampForAfterOrEqual == Long.MAX_VALUE ? Long.MAX_VALUE : stampForAfterOrEqual + duration,
                                getUTCStamp(before) + duration);
    } else {
      DateTime newReference = nowDT.plus(duration);
      DateTime furtherFuture = calculate(newReference, Stepping.AFTER_OR_EQUAL); 
      this.lastExecution = afterOrEqual;
      return new TimeWindowData(true, getUTCStamp(furtherFuture), getUTCStamp(afterOrEqual) + duration, getUTCStamp(afterOrEqual));
    }
    
  }
  
  
  private DateTime calculate(DateTime now, Stepping stepping) {
    return root.calculate(now, lastExecution, now, stepping);
  }
  
  
  
  @Override
  public RestrictionBasedTimeWindowDefinition getDefinition() {
    return definition;
  }
  
  
  public void setDefinition(RestrictionBasedTimeWindowDefinition twd) {
    this.definition = twd;
  }
  
  
  private DateTime createNow(long instant) {
    if (instant == Long.MAX_VALUE || instant == Long.MIN_VALUE) {
      return new DateTime(instant, DateTimeZone.UTC);
    } else {
      DateTimeZone dtz = getTimeZone();
      long local = instant;
      if (definition.considerDaylightSaving) {
        local += dtz.getOffset(local);
      } else {
        local += dtz.getStandardOffset(local);
      }
      return new DateTime(local, DateTimeZone.UTC);
    }
  }
  
  private long getUTCStamp(DateTime time) {
    long local = time.getMillis();
    if (local != Long.MAX_VALUE && local != Long.MIN_VALUE) {
      DateTimeZone dtz = getTimeZone();
      if (definition.considerDaylightSaving) {
        int offset = dtz.getOffset(local);
        long adjustedLocal = local - offset;
        int adjustedLocalOffset = dtz.getOffset(adjustedLocal);
        if (offset > adjustedLocalOffset) {
          return dtz.nextTransition(adjustedLocal);
        } else if (offset < adjustedLocalOffset) {
          return local - adjustedLocalOffset;
        } else {
          local -= offset;
        }
      } else {
        local -= dtz.getStandardOffset(local);
      }
    }
    return local;
  }
  
  
  private DateTimeZone getTimeZone() { // http://stackoverflow.com/questions/10684563/java-systemv-timezones-and-jodatime
    TimeZone tz = TimeZone.getTimeZone(definition.getTimezoneId());
    try {
      return DateTimeZone.forTimeZone(tz);
    } catch (IllegalArgumentException e) {
      return DateTimeZone.forID(definition.getTimezoneId().replaceAll("SystemV/", ""));  
    }
  }
  
  
  @Override
  public String toString() {
    return definition.ruleAndDuration + "@" + definition.getTimezoneId();
  }
  
  
  /**
   * RestrictionBasedTimeWindowDefinition 
   * ist nicht mehr immutable, da Setter setAValidTime exiistiert. 
   * setAValidTime ist aber private und wird nur hier lokal verwendet, sollte daher keine Probleme machen
   */
  public static class RestrictionBasedTimeWindowDefinition extends TimeWindowDefinition {
    
    private static final long serialVersionUID = 1L;
    
    private final static String TYPE = "RestrictionBased";
    
    private String ruleAndDuration;
    private String timezoneId;
    private boolean considerDaylightSaving;
    private Long aValidTime;
    
    public RestrictionBasedTimeWindowDefinition(String ruleAndDuration, String timezoneId, boolean considerDaylightSaving) {
      this.ruleAndDuration = ruleAndDuration;
      this.timezoneId = timezoneId;
      this.considerDaylightSaving = considerDaylightSaving;
    }

    
    public String getTimezoneId() {
      return timezoneId;
    }

    
    public boolean isConsiderDaylightSaving() {
      return considerDaylightSaving;
    }
    
    
    public String getRuleAndDuration() {
      return ruleAndDuration;
    }
    
    
    public Long getAValidTime() {
      return aValidTime;
    }
    
    
    private void setAValidTime(Long aValidTime) {
      this.aValidTime = aValidTime;;
    }
    
    
    @Override
    public TimeWindow constructTimeWindow() {
      return new TimeControlRuleBuilder().buildTimeWindow(this);
    }


    @Override
    public TimeWindowDefinition deserializeFromString(String string) { // nicer Format?
      int paraOpen = string.indexOf("(");
      int paraClose = string.indexOf(")");
      string = string.substring(paraOpen + 1, paraClose);
      int endOfRuleAndDuration = string.indexOf("@");
      int endOfTimezone = string.indexOf("@", endOfRuleAndDuration + 1);
      int endOfConsiderDST = string.indexOf("@", endOfTimezone + 1);
      if (endOfConsiderDST < 0) {
        endOfConsiderDST = string.length();
      }
      RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(string.substring(0, endOfRuleAndDuration),
                                                      string.substring(endOfRuleAndDuration + 1, endOfTimezone),
                                                      Boolean.parseBoolean(string.substring(endOfTimezone + 1, endOfConsiderDST)));
      if (endOfConsiderDST < string.length()) {
        def.setAValidTime(Long.parseLong(string.substring(endOfConsiderDST + 1)));
      }
      return def;
    }


    @Override
    public String serializeToString() {
      String string = ruleAndDuration+'@'+timezoneId+'@'+Boolean.toString(considerDaylightSaving);
      if (aValidTime != null) {
        string += "@" + aValidTime.longValue();
      }
      return TYPE + "(" + string + ")";
    }


    @Override
    public String getType() {
      return TYPE;
    }
    
  }
  
}
