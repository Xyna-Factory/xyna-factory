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

import org.apache.log4j.Logger;


public class GrepLogsCommands {

  public interface Constants {
    public interface Log {
      public interface Xyna {
        public static final String DIR = "/var/log/";
        public static final String NAME = "xyna";
      }

      public interface Dhcpd {
        public static final String DIR = "/opt/XynaFactory/persistent/";
        public static final String NAME = "dhcpd";
      }
    }
  }


  public static ShellCommand buildGrepLogsForMac(int numDays, String mac, Logger logger)
                             throws RemoteException {
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Xyna.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").option("-mtime", "-" + numDays).option("-name", "'" + Constants.Log.Xyna.NAME + "*'")
                            .option("-print0");
    shell.addPipeSign();

    shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("bzgrep").option("-i").parameter(mac);
    shell.addPipeSign();

    shell.addCommand("sort").option("-n");
    shell.addPipeSign();

    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }


  public static ShellCommand buildGrepLogsForNumDays(int numDays, String mac, String searchStr, Logger logger)
                             throws RemoteException {
    //NumDaysCommandXynaLog
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Xyna.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").option("-mtime", "-" + numDays)
                            .option("-name", "'" + Constants.Log.Xyna.NAME + "*'")
                            .option("-print0");
    shell.addPipeSign();

    shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("bzgrep").parameter(CommandTools.buildGrepConditionMacSearchStr(mac, searchStr, logger));
    shell.addPipeSign();

    shell.addCommand("sort").option("-n").option("--key=1,3");
    shell.addPipeSign();

    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }


  public static ShellCommand buildGrepLogsForDate(String inputDate, String mac, String searchStr, Logger logger)
                             throws RemoteException {
    //DateCommandXynaLog
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Xyna.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(Constants.Log.Xyna.NAME,
                                                                                        inputDate, logger))
                            .option("-print0");
    shell.addPipeSign();

    shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("bzgrep").option("-E")
                              .parameter("'" + DateCommandTools.getDateMMMd(inputDate, logger) + "'");
    shell.addPipeSign();

    if ((mac != null) || (searchStr != null)) {
      shell.addCommand("grep").parameter(CommandTools.buildGrepConditionMacSearchStr(mac, searchStr, logger));
      shell.addPipeSign();
      shell.addCommand("sort").option("-n").option("--key=1,3");
      shell.addPipeSign();
    }

    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }


  public static ShellCommand buildGrepLogsForDateHours(String inputDate, int startHour, int endHour,
                                                       String mac, String searchStr, Logger logger)
                             throws RemoteException {
    //DateHoursCommandXynaLog
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Xyna.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(Constants.Log.Xyna.NAME,
                                                                                        inputDate, logger))
                            .option("-print0");
    shell.addPipeSign();

    shell.addCommand("xargs").option("-0").option("-P", 5).option("-n", 1);
    shell.addCommand("bzgrep").parameter(CommandTools.buildGrepHoursCondition(startHour, endHour, inputDate,
                                                                              logger));
    shell.addPipeSign();

    if ((mac != null) || (searchStr != null)) {
      shell.addCommand("grep").parameter(CommandTools.buildGrepConditionMacSearchStr(mac, searchStr, logger));
      shell.addPipeSign();
      shell.addCommand("sort").option("-n").option("--key=1,3");
      shell.addPipeSign();
    }

    shell.addCommand("tail").option("-1000");
    
    shell.addPipeSign();
    shell.addCommand("strings");

    return shell;
  }



  public static ShellCommand buildGrepDhcpdLogsForToday(int numDays, String searchStr, Logger logger)
                             throws RemoteException {
    //NumDaysCommandDhcpdLog
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Dhcpd.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").option("-mtime", "-" + numDays)
                            .option("-name", "'" + Constants.Log.Dhcpd.NAME + "*'")
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


  public static ShellCommand buildGrepDhcpdLogsForDate(String inputDate, String searchStr, Logger logger)
                             throws RemoteException {
    //DateCommandDhcpdLog
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Dhcpd.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       Constants.Log.Dhcpd.NAME + "*", inputDate, logger))
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



  public static ShellCommand buildGrepDhcpdLogsForDateHours(String inputDate, int startHour, int endHour,
                                                            String searchStr, Logger logger)
                             throws RemoteException {
    //DateHoursCommandDhcpdLogForSearchString
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Dhcpd.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       Constants.Log.Dhcpd.NAME + "*", inputDate, logger))
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


  public static ShellCommand buildGrepDhcpdLogsForMac(String inputDate, String mac,
                                                      Logger logger) throws RemoteException {
    //DateCommandDhcpdLogForMac
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Dhcpd.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       Constants.Log.Dhcpd.NAME + "*", inputDate, logger))
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


  public static ShellCommand buildGrepDhcpdLogsForMacHours(String inputDate, int startHour, int endHour,
                                                           String mac, Logger logger) throws RemoteException {
    //DateHoursCommandDhcpdLogForMac
    ShellCommand shell = new ShellCommand();

    shell.addCommand("cd").parameter(Constants.Log.Dhcpd.DIR);
    shell.addLogicalAnd();

    shell.addCommand("find").parameter(DateCommandTools.getLogfilesPatternStringForFind(
                                       Constants.Log.Dhcpd.NAME + "*", inputDate, logger))
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
