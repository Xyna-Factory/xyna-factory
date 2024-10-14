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
package com.gip.xyna.xprc.xsched.timeconstraint;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.db.types.StringSerializable;


/**
 * Speicherung von Zeiten als long sowie die Information, ob die Zeit absolut oder relativ sein soll.
 * AbsRelTime ist immutable.
 */
public class AbsRelTime implements StringSerializable<AbsRelTime>, Serializable {

  private static final long serialVersionUID = 1L;
  
  private long timeInMillis; //Zeit in Millisekunden
  private boolean isRelative;
  
  private static final String PATTERN_STRING = "(abs|rel):\\s*(\\d+)";
  private static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);
  
  public AbsRelTime(long timeInMillis, boolean isRelative) {
    this.timeInMillis = timeInMillis;
    this.isRelative = isRelative;
  }
  
  public long getTime() {
    return timeInMillis;
  }

  public boolean isRelative() {
    return isRelative;
  }
  
  public boolean isAbsolute() {
    return ! isRelative;
  }
 
  @Override
  public String toString() {
    return "AbsRelTime("+timeInMillis+","+isRelative+")";
  }
  
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isRelative ? 1231 : 1237);
    result = prime * result + (int) (timeInMillis ^ (timeInMillis >>> 32));
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
    AbsRelTime other = (AbsRelTime) obj;
    if (isRelative != other.isRelative)
      return false;
    if (timeInMillis != other.timeInMillis)
      return false;
    return true;
  }

  /**
   * Statische Methode zum Parsen eines Strings in ein Timeout.
   * @param string
   * @return
   */
  public static AbsRelTime valueOf(String string) {
    if( string == null ) {
      return null;
    }
    Matcher matcher = PATTERN.matcher(string);
    if ( matcher.matches() ) {
      boolean relative = "rel".equals( matcher.group(1) );
      long time = Long.parseLong(matcher.group(2));
      return new AbsRelTime(time, relative);
    } else {
      throw new IllegalArgumentException(string + " does not match pattern "+PATTERN_STRING);
    }
  }
  
  /**
   * Legt eine neue AbsRelTime an
   * @param time
   * @return
   */
  public static AbsRelTime relative(long time) {
    return new AbsRelTime(time,true);
  }
  
  /**
   * Legt eine neue AbsRelTime an
   * @param time
   * @param timeUnit
   * @return
   */
  public static AbsRelTime relative(long time, TimeUnit timeUnit) {
    return new AbsRelTime(timeUnit.toMillis(time),true);
  }
  
  /**
   * Legt eine neue AbsRelTime an
   * @param time
   * @return
   */
  public static AbsRelTime absolute(long time) {
    return new AbsRelTime(time,false);
  }
 

  
  /**
   * Lesen aus einem String.
   * @param string
   * @return
   */
  public AbsRelTime deserializeFromString(String string) {
    return valueOf(string);
  }

  /**
   * Umwandlung in einen String.
   */
  public String serializeToString() {
    return (isRelative?"rel:":"abs:")+timeInMillis;
  }


  /**
   * Wandelt absolute Zeit in relative Zeit um, relativ zu timeBaseInMillis
   * @param timeBaseInMillis z.b. System.currentTimeMillis()
   * @return
   */
  public AbsRelTime toRelative(long timeBaseInMillis) {
    if( isRelative ) {
      //Aufruf war sinnlos...
      return this;
    }
    return relative( timeInMillis - timeBaseInMillis);
  }

  public long getRelativeTime(TimeUnit unit) {
    if( isRelative ) {
      return unit.convert(timeInMillis, TimeUnit.MILLISECONDS);
    } else {
      return unit.convert(timeInMillis-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
  }
  
  public long getRelativeTime(TimeUnit unit, long timeBase) {
    if( isRelative ) {
      return unit.convert(timeInMillis, TimeUnit.MILLISECONDS);
    } else {
      return unit.convert(timeInMillis, TimeUnit.MILLISECONDS) -timeBase;
    }
  }
  
  public long getAbsoluteTime() {
    if( isRelative ) {
      return System.currentTimeMillis()+timeInMillis;
    } else {
      return timeInMillis;
    }
  }
  
  public long getRelativeTime(long timeBaseInMillis) {
    if( isRelative ) {
      return timeBaseInMillis+timeInMillis;
    } else {
      return timeInMillis;
    }
  }
  
  
  
}
