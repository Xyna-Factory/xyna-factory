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


public class FilterColumnConfig {

  public static class Builder {
    private String _sqlColumnName;
    private String _xmomPath;
    
    public Builder sqlColumnName(String val) {
      this._sqlColumnName = val;
      return this;
    }
    public Builder xmomPath(String val) {
      this._xmomPath = val;
      return this;
    }
    public String getSqlColumnName() {
      return _sqlColumnName;
    }
    public String getXmomPath() {
      return _xmomPath;
    }
    public FilterColumnConfig build() {
      if (_sqlColumnName == null) {
        throw new RuntimeException("Sql column name missing");
      }
      if (_xmomPath == null) {
        throw new RuntimeException("Xmom path missing");
      }
      return FilterColumnConfig.newInstance(this);
    }
  }
  
  
  private final String sqlColumnName;
  private final String xmomPath;
  
  
  private FilterColumnConfig(String sqlColumnName, String xmomPath) {
    this.sqlColumnName = sqlColumnName;
    this.xmomPath = xmomPath;
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  public static FilterColumnConfig newInstance(Builder builder) {
    return new FilterColumnConfig(builder.getSqlColumnName(), builder.getXmomPath());
  }

  
  public String getSqlColumnName() {
    return sqlColumnName;
  }

  
  public String getXmomPath() {
    return xmomPath;
  }
  
}
