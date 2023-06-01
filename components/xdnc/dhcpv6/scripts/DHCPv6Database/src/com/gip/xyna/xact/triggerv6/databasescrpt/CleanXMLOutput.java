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
import java.util.Collection;
import java.util.Collections;
import java.util.List;



public class CleanXMLOutput {

  static int lastparent;
  static List<DHCPv6EncodingAdm> customchildren;
  
  
  
  public static void main(String arg[])
  {
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
    
   List<DHCPv6EncodingAdm> sortedlist = new ArrayList<DHCPv6EncodingAdm>();
    
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
    System.out.println("Keine doppelten IDs in Liste gefunden!");
    System.out.println("============================================================");


    System.out.println("optionsv6adm: ");
    System.out.println("============================================================");
    
    for(DHCPv6EncodingAdm e:sortedlist)
    {
      System.out.println(e.getId()+" : "+e.getParentId()+" : "+e.getTypeName()+" : "+e.getTypeEncoding()+" : "+e.getValueDataTypeName());
    }
    System.out.println("============================================================");

    
    
    Collection<DHCPv6Encoding> liste = new ArrayList<DHCPv6Encoding>();

    
    LoadList nonadmindatabase = new LoadList();
    nonadmindatabase.setUp();
    
    try
    {
      liste = nonadmindatabase.loadDHCPEntries();      
    }
    catch(Exception e)
    {
      System.out.println("Failed to read from database");
      e.printStackTrace();
    }

    
    List<DHCPv6Encoding> resultlist = new ArrayList<DHCPv6Encoding>();
    
    for(int z = 0;z<liste.toArray().length;z++)
    {
      resultlist.add((DHCPv6Encoding)liste.toArray()[z]);
    }
    
    Collections.sort(resultlist);
    
    id = -1;
    for(DHCPv6Encoding e:resultlist)
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
    System.out.println("Keine doppelten IDs in Liste gefunden!");
    System.out.println("============================================================");

        
    System.out.println("optionsv6: ");
    System.out.println("============================================================");
    

    
    
    for(DHCPv6Encoding e:resultlist)
    {
      System.out.println(e.getId()+" : "+e.getParentId()+" : "+e.getTypeName()+" : "+e.getTypeEncoding()+" : "+e.getValueDataTypeName());
    }
    
  }


}
