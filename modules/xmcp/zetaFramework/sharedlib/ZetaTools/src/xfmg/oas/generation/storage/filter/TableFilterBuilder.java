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

package xfmg.oas.generation.storage.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;



public class TableFilterBuilder {

  private final Map<String, FilterColumnConfig> _map = new HashMap<>();
  
  
  public TableFilterBuilder(List<FilterColumnConfig> list) {
    if (list == null) { return; }
    for (FilterColumnConfig conf : list) {
      _map.put(conf.getXmomPath(), conf);
    }
  }
  
  
  public TableFilter build(List<FilterColumnInput> input) {
    List<FilterColumn> ret = new ArrayList<>();
    if (input == null) { return buildImpl(ret); }
    for (FilterColumnInput col : input) {
      Optional<String> path = getTrimmedOrEmpty(col.getPath());
      if (path.isEmpty()) { continue; }
      Optional<String> filter = getTrimmedOrEmpty(col.getFilter());
      if (filter.isEmpty()) { continue; }
      FilterColumnConfig conf = _map.get(path.get());
      if (conf == null) { continue; }
      ret.add(new FilterColumn(conf, filter.get()));
    }
    return buildImpl(ret);
  }
  
  
  private TableFilter buildImpl(List<FilterColumn> list) {
    return new TableFilter(list);
  }
  
  
  private Optional<String> getTrimmedOrEmpty(String val) {
    if (val == null) { return Optional.empty(); }
    val = val.trim();
    if (val.isEmpty()) { return Optional.empty(); }
    return Optional.of(val);
  }

}
