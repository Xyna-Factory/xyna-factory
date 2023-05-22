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

import java.util.Optional;


public class PGPEncryptionParameter {
  
  // used to find publicKey from keyStore, optional
  private final Optional<PGPKeyStoreParameter> recipientParameter;
  // wrap pgp package in ascii armor
  private final boolean wrapInArmor;
  // parameters specific to the signing of the msg
  private final Optional<PGPSignatureParameter> signatureParams;
  // encryption algorithm to use
  private final SymmetricKeyAlgorithm encryption;
  // compression algorithm to use
  private final CompressionAlgorithm compression;
  // for password based encryption
  private final Optional<String> password;
  
  
  
  private PGPEncryptionParameter(Optional<PGPKeyStoreParameter> recipientParameter,
                                 boolean wrapInArmor,
                                 Optional<PGPSignatureParameter> signatureParams,
                                 SymmetricKeyAlgorithm encryption,
                                 CompressionAlgorithm compression,
                                 Optional<String> password) {
    this.recipientParameter = recipientParameter;
    this.wrapInArmor = wrapInArmor;
    this.signatureParams = signatureParams;
    this.encryption = encryption;
    this.compression = compression;
    this.password = password;
  }
  
  
  public Optional<PGPKeyStoreParameter>  getRecipientParameter() {
    return recipientParameter;
  }
  
  public boolean doWrapInArmor() {
    return wrapInArmor;
  }

  public Optional<PGPSignatureParameter> getSignatureParams() {
    return signatureParams;
  }
  
  public SymmetricKeyAlgorithm getEncryption() {
    return encryption;
  }

  public CompressionAlgorithm getCompression() {
    return compression;
  }
  
  public Optional<String>  getPassword() {
    return password;
  }
  
  public static PGPEncryptionParameterBuilder builder() {
    return new PGPEncryptionParameterBuilder();
  }
  
  public static class PGPEncryptionParameterBuilder {
    
    private Optional<String> recipient;
    private Optional<PublicKeyStoreProvider> provider;
    private Optional<Boolean> wrapInArmor; 
    private Optional<PGPSignatureParameter> signatureParams;
    private Optional<SymmetricKeyAlgorithm> encryption;
    private Optional<CompressionAlgorithm> compression;
    private Optional<String> password;
    
    private PGPEncryptionParameterBuilder() {
      recipient = Optional.empty();
      provider = Optional.empty();
      wrapInArmor = Optional.empty();
      signatureParams = Optional.empty();
      encryption = Optional.empty();
      compression = Optional.empty();
      password = Optional.empty();
    }
    
    public PGPEncryptionParameterBuilder recipient(String recipient) {
      this.recipient = Optional.of(recipient);
      return this;
    }
    
    public PGPEncryptionParameterBuilder provider(PublicKeyStoreProvider provider) {
      this.provider = Optional.of(provider);
      return this;
    }
    
    public PGPEncryptionParameterBuilder wrapInArmor(boolean wrapInArmor) {
      this.wrapInArmor = Optional.of(wrapInArmor);
      return this;
    }
    
    public PGPEncryptionParameterBuilder signatureParams(PGPSignatureParameter signatureParams) {
      this.signatureParams = Optional.of(signatureParams);
      return this;
    }
    
    public PGPEncryptionParameterBuilder encryption(SymmetricKeyAlgorithm encryption) {
      this.encryption = Optional.of(encryption);
      return this;
    }
    
    public PGPEncryptionParameterBuilder compression(CompressionAlgorithm compression) {
      this.compression = Optional.of(compression);
      return this;
    }
    
    public PGPEncryptionParameterBuilder password(String password) {
      this.password = Optional.of(password);
      return this;
    }
    
    public PGPEncryptionParameter build() {
      if ((recipient.isEmpty() || provider.isEmpty()) && password.isEmpty()) {
        throw new IllegalArgumentException("At least either key or password based encryption parameters have to be given.");
      }
      Optional<PGPKeyStoreParameter> userParams = Optional.empty();
      if (recipient.isPresent() && provider.isPresent()) {
        userParams = Optional.of(new PGPKeyStoreParameter(recipient.get(), provider.get()));
      }
      return new PGPEncryptionParameter(userParams,
                                        wrapInArmor.orElse(Boolean.TRUE),
                                        signatureParams,
                                        encryption.orElse(SymmetricKeyAlgorithm.CAST5),
                                        compression.orElse(CompressionAlgorithm.ZIP),
                                        password);
    }
  }
}
