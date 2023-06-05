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
package xact.dhcp.client.impl;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;

import xact.dhcp.enums.DHCPMessageType;
import xact.dhcp.enums.DHCPMessageType_ACK;
import xact.dhcp.enums.DHCPMessageType_DECLINE;
import xact.dhcp.enums.DHCPMessageType_DISCOVER;
import xact.dhcp.enums.DHCPMessageType_INFORM;
import xact.dhcp.enums.DHCPMessageType_NAK;
import xact.dhcp.enums.DHCPMessageType_OFFER;
import xact.dhcp.enums.DHCPMessageType_OTHER;
import xact.dhcp.enums.DHCPMessageType_RELEASE;
import xact.dhcp.enums.DHCPMessageType_REQUEST;
import xact.dhcp.enums.DHCPMessageType_REQUEST_ForRenew;

public enum MessageType {
  
  DISCOVER(1, DHCPMessageType_DISCOVER.class),
  OFFER(   2, DHCPMessageType_OFFER.class),
  REQUEST( 3, DHCPMessageType_REQUEST.class),
  DECLINE( 4, DHCPMessageType_DECLINE.class),
  ACK(     5, DHCPMessageType_ACK.class),
  NAK(     6, DHCPMessageType_NAK.class),
  RELEASE( 7, DHCPMessageType_RELEASE.class),
  INFORM(  8, DHCPMessageType_INFORM.class),

  REQUEST_RENEW( 3, DHCPMessageType_REQUEST_ForRenew.class),
  OTHER( 99, DHCPMessageType_OTHER.class)
  ;

  private static final Logger logger = CentralFactoryLogging.getLogger(MessageType.class);

  private Class<? extends DHCPMessageType> clazz;
  private String value;

  private MessageType(int value, Class<? extends DHCPMessageType> clazz ) {
    this.value = String.valueOf(value);
    this.clazz = clazz;
  }
  
  public String getValue() {
    return value;
  }
  
  public static MessageType valueOf(DHCPMessageType messageType) {
    if( messageType == null ) {
      throw new IllegalArgumentException(" no messageType");
    }
    for( MessageType mt : values() ) {
      if( messageType.getClass().equals(mt.clazz) ) {
        return mt;
      }
    }
    return OTHER;
  }

  public static DHCPMessageType instanceFor(String value) {
    for( MessageType mt : values() ) {
      if( mt.value.equals(value) ) {
        return mt.newInstance();
      }
    }
    return MessageType.OTHER.newInstance();
  }

  private DHCPMessageType newInstance() {
    try {
      return clazz.newInstance();
    } catch (Exception e) { //InstantiationException, IllegalAccessException
      logger.warn("Could not instantiate DHCPMessageType for " + name() );
      return new DHCPMessageType_OTHER();
    }
  }

}
