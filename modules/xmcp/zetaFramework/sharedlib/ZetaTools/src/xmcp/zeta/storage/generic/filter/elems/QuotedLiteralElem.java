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

import xmcp.zeta.storage.generic.filter.shared.JsonWriter;
import xmcp.zeta.storage.generic.filter.shared.LiteralTools;


public class QuotedLiteralElem extends RelationalOperand {

  private final String _content;
  
  
  public QuotedLiteralElem(String input) {
    this._content = new LiteralTools().adaptLiteralString(input);
  }
  
  
  @Override
  public boolean isFinished() {
    return true;
  }
  
  @Override
  public void writeJson(JsonWriter json) {
    json.addAttribute("QuotedLiteral", _content);
  }
  
  @Override
  public boolean containsWildcards() {
    return false;
  }
  
  @Override
  public boolean isNumerical() {
    return false;
  }

  @Override
  public String getContentString() {
    return _content;
  }


  @Override
  public boolean indicateAddWildcardAddEnd() {
    return false;
  }


  @Override
  public boolean indicateAddWildcardAddStart() {
    return false;
  }

}
