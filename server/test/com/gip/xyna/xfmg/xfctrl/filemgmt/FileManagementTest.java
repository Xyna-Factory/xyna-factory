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

package com.gip.xyna.xfmg.xfctrl.filemgmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

import junit.framework.TestCase;

import com.gip.xyna.FileUtils;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class FileManagementTest extends TestCase {

  private final static File STORAGE_DIR = new File("/tmp/filemgmt");
  private final static String TEST_FILE_NAME = "testFile.txt";
  private final static String TEST_FILE_PATH = "test/com/gip/xyna/xfmg/xfctrl/filemgmt/";
  private final static File TEST_FILE = new File(TEST_FILE_PATH + TEST_FILE_NAME);
  private final static String TEST_FILE_CONTENT = "this is a test file" + Constants.LINE_SEPARATOR
                                                + "with two lines"      + Constants.LINE_SEPARATOR;
  private final static String TEST_LOCATION = "test";
  private final static String FILEMGMT_TIMEOUT = "2 s";
  private final static int FILEMGMT_RETRIES = -1;
  
  private FileManagement fileMgmt;
  private IDGenerator idGen;
  
  
  @Override
  protected void setUp() throws Exception {
    initProperties();
    idGen = EasyMock.createMock(IDGenerator.class);
    final AtomicLong longGenerator = new AtomicLong(1);
    EasyMock.expect(idGen.getUniqueId()).andAnswer(new IAnswer<Long>() {

      public Long answer() throws Throwable {
        return longGenerator.getAndIncrement();
      }
    }).anyTimes();
    EasyMock.replay(idGen);
    
    fileMgmt = new FileManagement();
    initFileManagement(fileMgmt, STORAGE_DIR, idGen);
    super.setUp();
  }
  
  private void initProperties() throws PersistenceLayerException {
    initProperties(FILEMGMT_TIMEOUT, FILEMGMT_RETRIES);
  }
  
  private void initProperties(String duration) throws PersistenceLayerException {
    initProperties(duration, FILEMGMT_RETRIES);
  }
  
  private void initProperties(int retries) throws PersistenceLayerException {
    initProperties(FILEMGMT_TIMEOUT, retries);
  }
  
  private void initProperties(String duration, int retries) throws PersistenceLayerException {
    XynaProperty.FILE_MANAGEMENT_DEFAULT_TIMEOUT.set(Duration.valueOf(duration));
    XynaProperty.FILE_MANAGEMENT_STATE_TRANSITION_RETRIES.set(retries);
  }
  
  private void initFileManagement(FileManagement fileMgmt, File tmpDir, IDGenerator idGen) {
    try {
      Method init = FileManagement.class.getDeclaredMethod("initFileMgmt", File.class, IDGenerator.class);
      init.setAccessible(true);
      init.invoke(fileMgmt, tmpDir, idGen);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to initialize FileMgmt", e);
    }
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    fileMgmt.shutdown();
    FileUtils.deleteDirectoryRecursively(STORAGE_DIR);
    super.tearDown();
  }
  
  
  @Test
  public void testDefaultTimeoutCase() throws XynaException {
    initProperties();
    String id = storeDefaultTestFile();
    assertEquals("Id does not match the expected value", String.valueOf(idGen.getUniqueId() - 1), id);
    File upload = getTestUploadFile(id);
    assertTrue("Stored file should exist", upload.exists());
    String fileContent = FileUtils.readFileAsString(upload);
    assertEquals("Payload should have been preserved", TEST_FILE_CONTENT,  fileContent);
    sleep(getFileTimoutMillis() / 2);
    assertTrue("Stored file should exist", upload.exists());
    sleep(getFileTimoutMillis());
    assertFalse("Stored file should have been deleted", upload.exists());
  }
  
  
  @Test
  public void testDefaultDeleteTrip() throws XynaException {
    initProperties();
    String id = storeDefaultTestFile();
    assertEquals("Id does not match the expected value", String.valueOf(idGen.getUniqueId() - 1), id);
    File upload = getTestUploadFile(id);
    assertTrue("Stored file should exist", upload.exists());
    String fileContent = FileUtils.readFileAsString(upload);
    assertEquals("Payload should have been preserved", TEST_FILE_CONTENT,  fileContent);
    sleep(getFileTimoutMillis() / 2);
    assertTrue("Stored file should exist", upload.exists());
    fileMgmt.remove(id);
    assertFalse("Stored file should have been deleted", upload.exists());
  }
  
  
  @Test
  public void testDefaultDownloadTrip() throws XynaException {
    initProperties();
    String id = storeDefaultTestFile();
    TransientFile file = fileMgmt.retrieve(id);
    InputStream stream = file.openInputStream();
    try {
      String fileContent = read(file, stream);
      assertEquals("Payload should have been preserved", TEST_FILE_CONTENT,  fileContent);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    fileMgmt.remove(id);
    
  }
  
  
  @Test
  public void testUnclosedStreamGarbageCollection() throws XynaException, InterruptedException, ExecutionException {
    initProperties();
    final String id = storeDefaultTestFile();
    ExecutorService singleExec = Executors.newSingleThreadExecutor();
    TransientFile file = fileMgmt.retrieve(id);
    InputStream stream = file.openInputStream();
    String fileContent = read(file, stream);
    assertEquals("Payload should have been preserved", TEST_FILE_CONTENT,  fileContent);
    File upload = getTestUploadFile(id);
    assertTrue("Stored file should exist", upload.exists());
    
    Future<Boolean> removeFuture = singleExec.submit(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        return fileMgmt.remove(id);
      }
    });
    
    sleep(100);
    
    assertFalse("File should be locked from retrieve", removeFuture.isDone());
    
    file = null;
    stream = null;
    
    System.gc();
    sleep(500);
    System.gc();
    
    assertTrue("File should be freed from collected stream", removeFuture.isDone());
    assertTrue("File should have been succesfully deleted", removeFuture.get());
  }
  
  
  @Test
  public void testMultipleUncontendedReaders() throws XynaException, InterruptedException, ExecutionException {
    initProperties("30 s");
    final int CONCURRENCY = 8;
    final AtomicBoolean run = new AtomicBoolean(true);
    
    final ExecutorService exec = Executors.newFixedThreadPool(CONCURRENCY); 
    
    Map<String, Future<Void>> allFutures = new HashMap<String, Future<Void>>();
    final String id = storeDefaultTestFile();
    
    for (int i=0; i < CONCURRENCY; i++) {
      allFutures.put(id, exec.submit(new Callable<Void>() {
        public Void call() throws Exception {
          while (run.get()) {
            TransientFile file = fileMgmt.retrieve(id);
            int state = getFileState(file);
            assertTrue("It should never be locked", state >= 0);
            InputStream stream = file.openInputStream();
            try {
              String fileContent = read(file, stream);
              assertEquals("Payload should have been preserved", TEST_FILE_CONTENT,  fileContent);
            } finally {
              stream.close();
            }
          }
          return null;
        }
      }));
    }
    sleep(3000);
    run.set(false);
    for (Future<Void> future : allFutures.values()) {
      // checking for ExecutionException
      future.get();
    }
  }
  
  
  @Test
  public void testStarvingWriter() throws Throwable {
    initProperties("30 s", 20);
    final int READ_CONCURRENCY = 8;
    final AtomicBoolean run = new AtomicBoolean(true);
    final AtomicInteger concurrentReads = new AtomicInteger(0);
    
    final ExecutorService exec = Executors.newFixedThreadPool(READ_CONCURRENCY); 
    
    Map<String, Future<Void>> allFutures = new HashMap<String, Future<Void>>();
    final String id = storeDefaultTestFile();
    
    for (int i=0; i < READ_CONCURRENCY; i++) {
      allFutures.put(id, exec.submit(new Callable<Void>() {
        public Void call() throws Exception {
          while (run.get()) {
            try {
              TransientFile file = fileMgmt.retrieve(id);
              InputStream stream = file.openInputStream();
              try {
                concurrentReads.incrementAndGet();
                int state = getFileState(file);
                assertTrue("It should never be locked", state >= 0);
                String fileContent = read(file, stream);
                assertEquals("Payload should have been preserved", TEST_FILE_CONTENT,  fileContent);
              } finally {
                boolean success = false;
                while (run.get() && !success) {
                  int oldCurrentReads = concurrentReads.get();
                  if (oldCurrentReads > 1) {
                    success = concurrentReads.compareAndSet(oldCurrentReads, oldCurrentReads - 1);
                  }
                  Thread.sleep(10);
                }
                stream.close();
              }
            } catch (Exception e) {
              if (run.get()) {
                throw e;
              }
            }
          }
          return null;
        }
      }));
    }
    try {
      sleep(300);
      assertFalse("Should not have worked", fileMgmt.remove(id));
      sleep(300);
      assertFalse("Should not have worked", fileMgmt.remove(id));
      sleep(300);
      assertFalse("Should not have worked", fileMgmt.remove(id));
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    } finally {
      run.set(false);
      for (Future<Void> future : allFutures.values()) {
        // checking for ExecutionException
        future.get();
      }
    }
    
    assertTrue("Should have worked", fileMgmt.remove(id));
  }
  
  
  @Test
  public void testSelfBlockingAction() throws XynaException, InterruptedException, ExecutionException {
    initProperties(0);
    String id = storeDefaultTestFile();
    TransientFile file = fileMgmt.retrieve(id);
    InputStream stream = file.openInputStream();
    try {
      assertFalse("Our stream is still open!", fileMgmt.remove(id));
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    assertTrue("Our stream is now closed.", fileMgmt.remove(id));
  }
  
  
  @Test
  public void testReentrantStreamBehaviour() throws XynaException, InterruptedException, ExecutionException {
    initProperties(0);
    String id = storeDefaultTestFile();
    TransientFile file = fileMgmt.retrieve(id);
    InputStream stream1 = file.openInputStream();
    InputStream stream2 = file.openInputStream();
    InputStream stream3 = file.openInputStream();
    try {
      try {
        assertFalse("Our stream is still open!", fileMgmt.remove(id));
        stream1.close();
        assertTrue("Our stream is now closed.", fileMgmt.remove(id));
        assertEquals("Should have been same instance", stream1, stream2);
        assertEquals("Should have been same instance", stream2, stream3);
      } finally {
        stream1.close();
        stream2.close();
        stream3.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  // uses reflection to inspect internal variables, might break on TransientFile refactorings
  private int getFileState(TransientFile file) {
    try {
      Class<?> clazzWrapper = Class.forName("com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement$InternalTransientFileWrapper");
      Field wrappedField = clazzWrapper.getDeclaredField("wrappedFile");
      wrappedField.setAccessible(true);
      Object internalFile = wrappedField.get(file);
      
      Class<?> clazz = Class.forName("com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement$InternalTransientFile");
      Field stateField = clazz.getDeclaredField("state");
      stateField.setAccessible(true);
      AtomicInteger state = (AtomicInteger) stateField.get(internalFile);
      return state.get();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private InputStream openTestFileInputStream() {
    try {
      return new FileInputStream(TEST_FILE);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Failed to openTestFileInputStream", e);
    }
  }
  
  
  private String storeDefaultTestFile() {
    InputStream is = openTestFileInputStream();
    try {
      return fileMgmt.store(TEST_LOCATION, TEST_FILE_NAME, is);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        throw new RuntimeException("Failed to close testFileInputStream", e);
      }
    }
  }
  
  private File getTestUploadFile(String id) {
    return new File(STORAGE_DIR + "/" + TEST_LOCATION + "/" + id);
  }
  
  
  private long getFileTimoutMillis() {
    return Duration.valueOf(FILEMGMT_TIMEOUT).getDurationInMillis();
  }
  
  private String read(TransientFile file, InputStream openedInputStream) {
    StringBuilder sb = new StringBuilder();
    try {
      byte[] buffer = new byte[2048];
      int countRead=0;
      while((countRead=openedInputStream.read(buffer)) != -1){
          String readData = new String(buffer, 0, countRead);
          sb.append(readData);
      }
      return sb.toString();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private void sleep(long ms) {
    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
