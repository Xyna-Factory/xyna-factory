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
package xact.ldap.dictionary;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.novell.ldap.LDAPAttributeSchema;


public class LDAPAttributeTypeDictionaryEntry implements DictionaryEntry, Serializable {

  private static final long serialVersionUID = 1816119693353289191L;

  private List<String> names;
  private String mostProminentName;
  private String oid;
  private LDAPAttributeTypeDictionaryEntry superattribute;
  private boolean isList;
  private transient SyntaxDefinition syntax;

  public LDAPAttributeTypeDictionaryEntry() { /*Deserialization constructor*/ }

  LDAPAttributeTypeDictionaryEntry(LDAPAttributeSchema schema) {
    names = Arrays.asList(schema.getNames());
    if (names == null) {
      throw new RuntimeException("");
    }
    mostProminentName = LDAPObjectClassDictionaryEntry.seekMostProminentName(names);
    oid = schema.getID();
    String syntaxString = schema.getSyntaxString();
    if (syntaxString != null && syntaxString.length() > 0) { // might be inherited from parent
      syntax = SyntaxDefinition.getSyntaxDefinitionByOid(schema.getSyntaxString());
    }
    isList = !schema.isSingleValued();
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("");
    s.append("LDAPAttributeTypeDictionaryEntry { \n ");
    s.append(" oid: ").append(oid);
    s.append(", mostProminentName: ").append(mostProminentName);
    s.append(", names: [ ");
    for (String name : names) {
      s.append(name).append(" ");
    }
    s.append("] \n ");
    s.append(" \n } \n ");
    return s.toString();
  }


  public List<String> getNames() {
    return names;
  }


  public String getOid() {
    return oid;
  }


  public SyntaxDefinition getSyntax() {
    return syntax;
  }


  public LDAPAttributeTypeDictionaryEntry getSuperattribute() {
    return superattribute;
  }


  public boolean isList() {
    return isList;
  }


  public void setSuperattribute(LDAPAttributeTypeDictionaryEntry superattribute) {
    this.superattribute = superattribute;
    if (this.syntax == null) {
      this.syntax = superattribute.syntax;
    }
  }

  public String getProminentName() {
    return mostProminentName;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LDAPAttributeTypeDictionaryEntry) {
      return this.oid.equals(((LDAPAttributeTypeDictionaryEntry) obj).oid);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    // TODO cache
    if (oid == null) {
      return 0;
    } else {
      return oid.hashCode();
    }
  }


  public List<String> getDictionaryKeys() {
    List<String> dictionaryKeys = new ArrayList<String>();
    dictionaryKeys.add(oid);
    for (String name : names) {
      dictionaryKeys.add(name.toLowerCase());
    }
    return dictionaryKeys;
  }


  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    s.writeObject(syntax.getSyntaxOid());
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    String syntaxOid = (String) s.readObject();
    this.syntax = SyntaxDefinition.getSyntaxDefinitionByOid(syntaxOid);
  }

}
