/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import com.gip.xyna.utils.db.UnsupportingResultSetMetaData;


/**
 * Sehr einfache Implementierung der ResultSetMetaData, 
 * die nur Anzahl der Spalten und deren Namen kennt.
 */
public class SimpleResultSetMetaData extends UnsupportingResultSetMetaData {

  private ArrayList<String> columnNames;

  public SimpleResultSetMetaData(Collection<String> columnNames) {
    this.columnNames = new ArrayList<String>(columnNames);
  }
  
  public SimpleResultSetMetaData(Storable<?> storable) {
    Column[] columns = storable.getColumns();
    this.columnNames = new ArrayList<String>(columns.length);
    for( Column col : columns ) {
      columnNames.add( col.name() );
    }
  }

  @Override
  public int getColumnCount() throws SQLException {
    return columnNames.size();
  }
  
  @Override
  public String getColumnName(int column) throws SQLException {
    return columnNames.get(column-1);
  }
  
}
