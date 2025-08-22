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

package xmcp.zeta.storage.generic.filter.shared;

import java.util.ArrayList;
import java.util.List;


public class Replacer<T> {

  public List<T> replaceInList(List<T> list, int fromIndex, int toIndexInclusive, T replaceWith) {
    if (fromIndex >= list.size()) { return new ArrayList<T>(list); }
    if (toIndexInclusive >= list.size()) { return new ArrayList<T>(list); }
    List<T> ret = new ArrayList<T>();
    List<T> before = list.subList(0, fromIndex);
    List<T> after = list.subList(toIndexInclusive + 1, list.size());
    ret.addAll(before);
    ret.add(replaceWith);
    ret.addAll(after);
    return ret;
  }
  
}
