/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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


package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.BijectiveMap;


public class GenerationBaseMdmMapping {

  private final BijectiveMap<String, Class<?>> mdmObjectMappingToJavaClasses = new BijectiveMap<String, Class<?>>();
  private final Map<String, String> xmlToServerClassMap = new HashMap<>();
  
  public void put(String key, Class<?> value) {
    mdmObjectMappingToJavaClasses.put(key, value);
    xmlToServerClassMap.put(value.getName(), key);
  }
 
  public boolean containsKey(String key) {
    return mdmObjectMappingToJavaClasses.containsKey(key);
  }
  
  public String getInverse(Class<?> c) {
    return mdmObjectMappingToJavaClasses.getInverse(c);
  }
  
  public Class<?> get(String key) {
    return mdmObjectMappingToJavaClasses.get(key);
  }
  
  public String getInverseByServerClassName(String serverClassName) {
    return xmlToServerClassMap.get(serverClassName);
  }
  
  public Set<String> keySet() {
    return mdmObjectMappingToJavaClasses.keySet();
  }
  
}
