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
package com.gip.xyna.xprc.xsched.scheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.CapacityEntryInformation;
import com.gip.xyna.xprc.xsched.capacities.CapacityManagementReservationInterface;


/**
 *
 */
public class FilteredCapacityReservationTest extends TestCase {

  private final static int NORMAL = 0;
  private final static int LOCKED = 1;
  private final static int HAS_ALL = 2;
  
    
  private static class CapacityManagementMock implements CapacityManagementReservationInterface {

    List<Capacity> caps;
    
    private List<CapacityDemand> actualDemand = null;
    
    public CapacityManagementMock(Capacity ... caps) {
      this.caps = new ArrayList<Capacity>(Arrays.asList(caps));
    }

    public int getOwnBinding() {
      return 1;
    }
    
    private Capacity findCap(String capName) {
      for( Capacity c : caps ) {
        if( c.getCapName().equals(capName) ) {
          return c;
        }
      }
      return null;
    }

    /**
     * @param capacity
     */
    public void changeCapacity(Capacity capacity) {
      Capacity c = findCap(capacity.getCapName() );
      if( c!= null ) {
        c.setCardinality(capacity.getCardinality());
      } else {
        caps.add(capacity);
      }
      
    }

    public CapacityEntryInformation getCapacityEntryInformation(String capName) {
      Capacity c = findCap(capName);
      boolean hasAll = c!=null?c.getCardinality()==HAS_ALL:false;
      boolean isCapLocked = c!=null?c.getCardinality()==LOCKED:false;
      int total = 2;
      int own = hasAll ? 2 : 1;
      State state = isCapLocked ? State.DISABLED : State.ACTIVE;
      return new CapacityEntryInformation(capName,total,own,state,0,0,0);
    }

    public int reserveCapForForeignBinding(int binding, Capacity capacity) {
      // TODO Auto-generated method stub
      return 0;
    }

    public int transportReservedCaps() {
      // TODO Auto-generated method stub
      return 0;
    }
    
    public boolean communicateDemand(int binding, List<CapacityDemand> demand) {
      //System.err.println( "communicateDemand" );
      if( actualDemand == null ) {
        actualDemand = demand;
      } else {
        Assert.fail("communicateDemand called while actualDemand was already set");
      }
      return true;
    }
    
    public String expectDemand(List<CapacityDemand> expectedDemand) {
      if( actualDemand == null ) {
        if( expectedDemand.size() == 0 ) {
          return ""; 
        } else {
          Assert.fail("expectedDemand called while actualDemand was not set");
        }
      }
      try {
        if( expectedDemand.toString().equals(actualDemand.toString()) ) {
          return "";
        }
        return actualDemand + " <-> " + expectedDemand;
      } finally {
        actualDemand = null; //Demand wurde gepr�ft, daher leeren, um weiteren communicateDemand-Aufruf zu erlauben
      }
    }

  }
  /*
  private static class SchedulerMock implements ClusteredSchedulerInterface {
    //private List<CapacityDemand> emptyDemand = new ArrayList<CapacityDemand>(); //leere Liste um NPE zu verhindern
    private List<CapacityDemand> actualDemand = null;

    public void communicateDemandRemotely(int binding, List<CapacityDemand> demand) throws RemoteException {}

    public void notifySchedulerRemotely() throws RemoteException {}
/*
    public boolean communicateDemand(int binding, List<CapacityDemand> demand) {
      //System.err.println( "communicateDemand" );
      if( actualDemand == null ) {
        actualDemand = demand;
      } else {
        Assert.fail("communicateDemand called while actualDemand was already set");
      }
      return true;
    }
*
    public String expectDemand(List<CapacityDemand> expectedDemand) {
      if( actualDemand == null ) {
        if( expectedDemand.size() == 0 ) {
          return ""; 
        } else {
          Assert.fail("expectedDemand called while actualDemand was not set");
        }
      }
      try {
        if( expectedDemand.toString().equals(actualDemand.toString()) ) {
          return "";
        }
        return actualDemand + " <-> " + expectedDemand;
      } finally {
        actualDemand = null; //Demand wurde gepr�ft, daher leeren, um weiteren communicateDemand-Aufruf zu erlauben
      }
    }

    public Boolean resumeOrderRemotely(int binding, Long targetId, Long targetLaneId) throws RemoteException {
      // TODO Auto-generated method stub
      return null;
    }
    
  }
  */
  
  private List<CapacityDemand> createDemandList(CapacityDemand ... cds) {
    return Arrays.asList( cds);
  }
  private CapacityDemand createDemand(String name) {
    CapacityDemand cd = new CapacityDemand(name,3);
    cd.addUrgency(10, 1);
    return cd;
  }
  private CapacityDemand createDemand(String name, int urgency) {
    CapacityDemand cd = new CapacityDemand(name,3);
    cd.addUrgency(urgency, 1);
    return cd;
  }
  private CapacityDemand createNoDemand(String name) {
    return new CapacityDemand(name);
  }

  private void addOwnDemand(FilteredCapacityReservation fcr, List<CapacityDemand> demand) {
    for( CapacityDemand cd : demand) {
      fcr.setOwnDemand(cd);
    }
    fcr.communicateOwnDemand();
  }

  
  
  public void testNormal() {
    CapacityManagementMock cm = new CapacityManagementMock(new Capacity("AAA",NORMAL) );
    FilteredCapacityReservation fcr = new FilteredCapacityReservation(cm); 
    
    List<CapacityDemand> demand = createDemandList(createDemand("AAA"));
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(demand) );
    
    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
  }

  public void testFilteredBecauseLocked() {
    CapacityManagementMock cm = new CapacityManagementMock(new Capacity("AAA",LOCKED) );
    FilteredCapacityReservation fcr = new FilteredCapacityReservation(cm); 
    
    List<CapacityDemand> demand = createDemandList(createDemand("AAA"));
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(demand) ); //wird geschickt, damit bei unlock anderer 
    //Knoten sofort den Bedarf kennt  
    
    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
  }

  public void testFilteredBecauseHasAllCap() {
    CapacityManagementMock cm = new CapacityManagementMock(new Capacity("AAA",HAS_ALL) );
    FilteredCapacityReservation fcr = new FilteredCapacityReservation(cm); 
    
    List<CapacityDemand> demand = createDemandList(createDemand("AAA"));
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(demand) );

    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
  }

  public void testFilteredMixed() {
    CapacityManagementMock cm = new CapacityManagementMock(new Capacity("AAA",NORMAL), new Capacity("BBB",HAS_ALL) );
    FilteredCapacityReservation fcr = new FilteredCapacityReservation(cm); 
    
    List<CapacityDemand> demand = createDemandList(createDemand("AAA"),createDemand("BBB"));
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(demand) );

    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
  }



  public void testDemandChange() {
    CapacityManagementMock cm = new CapacityManagementMock(new Capacity("AAA",NORMAL) );
    FilteredCapacityReservation fcr = new FilteredCapacityReservation(cm); 
    
    List<CapacityDemand> demand = createDemandList(createDemand("AAA",10));
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(demand) );
    
    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
    
    //Bedarf �ndert sich
    List<CapacityDemand> demand2 = createDemandList(createDemand("AAA",20));
    addOwnDemand(fcr, demand2);
    Assert.assertEquals("", cm.expectDemand(demand2) );
    
    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand2);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
  }


  public void testUnlocked() {
    CapacityManagementMock cm = new CapacityManagementMock(new Capacity("AAA",LOCKED) );
    FilteredCapacityReservation fcr = new FilteredCapacityReservation(cm); 
    
    List<CapacityDemand> demand = createDemandList(createDemand("AAA"));
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(demand) );
    
    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
    
    //Unlock
    cm.changeCapacity( new Capacity("AAA",NORMAL));
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
    
    //zweite Bedarfsmeldung wird ignoriert, da Bedarf nicht ge�ndert
    addOwnDemand(fcr, demand);
    Assert.assertEquals("", cm.expectDemand(createDemandList()) );
   
  }

 
  
  
  
  
}
