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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;



public interface TimeControlPart {
  
  /**
   * berechnet den nächsten zeitpunkt (in richtung stepping), der bzgl der schrittweite am nähesten an "reference" liegt.
   * 
   * dazu braucht man einen stützpunkt, an dem man sich orientieren kann (aValidTime).
   */
  DateTime calculate(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping);
  
  boolean validate(DateTime aTime, DateTime aValidTime);
  
  TimeControlPart getNext();
  
  void setNext(TimeControlPart next);
  
  boolean hasNext();
  
  TimeControlPart getPrevious();
  
  void setPrevious(TimeControlPart previous);
  
  boolean hasPrevious();
  
  JodaTimeControlUnit getUnit();
  
  DateTime initialize(DateTime now);
  
  
  public static enum Stepping {
    AFTER, AFTER_OR_EQUAL, BEFORE, BEFORE_OR_EQUAL;
  }
  
  public static abstract class AbstractTimeControlPart implements TimeControlPart {
    
    protected final JodaTimeControlUnit unit;
    protected TimeControlPart next;
    protected TimeControlPart previous;
    
    public AbstractTimeControlPart(JodaTimeControlUnit unit) {
      this.unit = unit;
    }
    
    
    public JodaTimeControlUnit getUnit() {
      return unit;
    }
    
    public void setNext(TimeControlPart next) {
      this.next = next;
      next.setPrevious(this);
    }
    
    public TimeControlPart getNext() {
      return next;
    }
    
    public boolean hasNext() {
      return next != null;
    }
    
    public void setPrevious(TimeControlPart previous) {
      this.previous = previous;
    }
    
    public TimeControlPart getPrevious() {
      return previous;
    }
    
    public boolean hasPrevious() {
      return previous != null;
    }
    
    protected DateTime overflow(DateTime toBeOverflown) {
      return toBeOverflown.withFieldAdded(unit.getNextHigherUnit().getDateTimeFieldType().getDurationType(), 1);
    }
    
    
    protected DateTime underflow(DateTime toBeUnderflown) {
      return toBeUnderflown.withFieldAdded(unit.getNextHigherUnit().getDateTimeFieldType().getDurationType(), -1);
    }
    
    
    protected int getUnitMaxValue(DateTime from) {
      return unit.getProperty(from).getMaximumValue();
    }
    
    
    protected boolean checkHigherUnitOverflow(DateTime previous, DateTime afterwards) {
      JodaTimeControlUnit higherUnit = unit.getNextHigherUnit();
      if (higherUnit == null) {
        return false;
      } else {
        return checkDifferentFieldValues(higherUnit, previous, afterwards);
      }
    }
    
    
    protected boolean checkOverflow(DateTime previous, DateTime afterwards) {
      JodaTimeControlUnit current = unit;
      while (current != null) {
        if (checkDifferentFieldValues(current, previous, afterwards)) {
          return true;
        } else {
          current = current.getNextHigherUnit();
        }
      }
      return false;
    }
    
    
    protected boolean checkDifferentFieldValues(JodaTimeControlUnit unit, DateTime previous, DateTime afterwards) {
      return previous.get(unit.getDateTimeFieldType()) != afterwards.get(unit.getDateTimeFieldType());
    }
    
    
    
    public DateTime initialize(DateTime now) {
      DateTime init = initializeSelf(now);
      if (next != null) {
        init = next.initialize(init);
      }
      return init;
    }
    
    
    protected abstract DateTime initializeSelf(DateTime now);
    
    protected Stepping stepDown(Stepping stepping) {
      switch (stepping) {
        case AFTER :
          return Stepping.AFTER_OR_EQUAL;
        case BEFORE :
          return Stepping.BEFORE_OR_EQUAL;
        default :
          return stepping;
      }
    }
    
    protected Stepping stepUp(Stepping stepping) {
      switch (stepping) {
        case AFTER_OR_EQUAL :
          return Stepping.AFTER;
        case BEFORE_OR_EQUAL :
          return Stepping.BEFORE;
        default :
          return stepping;
      }
    }
    
    
    public DateTime calculate(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      // send the interimResult once through the whole pipeline to get a more approximate value for the actual calculation
      Stepping lowerStepping = stepDown(stepping);
      if (hasNext()) {
        interimResult = getNext().calculate(reference, aValidTime, interimResult, lowerStepping);
      }
      interimResult = calculateSelf(reference, aValidTime, interimResult, stepping);
      Stepping higherStepping = stepUp(stepping);
      switch (higherStepping) {
        case AFTER_OR_EQUAL :
        case AFTER :
          while (interimResult.isBefore(reference)) {
            interimResult = calculateSelf(reference, aValidTime, interimResult, higherStepping);
          }
          break;
        case BEFORE_OR_EQUAL :
        case BEFORE :
          while (interimResult.isAfter(reference)) {
            interimResult = calculateSelf(reference, aValidTime, interimResult, higherStepping);
          }
          break;
      }
      while (hasNext() && !getNext().validate(interimResult, aValidTime)) {
        interimResult = calculateSelf(reference, aValidTime, interimResult, higherStepping);
      }
      
      return interimResult;
    }
    
    
    public abstract DateTime calculateSelf(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping);
    
    
    public boolean validate(DateTime aTime, DateTime aValidTime) {
      if (!validateSelf(aTime, aValidTime)) {
        return false;
      }
      return validateNext(aTime, aValidTime);
    }
    
    
    public abstract boolean validateSelf(DateTime aTime, DateTime aValidTime);

    public boolean validateNext(DateTime aTime, DateTime aValidTime) {
      if (hasNext()) {
        return getNext().validate(aTime, aValidTime);
      } else {
        return true;
      }
    }

    
  }
  
  
  public static class StaticTimeControlPart extends AbstractTimeControlPart {

    private int value;
    private boolean toLast;
    
    public StaticTimeControlPart(JodaTimeControlUnit unit, int value, boolean toLast) {
      super(unit);
      this.value = value;
      this.toLast = toLast;
      if (toLast && value <= 0) {
        throw new RuntimeException("should not happen");
      }
    }

    @Override
    protected DateTime initializeSelf(DateTime now) {
      if (value < 0) {
        value = now.get(unit.getDateTimeFieldType());
      }
      while (value > getUnitMaxValue(now)) {
        now = overflow(now);
      }
      return now.withField(unit.getDateTimeFieldType(), getAdjustValue(now));
    }
    
    
    private int getAdjustValue(DateTime aTime) {
      if (toLast) {
        return getUnitMaxValue(aTime) - value + 1;
      } else {
        return value;
      }
    }
    

    @Override
    public DateTime calculateSelf(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      int adjustedValue = getAdjustValue(interimResult);
      boolean maxValueFlow = false;
      while (adjustedValue > getUnitMaxValue(interimResult)) {
        maxValueFlow = true;
        switch (stepping) {
          case AFTER :
          case AFTER_OR_EQUAL :
            interimResult = overflow(interimResult);
            break;
          case BEFORE :
          case BEFORE_OR_EQUAL :
            interimResult = underflow(interimResult);
            break;
          default :
            break;
        }
      }
      interimResult = interimResult.withField(unit.getDateTimeFieldType(), adjustedValue);
      switch (stepping) {
        case AFTER :
          if (!maxValueFlow) {
            interimResult = overflow(interimResult);
          }
          while (interimResult.isBefore(reference)) { // on _OR_EQUAL as well?
            interimResult = overflow(interimResult);
          }
          break;
        case BEFORE :
          if (!maxValueFlow) {
            interimResult = underflow(interimResult);
          }
          while (interimResult.isAfter(reference)) { // on _OR_EQUAL as well?
            interimResult = underflow(interimResult);
          }
          break;
        default :
          break;
      }
      return interimResult;
    }

    @Override
    public boolean validateSelf(DateTime aTime, DateTime aValidTime) {
      if (!(aTime.get(unit.getDateTimeFieldType()) == getAdjustValue(aTime))) {
        if (unit == JodaTimeControlUnit.YEAR) {
          throw new TimeViolationException(); // this could happen with whiteValues as well
        } else {
          return false;
        }
      } else {
        return true;
      }
    }
    
  }
  
  
  public static class PeriodicTimeControlPart extends AbstractTimeControlPart {
    
    private final int increment;
    private int startValue = -1;
    
    public PeriodicTimeControlPart(JodaTimeControlUnit unit, int startValue, int increment) {
      super(unit);
      this.increment = increment;
      this.startValue = startValue;
    }

    public int getIncrement() {
      return increment;
    }

    @Override
    protected DateTime initializeSelf(DateTime now) {
      if (startValue >= 0) {
        return now.withField(unit.getDateTimeFieldType(), startValue);
      } else {
        return now;
      }
    }


    public boolean validateSelf(DateTime aTime, DateTime aValidTime) {
      DateTime calculation = aValidTime;
      DateTime calculationOffset = aValidTime;
      if (calculation.isBefore(aTime)) {
        while (calculation.isBefore(aTime)) {
          calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), increment);
        }
        calculationOffset = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), -increment); 
      } else {
        while (calculation.isAfter(aTime)) {
          calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), -increment);
        }
        calculationOffset = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), increment);
      }
      return !checkOverflow(aTime, calculation) || !checkOverflow(aTime, calculationOffset);
    }



    public DateTime calculate(DateTime target, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      DateTime calculation = aValidTime;
      int max = (Integer.MAX_VALUE / 2 / increment) * increment; //max muss durch increment teilbar sein
      switch (stepping) {
        case AFTER_OR_EQUAL :
        case AFTER :
          if (calculation.isAfter(target)) {
            long units = unit.between(target, calculation);   
            //nächstkleinere (oder gleiche) zahl, die durch increment teilbar ist 
            units = (units/increment) * increment;
            while (units > 0) {
              int k = (int) Math.min(units, max);
              units -= k;
              calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), -k);
            }
          } else if (calculation.isBefore(target)) {
            long units = unit.between(calculation, target);            
            //nächstgrößere (oder gleiche) zahl, die durch increment teilbar ist 
            units = (units/increment) * increment + (units % increment == 0 ? 0 : increment);            
            while (units > 0) {
              int k = (int) Math.min(units, max);
              units -= k;
              calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), k);
            }
          }
          if (stepping == Stepping.AFTER && !checkDifferentFieldValues(unit, calculation, target)) {
            calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), increment);
          }
          while (!validateNext(calculation, aValidTime)) {
            calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), increment);
          }
          break;
        case BEFORE_OR_EQUAL :
        case BEFORE :
          if (calculation.isBefore(target)) {
            long units = unit.between(calculation, target);            
            units = (units/increment) * increment;
            while (units > 0) {
              int k = (int) Math.min(units, max);
              units -= k;
              calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), k);
            }
          } else if (calculation.isAfter(target)) {
            long units = unit.between(target, calculation);            
            units = (units/increment) * increment + (units % increment == 0 ? 0 : increment);
            while (units > 0) {
              int k = (int) Math.min(units, max);
              units -= k;
              calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), -k);
            }
          }
          if (stepping == Stepping.BEFORE && !checkDifferentFieldValues(unit, calculation, target)) {
            calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), - increment);
          }
          while (!validateNext(calculation, aValidTime)) {
            calculation = calculation.withFieldAdded(unit.getDateTimeFieldType().getDurationType(), - increment);
          }
          break;
      }
      return calculation;
    }
    
    public DateTime calculateSelf(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      //ntbd, calculate is overriden
      return null;
    }


  }
  
  
  public static class WhiteListTimeControlPart extends AbstractTimeControlPart {

    private final int[] allowedValues;
    private final int[] allowedNegativeValues;
    private int[] allWhiteValues;
    
    public WhiteListTimeControlPart(JodaTimeControlUnit unit, int[] values, int[] negValues) {
      super(unit);
      int[] defensiveCopy = new int[values.length];
      System.arraycopy(values, 0, defensiveCopy, 0, values.length);
      Arrays.sort(defensiveCopy);
      allowedValues = defensiveCopy;
      int[] negDefensiveCopy = new int[negValues.length];
      System.arraycopy(negValues, 0, negDefensiveCopy, 0, negValues.length);
      Arrays.sort(negDefensiveCopy);
      allowedNegativeValues = negDefensiveCopy;
    }

    
    protected int[] getAllWhiteValues(DateTime nextExecution) {
      int[] allWhiteValues = new int[allowedValues.length + allowedNegativeValues.length];
      System.arraycopy(allowedValues, 0, allWhiteValues, 0, allowedValues.length);
      int maxValue = getUnitMaxValue(nextExecution);
      for (int i = 0; i < allowedNegativeValues.length; i++) {
        allWhiteValues[allowedValues.length + i] = maxValue - allowedNegativeValues[i];
      }
      Arrays.sort(allWhiteValues);
      return allWhiteValues;
    }
    
    
    @Override
    protected DateTime initializeSelf(DateTime now) {
      if (allWhiteValues == null) {
        allWhiteValues = getAllWhiteValues(now);
      }
      int index;
      if (allWhiteValues.length == 1) {
        index = 0;
      } else {
        index = Arrays.binarySearch(allWhiteValues, now.get(unit.getDateTimeFieldType()));
        if (index < 0) {
          index = Math.abs(index) - 1;
        }
        if (index >= allWhiteValues.length) {
          index--;
        }
      }
      return now.withField(unit.getDateTimeFieldType(), getAllWhiteValues(now)[index]);
    }


    @Override
    public boolean validateSelf(DateTime aTime, DateTime aValidTime) {
      int[] allWhiteValues = getAllWhiteValues(aTime);
      return Arrays.binarySearch(allWhiteValues, aTime.get(unit.getDateTimeFieldType())) >= 0;
    }
    
    @Override
    public DateTime calculateSelf(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      int[] allWhiteValues = getAllWhiteValues(interimResult);
      int index = Arrays.binarySearch(allWhiteValues, interimResult.get(unit.getDateTimeFieldType()));
      if (index < 0) {
        index = Math.abs(index) - 1;
      }
      switch (stepping) {
        case AFTER :
          index++;
        case AFTER_OR_EQUAL :
          if (index >= allWhiteValues.length) {
            index = 0;
            interimResult = overflow(interimResult);
          }
          break;
        case BEFORE :
          index--;
        case BEFORE_OR_EQUAL :
          if (index < 0 || index >= allWhiteValues.length) {
            index = allWhiteValues.length - 1;
            interimResult = underflow(interimResult);
          }
          break;
      }
      interimResult = interimResult.withField(unit.getDateTimeFieldType(), allWhiteValues[index]);
      return interimResult;
    }

    
  }
  
  
  public static class DynamicWhiteRange extends WhiteListTimeControlPart {

    private final Map<Integer, Integer> dynamicRanges;
    
    public DynamicWhiteRange(JodaTimeControlUnit unit, int[] values, int[] negValues, Map<Integer, Integer> dynamicRanges) {
      super(unit, values, negValues);
      this.dynamicRanges = dynamicRanges;
    }
    
    
    @Override
    protected int[] getAllWhiteValues(DateTime nextExecution) {
      int[] staticWhiteValues = super.getAllWhiteValues(nextExecution);
      Set<Integer> dynamicValues = new HashSet<Integer>();
      int maxValue = getUnitMaxValue(nextExecution);
      for (Entry<Integer, Integer> entry : dynamicRanges.entrySet()) {
        int rangeStart = entry.getKey();
        int rangeEnd = maxValue - entry.getValue();
        while (rangeStart <= rangeEnd) {
          dynamicValues.add(rangeStart++);
        }
      }
      int allWhiteValues[] = new int[staticWhiteValues.length + dynamicValues.size()];
      System.arraycopy(staticWhiteValues, 0, allWhiteValues, 0, staticWhiteValues.length);
      Integer[] dynamicArray = dynamicValues.toArray(new Integer[dynamicValues.size()]);
      for (int i = 0; i < dynamicArray.length; i++) {
        allWhiteValues[staticWhiteValues.length + i] = dynamicArray[i].intValue();
      }
      Arrays.sort(allWhiteValues);
      return allWhiteValues;
    }

    
  }
  
 
  public static class DayInWeekControlPart extends WhiteListTimeControlPart {

    
    public DayInWeekControlPart(int value, boolean toLastDay) {
      super(JodaTimeControlUnit.DAY, getArrayForValue(value, toLastDay), getNegArrayForValue(value, toLastDay));
    }
    
    private static int[] getArrayForValue(int value, boolean toLastDay) {
      if (toLastDay) {
        return new int[0];
      } else {
        return new int[] {value};
      }
    }
    
    private static int[] getNegArrayForValue(int value, boolean toLastDay) {
      if (toLastDay) {
        return new int[] {value};
      } else {
        return new int[0];
      }
    }

    
    private DateTime adjust(DateTime nextExecution) {
      int offset = 0;
      if (nextExecution.getDayOfWeek() == DateTimeConstants.SATURDAY) {
        if (nextExecution.getDayOfMonth() == 1) {
          offset = 2;
        } else {
          offset = -1;
        }
      } else if (nextExecution.getDayOfWeek() == DateTimeConstants.SUNDAY) {
        if (nextExecution.getDayOfMonth() + 1 >= nextExecution.dayOfMonth().getMaximumValue()) {
          offset = -2;
        } else {
          offset = 1;
        }
      }
      return nextExecution.withField(unit.getDateTimeFieldType(), nextExecution.getDayOfMonth() + offset);
    }
    
    private DateTime unadjust(DateTime aTime) {
      int offset = 0;
      if (aTime.getDayOfWeek() == DateTimeConstants.FRIDAY) {
        if (aTime.getDayOfMonth() + 1 >= aTime.dayOfMonth().getMaximumValue()) {
          return aTime;
        }
        offset = 1;
      } else if (aTime.getDayOfWeek() == DateTimeConstants.MONDAY) {
        if (aTime.getDayOfMonth() - 1 <= 0) {
          return aTime;
        }
        offset = -1;
      } else {
        return aTime;
      }
      return aTime.withField(unit.getDateTimeFieldType(), aTime.getDayOfMonth() + offset);
    }
    
    @Override
    protected DateTime initializeSelf(DateTime now) {
      return adjust(super.initializeSelf(now));
    }
    
    
    @Override
    public boolean validateSelf(DateTime aTime, DateTime aValidTime) {
      if (!super.validateSelf(aTime, aValidTime)) {
        DateTime adjustedTime = unadjust(aTime);
        return super.validateSelf(adjustedTime, aValidTime);
      } else {
        return aTime.getDayOfWeek() != DateTimeConstants.SATURDAY && aTime.getDayOfWeek() != DateTimeConstants.SUNDAY; 
      }
    }
    
    
    @Override
    public DateTime calculateSelf(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      interimResult = unadjust(interimResult);
      interimResult = super.calculateSelf(reference, aValidTime, interimResult, stepping);
      return adjust(interimResult);
    }
    
  }
  
  
  public static class WeekDayInMonth extends AbstractTimeControlPart {

    private final int dayOfWeekValue;
    private final int value;
    
    public WeekDayInMonth(int dayValue, int value) {
      super(JodaTimeControlUnit.DAY);
      this.dayOfWeekValue = dayValue;
      this.value = value;
    }

    
    private List<Integer> getDaysOfMonth(DateTime nextExecution) {
      List<Integer> allCorrespondingDaysOfWeek = new ArrayList<Integer>();
      DateTime startDate = nextExecution.withDayOfMonth(1).withDayOfWeek(dayOfWeekValue); // withDayOfWeek might have underflown the Month
      if (startDate.getMonthOfYear() != nextExecution.getMonthOfYear()) {
        startDate = startDate.plusWeeks(1);
      }
      int currentMonth = nextExecution.getMonthOfYear();
      DateTime currentDate = startDate;
      while (currentDate.getMonthOfYear() == currentMonth) {
        allCorrespondingDaysOfWeek.add(currentDate.getDayOfMonth());
        currentDate = currentDate.plusWeeks(1);
      }
      return allCorrespondingDaysOfWeek;
    }

    
    @Override
    protected DateTime initializeSelf(DateTime now) {
      List<Integer> daysOfMonth = getDaysOfMonth(now);
      int adjustedValue = value;
      if (value <= 0) {
        adjustedValue = daysOfMonth.size() - value;
      }
      return now.withDayOfMonth(adjustedValue);
    }



    @Override
    public DateTime calculateSelf(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      interimResult = calculateSelfOnce(interimResult);
      switch (stepping) {
        case AFTER_OR_EQUAL :
          if (!interimResult.isBefore(reference)) {
            break;
          }
        case AFTER :
          interimResult = overflow(interimResult);
          interimResult = calculateSelfOnce(interimResult);
          break;
        case BEFORE_OR_EQUAL :
          if (!interimResult.isAfter(reference)) {
            break;
          }
        case BEFORE :
          interimResult = underflow(interimResult);
          interimResult = calculateSelfOnce(interimResult);
          break;
      }
      return interimResult;
    }
    
    
    private DateTime calculateSelfOnce(DateTime interimResult) {
      List<Integer> daysOfMonth = getDaysOfMonth(interimResult);
      int adjustedValue = value;
      if (value <= 0) {
        adjustedValue = daysOfMonth.size() + value;
      } else {
        adjustedValue--;
      }
      return interimResult.withDayOfMonth(daysOfMonth.get(adjustedValue));
    }


    @Override
    public boolean validateSelf(DateTime aTime, DateTime aValidTime) {
      List<Integer> daysOfMonth = getDaysOfMonth(aTime);
      int adjustedValue = value;
      if (value <= 0) {
        adjustedValue = daysOfMonth.size() + value;
      } else {
        adjustedValue--;
      }
      return aTime.getDayOfMonth() == daysOfMonth.get(adjustedValue).intValue();
      }
    
  }
  
  
  public static class DayOfWeekControlPart extends AbstractTimeControlPart {

    private TimeControlPart innerDayPart;
    private int[] values;
    
    public DayOfWeekControlPart(int[] values) {
      super(JodaTimeControlUnit.DAY_OF_WEEK);
      this.values = values;
    }

    
    public void setInnerDayPart(TimeControlPart innerDayPart) {
      if (innerDayPart == null) {
        this.innerDayPart = new TimeControlPart() {
          public JodaTimeControlUnit getUnit() {
            return null;
          }
          public TimeControlPart getNext() {
            return null;
          }
          public void setNext(TimeControlPart next) {
            
          }
          public boolean hasNext() {
            return false;
          }
          public DateTime initialize(DateTime now) {
            return now;
          }
          public DateTime calculate(DateTime reference, DateTime aValidTime, DateTime interimResult,
                                    Stepping stepping) {
            switch (stepping) {
              case AFTER :
                return interimResult.plusDays(1);
              case BEFORE :
                return interimResult.minusDays(1);
              default :
                return interimResult;
            }
          }
          public boolean validate(DateTime aTime, DateTime aValidTime) {
            return true;
          }
          public TimeControlPart getPrevious() {
            return null;
          }
          public void setPrevious(TimeControlPart previous) {
            
          }
          public boolean hasPrevious() {
            return false;
          }
        };
      } else {
        this.innerDayPart = innerDayPart;
      }
    }


    @Override
    protected DateTime initializeSelf(DateTime now) {
      DateTime init = innerDayPart.initialize(now);
      while (Arrays.binarySearch(values, init.get(unit.getDateTimeFieldType())) < 0) {
        init = innerDayPart.initialize(init.plusDays(1));
      }
      return init;
    }

    
    @Override
    public boolean validateSelf(DateTime aTime, DateTime aValidTime) {
      if (innerDayPart.validate(aTime, aValidTime)) {
        return Arrays.binarySearch(values, aTime.get(unit.getDateTimeFieldType())) >= 0; 
      } else {
        return false;
      }
    }
    

    @Override
    public DateTime calculateSelf(DateTime reference, DateTime aValidTime, DateTime interimResult, Stepping stepping) {
      DateTime calculation = innerDayPart.calculate(reference, aValidTime, interimResult, stepping);
      if (stepping == Stepping.AFTER_OR_EQUAL) {
        stepping = Stepping.AFTER;
      }
      if (stepping == Stepping.BEFORE_OR_EQUAL) {
        stepping = Stepping.BEFORE;
      }
      while (!validate(calculation, aValidTime)) {
        calculation = innerDayPart.calculate(reference, aValidTime, calculation, stepping);
      }
      return calculation;
    }
  }
    
  
  public static class TimeViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
  }
  

}
