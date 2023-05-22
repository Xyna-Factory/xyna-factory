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

import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;


public enum SymmetricKeyAlgorithm {
  
  IDEA("IDEA", SymmetricKeyAlgorithmTags.IDEA),
  TRIPLE_DES("TRIPLE_DES", SymmetricKeyAlgorithmTags.TRIPLE_DES),
  CAST5("CAST5", SymmetricKeyAlgorithmTags.CAST5),
  BLOWFISH("BLOWFISH", SymmetricKeyAlgorithmTags.BLOWFISH),
  SAFER("SAFER", SymmetricKeyAlgorithmTags.SAFER),
  DES("DES", SymmetricKeyAlgorithmTags.DES),
  AES_128("AES_128", SymmetricKeyAlgorithmTags.AES_128),
  AES_192("AES_192", SymmetricKeyAlgorithmTags.AES_192),
  AES_256("AES_256", SymmetricKeyAlgorithmTags.AES_256),
  TWOFISH("TWOFISH", SymmetricKeyAlgorithmTags.TWOFISH),
  CAMELLIA_128("CAMELLIA_128", SymmetricKeyAlgorithmTags.CAMELLIA_128),
  CAMELLIA_192("CAMELLIA_192", SymmetricKeyAlgorithmTags.CAMELLIA_192),
  CAMELLIA_256("CAMELLIA_256", SymmetricKeyAlgorithmTags.CAMELLIA_256);
  
  
  private final String name;
  private final int pgpTag;
  
  private SymmetricKeyAlgorithm(String name, int pgpTag) {
    this.name = name;
    this.pgpTag = pgpTag;
  }
  
  public String getName() {
    return name;
  }
  
  public int getPgpTag() {
    return pgpTag;
  }
  
  public static SymmetricKeyAlgorithm getByName(String name) {
    for (SymmetricKeyAlgorithm algorithm : values()) {
      if (algorithm.name.equalsIgnoreCase(name)) {
        return algorithm;
      }
    }
    throw new IllegalArgumentException("Name '" + name + "' does not identify a valid SymmetricKeyAlgorithm");
  }
  
  public static SymmetricKeyAlgorithm getByPGPTage(int pgpTag) {
    for (SymmetricKeyAlgorithm algorithm : values()) {
      if (algorithm.pgpTag == pgpTag) {
        return algorithm;
      }
    }
    throw new IllegalArgumentException("PGPTag '" + pgpTag + "' does not identify a valid SymmetricKeyAlgorithm");
  }
}