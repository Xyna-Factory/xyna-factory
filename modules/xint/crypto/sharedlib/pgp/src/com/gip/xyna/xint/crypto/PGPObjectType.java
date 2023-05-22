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

import org.bouncycastle.bcpg.Packet;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPMarker;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;

public enum PGPObjectType {
  // Produced from PGPObjectFactory.nextObject
  PGPSignatureList(PGPSignatureList.class),
  PGPSecretKeyRing(PGPSecretKeyRing.class),
  PGPPublicKeyRing(PGPPublicKeyRing.class),
  PGPPublicKey(PGPPublicKey.class),
  PGPCompressedData(PGPCompressedData.class),
  PGPLiteralData(PGPLiteralData.class),
  PGPEncryptedDataList(PGPEncryptedDataList.class),
  PGPOnePassSignatureList(PGPOnePassSignatureList.class),
  PGPMarker(PGPMarker.class),
  Packet(Packet.class),
  // Produced from PGPEncryptedDataList.next
  PGPPBEEncryptedData(PGPPBEEncryptedData.class),
  PGPPublicKeyEncryptedData(PGPPublicKeyEncryptedData.class),
  // Produced from PGPOnePassSignatureList.next
  PGPOnePassSignature(PGPOnePassSignature.class),
  // Produced from PGPSignatureList.next
  PGPSignature(PGPSignature.class)
  ;
  
  
  private final Class<?> pgpClass;
  
  PGPObjectType(Class<?> pgpClass) {
    this.pgpClass = pgpClass;
  }
  
  public static PGPObjectType determineByObject(Object obj) {
    for (PGPObjectType type : values()) {
      if (type.pgpClass.isInstance(obj)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown Object type: " + obj.getClass().getName());
  }
}