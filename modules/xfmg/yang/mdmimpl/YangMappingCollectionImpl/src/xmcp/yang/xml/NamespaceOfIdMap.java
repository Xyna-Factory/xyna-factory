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

package xmcp.yang.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class NamespaceOfIdMap {

  private Map<Integer, String> _map = new HashMap<>();

  
  public Optional<String> getNamespace(int id) {
    String val = _map.get(id);
    return Optional.ofNullable(val);
  }
  
  
  public String getExpectedNamespace(String idStr) {
    int id = -1;
    try {
      id = Integer.parseInt(idStr);
    } catch (Exception e) {
      throw new RuntimeException("Error parsing namespace id number: " + idStr);
    }
    String val = _map.get(id);
    if (val == null) { throw new IllegalArgumentException("Could not find namespace for id: " + id); }
    return val;
  }
  
  
  public Optional<String> getNamespace(String prefix) {
    if (!prefix.startsWith(Constants.PREFIX_OF_PREFIX)) { 
      throw new IllegalArgumentException("Unexpected format in prefix: " + prefix); 
    }
    String digits = prefix.substring(1);
    int id = Integer.parseInt(digits);
    return getNamespace(id);
  }
  
  
  public void add(int id, String namespace) {
    _map.put(id, namespace);
  }
  
  
  public void initFromPrefixNamespacePairs(Collection<String> list) {
    if (list == null) { return; }
    for (String item : list) {
      if (!item.startsWith(Constants.PREFIX_OF_PREFIX)) { 
        throw new IllegalArgumentException("Could not parse prefix-namespace-pair string: " + item); 
      }
      int index = item.indexOf(Constants.SEP_PREFIX_NAMESPACE);
      if (index < 0) { throw new IllegalArgumentException("Could not parse prefix-namespace-pair string: " + item); }
      String digits = item.substring(1, index);
      int id = Integer.parseInt(digits);
      String nsp = item.substring(index + 1);
      add(id, nsp);
    }
  }
  
}
