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
package xmcp.gitintegration.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemComparator<T> {

  private final Function<T, String> getUniqueIdFunction;
  private final List<BiFunction<T, T, Boolean>> componentEqualsFunctions;
  
  public ItemComparator(Function<T, String> getUniqueIdFunction, List<BiFunction<T, T, Boolean>> componentEqualsFunctions) {
    this.getUniqueIdFunction = getUniqueIdFunction;
    this.componentEqualsFunctions = componentEqualsFunctions;
  }

  public List<ItemDifference<T>> compare(Collection<? extends T> from, Collection<? extends T> to) {
    List<ItemDifference<T>> result = new ArrayList<>();
    from = from == null ? new ArrayList<T>() : new ArrayList<>(from);
    List<T> toList = to == null ? new ArrayList<T>() : new ArrayList<>(to);
    Map<String, T> toMap = toList.stream().collect(Collectors.toMap(getUniqueIdFunction, Function.identity()));
    String key;

    // iterate over from-list
    // create MODIFY and DELETE entries
    for(T fromEntry : from) {
      key = getUniqueIdFunction.apply(fromEntry);
      T toEntry = toMap.get(key);
      toList.remove(toEntry);
      if(toEntry == null || !checkComponentsAreEqual(fromEntry, toEntry)) {
        XynaContentDifferenceType type = toEntry == null ? XynaContentDifferenceType.DELETE : XynaContentDifferenceType.MODIFY;
        result.add(new ItemDifference<T>(type, fromEntry, toEntry));
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for(T toEntry : toList) {
      result.add(new ItemDifference<T>(XynaContentDifferenceType.CREATE, null, toEntry));
    }

    return result;
  }

  private boolean checkComponentsAreEqual(T fromEntry, T  toEntry) {
    for(BiFunction<T, T, Boolean> componentEqualsFunction : componentEqualsFunctions) {
      if(!componentEqualsFunction.apply(fromEntry, toEntry)) {
        return false;
      }
    }
    return true;
  }
}
