/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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



import java.nio.charset.StandardCharsets;
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
                                     XynaIdentityRepository identityRepo, String identity, long priority, String typeclass) {

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

      if ((identity == null) || (identity.isBlank())) {
          identity = generateAlias(identityRepo, keyPair.getPublicKey());
      }

      if (keyPair != null) {
        byte[] pubByte = keyPair.getPublicKey().getBytes();
        byte[] prvByte = keyPair.getPrivateKey().getBytes();
        writeKey(prvByte, pubByte, type, passPhrase, overWriteExisting, identityRepo, identity, priority, typeclass);
      } else {
        logger.warn("Error in generateKeyPair: No keypair generated!");
        NoSuchAlgorithmException e = new NoSuchAlgorithmException();
        throw new RuntimeException(e);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  private static String generateAlias(XynaIdentityRepository identityRepo, String publickey) {
    String adjustedPublickey= publickey;
    String[] subelements = publickey.trim().split("\\s+");
    if (subelements.length<2) {
      adjustedPublickey = publickey.trim();
    } else {
      adjustedPublickey = subelements[1].trim();
    }
    byte[] bytepublic = adjustedPublickey.getBytes(StandardCharsets.UTF_8);
    String identity = identityRepo.generateIdentity(bytepublic);
    return identity;
  }


  public static void generateKeyPair(Integer keySize, String passPhrase, boolean overWriteExisting, EncryptionType type,
                                     XynaIdentityRepository identityRepo) {
    generateKeyPair(keySize, passPhrase, overWriteExisting, type, identityRepo, "", 0, "");
  }


  public static void writeKey(byte[] privateKeyBlob, byte[] publicKeyBlob, EncryptionType encryptionTypeValue, String passphrase,
                              boolean overwriteExisting, XynaIdentityRepository identityRepo, String identity, long priority, String typeclass) {

    try {
      if (overwriteExisting) {
        identityRepo.clearAll();
      }

      identityRepo.addWithAttributes(Optional.ofNullable(identity), encryptionTypeValue, privateKeyBlob, publicKeyBlob, Optional.ofNullable(passphrase), priority, typeclass);

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

      identityRepo.add(Optional.empty(), encryptionTypeValue, privateKeyBlob, publicKeyBlob, Optional.ofNullable(passphrase));

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


}
