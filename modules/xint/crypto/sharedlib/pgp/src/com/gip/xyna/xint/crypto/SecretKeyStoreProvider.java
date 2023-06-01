/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.io.IOException;
import java.util.Optional;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

public interface SecretKeyStoreProvider {
  
  public PGPSecretKeyRingCollection openSecretKeyStore() throws IOException;
  
  public PGPPrivateKey derivePrivateKey(PGPSecretKey secretKey) throws PGPException;
  
  public static PGPPrivateKey derivePrivateKey(PGPSecretKey secretKey, Optional<String> passphrase) throws PGPException {
    BcPBESecretKeyDecryptorBuilder builder = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider());
    PBESecretKeyDecryptor decryptorFactory = builder.build(passphrase.orElse("").toCharArray());
    return secretKey.extractPrivateKey(decryptorFactory);
  }
}