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
package xact.ssh;

import java.util.ArrayList;
import java.util.List;


public enum AuthenticationMethod {
  PUBLICKEY(new String[] {"publickey"}, PublicKey.class),
  PASSWORD(new String[] {"password","keyboard-interactive"}, Password.class),
  HOSTBASED(new String[] {"hostbased"}, HostBased.class);
  
  private final String[] identifiers;
  private final Class<? extends AuthenticationMode> xynaRepresentation;
  
  private AuthenticationMethod(String[] identifiers, Class<? extends AuthenticationMode> xynaRepresentation) {
    this.identifiers = identifiers;
    this.xynaRepresentation = xynaRepresentation;
  }
  
  public String[] getIdentifiers() {
    return identifiers;
  }
  
  
  public static <M extends AuthenticationMode> AuthenticationMethod getByXynaRepresentation(M xynaRepresentation) {
    if (xynaRepresentation != null) {
      for (AuthenticationMethod mode : values()) {
        if (mode.xynaRepresentation.isInstance(xynaRepresentation)) {
          return mode;
        }
      }
    }
    return null;
  }
  
  
  public static <M extends AuthenticationMode> List<AuthenticationMethod> getByXynaRepresentation(List<M> xynaRepresentations) {
    List<AuthenticationMethod> methods = new ArrayList<AuthenticationMethod>();
    for (M xynaRepresentation : xynaRepresentations) {
      AuthenticationMethod method = getByXynaRepresentation(xynaRepresentation);
      if (method != null) {
        methods.add(method);
      }
    }
    return methods;
  }
  
}
