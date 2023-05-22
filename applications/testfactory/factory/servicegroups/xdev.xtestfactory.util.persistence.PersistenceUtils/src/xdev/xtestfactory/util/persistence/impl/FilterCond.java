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
package xdev.xtestfactory.util.persistence.impl;

import java.util.Collections;
import java.util.List;

import com.gip.xyna.xnwh.persistence.xmom.IFormula;
import com.gip.xyna.xnwh.persistence.xmom.IFormula.Accessor;

import xnwh.persistence.FilterCondition;

class FilterCond implements IFormula {
  
  private final String filterString;
  
  FilterCond(FilterCondition filter) {
    if (filter == null) {
      filterString = null;
    } else {
      filterString = filter.getFormula();
    }
  }

  public List<Accessor> getValues() {
    return Collections.emptyList();
  }

  public String getFormula() {
    return filterString;
  }
  
}