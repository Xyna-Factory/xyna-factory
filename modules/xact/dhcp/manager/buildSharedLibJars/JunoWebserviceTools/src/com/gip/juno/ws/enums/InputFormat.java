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
package com.gip.juno.ws.enums;


public enum InputFormat {
  MAC("mac"), 
  NUMBER("number"),
  NO_SPECIAL("nospecial"),
  IPv4("ipv4"),
  IPv4LIST("ipv4list"),
  IPv4EXCLUSIONS("ipv4exclusions"),
  IPv6("ipv6"),
  IPv6LIST("ipv6list"),
  IPv6EXCLUSIONS("ipv6exclusions");
  
  private String representation;
  
  private InputFormat(String representation) {
    this.representation = representation;
  }
  
  public String getStringRepresentation() {
    return representation;
  }
}
