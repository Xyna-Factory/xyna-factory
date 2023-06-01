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
package com.gip.xyna.utils.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 *
 */
public class XynaExceptionTest extends TestCase {

  public void testCreationArray() {
    String code = "code";
    String a1 = "a2";
    String a2 = "a1";
    XynaException xe1 = new XynaException(code, new String[] { a1, a2 });
    XynaException xe2 = new XynaException(new String[] { code, a1, a2 });
    assertEquals("code", xe1.getCode(), xe2.getCode());
    assertEquals("args length", xe1.getArgs().length, xe2.getArgs().length);
    assertEquals("args 1", xe1.getArgs()[0], xe2.getArgs()[0]);
    assertEquals("args 2", xe1.getArgs()[1], xe2.getArgs()[1]);
  }
  
  public void testNullCode() {
    XynaException x = new XynaException((String)null);
    assertNotNull(x.getMessage());
    System.out.println(x.getMessage());
  }
  
  public void testMultipleCauses() {
    ArrayList<Exception> exceptions = new ArrayList<Exception>();
    int causesDepth1 = 3;
    int causesDepth2 = 2;
    int stackDepth1 = 4;
    int stackDepth2 = 7;
    try {
      getExceptionWithStackTrace(stackDepth1, causesDepth1);
    } catch (Exception e1) {
      exceptions.add(e1);
    }
    try {
      getExceptionWithStackTrace(stackDepth2, causesDepth2);
    } catch (Exception e2) {
      e2.printStackTrace();
      exceptions.add(e2);
    }    
    XynaException x = new XynaException("").initCauses(exceptions.toArray(new Throwable[0]));
    x.printStackTrace();
    StringWriter sw = new StringWriter(); 
    x.printStackTrace(new PrintWriter(sw));
    //1 ganz oben, causes sind klar, 2 wegen zusammenfassung von "multiple causes"
    assertEquals(1+causesDepth1 + causesDepth2 + 2, sw.toString().split("textA").length);
    assertEquals(3, sw.toString().split("textB").length);
    assertEquals(1+causesDepth1*2 + stackDepth1 + causesDepth2*2 + stackDepth2, sw.toString().split("getExceptionWithStackTrace\\(XynaExceptionTest.java:68\\)").length);
  }
  
  private void getExceptionWithStackTrace(int length, int causesLength) throws Exception {
    if (length > 0) {
      getExceptionWithStackTrace(length -1, causesLength);
    } else {
      if (causesLength > 0) {
        try {        
          getExceptionWithStackTrace(2, causesLength-1);
        } catch (Exception e) {
          throw new Exception("textA", e);
        }
      } else {
        throw new Exception("textB");
      }
    }
  }

}
