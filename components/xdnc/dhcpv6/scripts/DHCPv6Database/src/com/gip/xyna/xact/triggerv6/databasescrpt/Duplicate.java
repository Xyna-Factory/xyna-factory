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

package com.gip.xyna.xact.triggerv6.databasescrpt;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class Duplicate {

  
  static List<Node> parents = new ArrayList<Node>();

  static List<DHCPv6EncodingAdm> sortedlist = new ArrayList<DHCPv6EncodingAdm>();
  
  static int counter;


  public static void printNodes(List<Node> list, int level)
  {
    for(Node n:list)
    {
      String space="";
      for(int i=0;i<level;i++)
      {
        space = space+"  ";
      }
      System.out.println(space+n.getIdentity().getTypeName()+" : "+n.getIdentity().getId()+" : "+n.getIdentity().getParentId());
      Duplicate.counter++;
      printNodes(n.getChildren(),level+1);
    }
  }
  
  
  public static void main(String arg[]) {
    Collection<DHCPv6EncodingAdm> list = new ArrayList<DHCPv6EncodingAdm>();

    // Datenbankanbindung aufbauen und Eintraege holen
    LoadAdmList anbindung = new LoadAdmList();
    anbindung.setUp();

    try {
      list = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      System.out.println("Failed to read from database");
      e.printStackTrace();
    }

    if (list.size() == 0) {
      System.out.println("Dataset from database empty");
      throw new IllegalArgumentException();
    }

    // Liste sortieren und auf doppelte IDs pruefen


    sortedlist.clear();
    
    for (int z = 0; z < list.toArray().length; z++) {
      sortedlist.add((DHCPv6EncodingAdm) list.toArray()[z]);
    }

    Collections.sort(sortedlist);

    // End of Data Marker entfernen

  //  sortedlist.remove(sortedlist.size() - 1);
    
    
    int id = -1;
    int counter = -1;
    
    Node iana = null;
    Node iata = null;
    Node iapd = null;
    Node leasequery = null;
    Node clientdata = null;
    Node relay = null;
    
    Node pad = null;
    Node eod = null;
    
    parents.clear();
    
   
    for (DHCPv6EncodingAdm e : sortedlist) {
      counter++; // Aktuelle Listenposition
      if (e.getId() > id) {
        id = e.getId();
      }
      else {
        System.out.println("Fehler: doppelte ID!");
      }

      if (e.getParentId() == null && !e.getTypeName().equals("Pad") && !e.getTypeName().equals("End-of-Data")) {
        Node node = new Node(copyEncoding(e));
        addChildren(node);
        parents.add(node);
      }
      
      if(e.getTypeName().equals("Pad"))
      {
        pad = new Node(copyEncoding(e));
      }
      
      if(e.getTypeName().equals("End-of-Data"))
      {
        eod = new Node(copyEncoding(e));
      }
      
      

      if (e.getTypeName().equals("RelayMessage")) {
        relay = parents.get(parents.size()-1);
      }

      if (e.getTypeName().equals("IA_NA")) {
        iana = parents.get(parents.size()-1);
      }

      if (e.getTypeName().equals("IA_TA")) {
        iata = parents.get(parents.size()-1);
      }

      if (e.getTypeName().equals("IA_PD")) {
        iapd = parents.get(parents.size()-1);

      }

      if (e.getTypeName().equals("LeaseQuery")) {
        leasequery = parents.get(parents.size()-1);

      }

      if (e.getTypeName().equals("ClientData")) {
        clientdata = parents.get(parents.size()-1);

      }


    }
    

    System.out.println("Keine doppelten IDs in Liste gefunden!");

    // Ab hier duplizieren


    addIANA(iana);
    System.out.println("IANA Option mit Unteroptionen+Kinder hinzugefuegt.");
    addIATA(iata);
    System.out.println("IATA Option mit Unteroptionen+Kinder hinzugefuegt.");
    addIAPD(iapd);
    System.out.println("IAPD Option mit Unteroptionen+Kinder hinzugefuegt.");
    addLeaseQuery(leasequery);
    System.out.println("LeaseQuery Option mit Unteroptionen+Kinder hinzugefuegt.");
    addClientData(clientdata);
    System.out.println("ClientData Option mit Unteroptionen+Kinder hinzugefuegt.");
    addRelay(relay);
    System.out.println("Relay Option mit Unteroptionen+Kinder hinzugefuegt.");

    
  
    createIndex();

    parents.add(eod);
    parents.add(0,pad);

    Duplicate.counter=0;
    printNodes(parents, 0);
    
    System.out.println("Anzahl aller Optionen mit Unteroptionen: "+Duplicate.counter);
  
    
    sortedlist.clear();

    
    createResultList(parents);
    

    // Hier alles umschreiben und in normale Liste speichern

    List<DHCPv6Encoding> resultlist = new ArrayList<DHCPv6Encoding>();

    DHCPv6Encoding tmp;

        
    for (DHCPv6EncodingAdm e : sortedlist) {
      tmp = new DHCPv6Encoding(e.getId(), e.getParentId(), e.getTypeName(), e.getTypeEncoding(), e.getEnterpriseNr(), e
                      .getValueDataTypeName(), e.getValueDataTypeArguments());
      resultlist.add(tmp);
    }


    LoadList nonadmindatabase = new LoadList();
    nonadmindatabase.setUp();

    try {
      if (resultlist != null) {
        nonadmindatabase.createListOfDHCPEncodingEntry(resultlist);
      }
      else {
        System.out.println("Konvertierte Liste fuer nichtadmin Datenbank leer!");
      }
    }
    catch (PersistenceLayerException e1) {
      e1.printStackTrace();
    }

    System.out.println("Liste mit duplizierten Eintraegen in optionsv6 gespeichert ...");
    System.out.println("============================================================");

    for (DHCPv6Encoding e : resultlist) {
      System.out.println(e.getId() + " : " + e.getParentId() + " : " + e.getTypeName() + " : " + e.getTypeEncoding());
    }

  }

  private static void createResultList(List<Node> list) {
    for(Node n:list)
    {
      sortedlist.add(n.getIdentity());
      createResultList(n.getChildren());
    }

    
  }


  private static void createChildIndizes(List<Node> children, int parentid)
  {
    for(Node n:children)
    {
      n.getIdentity().setId(Duplicate.counter);
      n.getIdentity().setParentId(parentid);
      Duplicate.counter++;
      createChildIndizes(n.getChildren(),Duplicate.counter-1);
    }
    
  }
  

  private static void createIndex() {
    // Eltern indizes erzeugen
    Duplicate.counter=1; // 1 wegen Pad davor
    
    for(Node n:parents)
    {
      n.getIdentity().setId(Duplicate.counter);
      Duplicate.counter++;
    }
    
    // Kinder Indizes erzeugen
    
    for(Node n:parents)
    {
      int id = n.getIdentity().getId();
      createChildIndizes(n.getChildren(), id);
    }
    
  }


  private static void addChildren(Node node) {
    for(DHCPv6EncodingAdm e:sortedlist)
    {
      if(e.getParentId()!=null)
      {
        if(e.getParentId()==node.getIdentity().getId())
        {
          Node childnode = new Node(e);
          addChildren(childnode);
          node.addChild(childnode);
        }
        
      }
    }
  }
  
  private static DHCPv6EncodingAdm copyEncoding(DHCPv6EncodingAdm input)
  {
    DHCPv6EncodingAdm result= new DHCPv6EncodingAdm(input.getId(),input.getEnterpriseNr(),input.getTypeName(),input.getTypeEncoding(),input.getEnterpriseNr(),input.getValueDataTypeName(),input.getValueDataTypeArguments(),input.getReadOnly(),input.getStatusFlag(),input.getGuiName(),input.getGuiAttribute(),input.getFixedGuiAttribute(),input.getGuiParameter(),input.getGuiAttributeId(),input.getFixedGuiAttributeId(),input.getGuiParameterId(),input.getGuiAttributeWertebereich(),input.getFixedGuiAttributeValue());
    return result;
  }
  
  private static List<Node> copylist(List<Node> list)
  {
    List<Node> result = new ArrayList<Node>();
    for(Node n:list)
    {
      Node newnode = new Node(copyEncoding(n.getIdentity()));
      newnode.setChildren(copylist(n.getChildren()));
      result.add(newnode);
    }
    return result;
  }
  
  private static void addSubnodes(Node parent, List<Long> dontadd)
  {
    List<Node> currentList = copylist(parents); 
    for(Node n:currentList)
    {
      if(!dontadd.contains(n.getIdentity().getTypeEncoding()))
      {
        parent.addChild(n);
      }
    }
    
  }



  private static void addLeaseQuery(Node leasequery) {

    Map<String, String> valuearguments = new HashMap<String, String>();

    valuearguments.put("\"nrBytes\"", "\"1\"");
    DHCPv6EncodingAdm e = new DHCPv6EncodingAdm(0, 1, "QueryType", 100001, null, "NonTLVUnsignedInteger",
                                                   valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    Node suboption = new Node(e);
    leasequery.addChild(suboption);
    

    
    
    e = new DHCPv6EncodingAdm(0, 1, "QueryLinkAddress", 100002, null, "NonTLVOctetString",
                                                   valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    leasequery.addChild(suboption);
    
    e = new DHCPv6EncodingAdm(0, 1, "RequestList", 6, null, "OctetString", valuearguments,
                                                   false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    leasequery.addChild(suboption);
    
    e = new DHCPv6EncodingAdm(0, 1, "IA_Address", 5, null, "IAAddress", valuearguments,
                                                   false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    leasequery.addChild(suboption);
    Node iaadress = suboption;
    
    e = new DHCPv6EncodingAdm(0, 1, "IPv6", 100001, null, "NonTLVIpV6Address", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T1", 100002, null, "NonTLVUnsignedInteger",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T2", 100003, null, "NonTLVUnsignedInteger",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);

    
    List<Long> dontadd = new ArrayList<Long>();

    dontadd.add(new Long(3));
    dontadd.add(new Long(4));
    dontadd.add(new Long(5));
    dontadd.add(new Long(25));
    dontadd.add(new Long(26));
    
    dontadd.add(new Long(44));
    dontadd.add(new Long(45));
    dontadd.add(new Long(48));

    addSubnodes(iaadress, dontadd);
    dontadd.clear();

  }

  
  

  private static void addClientData(Node clientdata) {

    Map<String, String> valuearguments = new HashMap<String, String>();
    
    // Client Last Transaction Time T=46
    valuearguments.put("\"nrBytes\"", "\"4\"");
    DHCPv6EncodingAdm e = new DHCPv6EncodingAdm(0, 1, "CLTTime", 46, null, "UnsignedInteger", valuearguments,
                                                   true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    Node suboption = new Node(e);
    clientdata.addChild(suboption);

    // IAAddress mit Kindern (nur direkte Kinder, nicht alle moeglichen Unteroptionen)

    e = new DHCPv6EncodingAdm(0, 1, "IA_Address", 5, null, "IAAddress", valuearguments,
                                    false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    clientdata.addChild(suboption);
    Node iaadress = suboption;
    
    
    e = new DHCPv6EncodingAdm(0, 1, "IPv6", 100001, null, "NonTLVIpV6Address", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);
    
    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T1", 100002, null, "NonTLVUnsignedInteger",
                                    valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);
    
    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T2", 100003, null, "NonTLVUnsignedInteger",
                                    valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);
    
    // IAPrefix mit Kindern (nur direkte Kinder, nicht alle moeglichen Unteroptionen)
    
    e = new DHCPv6EncodingAdm(0, 1, "IAPrefix", 26, null, "IAPDOption", valuearguments,
                                                   true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    clientdata.addChild(suboption);
    Node iaprefixsub = suboption;    

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T1", 100001, null, "NonTLVUnsignedInteger", valuearguments,
                                    false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaprefixsub.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T2", 100002, null, "NonTLVUnsignedInteger", valuearguments,
                                    false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaprefixsub.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"1\"");
    e = new DHCPv6EncodingAdm(0, 1, "PrefixLength", 100003, null, "NonTLVUnsignedInteger",
                                    valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaprefixsub.addChild(suboption);

    e = new DHCPv6EncodingAdm(0, 1, "IPv6", 100004, null, "NonTLVIpV6Address",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaprefixsub.addChild(suboption);

    
    // ClientID Option T=1
    e = new DHCPv6EncodingAdm(0, 1, "ClientID", 1, null, "DUID", valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    clientdata.addChild(suboption);
    Node clientid = suboption;
    
    // DUID LLT als Kind von ClientID
    e = new DHCPv6EncodingAdm(0, 1, "DUID-LLT", 1, null, "DUIDLLT", valuearguments,true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    clientid.addChild(suboption);
    Node duidllt = suboption;

    // DUID EN als Kind von ClientID
    e = new DHCPv6EncodingAdm(0, 1, "DUID-EN", 2, null, "DUIDEN", valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    clientid.addChild(suboption);
    Node duiden = suboption;


    // DUID LL als Kind von ClientID
    e = new DHCPv6EncodingAdm(0, 1, "DUID-LL", 3, null, "DUIDLL", valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    clientid.addChild(suboption);
    Node duidll = suboption;

    // Kindeskinder von DUID LLTs
    
    valuearguments.put("\"nrBytes\"", "\"2\"");
    e = new DHCPv6EncodingAdm(0, 1, "HardwareType", 1, null, "NonTLVUnsignedInteger",
                                                   valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    duidllt.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "Time", 2, null, "NonTLVUnsignedInteger",
                                                   valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    duidllt.addChild(suboption);

    e = new DHCPv6EncodingAdm(0, 1, "LinkLayerAddress", 3, null, "NonTLVOctetString",
                                                   valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    duidllt.addChild(suboption);
    
    // Kindeskinder von DUID ENs

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "EnterpriseNr", 1, null, "NonTLVUnsignedInteger",
                                                   valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    duiden.addChild(suboption);

    e = new DHCPv6EncodingAdm(0, 1, "Identifier", 2, null, "NonTLVOctetString",
                                                   valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    duiden.addChild(suboption);
    
    // Kindeskinder von DUID LLs

    valuearguments.put("\"nrBytes\"", "\"2\"");
    e = new DHCPv6EncodingAdm(0, 1, "HardwareType", 1, null, "NonTLVUnsignedInteger",
                                                   valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    duidll.addChild(suboption);

    e = new DHCPv6EncodingAdm(0, 1, "LinkLayerAddress", 2, null, "NonTLVOctetString",
                                                   valuearguments, true,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    duidll.addChild(suboption);

  }

  
  
  private static void addIANA(Node iana) {

    Map<String, String> valuearguments = new HashMap<String, String>();

    DHCPv6EncodingAdm e = new DHCPv6EncodingAdm(0, 1, "IAID", 100001, null, "NonTLVOctetString", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    Node suboption = new Node(e);
    iana.addChild(suboption);
    

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T1", 100002, null, "NonTLVUnsignedInteger", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iana.addChild(suboption);


    valuearguments.put("\"nrBytes\"", "\"4\"");
    e= new DHCPv6EncodingAdm(0, 1, "T2", 100003, null, "NonTLVUnsignedInteger", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iana.addChild(suboption);


    List<Long> dontadd = new ArrayList<Long>();


    dontadd.add(new Long(3));
    dontadd.add(new Long(4));
    dontadd.add(new Long(25));
    dontadd.add(new Long(26));
    
    dontadd.add(new Long(44));
    dontadd.add(new Long(45));
    dontadd.add(new Long(48));


    addSubnodes(iana, dontadd);
    dontadd.clear();

    Node iaadress = null;
    for(Node sn:iana.getChildren())
    {
      if(sn.getIdentity().getTypeName().equals("IA_Address"))
      {
        iaadress = sn;
      }
    }
    if(iaadress==null)
    {
      throw new RuntimeException("iaadress not found!");
    }
    
    e = new DHCPv6EncodingAdm(0, 1, "IPv6", 100001, null, "NonTLVIpV6Address", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e= new DHCPv6EncodingAdm(0, 1, "T1", 100002, null, "NonTLVUnsignedInteger",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);


    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T2", 100003, null, "NonTLVUnsignedInteger",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);

    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Long(3));
    dontadd.add(new Long(4));
    dontadd.add(new Long(5));
    dontadd.add(new Long(25));
    dontadd.add(new Long(26));
    
    dontadd.add(new Long(44));
    dontadd.add(new Long(45));
    dontadd.add(new Long(48));


    addSubnodes(iaadress, dontadd);
    dontadd.clear();
  }


  private static void addIATA(Node iata) {

    Map<String, String> valuearguments = new HashMap<String, String>();

    DHCPv6EncodingAdm e = new DHCPv6EncodingAdm(0, 1, "IAID", 100001, null, "NonTLVOctetString", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    Node suboption = new Node(e);
    iata.addChild(suboption);
    



    List<Long> dontadd = new ArrayList<Long>();


    dontadd.add(new Long(3));
    dontadd.add(new Long(4));
    dontadd.add(new Long(25));
    dontadd.add(new Long(26));
    
    dontadd.add(new Long(44));
    dontadd.add(new Long(45));
    dontadd.add(new Long(48));


    addSubnodes(iata, dontadd);
    dontadd.clear();

    Node iaadress = null;
    for(Node sn:iata.getChildren())
    {
      if(sn.getIdentity().getTypeName().equals("IA_Address"))
      {
        iaadress = sn;
      }
    }
    if(iaadress==null)
    {
      throw new RuntimeException("iaadress not found!");
    }
    
    e = new DHCPv6EncodingAdm(0, 1, "IPv6", 100001, null, "NonTLVIpV6Address", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e= new DHCPv6EncodingAdm(0, 1, "T1", 100002, null, "NonTLVUnsignedInteger",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);


    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T2", 100003, null, "NonTLVUnsignedInteger",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iaadress.addChild(suboption);

    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Long(3));
    dontadd.add(new Long(4));
    dontadd.add(new Long(5));
    dontadd.add(new Long(25));
    dontadd.add(new Long(26));
    
    dontadd.add(new Long(44));
    dontadd.add(new Long(45));
    dontadd.add(new Long(48));


    addSubnodes(iaadress, dontadd);
    dontadd.clear();
  }

  
  private static void addIAPD(Node iapd) {

    Map<String, String> valuearguments = new HashMap<String, String>();

    DHCPv6EncodingAdm e = new DHCPv6EncodingAdm(0, 1, "IAID", 100001, null, "NonTLVOctetString", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    Node suboption = new Node(e);
    iapd.addChild(suboption);
    

    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T1", 100002, null, "NonTLVUnsignedInteger", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iapd.addChild(suboption);


    valuearguments.put("\"nrBytes\"", "\"4\"");
    e= new DHCPv6EncodingAdm(0, 1, "T2", 100003, null, "NonTLVUnsignedInteger", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iapd.addChild(suboption);


    List<Long> dontadd = new ArrayList<Long>();


    dontadd.add(new Long(3));
    dontadd.add(new Long(4));
    dontadd.add(new Long(5));
    dontadd.add(new Long(25));
    
    dontadd.add(new Long(44));
    dontadd.add(new Long(45));
    dontadd.add(new Long(48));


    addSubnodes(iapd, dontadd);
    dontadd.clear();

    Node iapdoption = null;
    for(Node sn:iapd.getChildren())
    {
      if(sn.getIdentity().getTypeName().equals("IAPrefix"))
      {
        iapdoption = sn;
      }
    }
    if(iapdoption==null)
    {
      throw new RuntimeException("iapdoption not found!");
    }
    
    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T1", 100001, null, "NonTLVUnsignedInteger", valuearguments,
                                    false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iapdoption.addChild(suboption);



    valuearguments.put("\"nrBytes\"", "\"4\"");
    e = new DHCPv6EncodingAdm(0, 1, "T2", 100002, null, "NonTLVUnsignedInteger", valuearguments,
                                    false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iapdoption.addChild(suboption);

    valuearguments.put("\"nrBytes\"", "\"1\"");
    e = new DHCPv6EncodingAdm(0, 1, "PrefixLength", 100003, null, "NonTLVUnsignedInteger",
                                    valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iapdoption.addChild(suboption);


    e= new DHCPv6EncodingAdm(0, 1, "IPv6", 100004, null, "NonTLVIpV6Address",
                                               valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    iapdoption.addChild(suboption);


    // ab hier "Skript" um alle Kinder zu ergaenzen

    dontadd.add(new Long(3));
    dontadd.add(new Long(4));
    dontadd.add(new Long(5));
    dontadd.add(new Long(25));
    dontadd.add(new Long(26));
    
    dontadd.add(new Long(44));
    dontadd.add(new Long(45));
    dontadd.add(new Long(48));


    addSubnodes(iapdoption, dontadd);
    dontadd.clear();
  }

  
  
  private static void addRelay(Node relay) {

    Map<String, String> valuearguments = new HashMap<String, String>();

    valuearguments.put("\"nrBytes\"", "\"1\"");
    DHCPv6EncodingAdm e = new DHCPv6EncodingAdm(0, 1, "InnerType", 100001, null, "NonTLVUnsignedInteger", valuearguments,
                                    false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    Node suboption = new Node(e);
    relay.addChild(suboption);

    e = new DHCPv6EncodingAdm(0, 1, "TXID", 100002, null, "NonTLVOctetString", valuearguments, false,"","","","","",-1,-1,-1,"","");
    valuearguments.clear();
    suboption = new Node(e);
    relay.addChild(suboption);

    
    
    List<Long> dontadd = new ArrayList<Long>();


    addSubnodes(relay, dontadd);
    dontadd.clear();
    
  }
  
  


}
