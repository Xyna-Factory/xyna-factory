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

package com.gip.xyna.xnwh.persistence;



import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.memory.XynaMemoryPersistenceLayer;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;



public class ODSConnectionTest extends TestCase {

  private static final Logger logger = CentralFactoryLogging.getLogger(ODSConnectionTest.class);

  private ODS ods;
  
  
  public void setUp() {
    if (!new File("bin").exists()) {
      if (!new File("deploy").exists()) {
        Constants.SERVER_CLASS_DIR = "deploy/xynaserver.jar";
      } else {
        Constants.SERVER_CLASS_DIR = "classes";
      }
    }
    ods = ODSImpl.getInstance(false);
    try {
      ods.registerStorable(OrderInstanceDetails.class);
      ods.registerPersistenceLayer(123, XynaMemoryPersistenceLayer.class);
      long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                        ODSConnectionType.DEFAULT, new String[0]);
      ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    } catch (XynaException e) {
      logger.error("", e);
      fail(e.getMessage());
    }
  }

  public void tearDown() {
    new File(Constants.MDM_CLASSDIR).delete();
    new File(Constants.GENERATION_DIR).delete();
    new File(XynaProperty.PERSISTENCE_DIR).delete();
    ODSImpl.clearInstances();
  }


  public void testInsert() throws XynaException {
    ODSConnection con = ods.openConnection();
    try {
      assertEquals(false, con.containsObject(new OrderInstanceDetails(1l)));
      con.persistObject(new OrderInstanceDetails(1l));
      assertEquals(true, con.containsObject(new OrderInstanceDetails(1l)));
    }
    finally {
      con.closeConnection();
    }
  }
  
  private OrderInstanceDetails newOrderInstanceDetails(long id, String orderType) {
    OrderInstanceDetails oid = new OrderInstanceDetails(id);
    oid.setOrderType(orderType);
    return oid;
  }


  public void testQuery() throws XynaException {
    ODSConnection con = ods.openConnection();
    try {
      OrderInstanceDetails wid = newOrderInstanceDetails(1l, "bla");
      wid.setLastUpdate(System.currentTimeMillis());
      wid.setStatusAsString("somestatus");
      con.persistObject(wid);
      OrderInstance wi = new OrderInstance(1l);
      con.queryOneRow(wi);
      assertEquals("bla", wi.getOrderType());
      assertEquals("somestatus", wi.getStatusAsString());
    }
    finally {
      con.closeConnection();
    }
  }


  public void testQueryMore() throws XynaException {
    ODSConnection con = ods.openConnection();
    try {
      OrderInstanceDetails wid = newOrderInstanceDetails(1l, "bla");
      wid.setLastUpdate(System.currentTimeMillis());
      wid.setStatusAsString("somestatus");
      con.persistObject(wid);
      OrderInstance wi = new OrderInstance(1l);
      con.queryOneRow(wi);
      assertEquals("bla", wi.getOrderType());
      assertEquals("somestatus", wi.getStatusAsString());
    }
    finally {
      con.closeConnection();
    }
    long t0 = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      con = ods.openConnection();
      try {
        OrderInstanceDetails wid = newOrderInstanceDetails((long) i, "bla");
        wid.setLastUpdate(System.currentTimeMillis());
        wid.setStatusAsString("somestatus");
        con.persistObject(wid);
        OrderInstance wi = new OrderInstance((long) i);
        con.queryOneRow(wi);
      }
      finally {
        con.closeConnection();
      }
    }
    long needed = System.currentTimeMillis() - t0;
    t0 = System.currentTimeMillis();
    ArrayList<Object> bla = new ArrayList<Object>();
    for (int i = 0; i < 10000; i++) {
      bla.add(new Object());
      try {
        OrderInstanceDetails wid = newOrderInstanceDetails((long) i, "bla");
        wid.setLastUpdate(System.currentTimeMillis());
        wid.setStatusAsString("somestatus");
        bla.add(wid);
        bla.add(new Object());
        OrderInstance wi = new OrderInstance((long) i);
        bla.add(wi);
        bla.add(new Object());
      }
      finally {
        bla.add(new Object());
      }
    }
    bla.clear();
    long needed2 = System.currentTimeMillis() - t0;
    logger.debug("needed1=" + needed + " needed2=" + needed2 + " " + con);
  }


  public void testQueryWithGeneratedCode() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      for (int i = 0; i < 100; i++) {
        OrderInstanceDetails wid = newOrderInstanceDetails((long) i, "bla");
        wid.setLastUpdate(System.currentTimeMillis());
        wid.setStatusAsString("somestatus");
        con.persistObject(wid);
      }
      OrderInstance wi = con.queryOneRow(con
                      .prepareQuery(new Query<OrderInstance>("select * from orderarchive where id=?",
                                                             new ResultSetReader<OrderInstance>() {

                                                               public OrderInstance read(ResultSet rs)
                                                                               throws SQLException {
                                                                 assertEquals(17l, rs.getLong("id"));
                                                                 return new OrderInstance(13l);
                                                               }

                                                             }, "orderarchive")), new Parameter(17l));
      assertEquals(13l, (long) wi.getId());
    } finally {
      con.closeConnection();
    }
  }


  public void testQuerySeveralWithGeneratedCode() {
    try {
      ODSConnection con = ods.openConnection();
      PreparedQuery<OrderInstance> preparedQuery = con
                      .prepareQuery(new Query<OrderInstance>("select * from orderarchive where ("
                                      + OrderInstanceColumn.C_ID.getColumnName() + " > 15 and "
                                      + OrderInstanceColumn.C_ID.getColumnName() + " < 22) or "
                                      + OrderInstanceColumn.C_ORDER_TYPE.getColumnName() + "='bla88'",
                                                             new OrderInstance(1l).getReader(),
                                                             "orderarchive"));
      try {
        for (int i = 0; i < 100; i++) {
          OrderInstanceDetails wid = newOrderInstanceDetails((long) i, "bla" + i);
          wid.setLastUpdate(System.currentTimeMillis());
          wid.setStatusAsString("somestatus");
          con.persistObject(wid);
        }
        List<OrderInstance> list = con.query(preparedQuery, new Parameter(17l), -1);
        assertEquals(7, list.size());
        HashSet<Long> ids = new HashSet<Long>();
        for (OrderInstance wi : list) {
          ids.add(wi.getId());
        }
        assertTrue(ids.contains(16l));
        assertTrue(ids.contains(17l));
        assertTrue(ids.contains(18l));
        assertTrue(ids.contains(19l));
        assertTrue(ids.contains(20l));
        assertTrue(ids.contains(21l));
        assertTrue(ids.contains(88l));
      }
      finally {
        con.closeConnection();
      }
    }
    catch (XynaException e) {
      logger.error("failed because", e);
      fail("error occured during test: " + e.getMessage());
    }
  }


  public void testPersistObjectDifferentObject() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      OrderInstance wi = new OrderInstance(3l);
      wi.setOrderType("bla");
      wi.setStatusAsString("status");
      //, 6l, ExecutionType.UNKOWN);
      PersistenceLayerException e = null;
      try {
        con.persistObject(wi);
      }
      catch (PersistenceLayerException e1) {
        e = e1;
      }
      assertNotNull(e);
      assertTrue(e.getMessage().contains("invalid object"));
    }
    finally {
      con.closeConnection();
    }
  }


  public void testThreadSafetyWriteLock() throws PersistenceLayerException {
    // thread1 macht ein update, aber kein commit.
    // thread2 liest => sollte blockieren
    // thread1 committed => thread2 bekommt freigabe.
    ODSConnection con1 = ods.openConnection();
    try {
      OrderInstanceDetails wid = newOrderInstanceDetails((long) 7, "bla");
      wid.setLastUpdate(System.currentTimeMillis());
      wid.setStatusAsString("somestatus");
      final OrderInstance wi = new OrderInstance(7l);
      con1.persistObject(wid);
      Thread t = new Thread() {

        public void run() {
          ODSConnection con2 = ods.openConnection();
          try {
            con2.queryOneRow(wi);
          }
          catch (XynaException e) {
            fail(e.getMessage());
          }

        }
      };
      t.start();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        fail(e.getMessage());
      }
      // nun hat der andere thread zeit gehabt. darf aber noch nichts gelesen haben
      assertNotNull(wi.getStatusAsString());
      con1.commit();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        fail(e.getMessage());
      }
      // jetzt konnte er auslesen
      assertEquals("somestatus", wi.getStatusAsString());
    }
    finally {
      con1.closeConnection();
    }
  }


  public void testThreadSafetyReadLock() throws PersistenceLayerException {
    // thread 1 liest eine zeile,
    // thread 2 darf auch gleichzeitig lesen
    // thread 3 will writelock und muss warten
    ODSConnection con1 = ods.openConnection();
    try {
      final OrderInstanceDetails wid = newOrderInstanceDetails((long) 7, "bla");
      wid.setLastUpdate(System.currentTimeMillis());
      wid.setStatusAsString("somestatus");
      con1.persistObject(wid);
      con1.commit();
      OrderInstance wi = con1.queryOneRow(con1
                      .prepareQuery(new Query<OrderInstance>("select * from orderarchive where id = 7",
                                                                new ResultSetReader<OrderInstance>() {

                                                                  public OrderInstance read(ResultSet rs)
                                                                                  throws SQLException {
                                                                    // jetzt hat man readlock.


                                                                    Thread t = new Thread() {

                                                                      public void run() {
                                                                        ODSConnection con3 = ods.openConnection();
                                                                        OrderInstanceDetails wid2 = newOrderInstanceDetails(
                                                                                                                                   7l,
                                                                                                                                   "bla");
                                                                        wid2.setStatusAsString("blubb");
                                                                        try {
                                                                          con3.persistObject(wid2); // sollte blockieren
                                                                          con3.commit();
                                                                        }
                                                                        catch (PersistenceLayerException e) {
                                                                          logger.error(null, e);
                                                                          fail(e.getMessage());
                                                                        }
                                                                      }
                                                                    };
                                                                    t.start();
                                                                    try {
                                                                      Thread.sleep(1000);
                                                                    }
                                                                    catch (InterruptedException e) {
                                                                      fail(e.getMessage());
                                                                    }


                                                                    final OrderInstance w = new OrderInstance(7l);
                                                                    t = new Thread() {

                                                                      public void run() {
                                                                        ODSConnection con2 = ods.openConnection();
                                                                        try {
                                                                          con2.queryOneRow(w);
                                                                        }
                                                                        catch (XynaException e) {
                                                                          logger.error(null, e);
                                                                          fail(e.getMessage());
                                                                        }
                                                                      }
                                                                    };
                                                                    t.start();
                                                                    try {
                                                                      Thread.sleep(1000);
                                                                    }
                                                                    catch (InterruptedException e) {
                                                                      fail(e.getMessage());
                                                                    }
                                                                    // sollte nicht blockieren und neuer status ist noch
                                                                    // nicht gesetzt.
                                                                    assertEquals("somestatus", w.getStatusAsString());
                                                                    assertEquals(7l, (long) w.getId());

                                                                    return new OrderInstance(rs.getLong("id"));
                                                                  }

                                                                }, "orderarchive")), new Parameter());
      assertEquals(7l, (long) wi.getId());

      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        fail(e.getMessage());
      }
      // jetzt geht auch das schreiben, deshalb hat die nächste abfrage den aktuellen wert

      final OrderInstance w = new OrderInstance(7l);
      Thread t = new Thread() {

        public void run() {
          ODSConnection con2 = ods.openConnection();
          try {
            con2.queryOneRow(w);
          }
          catch (XynaException e) {
            logger.error(null, e);
            fail(e.getMessage());
          }
        }
      };
      t.start();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        fail(e.getMessage());
      }
      assertEquals("blubb", w.getStatusAsString());

    }
    finally {
      con1.closeConnection();
    }
  }


  /**
   * bei abgeleiteten klassen muss das fillobject korrekt funktinoieren
   * @throws PersistenceLayerException
   * @throws ObjectNotFoundException
   */
  public void testExtendedStorable() throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection();
    try {
      OrderInstanceDetails wid = newOrderInstanceDetails((long) 7, "bla");
      wid.setLastUpdate(System.currentTimeMillis());
      wid.setStatusAsString("somestatus");
      wid.setLastUpdate(123l);
      con.persistObject(wid);
      wid = newOrderInstanceDetails(7, "bla");
      con.queryOneRow(wid);
      assertEquals(123, wid.getLastUpdate());
    } finally {
      con.closeConnection();
    }
  }

}
