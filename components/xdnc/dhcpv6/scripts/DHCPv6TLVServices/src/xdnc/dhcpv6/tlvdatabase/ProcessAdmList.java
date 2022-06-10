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

package xdnc.dhcpv6.tlvdatabase;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import xdnc.dhcpv6.DHCPv6TLVServicesImpl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdnc.dhcpv6.db.storables.Condition;
import com.gip.xyna.xdnc.dhcpv6.db.storables.DeviceClass;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiAttribute;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiFixedAttribute;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiParameter;
import com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType;



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

  private List<DHCPv6EncodingAdm> sortedlist = new ArrayList<DHCPv6EncodingAdm>();
  private List<DHCPv6EncodingAdm> resultlist = new ArrayList<DHCPv6EncodingAdm>();

  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPv6TLVServicesImpl.class);
  
  private static String valueRange ="";
  
  public static final String VALUERANGE = "xdnc.dhcpv6.config.guifixedattribute.valuerange";
  
  public void process() {

    ProcessAdmList.valueRange = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(VALUERANGE);

    loadData();

    List<DHCPv6EncodingAdm> entriesWithStatus = new ArrayList<DHCPv6EncodingAdm>();

    for (DHCPv6EncodingAdm e : sortedlist) {
      if (e.getStatusFlag().length() > 0) {
        if (e.getReadOnly()) {
          logger.info("(deployTLV) Readonly Entries may not have a StatusFlag! (Entry with ID " + e.getId() + ")");
          return;
        }


        if (!e.getStatusFlag().equals("NEW") && !e.getStatusFlag().equals("MOD") && !e.getStatusFlag().equals("DEL")) {
          logger.info("(deployTLV) Invalid Statusflag (Entry with ID " + e.getId() + ")");
          return;
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


    // logger.info("Test GenerateDHCPConf: ");
    // for(int i=0;i<70;i++)
    // {
    // logger.info(generateDhcpConf(i));
    // logger.info(getOptionEncoding(i));
    //
    // }


  }


  private void commit() {
    LoadGuiLists guianbindung = new LoadGuiLists();
    guianbindung.setUp();

    try {
      guianbindung.createListOfGuiAttributeEntries(guiattributelist);
      guianbindung.createListOfGuiFixedAttributeEntries(guifixedattributelist);
      guianbindung.createListOfGuiParameterEntries(guiparameterlist);

    }
    catch (Exception e) {
      logger.info("(DeployTLV) Failed to write to database");
      e.printStackTrace();
    }

    LoadAdmList anbindung = new LoadAdmList();
    anbindung.setUp();

    try {
      if(resultlist.size()>0)anbindung.createListOfDHCPEncodingEntry(resultlist);
    }
    catch (Exception e) {
      logger.info("(DeployTLV) Failed to write to database");
      e.printStackTrace();
    }

    DuplicateTLV dup = new DuplicateTLV();
    dup.duplicate();
    logger.info("(deployTLV) Finished.");

  }


  private void loadData() {
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
      logger.info("(deployTLV) Failed to read from database");
      e.printStackTrace();
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

    Collection<DHCPv6EncodingAdm> list = new ArrayList<DHCPv6EncodingAdm>();

    // Datenbankanbindung aufbauen und Eintraege holen
    LoadAdmList anbindung = new LoadAdmList();
    anbindung.setUp();

    try {
      list = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      logger.info("(deployTLV) Failed to read from database");
      e.printStackTrace();
    }

    if (list.size() == 0) {
      logger.info("(deployTLV) Dataset from database empty");
      throw new IllegalArgumentException();
    }

    // Liste sortieren und auf doppelte IDs pruefen


    for (int z = 0; z < list.toArray().length; z++) {
      sortedlist.add((DHCPv6EncodingAdm) list.toArray()[z]);
    }

    Collections.sort(sortedlist);

    int id = -1;
    for (DHCPv6EncodingAdm e : sortedlist) {
      if (e.getId() > id) {
        id = e.getId();
      }
      else {
        logger.info("(deployTLV) Fehler: doppelte ID!");
      }
    }


  }


  private GuiAttribute createGuiAttribute(int guiattid, String name, String werteBereich, int entryid) {
    String optionEncoding = getOptionEncoding(entryid);
    String dhcpConf = generateDhcpConf(entryid);

    GuiAttribute neuerEintrag = new GuiAttribute();
    neuerEintrag.setGuiAttributeID(guiattid);
    neuerEintrag.setName(name);
    neuerEintrag.setOptionEncoding(optionEncoding);
    neuerEintrag.setDhcpConf(dhcpConf);
    neuerEintrag.setWerteBereich(werteBereich);

    return neuerEintrag;
  }


  private GuiFixedAttribute createGuiFixedAttribute(int guifixid, String name, String value, String valueRange,
                                                           int entryid) {
    String optionEncoding = getOptionEncoding(entryid);
    String dhcpConf = generateDhcpConf(entryid);

    GuiFixedAttribute neuerEintrag = new GuiFixedAttribute();
    neuerEintrag.setFixedAttributeID(guifixid);
    neuerEintrag.setName(name);
    neuerEintrag.setOptionEncoding(optionEncoding);
    neuerEintrag.setDhcpConf(dhcpConf);
    neuerEintrag.setValue(value);
    neuerEintrag.setValueRange(valueRange);

    return neuerEintrag;
  }


  private GuiParameter createGuiParameter(int guiparid, String name, int entryid) {
    String dhcpConf = generateDhcpConf(entryid);


    GuiParameter neuerEintrag = new GuiParameter();

    neuerEintrag.setGuiParameterID(guiparid);
    neuerEintrag.setName(name);
    neuerEintrag.setDhcpConf(dhcpConf);

    return neuerEintrag;
  }


  private void createEntries() {

    logger.info("(deployTLV) creating Entries ...");

    for (DHCPv6EncodingAdm e : sortedlist) {

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
          name = e.getGuiAttribute();
          werteBereich = e.getGuiAttributeWertebereich();

          GuiAttribute neuerEintrag = createGuiAttribute(guiattid, name, werteBereich, e.getId());

          logger.info("(deployTLV) New GuiAttribute: " + neuerEintrag.getGuiAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getWerteBereich());

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
          name = e.getFixedGuiAttribute();
          value = e.getFixedGuiAttributeValue();
          valueRange = ProcessAdmList.valueRange;


          GuiFixedAttribute neuerEintrag = createGuiFixedAttribute(guifixid, name, value, valueRange, e.getId());

          logger.info("(deployTLV) New GuiFixedAttribute: " + neuerEintrag.getGuiFixedAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getValue() + " ; " + neuerEintrag
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
          name = e.getGuiParameter();

          GuiParameter neuerEintrag = createGuiParameter(guiparid, name, e.getId());

          logger.info("(deployTLV) New GuiParameter: " + neuerEintrag.getGuiParameterID() + " ; " + neuerEintrag
                          .getName() + " ; " + neuerEintrag.getDhcpConf());

          guiparameterlist.add(neuerEintrag);
          newGuiParameter = neuerEintrag.getName();
          newGuiParameterid = neuerEintrag.getGuiParameterID();

        }

        DHCPv6EncodingAdm copy = new DHCPv6EncodingAdm(e.getId(), e.getParentId(), e.getTypeName(),
                                                       e.getTypeEncoding(), e.getEnterpriseNr(), e
                                                                       .getValueDataTypeName(), e
                                                                       .getValueDataTypeArguments(), e.getReadOnly(),
                                                       "", e.getGuiName(), newGuiAttribute, newGuiFixedAttribute,
                                                       newGuiParameter, newGuiAttributeid, newGuiFixedAttributeid, 
                                                       newGuiParameterid,newGuiAttributeWertebereich,newGuiFixedAttributeValue);
        resultlist.add(copy); // Statusflag entfernen

      }
      else if (e.getStatusFlag().equals("MOD")) {

        if (e.getGuiAttribute().length() > 0) {

          int guiattid;
          String name;
          String werteBereich;

          guiattid = e.getGuiAttributeId();
          name = e.getGuiAttribute();
          werteBereich = e.getGuiAttributeWertebereich();

          GuiAttribute neuerEintrag = createGuiAttribute(guiattid, name, werteBereich, e.getId());

          logger.info("(deployTLV) Modified GuiAttribute: " + neuerEintrag.getGuiAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getWerteBereich());

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
        if (e.getFixedGuiAttribute().length() > 0) {

          int guifixid;
          String name;
          String value;
          String valueRange;

          guifixid = e.getFixedGuiAttributeId();
          name = e.getFixedGuiAttribute();
          value = e.getFixedGuiAttributeValue();
          valueRange = ProcessAdmList.valueRange;


          GuiFixedAttribute neuerEintrag = createGuiFixedAttribute(guifixid, name, value, valueRange, e.getId());

          logger.info("(deployTLV) Modified GuiFixedAttribute: " + neuerEintrag.getGuiFixedAttributeID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getOptionEncoding() + " ; " + neuerEintrag
                                          .getDhcpConf() + " ; " + neuerEintrag.getValue() + " ; " + neuerEintrag
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

        if (e.getGuiParameter().length() > 0) {

          int guiparid;
          String name;

          guiparid = e.getGuiParameterId();
          name = e.getGuiParameter();

          GuiParameter neuerEintrag = createGuiParameter(guiparid, name, e.getId());

          logger.info("(deployTLV) Modified GuiParameter: " + neuerEintrag.getGuiParameterID() + " ; " + neuerEintrag
                                          .getName() + " ; " + neuerEintrag.getDhcpConf());


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


        DHCPv6EncodingAdm copy = new DHCPv6EncodingAdm(e.getId(), e.getParentId(), e.getTypeName(),
                                                       e.getTypeEncoding(), e.getEnterpriseNr(), e
                                                                       .getValueDataTypeName(), e
                                                                       .getValueDataTypeArguments(), e.getReadOnly(),
                                                       "", e.getGuiName(), e.getGuiAttribute(), e
                                                                       .getFixedGuiAttribute(), e.getGuiParameter(),
                                                                       e.getGuiAttributeId(),e.getFixedGuiAttributeId(),e.getGuiParameterId(),
                                                                       e.getGuiAttributeWertebereich(),e.getFixedGuiAttributeValue());
        resultlist.add(copy); // Statusflag entfernen

      }
      else if (e.getStatusFlag().equals("DEL")) {
        if (e.getGuiAttribute().length() > 0) {


          int guiattid;

          guiattid = e.getGuiAttributeId();

          GuiAttribute old = null;
          // alten Eintrag entfernen
          for (GuiAttribute g : guiattributelist) {
            if (g.getGuiAttributeID() == guiattid) {
              old = g;
            }
          }
          logger.info("(deployTLV) Delete GuiAttribute: " + old.getGuiAttributeID() + " ; " + old
                                          .getName() + " ; " + old.getOptionEncoding() + " ; " + old.getDhcpConf() + " ; " + old
                                          .getWerteBereich());

          boolean success = false;
          if (old != null)
            success = guiattributelist.remove(old);

          if (!success) {
            logger.info("(deployTLV) Could not delete GuiAttribute. Not found. Entry with ID " + e.getId());
          }

        }
        if (e.getFixedGuiAttribute().length() > 0) {

          int guifixid;

          guifixid = e.getFixedGuiAttributeId();

          GuiFixedAttribute old = null;
          // alten Eintrag entfernen
          for (GuiFixedAttribute g : guifixedattributelist) {
            if (g.getGuiFixedAttributeID() == guifixid) {
              old = g;
            }
          }

          logger.info("(deployTLV) Delete GuiFixedAttribute: " + old.getGuiFixedAttributeID() + " ; " + old
                          .getName() + " ; " + old.getOptionEncoding() + " ; " + old.getDhcpConf() + " ; " + old
                          .getValue() + " ; " + old.getValueRange());

          boolean success = false;
          if (old != null)
            success = guifixedattributelist.remove(old);

          if (!success) {
            logger.info("(deployTLV) Could not delete GuiFixedAttribute. Not found. Entry with ID " + e.getId());
          }


        }

        if (e.getGuiParameter().length() > 0) {

          int guiparid;

          guiparid = e.getGuiParameterId();

          GuiParameter old = null;
          // alten Eintrag entfernen
          for (GuiParameter g : guiparameterlist) {
            if (g.getGuiParameterID() == guiparid) {
              old = g;
            }
          }

          logger.info("(deployTLV) Delete GuiParameter: " + old.getGuiParameterID() + " ; " + old
                                          .getName() + " ; " + old.getDhcpConf());

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


  private void addParent(DHCPv6EncodingAdm current, List<DHCPv6EncodingAdm> result) {
    if (current.getParentId() != null) {
      addParent(sortedlist.get(current.getParentId()), result);
    }
    result.add(current);
  }


  private String generateDhcpConf(int id) {
    DHCPv6EncodingAdm current = sortedlist.get(id);
    if (current == null)
      return "";
    List<DHCPv6EncodingAdm> parenttree = new ArrayList<DHCPv6EncodingAdm>();


    addParent(current, parenttree);


    StringBuilder sb = new StringBuilder();

    sb.append("option ");

    for (DHCPv6EncodingAdm e : parenttree) {
      sb.append(e.getTypeName() + ".");
    }
    sb.deleteCharAt(sb.length() - 1);

    sb.append(" <VALUE>");

    return sb.toString();
  }


  private String getOptionEncoding(int id) {
    DHCPv6EncodingAdm current = sortedlist.get(id);
    if (current == null)
      return "";
    List<DHCPv6EncodingAdm> parenttree = new ArrayList<DHCPv6EncodingAdm>();


    addParent(current, parenttree);

    if (parenttree.size() == 0)
      return "";

    return Integer.toString((int) parenttree.get(0).getTypeEncoding());
  }


  private boolean checkEntries(List<DHCPv6EncodingAdm> list) {

    logger.info("(deployTLV) Starting check of entries with Statusflag ...");
    boolean failure = false;
    int guiattid = -1;
    int guifixid = -1;
    int guiparid = -1;

    // Eintraege holen mit Statusflag != leer


    for (DHCPv6EncodingAdm e : list) {

      // logger.info(e.getId()+"\t:\t"+e.getTypeName());

      guiattid = -1;
      guifixid = -1;
      guiparid = -1;


      if (e.getGuiAttribute().length() > 0) {
        if (e.getFixedGuiAttribute().length() > 0 || e.getFixedGuiAttributeValue().length() > 0) {
          logger.info("(deployTLV) Setting GuiAttributes AND FixedGuiAttributes not valid! (Entry with ID " + e
                          .getId() + ")");
          failure = true;
        }
        else // Syntaxcheck
        {
          if (e.getGuiAttributeWertebereich().length() == 0) {
            logger.info("(deployTLV) GuiAttribute arguments not set! (Entry with ID " + e.getId() + ")");
            failure = true;
          }

          if(e.getStatusFlag().equals("NEW"))
          {
            if(e.getGuiAttributeId()>0)
            {
              logger.info("(deployTLV) GuiAttributeID should be set to -1 creating new Entries! (Entry with ID " + e
                          .getId() + ")");
              failure = true;
            }
          }
          else // MOD or DEL
          {
            if(e.getGuiAttributeId()<0)
            {
              logger.info("(deployTLV) GuiAttributeID must be set! (Entry with ID " + e
                          .getId() + ")");
              failure = true;
            }
            else
            {
              guiattid = e.getFixedGuiAttributeId();
            }
          }
        }
      }
      else if (e.getFixedGuiAttribute().length() > 0) {
        if (e.getGuiAttribute().length() > 0 || e.getGuiAttributeWertebereich().length() > 0) {
          logger.info("(deployTLV) Setting GuiAttributes AND FixedGuiAttributes not valid (Entry with ID " + e
                          .getId() + ")");
          failure = true;
        }
        else // Syntaxcheck
        {
          if (e.getFixedGuiAttributeValue().length() == 0) {
            logger.info("(deployTLV) GuiFixedAttribute arguments not set! (Entry with ID " + e.getId() + ")");
            failure = true;
          }

          boolean found = false; // Liegt value in ValueRange?
          for (String s : ProcessAdmList.valueRange.split(",")) {
            if (s.equals(e.getFixedGuiAttributeValue()))
            {
              found = true;
            }
          }
          if (!found) {
            logger.info("(deployTLV) Given value doesn't fit given valueRange at Entry with ID " + e
                                            .getId());
            failure = true;
          }

          if (e.getStatusFlag().equals("NEW")) {
            if(e.getFixedGuiAttributeId()>0)
            {
              logger.info("(deployTLV) GuiFixedAttributeID should be set to -1 creating new Entries! (Entry with ID " + e
                          .getId() + ")");
              failure = true;
            }
          }            
          else // MOD or DEL
          {
            if(e.getFixedGuiAttributeId()<0)
            {
              logger.info("(deployTLV) GuiFixedAttributeID must be set! (Entry with ID " + e
                          .getId() + ")");
              failure = true;
            }
            else
            {
              guifixid = e.getFixedGuiAttributeId();
            }
            
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
          }
          
        }
        else // MOD or DEL
        {
          if(e.getGuiParameterId()<0)
          {
            logger.info("(deployTLV) GuiParameterID must be set! (Entry with ID " + e
                        .getId() + ")");
            failure = true;
          }
          else
          {
            guiparid = e.getGuiParameterId();
          }
          
        }

      }

      // Pruefen ob IDs zum Aendern oder Loeschen auch vorhanden ...
      if (!e.getStatusFlag().equals("NEW")) {
        if (guiattid != -1) {
          if (!guiattributeids.contains(guiattid)) {
            logger.info("(deployTLV) ID of guiattribute entry to modify or delete does not exist! (Entry with ID " + e
                                            .getId() + ")");
            failure = true;
          }
        }

        if (guifixid != -1) {
          if (!guifixedattributeids.contains(guifixid)) {
            logger.info("(deployTLV) ID of guifixedattribute entry to modify or delete does not exist! (Entry with ID " + e
                                            .getId() + ")");
            failure = true;
          }
        }

        if (guiparid != -1) {
          if (!guiparameterids.contains(guiparid)) {
            logger.info("(deployTLV) ID of guiparameter entry to modify or delete does not exist! (Entry with ID " + e
                                            .getId() + ")");
            failure = true;
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
              }
            }
            if(d.getFixedAttributes()!=null)
            {
              String[] fixedAttributes = d.getFixedAttributes().split(",");
              for(String s:fixedAttributes)
              {
                if(Integer.parseInt(s)==guifixid)
                {
                  logger.info("(deployTLV) Dependencies in class table: guifixedattributeid " + guifixid + " in entry with ID " + d
                              .getClassID() + ". " + "Remove first before deleting Entry (ID " + e
                              .getId() + ") with GuiFixedAttribute.");
                  failure = true;
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
              }
            }
            if(p.getFixedAttributes()!=null)
            {
              String[] fixedAttributes = p.getFixedAttributes().split(",");
              for(String s:fixedAttributes)
              {
                if(Integer.parseInt(s)==guifixid)
                {
                  logger.info("(deployTLV) Dependencies in pooltype table: guifixedattributeid " + guifixid + " in entry with ID " + p.getPoolTypeID() + ". " + "Remove first before deleting Entry (ID " + e
                              .getId() + ") with GuiFixedAttribute.");
                  failure = true;
                }
              }
            }
          }


        }

      }
    }
    if (failure) {
      logger.info("(deployTLV) One or more entries invalid ... check failed");
      return false;
    }
    logger.info("(deployTLV) Check successful. Valid entries.");
    return true;

  }
}
