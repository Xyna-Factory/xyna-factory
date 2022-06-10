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


package com.gip.juno.ws.handler;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.InstanceType;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.LocationData;
import com.gip.juno.ws.tools.ManagementData;
import com.gip.juno.ws.tools.NamedConnectionInfo;
import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.manager.SnmpContext;
import com.gip.xyna.utils.snmp.manager.SnmpContextImplApache;
import com.gip.xyna.utils.snmp.varbind.NullVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBindList;

/**
 * class that sends snmp commands that return status information;
 * to be used by checkStatus webservice
 */
public class StatusTools {

  public static class StatusElement {
    public String ip = "Unknown";
    public String status = "Unknown";
    public String service = "Unknown";
    public String exception = "None.";
  }

  public static class InstanceInfo {
    public String ip = "";
    public InstanceType type = InstanceType.Unknown;
    public String location = "";
  }

  private enum AnswerType { string, number }

  private static Logger _logger = Logger.getLogger("CheckStatus");

  public static class Constant {
    public static class IPv6 {
      public static class OID {
        public static final String DHCP_ADAPTER = "1.3.6.1.4.1.28747.1.12.1.2.1.3.23";
        public static final String TFTP = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.50.54";
        public static final String SNMP_ADAPTER = "1.3.6.1.4.1.28747.1.12.1.2.1.3.20";
        public static final String CONFIG_FILE_GENERATOR = "1.3.6.1.4.1.28747.1.12.1.2.1.3.26";
        public static final String TOD = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.50.55";

        //TODO
        public static final String DHCP = "";
        public static final String SOCKET_PORT = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.50.57";
        public static final String DNS = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.50.56";
        public static final String XYNA_FACTORY = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.50.50";
      }
      public static class Port {

        public static final String DHCP_ADAPTER = "1161";
        public static final String TFTP = "1161";
        public static final String SNMP_ADAPTER = "20261";
        public static final String CONFIG_FILE_GENERATOR = "20267";
        public static final String TOD = "1161";
        public static final String DHCP = "1161";
        public static final String SOCKET_PORT = "1161";
        public static final String DNS = "1161";
        public static final String XYNA_FACTORY = "1161";
      }
    }
  }


  private static final String _dnsPort = "1161";
  private static final String _dhcpPort = "1161";
  private static final String _dhcpAdapterPort = "1161";
  private static final String _tftpPort = "1161";
  private static final String _snmpAdapterPort = "20161";
  private static final String _configFileGeneratorPort = "20167";
  private static final String _toDPort = "1161";
  private static final String _socketPort = "1161";

  /*
  * woher kommen die OIDs:
  * ...1.13.20 steht in snmpd.conf
  * 3.1.1.3 ist immer der output des angegebenen skripts
  * 48.48.51 sind die ascii codes der eindeutigen im snmpd.conf angegeben id (z.b. 113)
  * (vgl man ascii)
  */
  private static final String _dnsOID = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.48.51";
  private static final String _dhcpOID = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.48.50";
  private static final String _tftpOID = ".1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.48.49";

  private static final String _dhcpAdapterOID = "1.3.6.1.4.1.28747.1.12.1.2.1.3.14";
  private static final String _snmpAdapterOID = "1.3.6.1.4.1.28747.1.12.1.2.1.3.11";
  private static final String _configFileGeneratorOID = "1.3.6.1.4.1.28747.1.12.1.2.1.3.17";
  private static final String _toDOID = "1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.48.53";
  private static final String _socketOID = "1.3.6.1.4.1.28747.1.13.20.3.1.1.3.48.49.55";

  private static final String _ok = "OK";
  private static final String _failed = "Failed";
  private static final String _exception = "Exception";

  private static final String _snmpTimeoutPropertyName = "snmp.timeout";
  private static int _snmpTimeout = 1000;
  private static final int  _snmpTimeoutDefault = 1000;

  public static List<StatusElement> checkStatusForIp(String ip, InstanceType type) throws RemoteException {
    try {
      _logger.info("entering checkStatusForIp");
      setSnmpTimeout();
      List<StatusElement> ret = null;
      if (type == InstanceType.Dpp) {
        ret = sendCommandsForDPPInstance(ip);
      } else {
        ret = new ArrayList<StatusElement>();
        ret.add(sendCommandForDnsInstance(ip));
      }
      return ret;
    } catch (RemoteException e) {
      throw e;
    } catch (Exception e) {
      _logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in CheckStatus", e);
    }
  }


  public static List<StatusElement> checkStatusForIpv6(String ip, InstanceType type) throws RemoteException {
    try {
      _logger.info("entering checkStatusForIpv6");
      setSnmpTimeout();
      List<StatusElement> ret = null;
      if (type == InstanceType.Dpp) {
        ret = sendIpv6CommandsForDPPInstance(ip);
      } else {
        ret = new ArrayList<StatusElement>();
        ret.add(sendIpv6CommandForDnsInstance(ip));
      }
      return ret;
    } catch (RemoteException e) {
      throw e;
    } catch (Exception e) {
      _logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in CheckStatus", e);
    }
  }


  private static void setSnmpTimeout() {
    try {
      Properties prop = PropertiesHandler.getWsProperties();
       String timeoutStr = prop.getProperty(_snmpTimeoutPropertyName);
       _snmpTimeout = Integer.parseInt(timeoutStr);
    } catch (Exception e) {
      _logger.error(e);
      _snmpTimeout = _snmpTimeoutDefault;
    }
    _logger.info("Going to use snmp timeout = " + _snmpTimeout);
  }

  public static List<InstanceInfo> getInstanceInfoList() throws RemoteException {
    List<NamedConnectionInfo> connections = getDPPConnections();
    List<InstanceInfo> ret = new ArrayList<InstanceInfo>();
    for (NamedConnectionInfo conn : connections) {
      InstanceInfo info = new InstanceInfo();
      info.ip = conn.ssh_ip;
      info.location = conn.location;
      info.type = InstanceType.Dpp;
      ret.add(info);
    }
    /*for (FailoverFlag flag: FailoverFlag.values()) {
      InstanceInfo info = new InstanceInfo();
      info.ip = getDnsIp(flag);
      info.location = "Dns";
      info.type = InstanceType.Dns;
      ret.add(info);
    }*/
    return ret;
  }


  private static List<NamedConnectionInfo> getDPPConnections() throws RemoteException {
    LocationData data = LocationData.getInstance(LocationSchema.service, _logger);
    return data.getAllNamedConnections(_logger);
  }

  private static List<StatusElement> sendCommandsForDPPInstance(String ip)
        throws RemoteException {
    List<StatusElement> ret = new ArrayList<StatusElement>();
    ret.add(sendCommand("DHCPAdapter", ip, _dhcpAdapterPort, _dhcpAdapterOID, AnswerType.string));
//    ret.add(sendCommand("TFTP", ip, _tftpPort, _tftpOID, AnswerType.number));
//    ret.add(sendCommand("SNMPAdapter", ip, _snmpAdapterPort, _snmpAdapterOID, AnswerType.string));
//    ret.add(sendCommand("ConfigFileGenerator", ip, _configFileGeneratorPort, _configFileGeneratorOID,
//        AnswerType.string));
//    ret.add(sendCommand("ToD", ip, _toDPort, _toDOID, AnswerType.number));
    ret.add(sendCommand("DHCP", ip, _dhcpPort, _dhcpOID, AnswerType.number));
//    ret.add(sendCommand("Sockets / Ports", ip, _socketPort, _socketOID, AnswerType.number));
    return ret;
  }


  private static List<StatusElement> sendIpv6CommandsForDPPInstance(String ip)
        throws RemoteException {
    List<StatusElement> ret = new ArrayList<StatusElement>();
    ret.add(sendCommand("DHCPAdapter V6", ip, Constant.IPv6.Port.DHCP_ADAPTER, Constant.IPv6.OID.DHCP_ADAPTER,
                        AnswerType.string));
    ret.add(sendCommand("TFTP V6", ip, Constant.IPv6.Port.TFTP, Constant.IPv6.OID.TFTP, AnswerType.number));
    ret.add(sendCommand("SNMPAdapter V6", ip, Constant.IPv6.Port.SNMP_ADAPTER, Constant.IPv6.OID.SNMP_ADAPTER,
                        AnswerType.string));
    ret.add(sendCommand("ConfigFileGenerator V6", ip, Constant.IPv6.Port.CONFIG_FILE_GENERATOR,
                        Constant.IPv6.OID.CONFIG_FILE_GENERATOR, AnswerType.string));
    ret.add(sendCommand("ToD V6", ip, Constant.IPv6.Port.TOD, Constant.IPv6.OID.TOD, AnswerType.number));
    //FIXME
    //ret.add(sendCommand("DHCP V6", ip, Constant.IPv6.Port.DHCP, Constant.IPv6.OID.DHCP, AnswerType.number));
    ret.add(sendCommand("Sockets / Ports V6", ip, Constant.IPv6.Port.SOCKET_PORT,
                        Constant.IPv6.OID.SOCKET_PORT, AnswerType.number));
    ret.add(sendCommand("Xyna Factory", ip, Constant.IPv6.Port.XYNA_FACTORY,
                        Constant.IPv6.OID.XYNA_FACTORY, AnswerType.number));
    return ret;
  }


  private static StatusElement sendCommandForDnsInstance(String ip)
        throws RemoteException {
    return sendCommand("DNS", ip, _dnsPort, _dnsOID, AnswerType.number);
  }

  private static StatusElement sendIpv6CommandForDnsInstance(String ip)
        throws RemoteException {
    return sendCommand("DNS V6", ip, Constant.IPv6.Port.DNS, Constant.IPv6.OID.DNS, AnswerType.number);
  }

  private static StatusElement sendCommand(String service, String ip, String port, String oid,
        AnswerType answerType) throws RemoteException {
    long start = System.currentTimeMillis();
    StatusElement ret = new StatusElement();
    ret.ip = ip;
    ret.service = service;
    try {
      String answer = sendSnmpCommand(oid, ip, port);
      //long s2 = System.currentTimeMillis();
      if (answerType == AnswerType.number) {
        if (checkNumberAnswer(answer)) {
          ret.status = _ok;
        } else {
          ret.status = _failed;
        }
      } else if (answerType == AnswerType.string) {
        if (checkStringAnswer(answer)) {
          ret.status = _ok;
        } else {
          ret.status = _failed;
        }
      }
      //long e2 = System.currentTimeMillis();
      //_logger.info("Checking answer: " + (e2-s2) + " ms"  );
    } catch (Exception e) {
      _logger.error(e);
      ret.exception = MessageBuilder.stackTraceToString(e);
      ret.status = _exception;
    }
    long end = System.currentTimeMillis();
    _logger.info("Complete command: " + (end-start) + " ms"  );
    return ret;
  }

  private static boolean checkStringAnswer(String answer) {
    if (answer.indexOf("running") >= 0) {
      return true;
    }
    return false;
  }

  private static boolean checkNumberAnswer(String answer) {
    if (answer.trim().indexOf("0") == 0) {
      return true;
    }
    return false;
  }


  private static String getDnsIp(FailoverFlag flag) throws RemoteException {
//    String jdbc = ManagementData.get(DBSchema.dns, flag, _logger).url;
//    String[] splitted = jdbc.split("/");
//    if (splitted.length != 4) {
//      _logger.error("StatusTools: Error while splitting jdbc String: " + jdbc);
//      throw new DPPWebserviceUnexpectedException("Error while splitting jdbc String: " + jdbc);
//    }
//    String ret = splitted[2];
//    _logger.info("Read Ip for Dns: " + ret);
//    return ret;
    return "";
  }


  private static String sendSnmpCommand(String oid, String ip, String port) throws IOException {
    _logger.info("Sending snmp command with oid = " + oid + ", ip = " + ip + ", port = " + port + ", timeout = "
        + _snmpTimeout);
    //long start = System.currentTimeMillis();
    /*
    SnmpAccessData snmpAccessData = SnmpAccessData.newSNMPv2c().host(ip).port(port).timeoutModel("simple",0,500)
        .community("public").build();
    */
    SnmpAccessData snmpAccessData = SnmpAccessData.newSNMPv2c().host(ip).port(port).timeoutModel(
        "simple",0,_snmpTimeout).community("public").build();
    SnmpContext snmp = new SnmpContextImplApache( snmpAccessData, 100 );
    //long end = System.currentTimeMillis();
    //_logger.info("build SnmpContext: " + (end-start) + " ms"  );
    try {
      VarBindList request = new VarBindList();
      request.add( new NullVarBind(oid));

      //start = System.currentTimeMillis();

      VarBindList response = snmp.get(request, "CheckStatus");

      //end = System.currentTimeMillis();
      //_logger.info("Snmp.Request: " + (end-start) + " ms" );

      //start = System.currentTimeMillis();

      boolean received = false;
      if ((response != null) && (response.get(0) != null) && (response.get(0).getValue() != null)) {
        received = true;
      }
      String resp = "No response.";
      if (received) {
        resp = response.get(0).getValue().toString();
        _logger.info("SNMP response = " + resp);
      } else {
        //_logger.info("SNMP answer is null.");
        //snmp.close();
        throw new DPPWebserviceUnexpectedException("SNMP answer is null.");
      }
      //end = System.currentTimeMillis();
      //_logger.info("Getting response out of VarBindList: " + (end-start) + " ms" );
      return resp;
    } finally {
      //start = System.currentTimeMillis();
      if( false ) {
        snmp.close();
      } else {
        final SnmpContext s = snmp;
        new Thread() {
          public void run() {
            s.close();
          }
        }.start();
        logThreads();
      }
      //end = System.currentTimeMillis();
      //_logger.info("Closing snmp: " + (end-start) + " ms" );
    }
  }

  private static void logThreads() {
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
          _logger.info( t.getName()+": "+sb.substring(2) );
        } else {
          _logger.info( t.getName()+": -");
        }
      }
    }
  }
}
