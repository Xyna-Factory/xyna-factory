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

import static org.junit.Assert.assertNotEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xint.crypto.PGPOperations.DebugDecryptionResult;
import com.gip.xyna.xint.crypto.PGPOperations.DecryptionResult;
import com.gip.xyna.xint.crypto.PGPOperations.DecryptionStructureLogNode;
import com.gip.xyna.xint.crypto.exceptions.NoSuchKeyException;
import com.gip.xyna.xint.crypto.util.GPGWrapper;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGDecryptionKeyRingParameter;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGRecipientParameter;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGSignageParameter;
import com.gip.xyna.xint.crypto.util.GPGWrapper.GPGSymmetricEncryptionParameter;
import com.gip.xyna.xint.crypto.util.test.TestGPGWrapper;

import junit.framework.TestCase;

public class TestPGPOperations extends TestCase {
  
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
  public void testSimpleEncryption() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = asString(PGPOperations.encrypt(asBytes(clearMsg),
                                                         PGPEncryptionParameter.builder()
                                                                               .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                               .provider(TestData.getDefaultPublicKeyStoreProvider())
                                                                               .build()));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    String decryptedMsg = decryptWithGPG(encryptedMsg,
                                         Optional.of(new GPGWrapper.GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)));
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
  }
  
  @Test
  public void testSimpleDecryption() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = encryptWithGPG(clearMsg, true, Optional.empty(), new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    DecryptionResult result = PGPOperations.decrypt(asBytes(encryptedMsg), 
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getDefaultSecretKeyStoreProvider(TestData.TEST_RECIPIENT_PASSPHRASE))
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_encryptedCompressedLiteral(), result);
  }
  

  @Test
  public void testSimpleLifeCycle() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = asString(PGPOperations.encrypt(asBytes(clearMsg),
                                                         PGPEncryptionParameter.builder()
                                                                               .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                               .provider(TestData.getDefaultPublicKeyStoreProvider())
                                                                               .build()));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    DecryptionResult result = PGPOperations.decrypt(asBytes(encryptedMsg),
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getDefaultSecretKeyStoreProvider(TestData.TEST_RECIPIENT_PASSPHRASE))
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_encryptedCompressedLiteral(), result);
  }
  
  @Test
  public void testSingedEncryption() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = asString(PGPOperations.encrypt(asBytes(clearMsg), 
                                                         PGPEncryptionParameter.builder()
                                                                               .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                               .provider(TestData.getDefaultPublicKeyStoreProvider())
                                                                               .signatureParams(PGPSignatureParameter.builder()
                                                                                                                     .sender(TestData.TEST_RECIPIENT_MAIL)
                                                                                                                     .provider(TestData.getDefaultSecretKeyStoreProvider(TestData.TEST_RECIPIENT_PASSPHRASE))
                                                                                                                     .build())
                                                                               .build()));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    String decryptedMsg = decryptWithGPG(encryptedMsg,
                                         Optional.of(new GPGWrapper.GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)));
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
  }
  
  @Test
  public void testSingedDecryption() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = encryptWithGPG(clearMsg, 
                                         true, 
                                         Optional.of(new GPGSignageParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)
                                                                              .signer(TestData.TEST_RECIPIENT_MAIL)),
                                         new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    DecryptionResult result = PGPOperations.decrypt(asBytes(encryptedMsg), 
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getDefaultSecretKeyStoreProvider(TestData.TEST_RECIPIENT_PASSPHRASE))
                                                                          .signatureProvider(TestData.getDefaultPublicKeyStoreProvider())
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertSignatureVerification(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_encryptedCompressedOnePassSignedLiteral(), result);
  }
  
  @Test
  public void testSingedLifeCycle() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = asString(PGPOperations.encrypt(asBytes(clearMsg), 
                                                         PGPEncryptionParameter.builder()
                                                                               .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                               .provider(TestData.getDefaultPublicKeyStoreProvider())
                                                                               .signatureParams(PGPSignatureParameter.builder()
                                                                                                                      .sender(TestData.TEST_RECIPIENT_MAIL)
                                                                                                                     .provider(TestData.getDefaultSecretKeyStoreProvider(TestData.TEST_RECIPIENT_PASSPHRASE))
                                                                                                                     .build())
                                                                               .build()));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    DecryptionResult result = PGPOperations.decrypt(asBytes(encryptedMsg),
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getDefaultSecretKeyStoreProvider(TestData.TEST_RECIPIENT_PASSPHRASE))
                                                                          .signatureProvider(TestData.getDefaultPublicKeyStoreProvider())
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertSignatureVerification(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_encryptedCompressedOnePassSignedLiteral(), result);
  }
  
  
  @Test
  public void testSingedLifeCycleWithLimitedKeyRings() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = asString(PGPOperations.encrypt(asBytes(clearMsg),
                                                         PGPEncryptionParameter.builder()
                                                                               .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                               .provider(TestData.getTestSenderPublicKeyStoreProvider())
                                                                               .signatureParams(PGPSignatureParameter.builder()
                                                                                                                     .sender(TestData.TEST_LOCAL_USER_MAIL)
                                                                                                                     .provider(TestData.getTestSenderSecretKeyStoreProvider())
                                                                                                                     .build())
                                                                               .build()));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    try {
      // wrong keyRing
      PGPOperations.decrypt(asBytes(encryptedMsg), 
                            PGPDecryptionParameter.builder()
                                                  .provider(TestData.getTestSenderSecretKeyStoreProvider())
                                                  .build());
      fail("We should have failed, key should not have been found.");
    } catch (NoSuchKeyException e) {
      // expected
    }
    
    
    DecryptionResult result = PGPOperations.decrypt(asBytes(encryptedMsg),
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getTestRecipientSecretKeyStoreProvider())
                                                                          .signatureProvider(TestData.getTestRecipientPublicKeyStoreProvider())
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertSignatureVerification(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_encryptedCompressedOnePassSignedLiteral(), result);
  }
  
  
  @Test
  public void testSingedUnarmoredLifeCycleWithLimitedKeyRings() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = PGPOperations.encrypt(asBytes(clearMsg), 
                                                PGPEncryptionParameter.builder()
                                                                      .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                      .provider(TestData.getTestSenderPublicKeyStoreProvider())
                                                                      .wrapInArmor(false)
                                                                      .signatureParams(PGPSignatureParameter.builder()
                                                                                                            .sender(TestData.TEST_LOCAL_USER_MAIL)
                                                                                                            .provider(TestData.getTestSenderSecretKeyStoreProvider())
                                                                                                            .build())
                                                                      .build());
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertNotAsciiArmor(asString(encryptedMsg));
    DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getTestRecipientSecretKeyStoreProvider())
                                                                          .signatureProvider(TestData.getTestRecipientPublicKeyStoreProvider())
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertSignatureVerification(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_encryptedCompressedOnePassSignedLiteral(), result);
  }
  
  
  @Test
  public void testGPGEncryptedSingedUnarmoredLifeCycle() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = encryptWithGPG(asBytes(clearMsg),
                                         false, 
                                         Optional.of(new GPGSignageParameter().passphrase(TestData.TEST_LOCAL_USER_PASSPHRASE)
                                                                              .signer(TestData.TEST_LOCAL_USER_MAIL)),
                                         new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertNotAsciiArmor(asString(encryptedMsg));
    DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getTestRecipientSecretKeyStoreProvider())
                                                                          .signatureProvider(TestData.getTestRecipientPublicKeyStoreProvider())
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertSignatureVerification(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_encryptedCompressedOnePassSignedLiteral(), result);
  }
  
  
  @Test
  public void testGPGDecryptionSingedUnarmoredLifeCycle() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = PGPOperations.encrypt(asBytes(clearMsg), 
                                                PGPEncryptionParameter.builder()
                                                                      .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                      .provider(TestData.getTestSenderPublicKeyStoreProvider())
                                                                      .wrapInArmor(false)
                                                                      .signatureParams(PGPSignatureParameter.builder()
                                                                                                            .sender(TestData.TEST_LOCAL_USER_MAIL)
                                                                                                            .provider(TestData.getTestSenderSecretKeyStoreProvider())
                                                                                                            .build())
                                                                      .build());
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertNotAsciiArmor(asString(encryptedMsg));
    String decryptedMsg = asString(decryptWithGPG(encryptedMsg,
                                                  Optional.of(new GPGWrapper.GPGDecryptionKeyRingParameter().passphrase(TestData.TEST_RECIPIENT_PASSPHRASE))));
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
  }
  
  
   
  @Test
  public void testGPGEncryptedHiddenSingedUnarmoredLifeCycle() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    try {
      String clearMsg = TestData.SAMPLE_TEXT;
      byte[] encryptedMsg = encryptWithGPG(asBytes(clearMsg),
                                           false, 
                                           Optional.of(new GPGSignageParameter().passphrase(TestData.TEST_LOCAL_USER_PASSPHRASE)
                                                                                .signer(TestData.TEST_LOCAL_USER_MAIL)),
                                           new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                      .hide(true));
      assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
      assertNotReadable(clearMsg, asString(encryptedMsg));
      assertNotAsciiArmor(asString(encryptedMsg));
      DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                      PGPDecryptionParameter.builder()
                                                                            .provider(TestData.getTestRecipientSecretKeyStoreProvider())
                                                                            .createDebugResult(true)
                                                                            .build());
      String decryptedMsg = assertAndReturnSingleResponse(result);
      assertSignatureVerification(result);
      assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
      assertDecryptionLog(decryptionLog_encryptedCompressedOnePassSignedLiteral(), result);
    } catch (UnsupportedOperationException e) {
      // FIXME currently unsupported
    }
  }
   
  
  @Test
  public void testGPGUnencryptedSingedLifeCycle() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = signOnlyWithGPG(asBytes(clearMsg),
                                          false, 
                                          Optional.of(new GPGSignageParameter().passphrase(TestData.TEST_LOCAL_USER_PASSPHRASE)
                                                                               .signer(TestData.TEST_LOCAL_USER_MAIL)),
                                          new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertNotAsciiArmor(asString(encryptedMsg));
    DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                    PGPDecryptionParameter.builder()
                                                                          .provider(TestData.getTestRecipientSecretKeyStoreProvider())
                                                                          .signatureProvider(TestData.getTestRecipientPublicKeyStoreProvider())
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertSignatureVerification(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_unencryptedCompressedOnePassSignedLiteral(), result);
  }
  
  
  @Test
  public void testPasswordBasedDecryption() throws NoSuchKeyException, IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException {
    final String PASSPHRASE = "password1234";
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = encryptSymmetricWithGPG(asBytes(clearMsg), false, true, Optional.empty(), Optional.empty(), Optional.of(new GPGSymmetricEncryptionParameter().passphrase(PASSPHRASE)));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertAsciiArmor(asString(encryptedMsg));
    DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                    PGPDecryptionParameter.builder()
                                                                          .password(PASSPHRASE)
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_passwordBasedEncryptedCompressedLiteral(), result);
  }
  

  
  @Test
  public void testPasswordBasedEncryption() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    final String PASSPHRASE = "password1234";
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = asString(PGPOperations.encrypt(asBytes(clearMsg),
                                                         PGPEncryptionParameter.builder()
                                                                               .password(PASSPHRASE)
                                                                               .build()));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    String decryptedMsg = decryptWithGPG(encryptedMsg,
                                         Optional.of(new GPGWrapper.GPGDecryptionKeyRingParameter().passphrase(PASSPHRASE)));
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
  }
  
  
  @Test
  public void testPasswordBasedLifeCycle() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    final String PASSPHRASE = "password1234";
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = PGPOperations.encrypt(asBytes(clearMsg),
                                                PGPEncryptionParameter.builder()
                                                                      .password(PASSPHRASE)
                                                                      .build());
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertAsciiArmor(asString(encryptedMsg));
    DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                    PGPDecryptionParameter.builder()
                                                                          .password(PASSPHRASE)
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_passwordBasedEncryptedCompressedLiteral(), result);
  }
  
  
  @Test
  public void testCustomStreams() throws IOException, PGPException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, NoSuchKeyException {
    final String PASSPHRASE = "password1234";
    String clearMsg = TestData.SAMPLE_TEXT;
    ByteArrayInputStream baEncIs = new ByteArrayInputStream(clearMsg.getBytes());
    ByteArrayOutputStream baEncOs = new ByteArrayOutputStream();
    PGPOperations.encrypt(baEncIs,
                          baEncOs,
                          PGPEncryptionParameter.builder()
                                                .password(PASSPHRASE)
                                                .build());
    byte[] encryptedMsg = baEncOs.toByteArray();
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertAsciiArmor(asString(encryptedMsg));
    System.out.println("encryptedMsg: " + asString(encryptedMsg));
    
    ByteArrayInputStream baDecIs = new ByteArrayInputStream(encryptedMsg);
    ByteArrayOutputStream baDecOs = new ByteArrayOutputStream();
    DecryptionResult result = PGPOperations.decrypt(baDecIs,
                                                    baDecOs,
                                                    PGPDecryptionParameter.builder()
                                                                          .password(PASSPHRASE)
                                                                          .createDebugResult(true)
                                                                          .build());
    assertEquals("There should have been no result", 0, result.getLiteralData().size()); 
    String decryptedMsg = asString(baDecOs.toByteArray());
    System.out.println("decryptedMsg: " + decryptedMsg);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_passwordBasedEncryptedCompressedLiteral(), result);
  }
  
  
  /*@Test
  public void testDoubleDecryption() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException, NoSuchKeyException, PGPException {
    final String PASSPHRASE = "password1234";
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = encryptSymmetricWithGPG(asBytes(clearMsg),
                                                  true,
                                                  true,
                                                  Optional.empty(),
                                                  Optional.of(new GPGRecipientParameter().recipient(TestData.TEST_RECIPIENT_MAIL)),
                                                  Optional.of(new GPGSymmetricEncryptionParameter().passphrase(PASSPHRASE)));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertAsciiArmor(asString(encryptedMsg));
    DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                    PGPDecryptionParameter.builder()
                                                                          .passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)
                                                                          .provider(TestData.getDefaultKeyStoreProvider())
                                                                          .password(PASSPHRASE)
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_passwordBasedEncryptedCompressedLiteral(), result);
  }
  
  
  @Test
  public void testDoubleEncryption() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException, NoSuchKeyException, PGPException {
    final String PASSPHRASE = "password1234";
    String clearMsg = TestData.SAMPLE_TEXT;
    String encryptedMsg = asString(PGPOperations.encrypt(asBytes(clearMsg),
                                                         PGPEncryptionParameter.builder()
                                                                               .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                               .provider(TestData.getDefaultKeyStoreProvider())
                                                                               .password(PASSPHRASE)
                                                                               .build()));
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, encryptedMsg);
    assertAsciiArmor(encryptedMsg);
    String decryptedMsg = decryptWithGPG(encryptedMsg,
                                         Optional.of(new GPGWrapper.GPGDecryptionKeyRingParameter().passphrase(PASSPHRASE)));
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    fail("While the decryption does indeed work GPG still logs: public key decryption failed: bad passphrase");
  }
  
  
  @Test
  public void testDoubleLifecycle() throws Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException, IOException, NoSuchKeyException, PGPException {
    final String PASSPHRASE = "password1234";
    String clearMsg = TestData.SAMPLE_TEXT;
    byte[] encryptedMsg = PGPOperations.encrypt(asBytes(clearMsg),
                                                PGPEncryptionParameter.builder()
                                                                      .recipient(TestData.TEST_RECIPIENT_MAIL)
                                                                      .provider(TestData.getDefaultKeyStoreProvider())
                                                                      .password(PASSPHRASE)
                                                                      .build());
    assertNotEquals("Message should have been encrypted.", clearMsg, encryptedMsg);
    assertNotReadable(clearMsg, asString(encryptedMsg));
    assertAsciiArmor(asString(encryptedMsg));
    DecryptionResult result = PGPOperations.decrypt(encryptedMsg, 
                                                    PGPDecryptionParameter.builder()
                                                                          .passphrase(TestData.TEST_RECIPIENT_PASSPHRASE)
                                                                          .provider(TestData.getDefaultKeyStoreProvider())
                                                                          .password(PASSPHRASE)
                                                                          .createDebugResult(true)
                                                                          .build());
    String decryptedMsg = assertAndReturnSingleResponse(result);
    assertEquals("Message should have been decrypted to original cleartext.", clearMsg, decryptedMsg);
    assertDecryptionLog(decryptionLog_passwordBasedEncryptedCompressedLiteral(), result);
  }*/
  
  
  private void assertSignatureVerification(DecryptionResult result) {
    List<Pair<PGPSignature, Boolean>> verifications = result.getSignatureVerifactionResults();
    for (Pair<PGPSignature, Boolean> verification : verifications) {
      assertTrue("Signature could not be validated", verification.getSecond());
    }
    
  }

  public static void assertAsciiArmor(String encryptedMessage) {
    assertAsciiArmor(encryptedMessage, TestCase::assertTrue);
  }
  
  public static void assertNotAsciiArmor(String encryptedMessage) {
    assertAsciiArmor(encryptedMessage, TestCase::assertFalse);
  }
  
  public static void assertAsciiArmor(String encryptedMessage, AssertSAM assertFunction) {
    final String ASCII_ARMOR_HEADER = "-----BEGIN PGP MESSAGE-----";
    final String ASCII_ARMOR_FOOTER = "-----END PGP MESSAGE-----";
    assertFunction.booleanAssert("Message header does not conform to expectation:\n"+encryptedMessage,
                                 encryptedMessage.startsWith(ASCII_ARMOR_HEADER));
    assertFunction.booleanAssert("Message footer does not conform to expectation:\n"+encryptedMessage,
                                 encryptedMessage.trim().endsWith(ASCII_ARMOR_FOOTER));
  }

  private interface AssertSAM {
    void booleanAssert(String message, boolean condition);
  }
  
  public static void assertNotReadable(String clearMessage, String encryptedMessage) {
    final int STRING_LENGTH = 3;
    Object[] clearWords = Arrays.stream(clearMessage.split("\\s")).filter(s -> s.length() >= STRING_LENGTH).toArray();
    String[] encryptedWords = encryptedMessage.split("\\s");
    for (Object clearWord : clearWords) {
      for (String encryptedWord : encryptedWords) {
        if (encryptedWord.equals(clearWord)) {
          fail("ClearMsg:\n"+clearMessage+ "\nEcryptedMsg:\n"+encryptedMessage+"\nWord collisions: "+clearWord);
        }
      }
    }
  }
  
  private void assertDecryptionLog(String expected, DecryptionResult result) {
    assertTrue("DebugDecryptionResult expected", result instanceof DebugDecryptionResult);
    DebugDecryptionResult debugResult = (DebugDecryptionResult) result;
    assertEquals("DecryptionLog does not meet it's expactation", expected, debugResult.getLogRoot().toString());
  }
  
  private static String decryptionLog_encryptedCompressedLiteral() {
    StringBuilder sb = new StringBuilder();
    sb.append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
        .append(PGPObjectType.PGPEncryptedDataList)
        .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
          .append(PGPObjectType.PGPPublicKeyEncryptedData)
          .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
            .append(PGPObjectType.PGPCompressedData)
            .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
              .append(PGPObjectType.PGPLiteralData)
            .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
          .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
        .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
      .append(DecryptionStructureLogNode.CHILDREN_END_MARKER);
    return sb.toString();
   }
  
  private static String decryptionLog_encryptedCompressedOnePassSignedLiteral() {
    StringBuilder sb = new StringBuilder();
    sb.append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
        .append(PGPObjectType.PGPEncryptedDataList)
        .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
          .append(PGPObjectType.PGPPublicKeyEncryptedData)
          .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
            .append(PGPObjectType.PGPCompressedData)
            .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
              .append(PGPObjectType.PGPOnePassSignatureList)
              .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
                .append(PGPObjectType.PGPOnePassSignature)
              .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
              .append(DecryptionStructureLogNode.CHILDREN_SEPERATION_MARKER)
              .append(PGPObjectType.PGPLiteralData)
              .append(DecryptionStructureLogNode.CHILDREN_SEPERATION_MARKER)
              .append(PGPObjectType.PGPSignatureList)
              .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
                .append(PGPObjectType.PGPSignature)
              .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
            .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
          .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
        .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
      .append(DecryptionStructureLogNode.CHILDREN_END_MARKER);
    return sb.toString();
  }
  
  private static String decryptionLog_unencryptedCompressedOnePassSignedLiteral() {
    StringBuilder sb = new StringBuilder();
    sb.append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
        .append(PGPObjectType.PGPCompressedData)
        .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
          .append(PGPObjectType.PGPOnePassSignatureList)
          .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
            .append(PGPObjectType.PGPOnePassSignature)
          .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
          .append(DecryptionStructureLogNode.CHILDREN_SEPERATION_MARKER)
          .append(PGPObjectType.PGPLiteralData)
          .append(DecryptionStructureLogNode.CHILDREN_SEPERATION_MARKER)
          .append(PGPObjectType.PGPSignatureList)
          .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
            .append(PGPObjectType.PGPSignature)
          .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
        .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
      .append(DecryptionStructureLogNode.CHILDREN_END_MARKER);
    return sb.toString();
  }
  
  private static String decryptionLog_passwordBasedEncryptedCompressedLiteral() {
    StringBuilder sb = new StringBuilder();
    sb.append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
        .append(PGPObjectType.PGPEncryptedDataList)
        .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
          .append(PGPObjectType.PGPPBEEncryptedData)
          .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
            .append(PGPObjectType.PGPCompressedData)
            .append(DecryptionStructureLogNode.CHILDREN_START_MARKER)
              .append(PGPObjectType.PGPLiteralData)
            .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
          .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
        .append(DecryptionStructureLogNode.CHILDREN_END_MARKER)
      .append(DecryptionStructureLogNode.CHILDREN_END_MARKER);
    return sb.toString();
   }

  private static String decryptWithGPG(String encryptedMsg, Optional<GPGDecryptionKeyRingParameter> decryptionParams) throws IOException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException {
    return decryptWithGPG(encryptedMsg, decryptionParams, FileUtils::writeStringToFile, FileUtils::readFileAsString);
  }
  
  private static byte[] decryptWithGPG(byte[] encryptedMsg, Optional<GPGDecryptionKeyRingParameter> decryptionParams) throws IOException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException {
    return decryptWithGPG(encryptedMsg, decryptionParams, TestPGPOperations::writeBytesToFile, TestPGPOperations::readBytesFromFile);
  }
  
  private static <F> F decryptWithGPG(F encryptedMsg, Optional<GPGDecryptionKeyRingParameter> decryptionParams, FileWriter<F> writer, FileReader<F> reader) throws IOException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException {
    File tmpFile = File.createTempFile("TestPGPOperations", ".encMsg");
    writer.writeFormatToFile(encryptedMsg, tmpFile);
    File decryptedFile = GPGWrapper.decrypt(tmpFile, decryptionParams);
    return reader.readFormatFromFile(decryptedFile);
  }
  
  private static String encryptWithGPG(String clearMsg, boolean armor, Optional<GPGSignageParameter> signageParameter, GPGRecipientParameter recipient) throws InterruptedException, ExecutionException, TimeoutException, IOException, Ex_FileWriteException {
    return encryptWithGPG(clearMsg, true, armor, signageParameter, recipient, FileUtils::writeStringToFile, FileUtils::readFileAsString);
  }
  
  private static byte[] encryptWithGPG(byte[] clearMsg, boolean armor, Optional<GPGSignageParameter> signageParameter, GPGRecipientParameter recipient) throws InterruptedException, ExecutionException, TimeoutException, IOException, Ex_FileWriteException {
    return encryptWithGPG(clearMsg, true, armor, signageParameter, recipient, TestPGPOperations::writeBytesToFile, TestPGPOperations::readBytesFromFile);
  }
  
  private static byte[] signOnlyWithGPG(byte[] clearMsg, boolean armor, Optional<GPGSignageParameter> signageParameter, GPGRecipientParameter recipient) throws InterruptedException, ExecutionException, TimeoutException, IOException, Ex_FileWriteException {
    return encryptWithGPG(clearMsg, false, armor, signageParameter, recipient, TestPGPOperations::writeBytesToFile, TestPGPOperations::readBytesFromFile);
  }

  private static byte[] encryptSymmetricWithGPG(byte[] clearMsg, boolean encrypt, boolean armor, Optional<GPGSignageParameter> signageParameter, Optional<GPGRecipientParameter> recipient, Optional<GPGSymmetricEncryptionParameter> symEncParams) throws InterruptedException, ExecutionException, TimeoutException, IOException, Ex_FileWriteException {
    return encryptWithGPG(clearMsg, encrypt, armor, signageParameter, recipient, symEncParams, TestPGPOperations::writeBytesToFile, TestPGPOperations::readBytesFromFile);
  }
  
  private static <F> F encryptWithGPG(F clearMsg, boolean encrypt, boolean armor, Optional<GPGSignageParameter> signageParameter, GPGRecipientParameter recipient, FileWriter<F> writer, FileReader<F> reader) throws InterruptedException, ExecutionException, TimeoutException, IOException, Ex_FileWriteException {
    return encryptWithGPG(clearMsg, encrypt, armor, signageParameter, Optional.of(recipient), Optional.empty(), writer, reader);
  }
  
  private static <F> F encryptWithGPG(F clearMsg, boolean encrypt, boolean armor, Optional<GPGSignageParameter> signageParameter, Optional<GPGRecipientParameter> recipient, Optional<GPGSymmetricEncryptionParameter> symEncParams, FileWriter<F> writer, FileReader<F> reader) throws InterruptedException, ExecutionException, TimeoutException, IOException, Ex_FileWriteException {
    File tmpFile = File.createTempFile("TestPGPOperations", ".clrMsg");
    writer.writeFormatToFile(clearMsg, tmpFile);
    File encryptedFile = GPGWrapper.encrypt(tmpFile, encrypt, armor, signageParameter, recipient, symEncParams);
    return reader.readFormatFromFile(encryptedFile);
  }
  
  private interface FileWriter<F> {
    void writeFormatToFile(F msg, File target) throws Ex_FileWriteException;
  }
  
  private interface FileReader<F> {
    F readFormatFromFile(File source) throws Ex_FileWriteException;
  }
  
  public static void writeBytesToFile(byte[] content, File f) throws Ex_FileWriteException {
    try {
      if (!f.exists()) {
        if (f.getParentFile() != null) {
          f.getParentFile().mkdirs();
        }
        f.createNewFile();
      }
      try (FileOutputStream out = new FileOutputStream(f)) {
        out.write(content);
        out.flush();
      }
    } catch (IOException e) {
      throw new Ex_FileWriteException(f.getAbsolutePath(), e);
    }
  }
  
  public static byte[] readBytesFromFile(File f) throws Ex_FileWriteException {
    try {
      if (!f.exists()) {
        throw new Ex_FileWriteException(f.getAbsolutePath());
      }
      if (f.length() == 0) {
        return new byte[0];
      }
      FileInputStream in = new FileInputStream(f);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StreamUtils.copy(in, baos);
      baos.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new Ex_FileWriteException(f.getAbsolutePath(), e);
    }
  }
  
  private static String assertAndReturnSingleResponse(DecryptionResult result) {
    assertEquals("There should have been a single result", 1, result.getLiteralData().size());
    return asString(result.getLiteralData().get(0));
  }
  
  
  private static byte[] asBytes(String msg) {
    return msg.getBytes(getDefaultCharset());
  }
  
  private static String asString(byte[] msg) {
    return new String(msg, getDefaultCharset());
  }
  
  private static Charset getDefaultCharset() {
    return Charset.forName("UTF-8");
  }
  
}

