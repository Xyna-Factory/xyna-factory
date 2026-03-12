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
import xmcp.zeta.storage.generic.filter.elems.TokenOpElem;
import xmcp.zeta.storage.generic.filter.elems.WildcardElem;
import xmcp.zeta.storage.generic.filter.shared.Enums;


public class WildcardHandler {

  public List<FilterElement> execute(List<FilterElement> input) {
    List<FilterElement> ret = new ArrayList<>();
    for (FilterElement elem : input) {
      boolean replace = false;
      if (elem instanceof TokenOpElem) {
        TokenOpElem toe = (TokenOpElem) elem;
        if (toe.getCategory() == Enums.LexedOperatorCategory.WILDCARD) {
          replace = true;
        }
      }
      if (replace) {
        ret.add(new WildcardElem());
      } else {
        ret.add(elem);
      }
    }
    return ret;
  }

}
