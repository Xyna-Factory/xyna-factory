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
package com.gip.xyna.xint.crypto.util.test;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xint.crypto.TestData;
import com.gip.xyna.xint.crypto.TestPGPOperations;
import com.gip.xyna.xint.crypto.util.GPGWrapper;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGKeyGenerationParameter;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGSignageParameter;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGRecipientParameter;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGDecryptionKeyRingParameter;

import junit.framework.TestCase;

public class TestGPGWrapper extends TestCase {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TestData.ensureTestCredentials();
  }
  

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  
  @Test
  public void testSimpleLifeCycle() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException {
    File clearFile = createSampleFile();
    String clearMessage = FileUtils.readFileAsString(clearFile);
    File encryptedFile = GPGWrapper.encrypt(clearFile,
                                            true,
                                            false,
                                            Optional.empty(),
                                            Optional.of(new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)),
                                            Optional.empty());
    String encryptedMessage = FileUtils.readFileAsString(encryptedFile);
    assertNotEquals(clearMessage, encryptedMessage);
    TestPGPOperations.assertNotReadable(clearMessage, encryptedMessage);
    File decryptedFile = GPGWrapper.decrypt(encryptedFile, Optional.of(new GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)));
    String decryptedMessage = FileUtils.readFileAsString(decryptedFile);
    assertEquals("Message should have been decrypted to original", clearMessage, decryptedMessage);
  }
  
  
  @Test
  public void testAsciiArmorLifeCycle() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException {
    File clearFile = createSampleFile();
    String clearMessage = FileUtils.readFileAsString(clearFile);
    File encryptedFile = GPGWrapper.encrypt(clearFile,
                                            true,
                                            true,
                                            Optional.empty(),
                                            Optional.of(new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)),
                                            Optional.empty());
    String encryptedMessage = FileUtils.readFileAsString(encryptedFile);
    assertNotEquals(clearMessage, encryptedMessage);
    TestPGPOperations.assertNotReadable(clearMessage, encryptedMessage);
    TestPGPOperations.assertAsciiArmor(encryptedMessage);
    File decryptedFile = GPGWrapper.decrypt(encryptedFile, Optional.of(new GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)));
    String decryptedMessage = FileUtils.readFileAsString(decryptedFile);
    assertEquals("Message should have been decrypted to original", clearMessage, decryptedMessage);
  }
  
  
  @Test
  public void testSelfSignedLifeCycle() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException {
    File clearFile = createSampleFile();
    String clearMessage = FileUtils.readFileAsString(clearFile);
    File encryptedFile = GPGWrapper.encrypt(clearFile,
                                            true,
                                            true,
                                            Optional.of(new GPGSignageParameter().signer(TestData.TEST_RECIPIENT_MAIL)
                                                                                 .passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)), 
                                            Optional.of(new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)),
                                            Optional.empty());
    String encryptedMessage = FileUtils.readFileAsString(encryptedFile);
    assertNotEquals(clearMessage, encryptedMessage);
    TestPGPOperations.assertNotReadable(clearMessage, encryptedMessage);
    TestPGPOperations.assertAsciiArmor(encryptedMessage);
    File decryptedFile = GPGWrapper.decrypt(encryptedFile, Optional.of(new GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)));
    String decryptedMessage = FileUtils.readFileAsString(decryptedFile);
    assertEquals("Message should have been decrypted to original", clearMessage, decryptedMessage);
  }
  
  
  @Test
  public void testSignedLifeCycle() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException {
    File clearFile = createSampleFile();
    String clearMessage = FileUtils.readFileAsString(clearFile);
    File encryptedFile = GPGWrapper.encrypt(clearFile,
                                            true,
                                            true,
                                            Optional.of(new GPGSignageParameter().signer(TestData.TEST_LOCAL_USER_MAIL)
                                                                                 .passphrase(TestData.TEST_LOCAL_USER_PASSPHRASE)), 
                                            Optional.of(new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)),
                                            Optional.empty());
    String encryptedMessage = FileUtils.readFileAsString(encryptedFile);
    assertNotEquals(clearMessage, encryptedMessage);
    TestPGPOperations.assertNotReadable(clearMessage, encryptedMessage);
    TestPGPOperations.assertAsciiArmor(encryptedMessage);
    File decryptedFile = GPGWrapper.decrypt(encryptedFile, Optional.of(new GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)));
    String decryptedMessage = FileUtils.readFileAsString(decryptedFile);
    assertEquals("Message should have been decrypted to original", clearMessage, decryptedMessage);
  }
  
  
  @Test
  public void testSimpleLifeCycleHiddenRecipient() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException {
    File clearFile = createSampleFile();
    String clearMessage = FileUtils.readFileAsString(clearFile);
    File encryptedFile = GPGWrapper.encrypt(clearFile,
                                            true,
                                            false,
                                            Optional.empty(),
                                            Optional.of(new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                                   .hide(true)),
                                            Optional.empty());
    String encryptedMessage = FileUtils.readFileAsString(encryptedFile);
    assertNotEquals(clearMessage, encryptedMessage);
    TestPGPOperations.assertNotReadable(clearMessage, encryptedMessage);
    File decryptedFile = GPGWrapper.decrypt(encryptedFile, Optional.of(new GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)));
    String decryptedMessage = FileUtils.readFileAsString(decryptedFile);
    assertEquals("Message should have been decrypted to original", clearMessage, decryptedMessage);
  }
  
  
  @Test
  public void testSimpleLifeCycleWithWrongKeyRing() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException {
    File clearFile = createSampleFile();
    String clearMessage = FileUtils.readFileAsString(clearFile);
    File recipientPublicKeyRing = GPGWrapper.exportKey(TestData.TEST_RECIPIENT_MAIL);
    File encryptedFile = GPGWrapper.encrypt(clearFile,
                                            true,
                                            false,
                                            Optional.empty(),
                                            Optional.of(new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                                   .keyRingFile(recipientPublicKeyRing.getAbsolutePath())),
                                                                       Optional.empty());
    String encryptedMessage = FileUtils.readFileAsString(encryptedFile);
    assertNotEquals(clearMessage, encryptedMessage);
    TestPGPOperations.assertNotReadable(clearMessage, encryptedMessage);
    File localUserSecretKeyRing = GPGWrapper.exportSecretKey(TestData.TEST_LOCAL_USER_MAIL, Optional.of(TestData.TEST_LOCAL_USER_PASSPHRASE));
    File decryptedFile = GPGWrapper.decrypt(encryptedFile,
                                            Optional.of(new GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_LOCAL_USER_PASSPHRASE)
                                                                                           .keyRingFile(localUserSecretKeyRing.getAbsolutePath())));
    String decryptedMessage = FileUtils.readFileAsString(decryptedFile);
    assertTrue("Message should have failed to be decrypted.", decryptedMessage.isEmpty());
    File recipientSecretKeyRing = GPGWrapper.exportSecretKey(TestData.TEST_RECIPIENT_MAIL, Optional.of(TestData.TEST_RECIPIENT_PASSPHRASE));
    decryptedFile = GPGWrapper.decrypt(encryptedFile,
                                       Optional.of(new GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)
                                                                                      .keyRingFile(recipientSecretKeyRing.getAbsolutePath())));
    decryptedMessage = FileUtils.readFileAsString(decryptedFile);
    assertEquals("Message should have been decrypted to original", clearMessage, decryptedMessage);
  }
  
  
  private File createSampleFile() {
    File tmpFile;
    try {
      tmpFile = File.createTempFile("TestGPGWrapper", ".sampleFile");
      try {
        FileUtils.writeStringToFile(TestData.SAMPLE_TEXT, tmpFile);
      } catch (Ex_FileWriteException e) {
        fail("Failed to write to temp file ' " + tmpFile.getName() + "': " + e.getMessage());
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      fail("Failed to create temp file: " + e.getMessage());
      throw new RuntimeException(e);
    }
    return tmpFile;
  }
  
  
}
