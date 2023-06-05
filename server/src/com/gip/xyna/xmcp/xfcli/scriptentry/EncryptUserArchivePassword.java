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
package com.gip.xyna.xmcp.xfcli.scriptentry;



import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.CreationAlgorithm;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils;



public class EncryptUserArchivePassword {

  public static void main(String[] args) {
    if (args.length != 7) {
      System.out.println("Expected 7 parameters.");
      System.exit(1);
    }
    String passwordHashed = CreatePassword(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
    System.out.println(passwordHashed);
  }


  private static String CreatePassword(String password, String c_alg, String c_round, String c_salt, String p_alg, String p_round,
                                       String p_salt_length) {

    Integer c_roundInt = null;
    Integer p_roundInt = null;
    CreationAlgorithm c_algObj;
    CreationAlgorithm p_algObj;
    int p_salt_lengthInt = XynaProperty.PASSWORD_PERSISTENCE_SALT_LENGTH.get();
    if (c_alg.equals("?")) {
      c_algObj = XynaProperty.PASSWORD_CREATION_HASH_ALGORITHM.getDefaultValue();
    } else {
      c_algObj = Enum.valueOf(CreationAlgorithm.class, c_alg);
    }
    if (c_round.equals("?")) {
      c_roundInt = XynaProperty.PASSWORD_CREATION_ROUNDS.getDefaultValue();
    } else {
      c_roundInt = Integer.valueOf(c_round);
    }
    if (c_salt.equals("?")) {
      c_salt = XynaProperty.PASSWORD_CREATION_STATIC_SALT.getDefaultValue();
    }
    if (p_alg.equals("?")) {
      p_algObj = XynaProperty.PASSWORD_PERSISTENCE_HASH_ALGORITHM.getDefaultValue();
    } else {
      p_algObj = Enum.valueOf(CreationAlgorithm.class, p_alg);
    }
    if (p_round.equals("?")) {
      p_roundInt = XynaProperty.PASSWORD_PERSISTENCE_ROUNDS.getDefaultValue();
    } else {
      p_roundInt = Integer.valueOf(p_round);
    }
    if (!p_salt_length.equals("?")) {
      p_salt_lengthInt = Integer.valueOf(p_salt_length);
    }


    String p_salt = PasswordCreationUtils.createPersistenceSalt(p_salt_lengthInt, XynaProperty.PASSWORD_CREATION_STATIC_SALT.get());

    String passwordHash = Hashpassword(password, c_algObj, c_roundInt, c_salt);
    passwordHash = Hashpassword(passwordHash, p_algObj, p_roundInt, p_salt);

    return passwordHash;
  }


  private static String Hashpassword(String password, CreationAlgorithm algo, Integer rounds, String salt) {
    
    if(algo == null) {
      return password;
    }


    if (algo.equals(CreationAlgorithm.MD5) && salt == null && rounds == null) {
      return PasswordCreationUtils.generateLegacyHash(password);
    } else {
      return algo.createPassword(password, salt, rounds);
    }
  }

}
