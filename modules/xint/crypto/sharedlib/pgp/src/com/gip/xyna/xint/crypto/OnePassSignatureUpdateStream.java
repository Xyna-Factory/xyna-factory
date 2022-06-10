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

import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.openpgp.PGPOnePassSignature;

public class OnePassSignatureUpdateStream extends OutputStream {
  
  private OutputStream parentOS;
  private PGPOnePassSignature onePassSignature;
  
  OnePassSignatureUpdateStream(OutputStream parentOS, PGPOnePassSignature onePassSignature) {
    this.parentOS = parentOS;
    this.onePassSignature = onePassSignature;
  }
  
  @Override
  public void write(int b) throws IOException {
    onePassSignature.update((byte) b);
    parentOS.write(b);
  }
  
  @Override
  public void write(byte[] b) throws IOException {
    onePassSignature.update(b);
    parentOS.write(b);
  }
  
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    onePassSignature.update(b, off, len);
    parentOS.write(b, off, len);
  }
  
}