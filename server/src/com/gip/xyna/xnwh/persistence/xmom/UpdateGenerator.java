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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.QualifiedStorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarDefinitionSite;
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
    StringBuilder updateBuilder = new StringBuilder();    
    updateBuilder.append("UPDATE ")
                 .append(relevantDBStorableColumn.getParentStorableInfo().getTableName())
                 .append(" SET ").append(relevantDBStorableColumn.getColumnName()).append(" = ");
    String beforeUpdateValue = updateBuilder.toString();
    updateBuilder = new StringBuilder();
    updateBuilder.append(" WHERE ").append(relevantDBStorableColumn.getParentStorableInfo().getPrimaryKeyName());
    if (needsLike) {
      updateBuilder.append(" LIKE ");
    } else {
      updateBuilder.append(" = ");
    }
    updateBuilder.append("?");
    String afterUpdateValue = updateBuilder.toString();
    return new UnfinishedUpdateStatement(beforeUpdateValue, afterUpdateValue, primaryKeySuffix, column);
  }
  
  
  public static Collection<UpdateGeneration> pruneDuplicatedUpdates(Collection<UpdateGeneration> updates) {
    return new HashSet<UpdateGeneration>(updates);
  }
  
  
  protected static class UpdateGeneration {
    
    private final UnfinishedUpdateStatement updateStatement;
    private final List<Integer> listIdxs;
    
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
      return updateStatement.getQualifiedColumn().hashCode();
    }
    
  }

  
  protected static class UnfinishedUpdateStatement {
    
    private final String beforeUpdateValue;
    private final String afterUpdateValue;
    private final String primaryKeySuffix;
    private final QualifiedStorableColumnInformation column;
    
    UnfinishedUpdateStatement(String beforeUpdateValue, String afterUpdateValue, String primaryKeySuffix, QualifiedStorableColumnInformation column) {
      this.beforeUpdateValue = beforeUpdateValue;
      this.afterUpdateValue = afterUpdateValue;
      this.primaryKeySuffix = primaryKeySuffix;
      this.column = column;
    }
    
    
    public Pair<String, Parameter> finish(String primaryKey, Object value) {
      Parameter params = new Parameter();
      StringBuilder updateBuilder = new StringBuilder();
      updateBuilder.append(beforeUpdateValue);
      if (value == null) {
        updateBuilder.append("NULL");
      } else {
        updateBuilder.append("?");
        params.add(value);
      }
      updateBuilder.append(afterUpdateValue);
      params.add(primaryKey + primaryKeySuffix);
      return Pair.of(updateBuilder.toString(), params);
    }
    
    public QualifiedStorableColumnInformation getQualifiedColumn() {
      return column;
    }
    
    
    public Pair<String, Parameter> getExistenceVerificationRequest(String primaryKey) {
      StringBuilder existenceVerificationBuilder = new StringBuilder()
        .append("SELECT count(*) FROM ")
        .append(column.getColumn().getParentStorableInfo().getTableName())
        .append(" WHERE ")
        .append(column.getColumn().getParentStorableInfo().getPrimaryKeyName())
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
      if (!beforeUpdateValue.equals(other.beforeUpdateValue)) {
        return false;
      }
      if (!afterUpdateValue.equals(other.afterUpdateValue)) {
        return false;
      }
      if (!primaryKeySuffix.equals(other.primaryKeySuffix)) {
        return false;
      }
      return column.equals(other.column);
    }
    
    @Override
    public int hashCode() {
      return column.hashCode();
    }
  }
  
  
}
