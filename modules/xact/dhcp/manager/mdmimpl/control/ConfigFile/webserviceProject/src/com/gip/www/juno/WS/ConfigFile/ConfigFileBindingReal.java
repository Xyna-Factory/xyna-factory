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

package com.gip.www.juno.WS.ConfigFile;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.axis.encoding.Base64;
import org.apache.log4j.Logger;

import com.gip.juno.cfgdecode.ConfigFileDecoder;
import com.gip.juno.cfgdecode.docsis.DocsisDecoding;
import com.gip.juno.cfggen.ConfigFile;
import com.gip.juno.cfggen.ConfigFileGenerator;
import com.gip.juno.cfggen.model.CableModemRequest;
import com.gip.juno.cfggen.model.DeviceDetails;
import com.gip.juno.cfggen.model.InitializedCableModem;
import com.gip.juno.cfggen.model.IpV4AddressList;
import com.gip.juno.cfggen.model.IsdnMta;
import com.gip.juno.cfggen.model.MtaRequest;
import com.gip.juno.cfggen.model.NcsMta;
import com.gip.juno.cfggen.model.SipMta;
import com.gip.juno.cfggen.model.SipMtaPort;
import com.gip.juno.cfggen.model.UninitializedMta;
import com.gip.juno.cfggen.model.UnregisteredCableModem;
import com.gip.juno.cfggen.model.UnregisteredMta;
import com.gip.juno.cfggen.textconfig.TextConfigGenerator;
import com.gip.juno.cfggen.textconfig.template.TextConfigTemplate;
import com.gip.juno.cfggen.tlvencoding.docsis.DocsisEncoding;
import com.gip.juno.cfggenV6.textconfig.template.TextConfigType;
import com.gip.juno.cfggenserver.db.xmldecode.XmlDecoderFactory;
import com.gip.juno.cfggenserver.db.xmldecode.decoders.XmlDecoder;
import com.gip.juno.cfggenserver.db.xmldecode.decoders.XmlDecoderType;
import com.gip.juno.ws.db.tables.audit.Dhcpv4PacketsHandler;
import com.gip.juno.ws.db.tables.dhcp.DppFixedAttributeHandler;
import com.gip.juno.ws.db.tables.dhcp.SharedNetworkHandler;
import com.gip.juno.ws.db.tables.dhcp.SubnetHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.handler.AuthenticationTools;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;
import com.gip.juno.ws.handler.LeasesTools;
import com.gip.juno.ws.handler.TextConfigTemplateTools;
import com.gip.juno.ws.handler.tables.DocsisEncodingTools;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainer;
import com.gip.juno.ws.tools.WSTools;
import com.gip.www.juno.Gui.WS.Messages.CableModemRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.ConfigFileGeneratorParameters_ctype;
import com.gip.www.juno.Gui.WS.Messages.DeviceDetails_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.InitializedCableModem_ctype;
import com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype;
import com.gip.www.juno.Gui.WS.Messages.IsdnMta_ctype;
import com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.NcsMta_ctype;
import com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.SipMtaPortList_ctype;
import com.gip.www.juno.Gui.WS.Messages.SipMtaPort_ctype;
import com.gip.www.juno.Gui.WS.Messages.SipMta_ctype;
import com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype;
import com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype;
import com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.TlvToAsciiResponse_ctype;
import com.gip.www.juno.Gui.WS.Messages.UninitializedMta_ctype;
import com.gip.www.juno.Gui.WS.Messages.UnregisteredCableModem_ctype;
import com.gip.www.juno.Gui.WS.Messages.UnregisteredMta_ctype;
import com.gip.www.juno.WS.ConfigFile.Tools.AuditDhcpPacketDatatype;
import com.gip.www.juno.WS.ConfigFile.Tools.AuditLeasesDatatype;
import com.gip.www.juno.WS.ConfigFile.Tools.AuditV6Datatype;
import com.gip.www.juno.WS.ConfigFile.Tools.DHCPSharedNetworkDatatype;
import com.gip.www.juno.WS.ConfigFile.Tools.DHCPSubnetDatatype;
import com.gip.www.juno.WS.ConfigFile.Tools.DppFixedAttributeDatatype;
import com.gip.www.juno.WS.ConfigFile.Tools.NodesToString;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xact.trigger.tlvdecoding.dhcp.DHCPConfigurationDecoder;
import com.gip.xyna.xact.trigger.tlvdecoding.dhcp.DecoderException;
import com.gip.xyna.xact.trigger.tlvencoding.dhcp.DHCPConfigurationEncoder;
import com.gip.xyna.xact.trigger.tlvencoding.dhcp.DHCPEncoding;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTree;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTreeReader;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.StringToMapUtil;

import dhcpdConf.IPAddress;
import dhcpdConf.Subnet;



public class ConfigFileBindingReal {

  private static ResultSetReader<DHCPv6Encoding> optionsV6reader = new ResultSetReader<DHCPv6Encoding>() {

    public DHCPv6Encoding read(ResultSet rs) throws SQLException {

      String tmp = rs.getString("valuedatatypeargumentsstring");
      tmp = tmp.replace("{", "");
      tmp = tmp.replace("}", "");
      tmp = tmp.replace(" ", "");

      // logger.info("Reading Encoding");
      // logger.info(rs.getInt("id"));
      // logger.info(rs.getInt("parentid"));
      // logger.info(rs.getString("typename"));
      // logger.info(rs.getLong("typeencoding"));
      // logger.info(rs.getInt("enterprisenr"));
      // logger.info(rs.getString("valuedatatypename"));

      int id = rs.getInt("id");
      Integer parentid = rs.getInt("parentid");
      if (parentid == 0)
        parentid = null;
      String typename = rs.getString("typename");
      Long typeencoding = rs.getLong("typeencoding");
      Integer enterprisenr = rs.getInt("enterprisenr");
      if (enterprisenr == 0)
        enterprisenr = null;
      String valuedatatypename = rs.getString("valuedatatypename");

      DHCPv6Encoding e = new DHCPv6Encoding(id, parentid, typename, typeencoding, enterprisenr, valuedatatypename,
                                            StringToMapUtil.toMap(tmp));
      return e;
    }

  };
  
  private static ResultSetReader<DHCPEncoding> optionsv4reader = new ResultSetReader<DHCPEncoding>() {

    public DHCPEncoding read(ResultSet rs) throws SQLException {

      String tmp = rs.getString("valuedatatypeargumentsstring");
      tmp = tmp.replace("{", "");
      tmp = tmp.replace("}", "");
      tmp = tmp.replace(" ", "");

      // logger.info("Reading Encoding");
      // logger.info(rs.getInt("id"));
      // logger.info(rs.getInt("parentid"));
      // logger.info(rs.getString("typename"));
      // logger.info(rs.getLong("typeencoding"));
      // logger.info(rs.getInt("enterprisenr"));
      // logger.info(rs.getString("valuedatatypename"));

      int id = rs.getInt("id");
      Integer parentid = rs.getInt("parentid");
      if (parentid == 0)
        parentid = null;
      String typename = rs.getString("typename");
      Integer typeencoding = rs.getInt("typeencoding");
      Integer enterprisenr = rs.getInt("enterprisenr");
      if (enterprisenr == 0)
        enterprisenr = null;
      String valuedatatypename = rs.getString("valuedatatypename");

      DHCPEncoding e = new DHCPEncoding(id, parentid, typename, typeencoding, enterprisenr, valuedatatypename,
          com.gip.xyna.xact.trigger.tlvencoding.util.StringToMapUtil.toMap(tmp));
      return e;
    }

  };

  private static ResultSetReader<AuditV6Datatype> auditV6reader = new ResultSetReader<AuditV6Datatype>() {

    public AuditV6Datatype read(ResultSet rs) throws SQLException {

      AuditV6Datatype a = new AuditV6Datatype(rs.getString("host"), rs.getString("ip"), rs.getString("inTime"),
                                              rs.getString("solicit"), rs.getString("advertise"));
      return a;
    }

  };

  private static ResultSetReader<AuditDhcpPacketDatatype> auditreader = new ResultSetReader<AuditDhcpPacketDatatype>() {

    public AuditDhcpPacketDatatype read(ResultSet rs) throws SQLException {

      AuditDhcpPacketDatatype a = new AuditDhcpPacketDatatype(rs.getString("host"), rs.getString("ip"), rs.getString("inTime"),
                                              rs.getString("discover"), rs.getString("offer"));
      return a;
    }

  };

  private static ResultSetReader<AuditLeasesDatatype> auditleasesreader = new ResultSetReader<AuditLeasesDatatype>() {

    public AuditLeasesDatatype read(ResultSet rs) throws SQLException {

      AuditLeasesDatatype a = new AuditLeasesDatatype(rs.getString("host"), rs.getString("ip"), rs.getString("startTime"),
                                              rs.getString("endTime"), rs.getLong("duration"),rs.getString("type"), rs.getString("remoteId"), rs.getString("dppInstance"));
      return a;
    }

  };

  private static ResultSetReader<DHCPSubnetDatatype> dhcpsubnetreader = new ResultSetReader<DHCPSubnetDatatype>() {

    public DHCPSubnetDatatype read(ResultSet rs) throws SQLException {

      DHCPSubnetDatatype a = new DHCPSubnetDatatype(rs.getLong("subnetID"), rs.getLong("sharedNetworkID"), rs.getString("subnet"),
                                              rs.getString("mask"), rs.getString("fixedAttributes"),rs.getString("attributes"), rs.getString("migrationState"));
      return a;
    }

  };

  private static ResultSetReader<DHCPSharedNetworkDatatype> dhcpsharednetworkreader = new ResultSetReader<DHCPSharedNetworkDatatype>() {

    public DHCPSharedNetworkDatatype read(ResultSet rs) throws SQLException {

      DHCPSharedNetworkDatatype a = new DHCPSharedNetworkDatatype(rs.getLong("sharedNetworkID"), rs.getLong("standortID"), rs.getString("sharedNetwork"),
                                              rs.getLong("cpednsID"), rs.getString("linkAddresses"),rs.getString("migrationState"));
      return a;
    }

  };

  private static ResultSetReader<DppFixedAttributeDatatype> dppfixedattributereader = new ResultSetReader<DppFixedAttributeDatatype>() {

    public DppFixedAttributeDatatype read(ResultSet rs) throws SQLException {

      DppFixedAttributeDatatype a = new DppFixedAttributeDatatype(rs.getLong("dppFixedAttributeID"), rs.getString("name"), rs.getString("eth0"),
                                              rs.getString("eth1"), rs.getString("eth2"),rs.getString("eth3"),rs.getString("domainName"),rs.getString("failover"),rs.getString("eth1peer"));
      return a;
    }

  };

  
  
  private static Logger logger = Logger.getLogger("ConfigFile");

  private static DHCPv6ConfigurationDecoder dec = null;


  private static void initializeDHCPv6Decoder() throws DPPWebserviceException {
    try {
      if (dec == null) // lazy Initialisierung
      {
        // Zugriff auf Datenbank optionsv6 um Decoder zu initialisieren
        List<DHCPv6Encoding> optionsv6 = null;
        SQLUtilsContainer sqlUtilsContainer = null;
        SQLUtilsCache.getForManagement("xynadhcpv6", logger);
        try {
          sqlUtilsContainer = SQLUtilsCache.getForManagement("xynadhcpv6", logger);
          SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

          String query = "SELECT * from optionsv6";

          optionsv6 = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), optionsV6reader);

          sqlUtils.commit();
        }
        catch (Exception e) {
          //logger.info("Error reading optionsv6", e);
          throw new DPPWebserviceException("Error: ", e);
        }
        finally {
          try {
            SQLUtilsCache.release(sqlUtilsContainer, logger);
          }
          catch (Exception e) {
            logger.info("", e);
          }
        }

        dec = new DHCPv6ConfigurationDecoder(optionsv6);
        logger.info("DHCPv6 Decoder initialized!");
      }
    }
    catch (Exception e) {
      logger.error("Problems initializing DHCPv6Decoder!", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00213"), e);
      
    }

  }
  
  private static DHCPConfigurationDecoder decv4 = null;
//  private static String XYNADHCP_DB = "xynadhcp";
//  private static String OPTIONSV4_TABLE = "optionsv4";
  
  private static void initializeDHCPv4Decoder() throws DPPWebserviceException {
    try {
      if (decv4 == null) // lazy Initialisierung
      {
        // Zugriff auf Datenbank optionsv4 um Decoder zu initialisieren
        List<DHCPEncoding> options = null;
        SQLUtilsContainer sqlUtilsContainer = null;
        SQLUtilsCache.getForManagement("xynadhcp", logger);
        try {
          sqlUtilsContainer = SQLUtilsCache.getForManagement("xynadhcp", logger);
          SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

          String query = "SELECT * from optionsv4";

          options = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), optionsv4reader);
          
          sqlUtils.commit();
        }
        catch (Exception e) {
          //logger.info("Error reading optionsv6", e);
          throw new DPPWebserviceException("Error: ", e);
        }
        finally {
          try {
            SQLUtilsCache.release(sqlUtilsContainer, logger);
          }
          catch (Exception e) {
            logger.info("Error releasing SQLUtils", e);
          }
        }

        decv4 = new DHCPConfigurationDecoder(options);
        logger.info("DHCPv4 Decoder initialized!");
      }
    }
    catch (Exception e) {
      logger.error("Problems initializing DHCPv6Decoder!", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00213"), e);
      
    }

  }
  
  private static DHCPConfigurationEncoder encv4 = null;
  private static void initializeDHCPv4Encoder() throws DPPWebserviceException {
    try {
      if (encv4 == null) // lazy Initialisierung
      {
        // Zugriff auf Datenbank optionsv4 um Decoder zu initialisieren
        List<DHCPEncoding> options = null;
        SQLUtilsContainer sqlUtilsContainer = null;
        SQLUtilsCache.getForManagement("xynadhcp", logger);
        try {
          sqlUtilsContainer = SQLUtilsCache.getForManagement("xynadhcp", logger);
          SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

          String query = "SELECT * from optionsv4";

          options = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), optionsv4reader);

          sqlUtils.commit();
        }
        catch (Exception e) {
          //logger.info("Error reading optionsv6", e);
          throw new DPPWebserviceException("Error: ", e);
        }
        finally {
          try {
            SQLUtilsCache.release(sqlUtilsContainer, logger);
          }
          catch (Exception e) {
            logger.info("", e);
          }
        }

        encv4 = new DHCPConfigurationEncoder(options);
        logger.info("DHCPv6 Encoder initialized!");
      }
    }
    catch (Exception e) {
      logger.error("Problems initializing DHCPv4Encoder!", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00213"), e);
      
    }

  }


  private static List<AuditV6Datatype> getAuditv6MemoryEntry(String host) throws DPPWebserviceException {
    try {
      List<AuditV6Datatype> auditv6list = null;
      SQLUtilsContainer sqlUtilsContainer = null;
      SQLUtilsCache.getForManagement("auditv6memory", logger);
      try {
        sqlUtilsContainer = SQLUtilsCache.getForManagement("auditv6memory", logger);
        SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

        String query = "SELECT * from dhcpv6packets where host = ?";

        com.gip.xyna.utils.db.Parameter parameter = new com.gip.xyna.utils.db.Parameter(host);

        // zum Testen:
        parameter = new com.gip.xyna.utils.db.Parameter(host);
        // ___

        auditv6list = sqlUtils.query(query, parameter, auditV6reader);

        sqlUtils.commit();
      }
      catch (Exception e) {
        //logger.info("Error reading optionsv6", e);
        throw new DPPWebserviceException("Error reading optionsv6: ", e);
      }
      finally {
        try {
          SQLUtilsCache.release(sqlUtilsContainer, logger);
        }
        catch (Exception e) {
          throw new DPPWebserviceException("Error: ", e);
        }
      }
      return auditv6list;
    }
    catch (Exception e) {
      logger.error("Error getting Auditv6Memory", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00202"), e);
    }
  }
  
  private static DBTableInfo dhcpv4PacketsDBInfo = new Dhcpv4PacketsHandler().getDBTableInfo(); 
 
  private static List<AuditDhcpPacketDatatype> getAuditDhcpPacketEntry(String host) throws DPPWebserviceException {
    try {
      List<AuditDhcpPacketDatatype> auditlist = null;
      SQLUtilsContainer sqlUtilsContainer = null;
      SQLUtilsCache.getForManagement(dhcpv4PacketsDBInfo.getSchema(), logger);
      try {
        sqlUtilsContainer = SQLUtilsCache.getForManagement(dhcpv4PacketsDBInfo.getSchema(), logger);
        SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

        String query = "SELECT * from " + dhcpv4PacketsDBInfo.getTablename() + " where host = ?";

        com.gip.xyna.utils.db.Parameter parameter = new com.gip.xyna.utils.db.Parameter(host);

        // zum Testen:
        parameter = new com.gip.xyna.utils.db.Parameter(host);
        // ___

        auditlist = sqlUtils.query(query, parameter, auditreader);

        sqlUtils.commit();
      }
      catch (Exception e) {
        //logger.info("Error reading optionsv6", e);
        throw new DPPWebserviceException("Error reading dhcp packets table: ", e);
      }
      finally {
        try {
          SQLUtilsCache.release(sqlUtilsContainer, logger);
        }
        catch (Exception e) {
          throw new DPPWebserviceException("Error: ", e);
        }
      }
      return auditlist;
    }
    catch (Exception e) {
      logger.error("Error getting Auditv6Memory", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00202"), e);
    }
  }


  private static DBTableInfo dhcpv4LeasesDBInfo = LeasesTools.getDBTableXynaInfoLeases(); 

  
  private static List<AuditLeasesDatatype> getAuditLeasesEntry(String ip) throws DPPWebserviceException {
    try {
      List<AuditLeasesDatatype> auditlist = null;
      SQLUtilsContainer sqlUtilsContainer = null;
      SQLUtilsCache.getForManagement(dhcpv4LeasesDBInfo.getSchema(), logger);
      try {
        sqlUtilsContainer = SQLUtilsCache.getForManagement(dhcpv4LeasesDBInfo.getSchema(), logger);
        SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

        String query = "SELECT * from " + dhcpv4LeasesDBInfo.getTablename() + " where ip = ?";

        com.gip.xyna.utils.db.Parameter parameter = new com.gip.xyna.utils.db.Parameter(ip);

        // zum Testen:
        parameter = new com.gip.xyna.utils.db.Parameter(ip);
        // ___

        auditlist = sqlUtils.query(query, parameter, auditleasesreader);

        sqlUtils.commit();
      }
      catch (Exception e) {
        //logger.info("Error reading optionsv6", e);
        throw new DPPWebserviceException("Error reading leases table: ", e);
      }
      finally {
        try {
          SQLUtilsCache.release(sqlUtilsContainer, logger);
        }
        catch (Exception e) {
          throw new DPPWebserviceException("Error: ", e);
        }
      }
      return auditlist;
    }
    catch (Exception e) {
      logger.error("Error getting AuditMemory", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00202"), e);
    }
  }

  private static DBTableInfo dhcpv4SubnetDBInfo = new SubnetHandler().getDBTableInfo(); 
  
  private static List<DHCPSubnetDatatype> getSubnetEntries() throws DPPWebserviceException {
    try {
      List<DHCPSubnetDatatype> subnetlist = null;
      SQLUtilsContainer sqlUtilsContainer = null;
      SQLUtilsCache.getForManagement(dhcpv4SubnetDBInfo.getSchema(), logger);
      try {
        sqlUtilsContainer = SQLUtilsCache.getForManagement(dhcpv4SubnetDBInfo.getSchema(), logger);
        SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

        String query = "SELECT * from " + dhcpv4SubnetDBInfo.getTablename();

        com.gip.xyna.utils.db.Parameter parameter = new com.gip.xyna.utils.db.Parameter();

        // zum Testen:
        parameter = new com.gip.xyna.utils.db.Parameter();
        // ___

        subnetlist = sqlUtils.query(query, parameter, dhcpsubnetreader);

        sqlUtils.commit();
      }
      catch (Exception e) {
        //logger.info("Error reading optionsv6", e);
        throw new DPPWebserviceException("Error reading subnet table: ", e);
      }
      finally {
        try {
          SQLUtilsCache.release(sqlUtilsContainer, logger);
        }
        catch (Exception e) {
          throw new DPPWebserviceException("Error: ", e);
        }
      }
      return subnetlist;
    }
    catch (Exception e) {
      logger.error("Error getting dhcp", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00202"), e);
    }
  }

  private static DBTableInfo dhcpv4SharedNetworkDBInfo = new SharedNetworkHandler().getDBTableInfo(); 
  
  private static List<DHCPSharedNetworkDatatype> getSharedNetworkEntries() throws DPPWebserviceException {
    try {
      List<DHCPSharedNetworkDatatype> sharednetworklist = null;
      SQLUtilsContainer sqlUtilsContainer = null;
      SQLUtilsCache.getForManagement(dhcpv4SharedNetworkDBInfo.getSchema(), logger);
      try {
        sqlUtilsContainer = SQLUtilsCache.getForManagement(dhcpv4SharedNetworkDBInfo.getSchema(), logger);
        SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

        String query = "SELECT * from " + dhcpv4SharedNetworkDBInfo.getTablename();

        com.gip.xyna.utils.db.Parameter parameter = new com.gip.xyna.utils.db.Parameter();

        // zum Testen:
        parameter = new com.gip.xyna.utils.db.Parameter();
        // ___

        sharednetworklist = sqlUtils.query(query, parameter, dhcpsharednetworkreader);

        sqlUtils.commit();
      }
      catch (Exception e) {
        //logger.info("Error reading optionsv6", e);
        throw new DPPWebserviceException("Error reading sharednetworks table: ", e);
      }
      finally {
        try {
          SQLUtilsCache.release(sqlUtilsContainer, logger);
        }
        catch (Exception e) {
          throw new DPPWebserviceException("Error: ", e);
        }
      }
      return sharednetworklist;
    }
    catch (Exception e) {
      logger.error("Error getting dhcp", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00202"), e);
    }
  }
  
  private static DBTableInfo dhcpv4DppFixedAttributeDBInfo = new DppFixedAttributeHandler().getDBTableInfo(); 
  
  private static List<DppFixedAttributeDatatype> getDppFixedAttributeEntries() throws DPPWebserviceException {
    try {
      List<DppFixedAttributeDatatype> dppfixedattributelist = null;
      SQLUtilsContainer sqlUtilsContainer = null;
      SQLUtilsCache.getForManagement(dhcpv4DppFixedAttributeDBInfo.getSchema(), logger);
      try {
        sqlUtilsContainer = SQLUtilsCache.getForManagement(dhcpv4DppFixedAttributeDBInfo.getSchema(), logger);
        SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

        String query = "SELECT * from " + dhcpv4DppFixedAttributeDBInfo.getTablename()+" order by dppFixedAttributeID";

        com.gip.xyna.utils.db.Parameter parameter = new com.gip.xyna.utils.db.Parameter();

        // zum Testen:
        parameter = new com.gip.xyna.utils.db.Parameter();
        // ___

        dppfixedattributelist = sqlUtils.query(query, parameter, dppfixedattributereader);

        sqlUtils.commit();
      }
      catch (Exception e) {
        //logger.info("Error reading optionsv6", e);
        throw new DPPWebserviceException("Error reading dppfixedattributes table: ", e);
      }
      finally {
        try {
          SQLUtilsCache.release(sqlUtilsContainer, logger);
        }
        catch (Exception e) {
          throw new DPPWebserviceException("Error: ", e);
        }
      }
      return dppfixedattributelist;
    }
    catch (Exception e) {
      logger.error("Error getting dhcp", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00202"), e);
    }
  }
  
  
  private static List<Node> getSolicitNodes(List<AuditV6Datatype> auditv6list) throws Exception {
    try {
      String solicit = auditv6list.get(0).getSolicit();

      byte[] sol = ByteUtil.toByteArray(solicit);

      String decodedSolicit = dec.decode(sol);

      StringBuilder builder = new StringBuilder();
      builder.append(decodedSolicit);
      TextConfigTree tree = new TextConfigTreeReader(new StringReader(builder.toString())).read();
      return tree.getNodes();
    }
    catch (Exception e) {
      logger.error("Failed to get Solicit Nodes.", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00214").setDescription("Decodierung des Solicit Bytecodes fehlgeschlagen"), e);
    }
    //return null;
  }
  
  private static String[] getVendorClassAndVendorSpecificString(
      List<AuditDhcpPacketDatatype> auditlist) {

    String[] vendorClassAndVendorSpec = new String[2];

    String request = auditlist.get(0).getRequest();
    byte[] req = com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil
        .toByteArray(request);

    List<Byte> vendorclassbytes = new ArrayList<Byte>();
    List<Byte> vendorspecificbytes = new ArrayList<Byte>();
    try {
      String decodedData = decv4.decode2(req, vendorclassbytes,
          vendorspecificbytes);
    } catch (DecoderException e) {
      e.printStackTrace();
      throw new RuntimeException("Error decoding VendorClass and VendorSpecifcInformation",e);
    }

    String vendorclassstring = "";
    // String vendorclasshex = "0x";
    for (byte b : vendorclassbytes) {
      vendorclassstring = vendorclassstring + (char) b;
      // vendorclasshex = vendorclasshex + Integer.toHexString(b);
    }
    vendorClassAndVendorSpec[0] = vendorclassstring;

    byte[] vsb = new byte[vendorspecificbytes.size()];
    for (int i = 0; i < vsb.length; i++) {
      vsb[i] = vendorspecificbytes.get(i);
    }

    String vendorspecificstring = "";
    try {
      vendorspecificstring = ByteUtil.toHexValue(vsb);
    } catch (Exception e) {
      logger.error("No VendorSpecificInformation Option could be extracted!");
    }

    //vendorspecificstring = replaceVendorName51by10(vendorspecificstring);
    
    vendorClassAndVendorSpec[1] = vendorspecificstring;

    return vendorClassAndVendorSpec;
  }
  // bitte beachten, Discover = Request. Eigentlich ist hier ein Request gespeichert, aber wird als Discover in der GUI gefuehrt.
  private static List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> getRequestNodes(List<AuditDhcpPacketDatatype> auditlist) throws Exception {
    try {
      String request = auditlist.get(0).getRequest();

      
      byte[] req = com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil.toByteArray(request);

      String decodedRequest = decv4.decode(req);

      
      StringBuilder builder = new StringBuilder();
      builder.append(decodedRequest);
      com.gip.xyna.xact.trigger.tlvencoding.dhcp.TextConfigTree tree = new com.gip.xyna.xact.trigger.tlvencoding.dhcp.TextConfigTreeReader(new StringReader(builder.toString())).read();
      return tree.getNodes();
    }
    catch (Exception e) {
      logger.error("Failed to get Discover Nodes.", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00223").setDescription("Decodierung des Discover Bytecodes fehlgeschlagen"), e);
    }
    //return null;
  }
  
  private static void printNodes(List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> nodes){
    for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node : nodes){
      logger.info("got node: " +node.getTypeName());
    }
  }


  private static List<Node> getAdvertiseNodes(List<AuditV6Datatype> auditv6list) throws Exception{
    try {
      String advertise = auditv6list.get(0).getAdvertise();
      byte[] adv = ByteUtil.toByteArray(advertise);

      String decodedAdvertise = dec.decode(adv);

      StringBuilder builder = new StringBuilder();
      builder.append(decodedAdvertise);
      TextConfigTree tree = new TextConfigTreeReader(new StringReader(builder.toString())).read();
      return tree.getNodes();
    }
    catch (Exception e) {
      logger.error("Failed to get Advertise Nodes.", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00215").setDescription("Decodierung des Advertise Bytecodes fehlgeschlagen"), e);

    }
    //return null;
  }

  // bitte beachten, Offer = Reply. Eigentlich ist hier ein Reply gespeichert, aber wird als Offer in der GUI gefuehrt.  
  private static List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> getReplyNodes(List<AuditDhcpPacketDatatype> auditlist) throws Exception{
    try {
      String reply = auditlist.get(0).getReply();
      byte[] rep = com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil.toByteArray(reply);

      
      String decodedReply = decv4.decode(rep);

      
      StringBuilder builder = new StringBuilder();
      builder.append(decodedReply);
      com.gip.xyna.xact.trigger.tlvencoding.dhcp.TextConfigTree tree = new com.gip.xyna.xact.trigger.tlvencoding.dhcp.TextConfigTreeReader(new StringReader(builder.toString())).read();
      return tree.getNodes();
    }
    catch (Exception e) {
      logger.error("Failed to get Offer Nodes.", e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00224").setDescription("Decodierung des Offer Bytecodes fehlgeschlagen"), e);

    }
    //return null;
  }

  public TextResponse_ctype generateAsciiFromTemplateForInitializedCableModem(GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype generateAsciiFromTemplateForInitializedCableModemRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype inreq = generateAsciiFromTemplateForInitializedCableModemRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateAsciiFromTemplateForInitializedCableModemRequest.getInputHeader().getUsername(),
                                                   generateAsciiFromTemplateForInitializedCableModemRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      // authenticate(inreq.getInputHeader());
      GenerateAsciiFromTemplateForInitializedCableModemInput_ctype inval = inreq
                      .getGenerateAsciiFromTemplateForInitializedCableModemInput();
      
      if(inval.getTextConfigGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }
      
      InitializedCableModem cm = toInitializedCableModem(inval.getInitializedCableModem());
      CableModemRequest request = toCableModemRequest(inval.getCableModemRequest());
      TextConfigGenerator tcg = createTextConfigGenerator(inval.getTextConfigGeneratorParameters());
      String retascii = tcg.create(request, cm).getContent();
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(retascii);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }
  }


  public TextResponse_ctype showPacketsAsAscii(ShowPacketsAsAsciiRequest_ctype showPacketsAsAsciiRequest)
                  throws Exception {

    try {

      initializeDHCPv6Decoder();

      String host = showPacketsAsAsciiRequest.getShowPacketsAsAsciiInput().getHost();
      // zum Testen:
      // host = "0000007a1200";
      // ___

      // Zugriff auf auditv6memory mit angegebenem host
      List<AuditV6Datatype> auditv6list = getAuditv6MemoryEntry(host);

      if (auditv6list != null) {
        if (auditv6list.size() > 0) {

          String solicit = auditv6list.get(0).getSolicit();

          byte[] sol = ByteUtil.toByteArray(solicit);

          String decodedSolicit = "";
          try
          {
            decodedSolicit = dec.decode(sol);
          }
          catch(Exception e)
          {
            logger.error("Error decoding Solicit: ",e);
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00214").setDescription("Decodierung des Solicit Bytecodes fehlgeschlagen"), e);

          }

          
          String advertise = auditv6list.get(0).getAdvertise();
          byte[] adv = ByteUtil.toByteArray(advertise);

          String decodedAdvertise = "";
          try
          {
            decodedAdvertise = dec.decode(adv);
          }
          catch(Exception e)
          {
            logger.error("Error decoding Advertise: ",e);
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00215").setDescription("Decodierung des Advertise Bytecodes fehlgeschlagen"), e);

          }

          String retascii = decodedSolicit+"###"+decodedAdvertise;
          
          TextResponse_ctype ret = new TextResponse_ctype();
          ret.setText(retascii);
          return ret;
        }
      }
      return null;
    }
    catch (Exception e) {
      logger.error("", e);
      if(e instanceof DPPWebserviceException)
      {
        throw e;
      }
      else
      {
        throw new DPPWebserviceException("Error: ", e);
      }
    }
  }

  
  public TextResponse_ctype showV4PacketsAsAscii(
      ShowV4PacketsAsAsciiRequest_ctype showV4PacketsAsAsciiRequest)
      throws Exception {

    try {

      initializeDHCPv4Decoder();

      String host = showV4PacketsAsAsciiRequest.getShowV4PacketsAsAsciiInput().getHost();
      // zum Testen:
      // host = "0000007a1200";
      // ___

      // Zugriff auf auditv6memory mit angegebenem host
      List<AuditDhcpPacketDatatype> auditlist = getAuditDhcpPacketEntry(host);

      if (auditlist != null) {
        if (auditlist.size() > 0) {

          String ip = auditlist.get(0).getIp();
          List<AuditLeasesDatatype> leaseslist = getAuditLeasesEntry(ip);
          List<DHCPSubnetDatatype> subnetlist = getSubnetEntries();
          List<DHCPSharedNetworkDatatype> sharednetworklist = getSharedNetworkEntries();
          List<DppFixedAttributeDatatype> dppfixedattributelist = getDppFixedAttributeEntries();
          
//          if(sharednetworklist!=null && sharednetworklist.size()>0)
//          {
//            for(DHCPSharedNetworkDatatype d:sharednetworklist)
//            {
//              logger.info("### Sharednetworkentry: "+d.getSharednetworkid()+":"+d.getStandortid()+":"+d.getSharednetwork()+":"+d.getCpednsid()+":"+d.getLinkaddresses()+":"+d.getMigrationstate());
//            }
//          }
          
          String type = "";
          String dppInstance = "";
          
          if(leaseslist!=null && leaseslist.size()>0)
          {
            logger.info("### Getting type and dppInstance of lease with ip "+ip+" ...");
            type = leaseslist.get(0).getType();
            dppInstance = leaseslist.get(0).getDppInstance();
          }
          else
          {
            logger.info("### leaseslist with ip "+ip+ " empty. No Lease found with that IP!");
          }
          logger.info("### type: "+type);
          logger.info("### dppInstance: "+dppInstance);
          
          
          
          String giaddr = ""; // IP 1
          // SharedNetworkID fuer IP ermitteln
          long sharednetworkid = -1;
          logger.info("### Getting sharedNetworkID ...");
          if(subnetlist!=null)
          {
            logger.info("### Checking for Subnet containing IP ("+ip+").");
            Subnet current;
            
            for(DHCPSubnetDatatype currentsubnet:subnetlist)
            {
              current = Subnet.parse(currentsubnet.getSubnet(),currentsubnet.getMask());
              if(current.contains(IPAddress.parse(ip)))
              {
                sharednetworkid = currentsubnet.getSharednetworkid();
              }
            }
          }
          logger.info("### SharedNetworkID: "+sharednetworkid);
          // Linkaddresse aus sharedNetwork lesen
          if(sharednetworklist!=null && sharednetworklist.size()>0 && sharednetworkid!=-1)
          {
            logger.info("### Getting LinkAddress from sharedNetwork with id "+sharednetworkid);
            for(DHCPSharedNetworkDatatype sn:sharednetworklist)
            {
              //logger.info("### current sharednetworkid: "+sn.getSharednetworkid());
              if(sn.getSharednetworkid()==sharednetworkid)
              {
                giaddr = sn.getLinkaddresses().split(",")[0]; //erste IP Adresse vor Komma oder ganz ohne Komma nehmen
              }
            }
          }
          if(giaddr.length()==0)logger.info("### no Linkaddress found in sharedNetwork with id "+sharednetworkid);
          logger.info("### giaddr=linkaddress= "+giaddr);
          
          String siaddr = "";
          String lookfor = "";
          if(dppInstance.length()>2)
          {
            lookfor = dppInstance.substring(0,dppInstance.length()-2);
            List<String> eth2ips = new ArrayList<String>();
            
            logger.info("### Looking for '"+lookfor+"' in dppfixedattributes failover.");
            
            if(dppfixedattributelist!=null&&dppfixedattributelist.size()>0)
            {
              for(DppFixedAttributeDatatype dfa:dppfixedattributelist)
              {
                if(dfa.getFailover()!=null && dfa.getFailover().contains(lookfor))
                {
                  eth2ips.add(dfa.getEth2());
                  logger.info("### found entry, getting eth2 ip");
                }
              }
            }
            if(eth2ips.size()==2)
            {
              logger.info("### setting siaddr if type ("+type+") contains docsis");
              if(dppInstance.contains("-m")&&type.contains("docsis"))siaddr = eth2ips.get(0);
              if(dppInstance.contains("-s")&&type.contains("docsis"))siaddr = eth2ips.get(1);
            }
            logger.info("### siaddr: "+siaddr);
          }
          else
          {
            logger.info("### no dppInstance given, can't look for failover. Could not get siaddr!");
          }
          
          String request = auditlist.get(0).getRequest();

          byte[] req = ByteUtil.toByteArray(request);

          String decodedRequest = "";
          try {
            decodedRequest = decv4.decode(req);
          } catch (Exception e) {
            logger.error("Error decoding Discover: ", e);
            throw new DPPWebserviceException(new MessageBuilder()
                .setDomain("F").setErrorNumber("00223").setDescription(
                    "Decodierung des Discover Bytecodes fehlgeschlagen"), e);

          }

          String reply = auditlist.get(0).getReply();
          byte[] rep = ByteUtil.toByteArray(reply);

          String decodedReply = "";
          try {
            decodedReply = decv4.decode(rep);
          } catch (Exception e) {
            logger.error("Error decoding Offer: ", e);
            throw new DPPWebserviceException(new MessageBuilder()
                .setDomain("F").setErrorNumber("00224").setDescription(
                    "Decodierung des Offer Bytecodes fehlgeschlagen"), e);

          }

          String preRequest = "CHAddr: "+host+"\n"+"GIAddr: "+giaddr+"\n";
          String preReply = "CHAddr: "+host+"\n"+"GIAddr: "+giaddr+"\n"+"SIAddr: "+siaddr+"\n";
          
          String retascii = preRequest+decodedRequest + "###" + preReply+decodedReply;

          TextResponse_ctype ret = new TextResponse_ctype();
          ret.setText(retascii);
          return ret;
        }
      }
      return null;
    } catch (Exception e) {
      logger.error("", e);
      if (e instanceof DPPWebserviceException) {
        throw e;
      } else {
        throw new DPPWebserviceException("Error: ", e);
      }
    }
  }

  public TextResponse_ctype generateAsciiFromString(GenerateAsciiFromStringRequest_ctype generateAsciiFromStringRequest)
                  throws Exception {

    try {

      initializeDHCPv6Decoder();

      String host = generateAsciiFromStringRequest.getGenerateAsciiFromStringInput().getHost();
      // zum Testen:
      // host = "0000007a1200";
      // ___

      // Zugriff auf auditv6memory mit angegebenem host
      List<AuditV6Datatype> auditv6list = getAuditv6MemoryEntry(host);

      if (auditv6list != null) {
        if (auditv6list.size() > 0) {
          List<Node> solicitNodes = getSolicitNodes(auditv6list);
          List<Node> advertiseNodes = getAdvertiseNodes(auditv6list);

          String configstring = NodesToString.generateString(solicitNodes, advertiseNodes);

          if (configstring != null)
            logger.info("ConfigString: " + configstring);

          GenerateAsciiFromStringRequest_ctype inreq = generateAsciiFromStringRequest;
          InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
          //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
          //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
          AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
          AuthenticationTools.authenticateAndAuthorize(generateAsciiFromStringRequest.getInputHeader().getUsername(),
                                                       generateAsciiFromStringRequest.getInputHeader().getPassword(),
                                                       "configfile", wsInvocationId, logger);

          com.gip.juno.cfggenV6.ConfigFileGenerator cfg = createConfigFileGeneratorV6(new ConfigFileGeneratorParameters_ctype(
                                                                                                                              "",
                                                                                                                              ""));

          com.gip.juno.cfggenV6.textconfig.TextConfig textcfg = cfg.generateTextConfigFromString(configstring);

          String retascii = textcfg.getContent();
          TextResponse_ctype ret = new TextResponse_ctype();
          ret.setText(retascii);
          return ret;
        }
      }
    }
    catch (Exception e) {
      logger.error("", e);
      if(e instanceof DPPWebserviceException)
      {
        throw e;
      }
      else
      {
        throw new DPPWebserviceException("Error: ", e);
      }
    }
    return null;
  }
  
  public TextResponse_ctype generateAsciiFromStringV4(
      GenerateAsciiFromStringV4Request_ctype generateAsciiFromStringV4Request)
      throws Exception {

    try {

      initializeDHCPv4Decoder();

      String host = generateAsciiFromStringV4Request
          .getGenerateAsciiFromStringV4Input().getHost();
      // zum Testen:
      // host = "0000007a1200";
      // ___

      // Zugriff auf auditv6memory mit angegebenem host
      List<AuditDhcpPacketDatatype> auditlist = getAuditDhcpPacketEntry(host);

      if (auditlist != null) {
        if (auditlist.size() > 0) {
          List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> requestNodes = getRequestNodes(auditlist);
          List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> replyNodes = getReplyNodes(auditlist);

          String[] vendorClassAndVendorSpec = getVendorClassAndVendorSpecificString(auditlist);
          
          
          //initializeDHCPv4Encoder();
//          String configstring = NodesToString.generateStringInputForConfigFileGenV4(requestNodes,
//              replyNodes,auditlist.get(0),encv4);
          String configstring = NodesToString.generateStringInputForConfigFileGenV4(requestNodes,
              replyNodes,auditlist.get(0),vendorClassAndVendorSpec);
          

          if (configstring != null)
            logger.info("ConfigString: " + configstring);

          GenerateAsciiFromStringV4Request_ctype inreq = generateAsciiFromStringV4Request;
          InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
          // AuthenticationTools.authenticate(inputHeader.getUsername(),
          // inputHeader.getPassword(), logger);
          // AuthenticationTools.checkPermissions(inputHeader.getUsername(),
          // "configfile", "*", logger);
          AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(
              null);
          AuthenticationTools.authenticateAndAuthorize(
              generateAsciiFromStringV4Request.getInputHeader().getUsername(),
              generateAsciiFromStringV4Request.getInputHeader().getPassword(),
              "configfile", wsInvocationId, logger);

          ConfigFileGenerator cfg = createConfigFileGenerator(new ConfigFileGeneratorParameters_ctype(
              "", ""));

          com.gip.juno.cfggen.textconfig.TextConfig textcfg = cfg.generateTextConfigFromString(configstring);

          String retascii = textcfg.getContent();
          TextResponse_ctype ret = new TextResponse_ctype();
          ret.setText(retascii);
          return ret;
        }
      }
    } catch (Exception e) {
      logger.error("", e);
      if (e instanceof DPPWebserviceException) {
        throw e;
      } else {
        throw new DPPWebserviceException("Error: ", e);
      }
    }
    return null;
  }
  



  public TextResponse_ctype generateAsciiFromTemplateForUnregisteredCableModem(GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype generateAsciiFromTemplateForUnregisteredCableModemRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype inreq = generateAsciiFromTemplateForUnregisteredCableModemRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateAsciiFromTemplateForUnregisteredCableModemRequest.getInputHeader().getUsername(),
                                                   generateAsciiFromTemplateForUnregisteredCableModemRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype inval = inreq
                      .getGenerateAsciiFromTemplateForUnregisteredCableModemInput();

      if(inval.getTextConfigGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      
      UnregisteredCableModem cm = toUnregisteredCableModem(inval.getUnregisteredCableModem());
      CableModemRequest request = toCableModemRequest(inval.getCableModemRequest());
      TextConfigGenerator tcg = createTextConfigGenerator(inval.getTextConfigGeneratorParameters());
      String retascii = tcg.create(request, cm).getContent();
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(retascii);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }
  }


  public TextResponse_ctype generateAsciiFromTemplateForSipMta(GenerateAsciiFromTemplateForSipMtaRequest_ctype generateAsciiFromTemplateForSipMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateAsciiFromTemplateForSipMtaRequest_ctype inreq = generateAsciiFromTemplateForSipMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateAsciiFromTemplateForSipMtaRequest.getInputHeader().getUsername(),
                                                   generateAsciiFromTemplateForSipMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateAsciiFromTemplateForSipMtaInput_ctype inval = inreq.getGenerateAsciiFromTemplateForSipMtaInput();
      
      if(inval.getTextConfigGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      
      SipMta mta = toSipMta(inval.getSipMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      TextConfigGenerator tcg = createTextConfigGenerator(inval.getTextConfigGeneratorParameters());
      String retascii = tcg.create(request, mta).getContent();
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(retascii);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateAsciiFromTemplateForNcsMta(GenerateAsciiFromTemplateForNcsMtaRequest_ctype generateAsciiFromTemplateForNcsMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateAsciiFromTemplateForNcsMtaRequest_ctype inreq = generateAsciiFromTemplateForNcsMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateAsciiFromTemplateForNcsMtaRequest.getInputHeader().getUsername(),
                                                   generateAsciiFromTemplateForNcsMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateAsciiFromTemplateForNcsMtaInput_ctype inval = inreq.getGenerateAsciiFromTemplateForNcsMtaInput();
      
      if(inval.getTextConfigGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      
      NcsMta mta = toNcsMta(inval.getNcsMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      TextConfigGenerator tcg = createTextConfigGenerator(inval.getTextConfigGeneratorParameters());
      String retascii = tcg.create(request, mta).getContent();
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(retascii);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateAsciiFromTemplateForIsdnMta(GenerateAsciiFromTemplateForIsdnMtaRequest_ctype generateAsciiFromTemplateForIsdnMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateAsciiFromTemplateForIsdnMtaRequest_ctype inreq = generateAsciiFromTemplateForIsdnMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateAsciiFromTemplateForIsdnMtaRequest.getInputHeader().getUsername(),
                                                   generateAsciiFromTemplateForIsdnMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateAsciiFromTemplateForIsdnMtaInput_ctype inval = inreq.getGenerateAsciiFromTemplateForIsdnMtaInput();
      
      if(inval.getTextConfigGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      IsdnMta mta = toIsdnMta(inval.getIsdnMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      TextConfigGenerator tcg = createTextConfigGenerator(inval.getTextConfigGeneratorParameters());
      String retascii = tcg.create(request, mta).getContent();
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(retascii);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateAsciiFromTemplateForUninitializedMta(GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype generateAsciiFromTemplateForUninitializedMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype inreq = generateAsciiFromTemplateForUninitializedMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateAsciiFromTemplateForUninitializedMtaRequest.getInputHeader().getUsername(),
                                                   generateAsciiFromTemplateForUninitializedMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateAsciiFromTemplateForUninitializedMtaInput_ctype inval = inreq
                      .getGenerateAsciiFromTemplateForUninitializedMtaInput();
      
      if(inval.getTextConfigGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      UninitializedMta mta = toUninitializedMta(inval.getUninitializedMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      TextConfigGenerator tcg = createTextConfigGenerator(inval.getTextConfigGeneratorParameters());
      String retascii = tcg.create(request, mta).getContent();
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(retascii);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateAsciiFromTemplateForUnregisteredMta(GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype generateAsciiFromTemplateForUnregisteredMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype inreq = generateAsciiFromTemplateForUnregisteredMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
//      AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//      AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateAsciiFromTemplateForUnregisteredMtaRequest.getInputHeader().getUsername(),
                                                   generateAsciiFromTemplateForUnregisteredMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateAsciiFromTemplateForUnregisteredMtaInput_ctype inval = inreq
                      .getGenerateAsciiFromTemplateForUnregisteredMtaInput();
      
      if(inval.getTextConfigGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      UnregisteredMta mta = toUnregisteredMta(inval.getUnregisteredMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      TextConfigGenerator tcg = createTextConfigGenerator(inval.getTextConfigGeneratorParameters());
      String retascii = tcg.create(request, mta).getContent();
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(retascii);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateTlvFromTemplateForInitializedCableModem(GenerateTlvFromTemplateForInitializedCableModemRequest_ctype generateTlvFromTemplateForInitializedCableModemRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateTlvFromTemplateForInitializedCableModemRequest_ctype inreq = generateTlvFromTemplateForInitializedCableModemRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
//      AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//      AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateTlvFromTemplateForInitializedCableModemRequest.getInputHeader().getUsername(),
                                                   generateTlvFromTemplateForInitializedCableModemRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateTlvFromTemplateForInitializedCableModemInput_ctype inval = inreq
                      .getGenerateTlvFromTemplateForInitializedCableModemInput();

      if(inval.getConfigFileGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      
      InitializedCableModem cm = toInitializedCableModem(inval.getInitializedCableModem());
      CableModemRequest request = toCableModemRequest(inval.getCableModemRequest());
      ConfigFileGenerator cfg = createConfigFileGenerator(inval.getConfigFileGeneratorParameters());
      byte[] bytes = cfg.generate(request, cm).getContent();
      String rettext = encodeBase64(bytes);
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(rettext);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateTlvFromTemplateForUnregisteredCableModem(GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype generateTlvFromTemplateForUnregisteredCableModemRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype inreq = generateTlvFromTemplateForUnregisteredCableModemRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
//      AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//      AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateTlvFromTemplateForUnregisteredCableModemRequest.getInputHeader().getUsername(),
                                                   generateTlvFromTemplateForUnregisteredCableModemRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateTlvFromTemplateForUnregisteredCableModemInput_ctype inval = inreq
                      .getGenerateTlvFromTemplateForUnregisteredCableModemInput();
      
      if(inval.getConfigFileGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      UnregisteredCableModem cm = toUnregisteredCableModem(inval.getUnregisteredCableModem());
      CableModemRequest request = toCableModemRequest(inval.getCableModemRequest());
      ConfigFileGenerator cfg = createConfigFileGenerator(inval.getConfigFileGeneratorParameters());
      byte[] bytes = cfg.generate(request, cm).getContent();
      String rettext = encodeBase64(bytes);
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(rettext);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateTlvFromString(GenerateTlvFromStringRequest_ctype generateTlvFromStringRequest)
                  throws Exception {
    try {

      initializeDHCPv6Decoder();

      String host = generateTlvFromStringRequest.getGenerateTlvFromStringInput().getHost();
      // zum Testen:
      // host = "0000007a1200";
      // ___

      // Zugriff auf auditv6memory mit angegebenem host
      List<AuditV6Datatype> auditv6list = getAuditv6MemoryEntry(host);

      if (auditv6list != null) {
        if (auditv6list.size() > 0) {
          List<Node> solicitNodes = getSolicitNodes(auditv6list);
          List<Node> advertiseNodes = getAdvertiseNodes(auditv6list);

          String configstring = NodesToString.generateString(solicitNodes, advertiseNodes);

          if (configstring != null)
            logger.info("ConfigString: " + configstring);

          GenerateTlvFromStringRequest_ctype inreq = generateTlvFromStringRequest;
          InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
          //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
          //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
          AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
          AuthenticationTools.authenticateAndAuthorize(generateTlvFromStringRequest.getInputHeader().getUsername(),
                                                       generateTlvFromStringRequest.getInputHeader().getPassword(),
                                                       "configfile", wsInvocationId, logger);

          com.gip.juno.cfggenV6.ConfigFileGenerator cfg = createConfigFileGeneratorV6(new ConfigFileGeneratorParameters_ctype(
                                                                                                                              "",
                                                                                                                              ""));
          byte[] bytes = cfg.generateConfigFileFromString(configstring).getContent();
          String rettext = encodeBase64(bytes);
          TextResponse_ctype ret = new TextResponse_ctype();
          ret.setText(rettext);
          return ret;
        }
      }
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      if(e instanceof DPPWebserviceException)
      {
        throw e;
      }
      else
      {
        throw new DPPWebserviceException("Error: ", e);
      }

    }
    return null;
  }
  
  public TextResponse_ctype generateTlvFromStringV4(
      GenerateTlvFromStringV4Request_ctype generateTlvFromStringV4Request)
      throws Exception {
    try {

      initializeDHCPv4Decoder();

      String host = generateTlvFromStringV4Request
          .getGenerateTlvFromStringV4Input().getHost();
      // zum Testen:
      // host = "0000007a1200";
      // ___

      // Zugriff auf auditv6memory mit angegebenem host
      List<AuditDhcpPacketDatatype> auditlist = getAuditDhcpPacketEntry(host);

      if (auditlist != null) {
        if (auditlist.size() > 0) {
          List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> requestNodes = getRequestNodes(auditlist);
          List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> replyNodes = getReplyNodes(auditlist);

          String[] vendorClassAndVendorSpec = getVendorClassAndVendorSpecificString(auditlist);
          
          
          //initializeDHCPv4Encoder();
//          String configstring = NodesToString.generateStringInputForConfigFileGenV4(requestNodes,
//              replyNodes,auditlist.get(0),encv4);
          String configstring = NodesToString.generateStringInputForConfigFileGenV4(requestNodes,
              replyNodes,auditlist.get(0),vendorClassAndVendorSpec);

          if (configstring != null)
            logger.info("ConfigString: " + configstring);
          
          

          GenerateTlvFromStringV4Request_ctype inreq = generateTlvFromStringV4Request;
          InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
          // AuthenticationTools.authenticate(inputHeader.getUsername(),
          // inputHeader.getPassword(), logger);
          // AuthenticationTools.checkPermissions(inputHeader.getUsername(),
          // "configfile", "*", logger);
          AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(
              null);
          AuthenticationTools.authenticateAndAuthorize(
              generateTlvFromStringV4Request.getInputHeader().getUsername(),
              generateTlvFromStringV4Request.getInputHeader().getPassword(),
              "configfile", wsInvocationId, logger);

          com.gip.juno.cfggen.ConfigFileGenerator cfg = createConfigFileGenerator(new ConfigFileGeneratorParameters_ctype(
              "", ""));
          
          ConfigFile configfile = cfg.generateConfigFileFromString(configstring);
          if (configfile == null){
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00220").setDescription("No config file could be generated for host " +auditlist.get(0).getHost() + " - check log file for reasons"));
          }
          byte[] bytes = configfile.getContent();
          String rettext = encodeBase64(bytes);
          TextResponse_ctype ret = new TextResponse_ctype();
          ret.setText(rettext);
          return ret;
        }
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      if (e instanceof DPPWebserviceException) {
        throw e;
      } else {
        throw new DPPWebserviceException("Error: ", e);
      }

    }
    return null;
  }


  //nur eine Testfunktion
  private static String replaceVendorName51by10(String configstring) {

    int lengthIn = configstring.length();
    String newString = configstring.substring(0, 22)+"0A"+configstring.substring(24, lengthIn);

    return newString;
  }


  public TextResponse_ctype generateTlvFromTemplateForSipMta(GenerateTlvFromTemplateForSipMtaRequest_ctype generateTlvFromTemplateForSipMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateTlvFromTemplateForSipMtaRequest_ctype inreq = generateTlvFromTemplateForSipMtaRequest;
      // authenticate(inreq.getInputHeader());
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateTlvFromTemplateForSipMtaRequest.getInputHeader().getUsername(),
                                                   generateTlvFromTemplateForSipMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateTlvFromTemplateForSipMtaInput_ctype inval = inreq.getGenerateTlvFromTemplateForSipMtaInput();
      
      if(inval.getConfigFileGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      SipMta mta = toSipMta(inval.getSipMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      ConfigFileGenerator cfg = createConfigFileGenerator(inval.getConfigFileGeneratorParameters());
      byte[] bytes = cfg.generate(request, mta).getContent();
      String rettext = encodeBase64(bytes);
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(rettext);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateTlvFromTemplateForNcsMta(GenerateTlvFromTemplateForNcsMtaRequest_ctype generateTlvFromTemplateForNcsMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateTlvFromTemplateForNcsMtaRequest_ctype inreq = generateTlvFromTemplateForNcsMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
//      AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//      AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateTlvFromTemplateForNcsMtaRequest.getInputHeader().getUsername(),
                                                   generateTlvFromTemplateForNcsMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateTlvFromTemplateForNcsMtaInput_ctype inval = inreq.getGenerateTlvFromTemplateForNcsMtaInput();
      
      if(inval.getConfigFileGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      NcsMta mta = toNcsMta(inval.getNcsMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      ConfigFileGenerator cfg = createConfigFileGenerator(inval.getConfigFileGeneratorParameters());
      byte[] bytes = cfg.generate(request, mta).getContent();
      String rettext = encodeBase64(bytes);
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(rettext);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateTlvFromTemplateForIsdnMta(GenerateTlvFromTemplateForIsdnMtaRequest_ctype generateTlvFromTemplateForIsdnMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateTlvFromTemplateForIsdnMtaRequest_ctype inreq = generateTlvFromTemplateForIsdnMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
//      AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//      AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateTlvFromTemplateForIsdnMtaRequest.getInputHeader().getUsername(),
                                                   generateTlvFromTemplateForIsdnMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateTlvFromTemplateForIsdnMtaInput_ctype inval = inreq.getGenerateTlvFromTemplateForIsdnMtaInput();
      
      if(inval.getConfigFileGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      IsdnMta mta = toIsdnMta(inval.getIsdnMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      ConfigFileGenerator cfg = createConfigFileGenerator(inval.getConfigFileGeneratorParameters());
      byte[] bytes = cfg.generate(request, mta).getContent();
      String rettext = encodeBase64(bytes);
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(rettext);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateTlvFromTemplateForUninitializedMta(GenerateTlvFromTemplateForUninitializedMtaRequest_ctype generateTlvFromTemplateForUninitializedMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateTlvFromTemplateForUninitializedMtaRequest_ctype inreq = generateTlvFromTemplateForUninitializedMtaRequest;
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
//      AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//      AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateTlvFromTemplateForUninitializedMtaRequest.getInputHeader().getUsername(),
                                                   generateTlvFromTemplateForUninitializedMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateTlvFromTemplateForUninitializedMtaInput_ctype inval = inreq
                      .getGenerateTlvFromTemplateForUninitializedMtaInput();
      
      if(inval.getConfigFileGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      UninitializedMta mta = toUninitializedMta(inval.getUninitializedMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      ConfigFileGenerator cfg = createConfigFileGenerator(inval.getConfigFileGeneratorParameters());
      byte[] bytes = cfg.generate(request, mta).getContent();
      String rettext = encodeBase64(bytes);
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(rettext);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }

  }


  public TextResponse_ctype generateTlvFromTemplateForUnregisteredMta(GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype generateTlvFromTemplateForUnregisteredMtaRequest)
                  throws java.rmi.RemoteException {
    try {
      GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype inreq = generateTlvFromTemplateForUnregisteredMtaRequest;
      // authenticate(inreq.getInputHeader());
      InputHeaderContent_ctype inputHeader = inreq.getInputHeader();
      //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
      //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
      AuthenticationTools.authenticateAndAuthorize(generateTlvFromTemplateForUnregisteredMtaRequest.getInputHeader().getUsername(),
                                                   generateTlvFromTemplateForUnregisteredMtaRequest.getInputHeader().getPassword(),
                                                   "configfile", wsInvocationId, logger);
      GenerateTlvFromTemplateForUnregisteredMtaInput_ctype inval = inreq
                      .getGenerateTlvFromTemplateForUnregisteredMtaInput();
      
      if(inval.getConfigFileGeneratorParameters().getContext().length()==0)
      {
        
        throw new DPPWebserviceException("Keine Parameter angegeben. Eingabefeld leer.");
      }

      UnregisteredMta mta = toUnregisteredMta(inval.getUnregisteredMta());
      MtaRequest request = toMtaRequest(inval.getMtaRequest());
      ConfigFileGenerator cfg = createConfigFileGenerator(inval.getConfigFileGeneratorParameters());
      byte[] bytes = cfg.generate(request, mta).getContent();
      String rettext = encodeBase64(bytes);
      TextResponse_ctype ret = new TextResponse_ctype();
      ret.setText(rettext);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceException("Error: ", e);
    }
  }


  /**
   * web service operation
   */
  public TlvToAsciiResponse_ctype tlvToAscii(TlvToAsciiRequest_ctype tlvToAsciiRequest) throws java.rmi.RemoteException {
    // authenticate(tlvToAsciiRequest.getInputHeader());
    InputHeaderContent_ctype inputHeader = tlvToAsciiRequest.getInputHeader();
//    AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//    AuthenticationTools.checkPermissions(inputHeader.getUsername(), "configfile", "*", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier(null);
    AuthenticationTools.authenticateAndAuthorize(tlvToAsciiRequest.getInputHeader().getUsername(),
                                                 tlvToAsciiRequest.getInputHeader().getPassword(),
                                                 "configfile", wsInvocationId, logger);
    try {
      String decoded = tlvToAscii(tlvToAsciiRequest.getTlvToAsciiInput().getText());
      TlvToAsciiResponse_ctype ret = new TlvToAsciiResponse_ctype();
      ret.setText(decoded);
      return ret;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("ERROR in tlvToAscii: ", e);
      throw new RemoteException(e.toString());
    }
    finally {
    }
  }


  public String tlvToAscii(String fileText) throws java.rmi.RemoteException {
    List<DocsisDecoding> list = DocsisEncodingTools.queryDocsisDecoding(logger);
    String decoded = decode(decodeBase64(fileText), list);
    return decoded;
  }


  /**
   * transform base64 encoded String into binary array
   */
  private byte[] decodeBase64(String text) {
    return Base64.decode(text);
  }


  /**
   * transform binary array into base64 encoded String
   */
  private String encodeBase64(byte[] bytes) {
    return Base64.encode(bytes);
  }


  /**
   * call decoding tool for binary file content
   */
  private String decode(final byte[] sourceFileData, List<DocsisDecoding> list) {
    ConfigFileDecoder decoder = new ConfigFileDecoder(list);
    String result;
    try {
      result = decoder.decode(sourceFileData);
    } catch (com.gip.juno.cfgdecode.DecoderException e) {
      throw new IllegalStateException("Failed to decode config file.", e);
    }
    return result;
  }


  private ConfigFileGenerator createConfigFileGenerator(ConfigFileGeneratorParameters_ctype params)
                  throws RemoteException {
    int parserPoolSize = 1;
    ConfigFileGenerator ret = new ConfigFileGenerator(getTextConfigTemplateList(), getDocsisEncodingList(),
                                                      params.getCmtsMicSecret(), params.getContext(), parserPoolSize);
    return ret;
  }


  private com.gip.juno.cfggenV6.ConfigFileGenerator createConfigFileGeneratorV6(
      ConfigFileGeneratorParameters_ctype params) throws RemoteException {
    int parserPoolSize = 1;

    com.gip.juno.cfggenV6.ConfigFileGenerator ret = new com.gip.juno.cfggenV6.ConfigFileGenerator(
        getTextConfigTemplateListV6(), getDocsisEncodingListV6(), params
            .getCmtsMicSecret(), params.getContext(), parserPoolSize);
    return ret;
  }

  private TextConfigGenerator createTextConfigGenerator(TextConfigGeneratorParameters_ctype params)
                  throws RemoteException {
    int parserPoolSize = 1;
    TextConfigGenerator ret = new TextConfigGenerator(getTextConfigTemplateList(), params.getContext(), parserPoolSize);
    return ret;
  }


  private MtaRequest toMtaRequest(MtaRequest_ctype request) {
    DeviceDetails details = toDeviceDetails(request.getDeviceDetails());
    boolean snmpNotif = request.isIsSendSnmpNotification();
    MtaRequest ret = new MtaRequest(request.getMacAddress(), request.getIpAddress(), request.getFileName(), details,
                                    snmpNotif,request.getRequestPktc(),request.getVersion());
    logger.info("Got MtaRequest data: " + ret.toString());
    return ret;
  }


  private CableModemRequest toCableModemRequest(CableModemRequest_ctype request) {
    DeviceDetails details = toDeviceDetails(request.getDeviceDetails());
    boolean snmpNotif = request.isIsSendSnmpNotification();
    String version = request.getVersion();
    CableModemRequest ret = new CableModemRequest(request.getMacAddress(), request.getIpAddress(),
                                                  request.getFileName(), details, snmpNotif,version);
    logger.info("Got CableModemRequest  data: " + ret.toString());
    return ret;
  }


  private DeviceDetails toDeviceDetails(DeviceDetails_ctype details) {
    DeviceDetails ret = new DeviceDetails(details.getVendorName(), details.getModelName(),
                                          details.getHardwareRevision(), details.getSoftwareRevision());
    logger.info("Got DeviceDetails data: " + ret.toString());
    return ret;
  }


  private InitializedCableModem toInitializedCableModem(InitializedCableModem_ctype cm) throws DPPWebserviceException,
                  FileNotFoundException {
    String cableModemMode = cm.getMode();
    int numcpes = Integer.parseInt(cm.getNumberOfCPEs());
    long ds = Long.parseLong(cm.getDownstreamSpeed());
    Long us = Long.parseLong(cm.getUpstreamSpeed());
    String configFile = null;
    String xml = cm.getXml();
    
    String ipMode = cm.getIpMode();
    String mtaEnable = cm.getMtaEnable();
    
    logger.debug("Got xml:" + xml);
    logger.debug("cpeIPs:" + cm.getCpeIPs());
    InitializedCableModem ret = new InitializedCableModem(cm.getMacAddress(), ds, us, cableModemMode, numcpes,
                                                          new IpV4AddressList(cm.getCpeIPs()), configFile,
                                                          buildMapFromXml(xml, XmlDecoderType.CM), ipMode,mtaEnable);
 


    logger.info("Got InitializedCableModem data: " + ret.toString());
    return ret;
  }


  private UnregisteredCableModem toUnregisteredCableModem(UnregisteredCableModem_ctype cm) {
    UnregisteredCableModem ret = new UnregisteredCableModem(cm.getMacAddress());
    logger.info("Got UnregisteredCableModem data: " + ret.toString());
    return ret;
  }


  private SipMta toSipMta(SipMta_ctype mta) throws DPPWebserviceException, FileNotFoundException {
    List<SipMtaPort> ports = new ArrayList<SipMtaPort>();
    SipMtaPortList_ctype portsinput = mta.getSipPorts();
    addSipPort(ports, portsinput.getSipMtaPort1());
    addSipPort(ports, portsinput.getSipMtaPort2());
    addSipPort(ports, portsinput.getSipMtaPort3());
    addSipPort(ports, portsinput.getSipMtaPort4());
    addSipPort(ports, portsinput.getSipMtaPort5());
    addSipPort(ports, portsinput.getSipMtaPort6());
    addSipPort(ports, portsinput.getSipMtaPort7());
    addSipPort(ports, portsinput.getSipMtaPort8());
    addSipPort(ports, portsinput.getSipMtaPort9());
    addSipPort(ports, portsinput.getSipMtaPort10());

    String xml = mta.getXml();
    SipMta ret = new SipMta(mta.getMac(), mta.getSoftswitchType(), mta.getSoftswitch(), "", ports, null,
                            buildMapFromXml(xml, XmlDecoderType.MTA));

    logger.info("Got SipMta data: " + ret.toString());
    return ret;
  }


  private void addSipPort(List<SipMtaPort> portlist, SipMtaPort_ctype port) throws DPPWebserviceException,
                  FileNotFoundException {
    if (port == null) {
      return;
    }
    portlist.add(toSipMtaPort(port));
  }


  private NcsMta toNcsMta(NcsMta_ctype mta) throws DPPWebserviceException, FileNotFoundException {
    String configFile = null;
    String xml = mta.getXml();
    NcsMta ret = new NcsMta(mta.getMac(), mta.getSoftswitchType(), mta.getSoftswitch(), "",
                            toListInteger(mta.getPortNumbers()), configFile, buildMapFromXml(xml, XmlDecoderType.MTA));
    logger.info("Got NcsMta data: " + ret.toString());
    return ret;
  }


  private IsdnMta toIsdnMta(IsdnMta_ctype mta) throws DPPWebserviceException, FileNotFoundException {
    List<SipMtaPort> ports = new ArrayList<SipMtaPort>();
    SipMtaPortList_ctype portsinput = mta.getSipPorts();
    addSipPort(ports, portsinput.getSipMtaPort1());
    addSipPort(ports, portsinput.getSipMtaPort2());
    addSipPort(ports, portsinput.getSipMtaPort3());
    addSipPort(ports, portsinput.getSipMtaPort4());
    addSipPort(ports, portsinput.getSipMtaPort5());
    addSipPort(ports, portsinput.getSipMtaPort6());
    addSipPort(ports, portsinput.getSipMtaPort7());
    addSipPort(ports, portsinput.getSipMtaPort8());
    addSipPort(ports, portsinput.getSipMtaPort9());
    addSipPort(ports, portsinput.getSipMtaPort10());
    String xml = mta.getXml();
    IsdnMta ret = new IsdnMta(mta.getMac(), mta.getSoftswitchType(), mta.getSoftswitch(), "", ports, null,
                              buildMapFromXml(xml, XmlDecoderType.MTA));
    logger.info("Got IsdnMta data: " + ret.toString());
    return ret;
  }


  private UninitializedMta toUninitializedMta(UninitializedMta_ctype mta) {
    UninitializedMta ret = new UninitializedMta(mta.getMac(), null);
    logger.info("Got UninitializedMta data: " + ret.toString());
    return ret;
  }


  private UnregisteredMta toUnregisteredMta(UnregisteredMta_ctype mta) {
    UnregisteredMta ret = new UnregisteredMta(mta.getMac());
    logger.info("Got UnregisteredMta data: " + ret.toString());
    return ret;
  }


  private List<Integer> toListInteger(int[] nums) {
    List<Integer> ret = new ArrayList<Integer>();
    for (int i : nums) {
      Integer newnum = Integer.valueOf(i);
      ret.add(newnum);
    }
    return ret;
  }


  private SipMtaPort toSipMtaPort(SipMtaPort_ctype port) throws DPPWebserviceException, FileNotFoundException {
    int portnum = Integer.parseInt(port.getPortNumber());
    String xml = port.getXml();
    SipMtaPort ret = new SipMtaPort(portnum, port.getDirectoryNumber(), port.getLocalNumber(), port.getAreaCode(),
                                    port.getUserName(), port.getPassword(), port.getRegistrarServer(),
                                    port.getProxyServer(), buildMapFromXml(xml, XmlDecoderType.MTA_SIP_PORT));
    logger.info("Got SipMtaPort data: " + ret.toString());
    return ret;
  }


  private List<TextConfigTemplate> getTextConfigTemplateList() throws RemoteException {
    List<TextConfigTemplate> ret = TextConfigTemplateTools.queryAllTextConfigTemplates(logger);
    // System.out.println(ret.size());
    logger.info("size of text config templates list = " + ret.size());
    return ret;
  }


  private List<com.gip.juno.cfggenV6.textconfig.template.TextConfigTemplate> getTextConfigTemplateListV6()
                  throws RemoteException {
    List<TextConfigTemplate> old = TextConfigTemplateTools.queryAllTextConfigTemplates(logger);
    List<com.gip.juno.cfggenV6.textconfig.template.TextConfigTemplate> result = new ArrayList<com.gip.juno.cfggenV6.textconfig.template.TextConfigTemplate>();


    for (TextConfigTemplate t : old) {
      TextConfigType tmptype;

      if (t.getType().equals(com.gip.juno.cfggen.textconfig.template.TextConfigType.CABLE_MODEM)) {
        tmptype = TextConfigType.CABLE_MODEM;
      }
      else {
        tmptype = TextConfigType.MTA;
      }

      com.gip.juno.cfggenV6.textconfig.template.TextConfigTemplate tmp = new com.gip.juno.cfggenV6.textconfig.template.TextConfigTemplate(
                                                                                                                                          t.getId(),
                                                                                                                                          tmptype,
                                                                                                                                          t.getTemplateName(),
                                                                                                                                          t.getConstraints(),
                                                                                                                                          t.getConstraintsScore(),
                                                                                                                                          t.getContent());
      result.add(tmp);

    }
    // System.out.println(ret.size());
    logger.info("size of text config templates list = " + result.size());
    return result;
  }


  private List<DocsisEncoding> getDocsisEncodingList() throws RemoteException {
    return DocsisEncodingTools.queryDocsisEncoding(logger);
  }


  private List<com.gip.juno.cfggenV6.tlvencoding.docsis.DocsisEncoding> getDocsisEncodingListV6()
                  throws RemoteException {
    List<DocsisEncoding> oldlist = DocsisEncodingTools.queryDocsisEncoding(logger);
    List<com.gip.juno.cfggenV6.tlvencoding.docsis.DocsisEncoding> result = new ArrayList<com.gip.juno.cfggenV6.tlvencoding.docsis.DocsisEncoding>();
    for (DocsisEncoding d : oldlist) {
      com.gip.juno.cfggenV6.tlvencoding.docsis.DocsisEncoding tmp = new com.gip.juno.cfggenV6.tlvencoding.docsis.DocsisEncoding(
                                                                                                                                d.getId(),
                                                                                                                                d.getParentId(),
                                                                                                                                d.getCmtsMicOrderNumber(),
                                                                                                                                d.getTypeName(),
                                                                                                                                d.getTypeEncoding(),
                                                                                                                                d.getValueDataTypeName(),
                                                                                                                                d.getValueDataTypeArguments());
      result.add(tmp);
    }

    return result;
  }


  private void validateXml(String xsdPrefix, String xml) throws DPPWebserviceException {
    WSTools.validate(xsdPrefix, xml, logger);
  }


  private Map<String, Object> buildMapFromXml(String xml, XmlDecoderType decoderType) throws FileNotFoundException,
                  DPPWebserviceException {
    logger.info("builtMapFromXml: Got xml = " + xml);
    String xsdPrefix = getXsdPrefix(decoderType);
    logger.debug("Got xsd prefix. " + xsdPrefix);

    validateXml(xsdPrefix, xml);
    File xsdFile = WSTools.getXsdFile(xsdPrefix);
    XmlDecoder decoder = XmlDecoderFactory.create(decoderType, xsdFile);
    Map<String, Object> map = decoder.decode(xml);
    return map;
  }


  private String getXsdPrefix(XmlDecoderType decoderType) throws DPPWebserviceException {
    if (decoderType == XmlDecoderType.CM) {
      return "cm";
    }
    else if (decoderType == XmlDecoderType.MTA) {
      return "mta";
    }
    else if (decoderType == XmlDecoderType.MTA_SIP_PORT) {
      return "mta_sip_port";
    }
    throw new DPPWebserviceException("ConfigFile: Unknown XmlDecoderType " + decoderType.toString());
  }
}
