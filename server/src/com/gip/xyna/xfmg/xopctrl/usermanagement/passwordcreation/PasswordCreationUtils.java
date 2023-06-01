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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;


public class PasswordCreationUtils {

  private static Logger logger = CentralFactoryLogging.getLogger(PasswordCreationUtils.class);

  public static enum EncryptionPhase {
    LOGIN {

      @Override
      public CreationAlgorithm getCreationAlgorithm() {
        return XynaProperty.PASSWORD_CREATION_HASH_ALGORITHM.get();
      }


      @Override
      public Integer getRounds() {
        return XynaProperty.PASSWORD_CREATION_ROUNDS.get();
      }


      @Override
      public String getSalt() {
        return XynaProperty.PASSWORD_CREATION_STATIC_SALT.get();
      }
    },
    
    PERSISTENCE {

      @Override
      public CreationAlgorithm getCreationAlgorithm() {
        return XynaProperty.PASSWORD_PERSISTENCE_HASH_ALGORITHM.get();
      }


      @Override
      public Integer getRounds() {
        return XynaProperty.PASSWORD_PERSISTENCE_ROUNDS.get();
      }


      @Override
      public String getSalt() {
        return PasswordCreationUtils.createPersistenceSalt(XynaProperty.PASSWORD_PERSISTENCE_SALT_LENGTH.get(), XynaProperty.PASSWORD_CREATION_STATIC_SALT.get());
      }
    };
    
    public abstract CreationAlgorithm getCreationAlgorithm();
    public abstract String getSalt();
    public abstract Integer getRounds();
  }
  
  public static String generatePassword(String cleartext, EncryptionPhase phase) {
    CreationAlgorithm algo = phase.getCreationAlgorithm();
    
    if (algo == null) {
      return cleartext;
    }
    
    String salt = phase.getSalt();
    Integer rounds = phase.getRounds();
    if (algo.equals(CreationAlgorithm.MD5) && salt == null && rounds == null) {
      return generateLegacyHash(cleartext);
    } else {
      return algo.createPassword(cleartext, salt, rounds);
    }
  }

  /**
   * Idee: hashe einmal zwischen GUI und Server, und einmal extra für die Datenbank. Vgl EncryptionPhase
   * Fälle: Konfigurierte HashFunktionen für Phase1+Phase2 Hashing (XynaProperty):
   *    1) (null, null)
   *    2) (X, null)
   *    3) (null, Y)
   *    4) (X, Y)
   * @param cleartext: 
   *    1, 3) nicht gehashed
   *    2, 4) X-gehashed
   * @param hashed
   *    1) nicht gehashed
   *    2) X-gehashed
   *    3) Y-gehashed
   *    4) X+Y-gehashed
   * @param phase
   * @return
   */
  public static boolean checkPassword(String cleartext, String hashed, EncryptionPhase phase) {
    if (phase.getCreationAlgorithm() == null) {
      return hashed.equals(cleartext);
    }
    
    CreationAlgorithm algo = CreationAlgorithm.extractAlgorithm(hashed);
    
    if (algo == null) {
      return hashed.equals(generateLegacyHash(cleartext));
    } else {
      return algo.checkPassword(cleartext, hashed);
    }
  }
  
  
  public static String generateLegacyHash(String password) {
    StringBuilder passwordBuilder = new StringBuilder();
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      try {
        md5.update(password.getBytes(Constants.DEFAULT_ENCODING));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      byte[] result = md5.digest();

      for (int i = 0; i < result.length; i++) {
        int halfbyte = (result[i] >>> 4) & 0x0F;
        int two_halfs = 0;
        do {
          if ((0 <= halfbyte) && (halfbyte <= 9))
            passwordBuilder.append((char) ('0' + halfbyte));
          else
            passwordBuilder.append((char) ('a' + (halfbyte - 10)));
          halfbyte = result[i] & 0x0F;
        } while (two_halfs++ < 1);
      }

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error during hash generation", e);
    }
    if (passwordBuilder.length() == 0) {
      throw new RuntimeException("Error during hash generation. generated password had length 0.");
    }

    return passwordBuilder.toString();
  }
  
  public static String createPersistenceSalt(int saltLength, String staticSalt) {
    byte[] bytes = new byte[saltLength];
    new SecureRandom().nextBytes(bytes);
    
    String salt;
    try {
      salt = new String(bytes, Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      salt = staticSalt;
      logger.warn("Could not generate salt, use static salt '" + salt + "'", e);
    }
    
    return salt;
  }
  
}
