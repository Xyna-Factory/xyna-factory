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
package com.gip.xyna.persistence.xsor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.management.SearchCriterionIndexDefinitionDeterminator;
import com.gip.xyna.xsor.indices.management.SearchCriterionSingleIndexDefinitionDeterminator;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.ComparisionAlgorithm;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.persistence.xsor.ConditionTreeNode.ConditionTreeLeafNode;
import com.gip.xyna.persistence.xsor.ConditionTreeNode.ConditionTreeIntermidiateNode;
import com.gip.xyna.persistence.xsor.helper.TypedValuesHelper;
import com.gip.xyna.persistence.xsor.indices.StorableBasedSearchCriterion;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ParsedQuery;



public class PreparedClusterQueryCreator<E> {

  private final static SearchCriterionIndexDefinitionDeterminator determinator = new SearchCriterionSingleIndexDefinitionDeterminator();
  private final static Pattern BYTEARRAY_IN_DISSECTER = Pattern.compile("'\\[[\\d\\s,-]+\\]'");
  
  public PreparedClusterQuery<E> prepareQuery(Query<E> query, Class<? extends Storable> storableClazz, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions) throws PersistenceLayerException {
    return prepareQuery(query.getTable(), query.getSqlString(), storableClazz, query.getReader(), indexDefinitions);
  }
  
  
  public PreparedClusterQuery<E> prepareQuery(String tableName, String sqlString, Class<? extends Storable> storableClazz, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions) throws PersistenceLayerException {
    return prepareQuery(tableName, sqlString, storableClazz, null);
  }
  
  
  private PreparedClusterQuery<E> prepareQuery(String tableName, String sqlString, Class<? extends Storable> storableClazz, ResultSetReader<? extends E> reader, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions) throws PersistenceLayerException {
    ParsedQuery parsedQuery = new ParsedQuery(sqlString);
    ConditionTreeNode root = ConditionTreeNode.parseIntoTree(parsedQuery) ;
    List<SearchCriterion> searchCriterion = normalizeParsedQuery(root, Storable.getPersistable(storableClazz).primaryKey());
    Map<Integer, SearchValue> prepreparedParameter = prepareFixedParams(root, storableClazz);
    SearchRequest searchRequest = new SearchRequest(tableName, searchCriterion);
    searchRequest.establishAppropriateIndexDefinitions(indexDefinitions, determinator);
    return new PreparedClusterQuery<E>(reader, parsedQuery.isForUpdate(), parsedQuery.isCountQuery(), searchRequest, parsedQuery.getSelection(), prepreparedParameter, sqlString);
  }
    
  
  private List<SearchCriterion> normalizeParsedQuery(ConditionTreeNode root, String nameOfPrimaryKey) {
    List<List<ConditionTreeLeafNode>> dnf = root.convertToLeafNodesInDNF();
    List<SearchCriterion> searchCritions = new ArrayList<SearchCriterion>();
    for (List<ConditionTreeLeafNode> list : dnf) {
      List<ColumnCriterion> columns = new ArrayList<ColumnCriterion>();
      for (ConditionTreeLeafNode columnCondition : list) {
        if (columnCondition.colName != null) {
          SearchColumnOperator operator = SearchColumnOperator.getSearchColumnOperatorBySqlRepresentation(columnCondition.comparision);
          boolean isPKinEquals = false;
          if (operator == SearchColumnOperator.EQUALS &&
              nameOfPrimaryKey != null &&
              nameOfPrimaryKey.equalsIgnoreCase(columnCondition.colName)) {
            isPKinEquals = true;         
          }
          ColumnCriterion column = new ColumnCriterion(columnCondition.colName, operator, columnCondition.paramMapping, isPKinEquals);
          columns.add(column);
        }
      }
      searchCritions.add(new StorableBasedSearchCriterion(columns));
    }
    return searchCritions;
  }
  
  
  private Map<Integer, SearchValue> prepareFixedParams(ConditionTreeNode node, Class<? extends Storable> storableClazz) {
    Map<Integer, SearchValue> prepreparedValues = new HashMap<Integer, SearchValue>();
    List<ConditionTreeLeafNode> fixedConditions = new ArrayList<ConditionTreeLeafNode>();
    if (node instanceof ConditionTreeLeafNode) {
      if (((ConditionTreeLeafNode)node).fixedValue != null)  {
        fixedConditions.add((ConditionTreeLeafNode)node);
      }
    } else if (node instanceof ConditionTreeIntermidiateNode) {
      for (ConditionTreeNode child : ((ConditionTreeIntermidiateNode)node).getChildren()) {
        prepreparedValues.putAll(prepareFixedParams(child, storableClazz));
      }
    }
    for (ConditionTreeLeafNode conditionTreeNode : fixedConditions) {
      SearchColumnOperator operatorForNode = SearchColumnOperator.getSearchColumnOperatorBySqlRepresentation(conditionTreeNode.comparision);
      if (operatorForNode == SearchColumnOperator.IN &&
          conditionTreeNode.fixedValue.startsWith("(") &&
          conditionTreeNode.fixedValue.endsWith(")")) {
        TypedValuesHelper.ValueType columnType = TypedValuesHelper.getValueTypeForColumn(conditionTreeNode.colName, storableClazz);
        if (columnType == TypedValuesHelper.ValueType.BYTE_ARRAY) {
          List<String> splitted = new ArrayList<String>();
          Matcher byteArrayDissecter = BYTEARRAY_IN_DISSECTER.matcher(conditionTreeNode.fixedValue);
          while (byteArrayDissecter.find()) {
            splitted.add(conditionTreeNode.fixedValue.substring(byteArrayDissecter.start(), byteArrayDissecter.end()));
          }
          Object typedArray = TypedValuesHelper.generateTypedArrayFromStringArray(conditionTreeNode.colName, splitted.toArray(new String[splitted.size()]), storableClazz);
          prepreparedValues.put(conditionTreeNode.paramMapping, new SearchValue(typedArray, ComparisionAlgorithm.ByteArray));
        } else {
          String unwrapped = conditionTreeNode.fixedValue.substring(1, conditionTreeNode.fixedValue.length() - 1);
          Object typedArray = TypedValuesHelper.generateTypedArrayFromStringArray(conditionTreeNode.colName, unwrapped.split(","), storableClazz);
          prepreparedValues.put(conditionTreeNode.paramMapping, new SearchValue(typedArray));
        }
      } else {
        TypedValuesHelper.ValueType columnType = TypedValuesHelper.getValueTypeForColumn(conditionTreeNode.colName, storableClazz);
        if (columnType == TypedValuesHelper.ValueType.STRING &&
            conditionTreeNode.fixedValue.startsWith("'") &&
            conditionTreeNode.fixedValue.endsWith("'")) {
          String unwrappedFxiedString = conditionTreeNode.fixedValue.substring(1, conditionTreeNode.fixedValue.length()-1);
          prepreparedValues.put(conditionTreeNode.paramMapping,
                                TypedValuesHelper.generateSearchValueFromStringValue(conditionTreeNode.colName, unwrappedFxiedString, storableClazz));
        } else {
          prepreparedValues.put(conditionTreeNode.paramMapping,
                                TypedValuesHelper.generateSearchValueFromStringValue(conditionTreeNode.colName, conditionTreeNode.fixedValue, storableClazz));
        }
        
        
      }
    }
    return prepreparedValues;
  }
    
  
 
}
