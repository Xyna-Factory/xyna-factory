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
package com.gip.xyna.xdev.xlibdev.repository;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import com.gip.xyna.FileUtils;
import com.gip.xyna.xdev.xlibdev.repository.Repository.VersionedObject;
import com.gip.xyna.xfmg.Constants;



public class FileSystemRepositoryTest extends TestCase {

  private static final String CONTENT_1 = "abcä\nx";
  private static final String CONTENT_2 = "abcöy";
  private static final String FILE_1 = "a/b/c.txt";
  private static final String FILE_2 = "a/c/d/c.txt";


  public void testSimpleLifeCycle() throws Exception {
    String basedir = "fsrtest";
    FileUtils.deleteDirectoryRecursively(new File(basedir));
    FileSystemRepository fsr = new FileSystemRepository(basedir, true);
    try {
      assertEquals(-1, fsr.getCurrentRevision());
      assertNull(fsr.getContentOfFileInRevision(FILE_1, 3));
      long r1 =
          fsr.saveFilesInNewRevision(new VersionedObject[] {
                                         new VersionedObject(FILE_1, new ByteArrayInputStream(CONTENT_1
                                             .getBytes(Constants.DEFAULT_ENCODING))),
                                         new VersionedObject(FILE_2, new ByteArrayInputStream(CONTENT_1
                                             .getBytes(Constants.DEFAULT_ENCODING)))}, "a");
      assertEquals(0, r1);
      assertEquals(CONTENT_1, readAndCloseInputStream(fsr.getContentOfFileInRevision(FILE_1, 0)));
      assertEquals(0, fsr.getCurrentRevision());
      assertEquals(CONTENT_1, readAndCloseInputStream(fsr.getContentOfFileInRevision(FILE_1, 3)));
      long r2 = fsr.deleteFilesInNewRevision(new String[] {FILE_1}, "b");
      assertEquals(1, r2);
      assertEquals(CONTENT_1, readAndCloseInputStream(fsr.getContentOfFileInRevision(FILE_1, 0)));
      assertNull(fsr.getContentOfFileInRevision(FILE_1, 3));
      long r3 =
          fsr.saveFilesInNewRevision(new VersionedObject[] {new VersionedObject(FILE_2, new ByteArrayInputStream(CONTENT_2
                                         .getBytes(Constants.DEFAULT_ENCODING)))}, "a2");
      long r4 =
          fsr.saveFilesInNewRevision(new VersionedObject[] {new VersionedObject(FILE_1, new ByteArrayInputStream(CONTENT_2
                                         .getBytes(Constants.DEFAULT_ENCODING)))}, "c");
      assertNull(fsr.getContentOfFileInRevision(FILE_1, r3));
      assertEquals(CONTENT_2, readAndCloseInputStream(fsr.getContentOfFileInRevision(FILE_2, r3)));
      assertEquals(CONTENT_2, readAndCloseInputStream(fsr.getContentOfFileInRevision(FILE_1, r4)));
      assertEquals(CONTENT_2, readAndCloseInputStream(fsr.getContentOfFileInRevision(FILE_2, r4)));
      assertTrue(Arrays.equals(new String[] {FILE_2}, fsr.listFiles(r3)));
    } finally {
      fsr.shutdown();
    }
  }


  public void testReinitialize() throws Exception {
    Random r = new Random();
    String basedir = "fsrtest";
    FileUtils.deleteDirectoryRecursively(new File(basedir));
    FileSystemRepository fsr = new FileSystemRepository(basedir, false);
    int nChanges = 100;
    try {
      for (int i = 0; i < nChanges; i++) {
        String f = getRandomFileName(r);
        String s = getRandomContent(r);
        if (r.nextFloat() < 0.2) {
          InputStream is = fsr.getContentOfFileInRevision(f, i);
          if (is != null) {
            fsr.deleteFilesInNewRevision(new String[] {f}, "delet");
          }
        } else {
          fsr.saveFilesInNewRevision(new VersionedObject[] {new VersionedObject(f, new ByteArrayInputStream(s
                                         .getBytes(Constants.DEFAULT_ENCODING)))}, s);
        }
      }

    } finally {
      fsr.shutdown();
    }
    fsr = new FileSystemRepository(basedir, true);
    try {
      String[] files = fsr.listFiles(nChanges);
      assertTrue(files.length > nChanges / 10);
      Set<String> allContents = new HashSet<String>();
      for (String f : files) {
        allContents.add(readAndCloseInputStream(fsr.getContentOfFileInRevision(f, r.nextInt(nChanges))));
      }
      assertTrue(allContents.size() > nChanges / 10);
    } finally {
      fsr.shutdown();
    }
  }


  public void testParallel() throws Exception {
    int n = 4;
    final int m = 200;
    String basedir = "fsrtest";
    FileUtils.deleteDirectoryRecursively(new File(basedir));
    final FileSystemRepository fsr = new FileSystemRepository(basedir, false);
    final AtomicInteger exceptionCnt = new AtomicInteger(0);
    final CountDownLatch l = new CountDownLatch(n);
    try {
      for (int i = 0; i < n; i++) {
        Thread t = new Thread(new Runnable() {

          public void run() {
            Random r = new Random();
            for (int i = 0; i < m; i++) {
              try {
                fsr.saveFilesInNewRevision(new VersionedObject[] {new VersionedObject(
                                                                                      r.nextBoolean() ? FILE_1 : FILE_2,
                                                                                      new ByteArrayInputStream(
                                                                                                               (r.nextBoolean() ? CONTENT_1 : CONTENT_2)
                                                                                                                   .getBytes(Constants.DEFAULT_ENCODING)))},
                                           "asd");
              } catch (Throwable e) {
                e.printStackTrace();
                exceptionCnt.incrementAndGet();
              }
            }
            l.countDown();
          }

        }, "t" + i);
        t.start();
      }
      l.await();
    } finally {
      fsr.shutdown();
    }
    assertEquals(0, exceptionCnt.get());
  }


  private String getRandomContent(Random r) {
    StringBuilder sb = new StringBuilder();
    int l = r.nextInt(1000);
    for (int i = 0; i < l; i++) {
      sb.append((char) (r.nextInt(100) + 10));
      if (r.nextFloat() < 0.05) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }


  private String getRandomFileName(Random r) {
    int l = r.nextInt(4) + 1;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < l; i++) {
      sb.append((char) ('a' + r.nextInt(25)));
      if (i < l - 1) {
        sb.append("/");
      }
    }
    return sb.toString();
  }


  private String readAndCloseInputStream(InputStream is) throws IOException {
    if (is == null) {
      return null;
    }
    return new Scanner(is, Constants.DEFAULT_ENCODING).useDelimiter("\\A").next();
  }

}
