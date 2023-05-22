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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * SimplePeriodicTimeWindow ist eine einfache Implementierung von TimeWindow, bei der 
 * sich das Zeitfenster periodisch �ffnet.
 * Im Konstruktor kann die PeriodenDauer und Offset und die Dauer der Offen-Phase angegeben werden.
 * PeriodenDauer und Offset werden im Format "%PeriodenDauer+Offset" angegeben.
 *
 * Beispiele:<br>
 * <ul>
 * <li>SimplePeriodicTimeWindow("%3600+0",60): Zeitfenster �ffnet sich zu Beginn jeder Stunde
 *     f�r eine Minute, also 0:00-0:01, 1:00-1:01, ...</li>
 * <li>SimplePeriodicTimeWindow("%3600+1800",300): Zeitfenster �ffnet sich in jeder Stunde 
 *     jeweils um halb f�r f�nf Minuten, also 0:30-0:35, 1:30-1:35, ...</li>
 * <li>SimplePeriodicTimeWindow("%86400+21600",7200): Zeitfenster �ffnet sich in jeden Tag 
 *     um 6 Uhr f�r 2 Stunden, 86400=24*60*60, 21600=6*60*60</li> 
 * </ul>    
 */
public class SimplePeriodicTimeWindow extends TimeWindow {
  
  private long periodLength;
  private long offset;
  private long duration;
  private SimplePeriodicTimeWindowDefinition definition;
  
  /**
   * @param modAddString enth�lt die Periodendauer in Sekunden
   * @param duration Dauer der Offen-Phase in Sekunden
   */
  public SimplePeriodicTimeWindow(String modAddString, long duration) {
    this.definition = new SimplePeriodicTimeWindowDefinition(modAddString,duration);
    SimplePeriodicTimeWindow tw = (SimplePeriodicTimeWindow) definition.constructTimeWindow();
    this.periodLength = tw.periodLength;
    this.offset = tw.offset;
    this.duration = tw.duration;
  }

  public SimplePeriodicTimeWindow(SimplePeriodicTimeWindowDefinition definition, long periodLength, long offset, long duration) {
    this.definition = definition;
    this.periodLength = periodLength;
    this.offset = offset;
    this.duration = duration;
  }

  @Override
  public TimeWindowDefinition getDefinition() {
    return definition;
  }
  
  @Override
  protected TimeWindowData recalculateInternal(long now) {
    long lastOpenedTimestamp = ((now-offset)/periodLength)*periodLength+offset;
    //lastOpenedTimestamp ist kleinergleich now
    
    long nextClose = lastOpenedTimestamp + duration; 
    long nextOpen = lastOpenedTimestamp + periodLength;
    long since;
    
    boolean isOpen = now < nextClose;
    if( isOpen ) {
      since = lastOpenedTimestamp;
    } else {
      //bereits geschlossen, deshalb ist nextClose in n�chster Periode
      since = nextClose;
      nextClose = nextClose + periodLength;
    }
    return new TimeWindowData(isOpen, nextOpen, nextClose, since);
  }

  @Override
  public String toString() {
    return "SimplePeriodicTimeWindow(\"%"+periodLength/1000+"+"+offset/1000+"\","+duration/1000+")";
  }
  
  public static class SimplePeriodicTimeWindowDefinition extends TimeWindowDefinition {
    private static final long serialVersionUID = 1L;
    private static final String TYPE = "SimplePeriodic";
    private static final String P_LONG = "(\\d+)";
    private static final String P_MODADD = "%"+P_LONG+"\\+"+P_LONG; //%(\d+)\+(\d+)
    private static final Pattern PATTERN_MODADD = Pattern.compile(P_MODADD);
    private static final String P_DEFINITION = TYPE+"\\(("+P_MODADD+"),"+P_LONG+"\\)";
    private static final Pattern PATTERN_DEFINITION = Pattern.compile(P_DEFINITION);
    
    
    private String modAddString;
    private long duration;
    private transient long periodLength;
    private transient long offset;
    
    public SimplePeriodicTimeWindowDefinition(String modAddString, long duration) {
      this.modAddString = modAddString;
      this.duration = duration;
    }
   
    @Override
    public String getType() {
      return TYPE;
    }
    
    public String getModAddString() {
      return modAddString;
    }
    
    public long getDuration() {
      return duration;
    }
    
    @Override
    public String toString() {
      return "TimeWindowDefinition("+TYPE+","+modAddString+","+duration+")";
    }
    
    public TimeWindow constructTimeWindow() {
      if( periodLength == 0 ) {
        parseModAddString();
      }
      return new SimplePeriodicTimeWindow(this, periodLength*1000L, offset*1000L, duration*1000L);
    }

    private void parseModAddString() {
      Matcher m = PATTERN_MODADD.matcher(modAddString);
      if( ! m.matches() ) {
        throw new IllegalArgumentException("\""+modAddString +"\" does not match pattern \""+P_MODADD+"\"");
      }
      this.periodLength = Long.parseLong(m.group(1));
      this.offset = Long.parseLong(m.group(2));
      
      if( duration >= periodLength ) {
        throw new IllegalArgumentException("duration "+duration+" cannot be larger than or equal to periodLength "+m.group(1));
      }
    }

    @Override
    public TimeWindowDefinition deserializeFromString(String string) {
      Matcher m = PATTERN_DEFINITION.matcher(string);
      if( ! m.matches() ) {
        throw new IllegalArgumentException("\""+string +"\" does not match pattern \""+P_DEFINITION+"\"");
      }

      String modAddString = m.group(1);
      long periodLength = Long.parseLong(m.group(2));
      long offset = Long.parseLong(m.group(3));
      long duration = Long.parseLong(m.group(4));
      
      SimplePeriodicTimeWindowDefinition definition = new SimplePeriodicTimeWindowDefinition(modAddString,duration);
      definition.periodLength = periodLength;
      definition.offset = offset;
      return definition;
    }

    @Override
    public String serializeToString() {
      return TYPE+"("+modAddString+","+duration+")";
    }

  }
  
}
