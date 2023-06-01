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

package com.gip.xyna.xint.crypto;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xint.crypto.PGPKeyStoreParameter.SubKeyExtractor;

// checks for desiredCapability satisfaction and tries to merge keys without userIds with their masters
class CapabilityCheckingRingMergingExtractor implements SubKeyExtractor<PGPPublicKeyRing, PGPPublicKey> {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(CapabilityCheckingRingMergingExtractor.class);
  
  private final PublicKeyCapability desiredCapability;
  private PGPPublicKey lastUserIdSupplier;
  
  public CapabilityCheckingRingMergingExtractor(PublicKeyCapability desiredCapability) {
    this.desiredCapability = desiredCapability;
  }

  public PGPPublicKey getInnerKey(PGPPublicKeyRing keyRing) {
    Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
    while (keyIter.hasNext()) {
      PGPPublicKey pgpPublicKey = (PGPPublicKey) keyIter.next();
      if (pgpPublicKey.getUserIDs().hasNext()) {
        lastUserIdSupplier = pgpPublicKey;
      }
      PublicKeyCapability capabilityOfKey = PublicKeyCapability.determineCapapilities(pgpPublicKey);
      if (capabilityOfKey.covers(desiredCapability)) {
        if (!pgpPublicKey.getUserIDs().hasNext() &&
            lastUserIdSupplier != null) {
          try {
            return new MergedPublicKey(pgpPublicKey, lastUserIdSupplier);
          } catch (PGPException e) {
            logger.warn("Failed to reconstruct key", e);
            return pgpPublicKey;
          }
        } else {
          return pgpPublicKey;
        }
      }
    }
    return null;
  }
  
}