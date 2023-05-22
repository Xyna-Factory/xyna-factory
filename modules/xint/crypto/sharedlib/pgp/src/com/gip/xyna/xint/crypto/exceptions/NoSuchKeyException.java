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
package com.gip.xyna.xint.crypto.exceptions;


public class NoSuchKeyException extends XCryptoException {

  private static final long serialVersionUID = 1L;
  
  private String userOrKeyID;

  public NoSuchKeyException(long keyID) {
    super(generateMessage(keyID));
    userOrKeyID = String.valueOf(keyID);
  }
  
  public NoSuchKeyException(long keyID, Throwable t) {
    super(generateMessage(keyID), t);
    userOrKeyID = String.valueOf(keyID);
  }
  
  public NoSuchKeyException(String userID) {
    super(generateMessage(userID));
    userOrKeyID = String.valueOf(userID);
  }
  
  public NoSuchKeyException(String msg, boolean overloadDistinction) {
    super(msg);
  }
  
  public NoSuchKeyException(String userID, Throwable t) {
    super(generateMessage(userID), t);
  }
  
  private static String generateMessage(long keyID) {
    return "Key with keyID '" + keyID + "' could not be found.";
  }
  
  private static String generateMessage(String userID) {
    return "A key with userID '" + userID + "' could not be found.";
  }
  
  public String getUserOrKeyID() {
    return userOrKeyID;
  }
  
}
