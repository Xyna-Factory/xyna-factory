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
package com.gip.xyna.utils.exceptions;



import java.io.File;

import junit.framework.TestCase;

import com.gip.xyna._1_5.exceptions.Codes2;
import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;
import com.gip.xyna.utils.exceptions.utils.FileUtils;

import ex.test.TestException1;
import ex.test.TestException2;
import ex.test.TestException3;
import ex.test.TestException5;
import ex.test2.TestException4;



public class ExceptionStorageTest extends TestCase {

  public ExceptionStorageTest() {
  }


  private static void printXF(XynaFault_ctype xf) {
    System.out.println("code=" + xf.getCode() + " summary = " + xf.getSummary() + " details=" + xf.getDetails());
  }


  public void setUp() throws Exception {
    //damit files als resource verfügbar sind
    FileUtils.copyFile(new File("AdditionalStorage.xml"), new File("classes/AdditionalStorage.xml"));
    FileUtils.copyFile(new File("ExampleExceptionStorage.xml"), new File("classes/ExampleExceptionStorage.xml"));
  }


  public void testLoadExampleStorage_1_0() throws Exception {
    XynaFault_ctype xf;
    xf = new XynaException(Codes2.CODE_UNEXPECTED_ERROR(124)).toXynaFault("EN");
    assertEquals("Summary fehlerhaft", "Unexpected Error 124 occurred", xf.getSummary());
    printXF(xf);
    xf = new XynaException(Codes2.CODE_AN_ERROR).toXynaFault();
    assertEquals("Summary fehlerhaft", "Es ist ein Fehler passiert", xf.getSummary());
    printXF(xf);
    xf = new XynaException(Codes2.CODE_XYNATEST_00002a_Es_ist__0__ein_Fehle).toXynaFault();
    assertEquals("Summary fehlerhaft", "Es ist " + ExceptionHandler.getErrorParameterLocator(0)
        + " ein Fehler passiert", xf.getSummary());
    printXF(xf);
  }
  
  public void testLoadExampleStorage_1_1() throws Exception {
    System.setProperty("SOME_DIR", "test");
    ExceptionStorage.loadFromFile("ExampleExceptionStorage.1.1.xml", 0);
    XynaFault_ctype xf;
    xf = new TestException2(124).toXynaFault("EN");
    assertEquals("Summary fehlerhaft", "Unexpected Error 124 occurred", xf.getSummary());
    printXF(xf);
    xf = new TestException1().toXynaFault();
    assertEquals("Summary fehlerhaft", "Es ist ein Fehler passiert", xf.getSummary());
    printXF(xf);
    xf = new TestException3().toXynaFault();
    assertEquals("Summary fehlerhaft", "Es ist " + ExceptionHandler.getErrorParameterLocator(0)
        + " ein Fehler passiert", xf.getSummary());
    printXF(xf);
    xf = new TestException4(124).toXynaFault();
    assertEquals("Summary fehlerhaft", "Es ist kein 124 Fehler passiert", xf.getSummary());
    printXF(xf);

    xf = new TestException5().toXynaFault();
    printXF(xf);
    assertEquals("Summary fehlerhaft", "Es ist TestEx5 Fehler passiert", xf.getSummary());
  }
}
