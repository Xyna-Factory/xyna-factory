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
import java.util.List;

import org.junit.Assert;

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.persistence.AbstractPersistenceLayerTest;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer.TransactionMode;

public abstract class XMLPersistenceLayerTest extends AbstractPersistenceLayerTest {

  private static final String tmpDir = "XMLPersistenceTest";
  
  private List<XMLPersistenceLayer> openPLs = new ArrayList<XMLPersistenceLayer>();
  
  public void setUp() {
    File tmpDirFile = new File(Constants.STORAGE_PATH + File.separator + tmpDir);
    tmpDirFile.mkdirs();
    File []childfiles = tmpDirFile.listFiles();
    if(childfiles != null) {
      for(File child : childfiles) {
       child.delete();
      }
    }
  }
  
  public void tearDown() {
    try {
      getConnection(tmpDir).deleteAll(PersistenceLayerTestStoreable.class);
    } catch (PersistenceLayerException e) {
      
    }
    File tmpDirFile = new File(Constants.STORAGE_PATH + File.separator + tmpDir);
    File []childfiles = tmpDirFile.listFiles();
    if(childfiles != null) {
      for(File child : childfiles) {
       child.delete();
      }
    }
    tmpDirFile.delete();
    System.out.println("closing " + openPLs.size() + " PLs.");
    for (XMLPersistenceLayer pl : openPLs) {
      try {
        pl.shutdown();
      } catch (PersistenceLayerException e) {
        e.printStackTrace();
      }
    }
    openPLs.clear();
  }
  
  
  @Override
  public PersistenceLayerConnection getConnection() {
    return getConnection(tmpDir);
  }
  
  public PersistenceLayerConnection getConnection(String dir) {
    try {
      XMLPersistenceLayer xmlPL = new XMLPersistenceLayer();
      openPLs.add(xmlPL);
      xmlPL.init(3L, dir, getTransactionMode().getStartParamIdentifier(), "false");
      PersistenceLayerConnection con = xmlPL.getConnection();
      con.addTable(PersistenceLayerTestStoreable.class, false, null);      
      return con;
    } catch(PersistenceLayerException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
    return null;
  }
  
  
  public abstract TransactionMode getTransactionMode();
  
  
  // this test for no isolation at all 
  public void testIsolation() {
    
    try {
      PersistenceLayerConnection connection1 = getConnection();
      try {
        PersistenceLayerConnection connection2 = getConnection();
        try {
          PersistenceLayerTestStoreable testObj = new PersistenceLayerTestStoreable("1", TESTSTRINGS[0], TESTSTRINGS[1]);
          
          connection1.persistObject(testObj);
          
          assertTrue(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertTrue(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          connection1.deleteOneRow(new PersistenceLayerTestStoreable("1", null, null));
          
          assertFalse(connection2.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          assertFalse(connection1.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
          
          connection2.deleteOneRow(new PersistenceLayerTestStoreable("1", null, null));
          
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
  
  
  // Should a performance test be part of every day JUnit-Execution?
  /*public void testPerformance() {
    System.out.println("[" + getTransactionMode() + "]");
    int objectAmount = 1000;
    int contentSize = 1000;
    setupArchive(objectAmount, contentSize);
    
    int queryAmount = 200;
    Random r = new Random();
    PersistenceLayerConnection con = getConnection();
    long queryStart = System.currentTimeMillis();
    try {
      try {
        for (int i = 0; i < queryAmount; i++) {
          PersistenceLayerTestStoreable queryObj = new PersistenceLayerTestStoreable(Integer.toString(r.nextInt(objectAmount)), null, null);
          con.queryOneRow(queryObj);
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        e.printStackTrace();
        fail(e.getMessage());
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    System.out.println(queryAmount + " Queries took: " + (System.currentTimeMillis() - queryStart));
    
    con = getConnection();
    long transactionQueryStart = System.currentTimeMillis();
    try {
      try {
        for (int i = 0; i < queryAmount; i++) {
          PersistenceLayerTestStoreable queryObj = new PersistenceLayerTestStoreable(Integer.toString(r.nextInt(objectAmount)), null, null);
          con.queryOneRow(queryObj);
          con.rollback();
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        e.printStackTrace();
        fail(e.getMessage());
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    System.out.println(queryAmount + " Transaction-Safe Queries took: " + (System.currentTimeMillis() - transactionQueryStart));
    
    int createdAmount = objectAmount;
    int creationAmount = 200;
    con = getConnection();
    long persistStart = System.currentTimeMillis();
    try {
      try {
        for (int i = createdAmount + 1; i < createdAmount + creationAmount; i++) {
          PersistenceLayerTestStoreable persistObj = new PersistenceLayerTestStoreable(Integer.toString(r.nextInt(objectAmount)), getRandomString(contentSize), getRandomString(contentSize));
          con.persistObject(persistObj);
        }
        con.commit();
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    System.out.println(creationAmount + " Persists with single commit took: " + (System.currentTimeMillis() - persistStart));
    
    createdAmount += creationAmount;
    con = getConnection();
    long persistWithCommitStart = System.currentTimeMillis();
    try {
      try {
        for (int i = createdAmount + 1; i < createdAmount + creationAmount; i++) {
          PersistenceLayerTestStoreable persistObj = new PersistenceLayerTestStoreable(Integer.toString(r.nextInt(objectAmount)), getRandomString(contentSize), getRandomString(contentSize));
          con.persistObject(persistObj);
          con.commit();
        }
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    System.out.println(creationAmount + " Persists and commits took: " + (System.currentTimeMillis() - persistWithCommitStart));
    
  }
  
  
  protected void setupArchive(int objects, int contentSize) {
    long setupStart = System.currentTimeMillis();
    try {
      PersistenceLayerConnection con = getConnection();
      try {
        Collection<PersistenceLayerTestStoreable> collectionsPers = new ArrayList<AbstractPersistenceLayerTest.PersistenceLayerTestStoreable>(); 
        for(int i = 0; i < objects; i++) {
          PersistenceLayerTestStoreable testObj1 = new PersistenceLayerTestStoreable(Integer.toString(i), getRandomString(contentSize), getRandomString(contentSize));
          collectionsPers.add(testObj1);
        }
        con.persistCollection(collectionsPers);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Setup could not be completed: " + e.getMessage());
    }
    System.out.println("Setup took: " + (System.currentTimeMillis() - setupStart) + " for #" + objects + " with contentSize " + contentSize);
  }*/
  
  
  
}
