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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.QualifiedStorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarDefinitionSite;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;



public class UpdateGenerator {
  
  
  public static List<UpdateGeneration> parse(XMOMStorableStructureInformation info, List<String> updates) {
    List<UpdateGeneration> updateStatements = new ArrayList<UpdateGeneration>(); 
    for (String updatePath : updates) {
      PersistenceExpressionVisitors.UpdateParsingResult upr = PersistenceExpressionVisitors.parseUpdatePath(updatePath, info);
      UnfinishedUpdateStatement updateStatement = buildUpdate(upr.getPrimaryKeyListSuffix(), upr.getColumn(), upr.needsLike());
      UpdateGeneration update = new UpdateGeneration(updateStatement, upr.getListIndizesForRootObject());
      updateStatements.add(update);
    }
    return updateStatements;
  }
  
  
  protected static UnfinishedUpdateStatement buildUpdate(String primaryKeySuffix, QualifiedStorableColumnInformation column, boolean needsLike) {
    StorableColumnInformation relevantDBStorableColumn = column.getColumn();
    if (relevantDBStorableColumn.getDefinitionSite() == VarDefinitionSite.DATATYPE &&
        relevantDBStorableColumn.isStorableVariable()) {
      relevantDBStorableColumn = relevantDBStorableColumn.getCorrespondingReferenceIdColumn();
    }
    if (relevantDBStorableColumn.isList() && relevantDBStorableColumn.getPrimitiveType() != null) {
      relevantDBStorableColumn = column.getColumn().getStorableVariableInformation().getColumnInfo(column.getColumn().getVariableName());
    } 
    return new UnfinishedUpdateStatement(needsLike, primaryKeySuffix, relevantDBStorableColumn, column);
  }
  
  
  public static Collection<UpdateGeneration> pruneDuplicatedUpdates(Collection<UpdateGeneration> updates) {
    return new HashSet<UpdateGeneration>(updates);
  }
  
  
  protected static class UpdateGeneration implements Comparable<UpdateGeneration> {
    
    private final UnfinishedUpdateStatement updateStatement;
    private final List<Integer> listIdxs; //not null
    
    public UpdateGeneration(UnfinishedUpdateStatement updateStatement, List<Integer> listIdxs) {
      this.updateStatement = updateStatement;
      this.listIdxs = listIdxs;
    }

    
    public List<Integer> getListIndizesForRootObject() {
      return listIdxs;
    }

    
    public UnfinishedUpdateStatement getUnfinishedUpdateStatement() {
      return updateStatement;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof UpdateGeneration)) {
        return false;
      }
      UpdateGeneration other = (UpdateGeneration) obj;
      
      if (listIdxs.size() == other.listIdxs.size()) {
        for (int i = 0; i < listIdxs.size(); i++) {
          if (!listIdxs.get(i).equals(other.listIdxs.get(i))) {
            return false;
          }
        }
        return updateStatement.equals(other.updateStatement);
      } else {
        return false;
      }
    }
    
    
    @Override
    public int hashCode() {
      int hash = 0;
      for (QualifiedStorableColumnInformation column : updateStatement.getQualifiedColumn()) {
        hash += column.hashCode();
      }
      return hash;
    }


    @Override
    public int compareTo(UpdateGeneration o) {
      return compare(this, o);
    }
    

    private static int compare(UpdateGeneration u1, UpdateGeneration u2) {
      if (u1.listIdxs.size() != u2.listIdxs.size()) {
        return u1.listIdxs.size() - u2.listIdxs.size();
      }

      for (int i = 0; i < u1.listIdxs.size(); i++) {
        if (!u1.listIdxs.get(i).equals(u2.listIdxs.get(i))) {
          return u1.listIdxs.get(i) - u2.listIdxs.get(i);
        }
      }

      if (u1.getUnfinishedUpdateStatement().needsLike != u2.getUnfinishedUpdateStatement().needsLike) {
        return u1.getUnfinishedUpdateStatement().needsLike ? 1 : -1;
      }

      if (u1.getUnfinishedUpdateStatement().parentInfo.tableName != u2.getUnfinishedUpdateStatement().parentInfo.tableName) {
        return u1.getUnfinishedUpdateStatement().parentInfo.tableName.compareTo(u2.getUnfinishedUpdateStatement().parentInfo.tableName);
      }

      return 0;
    }
    
    
    public static boolean canCombine(UpdateGeneration u1, UpdateGeneration u2) {
      return compare(u1, u2) == 0;
    }
    
    
    public void combine(UpdateGeneration other) {
      updateStatement.columns.addAll(other.updateStatement.columns);
      updateStatement.values.putAll(other.getUnfinishedUpdateStatement().values);
    }
  }

  
  protected static class UnfinishedUpdateStatement {
    // UPDATE <table> SET <column=value - pairs> WHERE <primaryKey> <LIKE OR => ?
    private static final String updateTemplate = "UPDATE %s SET %s WHERE %s %s ?";
    private final boolean needsLike;
    private final String primaryKeySuffix;
    private final List<QualifiedStorableColumnInformation> columns;
    private final HashMap<Integer, Object> values;
    private final StorableStructureInformation parentInfo;
    
    UnfinishedUpdateStatement(boolean needsLike, String primaryKeySuffix, StorableColumnInformation relevantDBStorableColumn, QualifiedStorableColumnInformation column) {
      this.needsLike = needsLike;
      this.primaryKeySuffix = primaryKeySuffix;
      this.columns = new ArrayList<>();
      this.values = new HashMap<>();
      this.parentInfo = relevantDBStorableColumn.getParentStorableInfo();
      addColumn(column);
    }
    
    public void addColumn(QualifiedStorableColumnInformation column) {
      columns.add(column);
    }
    
    public void setParam(QualifiedStorableColumnInformation column, Object value) {
      if(value != null) {
        values.put(columns.indexOf(column), value);
      }
    }
    
    public Pair<String, Parameter> finish(String primaryKey) {
      Parameter params = fillParameter();
      String comparison = needsLike ? "LIKE" : "=";
      String columnInfo = createColumnUpdateSql();
      String updateSql = String.format(updateTemplate, parentInfo.getTableName(), columnInfo, parentInfo.getPrimaryKeyName(), comparison);
      params.add(primaryKey + primaryKeySuffix);
      return Pair.of(updateSql, params);
    }


    public Pair<String, Parameter> finishInsert(String primaryKey, String typename) {
      Parameter params = fillParameter();
      List<String> columnNames = columns.stream().map(c -> c.getColumn().getColumnName()).collect(Collectors.toList());

      StringBuilder sb = new StringBuilder();
      sb.append("insert into ").append(parentInfo.getTableName()).append(" (");
      sb.append(String.join(", ", columnNames));

      sb.append(", ").append(parentInfo.getPrimaryKeyName());
      params.add(primaryKey + primaryKeySuffix);
      String typeColName = getColName(VarType.TYPENAME);
      if (typeColName != null) {
        sb.append(", ").append(typeColName);
        params.add(typename);
      }
      String parentIdColName = getColName(VarType.EXPANSION_PARENT_FK);
      if (parentIdColName != null) {
        sb.append(", ").append(parentIdColName);
        params.add(primaryKey);
      }
      String listIdxColName = getColName(VarType.LIST_IDX);
      if (listIdxColName != null) {
        sb.append(", ").append(listIdxColName);
        params.add(primaryKeySuffix.substring(primaryKeySuffix.lastIndexOf("#") + 1));
      }

      sb.append(") values (? ");
      sb.append(", ?".repeat(params.size() - 1));
      sb.append(")");
      return Pair.of(sb.toString(), params);
    }

 
    private String getColName(VarType type) {
      StorableColumnInformation colInfo = parentInfo.getColInfoByVarType(type);
      if (colInfo != null) {
        return colInfo.getColumnName();
      }
      return null;
    }


    private Parameter fillParameter() {
      Parameter params = new Parameter();
      for (int i = 0; i < columns.size(); i++) {
        Object value = values.get(i);
        if (value != null) {
          params.add(value);
        }
      }
      return params;
    }
    
    private String createColumnUpdateSql() {
      StringBuilder sb = new StringBuilder();
      for(int i=0; i< columns.size()-1; i++) {
        appendSingleColumnUpdateSql(sb, columns.get(i));
        sb.append(", ");
      }
      appendSingleColumnUpdateSql(sb, columns.get(columns.size()-1));
      
      return sb.toString();
    }
    
    private void appendSingleColumnUpdateSql(StringBuilder sb, QualifiedStorableColumnInformation column) {
      sb.append(column.getColumn().getColumnName());
      sb.append(" = ");
      sb.append(values.containsKey(columns.indexOf(column))? "?" : "NULL");
    }
    
    public List<QualifiedStorableColumnInformation> getQualifiedColumn() {
      return columns;
    }
    
    
    public Pair<String, Parameter> getExistenceVerificationRequest(String primaryKey) {
      StringBuilder existenceVerificationBuilder = new StringBuilder()
        .append("SELECT count(*) FROM ")
        .append(parentInfo.getTableName())
        .append(" WHERE ")
        .append(parentInfo.getPrimaryKeyName())
        .append(" = ?");
      return Pair.of(existenceVerificationBuilder.toString(), new Parameter(primaryKey + primaryKeySuffix));
    }
    
    
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof UnfinishedUpdateStatement)) {
        return false;
      }
      UnfinishedUpdateStatement other = (UnfinishedUpdateStatement) obj;
      if (!primaryKeySuffix.equals(other.primaryKeySuffix)) {
        return false;
      }
      return columns.equals(other.columns);
    }
    
    @Override
    public int hashCode() {
      return columns.hashCode();
    }
  }

  //if there are multiple updates for the same row, combine them
  public static void combineUpdates(List<UpdateGeneration> updates) {
    Collections.sort(updates);
    for (int i = updates.size() - 1; i >= 1; i--) {
      UpdateGeneration u1 = updates.get(i);
      UpdateGeneration u2 = updates.get(i - 1);
      if (UpdateGeneration.canCombine(u1, u2)) {
        u2.combine(u1);
        updates.remove(u1);
      }
    }
  }
  
  
}
