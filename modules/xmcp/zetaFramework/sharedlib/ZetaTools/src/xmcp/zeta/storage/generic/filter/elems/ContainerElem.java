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
import xmcp.zeta.storage.generic.filter.shared.JsonWriter;


public class ContainerElem implements LogicalOperand {

  private List<FilterElement> _children = new ArrayList<>();
  
  
  public ContainerElem(List<FilterElement> children) {
    this._children.addAll(children);
  }
  
  
  @Override
  public boolean isFinished() {
    /*
    for (FilterElement child : _children) {
      if (!child.isFinished()) { return false; }
    }
    return true;
    */
    return (_children.size() == 1) && (_children.get(0).isFinished());
  }
  
  
  private void rebuild(List<FilterElement> newChildren) {
    _children = newChildren;
  }
  
  
  @Override
  public void parse(FilterInputParser parser) {
    for (FilterElement child : _children) {
      if (!child.isFinished()) {
        child.parse(parser);
      }
    }
    List<FilterElement> adapted = parser.parseOperators(this._children);
    rebuild(adapted);
    for (FilterElement child : _children) {
      if (!child.isFinished()) {
        child.parse(parser);
      }
    }
    if (!isFinished()) {
      throw new IllegalArgumentException("Error parsing of child element of filter input expression failed, " +
                                         "state still unfinished.");
    }
  }
  
  
  @Override
  public void writeJson(JsonWriter json) {
    json.openObjectAttribute("Container");
    json.openListAttribute("children");
    boolean isfirst = true;
    for (FilterElement child : _children) {
      if (isfirst) { isfirst = false; }
      else { json.continueList(); }
      child.writeJson(json);
    }
    json.closeList();
    json.closeObject();
  }
  
  
  public FilterElement verifyAndExtractSingleChild() {
    if (_children.size() != 1) {
      throw new RuntimeException("Error parsing filter input expression: Container left with unexpected number of child elements.");
    }
    FilterElement child = _children.get(0);
    if (child instanceof ContainerElem) {
      return ((ContainerElem) child).verifyAndExtractSingleChild();
    }
    return child;
  }
  
  
  @Override
  public void writeSql(String colname, StringBuilder str) {
    throw new RuntimeException("SQL output not supported for class " + this.getClass().getName());
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
