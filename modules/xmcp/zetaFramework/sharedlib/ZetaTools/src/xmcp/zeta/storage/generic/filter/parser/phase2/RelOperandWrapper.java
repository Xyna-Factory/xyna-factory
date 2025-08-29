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

import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.elems.RelOperandContainer;
import xmcp.zeta.storage.generic.filter.elems.RelationalOperand;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class RelOperandWrapper {

  public List<FilterElement> execute(List<FilterElement> list) {
    List<FilterElement> elems = list;
    Replacer<FilterElement> replacer = new Replacer<FilterElement>();
    int pos = 0;
    while (true) {
      int from = getIndexNextMatchStart(elems, pos);
      if (from < 0) { break; }
      pos = from + 1;
      int to = getIndexMatchEnd(elems, from);
      if (to <= from) { continue; }
      RelOperandContainer wrapped = wrapElements(from, to + 1, elems);
      elems = replacer.replaceInList(elems, from, to + 1, wrapped);
    }
    return elems;
  }
  
  
  private RelOperandContainer wrapElements(int fromInclusive, int toExclusive, List<FilterElement> list) {
    List<RelationalOperand> ret = new ArrayList<>();
    List<FilterElement> toWrap = list.subList(fromInclusive, toExclusive);
    for (FilterElement elem : toWrap) {
      ret.add((RelationalOperand) elem);
    }
    return new RelOperandContainer(ret);
  }
  
  
  private int getIndexNextMatchStart(List<FilterElement> list, int from) {
    for (int i = from; i < list.size() - 1; i++) {
      FilterElement elem = list.get(i);
      if (elem instanceof RelationalOperand) {
        return i;
      }
    }
    return -1;
  }
  
  
  private int getIndexMatchEnd(List<FilterElement> list, int from) {
    int matchEnd = from;
    for (int i = from + 1; i < list.size(); i++) {
      FilterElement elem = list.get(i);
      if (elem instanceof RelationalOperand) {
        matchEnd = i;
      } else {
        return matchEnd;
      }
    }
    return matchEnd;
  }
  
}
