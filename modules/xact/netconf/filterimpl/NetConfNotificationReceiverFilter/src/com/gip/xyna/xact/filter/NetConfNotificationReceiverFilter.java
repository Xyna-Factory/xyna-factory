/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package com.gip.xyna.xact.filter;


import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.NetConfNotificationReceiverTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.netconf.datatypes.NetConfEvent;


public class NetConfNotificationReceiverFilter extends ConnectionFilter<NetConfNotificationReceiverTriggerConnection> {

  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverFilter.class);

  private String regex_Valid = "(<notification[\\w\\W]*<\\/notification>)";
  private String regex_EventTime = "<notification[\\w\\W]*<eventTime>([\\w\\W]*)<\\/eventTime>[\\w\\W]*<\\/notification>";


  /**
   * Called to create a configuration template to parse configuration and show configuration options.
   * @return NetConfNotificationReceiverConfigurationParameter template
   */
  @Override
  public FilterConfigurationParameter createFilterConfigurationTemplate() {
    return new NetConfNotificationReceiverConfigurationParameter();
  }


  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * This method returns a FilterResponse object, which includes the XynaOrder if the filter is responsible for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but the request is handled without creating a XynaOrder the 
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the request should be handled by an older version of the filter in another application version, the returned
   *    object must be: FilterResponse.responsibleButTooNew().
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         Results in onError() being called by Xyna Processing.
   */
  @Override
  public FilterResponse createXynaOrder(NetConfNotificationReceiverTriggerConnection tc, FilterConfigurationParameter baseConfig)
      throws XynaException {

    FilterResponse answer = FilterResponse.notResponsible();
    XynaOrder xynaOrder = null;

    String FilterTargetWF = "";
    try {
      FilterTargetWF = tc.getFilterTargetWF();
    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "Filter - createXynaOrder failed", ex);
    }

    DestinationKey destKey = new DestinationKey(FilterTargetWF);

    String RD_IP = "";
    String RD_ID = "";
    long longEventTime = 0;
    String message = "";
    try {
      RD_IP = tc.getIP();
      RD_ID = tc.getID();
      message = tc.getMessage();
    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "Filter - createXynaOrder failed", ex);
    }

    Pattern pattern_valid = Pattern.compile(regex_Valid);
    Matcher matcher_valid = pattern_valid.matcher(message);
    if (matcher_valid.matches()) {

      try {
        String valid_message = matcher_valid.group(1);

        Pattern pattern_EventTime = Pattern.compile(regex_EventTime);
        Matcher matcher_EventTime = pattern_EventTime.matcher(valid_message);

        String eventTime = "N/A";
        if (matcher_EventTime.matches()) {
          
          eventTime = matcher_EventTime.group(1);
          
          try {
            Instant instant1 = Instant.parse(eventTime);
            longEventTime = instant1.toEpochMilli();
          } catch(Exception ex1) {
            try {
              DateTimeFormatter formatter0 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
              ZonedDateTime zonedtime = ZonedDateTime.parse(eventTime, formatter0);
              Instant instant2 = zonedtime.toInstant();
              longEventTime = instant2.toEpochMilli();
            } catch(Exception ex2) {
              logger.warn("NetConfNotificationReceiver: " + "Filter - createXynaOrder - EventTime failed", ex2);
            }
          }
          
        }
      } catch (Exception ex) {
        logger.warn("NetConfNotificationReceiver: " + "Filter - createXynaOrder failed", ex);
      }

      NetConfEvent event = new NetConfEvent();
      event.setIP(RD_IP);
      event.setDeviceID(RD_ID);
      event.setEventTime(longEventTime);
      event.setEvent(message);

      xynaOrder = new XynaOrder(destKey, event);
      answer = FilterResponse.responsible(xynaOrder);
    }
    return answer;
  }

  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  @Override
  public void onResponse(GeneralXynaObject response, NetConfNotificationReceiverTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }


  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, NetConfNotificationReceiverTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    //TODO implementation
    //TODO update dependency xml file
    return "Filter configured via NetConfNotificationReceiver";
  }


  /**
   * Called once for each filter instance when it is deployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  @Override
  public void onDeployment(EventListener triggerInstance) {
    super.onDeployment(triggerInstance);
  }


  /**
   * Called once for each filter instance when it is undeployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  @Override
  public void onUndeployment(EventListener triggerInstance) {
    super.onUndeployment(triggerInstance);
  }

}
