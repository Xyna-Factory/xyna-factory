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
package xdnc.dhcpv6.update;






public class DHCPPoolsUpdaterTest {//extends BaseForTests {

  private static final String ipstart_1 = "eeee:0000:0000:0000:0100::0000";
  private static final String ipend_1 = "eeee:0000:0000:0000:0200::0000";
  private static final String ipstart_1b = "eeee:0000:0000:0000:0100:0000:0000:0000";
  private static final String ipend_1b = "eeee:0000:0000:0000:0200:0000:0000:0000";
  private static final String ipstart_2 = "eeee:0000:0000:0000:0110::0000";
  private static final String ipstart_2b = "eeee:0000:0000:0000:0110:0000:0000:0000";
  private static final String ipend_2 = "eeee:0000:0000:0000:0210::0000";
  private static final String ipend_2b = "eeee:0000:0000:0000:0210:0000:0000:0000";

//  public void test1() throws XynaException {
//    ODS ods = initODS();
//    Map<String, OrderedVirtualObjectPool<IP, Lease>> cache = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>();
//    DHCPPoolsUpdater updater = new DHCPPoolsUpdater(ods, cache, 2, XynaFactory.getInstance().getIDGenerator(),0,"");
//    List<SharedNetwork> sharedNetworks = new ArrayList<SharedNetwork>();
//
//    SharedNetwork sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    Subnet subnet = new Subnet("0000::", 64);
//    subnet.setGUIId(50);
//    Range range = new Range(ipstart_1, ipend_1, 128, "pooltype");
//    range.setGUIId(51);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//
//    updater.updateData(sharedNetworks);
//
//    assertEquals(0, cache.size());
//
//    check1(ods, ODSConnectionType.DEFAULT);
//    check1(ods, ODSConnectionType.HISTORY);
//
//    IPPoolStorable pool;
//    ODSConnection con = ods.openConnection();
//    try {
//      Collection<IPPoolStorable> pools = con.loadCollection(IPPoolStorable.class);
//      assertEquals(1, pools.size());
//      pool = pools.iterator().next();
//      assertEquals(51, pool.getGuiid());
//      assertEquals(5, pool.getId());
//      assertEquals("linkAddresses", pool.getLinkAddresses());
//      assertEquals("pooltype", pool.getPooltype());
//      assertEquals(128, pool.getPrefixlength());
//      assertEquals("mySharedNetwork", pool.getSharedNetwork());
//      assertEquals(50, pool.getSubnetguiid());
//    } finally {
//      con.closeConnection();
//    }
//
//
//    //ein lease vergeben
//    OrderedVirtualObjectPool<IP, Lease> vpool =
//        new OrderedVirtualObjectPool<IP, Lease>(new LeaseCreator(), new PoolPersistenceStrategy() {
//
//          public PersistenceAction doPersist() {
//            return PersistenceAction.PERSIST_SYNCHRONOUSLY;
//          }
//          
//          public void onPersistIsStillRunning(int sizeOfPersistenceBufferInRunningJob,
//              int sizeOfRemainingPersistenceBuffer) {
//          }
//
//        }, pool, ods);
//    vpool.createFromInterval(new Condition<PoolElementInterval<IP>>() {
//
//      public Parameter getParameter() {
//        return null;
//      }
//
//      public String getQuery() {
//        return null;
//      }
//
//      public Class<? extends IPInterval> getStorableClass() {
//        return IPInterval.class;
//      }
//
//    }, new PoolElementAction<Lease>() {
//
//      public void execute(Lease arg0) {
//        arg0.setExpirationTime(400);
//      }
//    }, null);
//
//
//    //update auf intervalle
//    sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    subnet = new Subnet("0000::", 64);
//    subnet.setGUIId(50);
//    range = new Range(ipstart_2, ipend_2, 128, "pooltype");
//    range.setGUIId(51);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//
//    updater.updateData(sharedNetworks);
//
//    check2(ods, ODSConnectionType.DEFAULT);
//    check2(ods, ODSConnectionType.HISTORY);
//
//  }
//
//
//  private void check1(ODS ods, ODSConnectionType t) throws PersistenceLayerException {
//    ODSConnection con = ods.openConnection(t);
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//      assertEquals(1, intervals.size());
//      Iterator<IPInterval> it = intervals.iterator();
//      IPInterval i1 = it.next();
//      assertEquals(5, i1.getPoolId());
//      assertTrue(ipstart_1.equals(i1.getStart().getIp()) || ipstart_1b.equals(i1.getStart().getIp()));
//      assertTrue(ipend_1.equals(i1.getEnd().getIp()) || ipend_1b.equals(i1.getEnd().getIp()));
//    } finally {
//      con.closeConnection();
//    }
//  }
//
//
//  private void check2(ODS ods, ODSConnectionType t) throws PersistenceLayerException {
//    ODSConnection con = ods.openConnection(t);
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//      assertEquals(1, intervals.size());
//      Iterator<IPInterval> it = intervals.iterator();
//      IPInterval i1 = it.next();
//      assertEquals(5, i1.getPoolId());
//      assertTrue(ipstart_2.equals(i1.getStart().getIp()) || ipstart_2b.equals(i1.getStart().getIp()));
//      assertTrue(ipend_2.equals(i1.getEnd().getIp()) || ipend_2b.equals(i1.getEnd().getIp()));
//
//    } finally {
//      con.closeConnection();
//    }
//  }
//
//  
//  private static final String ipstart_124 = "eeee:0000:0000:0000:0000:0000:0000:0010";
//  private static final String ipend_124 = "eeee:0000:0000:0000:0000:0000:0000:004f";
//  private static final int prefix_124 = 124;
//  private static final String ipstart_128 = "eeee:0000:0000:0000:0000:0000:0000:0013";
//  private static final String ipstart_128expected = "eeee:0000:0000:0000:0000:0000:0000:0020";  
//  private static final String ipend_128 = "eeee:0000:0000:0000:0000:0000:0000:0030";
//  private static final int prefix_128 = 128;
//  
//  public void test2() throws XynaException {
//    ODS ods = initODS();
//    Map<String, OrderedVirtualObjectPool<IP, Lease>> cache = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>();
//    DHCPPoolsUpdater updater = new DHCPPoolsUpdater(ods, cache, 2, XynaFactory.getInstance().getIDGenerator(),0,"");
//    List<SharedNetwork> sharedNetworks = new ArrayList<SharedNetwork>();
//
//    SharedNetwork sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    Subnet subnet = new Subnet("fe80::", 64);
//    subnet.setGUIId(60);
//    Range range = new Range(ipstart_124, ipend_124, prefix_124, "pooltype");
//    range.setGUIId(61);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//
//    updater.updateData(sharedNetworks);
//
//    assertEquals(0, cache.size());
//
//    check1test2(ods, ODSConnectionType.DEFAULT);
//    check1test2(ods, ODSConnectionType.HISTORY);
//
//    IPPoolStorable pool;
//    ODSConnection con = ods.openConnection();
//    try {
//      Collection<IPPoolStorable> pools = con.loadCollection(IPPoolStorable.class);
//      assertEquals(1, pools.size());
//      pool = pools.iterator().next();
//      assertEquals(61, pool.getGuiid());
//      assertEquals(5, pool.getId());
//      assertEquals("linkAddresses", pool.getLinkAddresses());
//      assertEquals("pooltype", pool.getPooltype());
//      assertEquals(prefix_124, pool.getPrefixlength());
//      assertEquals("mySharedNetwork", pool.getSharedNetwork());
//      assertEquals(60, pool.getSubnetguiid());
//    } finally {
//      con.closeConnection();
//    }
//
//
//    //update auf intervalle
//    sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    subnet = new Subnet("fe80::", 64);
//    subnet.setGUIId(60);
//    range = new Range(ipstart_128, ipend_128, prefix_128, "pooltype");
//    range.setGUIId(62);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//    
//    updater.updateData(sharedNetworks);
//
//    check2test2(ods, ODSConnectionType.DEFAULT);
//    check2test2(ods, ODSConnectionType.HISTORY);
//
//  }
//  
//  private void check1test2(ODS ods, ODSConnectionType t) throws PersistenceLayerException {
//    ODSConnection con = ods.openConnection(t);
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//      assertEquals(1, intervals.size());
//      Iterator<IPInterval> it = intervals.iterator();
//      IPInterval i1 = it.next();
//      assertEquals(5, i1.getPoolId());
//      assertEquals(ipstart_124, i1.getStart().getIp());
//      assertEquals(ipend_124, i1.getEnd().getIp());
//
//    } finally {
//      con.closeConnection();
//    }
//  }
//  
//  private void check2test2(ODS ods, ODSConnectionType t) throws PersistenceLayerException {
//    ODSConnection con = ods.openConnection(t);
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//      System.out.println("Intervall-Anzahl = " +intervals.size());
//      
//      assertEquals(1, intervals.size());
//      Iterator<IPInterval> it = intervals.iterator();
//      IPInterval i1 = it.next();
//      System.out.println("prefixlength = " +i1.getPrefixlength()+ ", start = " +i1.getStart().getIp()+ ", end = " +i1.getEnd().getIp());
//      assertEquals(5, i1.getPoolId());
//      assertEquals(ipstart_128, i1.getStart().getIp());
//      assertEquals(ipend_128, i1.getEnd().getIp()); 
//      assertEquals(prefix_128, i1.getPrefixlength());
//      
//    } finally {
//      con.closeConnection();
//    }
//  }
//  
//  
//  public void test3() throws XynaException {
//    ODS ods = initODS();
//    Map<String, OrderedVirtualObjectPool<IP, Lease>> cache = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>();
//    DHCPPoolsUpdater updater = new DHCPPoolsUpdater(ods, cache, 2, XynaFactory.getInstance().getIDGenerator(),0,"");
//    List<SharedNetwork> sharedNetworks = new ArrayList<SharedNetwork>();
//
//    SharedNetwork sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    Subnet subnet = new Subnet("fe80::", 64);
//    subnet.setGUIId(60);
//    Range range = new Range(ipstart_124, ipend_124, prefix_124, "pooltype");
//    range.setGUIId(61);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//
//    updater.updateData(sharedNetworks);
//
//    assertEquals(0, cache.size());
//
//    check1test2(ods, ODSConnectionType.DEFAULT);
//    check1test2(ods, ODSConnectionType.HISTORY);
//
//    IPPoolStorable pool;
//    ODSConnection con = ods.openConnection();
//    try {
//      Collection<IPPoolStorable> pools = con.loadCollection(IPPoolStorable.class);
//      assertEquals(1, pools.size());
//      pool = pools.iterator().next();
//      assertEquals(61, pool.getGuiid());
//      assertEquals(5, pool.getId());
//      assertEquals("linkAddresses", pool.getLinkAddresses());
//      assertEquals("pooltype", pool.getPooltype());
//      assertEquals(prefix_124, pool.getPrefixlength());
//      assertEquals("mySharedNetwork", pool.getSharedNetwork());
//      assertEquals(60, pool.getSubnetguiid());
//    } finally {
//      con.closeConnection();
//    }
//
//
//    //ein lease vergeben
//    OrderedVirtualObjectPool<IP, Lease> vpool =
//        new OrderedVirtualObjectPool<IP, Lease>(new LeaseCreator(), new PoolPersistenceStrategy() {
//
//          public PersistenceAction doPersist() {
//            return PersistenceAction.PERSIST_SYNCHRONOUSLY;
//          }
//          
//          public void onPersistIsStillRunning(int sizeOfPersistenceBufferInRunningJob,
//              int sizeOfRemainingPersistenceBuffer) {
//          }
//
//        }, pool, ods);
//    Lease l = vpool.createFromInterval(new Condition<PoolElementInterval<IP>>() {
//
//      public Parameter getParameter() {
//        return null;
//      }
//
//      public String getQuery() {
//        return null;
//      }
//
//      public Class<? extends IPInterval> getStorableClass() {
//        return IPInterval.class;
//      }
//
//    }, new PoolElementAction<Lease>() {
//
//      public void execute(Lease arg0) {
//        arg0.setExpirationTime(400);
//      }
//    }, null);
//    assertNotNull(l);
//    assertEquals(prefix_124, l.getPrefixlength());
//    assertEquals(ipstart_124, l.getIp());
//    
//    //update auf intervalle
//    sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    subnet = new Subnet("fe80::", 64);
//    subnet.setGUIId(60);
//    range = new Range(ipstart_128, ipend_128, prefix_128, "pooltype");
//    range.setGUIId(62);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//    
//    updater.updateData(sharedNetworks);
//
//    check3test2(ods, ODSConnectionType.DEFAULT);
//    check3test2(ods, ODSConnectionType.HISTORY);
//
//  }
//
//  private void check3test2(ODS ods, ODSConnectionType t) throws PersistenceLayerException {
//    ODSConnection con = ods.openConnection(t);
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//      System.out.println("Intervall-Anzahl = " +intervals.size());
//      
//      assertEquals(1, intervals.size());
//      Iterator<IPInterval> it = intervals.iterator();
//      IPInterval i1 = it.next();
//      System.out.println("prefixlength = " +i1.getPrefixlength()+ ", start = " +i1.getStart().getIp()+ ", end = " +i1.getEnd().getIp());
//      assertEquals(5, i1.getPoolId());
//      assertEquals(ipstart_128expected, i1.getStart().getIp());
//      assertEquals(ipend_128, i1.getEnd().getIp()); 
//      assertEquals(prefix_128, i1.getPrefixlength());
//      
//    } finally {
//      con.closeConnection();
//    }
//  }
//
//  private static final String ipstart_56 = "2a02:0378:ff20:f000:0000:0000:0000:0000";
//  private static final String ipend_56 = "2a02:0378:ff20:f0ff:ffff:ffff:ffff:ffff";
//  private static final int prefix_56 = 56;
//
//
//  public void test4() throws XynaException {
//    ODS ods = initODS();
//    Map<String, OrderedVirtualObjectPool<IP, Lease>> cache = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>();
//    DHCPPoolsUpdater updater = new DHCPPoolsUpdater(ods, cache, 2, XynaFactory.getInstance().getIDGenerator(),0,"");
//    List<SharedNetwork> sharedNetworks = new ArrayList<SharedNetwork>();
//
//    SharedNetwork sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    Subnet subnet = new Subnet("fe80::", 64);
//    subnet.setGUIId(60);
//    Range range = new Range(ipstart_56, ipend_56, prefix_56, "pooltype");
//    range.setGUIId(61);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//
//    updater.updateData(sharedNetworks);
//
//    assertEquals(0, cache.size());
//
//    check1test4(ods, ODSConnectionType.DEFAULT);
//    check1test4(ods, ODSConnectionType.HISTORY);
//  }
//
//
//  private void check1test4(ODS ods, ODSConnectionType t) throws PersistenceLayerException {
//    ODSConnection con = ods.openConnection(t);
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//
//      assertEquals(1, intervals.size());
//      Iterator<IPInterval> it = intervals.iterator();
//      IPInterval i1 = it.next();
//      assertEquals(5, i1.getPoolId());
//      assertEquals(ipstart_56, i1.getStart().getIp());
//      assertEquals(ipend_56, i1.getEnd().getIp());
//      assertEquals(prefix_56, i1.getPrefixlength());
//
//    } finally {
//      con.closeConnection();
//    }
//  }
//  
//
//  private static final String ipstart_test5 = "2a02:0378:ff20:f000:0000:0000:0000:0000";
//  private static final String ipend_test5 = "2a02:0378:ff20:f0ff:ffff:ffff:ffff:ffff";
//  private static final int prefix_test5 = 127;
//
//  
//  public void test5() throws XynaException {
//    ODS ods = initODS();
//    Map<String, OrderedVirtualObjectPool<IP, Lease>> cache = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>();
//    DHCPPoolsUpdater updater = new DHCPPoolsUpdater(ods, cache, 2, XynaFactory.getInstance().getIDGenerator(),0,"");
//    
//    List<SharedNetwork> sharedNetworks = new ArrayList<SharedNetwork>();
//    SharedNetwork sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//    Subnet subnet = new Subnet("fe80::", 64);
//    subnet.setGUIId(60);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//
//    Range range = new Range("0:1000::0", "0:2000::0", 44, "pooltype");
//    range.setGUIId(61);
//    subnet.addRange(range);
//    range = new Range("0:2001::0", "0:3000::0", 45, "pooltype");
//    range.setGUIId(62);
//    subnet.addRange(range);
//    range = new Range("0:3001::0", "0:4000::0", 127, "pooltype");
//    range.setGUIId(63);
//    subnet.addRange(range);
//    range = new Range("0:4001::0", "0:5000::0", 128, "pooltype");
//    range.setGUIId(63);
//    subnet.addRange(range);
//    range = new Range(ipstart_test5, "2a02:0378:ff21::0", 127, "pooltype");
//    range.setGUIId(63);
//    subnet.addRange(range);
//    
//    updater.updateData(sharedNetworks);
//    
//    
//    sharedNetworks = new ArrayList<SharedNetwork>();
//
//    sharedNetwork = new SharedNetwork("mySharedNetwork", "bla");
//    sharedNetwork.setLinkAddresses("linkAddresses");
//
//    subnet = new Subnet("fe80::", 64);
//    subnet.setGUIId(60);
//    range = new Range(ipstart_test5, ipend_test5, prefix_test5, "pooltype");
//    range.setGUIId(61);
//    subnet.addRange(range);
//    sharedNetwork.addSubnet(subnet);
//    sharedNetworks.add(sharedNetwork);
//
//    updater.updateData(sharedNetworks);
//
//    assertEquals(0, cache.size());
//
//    check1test5(ods, ODSConnectionType.DEFAULT);
//    check1test5(ods, ODSConnectionType.HISTORY);
//  }
//
//
//  private void check1test5(ODS ods, ODSConnectionType t) throws PersistenceLayerException {
//    ODSConnection con = ods.openConnection(t);
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//
//      assertEquals(1, intervals.size());
//      Iterator<IPInterval> it = intervals.iterator();
//      IPInterval i1 = it.next();
//      assertEquals(5, i1.getPoolId());
//      assertEquals(ipstart_test5, i1.getStart().getIp());
//      assertEquals(ipend_test5, i1.getEnd().getIp());
//      assertEquals(prefix_test5, i1.getPrefixlength());
//
//    } finally {
//      con.closeConnection();
//    }
//  }
//  
//  public void test6() throws XynaException, IOException, DHCPPoolsFormatException {
//    ODS ods = initODS();
//    Map<String, OrderedVirtualObjectPool<IP, Lease>> cache = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>();
//    DHCPPoolsUpdater updater = new DHCPPoolsUpdater(ods, cache, 2, XynaFactory.getInstance().getIDGenerator(),0,"");
//    
//    DHCPPoolsParser parser = new DHCPPoolsParser(new FileInputStream(new File("classes/networkv6.before")));
//    parser.parse();
//    updater.updateData(parser.getSharedNetworks());
//    
//    ODSConnection con = ods.openConnection();
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//
//      assertEquals(13, intervals.size());
//    } finally {
//      con.closeConnection();
//    }
//    
//    parser = new DHCPPoolsParser(new FileInputStream(new File("classes/networkv6.after")));
//    parser.parse();
//    updater.updateData(parser.getSharedNetworks());
//    
//    //assert: nur noch daten von networkv6.after
//    con = ods.openConnection();
//    try {
//      Collection<IPInterval> intervals = con.loadCollection(IPInterval.class);
//
//      assertEquals(10, intervals.size());
//      checkContains(intervals, 6, "CM-Pool", "2001:cafe:cafe:0000:0000:0000:0000:0000", "2001:cafe:cafe:0000:0000:0000:0fff:ffff", 128);
//      checkContains(intervals, 7, "MTA-Pool", "2001:cafe:cafe:1000:0000:0000:0000:0000", "2001:cafe:cafe:1000:0000:0000:0fff:ffff", 128);
//      checkContains(intervals, 9, "CPE-Pool", "2001:cafe:cafe:8000:0000:0000:0000:0000", "2001:cafe:cafe:8000:0000:0000:0000:ffff", 120);
//      checkContains(intervals, 15, "CPE-Pool", "2001:cafe:cafe:5000:0000:0000:0000:0000", "2001:cafe:cafe:5000:0000:0000:0fff:ffff", 128);
//      checkContains(intervals, 16, "CM-Pool", "2002:00de:caff:bad1:0000:0000:0000:0001", "2002:00de:caff:bad1:0000:0000:0fff:ffff", 128);
//      checkContains(intervals, 17, "MTA-Pool", "2002:00de:caff:bad1:0000:0000:2000:0001", "2002:00de:caff:bad1:0000:0000:2fff:0000", 128);
//      checkContains(intervals, 18, "CPE-Pool", "2002:00de:caff:bad1:0000:0000:3000:0000", "2002:00de:caff:bad1:0000:0000:3fff:ffff", 112);
//      checkContains(intervals, 19, "CPE-Pool", "2002:00de:caff:bad1:4000:0000:0000:0000", "2002:00de:caff:bad1:4000:0000:0fff:ffff", 128);
//      checkContains(intervals, 20, "CPE-Pool", "2a02:0378:ff20:f000:0000:0000:0000:0000", "2a02:0378:ff20:ffff:ffff:ffff:ffff:ffff", 56);
//      checkContains(intervals, 21, "CPE-Pool", "2a02:0378:ff20:0000:0000:0000:0000:0001", "2a02:0378:ff20:0000:0000:0000:0fff:ffff", 128);
//      
//    } finally {
//      con.closeConnection();
//    }
//    
//        
//  }
//
//
//  private void checkContains(Collection<IPInterval> intervals, int poolId, String poolType, String startIp,
//                             String endIp, int prefix) {
//    boolean found = false;
//    for (IPInterval i : intervals) {
//      System.out.println(i.getPoolId() + ": " + i);
//      if (i.getStart().getIp().equals(startIp) && i.getEnd().getIp().equals(endIp) && i.getPrefixlength() == prefix) {
//        found = true;
//        break;
//      }
//    }
//    assertTrue("did not found pool " + poolId, found);
//  }
//
}
