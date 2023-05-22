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
package com.gip.xyna.xint.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;

import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xint.crypto.util.GPGWrapper;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGKeyGenerationParameter;

public class TestData {
  
  public final static String TEST_RECIPIENT_KEY_TYPE = "RSA";
  public final static String TEST_RECIPIENT_KEY_SIZE = "2048";
  public final static String TEST_RECIPIENT_NAME = "Test GPGWrapper";
  public final static String TEST_RECIPIENT_MAIL = "test.gpgwrapper@gip.com";
  public final static String TEST_RECIPIENT_COMMENT = "no comment";
  public final static String TEST_RECIPIENT_PASSPHRASE = "password";
  
  public final static String TEST_LOCAL_USER_KEY_TYPE = "RSA";
  public final static String TEST_LOCAL_USER_KEY_SIZE = "2048";
  public final static String TEST_LOCAL_USER_NAME = "Local User GPGWrapper";
  public final static String TEST_LOCAL_USER_MAIL = "local.user.gpgwrapper@gip.com";
  public final static String TEST_LOCAL_USER_COMMENT = "no comment";
  public final static String TEST_LOCAL_USER_PASSPHRASE = "localpass";
  
  private final static String USER_HOME_PATH = System.getProperty("user.home");
  private final static String PUP_KEY_FILE_SUFFIX = "/.gnupg/pubring.gpg";
  private final static String SEK_KEY_FILE_SUFFIX = "/.gnupg/secring.gpg";
  

  public final static String SAMPLE_TEXT =
    "Jemand musste Josef K. verleumdet haben, denn ohne dass er etwas B�ses getan h�tte, wurde er eines Morgens verhaftet."
    + " �Wie ein Hund!� sagte er, es war, als sollte die Scham ihn �berleben."
    + " Als Gregor Samsa eines Morgens aus unruhigen Tr�umen erwachte, fand er sich in seinem Bett zu einem ungeheueren Ungeziefer verwandelt."
    + " Und es war ihnen wie eine Best�tigung ihrer neuen Tr�ume und guten Absichten,"
    + " als am Ziele ihrer Fahrt die Tochter als erste sich erhob und ihren jungen K�rper dehnte.";
  
  
  public static void ensureTestCredentials() throws InterruptedException, ExecutionException, TimeoutException {
    String keyListing = GPGWrapper.listKeys();
    if (!keyListing.contains(TEST_RECIPIENT_MAIL)) {
      createTestCredentials();
    }
    if (!keyListing.contains(TEST_LOCAL_USER_MAIL)) {
      createLocalUserCredentials();
    }
  }

  private static void createTestCredentials() {
    GPGKeyGenerationParameter keyGen = new GPGKeyGenerationParameter();
    keyGen.keyType(TEST_RECIPIENT_KEY_TYPE)
          .keyLength(TEST_RECIPIENT_KEY_SIZE)
          .name(TEST_RECIPIENT_NAME)
          .email(TEST_RECIPIENT_MAIL)
          .comment(TEST_RECIPIENT_COMMENT)
          .passphrase(TEST_RECIPIENT_PASSPHRASE);
    try {
      GPGWrapper.generateKey(keyGen);
    } catch (Ex_FileWriteException | IOException | InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static void createLocalUserCredentials() {
    GPGKeyGenerationParameter keyGen = new GPGKeyGenerationParameter();
    keyGen.keyType(TEST_LOCAL_USER_KEY_TYPE)
          .keyLength(TEST_LOCAL_USER_KEY_SIZE)
          .name(TEST_LOCAL_USER_NAME)
          .email(TEST_LOCAL_USER_MAIL)
          .comment(TEST_LOCAL_USER_COMMENT)
          .passphrase(TEST_LOCAL_USER_PASSPHRASE);
    try {
      GPGWrapper.generateKey(keyGen);
    } catch (Ex_FileWriteException | IOException | InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public static PublicKeyStoreProvider getDefaultPublicKeyStoreProvider() {
    return new FileBasedPublicKeyStoreProvider(new File(USER_HOME_PATH + PUP_KEY_FILE_SUFFIX));
  }
  
  public static SecretKeyStoreProvider getDefaultSecretKeyStoreProvider(String passphrase) {
    return new FileBasedSecretKeyStoreProvider(new File(USER_HOME_PATH + SEK_KEY_FILE_SUFFIX), Optional.ofNullable(passphrase));
  }
  
  public static PublicKeyStoreProvider getTestSenderPublicKeyStoreProvider() {
    try {
      File pupFile = GPGWrapper.exportKey(TEST_RECIPIENT_MAIL);
      return new FileBasedPublicKeyStoreProvider(pupFile);
    } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static SecretKeyStoreProvider getTestSenderSecretKeyStoreProvider() {
    try {
      File secFile = GPGWrapper.exportSecretKey(TEST_LOCAL_USER_MAIL, Optional.of(TEST_LOCAL_USER_PASSPHRASE));
      return new FileBasedSecretKeyStoreProvider(secFile, Optional.of(TEST_LOCAL_USER_PASSPHRASE));
    } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static PublicKeyStoreProvider getTestRecipientPublicKeyStoreProvider() {
    try {
      File pupFile = GPGWrapper.exportKey(TEST_LOCAL_USER_MAIL);
      return new FileBasedPublicKeyStoreProvider(pupFile);
    } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static SecretKeyStoreProvider getTestRecipientSecretKeyStoreProvider() {
    try {
      File secFile = GPGWrapper.exportSecretKey(TEST_RECIPIENT_MAIL, Optional.of(TEST_RECIPIENT_PASSPHRASE));
      return new FileBasedSecretKeyStoreProvider(secFile, Optional.of(TEST_RECIPIENT_PASSPHRASE));
    } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
}
