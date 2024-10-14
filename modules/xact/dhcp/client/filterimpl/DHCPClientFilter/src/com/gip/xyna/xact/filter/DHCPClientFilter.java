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
package com.gip.xyna.xact.filter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.DHCPClientTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.dhcp.DHCPPacket;
import xact.dhcp.client.DHCPUtils;
import xact.dhcp.client.DHCPUtils.DHCPFields;
import xprc.synchronization.CorrelationId;
import xprc.synchronization.SynchronizationAnswer;

public class DHCPClientFilter extends ConnectionFilter<DHCPClientTriggerConnection> {

  private static final long serialVersionUID = 1L;
  
  private static Logger logger = CentralFactoryLogging.getLogger(DHCPClientFilter.class);

  
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
  public FilterResponse createXynaOrder(DHCPClientTriggerConnection tc) throws XynaException {
    if (logger.isDebugEnabled()) {
      logger.debug("DHCP packet received!");
    }
    
    DHCPPacket dhcpPacket = null;
    try {
      tc.parseDhcpPacket();
      dhcpPacket = createDHCPPacket(tc);
    }
    catch (IOException e) {
      logger.warn("Could not parse dhcp packet", e);
      return FilterResponse.notResponsible();
    }

    DestinationKey dk = new DestinationKey(tc.getOrderType());
    XynaOrder order = new XynaOrder(dk, dhcpPacket);
    
    return FilterResponse.responsible(order);
  }

  private DHCPPacket createDHCPPacket(DHCPClientTriggerConnection tc) {
    DHCPPacket dp = new DHCPPacket();
    EnumMap<DHCPFields, Object> map = DHCPUtils.analyzePacket(tc.getDhcpPacket());
    if( logger.isDebugEnabled() ) {
      logger.debug( analyzedPacketToString(map) );
    }
    
    dp.setOp( (Integer)map.get(DHCPFields.op) );
    dp.setHtype((Integer) map.get(DHCPFields.htype) );
    dp.setHlen( (Integer)map.get(DHCPFields.hlen) );
    dp.setHops( (Integer)map.get(DHCPFields.hops) );
    dp.setXid( (String)map.get(DHCPFields.xid) );
    
    dp.setSecs( (Integer)map.get(DHCPFields.secs) );
    dp.setFlags( (Integer)map.get(DHCPFields.flags) );
    
    dp.setCiaddr( (String)map.get(DHCPFields.ciaddr) );
    dp.setYiaddr( (String)map.get(DHCPFields.yiaddr) );
    
    dp.setSiaddr( (String)map.get(DHCPFields.siaddr) );
    dp.setGiaddr( (String)map.get(DHCPFields.giaddr) );
    dp.setChaddr( (String)map.get(DHCPFields.chaddr) );
    dp.setSname( (String)map.get(DHCPFields.sname) );
    dp.setFile( (String)map.get(DHCPFields.file) );
    
    dp.setOptions(tc.getOptionsAsString());
    
    return dp;
  }
  
  private String analyzedPacketToString(EnumMap<DHCPFields, Object> map) {
    StringBuilder sb = new StringBuilder();
    for( DHCPFields df : DHCPFields.values() ) {
      sb.append(df).append(':').append(map.get(df)).append('\n');
    }
    return sb.toString();
  }

  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(GeneralXynaObject response, DHCPClientTriggerConnection tc) {
    //nichts zu tun
  }

  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, DHCPClientTriggerConnection tc) {
    //nichts zu tun
    //TODO loggen?
    logger.debug( e[0].getMessage() );
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "DHCP v4 Client Filter";
  }

}
