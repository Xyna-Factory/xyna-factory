/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package pkg;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.gip.xyna.xnwh.persistence.Parameter;

import xmcp.zeta.storage.generic.filter.FilterColumnConfig;
import xmcp.zeta.storage.generic.filter.FilterColumnInput;
import xmcp.zeta.storage.generic.filter.TableFilter;


public class TableInfoExample {
  
  // instead of this class, a real-life gui-example would use xmcp.tables.datatypes.TableInfo
  public static class TableInfo {
    private List<TableColumn> columns;
    public List<TableColumn> getColumns() {
      return columns;
    }
    public void setColumns(List<TableColumn> columns) {
      this.columns = columns;
    }
  }
    
  // instead of this class, a real-life gui-example would use xmcp.tables.datatypes.TableColumn
  public static class TableColumn {
    private String path;
    private String filter;
    public String getPath() {
      return path;
    }
    public void setPath(String path) {
      this.path = path;
    }
    public String getFilter() {
      return filter;
    }
    public void setFilter(String filter) {
      this.filter = filter;
    }
  }
  
  // instead of this class, a real-life gui-example would use a modelled xmom-datatype 
  //  that provides the data for a gui table
  public static class XmomGuiTableExampleData {
    private String col1;
    private String col2;
    public String getCol1() {
      return col1;
    }
    public void setCol1(String col1) {
      this.col1 = col1;
    }
    public String getCol2() {
      return col2;
    }
    public void setCol2(String col2) {
      this.col2 = col2;
    }
  }
  
  public static class Constants {
    public static class SqlTableColumnNames {
      public static final String COLUMN_1 = "column_1";
      public static final String COLUMN_2 = "column_2";
    }
    public static class XmomAttributeNames {
      public static final String COL_1 = "col1";
      public static final String COL_2 = "col2";
    }
  }
  
  
  @Test
  public void test1() {
    try {
      TableInfo tableInfo = new TableInfo();
      List<TableColumn> colList = new ArrayList<>();
      TableColumn col = new TableColumn();
      col.setFilter("*my-filter-val-1");
      col.setPath("col1");
      colList.add(col);
      col = new TableColumn();
      col.setFilter("'=my-filter-val-2'");
      col.setPath("col2");
      colList.add(col);
      tableInfo.setColumns(colList);
      
      TableFilter tf = buildTableFilter(tableInfo);
      
      Parameter param = tf.buildParameter();
      log(tf.getWhereClause());
      logParameter(param);
      assertEquals(" WHERE (`column_1` LIKE ?) AND (`column_2` = ?)", tf.getWhereClause());
      assertEquals(2, param.size());
      assertEquals("%my-filter-val-1", param.get(0));
      assertEquals("=my-filter-val-2", param.get(1));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  private TableFilter buildTableFilter(TableInfo tableInfo) {
    Map<String, FilterColumnConfig> colConfigMap = initMap();
    List<FilterColumnInput> adaptedColumns = new ArrayList<>();
    List<FilterColumnConfig> configList = new ArrayList<>();
    
    for (TableColumn tc : tableInfo.getColumns()) {
      FilterColumnConfig fcc = colConfigMap.get(tc.getPath());
      if (fcc != null) {
        adaptedColumns.add(adapt(tc));
        configList.add(fcc);
      }
    }
    Function<String, String> escape = (x -> "`" + x + "`");
    
    TableFilter ret = TableFilter.builder(configList).build(adaptedColumns, escape);
    return ret;
  }
  
  
  private Map<String, FilterColumnConfig> initMap() {
    Map<String, FilterColumnConfig> colConfigMap = new HashMap<>();
    FilterColumnConfig confCol = FilterColumnConfig.builder().sqlColumnName(Constants.SqlTableColumnNames.COLUMN_1).
                                                              xmomPath(Constants.XmomAttributeNames.COL_1).build();
    colConfigMap.put(confCol.getXmomPath(), confCol);
    confCol = FilterColumnConfig.builder().sqlColumnName(Constants.SqlTableColumnNames.COLUMN_2).
                                           xmomPath(Constants.XmomAttributeNames.COL_2).build();
    colConfigMap.put(confCol.getXmomPath(), confCol);
    return colConfigMap;
  }
  
  
  private FilterColumnInput adapt(TableColumn tc) {
    FilterColumnInput ret = new FilterColumnInput();
    ret.setFilter(tc.getFilter());
    ret.setPath(tc.getPath());
    return ret;
  }
  
  
  private void logParameter(Parameter param) {
    for (int i = 0; i < param.size(); i++) {
      Object obj = param.get(i);
      log("SQL Parameter: " + obj);
    }
  }
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new TableInfoExample().test1();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
