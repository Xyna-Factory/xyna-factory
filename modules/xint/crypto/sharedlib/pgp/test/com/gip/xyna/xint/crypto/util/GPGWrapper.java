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
package com.gip.xyna.xint.crypto.util;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.shell.ShellCommand;
import com.gip.xyna.utils.shell.ShellExecutionResponse;
import com.gip.xyna.utils.shell.ShellExecutor;

public class GPGWrapper {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(GPGWrapper.class);

  private final static long SHELL_COMMAND_TIMEOUT = 3000;
  private final static String GPG_EXECUTABLE = "gpg";
  private final static String ENCRYPT_PARAMETER = "--encrypt";
  private final static String DECRYPT_PARAMETER = "--decrypt";
  private final static String LISTKEYS_PARAMETER = "--list-keys";
  private final static String BATCH_PARAMETER = "--batch";
  private final static String GENERATEKEY_PARAMETER = "--gen-key";
  private final static String SIGN_PARAMETER = "--sign";
  private final static String ARMOR_PARAMETER = "--armor";
  private final static String RECIPIENT_PARAMETER = "-r";
  private final static String PASSPHRASE_PARAMETER = "--passphrase";
  private final static String LOCAL_USER_PARAMETER = "--local-user";
  private final static String OUTPUT_PARAMETER = "--output";
  private final static String HIDE_RECIPIENT_PARAMETER = "--hidden-recipient";
  private final static String ENCRYPTED_RESULT_SUFFIX = ".enc";
  private final static String EXPORT_PARAMETER = "--export";
  private final static String EXPORT_SECRET_KEYS_PARAMETER = "--export-secret-keys";
  private final static String KEYRING_PARAMETER = "--keyring";
  private final static String SECRET_KEYRING_PARAMETER = "--secret-keyring";
  private final static String SYMMETRIC_ENCRYPTION_PARAMETER = "--symmetric";
  
  
  private final static ShellExecutor executor = new ShellExecutor();
  

  public static File encrypt(File clearFile, boolean encrypt, boolean armor, 
                             Optional<GPGSignageParameter> signageParameter, 
                             Optional<GPGRecipientParameter> recipient,
                             Optional<GPGSymmetricEncryptionParameter> symmetric) throws InterruptedException, ExecutionException, TimeoutException {
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append(GPG_EXECUTABLE)
                  .append(" ");
    if (encrypt) {
      commandBuilder.append(ENCRYPT_PARAMETER);
    }
    commandBuilder.append(" ")
                  .append(OUTPUT_PARAMETER)
                  .append(" ")
                  .append(clearFile.getAbsolutePath())
                  .append(ENCRYPTED_RESULT_SUFFIX)
                  .append(" ");
    signageParameter.ifPresent(s -> {
      commandBuilder.append(SIGN_PARAMETER)
                    .append(" ");
      s.getSigner().ifPresent(u -> {
        commandBuilder.append(LOCAL_USER_PARAMETER)
                      .append(" ")
                      .append(u)
                      .append(" ");
       });
      s.getPassphrase().ifPresent(p -> {
        commandBuilder.append(PASSPHRASE_PARAMETER) // TODO passing passphrase via cmd is a security concern, but good enough for a test util class
                      .append(" ")
                      .append(p)
                      .append(" ");
       });
    });
    if (armor) {
      commandBuilder.append(ARMOR_PARAMETER)
                    .append(" ");      
    }
    recipient.ifPresent(r -> {
      commandBuilder.append(r.hideRecipient() ? HIDE_RECIPIENT_PARAMETER : RECIPIENT_PARAMETER)
                    .append(" ")
                    .append(r.getRecipient())
                    .append(" ");
      r.keyRingFile.ifPresent(f -> {
        commandBuilder.append(KEYRING_PARAMETER)
                      .append(" ")
                      .append(f)
                      .append(" ");
      });
    });
    symmetric.ifPresent(s -> {
      commandBuilder.append(BATCH_PARAMETER) // to enable usage of PASSPHRASE_PARAMETER, CLI would rather ask interactive
                    .append(" ")
                    .append(SYMMETRIC_ENCRYPTION_PARAMETER)
                    .append(" ")
                    .append(PASSPHRASE_PARAMETER)
                    .append(" ")
                    .append(s.getPassphrase())
                    .append(" ");
    });
    commandBuilder.append(clearFile.getAbsolutePath());
    logger.debug("GPGWrapper.encrypt.request: " + commandBuilder.toString());
    ShellExecutionResponse response = executor.execute(cmd(commandBuilder.toString()));
    logger.debug("GPGWrapper.encrypt.response: " + response);
    File encryptedFile = new File(clearFile.getParentFile(), clearFile.getName() + ENCRYPTED_RESULT_SUFFIX);
    return encryptedFile;
  }
  
  
  public static File decrypt(File encryptedFile, Optional<GPGDecryptionKeyRingParameter> keyRingParameter) throws InterruptedException, ExecutionException, TimeoutException, IOException {
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append(GPG_EXECUTABLE)
                  .append(" ");
    keyRingParameter.ifPresent(k -> {
      k.getPassphrase().ifPresent(p -> {
        commandBuilder.append(PASSPHRASE_PARAMETER) // TODO passing passphrase via cmd is a security concern, but good enough for a test util class
                      .append(" ")
                      .append(p)
                      .append(" ");
        
       });
      k.getKeyRingFile().ifPresent(f -> {
        commandBuilder.append(SECRET_KEYRING_PARAMETER)
                      .append(" ")
                      .append(f)
                      .append(" ");
        
       });
    });
    commandBuilder.append(DECRYPT_PARAMETER)
                  .append(" ");
    commandBuilder.append(encryptedFile.getAbsolutePath());
    File decryptedFile = File.createTempFile(encryptedFile.getName(), ".msg");
    commandBuilder.append(" > ")
                  .append(decryptedFile.getAbsolutePath());
    logger.debug("GPGWrapper.decrypt.request: " + commandBuilder.toString());
    ShellExecutionResponse response = executor.execute(cmd(commandBuilder.toString()));
    logger.debug("GPGWrapper.decrypt.response: " + response.getOutput());
    return decryptedFile;
  }


  public static String listKeys() throws InterruptedException, ExecutionException, TimeoutException {
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append(GPG_EXECUTABLE)
                  .append(" ")
                  .append(LISTKEYS_PARAMETER)
                  .append(" ");
    ShellExecutionResponse response = executor.execute(cmd(commandBuilder.toString()));
    return response.getOutput();
  }
  
  
  public static void generateKey(GPGKeyGenerationParameter keyGen) throws IOException, Ex_FileWriteException, InterruptedException, ExecutionException, TimeoutException {
    File tmpFile = File.createTempFile("BatchGeneration", ".parameter");
    FileUtils.writeStringToFile(keyGen.asText(), tmpFile);
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append(GPG_EXECUTABLE)
                  .append(" ")
                  .append(BATCH_PARAMETER)
                  .append(" ")
                  .append(GENERATEKEY_PARAMETER)
                  .append(" ")
                  .append(tmpFile.getAbsolutePath());
    ShellExecutionResponse response = executor.execute(cmd(commandBuilder.toString()));
    logger.debug("GPGWrapper.generateKey.response: " + response.getOutput());
  }
  
  
  public static File exportKey(String userIdentifier) throws InterruptedException, ExecutionException, TimeoutException, IOException {
    File file = File.createTempFile("KeyExport", ".keyring");
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append(GPG_EXECUTABLE)
                  .append(" ")
                  .append(ARMOR_PARAMETER)
                  .append(" ")
                  .append(EXPORT_PARAMETER)
                  .append(" ")
                  .append(userIdentifier)
                  .append(" > ")
                  .append(file.getAbsolutePath());
    logger.debug("GPGWrapper.exportKey.request: " + commandBuilder.toString());
    ShellExecutionResponse response = executor.execute(cmd(commandBuilder.toString()));
    logger.debug("GPGWrapper.exportKey.response: " + response.getOutput());
    return file;
  }
  
  public static File exportSecretKey(String userIdentifier, Optional<String> passphrase) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    File file = File.createTempFile("KeyExport", ".secretkeyring");
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append(GPG_EXECUTABLE)
                  .append(" ")
                  .append(ARMOR_PARAMETER)
                  .append(" ")
                  .append(EXPORT_SECRET_KEYS_PARAMETER)
                  .append(" ")
                  .append(userIdentifier)
                  .append(" ");
    passphrase.ifPresent(p -> {
      commandBuilder.append(PASSPHRASE_PARAMETER) // TODO passing passphrase via cmd is a security concern, but good enough for a test util class
                    .append(" ")
                    .append(p)
                    .append(" ");
     });
    commandBuilder.append(" > ")
                  .append(file.getAbsolutePath());
    logger.debug("GPGWrapper.exportSecretKey.request: " + commandBuilder.toString());
    ShellExecutionResponse response = executor.execute(cmd(commandBuilder.toString()));
    logger.debug("GPGWrapper.exportSecretKey.response: " + response.getOutput());
    return file;
  }
  
  
  private static ShellCommand cmd(String cmd) {
    return ShellCommand.cmd(cmd).timeout(SHELL_COMMAND_TIMEOUT);
  }
  
  
  public final static class GPGKeyGenerationParameter {
    
    private String keyType;
    private String keyLength;
    private String name;
    private String comment;
    private String email;
    private String passphrase;
    
    public GPGKeyGenerationParameter() {
    }
    
    public GPGKeyGenerationParameter keyType(String keyType) {
      this.keyType = keyType;
      return this;
    }
    
    public GPGKeyGenerationParameter keyLength(String keyLength) {
      this.keyLength = keyLength;
      return this;
    }
    
    public GPGKeyGenerationParameter name(String name) {
      this.name = name;
      return this;
    }
    
    public GPGKeyGenerationParameter comment(String comment) {
      this.comment = comment;
      return this;
    }
    
    public GPGKeyGenerationParameter email(String email) {
      this.email = email;
      return this;
    }
    
    public GPGKeyGenerationParameter passphrase(String passphrase) {
      this.passphrase = passphrase;
      return this;
    }

    
    public String asText() {
      return "Key-Type: " + keyType + "\n" +
             "Key-Length: " + keyLength + "\n" +
             "Name-Real: " + name + "\n" +
             "Name-Comment: " + comment + "\n" +
             "Name-Email: " + email + "\n" + 
             "Expire-Date: 0\n" + 
             "Passphrase: " + passphrase + "\n" + "%commit";
    }
  }
  
  
  public final static class GPGSignageParameter {
    
    private Optional<String> signer;
    private Optional<String> passphrase; 
    
    
    public GPGSignageParameter() {
      signer = Optional.empty();
      passphrase = Optional.empty();
    }
    
    public GPGSignageParameter signer(String signer) {
      this.signer = Optional.of(signer);
      return this;
    }
    
    public Optional<String> getSigner() {
      return signer;
    }
    
    public GPGSignageParameter passphrase(String passphrase) {
      this.passphrase = Optional.of(passphrase);
      return this;
    }
    
    public Optional<String> getPassphrase() {
      return passphrase;
    }
  }
  
  public final static class GPGSymmetricEncryptionParameter {
    
    private String passphrase; 
    
    
    public GPGSymmetricEncryptionParameter() {
    }
    
    public GPGSymmetricEncryptionParameter passphrase(String passphrase) {
      this.passphrase = passphrase;
      return this;
    }
    
    public String getPassphrase() {
      return passphrase;
    }
  }
  
  
  public final static class GPGRecipientParameter {
    
    private String recipient;
    private boolean hide;
    private Optional<String> keyRingFile;
    
    public GPGRecipientParameter() {
      keyRingFile = Optional.empty();
    }
    
    public GPGRecipientParameter recipient(String recipient) {
      this.recipient = recipient;
      return this;
    }
    
    public String getRecipient() {
      return recipient;
    }
    
    public GPGRecipientParameter hide(boolean hide) {
      this.hide = hide;
      return this;
    }
    
    public boolean hideRecipient() {
      return hide;
    }
    
    public GPGRecipientParameter keyRingFile(String keyRingFile) {
      this.keyRingFile = Optional.of(keyRingFile);
      return this;
    }
    
    public Optional<String> getKeyRingFile() {
      return keyRingFile;
    }
  }
  
  
  public final static class GPGDecryptionKeyRingParameter {
    
    private Optional<String> keyRingFile;
    private Optional<String> passphrase;
    
    public GPGDecryptionKeyRingParameter() {
      keyRingFile = Optional.empty();
      passphrase = Optional.empty();
    }
    
    public GPGDecryptionKeyRingParameter keyRingFile(String keyRingFile) {
      this.keyRingFile = Optional.of(keyRingFile);
      return this;
    }
    
    public Optional<String> getKeyRingFile() {
      return keyRingFile;
    }
    
    public GPGDecryptionKeyRingParameter passphrase(String passphrase) {
      this.passphrase = Optional.of(passphrase);
      return this;
    }
    
    public Optional<String> getPassphrase() {
      return passphrase;
    }
  }


}
