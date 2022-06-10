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
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.tools.PropertiesHandler;



public class GrepLogsCommandsv6 {

  public static class Constants {
    public static class PropertyNames {
      public static final String DHCPDV6_LOG_DIR = "ws.grep.logs.v6.dhcpd.v6.log.directory";
      public static final String DHCPDV6_LOG_FILE_NAME_NO_SUFFIX =
        "ws.grep.logs.v6.dhcpd.v6.log.file.name.no.suffix";
    }
  }

  protected String _dhcpdv6LogDir = null;
  protected String _dhcpdv6LogFileNameBase = null;


  public GrepLogsCommandsv6(Properties wsProperties, Logger logger) throws RemoteException {
    _dhcpdv6LogDir = PropertiesHandler.getProperty(wsProperties, Constants.PropertyNames.DHCPDV6_LOG_DIR,
                                                   logger);
    _dhcpdv6LogFileNameBase = PropertiesHandler.getProperty(wsProperties,
                              Constants.PropertyNames.DHCPDV6_LOG_FILE_NAME_NO_SUFFIX, logger);
  }


  public ShellCommand buildGrepDhcpdLogsForToday(int numDays, String searchStr, Logger logger)
                             throws RemoteException {
    //NumDaysCommandDhcpdLog
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(_dhcpdv6LogDir);
    shell.addLogicalAnd();

    shell.addCommand("find").option("-mtime", "-" + numDays)
                            .option("-name", "'" + _dhcpdv6LogFileNameBase + "*'")
                            .option("-print0");
    shell.addPipeSign();

    //shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("xargs").option("-0").option("-P", 1).option("-n", 1);
    shell.addCommand("bzgrep").parameter(CommandTools.buildGrepConditionSearchStr(searchStr, logger));
    shell.addPipeSign();

    /*
    shell.addCommand("sort").option("-n").option("--key=1,3");
    shell.addPipeSign();
    */
    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }


  public ShellCommand buildGrepDhcpdLogsForDate(String inputDate, String searchStr, Logger logger)
                             throws RemoteException {
    //DateCommandDhcpdLog
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(_dhcpdv6LogDir);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       _dhcpdv6LogFileNameBase + "*", inputDate, logger))
                            .option("-print0");
    shell.addPipeSign();

    //shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("xargs").option("-0").option("-P", 1).option("-n", 1);
    shell.addCommand("bzgrep").option("-E").parameter("'" + DateCommandTools.getDateMMMd(inputDate, logger) + "'");
    shell.addPipeSign();

    if (searchStr != null) {
      shell.addCommand("grep").parameter(CommandTools.buildGrepConditionSearchStr(searchStr, logger));
      shell.addPipeSign();
      /*
      shell.addCommand("sort").option("-n").option("--key=1,3");
      shell.addPipeSign();
      */
    }

    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }



  public ShellCommand buildGrepDhcpdLogsForDateHours(String inputDate, int startHour, int endHour,
                                                            String searchStr, Logger logger)
                             throws RemoteException {
    //DateHoursCommandDhcpdLogForSearchString
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(_dhcpdv6LogDir);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       _dhcpdv6LogFileNameBase + "*", inputDate, logger))
                            .option("-print0");
    shell.addPipeSign();

    //shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("xargs").option("-0").option("-P", 1).option("-n", 1);
    shell.addCommand("bzgrep").parameter(CommandTools.buildGrepHoursCondition(startHour, endHour, inputDate,
                                                                              logger));
    shell.addPipeSign();

    if (searchStr != null) {
      shell.addCommand("grep").parameter(CommandTools.buildGrepConditionSearchStr(searchStr, logger));
      shell.addPipeSign();
      /*
      shell.addCommand("sort").option("-n").option("--key=1,3");
      shell.addPipeSign();
      */
    }

    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }


  public ShellCommand buildGrepDhcpdLogsForMac(String inputDate, String mac,
                                                      Logger logger) throws RemoteException {
    //DateCommandDhcpdLogForMac
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(_dhcpdv6LogDir);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       _dhcpdv6LogFileNameBase + "*", inputDate, logger))
                            .option("-print0");
    shell.addPipeSign();

    //shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("xargs").option("-0").option("-P", 1).option("-n", 1);
    shell.addCommand("bzgrep").option("-E")
                     .parameter("'" + DateCommandTools.getDateMMMd(inputDate, logger) + "'");
    shell.addPipeSign();

    if (mac != null) {
      shell.addCommand("grep").parameter(CommandTools.buildGrepConditionMac(mac, logger));
      shell.addPipeSign();

      /*
      shell.addCommand("sort").option("-n").option("--key=1,3");
      shell.addPipeSign();
      */
    }
    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }


  public ShellCommand buildGrepDhcpdLogsForMacHours(String inputDate, int startHour, int endHour,
                                                           String mac, Logger logger) throws RemoteException {
    //DateHoursCommandDhcpdLogForMac
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(_dhcpdv6LogDir);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       _dhcpdv6LogFileNameBase + "*", inputDate, logger))
                            .option("-print0");
    shell.addPipeSign();

    //shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("xargs").option("-0").option("-P", 1).option("-n", 1);
    shell.addCommand("bzgrep").parameter(CommandTools.buildGrepHoursCondition(startHour, endHour, inputDate,
                                                                              logger));
    shell.addPipeSign();

    if (mac != null) {
      shell.addCommand("grep").parameter(CommandTools.buildGrepConditionMac(mac, logger));
      shell.addPipeSign();

      /*
      shell.addCommand("sort").option("-n").option("--key=1,3");
      shell.addPipeSign();
      */
    }
    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }

}
