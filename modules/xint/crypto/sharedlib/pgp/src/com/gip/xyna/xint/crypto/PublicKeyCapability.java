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

import java.util.Arrays;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPPublicKey;

public enum PublicKeyCapability {
  
  GENERAL(new int[] {PublicKeyAlgorithmTags.RSA_GENERAL, PublicKeyAlgorithmTags.DSA, PublicKeyAlgorithmTags.EC, PublicKeyAlgorithmTags.ECDH, PublicKeyAlgorithmTags.ECDSA,
                  PublicKeyAlgorithmTags.ELGAMAL_GENERAL, PublicKeyAlgorithmTags.DIFFIE_HELLMAN, PublicKeyAlgorithmTags.EDDSA}) {
    public boolean covers(PublicKeyCapability desiredCapability) {
      return true;
    }
  }, 
  SIGN(new int[] {PublicKeyAlgorithmTags.RSA_SIGN}) {
    public boolean covers(PublicKeyCapability desiredCapability) {
      switch (desiredCapability) {
        case SIGN :
          return true;
        default :
          return false;
      }
    }
  }, 
  ENCRYPT(new int[] {PublicKeyAlgorithmTags.RSA_ENCRYPT, PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT}) {
    public boolean covers(PublicKeyCapability desiredCapability) {
      switch (desiredCapability) {
        case ENCRYPT :
          return true;
        default :
          return false;
      }
    }
  },
  UNKNOWN(new int[] {}) {
    public boolean covers(PublicKeyCapability desiredCapability) {
      return false;
    }
  };
  
  private final int[] numericRepresentations;
  
  private PublicKeyCapability(int[] numericRepresentations) {
    this.numericRepresentations = numericRepresentations;
    Arrays.sort(this.numericRepresentations);
  }
  
  public static PublicKeyCapability determineCapapilities(PGPPublicKey pubKey) {
    for (PublicKeyCapability capability : values()) {
      if (Arrays.binarySearch(capability.numericRepresentations, pubKey.getAlgorithm()) >= 0) {
        return capability;
      }
    }
    return UNKNOWN;
  }

  public abstract boolean covers(PublicKeyCapability desiredCapability);

}
