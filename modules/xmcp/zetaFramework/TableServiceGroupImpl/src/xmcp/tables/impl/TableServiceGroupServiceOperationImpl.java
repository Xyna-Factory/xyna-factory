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
package xmcp.tables.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

import base.Text;
import xmcp.tables.TableServiceGroupServiceOperation;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.tables.datatypes.query.QueryColumns;
import xmcp.tables.datatypes.query.QueryMemberFilterCondition;
import xmcp.tables.datatypes.query.QueryOnlySelectedColumns;
import xmcp.zeta.TableHelper;
import xnwh.persistence.FilterCondition;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SortCriterion;
import xprc.xpce.datatype.DatatypeInspector;
import xprc.xpce.datatype.NamedVariableMember;


public class TableServiceGroupServiceOperationImpl implements ExtendedDeploymentTask, TableServiceGroupServiceOperation {

  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public FilterCondition buildCombinedFilterCondition(QueryMemberFilterCondition queryMemberFilterCondition, FilterCondition filterCondition) {
    // build primary_filter_condition_string from queryMemberFilterCondition
    String primaryFilterConditionString = "";

    String operator = queryMemberFilterCondition.getOperator();
    String path = queryMemberFilterCondition.getPath();
    String value = queryMemberFilterCondition.getValue();

    if (operator != null) {
      if (operator.equals("=") || operator.equals("==") || operator.equalsIgnoreCase("EQUALS")) {
        primaryFilterConditionString = "%0%." + path + "==\"" + value + "\"";
      } else if (operator.equals("!=") || operator.equals("!==") || operator.equalsIgnoreCase("NOT EQUALS")) {
        primaryFilterConditionString = "%0%." + path + "!=\"" + value + "\"";
      } else if (operator.equalsIgnoreCase("CONTAINS")) {
        primaryFilterConditionString = "glob(%0%." + path + ",\"*" + value + "*\")";
      }
    }

    // extract additional_filter_condition_string from filterCondition
    String additionalFilterConditionString = filterCondition.getFormula();

    // combine filter condition strings
    String resultFilterConditionString = primaryFilterConditionString;
    if (!resultFilterConditionString.isEmpty() && !additionalFilterConditionString.isEmpty()) {
      resultFilterConditionString += " && ";
    }
    resultFilterConditionString += additionalFilterConditionString;
    return new FilterCondition(resultFilterConditionString);
  }

  public FilterCondition buildFilterConditionFromTableInfo(TableInfo tableInfo, Text text) {
    // if a member_path is passed as input argument this member_path has to be removed (e.g. site.x, site.y, site.z => x, y, z)
    String removeMemberPath = "";
    if ((text != null) && (text.getText() != null) && !text.getText().isEmpty()) {
      removeMemberPath = text.getText() + ".";
    }

    StringBuilder filterCondition = new StringBuilder();
    for (int i = 0; i < tableInfo.getColumns().size(); ++i) {
      String filterStr = tableInfo.getColumns().get(i).getFilter();
      String memberPath = tableInfo.getColumns().get(i).getPath();

      if ((filterStr != null) && !filterStr.isEmpty() && (memberPath != null) && !memberPath.isEmpty()) {
        boolean acceptFilterConditionPart = false;
        if (removeMemberPath.isEmpty()) {
          acceptFilterConditionPart = true;
        } else {
          if (memberPath.contains(removeMemberPath)) {
            memberPath = memberPath.replaceFirst(removeMemberPath, "");
            acceptFilterConditionPart = true;
          }
        }

        if (acceptFilterConditionPart) {
          if (filterCondition.length() > 0) {
            filterCondition.append(" && ");
          }
          if(filterStr.equals("\"\"")) filterCondition.append("(%0%." + memberPath + "==\"\" || %0%." + memberPath + "==null)");
          else filterCondition.append("glob(%0%." + memberPath + ", \"*" + filterStr + "*\")");
        }
      }
    }

    return new xnwh.persistence.FilterCondition(filterCondition.toString());
  }


  public Container buildQueryParametersFromTableInfo(TableInfo tableInfo, QueryColumns queryColumns) {
    // ----------------------------------------------------------------------------------------------------------------------------------------------------------------
    // INPUT: tableInfo, queryColumns
    // ----------------------------------------------------------------------------------------------------------------------------------------------------------------
    // RETURN:  
    // - Selection Mask with TableInfo's root type
    //    queryColumns -> QueryAllColumns:          No columns are selected in the Selection Mask ( <=> take every column )
    //    queryColumns -> QueryOnlySelectedColumns: Only columns of the tableInfo Object (and optionally the additional columns given in queryColumns) are selected
    // - Combined Filter Condition for all Columns (e.g. "glob(%0%.member_path_i, "*filter_i*") && glob(%0%.member_path_k, "*filter_k*") ...")
    // - QueryParameter with a list of SortCriterion (Order: most left column first. Currently only sorting by one column is supported by the Zeta Framework.)
    // ----------------------------------------------------------------------------------------------------------------------------------------------------------------
    // REQUIRED DATATYPES: 
    // xnwh.persistence.*              (SelectionMask, FilterCondition, QueryParameter, SortCriterion)
    // xmcp.tables.datatypes.query.*   (QueryColumns, QueryAllColumns, QueryOnlySelectedColumns)
    // ----------------------------------------------------------------------------------------------------------------------------------------------------------------

    List<String> selectedColumns = new ArrayList<>();
    List<SortCriterion> sortCriteria = new ArrayList<>();

    StringBuilder filterCondition = new StringBuilder();
    for (TableColumn tableColumn : tableInfo.getColumns()) {
      String memberPath = tableColumn.getPath();

      // skip empty paths
      if ((memberPath == null) || (memberPath.isEmpty())) {
        continue;
      }

      // automatically use paths as selected columns in the selection mask    
      selectedColumns.add("%0%." + memberPath);

      // add filter condition for current column
      if (tableColumn.getDisableFilter() == false) {
        String filter = tableColumn.getFilter();
        if ((filter != null) && !filter.isEmpty()) {
          if (filterCondition.length() > 0) {
            filterCondition.append(" && ");
          }
          
          buildSingleFilter(filterCondition, memberPath, filter);
        }
      }

      // add sort criterion for current column
      if (!tableColumn.getDisableSort()) {
        String sort = tableColumn.getSort();
        if ((sort != null) && !sort.isEmpty()) {
          sort = sort.toUpperCase();
          if (sort.equals("ASC") || sort.equals("DSC")) { // only accept "ASC" or "DSC"
            SortCriterion newSortCriterion = new SortCriterion();
            newSortCriterion.setCriterion("%0%." + memberPath);
            newSortCriterion.setReverse(sort.equals("DSC"));
            sortCriteria.add(newSortCriterion);
          }
        }
      }
    }

    // =====================
    // build FilterCondition
    // =====================
    FilterCondition resultFilterCondition = new FilterCondition(filterCondition.toString());

    // ===================
    // build SelectionMask
    // ===================
    xnwh.persistence.SelectionMask resultSelectionMask = new xnwh.persistence.SelectionMask();
    resultSelectionMask.setRootType(tableInfo.getRootType());

    // Select columns only if queryColumns is set to the derived type QuerySelectedColumns
    if (queryColumns instanceof QueryOnlySelectedColumns) {
      // select additional columns
      QueryOnlySelectedColumns queryOnlySelectedColumns = (QueryOnlySelectedColumns) queryColumns; // cast queryColumns to derived datatype QueryOnlySelectedColumns
      if (queryOnlySelectedColumns.getAdditionalColumns() != null) {
        for (TableColumn tableColumn : queryOnlySelectedColumns.getAdditionalColumns()) {
          selectedColumns.add("%0%." + tableColumn.getPath());
        }
      }
      // set selected columns in selection mask
      resultSelectionMask.setColumns(selectedColumns);
    }

    // ====================
    // build QueryParameter
    // ====================
    QueryParameter resultQueryParameter = new QueryParameter();
    resultQueryParameter.setQueryHistory(false);
    resultQueryParameter.setSortCriterion(sortCriteria);
    if (tableInfo.getLimit() != null) {
      resultQueryParameter.setMaxObjects(tableInfo.getLimit());
    } else {
      resultQueryParameter.setMaxObjects(-1);
    }

    // return Container (absolute path = com.gip.xyna.xdev.xfractmod.xmdm.Container)
    return new Container(resultSelectionMask, resultFilterCondition, resultQueryParameter);
  }

  private static void buildSingleFilter(StringBuilder filterCondition, String memberPath, String filter) {
    if(filter.equals("\"\"")) {
      filterCondition.append("(%0%." + memberPath + "==\"\" || %0%." + memberPath + "==null)");
      return;
    }
    
    String[] filterArr = {filter};
    // Bugz 25212: Split by | if exists and use or
    if (filter.contains("|")) {
      filterArr = filter.split("\\|");
    }
    
    if (filterArr.length > 1) {
      filterCondition.append(" ( ");
    }
    
    for (int j=0; j<filterArr.length; j++) {
      if (j>0) {
        filterCondition.append(" || ");
      }
      
      boolean addStar = true;
      // Bugz 25212: Only add *, if not exists already
      if (filterArr[j].startsWith("\"") && filterArr[j].endsWith("\"")) {
        filterArr[j] = filterArr[j].substring(1, filterArr[j].length()-1);
        // No *
        addStar = false;
      }
      if (filterArr[j].contains("*")) {
        // No *
        addStar = false;
      }
      
      // escape "
      int i = 0;
      StringBuilder filterBuilder = new StringBuilder(filterArr[j]);
      while (i < filterBuilder.length()) {
        if (Character.toString(filterBuilder.charAt(i)).equals("\"")) {
          filterBuilder.insert(i, "\\");
          i += 2;
        } else {
          i++;
        }
      }
      if (addStar) {
        filterBuilder.insert(0, "*").append("*");
      }
      filterCondition.append("glob(%0%." + memberPath + ", \"" + filterBuilder.toString() + "\")");
    }
    
    if (filterArr.length > 1) {
      filterCondition.append(" ) ");
    }
  }
  
//  public static void main(String[] args) {
//    StringBuilder filterCondition = new StringBuilder();
//    
//    buildSingleFilter(filterCondition, "column", "\"5\"|bar*|hello|world");
//    
//    System.out.println(filterCondition.toString());
//  }
//

  public TableInfo buildTableInfoAndSelectColumns(TableInfo tableInfo, GeneralXynaObject anyType, List<? extends TableColumn> tableColumn) {
    // This service needs DOM Inspector 1.0 (XMOM path: xprc.xpce.datatype.*)

    //===============
    // initialization
    //===============

    // null protection
    if ((anyType == null) || (tableColumn == null)) {
      return new TableInfo();
    }

    // null protection
    if (tableInfo == null) {
      tableInfo = new TableInfo();
      tableInfo.setBootstrap(true);
    }

    if (tableInfo.getBootstrap()) {
      //==============================
      // generate new tableInfo Object
      //==============================
      DatatypeInspector datatypeInspector = DatatypeInspector.inspectDatatype(anyType);

      // set root type
      tableInfo.setRootType(datatypeInspector.getTypeName().getText());

      // set columns
      List<? extends xprc.xpce.datatype.NamedVariableMember> named_variable_members = datatypeInspector.listAllVariableMembers();
      List<TableColumn> allTableColumns = new ArrayList<>();
      for (NamedVariableMember member : named_variable_members) {
        TableColumn newColumn = new TableColumn();
        newColumn.setName(member.getLabel());
        newColumn.setPath(member.getVarName());
        allTableColumns.add(newColumn);
      }

      //===============
      // select columns
      //===============

      // Laufzeit O(n^2). Allerdings kein Problem bei n=#Spalten $\approx$ 10.
      List<TableColumn> selectedColumns = new ArrayList<>();

      // iterate over selected columns
      for (TableColumn modelledSelectedColumn : tableColumn) {
        String selectPath = modelledSelectedColumn.getPath();
        // null protection / ignore empty paths
        if ((selectPath == null) || selectPath.isEmpty()) {
          continue;
        }

        // convenience: UPPERCASE (modelled datatype members are unique ignoring case)
        selectPath = selectPath.toUpperCase();

        // iterate over all available columns
        for (TableColumn columnCandidate : allTableColumns) {
          String path = columnCandidate.getPath();
          // null protection / ignore empty paths
          if ((path == null) || (path.isEmpty()))
            continue;

          path = path.toUpperCase();
          if (path.equals(selectPath)) {
            TableColumn newColumn = modelledSelectedColumn.clone();
            // overwrite name (if not set)
            if ((modelledSelectedColumn.getName() == null) || modelledSelectedColumn.getName().isEmpty()) {
              newColumn.setName(columnCandidate.getName());
            }
            // overwrite path
            newColumn.setPath(columnCandidate.getPath());

            // add new colum to select columns
            selectedColumns.add(newColumn);
          }
        }
      }
      tableInfo.setColumns(selectedColumns);
      tableInfo.setBootstrap(false);
    }

    // is limit not set in tableInfo, look in the property "zeta.table.limit"
    if (tableInfo.getLimit() == null) {
      Configuration configuration =
          com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
      XynaPropertyUtils.XynaPropertyWithDefaultValue property = configuration.getPropertyWithDefaultValue("zeta.table.limit");
      // set limit to the value of the Xyna Property or, if it is null, to -1 
      if (property == null) {
        tableInfo.setLimit(-1);
      } else {
        tableInfo.setLimit(Integer.parseInt(property.getValueOrDefValue()));
      }
    }
    return tableInfo;
  }

  public QueryParameter mergeSortings(QueryParameter queryParameter, List<? extends SortCriterion> sortCriterions) {
    List<SortCriterion> combinedSortCriteria = new ArrayList<SortCriterion>(queryParameter.getSortCriterion());
    for (SortCriterion additionalSortCriterion : sortCriterions) {
        boolean elementAlreadyExisting = false;
        for (SortCriterion sortCriterion : combinedSortCriteria) {
            if (additionalSortCriterion.getCriterion().equals(sortCriterion.getCriterion())) {
                elementAlreadyExisting = true;
            }
        }
        if (!elementAlreadyExisting) {
            combinedSortCriteria.add(additionalSortCriterion);
        }
    }
    queryParameter.setSortCriterion(combinedSortCriteria);
    return queryParameter;
  }

  public List<GeneralXynaObject> paginate(TableInfo tableInfo, List<GeneralXynaObject> anyType) {
    // null protection
//    if ((tableInfo == null) || (anyType == null)) {
//      return anyType;
//    }
//    int length = anyType.size();
//
//    //***************************************************************************************************
//    // Call by reference. You won't see this in the Process Modeller. But it works. Hopefully.
//    // tableInfo INPUT will be changed without using an output. 
//    // Q: Why do we need this quirk?
//    // A: TableInfo output AND (list of) anyType is not (yet) supported by Xyna (Container(obj, anyType))
//    //***************************************************************************************************
//    tableInfo.setLength(length);
//    int pageSize = tableInfo.getPageSize();
//    // empty or negative page size => don't paginize
//    if (pageSize <= 0) {
//      return anyType;
//    }
//    int pageIndex = tableInfo.getPageIndex();
//    // negative page_index => don't paginize
//    if (pageIndex < 0) {
//      return anyType;
//    }
//
//    int start = Math.min(pageSize * pageIndex, length - 1);
//    int end = Math.min(start + pageSize, length);
//    if ((start < 0) || (start > end)) {
//      return anyType;
//    }
//
//    return anyType.subList(start, end);
    return null; // TODO: remove method when fixing ZETA-95
  }

  public List<GeneralXynaObject> sortAndFilterTable(TableInfo tableInfo, List<GeneralXynaObject> anyType) {
    // null check
    if ((tableInfo == null) || (tableInfo.getColumns() == null)) {
      return anyType;
    }

    //=====================
    // filter list elements
    //=====================
    List<GeneralXynaObject> filteredList = new ArrayList<>();
    String filter;
    String path;
    boolean addCurrentElement = true;

    for (GeneralXynaObject row : anyType) { // for every element in the list <=> for every (potential) row in the table

      addCurrentElement = true;
      for (TableColumn col : tableInfo.getColumns()) { // for every column in the row
        // ignore columns that are not filterable
        if (col.getDisableFilter()) {
          continue;
        }

        filter = col.getFilter();
        path = col.getPath();

        if ((filter == null) || filter.isEmpty()) { // empty filter? => test passed for this column
          continue;
        }
        try {
          if (String.valueOf(row.get(path)).toUpperCase().contains(filter.toUpperCase())) { // filter condition matches
            continue;
          } else {
            addCurrentElement = false; // filter condition doesn't match => skip this row
            break;
          }
        } catch (Exception e) {
          // ignore
        }
      }

      // add this row if it matches filter criteria
      if (addCurrentElement) {
        filteredList.add(row);
      }

    }

    // assign filtered list to output
    anyType = new ArrayList<>(filteredList);
    filteredList.clear(); // free ressources (GC do your job!)

    //==========
    // sort list
    //==========

    // determine column to sort for
    String sortedColumnTemp = "";
    boolean ascendingOrderTemp = true;
    for (TableColumn tc : tableInfo.getColumns()) {
      if (!tc.getDisableSort() && (tc.getSort() != null) && !tc.getSort().isEmpty()) {
        sortedColumnTemp = tc.getPath();
        ascendingOrderTemp = tc.getSort().equalsIgnoreCase("ASC");
        break;
      }
    }
    final String sortedColumn = sortedColumnTemp;
    final boolean ascendingOrder = ascendingOrderTemp;
    Comparator<GeneralXynaObject> comparator = new Comparator<GeneralXynaObject>() {

      public int compare(GeneralXynaObject e1, GeneralXynaObject e2) {

        Object value1 = getValue(e1);
        Object value2 = getValue(e2);

        if (value1 == null) {
          if (value2 == null) {
            return 0;
          } else {
            return 1;
          }
        } else if (value2 == null) {
          return -1;
        }
        if (value1 instanceof Comparable) {
          // compare Numbers
          if (value1 instanceof Number) {
            double vd1 = ((Number) value1).doubleValue();
            double vd2 = ((Number) value2).doubleValue();
            return (int) Math.signum(vd1 - vd2) * (ascendingOrder ? 1 : -1);
            // compare Strings
          } else {
            String vs1 = value1.toString();
            String vs2 = value2.toString();
            return vs1.compareToIgnoreCase(vs2) * (ascendingOrder ? 1 : -1);
          }
        } else {
          throw new RuntimeException("unsupported type: " + value1.getClass().getName());
        }
      }


      private Object getValue(GeneralXynaObject obj) {
        try {
          return obj.get(sortedColumn);
        } catch (Exception e) { // not instantiated members lead to this exception => return null
          return null;
        }
      }

    };

    if (!sortedColumn.isEmpty()) {
      Collections.sort(anyType, comparator);
    }
    return anyType;
  }
  
  
  @Override
  public List<GeneralXynaObject> tableHelperFilter(TableInfo tableInfo, List<GeneralXynaObject> anyTypes) {
    if(anyTypes == null || anyTypes.isEmpty())
      return anyTypes;
    
    TableHelper<GeneralXynaObject, TableInfo> tableHelper = createTableHelper(anyTypes.get(0), tableInfo);
    return anyTypes.stream().filter(tableHelper.filter()).collect(Collectors.toList());
  }
  
  @Override
  public List<GeneralXynaObject> tableHelperLimit(TableInfo tableInfo, List<GeneralXynaObject> anyTypes) {
    if(anyTypes == null || anyTypes.isEmpty())
      return anyTypes;
    TableHelper<GeneralXynaObject, TableInfo> tableHelper = createTableHelper(anyTypes.get(0), tableInfo);
    return tableHelper.limit(anyTypes);
  }
  
  @Override
  public List<GeneralXynaObject> tableHelperSort(TableInfo tableInfo, List<GeneralXynaObject> anyTypes) {
    if(anyTypes == null || anyTypes.isEmpty())
      return anyTypes;
    TableHelper<GeneralXynaObject, TableInfo> tableHelper = createTableHelper(anyTypes.get(0), tableInfo);
    tableHelper.sort(anyTypes);
    return anyTypes;
  }
  
  @Override
  public List<GeneralXynaObject> tableHelperSortFilterLimit(TableInfo tableInfo, List<GeneralXynaObject> anyTypes) {
    if(anyTypes == null || anyTypes.isEmpty())
      return anyTypes;
    TableHelper<GeneralXynaObject, TableInfo> tableHelper = createTableHelper(anyTypes.get(0), tableInfo);
    anyTypes = anyTypes.stream().filter(tableHelper.filter()).collect(Collectors.toList());
    tableHelper.sort(anyTypes);
    return tableHelper.limit(anyTypes);
  }
  
  private TableHelper<GeneralXynaObject, TableInfo> createTableHelper(GeneralXynaObject anyType, TableInfo tableInfo){
    DatatypeInspector datatypeInspector = DatatypeInspector.inspectDatatype(anyType);
    List<? extends NamedVariableMember> variableMembers = datatypeInspector.listAllVariableMembers();
    TableHelper<GeneralXynaObject, TableInfo> helper = TableHelper.<GeneralXynaObject, TableInfo>init(tableInfo);
    for (NamedVariableMember vm : variableMembers) {
      helper.addSelectFunction(vm.getVarName(), s -> {
        try {
          return s.get(vm.getVarName());
        } catch (InvalidObjectPathException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      });
    }
    return helper.limitConfig(TableInfo::getLimit)
    .sortConfig(ti -> {
      for (TableColumn tc : ti.getColumns()) {
        TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
        if(sort != null)
          return sort;
      }
      return null;
    })
    .filterConfig(ti -> 
      ti.getColumns().stream()
      .filter(tableColumn -> 
        !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null
      )
      .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
      .collect(Collectors.toList())
    );
  }

  @Override
  public TableInfo setLength(TableInfo tableInfo, List<GeneralXynaObject> listEntries) {
    if (listEntries != null) {
      tableInfo.setLength(listEntries.size());
    } else {
      tableInfo.setLength(0);
    }

    return tableInfo;
  }

}
