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

package com.gip.xyna.xact.triggerv6.databasescrpt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xact.dhcpv6.db.storables.GuiAttribute;
import com.gip.xyna.xact.dhcpv6.db.storables.GuiFixedAttribute;
import com.gip.xyna.xact.dhcpv6.db.storables.GuiParameter;



public class CleanXMLOutput2 {

  private static Collection<GuiAttribute> guiattributelist;
  private static Collection<GuiFixedAttribute> guifixedattributelist;
  private static Collection<GuiParameter> guiparameterlist;
  private static List<DHCPv6EncodingAdm> sortedlist = new ArrayList<DHCPv6EncodingAdm>();

  
  public static void main(String arg[])
  {
    LoadGuiLists guianbindung = new LoadGuiLists();
    guianbindung.setUp();
    
    try
    {
      guiattributelist = guianbindung.loadGuiAttribute();
      guifixedattributelist = guianbindung.loadGuiFixedAttribute();      
      guiparameterlist = guianbindung.loadGuiParameter();      

    }
    catch(Exception e)
    {
      System.out.println("Failed to read from database");
      e.printStackTrace();
    }

    System.out.println("==================================================================");
    System.out.println("GuiAttribute List ( ID : Encoding : WerteBereich : DhcpConf)");
    System.out.println("==================================================================");

    for(GuiAttribute g:guiattributelist)
    {
      int cut;
      if(g.getWerteBereich().length()>10)
      {
        cut = 7;
      }
      else
      {
        cut = g.getWerteBereich().length();
      }
      System.out.println(g.getGuiAttributeID()+"\t:\t"+g.getOptionEncoding()+"\t:\t"+g.getWerteBereich().substring(0, cut)+"\t:\t"+g.getDhcpConf());
    }
      
    System.out.println("==================================================================");
    System.out.println("GuiFixedAttribute List ( ID : OptionEncoding : DhcpConf )");
    System.out.println("==================================================================");

    for(GuiFixedAttribute f:guifixedattributelist)
    {
      System.out.println(f.getGuiFixedAttributeID()+"\t:\t"+f.getOptionEncoding()+"\t:\t"+f.getDhcpConf());
    }
      
    System.out.println("==================================================================");
    System.out.println("GuiParameter List ( ID : DhcpConf )");
    System.out.println("==================================================================");

    for(GuiParameter p:guiparameterlist)
    {
      System.out.println(p.getGuiParameterID()+"\t:\t"+p.getDhcpConf());

    }
      
    System.out.println("=================================");

    
    
    Collection<DHCPv6EncodingAdm> list = new ArrayList<DHCPv6EncodingAdm>();
    
    // Datenbankanbindung aufbauen und Eintraege holen
    LoadAdmList anbindung = new LoadAdmList();
    anbindung.setUp();
    
    try
    {
      list = anbindung.loadDHCPEntries();      
    }
    catch(Exception e)
    {
      System.out.println("Failed to read from database");
      e.printStackTrace();
    }
    
    if (list.size()==0) 
    {
      System.out.println("Dataset from database empty");
      throw new IllegalArgumentException();
    }
    
    // Liste sortieren und auf doppelte IDs pruefen
    
   
    
    for(int z = 0;z<list.toArray().length;z++)
    {
      sortedlist.add((DHCPv6EncodingAdm)list.toArray()[z]);
    }
    
    Collections.sort(sortedlist);
    
    int id = -1;
    for(DHCPv6EncodingAdm e:sortedlist)
    {
      if(e.getId()> id)
      {
        id = e.getId();
      }
      else
      {
        System.out.println("Fehler: doppelte ID!");
      }
    }

    System.out.println("==================================================================");
    System.out.println("optionv6adm (ID : Status : TypeName )");
    System.out.println("==================================================================");

    for(DHCPv6EncodingAdm e:sortedlist)
    {
      System.out.println(e.getId()+"\t:\t"+e.getStatusFlag()+"\t:\t"+e.getTypeName());
    }

    
    
    
  }


}
