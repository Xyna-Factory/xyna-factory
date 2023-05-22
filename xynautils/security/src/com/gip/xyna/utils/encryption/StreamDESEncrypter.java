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
package com.gip.xyna.utils.encryption;

import java.io.InputStream;
import java.io.OutputStream;

import java.security.Provider;
import java.security.Security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Klasse zum verschluesseln von Streams
 */
public class StreamDESEncrypter {

  private Cipher ecipher;
  private Cipher dcipher;
  // Buffer used to transport the bytes from one stream to another
  byte[] buf = new byte[1024];

  // This method returns all available services types

  public static String[] getServiceTypes() {
    Set result = new HashSet();

    // All all providers
    Provider[] providers = Security.getProviders();
    for (int i = 0; i < providers.length; i++) {
      // Get services provided by each provider
      Set keys = providers[i].keySet();
      for (Iterator it = keys.iterator(); it.hasNext(); ) {
        String key = (String)it.next();
        key = key.split(" ")[0];

        if (key.startsWith("Alg.Alias.")) {
          // Strip the alias
          key = key.substring(10);
        }
        int ix = key.indexOf('.');
        result.add(key.substring(0, ix));
      }
    }
    return (String[])result.toArray(new String[result.size()]);
  }

  public static String[] getCryptoImpls(String serviceType) {
    Set result = new HashSet();

    // All all providers
    Provider[] providers = Security.getProviders();
    for (int i = 0; i < providers.length; i++) {
      // Get services provided by each provider
      Set keys = providers[i].keySet();
      for (Iterator it = keys.iterator(); it.hasNext(); ) {
        String key = (String)it.next();
        key = key.split(" ")[0];

        if (key.startsWith(serviceType + ".")) {
          result.add(key.substring(serviceType.length() + 1));
        }
        else if (key.startsWith("Alg.Alias." + serviceType + ".")) {
          // This is an alias
          result.add(key.substring(serviceType.length() + 11));
        }
      }
    }
    return (String[])result.toArray(new String[result.size()]);
  }

  /**
   * generiert einen key mittels der uebergebenen passphrase (muss 8 zeichen lang sein)
   * @param passPhrase
   * @throws Exception
   */
  public static SecretKey generateKey(String passPhrase) throws Exception {
    return new SecretKeySpec(passPhrase.getBytes(), "DES");
    /*    // 8-byte Salt
    byte[] salt =
    { (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32, (byte)0x56, (byte)0x35, (byte)0xE3,
      (byte)0x03 };

    // Iteration count
    int iterationCount = 19;
    KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
    return SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);*/
  }

  /**
   * initialisiert den encrypter. danach koennen mehrere streams mit dem gleichen key ver- und entschluesselt werden.
   * @param key
   * @throws Exception
   */
  public StreamDESEncrypter(SecretKey key) throws Exception {
    // Create an 8-byte initialization vector
    /*byte[] iv = new byte[] { (byte)0x8E, (byte)0x12, (byte)0x39, (byte)0x9C, (byte)0x07, (byte)0x72, (byte)0x6F, (byte)0x5A };
    AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);*/
    ecipher = Cipher.getInstance(key.getAlgorithm());
    dcipher = Cipher.getInstance(key.getAlgorithm());
    // CBC requires an initialization vector
    ecipher.init(Cipher.ENCRYPT_MODE, key); //, paramSpec);
    dcipher.init(Cipher.DECRYPT_MODE, key); //, paramSpec);
  }

  /**
   * liest die daten aus dem inputstream und schreibt sie verschluesselt in den outputstream
   * @param in
   * @param out
   * @throws Exception
   */
  public void encrypt(InputStream in, OutputStream out) throws Exception {
    // Bytes written to out will be encrypted
    out = new CipherOutputStream(out, ecipher);

    // Read in the cleartext bytes and write to out to encrypt
    int numRead = 0;
    while ((numRead = in.read(buf)) >= 0) {
      out.write(buf, 0, numRead);
    }
    out.close();
  }

  /**
   * liest die daten aus dem inputstream und schreibt sie entschluesselt in den outputstream
   * @param in
   * @param out
   * @throws Exception
   */
  public void decrypt(InputStream in, OutputStream out) throws Exception {
    // Bytes read from in will be decrypted
    in = new CipherInputStream(in, dcipher);

    // Read in the decrypted bytes and write the cleartext to out
    int numRead = 0;
    while ((numRead = in.read(buf)) >= 0) {
      out.write(buf, 0, numRead);
    }
    out.close();
  }

}