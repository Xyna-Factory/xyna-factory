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
package com.gip.xyna.xnwh.persistence.xmom;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.db.UnsupportingResultSet;
import com.gip.xyna.xnwh.persistence.ResultSetReader;

/**
 * In cases of a necessary Query-Split (inclusions of lists in hierarchy) there do exist 2 representations of the hierachy in a QueryTreeNode-Format
 * Pre-Flattening: 
 *   Is a direct representation of the StorableStructure, every storable is represented as a node with it's non-list children as contained as joinChildren
 *   and it's list children contained as unionChildren.  
 * Post-Flattening:
 *   Flattening does recursively collect all joinChildren and stores them in the current parent, the collects all unionChildren of all collected and previous
 *   present joinChildren and stores them in the parents. All collected joins will have all their children removed.
 * StorableNodeReader-Layout is based on the Pre-Flattening Tree as the parent child relationships have to be retained while the query is build upon the
 * PostFlattening representation.
 * 
 * Example:
 * 0 - 1L - 2 - 3L
 *        - 4L
 *        
 * Pre-Tree:
 * 0 j: []
 *   u: [1] - 1 j: [2] - 2 j: []
 *                         u: [3]
 *              u: [4]
 * 
 * Post-Tree:
 * 0 j: []
 *   u: [1] - 1 j: [2] - 2 j: []
 *                         u: []
 *              u: [3, 4]
 * 
 * The ResultSet from a query based on the Post-Tree will return entries in the following order:
 * 3
 * 4
 * 2
 * 1
 * In a naive traversal of the Pre-Tree we would read 3 data first and would then on our way back to 2 try to read it's data as well,
 * which would not contain any of it's values as the 3 readout would have positioned us on a 4-Row. Traversal is therefore structured to read every 
 * sublist of a node first before reading non-list. 
 */
public class StorableNodeReader implements ResultSetReader {

  
  public StorableNodeReader(String tablename, Map<String, String> columnNameAliases, List<ChildReaderData> readers, ResultSetReader localReader) {
    this.columnNameAliases = columnNameAliases;
    this.readers = readers;
    this.localReader = localReader;
    this.tablename = tablename;
  }
  
  public StorableNodeReader(String tablename, Map<String, String> columnNameAliases, List<ChildReaderData> readers, ResultSetReader localReader, String rootIdentifier, String localIndexIdentifier) {
    this(tablename, columnNameAliases, readers, localReader);
    isList = true;
    this.rootIdentifier = rootIdentifier;
    this.localIndexIdentifier = localIndexIdentifier;
  }
  
  private Map<String, String> columnNameAliases;
  private List<ChildReaderData> readers;
  private ResultSetReader localReader;
  private String tablename;
  private boolean isList = false;
  private String rootIdentifier; // only needed if list
  private String localIndexIdentifier;  // only needed if list
  
  Map<String, Object> additionalResultSetData = new HashMap<String, Object>();
  
  
  enum ReadMode {
    readList, gotoList, readNonList;
  }
  
  
  public Object read(ResultSet rs) throws SQLException {
    return read(rs, ReadMode.readList);
  }
  
  public Object read(ResultSet rs, ReadMode mode) throws SQLException {
    if (mode != ReadMode.readNonList) {
      for (ChildReaderData child : readers) {
        if (child.reader.isList) {
          Object o = child.reader.readList(rs);
          additionalResultSetData.put(QueryGenerator.generateColumnAliasIdentifier(child.resultColumnTableName, child.resultColumnName), o);
        } else {
          child.reader.read(rs, ReadMode.gotoList);
        }
      }
    }
    if (mode != ReadMode.gotoList) {
      for (ChildReaderData child : readers) {
        if (!child.reader.isList) {
          String conditionalColumnName = QueryGenerator.generateColumnAliasIdentifier(child.readConditionColumnTableName, child.readConditionColumnName);
          WrappingResultSet childWrs = new WrappingResultSet(child.readConditionColumnTableName, rs, columnNameAliases, additionalResultSetData);
          if (columnNameAliases.containsKey(conditionalColumnName) &&
                          childWrs.getObject(child.readConditionColumnName) != null &&
                          !childWrs.wasNull()) {
            Object o = child.reader.read(childWrs, ReadMode.readNonList);
            additionalResultSetData.put(QueryGenerator.generateColumnAliasIdentifier(child.resultColumnTableName, child.resultColumnName), o);
          }
        }
      }
      WrappingResultSet wrs = new WrappingResultSet(tablename, rs, columnNameAliases, additionalResultSetData);
      Object result = localReader.read(wrs);
      additionalResultSetData.clear();
      return result;
    } else {
      return null;
    }
  }
  
  
  public List<Object> readList(ResultSet rs) throws SQLException {
    WrappingResultSet wrs = new WrappingResultSet(tablename, rs, columnNameAliases, additionalResultSetData);
    String currentRootIdentifier = wrs.getString(rootIdentifier);
    if (currentRootIdentifier == null) {
      return new ArrayList<Object>();
    }
    Set<Integer> visitedIndices = new HashSet<Integer>();
    List<Object> result = new ArrayList<Object>();
    while (!wrs.isAfterLast() && currentRootIdentifier.equals(wrs.getString(rootIdentifier))) {
      int currentIndex = wrs.getInt(localIndexIdentifier);
      if (wrs.wasNull() || currentIndex < 0) {
        break;
      }
      if (!visitedIndices.contains(currentIndex)) {
        visitedIndices.add(currentIndex);
        addToSparseList(result, currentIndex, read(wrs, ReadMode.readList));
      }
      wrs.next();
    }
    return result;
  }
  
  
  
  static <O> void addToSparseList(List<O> result, int index, O element) {
    while (index >= result.size()) {
      result.add(null);
    }
    result.set(index, element);
  }
  
  
  static class ChildReaderData {
    
    String readConditionColumnName;
    String readConditionColumnTableName;
    String resultColumnName;
    String resultColumnTableName;
    StorableNodeReader reader;
    int sublists;
    
    ChildReaderData(String readConditionColumnName, String readConditionColumnTableName,
                    String resultColumnName, String resultColumnTableName,
                    StorableNodeReader reader, int sublists) {
      this.readConditionColumnName = readConditionColumnName;
      this.readConditionColumnTableName = readConditionColumnTableName;
      this.resultColumnName = resultColumnName;
      this.resultColumnTableName = resultColumnTableName;
      this.reader = reader;
      this.sublists = sublists;
    }
    
  }
  
  
  private static class WrappingResultSet extends UnsupportingResultSet {
    
    private final Map<String, String> selectedColumns;
    private final Map<String, Object> additionalObjects;
    private final String tablename;
    private final ResultSet rs;
    private boolean canAnswerWasNullWithTrue = false;
    
    private WrappingResultSet(String tablename, ResultSet rs, Map<String, String> selectedColumns, Map<String, Object> additionalObjects) {
      if (rs instanceof WrappingResultSet) {
        this.rs = ((WrappingResultSet)rs).rs;
      } else {
        this.rs = rs;
      }
      this.tablename = tablename;
      this.selectedColumns = selectedColumns;
      this.additionalObjects = additionalObjects;
    }
    
    
    
    @Override
    public String getString(String columnName) throws SQLException {
      String aliasIdentifier = QueryGenerator.generateColumnAliasIdentifier(tablename, columnName);
      if (selectedColumns.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = false;
        return rs.getString(selectedColumns.get(aliasIdentifier));
      } else {
        canAnswerWasNullWithTrue = true;
        return null;
      }
    }
    
    @Override
    public long getLong(String columnName) throws SQLException {
      String aliasIdentifier = QueryGenerator.generateColumnAliasIdentifier(tablename, columnName);
      if (selectedColumns.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = false;
        return rs.getLong(selectedColumns.get(aliasIdentifier));
      } else {
        canAnswerWasNullWithTrue = true;
        return 0;
      }
    }
    
    @Override
    public int getInt(String columnName) throws SQLException {
      String aliasIdentifier = QueryGenerator.generateColumnAliasIdentifier(tablename, columnName);
      if (selectedColumns.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = false;
        return rs.getInt(selectedColumns.get(aliasIdentifier));
      } else {
        canAnswerWasNullWithTrue = true;
        return 0;
      }
    }
    
    @Override
    public Object getObject(String columnName) throws SQLException {
      String aliasIdentifier = QueryGenerator.generateColumnAliasIdentifier(tablename, columnName);
      if (additionalObjects.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = true;
        return additionalObjects.get(aliasIdentifier);
      } else if (selectedColumns.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = false;
        return rs.getObject(selectedColumns.get(aliasIdentifier));
      } else {
        canAnswerWasNullWithTrue = true;
        return null;
      }
    }
    
    @Override
    public Blob getBlob(String columnName) throws SQLException {
      String aliasIdentifier = QueryGenerator.generateColumnAliasIdentifier(tablename, columnName);
      if (selectedColumns.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = false;
        return rs.getBlob(selectedColumns.get(aliasIdentifier));
      } else {
        canAnswerWasNullWithTrue = true;
        return null;
      }
    }
    
    @Override
    public boolean getBoolean(String columnName) throws SQLException {
      String aliasIdentifier = QueryGenerator.generateColumnAliasIdentifier(tablename, columnName);
      if (selectedColumns.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = false;
        return rs.getBoolean(selectedColumns.get(aliasIdentifier));
      } else {
        canAnswerWasNullWithTrue = true;
        return false;
      }
    }
    
    
    @Override
    public double getDouble(String columnName) throws SQLException {
      String aliasIdentifier = QueryGenerator.generateColumnAliasIdentifier(tablename, columnName);
      if (selectedColumns.containsKey(aliasIdentifier)) {
        canAnswerWasNullWithTrue = false;
        return rs.getDouble(selectedColumns.get(aliasIdentifier));
      } else {
        canAnswerWasNullWithTrue = true;
        return 0.0;
      }
    }
    
    @Override
    public boolean wasNull() throws SQLException {
      if (canAnswerWasNullWithTrue) {
        return true;
      } else {
        return rs.wasNull();
      }
    }
    
    @Override
    public boolean next() throws SQLException {
      return rs.next();
    }
    
    
    @Override
    public boolean isAfterLast() throws SQLException {
      return rs.isAfterLast();
    }
    
  }
  
}
