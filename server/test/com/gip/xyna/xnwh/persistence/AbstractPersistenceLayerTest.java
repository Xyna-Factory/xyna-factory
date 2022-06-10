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
package com.gip.xyna.xnwh.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

public abstract class AbstractPersistenceLayerTest extends TestCase {
  
  private static final int STRINGNUM = 6;
  protected static final int STRINGSIZE = 10576; // 1048576
  protected static String []TESTSTRINGS = new String[STRINGNUM];
  
  
  public abstract PersistenceLayerConnection getConnection();
  
  
  protected static String getRandomString(int n) {
    String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    StringBuilder builder = new StringBuilder();
    Random rnd = new Random();
    for(int i = 0; i < n; i++) {
      builder.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
    }
    return builder.toString();
  }
  
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Erzeugung von 1 MB-groﬂen Teststrings
    for(int i = 0; i < STRINGNUM; i++) {
      TESTSTRINGS[i] = getRandomString(STRINGSIZE);
    }
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    TESTSTRINGS = new String[STRINGNUM];
    super.tearDown();
  }
  
  
  public void testOneObjectStoreAndQuery() {
        
    try {
      PersistenceLayerConnection connection = getConnection();
      PersistenceLayerTestStoreable testObj = new PersistenceLayerTestStoreable("1", TESTSTRINGS[0], TESTSTRINGS[1]);
      
      connection.persistObject(testObj);
      
      Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
      
      PersistenceLayerTestStoreable queryObj = new PersistenceLayerTestStoreable("1", null, null);
      connection.queryOneRow(queryObj);
      
      Assert.assertEquals("1", queryObj.getId());
      Assert.assertEquals(TESTSTRINGS[0], queryObj.getSpalte1());
      Assert.assertEquals(TESTSTRINGS[1], queryObj.getSpalte2());
      
      connection.deleteOneRow(new PersistenceLayerTestStoreable("1", null, null));
      
      Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("1", null, null)));
      try {
        connection.queryOneRow(queryObj);
        Assert.fail("Exception erwartet.");
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      }
      
      
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
  
 public void testObjectCollectionStoreAndDeleteAll() {
    try {
      PersistenceLayerConnection connection = getConnection();
      
      Collection<PersistenceLayerTestStoreable> collectionsPers = new ArrayList<AbstractPersistenceLayerTest.PersistenceLayerTestStoreable>(); 
      for(int i = 0; i < 6; i+=2) {
        PersistenceLayerTestStoreable testObj1 = new PersistenceLayerTestStoreable(Integer.toString(i), TESTSTRINGS[i], TESTSTRINGS[i + 1]);
        collectionsPers.add(testObj1);
      }
      
      connection.persistCollection(collectionsPers);
      
      Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
      Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
      Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
      
      connection.deleteAll(PersistenceLayerTestStoreable.class);
      
      Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
      Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
      Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
      
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
 
 
 public void testObjectCollectionStoreAndDeleteAllAndPersistAnew() {
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
     
     connection = getConnection();
     
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
     
     connection.deleteAll(PersistenceLayerTestStoreable.class);
     
     Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
     Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
     Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
     
     connection.persistCollection(collectionsPers);
     
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
     
     connection.commit();
     
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
     Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
     
   } catch (Exception e) {
     e.printStackTrace();
     Assert.fail(e.getMessage());
   }
 }
 
  
  public void testObjectCollectionStoreAndQuery() {
    try {
      PersistenceLayerConnection connection = getConnection();
      
      for(int i = 0; i < 6; i+=2) {
        PersistenceLayerTestStoreable testObj1 = new PersistenceLayerTestStoreable(Integer.toString(i), TESTSTRINGS[i], TESTSTRINGS[i + 1]);
        connection.persistObject(testObj1);
      }
      
      Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
      Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
      Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
      
      PersistenceLayerTestStoreable queryObj = new PersistenceLayerTestStoreable("0", null, null);
      connection.queryOneRow(queryObj);
      Assert.assertEquals("0", queryObj.getId());
      Assert.assertEquals(TESTSTRINGS[0], queryObj.getSpalte1());
      Assert.assertEquals(TESTSTRINGS[1], queryObj.getSpalte2());
      
      queryObj = new PersistenceLayerTestStoreable("2", null, null);
      connection.queryOneRow(queryObj);
      Assert.assertEquals("2", queryObj.getId());
      Assert.assertEquals(TESTSTRINGS[2], queryObj.getSpalte1());
      Assert.assertEquals(TESTSTRINGS[3], queryObj.getSpalte2());
      
      queryObj = new PersistenceLayerTestStoreable("4", null, null);
      connection.queryOneRow(queryObj);
      Assert.assertEquals("4", queryObj.getId());
      Assert.assertEquals(TESTSTRINGS[4], queryObj.getSpalte1());
      Assert.assertEquals(TESTSTRINGS[5], queryObj.getSpalte2());
      
      connection.delete(Arrays.asList(new PersistenceLayerTestStoreable[] {
          new PersistenceLayerTestStoreable("0", null, null),
          new PersistenceLayerTestStoreable("2", null, null),
          new PersistenceLayerTestStoreable("4", null, null)
      }));
      
      Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
      Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
      Assert.assertFalse(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
      
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
  
  
  public void testTryInfluencePersistingObject() throws Exception {
    PersistenceLayerConnection connection = getConnection();

    Collection<PersistenceLayerTestStoreable> collectionsPers = new ArrayList<AbstractPersistenceLayerTest.PersistenceLayerTestStoreable>(); 
    for(int i = 0; i < 6; i+=2) {
      PersistenceLayerTestStoreable testObj1 = new PersistenceLayerTestStoreable(Integer.toString(i), TESTSTRINGS[i], TESTSTRINGS[i + 1]);
      collectionsPers.add(testObj1);
    }
    
    connection.persistCollection(collectionsPers);
    
    for(int i = 10; i < 12; i++) {
      collectionsPers = new ArrayList<AbstractPersistenceLayerTest.PersistenceLayerTestStoreable>(30); 
      for(int j = 0; j < 30; j++) {
        PersistenceLayerTestStoreable testObj1 = new PersistenceLayerTestStoreable(Integer.toString(i) + "," + Integer.toString(j), getRandomString(STRINGSIZE), getRandomString(STRINGSIZE));
        collectionsPers.add(testObj1);
      }
      connection.persistCollection(collectionsPers);
    }
      
    DeleteObjectThread testThread = new DeleteObjectThread(connection, Arrays.asList(new PersistenceLayerTestStoreable[] { new PersistenceLayerTestStoreable("10,0", null, null)}));
    testThread.start();
    
    Thread.sleep(135);
    
    testThread.stop();
    
    Thread.sleep(3000);
    
    Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("0", null, null)));
    Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("2", null, null)));
    Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable("4", null, null)));
    
    for(int k = 10; k < 12; k++) {
      for(int j = 0; j < 30; j++) {
       // wenn Test hier fehlschl‰gt, war das Lˆschen erfolgreich --> Testziel verfehlt ....
       Assert.assertTrue(connection.containsObject(new PersistenceLayerTestStoreable(Integer.toString(k) + "," + Integer.toString(j), null, null)));
      }
    }
      
  }

  
  // Threadklasse, die ein sehr groﬂes Objekt persistieren will.
  private class DeleteObjectThread extends Thread {
    
    Connection connection;
    Collection<PersistenceLayerTestStoreable> collectionsPers;
    
    public DeleteObjectThread(Connection connection, Collection<PersistenceLayerTestStoreable> collectionsPers) {
      this.connection = connection;
      this.collectionsPers = collectionsPers;
    }
    
    public void run() {
     
      try {
        connection.delete(collectionsPers);
      } catch(PersistenceLayerException e) {
        e.printStackTrace();
        Assert.fail(e.getMessage());
      }
    }
  }
  
  // Storeable-Objekt zum Testen
  @Persistable(primaryKey = PersistenceLayerTestStoreable.ID, tableName = PersistenceLayerTestStoreable.TABLENAME)
  public static class PersistenceLayerTestStoreable extends Storable<PersistenceLayerTestStoreable> {

    private static final long serialVersionUID = -7148095017245228417L;
    
    public static final String TABLENAME = "PersistenceLayerTestStoreable";
    public static final String ID = "id";
    public static final String SPALTE1 = "spalte1";
    public static final String SPALTE2 = "spalte2";
    
    @Column(name = ID)
    private String id;
    
    @Column(name = SPALTE1)
    private String spalte1;
    
    @Column(name = SPALTE2)
    private String spalte2;
    
    public PersistenceLayerTestStoreable() {
    }
    
    public PersistenceLayerTestStoreable(String id, String spalte1, String spalte2) {
      this.id = id;
      this.spalte1 = spalte1;
      this.spalte2 = spalte2;
    }
    
    public String getId() {
      return id;
    }
 
    public void setId(String id) {
      this.id = id;
    }

    public String getSpalte1() {
      return spalte1;
    }

    public void setSpalte1(String spalte1) {
      this.spalte1 = spalte1;
    }

    public String getSpalte2() {
      return spalte2;
    }

    public void setSpalte2(String spalte2) {
      this.spalte2 = spalte2;
    }

    @Override
    public ResultSetReader<? extends PersistenceLayerTestStoreable> getReader() {
      return new ResultSetReader<PersistenceLayerTestStoreable>() {

        public PersistenceLayerTestStoreable read(ResultSet rs)
            throws SQLException {
          return new PersistenceLayerTestStoreable(rs.getString(ID),
                rs.getString(SPALTE1), rs.getString(SPALTE2));          
        }
        
      };
    }

    @Override
    public Object getPrimaryKey() {
      return id;
    }

    @Override
    public <U extends PersistenceLayerTestStoreable> void setAllFieldsFromData(U data) {
      PersistenceLayerTestStoreable cast = data;
      id =  cast.id;
      spalte1 = cast.spalte1;
      spalte2 = cast.spalte2;      
    }
    
  }  

}
