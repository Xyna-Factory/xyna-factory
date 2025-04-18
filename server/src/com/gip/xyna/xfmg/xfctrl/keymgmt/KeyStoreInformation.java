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
package com.gip.xyna.xfmg.xfctrl.keymgmt;


public class KeyStoreInformation {
  
  private final String name;
  private final String type;
  private final String filename;
  
  public KeyStoreInformation(String name, String type, String filename) {
    this.name = name;
    this.type = type;
    this.filename = filename;
  }
  
  KeyStoreInformation(KeyStoreStorable storable) {
    this(storable.getName(), storable.getType(), storable.getFilename());
  }
  
  public String getName() {
    return name;
  }
  
  public String getType() {
    return type;
  }
  
  public String getFilename() {
    return filename;
  }

}
