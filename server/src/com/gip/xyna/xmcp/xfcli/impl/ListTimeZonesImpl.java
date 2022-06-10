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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.TimeZone;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.ListTimeZones;



public class ListTimeZonesImpl extends XynaCommandImplementation<ListTimeZones> {

  public void execute(OutputStream statusOutputStream, ListTimeZones payload) throws XynaException {
    String timeZoneIDs[] = TimeZone.getAvailableIDs();

    int utc = TimeZone.getTimeZone("UTC").getRawOffset();
    int local = TimeZone.getDefault().getRawOffset();

    TimeZone localTZ = TimeZone.getDefault();
    String str;

    if (payload.getVerbose()) {
      str = String.format("%-25s  %-10s  %s  %s", "TimeZone ID", "GMT-Offset", "has DST", "is current TimeZone");
      writeLineToCommandLine(statusOutputStream, str);
      writeLineToCommandLine(statusOutputStream, "===================================================================");
    }

    for (String timeZoneID : timeZoneIDs) {
      TimeZone tz = TimeZone.getTimeZone(timeZoneID);

      int rawOffset = tz.getRawOffset();

      if (payload.getWorld() || (local == rawOffset) || (utc == rawOffset)) {
        if (payload.getVerbose()) {
          str = String.format("%-31s GMT%+02d %9s %s", timeZoneID, tz.getOffset(0) / 1000 / 60 / 60, (tz.getDSTSavings() != 0) ? "(has DST)" : "", (localTZ.equals(tz)) ? "LOCAL TIME ZONE" : "");
        } else {
          str = timeZoneID;
        }

        writeLineToCommandLine(statusOutputStream, str);
      }
    }
  }

}
