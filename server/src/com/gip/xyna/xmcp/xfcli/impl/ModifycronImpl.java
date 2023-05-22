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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.TimeZone;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Modifycron;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;



public class ModifycronImpl extends XynaCommandImplementation<Modifycron> {

  @Override
  public void execute(OutputStream statusOutputStream, Modifycron payload) throws XynaException {
    Long id = null;
    try {
      id = new Long(payload.getId());
    }
    catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, "Unparsable ID: " + payload.getId() + ", must be Long.");
      return;
    }

    Long interval = null;
    if (payload.getInterval() != null) {
      try {
        interval = CronLikeOrderCreationParameter.parseInterval(payload.getInterval());
      } catch ( RuntimeException e ) {
        writeLineToCommandLine(statusOutputStream, "Unparsable <interval>: " + payload.getInterval() + ", must be Long with optional suffix [ms|s|m|h|d].");
      }
    }

    TimeZone timeZone = TimeZone.getTimeZone( "UTC" );
    
    if ( payload.getTimeZone() != null ) {
      if ( CronLikeOrderCreationParameter.verifyTimeZone(payload.getTimeZone()) ) {
        timeZone = TimeZone.getTimeZone( payload.getTimeZone() );
      } else {
        writeLineToCommandLine(statusOutputStream, "Unknown <timeZone>: " + payload.getTimeZone() + ", please refer to the command listTimeZones for a list of all supported time zones." );
        return;
      }
    }
    
    Boolean useDST = null;
    if (payload.getDst() != null) {
      useDST = Boolean.parseBoolean(payload.getDst());
    }
    
    if ((useDST != null) && (interval != null)) {
      if (useDST && !CronLikeOrderCreationParameter.verifyIntervalQualifiesForDST(interval)) {
        writeLineToCommandLine(statusOutputStream, "Invalid <useDaylightSavingTime>: " + payload.getDst()
            + ", daylight saving time can only be used for intervals that are multiple of whole days.");
        return;
      }
    }
    
    if ((useDST != null) && (payload.getTimeZone() != null)) {
      if (useDST && (timeZone.getDSTSavings() == 0)) {
        writeLineToCommandLine(statusOutputStream,
                               "Invalid <useDaylightSavingTime>: " + payload.getDst()
                                   + ", daylight saving time can only be used in time zones that actually use it.");
        return;
      }
    }
    
    Long firstStartupTime = null;

    if (payload.getFirstExecutionTime() != null) {
      try {
        firstStartupTime = new Long(payload.getFirstExecutionTime());

        if (firstStartupTime == 0) {
          firstStartupTime = System.currentTimeMillis();
        }
      } catch (NumberFormatException e) {
        try {
          firstStartupTime = CronLikeOrderCreationParameter.parseDate(payload.getFirstExecutionTime(), timeZone);
        } catch ( RuntimeException re ) {
          writeLineToCommandLine(statusOutputStream, "Unparsable <firstExecutionTime>: " + payload.getFirstExecutionTime() + ", must be Long or ISO timestamp.");
        }
      }
    }
    
    
    Boolean enabled = null;
    if (payload.getEnabled() != null) {
      enabled = Boolean.parseBoolean(payload.getEnabled());
    }

    OnErrorAction onError = null;
    if (payload.getOnError() != null) {
      try {
        onError = OnErrorAction.valueOf(payload.getOnError().toUpperCase());
      }
      catch (IllegalArgumentException e) {
        writeToCommandLine(
                           statusOutputStream,
                           "Invalid value for onError '" + payload.getOnError() + "'; Disable, Drop and Ignore are valid values.\n");
      }
    }

    String label = null;
    if (payload.getLabel() != null) {
      label = payload.getLabel();
    }

    DestinationKey destination = null;
    if (payload.getOrderType() != null) {
      destination = new DestinationKey(payload.getOrderType());
    }

    Channel xmcp = factory.getXynaMultiChannelPortalPortal();
    CronLikeOrder x =
        xmcp.modifyCronLikeOrder(id, label, destination, null, firstStartupTime, payload.getTimeZone(), interval,
                                 useDST, enabled, onError, payload.getCustom0(), payload.getCustom1(),
                                 payload.getCustom2(), payload.getCustom3());
    if (x == null) {
      writeLineToCommandLine(statusOutputStream, "Error: could not modify cron like order.");
    }
    else {
      writeLineToCommandLine(statusOutputStream, "Successfully modified cron like order.");
    }
  }

}
