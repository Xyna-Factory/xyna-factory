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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class XMOMSearchDispatcher {
  
  
  @SuppressWarnings("unchecked")
  private static enum XMOMSearchProvider {
    
    XMOM_DATABASE(XMOMDatabaseEntryColumn.ALL_FORWARD_RELATIONS),
    DEPLOYMENT_ITEM_MGMT(Arrays.asList(XMOMDatabaseEntryColumn.CALLEDBY,
                                       XMOMDatabaseEntryColumn.POSSESSEDBY,
                                       XMOMDatabaseEntryColumn.EXTENDEDBY,
                                       XMOMDatabaseEntryColumn.INSTANCESERVICEREFERENCEOF,
                                       XMOMDatabaseEntryColumn.NEEDEDBY,
                                       XMOMDatabaseEntryColumn.PRODUCEDBY,
                                       XMOMDatabaseEntryColumn.POSSESSEDBY,
                                       XMOMDatabaseEntryColumn.USEDINIMPLOF));
    
    private final Set<XMOMDatabaseEntryColumn> coveredColumns;
    
    private XMOMSearchProvider(Collection<XMOMDatabaseEntryColumn>... coveredColumns) {
      this.coveredColumns = new HashSet<XMOMDatabaseEntryColumn>();
      for (Collection<XMOMDatabaseEntryColumn> coveredColumnSet : coveredColumns) {
        this.coveredColumns.addAll(coveredColumnSet);
      }
    }
    
    public Set<XMOMDatabaseEntryColumn> getCoveredColumns() {
      return coveredColumns;
    }

    public static XMOMSearchProvider determineBySelect(XMOMDatabaseSelect select) {
      
      XMOMSearchProvider provider = determineBySelection(select.getSelection());
      if (provider != XMOM_DATABASE) {
        // assert only restriction on fqName
        Set<XMOMDatabaseEntryColumn> conditions = select.getColumnsWithinWhereClauses();
        if (conditions.size() != 1 ||
            conditions.iterator().next() != XMOMDatabaseEntryColumn.FQNAME) {
          return XMOM_DATABASE;
        }
      }
      return provider;
    }
    
    public static XMOMSearchProvider determineBySelection(Set<XMOMDatabaseEntryColumn> selection) {
      // Generic searches
      if (XMOMDatabaseEntryColumn.ALL_GENERICS.containsAll(selection)) {
        return XMOM_DATABASE;
      }
      Set<XMOMDatabaseEntryColumn> relationSelection = new HashSet<XMOMDatabaseEntryColumn>(selection);
      relationSelection.removeAll(XMOMDatabaseEntryColumn.ALL_GENERICS);
      for (XMOMSearchProvider provider : values()) {
        if (provider.coveredColumns.containsAll(relationSelection)) {
          return provider;
        }
      }
      // fallback to XMOM-DB
      return XMOM_DATABASE;
    }
    
  }
  
  public static XMOMDatabaseSearchResult dispatchXMOMDatabaseSelects(List<XMOMDatabaseSelect> selects, int maxRows, Long revision)
                  throws XNWH_InvalidSelectStatementException, PersistenceLayerException {
    Map<XMOMSearchProvider, List<XMOMDatabaseSelect>> analysedSearches =
                    new EnumMap<XMOMSearchDispatcher.XMOMSearchProvider, List<XMOMDatabaseSelect>>(XMOMSearchProvider.class);
    for (XMOMDatabaseSelect select : selects) {
      XMOMSearchProvider provider = analyseSelect(select);
      List<XMOMDatabaseSelect> subSelects = analysedSearches.get(provider);
      if (subSelects == null) {
        subSelects = new ArrayList<XMOMDatabaseSelect>();
        analysedSearches.put(provider, subSelects);
      }
      subSelects.add(select);
    }
    Collection<XMOMDatabaseSearchResult> results = new ArrayList<XMOMDatabaseSearchResult>();
    for (XMOMSearchProvider provider : XMOMSearchProvider.values()) {
      try {
        XMOMDatabaseSearchResult result = executeSelect(provider, analysedSearches.get(provider), maxRows, revision);
        results.add(result);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // ntbd
      }
    }
    return mergeResults(results, maxRows);
  }
  
  
  private static XMOMSearchProvider analyseSelect(XMOMDatabaseSelect select) {
    return XMOMSearchProvider.determineBySelect(select);
  }
  
  
  public static XMOMDatabaseSearchResult executeSelect(XMOMSearchProvider provider, List<XMOMDatabaseSelect> selects, int maxRows, Long revision) throws XNWH_InvalidSelectStatementException, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if (selects != null && selects.size() > 0) {
      switch (provider) {
        case DEPLOYMENT_ITEM_MGMT :
          return searchDeploymentItemManagement(selects, maxRows, revision);
        case XMOM_DATABASE :
          return searchXMOMDatabase(selects, maxRows, revision);
        /*case DEPENDENCY_REGISTER :
          return searchDependencyRegister(selects, maxRows, revision);*/
        default :
          throw new IllegalArgumentException("Unknown provider for XMOMSelect dispatching: " + provider);
      }
    } else {
      return XMOMDatabaseSearchResult.empty();
    }
  }
  
  

  public static XMOMDatabaseSearchResult searchXMOMDatabase(List<XMOMDatabaseSelect> selects, int maxRows, Long revision)
                  throws XNWH_InvalidSelectStatementException, PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase().searchXMOMDatabase(selects, maxRows, revision);
  }


  public static XMOMDatabaseSearchResult searchDeploymentItemManagement(List<XMOMDatabaseSelect> selects, int maxRows, Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Map<Long, Map<String, XMOMDatabaseSearchResultEntry>> results = new HashMap<Long, Map<String, XMOMDatabaseSearchResultEntry>>();
    DeploymentItemStateManagementImpl dismi = (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    Map<XMOMDatabaseType, Integer> counts = new HashMap<XMOMDatabaseType, Integer>();
    for (XMOMDatabaseSelect select : selects) {
      // extract a single backward relation
      Set<XMOMDatabaseEntryColumn> selection = new HashSet<XMOMDatabaseEntryColumn>(select.getSelection());
      selection.retainAll(XMOMSearchProvider.DEPLOYMENT_ITEM_MGMT.getCoveredColumns());
      if (selection.size() <= 0) {
        throw new IllegalArgumentException("Classified search as DeploymentItem but no backward relation was retained '" + selection + "'!");
      }
      // extract the single fqName
      String usedObject = (String) select.getParameter().get(0);
      Map<Long, Map<String, XMOMDatabaseSearchResultEntry>> newResults = dismi.searchByBackwardRelation(selection, usedObject, revision);
      for (Entry<Long, Map<String, XMOMDatabaseSearchResultEntry>> mapEntry : newResults.entrySet()) {
        Map<String, XMOMDatabaseSearchResultEntry> resultSubMapForRev = results.get(mapEntry.getKey());
        if (resultSubMapForRev == null) {
          resultSubMapForRev = new HashMap<String, XMOMDatabaseSearchResultEntry>();
          results.put(mapEntry.getKey(), resultSubMapForRev);
        }
        for (Entry<String, XMOMDatabaseSearchResultEntry> entry : mapEntry.getValue().entrySet()) {
          XMOMDatabaseSearchResultEntry currentEntry = resultSubMapForRev.get(entry.getKey());
          if (currentEntry == null) {
            resultSubMapForRev.put(entry.getKey(), entry.getValue());
            XMOMDatabaseType t = entry.getValue().getType();
            //analog zu XMOMDatabase.executePreparedSelect
            if (t == XMOMDatabaseType.WORKFLOW || t == XMOMDatabaseType.OPERATION) {
              t = XMOMDatabaseType.SERVICE;
            }
            Integer i = counts.get(t);
            if (i == null) {
              counts.put(t, 1);
            } else {
              counts.put(t, i + 1);
            }
          } else {
            currentEntry.setWeigth(currentEntry.getWeigth() + 1);
          }
        }
      }      
    }
    return generateResult(results, maxRows, counts);
  }
  
  
  private static XMOMDatabaseSearchResult generateResult(Map<Long, Map<String, XMOMDatabaseSearchResultEntry>> results, int maxRows, Map<XMOMDatabaseType, Integer> counts) {
    List<XMOMDatabaseSearchResultEntry> sortedList = new ArrayList<XMOMDatabaseSearchResultEntry>();
    for (Map<String, XMOMDatabaseSearchResultEntry> m : results.values()) {
      sortedList.addAll(m.values());
    }
    XMOMDatabase.sortResultList(sortedList);
    List<XMOMDatabaseSearchResultEntry> trimmedList = sortedList;
    if (maxRows >= 0 &&
        sortedList.size() > maxRows) {
      trimmedList = sortedList.subList(0, maxRows);
    }
    int countAll = 0;
    for (Entry<XMOMDatabaseType, Integer> e : counts.entrySet()) {
      countAll += e.getValue();
    }
    
    return new XMOMDatabaseSearchResult(trimmedList, countAll, counts);
  }
  
  
  private static XMOMDatabaseSearchResult mergeResults(Collection<XMOMDatabaseSearchResult> results, int maxRows) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Map<Long, Map<String, XMOMDatabaseSearchResultEntry>> revMap = new HashMap<Long, Map<String,XMOMDatabaseSearchResultEntry>>();
    Map<XMOMDatabaseType, Integer> counts = new HashMap<XMOMDatabaseType, Integer>();
    for (XMOMDatabaseSearchResult result : results) {
      for (XMOMDatabaseSearchResultEntry entry : result.getResult()) {
        if (entry.getType() == null) {
          throw new RuntimeException(entry.getFqName() + " type is null");
        }
        Long revision;
        try {
          revision = rm.getRevision(entry.getRuntimeContext());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
          //ignore
          continue;
        }
        Map<String, XMOMDatabaseSearchResultEntry> entryMap = revMap.get(revision);
        if (entryMap == null) {
          entryMap = new HashMap<String, XMOMDatabaseSearchResultEntry>();
          revMap.put(revision, entryMap);
        }
        XMOMDatabaseSearchResultEntry currentEntry = entryMap.get(entry.getFqName());
        if (currentEntry == null) {
          entryMap.put(entry.getFqName(), entry);
        } else {
          currentEntry.setWeigth(currentEntry.getWeigth() + 1);
        }
      }
      for (Entry<XMOMDatabaseType, Integer> e : result.getCounts().entrySet()) {
        Integer i = counts.get(e.getKey());
        if (i == null) {
          counts.put(e.getKey(), e.getValue());
        } else {
          counts.put(e.getKey(), e.getValue() + i);
        }
      }
    }
    return generateResult(revMap, maxRows, counts);
  }


}
