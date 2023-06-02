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
package com.gip.xyna.xint.crypto;

import java.util.Optional;



public class PGPDecryptionParameter {
  
  private final Optional<PGPKeyStoreParameter> userParameter;
  private final Optional<String> password;
  private final boolean createDebugResult;
  
  
  
  private PGPDecryptionParameter(Optional<PGPKeyStoreParameter> userParameter,
                                 Optional<String> password,
                                 boolean createDebugResult) {
    this.userParameter = userParameter;
    this.password = password;
    this.createDebugResult = createDebugResult;
  }
  
  public Optional<PGPKeyStoreParameter> getUserParameter() {
    return userParameter;
  }
  
  public Optional<String> getPassword() {
    return password;
  }
  
  public boolean createDebugResult() {
    return createDebugResult;
  }
  
  public static PGPDecryptionParameterBuilder builder() {
    return new PGPDecryptionParameterBuilder();
  }
  
  
  public static class PGPDecryptionParameterBuilder {
    
    private Optional<SecretKeyStoreProvider> provider;
    private Optional<PublicKeyStoreProvider> signatureProvider;
    private Optional<String> password;
    private Optional<Boolean> createDebugResult;
    
    private PGPDecryptionParameterBuilder() {
      provider = Optional.empty();
      createDebugResult = Optional.empty();
      password = Optional.empty();
      signatureProvider = Optional.empty();
    }
    
    
    public PGPDecryptionParameterBuilder provider(SecretKeyStoreProvider provider) {
      this.provider = Optional.of(provider);
      return this;
    }
    
    public PGPDecryptionParameterBuilder signatureProvider(PublicKeyStoreProvider signatureProvider) {
      this.signatureProvider = Optional.of(signatureProvider);
      return this;
    }
    
    public PGPDecryptionParameterBuilder createDebugResult(boolean createDebugResult) {
      this.createDebugResult = Optional.of(createDebugResult);
      return this;
    }
    
    public PGPDecryptionParameterBuilder password(String password) {
      this.password = Optional.of(password);
      return this;
    }
    
    public PGPDecryptionParameter build() {
      if (provider.isEmpty() && password.isEmpty()) {
        throw new IllegalArgumentException("Decryption requires either keystore or symmetric decryption parameters.");
      }
      Optional<PGPKeyStoreParameter> userParameter = Optional.empty();
      if (provider.isPresent()) {
        userParameter = Optional.of(new PGPKeyStoreParameter(signatureProvider, provider.get()));
      }
      return new PGPDecryptionParameter(userParameter,
                                        password,
                                        createDebugResult.orElse(Boolean.FALSE));
    }
    
  }


}
