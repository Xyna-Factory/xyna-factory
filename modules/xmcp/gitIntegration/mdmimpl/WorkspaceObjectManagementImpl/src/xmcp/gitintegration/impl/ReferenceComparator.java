/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.Reference;
import xmcp.gitintegration.WorkspaceContentDifferenceType;

public class ReferenceComparator {
  public List<ItemDifference<Reference>> compare(Collection<? extends Reference> from, Collection<? extends Reference> to) {
    List<ItemDifference<Reference>> result = new ArrayList<ItemDifference<Reference>>();
    from = from == null ? new ArrayList<Reference>() : from;
    List<Reference> toList = to == null ? new ArrayList<Reference>() : new ArrayList<Reference>(to);
    Map<String, Reference> toMap = toList.stream().collect(Collectors.toMap(ReferenceComparator::keyMap, Function.identity()));
    String key;

    // iterate over from-list
    // create MODIFY and DELETE entries
    for (Reference fromEntry : from) {
      key = keyMap(fromEntry);
      Reference toEntry = toMap.get(key);
      toList.remove(toEntry);
      if (!compareReferenceTags(fromEntry, toEntry)) {
        Class<? extends WorkspaceContentDifferenceType> type = toEntry == null ? DELETE.class : MODIFY.class;
        result.add(new ItemDifference<Reference>(type, fromEntry, toEntry));
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for (Reference tag : toList) {
      result.add(new ItemDifference<Reference>(CREATE.class, null, tag));
    }

    return result;
  }
  
  private static String keyMap(Reference t) {
    return t.getPath();
  }


  //to may be null
  private boolean compareReferenceTags(Reference from, Reference to) {
    return to != null && Objects.equals(from.getPath(), to.getPath()) && Objects.equals(from.getType(), to.getType());
  }
}
