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
package com.gip.xyna.xnwh.persistence.xml;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import com.gip.xyna.utils.concurrent.FutureCollection;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.persistence.AbstractPersistenceLayerTest;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer.TransactionMode;

public class XMLPersistenceLayerFullTransactionTest extends XMLPersistenceLayerTest {


  private static final String tmpDir2 = "XMLPersistenceTest2";
  
  @Override
  public TransactionMode getTransactionMode() {
    return TransactionMode.FULL_TRANSACTION;
  }
  
  
  
  public void testIsolation() {
    
    try {
      PersistenceLayerConnection connection1 = getConnection();
      try {
        PersistenceLayerConnection connection2 = getConnection();
        try {
          PersistenceLayerTestStoreable testObj = new PersistenceLayerTestStoreable("1", TESTSTRINGS[0], TESTSTRINGS[1]);
          
          connection1.persistObject(testObj);
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          connection1.commit();
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertTrue(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          connection1.deleteOneRow(new PersistenceLayerTestStoreable("1", null, null));
          
          assertTrue(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          connection1.rollback();
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertTrue(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          connection2.deleteOneRow(new PersistenceLayerTestStoreable("1", null, null));
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          connection2.commit();
          
          assertFalse(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
        } finally {
          connection2.closeConnection();
        }
      } finally {
        connection1.closeConnection();
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
  
  
  public void testUpdateChain() {
    
    try {
      PersistenceLayerConnection connection1 = getConnection();
      try {
        PersistenceLayerConnection connection2 = getConnection();
        try {
          PersistenceLayerTestStoreable testObj = new PersistenceLayerTestStoreable("1", TESTSTRINGS[0], TESTSTRINGS[1]);
          
          connection1.persistObject(testObj);
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          PersistenceLayerTestStoreable queryObj = new PersistenceLayerTestStoreable("1", null, null);
          connection1.queryOneRow(queryObj);
          
          Assert.assertEquals("1", queryObj.getId());
          Assert.assertEquals(TESTSTRINGS[0], queryObj.getSpalte1());
          Assert.assertEquals(TESTSTRINGS[1], queryObj.getSpalte2());
          
          testObj.setSpalte2(TESTSTRINGS[2]);
          connection1.persistObject(testObj);
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          queryObj = new PersistenceLayerTestStoreable("1", null, null);
          connection1.queryOneRow(queryObj);
          
          Assert.assertEquals("1", queryObj.getId());
          Assert.assertEquals(TESTSTRINGS[0], queryObj.getSpalte1());
          Assert.assertEquals(TESTSTRINGS[2], queryObj.getSpalte2());
          
          testObj.setSpalte2(TESTSTRINGS[3]);
          connection1.persistObject(testObj);
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          queryObj = new PersistenceLayerTestStoreable("1", null, null);
          connection1.queryOneRow(queryObj);
          
          Assert.assertEquals("1", queryObj.getId());
          Assert.assertEquals(TESTSTRINGS[0], queryObj.getSpalte1());
          Assert.assertEquals(TESTSTRINGS[3], queryObj.getSpalte2());
          
          connection1.commit();
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertTrue(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          queryObj = new PersistenceLayerTestStoreable("1", null, null);
          connection1.queryOneRow(queryObj);
          
          Assert.assertEquals("1", queryObj.getId());
          Assert.assertEquals(TESTSTRINGS[0], queryObj.getSpalte1());
          Assert.assertEquals(TESTSTRINGS[3], queryObj.getSpalte2());
          
          queryObj = new PersistenceLayerTestStoreable("1", null, null);
          connection2.queryOneRow(queryObj);
          
          Assert.assertEquals("1", queryObj.getId());
          Assert.assertEquals(TESTSTRINGS[0], queryObj.getSpalte1());
          Assert.assertEquals(TESTSTRINGS[3], queryObj.getSpalte2());
          
          
        } finally {
          connection2.closeConnection();
        }
      } finally {
        connection1.closeConnection();
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
  
  
  public void testConcurrentDeleteAlls() {
    final int concurrencyLevel = 10;
    ExecutorService exSe = Executors.newFixedThreadPool(concurrencyLevel);
    try {
      PersistenceLayerConnection connection = getConnection();
      
      Collection<PersistenceLayerTestStoreable> collectionsPers = new ArrayList<AbstractPersistenceLayerTest.PersistenceLayerTestStoreable>(); 
      for(int i = 0; i < 6; i+=2) {
        PersistenceLayerTestStoreable testObj1 = new PersistenceLayerTestStoreable(Integer.toString(i), TESTSTRINGS[i], TESTSTRINGS[i + 1]);
        collectionsPers.add(testObj1);
      }
      
      connection.persistCollection(collectionsPers);
      
      connection.commit();
      connection.closeConnection();
      
      CountDownLatch startLatch = new CountDownLatch(1);
      
      FutureCollection<Void> futures = new FutureCollection<Void>();
      for (int i = 0; i < concurrencyLevel; i++) {
        futures.add(exSe.submit(new CopyCallable(startLatch, getConnection(), getConnection(tmpDir2))));
      }
      
      startLatch.countDown();
      
      futures.get(10L, TimeUnit.SECONDS);
      
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    } finally {
      File tmpDirFile = new File(Constants.STORAGE_PATH + File.separator + tmpDir2);
      File []childfiles = tmpDirFile.listFiles();
      if(childfiles != null) {
        for(File child : childfiles) {
         child.delete();
        }
      }
      tmpDirFile.delete();
      exSe.shutdown();
    }
  }
  
  
  private class CopyCallable implements Callable<Void> {
    
    private CountDownLatch latch;
    private PersistenceLayerConnection connection;
    private PersistenceLayerConnection connection2;
    
    public CopyCallable(CountDownLatch latch, PersistenceLayerConnection connection, PersistenceLayerConnection connection2) {
      this.latch = latch;
      this.connection = connection;
      this.connection2 = connection2;
    }

    public Void call() throws Exception {
      latch.await();
      
      Collection<PersistenceLayerTestStoreable> toBeCopied = null;
      try {
        toBeCopied = connection.loadCollection(PersistenceLayerTestStoreable.class);
      } finally {
        connection.closeConnection();
      }

      try {
        if (toBeCopied != null) {
          connection2.deleteAll(PersistenceLayerTestStoreable.class);
          connection2.persistCollection(toBeCopied);
          connection2.commit();
        }
      } finally {
        connection2.closeConnection();
      }
      
      return null;
    }
    
  }
  
  

}
