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

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;


public class FileBasedSecretKeyStoreProvider implements SecretKeyStoreProvider {
  
  
  private final File privateRing;
  private final Optional<String> passphrase;
  
  public FileBasedSecretKeyStoreProvider(File privateRing, Optional<String> passphrase) {
    this.privateRing = privateRing;
    this.passphrase = passphrase;
  }
  

  public PGPSecretKeyRingCollection openSecretKeyStore() throws IOException {
    try {
      InputStream is = PGPUtil.getDecoderStream(new FileInputStream(privateRing));
      return new PGPSecretKeyRingCollection(is, new BcKeyFingerprintCalculator());
    } catch (FileNotFoundException | PGPException e) {
      throw new IOException(e);
    }
  }


  public PGPPrivateKey derivePrivateKey(PGPSecretKey secretKey) throws PGPException {
    return SecretKeyStoreProvider.derivePrivateKey(secretKey, passphrase);
  }

}
