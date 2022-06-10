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



import java.rmi.RemoteException;

import junit.framework.TestCase;

import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;



/**
 * Methode prepareExceptionHandler() anpassen um die verschiedenen Optionen zu
 * testen.
 * 
 * 
 */
public class ExceptionHandlerTest extends TestCase {

  private static final String TEST_CODE = "XYNA-Test";


  public void tearDown() {
    ExceptionHandler.deleteErrorMessageCache();
    ExceptionHandler.initErrorMessageCache();
  }


  public void testHandleException_noCache() throws RemoteException {
    XynaException xe = new XynaException(TEST_CODE, new String[] {"a", "b"});

    try {
      ExceptionHandler.handleException(xe);
      fail("XynaFault expected");
    } catch (XynaFault_ctype e) {
      assertEquals("Code", XynaCode.NO_MSG_FOR_CODE.getCode(), e.getCode());
      if (!e.getMessage().contains("a, b")) {
        fail("parameter in message expected");
      }
    }
  }


  public void testParameterContainsExceptionParameterSyntax() throws RemoteException {
    try {
      ExceptionHandler.cacheErrorMessage(TEST_CODE, "%0%, %1%");
    } catch (XynaException e) {
      fail("No Exception expected");
    }
    String a = "%1%.%0%";
    String b = "%0%.%1%";
    XynaException xe = new XynaException(TEST_CODE, new String[] {a, b});
    try {
      ExceptionHandler.handleException(xe);
      fail("XynaFault expected");
    } catch (XynaFault_ctype e) {
      assertEquals("Code", TEST_CODE, e.getCode());
      assertEquals("[" + TEST_CODE + "] " + a + ", " + b, e.getMessage());
    }
  }
  
  public void testParametersContainsRegexReplacementCharacters() throws RemoteException {
    try {
      ExceptionHandler.cacheErrorMessage(TEST_CODE, "$1%0%$2, $0%1%$03");
    } catch (XynaException e) {
      fail("No Exception expected");
    }
    String a = "%1%.%0% $% $013.|-((9)\\(\\$\\\\x";
    String b = "$3$1$$ngf%0%.%1%";
    XynaException xe = new XynaException(TEST_CODE, new String[] {a, b});
    try {
      ExceptionHandler.handleException(xe);
      fail("XynaFault expected");
    } catch (XynaFault_ctype e) {
      assertEquals("Code", TEST_CODE, e.getCode());
      assertEquals("[" + TEST_CODE + "] $1" + a + "$2, $0" + b + "$03", e.getMessage());
    }
  }


  public void testCacheErrorMessage_DuplicatedCode() {
    String message = "XynaTestMessage";
    String message2 = "XynaTestMessage2";
    try {
      ExceptionHandler.cacheErrorMessage(TEST_CODE, message);
    } catch (XynaException e) {
      fail("No Exception expected");
    }
    try {
      ExceptionHandler.cacheErrorMessage(TEST_CODE, message2);
      fail("XynaException (duplicated code) expected");
    } catch (XynaException e) {
      assertEquals("Code", XynaCode.DUPLICATE_CODE.getCode(), e.getCode());
    }
  }


  public void testCacheErrorMessage_MoreThen10Args() {
    String message = "XynaTestMessage with arguments ";
    for (int i = 0; i < 13; i++) {
      message += ExceptionHandler.getErrorParameterLocator(i);
      if (i < 12) {
        message += ", ";
      }
    }
    try {
      ExceptionHandler.cacheErrorMessage(TEST_CODE, message);
    } catch (XynaException e) {
      fail("All message formats are allowed");
    }
  }


  public void testHandleException_XynaException() throws XynaException, RemoteException {
    String message = "XynaTestMessage";
    ExceptionHandler.cacheErrorMessage(TEST_CODE, message);
    XynaException xe = new XynaException(TEST_CODE);

    checkXynaException(TEST_CODE, message, xe);
  }


  public void testHandleException_XynaException_Arguments() throws XynaException, RemoteException {
    String message =
        "XynaTestMessage with arguments " + ExceptionHandler.getErrorParameterLocator(0) + " and "
            + ExceptionHandler.getErrorParameterLocator(1);
    ExceptionHandler.cacheErrorMessage(TEST_CODE, message);
    XynaException xe = new XynaException(TEST_CODE, new String[] {"arg1", "arg2"});
    String expectedMessage = "XynaTestMessage with arguments arg1 and arg2";
    checkXynaException(TEST_CODE, expectedMessage, xe);
  }


  public void testHandleException_XynaException_TooManyArguments() throws XynaException, RemoteException {
    String message = "XynaTestMessage with argument " + ExceptionHandler.getErrorParameterLocator(0);
    ExceptionHandler.cacheErrorMessage(TEST_CODE, message);
    XynaException xe = new XynaException(TEST_CODE, new String[] {"arg1", "arg2"});
    String expectedMessage = "XynaTestMessage with argument arg1";
    checkXynaException(xe.getCode(), expectedMessage, xe);
  }


  private void checkXynaException(String code, String message, XynaException e) throws RemoteException {
    try {
      ExceptionHandler.handleException(e);
      fail("XynaFault expected");
    } catch (XynaFault_ctype xf) {
      assertEquals("Code", code, xf.getCode());
      assertEquals("Summary", message, xf.getSummary());
      assertEquals("Details", ExceptionHandler.getStackTraceAsString(e), xf.getDetails());
    }
  }


  public void testHandleException_XynaException_TooLessArguments() throws XynaException, RemoteException {
    String message = "XynaTestMessage with argument %%0";
    ExceptionHandler.cacheErrorMessage(TEST_CODE, message);
    XynaException xe = new XynaException(TEST_CODE);
    String expectedMessage = "XynaTestMessage with argument %%0";

    checkXynaException(xe.getCode(), expectedMessage, xe);
  }


  // TODO: test handleException_XynaException_StackTrace
  /*
   * public void testHandleException_XynaException_StackTrace() { fail("not yet
   * implemented"); }
   */

  public void testHandleException_RemoteException() throws XynaFault_ctype {
    String message = "RemoteExp";
    RemoteException e = new RemoteException(message);
    try {
      ExceptionHandler.handleException(e);
      fail("RemoteException expected");
    } catch (RemoteException re) {
      assertEquals("Message", message, re.getMessage());
    }
  }


  public void testHandleException_Exception() throws RemoteException {
    String message = "Exception";
    Exception e = new Exception(message);
    try {
      ExceptionHandler.handleException(e);
      fail("XynaFault expected");
    } catch (XynaFault_ctype xf) {
      assertEquals("Code " + xf.getCode(), XynaCode.UNKNOWN.getCode(), xf.getCode());
      assertEquals("Summary " + xf.getSummary(), message, xf.getSummary());
      assertEquals("Details " + xf.getDetails(), ExceptionHandler.getStackTraceAsString(e), xf.getDetails());
    }
  }


  public void testHandleException_XynaFault() throws RemoteException {
    String details = "details";
    String summary = "summary";
    XynaFault_ctype fault = new XynaFault_ctype();
    fault.setCode(TEST_CODE);
    fault.setDetails(details);
    fault.setSummary(summary);
    try {
      ExceptionHandler.handleException(fault);
      fail("XynaFault expected");
    } catch (XynaFault_ctype e) {
      assertEquals("Code", TEST_CODE, e.getCode());
      assertEquals("Summary", summary, e.getSummary());
      assertEquals("Details", details, e.getDetails());
    }
  }


  // TODO: test language
  // TODO: test filter
  // TODO: test cached messages

  public void testHandleException_DefaultXynaFault() throws RemoteException {
    NullPointerException npe = new NullPointerException("XynaTest");
    try {
      ExceptionHandler.handleException(npe);
    } catch (XynaFault_ctype e) {
      assertEquals("Code", XynaCode.UNKNOWN.getCode(), e.getCode());
      assertEquals("Summary", npe.getMessage(), e.getSummary());
    }
  }


  public void testParameterIsNull() throws Exception {
    ExceptionHandler.cacheErrorMessage("ABC", "1" + ExceptionHandler.getErrorParameterLocator(0) + "2"
        + ExceptionHandler.getErrorParameterLocator(1) + "3" + ExceptionHandler.getErrorParameterLocator(2));
    try {
      throw ExceptionHandler.toXynaFault(new XynaException("ABC", new String[] {null, ""}));
    } catch (XynaFault_ctype xf) {
      assertEquals("wrong code", "ABC", xf.getCode());
      assertEquals("wrong message", "123" + ExceptionHandler.getErrorParameterLocator(2), xf.getSummary());
    }
  }

}
