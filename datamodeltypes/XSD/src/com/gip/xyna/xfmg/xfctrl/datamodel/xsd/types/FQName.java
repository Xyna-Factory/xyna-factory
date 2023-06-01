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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types;

import org.w3c.dom.Node;

/**
 *
 */
public class FQName {
  
  public FQName(String namespace, String name) {
    this.namespace = namespace;
    this.name = name;
  }
  private final String namespace;
  private final String name;
 
  @Override
  public String toString() {
    return namespace+":"+name;
  }
  
  public String toQualifiedName() {
    return namespace+":"+name;
  }
  public String toOptionalQualifiedName(String defNS) {
    if( defNS == null ) {
      if( namespace == null ) {
        return name;
      }
    } else {
      if( defNS.equals(namespace) ) {
        return name;
      }
    }
    return namespace+":"+name;
  }
  
  public String getName() {
    return name;
  }
  public String getNamespace() {
    return namespace;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FQName other = (FQName) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (namespace == null) {
      if (other.namespace != null)
        return false;
    } else if (!namespace.equals(other.namespace))
      return false;
    return true;
  }

  
  /**
   * parsen von
   * 1) <name>
   * 2) <prefix>:<name>
   * 3) {<namespace>}<name> 
   */
  public static FQName parse(String type, Node context) {
    int idx = type.indexOf(":");
    if (idx > -1) {
      String ns = type.substring(0, idx);
      String tn = type.substring(idx + 1);
      String nsuri = context.lookupNamespaceURI(ns);
      if (nsuri == null) {
        throw new RuntimeException("namespace prefix \"" + ns + "\" could not be resolved");
      }
      return new FQName(nsuri, tn);
    } else if (type.startsWith("{")) {
      idx = type.indexOf("}");
      if (idx == -1) {
        throw new RuntimeException("Namespace syntax invalid. missing } in <" + type + ">.");
      }
      return new FQName(type.substring(1, idx), type.substring(idx + 1));
    } else {
      return new FQName(null, type);
    }
  }

}
