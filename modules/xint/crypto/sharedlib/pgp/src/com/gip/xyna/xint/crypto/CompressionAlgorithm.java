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

import org.bouncycastle.bcpg.CompressionAlgorithmTags;


public enum CompressionAlgorithm {
  
  UNCOMPRESSED("UNCOMPRESSED", CompressionAlgorithmTags.UNCOMPRESSED),
  ZIP("ZIP", CompressionAlgorithmTags.ZIP),
  ZLIB("ZLIB", CompressionAlgorithmTags.ZLIB),
  BZIP2("BZIP2", CompressionAlgorithmTags.BZIP2);
  
  
  private final String name;
  private final int pgpTag;
  
  private CompressionAlgorithm(String name, int pgpTag) {
    this.name = name;
    this.pgpTag = pgpTag;
  }
  
  public String getName() {
    return name;
  }
  
  public int getPgpTag() {
    return pgpTag;
  }
  
  public static CompressionAlgorithm getByName(String name) {
    for (CompressionAlgorithm algorithm : values()) {
      if (algorithm.name.equalsIgnoreCase(name)) {
        return algorithm;
      }
    }
    throw new IllegalArgumentException("Name '" + name + "' does not identify a valid CompressionAlgorithm");
  }
  
  public static CompressionAlgorithm getByPGPTage(int pgpTag) {
    for (CompressionAlgorithm algorithm : values()) {
      if (algorithm.pgpTag == pgpTag) {
        return algorithm;
      }
    }
    throw new IllegalArgumentException("PGPTag '" + pgpTag + "' does not identify a valid CompressionAlgorithm");
  }
}