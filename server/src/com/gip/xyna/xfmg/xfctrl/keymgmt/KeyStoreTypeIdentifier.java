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

import com.gip.xyna.update.Version;

public final class KeyStoreTypeIdentifier {
  
  private final static String DEFAULT_STARTING_VERSION = "1.0.0";
  
  private final String name;
  private final Version version;
  
  public KeyStoreTypeIdentifier(String name, String version) {
    this.name = name;
    this.version = new Version(version);
  }
  
  public KeyStoreTypeIdentifier(String name) {
    this(name, DEFAULT_STARTING_VERSION);
  }

  
  public String getName() {
    return name;
  }

  
  public Version getVersion() {
    return version;
  }
  

  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof KeyStoreTypeIdentifier)) {
      return false;
    }
    
    KeyStoreTypeIdentifier o = (KeyStoreTypeIdentifier) obj;
    if (name==null ^ 
        o.name==null) {
      return false;
    }
    if (name!=null && 
        o.name!=null &&
        !name.equals(o.name)) {
      return false;
    }
    if (version==null ^ o.version==null) {
      return false;
    }
    if (version!=null && 
        o.version!=null &&
        !version.toString().equals(o.version.toString())) {
      return false;
    }
    return true;
  }
  
  
  public int hashCode() {
    int h = name == null ? 0 : name.hashCode();
    return h * 31 + (version == null ? 0 : version.getString().hashCode());
  }
  
  @Override
  public String toString() {
    // TODO include version once relevant
    return String.valueOf(name);
  }


  public static KeyStoreTypeIdentifier with(String name) {
    return new KeyStoreTypeIdentifier(name);
  }
  
  
  public static KeyStoreTypeIdentifier with(String name, String version) {
    return new KeyStoreTypeIdentifier(name, version);
  }
  
  
}