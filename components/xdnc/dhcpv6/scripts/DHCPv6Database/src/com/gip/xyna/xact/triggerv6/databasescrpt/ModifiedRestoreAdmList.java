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

package com.gip.xyna.xact.triggerv6.databasescrpt;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ModifiedRestoreAdmList {

  static List<DHCPv6EncodingAdm> liste = new ArrayList<DHCPv6EncodingAdm>();


  public static void main(String argh[]) throws PersistenceLayerException {
    ModifiedRestoreAdmList.fillListe();
    LoadAdmList anbindung = new LoadAdmList();


    anbindung.setUp();
    anbindung.createListOfDHCPEncodingEntry(liste);

    System.out.println("Liste:");

    for (DHCPv6EncodingAdm bla : liste) {
      System.out.println(bla.getId() + " : " + bla.getParentId() + " : " + bla.getTypeName() + " : " + bla
                      .getTypeEncoding() + " : " + bla.getValueDataTypeName());
    }


  }


  public static void fillListe() {
    // Optionen anlegen

    Map<String, String> valuearguments = new HashMap<String, String>();

    int newid = 0;
    // Anfang
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "Pad", 0, null, "Padding", valuearguments,
                                                           false, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // ClientID Option T=1
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "ClientID", 1, null, "DUID", valuearguments,
                                                           true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    int clientid = newid;
    newid++;

    // ServerID Option T=2
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "ServerID", 2, null, "DUID", valuearguments,
                                                           true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    int serverid = newid;
    newid++;


    // IA_NA Option T=3
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "IA_NA", 3, null, "IANA", valuearguments, true,
                                                           "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // IA_TA Option T=4
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "IA_TA", 4, null, "IATA", valuearguments, true,
                                                           "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // IA_Address Option T=5
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "IA_Address", 5, null, "IAAddress", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // RequestList Option T=6
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "RequestList", 6, null, "OctetString", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Preference Option T=7
    valuearguments.put("\"nrBytes\"", "\"1\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "Preference", 7, null, "UnsignedInteger", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Elapsed Time T=8
    valuearguments.put("\"nrBytes\"", "\"2\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "ElapsedTime", 8, null, "UnsignedInteger", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Relay Message T=9
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "RelayMessage", 9, null, "RelayMessage", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Statuscode Message T=13
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "StatusCode", 13, null, "OctetString", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    // Rapid Commit T=14
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "RapidCommit", 14, null, "OctetString", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // User Class T=15
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "UserClass", 15, null, "OctetString", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Vendor Class T=16
    valuearguments.put("\"enterprisenr\"", "\"4491\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "VendorClass4491", 16, null, "EOctetString",
                                               valuearguments, true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Vendor Class T=16 mit Enterprisenr 872 (in Traces Value leer)
    valuearguments.put("\"enterprisenr\"", "\"872\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "VendorClass872", 16, null, "EContainer", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Vendor Class T=16 mit Enterprisenr 311 (in Traces Value leer)
    valuearguments.put("\"enterprisenr\"", "\"311\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "VendorClass311", 16, null, "EOctetString", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    // Vendor Specific Information T=17
    valuearguments.put("\"enterprisenr\"", "\"4491\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "VendorSpecificInformation4491", 17, null,
                                                           "EContainer", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    int vendorid4491 = newid;
    newid++;

    // Vendor Specific Information T=17
    valuearguments.put("\"enterprisenr\"", "\"3561\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "VendorSpecificInformation3561", 17, null,
                                                           "EContainer", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    int vendorid3561 = newid;
    newid++;


    // InterfaceID T=18
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "InterfaceID", 18, null, "OctetString", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Reconfigure Message T=19
    valuearguments.put("\"nrBytes\"", "\"1\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "ReconfigureMessage", 19, null,
                                                           "UnsignedInteger", valuearguments, true, "", "", "", "", "",
                                                           -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // ReconfigureAccept T=20
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "ReconfigureAccept", 20, null, "Container", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // DNSServer T=23
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "DNSServer", 23, null, "IpV6AddressList", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // DNSSearchList T=24
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "DNSSearchList", 24, null, "OctetString", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // IA PD T=25
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "IA_PD", 25, null, "IAPD", valuearguments,
                                                           true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // IA Prefix T=26
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "IAPrefix", 26, null, "IAPDOption", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // RemoteID T=37
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "RemoteID", 37, null, "OctetString", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    // FQDN T=39
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "FQDN", 39, null, "OctetString", valuearguments, true, "",
                                               "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // LeaseQueryV6 T=44
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "LeaseQuery", 44, null, "LeaseQuery", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // ClientData T=45
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "ClientData", 45, null, "Container", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Client Link Option T=48
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "ClientLink", 48, null, "OctetString", valuearguments,
                                               true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    // RelayID T=53
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "RelayID", 53, null, "DUID", valuearguments,
                                                           true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    int relayid = newid;
    newid++;

    // OPTION_AFTR_NAME T=64
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, null, "OPTION_AFTR_NAME", 64, null, "OctetString",
                                               valuearguments, true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Testoptionen fuer Check

    // ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "Testoption1", 200, null, "OctetString",
    // valuearguments,
    // false, "NEW","","TestOption1","","",-1,-1,-1,"Integer",""));
    // valuearguments.clear();
    // newid++;


    // Testoption 101

    // valuearguments.put("\"enterprisenr\"", "\"4711\"");
    // ModifiedModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "Option1014711", 101, null,
    // "EOctetString",
    // valuearguments, false, "NEW","","","",""));
    // valuearguments.clear();
    // newid++;
    //
    // valuearguments.put("\"enterprisenr\"", "\"4712\"");
    // ModifiedModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "Option1014712", 101, null,
    // "EOctetString",
    // valuearguments, false, "DEL","","","",""));
    // valuearguments.clear();
    // newid++;


    // ======================== Ende der Elternoptionen

    // Beginn DUID Kinderoptionen


    int[] duidllt = new int[3];
    int[] duiden = new int[3];
    int[] duidll = new int[3];


    // DUID LLT als Kind von ClientID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, clientid, "DUID-LLT", 1, null, "DUIDLLT", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duidllt[0] = newid;
    newid++;

    // DUID EN als Kind von ClientID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, clientid, "DUID-EN", 2, null, "DUIDEN", valuearguments, true, "",
                                               "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duiden[0] = newid;
    newid++;


    // DUID LL als Kind von ClientID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, clientid, "DUID-LL", 3, null, "DUIDLL", valuearguments, true, "",
                                               "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duidll[0] = newid;
    newid++;

    // DUID LLT als Kind von ServerID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, serverid, "DUID-LLT", 1, null, "DUIDLLT", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duidllt[1] = newid;
    newid++;

    // DUID EN als Kind von ServerID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, serverid, "DUID-EN", 2, null, "DUIDEN", valuearguments, true, "",
                                               "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duiden[1] = newid;
    newid++;

    // DUID LL als Kind von ServerID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, serverid, "DUID-LL", 3, null, "DUIDLL", valuearguments, true, "",
                                               "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duidll[1] = newid;
    newid++;

    // DUID LLT als Kind von RelayID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, relayid, "DUID-LLT", 1, null, "DUIDLLT", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duidllt[2] = newid;
    newid++;

    // DUID EN als Kind von RelayID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, relayid, "DUID-EN", 2, null, "DUIDEN", valuearguments, true, "",
                                               "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duiden[2] = newid;
    newid++;

    // DUID LL als Kind von RelayID
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, relayid, "DUID-LL", 3, null, "DUIDLL", valuearguments, true, "",
                                               "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    duidll[2] = newid;
    newid++;


    // Kindeskinder von DUID LLTs

    valuearguments.put("\"nrBytes\"", "\"2\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidllt[0], "HardwareType", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"4\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, duidllt[0], "Time", 2, null, "NonTLVUnsignedInteger",
                                               valuearguments, true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidllt[0], "LinkLayerAddress", 3, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"2\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidllt[1], "HardwareType", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"4\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, duidllt[1], "Time", 2, null, "NonTLVUnsignedInteger",
                                               valuearguments, true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidllt[1], "LinkLayerAddress", 3, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    valuearguments.put("\"nrBytes\"", "\"2\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidllt[2], "HardwareType", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"4\"");
    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, duidllt[2], "Time", 2, null, "NonTLVUnsignedInteger",
                                               valuearguments, true, "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidllt[2], "LinkLayerAddress", 3, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // Kindeskinder von DUID ENs

    valuearguments.put("\"nrBytes\"", "\"4\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duiden[0], "EnterpriseNr", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duiden[0], "Identifier", 2, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"4\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duiden[1], "EnterpriseNr", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duiden[1], "Identifier", 2, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"4\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duiden[2], "EnterpriseNr", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duiden[2], "Identifier", 2, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    // Kindeskinder von DUID LLs

    valuearguments.put("\"nrBytes\"", "\"2\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidll[0], "HardwareType", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidll[0], "LinkLayerAddress", 2, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"2\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidll[1], "HardwareType", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidll[1], "LinkLayerAddress", 2, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"2\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidll[2], "HardwareType", 1, null,
                                                           "NonTLVUnsignedInteger", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, duidll[2], "LinkLayerAddress", 2, null,
                                                           "NonTLVOctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    // Kinder von Option 17 (ID=13) (Vendor Specific Information) TODO Childrenmethode anpassen

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_ORO", 1, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_DEVICE_TYPE", 2, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_EMBEDDED_COMPONENTS_LIST",
                                                           3, null, "OctetString", valuearguments, true, "", "", "",
                                                           "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_DEVICE_SERIAL_NUMBER", 4,
                                                           null, "OctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_HARDWARE_VERSION_NUMBER", 5,
                                                           null, "OctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_SOFTWARE_VERSION_NUMBER", 6,
                                                           null, "OctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_BOOT_ROM_VERSION", 7, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_VENDOR_OUI", 8, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_MODEL_NUMBER", 9, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_VENDOR_NAME", 10, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_TFTP_SERVERS", 32, null,
                                                           "IpV6AddressList", valuearguments, true, "", "", "", "", "",
                                                           -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_CONFIG_FILE_NAME", 33, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_SYSLOG_SERVERS", 34, null,
                                                           "IpV6AddressList", valuearguments, true, "", "", "", "", "",
                                                           -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_MODEM_CAPABILITIES", 35,
                                                           null, "OctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_DEVICE_ID", 36, null,
                                                           "MacAddress", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "OPTION_RFC868_SERVERS", 37, null,
                                                           "IpV6AddressList", valuearguments, true, "", "", "", "", "",
                                                           -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    valuearguments.put("\"nrBytes\"", "\"4\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_TIME_OFFSET", 38, null,
                                                           "UnsignedInteger", valuearguments, true, "", "", "", "", "",
                                                           -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // 1025 und 1026
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_DOCS_CMTS_CAP", 1025, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_CM_MAC_ADDR", 1026, null,
                                                           "MacAddress", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;


    valuearguments.put("\"nrBytes\"", "\"1\"");
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_IP_PREF", 39, null,
                                                           "UnsignedInteger", valuearguments, true, "", "", "", "", "",
                                                           -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_CCC", 2170, null,
                                                           "Container", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();

    int vendorsub2170 = newid;
    newid++;


    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorid4491, "CL_OPTION_CCCV6", 2171, null,
                                                           "Container", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    int vendorsub2171 = newid;
    newid++;

    // Suboptionen zu VendorSpecificInformation Suboptionen

    // 2170
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorsub2170, "PrimaryServerv4Address", 1, null,
                                                           "IpV4Address", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorsub2170, "SecondaryServerv4Address", 2, null,
                                                           "IpV4Address", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // 2171
    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorsub2171, "PrimaryServerv6SelectorID", 1, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorsub2171, "SecondaryServerv6SelectorID", 2,
                                                           null, "OctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorsub2171,
                                                           "ServiceProviderProvisioningServerAddress", 3, null,
                                                           "OctetString", valuearguments, true, "", "", "", "", "", -1,
                                                           -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorsub2171, "ServiceProviderKerberosRealmName", 6,
                                                           null, "OctetString", valuearguments, true, "", "", "", "",
                                                           "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;

    // // Veraltet, hier nochmal rein fuer Stuttgarttest
    // ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, vendorclassid, "Data", 101, null, "OctetString",
    // valuearguments, true, "","","","","",-1,-1,-1,"",""));
    // valuearguments.clear();
    // newid++;

    ModifiedRestoreAdmList.liste
                    .add(new DHCPv6EncodingAdm(newid, vendorid3561, "ACS-URL", 1, null, "ACS", valuearguments, true,
                                               "", "", "", "", "", -1, -1, -1, "", ""));
    valuearguments.clear();
    newid++;


//    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(newid, null, "Testoption1", 200, null, "OctetString",
//                                                           valuearguments, false, "NEW", "", "TestOption1", "", "", -1,
//                                                           -1, -1, "Integer", ""));
//    valuearguments.clear();
//    newid++;


    // End of Data Markierung

    ModifiedRestoreAdmList.liste.add(new DHCPv6EncodingAdm(50000, null, "End-of-Data", 50000, null, "EndOfDataMarker",
                                                           valuearguments, false, "", "", "", "", "", -1, -1, -1, "",
                                                           ""));
    valuearguments.clear();

  }


}
