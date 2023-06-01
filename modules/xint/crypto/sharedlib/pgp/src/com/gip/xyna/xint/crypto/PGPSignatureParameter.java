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

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;

import com.gip.xyna.xint.crypto.exceptions.NoSuchKeyException;

public class PGPSignatureParameter {
  
  
  private PGPKeyStoreParameter senderParameter;
  // hash algorithm for signature
  private final HashAlgorithm hash;
  
  private PGPSignatureParameter(PGPKeyStoreParameter senderParameter,
                                HashAlgorithm hash) {
    this.senderParameter = senderParameter;
    this.hash = hash;
  }
  
  public PGPSecretKey getSignatureSecretKey() throws NoSuchKeyException {
    return senderParameter.findSecretKey();
  }
  
  public PGPPrivateKey getSignaturePrivateKey() throws PGPException, NoSuchKeyException {
    return senderParameter.findPrivateKey();
  }
  
  public HashAlgorithm getHashAlgorithm() {
    return hash;
  }

  
  public static PGPSignatureParameter.PGPSignatureParameterBuilder builder() {
    return new PGPSignatureParameterBuilder();
  }
  
  public static class PGPSignatureParameterBuilder {
    
    private Optional<String> sender;
    private Optional<SecretKeyStoreProvider> provider;
    private Optional<HashAlgorithm> hash;
    
    private PGPSignatureParameterBuilder() {
      sender = Optional.empty();
      provider = Optional.empty();
      hash = Optional.empty();
    }
    
    public PGPSignatureParameter.PGPSignatureParameterBuilder sender(String sender) {
      this.sender = Optional.of(sender);
      return this;
    }
    
    public PGPSignatureParameter.PGPSignatureParameterBuilder provider(SecretKeyStoreProvider provider) {
      this.provider = Optional.of(provider);
      return this;
    }
    
    public PGPSignatureParameter.PGPSignatureParameterBuilder hash(HashAlgorithm hash) {
      this.hash = Optional.of(hash);
      return this;
    }
    
    public PGPSignatureParameter build() {
      return new PGPSignatureParameter(new PGPKeyStoreParameter(sender.orElseThrow(() -> new RuntimeException("Sender has to be set")),
                                                                provider.orElseThrow(() -> new RuntimeException("KeyStoreProvider has to be set"))),
                                       hash.orElse(HashAlgorithm.SHA256));
    }
  }
}