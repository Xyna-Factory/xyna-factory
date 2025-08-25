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

package xmcp.zeta.storage.generic.filter.parser.phase2;

import java.util.ArrayList;
import java.util.List;

import xmcp.zeta.storage.generic.filter.elems.ContainerElem;
import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.elems.TokenOpElem;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class ParenthesesHandler {

  public List<FilterElement> execute(List<FilterElement> input) {
    List<FilterElement> list = input;
    Replacer<FilterElement> replacer = new Replacer<FilterElement>();
    List<Integer> matched = getIndicesForOpen(list);
    for (int i = matched.size() - 1; i >= 0; i--) {
      int fromIndex = matched.get(i);
      int toIndex = getIndexForMatchedClose(list, fromIndex + 1);
      ContainerElem newElem = new ContainerElem(list.subList(fromIndex + 1, toIndex));
      list = replacer.replaceInList(list, fromIndex, toIndex + 1, newElem);
    }
    return list;
  }
  
  
  private List<Integer> getIndicesForOpen(List<FilterElement> list) {
    List<Integer> ret = new ArrayList<>();
    for (int i = 0; i < list.size(); i++) {
      FilterElement elem = list.get(i);
      if (elem instanceof TokenOpElem) {
        TokenOpElem toe = (TokenOpElem) elem;
        if (toe.getCategory() == Enums.LexedOperatorCategory.OPEN) {
          ret.add(i);
        }
      }
    }
    return ret;
  }
  
  
  private int getIndexForMatchedClose(List<FilterElement> list, int from) {
    for (int i = from; i < list.size(); i++) {
      FilterElement elem = list.get(i);
      if (elem instanceof TokenOpElem) {
        TokenOpElem toe = (TokenOpElem) elem;
        if (toe.getCategory() == Enums.LexedOperatorCategory.CLOSE) {
          return i;
        }
      }
    }
    throw new IllegalArgumentException("Syntax error in filter expression: Parentheses not closed.");
  }
  
}
