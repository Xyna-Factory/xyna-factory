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
import java.util.Optional;

import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public class ContainerElem implements LogicalOperand {

  private List<FilterElement> _children = new ArrayList<>();
  
  
  public ContainerElem(List<FilterElement> children) {
    this._children.addAll(children);
  }
  
  
  @Override
  public boolean isFinished() {
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
  public void writeSql(String colname, SqlWhereClauseData sql) {
    throw new RuntimeException("SQL output not supported for class " + this.getClass().getName());
  }
  
  
  @Override
  public Optional<FilterElement> getChild(int index) {
    if (index < _children.size()) {
      return Optional.ofNullable(_children.get(index));
    }
    return Optional.empty();
  }
  
  
  @Override
  public String getInfoString() {
    return "CONTAINER";
  }

}
