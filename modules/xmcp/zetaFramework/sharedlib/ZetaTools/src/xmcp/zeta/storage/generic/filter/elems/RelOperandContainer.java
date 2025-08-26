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


public class RelOperandContainer extends RelationalOperand {

  private List<RelationalOperand> _children = new ArrayList<>();
  
  
  public RelOperandContainer(List<RelationalOperand> children) {
    this._children.addAll(children);
  }
  
  @Override
  public boolean isFinished() {
    for (RelationalOperand child : _children) {
      if (!child.isFinished()) { return false; }
    }
    return true;
  }
  
  
  @Override
  public boolean isNumerical() {
    return false;
  }
  

  @Override
  public void writeJson(JsonWriter json) {
    json.openObjectAttribute("RelOperandContainer");
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


  @Override
  public boolean containsWildcards() {
    for (RelationalOperand child : _children) {
      if (child.containsWildcards()) { return true; }
    }
    return false;
  }

  @Override
  public String getContentString() {
    StringBuilder s = new StringBuilder();
    for (RelationalOperand child : _children) {
      s.append(child.getContentString());
    }
    return s.toString();
  }
  
}
