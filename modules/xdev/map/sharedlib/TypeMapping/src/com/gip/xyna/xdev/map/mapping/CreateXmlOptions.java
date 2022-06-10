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
package com.gip.xyna.xdev.map.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class CreateXmlOptions {
  
  private boolean omitNullTags;//Weglassen leerer Tags
  private boolean omitSingleNamespacePrefix; //Weglassen des NamespacePrefix, wenn es nur einen Namespace gibt
  private boolean booleanAsInteger; //Ausgeben von Booleans als 1/0 statt true/false 
  
  private Map<String,String> namespacePrefixes;
  
  public CreateXmlOptions() {
    this.namespacePrefixes = new HashMap<String,String>();
  }
  
  public void addNamespace(String namespace, String prefix) {
    if( ! namespacePrefixes.containsKey(namespace) ) {
      namespacePrefixes.put( namespace, prefix);
    }
  }

  public void addNamespaces(List<String> namespaces) {
    if( omitSingleNamespacePrefix && namespaces.size() == 1 ) {
      namespacePrefixes.put( namespaces.get(0), null );
    } else {
      for( int i=0; i< namespaces.size(); ++i ) {
        String ns = namespaces.get(i);
        if( ns != null ) {
          namespacePrefixes.put( ns, "p"+(i+1) );
        }
      }
    } 
  }
 
  public Map<String,String> getNamespacePrefixes() {
    return namespacePrefixes;
  }

  public String getNamespacePrefix(String namespace) {
    return namespacePrefixes.get(namespace);
  }

  public boolean omitNullTags() {
    return omitNullTags;
  }
  
  public void setOmitNullTags(boolean omitNullTags) {
    this.omitNullTags = omitNullTags;
  }
  
  public void setOmitSingleNamespacePrefix(boolean omitSingleNamespacePrefix) {
    this.omitSingleNamespacePrefix = omitSingleNamespacePrefix;
  }
  
  public boolean booleanAsInteger() {
    return booleanAsInteger;
  }
 
  public void setBooleanAsInteger(boolean booleanAsInteger) {
    this.booleanAsInteger = booleanAsInteger;
  }
  

}
