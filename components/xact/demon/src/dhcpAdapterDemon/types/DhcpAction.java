/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.types;

public enum DhcpAction {

  ALL(              "all",               "x"),
  DHCPREQUEST_NEW(  "DhcpRequest New",   "3n"),
  DHCPREQUEST_RENEW("DhcpRequest Renew", "3r"),
  DHCPRELEASE(      "DhcpRelease",       "7"),
  LEASEEXPIRE(      "LeaseExpire",       "99"),
  IGNORE(           "Ignore",            "");

  private String code;
  private String name;

  private DhcpAction( String name, String code ) {
    this.name=name;
    this.code=code;
  }
  
  /**
   * Gibt die passende ACTION für den übergebenen ActionCode zurück
   * @param code
   * @return
   */
  public static DhcpAction parse(String code) {
    for( DhcpAction a : values() ) {
      if( a.code.equals(code) ) {
        return a;
      }
    }
    return IGNORE; //alle unbekannten Actions werden ignoriert
  }
  
  public String getCode() {
    return code;
  }

  public static DhcpAction fromSnmpIndex(String index) {
    return values()[ Integer.parseInt( index ) -1 ];
  }

  @Override
  public String toString() {
    return name;
  }
  
}
