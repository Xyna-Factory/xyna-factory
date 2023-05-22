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
package com.gip.xyna.xfmg.xfctrl.keymgmt;

import java.io.File;


public class ModuleManagedKeyStore implements KeyStore {

  private final String name;
  private final String filename;
  private final String passphrase;
  
  
  public ModuleManagedKeyStore(KeyStoreStorable storable, String passphrase) {
    this.name = storable.getName();
    this.filename = storable.getFilename();
    this.passphrase = passphrase;
  }
  
  public String getName() {
    return name;
  }
  
  public File getFile() {
    return new File(filename);
  }
  
  
  public String getPassphrase() {
    return passphrase;
  }
  
}