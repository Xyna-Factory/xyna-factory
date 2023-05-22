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
package com.gip.xyna.xnwh.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.xmlshell.XynaXMLShellPersistenceLayer;
import com.gip.xyna.xnwh.persistence.xmlshell.ResultNode;
import com.gip.xyna.xnwh.persistence.xmlshell.ResultNodeHolder;
import com.gip.xyna.xnwh.persistence.xmlshell.ResultNode.OPERATOR;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;

import junit.framework.TestCase;


public class XMLShellTest extends TestCase {

  private ODS ods;
  
  protected void setUp() throws Exception {
    super.setUp();
    
    ods = ODSImpl.getInstance(false);
    
    String[] testParam = {"Test"};
    String[] testParamHistory = {"TestHistory"};
    ods.registerPersistenceLayer(42, XynaXMLShellPersistenceLayer.class);
    long id = ods.instantiatePersistenceLayerInstance(42, "test", ODSConnectionType.DEFAULT, testParam);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    id = ods.instantiatePersistenceLayerInstance(42, "test", ODSConnectionType.HISTORY, testParamHistory);    
    ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id);
    ods.registerStorable(OrderInstance.class);
    ods.registerStorable(OrderInstanceDetails.class);
  }
    
  
  // Single Persist & Query & Delete
  public void testSingleAccess() throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    final long TESTORDER_ID = 111l;
    final String TESTORDER_CUSTOM0 = "test Custom0";
    final String TESTORDER_ORDERTYPE = "de.test.Ordertype";
    final int TESTORDER_MONITORINGLEVEL = 20;
    final long TESTORDER_STOPTIME = Long.MAX_VALUE;
    
    OrderInstanceDetails testOrder = new OrderInstanceDetails(TESTORDER_ID);
    testOrder.setCustom0(TESTORDER_CUSTOM0);
    testOrder.setOrderType(TESTORDER_ORDERTYPE);
    testOrder.setMonitoringLevel(TESTORDER_MONITORINGLEVEL);
    testOrder.setStopTime(TESTORDER_STOPTIME);
    // persist it
    ODSConnection con = ods.openConnection();
    try {
      con.persistObject(testOrder);
      con.commit();
    } finally {
      con.closeConnection();
    }
        
    OrderInstanceDetails loadedOrder = new OrderInstanceDetails(TESTORDER_ID);
    // does an instance with that primaryKey exist
    con = ods.openConnection();
    try {
      assertEquals(true, con.containsObject(loadedOrder));
    
    
      // do the values match
      con.queryOneRow(loadedOrder);
      assertEquals(TESTORDER_CUSTOM0, loadedOrder.getCustom0());
      assertEquals(TESTORDER_ORDERTYPE, loadedOrder.getOrderType());
      assertEquals(TESTORDER_MONITORINGLEVEL, loadedOrder.getMonitoringLevel());
      assertEquals(TESTORDER_STOPTIME, loadedOrder.getStopTime());
    } finally {
      con.closeConnection();
    }
      
    Collection<OrderInstanceDetails> col = new ArrayList<OrderInstanceDetails>();
    col.add(loadedOrder);
    
    con = ods.openConnection();
    try {
      // delete it
      con.delete(col);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    con = ods.openConnection();
    try {
      //no longer contained?
      assertEquals(false, con.containsObject(loadedOrder));
      
      //Object not found?
      try {
        con.queryOneRow(loadedOrder);
        fail("Object should not have been found and a ObjectNotFoundException should have been thrown.");
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        ;
      }

    } finally {
      con.closeConnection();
    }
    
    //clear the table for next test
    con = ods.openConnection();
    try {
      con.deleteAll(OrderInstanceDetails.class);
      con.commit();
    } finally {
      con.closeConnection();
    }   
  }
  
  
  // Collection Persist & Load & Delete 
  public void testMultipleAccess() throws PersistenceLayerException {
        
    final long[] TESTORDER_ID = {111l, 112l, 113l, 114l, 115l, 116l};
    final String[] TESTORDER_ORDERTYPE = {"de.test.Ordertype1", "de.test.Ordertype2", "de.test.Ordertype3", "de.test.Ordertype4", "de.test.Ordertype5", "de.test.Ordertype6"};
    final int[] TESTORDER_MONITORINGLEVEL = {0, 5, 10, 10, 15, 20};
    final long[] TESTORDER_STOPTIME = {111111l, 222222l, 333333l, 444444l, 555555l, 666666l};
    
    Collection<OrderInstanceDetails> testOrders = new ArrayList<OrderInstanceDetails>();
    for (int i=0; i<TESTORDER_ID.length; i++) {
      OrderInstanceDetails testOrder = new OrderInstanceDetails(TESTORDER_ID[i]);
      testOrder.setOrderType(TESTORDER_ORDERTYPE[i]);
      testOrder.setMonitoringLevel(TESTORDER_MONITORINGLEVEL[i]);
      testOrder.setStopTime(TESTORDER_STOPTIME[i]);
      testOrders.add(testOrder);
    }
    
    ODSConnection con = ods.openConnection();
    try {
      con.persistCollection(testOrders);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    //do they exist?
    con = ods.openConnection();
    try {
      for (int i=0; i<TESTORDER_ID.length; i++) { 
        assertEquals(true, con.containsObject(new OrderInstanceDetails(TESTORDER_ID[i])));
      }
    } finally {
      con.closeConnection();
    }
    
    // do the values match
    con = ods.openConnection();
    Collection<OrderInstanceDetails> col = null;
    try {
      col = con.loadCollection(OrderInstanceDetails.class);
    } finally {
      con.closeConnection();
    }   
    assertNotNull(col);   
    assertEquals(TESTORDER_ID.length, col.size());
    for (OrderInstanceDetails oiLoaded : col) {
      for (int i=0; i<TESTORDER_ID.length; i++) {
        if (oiLoaded.getId() == TESTORDER_ID[i]) {
          assertEquals(TESTORDER_ORDERTYPE[i], oiLoaded.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[i], oiLoaded.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[i], oiLoaded.getStopTime());
        }
      }       
    }
    
    con = ods.openConnection();
    try {
      con.deleteAll(OrderInstanceDetails.class);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    con = ods.openConnection();
    try {
      for (int i=0; i<TESTORDER_ID.length; i++) { 
        assertEquals(false, con.containsObject(new OrderInstanceDetails(TESTORDER_ID[i])));
      }
    } finally {
      con.closeConnection();
    }
  }
  
  
  // have fun with ResultNodes TODO Test won't work without access to package
  public void testResultNodes() throws PersistenceLayerException {
    //we can't test negations until we are able to pass an 'allSet' into negated holders
    
    // ({1,2,3} && {2,3,4}) || ({5,6,7} && {7,8,9}) || {10} ==> {2,3,7,10}
    ResultNode root = new ResultNode();
    ResultNodeHolder testHolder = new ResultNodeHolder(null);
    root.setNext(testHolder);
    
    ResultNode testNode1 = new ResultNode();
    Set<String> set1 = new HashSet<String>();
    set1.add("1");set1.add("2");set1.add("3");
    testNode1.setResult(set1);
    testHolder.setContent(testNode1);
    
    testNode1.setNext(new ResultNode());
    Set<String> set2 = new HashSet<String>();
    set2.add("2");set2.add("3");set2.add("4");
    testNode1.next().setResult(set2);
    testNode1.next().setOperator(OPERATOR.AND);
    
    ResultNodeHolder testHolder2 = new ResultNodeHolder(OPERATOR.OR);
    testHolder.setNext(testHolder2);
    ResultNode testNode2 = new ResultNode();
    Set<String> set3 = new HashSet<String>();
    set3.add("5");set3.add("6");set3.add("7");
    testNode2.setResult(set3);
    testHolder2.setContent(testNode2);
    
    testNode2.setNext(new ResultNode());
    Set<String> set4 = new HashSet<String>();
    set4.add("7");set4.add("8");set4.add("9");
    testNode2.next().setResult(set4);
    testNode2.next().setOperator(OPERATOR.AND);
    
    testHolder2.setNext(new ResultNode());
    Set<String> set5 = new HashSet<String>();
    set5 = new HashSet<String>();
    set5.add("10");
    testHolder2.next().setResult(set5);
    testHolder2.next().setOperator(OPERATOR.OR);
    
    Set<String> testResult = new HashSet<String>();
    testResult.add("2");testResult.add("3");testResult.add("7");testResult.add("10");
    
    Set<String> result = root.next().process();
    assertEquals(testResult.size(), result.size());
    assertEquals(true, result.containsAll(testResult));
    
    
    // {1,2,3,4,5} && ({1,2} || {4,5}) && ({1,2,3,4}) ==> {1,2,4}
    root = new ResultNode();
    testNode1 = new ResultNode();
    root.setNext(testNode1);
    set1.clear();
    set1.add("1");set1.add("2");set1.add("3");set1.add("4");set1.add("5");
    testNode1.setResult(set1);

    testHolder = new ResultNodeHolder(OPERATOR.AND);
    testNode1.setNext(testHolder);
    testNode2 = new ResultNode();
    testHolder.setContent(testNode2);
    set2.clear();
    set2.add("1");set2.add("2");
    testNode2.setResult(set2);

    
    testNode2.setNext(new ResultNode());
    set3.clear();
    set3.add("4");set3.add("5");
    testNode2.next().setResult(set3);
    testNode2.next().setOperator(OPERATOR.OR);
    
    testHolder2 = new ResultNodeHolder(OPERATOR.AND);
    testHolder.setNext(testHolder2);
    ResultNode testNode4 = new ResultNode();
    testHolder2.setContent(testNode4);
    set4.clear();
    set4.add("1");set4.add("2");set4.add("3");set4.add("4");
    testNode4.setResult(set4);
    
    testResult.clear();
    testResult.add("1");testResult.add("2");testResult.add("4");
    
    result = root.next().process();
    assertEquals(testResult.size(), result.size());
    assertEquals(true, result.containsAll(testResult));
    
  }
  

  // Test Querys with OrderInstanceSelect
  public void testQueries() throws PersistenceLayerException, XNWH_InvalidSelectStatementException {
    final long[] TESTORDER_ID = {1l, 22l, 333l, 4444l, 5555l, 66666l, 7777777l, 88888888l, 999999999l, 1000000000l, 1000000001l};
    final String[] TESTORDER_ORDERTYPE = {"de.test.ordertype", "com.test.ordertype", "com.test.ordertype", "xmcp.manualinteraction.ManualInteraction", "com.test.ordertype", "de.test.ordertype", "com.gip.xyna.Cancel", "ManualInteractionRedirection", "com.test.ordertype", "de.test.ordertype", "de.test.ordertype"};
    final int[] TESTORDER_MONITORINGLEVEL = {5, 20, 20, 15, 10, 5, 0, 20, 10, 5, 15};
    final long[] TESTORDER_STOPTIME = {1000000001l, 1000000002l, 1000000003l, 1000000004l, 1000000005l, 1000000006l, 1000000007l, 1000000008l, 1000000009l, 1000000010l, 1000000011l};
    
    Collection<OrderInstanceDetails> testOrders = new ArrayList<OrderInstanceDetails>();
    for (int i=0; i<TESTORDER_ID.length; i++) {
      OrderInstanceDetails testOrder = new OrderInstanceDetails(TESTORDER_ID[i]);
      testOrder.setOrderType(TESTORDER_ORDERTYPE[i]);
      testOrder.setMonitoringLevel(TESTORDER_MONITORINGLEVEL[i]);
      testOrder.setStopTime(TESTORDER_STOPTIME[i]);
      testOrders.add(testOrder);
    }
    
    ODSConnection con = ods.openConnection();
    try {
      con.persistCollection(testOrders);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    OrderInstanceSelect ois = new OrderInstanceSelect();
    //                                                                         7      and                                    4              ==> 2 (#9,10)
    ois.selectAllForOrderInstance().selectStopTime().whereId().isBiggerThan(7000000l).and().whereOrderType().isEqual("de.test.ordertype");
    
    con = ods.openConnection();
    try {
      PreparedQuery<OrderInstance> prep= con.prepareQuery(new Query<OrderInstance>(ois.getSelectString(), ois.getReader()));
      List<OrderInstance> loadedOrders = new ArrayList<OrderInstance>();
      loadedOrders = con.query(prep, ois.getParameter(), 100);
      assertEquals(2, loadedOrders.size());
      for (OrderInstance oi : loadedOrders) {
        if (oi.getId() == TESTORDER_ID[9]) {
          assertEquals(TESTORDER_ORDERTYPE[9], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[9], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[9], oi.getStopTime());
        } else if (oi.getId() == TESTORDER_ID[10]) {
          assertEquals(TESTORDER_ORDERTYPE[10], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[10], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[10], oi.getStopTime());
        } else {
          fail("unexpected id in search result");
        }
      }
    } finally {
      con.closeConnection();
    }
    
    ois = new OrderInstanceSelect();
    //default query from GUI, filter all intern WFs     7     ==> #0,1,2,4,5,8,9,10
    ois.selectAllForOrderInstance().selectStopTime().whereNot(ois.newWC().whereOrderType().isEqual("com.gip.xyna.Cancel").or()
                                                              .whereOrderType().isEqual("ManualInteractionRedirection").or()
                                                              .whereOrderType().isEqual("com.gip.xyna.ResumeOrder").or()
                                                              .whereOrderType().isEqual("com.gip.xyna.ResumeMultipleOrders").or()
                                                              .whereOrderType().isEqual("com.gip.xyna.SuspendAllOrders").or()
                                                              .whereOrderType().isEqual("com.gip.xyna.SuspendOrder").or()
                                                              .whereOrderType().isEqual("xmcp.manualinteraction.ManualInteraction")
                                                              );
    con = ods.openConnection();
    try {
      PreparedQuery<OrderInstance> prep= con.prepareQuery(new Query<OrderInstance>(ois.getSelectString(), ois.getReader()));
      List<OrderInstance> loadedOrders = new ArrayList<OrderInstance>();
      loadedOrders = con.query(prep, ois.getParameter(), 100);
      
      for (OrderInstance oiLoaded : loadedOrders) {
        for (int i = 0; i<TESTORDER_ID.length; i++) {
          if (oiLoaded.getId() == TESTORDER_ID[i]) {
            if (oiLoaded.getId() == 3 || oiLoaded.getId() == 6 || oiLoaded.getId() == 7) { //you are not supposed to be in here
              fail("Intern orderTypes found while excluding them");
            }
            assertEquals(TESTORDER_ORDERTYPE[i], oiLoaded.getOrderType());
            assertEquals(TESTORDER_MONITORINGLEVEL[i], oiLoaded.getMonitoringLevel());
            assertEquals(TESTORDER_STOPTIME[i], oiLoaded.getStopTime());
          }
        }
      }
    } finally {
      con.closeConnection();
    }

    ois = new OrderInstanceSelect();
    //                                                                                     (    8    and                   3 ==> 2) or (                                         5         and                                5 ==> 2) ==> 4 (1,2,9,10)  
    ois.selectAllForOrderInstance().selectStopTime().where(ois.newWC().whereId().isBiggerThan(4000l).and().whereId().isLike("1*")).or().where(ois.newWC().whereOrderType().isLike("com*").and().whereMonitoringLevel().isBiggerThan(10));
    con = ods.openConnection();
    try {
      PreparedQuery<OrderInstance> prep= con.prepareQuery(new Query<OrderInstance>(ois.getSelectString(), ois.getReader()));
      List<OrderInstance> loadedOrders = new ArrayList<OrderInstance>();
      loadedOrders = con.query(prep, ois.getParameter(), 100);
      
      for (OrderInstance oi : loadedOrders) {
        if (oi.getId() == TESTORDER_ID[9]) {
          assertEquals(TESTORDER_ORDERTYPE[9], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[9], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[9], oi.getStopTime());
        } else if (oi.getId() == TESTORDER_ID[10]) {
          assertEquals(TESTORDER_ORDERTYPE[10], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[10], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[10], oi.getStopTime());
        } else if (oi.getId() == TESTORDER_ID[1]) {
          assertEquals(TESTORDER_ORDERTYPE[1], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[1], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[1], oi.getStopTime());
        } else if (oi.getId() == TESTORDER_ID[2]) {
          assertEquals(TESTORDER_ORDERTYPE[2], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[2], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[2], oi.getStopTime());
        } else {
          fail("unexpected id in search result");
        }
      }
    } finally {
      con.closeConnection();
    }
    
    
    con = ods.openConnection();
    try {                                                                                                                                            //       8 and     6    ==> 3 (#3,4,5)
                                                                                                                                                     //      4000    7000000
      PreparedQuery<OrderInstance> prep= con.prepareQuery(new Query<OrderInstance>("select id, stoptime, monitoringlevel, ordertype from orderarchive where id > ? and id < ?", ois.getReader()));
      List<OrderInstance> loadedOrders = new ArrayList<OrderInstance>();
      Parameter param = new Parameter(4000, 7000000);
      loadedOrders = con.query(prep, param, 100);
      
      for (OrderInstance oi : loadedOrders) {
        if (oi.getId() == TESTORDER_ID[3]) {
          assertEquals(TESTORDER_ORDERTYPE[3], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[3], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[3], oi.getStopTime());
        } else if (oi.getId() == TESTORDER_ID[4]) {
          assertEquals(TESTORDER_ORDERTYPE[4], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[4], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[4], oi.getStopTime());
        } else if (oi.getId() == TESTORDER_ID[5]) {
          assertEquals(TESTORDER_ORDERTYPE[5], oi.getOrderType());
          assertEquals(TESTORDER_MONITORINGLEVEL[5], oi.getMonitoringLevel());
          assertEquals(TESTORDER_STOPTIME[5], oi.getStopTime());
        } else {
          fail("unexpected id in search result");
        }
      }
    } finally {
      con.closeConnection();
    }
    
    con = ods.openConnection();
    try {
      con.deleteAll(OrderInstanceDetails.class);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  
  public final OrderInstanceDetails victim = new OrderInstanceDetails(666l);
  
  // Test Concurrent Access (with Connections to History & Default )
  public void testConcurrentAccess() throws InterruptedException {
    // reads waiting for a sleeping write
    WriteRunnable writeRun = new WriteRunnable(ODSConnectionType.DEFAULT, 3000);
    Thread write = new Thread(writeRun);
    write.start();
    
    Thread.sleep(250);
    
    ReadRunnable readRun1 = new ReadRunnable(ODSConnectionType.DEFAULT);
    Thread read1 = new Thread(readRun1);
    read1.start();
    ReadRunnable readRun2 = new ReadRunnable(ODSConnectionType.DEFAULT);
    Thread read2 = new Thread(readRun2);
    read2.start();
    ReadRunnable readRun3 = new ReadRunnable(ODSConnectionType.DEFAULT);
    Thread read3 = new Thread(readRun3);
    read3.start();
    
    assertEquals(false, writeRun.isFinished());
    assertEquals(false, readRun1.isFinished());
    assertEquals(false, readRun2.isFinished());
    assertEquals(false, readRun3.isFinished());
    
    Thread.sleep(3500);
    
    assertEquals(true, writeRun.isFinished());
    assertEquals(true, readRun1.isFinished());
    assertEquals(true, readRun2.isFinished());
    assertEquals(true, readRun3.isFinished());
    
    assertEquals(true, writeRun.getStopTime() <= readRun1.getStopTime());
    assertEquals(true, writeRun.getStopTime() <= readRun2.getStopTime());
    assertEquals(true, writeRun.getStopTime() <= readRun3.getStopTime());
    
    
    // write waiting for sleeping write
    writeRun = new WriteRunnable(ODSConnectionType.DEFAULT, 3000);
    write = new Thread(writeRun);
    write.start();
    
    Thread.sleep(250);
    
    WriteRunnable writeRun1 = new WriteRunnable(ODSConnectionType.DEFAULT, 3000);
    Thread write1 = new Thread(writeRun1);
    write1.start();
    WriteRunnable writeRun2 = new WriteRunnable(ODSConnectionType.DEFAULT, 3000);
    Thread write2 = new Thread(writeRun2);
    write2.start();

    assertEquals(false, writeRun.isFinished());
    assertEquals(false, writeRun1.isFinished());
    assertEquals(false, writeRun2.isFinished());
    
    Thread.sleep(3500);
    
    assertEquals(true, writeRun.isFinished());
    assertEquals(false, writeRun1.isFinished());
    assertEquals(false, writeRun2.isFinished());
    
    Thread.sleep(3000);
    
    assertEquals(true, ((writeRun1.isFinished() || writeRun2.isFinished()) && !(writeRun1.isFinished() && writeRun2.isFinished())));
    
    Thread.sleep(3000);
    
    assertEquals(true, writeRun1.isFinished());
    assertEquals(true, writeRun2.isFinished());
    
    assertEquals(true, writeRun.getStopTime() <= writeRun1.getStopTime());
    assertEquals(true, writeRun.getStopTime() <= writeRun2.getStopTime());
    
    // several read & writes to Default & History
    writeRun1 = new WriteRunnable(ODSConnectionType.DEFAULT, 6000);
    write1 = new Thread(writeRun1);
    write1.start();
    
    Thread.sleep(250);
    
    readRun1 = new ReadRunnable(ODSConnectionType.DEFAULT);
    read1 = new Thread(readRun1);
    read1.start();
    
    Thread.sleep(250);
    
    writeRun2 = new WriteRunnable(ODSConnectionType.HISTORY, 3000);
    write2 = new Thread(writeRun2);
    write2.start();
    
    Thread.sleep(250);
    
    readRun2 = new ReadRunnable(ODSConnectionType.HISTORY);
    read2 = new Thread(readRun2);
    read2.start();
    
    assertEquals(false, writeRun1.isFinished());
    assertEquals(false, writeRun2.isFinished());
    assertEquals(false, readRun1.isFinished());
    assertEquals(false, readRun2.isFinished());
    
    Thread.sleep(3500);
    
    assertEquals(false, writeRun1.isFinished());
    assertEquals(true, writeRun2.isFinished());
    assertEquals(false, readRun1.isFinished());
    assertEquals(true, readRun2.isFinished());
    
    Thread.sleep(3500);
    
    assertEquals(true, writeRun1.isFinished());
    assertEquals(true, writeRun2.isFinished());
    assertEquals(true, readRun1.isFinished());
    assertEquals(true, readRun2.isFinished());
    
    assertEquals(true, readRun1.getStopTime() >= writeRun1.getStopTime());
    assertEquals(true, writeRun1.getStopTime() >= readRun2.getStopTime());
    assertEquals(true, readRun2.getStopTime() >= writeRun2.getStopTime());
    
  }
  
  private class ReadRunnable implements Runnable {
    
    private Long stopTime = null;
    private ODSConnectionType type;
    
    public ReadRunnable(ODSConnectionType type) {
      this.type = type;
    }
    
    public void run() {
      ODSConnection con = ods.openConnection(type);
      try {
        try {
          con.queryOneRow(victim);
          this.stopTime = System.currentTimeMillis();
        } catch (PersistenceLayerException e) {
          fail(e.getMessage());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          fail(e.getMessage());
        }
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          fail(e.getMessage());
        }
      }      
    }
    
    public boolean isFinished() {
      return (this.stopTime != null);
    }
    
    public Long getStopTime() {
      return this.stopTime;
    }
  }
  
  private class WriteRunnable implements Runnable {
    
    private Long stopTime = null;
    private ODSConnectionType type;
    private long sleepTime;
    
    public WriteRunnable(ODSConnectionType type, long sleepTime) {
      this.type = type;
      this.sleepTime = sleepTime;
    }
    
    public void run() {
      ODSConnection con = ods.openConnection(type);
      try {
        con.persistObject(victim);
        
        Thread.sleep(this.sleepTime);
        this.stopTime = System.currentTimeMillis();
        con.commit();
      } catch (PersistenceLayerException e) {
        fail(e.getMessage());
      } catch (InterruptedException e) {
        fail(e.getMessage());
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          fail(e.getMessage());
        }
      }
    }
    
    public boolean isFinished() {
      return (this.stopTime != null);
    }
    
    public Long getStopTime() {
      return this.stopTime;
    }    
  }
  
  
}
