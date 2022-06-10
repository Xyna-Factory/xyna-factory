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
package xact.ldap.dictionary;


public enum CommonOidRealm {
  //http://www.alvestrand.no/objectid/1.3.6.1.4.1.html
    
  // inside core:
  // 1.3.6.1.4.1.1466.115.121.1.*
  // 2.5.6.*
  // 1.3.6.1.4.1.250.3.*
  // 0.9.2342.19200300.100.1.*
  // 1.3.6.1.1.3.*
  // 1.2.840.113549.1.9.*
  
  // our own:
  // 0.0.0.*
  
  CORE("core", new String[] {"1.3.6.1.4.1.1466.115.121.1",
                             "2.5.6",
                             "1.3.6.1.4.1.250.3",
                             "0.9.2342.19200300.100.1",
                             "1.3.6.1.1.3",
                             "1.2.840.113549.1.9"}),
  HSS("hss", new String[] {"0.0.0"}),
  UNKNOWN("misc", new String[0]);
  
  private final String name;
  private final String[] oidPrefixes;
  
  CommonOidRealm(String name, String[] oidPrefixes) {
    this.name = name;
    this.oidPrefixes = oidPrefixes;
  }
  
  public static CommonOidRealm getCommenOidRealm(String oid) {
    for (CommonOidRealm realm : values()) {
      for (String prefix : realm.oidPrefixes) {
        if (oid.startsWith(prefix)) {
          return realm;
        }
      }
    }
    return UNKNOWN;
  }
  
  public String getName() {
    return name;
  }
  
}
