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
package com.gip.xyna.utils.ldap;


public class LDAPException extends Exception {
  
  private int code;

  protected enum ErrorCode {     
    NO_ELEMENT_FOUND(100,"no Element found with %"),
    INVALID_MAC(110,"invalidmac: %"),
    MULTI_ELEMENTS_FOUND(120,"more than one Element found with %"),
    ONLY_DYNAMIC_STATIC(130,"configType must be dynamic or static"),
    NO_LDAP_ATTRIBUTE(140,"no LDAP attribute with name=%");
        
    int code;
    String message;
    private ErrorCode(int code, String message){
      this.code=code;
      this.message=message;
    }  
  }
  
  private ErrorCode errorCode;
  
  public LDAPException(ErrorCode errorCode, String[] message){
    super(replaceArray(errorCode.message,"%",message));
    this.errorCode=errorCode;
    setCode(errorCode.code);    
  }
  
  /**
   * @param message
   * @param regexp
   * @param replacement
   * @return
   */
  private static String replaceArray(String message, String regexp, String[] replacement) {
    for (int i = 0; i < replacement.length; i++) {
      message = message.replaceFirst("%", replacement[i]);
    }
    return message;
  }

  public LDAPException(ErrorCode en, String message){
    this(en, new String[]{message});    
  }
  
  public LDAPException(ErrorCode en){
    this(en, new String[]{});
  }
  
  public ErrorCode getErrorCode(){
    return errorCode;
  }

  
  public int getCode() {
    return code;
  }

  
  public void setCode(int code) {
    this.code = code;
  }

}
