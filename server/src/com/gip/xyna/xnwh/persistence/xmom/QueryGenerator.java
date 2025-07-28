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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceAccessControl.PersistenceAccessContext;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors.CastCondition;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors.FormulaParsingResult;
import com.gip.xyna.xnwh.persistence.xmom.StorableNodeReader.ChildReaderData;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation.ColumnInfoRecursionMode;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableVariableType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarDefinitionSite;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;


public class QueryGenerator {
  
  public static String ROOT_TYPE_IDENTIFIER = "%0%";
  
  private static final Logger logger = CentralFactoryLogging.getLogger(QueryGenerator.class);
  
  private static final XynaPropertyBoolean USE_UNION_ALL_PROPERTY = 
                   new XynaPropertyBoolean("xnwh.persistence.unionall", true).setHidden(true);
  
  private static final String CAST_MARKER_START = "#" + Functions.CAST_FUNCTION_NAME + "(";
  private static final String CAST_MARKER_END = ")";
  public final Function<String, String> escape;
  
  
  public QueryGenerator() {
    this(Function.identity());
  }
  

  public QueryGenerator(Function<String, String> escape) {
    this.escape = escape;
  }

  public QueryPiplineElement<?, ?> parse(XMOMStorableStructureInformation info, List<String> selectedUnresolvedColumns, IFormula condition,
                                         QueryParameter queryParameter, boolean forUpdate, Parameter paras, PersistenceAccessContext context) {
    FormulaParsingResult fpr = PersistenceExpressionVisitors.parseFormula(condition, info, paras, this);
    return build(info, selectedUnresolvedColumns, fpr, queryParameter, forUpdate, paras, context);
  }
  
  public QueryPiplineElement<?, ?> count(XMOMStorableStructureInformation info, IFormula condition, QueryParameter queryParameter, Parameter paras , PersistenceAccessContext context) {
    FormulaParsingResult fpr = PersistenceExpressionVisitors.parseFormula(condition, info, paras, this);
    
    List<QualifiedStorableColumnInformation> conditionQueryColumns = new ArrayList<QualifiedStorableColumnInformation>();
    conditionQueryColumns.addAll(fpr.getColumnsFromConditions());
    StringBuilder conditionQueryBuilder = new StringBuilder(buildConditionQuery(info, conditionQueryColumns, null, true, queryParameter));
    conditionQueryBuilder.append(fpr.getSqlString(null));
    if (queryParameter.getMaxObjects() > 0) {
      conditionQueryBuilder.append(" LIMIT ")
                           .append(queryParameter.getMaxObjects())
                           .append(") AS limitedsubquery");
    }
    appendConditionOnCurrentVersionIfNecessary(conditionQueryBuilder, paras, info, fpr, queryParameter);
    QueryPiplineElement<?, ?> qpe = new FirstQueryPiplineElement<>(conditionQueryBuilder.toString(), new ResultSetReader<Integer>() {

      @Override
      public Integer read(ResultSet rs) throws SQLException {
        return rs.getInt(1);
      }
    }, -1, paras);
    qpe.rootTableName = info.getTableName();
    return qpe;
  }
  
  
  @SuppressWarnings({"rawtypes", "unchecked"})
  QueryPiplineElement<?, ?> build(XMOMStorableStructureInformation info, List<String> selectedUnresolvedColumns, FormulaParsingResult fpr,
                                 QueryParameter queryParameter, boolean forUpdate, Parameter paras, PersistenceAccessContext context) {
    AliasDictionary aliasDictionary = new AliasDictionary(info);
    List<QualifiedStorableColumnInformation> selectedColumns = resolveColumns(info, selectedUnresolvedColumns, aliasDictionary);
    if (context != PersistenceAccessControl.allAccess()) {
      if (XMOMPersistenceOperationAlgorithms.allowReResolution.get()) {
        List<QualifiedStorableColumnInformation> reResolvedColumns = reResolveColumns(selectedColumns);
        reResolvedColumns = restrictToReachableColumns(reResolvedColumns, fpr.getCasts()); 
        XMOMPersistenceOperationAlgorithms.validateSelectionAgainstCallingContext(context, reResolvedColumns);
      } else {
        XMOMPersistenceOperationAlgorithms.validateSelectionAgainstCallingContext(context, selectedColumns);
      }
    }
    
    List<QualifiedStorableColumnInformation> participatingColumns = new ArrayList<QualifiedStorableColumnInformation>();
    participatingColumns.addAll(selectedColumns);
    participatingColumns.addAll(fpr.getColumnsFromConditions());
    participatingColumns.addAll(extractColumnsFromSortCriteria(queryParameter.getSortCriterions(), info));
    
    boolean querySplitNecessary = isQuerySplitNecessary(info, participatingColumns);
    if (querySplitNecessary) {
      // 1. Query select: DISTINCT root.pk    maxRows: from queryParams  condition: from formula  params: from formula     sort: from queryParams
      // 2. Query select: from selectionMask  maxRows: -1                condition: on pk         params: from 1. Query    sort: for reader
      List<QualifiedStorableColumnInformation> conditionQueryColumns = new ArrayList<QualifiedStorableColumnInformation>();
      conditionQueryColumns.addAll(fpr.getColumnsFromConditions());
      conditionQueryColumns.addAll(extractColumnsFromSortCriteria(queryParameter.getSortCriterions(), info));
      StringBuilder conditionQueryBuilder = new StringBuilder(buildConditionQuery(info, conditionQueryColumns, null, false, queryParameter));
      
      conditionQueryBuilder.append(fpr.getSqlString(null));
      appendConditionOnCurrentVersionIfNecessary(conditionQueryBuilder, paras, info, fpr, queryParameter);
      
      // FIXME sort criteria might be from subStorable and not contained in splitted selection selection!
      // just append extractColumnsFromSortCriteria(...) in buildConditionQuery?
      appendSortCriterion(queryParameter.getSortCriterions(), conditionQueryBuilder, info, null);
      
      if (forUpdate) {
        conditionQueryBuilder.append(" FOR UPDATE");
      }
      
      FirstQueryPiplineElement conditionQuery = new FirstQueryPiplineElement(conditionQueryBuilder.toString(),
                                                                             //new SingleColumnReader(info),
                                                                             new DistinctLimitSingleColumnReader(info, queryParameter.getMaxObjects()),
                                                                             -1, paras);
      conditionQuery.rootTableName = info.getTableName();
      
      QueryTreeNode queryRoot = buildQueryTree(info, selectedColumns, new ArrayList<StorableColumnInformation>());
      resortQueryTreeNode(queryRoot);
      
      DataQueryPiplineElement dataQuery = new DataQueryPiplineElement(buildDataQuery(queryRoot, selectedColumns, aliasDictionary),
                                                                      buildReaderFromQueryTree(queryRoot, selectedColumns, aliasDictionary),
                                                                      info.getColInfoByVarType(VarType.PK), escape);
      dataQuery.rootTableName = info.getTableName();
      dataQuery.setAliasDictionary(aliasDictionary);
      dataQuery.setSelectedColumns(selectedColumns);
      conditionQuery.setNext(dataQuery);
      return conditionQuery;
    } else {
      QueryTreeNode queryRoot = buildQueryTree(info, participatingColumns, new ArrayList<StorableColumnInformation>());
      resortQueryTreeNode(queryRoot);
      
      StringBuilder queryBuilder = new StringBuilder();
      queryBuilder.append(buildSelection(queryRoot, selectedColumns, aliasDictionary));
      queryBuilder.append(" FROM ")
                  .append(escape.apply(queryRoot.localInfo.getTableName()));
      queryBuilder.append(buildJoinedTable(queryRoot, selectedColumns, aliasDictionary));
      
      queryBuilder.append(fpr.getSqlString(aliasDictionary));
      
      appendConditionOnCurrentVersionIfNecessary(queryBuilder, paras, info, fpr, queryParameter);
      
      appendSortCriterion(queryParameter.getSortCriterions(), queryBuilder, info, aliasDictionary);
      
      if (forUpdate) {
        queryBuilder.append(" FOR UPDATE");
      }
      
      QueryPiplineElement qpe = new FirstQueryPiplineElement(queryBuilder.toString(), buildReaderFromQueryTree(queryRoot, selectedColumns, aliasDictionary), queryParameter.getMaxObjects(), paras);
      qpe.rootTableName = info.getTableName();
      qpe.setAliasDictionary(aliasDictionary);
      qpe.setSelectedColumns(selectedColumns);
      return qpe;
    }
  }
  
  
  private List<QualifiedStorableColumnInformation> reResolveColumns(List<QualifiedStorableColumnInformation> columns) {
    List<QualifiedStorableColumnInformation> reResolved = new ArrayList<>();
    for (QualifiedStorableColumnInformation qsci : columns) {
      reResolved.add(reResolveColumn(qsci));
    }
    return reResolved;
  }
  
  private QualifiedStorableColumnInformation reResolveColumn(QualifiedStorableColumnInformation column) {
    // dann hoch zum root
    // den root neu auflösen
    // dann runter dem accessPath entlang
    // und dann neu bauen mit dem richtigem Besitzer
    XMOMStorableStructureInformation root;
    if (column.getAccessPath().size() <= 0) {
      root = column.getColumn().getParentXMOMStorableInformation();
    } else {
      root = column.getAccessPath().get(0).getParentXMOMStorableInformation();
    }
    XMOMStorableStructureInformation reResolvedRoot = 
                    XMOMStorableStructureCache.getInstance(root.getDefiningRevision()).getStructuralInformation(root.getFqXmlName());
    List<StorableColumnInformation> reResolvedAccessPath = new ArrayList<>();
    for (StorableColumnInformation colInfo : column.getAccessPath()) {
      StorableStructureInformation currentElement;
      if (reResolvedAccessPath.size() <= 0) {
        currentElement = reResolvedRoot;
      } else {
        currentElement = reResolvedAccessPath.get(reResolvedAccessPath.size() - 1).getStorableVariableInformation();
      }
      StorableColumnInformation reResolvedColumn = getColumnInfoAcrossHierarchy(currentElement, colInfo.getColumnName());
      if (reResolvedColumn == null) {
        throw new RuntimeException("Failed to reResolve column " + colInfo.getColumnName());
      }
      reResolvedAccessPath.add(reResolvedColumn);
    }
    StorableStructureInformation lastStructure;
    if (reResolvedAccessPath.size() <= 0) {
      lastStructure = reResolvedRoot;
    } else {
      lastStructure = reResolvedAccessPath.get(reResolvedAccessPath.size() - 1).getStorableVariableInformation();
    }
    StorableColumnInformation lastColumn = getColumnInfoAcrossHierarchy(lastStructure, column.getColumn().getColumnName());
    if (lastColumn == null) {
      throw new RuntimeException("Failed to reResolve lastColumn " + column.getColumn().getColumnName());
    }
    return new QualifiedStorableColumnInformation(lastColumn, reResolvedAccessPath);
  }
  
  
  private StorableColumnInformation getColumnInfoAcrossHierarchy(StorableStructureInformation from, String columnName) {
    StorableColumnInformation column;
    StorableStructureInformation currentContext = from;
    while (currentContext != null) {
      column = currentContext.getColumnInfoByName(columnName);
      if (column != null) {
        return column;
      }
      currentContext = currentContext.getSuperEntry() == null ? null : currentContext.getSuperEntry().getInfo();
    }
    column = from.getColumnInfoByName(columnName);
    if (column == null) {
      for (StorableStructureInformation ssi : from.getSubEntriesRecursivly()) {
        column = ssi.getColumnInfoByName(columnName);
        if (column != null) {
          return column;
        }
      }
    }
    return column;
  }
  
  
  private List<QualifiedStorableColumnInformation> restrictToReachableColumns(List<QualifiedStorableColumnInformation> reResolvedColumns, List<CastCondition> casts) {
    // for each cast
    // all columns that pass that cast
    // check if there reachable in this or the super 
    // else they should be reachable in sub only and should be removed
    List<QualifiedStorableColumnInformation> restrictedColumns = new ArrayList<>(reResolvedColumns);
    for (CastCondition cc : casts) {
      Iterator<QualifiedStorableColumnInformation> iter = restrictedColumns.iterator();
      while (iter.hasNext()) {
        QualifiedStorableColumnInformation qsci = iter.next();
        if (castCovers(cc.accessPath, qsci)) {
          StorableStructureInformation ssi;
          if (cc.accessPath.size() <= 0) {
            ssi = qsci.column.getParentStorableInfo();
          } else {
            StorableColumnInformation sci = qsci.getAccessPath().get(cc.accessPath.size() - 1);
            ssi = sci.getStorableVariableInformation();
          }
          if (!isEqualOrSuper(ssi, cc.typename)) {
            iter.remove();
          }
        }
      }
    }
    return restrictedColumns;
  }
  

  private boolean isEqualOrSuper(StorableStructureInformation ssi, String typename) {
    if (ssi.getFqXmlName().equals(typename)) {
      return true;
    } else {
      if (ssi.hasSuper()) {
        return isEqualOrSuper(ssi.getSuperEntry().getInfo(), typename);
      } else {
        return false;
      }
    }
  }

  private boolean castCovers(List<String> accessPath, QualifiedStorableColumnInformation qsci) {//TODO: escape?
    String fullPath = "";
    for (StorableColumnInformation sci : qsci.accessPath) {
      fullPath += sci.getColumnName() + ".";
    }
    String accessPathString = StringUtils.joinStringArray(accessPath.toArray(new String[0]), ".");
    return fullPath.startsWith(accessPathString);
  }

  private void resortQueryTreeNode(QueryTreeNode node) {
    Collections.sort(node.joinChildren, new Comparator<QueryTreeNode>() {
      public int compare(QueryTreeNode o1, QueryTreeNode o2) {
        return -(countSublists(o1.localInfo, false) - countSublists(o2.localInfo, false));
      }
    });
    for (QueryTreeNode child : node.joinChildren) {
      resortQueryTreeNode(child);
    }
    for (QueryTreeNode child : node.unionChildren) {
      resortQueryTreeNode(child);
    }
  }
  
 
  private void appendSortCriterion(SortCriterion[] sortCriterion, StringBuilder queryBuilder, XMOMStorableStructureInformation info, AliasDictionary dictionary) {
    if (sortCriterion!= null && sortCriterion.length > 0) {
      queryBuilder.append(" ORDER BY");
      boolean first = true;
      for (SortCriterion sc : sortCriterion) {
        QualifiedStorableColumnInformation orderBy = extractColumnFromSortCriterion(sc, info);
        if (first) {
          first = false;          
        } else {
          queryBuilder.append(",");
        }
        queryBuilder.append(" ");
        String escColumnName = escape.apply(orderBy.getColumn().getColumnName());
        if (dictionary == null) {
          String escTableName = escape.apply(orderBy.getColumn().getParentStorableInfo().getTableName());
          queryBuilder.append(escTableName).append(".").append(escColumnName);
        } else {
          String escTableName = escape.apply(dictionary.getTableAlias(orderBy.getAccessPath()));
          queryBuilder.append(escTableName).append(".").append(escColumnName);
        }
        if (sc.isReverse()) {
          queryBuilder.append(" DESC");
        }
        //ASC ist default
      }
    }    
  }
  
  private Collection<QualifiedStorableColumnInformation> extractColumnsFromSortCriteria(SortCriterion[] sortCriterion, XMOMStorableStructureInformation info) {
    Collection<QualifiedStorableColumnInformation> orderByColumns = new ArrayList<QueryGenerator.QualifiedStorableColumnInformation>();
    if (sortCriterion!= null && sortCriterion.length > 0) {
      for (SortCriterion sc : sortCriterion) {
        orderByColumns.add(extractColumnFromSortCriterion(sc, info));
      }
    }
    return orderByColumns;
  }
  
  
  private QualifiedStorableColumnInformation extractColumnFromSortCriterion(SortCriterion sortCriterion, XMOMStorableStructureInformation info) {
    if (!sortCriterion.getCriterion().startsWith("%0%")) {
      throw new RuntimeException("Missing leading root var!");
    }
    if (XMOMPersistenceOperationAlgorithms.allowReResolution.get()) {
      return extractColumnFromSortCriterionInternally(sortCriterion.getCriterion().substring(3), info.reresolveMergedClone());
    } else {
      return extractColumnFromSortCriterionInternally(sortCriterion.getCriterion().substring(3), info);
    }
  }
  
  private QualifiedStorableColumnInformation extractColumnFromSortCriterionInternally(String sortCriterion, StorableStructureInformation info) {
    if (sortCriterion.startsWith(".")) {
      String varPart = sortCriterion.substring(1);
      String nextPart = null; 
      if (varPart.contains(".")) {
        int dotIndex = varPart.indexOf('.');
        int castIndex = varPart.indexOf('#');
        if (castIndex > 0 && castIndex < dotIndex) {
          nextPart = varPart.substring(castIndex);
          varPart = varPart.substring(0, castIndex);
        } else {
          nextPart = varPart.substring(dotIndex);
          varPart = varPart.substring(0, dotIndex);
        }
      }
      StorableColumnInformation columnInfo = info.getColumnInfoByNameAcrossHierachy(varPart);
      if (columnInfo == null) {
        throw new RuntimeException("Column " + varPart + " does not exist in " + info.getFqXmlName());
      }
      StorableStructureInformation nextInfo = null;
      if (nextPart != null) {
        if (columnInfo.isStorableVariable()) {
          nextInfo = columnInfo.getStorableVariableInformation();
        } else {
          nextPart = null;
        }
      }
      if (nextPart != null) {
        QualifiedStorableColumnInformation subColumn = extractColumnFromSortCriterionInternally(nextPart, nextInfo);
        subColumn.getAccessPath().add(0, columnInfo);
        return subColumn;
      } else {
        if (columnInfo.isList()) {
          throw new RuntimeException("Sort criterion contains variable of list type: " + varPart);
        }
        return new QualifiedStorableColumnInformation(columnInfo, new ArrayList<StorableColumnInformation>());
      }
    } else if (sortCriterion.startsWith(CAST_MARKER_START)) {
      String fqName = sortCriterion.substring(CAST_MARKER_START.length() + 1, sortCriterion.indexOf(CAST_MARKER_END) - 1);
      return extractColumnFromSortCriterionInternally(sortCriterion.substring(sortCriterion.indexOf(CAST_MARKER_END) + 1), findSubType(info, fqName));
    } else {
      throw new RuntimeException("Invalid sort criterion");
    }
  }
  
  
  private StorableStructureInformation findSubType(StorableStructureInformation root, String fqName) {
    // TODO allow DownCasts as well?
    if (root.getFqXmlName().equals(fqName)) {
      return root;
    }
    for (StorableStructureInformation subEntry : root.getSubEntriesRecursivly()) {
      if (subEntry.getFqXmlName().equals(fqName)) {
        return subEntry;
      }
    }
    throw new RuntimeException(fqName + " is not a subType of " + root.getFqXmlName());
  }

  
  
  private QueryTreeNode buildQueryTree(StorableStructureInformation root, List<QualifiedStorableColumnInformation> selectedColumns,
                                       List<StorableColumnInformation> accessPath) {
    List<QueryTreeNode> joinChildren = new ArrayList<QueryTreeNode>();
    List<QueryTreeNode> unionChildren = new ArrayList<QueryTreeNode>();
    for (StorableColumnInformation column : root.getColumnInfo(ColumnInfoRecursionMode.ONLY_LOCAL)) {
      List<StorableColumnInformation> newAccessPath = new ArrayList<StorableColumnInformation>(accessPath);
      newAccessPath.add(column);
      if (column.isStorableVariable() && selectionContainsColumnFromThatPath(newAccessPath, selectedColumns)) {
        if (column.isList()) {
          QueryTreeNode unionChild = buildQueryTree(column.getStorableVariableInformation(), selectedColumns, newAccessPath);
          unionChildren.add(unionChild);
        } else {
          QueryTreeNode joinChild = buildQueryTree(column.getStorableVariableInformation(), selectedColumns, newAccessPath);
          joinChildren.add(joinChild);
        }
      }
      if (column.getType() == VarType.TYPENAME) {
        QualifiedStorableColumnInformation typenameCol = new QualifiedStorableColumnInformation(column, accessPath);
        selectedColumns.add(typenameCol);
      }
    }
    return new QueryTreeNode(root, accessPath, joinChildren, unionChildren);
  }
  
  
  private boolean selectionContainsColumnFromThatPath(List<StorableColumnInformation> linkToRoot, List<QualifiedStorableColumnInformation> selectedColumns) {
    for (QualifiedStorableColumnInformation selectedColumn : selectedColumns) {
      for (StorableColumnInformation accessPathPart : selectedColumn.getAccessPath()) {
        if (accessPathPart == linkToRoot.get(linkToRoot.size() - 1)) {
          return true;
        }
      }
    }
    return false;
  }
  
  
  private String buildSelection(QueryTreeNode queryTreeNode, List<QualifiedStorableColumnInformation> columns, AliasDictionary dictionary) {
    StringBuilder build = new StringBuilder();
    build.append("SELECT ");
    for (QualifiedStorableColumnInformation column : columns) {
      if (column != columns.get(0)) {
        build.append(", ");
      }
      if (isJoinedReachable(queryTreeNode, column)) {
        String table = dictionary.getTableAlias(column.getAccessPath());
        if (table == null) {
          String escTableName = escape.apply(column.getColumn().getParentStorableInfo().getTableName());
          build.append(escTableName);
        } else {
          build.append(escape.apply(table));
        }
        String escColName = escape.apply(column.getColumn().getColumnName());
        build.append('.').append(escColName);
      } else {
        if (column.getColumn().getType() == VarType.LIST_IDX) {
          build.append("-1");
        } else {
          build.append("null");
        }
      }
      build.append(" AS ")
           .append(dictionary.getOrCreateColumnAlias(column));
    }
    if (columns.size() <= 0) {
      build.append(" * ");
    }
    return build.toString();
  }
  
  
  private String buildJoinedTable(QueryTreeNode node, List<QualifiedStorableColumnInformation> columns, AliasDictionary dictionary) {
    StringBuilder build = new StringBuilder();
    if (node.joinChildren != null && node.joinChildren.size() > 0) {
      for (QueryTreeNode joinEntry : node.joinChildren) {
        build.append(buildJoinCriterion(joinEntry, false, dictionary));
      }
    }
    if (node.unionChildren != null && node.unionChildren.size() > 0) {
      for (QueryTreeNode unionEntry : node.unionChildren) {
        if (USE_UNION_ALL_PROPERTY.get()) {
          build.append(" UNION ALL (");
        } else {
          build.append(" UNION (");
        }
        build.append(buildSelection(unionEntry, columns, dictionary));
        build.append(buildJoinedTable(unionEntry, columns, dictionary)); 
        build.append(")");
      }
    }
    return build.toString();
  }
  
  
  private String buildJoinCriterion(QueryTreeNode node, boolean useInnerJoinForCurrentNode, AliasDictionary dictionary) {
    StringBuilder build = new StringBuilder();
    for (int i = 0; i < node.accessPath.size() - 1; i++) {
      QualifiedStorableColumnInformation joinColumn = 
        new QualifiedStorableColumnInformation(node.accessPath.get(i), node.accessPath.subList(0, i));
      build.append(buildJoinCriterion(joinColumn, false, dictionary));
    }
    build.append(buildJoinCriterionRecursivly(node, useInnerJoinForCurrentNode, dictionary));
    return build.toString();
  }
  
  private String buildJoinCriterionRecursivly(QueryTreeNode node, boolean useInnerJoinForCurrentNode, AliasDictionary dictionary) {
    StringBuilder build = new StringBuilder();
    if (node.accessPath.size() > 0) {
      build.append(buildJoinCriterion(node.accessPath.get(node.accessPath.size() - 1), node.accessPath.subList(0, node.accessPath.size() - 1) , useInnerJoinForCurrentNode, dictionary));
    }
    List<QueryTreeNode> invertedJoins = new ArrayList<QueryGenerator.QueryTreeNode>(node.joinChildren);
    Collections.reverse(invertedJoins);
    for (QueryTreeNode joinEntry : invertedJoins) {
      build.append(buildJoinCriterionRecursivly(joinEntry, false, dictionary));
    }
    return build.toString();
  }

  
  private String buildJoinCriterion(QualifiedStorableColumnInformation datatypeColumnToJoin, boolean useInnerJoin, AliasDictionary dictionary) {
    return buildJoinCriterion(datatypeColumnToJoin.getColumn(), datatypeColumnToJoin.getAccessPath(), useInnerJoin, dictionary);
  }
  
  
  private String buildJoinCriterion(StorableColumnInformation column, List<StorableColumnInformation> accessPath, boolean useInnerJoin, AliasDictionary dictionary) {
    String ownTable = null;
    String parentTable = null;
    if (dictionary != null) {
      List<StorableColumnInformation> continuedPath = new ArrayList<StorableColumnInformation>(accessPath);
      continuedPath.add(column);
      ownTable = dictionary.getTableAlias(continuedPath);
      parentTable = dictionary.getTableAlias(accessPath);
    }
    
    StringBuilder build = new StringBuilder();
    if (useInnerJoin) {
      build.append(" INNER");
    } else {
      build.append(" LEFT");
    }
    build.append(" JOIN ")
      .append(escape.apply(column.getStorableVariableInformation().getTableName()));
    if (ownTable != null) {
      build.append(" ")
           .append(escape.apply(ownTable));
    }
    build.append(" ON ");
    if (parentTable == null) {
      build.append(escape.apply(column.getParentStorableInfo().getTableName()));
    } else {
      build.append(escape.apply(parentTable));
    }
    build.append('.');
    if (column.getStorableVariableType() == StorableVariableType.EXPANSION) {
      build.append(escape.apply(column.getParentStorableInfo().getPrimaryKeyName()))
           .append(" = ");
      if (ownTable == null) {
        build.append(escape.apply(column.getStorableVariableInformation().getTableName()));
      } else {
        build.append(escape.apply(ownTable));
      }
      build.append('.');
      StorableColumnInformation parentFk = column.getStorableVariableInformation().getColInfoByVarType(VarType.EXPANSION_PARENT_FK);
      if (parentFk == null) {
        parentFk = column.getStorableVariableInformation().getColInfoByVarType(VarType.UTILLIST_PARENT_FK);
      }
      build.append(escape.apply(parentFk.getColumnName()));
    } else {
      build.append(column.getCorrespondingReferenceIdColumn().getColumnName()).append(" = ");
      if (ownTable == null) {
        build.append(escape.apply(column.getStorableVariableInformation().getTableName()));
      } else {
        build.append(escape.apply(ownTable));
      }
      String refColName;
      if (column.getCorrespondingReferencedIdColumn() != null) {
        refColName = column.getCorrespondingReferencedIdColumn().getColumnName();
      } else {
        refColName = column.getStorableVariableInformation().getPrimaryKeyName();
      }
      
      build.append('.').append(escape.apply(refColName));
    }
    return build.toString();
  }
  
  
  private boolean isJoinedReachable(QueryTreeNode node, QualifiedStorableColumnInformation column) {
    if (covers(node.accessPath, column.accessPath)) {
      return true;
    } else {
      for (QueryTreeNode joins : node.joinChildren) {
        if (isJoinedReachable(joins, column)) {
          return true;
        }
      }
    }
    return false;
  }
  
  
  static boolean covers(List<StorableColumnInformation> coverage, List<StorableColumnInformation> toBeCovered) {
    if (toBeCovered.size() > coverage.size()) {
      return false;
    } else {
      for (int i = 0; i < coverage.size(); i++) {
        if (i >= toBeCovered.size()) {
          return true;
        } else {
          if (coverage.get(i) != toBeCovered.get(i)) {
            return false;
          }
        }
      }
      return true;
    }
  }
  
  
  String buildDataQuery(QueryTreeNode qtn, List<QualifiedStorableColumnInformation> columns, AliasDictionary dictionary) {
    if (logger.isTraceEnabled()) {
      trace("reader-tree pre flattening:");
      trace(qtn.print(""));
    }
    QueryTreeNode flattenedRoot = flattenQueryTree(qtn);
    if (logger.isTraceEnabled()) {
      trace("reader-tree post flattening:");
      trace(flattenedRoot.print(""));
    }
    List<QualifiedStorableColumnInformation> order = new ArrayList<QualifiedStorableColumnInformation>();
    QualifiedStorableColumnInformation pkColumn = new QualifiedStorableColumnInformation(qtn.localInfo.getColInfoByVarType(VarType.PK), qtn.accessPath);
    order.add(pkColumn);
    StringBuilder queryBuilder = new StringBuilder(
      buildDataQueryRecursivly(flattenedRoot, columns, order, dictionary, false));
    if (order.size() > 0) {
      queryBuilder.append(" ORDER BY ");
      Iterator<QualifiedStorableColumnInformation> orderByIterator = order.iterator();
      while (orderByIterator.hasNext()) {
        QualifiedStorableColumnInformation orderbb = orderByIterator.next();
        StringBuilder sb = new StringBuilder();
        for (StorableColumnInformation ccc : orderbb.accessPath) {
          sb.append(ccc.getParentStorableInfo().getTableName()).append(".");
        }
        sb.append(escape.apply(orderbb.getColumn().getParentStorableInfo().getTableName())).append(".");
        queryBuilder.append(dictionary.getOrCreateColumnAlias(orderbb)).append(" DESC"); //damit das rootelement am ende kommt (hat als listen-index überall NULL)
        if (orderByIterator.hasNext()) {
          queryBuilder.append(", ");
        }
      }
    }
    String queryString = queryBuilder.toString();
    if (queryString.endsWith(", ")) {
      return queryString.substring(0, queryString.length() - 2);
    } else {
      return queryString;
    }
  }
  
  
  private QueryTreeNode flattenQueryTree(QueryTreeNode root) {
    List<QueryTreeNode> joinChildren = new ArrayList<QueryTreeNode>();
    List<QueryTreeNode> expandedJoins = collectAllJoins(root);
    for (QueryTreeNode join : expandedJoins) {
      joinChildren.add(new QueryTreeNode(join.localInfo, join.accessPath, 
                       new ArrayList<QueryTreeNode>(), new ArrayList<QueryTreeNode>()));
    }
    List<QueryTreeNode> unionChildren = new ArrayList<QueryTreeNode>();
    List<QueryTreeNode> expandedUnions = collectAllUnions(expandedJoins);
    expandedUnions.addAll(root.unionChildren);
    for (QueryTreeNode expandedUnionChild : expandedUnions) {
      unionChildren.add(flattenQueryTree(expandedUnionChild));
    }
    return new QueryTreeNode(root.localInfo, root.accessPath, joinChildren, unionChildren);
  }
  
  
  // everything that is reachable via joins
  private List<QueryTreeNode> collectAllJoins(QueryTreeNode root) {
    List<QueryTreeNode> allJoins = new ArrayList<QueryTreeNode>();
    for (QueryTreeNode joinChild : root.joinChildren) {
      allJoins.addAll(collectAllJoins(joinChild));
      allJoins.add(joinChild);
    }
    return allJoins;
  }
  
  
  //every direct union that is join reachable (collectedJoins collected from collectAllJoins)
  private List<QueryTreeNode> collectAllUnions(List<QueryTreeNode> collectedJoins) {
    List<QueryTreeNode> allUnions = new ArrayList<QueryTreeNode>();
    for (QueryTreeNode joinChild : collectedJoins) {
      for (QueryTreeNode unionEntry : joinChild.unionChildren) {
        allUnions.add(unionEntry);
      }
    }
    return allUnions;
  }
  
  
  String buildDataQueryRecursivly(QueryTreeNode currentNode,
                                  List<QualifiedStorableColumnInformation> columns,
                                  List<QualifiedStorableColumnInformation> order,
                                  AliasDictionary dictionary, boolean inner) {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append(buildSelection(currentNode, columns, dictionary));
    queryBuilder.append(" FROM ");
    if (currentNode.accessPath.size() <= 0) {
      queryBuilder.append(escape.apply(currentNode.localInfo.getTableName()));
    } else {
      queryBuilder.append(escape.apply(currentNode.accessPath.get(0).getParentStorableInfo().getTableName()));
    }
    queryBuilder.append(buildJoinCriterion(currentNode, inner, dictionary));
    for (QueryTreeNode unionChild : currentNode.unionChildren) {
      if (USE_UNION_ALL_PROPERTY.get()) {
        queryBuilder.append(" UNION ALL ");
      } else {
        queryBuilder.append(" UNION ");
      }
      QualifiedStorableColumnInformation listIdxCol = new QualifiedStorableColumnInformation(unionChild.localInfo.getColInfoByVarType(VarType.LIST_IDX),
                                                                                             unionChild.accessPath);
      if (listIdxCol != null && columns.contains(listIdxCol)) {
        order.add(listIdxCol);
      }
      queryBuilder.append(buildDataQueryRecursivly(unionChild, columns, order, dictionary, true));
    }
    return queryBuilder.toString();
  }
  
  private boolean checkForListAccess(Collection<QualifiedStorableColumnInformation> columnsWithConditions) {
    for (QualifiedStorableColumnInformation column : columnsWithConditions) {
      for (StorableColumnInformation pathPart : column.getAccessPath()) {
        if(pathPart.isList()) {
          return true;
        }
      }
    }
    return false;
  }
  
  String buildConditionQuery(XMOMStorableStructureInformation rootInfo, Collection<QualifiedStorableColumnInformation> columnsWithConditions, AliasDictionary dictionary, boolean count, QueryParameter queryParameter) {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append("SELECT ");
    if (count) {
      if (queryParameter.getMaxObjects() > 0) {
        queryBuilder.append("COUNT(*) FROM ( SELECT ");
      } else {
        queryBuilder.append("COUNT( ");
        if(checkForListAccess(columnsWithConditions)) {
          queryBuilder.append("DISTINCT ");
        }
      }
    }
    String escTableName = escape.apply(rootInfo.getTableName());
    String escPrimaryKey = escape.apply(rootInfo.getPrimaryKeyName());
    queryBuilder.append(escTableName).append(".").append(escPrimaryKey);
    if (count &&
        queryParameter.getMaxObjects() <= 0) {
      queryBuilder.append(")");   
    }
    queryBuilder.append(" FROM ").append(escTableName);
    List<QualifiedStorableColumnInformation> completeJoinPaths = new ArrayList<QualifiedStorableColumnInformation>(); // no Set to preserve traversal order
    for (QualifiedStorableColumnInformation column : columnsWithConditions) {
      for (StorableColumnInformation pathPart : column.getAccessPath()) {
        int index = column.getAccessPath().indexOf(pathPart);
        QualifiedStorableColumnInformation qualCol = new QualifiedStorableColumnInformation(pathPart, column.getAccessPath().subList(0, index));
        if (!completeJoinPaths.contains(qualCol)) {
          completeJoinPaths.add(qualCol);
        }
      }
    }
    for (QualifiedStorableColumnInformation join : completeJoinPaths) {
      queryBuilder.append(buildJoinCriterion(join, false, dictionary));
    }
    return queryBuilder.toString();
  }
  
  
  static boolean resolvePath(StorableStructureInformation rootInfo, StorableColumnInformation conditionColumn, Stack<StorableColumnInformation> path) {
    for (StorableColumnInformation column : rootInfo.getColumnInfo(false)) {
      if (column == conditionColumn) {
        return true;
      } else if (column.isStorableVariable()) {
        path.push(column);
        if (resolvePath(column.getStorableVariableInformation(), conditionColumn, path)) {
          return true;
        } else {
          path.pop();
        }
      }
    }
    return false;
  }
  
  
  /* Column Resolution According to "Xyna XMOM GUIs v0.25" 4.2.2.3.
   * (1) %0% gilt implizit und liefert nur Primärschlüssel (uid und timestamp) zurück.
   * (2) POINTER_AUF_REFERENZIERTE_DATEN liefert deren Primärschlüssel
   * (3) POINTER_AUF_SIMPLE_DATEN gibt simple Werte zurück
   * (4) POINTER_AUF_EXPANDIERTE_DATEN gibt Werte der simplen Membervariablen und Primärschlüssel referenzierter Member zurück. Listen und komplexe Membervariablen werden ausgespart.
   * (5) POINTER_AUF_DATEN.* liefert rekursiv alle simplen und expandierten Membervariablen zurück, sowie alle Primärschlüssel referenzierter Membervariablen.
   * (6) Listen analog mit Zusatz []
   */
  private List<QualifiedStorableColumnInformation> resolveColumns(XMOMStorableStructureInformation root, List<String> unresolvedColumns, AliasDictionary dictionary) {
    Map<String, QualifiedStorableColumnInformation> resolvedColumns = new HashMap<String, QualifiedStorableColumnInformation>();
    if (unresolvedColumns != null) {
      List<String> unresolvedTransformedColumns = new ArrayList<String>();
      for (String column : unresolvedColumns) {
        if (column.equals("%0%")) { // (1)
          unresolvedTransformedColumns.add("%0%." + root.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER).getColumnName());
          if (root.usesHistorization()) {
            unresolvedTransformedColumns.add("%0%." + root.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP).getColumnName());
          }
          unresolvedTransformedColumns.add("%0%." + root.getColInfoByVarType(VarType.TYPENAME).getColumnName());
        } else if (column.contains("[]")) { // (6)
          unresolvedTransformedColumns.add(column.replaceAll("\\[\\]", ""));
        } else {
          unresolvedTransformedColumns.add(column);
        }
      }
      if (unresolvedTransformedColumns.size() > 0) {
        for (StorableColumnInformation column : getManagementColumns(root)) {
          List<StorableColumnInformation> emptyList = Collections.emptyList();
          resolvedColumns.put(dictionary.getOrCreateColumnAlias(column, emptyList), new QualifiedStorableColumnInformation(column, emptyList));
        }
      }
      for (String variableAccess : unresolvedTransformedColumns) {
        String[] variableAccessParts = splitVariableAccess(variableAccess);
        if (variableAccessParts.length < 2) {
          throw new RuntimeException("Invalid varAccess: " + variableAccess);
        }
        
        StorableStructureInformation currentContext = root;
        String relativePathToParentXMOMStorable = "";
        List<StorableColumnInformation> accessStack = new ArrayList<StorableColumnInformation>();

        for (int currentPosition = 1; currentPosition < variableAccessParts.length; currentPosition++) {
          String currentAccessPart = variableAccessParts[currentPosition];
          if (currentAccessPart.contains(CAST_MARKER_START)) {
            String columnAndCast = currentAccessPart;
            currentAccessPart = columnAndCast.substring(0, columnAndCast.indexOf(CAST_MARKER_START));
            String dynamicType = columnAndCast.substring(columnAndCast.indexOf(CAST_MARKER_START) + CAST_MARKER_START.length(),
                                                         columnAndCast.length() - 1);
            Set<StorableStructureInformation> subs = currentContext.getSubEntriesRecursivly();
            subs.add(currentContext);
            for (StorableStructureInformation sub : subs) {
              if (sub.getFqClassNameForDatatype().equals(dynamicType)) {
                currentContext = sub;
              }
            }
          }
          if (currentAccessPart.equals("*")) { // (5)
            List<QualifiedStorableColumnInformation> columns = currentContext.getColumnInfoAndPath(ColumnInfoRecursionMode.IDENTITY_ONLY_FOR_REFERENCES);
            for (QualifiedStorableColumnInformation entry : columns) {
              List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
              newAccessStack.addAll(entry.getAccessPath());
              if (entry.getColumn().getDefinitionSite() != VarDefinitionSite.DATATYPE) {
                resolvedColumns.put(dictionary.getOrCreateColumnAlias(entry.getColumn(), newAccessStack),
                                    new QualifiedStorableColumnInformation(entry.getColumn(), newAccessStack));
              }
            }
            // if we end with * on a referenced list we have to extend the retrieved columns onto the referenced storable and not terminate in the synthetic table
            if (currentContext.isReferencedList()) {
              for (StorableColumnInformation column : currentContext.getColumnInfo(false)) {
                if (column.isStorableVariable()) {
                  columns = column.getStorableVariableInformation().getColumnInfoAndPath(ColumnInfoRecursionMode.IDENTITY_ONLY_FOR_REFERENCES);
                  for (QualifiedStorableColumnInformation entry : columns) {
                    List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
                    newAccessStack.add(column);
                    newAccessStack.addAll(entry.getAccessPath());
                    if (entry.getColumn().getDefinitionSite() != VarDefinitionSite.DATATYPE) {
                      resolvedColumns.put(dictionary.getOrCreateColumnAlias(entry.getColumn(), newAccessStack),
                                          new QualifiedStorableColumnInformation(entry.getColumn(), newAccessStack));
                    }
                  }
                }
              }
            }
          } else {
            if (relativePathToParentXMOMStorable.length() > 0) {
              relativePathToParentXMOMStorable += ".";
            }
            relativePathToParentXMOMStorable += currentAccessPart;

            StorableColumnInformation info = currentContext.getColumnInfoByName(currentAccessPart);
            if (info == null && currentContext.isReferencedList()) {
              StorableColumnInformation forwardReference = currentContext.getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
              StorableStructureInformation resolvedContext = forwardReference.getStorableVariableInformation();
              StorableColumnInformation resolvedInfo = resolvedContext.getColumnInfoByName(currentAccessPart);
              if (resolvedInfo != null) {
                currentContext = resolvedContext;
                info = resolvedInfo;
                accessStack.add(forwardReference);
              }
            }
            if (info == null) {
              //evtl wegen flattening nichts zu finden. vom flatteningtransformator wurden aber ja vollständige pfade bereits ersetzt. d.h. es kann sich
              //höchstens um einen teilweisen pfad handeln. z.b. ist a.b.c.d eine in a geflattete referenz. dann könnte der accesspath a.b.c.* oder a,b.c sein.
              boolean endsWithStar = variableAccessParts[variableAccessParts.length - 1].equals("*");
              for (int j = currentPosition + 1; j < variableAccessParts.length - (endsWithStar ? 1 : 0); j++) {
                relativePathToParentXMOMStorable += "." + variableAccessParts[j];
              }
              boolean foundFlatMember = false;
              for (QualifiedStorableColumnInformation entry : currentContext.getColumnInfoAndPath(ColumnInfoRecursionMode.ONLY_LOCAL)) {
                if (entry.getColumn().isFlattened() && entry.getColumn().getPath().startsWith(relativePathToParentXMOMStorable + ".")) {
                  foundFlatMember = true;
                  //geflachte member die einen tieferen pfad haben, nur dann hinzufügen, wenn accessPath mit * endet.
                  if (endsWithStar || 
                      flattenedColNeedsToBeIncluded(entry.getColumn(), relativePathToParentXMOMStorable)) {

                    if (entry.getColumn().isStorableVariable()) {
                      if (entry.getColumn().getStorableVariableType() == StorableVariableType.REFERENCE) {
                        List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
                        newAccessStack.addAll(entry.getAccessPath());
                        addReferenceIdentifiersToMap(resolvedColumns, entry.getColumn(), newAccessStack, dictionary);
                      } else {
                        //expandierte typen gibts bei flattening nur listen
                        
                        //falls !endsWithStar ist die liste konkret markiert, dann nicht rekursiv suchen
                        ColumnInfoRecursionMode recursionMode;
                        if (!endsWithStar) {
                          recursionMode = ColumnInfoRecursionMode.ONLY_LOCAL;
                        } else {
                          recursionMode = ColumnInfoRecursionMode.FULL_RECURSIVE;
                        }
                        for (QualifiedStorableColumnInformation subEntry : entry.getColumn().getStorableVariableInformation().getColumnInfoAndPath(recursionMode)) {
                          if (subEntry.getColumn().getDefinitionSite() != VarDefinitionSite.DATATYPE) {
                            List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
                            newAccessStack.addAll(entry.getAccessPath());
                            newAccessStack.add(entry.getColumn());
                            newAccessStack.addAll(subEntry.getAccessPath());
                            resolvedColumns.put(dictionary.getOrCreateColumnAlias(subEntry.getColumn(), newAccessStack),
                                                new QualifiedStorableColumnInformation(subEntry.getColumn(), newAccessStack));
                          }
                        }
                      }
                    } else {
                      List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
                      newAccessStack.addAll(entry.getAccessPath());
                      resolvedColumns.put(dictionary.getOrCreateColumnAlias(entry.getColumn(), newAccessStack),
                                          new QualifiedStorableColumnInformation(entry.getColumn(), newAccessStack));
                    }
                  }
                }
              }
              if (!foundFlatMember) {
                throw new RuntimeException("Column " + relativePathToParentXMOMStorable + " does not exist in " + root.getFqXmlName());
              }
              break;
            } else if (currentPosition + 1 >= variableAccessParts.length) {
              //letzter part
              if (info.getDefinitionSite() == VarDefinitionSite.DATATYPE) {
                if (info.isStorableVariable()) {
                  if (info.getStorableVariableType() == StorableVariableType.REFERENCE) { // (2)
                    addReferenceIdentifiersToMap(resolvedColumns, info, accessStack, dictionary);
                  } else { // (4)
                    Set<StorableColumnInformation> subColumns = info.getStorableVariableInformation().getColumnInfo(false);
                    for (StorableColumnInformation subColumn : subColumns) {
                      if (!subColumn.isList()) {
                        if (subColumn.isFlattened() && subColumn.getPath().contains(".")) { //flatteningpath ist hier relativ zum info-storable.
                          //nicht adden, weil ist in wirklichkeit ein member mindestens eine hierarchieebene weiter
                        } else if (subColumn.isStorableVariable() && subColumn.getStorableVariableType() == StorableVariableType.REFERENCE) {
                          List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
                          newAccessStack.add(info);
                          newAccessStack.add(subColumn);
                          StorableColumnInformation uidCol =
                              subColumn.getStorableVariableInformation()
                                  .getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
                          resolvedColumns.put(dictionary.getOrCreateColumnAlias(uidCol, newAccessStack),
                                              new QualifiedStorableColumnInformation(uidCol, newAccessStack));
                          StorableColumnInformation pkCol = subColumn.getStorableVariableInformation().getColInfoByVarType(VarType.PK);
                          resolvedColumns.put(dictionary.getOrCreateColumnAlias(pkCol, newAccessStack),
                                              new QualifiedStorableColumnInformation(pkCol, newAccessStack));
                          StorableColumnInformation typeCol = subColumn.getStorableVariableInformation().getColInfoByVarType(VarType.TYPENAME);
                          resolvedColumns.put(dictionary.getOrCreateColumnAlias(typeCol, newAccessStack),
                                              new QualifiedStorableColumnInformation(typeCol, newAccessStack));
                        } else if (subColumn.getPrimitiveType() != null) {
                          List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
                          newAccessStack.add(info);
                          resolvedColumns.put(dictionary.getOrCreateColumnAlias(subColumn, newAccessStack),
                                              new QualifiedStorableColumnInformation(subColumn, newAccessStack));
                        }
                      }
                    }
                  }
                }
              } else if (!info.isPersistenceType(PersistenceTypeInformation.TRANSIENCE)) { // (3)
                resolvedColumns.put(dictionary.getOrCreateColumnAlias(info, accessStack),
                                    new QualifiedStorableColumnInformation(info, accessStack));
              }
            } else {
              if (!info.isStorableVariable()) {
                throw new RuntimeException("Invalid varAccess: " + variableAccess + " at position '" + currentAccessPart + "'.");
              }
              currentContext = info.getStorableVariableInformation();
              accessStack.add(info);
              for (StorableColumnInformation column : getManagementColumns(currentContext)) {
                resolvedColumns.put(dictionary.getOrCreateColumnAlias(column, accessStack),
                                    new QualifiedStorableColumnInformation(column, accessStack));
              }
              relativePathToParentXMOMStorable = "";
            }
          }
        }
      }
    }
    if (resolvedColumns.size() == 0) {
      for (QualifiedStorableColumnInformation entry : root.getColumnInfoAndPath(ColumnInfoRecursionMode.FULL_RECURSIVE)) {
        if (entry.getColumn().getDefinitionSite() != VarDefinitionSite.DATATYPE) {
          resolvedColumns.put(dictionary.getOrCreateColumnAlias(entry.getColumn(), entry.getAccessPath()), entry);
        }
      }
    }
    return CollectionUtils.transform(resolvedColumns.entrySet(), 
             new Transformation<Entry<String, QualifiedStorableColumnInformation>, QualifiedStorableColumnInformation>() {
              public QualifiedStorableColumnInformation transform(Entry<String, QualifiedStorableColumnInformation> from) {
                return from.getValue();
              }
            });
  }
  

  private String[] splitVariableAccess(String variableAccess) {
    List<String> variableAccessParts = new ArrayList<>();
    boolean insideCast = false;
    int previousIndex = 0;
    int index = variableAccess.indexOf('.', previousIndex);
    while (index >= 0) {
      String variableAccessPart = variableAccess.substring(previousIndex, index);
      if (insideCast) {
        String previousPart = variableAccessParts.remove(variableAccessParts.size() - 1);
        if (variableAccessPart.contains(CAST_MARKER_END)) {
          insideCast = false;
        }
        variableAccessPart = previousPart + "." + variableAccessPart;
        variableAccessParts.add(variableAccessPart);
      } else {
        variableAccessParts.add(variableAccessPart);
        if (variableAccessPart.contains(CAST_MARKER_START)) {
          insideCast = true;
        }
      }
      
      previousIndex = index + 1; // skip the dot
      index = variableAccess.indexOf('.', previousIndex);
    }
    variableAccessParts.add(variableAccess.substring(previousIndex));
    return variableAccessParts.toArray(new String[0]);
  }


  private void addReferenceIdentifiersToMap(Map<String, QualifiedStorableColumnInformation> resolvedColumns, StorableColumnInformation referenceColumn,
                                            List<StorableColumnInformation> accessStack, AliasDictionary dictionary) {
    List<StorableColumnInformation> newAccessStack = new ArrayList<StorableColumnInformation>(accessStack);
    newAccessStack.add(referenceColumn);
    XMOMStorableStructureInformation subInfo = (XMOMStorableStructureInformation) referenceColumn.getStorableVariableInformation();
    StorableColumnInformation uidCol = subInfo.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
    resolvedColumns.put(dictionary.getOrCreateColumnAlias(uidCol, newAccessStack),
                        new QualifiedStorableColumnInformation(uidCol, newAccessStack));
    StorableColumnInformation typeCol = subInfo.getColInfoByVarType(VarType.TYPENAME);
    resolvedColumns.put(dictionary.getOrCreateColumnAlias(typeCol, newAccessStack),
                        new QualifiedStorableColumnInformation(typeCol, newAccessStack));
    if (subInfo.usesHistorization()) {
      StorableColumnInformation histoCol = subInfo
                      .getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
      resolvedColumns.put(dictionary.getOrCreateColumnAlias(histoCol, newAccessStack),
                          new QualifiedStorableColumnInformation(histoCol, newAccessStack));
      StorableColumnInformation curVerCol = subInfo
                      .getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG);
      resolvedColumns.put(dictionary.getOrCreateColumnAlias(curVerCol, newAccessStack),
                          new QualifiedStorableColumnInformation(curVerCol, newAccessStack));
      StorableColumnInformation pkCol = subInfo.getColInfoByVarType(VarType.PK);
      resolvedColumns.put(dictionary.getOrCreateColumnAlias(pkCol, newAccessStack),
                          new QualifiedStorableColumnInformation(pkCol, newAccessStack));
    }
  }
  
  /**
   * voraussetzung: selectionmask zeigt auf den relativen pfad (ohne *), und die column liegt irgendwo in dieser teil-hierarchie,
   * @eturn true falls die column direkt dem pfad entspricht, oder
   *             falls die column referenz oder simple type ist UND maximal 1 hierarchiestufe unterhalb liegt
   */
  private boolean flattenedColNeedsToBeIncluded(StorableColumnInformation column, String relativePathToParentXMOMStorable) {
    if (column.getPath().equals(relativePathToParentXMOMStorable)) {
      return true;
    }
    if (!column.getPath().substring(relativePathToParentXMOMStorable.length() + 1).contains(".")
        && !column.isList()) {
      if (column.isStorableVariable()) {
        if (column.getStorableVariableType() == StorableVariableType.REFERENCE) {
          return true;
        }
      } else {
        return true;
      }
    }
    return false;
  }



  private List<StorableColumnInformation> getManagementColumns(StorableStructureInformation info) {
    List<StorableColumnInformation> storableOnlyColumnsToAdd = new ArrayList<StorableColumnInformation>();
    storableOnlyColumnsToAdd.add(info.getColInfoByVarType(VarType.PK));
    StorableColumnInformation typeName = info.getColInfoByVarType(VarType.TYPENAME);
    if (typeName != null) {
      storableOnlyColumnsToAdd.add(typeName);
    }
    StorableColumnInformation listIdx = info.getColInfoByVarType(VarType.LIST_IDX);
    if (listIdx != null) {
      storableOnlyColumnsToAdd.add(listIdx);
    }
    StorableColumnInformation parentFk = info.getColInfoByVarType(VarType.UTILLIST_PARENT_FK);
    if (parentFk == null) {
      parentFk = info.getColInfoByVarType(VarType.EXPANSION_PARENT_FK);
    }
    if (parentFk != null) {
      storableOnlyColumnsToAdd.add(parentFk);
    }
    return storableOnlyColumnsToAdd;
  }
  
  
  @SuppressWarnings("rawtypes")
  private ResultSetReader buildReaderFromQueryTree(QueryTreeNode node, List<QualifiedStorableColumnInformation> columns, AliasDictionary dictionary) {
    Map<String, String> columnNameAliases = new HashMap<String, String>();
    for (QualifiedStorableColumnInformation entry : columns) {
      columnNameAliases.put(generateColumnAliasIdentifier(dictionary.getTableAlias(entry.getAccessPath()), entry.getColumn().getColumnName()),
                            dictionary.getOrCreateColumnAlias(entry));
    }
    return buildReaderFromQueryTreeNode(node, columnNameAliases, columns, dictionary);
  }
  
  
  static String generateColumnAliasIdentifier(String tableName, String columnName) {
    return tableName + "_" + columnName;
  }
  
  
  @SuppressWarnings("rawtypes")
  private StorableNodeReader buildReaderFromQueryTreeNode(QueryTreeNode node, Map<String, String> columnNameAliases, List<QualifiedStorableColumnInformation> selection, AliasDictionary dictionary) {
    boolean skipChildren = false;
    ResultSetReader localReader = node.localInfo.getResultSetReaderForDatatype();
    if (node.localInfo.isSyntheticStorable()) { // info for utilityStorable as used by primitive or referenced lists
      if (node.localInfo.getPossessingColumn().isList() && node.localInfo.getPossessingColumn().getPrimitiveType() != null) { // primitive list
        for (StorableColumnInformation column : node.localInfo.getColumnInfo(false)) {
          if (column.getPrimitiveType() != null && column.getType() == VarType.DEFAULT) {
            localReader = new SingleColumnReader(column);
            break;
          }
        }
      } else { // complex list
        if (node.joinChildren != null && node.joinChildren.size() > 0) {
          QueryTreeNode joinChild = node.joinChildren.iterator().next();
          localReader = buildReaderFromQueryTreeNode(joinChild, columnNameAliases, selection, dictionary);
        } else {
          localReader = new ResultSetReader() {
            public Object read(ResultSet rs) throws SQLException { // null reader
              return null;
            }
          };
        }
        skipChildren = true;
      }
    }
    
    
    List<ChildReaderData> childReaders = new ArrayList<ChildReaderData>();
    if (!skipChildren) {
      for (QueryTreeNode joinChild : node.joinChildren) {
          ChildReaderData data = 
            new ChildReaderData(joinChild.localInfo.getColInfoByVarType(VarType.PK).getColumnName(),
                                dictionary.getTableAlias(joinChild.accessPath),
                                joinChild.accessPath.get(joinChild.accessPath.size() - 1).getColumnName(),
                                dictionary.getTableAlias(joinChild.accessPath.subList(0, joinChild.accessPath.size() - 1)),
                                buildReaderFromQueryTreeNode(joinChild, columnNameAliases, selection, dictionary),
                                countSublists(joinChild.localInfo, false));
          childReaders.add(data);
      }
      
      
      
      for (QueryTreeNode unionChild : node.unionChildren) {
        ChildReaderData data = new ChildReaderData(unionChild.accessPath.get(unionChild.accessPath.size() - 1).getParentStorableInfo().getColInfoByVarType(VarType.PK).getColumnName(),
                                                   dictionary.getTableAlias(unionChild.accessPath.subList(0, unionChild.accessPath.size() - 1)),
                                                   unionChild.accessPath.get(unionChild.accessPath.size() - 1).getColumnName(),
                                                   dictionary.getTableAlias(unionChild.accessPath.subList(0, unionChild.accessPath.size() - 1)),
                                                   buildReaderFromQueryTreeNode(unionChild, columnNameAliases, selection, dictionary),
                                                   countSublists(unionChild.localInfo, true));
        childReaders.add(data);
      }
    }
    
    String tableName = dictionary.getTableAlias(node.accessPath);
    if (node.localInfo.getPossessingColumn() != null && node.localInfo.getPossessingColumn().isList()) {
      StorableColumnInformation rootIdentifier = node.localInfo.getColInfoByVarType(VarType.EXPANSION_PARENT_FK );
      if (rootIdentifier == null) {
        rootIdentifier = node.localInfo.getColInfoByVarType(VarType.UTILLIST_PARENT_FK);
      }
      StorableColumnInformation localIndexIdentifier = node.localInfo.getColInfoByVarType(VarType.LIST_IDX);
      if (rootIdentifier == null) {
        throw new RuntimeException("no root identifier!");
      }
      if (localIndexIdentifier == null) {
        throw new RuntimeException("no localIndexIdentifier identifier!");
      }
      return new StorableNodeReader(tableName, columnNameAliases, childReaders, localReader,
                                    rootIdentifier.getColumnName(), localIndexIdentifier.getColumnName());
    } else {
      return new StorableNodeReader(tableName, columnNameAliases, childReaders, localReader);
    }
  }
  
  
  private boolean isQuerySplitNecessary(XMOMStorableStructureInformation root, Collection<QualifiedStorableColumnInformation> columns) {
    for (QualifiedStorableColumnInformation column : columns) {
      for (StorableColumnInformation pathPart : column.getAccessPath()) {
        if (pathPart.isList()) {
          return true;
        }
      }
    }
    return false;
  }
  
  
  private void appendConditionOnCurrentVersionIfNecessary(StringBuilder queryBuilder, Parameter params, XMOMStorableStructureInformation info,
                                                          FormulaParsingResult fpr, QueryParameter queryParameter) {
    if (info.usesHistorization() && !queryParameter.queryAcrossHistory()) {
      if (fpr.getColumnsFromConditions() != null &&
          fpr.getColumnsFromConditions().size() > 0) {
        for (QualifiedStorableColumnInformation column : fpr.getColumnsFromConditions()) {
          if (column.getColumn().isPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG) ||
              column.getColumn().isPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP)) {
            return;
          }
        }
      }
      if (fpr.getColumnsFromConditions().size() <= 0) {
        queryBuilder.append(" WHERE ");
      } else {
        queryBuilder.append(" AND ");
      }
      String columnName = info.getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG).getColumnName();
      queryBuilder.append(escape.apply(info.getTableName()))
                  .append(".")
                  .append(escape.apply(columnName))
                  .append(" = ?");
      params.add(true);
    }
  }
  
  
  private int countSublists(StorableStructureInformation info, boolean isList) {
    final AtomicInteger sublistCounter = new AtomicInteger(isList ? 1 : 0);
    info.traverse(new StorableStructureVisitor() {
      
      public StorableStructureRecursionFilter getRecursionFilter() {
        return XMOMStorableStructureCache.ALL_RECURSIONS;
      }
      
      public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) { }
      
      public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
        if (columnLink != null && columnLink.isList()) {
          sublistCounter.incrementAndGet();
        }
      }
    });
    return sublistCounter.get();
  }
  
  
  private static class QueryTreeNode {
    
    List<StorableColumnInformation> accessPath;
    StorableStructureInformation localInfo;
    List<QueryTreeNode> joinChildren;
    List<QueryTreeNode> unionChildren;
    
    QueryTreeNode(StorableStructureInformation localInfo, List<StorableColumnInformation> accessPath,
                  List<QueryTreeNode> joinChildren, List<QueryTreeNode> unionChildren) {
      this.localInfo = localInfo;
      this.joinChildren = joinChildren;
      this.unionChildren = unionChildren;
      this.accessPath = accessPath;
    }
    
    
    String print(String indentation) {
      StringBuilder sb = new StringBuilder();
      sb.append(indentation).append(localInfo.getFqXmlName()).append(" [").append(localInfo.getTableName()).append("]\n");
      if (joinChildren != null && joinChildren.size() > 0) {
        sb.append(indentation).append("joins:").append("\n");
        for (QueryTreeNode node : joinChildren) {
          sb.append(node.print(indentation + "  "));
        }
      }
      if (unionChildren != null && unionChildren.size() > 0) {
        sb.append(indentation).append("unions:").append("\n");
        for (QueryTreeNode node : unionChildren) {
          sb.append(node.print(indentation + "  "));
        }
      }
      return sb.toString();
    }
    
  }
  
  
  static abstract class QueryPiplineElement<P, T> {
    
    private QueryPiplineElement<T, ?> next;
    protected String sqlString;
    protected ResultSetReader<T> reader;
    protected Parameter params;
    protected AliasDictionary dictionary;
    protected List<QualifiedStorableColumnInformation> selectedColumns;
    protected int maxObjects;
    protected List<T> interimResult;
    protected String rootTableName;
    
    
    
    QueryPiplineElement(String sqlString, ResultSetReader<T> reader, int maxObjects, Parameter params) {
      this.sqlString = sqlString;
      this.reader = reader;
      this.params = params;
      this.maxObjects = maxObjects;
    }
    
    QueryPiplineElement(String sqlString, ResultSetReader<T> reader, int maxObjects) {
      this(sqlString, reader, maxObjects, new Parameter());
    }
    
    
    public QueryPiplineElement<T, ?> getNext(List<T> previousResult) {
      interimResult = postProcess(previousResult);
      if (next != null) {
        next.prepare(interimResult);
      }
      return next;
    }
    
    public boolean hasNext() {
      return next != null;
    }
    
    
    protected abstract void prepare(Collection<P> previousResult);
    
    protected abstract List<T> postProcess(List<T> result);
    
    ;
    public void setNext(QueryPiplineElement<T, ?> next) {
      this.next = next;
    }

    
    public String getSqlString() {
      return sqlString;
    }

    
    public ResultSetReader<T> getReader() {
      return reader;
    }

    
    public Parameter getParams() {
      return params;
    }
    
    
    public AliasDictionary getAliasDictionary() {
      return dictionary;
    }
    
    
    public void setAliasDictionary(AliasDictionary dictionary) {
      this.dictionary = dictionary;
    }
    
    public List<QualifiedStorableColumnInformation> getSelectedColumns() {
      return selectedColumns;
    }
    
    
    public void setSelectedColumns(List<QualifiedStorableColumnInformation> selectedColumns) {
      this.selectedColumns = selectedColumns;
    }

    public int getMaxObjects() {
      return maxObjects;
    }
    
    public List<T> getInterimResult() {
      return interimResult;
    }
    
    public String getRootTableName() {
      return rootTableName;
    }
    
  }
  
  
  static class FirstQueryPiplineElement<P, T>  extends QueryPiplineElement<P, T> {

    FirstQueryPiplineElement(String sqlString, ResultSetReader<T> reader, int maxObjects) {
      super(sqlString, reader, maxObjects);
    }
    
    FirstQueryPiplineElement(String sqlString, ResultSetReader<T> reader, int maxObjects, Parameter params) {
      super(sqlString, reader, maxObjects, params);
    }

    @Override
    protected void prepare(Collection<P> previousResult) {
      // ntbd
    }
    
    
    @Override
    protected List<T> postProcess(List<T> result) {
      List<T> list = new ArrayList<T>();
      for (T t : result) {
        if (t != null) {
          list.add(t);
        }
      }
      return list;
    }
    
  }
  

  private static String PQC /* possibly qualified column - regexp pattern */ = "[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?";
  public static String PLACE_FOR_WHERE_PATTERN = "\\s+FROM\\s+"+PQC+"(((\\s+(LEFT|RIGHT))?\\s+((INNER|OUTER)\\s+)?)?\\s*JOIN\\s+"+PQC+"(\\s+[a-zA-Z0-9_]+)?\\s+ON\\s+"+PQC+"\\s*=\\s*"+PQC+")*";
  
  static class DataQueryPiplineElement<P, T>  extends QueryPiplineElement<P, T> {
    
    private final Function<String, String> escape;

    private static Pattern placeForWhere = 
      Pattern.compile(PLACE_FOR_WHERE_PATTERN, Pattern.CASE_INSENSITIVE);
    
    private final StorableColumnInformation rootPk;
    
    DataQueryPiplineElement(String sqlString, ResultSetReader<T> reader, StorableColumnInformation rootPk, Function<String, String> escape) {
      this(sqlString, reader, new Parameter(), rootPk, escape);
    }
    
    DataQueryPiplineElement(String sqlString, ResultSetReader<T> reader,  Parameter params, StorableColumnInformation rootPk, Function<String, String> escapeString) {
      super(sqlString, reader, -1, params);
      this.rootPk = rootPk;
      this.escape = escapeString;
    }
    

    @Override
    protected void prepare(Collection<P> previousResult) {
      StringBuilder conditionBuilder = new StringBuilder();
      String escTableName = escape.apply(rootPk.getParentStorableInfo().getTableName());
      String escColName = escape.apply(rootPk.getColumnName());
      conditionBuilder.append(" WHERE ").append(escTableName).append(".").append(escColName);
      if (previousResult.size() > 1) {
        conditionBuilder.append(" IN (");
        Iterator<P> previousResultIterator = previousResult.iterator();
        while (previousResultIterator.hasNext()) {
          previousResultIterator.next();
          conditionBuilder.append("?");
          if (previousResultIterator.hasNext()) {
            conditionBuilder.append(", ");
          }
        }
        conditionBuilder.append(") ");
      } else {
        conditionBuilder.append(" = ? "); 
      }
      
      StringBuffer queryBuffer = new StringBuffer();
      Matcher m = placeForWhere.matcher(sqlString); // teuer
      int k = 0;
      while (m.find()) {
        m.appendReplacement(queryBuffer, m.group(0) + conditionBuilder.toString());
        k++;
      }
      List<Object> paramsList;
      if (params != null) {
        paramsList = new ArrayList<>(previousResult.size() * k + params.size());
        for (int i = 0; i<params.size(); i++) {
          paramsList.add(params.get(i));
        }
      } else {
        paramsList = new ArrayList<>(previousResult.size() * k);
      }
      for (int i = 0; i < k; i++) {
        for (P t : previousResult) {
          paramsList.add(t);
        }
      }
      params = new Parameter(paramsList.toArray());
      m.appendTail(queryBuffer);
      this.sqlString = queryBuffer.toString();
    }

    @Override
    protected List<T> postProcess(List<T> result) {
      return result;
    }
    
  }
  
  
  // dictionary is appearing in the sampler...we could cache those...
  static class AliasDictionary {
    
    private int tableCounter = 0;
    private int columnCounter = 0;
    private AliasDictionaryNode root;
    
    AliasDictionary(XMOMStorableStructureInformation rootInfo) {
      this.root = new AliasDictionaryNode(rootInfo.getTableName());
    }
    
    
    String getTableAlias(List<StorableColumnInformation> storableVariablePath) {
      return getDictionaryNode(storableVariablePath).tableAlias;
    }
    
    String getOrCreateColumnAlias(QualifiedStorableColumnInformation qsci) {
      return getOrCreateColumnAlias(qsci.getColumn(), qsci.getAccessPath());
    }
    
    String getOrCreateColumnAlias(StorableColumnInformation column, List<StorableColumnInformation> storableVariablePath) {
      AliasDictionaryNode node = getDictionaryNode(storableVariablePath);
      Pair<StorableColumnInformation, String> entry = node.columnAliasMap.get(column.getColumnName());
      String columnAlias;
      if (entry == null) {
        columnAlias = null;
      } else {
        columnAlias = entry.getSecond();
      }
      if (columnAlias == null) {
        columnAlias = generateColumnName();
        node.columnAliasMap.put(column.getColumnName(), Pair.of(column, columnAlias));
        debugAlias(storableVariablePath, column, node, columnAlias);
      }
      return columnAlias;
    }
    

    private void debugAlias(List<StorableColumnInformation> storableVariablePath, StorableColumnInformation column,
                            AliasDictionaryNode node, String alias) {
      if (logger.isTraceEnabled()) {
        StringBuilder sb = new StringBuilder();
        for (StorableColumnInformation path : storableVariablePath) {
          sb.append(path.getColumnName()).append('.');
        }
        sb.append(column.getColumnName()).append(" ---> ").append(node.tableAlias + "." + alias);
        trace(sb.toString());
      }
    }


    String getColumnAlias(QualifiedStorableColumnInformation qsci) {
      return getColumnAlias(qsci.getColumn(), qsci.getAccessPath());
    }
    
    
    String getColumnAlias(StorableColumnInformation column, List<StorableColumnInformation> storableVariablePath) {
      AliasDictionaryNode node = getDictionaryNode(storableVariablePath);
      Pair<StorableColumnInformation, String> entry = node.columnAliasMap.get(column.getColumnName());
      String columnAlias;
      if (entry == null) {
        columnAlias = null;
      } else {
        columnAlias = entry.getSecond();
      }
      if (columnAlias == null) {
        columnAlias = column.getParentStorableInfo().getTableName() + "." + column.getColumnName();
      }
      return columnAlias;
    }
    
    
    private String generateTableAlias() {
      return "tbl"+tableCounter++;
    }
    
    private String generateColumnName() {
      return "col"+columnCounter++;
    }
    
    private AliasDictionaryNode getDictionaryNode(List<StorableColumnInformation> storableVariablePath) {
      AliasDictionaryNode current = root;
      for (int i = 0; i < storableVariablePath.size(); i++) {
        AliasDictionaryNode next = current.children.get(storableVariablePath.get(i).getColumnName());
        if (next == null) {
          next = new AliasDictionaryNode(generateTableAlias());
          current.children.put(storableVariablePath.get(i).getColumnName(), next);
        }
        current = next;
      }
      return current;
    }
    
  }

  
  private static void trace(String s) {
    logger.trace(s);
  }

  
  static class AliasDictionaryNode {
    
    private final String tableAlias;
    private final Map<String, Pair<StorableColumnInformation, String>> columnAliasMap = new HashMap<String, Pair<StorableColumnInformation, String>>();
    private final Map<String, AliasDictionaryNode> children = new HashMap<String, AliasDictionaryNode>();
    
    AliasDictionaryNode(String tableAlias) {
      this.tableAlias = tableAlias;
    }
    
  }
  
  
  static class QualifiedStorableColumnInformation {
    
    private final StorableColumnInformation column;
    private final List<StorableColumnInformation> accessPath;
    
    QualifiedStorableColumnInformation(StorableColumnInformation column, List<StorableColumnInformation> accessPath) {
      this.column = column;
      this.accessPath = new ArrayList<StorableColumnInformation>(accessPath);
    }
    
    
    StorableColumnInformation getColumn() {
      return column;
    }


    List<StorableColumnInformation> getAccessPath() {
      return accessPath;
    }


    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof QualifiedStorableColumnInformation)) {
        return false;
      }
      QualifiedStorableColumnInformation o = (QualifiedStorableColumnInformation) obj;
      if (Objects.equals(getColumn().getColumnName(), o.getColumn().getColumnName())) {
        if (getAccessPath().size() != o.getAccessPath().size()) {
          return false;
        } else {
          for (int i = 0; i < getAccessPath().size(); i++) {
            if (!Objects.equals(getAccessPath().get(i).getColumnName(), o.getAccessPath().get(i).getColumnName())) {
              return false;
            }
          }
          return true;
        }
      } else {
        return false;
      }
    }
    
    @Override
    public int hashCode() {
      return getColumn().getColumnName().hashCode();
    }
    
  }
}
