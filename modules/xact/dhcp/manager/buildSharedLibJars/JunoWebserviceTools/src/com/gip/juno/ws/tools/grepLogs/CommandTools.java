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

import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.MessageBuilder;


public class CommandTools {


  public static String buildGrepConditionMacSearchStr(String mac, String searchStr, Logger logger)
                     throws RemoteException {
    String ret = "";
    if ((mac == null) && (searchStr == null)) {
      ret = " ' ' ";
    } else if (mac == null) {
      ret = " -E -i '" + StringTools.transformSearchString(searchStr, "|", logger) + "'";
    } else if (searchStr == null) {
      ret = StringTools.adjustMac(mac, logger);
    } else {
      String after = ".*" + StringTools.adjustMac(mac, logger);
      String transformed = StringTools.transformSearchString(searchStr, "|", after, logger);
      ret = " -E -i '" + transformed + "'";
    }
    return ret;
  }


  public static String buildGrepConditionSearchStr(String searchStr, Logger logger) throws RemoteException {
    String ret = "";
    if (searchStr == null) {
      ret = " ' ' ";
    }
    else {
      ret = " -E -i '" + StringTools.transformSearchStringNoCheck(searchStr, "|", logger) + "'";
    }
    return ret;
  }


  public static String buildGrepConditionMac(String mac, Logger logger) throws RemoteException {
    String ret = "";
    if (mac == null) {
      ret = " ' ' ";
    }
    else {
      ret = StringTools.adjustMac(mac, logger);
    }
    return ret;
  }


  public static String buildGrepHoursCondition(int startHour, int endHour, String inputDate, Logger logger)
        throws RemoteException {
    StringBuilder ret = new StringBuilder("");
    if (startHour >= endHour) {
      MessageBuilder builder = new MessageBuilder();
      builder.setErrorNumber("00209");
      builder.setDescription("Start hour must be before end hour.");
      logger.error("Start hour must be before end hour.");
      throw new DPPWebserviceIllegalArgumentException(builder);
    }
    String date = DateCommandTools.getDateMMMd(inputDate, logger);
    ret.append(" -E '");
    for (int i = startHour; i < endHour; i++) {
      String hour = Integer.toString(i);
      if (i < 10) {
        hour = "0" + hour;
      }
      if (i > startHour) {
        ret.append("|");
      }
      ret.append(date).append(" ").append(hour);
    }
    ret.append("' ");
    return ret.toString();
  }

}
