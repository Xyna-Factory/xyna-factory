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

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


public enum ObjectClassType {

  ABSTRACT(0,"LDAPAbstractObjectclass","xact.ldap"),
  STRUCTURAL(1,"LDAPDataNode","xact.ldap"),
  AUXILIARY(2,"LDAPAuxiliaryObjectclass","xact.ldap");
  
  private final int identifier;
  private final String basetypeName;
  private final String basetypePath;
  
  private ObjectClassType(int identifier, String basetypeName, String basetypePath) {
    this.identifier = identifier;
    this.basetypeName = basetypeName;
    this.basetypePath = basetypePath;
  }
  
  public static ObjectClassType getTypeByIdentifier(int identifier) {
    for (ObjectClassType type : values()) {
      if (type.identifier == identifier) {
        return type;
      }
    }
    throw new RuntimeException("Unknown objectClassType identifier: " + identifier);
  }
  
  
  public XmomType getXmomType() {
    return new XmomType(basetypePath, basetypeName, basetypeName);
  }

}
