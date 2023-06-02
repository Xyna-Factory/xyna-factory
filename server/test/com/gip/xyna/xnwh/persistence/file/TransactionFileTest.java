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
package com.gip.xyna.xnwh.persistence.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.gip.xyna.XynaRuntimeException;




public class TransactionFileTest extends TestCase {
  
  private static final File tmpDir = new File(System.getProperty("java.io.tmpdir") + "/TransactionFileTest/tmp");
  private static final File tmp2Dir = new File(System.getProperty("java.io.tmpdir") + "/TransactionFileDaten");
  
  private static final String TESTSTRING1 = "Bla Blubb blubb bla";
  private static final String TESTSTRING2 = "Foo Bar foo bar";
  private static final String TESTSTRING3 = "Bla Bar foo blubb";
  
  
  public void setUp() {
    tmpDir.mkdirs();
    tmp2Dir.mkdirs();
    File []childfiles = tmpDir.listFiles();
    for(File child : childfiles) {
      child.delete();
    }
    childfiles = tmp2Dir.listFiles();
    for(File child : childfiles) {
      child.delete();
    }
  }
  
  public void tearDown() {
    File []childfiles = tmpDir.listFiles();
    for(File child : childfiles) {
      child.delete();
    }
    tmpDir.delete();
    childfiles = tmp2Dir.listFiles();
    for(File child : childfiles) {
      child.delete();
    }
    tmp2Dir.delete();
  }
  
  
  // Originaldatei existiert nicht
  public void testCreateNewFile() {
    
    File oFile = new File(tmp2Dir, "oFile");
    TransactionFile tFile = new TransactionFile(oFile, tmpDir);
    
    Assert.assertTrue(!oFile.exists());
    Assert.assertTrue(tFile.exists());
    
    writeTestContent(TESTSTRING1, tFile);
    
    Assert.assertTrue(!oFile.exists());
    Assert.assertTrue(tFile.exists());
    
    tFile.commit();
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(!tFile.exists());
    
    Assert.assertEquals(TESTSTRING1, readTestContent(oFile));
  }

  // Originaldatei existiert 
  public void testOverwriteExistingFile() {
    
    File oFile = new File(tmp2Dir, "oFile");
    
    writeTestContent(TESTSTRING2, oFile);
    
    Assert.assertEquals(TESTSTRING2, readTestContent(oFile));
    Assert.assertTrue(oFile.exists());
    
    TransactionFile tFile = new TransactionFile(oFile, tmpDir);
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(tFile.exists());
    
    writeTestContent(TESTSTRING1, tFile);
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(tFile.exists());
    Assert.assertEquals(TESTSTRING2, readTestContent(oFile));
    
    tFile.commit();
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(!tFile.exists());
    
    Assert.assertEquals(TESTSTRING1, readTestContent(oFile));
     
  }
  
  // Parralleler Zugriff
  public void testTwoAccess() {
    
    File oFile = new File(tmp2Dir, "oFile");
    
    writeTestContent(TESTSTRING2, oFile);
    
    Assert.assertEquals(TESTSTRING2, readTestContent(oFile));
    Assert.assertTrue(oFile.exists());
    
    TransactionFile tFile1 = new TransactionFile(oFile, tmpDir);
    TransactionFile tFile2 = new TransactionFile(oFile, tmpDir);
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(tFile1.exists());
    Assert.assertTrue(tFile2.exists());
    
    writeTestContent(TESTSTRING1, tFile1);
    writeTestContent(TESTSTRING3, tFile2);
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(tFile2.exists());
    Assert.assertTrue(tFile2.exists());
    Assert.assertEquals(TESTSTRING2, readTestContent(oFile));
    Assert.assertEquals(TESTSTRING1, readTestContent(tFile1));
    Assert.assertEquals(TESTSTRING3, readTestContent(tFile2));
    
    tFile1.commit();
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(!tFile1.exists());
    Assert.assertTrue(tFile2.exists());
    Assert.assertEquals(TESTSTRING3, readTestContent(tFile2));
    Assert.assertEquals(TESTSTRING1, readTestContent(oFile));
    
    tFile2.commit();
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(!tFile1.exists());
    Assert.assertTrue(!tFile2.exists());
    Assert.assertEquals(TESTSTRING3, readTestContent(oFile));
  }
 
  
  // commit zweimal ausführen
  public void testCommitTwice() {
    
    File oFile = new File(tmp2Dir, "oFile");
    TransactionFile tFile = new TransactionFile(oFile, tmpDir);
    
    Assert.assertTrue(!oFile.exists());
    Assert.assertTrue(tFile.exists());
    
    writeTestContent(TESTSTRING1, tFile);
    
    Assert.assertTrue(!oFile.exists());
    Assert.assertTrue(tFile.exists());
    
    tFile.commit();
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(!tFile.exists());
    
    Assert.assertEquals(TESTSTRING1, readTestContent(oFile));
    
    try {
      tFile.commit();
      fail("Exception erwartet.");
    } catch(XynaRuntimeException e) {
      Assert.assertEquals("Commit is not allowed to be called twice.", e.getMessage());
    }
  }
  
  // temporäre Datei existiert im gleichen Verzeichnis
  public void testCreateTempInSameDirectory() {
    
    File oFile = new File(tmp2Dir, "oFile");
    TransactionFile tFile = new TransactionFile(oFile);
    
    Assert.assertTrue(!oFile.exists());
    Assert.assertTrue(tFile.exists());
    Assert.assertEquals(tmp2Dir.getAbsolutePath(), tFile.getParent());
    
    
    writeTestContent(TESTSTRING1, tFile);
    
    Assert.assertTrue(!oFile.exists());
    Assert.assertTrue(tFile.exists());
    
    tFile.commit();
    
    Assert.assertTrue(oFile.exists());
    Assert.assertTrue(!tFile.exists());
    
    Assert.assertEquals(TESTSTRING1, readTestContent(oFile));
    
    try {
      tFile.commit();
      fail("Exception erwartet.");
    } catch(XynaRuntimeException e) {
      Assert.assertEquals("Commit is not allowed to be called twice.", e.getMessage());
    }
  }
  
  
  // Hilfsmethode; schreibt Daten in Datei
  private void writeTestContent(String content, File file) {
    try {
      FileWriter writer = new FileWriter(file);
      writer.append(content);
      writer.close();
    } catch(Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
  
//Hilfsmethode; liest Daten in Datei
  private String readTestContent(File file) {
    StringBuilder tmp = new StringBuilder();
    try {
      String line;
      BufferedReader reader = new BufferedReader(new FileReader(file));
      while((line = reader.readLine()) != null) {
        tmp.append(line);
      }
      reader.close();
    } catch(Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
    return tmp.toString();
  }
   
  
  

}
