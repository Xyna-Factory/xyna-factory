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
package com.gip.xyna.xfmg.xfctrl.keymgmt;

import java.io.FileInputStream;
import java.util.Map;

public class JavaSecurityKeyStore implements KeyStore {

  private String name;
  private String extfilename;
  private String filetype;
  
  private transient java.security.KeyStore keyStore;
  
  
  public JavaSecurityKeyStore(KeyStoreStorable storable) {
    this.name = storable.getName();
    this.extfilename = storable.getFilename();
    this.filetype = JavaSecurityStoreType.FILE_TYPE.getFromMap(storable.getParameterMap());
  }

  
  public String getName() {
    return name;
  }

  
  public String getPassphrase() {
    return JavaSecurityStoreType.getPassphrase(name);
  }

  
  public Class<java.security.KeyStore> getTargetClass() {
    return java.security.KeyStore.class;
  }

  public java.security.KeyStore convert(Map<String, Object> parsedParams) throws Exception {
    if (keyStore == null) {
      java.security.KeyStore ks = java.security.KeyStore.getInstance(filetype);
      String passphrase = getPassphrase();
      char[] passphraseChars = null;
      if (passphrase != null && passphrase.length() > 0) {
        passphraseChars = passphrase.toCharArray();
      }
      try (FileInputStream fis = new FileInputStream(extfilename)) {
        ks.load(fis, passphraseChars);
      }
      this.keyStore = ks;
    }
    return keyStore;
  }


  
  
}
