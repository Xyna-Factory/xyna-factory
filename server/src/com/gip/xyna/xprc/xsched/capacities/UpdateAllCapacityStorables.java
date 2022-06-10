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
package com.gip.xyna.xprc.xsched.capacities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;


/**
 *
 */
public abstract class UpdateAllCapacityStorables {

  public abstract List<CapacityStorable> performUpdate(ODSConnection con) throws PersistenceLayerException;
  
  
  /**
   * @param capacityStorableQueries
   * @param ownBinding
   * @return
   */
  public static UpdateAllCapacityStorables forOwnUsage(CapacityStorableQueries capacityStorableQueries, int ownBinding) {
    return new UACSForOwnUsage(capacityStorableQueries,ownBinding);
  }

  /**
   * @param capacityStorableQueries
   * @param oldBinding
   * @param ownBinding
   * @return
   */
  public static UpdateAllCapacityStorables changeBinding(CapacityStorableQueries capacityStorableQueries, int oldBinding,
                                                         int ownBinding) {
    return new UACSChangeBinding(capacityStorableQueries, oldBinding, ownBinding);
  }


  /**
   * @param capacityStorableQueries
   * @param ownBinding
   * @return
   */
  public static UpdateAllCapacityStorables addCapsForBinding(CapacityStorableQueries capacityStorableQueries,
                                                             int ownBinding) {
    return new UACSAddCapsForBinding(capacityStorableQueries, ownBinding);
  }

  
  
  public static class UACSForOwnUsage extends UpdateAllCapacityStorables {
    private CapacityStorableQueries capacityStorableQueries;
    private int ownBinding;
    public UACSForOwnUsage( CapacityStorableQueries capacityStorableQueries, int ownBinding) {
      this.capacityStorableQueries = capacityStorableQueries;
      this.ownBinding = ownBinding;
    }
    public List<CapacityStorable> performUpdate(ODSConnection con) throws PersistenceLayerException {
      List<CapacityStorable> allCapacities = capacityStorableQueries.loadAllForUpdate(con);
      ArrayList<CapacityStorable> ownCapacities = new ArrayList<CapacityStorable>();
      
      //cardinalities zählen
      HashMap<String, AtomicInteger> cardinalities = new HashMap<String, AtomicInteger>();
      for (CapacityStorable cs : allCapacities) {
        AtomicInteger cardCounter = cardinalities.get(cs.getName());
        if (cardCounter == null) {
          cardCounter = new AtomicInteger(0);
          cardinalities.put(cs.getName(), cardCounter);
        }
        cardCounter.addAndGet(cs.getCardinality());
      }

      //cardinalities umtragen
      for (CapacityStorable cs : allCapacities) {
        if (cs.getBinding() == ownBinding) {
          ownCapacities.add(cs);
          cs.setCardinality(cardinalities.get(cs.getName()).get());
        } else {
          cs.setCardinality(0);
        }
      }

      con.persistCollection(allCapacities);
      con.commit();

      return ownCapacities;
    }
  }
  
  public static class UACSChangeBinding extends UpdateAllCapacityStorables {
    private CapacityStorableQueries capacityStorableQueries;
    private int oldBinding;
    private int ownBinding;
    public UACSChangeBinding( CapacityStorableQueries capacityStorableQueries, int oldBinding, int ownBinding ) {
      this.capacityStorableQueries = capacityStorableQueries;
      this.oldBinding = oldBinding;
      this.ownBinding = ownBinding;
    }
    
    public List<CapacityStorable> performUpdate(ODSConnection con) throws PersistenceLayerException {
      List<CapacityStorable> allCapacities = capacityStorableQueries.loadAllForUpdate(con);
      if( allCapacities.size() == 0 ) {
        return allCapacities; //nichts zu tun
      }
      ArrayList<CapacityStorable> ownCapacities = new ArrayList<CapacityStorable>();

      for (CapacityStorable cs : allCapacities) {
        if( oldBinding == cs.getBinding() ) {
          cs.setBinding(ownBinding);
          ownCapacities.add(cs);
        }
      }

      con.persistCollection(ownCapacities);
      con.commit();
      return ownCapacities;
    }
  }
  
  public static class UACSAddCapsForBinding extends UpdateAllCapacityStorables {
    private CapacityStorableQueries capacityStorableQueries;    
    private int ownBinding;
    public UACSAddCapsForBinding( CapacityStorableQueries capacityStorableQueries, int ownBinding ) {
      this.capacityStorableQueries = capacityStorableQueries;
      this.ownBinding = ownBinding;
    }
    
    public List<CapacityStorable> performUpdate(ODSConnection con) throws PersistenceLayerException {
      List<CapacityStorable> allCapacities = capacityStorableQueries.loadAllForUpdate(con);
      if( allCapacities.size() == 0 ) {
        return allCapacities; //nichts zu tun
      }
      
      ArrayList<CapacityStorable> ownCapacities = new ArrayList<CapacityStorable>();
      HashMap<String,State> caps = new HashMap<String,State>();
      for (CapacityStorable cs : allCapacities) {
        caps.put(cs.getName(), cs.getState() );
        if( ownBinding == cs.getBinding() ) {
          ownCapacities.add(cs);
        }
      }
      for (CapacityStorable cs : ownCapacities) {
        if( ownBinding == cs.getBinding() ) {
          caps.remove(cs.getName());
        }
      }

      for( Map.Entry<String,State> cap : caps.entrySet() ) {
        long newId = XynaFactory.getInstance().getIDGenerator().getUniqueId(); // langsam: IDGenerator braucht potentiell IO calls
        CapacityStorable newCapStorable = new CapacityStorable(newId, ownBinding);
        newCapStorable.setName(cap.getKey());
        newCapStorable.setCardinality(0);
        newCapStorable.setState(cap.getValue());
        ownCapacities.add(newCapStorable);
      }
      
      con.persistCollection(ownCapacities);
      con.commit();
      return ownCapacities;
    }
  }



  
  
}
