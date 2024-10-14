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

package xmcp.factorymanager.impl.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.xprc.xsched.timeconstraint.windows.MultiTimeWindow.MultiTimeWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.RestrictionBasedTimeWindow.RestrictionBasedTimeWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.SimplePeriodicTimeWindow.SimplePeriodicTimeWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindowDefinition;

import xmcp.factorymanager.shared.MultiTimeWindow;
import xmcp.factorymanager.shared.RestrictionBasedTimeWindow;
import xmcp.factorymanager.shared.SimpleTimeWindow;
import xmcp.factorymanager.shared.TimeUnit;
import xmcp.factorymanager.shared.TimeWindow;

public class TimeWindowConverter {
  
  private static final String TIME_UNIT_YEAR = "Year";
  private static final String TIME_UNIT_MONTH = "Month";
  private static final String TIME_UNIT_DAY = "Day";
  private static final String TIME_UNIT_DAY_OF_WEEK = "DayOfWeek";
  private static final String TIME_UNIT_HOUR = "Hour";
  private static final String TIME_UNIT_MINUTE = "Minute";
  private static final String TIME_UNIT_SECOND = "Second";
  private static final String TIME_UNIT_MILLIS = "Millis";
  
  
  private TimeWindowConverter() {
    
  }
  
  private static class Restriction {
    private Integer day;
    private Integer month;
    private Integer firstOrWhat;
    private Integer weekday;
    private Integer[] weekdays;
  }
  
  public static String generateReadableRecurringInterval(RestrictionBasedTimeWindow restrictionBasedTimeWindow) {
    if(restrictionBasedTimeWindow == null)
      return null;
    String type = null;
    Integer stride = null;
    Restriction restriction = new Restriction();
    
    List<? extends TimeUnit> timeUnits = restrictionBasedTimeWindow.getRestriction();
    for (TimeUnit timeUnit : timeUnits) {
      switch(timeUnit.getUnit()) {
        case TIME_UNIT_YEAR:
          type = TIME_UNIT_YEAR;
          stride = Integer.valueOf(timeUnit.getValue().substring(1)); // :1
          break;
        case TIME_UNIT_MONTH:
          if(timeUnit.getValue().contains(":")) {
            type = TIME_UNIT_MONTH;
            stride = parseMonth(timeUnit.getValue().substring(1));
          } else {
            restriction.month = parseMonth(timeUnit.getValue());
          }
          break;
        case TIME_UNIT_DAY:
          if(timeUnit.getValue().contains(":")) {
            type = TIME_UNIT_DAY;
            stride = Integer.valueOf(timeUnit.getValue().substring(1) );
          } else if(timeUnit.getValue().contains("#")) {
            String[] split = timeUnit.getValue().split("#");
            
            if ( split[0].contains("L"))
              restriction.firstOrWhat = 5 - ( Integer.valueOf(split[0]) - 1 );
            else
              restriction.firstOrWhat = Integer.valueOf(split[0]) - 1;
            
            if (restriction.firstOrWhat < 0 || restriction.firstOrWhat > 4 )
              throw new Error( "not supported" );
            
            restriction.weekday = parseWeekday(split[1]);
          } else {
            restriction.day = Integer.valueOf(timeUnit.getValue());
          }
          break;
        case TIME_UNIT_HOUR:
          if(timeUnit.getValue().contains(":")) {
            type = TIME_UNIT_HOUR;
            stride = Integer.valueOf(timeUnit.getValue().substring(1));
          }
          break;
        case TIME_UNIT_MINUTE:
          if(timeUnit.getValue().contains(":")) {
            type = TIME_UNIT_MINUTE;
            stride = Integer.valueOf(timeUnit.getValue().substring(1));
          }          
          break;
        case TIME_UNIT_SECOND:
          if(timeUnit.getValue().contains(":")) {
            type = TIME_UNIT_SECOND;
            stride = Integer.valueOf(timeUnit.getValue().substring(1));
          }
          break;
        case TIME_UNIT_MILLIS:
          if(timeUnit.getValue().contains(":")) {
            type = TIME_UNIT_MILLIS;
            stride = Integer.valueOf(timeUnit.getValue().substring(1));
          }
          break;
        
        case TIME_UNIT_DAY_OF_WEEK:
          if(timeUnit.getValue() != null) {
            type = TIME_UNIT_DAY_OF_WEEK;
            restriction.weekdays = parseWeekdays(timeUnit.getValue());
          }
          break;
      }
    }
    StringBuilder result = new StringBuilder();
    if(type == null)
      return "";
    switch(type) {
      case TIME_UNIT_MILLIS:
        result.append("Every ").append(stride).append(stride > 1 ? " miliseconds" : " millisecond");
        break;
      case TIME_UNIT_SECOND:
        result.append("Every ").append(stride).append(stride > 1 ? " seconds" : " second");
        break;
      case TIME_UNIT_MINUTE:
        result.append("Every ").append(stride).append(stride > 1 ? " minutes" : " minute");
        break;
      case TIME_UNIT_HOUR:
        result.append("Every ").append(stride).append(stride > 1 ? " hours" : " hour");
        break;
      case TIME_UNIT_DAY:
        result.append("Every ").append(stride).append(stride > 1 ? " days" : " day");
        break;
      case TIME_UNIT_DAY_OF_WEEK:
        result.append("Every week on");
        try {
          Integer[] days = restriction.weekdays;
          for (int j = 0; j < days.length; j++) {
            switch ( days[j] ) {
              case 1: result.append(" monday");    break;
              case 2: result.append(" tuesday");   break;
              case 3: result.append(" wednesday"); break;
              case 4: result.append(" thursday");  break;
              case 5: result.append(" friday");    break;
              case 6: result.append(" saturday");  break;
              case 7: result.append(" sunday");    break;
            }
            if (j+1 < days.length) {
              result.append(",");
            }
          }
        } catch (Exception ex) {
          // nothing
        }
        break;
      case TIME_UNIT_MONTH:
        result.append("Every ").append(stride).append(stride > 1 ? " months" : " month").append(" on");
        
        if (restriction.day != null) {
          result.append(" ").append(restriction.day + numberSuffix(restriction.day));
        } else if ( restriction.firstOrWhat != null && restriction.weekday != null) {
          switch ( restriction.firstOrWhat ) {
            case 0: result.append(" first");   break;
            case 1: result.append(" second");  break;
            case 2: result.append(" third");   break;
            case 3: result.append(" fourth");  break;
            case 4: result.append(" last");    break;
          }
          
          switch ( restriction.weekday ) {
            case 1: result.append(" monday");    break;
            case 2: result.append(" tuesday");   break;
            case 3: result.append(" wednesday"); break;
            case 4: result.append(" thursday");  break;
            case 5: result.append(" friday");    break;
            case 6: result.append(" saturday");  break;
            case 7: result.append(" sunday");    break;
          }
        }        
        break;
      case TIME_UNIT_YEAR:
        result.append("Every ").append(stride).append(stride > 1 ? " years" : " year").append(" on");
        if (restriction.day != null && restriction.month != null) {
          switch (restriction.month){
            case 1: result.append(" january");   break;
            case 2: result.append(" february");  break;
            case 3: result.append(" march");     break;
            case 4: result.append(" april");     break;
            case 5: result.append(" may");       break;
            case 6: result.append(" june");      break;
            case 7: result.append(" july");      break;
            case 8: result.append(" august");    break;
            case 9: result.append(" september"); break;
            case 10: result.append(" october");  break;
            case 11: result.append(" november"); break;
            case 12: result.append(" december"); break;
          }          
          result.append(" ").append(restriction.day + numberSuffix(restriction.day));
        } else if(restriction.firstOrWhat != null && restriction.weekday != null) {
          switch (restriction.firstOrWhat ) {
            case 0: result.append(" first");   break;
            case 1: result.append(" second");  break;
            case 2: result.append(" third");   break;
            case 3: result.append(" fourth");  break;
            case 4: result.append(" last");    break;
          }
          
          switch (restriction.weekday) {
            case 1: result.append(" monday");    break;
            case 2: result.append(" tuesday");   break;
            case 3: result.append(" wednesday"); break;
            case 4: result.append(" thursday");  break;
            case 5: result.append(" friday");    break;
            case 6: result.append(" saturday");  break;
            case 7: result.append(" sunday");    break;
          }
          
          result.append(" in");
          
          switch (restriction.month) {
            case 1: result.append(" january");   break;
            case 2: result.append(" february");  break;
            case 3: result.append(" march");     break;
            case 4: result.append(" april");     break;
            case 5: result.append(" may");       break;
            case 6: result.append(" june");      break;
            case 7: result.append(" july");      break;
            case 8: result.append(" august");    break;
            case 9: result.append(" september"); break;
            case 10: result.append(" october");  break;
            case 11: result.append(" november"); break;
            case 12: result.append(" december"); break;
          }
        }
        break;
    }
    return result.toString();
  }
  
  private static String numberSuffix(int num){
    switch (num) {
      case 1:
      case 21:
      case 31:
        return "st";
        
      case 2:
      case 22:
        return "nd";
        
      case 3:
      case 23:
        return "rd";
        
      default:
        return "th";
    }
  }
  
  private static Integer[] parseWeekdays(String day){
    Integer[] weekDays;
    String[] daySplit = day.split(",");
    if (daySplit.length > 1) {
      weekDays = new Integer[daySplit.length];
      for (int i = 0; i < daySplit.length; i++) {
        weekDays[i] = parseWeekday(daySplit[i]);
      }
    } else {
      weekDays = new Integer[] {parseWeekday(day)};
    }
    return weekDays;      
  }
  
  private static Integer parseWeekday(String day){
    switch ( day ) {
      case "MON": return 1;
      case "TUE": return 2;
      case "WED": return 3;
      case "THU": return 4;
      case "FRI": return 5;
      case "SAT": return 6;
      case "SUN": return 7;
      default:    return Integer.valueOf(day);
    }
  }
  
  private static Integer parseMonth(String month){
    switch ( month ) {
      case "JAN": return 1;
      case "FEB": return 2;
      case "MAR": return 3;
      case "APR": return 4;
      case "MAY": return 5;
      case "JUN": return 6;
      case "JUL": return 7;
      case "AUG": return 8;
      case "SEP": return 9;
      case "OCT": return 10;
      case "NOV": return 11;
      case "DEC": return 12;
      default:    return Integer.valueOf(month);
    }
  }
  
  public static TimeWindowDefinition convertTimeWindow(TimeWindow timeWindow, String timezoneId, boolean considerDst) {
    if(timeWindow == null)
      return null;
    if(timeWindow instanceof RestrictionBasedTimeWindow) {
      return createRestrictionBasedTimeWindow((RestrictionBasedTimeWindow) timeWindow, timezoneId, considerDst);
    } else {
      throw new UnsupportedOperationException("Could not recognize TimeWindowCreationParamters");
    }
  }
  
  private static RestrictionBasedTimeWindowDefinition createRestrictionBasedTimeWindow(RestrictionBasedTimeWindow rbtw, String timezoneid, boolean considerDST) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    
    for (int i = 0; i < rbtw.getRestriction().size(); i++) {
      sb.append(rbtw.getRestriction().get(i).getUnit())
        .append("=")
        .append(rbtw.getRestriction().get(i).getValue());
      if (i + 1 < rbtw.getRestriction().size()) {
        sb.append(";");
      }
    }
    sb.append("][");
    if (rbtw.getDuration() != null && !rbtw.getDuration().isEmpty()) {
      for (int i = 0; i < rbtw.getDuration().size(); i++) {
        sb.append(rbtw.getDuration().get(i).getUnit())
          .append("=")
          .append(rbtw.getDuration().get(i).getValue());
        if (i + 1 < rbtw.getDuration().size()) {
          sb.append(";");
        }
      }
    }else {
      sb.append("Second=0");
    }
    sb.append("]");
    return new RestrictionBasedTimeWindowDefinition(sb.toString(), timezoneid, considerDST);
  }
  
  public static TimeWindow convert(TimeWindowDefinition twd) {
    if (twd instanceof RestrictionBasedTimeWindowDefinition) {
      return convertRestrictionBasedTimeWindowDefinition((RestrictionBasedTimeWindowDefinition) twd);
    } else if( twd instanceof SimplePeriodicTimeWindowDefinition) {
      return convertSimplePeriodicTimeWindowDefinition((SimplePeriodicTimeWindowDefinition) twd);
    } else if( twd instanceof MultiTimeWindowDefinition ) {
      return convertMultiTimeWindow((MultiTimeWindowDefinition)twd);
    } else {
      throw new UnsupportedOperationException("Could not recognize TimeWindowCreationParamters");
    }
  }
  
  private static MultiTimeWindow convertMultiTimeWindow(MultiTimeWindowDefinition mtwd) {
    if(mtwd == null)
      return null;
    List<TimeWindow> timeWindows = new ArrayList<>(mtwd.size());
    for (int i = 0; i < mtwd.size(); i++) {
      timeWindows.add(convert(mtwd.getDefinition(i)));
    }
    return new MultiTimeWindow(timeWindows);
  }
  
  private static SimpleTimeWindow convertSimplePeriodicTimeWindowDefinition(SimplePeriodicTimeWindowDefinition sptwd) {
    return new SimpleTimeWindow(sptwd.getModAddString(), String.valueOf(sptwd.getDuration()));
  }
  
  private static TimeWindow convertRestrictionBasedTimeWindowDefinition(RestrictionBasedTimeWindowDefinition rbtwd) {
    String ruleAndDuration = rbtwd.getRuleAndDuration();
    String rulePart;
    String durationPart;
    if (ruleAndDuration.contains("Restriction[")) {
      int restrictionStart = ruleAndDuration.indexOf("Restriction[") + "Restriction[".length();
      int restrictionEnd = ruleAndDuration.indexOf(']', restrictionStart);
      rulePart = ruleAndDuration.substring(restrictionStart, restrictionEnd);
      int durationStart = ruleAndDuration.indexOf("Duration[") + "Duration[".length();
      int durationEnd = ruleAndDuration.indexOf(']', durationStart);
      durationPart = ruleAndDuration.substring(durationStart, durationEnd);
    } else {
      String[] ruleDurationSplit = ruleAndDuration.split("\\]\\[");
      if (ruleDurationSplit.length == 2) {
        rulePart = ruleDurationSplit[0].substring(1);
        durationPart = ruleDurationSplit[1].substring(0, ruleDurationSplit[1].length() - 1);
      } else if (ruleAndDuration.startsWith("[") && ruleAndDuration.endsWith("]")) {
        durationPart = "Millis=1";
        rulePart = ruleAndDuration.substring(1, ruleAndDuration.length() - 1);
      } else {
        return null;
      }
    }
    return new RestrictionBasedTimeWindow(parseTimeUnitValues(durationPart), parseTimeUnitValues(rulePart));
  }
  
  private static List<TimeUnit> parseTimeUnitValues(String unitValues) {
    if (unitValues.contains(";")) {
      String[] unitValuesSplit = unitValues.split(";");
      List<TimeUnit> units = new ArrayList<>(unitValuesSplit.length);
      for (int i = 0; i < unitValuesSplit.length; i++) {
        units.add(parseTimeUnitValue(unitValuesSplit[i]));
      }
      return units;
    } else {
      return Arrays.asList(parseTimeUnitValue(unitValues));
    }
  }
  
  
  private static TimeUnit parseTimeUnitValue(String unitValues) {
    String[] unitValuesSplit = unitValues.split("=");
    String unit = "";
    String value = "";
    if(unitValuesSplit.length > 0) {
      unit = unitValuesSplit[0];
    }
    if(unitValuesSplit.length > 1) {
      value = unitValuesSplit[1];
    }
    return new TimeUnit(unit, value);
  }
  
}
