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
package com.gip.xyna.utils.exceptions.exceptioncode;

import junit.framework.TestCase;


public class ExceptionCodeManagementTest extends TestCase {

  public void testCreateCodeGroupNormal() throws DuplicateCodeGroupException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
  }
  
  public void testCreateCodeGroupExisting() {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    try {
      ecm.createCodeGroup("name");
    } catch (DuplicateCodeGroupException e) {
      fail(e.getMessage());
    }
    DuplicateCodeGroupException exception = null;
    try {
      ecm.createCodeGroup("name");
    } catch (DuplicateCodeGroupException e) {
      exception = e;
    }
    assertNotNull(exception);
  }
  
  public void testRemoveCodeGroup() throws CodeGroupUnknownException, DuplicateCodeGroupException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    ecm.removeCodeGroup("name");
  }
  
  public void testRemoveNonExistingCodeGroup() throws DuplicateCodeGroupException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    CodeGroupUnknownException exception = null;
    try {
      ecm.removeCodeGroup("name2");
    } catch (CodeGroupUnknownException e) {
      exception = e;
    }
    assertNotNull(exception);
  }
  
  public void testAddPattern() throws DuplicateCodeGroupException, CodeGroupUnknownException, InvalidPatternException, OverlappingCodePatternException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    ecm.addExceptionCodePattern("name", "XYNA-[[]]", 412, 14, 5);    
    ecm.addExceptionCodePattern("name", "[[]]", 412, 14, 5);    
    ecm.addExceptionCodePattern("name", "XYNA-[[]]GE", 412, 14, 5);    
  }
  
  public void testAddInvalidPattern() throws DuplicateCodeGroupException, CodeGroupUnknownException, OverlappingCodePatternException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    InvalidPatternException ipe = null;
    try {
      ecm.addExceptionCodePattern("name", "XYNA-[[]peng", 412, 14, 5);   
    } catch (InvalidPatternException e) {
      ipe = e;
    }
    assertNotNull(ipe);
  }
  
  public void testAddPatternToUnknownCodeGroup() throws DuplicateCodeGroupException, InvalidPatternException, OverlappingCodePatternException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    CodeGroupUnknownException exception = null;
    try {
      ecm.addExceptionCodePattern("name2", "XYNA-[[]]", 412, 14, 5);    
    } catch (CodeGroupUnknownException e) {
      exception = e;
    }
    assertNotNull(exception);
  }
  
  public void testAddPatternInvalidPadding() throws DuplicateCodeGroupException, CodeGroupUnknownException, InvalidPatternException, OverlappingCodePatternException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    IllegalArgumentException exception = null;
    try {      
      ecm.addExceptionCodePattern("name", "XYNA-[[]]", 2, 140, 2);    
    } catch (IllegalArgumentException e) {
      exception = e;
    }
    assertNotNull(exception);
  }
  
  public void testOverlappingCodes() throws CodeGroupUnknownException, InvalidPatternException, DuplicateCodeGroupException, OverlappingCodePatternException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    ecm.createCodeGroup("name2");
    ecm.addExceptionCodePattern("name", "XYNA-[[]]", 2, 140, 3);    
    OverlappingCodePatternException exception = null;
    try {      
      ecm.addExceptionCodePattern("name2", "XYNA-[[]]", 2, 140, 3);    
    } catch (OverlappingCodePatternException e) {
      exception = e;
    }
    assertNotNull("expected " + OverlappingCodePatternException.class.getSimpleName(), exception);
    
    exception = null;
    try {      
      ecm.addExceptionCodePattern("name2", "XYNA-[[]]", 0, 3, 3);    
    } catch (OverlappingCodePatternException e) {
      exception = e;
    }
    assertNotNull("expected " + OverlappingCodePatternException.class.getSimpleName(), exception);

    exception = null;
    try {      
      ecm.addExceptionCodePattern("name2", "XYNA-[[]]", 141, 1, 3);    
    } catch (OverlappingCodePatternException e) {
      exception = e;
    }
    assertNotNull("expected " + OverlappingCodePatternException.class.getSimpleName(), exception);

    exception = null;
    try {      
      ecm.addExceptionCodePattern("name", "XYNA-[[]]", 141, 1, 3);    
    } catch (OverlappingCodePatternException e) {
      exception = e;
    }
    assertNotNull("expected " + OverlappingCodePatternException.class.getSimpleName(), exception);

    exception = null;
    try {      
      ecm.addExceptionCodePattern("name", "XYNA-[[]]", 10, 3, 3);    
    } catch (OverlappingCodePatternException e) {
      exception = e;
    }
    assertNotNull("expected " + OverlappingCodePatternException.class.getSimpleName(), exception);

    ecm.addExceptionCodePattern("name", "XYNA-[[]]", 10, 3, 4);    
    ecm.addExceptionCodePattern("name", "XYNA-[[]]", 141, 1, 4);    
    ecm.addExceptionCodePattern("name2", "XYNA-[[]]", 0, 3, 4);    
    ecm.addExceptionCodePattern("name2", "XYNA-[[]]", 5, 5, 4);    
  }
  
  public void testCreateCode() throws DuplicateCodeGroupException, CodeGroupUnknownException, InvalidPatternException, NoCodeAvailableException, OverlappingCodePatternException {
    ExceptionCodeManagement ecm = new ExceptionCodeManagement();
    ecm.createCodeGroup("name");
    ecm.addExceptionCodePattern("name", "XYNA-[[]]", 412, 14, 5);
    ecm.addExceptionCodePattern("name", "abc[[]]def", 0, 50, 2);
    for (int i = 0; i<14; i++) {
      String code = ecm.createExceptionCode("name");
      assertEquals("XYNA-00" + (412+i), code);
    }
    for (int i = 0; i<50; i++) {
      String code = ecm.createExceptionCode("name");
      String paddedNumber = "" + i;
      if (i<10) {
        paddedNumber = "0" + paddedNumber;
      }
      assertEquals("abc" + paddedNumber + "def", code);
    }
    NoCodeAvailableException exception = null;
    try {
      String code = ecm.createExceptionCode("name");
    } catch (NoCodeAvailableException e) {
      exception = e;
    }
    assertNotNull(exception);
  }
  
}
