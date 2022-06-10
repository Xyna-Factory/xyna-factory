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
 * data about relationships between database tables:
 * for example a column with an ID value which has a foreign key constraint
 * to another table where the actual value that matches that ID can be looked up
 */
public class ForeignValueData {
  
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
  
  public ForeignValueData(String table, String conditionCol, String conditionVal, String valueCol,
        String schema, LookupStyle lookupStyle) {
    _table = table;
    _conditionCol = conditionCol;
    _conditionVal = conditionVal;
    _valueCol = valueCol;
    _schema = schema;
    _lookupStyle = lookupStyle;
  }  

  public void setConditionVal(String value) {
    _conditionVal = value;
  }
}
