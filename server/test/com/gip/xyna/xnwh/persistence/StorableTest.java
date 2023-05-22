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

import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xsched.SchedulerBean;


public class StorableTest extends TestCase {


  private static final Logger logger = Logger.getLogger(Storable.class);


  @Override
  public void setUp() throws XynaException {
    IDGenerator idGenerator = EasyMock.createMock(IDGenerator.class);
    AtomicLong longGenerator = new AtomicLong(1);
    EasyMock.expect(idGenerator.getUniqueId()).andReturn(longGenerator.getAndIncrement());
    EasyMock.expect(idGenerator.getUniqueId()).andReturn(longGenerator.getAndIncrement());
    EasyMock.replay(idGenerator);
    IDGenerator.setInstance(idGenerator);
  }


  @Override
  public void tearDown() {
    IDGenerator.setInstance(null);
  }


  public void testGetPersistable() {
    //OrderInstanceDetails oid = new OrderInstanceDetails(new XynaOrder(new DestinationKey("ordertype"), new SchedulerBean()));
    Persistable s = Storable.getPersistable(OrderInstanceDetails.class);
    assertEquals("orderarchive", s.tableName());
    assertEquals("id", s.primaryKey());
  }
  
  public void testGetColumns() throws XynaException {
    OrderInstanceDetails oid = new OrderInstanceDetails(new XynaOrder(new DestinationKey("ordertype"), new SchedulerBean()));
    Column[] cols = oid.getColumns();
    assertEquals(18, cols.length);
    OrderInstance oi = new OrderInstance(new XynaOrder(new DestinationKey("ordertype"), new SchedulerBean()));
    cols = oi.getColumns();
    assertEquals(16, cols.length);
    assertEquals(ColumnType.INHERIT_FROM_JAVA, cols[0].type());
  }
  
  public void testGetFieldValue() throws XynaException {
    OrderInstanceDetails oid = new OrderInstanceDetails(new XynaOrder(new DestinationKey("myordertype"), new SchedulerBean()));
    Column[] cols = oid.getColumns();
    boolean foundOrderTypeCol = false;
    for (int i = 0; i<cols.length; i++) {
      if (cols[i].name().equals(OrderInstanceColumn.C_ORDER_TYPE.toString())) {
        Object o = oid.getValueByColName(cols[i]);
        assertEquals("myordertype", o);
        foundOrderTypeCol = true;
      }
    }
    assertTrue(foundOrderTypeCol);
    oid.setOrderType("myordertype2");
    for (int i = 0; i<cols.length; i++) {
      if (cols[i].name().equals(OrderInstanceColumn.C_ORDER_TYPE.toString())) {
        Object o = oid.getValueByColName(cols[i]);
        assertEquals("myordertype2", o);
      }
    }
    long count = getGoodLoopCountForComputerSpeed();
    logger.debug("count = " + count);
    //performance ?
    for (int i = 0; i<cols.length; i++) {
      if (cols[i].name().equals(OrderInstanceColumn.C_ORDER_TYPE.toString())) {
        long t = System.currentTimeMillis();
        for (int j = 0; j<count; j++) {
          Object o = oid.getValueByColName(cols[i]);
          assertEquals("myordertype2", o);
        }
        long diff1 = System.currentTimeMillis() - t;
        t = System.currentTimeMillis();
        for (int j = 0; j<count; j++) {
          //ein equals zus�tzlich zu
          if (cols[i].name().equals("orderType")) {
            Object o = oid.getOrderType();          
            assertEquals("myordertype2", o);
          }
        }
        long diff2 = System.currentTimeMillis() - t;
        double relation = 1.0 * diff1 / diff2;
        System.out.println(relation + " " + diff1 + " " + diff2);
        assertTrue(relation < 3); //maximal drittel so schnell. erfahrungswerte liegen zwischen 1 und 2
      }
    }
  }
  
  private long getGoodLoopCountForComputerSpeed() {
    long diff = 0;
    long testcount = 10;
    while (diff < 500) {
      testcount *= 2;
      long t = System.currentTimeMillis();
      for (int i = 0; i<testcount; i++) {
        //ein equals zus�tzlich zu
        if ("orderType".equals("orderType")) {
          Object o = new String("myordertype2");          
          assertEquals("myordertype2", o);
        }
      }
      diff = System.currentTimeMillis() - t;
    }
    return testcount;
  }

}
