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
package com.gip.xyna.xnwh.persistence.javaserialization;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.Assert;

import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.AbstractPersistenceLayerTest;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

public class JavaSerializationPersistenceLayerTest extends AbstractPersistenceLayerTest {
  
  
  public void setUp() {
    File tmpDirFile = new File(XynaProperty.PERSISTENCE_DIR + File.separator + PersistenceLayerTestStoreable.TABLENAME);
    tmpDirFile.mkdirs();
    File []childfiles = tmpDirFile.listFiles();
    if(childfiles != null) {
      for(File child : childfiles) {
       child.delete();
      }
    }
  }
  
  public void tearDown() {
    File tmpDirFile = new File(XynaProperty.PERSISTENCE_DIR + File.separator + PersistenceLayerTestStoreable.TABLENAME);
    File []childfiles = tmpDirFile.listFiles();
    if(childfiles != null) {
      for(File child : childfiles) {
       child.delete();
      }
    }
    tmpDirFile.delete();
  }
  
  private XynaJavaSerializationPersistenceLayer layerInstance;
  
  private XynaJavaSerializationPersistenceLayer getLayer() {
    if (layerInstance == null) {
      try {
        layerInstance = new XynaJavaSerializationPersistenceLayer();
      } catch(PersistenceLayerException e) {
        e.printStackTrace();
        Assert.fail(e.getMessage());
      }
    }
    return layerInstance;
  }
  
  @Override
  public PersistenceLayerConnection getConnection() {
    try {
      XynaJavaSerializationPersistenceLayer javaserializationPL = getLayer();
      PersistenceLayerConnection connection = javaserializationPL.getConnection();
      connection.addTable(PersistenceLayerTestStoreable.class, false, null);
      return connection;      
    } catch(PersistenceLayerException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
    return null;
  }
  
  

  public void testTransactionSafeLoadCollectionRewrite() throws PersistenceLayerException {
    File tmpDirFile = new File(XynaProperty.PERSISTENCE_DIR + File.separator + TestStorable.TABLENAME);
    tmpDirFile.mkdirs();
    try {
      PersistenceLayerConnection con = getConnection();
      try {
        con.addTable(TestStorable.class, false, null);
        TestStorable ts = new TestStorable(1, 2L, "Baum", null);
        con.persistObject(ts);
        ts = new TestStorable(2, 2L, "Baum", null);
        con.persistObject(ts);
        ts = new TestStorable(3, 2L, "Baum", null);
        con.persistObject(ts);
        con.commit();
      } finally {
        con.closeConnection();
      }
      
      /*System.out.println("After single persists");
      for (File file : tmpDirFile.listFiles()) {
        System.out.println(file.getName() + ": " + file.length());
      }*/
      
      Collection<TestStorable> tss = con.loadCollection(TestStorable.class);
      con.persistCollection(tss);
      con.commit();
      
      /*System.out.println("After collection persists");
      for (File file : tmpDirFile.listFiles()) {
        System.out.println(file.getName() + ": " + file.length());
      }*/
      
      TestStorable.throwErrorOnWrite = true;
      try {
        con.loadCollection(TestStorable.class);
        fail("Expected UnsatisfiedLinkError from TestStorable.throwErrorOnWrite");
      } catch (UnsatisfiedLinkError e) {
        // ntbd
      }
      
      /*System.out.println("After error");
      for (File file : tmpDirFile.listFiles()) {
        System.out.println(file.getName() + ": " + file.length());
      }*/
      
      TestStorable.throwErrorOnWrite = false;
    } finally {
      /*for (File file : tmpDirFile.listFiles()) {
        System.out.println(file.getName() + ": " + file.length());
      }*/
      File []childfiles = tmpDirFile.listFiles();
      if(childfiles != null) {
        for(File child : childfiles) {
         child.delete();
        }
      }
      tmpDirFile.delete();
    }
  }
  
}
