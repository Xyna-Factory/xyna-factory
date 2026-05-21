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


public class RelOperandContainer extends RelationalOperandElem {

  private List<RelationalOperandElem> _children = new ArrayList<>();
  
  
  public RelOperandContainer(List<RelationalOperandElem> children) {
    this._children.addAll(children);
  }
  
  @Override
  public boolean isFinished() {
    for (RelationalOperandElem child : _children) {
      if (!child.isFinished()) { return false; }
    }
    return true;
  }
  
  
  @Override
  public boolean isNumerical() {
    return false;
  }
  

  @Override
  public boolean containsWildcards() {
    for (RelationalOperandElem child : _children) {
      if (child.containsWildcards()) { return true; }
    }
    return false;
  }

  @Override
  public String getContentString() {
    StringBuilder s = new StringBuilder();
    for (RelationalOperandElem child : _children) {
      s.append(child.getContentString());
    }
    return s.toString();
  }
  
  
  @Override
  public boolean indicateAddWildcardAddEnd() {
    if (_children.size() < 1) {
      throw new RuntimeException("Syntax error in filter input expression: Empty RelOperandContainer.");
    }
    if (containsWildcards()) { return false; }
    return _children.get(_children.size() - 1).indicateAddWildcardAddEnd();
  }


  @Override
  public boolean indicateAddWildcardAddStart() {
    if (_children.size() < 1) {
      throw new RuntimeException("Syntax error in filter input expression: Empty RelOperandContainer.");
    }
    if (containsWildcards()) { return false; }
    return _children.get(0).indicateAddWildcardAddStart();
  }
  
  
  @Override
  public String getContentAdaptedForSqlEquals() {
    StringBuilder s = new StringBuilder();
    for (RelationalOperandElem child : _children) {
      s.append(child.getContentAdaptedForSqlEquals());
    }
    return s.toString();
  }


  @Override
  public String getContentAdaptedForSqlLike() {
    StringBuilder s = new StringBuilder();
    for (RelationalOperandElem child : _children) {
      s.append(child.getContentAdaptedForSqlLike());
    }
    return s.toString();
  }
  
}
