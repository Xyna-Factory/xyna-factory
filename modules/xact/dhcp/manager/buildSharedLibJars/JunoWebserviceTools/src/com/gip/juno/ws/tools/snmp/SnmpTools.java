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


package com.gip.juno.ws.tools.snmp;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.SnmpAccessData.SADBuilder;
import com.gip.xyna.utils.snmp.exception.SnmpResponseException;
import com.gip.xyna.utils.snmp.manager.SnmpContext;
import com.gip.xyna.utils.snmp.manager.SnmpContextImplApache;
import com.gip.xyna.utils.snmp.varbind.*;



public class SnmpTools {

  private static final String SNMP_COMMUNITY_READ = "ws.cm.management.snmp.community.read";
  private static final String SNMP_COMMUNITY_READWRITE = "ws.cm.management.snmp.community.readwrite";

  private static String _communityRead = "public";
  private static String _communityReadWrite = "public";

  private static final int SOCKET_TIMEOUT = 1000;

  public static String getCommunityRead() {
    try {
      Properties properties = PropertiesHandler.getWsProperties();
      _communityRead = properties.getProperty(SNMP_COMMUNITY_READ, "public");
    }
    catch (Exception e) {
      Logger.getLogger(SnmpTools.class).error(
            "Error while loading community string. Is file 'xyna.ws.properties' accessible?", e);
    }
    return _communityRead;
  }


  public static String getCommunityReadWrite() {
    try {
      Properties properties = PropertiesHandler.getWsProperties();
      _communityReadWrite = properties.getProperty(SNMP_COMMUNITY_READWRITE, "public");
    }
    catch (Exception e) {
      Logger.getLogger(SnmpTools.class).error(
        "Error while loading community string. Is \"xyna.ws.properties\" accessible?", e);
    }
    return _communityReadWrite;
  }


  private static void logThreads(Logger logger) {
    Thread[] tarray = new Thread[ Thread.activeCount() ];
    Thread.enumerate(tarray);

    for( Thread t : tarray ) {
      if( t != null ) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stes = t.getStackTrace();
        for( StackTraceElement ste :stes ) {
          sb.append( ", "+ste );
        }
        if( sb.length() > 0 ) {
          logger.info( t.getName()+": "+sb.substring(2) );
        } else {
          logger.info( t.getName()+": -");
        }
      }
    }
  }


  /**
   * returns result of snmp call
   */
  public static SnmpSetResponseStatus sendSnmpSetCommand(SnmpCommandInput input, VarBind varBind,
                                     Logger logger) throws IOException {
    logger.info("Sending snmp command for ip = "  + input.getIp() + ", port = " + input.getPort()
                           + ", snmpTimeout = " + input.getSnmpTimeout());
    SADBuilder builder = SnmpAccessData.newSNMPv2c().host(input.getIp()).port(input.getPort())
        .timeoutModel("simple", 0, input.getSnmpTimeout()).community(input.getCommunityReadWrite());
    SnmpAccessData snmpAccessData = builder.build();
    logger.info("Using socket timeout for snmp : " + input.getSocketTimeout());
    SnmpContext snmp = new SnmpContextImplApache(snmpAccessData, SOCKET_TIMEOUT);
    try {
      VarBindList request = new VarBindList();
      request.add(varBind);
      snmp.set(request, "SnmpTools");
      return new SnmpSetResponseStatus(true);
    }
    catch (SnmpResponseException e) {
      SnmpSetResponseStatus ret = new SnmpSetResponseStatus(e);
      logger.error("snmp set command returns error, error status = " + ret.getErrorStatus()
                   + ", exception: \n", e);
      return ret;
    }
    finally {
      if(!input.getLogThreads()) {
        snmp.close();
      }
      else {
        final SnmpContext s = snmp;
        new Thread() {
          public void run() {
            s.close();
          }
        }.start();
        logThreads(logger);
      }
    }
  }


  /**
   * sends one oid from list after the other untill one does not return an error
   *
   */
  public static void sendOids(SnmpCommandInput input, String oidList, Logger _logger)
                      throws JunoSnmpToolsException, RemoteException {
    // parse property from file to get oids and values
    String[] oidGroups = oidList.split(";");
    for (String part : oidGroups) {
      if (part.trim().length() < 1) {
        continue;
      }
      try {
        OidTypeValueGroup group = new OidTypeValueGroup(part);
        VarBind varBind = group.toVarBind();
        _logger.info("received oid for command: " + group.toString());

        SnmpSetResponseStatus status = SnmpTools.sendSnmpSetCommand(input, varBind, _logger);
        if (status.isStatusOK()) {
          _logger.info("Snmp reboot command returns success.");
          return;
        }
      }
      catch (Exception e) {
        _logger.error("", e);
      }
    }
    throw new JunoSnmpToolsException("No oid sent to reboot cm was successful.");
  }

}
