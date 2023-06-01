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

package com.gip.xyna.xprc.xsched;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.gip.xyna.xprc.xsched.capacities.CapacityUsageSlotInformation;



public final class ExtendedCapacityUsageInformation implements Serializable {

  private static final long serialVersionUID = -2935609513345571506L;
  
  public final static long ORDER_ID_FOR_UNOCCUPIED_CAPACITY = -1;
  public final static long ORDER_ID_FOR_DISABLED_CAPACITY = -2;
  public final static long ORDER_ID_FOR_RESERVED_CAPACITY = -3;
  
  //TODO refactoren, damit capacityName Key ist
  //besser wäre TreeMap<String, List<CapacityUsageSlotInformation>> slotInformation;
  private final TreeMap<Integer, HashSet<CapacityUsageSlotInformation>> slotInformation;


  public ExtendedCapacityUsageInformation() {
    slotInformation = new TreeMap<Integer, HashSet<CapacityUsageSlotInformation>>();
  }


  public synchronized void addSlotInformation(Integer capacityIndex, CapacityUsageSlotInformation info) {
    if (capacityIndex == null)
      throw new IllegalArgumentException("Null is not allowed as index!");
    if (info == null)
      throw new IllegalArgumentException("Null is not allowed as index!");
    if (this.slotInformation.get(capacityIndex) == null) {
      slotInformation.put(capacityIndex, new HashSet<CapacityUsageSlotInformation>());
    }
    slotInformation.get(capacityIndex).add(info);
  }


  public synchronized SortedMap<Integer, HashSet<CapacityUsageSlotInformation>> getSlotInformation() {
    return Collections.unmodifiableSortedMap(slotInformation);
  }

  private static class MergeCapData {
    private int cardinality = 0;
    private List<CapacityUsageSlotInformation> orders = new ArrayList<CapacityUsageSlotInformation>();
    private List<CapacityUsageSlotInformation> unused = new ArrayList<CapacityUsageSlotInformation>();
    private String capacityName;
  }
  
 
  /**
   * @param ecuiList
   * @return
   */
  public static ExtendedCapacityUsageInformation merge(List<ExtendedCapacityUsageInformation> ecuiList) {
    if( ecuiList == null || ecuiList.size() == 0 ) {
      return new ExtendedCapacityUsageInformation();
    }
    if( ecuiList.size() == 1 ) {
      return ecuiList.get(0);
    }
    
    //Daten zu den Capacities zusammenfassen
    TreeMap<String, MergeCapData> mcdMap = new TreeMap<String, MergeCapData>();
    for( ExtendedCapacityUsageInformation ecui : ecuiList ) {
      for( HashSet<CapacityUsageSlotInformation> cusis : ecui.slotInformation.values() ) {
        boolean first = true;
        MergeCapData mcd = null;
        for( CapacityUsageSlotInformation cusi : cusis ) {
          if( first ) { 
            //erster Eintrag zur Intialisierung von mcd; HashSet macht hier alles kompliziert...
            String capName = cusi.getCapacityName();
            mcd = mcdMap.get(capName);
            if ( mcd == null ) {
              mcd = new MergeCapData();
              mcd.capacityName = capName;
              mcdMap.put( capName, mcd );
            }
            mcd.cardinality += cusi.getMaxSlotIndex()+1; //Cardinality dieses Knotens zur Gesamt-Cardinality hinzufügen
            first = false;
          }
          if( cusi.isOccupied() ) {
            mcd.orders.add( cusi );
          } else {
            mcd.unused.add( cusi );
          }
        }
      }
    }
    
    //alle Informationen gesammelt, nun zusammenfassen
    ExtendedCapacityUsageInformation merged = new ExtendedCapacityUsageInformation();
    int privateCapacityIndex = 0;
    for( MergeCapData mcd : mcdMap.values() ) {
      ++privateCapacityIndex;

      ArrayList<CapacityUsageSlotInformation> genCusis = new ArrayList<CapacityUsageSlotInformation>(mcd.cardinality);

      if( mcd.orders.size() + mcd.unused.size() != mcd.cardinality ) {
        //FIXME Fehlermeldung
      }
      
      //Einträge für benutzte Caps
      int slotIndex = -1;
      Collections.sort( mcd.orders, new CapacityUsageSlotInformationComparator() );
      
      for( CapacityUsageSlotInformation order : mcd.orders ) {
        ++slotIndex;
        genCusis.add( new CapacityUsageSlotInformation(
          mcd.capacityName, 
          privateCapacityIndex, 
          true, //occupied, 
          order.getUsingOrderType(), //usingOrderType, 
          order.getUsingOrderId(), //usingOrderId,
          order.isTransferable(),
          order.getBinding(),
          slotIndex, 
          mcd.cardinality-1 //maxSlotIndex
          )
        );
      }
      
      //Einträge für ungenutzte Caps erzeugen
      Collections.sort( mcd.unused, new CapacityUsageSlotInformationComparator() );
      for( CapacityUsageSlotInformation unused : mcd.unused ) {
        ++slotIndex;
        genCusis.add( new CapacityUsageSlotInformation(
          mcd.capacityName, 
          privateCapacityIndex, 
          false, //occupied, 
          unused.getUsingOrderType(), 
          unused.getUsingOrderId(),
          false,
          unused.getBinding(),
          slotIndex, 
          mcd.cardinality-1 //maxSlotIndex
          )
        );
      }

      merged.slotInformation.put( privateCapacityIndex, new HashSet<CapacityUsageSlotInformation>(genCusis) );      
    }
    
    return merged;
  }
  
  private static class CapacityUsageSlotInformationComparator implements Comparator<CapacityUsageSlotInformation>, Serializable {

    private static final long serialVersionUID = 1L;

    public int compare(CapacityUsageSlotInformation o1, CapacityUsageSlotInformation o2) {
      int cmp = compareLong(o1.getUsingOrderId(), o2.getUsingOrderId() );
      if( cmp == 0 ) {
        return compareLong(o1.getBinding(), o2.getBinding() );
      } else {
        return cmp;
      }
    }
    
    private int compareLong( long l1, long l2 ) {
      if( l1 > l2 ) {
        return 1;
      }
      if( l2 > l1 ) {
        return -1;
      }
      return 0;
    }
    
  }
}
