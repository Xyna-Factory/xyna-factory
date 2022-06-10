/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.juno.ws.tools;

import com.gip.juno.ws.enums.LookupStyle;

/**
 * Builder class for class ForeignValueData
 */
public class ForeignValueDataBuilder {

  /**
   * name of table in which to lookup value
   */
  private String _table = "";
  
  /**
   * name of column in foreign table in which the same value as in this column should be found
   * (or has to be found, with a foreign key constraint)
   */
  private String _conditionCol = "";
  
  /**
   * the actual value that appears in both tables
   */
  private String _conditionVal = "";
  
  /**
   * column name in the foreign table where the value we want to get is to be found
   */
  private String _valueCol = "";
  
  /**
   * database schema
   */
  private String _schema = "";
  
  private LookupStyle _lookupStyle = LookupStyle.singleval;
  
  public String getTable() { return _table; }
  public String getConditionCol() { return _conditionCol; }
  public String getConditionVal() { return _conditionVal; }
  public String getValueCol() { return _valueCol; }
  public String getSchema() { return _schema; }
  public LookupStyle getLookupStyle() { return _lookupStyle; }
  
  public ForeignValueDataBuilder setConditionCol(String value) {
    _conditionCol = value;
    return this;
  }
  
  public ForeignValueDataBuilder setTable(String value) {
    _table = value;
    return this;
  }
  
  public ForeignValueDataBuilder setConditionVal(String value) {
    _conditionVal = value;
    return this;
  }
  
  public ForeignValueDataBuilder setValueCol(String value) {
    _valueCol = value;
    return this;
  }
  
  public ForeignValueDataBuilder setSchema(String value) {
    _schema = value;
    return this;
  }
  
  public ForeignValueDataBuilder setLookupStyle(LookupStyle val) {
    _lookupStyle = val;
    return this;
  }
  
  public ForeignValueData build() {
    if (_table.equals("")) {
      return null;
    }
    if (_conditionCol.equals("")) {
      return null;
    }
    if (_conditionVal.equals("")) {
      return null;
    }
    if (_valueCol.equals("")) {
      return null;
    }
    if (_schema.equals("")) {
      return null;
    }
    return new ForeignValueData(_table, _conditionCol, _conditionVal, _valueCol, _schema, _lookupStyle);
  }
}

