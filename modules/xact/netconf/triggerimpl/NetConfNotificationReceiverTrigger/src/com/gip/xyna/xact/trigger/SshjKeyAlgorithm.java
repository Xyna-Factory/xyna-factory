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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hierynomus.sshj.key.KeyAlgorithm;
import com.hierynomus.sshj.key.KeyAlgorithms;
import com.hierynomus.sshj.transport.mac.Macs;

import net.schmizz.sshj.common.Factory;


public enum SshjKeyAlgorithm {
  
  SSHDSA(KeyAlgorithms.SSHDSA()),
  EdDSA25519CertV01(KeyAlgorithms.EdDSA25519CertV01()),
  EdDSA25519(KeyAlgorithms.EdDSA25519()),
  ECDSASHANistp521CertV01(KeyAlgorithms.ECDSASHANistp521CertV01()),
  ECDSASHANistp521(KeyAlgorithms.ECDSASHANistp521()),
  ECDSASHANistp384CertV01(KeyAlgorithms.ECDSASHANistp384CertV01()),
  ECDSASHANistp384(KeyAlgorithms.ECDSASHANistp384()),
  ECDSASHANistp256CertV01(KeyAlgorithms.ECDSASHANistp256CertV01()),
  ECDSASHANistp256(KeyAlgorithms.ECDSASHANistp256()),
  RSASHA512(KeyAlgorithms.RSASHA512()),
  RSASHA256(KeyAlgorithms.RSASHA256()),
  SSHRSACertV01(KeyAlgorithms.SSHRSACertV01()),
  SSHDSSCertV01(KeyAlgorithms.SSHDSSCertV01())
  ;
  
  private final KeyAlgorithms.Factory factory;
  
  private SshjKeyAlgorithm(KeyAlgorithms.Factory factory) {
    this.factory = factory;
  }
  
  public KeyAlgorithms.Factory getFactory() {
    return factory;
  }
  
  public static String getDescription() {
    StringBuilder ret = new StringBuilder();
    boolean isFirst = true;
    for (SshjKeyAlgorithm val : values()) {
      if (isFirst) { isFirst = false; }
      else { ret.append(":"); }
      ret.append(val.toString());
    }
    return ret.toString();
  }
  
  public static List<SshjKeyAlgorithm> valuesAsList() {
    return Arrays.asList(values());
  }
  
  public static List<Factory.Named<KeyAlgorithm>> extractFactories(List<SshjKeyAlgorithm> input) {
    List<Factory.Named<KeyAlgorithm>> ret = new ArrayList<>();
    for (SshjKeyAlgorithm val : input) { ret.add(val.getFactory()); }
    return ret;
  }
  
}
