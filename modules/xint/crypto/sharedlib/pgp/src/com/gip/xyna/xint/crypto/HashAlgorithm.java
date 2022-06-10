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
package com.gip.xyna.xint.crypto;

import org.bouncycastle.bcpg.HashAlgorithmTags;


public enum HashAlgorithm {
  
  MD5("MD5",HashAlgorithmTags.MD5),
  SHA1("SHA1", HashAlgorithmTags.SHA1),
  RIPEMD160("RIPEMD160", HashAlgorithmTags.RIPEMD160),
  DOUBLE_SHA("DOUBLE_SHA", HashAlgorithmTags.DOUBLE_SHA),
  MD2("MD2", HashAlgorithmTags.MD2),
  TIGER_192("TIGER_192", HashAlgorithmTags.TIGER_192),
  HAVAL_5_160("HAVAL_5_160", HashAlgorithmTags.HAVAL_5_160),
  SHA256("SHA256", HashAlgorithmTags.SHA256),
  SHA384("SHA384", HashAlgorithmTags.SHA384),
  SHA512("SHA512", HashAlgorithmTags.SHA512),
  SHA224("SHA224", HashAlgorithmTags.SHA224);
  
  
  private final String name;
  private final int pgpTag;
  
  private HashAlgorithm(String name, int pgpTag) {
    this.name = name;
    this.pgpTag = pgpTag;
  }
  
  public String getName() {
    return name;
  }
  
  public int getPgpTag() {
    return pgpTag;
  }
  
  public static HashAlgorithm getByName(String name) {
    for (HashAlgorithm algorithm : values()) {
      if (algorithm.name.equalsIgnoreCase(name)) {
        return algorithm;
      }
    }
    throw new IllegalArgumentException("Name '" + name + "' does not identify a valid HashAlgorithm");
  }
  
  public static HashAlgorithm getByPGPTage(int pgpTag) {
    for (HashAlgorithm algorithm : values()) {
      if (algorithm.pgpTag == pgpTag) {
        return algorithm;
      }
    }
    throw new IllegalArgumentException("PGPTag '" + pgpTag + "' does not identify a valid HashAlgorithm");
  }
}