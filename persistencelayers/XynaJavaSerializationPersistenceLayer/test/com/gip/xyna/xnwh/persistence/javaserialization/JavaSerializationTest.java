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

package com.gip.xyna.xnwh.persistence.javaserialization;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibClassLoader;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;



public class JavaSerializationTest extends TestCase {

  private static final Logger logger = Logger.getLogger(JavaSerializationTest.class);
  private static final String ORDERINSTANCE_TABLE = new OrderInstance().getTableName();

  private ODS ods;


  protected void setUp() throws Exception {
    super.setUp();
    File f = new File(XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator + ORDERINSTANCE_TABLE
                    + Constants.fileSeparator + ORDERINSTANCE_TABLE);
    if (f.exists())
      f.delete();
    f = new File(XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator + ORDERINSTANCE_TABLE + Constants.fileSeparator
                    + ORDERINSTANCE_TABLE + XynaProperty.INDEX_SUFFIX);
    if (f.exists())
      f.delete();
    f = new File(XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator + ORDERINSTANCE_TABLE);
    if (f.exists())
      f.delete();
    f = new File(XynaProperty.PERSISTENCE_DIR);
    if (f.exists())
      f.delete();
    ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(15, XynaJavaSerializationPersistenceLayer.class);
    long id = ods.instantiatePersistenceLayerInstance(ods.getJavaPersistenceLayerID(), "test",
                                                      ODSConnectionType.DEFAULT, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    ods.registerStorable(OrderInstance.class);
  }


  protected void tearDown() throws Exception {
    super.tearDown();
    ods.openConnection().deleteAll(OrderInstance.class);
  }


  private OrderInstance newOrderInstance(long id, String orderType) {
    OrderInstance oi = new OrderInstance(id);
    oi.setOrderType(orderType);
    return oi;
  }


  public void testPersistForServerShutdown() throws XynaException {
    ODSConnection con = ods.openConnection();
    try {
      OrderInstance wf1 = newOrderInstance(1l, "MyFQName1");
      OrderInstance wf2 = newOrderInstance(2l, "MyFQName2");
      OrderInstance wf3 = newOrderInstance(3l, "MyFQName3");
      OrderInstance wf4 = newOrderInstance(4l, "MyFQName4");
      OrderInstance wf5 = newOrderInstance(5l, "MyFQName5");
      OrderInstance wf6 = newOrderInstance(6l, "MyFQName6");
      OrderInstance wf7 = newOrderInstance(7l, "MyFQName7");
      OrderInstance wf8 = newOrderInstance(8l, "MyFQName8");
      ArrayList<OrderInstance> storeCol = new ArrayList<OrderInstance>();
      assertEquals(false, con.containsObject(wf1));
      assertEquals(false, con.containsObject(wf2));
      assertEquals(false, con.containsObject(wf3));
      assertEquals(false, con.containsObject(wf4));
      assertEquals(false, con.containsObject(wf5));
      assertEquals(false, con.containsObject(wf6));
      assertEquals(false, con.containsObject(wf7));
      assertEquals(false, con.containsObject(wf8));
      storeCol.add(wf1);
      storeCol.add(wf2);
      storeCol.add(wf3);
      storeCol.add(wf4);
      storeCol.add(wf5);
      storeCol.add(wf6);
      storeCol.add(wf7);
      storeCol.add(wf8);

      con.persistCollection(storeCol);

      //although they are not accessible we can confirm their existence 
      assertEquals(true, con.containsObject(wf1));
      assertEquals(true, con.containsObject(wf2));
      assertEquals(true, con.containsObject(wf3));
      assertEquals(true, con.containsObject(wf4));
      assertEquals(true, con.containsObject(wf5));
      assertEquals(true, con.containsObject(wf6));
      assertEquals(true, con.containsObject(wf7));
      assertEquals(true, con.containsObject(wf8));

    } finally {
      con.closeConnection();
    }
  }


  public void testReloadOnCreation() throws XynaException {
    testPersistForServerShutdown();
    ODSConnection con = ods.openConnection();
    try {
      //archive should be reloaded
      assertEquals(true, con.containsObject(new OrderInstance(1l)));
      assertEquals(true, con.containsObject(new OrderInstance(2l)));
      assertEquals(true, con.containsObject(new OrderInstance(3l)));
      assertEquals(true, con.containsObject(new OrderInstance(4l)));
      assertEquals(true, con.containsObject(new OrderInstance(5l)));
      assertEquals(true, con.containsObject(new OrderInstance(6l)));
      assertEquals(true, con.containsObject(new OrderInstance(7l)));
      assertEquals(true, con.containsObject(new OrderInstance(8l)));

      ArrayList<OrderInstance> loadedCol = new ArrayList<OrderInstance>();
      loadedCol = (ArrayList<OrderInstance>) con.loadCollection(OrderInstance.class);

      // the order is not preserved in general
      List<String> expectedFqNames = new ArrayList<String>();
      expectedFqNames.add("MyFQName1");
      expectedFqNames.add("MyFQName2");
      expectedFqNames.add("MyFQName3");
      expectedFqNames.add("MyFQName4");
      expectedFqNames.add("MyFQName5");
      expectedFqNames.add("MyFQName6");
      expectedFqNames.add("MyFQName7");
      expectedFqNames.add("MyFQName8");
      outer: for (String s : expectedFqNames) {
        for (int i = 0; i < 8; i++) {
          if (loadedCol.get(i).getOrderType().equals(s)) {
            continue outer;
          }
        }
        fail("Expected FQ name not found");
      }

      //after loading the stream they should now be saved in an accessible state
      OrderInstance wf = new OrderInstance(4l);
      con.queryOneRow(wf);
      assertEquals("MyFQName4", wf.getOrderType());
      for (int i=0; i<8; i++) {
        if (loadedCol.get(i).getId() == 4) {
          assertEquals(loadedCol.get(i).getOrderType(), wf.getOrderType());
        }
      }

      //we can now drop the Table
      con.deleteAll(OrderInstance.class);

      boolean b = con.containsObject(new OrderInstance(1l));
      assertFalse("Object exists though table has been dropped", b);

    } finally {
      con.closeConnection();
    }
  }


  // no longer unsupported

  public void testUnsupportedFeature() throws XynaException {
    ODSConnection con = ods.openConnection();
    try {
      OrderInstance wf = newOrderInstance(1l, "MyFQName");
      con.persistObject(wf);
      wf.setOrderType("my very loooooooooooong name should raise an exception");
      try {
        con.persistObject(wf);
        //fail("Expected exception was not thrown");
      } catch (PersistenceLayerException e) {
        //assertEquals("Updates of variable size not supported", e.getCode());
        fail("Updates of variable size are now supported");
      } catch (Exception e) {
        fail("Expected exception was not thrown");
      }

    } finally {
      con.closeConnection();
    }
  }


  public void testDelete() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      List<OrderInstance> list = new ArrayList<OrderInstance>();
      list.add(new OrderInstance(3l));
      list.add(new OrderInstance(4l));
      con.persistCollection(list);
      Collection<OrderInstance> loaded = con.loadCollection(OrderInstance.class);
      assertEquals(2, loaded.size());

      list.clear();
      list.add(new OrderInstance(3l));
      con.delete(list);
      loaded = con.loadCollection(OrderInstance.class);
      assertEquals(1, loaded.size());
      assertEquals(4l, (long) loaded.iterator().next().getId());
    } finally {
      con.closeConnection();
    }
  }


  public void testDeleteCases() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {

      OrderInstance wf1 = newOrderInstance(1l, "MyFQName1");
      OrderInstance wf2 = newOrderInstance(2l, "MyFQName2");
      OrderInstance wf3 = newOrderInstance(3l, "MyFQName3");
      OrderInstance wf4 = newOrderInstance(4l, "MyFQName4");

      con.persistObject(wf1);
      con.persistObject(wf2);
      con.persistObject(wf3);
      con.persistObject(wf4);

      assertEquals(true, con.containsObject(wf1));
      assertEquals(true, con.containsObject(wf2));
      assertEquals(true, con.containsObject(wf3));
      assertEquals(true, con.containsObject(wf4));

      List<OrderInstance> list = new ArrayList<OrderInstance>();
      list.add(wf1);
      list.add(wf2);
      list.add(wf3);
      list.add(wf4);

      Long timeDeleteAll = System.currentTimeMillis();
      con.delete(list);
      timeDeleteAll = -timeDeleteAll + System.currentTimeMillis();

      OrderInstance wf5 = newOrderInstance(4l, "MyFQName4");
      con.persistObject(wf1);
      con.persistObject(wf2);
      con.persistObject(wf3);
      con.persistObject(wf4);
      con.persistObject(wf5);

      Long timeDeleteSome = System.currentTimeMillis();
      con.delete(list);
      timeDeleteSome = -timeDeleteSome + System.currentTimeMillis();

      // We are assuming that deleting 4 Elements of 4 is faster than deleting (the same) 4 out of 5
      assert (timeDeleteAll < timeDeleteSome);

    } finally {
      con.closeConnection();
    }
  }


  public void testThreadSafety() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      OrderInstance wf1 = newOrderInstance(1l, "number1");
      final OrderInstance wf2 = newOrderInstance(1l, "number2");

      final OrderInstance wfQuery = new OrderInstance(1l);
      Thread writeThread = new Thread() {

        public void run() {
          ODSConnection myCon = ods.openConnection();
          try {
            myCon.persistObject(wf2);
          } catch (XynaException e) {
            fail(e.getMessage());
          } finally {
            try {
              myCon.closeConnection();
            } catch (PersistenceLayerException e) {
              e.printStackTrace();
            }
          }
        }
      };

      //create some threads, one read, one write and try to run against the long persist
      con.persistObject(wf1);
      writeThread.setName("writeThread");
      writeThread.start();

      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      try {
        con.queryOneRow(wfQuery);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        e1.printStackTrace();
      }
      assertEquals(wfQuery.getOrderType(), wf1.getOrderType());


      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      con.commit();

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      try {
        con.queryOneRow(wfQuery);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        e1.printStackTrace();
      }
      assertEquals(wfQuery.getOrderType(), wf2.getOrderType());

    } finally {
      con.closeConnection();
    }
  }


  public void testTIStatusConversions() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      //persist Single and load it
      OrderInstance wf1 = newOrderInstance(1l, "MyFQName1");
      OrderInstance wf2 = newOrderInstance(2l, "MyFQName2");
      OrderInstance wf3 = newOrderInstance(3l, "MyFQName3");
      OrderInstance wf4 = newOrderInstance(4l, "MyFQName4");

      con.persistObject(wf1);
      con.persistObject(wf2);
      con.persistObject(wf3);
      con.persistObject(wf4);

      assertEquals(true, con.containsObject(wf1));
      assertEquals(true, con.containsObject(wf2));
      assertEquals(true, con.containsObject(wf3));
      assertEquals(true, con.containsObject(wf4));

      List<OrderInstance> list = new ArrayList<OrderInstance>();
      //This will perform no conversion, Status is still SINGLE
      Long timeReadSingleCol = System.currentTimeMillis();
      list = (List<OrderInstance>) con.loadCollection(OrderInstance.class);
      timeReadSingleCol = -timeReadSingleCol + System.currentTimeMillis();

      assertEquals(list.get(0).getOrderType(), wf1.getOrderType());
      assertEquals(list.get(1).getOrderType(), wf2.getOrderType());
      assertEquals(list.get(2).getOrderType(), wf3.getOrderType());
      assertEquals(list.get(3).getOrderType(), wf4.getOrderType());

      //this will store them as BLOB
      con.persistCollection(list);

      //This will perform an implicit conversion to SINGLE
      Long timeReadBlobCol = System.currentTimeMillis();
      list = (List<OrderInstance>) con.loadCollection(OrderInstance.class);
      timeReadBlobCol = -timeReadBlobCol + System.currentTimeMillis();

      // Entries don't have to maintain their position, find a better check
      //assertEquals(list.get(0).getOrderType(), wf1.getOrderType());
      //assertEquals(list.get(1).getOrderType(), wf2.getOrderType());
      //assertEquals(list.get(2).getOrderType(), wf3.getOrderType());
      //assertEquals(list.get(3).getOrderType(), wf4.getOrderType());

      //Single has to be faster because no conversion is performed
      assert (timeReadSingleCol < timeReadBlobCol);

    } finally {
      con.closeConnection();
    }
  }


  private ClassLoaderBase cl = null;
  private static final String SHAREDLIB_NAME = "testSharedLib";


  private Storable getStorableLoadedByClassLoader() throws XynaException {
    Class c = null;
    try {
      c = cl.loadClass("test.MyStorable");
    } catch (ClassNotFoundException e) {
      fail(e.getMessage());
    }
    Object o = null;
    try {
      o = c.newInstance();
    } catch (InstantiationException e) {
      fail(e.getMessage());
    } catch (IllegalAccessException e) {
      fail(e.getMessage());
    }
    Storable s = (Storable) o;
    return s;
  }


  public void testLoadClassLoadedObject() throws XynaException {
    File sharedLibJar = new File(Constants.SHAREDLIB_BASEDIR + SHAREDLIB_NAME + "/TestStorable.jar");
    sharedLibJar.getParentFile().mkdirs();
    try {
      //sharedlib erstellen
      try {
        File sourceJar = new File("test/TestStorable.jar");
        logger.debug("copying file " + sourceJar.getCanonicalPath());
        System.out.println("copying file " + sourceJar.getCanonicalPath());
        FileUtils.copyFile(sourceJar, sharedLibJar);
      } catch (IOException e) {
        fail(e.getMessage());
      }

      try {
        cl = new SharedLibClassLoader(SHAREDLIB_NAME, VersionManagement.REVISION_WORKINGSET);
      } catch (XynaException e) {
        fail(e.getMessage());
      }


      //Easy mock konfigurieren, weil serializableclassloaded object auf xynafactory zugreift
      //XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
      //.getClassLoaderDispatcher().getClassLoaderByType(currentClassLoaderType, currentClassLoaderID);

      XynaFactory xf = EasyMock.createMock(XynaFactory.class);
      XynaFactory.setInstance(xf);

      XynaFactoryManagementBase xfmb = EasyMock.createMock(XynaFactoryManagementBase.class);
      EasyMock.expect(xf.getFactoryManagement()).andReturn(xfmb);

      XynaFactoryControl xfc = EasyMock.createMock(XynaFactoryControl.class);
      EasyMock.expect(xfmb.getXynaFactoryControl()).andReturn(xfc);

      ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);
      EasyMock.expect(xfc.getClassLoaderDispatcher()).andReturn(cld);

      EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.SharedLib, SHAREDLIB_NAME, VersionManagement.REVISION_WORKINGSET)).andReturn(cl);

      EasyMock.replay(xf, xfmb, xfc, cld);


      ods.registerStorable(getStorableLoadedByClassLoader().getClass());
      ODSConnection con = ods.openConnection();
      try {
        Storable s = getStorableLoadedByClassLoader();
        con.persistObject(s);
        Storable s2 = getStorableLoadedByClassLoader();
        con.queryOneRow(s2);
        assertEquals(s.getPrimaryKey(), s2.getPrimaryKey());
      } finally {
        con.deleteAll(getStorableLoadedByClassLoader().getClass());
        con.closeConnection();
      }
    } finally {
      //sharedlib loeschen
      sharedLibJar.delete();
    }
  }
}
