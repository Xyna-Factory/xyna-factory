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
      //NoSuchAlgorithmException e = new NoSuchAlgorithmException();
      //throw new RuntimeException(e);
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

/*
  // Alternative: Works with new JSCH Fork from github.com/mwiede/jsch (version 0.2.5 - 07.12.2022)
  public static void generateKeyPair_JSCH(Integer keysize, String passphrase, boolean overwriteExisting, EncryptionType type,
                                          XynaIdentityRepository identityRepo) {

    com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
    int size = 1024;
    if (keysize != null) {
      size = keysize.intValue();
    }
    com.jcraft.jsch.KeyPair pair;

    // int JSCHType = com.jcraft.jsch.KeyPair.UNKNOWN;
    int JSCHType = com.jcraft.jsch.KeyPair.RSA; // Default;
    if (type.getStringRepresentation().equalsIgnoreCase(EncryptionType.DSA.getStringRepresentation())) {
      JSCHType = com.jcraft.jsch.KeyPair.DSA;
    }
    if (type.getStringRepresentation().equalsIgnoreCase(EncryptionType.RSA.getStringRepresentation())) {
      JSCHType = com.jcraft.jsch.KeyPair.RSA;
    }

    try {
      pair = com.jcraft.jsch.KeyPair.genKeyPair(jsch, JSCHType, size);
    } catch (com.jcraft.jsch.JSchException e) {
      throw new RuntimeException("", e);
    }
    if (passphrase != null) {
      pair.setPassphrase(passphrase);
    }
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    pair.writePrivateKey(baos);
    byte[] privateKeyBlob = baos.toByteArray();
    baos = new java.io.ByteArrayOutputStream();
    pair.writePublicKey(baos, "");
    byte[] publicKeyBlob = baos.toByteArray();

    // String StringPrivate =null;
    // String StringPublic = null;
    // try {
    //     StringPrivate = new String(privateKeyBlob,"UTF-8");
    //     StringPublic = new String(publicKeyBlob,"UTF-8");
    // } catch (Exception e) {
    //     throw new RuntimeException(e);
    // }
    // byte[] rsaPubByte = StringPublic.getBytes();
    // byte[] rsaPrvByte = StringPrivate.getBytes();

    writeKey(privateKeyBlob, publicKeyBlob, type, passphrase, overwriteExisting, identityRepo);

  }
*/

}
