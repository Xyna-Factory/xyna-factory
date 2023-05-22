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
package xact.dhcp.client;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.tlvdecoding.dhcp.DHCPConfigurationDecoder;
import com.gip.xyna.xact.tlvencoding.dhcp.DHCPConfigurationEncoder;
import com.gip.xyna.xact.tlvencoding.dhcp.DHCPEncoding;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

public class EncoderBuilder {

  private static final Logger logger = CentralFactoryLogging.getLogger(EncoderBuilder.class);

  private static Collection<DHCPEncoding> dhcpEncodings;
  private static boolean odsUsed;

  public static XynaPropertyBoolean USE_DATABASE = new XynaPropertyBoolean("xact.dhcp.client.use_optionsv4_table", false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Modul DHCPClient liest Tabelle optionsv4 f�r DHCP-Encodings "+
                                                         "(ansonsten werden Default-Eintr�ge verwendet).")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Module DHCPClient reads table optionsv4 for DHCP-encodings "+
                                                         "(otherwise default entries will be used).");
  
  public static void onDeployment() throws PersistenceLayerException {
    if( USE_DATABASE.get() ) {
      odsUsed = true;
      ODS ods = ODSImpl.getInstance(true);
      ods.registerStorable(DHCPEncoding.class);
      ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        dhcpEncodings = connection.loadCollection(DHCPEncoding.class);
        if( dhcpEncodings == null || dhcpEncodings.isEmpty() ) {
          logger.warn(" no DHCP-encodings found");
        }
      }
      finally {
        try {
          connection.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Failed to close connection", e);
        }
      }
    } else {
      dhcpEncodings = createBaseDhcpEncodings();
    }
  }


  public static void onUndeployment() throws PersistenceLayerException {
    if( odsUsed ) {
      ODS ods = ODSImpl.getInstance(true);
      ods.unregisterStorable(DHCPEncoding.class);
    }
  }
  
  public static DHCPConfigurationEncoder buildEncoder() {
    return new DHCPConfigurationEncoder(new ArrayList<DHCPEncoding>(dhcpEncodings));
  }

  public static DHCPConfigurationDecoder buildDecoder() {
    return new DHCPConfigurationDecoder(new ArrayList<DHCPEncoding>(dhcpEncodings));
  }


  private static Collection<DHCPEncoding> createBaseDhcpEncodings() {
    DHCPEncodingBuilder eb = new DHCPEncodingBuilder();
    eb.fillBaseOptions();
    return eb.getDhcpEncodings();
  }

  
  
  public static class DHCPEncodingBuilder {
    
    private List<DHCPEncoding> dhcpEncodings = new ArrayList<DHCPEncoding>();
    private AtomicInteger id = new AtomicInteger(); 
    
    public List<DHCPEncoding> getDhcpEncodings() {
      return dhcpEncodings;
    }
    
    private DHCPEncoding createDHCPEncoding(String typeName, int typeEncoding, String valueDataTypeName) {
      Map<String, String> valueDataTypeArguments = Collections.emptyMap();
      return createDHCPEncoding_Map(typeName,typeEncoding,valueDataTypeName,valueDataTypeArguments);
    }
    
    private DHCPEncoding createDHCPEncoding_Bytes(String typeName, int typeEncoding, String valueDataTypeName, int bytes) {
      //Map<String, String> valueDataTypeArguments = Collections.singletonMap("\"nrBytes\"", "\""+bytes+"\"");
      Map<String, String> valueDataTypeArguments = Collections.singletonMap("nrBytes", ""+bytes);
      return createDHCPEncoding_Map(typeName,typeEncoding,valueDataTypeName,valueDataTypeArguments);
   }
    
    private DHCPEncoding createDHCPEncoding_KeyValue(String typeName, int typeEncoding, String valueDataTypeName, String key, String value) {
      //Map<String, String> valueDataTypeArguments = Collections.singletonMap("\""+key+"\"", "\""+value+"\"");
      Map<String, String> valueDataTypeArguments = Collections.singletonMap(key, value);
      return createDHCPEncoding_Map(typeName,typeEncoding,valueDataTypeName,valueDataTypeArguments);
    }

    private DHCPEncoding createDHCPEncoding_Map(String typeName, int typeEncoding, String valueDataTypeName, Map<String, String> valueDataTypeArguments) {
      DHCPEncoding de = new DHCPEncoding(id.getAndIncrement(), null, typeName, typeEncoding, null, valueDataTypeName, valueDataTypeArguments );
      dhcpEncodings.add(de);
      return de;
    }

    public void fillBaseOptions() {
      // Optionen anlegen

      // Anfang
      createDHCPEncoding( "Pad", 0, "Padding");

      // Subnet Option T=1
      createDHCPEncoding( "Subnet", 1, "IpV4Address");

      // Time Offset Option T=2
      createDHCPEncoding_Bytes( "TimeOffset", 2, "UnsignedInteger", 4);

      // Router Option T=3
      createDHCPEncoding( "Router", 3, "IpV4AddressList");

      // Time Servers Option T=4
      createDHCPEncoding( "TimeServers", 4, "IpV4AddressList");

      // Domain Name Servers Option T=6
      createDHCPEncoding( "DomainNameServers", 6, "IpV4AddressList");

      // Log Servers Option T=7
      createDHCPEncoding( "LogServers", 7, "IpV4AddressList");

      // Hostname Option T=12
      createDHCPEncoding( "Hostname", 12, "OctetString");

      // Domainname Option T=15
      createDHCPEncoding( "Domainname", 15, "OctetString");

      // Vendor Specific Information Option T=43
      createDHCPEncoding( "VendorSpecificInformation43", 43, "Container");

      // requested address Option T=50
      createDHCPEncoding( "RequestedAddress", 50, "IpV4Address");

      // LeaseTime Option T=51
      createDHCPEncoding_Bytes( "LeaseTime", 51, "UnsignedInteger", 4);

      // DHCP Message Option T=53
      createDHCPEncoding_Bytes( "DHCPMessageType", 53, "UnsignedInteger", 1);

      // Server Identifier Option T=54
      createDHCPEncoding( "ServerIdentifier", 54, "IpV4Address");

      // Parameter Request List Option T=55
      createDHCPEncoding( "ParameterRequestList", 55, "OctetString");

      // Maximum DHCP Message Size Option T=57
      createDHCPEncoding_Bytes( "MaximumDHCPMessageSize", 57, "UnsignedInteger", 2);

      // Renewal Time Option T=58
      createDHCPEncoding_Bytes( "RenewalTime", 58, "UnsignedInteger", 4);

      // Rebinding Time Option T=59
      createDHCPEncoding_Bytes( "RebindingTime", 59, "UnsignedInteger", 4);

      // Vendor Class Identifier (Docsis) T=60
      createDHCPEncoding_KeyValue( "VendorClassIdentifierDocsis", 60, "VContainer", "encoding", "docsis2.0:");

      // Vendor Class Identifier (Pkt) T=60
      createDHCPEncoding_KeyValue( "VendorClassIdentifierDocsis", 60, "VContainer", "encoding", "pktc1.5:");

      // Client Identifier Option T = 61
      createDHCPEncoding( "ClientIdentifier", 61, "OctetString");

      // TFTP Server Name T=66
      createDHCPEncoding( "TFTPServerName", 66, "OctetString");

      // Bootfile Name T=67
      createDHCPEncoding( "BootFileName", 67, "OctetString");

      // Agent Information Option T=82
      int parentId = createDHCPEncoding( "AgentInformation", 82, "Container").getId();
      //Agent Circuit ID SubOption T=1
      createDHCPEncoding( "AgentCircuitID", 1, "OctetString" ).setParentId(parentId);
      // Agent Remote ID Option T=2
      createDHCPEncoding( "AgentRemoteID", 2, "OctetString").setParentId(parentId);
      // Subscriber-ID T=6
      createDHCPEncoding( "SubscriberID", 6, "OctetString").setParentId(parentId);


      // Client Last Transaction Time Option T=91
      createDHCPEncoding_Bytes("ClientLastTransactionTime", 91, "UnsignedInteger", 4);

      // Docsis Option T= 122
      createDHCPEncoding( "Docsis", 122, "Container");

      // VendorSpecificInformation T=125
      createDHCPEncoding_KeyValue( "VendorSpecificInformation", 125, "EContainer", "enterprisenr", "4491");



      // End of Data Markierung
      createDHCPEncoding( "End-of-Data", 255, "EndOfDataMarker");

    }
  }


  public static void main(String[] args) {
    buildEncoder();
  }



}