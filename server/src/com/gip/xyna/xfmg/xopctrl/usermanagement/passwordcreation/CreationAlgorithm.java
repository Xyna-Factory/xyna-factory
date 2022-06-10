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
package com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mindrot.jbcrypt.BCrypt;

import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.xfmg.Constants;


public enum CreationAlgorithm {

  MD5("1") {

    @Override
    public String createPassword(String cleartext, String salt, Integer rounds) {
      return createMessageDigestPassword(cleartext, salt, rounds, "MD5");
    }

    @Override
    public boolean checkPassword(String plaintext, String hashed) {
      Integer rounds = extractRounds(hashed);
      String salt = extractSalt(hashed, 16);

      return hashed.equals(createPassword(plaintext, salt, rounds));
    }
  },
  
  SHA256("5") {

    @Override
    public String createPassword(String cleartext, String salt, Integer rounds) {
      return createMessageDigestPassword(cleartext, salt, rounds, "SHA-256");
    }
    
    @Override
    public boolean checkPassword(String plaintext, String hashed) {
      Integer rounds = extractRounds(hashed);
      String salt = extractSalt(hashed, 32);

      return hashed.equals(createPassword(plaintext, salt, rounds));
    }
  },
  
  BCRYPT("2a") {
    
    @Override
    public String createPassword(String cleartext, String salt, Integer rounds) {
      //das �bergebene Salt wird ignoriert und ein neues generiert;
      //der Bcrypt-Algorithmus ben�tigt ein 128-Bit Salt;
      //hashpw erwartet das Salt im Format $<algo>$<rounds>$<salt>,
      //wobei <salt> Base64 kodiert sein muss
      salt = BCrypt.gensalt(rounds);
      return BCrypt.hashpw(cleartext, salt);
    }
    
    public boolean checkPassword(String plaintext, String hashed) {
      return BCrypt.checkpw(plaintext, hashed);
    }
  };
  
  private final static char MARKER = '$';
  
  private final String identifier;
  
  private CreationAlgorithm(String identifier) {
    this.identifier = identifier;
  }
  
  public String getIdentifier() {
    return identifier;
  }
  
  public abstract String createPassword(String cleartext, String salt, Integer rounds);

  public abstract boolean checkPassword(String cleartext, String hashed);


  protected String createMessageDigestPassword(String cleartext, String salt, Integer rounds, String messageDigestAlgorithm) {
    long executions;
    if (rounds == null) {
      executions = 1;
    } else {
      executions = Math.round(Math.max(1, Math.pow(2.0, rounds)));
    }
    byte[] saltBytes;
    try {
      if (salt == null) {
        saltBytes = new byte[0];
      } else {
        saltBytes = salt.getBytes(Constants.DEFAULT_ENCODING);
      }

      MessageDigest messagedigest = MessageDigest.getInstance(messageDigestAlgorithm);
      byte[] currentBytes = cleartext.getBytes(Constants.DEFAULT_ENCODING);
      for (int i = 0; i < executions; i++) {
        currentBytes = merge(currentBytes, saltBytes);
        messagedigest.update(currentBytes);
        currentBytes = messagedigest.digest();
      }

      String encodedValue = Base64.encode(merge(saltBytes, currentBytes));
      StringBuilder passwordBuilder = new StringBuilder().append(MARKER).append(identifier).append(MARKER);
      if (rounds == null) {
        passwordBuilder.append("00");  
      } else {
        if (rounds < 10) {
          passwordBuilder.append('0');
        }
        passwordBuilder.append(rounds);
      }
      passwordBuilder.append(MARKER).append(encodedValue);
      return passwordBuilder.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error during password generation", e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Error during password generation", e);
    }

  }
  
  
  private static byte[] merge(byte[] a, byte[] b) {
    byte[] bytes = new byte[a.length + b.length];
    System.arraycopy(a, 0, bytes, 0, a.length);
    System.arraycopy(b, 0, bytes, a.length, b.length);
    return bytes;
  }
    
  
  public static CreationAlgorithm getFromIdentifier(String identifier) {
    for (CreationAlgorithm algo : values()) {
      if (algo.identifier.equalsIgnoreCase(identifier)) {
        return algo;
      }
    }
    throw new IllegalArgumentException("Invalid creation algorithm identifier: " + identifier);
  }

  
  /**
   * Extrahiert die Rundenzahl 'rounds' aus einem String der Form $&lt;algorithm id&gt;$&lt;rounds&gt;$&lt;salt&gt;&lt;password&gt;,
   * wobei 'salt' und 'password' Base64 kodiert sind
   * @param hashed
   * @return
   */
  protected Integer extractRounds(String hashed) {
    String[] split = hashed.split("\\"+MARKER);
    return Integer.valueOf(split[2]);
  }
  
  
  /**
   * Extrahiert 'salt' aus einem String der Form $&lt;algorithm id&gt;$&lt;rounds&gt;$&lt;salt&gt;&lt;password&gt;,
   * wobei 'salt' und 'password' Base64 kodiert sind
   * @param hashed
   * @param passwordLength L�nge des im String enthaltenen Passworts
   * @return
   */
  protected String extractSalt(String hashed, int passwordLength) {
    String[] split = hashed.split("\\"+MARKER);
    String saltAndPw = split[3];
    
    try {
      //Base64 decodieren
      byte[] bytes = Base64.decode(saltAndPw);
      
      //Passwort-Anteil abschneiden
      byte[] saltBytes = new byte[bytes.length - passwordLength];
      System.arraycopy(bytes, 0, saltBytes, 0, saltBytes.length);
      
      return new String(saltBytes, Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      throw new RuntimeException("Error during password generation", e);
    }
  }

  
  /**
   * Extrahiert den CreationAlgoritm aus einem String der Form $&lt;algorithm id&gt;$&lt;rounds&gt;$&lt;salt&gt;&lt;password&gt;
   * @param hashed
   * @return
   */
  public static CreationAlgorithm extractAlgorithm(String hashed) {
    if (!hashed.startsWith("" + MARKER)) {
      return null; //mit PasswordCreationUtils.generateLegacyHash verschl�sselt
    }
    
    String[] split = hashed.split("\\"+MARKER);
    return CreationAlgorithm.getFromIdentifier(split[1]);
  }
}
