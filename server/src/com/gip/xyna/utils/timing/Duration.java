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
package com.gip.xyna.utils.timing;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.misc.StringSplitter;
import com.gip.xyna.utils.misc.StringSplitter.SplitApply;


/**
 * Duration ist eine immutable Klasse zur Speicherung einer Zeitdauer unter Angabe eines Betrags und einer Einheit.
 * Duration ist StringSerializable, mit einer lesbaren Darstellung als "10 s", "55 ms", "3 min" etc.
 * Mögliche Einheiten sind {Tag "d", Stunde "h", Minute "min", Sekunde "s", Milli- "ms", Mikro- "µs" und Nanosekunde "ns"}
 * Diese Einheiten ergeben sich aus der vollständigen Abdeckung der Konstanten aus {@link java.util.concurrent.TimeUnit TimeUnit}.
 * Eine Kombination "4 min 30 s" ist nicht möglich!
 * 
 * Equals und Hash berücksichtigen nur die Zeit in Millisekunden, d.h. "60 s" und "1 min" sind gleich.
 */
public class Duration implements StringSerializable<Duration>, Serializable {
  private static final long serialVersionUID = 1L;
  private static final String P_STRING = "(\\d+)\\s*(d|h|min|s|ms|µs|ns|)";
  private static final StringSplitter splitter = new StringSplitter(P_STRING);
  private static final Pattern PATTERN = Pattern.compile(P_STRING);
   
  private static final Map<TimeUnit,String> UNITS;
  private static final Map<String,TimeUnit> NAMES;
  static {
    EnumMap<TimeUnit,String> units = new EnumMap<TimeUnit,String>(TimeUnit.class);
    Map<String,TimeUnit> names = new HashMap<String,TimeUnit>();
    for( TimeUnit unit : TimeUnit.values() ) {
      String name = nameOf(unit);
      units.put( unit, name );
      names.put( name, unit );
    }
    UNITS = Collections.unmodifiableMap(units);
    NAMES = Collections.unmodifiableMap(names);
  }
  private static final TimeUnit[] UNITSIZE = new TimeUnit[TimeUnit.values().length];
  static { //komische Befüllung, da Java 1.5 keine MINUTES, HOURS und DAYS kennt
    addUnit( UNITSIZE, 0, "ns");
    addUnit( UNITSIZE, 1, "µs");
    addUnit( UNITSIZE, 2, "ms");
    addUnit( UNITSIZE, 3, "s"); 
    addUnit( UNITSIZE, 4, "min"); 
    addUnit( UNITSIZE, 5, "h"); 
    addUnit( UNITSIZE, 6, "d");
  }
  private static void addUnit(TimeUnit[] array, int index, String name) {
    TimeUnit unit = NAMES.get(name);
    if( unit != null ) {
      array[index] = unit;
    }
  }
  
 
  private final long number;
  private final TimeUnit unit;
  
  /**
   * Konstruktor mit Angabe in Millisekunden
   * @param millis
   */
  public Duration(long millis) {
    this.number = millis;
    this.unit = TimeUnit.MILLISECONDS;
  }
  
  

  private static String nameOf(TimeUnit unit) {
    switch( unit ) {
      case SECONDS: return "s";
      case MILLISECONDS: return "ms";
      case MICROSECONDS: return "µs";
      case NANOSECONDS: return "ns";
      default: //MINUTES, HOURS und DAYS in Java 1.5 nicht bekannt
        String name = unit.name();
        if( "MINUTES".equals(name) ) {
          return "min";
        } else if( "HOURS".equals(name) ) {
          return "h";
        } else if( "DAYS".equals(name) ) {
          return "d";
        } else {
          throw new IllegalArgumentException("Unexpected TimeUnit "+unit);
        }
    }
  }

  /**
   * Konstruktor mit Angabe der Einheit
   * @param duration
   * @param unit
   */
  public Duration(long duration, TimeUnit unit) {
    if( unit == null ) {
      throw new IllegalArgumentException("TimeUnit must not be null");
    }
    this.number = duration;
    this.unit = unit;
  }
 
  @Override
  public String toString() {
    return serializeToString();
  }
  
  
  
  
  @Override
  public int hashCode() {
    long millis = getDurationInMillis();
    return (int) (millis ^ (millis >>> 32));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Duration other = (Duration) obj;
    return getDurationInMillis() == other.getDurationInMillis();
  }

  /**
   * Ausgabe der Duration in Millisekunden
   * @return
   */
  public long getDurationInMillis() {
    return TimeUnit.MILLISECONDS.convert(number, this.unit );
  }
  
  /**
   * Ausgabe der Duration in angegebener Einheit
   * @param unit
   * @return
   */
  public long getDuration(TimeUnit unit) {
    return unit.convert(number, this.unit );
  }
  
  /**
   * Parsen der Zeitdauer mit Defaulteinheit Sekunde, wenn im String die Einheit fehlt
   * @param string
   * @return
   * @throws IllegalArgumentException wenn String nicht parsebar ist
   */
  public static Duration valueOf(String string) {
    return valueOf(string, TimeUnit.SECONDS);
  }
  
  /**
   * Parsen der Zeitdauer mit angebener Einheit, wenn im String die Einheit fehlt
   * @param string
   * @param defaultTimeUnit
   * @return
   * @throws IllegalArgumentException wenn String nicht parsebar ist
   */
  public static Duration valueOf(String string, TimeUnit defaultTimeUnit) {
    if( string == null ) {
      return null;
    }
    Matcher m = PATTERN.matcher(string);
    if( m.matches() ) {
      return matchToDuration(m, defaultTimeUnit);
    } else {
      throw new IllegalArgumentException("\""+string+"\" does not match pattern \""+P_STRING+"\"");
    }
  }

  private static Duration matchToDuration(Matcher m, TimeUnit defaultTimeUnit) {
    long duration = Long.parseLong(m.group(1));
    TimeUnit unit = unitOf(m.group(2),defaultTimeUnit);
    if( unit == null ) { //TODO MINUTES, HOURS und DAYS in Java 1.5 nicht bekannt
      if( "min".equals(m.group(2)) ) {
        duration = duration * 60;
      } else if( "h".equals(m.group(2)) ) {
        duration = duration * 60 * 60;
      } else if( "d".equals(m.group(2)) ) {
        duration = duration * 60 * 60 * 24;
      }
      unit = TimeUnit.SECONDS;
    }
    return new Duration(duration,unit);
  }


  public static Duration valueOfSum(String string) {
    List<Pair<String, Duration>> list = splitter.splitAndApply(string, new SplitToDuration() );
    //Separatoren werden nicht erwartet, diese führen unten zu einer IllegalArgumentException 
    
    long millis = 0;
    long nanos =0;
    
    for( Pair<String, Duration> pair : list) {
      Duration dur = pair.getSecond();
      if( dur != null ) {
        if( dur.getUnit() == TimeUnit.NANOSECONDS) {
          nanos += dur.getNumber();
        } else if( dur.getUnit() == TimeUnit.MICROSECONDS) {
          nanos += dur.getNumber()*1000L;
        } else {
          millis += dur.getDurationInMillis();
        }
      }
      if( pair.getFirst() != null && pair.getFirst().trim().length() > 0 ) {
        throw new IllegalArgumentException("\""+string+"\" contains unexpected \""+pair.getFirst()+"\"");
      }
    }
    
    Duration dur = null;
    if( nanos == 0 ) {
      dur = new Duration(millis);
    } else if( nanos%10000000 != 0 ) {
      dur = new Duration(millis +nanos/10000000 );
    } else if( nanos%1000 == 0 ) {
      dur = new Duration( millis*1000 + nanos/1000, TimeUnit.MICROSECONDS );
    } else {
      dur = new Duration( millis*1000000 + nanos, TimeUnit.NANOSECONDS );
    }
    return dur.convertToLargestUnitWithoutPrecisionLost();
  }
  
  private static class SplitToDuration implements SplitApply<String,Duration> {

    public Duration applyMatch(Matcher matcher) {
      return matchToDuration( matcher, TimeUnit.SECONDS);
    }

    public String applySeparator(String sep) {
      return sep;
    }
    
  }
  
  
  
  /**
   * Umwandlung Einheiten-String in Einheit
   * @param unit
   * @param defaultTimeUnit
   * @return
   */
  public static TimeUnit unitOf(String unit, TimeUnit defaultTimeUnit) {
    if( unit.length() == 0 ) {
      return defaultTimeUnit;
    } else {
      if( "s".equals(unit) ) {
        return TimeUnit.SECONDS;
      } else if( "ms".equals(unit) ) {
        return TimeUnit.MILLISECONDS;
      } else {
        //seltener Rest über Map
        for( Map.Entry<TimeUnit,String> entry : UNITS.entrySet() ) {
          if( entry.getValue().equals(unit) ) {
            return entry.getKey();
          }
        }
        if( "min".equals(unit) || "h".equals(unit) || "d".equals(unit) ) { //TODO MINUTES, HOURS und DAYS in Java 1.5 nicht bekannt
          return null;
        }
      }
    }
    throw new IllegalArgumentException("Unexpected unit \""+unit+"\"");
  }

  public Duration deserializeFromString(String string) {
    return Duration.valueOf(string);
  }


  public String serializeToString() {
    return number + " "+UNITS.get(unit); 
  }

  /**
   * Liefert Zahlenwert
   * @return
   */
  public long getNumber() {
    return number;
  }
  
  /**
   * Liefert Einheit
   * @return
   */
  public TimeUnit getUnit() {
    return unit;
  }

  /**
   * Konvertiert Duration in andere Einheit. Achtung: bei Konversion in größere Einheit kann Präzisionsverlust auftreten!
   * @param unit
   * @return Duration mit andere Einheit
   */
  public Duration convertTo(TimeUnit unit) {
    if( this.unit == unit ) {
      return this;
    } else {
      return new Duration( getDuration(unit), unit);
    }
  }
  
  public Duration convertToLargestUnitWithoutPrecisionLost() {
    int unitIndex = 0;
    for( ;unitIndex<UNITSIZE.length;++unitIndex ) {
      if( UNITSIZE[unitIndex] == unit ) break;
    }
    for( ;unitIndex<UNITSIZE.length;++unitIndex ) {
      if( canConvertWithoutPrecisionLostTo(UNITSIZE[unitIndex]) ) {
        continue;
      } else {
        return convertTo(UNITSIZE[unitIndex-1]);
      }
    }
    return convertTo(UNITSIZE[UNITSIZE.length-1]);
  }
  
  private boolean canConvertWithoutPrecisionLostTo(TimeUnit unit) {
    return this.unit.convert(getDuration(unit),unit) == number;
  }

  public String toSumString() {
    StringBuilder sb = new StringBuilder();
    long dur = number;
    String sep = "";
    for( int us = UNITSIZE.length-1; us >=0; --us ) {
      TimeUnit tu = UNITSIZE[us];
      long v = tu.convert(dur,unit);
      if( v != 0 ) {
        sb.append(sep).append(v).append(" ").append(UNITS.get(tu));
        sep = " ";
        dur -= unit.convert(v,tu);
      }
      if( dur == 0 ) {
        break;
      }
    }
    return sb.toString();
  }
  
}
