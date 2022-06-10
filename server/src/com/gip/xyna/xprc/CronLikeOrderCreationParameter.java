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
package com.gip.xyna.xprc;



import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter.CronLikeOrderCreationParameterBuilder.Type;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.JodaTimeControlUnit;


public class CronLikeOrderCreationParameter extends XynaOrderCreationParameter {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(CronLikeOrderCreationParameter.class);

  private enum TimeUnit {
    MILLISECONDS("ms"), SECONDS("s"), MINUTES("m"), HOURS("h"), DAYS("d");

    private String unitSuffix;


    private TimeUnit(String suffix) {
      this.unitSuffix = suffix;
    }


    public static TimeUnit fromUnit(String unit) {
      for (TimeUnit e : values()) {
        if (e.unitSuffix.equals(unit)) {
          return e;
        }
      }

      return null;
    }
  };


  private static Pattern intervalPattern = Pattern.compile("(\\d+)(|ms|s|m|h|d)");
  private static Pattern datePattern = Pattern
      .compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d*))?");

  private static final long serialVersionUID = -4336481768584885822L;

  private String label;
  //Startzeitpunkt und Intervall zwischen zwei Events für periodische Vorgänge
  private Long startTime;
  private String originTimeZoneID;
  private Long interval;
  private String calendarDefinition;
  private boolean executeImmediately;
  private Boolean considerDaylightSaving;
  private Boolean enabled;
  private OnErrorAction onError;
  private Long rootOrderId;
  
  //TODO diese customfelder sind hier irgendwie redundant zu den customfeldern in der oberklasse.
  //evtl will man aber unterscheiden zwischen customfeldern für die clo-tabelle und customfeldern für die gespawnten aufträge
  //dann benötigt man einen speicherort... derzeit werden die in der gui angegebenen customfelder für beides verwendet.
  private String cronLikeOrderCustom0;
  private String cronLikeOrderCustom1;
  private String cronLikeOrderCustom2;
  private String cronLikeOrderCustom3;


  protected CronLikeOrderCreationParameter(DestinationKey dk, GeneralXynaObject... inputPayload) {
    super(dk, inputPayload);
  }
  
  public CronLikeOrderCreationParameter(DestinationKey dk, Long startTime, Long interval,
                                        GeneralXynaObject... inputPayload) {
    this(dk, startTime, null, interval, false, null, true, OnErrorAction.DISABLE, null, null,
         null, null, inputPayload);
  }


  @Deprecated
  public CronLikeOrderCreationParameter(DestinationKey dk, Long startTime, Long interval, Long rootOrderId,
                                        GeneralXynaObject... inputPayload) {
    this(dk, startTime, null, interval, false, rootOrderId, true, OnErrorAction.DISABLE, null,
         null, null, null, inputPayload);
  }


  @Deprecated
  public CronLikeOrderCreationParameter(DestinationKey dk, Long startTime, Long interval, Long rootOrderId,
                                        Boolean enabled, OnErrorAction onError, GeneralXynaObject... inputPayload) {
    this(dk, startTime, null, interval, false, rootOrderId, enabled, onError, null, null, null,
         null, inputPayload);
  }


  public CronLikeOrderCreationParameter(DestinationKey dk, Long startTime, String timeZoneID, Long interval,
                                        Boolean useDST, Long rootOrderId, Boolean enabled, OnErrorAction onError,
                                        String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3,
                                        GeneralXynaObject... inputPayload) {
    super(dk, inputPayload);
    setCustom0(cloCustom0);
    setCustom1(cloCustom1);
    setCustom2(cloCustom2);
    setCustom3(cloCustom3);
    this.label = dk.getOrderType();
    setStartTime(startTime);
    if (timeZoneID == null) {
      this.originTimeZoneID = Constants.DEFAULT_TIMEZONE;
    } else {
      this.originTimeZoneID = timeZoneID;
    }
    if (useDST == null) {
      this.considerDaylightSaving = false;
    } else {
      this.considerDaylightSaving = useDST;
    }
    this.enabled = enabled;
    this.onError = onError;
    this.rootOrderId = rootOrderId;
    this.cronLikeOrderCustom0 = cloCustom0;
    this.cronLikeOrderCustom1 = cloCustom1;
    this.cronLikeOrderCustom2 = cloCustom2;
    this.cronLikeOrderCustom3 = cloCustom3;

    setInterval(interval);
  }


  @Deprecated
  public CronLikeOrderCreationParameter(String orderType, Long startTime, Long interval,
                                        GeneralXynaObject... inputPayload) {
    this(orderType, orderType, startTime, null, interval, false, null, true,
         OnErrorAction.DISABLE, null, null, null, null, inputPayload);
  }


  public CronLikeOrderCreationParameter(String orderType, Long startTime, String timeZoneID, Long interval,
                                        Boolean useDST, String cloCustom0, String cloCustom1, String cloCustom2,
                                        String cloCustom3, GeneralXynaObject... inputPayload) {
    this(orderType, orderType, startTime, timeZoneID, interval, useDST, null, true, OnErrorAction.DISABLE, cloCustom0,
         cloCustom1, cloCustom2, cloCustom3, inputPayload);
  }


  @Deprecated
  public CronLikeOrderCreationParameter(String label, String orderType, Long startTime, Long interval, Boolean enabled,
                                        OnErrorAction onError, GeneralXynaObject... inputPayload) {
    this(label, orderType, startTime, null, interval, false, null, enabled, onError, null, null,
         null, null, inputPayload);
  }


  public CronLikeOrderCreationParameter(String label, String orderType, Calendar startTimeWithTimeZone, Long interval,
                                        Boolean useDST, Boolean enabled, OnErrorAction onError, String cloCustom0,
                                        String cloCustom1, String cloCustom2, String cloCustom3,
                                        GeneralXynaObject... inputPayload) {
    this(label, orderType, startTimeWithTimeZone.getTimeInMillis(), startTimeWithTimeZone.getTimeZone().getID(),
         interval, useDST, null, enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, inputPayload);
  }


  public CronLikeOrderCreationParameter(String label, String orderType, Long startTime, String timeZoneID,
                                        Long interval, Boolean useDST, Boolean enabled, OnErrorAction onError,
                                        String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3,
                                        GeneralXynaObject... inputPayload) {
    this(label, orderType, startTime, timeZoneID, interval, useDST, null, enabled, onError, cloCustom0, cloCustom1,
         cloCustom2, cloCustom3, inputPayload);
  }


  public CronLikeOrderCreationParameter(String label, String orderType, Long startTime, Long interval,
                                        Long rootOrderId, Boolean enabled, OnErrorAction onError,
                                        GeneralXynaObject... inputPayload) {
    this(label, orderType, startTime, null, interval, false, rootOrderId, enabled, onError, null,
         null, null, null, inputPayload);
  }


  public CronLikeOrderCreationParameter(String label, String orderType, Long startTime, String timeZoneID,
                                        Long interval, Boolean useDST, Long rootOrderId, Boolean enabled,
                                        OnErrorAction onError, String cloCustom0, String cloCustom1, String cloCustom2,
                                        String cloCustom3, GeneralXynaObject... inputPayload) {
    this(new DestinationKey(orderType), startTime, timeZoneID, interval, useDST, rootOrderId, enabled, onError,
         cloCustom0, cloCustom1, cloCustom2, cloCustom3, inputPayload);
    this.label = label; // overwrite label
  }


  public void setLabel(String label) {
    this.label = label;
  }
  
  public String getLabel() {
    return label;
  }
  
  /**
   * Setzt die Startzeit. Wird null oder 0 übergeben, so 
   * wird die startTime auf System.currentTimeMillis() und 
   * executeImmediately auf true gesetzt.
   */
  public void setStartTime(Long startTime) {
    if (startTime == null || 
        startTime.longValue() == 0) {
      this.startTime = System.currentTimeMillis();
      this.executeImmediately = true;
    } else {
      this.startTime = startTime;
    }
  }


  public Long getStartTime() {
    return startTime;
  }
  
  
  public Boolean getConsiderDaylightSaving() {
    return considerDaylightSaving;
  }
  

  public String getTimeZoneID() {
    return originTimeZoneID;
  }
  
  
  public void setOriginTimeZoneID(String timeZoneID) {
    if (timeZoneID == null) {
      originTimeZoneID = Constants.DEFAULT_TIMEZONE;
    } else {
      // FIXME: we need to perform some checks for validating the input, don't we?
      originTimeZoneID = timeZoneID;
    }
  }
  
  
  public String getOriginTimeZoneID() {
    return originTimeZoneID;
  }
  
  /**
   * Setzt das Intervall. Dabei wird auch die calendarDefinition neu berechnet,
   * falls ein neuer Wert größer als 0 übergeben wird.
   * @param interval
   */
  public void setInterval(Long interval) {
    if (interval != null) {
      if (interval < 0) {
        throw new IllegalArgumentException("interval must be positive (got " + interval + ")");
      }
      if (interval != 0 && !interval.equals(this.interval)) {
        //calendarDefinition neu berechnen, falls sich das Intervall geändert hat
        this.calendarDefinition = generateCalendarDefinition(interval);
      }
      this.interval = interval;
    } else {
      this.interval = 0L;
    }
  }


  public Long getInterval() {
    return interval;
  }

  public void setCalendarDefinition(String calendarDefinition) {
    this.calendarDefinition = calendarDefinition;
  }

  public String getCalendarDefinition() {
    if (calendarDefinition == null || calendarDefinition.length() == 0) {
      if (interval != null && interval > 0) {
        calendarDefinition = generateCalendarDefinition(interval);
      }
    }
    return calendarDefinition;
  }

  public void setExecuteImmediately(boolean executeImmediately) {
    this.executeImmediately = executeImmediately;
  }
  
  
  public boolean executeImmediately() {
    return executeImmediately;
  }

  public String getCronLikeOrderCustom0() {
    return cronLikeOrderCustom0;
  }
  
  public String getCronLikeOrderCustom1() {
    return cronLikeOrderCustom1;
  }
  
  public String getCronLikeOrderCustom2() {
    return cronLikeOrderCustom2;
  }
  
  public String getCronLikeOrderCustom3() {
    return cronLikeOrderCustom3;
  }
  
  
  public void setConsiderDaylightSaving( Boolean useDST ) {
    if (useDST == null) {
      considerDaylightSaving = false;
    } else {
      // FIXME: we need to perform some checks for validating the input, don't we?
      considerDaylightSaving = useDST;
    }
  }
  
  
  public Boolean isConsiderDaylightSaving() {
    return considerDaylightSaving;
  }
  
  
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
  
  public Boolean isEnabled() {
    return enabled;
  }
  
  public void setOnError(OnErrorAction onError) {
    this.onError = onError;
  }
  
  public OnErrorAction getOnError() {
    return onError;
  }
  
  public Long getRootOrderId() {
    return rootOrderId;
  }
  
  public void setRootOrderId(Long rootOrderId) {
    this.rootOrderId = rootOrderId;
  }
  
  public void setCronLikeOrderCustom0(String custom) {
    this.cronLikeOrderCustom0 = custom;
    super.setCustom0(custom);
  }
  
  public void setCronLikeOrderCustom1(String custom) {
    this.cronLikeOrderCustom1 = custom;
    super.setCustom1(custom);
  }
  
  public void setCronLikeOrderCustom2(String custom) {
    this.cronLikeOrderCustom2 = custom;
    super.setCustom2(custom);
  }
  
  public void setCronLikeOrderCustom3(String custom) {
    this.cronLikeOrderCustom3 = custom;
    super.setCustom3(custom);
  }
  
  public static long parseInterval(String input) {
    Matcher intervalMatcher = intervalPattern.matcher(input);

    if (intervalMatcher.matches()) {
      long interval = Long.parseLong(intervalMatcher.group(1));
      String unitString = intervalMatcher.group(2);
      
      if ( unitString.length() > 0 ) {
        TimeUnit unit = TimeUnit.fromUnit(unitString);

        switch (unit) {
          case DAYS :
            interval *= 24;
          case HOURS :
            interval *= 60;
          case MINUTES :
            interval *= 60;
          case SECONDS :
            interval *= 1000;
          case MILLISECONDS :
          default :
        }
      }
      
      return interval;
    } else {
      throw new RuntimeException( "Cannot parse input" );
    }
  }

  public static long parseDate(String input, TimeZone tz) {
    Matcher dateMatcher = datePattern.matcher(input);

    if (dateMatcher.matches()) {
      int year = Integer.parseInt(dateMatcher.group(1));
      int month = Integer.parseInt(dateMatcher.group(2)) - 1;
      int date = Integer.parseInt(dateMatcher.group(3));
      int hourOfDay = Integer.parseInt(dateMatcher.group(4));
      int minute = Integer.parseInt(dateMatcher.group(5));
      int second = Integer.parseInt(dateMatcher.group(6));
      int millisecond = 0;

      if (dateMatcher.group(7) != null) {
        String fractionalSecondString = "0." + dateMatcher.group(7);
        float fractionalSecond = Float.parseFloat(fractionalSecondString);
        millisecond = Math.round(1000.f * fractionalSecond);
      }

      Calendar cal = Calendar.getInstance(tz);
      cal.set(year, month, date, hourOfDay, minute, second);
      cal.set(Calendar.MILLISECOND, millisecond);

      if (logger.isTraceEnabled()) {
        logger.trace(cal.getTimeInMillis()+"ms");
      }
      return cal.getTimeInMillis();
    } else {
      try {
        if (logger.isTraceEnabled()) {
          logger.trace(Long.parseLong(input)+"ms");
        }
        return Long.parseLong(input);
      } catch (NumberFormatException nfe) {
        throw new RuntimeException("Unable to parse first execution time \"" + input + "\"");
      }
    }
  }


  public static boolean verifyTimeZone(String timezoneID) {
    TimeZone timeZone = TimeZone.getTimeZone(timezoneID);
    return timeZone.getID().equals(timezoneID);
  }


  public static boolean verifyIntervalQualifiesForDST(long interval) {
    long dayInMS = 24 * 60 * 60 * 1000L;
    return ((interval % dayInMS) == 0);
  }


  public static boolean verifyTimeZoneHasDST(String timeZoneID) {
    TimeZone timeZone = TimeZone.getTimeZone(timeZoneID);
    return (timeZone.getDSTSavings() != 0);
  }
  
  
  /**
   * Wandelt ein Millisekunden-Intervall in eine CalendarDefinition für ein RestrictionBasedTimeWindow um.
   * @param interval
   * @return
   */
  public static String generateCalendarDefinition(long interval) {
    if (interval <= 0) {
      return null;
    }
    
    if (interval % (1000L*60*60*24) == 0) {
      return "[" + JodaTimeControlUnit.DAY.getStringIdentifier() + "=:" +  interval / (1000L*60*60*24) + "]";
    } else if (interval % (1000L*60*60) == 0) {
      return "[" + JodaTimeControlUnit.HOUR.getStringIdentifier() + "=:" +  interval / (1000L*60*60) + "]";
    } else if (interval % (1000L*60) == 0) {
      return "["+ JodaTimeControlUnit.MINUTE.getStringIdentifier() + "=:" + interval / (1000L*60) + "]";
    } else if (interval % (1000L) == 0) {
      return "[" + JodaTimeControlUnit.SECOND.getStringIdentifier() + "=:" + interval / 1000L + "]";
    } else {
      return "[" + JodaTimeControlUnit.MILLISECOND.getStringIdentifier() + "=:" + interval + "]";
    }
  }
  
  /**
   * Liefert einen CronLikeOrderCreationParameterBuilder, mit einem CronLikeOrderCreationParameter, in dem
   * DestinationKey und InputPaylod gesetzt sind.
   * @param dk
   * @param inputPayload
   * @return
   */
  public static CronLikeOrderCreationParameterBuilder<? extends CronLikeOrderCreationParameter> newClocpForCreate(DestinationKey dk, GeneralXynaObject... inputPayload) {
    return new CronLikeOrderCreationParameterBuilder<CronLikeOrderCreationParameter>(new CronLikeOrderCreationParameter(dk, inputPayload), Type.Create);
  }

  /**
   * Liefert einen CronLikeOrderCreationParameterBuilder, mit einem leeren CronLikeOrderCreationParameter.
   * @return
   */
  public static CronLikeOrderCreationParameterBuilder<? extends CronLikeOrderCreationParameter> newClocpForModify() {
    CronLikeOrderCreationParameter clocp = new CronLikeOrderCreationParameter(new DestinationKey(""), new Container());
    clocp.setDestinationKeyNull();
    clocp.setInputPayloadNull();
    clocp.setCustomStringContainer(new CustomStringContainer(null, null, null, null));
    return new CronLikeOrderCreationParameterBuilder<CronLikeOrderCreationParameter>(clocp, Type.Modify);
  }
  
  
  /**
   * Baut ein CronLikeOrderCreationParameter-Objekt. Dabei kann über Type gesteuert werden, ob
   * die CronLikeOrderCreationParameter für ein Create oder ein Modify verwendet werden sollen.
   * 
   * Zum Beispiel können CronLikeOrderCreationParameter zum Ändern des Labels und der calendarDefinition
   * wie folgt erzeugt werden: 
   * 
   * {@code 
   *   CronLikeOrderCreationParameter clocp = CronLikeOrderCreationParameter.newClocpForModify().label("new label").calendarDefintion("[Hour=:1]").build();
   * }
   */
  public static class CronLikeOrderCreationParameterBuilder<T extends CronLikeOrderCreationParameter> {
    private Type type;
    private T clocp;
    
    public enum Type {
      Create, Modify;
    }
    
    public CronLikeOrderCreationParameterBuilder(T clocp, Type type) {
      this.type = type;
      this.clocp = clocp;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> destinationKey(DestinationKey destinationKey) {
      if (type == Type.Modify && destinationKey == null) {
        clocp.setDestinationKeyNull();
      } else {
        clocp.setDestinationKey(destinationKey);
      }
      return this;
    }

    public CronLikeOrderCreationParameterBuilder<T> inputPayload(GeneralXynaObject inputPayload) {
      if (type == Type.Modify && inputPayload == null) {
        clocp.setInputPayloadNull();
      } else {
        clocp.setInputPayload(inputPayload);
      }
      return this;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> label(String label) {
      ((CronLikeOrderCreationParameter)clocp).label = label;
      return this;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> startTime(Long startTime) {
      if (type == Type.Modify && startTime == null) {
        ((CronLikeOrderCreationParameter)clocp).startTime = null;
      } else {
        clocp.setStartTime(startTime);
      }
      return this;
    }

    public CronLikeOrderCreationParameterBuilder<T> timeZoneId(String timeZoneID) {
      if (type == Type.Create && timeZoneID == null) {
        ((CronLikeOrderCreationParameter)clocp).originTimeZoneID = Constants.DEFAULT_TIMEZONE;
      } else {
        ((CronLikeOrderCreationParameter)clocp).originTimeZoneID = timeZoneID;
      }
      return this;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> calendarDefinition(String calendarDefinition) {
      ((CronLikeOrderCreationParameter)clocp).calendarDefinition = calendarDefinition;
      return this;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> useDST(Boolean useDST) {
      if (type == Type.Create && useDST == null) {
        ((CronLikeOrderCreationParameter)clocp).considerDaylightSaving = false;
      } else {
        ((CronLikeOrderCreationParameter)clocp).considerDaylightSaving = useDST;
      }
      return this;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> enabled(Boolean enabled) {
      ((CronLikeOrderCreationParameter)clocp).enabled = enabled;
      return this;
    }

    public CronLikeOrderCreationParameterBuilder<T> onError(OnErrorAction onError) {
      ((CronLikeOrderCreationParameter)clocp).onError = onError;
      return this;
    }

    public CronLikeOrderCreationParameterBuilder<T> rootOrderId(Long rootOrderId) {
      ((CronLikeOrderCreationParameter)clocp).rootOrderId = rootOrderId;
      return this;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> custom0(String custom0) {
      clocp.setCustom0(custom0);
      ((CronLikeOrderCreationParameter)clocp).cronLikeOrderCustom0 = custom0;
      return this;
    }

    public CronLikeOrderCreationParameterBuilder<T> custom1(String custom1) {
      clocp.setCustom1(custom1);
      ((CronLikeOrderCreationParameter)clocp).cronLikeOrderCustom1 = custom1;
      return this;
    }

    public CronLikeOrderCreationParameterBuilder<T> custom2(String custom2) {
      clocp.setCustom2(custom2);
      ((CronLikeOrderCreationParameter)clocp).cronLikeOrderCustom2 = custom2;
      return this;
    }
    
    public CronLikeOrderCreationParameterBuilder<T> custom3(String custom3) {
      clocp.setCustom3(custom3);
      ((CronLikeOrderCreationParameter)clocp).cronLikeOrderCustom3 = custom3;
      return this;
    }
    
    
    public T build() {
      return clocp;
    }
  }
  
  
}
