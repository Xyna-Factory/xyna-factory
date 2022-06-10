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
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.MessageBuilder;


public class StringTools {

  /**
   * inserts colons into mac address string
   */
  public static String adjustMac(String mac, Logger logger) throws RemoteException {
    if (mac.contains(":")) {
      logger.error("Wrong format for Mac Address: No colons allowed.");
      throw new DPPWebserviceIllegalArgumentException(new MessageBuilder().setDescription(
          "Wrong format for Mac Address: No colons allowed.").setErrorNumber("00205"));
    }
    StringBuilder builder = new StringBuilder();
    int i=0;
    for (; i <= mac.length()-3; i=i+2) {
      builder.append(mac.substring(i, i+2));
      builder.append(":");
    }
    builder.append(mac.substring(i, mac.length()));
    String ret = builder.toString();
    ret = ret.toLowerCase();
    logger.info("Transformed Mac String (" + mac + ") to: " + ret);
    return ret;
  }

  public static String transformSearchString(String searchStr, String separator, Logger logger)
        throws RemoteException {
    return transformSearchString(searchStr, separator, "", true, logger);
  }

  public static String transformSearchStringNoCheck(String searchStr, String separator, Logger logger)
        throws RemoteException {
    return transformSearchString(searchStr, separator, "", false, logger);
  }


  public static String transformSearchString(String searchStr, String separator, String after,
      Logger logger) throws RemoteException {
    return transformSearchString(searchStr, separator, after, true, logger);
  }

  /**
   * splits searchStr at commas, and concats parts again, by inserting 'separator' and 'after'
   * parameters inbetween; 'after'-string also after last part
   */
  public static String transformSearchString(String searchStr, String separator, String after,
        boolean doCheckCharacters, Logger logger) throws RemoteException {
    String[] values = splitCSV(searchStr);
    if (doCheckCharacters) {
      checkCharacters(values, logger);
    }
    String ret = concat(values, separator, after);
    logger.info("Transformed searchString (" + searchStr + ") to: " + ret);
    return ret;
  }

  private static String[] splitCSV(String csv) {
    return csv.split(",");
  }

  private static boolean checkCharacters(String[] values, Logger logger)
        throws RemoteException {
    for (String val : values) {
      Pattern patt = Pattern.compile("\\w*");
      if (!patt.matcher(val).matches()) {
        logger.error("Only alpha-numeric characters are allowed in SearchString.");
        throw new DPPWebserviceIllegalArgumentException(
          "Only alpha-numeric characters are allowed in SearchString.");
      }
    }
    return true;
  }

  private static String concat(String[] values, String separator, String after) {
    if (values.length < 1) {
      return "";
    }
    StringBuilder ret = new StringBuilder(values[0]);
    ret.append(after);
    for (int i=1; i < values.length; i++) {
      ret.append(separator);
      ret.append(values[i]);
      ret.append(after);
    }
    return ret.toString();
  }
}
