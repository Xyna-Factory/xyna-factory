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

package xfmg.oas.generation.storage.tools;


public class FilterColumn {

  private final FilterColumnConfig config;
  private final String value;
  
  
  public FilterColumn(FilterColumnConfig config, String value) {
    if (config == null) { throw new IllegalArgumentException("FilterColumnConfig is null"); }
    if (value == null) { throw new IllegalArgumentException("Filter column value is null"); }
    this.config = config;
    this.value = adaptWildcards(value);
  }
  
  
  private String adaptWildcards(String val) {
    String ret = val.trim();
    ret = ret.replace("*", "%");
    if (!ret.startsWith("%")) {
      ret = "%" + ret;
    }
    if (!ret.endsWith("%")) {
      ret = ret + "%";
    }
    return ret;
  }

  
  public FilterColumnConfig getConfig() {
    return config;
  }

  
  public String getValue() {
    return value;
  }
  
  
  public String getSqlColumnName() {
    return config.getSqlColumnName();
  }
  
}
