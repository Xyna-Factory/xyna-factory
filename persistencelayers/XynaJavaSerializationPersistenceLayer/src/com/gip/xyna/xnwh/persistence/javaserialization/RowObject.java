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

package com.gip.xyna.xnwh.persistence.javaserialization;

import java.io.Serializable;
import java.util.ArrayList;


public class RowObject extends ArrayList<ColumnObject> implements Serializable {
  
  private static final long serialVersionUID = 6959025076230525002L;
  
  
  private Object primaryKey;

  
  public RowObject() {
    super();
  }

  public RowObject(Object pk) {
    super();
    this.primaryKey = pk;
  }
  
  
  public Object getPrimaryKey() {
    return primaryKey;
  }

  
  public void setPrimaryKey(Object pk) {
    this.primaryKey = pk;
  }
  
  @Override
  public boolean equals(Object elem) {    
    return primaryKey.hashCode() == elem.hashCode();
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("PrimaryKey: ");
    sb.append(primaryKey.toString());
    sb.append(" Rows: ");
    sb.append(size());
    sb.append("\n");
    for (ColumnObject column : this) {
      sb.append(column.toString());
      sb.append("\n");
    }    
    return sb.toString();
  }
  
}
