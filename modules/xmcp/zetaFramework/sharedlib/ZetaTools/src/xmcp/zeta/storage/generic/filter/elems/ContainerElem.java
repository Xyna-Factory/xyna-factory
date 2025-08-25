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

package xmcp.zeta.storage.generic.filter.elems;

import java.util.ArrayList;
import java.util.List;

import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;


public class ContainerElem implements LogicalOperand {

  private List<FilterElement> children = new ArrayList<>();
  
  
  public ContainerElem(List<FilterElement> children) {
    this.children.addAll(children);
  }
  
  
  public boolean isFinished() {
    for (FilterElement child : children) {
      if (!child.isFinished()) { return false; }
    }
    return true;
  }
  
  
  private void rebuild(List<FilterElement> newChildren) {
    this.children = newChildren;
  }
  
  
  public void parse(FilterInputParser parser) {
    List<FilterElement> adapted = parser.parseOperators(this.children);
    rebuild(adapted);
    for (FilterElement child : children) {
      if (!child.isFinished()) {
        child.parse(parser);
      }
      if (!child.isFinished()) {
        throw new IllegalArgumentException("Error parsing of child element of filter input expression failed, " +
                                           "state still unfinished.");
      }
    }
  }
  
   
  
  // contains, set of enum elem-type?
  
  // containedtokens, nur lexed tokens
  
  // contains lexed-token
  
  // replace (first pos replaced, last pos replaced, new elem)  
  // -> rebuild()
  
  // rebuild (input neue child-liste)
  // -> neu init contains
  
  // is finished: nur prüfen ob contained-tokens leer? N, alle childs isfinised
}
