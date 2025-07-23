/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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


package com.gip.xyna.xact.trigger;

import com.hierynomus.sshj.transport.mac.Macs;


public enum SshjMacFactory {
  
  HMACSHA2256(Macs.HMACSHA2256()),
  HMACSHA2256Etm(Macs.HMACSHA2256Etm()),
  HMACSHA2512(Macs.HMACSHA2512()),
  HMACSHA2512Etm(Macs.HMACSHA2512Etm()),
  HMACSHA1(Macs.HMACSHA1()),
  HMACSHA1Etm(Macs.HMACSHA1Etm()),
  HMACSHA196(Macs.HMACSHA196()),
  HMACSHA196Etm(Macs.HMACSHA196Etm()),
  HMACMD5(Macs.HMACMD5()),
  HMACMD5Etm(Macs.HMACMD5Etm()),
  HMACMD596(Macs.HMACMD596()),
  HMACMD596Etm(Macs.HMACMD596Etm()),
  HMACRIPEMD160(Macs.HMACRIPEMD160()),
  HMACRIPEMD160Etm(Macs.HMACRIPEMD160Etm()),
  HMACRIPEMD16096(Macs.HMACRIPEMD16096()),
  HMACRIPEMD160OpenSsh(Macs.HMACRIPEMD160OpenSsh())
  ;
  
  private final Macs.Factory factory;
  
  private SshjMacFactory(Macs.Factory factory) {
    this.factory = factory;
  }

  
  public Macs.Factory getFactory() {
    return factory;
  }
  
  public static String getDescription() {
    StringBuilder ret = new StringBuilder();
    boolean isFirst = true;
    for (SshjMacFactory val : values()) {
      if (isFirst) { isFirst = false; }
      else { ret.append(":"); }
      ret.append(val.toString());
    }
    return ret.toString();
  }
  
}
