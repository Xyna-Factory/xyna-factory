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
package com.gip.xyna.xnwh.selection.parsing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;


public class SearchRequestBean implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private ArchiveIdentifier archiveIdentifier;
  private String selection;
  private int maxRows;
  private Map<String,String> filterEntries;
  private Map<String, String> additionalParameter;
  private List<String> factoryNodesFilter;
  private List<OrderBy> orderBys;
  

  public static class OrderBy implements Serializable {

    private static final long serialVersionUID = 1L;
    public final String colName;
    public final boolean asc;


    public OrderBy(String colName, boolean asc) {
      this.colName = colName;
      this.asc = asc;
    }
  }
  
  public SearchRequestBean() {
    
  }
  
  public SearchRequestBean(ArchiveIdentifier archiveIdentifier, int maxRows) {
    this.archiveIdentifier = archiveIdentifier;
    this.selection = "*";
    this.maxRows = maxRows;
  }


  public SearchRequestBean(SearchRequestBean srb) {
    archiveIdentifier = srb.archiveIdentifier;
    selection = srb.selection;
    maxRows = srb.maxRows;
    filterEntries = srb.filterEntries == null ? null : new HashMap<String, String>(srb.filterEntries);
    additionalParameter = srb.additionalParameter == null ? null : new HashMap<String, String>(srb.additionalParameter);
    factoryNodesFilter = srb.factoryNodesFilter == null ? null : new ArrayList<String>(srb.factoryNodesFilter);
    orderBys = srb.orderBys == null ? null : new ArrayList<OrderBy>(srb.orderBys);
  }

  public ArchiveIdentifier getArchiveIdentifier() {
    return archiveIdentifier;
  }
  
  public void setArchiveIdentifier(ArchiveIdentifier archiveIdentifier) {
    this.archiveIdentifier = archiveIdentifier;
  }
  
  public String getSelection() {
    return selection;
  }
  
  public void setSelection(String selection) {
    this.selection = selection;
  }
  
  public int getMaxRows() {
    return maxRows;
  }
  
  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }
  
  public Map<String, String> getFilterEntries() {
    return filterEntries;
  }
  
  public void setFilterEntries(Map<String, String> filterEntries) {
    this.filterEntries = filterEntries;
  }
  
  public void addFilterEntry(String key, String value) {
    if( filterEntries == null ) {
      filterEntries = new HashMap<String,String>();
    }
    filterEntries.put(key,value);
  }

  public String getAdditionalParameter(String key) {
    if (additionalParameter == null) {
      return null;
    }
    return additionalParameter.get(key);
  }

  public void addAdditionalParameter(String key, String value) {
    if (additionalParameter == null) {
      additionalParameter = new HashMap<>();
    }
    additionalParameter.put(key, value);
  }

  /**
   * jeder filter ist entweder
   * "local"
   * "&lt;factoryNodeName&gt;"
   * "&lt;wildcard-expression&gt;",die serverseitig in eine liste von factorynodes ausge-x-t wird, z.b. ACS* für ACS1 und ACS2
   * 
   * die liste der nodes wird immer "geodert" gesehen, d.h. die ergebnisse der suchen werden vereinigt.
   * 
   * leere liste wird genauso behandelt wie eine liste mit dem einzigen element "local".
   */
  public void addFactoryNodeFilter(String factoryNodeFilter) {
    if (factoryNodesFilter == null) {
      factoryNodesFilter = new ArrayList<String>();
    }
    factoryNodesFilter.add(factoryNodeFilter);
  }

  public List<String> getFactoryNodesFilter() {
    return factoryNodesFilter;
  }

  public void clearFactoryNodesFilter() {
    factoryNodesFilter = null;
  }


  public boolean isLocal() {
    return factoryNodesFilter == null || factoryNodesFilter.size() == 0
        || (factoryNodesFilter.size() == 1 && factoryNodesFilter.contains(NodeManagement.FACTORYNODE_LOCAL));
  }

  public void addOrderBy(String colName, boolean asc) {
    if (orderBys == null) {
      orderBys = new ArrayList<OrderBy>();
    }
    orderBys.add(new OrderBy(colName, asc));
  }
  
  public List<OrderBy> getOrderBys() {
    if (orderBys == null) {
      return Collections.emptyList();
    }
    return orderBys;
  }

}
