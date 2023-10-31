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
package xact.ssh.generatekeypackage;



import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import org.apache.log4j.Logger;
import com.gip.xyna.CentralFactoryLogging;

import xact.ssh.EncryptionType;
import xact.ssh.XynaIdentityRepository;



public class ExtdKeyGeneration {

  private static final Logger logger = CentralFactoryLogging.getLogger(ExtdKeyGeneration.class);


  public static void generateKeyPair(Integer keySize, String passPhrase, boolean overWriteExisting, EncryptionType type,
                                     XynaIdentityRepository identityRepo) {

    if ((passPhrase != null) && (!passPhrase.isEmpty())) {
      logger.warn("Error in generateKeyPair: passphrase not implemented!");
    }

    try {
      KeyPairGenerator KeyGen = KeyPairGenerator.getInstance(type.getStringRepresentation());
      KeyGen.initialize(keySize, new SecureRandom());
      java.security.KeyPair Pair = KeyGen.generateKeyPair();

      ExtdKeyPairElement keyPair = null;
      if (type.getStringRepresentation().equalsIgnoreCase(xact.ssh.EncryptionType.RSA.getStringRepresentation())) {
        keyPair = ExtdKeyGenerationHelperClass.transformKeyPairRSA(Pair);
      }
      if (type.getStringRepresentation().equalsIgnoreCase(xact.ssh.EncryptionType.DSA.getStringRepresentation())) {
        keyPair = ExtdKeyGenerationHelperClass.transformKeyPairDSA(Pair);
      }

      if (keyPair != null) {
        byte[] pubByte = keyPair.getPublicKey().getBytes();
        byte[] prvByte = keyPair.getPrivateKey().getBytes();
        writeKey(prvByte, pubByte, type, passPhrase, overWriteExisting, identityRepo);
      } else {
        logger.warn("Error in generateKeyPair: No keypair generated!");
        NoSuchAlgorithmException e = new NoSuchAlgorithmException();
        throw new RuntimeException(e);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  public static void writeKey(byte[] privateKeyBlob, byte[] publicKeyBlob, EncryptionType encryptionTypeValue, String passphrase,
                              boolean overwriteExisting, XynaIdentityRepository identityRepo) {

    try {
      if (overwriteExisting) {
        identityRepo.clearAll();
      }

      identityRepo.add(Optional.empty(), encryptionTypeValue, publicKeyBlob, privateKeyBlob, Optional.ofNullable(passphrase));

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


}
