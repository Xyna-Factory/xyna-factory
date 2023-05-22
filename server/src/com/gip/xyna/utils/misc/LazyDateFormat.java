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
package com.gip.xyna.utils.misc;



import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.LruCache;



/**
 * LazyDateFormat kann Umwandlungen von long-Timestamps in Strings und zur�ck vornehmen.
 * Die eigentlichen Umwandlungen werden von SimpleDateFormat erledigt. Daf�r werden die 
 * SimpleDateFormat threadsafe gekapselt und in einem Cache aufbewahrt.
 * LazyDateFormat ist lazy, da LazyDateFormat die umgewandelten Daten samt Eingabe aufbewahrt,
 * so dass beispielsweise mehrfache toMillis(String date, String format)-Aufrufe date nicht 
 * mehr parsen m�ssen, solange date und format gleich bleiben.
 */
public class LazyDateFormat implements Serializable {

  private static final long serialVersionUID = 3518333764644088872L;

  //lastDate, lastFormat und millis m�ssen immer konsistent zusammenpassen und bilden dann einen Cache. 
  //falls erneut einer der werte umgerechnet werden soll, kann der cache verwendet werden
  private String lastDate; //indikator f�r millis-cache
  private String lastFormat;
  private long lastMillis;
  private transient DateFormat dateFormat;


  /**
   * Cache initialisieren mit zusammenpassenden Werten. Falls Werte nicht zusammenpassen, wird der Cache nicht initialisiert
   */
  public LazyDateFormat(String date, String format) {
    try {
      validate(date, format);
    } catch (RuntimeException e) {
      //ignore
    }
  }


  public LazyDateFormat() {
  }


  public void validate(String date, String format) {
    toMillis(date, format);
  }


  public String format(long millis, String format) {
    if (!isCacheUpToDate(millis, format)) {
      lastDate = getOrCreateDateFormat().format(millis);
      lastMillis = millis;
    }
    return lastDate;
  }


  public long toMillis(String date, String format) {
    if (!isCacheUpToDate(date, format)) {
      try {
        lastMillis = getOrCreateDateFormat().parse(date);
        lastDate = date;
      } catch (ParseException e) {
        throw new RuntimeException("'" + date + "' does not match Format '" + format + "'.", e);
      }
    }
    return lastMillis;
  }


  private DateFormat getOrCreateDateFormat() {
    if (dateFormat == null) {
      dateFormat = DateFormatCache.getDateFormat(lastFormat);
    }
    return dateFormat;
  }


  private boolean isCacheUpToDate(long millis, String format) {
    if (format.equals(lastFormat)) {
      return millis == lastMillis;
    }
    dateFormat = null; //dateFormat-Cache refresh erzwingen
    lastFormat = format;
    return false;
  }


  private boolean isCacheUpToDate(String date, String format) {
    if (format.equals(lastFormat)) {
      return date.equals(lastDate);
    }
    dateFormat = null; //dateFormat-Cache refresh erzwingen
    lastFormat = format;
    return false;
  }


  public static class DateFormat {

    //public static int createCnt = 0;
    //public static int formatCnt = 0;
    //public static int parseCnt = 0;

    private String format;
    private SimpleDateFormat sdf;

    private static final Pattern formatRegexp = Pattern.compile("^(.*?)((?:[|, ](?:locale|timezone)=[^,\\| ]+?)*)$");


    private static DateFormat fromFormatStringWithOptionalParameters(String s) {
      Matcher m = formatRegexp.matcher(s);
      if (m.matches()) {
        String f = m.group(1);
        String locale = null;
        String timezone = null;
        if (m.group(2).length() > 0) {
          String[] parts = m.group(2).split("[=\\|, ]"); // -> ["", "timezone", "UTC"]
          for (int i = 0; i < parts.length / 2; i++) {
            String name = parts[2 * i + 1];
            if (name == null) {
              continue;
            }
            String val = parts[2 * i + 2];
            if (name.equals("locale")) {
              locale = val;
            } else if (name.equals("timezone")) {
              timezone = val;
            }
          }
        }
        return new DateFormat(f, locale, timezone);
      } else {
        throw new RuntimeException(); //matched immer
      }
    }


    public DateFormat(String format, String locale, String timezone) {
      this.format = format;
      //++createCnt;
      Locale l;
      if (locale != null) {
        l = new Locale(locale);
      } else {
        l = Locale.getDefault();
      }

      try {
        this.sdf = new SimpleDateFormat(format, l);
      } catch (RuntimeException e) {
        throw new RuntimeException("format '" + format + "' is no valid date format.", e);
      }
      if (timezone != null) {
        this.sdf.setTimeZone(TimeZone.getTimeZone(timezone));
      }
    }


    public String getFormat() {
      return format;
    }


    public synchronized long parse(String timestamp) throws ParseException {
      //++parseCnt;
      return sdf.parse(timestamp).getTime();
    }


    public synchronized String format(long timestamp) {
      //++formatCnt;
      return sdf.format(new Date(timestamp));
    }

  }


  public static class DateFormatCache {

    static LruCache<String, DateFormat> cache = new LruCache<String, DateFormat>(16);


    public static DateFormat getDateFormat(String format) {
      DateFormat dateFormat = cache.get(format);
      if (dateFormat == null) {
        dateFormat = DateFormat.fromFormatStringWithOptionalParameters(format);
        cache.put(format, dateFormat);
      }
      return dateFormat;
    }


    public static void clear() {
      cache.clear();
    }

  }

}
