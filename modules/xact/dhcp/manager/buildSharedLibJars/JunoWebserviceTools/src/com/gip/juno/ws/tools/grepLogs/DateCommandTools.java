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

package com.gip.juno.ws.tools.grepLogs;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;


public class DateCommandTools {

  public static class ParseDateResult {
    Date date;
    SimpleDateFormat format;
  }


  public static String getLogfilesPatternStringForGrep(String logname, String inputDate, Logger logger)
        throws RemoteException {
    String ret = "";
    List<String> files = getLogfilesPatternStrings(logname, inputDate, logger);
    for (String s : files) {
      ret = ret + " " + s;
    }
    return ret;
  }

  public static String getLogfilesPatternStringForFind(String logname, String inputDate, Logger logger)
        throws RemoteException {
    List<String> files = getLogfilesPatternStrings(logname, inputDate, logger);
    if (files.size() < 1) {
      //should not happen
      return logname + ".log";
    }
    String ret = " \\( -name '" + files.get(0) + "' ";
    for (int i = 1; i < files.size(); i++) {
      ret += " -or -name '" + files.get(i) + "' ";
    }
    ret += " \\) ";
    return ret;
  }

  public static List<String> getLogfilesPatternStrings(String logname, String inputDate, Logger logger)
        throws RemoteException {
    List<String> ret = new ArrayList<String>();
    if (isToday(inputDate, logger)) {
      ret.add(logname + "*" + inputDate + "*");
      ret.add(logname + ".log");
      return ret;
    }
    else if (isYesterday(inputDate, logger)) {
      ret.add(logname + "*" + inputDate + "*");
      ret.add(logname + "*" + getNextDay(inputDate, logger) + "*");
      ret.add(logname + ".log");
      return ret;
    }
    ret.add(logname + "*" + inputDate + "*");
    ret.add(logname + "*" + getNextDay(inputDate, logger) + "*");
    return ret;
  }

  public static ParseDateResult parseDate(String inputDate, Logger logger) throws RemoteException {
    ParseDateResult ret = new ParseDateResult();
    if (inputDate.length() == 6) {
      ret.format = new SimpleDateFormat("yyMMdd", Locale.ENGLISH);
    } else if (inputDate.length() == 8) {
      ret.format = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    } else {
      throw new DPPWebserviceIllegalArgumentException("Date in string " + inputDate
          + " has wrong format, expected either format yyyyMMdd or format yyMMdd");
    }
    try {
      ret.date =  ret.format.parse(inputDate);
      return ret;
    }
    catch (Exception e) {
      logger.error("Date in string " + inputDate + " has wrong format, expected Format yyMMdd");
      throw new DPPWebserviceIllegalArgumentException("Date in string " + inputDate
          + " has wrong format, expected Format yyMMdd");
    }
  }

  public static String getNextDay(String inputDate, Logger logger) throws RemoteException {
    ParseDateResult parsed = parseDate(inputDate, logger);
    try {
      GregorianCalendar cal = new GregorianCalendar();
      cal.setTime(parsed.date);
      cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
      return parsed.format.format(cal.getTime());
    } catch (Exception e) {
      throw new DPPWebserviceUnexpectedException("Error while formatting date string", e);
    }
  }

  public static boolean isToday(String inputDate, Logger logger) throws RemoteException {
    ParseDateResult parsed = parseDate(inputDate, logger);
    try {
      GregorianCalendar cal = new GregorianCalendar();
      String todayFormated = parsed.format.format(cal.getTime());
      cal.setTime(parsed.date);
      String inputFormated = parsed.format.format(cal.getTime());
      if (todayFormated.equals(inputFormated)) {
        return true;
      }
      return false;
    } catch (Exception e) {
      throw new DPPWebserviceUnexpectedException("Error while formatting date string", e);
    }
  }

  public static boolean isYesterday(String inputDate, Logger logger) throws RemoteException {
    ParseDateResult parsed = parseDate(inputDate, logger);
    try {
      GregorianCalendar cal = new GregorianCalendar();
      cal.add(GregorianCalendar.DAY_OF_MONTH, -1);
      String yesterdayFormated = parsed.format.format(cal.getTime());
      cal.setTime(parsed.date);
      String inputFormated = parsed.format.format(cal.getTime());
      if (yesterdayFormated.equals(inputFormated)) {
        return true;
      }
      return false;
    } catch (Exception e) {
      throw new DPPWebserviceUnexpectedException("Error while formatting date string", e);
    }
  }


  public static String getDateMMMd(String inputDate, Logger logger) throws RemoteException {
    ParseDateResult parsed = parseDate(inputDate, logger);
    try {
      SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
      String month =  monthFormat.format(parsed.date);
      SimpleDateFormat dayFormat = new SimpleDateFormat("d");
      String day = dayFormat.format(parsed.date);
      return month + "[[:space:]]*" + day;
    } catch (Exception e) {
      throw new DPPWebserviceUnexpectedException("Error while formatting date string", e);
    }
  }
}
