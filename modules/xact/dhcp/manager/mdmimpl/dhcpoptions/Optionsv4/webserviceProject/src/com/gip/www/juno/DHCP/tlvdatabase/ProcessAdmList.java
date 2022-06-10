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
package com.gip.www.juno.DHCP.tlvdatabase;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl;
import com.gip.www.juno.DHCP.tlvdatabase.LoadGuiLists;
import com.gip.xyna.xdnc.dhcp.db.storables.Condition;
import com.gip.xyna.xdnc.dhcp.db.storables.DeviceClass;
import com.gip.xyna.xdnc.dhcp.db.storables.GuiAttribute;
import com.gip.xyna.xdnc.dhcp.db.storables.GuiFixedAttribute;
import com.gip.xyna.xdnc.dhcp.db.storables.GuiParameter;
import com.gip.xyna.xdnc.dhcp.db.storables.PoolType;


public class ProcessAdmList {
  
  private Collection<GuiAttribute> guiattributelist;
  private Collection<GuiFixedAttribute> guifixedattributelist;
  private Collection<GuiParameter> guiparameterlist;

  private Collection<DeviceClass> deviceclasslist;
  private Collection<Condition> conditionlist;
  private Collection<PoolType> pooltypelist;

  private List<Integer> guiattributeids = new ArrayList<Integer>();
  private List<Integer> guifixedattributeids = new ArrayList<Integer>();
  private List<Integer> guiparameterids = new ArrayList<Integer>();

  private List<DHCPEncoding> sortedlist = new ArrayList<DHCPEncoding>();
  private List<DHCPEncoding> resultlist = new ArrayList<DHCPEncoding>();

  static Logger logger= Logger.getLogger(Optionsv4BindingImpl.class);
  

  private static String valueRange ="";
  
  public static final String WS_PROPERTY_VALUERANGE = "xdnc.dhcp.config.guifixedattribute.valuerange";
  
  public void process() throws Exception {

    Properties wsProperties;
    try {
      wsProperties = PropertiesHandler.getWsProperties();
      ProcessAdmList.valueRange = PropertiesHandler.getProperty(wsProperties, WS_PROPERTY_VALUERANGE, logger);
    }
    catch (RemoteException e1) {
      //logger.info("(Optionsv4) Problems while reading WS Property valuerange");
//      throw new Exception("(Optionsv4) Problems while reading WS Property valuerange",e1);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("W").setErrorNumber("00101").setDescription("xdnc.dhcp.config.guifixedattribute.valuerange kann nicht ausgelesen werden. Webservice Properties gesetzt?"));
    }


    loadData();//laed alle Daten aus guiattribut usw. -Tabellen

    List<DHCPEncoding> entriesWithStatus = new ArrayList<DHCPEncoding>();

    for (DHCPEncoding e : sortedlist) {
      if (e.getStatusFlag().length() > 0) {
        if (e.getReadOnly()) {
          logger.info("(deployTLV) Readonly Entries may not have a StatusFlag! (Entry with ID " + e.getId() + ")");
          throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Readonly Entries may not have a StatusFlag! (Entry with ID " + e.getId() + ")"));    
        }


        if (!e.getStatusFlag().equals("NEW") && !e.getStatusFlag().equals("MOD") && !e.getStatusFlag().equals("DEL")) {
          logger.info("(deployTLV) Invalid Statusflag (Entry with ID " + e.getId() + ")");
          throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Invalid Statusflag (Entry with ID " + e.getId() + ")"));    
        }
        else {
          entriesWithStatus.add(e);
        }
      }
    }


    boolean checkflag = checkEntries(entriesWithStatus);
    if (checkflag == true) {
      createEntries();
      logger.info("(deployTLV) committing changes ...");
      commit();
    }
    else
    {
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) One or more entries invalid!"));    
      
    }


    // logger.info("Test GenerateDHCPConf: ");
    // for(int i=0;i<70;i++)
    // {
    // logger.info(generateDhcpConf(i));
    // logger.info(getOptionEncoding(i));
    //
    // }


  }
  
  private void commit() throws Exception {
    LoadGuiLists guianbindung = new LoadGuiLists();
    guianbindung.setUp();

    try {
      guianbindung.createListOfGuiAttributeEntries(guiattributelist);
      guianbindung.createListOfGuiFixedAttributeEntries(guifixedattributelist);
      guianbindung.createListOfGuiParameterEntries(guiparameterlist);

    }
    catch (Exception e) {
      logger.error("(DeployTLV) Failed to write to database");
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00203").setDescription("Fehler beim Schreiben von guiattribute, guifixedattribute oder guiparameter."),e);    
    }

    LoadAdmList anbindung = new LoadAdmList();
    anbindung.setUp();

    try {
      if(resultlist.size()>0)anbindung.createListOfDHCPEncodingEntry(resultlist);
    }
    catch (Exception e) {
      //logger.info("(DeployTLV) Failed to write to database");
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00203").setDescription("Fehler beim Schreiben von optionsv6adm."),e);    

    }

//    DuplicateTLV dup = new DuplicateTLV();
//    dup.duplicate();
    logger.info("(deployTLV) Finished.");

  }
  
  private void loadData() throws Exception {
    LoadGuiLists guianbindung = new LoadGuiLists();
    guianbindung.setUp();

    try {
      guiattributelist = guianbindung.loadGuiAttribute();
      guifixedattributelist = guianbindung.loadGuiFixedAttribute();
      guiparameterlist = guianbindung.loadGuiParameter();
      deviceclasslist = guianbindung.loadDeviceClass();
      conditionlist = guianbindung.loadCondition();
      pooltypelist = guianbindung.loadPoolType();

    }
    catch (Exception e) {
      logger.error("(deployTLV) Failed to read from database.",e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00203").setDescription("Zugriff auf dhcpv6 Datenbank fehlgeschlagen."),e);    

    }


    for (GuiAttribute g : guiattributelist) {
      guiattributeids.add(g.getGuiAttributeID());
    }


    for (GuiFixedAttribute f : guifixedattributelist) {
      guifixedattributeids.add(f.getGuiFixedAttributeID());
    }


    for (GuiParameter p : guiparameterlist) {
      guiparameterids.add(p.getGuiParameterID());

    }

    Collection<DHCPEncoding> list = new ArrayList<DHCPEncoding>();

    // Datenbankanbindung aufbauen und Eintraege holen
    LoadAdmList anbindung = new LoadAdmList();
    anbindung.setUp();

    try {
      list = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      logger.error("(deployTLV) Failed to read from database.",e);
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00203").setDescription("Zugriff auf xynadhcp.optionsv4 fehlgeschlagen."),e);    
    }

    if (list.size() == 0) {
      logger.error("(deployTLV) Dataset from database empty");
      throw new DPPWebserviceException(new MessageBuilder().setDomain("D").setErrorNumber("00203").setDescription("xynadhcp.optionsv4 Tabelle leer."));    
    }

    // Liste sortieren und auf doppelte IDs pruefen


    for (int z = 0; z < list.toArray().length; z++) {
      sortedlist.add((DHCPEncoding) list.toArray()[z]);
    }

    Collections.sort(sortedlist);

    int id = -1;
    for (DHCPEncoding e : sortedlist) {
      if (e.getId() > id) {
        id = e.getId();
      }
      else {
        logger.error("(deployTLV) Fehler: doppelte ID!");
        throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("Eintrag mit doppelter ID in optionsv4 enthalten."));    
      }
    }
  }

  private boolean checkEntries(List<DHCPEncoding> list) throws Exception {

    logger.info("(deployTLV) Starting check of entries with Statusflag ...");
    boolean failure = false;
    int guiattid = -1;
    int guifixid = -1;
    int guiparid = -1;

    // Eintraege holen mit Statusflag != leer


    for (DHCPEncoding e : list) {

      // logger.info(e.getId()+"\t:\t"+e.getTypeName());

      guiattid = -1;
      guifixid = -1;
      guiparid = -1;


      if (e.getGuiAttribute().length() > 0) {
        if (e.getFixedGuiAttribute().length() > 0 || e.getFixedGuiAttributeValue().length() > 0) {
          logger.info("(deployTLV) Setting GuiAttributes AND FixedGuiAttributes not valid! (Entry with ID " + e
                          .getId() + ")");
          failure = true;
          throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Setting GuiAttributes AND FixedGuiAttributes not valid! (Entry with ID " + e
                                                                                                                      .getId() + ")"));    
          
        }
        else // Syntaxcheck
        {
          if (e.getGuiAttributeWertebereich().length() == 0) {
            logger.info("(deployTLV) GuiAttribute arguments not set! (Entry with ID " + e.getId() + ")");
            failure = true;
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiAttribute arguments not set! (Entry with ID " + e.getId() + ")"));    

          }

          if(e.getStatusFlag().equals("NEW"))
          {
            if(e.getGuiAttributeId()>0)
            {
              logger.info("(deployTLV) GuiAttributeID should be set to -1 creating new Entries! (Entry with ID " + e
                          .getId() + ")");
              failure = true;
              throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiAttributeID should be set to -1 creating new Entries! (Entry with ID " + e
                                                                                                                          .getId() + ")"));    

            }
          }
          else // MOD or DEL
          {
//            if(e.getGuiAttributeId()<0)
//            {
//              logger.info("(deployTLV) GuiAttributeID must be set! (Entry with ID " + e
//                          .getId() + ")");
//              failure = true;
//              throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiAttributeID must be set! (Entry with ID " + e
//                                                                                                                          .getId() + ")"));    
//
//              
//            }
//            else
//            {
              guiattid = e.getFixedGuiAttributeId();
//            }
          }
        }
      }
      else if (e.getFixedGuiAttribute().length() > 0) {
        if (e.getGuiAttribute().length() > 0 || e.getGuiAttributeWertebereich().length() > 0) {
          logger.info("(deployTLV) Setting GuiAttributes AND FixedGuiAttributes not valid (Entry with ID " + e
                          .getId() + ")");
          failure = true;
          throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Setting GuiAttributes AND FixedGuiAttributes not valid (Entry with ID " + e
                                                                                                                      .getId() + ")"));    

          
        }
        else // Syntaxcheck
        {
          if (e.getFixedGuiAttributeValue().length() == 0) {
            logger.info("(deployTLV) GuiFixedAttribute arguments not set! (Entry with ID " + e.getId() + ")");
            failure = true;
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiFixedAttribute arguments not set! (Entry with ID " + e.getId() + ")"));    

          }

          boolean found = false; // Liegt value in ValueRange?
          for (String s : ProcessAdmList.valueRange.split(",")) {
            if (s.equals(e.getFixedGuiAttributeValue()))
            {
              found = true;
            }
          }
          if (!found) {
            logger.info("(deployTLV) Given value has to match valueRange: "+valueRange+" at Entry with ID " + e
                                            .getId());
            failure = true;

            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Given value has to match valueRange: "+valueRange+" at Entry with ID " + e
                                .getId()));    

          }

          if (e.getStatusFlag().equals("NEW")) {
            if(e.getFixedGuiAttributeId()>0)
            {
              logger.info("(deployTLV) GuiFixedAttributeID should be set to -1 creating new Entries! (Entry with ID " + e
                          .getId() + ")");
              failure = true;
              throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiFixedAttributeID should be set to -1 creating new Entries! (Entry with ID " + e
                                                                                                                          .getId() + ")"));    

            }
          }            
          else // MOD or DEL
          {
//            if(e.getFixedGuiAttributeId()<0)
//            {
//              logger.info("(deployTLV) GuiFixedAttributeID must be set! (Entry with ID " + e
//                          .getId() + ")");
//              failure = true;
//              throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiFixedAttributeID must be set! (Entry with ID " + e
//                                                                                                                          .getId() + ")"));    
//
//            }
//            else
//            {
              guifixid = e.getFixedGuiAttributeId();
//            }
            
          }
        }
      }

      if (e.getGuiParameter().length() > 0) {

        if(e.getStatusFlag().equals("NEW"))
        {
          if(e.getGuiParameterId()>0)
          {
            logger.info("(deployTLV) GuiParameterID should be set to -1 creating new Entries! (Entry with ID " + e
                        .getId() + ")");
            failure = true;
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiParameterID should be set to -1 creating new Entries! (Entry with ID " + e
                                                                                                                        .getId() + ")"));    

            
          }
          
        }
        else // MOD or DEL
        {
//          if(e.getGuiParameterId()<0)
//          {
//            logger.info("(deployTLV) GuiParameterID must be set! (Entry with ID " + e
//                        .getId() + ")");
//            failure = true;
//            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) GuiParameterID must be set! (Entry with ID " + e
//                                                                                                                        .getId() + ")"));    
//
//          }
//          else
//          {
            guiparid = e.getGuiParameterId();
//          }
          
        }

      }

      // Pruefen ob IDs zum Aendern oder Loeschen auch vorhanden ...
      if (!e.getStatusFlag().equals("NEW")) {
        if (guiattid != -1) {
          if (!guiattributeids.contains(guiattid)) {
            logger.info("(deployTLV) ID of guiattribute entry to modify or delete does not exist! (Entry with ID " + e
                                            .getId() + ")");
            failure = true;
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) ID of guiattribute entry to modify or delete does not exist! (Entry with ID " + e
                                                                                                                        .getId() + ")"));    

          }
        }

        if (guifixid != -1) {
          if (!guifixedattributeids.contains(guifixid)) {
            logger.info("(deployTLV) ID of guifixedattribute entry to modify or delete does not exist! (Entry with ID " + e
                                            .getId() + ")");
            failure = true;
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) ID of guifixedattribute entry to modify or delete does not exist! (Entry with ID " + e
                                                                                                                        .getId() + ")"));    

          }
        }

        if (guiparid != -1) {
          if (!guiparameterids.contains(guiparid)) {
            logger.info("(deployTLV) ID of guiparameter entry to modify or delete does not exist! (Entry with ID " + e
                                            .getId() + ")");
            failure = true;
            throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) ID of guiparameter entry to modify or delete does not exist! (Entry with ID " + e
                                                                                                                        .getId() + ")"));    
          }
        }

        if (e.getStatusFlag().equals("DEL")) // Abhaengigkeiten pruefen
        {
          for (DeviceClass d : deviceclasslist) {
            if(d.getAttributes()!=null)
            {
              if (d.getAttributes().contains("" + guiattid + "=")) {
                logger.info("(deployTLV) Dependencies in class table: guiattributeid " + guiattid + " in entry with ID " + d
                                                .getClassID() + ". " + "Remove first before deleting Entry (ID " + e
                                                .getId() + ") with GuiAttribute.");
                failure = true;
                throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Dependencies in class table: guiattributeid " + guiattid + " in entry with ID " + d
                                                                                                                            .getClassID() + ". " + "Remove first before deleting Entry (ID " + e
                                                                                                                            .getId() + ") with GuiAttribute."));    

              }
            }
            if(d.getFixedAttributes()!=null)
            {
              String[] fixedAttributes = d.getFixedAttributes().split(",");
              for(String s:fixedAttributes)
              {
                if(Integer.parseInt(s.replace(":",""))==guifixid)
                {
                  logger.info("(deployTLV) Dependencies in class table: guifixedattributeid " + guifixid + " in entry with ID " + d
                              .getClassID() + ". " + "Remove first before deleting Entry (ID " + e
                              .getId() + ") with GuiFixedAttribute.");
                  failure = true;
                  throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Dependencies in class table: guifixedattributeid " + guifixid + " in entry with ID " + d
                                                                                                                              .getClassID() + ". " + "Remove first before deleting Entry (ID " + e
                                                                                                                              .getId() + ") with GuiFixedAttribute."));    

                }
              }
            }
          }
          for (Condition c : conditionlist) {
            if(c.getParameter()!=null)
            {
              if (c.getParameter().contains(String.valueOf(guiparid))) {
                logger.info("(deployTLV) Dependencies in classcondition table: guiparameterid " + guiparid + " in entry with ID " + c
                                                .getConditionID() + ". " + "Remove first before deleting Entry (ID " + e
                                                .getId() + ") with GuiParameter.");
                failure = true;
                throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Dependencies in classcondition table: guiparameterid " + guiparid + " in entry with ID " + c
                                                                                                                            .getConditionID() + ". " + "Remove first before deleting Entry (ID " + e
                                                                                                                            .getId() + ") with GuiParameter."));    


              }
            }
          }
          for (PoolType p : pooltypelist) {
            if(p.getAttributes()!=null)
            {
              if (p.getAttributes().contains("" + guiattid + "=")) {
                logger.info("(deployTLV) Dependencies in pooltype table: guiattributeid " + guiattid + " in entry with ID " + p
                                                .getPoolTypeID() + ". " + "Remove first before deleting Entry (ID " + e
                                                .getId() + ") with GuiAttribute.");
                failure = true;
                throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Dependencies in pooltype table: guiattributeid " + guiattid + " in entry with ID " + p
                                                                                                                            .getPoolTypeID() + ". " + "Remove first before deleting Entry (ID " + e
                                                                                                                            .getId() + ") with GuiAttribute."));    

              }
            }
            if(p.getFixedAttributes()!=null)
            {
              String[] fixedAttributes = p.getFixedAttributes().split(",");
              for(String s:fixedAttributes)
              {
                if(Integer.parseInt(s.replace(":",""))==guifixid)
                {
                  logger.info("(deployTLV) Dependencies in pooltype table: guifixedattributeid " + guifixid + " in entry with ID " + p.getPoolTypeID() + ". " + "Remove first before deleting Entry (ID " + e
                              .getId() + ") with GuiFixedAttribute.");
                  failure = true;
                  throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) Dependencies in pooltype table: guifixedattributeid " + guifixid + " in entry with ID " + p.getPoolTypeID() + ". " + "Remove first before deleting Entry (ID " + e
                                                                                                                              .getId() + ") with GuiFixedAttribute."));    

                  
                }
              }
            }
          }


        }

      }
    }
    if (failure) {
      logger.info("(deployTLV) One or more entries invalid ... check failed");
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00217").setDescription("(deployTLV) One or more entries invalid ... check failed"));    

    }
    logger.info("(deployTLV) Check successful. Valid entries.");
    return true;

  }
  
  private void createEntries() {

    logger.info("(deployTLV) creating Entries ...");

    for (DHCPEncoding e : sortedlist) {

      // Neue Eintraege
      if (e.getStatusFlag().equals("NEW")) {
        String newGuiAttribute = e.getGuiAttribute();
        String newGuiFixedAttribute = e.getFixedGuiAttribute();
        String newGuiParameter = e.getGuiParameter();
        int newGuiAttributeid = -1;
        int newGuiFixedAttributeid = -1;
        int newGuiParameterid = -1;
        String newGuiAttributeWertebereich = "";
        String newGuiFixedAttributeValue = "";

        if (e.getGuiAttribute().length() > 0) {

          int guiattid;
          String name;
          String werteBereich;

          guiattid = Collections.max(guiattributeids) + 1;
          guiattributeids.add(guiattid);
          name = e.getGuiAttribute();
          werteBereich = e.getGuiAttributeWertebereich();

          GuiAttribute neuerEintrag = createGuiAttribute(guiattid, name, werteBereich, e.getId());

          logger.info("(deployTLV) New GuiAttribute: " + neuerEintrag.getGuiAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getXdhcpConf() + " ; " + neuerEintrag.getWerteBereich());

          guiattributelist.add(neuerEintrag);
          newGuiAttribute = neuerEintrag.getName();
          newGuiAttributeid = neuerEintrag.getGuiAttributeID();
          newGuiAttributeWertebereich = neuerEintrag.getWerteBereich();

        }
        if (e.getFixedGuiAttribute().length() > 0) {

          int guifixid;
          String name;
          String value;
          String valueRange;

          guifixid = Collections.max(guifixedattributeids) + 1;
          guifixedattributeids.add(guifixid);
          name = e.getFixedGuiAttribute();
          value = e.getFixedGuiAttributeValue();
          valueRange = ProcessAdmList.valueRange;


          GuiFixedAttribute neuerEintrag = createGuiFixedAttribute(guifixid, name, value, valueRange, e.getId());

          logger.info("(deployTLV) New GuiFixedAttribute: " + neuerEintrag.getGuiFixedAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getXdhcpConf() + " ; " + neuerEintrag.getValue() + " ; " + neuerEintrag
                                          .getValueRange());

          guifixedattributelist.add(neuerEintrag);
          newGuiFixedAttribute = neuerEintrag.getName();
          newGuiFixedAttributeid = neuerEintrag.getGuiFixedAttributeID();
          newGuiFixedAttributeValue = neuerEintrag.getValue();
        }

        if (e.getGuiParameter().length() > 0) {

          int guiparid;
          String name;

          guiparid = Collections.max(guiparameterids) + 1;
          guiparameterids.add(guiparid);
          name = e.getGuiParameter();

          GuiParameter neuerEintrag = createGuiParameter(guiparid, name, e.getId());

          logger.info("(deployTLV) New GuiParameter: " + neuerEintrag.getGuiParameterID() + " ; " + neuerEintrag
                          .getName() + " ; " + neuerEintrag.getDhcpConf() + " ; " + neuerEintrag.getXdhcpConf());

          guiparameterlist.add(neuerEintrag);
          newGuiParameter = neuerEintrag.getName();
          newGuiParameterid = neuerEintrag.getGuiParameterID();

        }

        DHCPEncoding copy = new DHCPEncoding(e.getId(), e.getParentId(), e.getTypeName(),
                                                       e.getTypeEncoding(), e.getEnterpriseNr(), e
                                                                       .getValueDataTypeName(), e
                                                                       .getValueDataTypeArguments(), e.getReadOnly(),
                                                       "", e.getGuiName(), newGuiAttribute, newGuiFixedAttribute,
                                                       newGuiParameter, newGuiAttributeid, newGuiFixedAttributeid, 
                                                       newGuiParameterid,newGuiAttributeWertebereich,newGuiFixedAttributeValue);
        resultlist.add(copy); // Statusflag entfernen

      }
      else if (e.getStatusFlag().equals("MOD")) {
        int newGuiAttributeId = e.getGuiAttributeId();
        int newGuiFixedAttributeId = e.getFixedGuiAttributeId();
        int newGuiParameterId = e.getGuiParameterId();

        if (e.getGuiAttribute().length() > 0) {

          int guiattid;
          String name;
          String werteBereich;

          guiattid = e.getGuiAttributeId();
          if(guiattid<0)
          {
            guiattid = Collections.max(guiattributeids) + 1;
            guiattributeids.add(guiattid);
          }
          newGuiAttributeId=guiattid;
          name = e.getGuiAttribute();
          werteBereich = e.getGuiAttributeWertebereich();

          GuiAttribute neuerEintrag = createGuiAttribute(guiattid, name, werteBereich, e.getId());

          logger.info("(deployTLV) Modified GuiAttribute: " + neuerEintrag.getGuiAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getXdhcpConf() + " ; " + neuerEintrag.getWerteBereich());

          GuiAttribute old = null;
          // alten Eintrag entfernen
          for (GuiAttribute g : guiattributelist) {
            if (g.getGuiAttributeID() == guiattid) {
              old = g;
            }
          }

          if (old != null)
            guiattributelist.remove(old);

          guiattributelist.add(neuerEintrag);
          

        }
        else // GuiAttribute Eintrag vielleicht entfernt
        {
          int guiattid = e.getGuiAttributeId();
          if(guiattid>0)
          {
            GuiAttribute old = null;
            // alten Eintrag entfernen
            for (GuiAttribute g : guiattributelist) {
              if (g.getGuiAttributeID() == guiattid) {
                old = g;
              }
            }
            
            newGuiAttributeId = -1;

            if (old != null)
              guiattributelist.remove(old);
          }
        }

        
        if (e.getFixedGuiAttribute().length() > 0) {

          int guifixid;
          String name;
          String value;
          String valueRange;

          guifixid = e.getFixedGuiAttributeId();
          if(guifixid<0)
          {
            guifixid = Collections.max(guifixedattributeids) + 1;
            guifixedattributeids.add(guifixid);
          }
          newGuiFixedAttributeId=guifixid;
          name = e.getFixedGuiAttribute();
          value = e.getFixedGuiAttributeValue();
          valueRange = ProcessAdmList.valueRange;


          GuiFixedAttribute neuerEintrag = createGuiFixedAttribute(guifixid, name, value, valueRange, e.getId());

          logger.info("(deployTLV) Modified GuiFixedAttribute: " + neuerEintrag.getGuiFixedAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getXdhcpConf() + " ; " + " ; " + neuerEintrag.getValue() + " ; " + neuerEintrag
                                          .getValueRange());


          GuiFixedAttribute old = null;
          // alten Eintrag entfernen
          for (GuiFixedAttribute g : guifixedattributelist) {
            if (g.getGuiFixedAttributeID() == guifixid) {
              old = g;
            }
          }

          if (old != null)
            guifixedattributelist.remove(old);

          guifixedattributelist.add(neuerEintrag);

        }
        else // GuiFixedAttribute Eintrag vielleicht entfernt
        {
          int guifixid = e.getFixedGuiAttributeId();
          if(guifixid>0)
          {
            GuiFixedAttribute old = null;
            // alten Eintrag entfernen
            for (GuiFixedAttribute g : guifixedattributelist) {
              if (g.getGuiFixedAttributeID() == guifixid) {
                old = g;
              }
            }
            newGuiFixedAttributeId = -1; // Id zuruecksetzen
            if (old != null)
              guifixedattributelist.remove(old);
          }
        }

        if (e.getGuiParameter().length() > 0) {

          int guiparid;
          String name;

          guiparid = e.getGuiParameterId();
          if(guiparid<0)
          {
            guiparid = Collections.max(guiparameterids) + 1;
            guiparameterids.add(guiparid);
          }
          newGuiParameterId=guiparid;
          name = e.getGuiParameter();

          GuiParameter neuerEintrag = createGuiParameter(guiparid, name, e.getId());

          logger.info("(deployTLV) Modified GuiParameter: " + neuerEintrag.getGuiParameterID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getDhcpConf() + " ; " + neuerEintrag.getXdhcpConf());


          GuiParameter old = null;
          // alten Eintrag entfernen
          for (GuiParameter g : guiparameterlist) {
            if (g.getGuiParameterID() == guiparid) {
              old = g;
            }
          }

          if (old != null)
            guiparameterlist.remove(old);

          guiparameterlist.add(neuerEintrag);

        }
        else // GuiParameter Eintrag vielleicht entfernt
        {
          int guiparid = e.getGuiParameterId();
          if(guiparid>0)
          {
            GuiParameter old = null;
            // alten Eintrag entfernen
            for (GuiParameter g : guiparameterlist) {
              if (g.getGuiParameterID() == guiparid) {
                old = g;
              }
            }
            
            newGuiParameterId = -1;

            if (old != null)
              guiparameterlist.remove(old);

          }
          
        }


        DHCPEncoding copy = new DHCPEncoding(e.getId(), e.getParentId(), e.getTypeName(),
                                                       e.getTypeEncoding(), e.getEnterpriseNr(), e
                                                                       .getValueDataTypeName(), e
                                                                       .getValueDataTypeArguments(), e.getReadOnly(),
                                                       "", e.getGuiName(), e.getGuiAttribute(), e
                                                                       .getFixedGuiAttribute(), e.getGuiParameter(),
                                                                       newGuiAttributeId,newGuiFixedAttributeId,newGuiParameterId,
                                                                       e.getGuiAttributeWertebereich(),e.getFixedGuiAttributeValue());
        resultlist.add(copy); // Statusflag entfernen

      }
      else if (e.getStatusFlag().equals("DEL")) {
        if (e.getGuiAttributeId()>0) {


          int guiattid;

          guiattid = e.getGuiAttributeId();

          GuiAttribute old = null;
          // alten Eintrag entfernen
          for (GuiAttribute g : guiattributelist) {
            if (g.getGuiAttributeID() == guiattid) {
              old = g;
            }
          }
          if(old != null)logger.info("(deployTLV) Delete GuiAttribute: " + old.getGuiAttributeID() + " ; " + old
                                          .getName() + " ; " + old.getOptionEncoding() + " ; " + old.getDhcpConf() + " ; " + old.getXdhcpConf() + " ; " + old
                                          .getWerteBereich());

          boolean success = false;
          if (old != null)
            success = guiattributelist.remove(old);

          if (!success) {
            logger.info("(deployTLV) Could not delete GuiAttribute. Not found. Entry with ID " + e.getId());
          }

        }
        if (e.getFixedGuiAttributeId() > 0) {

          int guifixid;

          guifixid = e.getFixedGuiAttributeId();

          GuiFixedAttribute old = null;
          // alten Eintrag entfernen
          for (GuiFixedAttribute g : guifixedattributelist) {
            if (g.getGuiFixedAttributeID() == guifixid) {
              old = g;
            }
          }

          if(old!=null)logger.info("(deployTLV) Delete GuiFixedAttribute: " + old.getGuiFixedAttributeID() + " ; " + old
                          .getName() + " ; " + old.getOptionEncoding() + " ; " + old.getDhcpConf() + " ; " + old.getXdhcpConf() + " ; " + old
                          .getValue() + " ; " + old.getValueRange());

          boolean success = false;
          if (old != null)
            success = guifixedattributelist.remove(old);

          if (!success) {
            logger.info("(deployTLV) Could not delete GuiFixedAttribute. Not found. Entry with ID " + e.getId());
          }


        }

        if (e.getGuiParameterId() > 0) {

          int guiparid;

          guiparid = e.getGuiParameterId();

          GuiParameter old = null;
          // alten Eintrag entfernen
          for (GuiParameter g : guiparameterlist) {
            if (g.getGuiParameterID() == guiparid) {
              old = g;
            }
          }

          if(old!=null)logger.info("(deployTLV) Delete GuiParameter: " + old.getGuiParameterID() + " ; " + old
                                          .getName() + " ; " + old.getDhcpConf() + " ; " + old.getXdhcpConf());

          boolean success = false;
          if (old != null)
            success = guiparameterlist.remove(old);

          if (!success) {
            logger.info("(deployTLV) Could not delete GuiParameter. Not found. Entry with ID " + e.getId());
          }


        }

      }
      else {
        resultlist.add(e);
      }


    }

  }
  
  private String generateXdhcpConf(int id) {
    DHCPEncoding current = null;
    for(DHCPEncoding e:sortedlist)
    {
      if(e.getId()==id) current = e;
    }
    if (current == null)
      return "";
    List<DHCPEncoding> parenttree = new ArrayList<DHCPEncoding>();


    addParent(current, parenttree);

    StringBuilder sb = new StringBuilder();

    sb.append("option ");

    for (DHCPEncoding e : parenttree) {
      sb.append(e.getTypeName() + ".");
    }
    sb.deleteCharAt(sb.length() - 1);

    sb.append(" <VALUE>");

    return sb.toString();
  }
  
  private String getOptionEncoding(int id) {
    DHCPEncoding current = null;
    for(DHCPEncoding e:sortedlist)
    {
      if(e.getId()==id) current = e;
    }
    if (current == null)
      return "";
    List<DHCPEncoding> parenttree = new ArrayList<DHCPEncoding>();
    
    addParent(current, parenttree);

    if (parenttree.size() == 0)
      return "";

    return Integer.toString((int) parenttree.get(0).getTypeEncoding());
  }
  
  private void addParent(DHCPEncoding current, List<DHCPEncoding> result) {
    if (current.getParentId() != null) {
      DHCPEncoding hit = null;
      for(DHCPEncoding e:sortedlist)
      {
        if(e.getId()==current.getParentId()) hit = e;
      }
      
      if(hit!=null)addParent(hit, result);
    }
    result.add(current);
  }
  
  private GuiAttribute createGuiAttribute(int guiattid, String name, String werteBereich, int entryid) {
    String optionEncoding = getOptionEncoding(entryid);
    String xdhcpConf = generateXdhcpConf(entryid);

    GuiAttribute neuerEintrag = new GuiAttribute();
    neuerEintrag.setGuiAttributeID(guiattid);
    neuerEintrag.setName(name);
    neuerEintrag.setOptionEncoding(optionEncoding);
    neuerEintrag.setDhcpConf("");//FIXME: was muss hier rein?
    neuerEintrag.setXdhcpConf(xdhcpConf);
    neuerEintrag.setWerteBereich(werteBereich);

    return neuerEintrag;
  }


  private GuiFixedAttribute createGuiFixedAttribute(int guifixid, String name, String value, String valueRange,
                                                           int entryid) {
    String optionEncoding = getOptionEncoding(entryid);
    String xdhcpConf = generateXdhcpConf(entryid);

    GuiFixedAttribute neuerEintrag = new GuiFixedAttribute();
    neuerEintrag.setFixedAttributeID(guifixid);
    neuerEintrag.setName(name);
    neuerEintrag.setOptionEncoding(optionEncoding);
    neuerEintrag.setDhcpConf("");//FIXME
    neuerEintrag.setXdhcpConf(xdhcpConf);
    neuerEintrag.setValue(value);
    neuerEintrag.setValueRange(valueRange);

    return neuerEintrag;
  }


  private GuiParameter createGuiParameter(int guiparid, String name, int entryid) {
    String xdhcpConf = generateXdhcpConf(entryid);


    GuiParameter neuerEintrag = new GuiParameter();

    neuerEintrag.setGuiParameterID(guiparid);
    neuerEintrag.setName(name);
    neuerEintrag.setDhcpConf("");//FIXME
    neuerEintrag.setXdhcpConf(xdhcpConf);

    return neuerEintrag;
  }
  
}
