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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class IdOfNamespaceMap {

  private int _nextId = 0;
  private Map<String, Integer> _map = new HashMap<>();
  
  
  public int getId(String namespace) {
    Integer id = _map.get(namespace);
    if (id == null) {
      id = getNextId();
      _map.put(namespace, id);
    } 
    return id;
  }
  
  protected int getNextId() {
    int ret = _nextId;
    _nextId = _nextId +1;
    return ret;
  }
  
  public List<String> toPrefixNamespacePairList() {
    List<String> ret = new ArrayList<>();
    for (Entry<String, Integer> entry : _map.entrySet()) {
      String str = idToPrefix(entry.getValue()) + Constants.SEP_PREFIX_NAMESPACE + entry.getKey();
      ret.add(str);
    }
    return ret;
  }
  
  public String idToPrefix(int id) {
    return Constants.PREFIX_OF_PREFIX + id;
  }
  
}
