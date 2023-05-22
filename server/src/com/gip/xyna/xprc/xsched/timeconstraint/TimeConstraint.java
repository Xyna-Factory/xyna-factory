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
package com.gip.xyna.xprc.xsched.timeconstraint;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.db.types.StringSerializable;


/**
 * TimeConstraints f�r das Scheduling. 
 * Es gibt 3 immutable Subklassen, die jedoch nicht direkt instantiert werden k�nnen .
 * <ul>
 * <li> {@link TimeConstraint_Start}</li>
 * <li> {@link TimeConstraint_Start_Timeout}</li>
 * <li> {@link TimeConstraint_Window}</li>
 * </ul>
 * Instantiiert werden sie �ber die statische Methoden
 * <ul>
 * <li> {@link #immediately()}</li>
 * <li> {@link #at(long)}</li>
 * <li> {@link #delayed(long)} und {@link #delayed(long, TimeUnit)}</li>
 * <li> {@link #schedulingWindow(String)}</li>
 * <li> {@link #schedulingWindow(String, TimeConstraint_Start)}</li>
 * </ul>
 * Die SchedulingTimeouts k�nnen f�r immediately, at und delayed anschlie�end gesetzt 
 * werden �ber die Methoden
 * <ul>
 * <li> {@link TimeConstraint_Start#withSchedulingTimeout(long) withSchedulingTimeout(long)} und 
 *      {@link TimeConstraint_Start#withSchedulingTimeout(long, TimeUnit) withSchedulingTimeout(long, TimeUnit)}</li>
 * <li> {@link TimeConstraint_Start#withAbsoluteSchedulingTimeout(long) withAbsoluteSchedulingTimeout(long)}</li>
 * </ul>
 * Auch ein Zeitfenster kann danach noch angeben werden
 * <ul>
 * <li> {@link TimeConstraint_Start#withTimeWindow(String) withTimeWindow(String)}</li>
 * <li> {@link TimeConstraint_Start#withTimeWindow(String, TimeConstraint_Start) withTimeWindow(String, TimeConstraint_Start)}</li>
 * </ul>
 * F�r die Zeitfenster kann eine weiterer TimeConstraint definiert werden, der innerhalb des Zeitfensters gilt.
 * Damit kann ereicht werden, dass der Auftrag im Zeitfenster versp�tet startet oder ein SchedulingTimeout einh�lt.  
 * TODO dies ist derzeit nur rudiment�r m�glich!
 * <br>
 * Da die TimeConstraints immutable sind, k�nnen sie problemlos mehrmals verwendet werden 
 * und als Konstanten definiert werden, um h�ufiges Instantiieren zu vermeiden.
 * <br>
 * Beispiele:
 * <ul>
 * <li><code>TimeConstraint.immediately();</code> 
 *     Auftrag kann sofort starten, kein Timeout</li>
 * <li><code>TimeConstraint.immediately().withSchedulingTimeout(5000);</code>
 *     Auftrag kann sofort starten, SchedulingTimeout 5 s</li>
 * <li><code>TimeConstraint.delayed(10, TimeUnit.SECONDS).withSchedulingTimeout(2500, TimeUnit.MILLISECONDS);</code>
 *     Auftrag kann fr�hstens 10 Sekunden nach Eingang in die Factory starten, SchedulingTimeout 2.5 s</li>
 * <li><code>long now = System.currentTimeMillis(); TimeConstraint.at( now+5000 ).withAbsoluteSchedulingTimeout( now + 10000);  </code>
 *     Auftrag kann fr�hstens in genau 5 Sekunden und muss bis sp�tensten in 10 Sekunden geschedult sein</li>
 * <li><code>TimeConstraint.schedulingWindow("saturday_night");</code>
 *     Auftrag darf nur Samstags nachts laufen (richtige TimeWindow-Erzeugung vorausgesetzt</li>
 * <li><code>long nextMonth = now + 31*24*60*60*1000; TimeConstraint.at(nextMonth).withTimeWindow("saturday_night");</code>
 *     Auftrag muss noch einen Monat warten, bevor in der n�chsten Samstag-Nacht laufen darf</li>
 * <li><code>TimeConstraint.delayed(7,TimeUnit.DAYS).withTimeWindow("saturday_night")</code>
 *     Auftrag wird ab n�chster Woche und nur Samstags nachts laufen </li>
 * <li><code>TimeConstraint.schedulingWindow("saturday_night", TimeConstraint.delayed(1,TimeUnit.HOURS) );</code>
 *     Auftrag wird in n�chster Samstag-Nacht fr�hstens eine Stunde nach Beginn der Nacht geschedult</li>
 * <li><code>TimeConstraint.schedulingWindow("saturday_night", TimeConstraint.immediately().withSchedulingTimeout(1,TimeUnit.HOURS) );</code>
 *     Auftrag darf in n�chster Samstag-Nacht in der ersten Stunde geschedult werden</li>
 * </ul>
 */
public abstract class TimeConstraint implements StringSerializable<TimeConstraint>, Serializable {

  private static final long serialVersionUID = 1L;
  private static final TimeConstraint_Start IMMEDIATELY = new TimeConstraint_Start(0L,true);

  /**
   * Ausgabe des absoluten Startzeitpunkts
   * @param entranceTimestamp Zeitpunkt des Auftragseingangs
   * @return
   */
  public abstract long startTimestamp(long entranceTimestamp);

  /**
   * Ausgabe des absoluten SchedulingTimeouts
   * @param entranceTimestamp Zeitpunkt des Auftragseingangs
   * @return
   */
  public Long schedulingTimeout(long entranceTimestamp) {
    return null;
  }

  public TimeConstraint deserializeFromString(String string) {
    return TimeConstraint.valueOf(string);
  }

  public abstract String serializeToString();

  
  
  /**
   * Auftrag darf sofort nach Auftragseingang starten
   * @return
   */
  public static TimeConstraint_Start immediately() {
    return IMMEDIATELY;
  }
  
  /**
   * Auftrag darf erst nach den angegebenen Zeitpunkt starten
   * @param timestamp
   * @return
   */
  public static TimeConstraint_Start at(long timestamp) {
    return new TimeConstraint_Start(timestamp,false);
  }
  
  /**
   * Auftrag darf nach angegebener Verz�gerung nach dem Auftragseingang starten
   * @param duration in Millisekunden
   * @return
   */
  public static TimeConstraint_Start delayed(long duration) {
    return new TimeConstraint_Start(duration,true);
  }
  
  /**
   * Auftrag darf nach angegebener Verz�gerung nach dem Auftragseingang starten
   * @param duration
   * @param unit
   * @return
   */
  public static TimeConstraint_Start delayed(long duration, TimeUnit unit) {
    return new TimeConstraint_Start(TimeUnit.MILLISECONDS.convert(duration, unit),true);
  }
 
  /**
   * Auftrag darf nur in dem angegebenen Zeitfenster starten
   * @param windowName
   * @return
   */
  public static TimeConstraint schedulingWindow(String windowName) {
    return new TimeConstraint_Window(IMMEDIATELY,windowName,IMMEDIATELY);
  }
  
  /**
   * Auftrag darf nur in dem angegebenen Zeitfenster starten
   * @param windowName
   * @return
   */
  public static TimeConstraint schedulingWindow(String windowName, TimeConstraint_Start tcInWindow) {
    return new TimeConstraint_Window(IMMEDIATELY,windowName,tcInWindow);
  }
  
  /**
   * Startzeit ist festgelegt, kein SchedulingTimeout
   */
  public static class TimeConstraint_Start extends TimeConstraint {
    private static final long serialVersionUID = 1L;

    private final AbsRelTime start;
  
    public TimeConstraint_Start(AbsRelTime start) {
      this.start = start;
    }
    
    protected TimeConstraint_Start(long startTime, boolean startTimeIsRelative) {
      this.start = new AbsRelTime(startTime, startTimeIsRelative);
    }
    
    @Override
    public String toString() {
      if( start.getTime() == 0L ) {
        return "TimeConstraint.immediately()";
      } else {
        if( start.isRelative() ) {
          return "TimeConstraint.delayed("+start.getTime()+")";
        } else {
          return "TimeConstraint.at("+start.getTime()+")";
        }
      }
    }
    
    public String serializeToString() {
      return "start("+start.serializeToString()+")";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((start == null) ? 0 : start.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      TimeConstraint_Start other = (TimeConstraint_Start) obj;
      if (start == null) {
        if (other.start != null)
          return false;
      } else if (!start.equals(other.start))
        return false;
      return true;
    }

    /**
     * Ausgabe des absoluten Startzeitpunkts
     * @param entranceTimestamp Zeitpunkt des Auftragseingangs
     * @return
     */
    public long startTimestamp(long entranceTimestamp) {
      if( start.getTime() == 0L ) {
        return entranceTimestamp;
      } else {
        if( start.isRelative() ) {
          return entranceTimestamp + start.getTime();
        } else {
          return start.getTime();
        }
      }
    }

    /**
     * Ausgabe des absoluten SchedulingTimeouts
     * @param entranceTimestamp Zeitpunkt des Auftragseingangs
     * @return
     */
    public Long schedulingTimeout(long entranceTimestamp) {
      return null;
    }
    
    /**
     * Setzen des relativen SchedulingTimeouts
     * @param duration
     * @return
     */
    public TimeConstraint_Start_Timeout withSchedulingTimeout(long duration) {
      return new TimeConstraint_Start_Timeout(this,duration,true);
    }
    
    /**
     * Setzen des relativen SchedulingTimeouts
     * @param duration
     * @param unit
     * @return
     */
    public TimeConstraint_Start_Timeout withSchedulingTimeout(long duration, TimeUnit unit) {
      return new TimeConstraint_Start_Timeout(this,TimeUnit.MILLISECONDS.convert(duration, unit),true);
    }
    
    /**
     * Setzen des absoluten SchedulingTimeouts
     * @param timestamp
     * @return
     */
    public TimeConstraint_Start_Timeout withAbsoluteSchedulingTimeout(long timestamp) {
      return new TimeConstraint_Start_Timeout(this,timestamp,false);
    }

    /**
     * Mit einem SchedulingWindow.
     * @param windowName
     * @return
     */
    public TimeConstraint withTimeWindow(String windowName) {
      return new TimeConstraint_Window(this,windowName,IMMEDIATELY);
    }
    
    /**
     * Mit einem SchedulingWindow und TimeConstraint im TimeWindow 
     * @param windowName
     * @param tcInWindow
     * @return
     */
    public TimeConstraint withTimeWindow(String windowName, TimeConstraint_Start tcInWindow) {
      return new TimeConstraint_Window(this,windowName,tcInWindow);
    }
    
    protected boolean isPlainImmediately() {
      return start.getTime() == 0L;
    }
    
    
    // for webservice backward conversions
    public AbsRelTime getStart() {
      return start;
    }
    
  }
    
  /**
   * Startzeit und SchedulingTimeout sind festgelegt
   */
  public static class TimeConstraint_Start_Timeout extends TimeConstraint_Start {
    private static final long serialVersionUID = 1L;

    private final AbsRelTime schedulingTimeout;

    protected TimeConstraint_Start_Timeout(TimeConstraint_Start tcStart, long schedulingTimeout, boolean schedulingTimeoutIsRelative) {
      super(tcStart.start);
      this.schedulingTimeout = new AbsRelTime(schedulingTimeout,schedulingTimeoutIsRelative);
    }
       
    public TimeConstraint_Start_Timeout(AbsRelTime start, AbsRelTime schedulingTimeout) {
      super(start);
      this.schedulingTimeout = schedulingTimeout;
    }

    public Long schedulingTimeout(long entranceTimestamp) {
      if( schedulingTimeout.getTime() == 0L ) {
        return null;
      }
      if( schedulingTimeout.isRelative() ) {
        return startTimestamp(entranceTimestamp) + schedulingTimeout.getTime();
      } else {
        return schedulingTimeout.getTime();
      }
    }

    @Override
    public String toString() {
      if( schedulingTimeout.isRelative() ) {
        return super.toString()+".withSchedulingTimeout("+schedulingTimeout.getTime()+")";
      } else {
        return super.toString()+".withAbsoluteSchedulingTimeout("+schedulingTimeout.getTime()+")";
      }
    }

    public String serializeToString() {
      return super.serializeToString()+"_schedTimeout("+schedulingTimeout.serializeToString()+")"; 
    }

    protected boolean isPlainImmediately() {
      return false;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((schedulingTimeout == null) ? 0 : schedulingTimeout.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      TimeConstraint_Start_Timeout other = (TimeConstraint_Start_Timeout) obj;
      if (schedulingTimeout == null) {
        if (other.schedulingTimeout != null)
          return false;
      } else if (!schedulingTimeout.equals(other.schedulingTimeout))
        return false;
      return true;
    }
    
    
    // for webservice backward conversions
    public AbsRelTime getSchedulingTimeout() {
      return schedulingTimeout;
    }

  }

  /**
   * Zeitfenster ist festgelegt
   */
  public static class TimeConstraint_Window extends TimeConstraint {
    private static final long serialVersionUID = 1L;
    private final String windowName;
    private final TimeConstraint_Start tcBeforeWindow;
    private final TimeConstraint_Start tcInWindow;
    
 
    public TimeConstraint_Window(TimeConstraint_Start tcBeforeWindow, String windowName, TimeConstraint_Start tcInWindow) {
      this.tcBeforeWindow = tcBeforeWindow;
      this.windowName = windowName;
      this.tcInWindow = tcInWindow;
    }

    public String getWindowName() {
      return windowName;
    }

    /**
     * Ausgabe des Zeitpunktes, ab dem TimeWindow gilt
     */
    @Override
    public long startTimestamp(long entranceTimestamp) {
      return tcBeforeWindow.startTimestamp(entranceTimestamp);
    }
    
    /**
     * SchedulingTimeout, bevor TimeWindow gilt
     */
    @Override
    public Long schedulingTimeout(long entranceTimestamp) {
      return tcBeforeWindow.schedulingTimeout(entranceTimestamp);
    }

    /**
     * R�ckgabe der TimeConstraints, die innerhalb des Zeitfensters gelten
     * @return
     */
    public TimeConstraint_Start getInnerTimeConstraint() {
      return tcInWindow;
    }
    
    /**
     * R�ckgabe der TimeConstraints, die vor dem Zeitfensters gelten
     * @return
     */
    public TimeConstraint_Start getBeforeTimeConstraint() {
      return tcBeforeWindow;
    }

    public String serializeToString() {
      return tcBeforeWindow.serializeToString()+"_window("+windowName+")_"+tcInWindow.serializeToString();
    }

    @Override
    public String toString() {
      boolean beforeWindowEmpty = tcBeforeWindow.isPlainImmediately();
      boolean inWindowEmpty = tcInWindow.isPlainImmediately();
      StringBuilder sb = new StringBuilder();
      
      if( beforeWindowEmpty ) {
        sb.append("TimeConstraint.schedulingWindow(");
      } else {
        sb.append(tcBeforeWindow.toString()).append(".withTimeWindow(");
      }
      sb.append("\"").append(windowName).append("\"");
      if( ! inWindowEmpty ) {
        sb.append(",").append(tcInWindow.toString());
      }
      sb.append(")");
      return sb.toString();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((tcBeforeWindow == null) ? 0 : tcBeforeWindow.hashCode());
      result = prime * result + ((tcInWindow == null) ? 0 : tcInWindow.hashCode());
      result = prime * result + ((windowName == null) ? 0 : windowName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      TimeConstraint_Window other = (TimeConstraint_Window) obj;
      if (tcBeforeWindow == null) {
        if (other.tcBeforeWindow != null)
          return false;
      } else if (!tcBeforeWindow.equals(other.tcBeforeWindow))
        return false;
      if (tcInWindow == null) {
        if (other.tcInWindow != null)
          return false;
      } else if (!tcInWindow.equals(other.tcInWindow))
        return false;
      if (windowName == null) {
        if (other.windowName != null)
          return false;
      } else if (!windowName.equals(other.windowName))
        return false;
      return true;
    }
    
    
    // for webservice backward conversions
    public TimeConstraint_Start getTcBefore() {
      return tcBeforeWindow;
    }
    
  }
  
  private static final String P_ABS_REL_TIME = "\\(([^)]+)\\)";
  private static final String P_NORMAL = "start"+P_ABS_REL_TIME+"(_schedTimeout"+P_ABS_REL_TIME+")?";
  private static final String P_WINDOW_NAME = "\\(([^)]+)\\)";
  private static final String P_WINDOW = "("+P_NORMAL+")_window"+P_WINDOW_NAME+"_("+P_NORMAL+")";
  private static final Pattern PATTERN_NORMAL = Pattern.compile(P_NORMAL);
  private static final Pattern PATTERN_WINDOW = Pattern.compile(P_WINDOW);
    
  public static TimeConstraint valueOf(String string) {
    if( string == null ) {
      return null;
    }
    Matcher matcher = PATTERN_NORMAL.matcher(string);
    if( matcher.matches() ) {
      return timeConstraint_Start(matcher.group(1), matcher.group(3) );
    } else {
      matcher = PATTERN_WINDOW.matcher(string);
      if( matcher.matches() ) {
        TimeConstraint_Start tcBeforeWindow = timeConstraint_Start(matcher.group(2), matcher.group(4) );
        String windowName = matcher.group(5);
        TimeConstraint_Start tcInWindow = timeConstraint_Start(matcher.group(7), matcher.group(9) );
        return new TimeConstraint_Window(tcBeforeWindow, windowName, tcInWindow);
      } else {
        throw new IllegalArgumentException(string + " does not match pattern "+P_NORMAL +" or "+PATTERN_WINDOW );
      }
    }
  }

  private static TimeConstraint_Start timeConstraint_Start(String start, String schedTimeout) {
    if( schedTimeout == null ) {
      return new TimeConstraint_Start(AbsRelTime.valueOf(start));
    } else {
      return new TimeConstraint_Start_Timeout(AbsRelTime.valueOf(start), AbsRelTime.valueOf(schedTimeout) );
    }
  }
  
  
}
