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

package com.gip.xyna.xnwh.persistence.memory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import junit.framework.TestCase;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;



public abstract class MemoryPersistenceLayerTransactionTestCase extends TestCase {


  public abstract PersistenceLayer getPersistenceLayer();


  public void test_01_InsertAndRollback() throws PersistenceLayerException {

    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

    TestStorable testEntry = new TestStorable(123L, 456L);

    connection.persistObject(testEntry);

    TestStorable otherTestEntry = new TestStorable(123L);
    try {
      connection.queryOneRow(otherTestEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      fail ("Uncommitted object not found in same transaction");
    }
    assertEquals(otherTestEntry.getTrivialId(), testEntry.getTrivialId());

    connection.rollback();

    boolean rollbackSuccessful = false;
    try {
      connection.queryOneRow(testEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
      rollbackSuccessful = true;
    }

    if (!rollbackSuccessful) {
      fail("Rollback failed.");
    }

    rollbackSuccessful = false;
    connection = getPersistenceLayer().getConnection();
    try {
      connection.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      rollbackSuccessful = true;
    }

    if (!rollbackSuccessful) {
      fail("Rollback failed.");
    }

  }


  public void test_02_UpdateAndRollback() throws PersistenceLayerException {

    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

    TestStorable testEntry = new TestStorable();
    testEntry.setId(123L);
    testEntry.setTrivialId(456L);

    connection.persistObject(testEntry);
    connection.commit();
    connection.closeConnection();
    testEntry = null;

    connection = getPersistenceLayer().getConnection();

    try {
      connection.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("Object not found after commit");
    }

    TestStorable overwritingTestEntry = new TestStorable(123L, 789L);
    connection.persistObject(overwritingTestEntry);
    TestStorable readEntry = new TestStorable(123L);
    try {
      connection.queryOneRow(readEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      fail ("Object got lost during update");
    }
    assertEquals(789L, readEntry.getTrivialId());

    connection.rollback();

    TestStorable newTestEntry = new TestStorable(123L);
    try {
      connection.queryOneRow(newTestEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      fail ("Object got lost during rollback");
    }
    assertEquals(456L, newTestEntry.getTrivialId());

    connection.commit();
    connection.closeConnection();

  }


  public void test_03_TransactionVisibility1() throws PersistenceLayerException {

    {
      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable();
      testEntry.setId(123L);
      testEntry.setTrivialId(456L);

      connection.persistObject(testEntry);
      connection.commit();
      connection.closeConnection();
    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    TestStorable overwritingTestEntry = new TestStorable(123L, 789L);
    connection1.persistObject(overwritingTestEntry);


    PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    TestStorable readEntry = new TestStorable(123L);
    try {
      connection2.queryOneRow(readEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      fail("Object got lost during update");
    }
    assertEquals(456L, readEntry.getTrivialId());


    connection1.rollback();


    try {
      connection2.queryOneRow(readEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      fail("Object got lost during update");
    }
    assertEquals(456L, readEntry.getTrivialId());


    connection1.persistObject(overwritingTestEntry);
    connection1.commit();


    try {
      connection2.queryOneRow(readEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      fail("Object got lost during update");
    }
    assertEquals(789L, readEntry.getTrivialId());

  }


  public void  test_04_TransactionVisibility2() throws PersistenceLayerException {
    
    {

      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable(123L, 456L);
      connection.persistObject(testEntry);

      testEntry = new TestStorable(234L, 890L);
      connection.persistObject(testEntry);

      connection.commit();
      connection.closeConnection();

    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    PreparedQuery<TestStorable> preparedQuery =
        connection1.prepareQuery(new Query<TestStorable>("select * from " + TestStorable.TABLE_NAME + " where "
            + TestStorable.COL_ID + " = ?", TestStorable.reader));

    List<TestStorable> queryResult = connection1.query(preparedQuery, new Parameter(123L), -1);

    assertEquals(456L, queryResult.get(0).getTrivialId());

    // overwrite the field trivial id with a different storable
    TestStorable overwritingTestEntry = new TestStorable(123L, 789L);
    connection1.persistObject(overwritingTestEntry);

    queryResult = connection1.query(preparedQuery, new Parameter(123L), -1);

    assertEquals(789, queryResult.get(0).getTrivialId());

    PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    List<TestStorable> queryResult2 = connection2.query(preparedQuery, new Parameter(123L), -1);
    assertEquals(456L, queryResult2.get(0).getTrivialId());

    // rollback in connection1
    connection1.rollback();


    queryResult2 = connection2.query(preparedQuery, new Parameter(123L), -1);
    assertEquals(456L, queryResult2.get(0).getTrivialId());


    // erneut update in connection1 und commit
    TestStorable overwritingTestEntry2 = new TestStorable(123L, 7890L);
    connection1.persistObject(overwritingTestEntry2);
    connection1.commit();


    queryResult2 = connection2.query(preparedQuery, new Parameter(123L), -1);
    assertEquals(7890L, queryResult2.get(0).getTrivialId());


    // delete in connection1 ohne commit
    connection1.deleteOneRow(new TestStorable(123L));


    queryResult2 = connection2.query(preparedQuery, new Parameter(123L), -1);
    assertEquals(7890L, queryResult2.get(0).getTrivialId());


    // rollback in connection1 sollte keinen Einfluss auf connection2 haben
    connection1.rollback();


    queryResult2 = connection2.query(preparedQuery, new Parameter(123L), -1);
    assertEquals(7890L, queryResult2.get(0).getTrivialId());


    // now delete in connection1 and commit
    connection1.deleteOneRow(new TestStorable(123L));
    connection1.commit();


    // expect the entry to be gone
    queryResult2 = connection2.query(preparedQuery, new Parameter(123L), -1);
    assertEquals(0, queryResult2.size());

  }


  public void test_05_SelectForUpdate1() throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY,
      InterruptedException {

    {
      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable();
      testEntry.setId(123L);
      testEntry.setTrivialId(456L);

      connection.persistObject(testEntry);
      connection.commit();
      connection.closeConnection();
    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    TestStorable readForUpdateEntry = new TestStorable(123L);
    connection1.queryOneRowForUpdate(readForUpdateEntry);


    final AtomicBoolean secondThreadFinished = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();
    Thread t = new Thread(new Runnable() {

      public void run() {

        TestStorable readForUpdateEntry2 = new TestStorable(123L);
        try {
          PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
          connection2.queryOneRow(readForUpdateEntry2);
          secondThreadFinished.set(true);
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (!secondThreadFinished.get()) {
      fail("second thread did not finish in time");
    }

  }


  public void test_06_SelectForUpdate2() throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY,
      InterruptedException {

    {
      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable();
      testEntry.setId(123L);
      testEntry.setTrivialId(456L);

      connection.persistObject(testEntry);
      connection.commit();
      connection.closeConnection();
    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    TestStorable readForUpdateEntry = new TestStorable(123L);
    connection1.queryOneRowForUpdate(readForUpdateEntry);


    final AtomicBoolean secondThreadFinished = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();
    Thread t = new Thread(new Runnable() {

      public void run() {

        PersistenceLayerConnection connection2;
        try {
          connection2 = getPersistenceLayer().getConnection();
        } catch (PersistenceLayerException e1) {
          throw new RuntimeException(e1);
        }

        TestStorable readForUpdateEntry2 = new TestStorable(123L);
        try {
          connection2.queryOneRowForUpdate(readForUpdateEntry2);
          secondThreadFinished.set(true);
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (secondThreadFinished.get()) {
      fail("second thread should not have finish before commit");
    }

    connection1.commit();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (!secondThreadFinished.get()) {
      fail("second thread did not finish after commit");
    }

  }


  public void test_07_SelectForUpdate3() throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY,
      InterruptedException {

    {
      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable();
      testEntry.setId(123L);
      testEntry.setTrivialId(456L);

      connection.persistObject(testEntry);
      connection.commit();
      connection.closeConnection();
    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    TestStorable readForUpdateEntry = new TestStorable(123L, 9999L);
    connection1.persistObject(readForUpdateEntry);


    final AtomicBoolean secondThreadFinished = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();
    Thread t = new Thread(new Runnable() {

      public void run() {

        PersistenceLayerConnection connection2;
        try {
          connection2 = getPersistenceLayer().getConnection();
        } catch (PersistenceLayerException e1) {
          throw new RuntimeException(e1);
        }

        TestStorable readForUpdateEntry2 = new TestStorable(123L);
        try {
          connection2.queryOneRowForUpdate(readForUpdateEntry2);
          secondThreadFinished.set(true);
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (secondThreadFinished.get()) {
      fail("second thread should not have finish before commit");
    }

    connection1.commit();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (!secondThreadFinished.get()) {
      fail("second thread did not finish after commit");
    }

  }


  public void test_08_SelectForUpdate4() throws PersistenceLayerException, InterruptedException {

    {

      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
      connection.addTable(TestStorable.class, false, null);

      TestStorable testEntry = new TestStorable(123L, 456L);
      connection.persistObject(testEntry);

      testEntry = new TestStorable(234L, 890L);
      connection.persistObject(testEntry);

      connection.commit();
      connection.closeConnection();

    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    PreparedQuery<TestStorable> preparedQuery =
        connection1.prepareQuery(new Query<TestStorable>("select * from " + TestStorable.TABLE_NAME + " where "
            + TestStorable.COL_TRIVIAL_ID + " = ? for update", TestStorable.reader));

    List<TestStorable> queryResult = connection1.query(preparedQuery, new Parameter(456L), -1);


    final AtomicBoolean secondThreadFinished = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();
    Thread t = new Thread(new Runnable() {

      public void run() {

        PersistenceLayerConnection connection2;
        try {
          connection2 = getPersistenceLayer().getConnection();
        } catch (PersistenceLayerException e1) {
          throw new RuntimeException(e1);
        }

        TestStorable readForUpdateEntry2 = new TestStorable(123L);
        try {
          connection2.queryOneRowForUpdate(readForUpdateEntry2);
          secondThreadFinished.set(true);
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (secondThreadFinished.get()) {
      fail("second thread should not have finish before commit");
    }

    connection1.commit();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (!secondThreadFinished.get()) {
      fail("second thread did not finish after commit");
    }

  }


  public void test_09_SelectForUpdate5() throws PersistenceLayerException, InterruptedException {

    {

      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable(123L, 456L);
      connection.persistObject(testEntry);

      testEntry = new TestStorable(234L, 890L);
      connection.persistObject(testEntry);

      connection.commit();
      connection.closeConnection();

    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    PreparedQuery<TestStorable> preparedQuery =
        connection1.prepareQuery(new Query<TestStorable>("select * from " + TestStorable.TABLE_NAME + " where "
            + TestStorable.COL_TRIVIAL_ID + " = ? for update", TestStorable.reader));

    List<TestStorable> queryResult = connection1.query(preparedQuery, new Parameter(456L), -1);


    final AtomicBoolean secondThreadFinished = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();
    Thread t = new Thread(new Runnable() {

      public void run() {

        TestStorable readForUpdateEntry2 = new TestStorable(123L);
        try {
          PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
          connection2.queryOneRowForUpdate(readForUpdateEntry2);
          assertEquals(readForUpdateEntry2.getTrivialId(), 958L);
          secondThreadFinished.set(true);
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (secondThreadFinished.get()) {
      fail("second thread should not have finish before commit");
    }

    connection1.persistObject(new TestStorable(123L, 958L));

    connection1.commit();

    Thread.sleep(100);

    if (secondThreadFailed.get()) {
      fail("second thread failed");
    } else if (!secondThreadFinished.get()) {
      fail("second thread did not finish after commit");
    }

  }


  public void test_10_Delete1() throws PersistenceLayerException {

    {

      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable(123L, 456L);
      connection.persistObject(testEntry);

      testEntry = new TestStorable(234L, 890L);
      connection.persistObject(testEntry);

      connection.commit();
      connection.closeConnection();

    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    connection1.deleteOneRow(new TestStorable(123L));

    boolean objectStillInTransaction;
    try {
      connection1.queryOneRow(new TestStorable(123L));
      objectStillInTransaction = true;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      objectStillInTransaction = false;
    }

    if (objectStillInTransaction) {
      fail("object still in transaction after delete");
    }

    PreparedQuery<TestStorable> preparedQuery =
      connection1.prepareQuery(new Query<TestStorable>("select * from " + TestStorable.TABLE_NAME + " where "
          + TestStorable.COL_ID + " = ?", TestStorable.reader));

    List<TestStorable> queryResult = connection1.query(preparedQuery, new Parameter(123L), -1);

    assertEquals(0, queryResult.size());


    PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();

    try {
      connection2.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("Object not found in other transaction before commit");
    }


    connection1.commit();


    boolean foundInSecondConnection;
    try {
      connection2.queryOneRow(new TestStorable(123L));
      foundInSecondConnection = true;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      foundInSecondConnection = false;
    }

    if (foundInSecondConnection) {
      fail("Object found in other connection even after commit");
    }

  }


  public void test_11_Delete2() throws PersistenceLayerException {

    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 456L));
    try {
      connection.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("object not available after persistobject");
    }
    connection.deleteOneRow(new TestStorable(123L));

    try {
      connection.queryOneRow(new TestStorable(123L));
      fail("found though deleted");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
    }
    connection.commit();
    try {
      connection.queryOneRow(new TestStorable(123L));
      fail("found though deleted");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
    }
  }
  

  public void test_11_Delete2a() throws PersistenceLayerException {

    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 456L));
    try {
      connection.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("object not available after persistobject");
    }
    connection.deleteOneRow(new TestStorable(123L));

    try {
      connection.queryOneRow(new TestStorable(123L));
      fail("found though deleted");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
    }
    connection.commit();
    try {
      connection.queryOneRow(new TestStorable(123L));
      fail("found though deleted");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
    }
  }


  public void test_11_Delete2b() throws PersistenceLayerException {

    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 456L));
    try {
      connection.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("object not available after persistobject");
    }
    connection.commit();
    
    connection.persistObject(new TestStorable(123L, 123L));
    
    connection.deleteOneRow(new TestStorable(123L));

    try {
      connection.queryOneRow(new TestStorable(123L));
      fail("found though deleted");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
    }
    connection.commit();
    try {
      connection.queryOneRow(new TestStorable(123L));
      fail("found though deleted");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
    }
  }

  public void test_11a_Delete3() throws PersistenceLayerException {

    {

      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable(123L, 456L);
      connection.persistObject(testEntry);

      testEntry = new TestStorable(234L, 890L);
      connection.persistObject(testEntry);

      connection.commit();
      connection.closeConnection();

    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    connection1.deleteOneRow(new TestStorable(123L));
    connection1.deleteOneRow(new TestStorable(123L));

    boolean objectStillInTransaction;
    try {
      connection1.queryOneRow(new TestStorable(123L));
      objectStillInTransaction = true;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      objectStillInTransaction = false;
    }

    if (objectStillInTransaction) {
      fail("object still in transaction after delete");
    }

    connection1.commit();

  }


  public void test_11b_Delete4() throws PersistenceLayerException {

    {

      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

      TestStorable testEntry = new TestStorable(123L, 456L);
      connection.persistObject(testEntry);

      testEntry = new TestStorable(234L, 890L);
      connection.persistObject(testEntry);

      connection.commit();
      connection.closeConnection();

    }

    PersistenceLayerConnection connection1 = getPersistenceLayer().getConnection();

    connection1.deleteOneRow(new TestStorable(123L, 456L));
    connection1.persistObject(new TestStorable(123L, 456L));

    connection1.commit();

    try {
      connection1.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("object still in transaction after delete");
    }

  }
  
  public void test_11c_Delete5() throws PersistenceLayerException {
    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 456L));
 /*   try {
      connection.queryOneRow(new TestStorable(123L));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("object not available after persistobject");
    }*/
    List<TestStorable> l = new ArrayList<TestStorable>();
    l.add(new TestStorable(123L));
    
    connection.delete(l);

    connection.commit();
    try {
      connection.queryOneRow(new TestStorable(123L));
      fail("found though deleted");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // expected
    }
  }

  public void test_12_BlockingInsert() throws PersistenceLayerException, InterruptedException {

    final AtomicBoolean secondThreadFinished = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();

    final PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 456L));

    final CountDownLatch latch = new CountDownLatch(1);

    Thread t = new Thread(new Runnable() {

      public void run() {

        TestStorable readForUpdateEntry2 = new TestStorable(123L, 958L);
        try {
          PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
          connection2.persistObject(readForUpdateEntry2);
          assertEquals(readForUpdateEntry2.getTrivialId(), 958L);
          secondThreadFinished.set(true);
          latch.await();
          connection2.commit();
          connection2.closeConnection();
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(100);

    assertEquals(false, secondThreadFailed.get());
    assertEquals(false, secondThreadFinished.get());

    connection.commit();

    Thread.sleep(100);

    assertEquals(false, secondThreadFailed.get());
    assertEquals(true, secondThreadFinished.get());

    secondThreadFailed.set(false);
    secondThreadFinished.set(false);

    // read within first transaction and expect the old value, expect a "for update" to block
    t = new Thread(new Runnable() {

      public void run() {

        TestStorable readForUpdateEntry2 = new TestStorable(123L);
        try {
          connection.queryOneRow(readForUpdateEntry2);
          assertEquals(readForUpdateEntry2.getTrivialId(), 456L);
          connection.queryOneRowForUpdate(readForUpdateEntry2);
          secondThreadFinished.set(true);
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          secondThreadFailed.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(100);

    assertEquals(false, secondThreadFailed.get());
    assertEquals(false, secondThreadFinished.get());

    latch.countDown();

    Thread.sleep(100);

    assertEquals(false, secondThreadFailed.get());
    assertEquals(true, secondThreadFinished.get());

  }


  public abstract int test_13_getCountForOutOfMemoryOnInserts();


  public void test_13_testOutOfMemoryForRollbackOnInserts() throws PersistenceLayerException {

    for (int i = 0; i < test_13_getCountForOutOfMemoryOnInserts(); i++) {
      PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
      connection.persistObject(new TestStorable(123L, 456L));
      connection.rollback();
      connection.closeConnection();
    }

  }


  public abstract int test_14_getCountForOutOfMemoryOnUpdates();


  public void test_14_testOutOfMemoryForRollbackOnUpdates() throws PersistenceLayerException {

    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 567L));

    for (int i = 0; i < test_14_getCountForOutOfMemoryOnUpdates(); i++) {
      connection = getPersistenceLayer().getConnection();
      connection.persistObject(new TestStorable(123L, 456L));
      connection.rollback();
      connection.closeConnection();
    }

  }
  
  
  public void test_15_evaluateIndexAndOrderByInteraction() throws PersistenceLayerException {

    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();

    long AMOUNT = 100;
    Random r = new Random();
    for (long i = 0; i < 100; i++) {
      TestStorable testEntry = new TestStorable(Math.abs(r.nextLong()), AMOUNT-i);
      connection.persistObject(testEntry);
    }
    
    int MAX_ROWS = 10;
    
    connection.commit();
    
    String ascendingForUpdate = "select * from " + TestStorable.TABLE_NAME + " where " + TestStorable.COL_ID + " > ? order by " + TestStorable.COL_TRIVIAL_ID + " asc for update";
    PreparedQuery<TestStorable> pq = connection.prepareQuery(new Query<TestStorable>(ascendingForUpdate, TestStorable.reader));
    List<TestStorable> result = connection.query(pq, new Parameter(-1L), MAX_ROWS);
    Long previousTrivialId = null;
    for (TestStorable testStorable : result) {
      if (previousTrivialId != null) {
        assertTrue("Storables should have been returned in ascending order.", previousTrivialId < testStorable.getTrivialId());
      }
      previousTrivialId = testStorable.getTrivialId();
    }
    assertEquals("Storables should have been returned in ascending order.", new Long(MAX_ROWS), previousTrivialId);
    assertEquals(result.size(), MAX_ROWS);
    
    String descending = "select * from " + TestStorable.TABLE_NAME + " where " + TestStorable.COL_ID + " > ? order by " + TestStorable.COL_TRIVIAL_ID + " desc";
    pq = connection.prepareQuery(new Query<TestStorable>(descending, TestStorable.reader));
    result = connection.query(pq, new Parameter(-1L), MAX_ROWS);
    previousTrivialId = null;
    for (TestStorable testStorable : result) {
      if (previousTrivialId != null) {
        assertTrue("Storables should have been returned in descending order.", previousTrivialId > testStorable.getTrivialId());
      }
      previousTrivialId = testStorable.getTrivialId();
    }
    assertEquals("Storables should have been returned in descending order.", new Long(AMOUNT-10+1), previousTrivialId);
    assertEquals(result.size(), MAX_ROWS);
  }
  
  
  public void test_16_testUpdateWithSeparateConnection() throws PersistenceLayerException {
    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 567L));
    connection.commit();
    connection.closeConnection();
    
    connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 568L));
    
    
    PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    connection2.persistObject(new TestStorable(123L, 569L));
    connection2.commit();
    connection2.closeConnection();
    
    connection.commit();
    connection.closeConnection();
    
    connection = getPersistenceLayer().getConnection();
    try {
      TestStorable ts = new TestStorable(123L);
      connection.queryOneRow(ts);
      assertEquals(568L , ts.getTrivialId());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("object not found");
    }
    connection.closeConnection();
    
  }
  
  public void test_17_testUpdateDeleted() throws PersistenceLayerException {
    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 567L));
    connection.commit();
    connection.closeConnection();
    
    connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 568L));
    
    PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    connection2.deleteOneRow(new TestStorable(123L));
    connection2.commit();
    connection2.closeConnection();
    
    connection.commit();
    connection.closeConnection();
    
    connection = getPersistenceLayer().getConnection();
    try {
      TestStorable ts = new TestStorable(123L);
      connection.queryOneRow(ts);
      assertEquals(568L , ts.getTrivialId());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("object not found");
    }
    connection.closeConnection();
  }
  
  public void test_18_concurrentInserts() throws PersistenceLayerException {
    PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    connection.persistObject(new TestStorable(123L, 567L));
    
    PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    connection2.persistObject(new TestStorable(123L, 567L));

    connection.commit();
    connection2.commit();
    
    connection2.deleteOneRow(new TestStorable(123L));
    connection2.commit();
    
    PreparedQuery<TestStorable> preparedQuery =
        connection.prepareQuery(new Query<TestStorable>("select * from " + TestStorable.TABLE_NAME + " where "
            + TestStorable.COL_TRIVIAL_ID + " = ?", TestStorable.reader));

    List<TestStorable> queryResult = connection.query(preparedQuery, new Parameter(567L), -1);

    assertEquals(0, queryResult.size());
    connection.closeConnection();
  }
  

  public void test_19_updateAndSelectDeadlock() throws PersistenceLayerException {
    //vorbereitung
    final PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    final long i1 = -188111L;
    final long i2 = -125234221L;
    connection.persistObject(new TestStorable(i1, 567L));
    connection.persistObject(new TestStorable(i2, 568L));
    connection.commit();


    //query vorbereitung
    final PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    final PreparedQuery<Long> pq = connection2.prepareQuery(new Query<Long>("select * from " + TestStorable.TABLE_NAME + " where (" + TestStorable.COL_ID + " = ? OR " + TestStorable.COL_ID + " = ?)", new ResultSetReader<Long>() {

      @Override
      public Long read(ResultSet rs) throws SQLException {
        return rs.getLong(TestStorable.COL_TRIVIAL_ID);
      }
      
    }));

    final Random r = new Random();
    for (int i = 0; i < 1000; i++) {
      final int idx = i;

      //gleichzeitiges commit und query:
      final CountDownLatch lStart = new CountDownLatch(1);
      final CountDownLatch lEnd = new CountDownLatch(2);
      Thread t1 = new Thread(new Runnable() {

        @Override
        public void run() {
          //updates
          try {
            connection.persistObject(new TestStorable(i1, r.nextLong()));
            connection.persistObject(new TestStorable(i2, r.nextLong()));
            long nanos = r.nextInt(10000);
            try {
              lStart.await();
            } catch (InterruptedException e) {
            }
            LockSupport.parkNanos(nanos);
            connection.commit();
          } catch (PersistenceLayerException e) {
            throw new RuntimeException(e);
          } finally {
            lEnd.countDown();
          }
        }

      });
      Thread t2 = new Thread(new Runnable() {

        @Override
        public void run() {
          long nanos = r.nextInt(10000);
          try {
            lStart.await();
          } catch (InterruptedException e) {
          }
          LockSupport.parkNanos(nanos);
          try {
            List<Long> trivialIds = connection2.query(pq, new Parameter(i1, i2), -2);
            try {
              Thread.sleep(5);
            } catch (InterruptedException e) {
            }
            assertEquals(2, trivialIds.size());
            System.out.println(idx + ") " + trivialIds);
          } catch (PersistenceLayerException e) {
            throw new RuntimeException(e);
          } finally {
            lEnd.countDown();
          }
        }

      });
      t1.start();
      t2.start();
      lStart.countDown();
      try {
        if (!lEnd.await(5, TimeUnit.SECONDS)) {
          Exception e = new Exception();
          e.setStackTrace(t1.getStackTrace());
          e.printStackTrace();
          e = new Exception();
          e.setStackTrace(t2.getStackTrace());
          e.printStackTrace();
          assertTrue("deadlock", false);
        }
      } catch (InterruptedException e) {
      }
    }
  }
  
  public void test20_selectWithIndex() throws PersistenceLayerException {
    final PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    final long i1 = 188111L;
    final long i2 = -125234221L;
    connection.persistObject(new TestStorable(i1, 567L));
    connection.persistObject(new TestStorable(i2, 568L));
    connection.commit();
    final PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    final PreparedQuery<Long> pq = connection2.prepareQuery(new Query<Long>("select * from " + TestStorable.TABLE_NAME + " where " + TestStorable.COL_ID + " > 0", new ResultSetReader<Long>() {

      @Override
      public Long read(ResultSet rs) throws SQLException {
        return rs.getLong(TestStorable.COL_TRIVIAL_ID);
      }
      
    }));
    List<Long> trivialIds = connection2.query(pq, new Parameter(), -2);
    assertEquals(1, trivialIds.size());
    assertEquals(567L, (long)trivialIds.get(0));

    final PreparedQuery<Long> pq2 = connection2.prepareQuery(new Query<Long>("select * from " + TestStorable.TABLE_NAME + " where " + TestStorable.COL_ID + " > 0 OR " + TestStorable.COL_TRIVIAL_ID + " > ? order by " + TestStorable.COL_ID, new ResultSetReader<Long>() {

      @Override
      public Long read(ResultSet rs) throws SQLException {
        return rs.getLong(TestStorable.COL_TRIVIAL_ID);
      }
      
    }));
    trivialIds = connection2.query(pq2, new Parameter(567L), -2);
    assertEquals(2, trivialIds.size());
    assertEquals(568L, (long)trivialIds.get(0));
    assertEquals(567L, (long)trivialIds.get(1));
  }
  
  public void test21_selectWithIndex2() throws PersistenceLayerException {
    final PersistenceLayerConnection connection = getPersistenceLayer().getConnection();
    final long i1 = 188111L;
    final long i2 = 123411L;
    connection.persistObject(new TestStorable(i1, 567L, "a"));
    connection.persistObject(new TestStorable(i2, 568L, "b"));
    connection.commit();
    final PersistenceLayerConnection connection2 = getPersistenceLayer().getConnection();
    final PreparedQuery<Long> pq = connection2.prepareQuery(new Query<Long>("select * from " + TestStorable.TABLE_NAME + " where (" + TestStorable.COL_STRINGCOL + " = ? or "+ TestStorable.COL_STRINGCOL + " = ?) and " + TestStorable.COL_TRIVIAL_ID + " = ? and " + TestStorable.COL_ID + " > ?" , new ResultSetReader<Long>() {

      @Override
      public Long read(ResultSet rs) throws SQLException {
        return rs.getLong(TestStorable.COL_TRIVIAL_ID);
      }
      
    }));
    List<Long> trivialIds = connection2.query(pq, new Parameter("a", "b", 567L, 0L), -2);
    assertEquals(1, trivialIds.size());
    assertEquals(567L, (long)trivialIds.get(0));

  }
}
