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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Starttimecontrolledorder;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;



public class StarttimecontrolledorderImpl extends XynaCommandImplementation<Starttimecontrolledorder> {

  public void execute(OutputStream statusOutputStream, Starttimecontrolledorder payload) throws XynaException {
    TimeZone timeZone = TimeZone.getTimeZone( "UTC" );
    
    if ( payload.getTimeZone() != null ) {
      if ( CronLikeOrderCreationParameter.verifyTimeZone(payload.getTimeZone()) ) {
        timeZone = TimeZone.getTimeZone( payload.getTimeZone() );
      } else {
        writeLineToCommandLine(statusOutputStream, "Unknown <timeZone>: " + payload.getTimeZone() + ", please refer to the command listTimeZones for a list of all supported time zones." );
        return;
      }
    }
    
    boolean useDST = payload.getUseDaylightSavingTime();
    if ( useDST && ( timeZone.getDSTSavings() == 0 ) ) {
      writeLineToCommandLine(statusOutputStream, "Invalid <useDaylightSavingTime>: " + payload.getUseDaylightSavingTime() + ", daylight saving time can only be used in time zones that actually use it." );
      return;
    }
    
    Long firstStartupTime = null;
    boolean isISOTimeStamp = false;
    
    try {
      firstStartupTime = new Long(payload.getFirstExecutionTime());
    } catch (NumberFormatException e) {
      try {
        firstStartupTime = CronLikeOrderCreationParameter.parseDate(payload.getFirstExecutionTime(), timeZone);
        isISOTimeStamp = true;
      } catch ( RuntimeException re ) {
        writeLineToCommandLine(statusOutputStream, "Unparsable <firstExecutionTime>: " + payload.getFirstExecutionTime() + ", must be Long or ISO timestamp.");
      }
    }
    
    if ( !isISOTimeStamp && ( payload.getTimeZone() != null ) ) {
      writeLineToCommandLine(statusOutputStream, "Time zones are not supported when start time is specified in milliseconds since 1970.");
      return;
    }
    
    if ( isISOTimeStamp && ( payload.getTimeZone() == null ) ) {
      writeLineToCommandLine(statusOutputStream, "WARNING: No time zone specified, assuming UTC." );
    }

    Long offset = Long.valueOf(0);
    
    if (payload.getRelative()) {
      if ( isISOTimeStamp ) {
        writeLineToCommandLine(statusOutputStream, "Relative times are not supported in conjuction with ISO timestamps. Use milliseconds instead.");
        return;
      } else {
        offset = System.currentTimeMillis();
      }
    }
    
    Boolean enabled = null;
    if (payload.getDisabled()) {
      enabled = Boolean.FALSE;
    } else {
      enabled = Boolean.TRUE;
    }
    
    OnErrorAction onError = null;
    if (payload.getOnError() == null) {
      onError = OnErrorAction.DISABLE;
    } else {
      try {
        onError = OnErrorAction.valueOf(payload.getOnError().toUpperCase());
      } catch (IllegalArgumentException e) {
        writeToCommandLine(statusOutputStream, "Invalid value for onError '" + payload.getOnError()
            + "'; Disable, Drop and Ignore are valid values.\n");
        return;
      }
    }

    String label = null;
    if (payload.getLabel() == null) {
      label = payload.getOrderType();
    } else {
      label = payload.getLabel();
    }

    Long startTime = null;
    if (firstStartupTime == null) {
      startTime = offset;
    } else {
      startTime = offset + firstStartupTime;
    }

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.getRuntimeContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    long revision;
    try {
      revision = revisionManagement.getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      writeLineToCommandLine(statusOutputStream,
                               "RuntimeContext not found: " + runtimeContext);
      return;
    }

    GeneralXynaObject orderPayload = null;
    if (payload.getInputPayloadFile() != null) {
      File inputPayloadFile = new File(payload.getInputPayloadFile());
      if (!inputPayloadFile.exists()) {
        writeLineToCommandLine(statusOutputStream, "Specified file for input payload <" + payload.getInputPayloadFile()
            + "> does not exist.");
        return;
      }
      Document doc = XMLUtils.parse(inputPayloadFile);
      Element root = doc.getDocumentElement();
      List<Element> payloadObjects = XMLUtils.getChildElements(root);

      if (payloadObjects.size() == 1) {
        orderPayload = XynaObject.generalFromXml(XMLUtils.getXMLString(payloadObjects.get(0), false), revision);
      } else if (payloadObjects.size() > 1) {
        GeneralXynaObject[] xynaObjectPayloadList = new GeneralXynaObject[payloadObjects.size()];
        for (int i = 0; i < payloadObjects.size(); i++) {
          xynaObjectPayloadList[i] = XynaObject.generalFromXml(XMLUtils.getXMLString(payloadObjects.get(i), false), revision);
        }
        orderPayload = new Container(xynaObjectPayloadList);
      }
    } else {
      orderPayload = new Container();
    }

    DestinationKey destinationKey = null;
    if (payload.getOrderType() != null) {
      destinationKey = new DestinationKey(payload.getOrderType(), runtimeContext);
    }
    
    CronLikeOrderCreationParameter clocp = CronLikeOrderCreationParameter.newClocpForCreate(destinationKey, orderPayload).
                    label(label).startTime(startTime).timeZoneId(timeZone.getID()).
                    calendarDefinition(payload.getCalendarDefinition()).useDST(useDST).enabled(enabled).
                    onError(onError).custom0(payload.getCustom0()).custom1(payload.getCustom1()).
                    custom2(payload.getCustom2()).custom3(payload.getCustom3()).build();
    
    CommandControl.tryLock(CommandControl.Operation.CRON_CREATE, revision);
    try {
      Channel xmcp = factory.getXynaMultiChannelPortalPortal();
      CronLikeOrder x = xmcp.startCronLikeOrder(clocp);
      if (x == null) {
        writeLineToCommandLine(statusOutputStream, "Error: could not register time controlled order.");
      } else {
        writeLineToCommandLine(statusOutputStream, "Successfully registered time controlled order.");
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.CRON_CREATE, revision);
    }
  }

}
