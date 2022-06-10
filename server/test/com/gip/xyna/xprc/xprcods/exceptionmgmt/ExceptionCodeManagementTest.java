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
package com.gip.xyna.xprc.xprcods.exceptionmgmt;



import junit.framework.TestCase;

import org.w3c.dom.Document;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageInstance;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageParserFactory;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.memory.XynaMemoryPersistenceLayer;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



public class ExceptionCodeManagementTest extends TestCase {

  private ODS ods;


  public void setUp() throws XynaException {
    ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(42, XynaMemoryPersistenceLayer.class);
    long persId = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                          ODSConnectionType.DEFAULT, null);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, persId);
    
    //TODO exCodeMgmt.setAutomaticCodeGroupGeneration(true); muss nun über Property umgesetzt werden
  }
  
  @Override
  protected void tearDown() throws Exception {
    ODSImpl.clearInstances();
    super.tearDown();
  }
  
  private String getXML(String typePath, String code) {
    String codeAtt = "";
    if (code != null) {
      codeAtt = "Code=\"" + code + "\"";
    }
    String xml = "<ExceptionStore xmlns=\"http://www.gip.com/xyna/3.0/utils/message/storage/1.1\""
      + " Name=\"ExampleExceptionStore\" Version=\"1.0\" Type=\"ExceptionMasterFile\""
      + " DefaultLanguage=\"DE\">"
      + "<Description>zeigt, wie man Fehlernachrichten in XML verwaltet.</Description>"
      + "<ExceptionType TypeName=\"TestException1\" "+ codeAtt+ " TypePath=\"" + typePath + "\" >"
      + "<MessageText Language=\"DE\">Es ist ein Fehler passiert</MessageText>" + "</ExceptionType>"
      + "</ExceptionStore>";
    return xml;
  }


  public void testCheckExceptionCodeNormal() throws XynaException {
    BlackExceptionCodeManagement exCodeMgmt = new BlackExceptionCodeManagement(ods);
    exCodeMgmt.init();
    //exCodeMgmt.setAutomaticCodeGroupGeneration(true);
       
    Document doc = XMLUtils.parseString(getXML("ex.test", null), true);
    exCodeMgmt.checkExceptionCode(doc);
    ExceptionStorageInstance esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
    assertEquals(1, esi.getEntries().size());
    assertEquals("XYNA-00000", esi.getEntries().get(0).getCode());
  }
  
  public void testCheckSaveAllocatedCode() throws XynaException {
    BlackExceptionCodeManagement exCodeMgmt = new BlackExceptionCodeManagement(ods);
    exCodeMgmt.init();
    //exCodeMgmt.setAutomaticCodeGroupGeneration(true);
       
    for (int i = 0; i<1000; i++) {
      Document doc = XMLUtils.parseString(getXML("ex.test", null), true);
      exCodeMgmt.checkExceptionCode(doc);
      ExceptionStorageInstance esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
      assertEquals(1, esi.getEntries().size());
      String expectedNumber = "" + i;
      if (i<10) {
        expectedNumber = "00" + expectedNumber;
      } else if (i<100) {
        expectedNumber = "0" + expectedNumber;
      }
      assertEquals("XYNA-00" + expectedNumber, esi.getEntries().get(0).getCode());
    }

  }
  
  public void testDontSubstituteExistingCode() throws XynaException {
    BlackExceptionCodeManagement exCodeMgmt = new BlackExceptionCodeManagement(ods);
    exCodeMgmt.init();
    //exCodeMgmt.setAutomaticCodeGroupGeneration(true);
       
    Document doc = XMLUtils.parseString(getXML("ex.test", "somecode"), true);
    exCodeMgmt.checkExceptionCode(doc);
    ExceptionStorageInstance esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
    assertEquals(1, esi.getEntries().size());
    assertEquals("somecode", esi.getEntries().get(0).getCode());
  }
  
  public void testCodeGroupNameSpecificCodes() throws XynaException {
    BlackExceptionCodeManagement exCodeMgmt = new BlackExceptionCodeManagement(ods);
    exCodeMgmt.init();
    //exCodeMgmt.setAutomaticCodeGroupGeneration(true);
   
    for (int i = 0; i<10; i++) {
      Document doc = XMLUtils.parseString(getXML("ex" + i + ".test", null), true);
      exCodeMgmt.checkExceptionCode(doc);
      ExceptionStorageInstance esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
      assertEquals(1, esi.getEntries().size());
      int k = (i*100);
      String expectedNumber = "" + k;
      if (k < 10) {
        expectedNumber = "000" + expectedNumber;
      } else if (k<100) {
        expectedNumber = "00" + expectedNumber;
      } else if (k<1000) {
        expectedNumber = "0" + expectedNumber;
      }
      assertEquals("XYNA-0" + expectedNumber, esi.getEntries().get(0).getCode());
    }

  }
  
  public void testWithODS() throws XynaException {
    BlackExceptionCodeManagement exCodeMgmt = new BlackExceptionCodeManagement(ods);
    exCodeMgmt.init();
    //exCodeMgmt.setAutomaticCodeGroupGeneration(true);
       
    Document doc = XMLUtils.parseString(getXML("ex.test", null), true);
    exCodeMgmt.checkExceptionCode(doc);
    ExceptionStorageInstance esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
    assertEquals(1, esi.getEntries().size());
    assertEquals("XYNA-00000", esi.getEntries().get(0).getCode());
    
    exCodeMgmt = new BlackExceptionCodeManagement(ods);
    exCodeMgmt.init();
    //exCodeMgmt.setAutomaticCodeGroupGeneration(true);
       
    doc = XMLUtils.parseString(getXML("ex.test", null), true);
    exCodeMgmt.checkExceptionCode(doc);
    esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
    assertEquals(1, esi.getEntries().size());
    assertEquals("XYNA-00001", esi.getEntries().get(0).getCode());
  }
  
  public void testPredefinedCodeGroups() throws XynaException {
    BlackExceptionCodeManagement exCodeMgmt = new BlackExceptionCodeManagement(ods);
    exCodeMgmt.init();
    //exCodeMgmt.setAutomaticCodeGroupGeneration(true);
    exCodeMgmt.createCodeGroup("predefined");
    exCodeMgmt.addExceptionCodePattern("predefined", "XYNA-[[]]", 0, 1000, 5);
    exCodeMgmt.addExceptionCodePattern("predefined", "XYNA-[[]]", 1000, 45, 5);
    exCodeMgmt.addExceptionCodePattern("predefined", "XYNA-[[]]", 1145, 2, 5);
       
    Document doc = XMLUtils.parseString(getXML("ex.test", null), true);
    exCodeMgmt.checkExceptionCode(doc);
    ExceptionStorageInstance esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
    assertEquals(1, esi.getEntries().size());
    assertEquals("XYNA-01045", esi.getEntries().get(0).getCode());
    
    doc = XMLUtils.parseString(getXML("ex2.test", null), true);
    exCodeMgmt.checkExceptionCode(doc);
    esi = ExceptionStorageParserFactory.getParser(doc).parse(true, 0);
    assertEquals(1, esi.getEntries().size());
    assertEquals("XYNA-01147", esi.getEntries().get(0).getCode());
   
  }

}
