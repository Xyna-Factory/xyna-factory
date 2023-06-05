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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamespacePrefixCache {
  
  public static final String NAMESPACE_XSI = "http://www.w3.org/2001/XMLSchema-instance";
  public static final String NAMESPACE_XS = "http://www.w3.org/2001/XMLSchema";
  public static final String NAMESPACE_XMLNS = "http://www.w3.org/2000/xmlns/";
  
  private boolean omitSingleNamespacePrefix;
  private Map<String,String> namespacePrefixes = new HashMap<String,String>();
  private List<String> knownNamepaces = new ArrayList<String>();
  private Map<String,String> usedNamespacePrefixes = new HashMap<String,String>();
  private int prefixCounter = 0;
  
  public NamespacePrefixCache(boolean omitSingleNamespacePrefix) {
    this.omitSingleNamespacePrefix = omitSingleNamespacePrefix;
    addNamespace(NAMESPACE_XSI, "xsi");
    addNamespace(NAMESPACE_XS, "xs");
    addNamespace(NAMESPACE_XMLNS, "xmlns");
  }

  public void addNamespace(String namespace, String prefix) {
    if( ! namespacePrefixes.containsKey(namespace) ) {
      knownNamepaces.add(namespace);
      namespacePrefixes.put( namespace, prefix);
    }
  }

  public void addNamespaces(List<String> namespaces) {
    knownNamepaces.addAll(namespaces);
  }
 
  public Map<String,String> getNamespacePrefixes() {
    return namespacePrefixes;
  }

  public String getNamespacePrefix(String namespace) {
    String prefix = usedNamespacePrefixes.get(namespace);
    if( prefix == null ) {
      prefix = createNamespacePrefix(namespace);
      usedNamespacePrefixes.put(namespace, prefix);
    }
    return prefix;
  }
  
  private String createNamespacePrefix(String namespace) {
    String prefix = namespacePrefixes.get(namespace);
    if( prefix != null ) {
      return prefix;
    }
    if( prefixCounter == 0 && omitSingleNamespacePrefix ) {
      prefix = "";
    } else {
      prefix = "p"+prefixCounter;
    }
    ++prefixCounter;
    return prefix;
  }

  public Map<String,String> getUsedNamespacePrefixes() {
    return usedNamespacePrefixes;
  }
}
