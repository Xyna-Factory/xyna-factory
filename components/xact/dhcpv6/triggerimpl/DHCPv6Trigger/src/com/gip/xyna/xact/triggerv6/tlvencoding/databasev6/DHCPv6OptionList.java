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

package com.gip.xyna.xact.triggerv6.tlvencoding.databasev6;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class DHCPv6OptionList {

  static List<DHCPv6Encoding> liste = new ArrayList<DHCPv6Encoding>();


  public static void main(String argh[]) throws PersistenceLayerException {
    DHCPv6OptionList.fillListe();
    LoadConfigv6 anbindung = new LoadConfigv6();


    anbindung.setUp();
    anbindung.createListOfDHCPEncodingEntry(liste);

    System.out.println("Liste:");

    for (DHCPv6Encoding bla : liste) {
      System.out.println(bla.getId() + " : " + bla.getParentId() + " : " + bla.getTypeName() + " : " + bla
                      .getTypeEncoding() + " : " + bla.getValueDataTypeName());
    }


  }


  // Fuegt Kinder hinzu. Parameter: letzte ID, ID des Elternteils f�r die Kinder und Encodings von Kindern, die nicht
  // hinzugefuegt werden sollen
  public static void addRelayChildren(int lastID, int parentID) {
    DHCPv6Encoding currentEntry;
    DHCPv6Encoding result;
    int tmpcounter = 1;

    for (int i = 1; i < 21; i++) // Elternoptionen hinzuf�gen
    {
      currentEntry = DHCPv6OptionList.liste.get(i);
      result = new DHCPv6Encoding(lastID + tmpcounter, parentID, currentEntry.getTypeName(), currentEntry
                      .getTypeEncoding(), currentEntry.getEnterpriseNr(), currentEntry.getValueDataTypeName(),
                                  currentEntry.getValueDataTypeArguments());
      liste.add(result);
      tmpcounter++;
    }

    for (int i = 21; i < 397; i++) // Alle Kinderoptionen hinzufuegen
    {
      currentEntry = DHCPv6OptionList.liste.get(i);

      result = new DHCPv6Encoding(lastID + tmpcounter, currentEntry.getParentId() + lastID, currentEntry.getTypeName(),
                                  currentEntry.getTypeEncoding(), currentEntry.getEnterpriseNr(), currentEntry
                                                  .getValueDataTypeName(), currentEntry.getValueDataTypeArguments());
      liste.add(result);
      tmpcounter++;
    }


  }


  // Fuegt Kinder hinzu. Parameter: letzte ID, ID des Elternteils f�r die Kinder und Encodings von Kindern, die nicht
  // hinzugefuegt werden sollen
  public static void addChildren(int lastID, int parentID, List<Integer> dontadd) {
    DHCPv6Encoding currentEntry;
    DHCPv6Encoding result;
    int tmpcounter = 1;

    List<Integer> dontaddids = new ArrayList<Integer>();

    for (int i = 1; i < 21; i++) // Elternoptionen hinzuf�gen
    {
      currentEntry = DHCPv6OptionList.liste.get(i);
      if (!dontadd.contains(currentEntry.getTypeEncoding())) {
        result = new DHCPv6Encoding(lastID + tmpcounter, parentID, currentEntry.getTypeName(), currentEntry
                        .getTypeEncoding(), currentEntry.getEnterpriseNr(), currentEntry.getValueDataTypeName(),
                                    currentEntry.getValueDataTypeArguments());
        liste.add(result);
        tmpcounter++;
      }
      else {
        dontaddids.add(currentEntry.getId());
      }

    }

    for (int i = 21; i < 41; i++) // (DUID) Kinderoptionen als Kindeskinder hinzufuegen (DUID LLT/EN/LL) inklusive
                                  // Subtypen
    {
      currentEntry = DHCPv6OptionList.liste.get(i);

      int missing = 0; // fehlende Elemente in der duplizierten Liste bei ParentIDs beachten, die durch dontadd nicht
                       // angehaengt werden
      for (int t : dontaddids) {
        if (t < currentEntry.getParentId())
          missing++;
      }
      result = new DHCPv6Encoding(lastID + tmpcounter, currentEntry.getParentId() + lastID - missing, currentEntry
                      .getTypeName(), currentEntry.getTypeEncoding(), currentEntry.getEnterpriseNr(), currentEntry
                      .getValueDataTypeName(), currentEntry.getValueDataTypeArguments());
      liste.add(result);
      tmpcounter++;
    }

    for (int i = 41; i < 59; i++) // (DUID) Kinderoptionen als Kindeskinder hinzufuegen (DUID LLT/EN/LL) inklusive
                                  // Subtypen
    {
      currentEntry = DHCPv6OptionList.liste.get(i);

      int missing = 0; // fehlende Elemente in der duplizierten Liste bei ParentIDs beachten, die durch dontadd nicht
                       // angehaengt werden
      for (int t : dontaddids) {
        if (t < currentEntry.getParentId())
          missing++;
      }
      result = new DHCPv6Encoding(lastID + tmpcounter, currentEntry.getParentId() + lastID - missing, currentEntry
                      .getTypeName(), currentEntry.getTypeEncoding(), currentEntry.getEnterpriseNr(), currentEntry
                      .getValueDataTypeName(), currentEntry.getValueDataTypeArguments());
      liste.add(result);
      tmpcounter++;
    }


  }


  public static void fillListe() {
    // Optionen anlegen

    Map<String, String> valuearguments = new HashMap<String, String>();
    List<Integer> dontadd = new ArrayList<Integer>();


    // Anfang
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(0, null, "Pad", 0, null, "Padding", valuearguments));
    valuearguments.clear();

    // ClientID Option T=1
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(1, null, "ClientID", 1, null, "DUID", valuearguments));
    valuearguments.clear();

    // ServerID Option T=2
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(2, null, "ServerID", 2, null, "DUID", valuearguments));
    valuearguments.clear();

    // IA_NA Option T=3
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(3, null, "IA_NA", 3, null, "IANA", valuearguments));
    valuearguments.clear();

    // IA_TA Option T=4
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(4, null, "IA_TA", 4, null, "IATA", valuearguments));
    valuearguments.clear();

    // IA_Address Option T=5
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(5, null, "IA_Address", 5, null, "IAAddress", valuearguments));
    valuearguments.clear();

    // RequestList Option T=6
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(6, null, "RequestList", 6, null, "OctetString", valuearguments));
    valuearguments.clear();

    // Preference Option T=7
    valuearguments.put("\"nrBytes\"", "\"1\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(7, null, "Preference", 7, null, "UnsignedInteger", valuearguments));
    valuearguments.clear();

    // Elapsed Time T=8
    valuearguments.put("\"nrBytes\"", "\"2\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(8, null, "ElapsedTime", 8, null, "UnsignedInteger", valuearguments));
    valuearguments.clear();

    // Relay Message T=9
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(9, null, "RelayMessage", 9, null, "RelayMessage", valuearguments));
    valuearguments.clear();

    // Rapid Commit T=14
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(10, null, "RapidCommit", 14, null, "OctetString", valuearguments));
    valuearguments.clear();

    // User Class T=15
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(11, null, "UserClass", 15, null, "OctetString", valuearguments));
    valuearguments.clear();

    // Vendor Class T=16
    valuearguments.put("\"enterprisenr\"", "\"4491\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(12, null, "VendorClass", 16, null, "EContainer", valuearguments));
    valuearguments.clear();

    // Vendor Specific Information T=17
    valuearguments.put("\"enterprisenr\"", "\"4491\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(13, null, "VendorSpecificInformation", 17, null, "EContainer",
                                                  valuearguments));
    valuearguments.clear();

    // InterfaceID T=18
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(14, null, "InterfaceID", 18, null, "OctetString", valuearguments));
    valuearguments.clear();

    // Reconfigure Message T=19
    valuearguments.put("\"nrBytes\"", "\"1\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(15, null, "ReconfigureMessage", 19, null, "UnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    // ReconfigureAccept T=20
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(16, null, "ReconfigureAccept", 20, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    // DNSServer T=23
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(17, null, "DNSServer", 23, null, "IpV6AddressList", valuearguments));
    valuearguments.clear();

    // DNSSearchList T=24
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(18, null, "DNSSearchList", 24, null, "OctetString", valuearguments));
    valuearguments.clear();

    // IA PD T=25
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(19, null, "IA_PD", 25, null, "IAPD", valuearguments));
    valuearguments.clear();

    // IA Prefix T=26
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(20, null, "IAPrefix", 26, null, "IAPDOption", valuearguments));
    valuearguments.clear();


    // ======================== Ende der Elternoptionen

    // Beginn DUID Kinderoptionen

    // DUID LLT als Kind von ClientID
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(21, 1, "DUID-LLT", 1, null, "DUIDLLT", valuearguments));
    valuearguments.clear();

    // DUID EN als Kind von ClientID
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(22, 1, "DUID-EN", 2, null, "DUIDEN", valuearguments));
    valuearguments.clear();

    // DUID LL als Kind von ClientID
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(23, 1, "DUID-LL", 3, null, "DUIDLL", valuearguments));
    valuearguments.clear();

    // DUID LLT als Kind von ServerID
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(24, 2, "DUID-LLT", 1, null, "DUIDLLT", valuearguments));
    valuearguments.clear();

    // DUID EN als Kind von ServerID
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(25, 2, "DUID-EN", 2, null, "DUIDEN", valuearguments));
    valuearguments.clear();

    // DUID LL als Kind von ServerID
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(26, 2, "DUID-LL", 3, null, "DUIDLL", valuearguments));
    valuearguments.clear();

    // Kindeskinder von DUID LLTs

    valuearguments.put("\"nrBytes\"", "\"2\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(27, 21, "HardwareType", 1, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(28, 21, "Time", 2, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(29, 21, "LinkLayerAddress", 3, null, "NonTLVOctetString",
                                                  valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"2\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(30, 24, "HardwareType", 1, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(31, 24, "Time", 2, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(32, 24, "LinkLayerAddress", 3, null, "NonTLVOctetString",
                                                  valuearguments));
    valuearguments.clear();

    // Kindeskinder von DUID ENs

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(33, 22, "EnterpriseNr", 1, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(34, 22, "Identifier", 2, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(35, 25, "EnterpriseNr", 1, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(36, 25, "Identifier", 2, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();


    // Kindeskinder von DUID LLs

    valuearguments.put("\"nrBytes\"", "\"2\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(37, 23, "HardwareType", 1, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(38, 23, "LinkLayerAddress", 2, null, "NonTLVOctetString",
                                                  valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"2\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(39, 26, "HardwareType", 1, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(40, 26, "LinkLayerAddress", 2, null, "NonTLVOctetString",
                                                  valuearguments));
    valuearguments.clear();

    // Kinder von Option 17 (ID=13) (Vendor Specific Information) TODO Childrenmethode anpassen

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(41, 13, "CL_OPTION_ORO", 1, null, "OctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(42, 13, "CL_OPTION_DEVICE_TYPE", 2, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(43, 13, "CL_OPTION_EMBEDDED_COMPONENTS_LIST", 3, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(44, 13, "CL_OPTION_DEVICE_SERIAL_NUMBER", 4, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(45, 13, "CL_OPTION_HARDWARE_VERSION_NUMBER", 5, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(46, 13, "CL_OPTION_SOFTWARE_VERSION_NUMBER", 6, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(47, 13, "CL_OPTION_BOOT_ROM_VERSION", 7, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(48, 13, "CL_OPTION_VENDOR_OUI", 8, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(49, 13, "CL_OPTION_MODEL_NUMBER", 9, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(50, 13, "CL_OPTION_VENDOR_NAME", 10, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(51, 13, "CL_OPTION_TFTP_SERVERS", 32, null, "IpV6AddressList",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(52, 13, "CL_OPTION_CONFIG_FILE_NAME", 33, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(53, 13, "CL_OPTION_SYSLOG_SERVERS", 34, null, "IpV6AddressList",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(54, 13, "CL_OPTION_MODEM_CAPABILITIES", 35, null, "OctetString",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(55, 13, "CL_OPTION_MODEM_CAPABILITIES", 36, null, "MacAddress",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(56, 13, "OPTION_RFC868_SERVERS", 37, null, "IpV6AddressList",
                                                  valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(57, 13, "CL_OPTION_TIME_OFFSET", 38, null, "UnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"1\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(58, 13, "CL_OPTION_IP_PREF", 39, null, "UnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();


    // Kinder von IA NA (Option 3 / ID 3)

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(59, 3, "IAID", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(60, 3, "T1", 102, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(61, 3, "T2", 103, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    // ab hier "Skript" um alle Kinder zu ergaenzen

    // IANA

    dontadd.add(new Integer(3));
    dontadd.add(new Integer(4));
    dontadd.add(new Integer(25));
    dontadd.add(new Integer(26));

    DHCPv6OptionList.addChildren(61, 3, dontadd);
    dontadd.clear();

    // ID = 115

    // IAAddress (ID 64)

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(116, 64, "IPv6", 101, null, "NonTLVIpV6Address", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(117, 64, "T1", 102, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(118, 64, "T2", 103, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Integer(3));
    dontadd.add(new Integer(4));
    dontadd.add(new Integer(5));
    dontadd.add(new Integer(25));
    dontadd.add(new Integer(26));

    DHCPv6OptionList.addChildren(118, 64, dontadd);
    dontadd.clear();

    // ID = 171


    // Kinder von IA PD (Option 25 / ID 19)

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(172, 19, "IAID", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(173, 19, "T1", 102, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(174, 19, "T2", 103, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Integer(3));
    dontadd.add(new Integer(4));
    dontadd.add(new Integer(5));
    dontadd.add(new Integer(25));

    DHCPv6OptionList.addChildren(174, 19, dontadd);
    dontadd.clear();

    // ID = 228


    // IA_PD Option (ID 190)


    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(229, 190, "T1", 101, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(230, 190, "T2", 102, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"1\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(231, 190, "PrefixLength", 103, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(232, 190, "IPv6", 104, null, "NonTLVIpV6Address", valuearguments));
    valuearguments.clear();

    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Integer(3));
    dontadd.add(new Integer(4));
    dontadd.add(new Integer(5));
    dontadd.add(new Integer(25));
    dontadd.add(new Integer(26));

    DHCPv6OptionList.addChildren(232, 190, dontadd);
    dontadd.clear();

    // ID = 285


    // Kinder von IA TA (Option 4 / ID 4)

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(286, 4, "IAID", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Integer(3));
    dontadd.add(new Integer(4));
    dontadd.add(new Integer(25));
    dontadd.add(new Integer(26));

    DHCPv6OptionList.addChildren(286, 4, dontadd);
    dontadd.clear();

    // ID = 340

    // IAAddress (ID 289)

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(341, 289, "IPv6", 101, null, "NonTLVIpV6Address", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(342, 289, "T1", 102, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(343, 289, "T2", 103, null, "NonTLVUnsignedInteger", valuearguments));
    valuearguments.clear();

    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Integer(3));
    dontadd.add(new Integer(4));
    dontadd.add(new Integer(5));
    dontadd.add(new Integer(25));
    dontadd.add(new Integer(26));

    DHCPv6OptionList.addChildren(343, 289, dontadd);
    dontadd.clear();

    // ID = 396


    // Kinder zu RelayMessage Option T=9 + Alle Kinder zu den Roten nochmal!

    valuearguments.put("\"nrBytes\"", "\"1\"");
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(397, 9, "InnerType", 101, null, "NonTLVUnsignedInteger",
                                                  valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(398, 9, "TXID", 102, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    // ab hier "Skript" um alle Kinder zu ergaenzen

    DHCPv6OptionList.addRelayChildren(398, 9);

    // ID = 794

    // Kinder von Vendor Class (diverse ID)
    DHCPv6OptionList.liste.add(new DHCPv6Encoding(795, 12, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(796, 71, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(797, 127, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(798, 183, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(799, 241, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(800, 296, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(801, 352, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(802, 410, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(803, 469, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(804, 525, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(805, 581, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(806, 639, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(807, 694, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(808, 750, "Data", 101, null, "NonTLVOctetString", valuearguments));
    valuearguments.clear();

    // End of Data Markierung

    DHCPv6OptionList.liste.add(new DHCPv6Encoding(5000, null, "End-of-Data", 5000, null, "EndOfDataMarker",
                                                  valuearguments));
    valuearguments.clear();


  }


}
