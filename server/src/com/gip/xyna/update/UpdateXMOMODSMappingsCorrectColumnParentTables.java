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
package com.gip.xyna.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;

public class UpdateXMOMODSMappingsCorrectColumnParentTables extends UpdateJustVersion {
  

  public UpdateXMOMODSMappingsCorrectColumnParentTables(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
    setExecutionTime(ExecutionTime.endOfFactoryStart);
  }
  

  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(XMOMODSMapping.class);
    Collection<XMOMODSMapping> allMappings;
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      allMappings = con.loadCollection(XMOMODSMapping.class);
    } finally {
      con.closeConnection();
    }
    
    Collection<XMOMODSMapping> modified = new ArrayList<>();
    Collection<XMOMODSMapping> toDelete = new ArrayList<>();
    
    Map<String, Pair<XMOMODSMapping, Collection<XMOMODSMapping>>> indexedMappings = new HashMap<>();
    for (XMOMODSMapping mapping : allMappings) {
      String key = createKey(mapping);
      if (!indexedMappings.containsKey(key)) {
        Pair<XMOMODSMapping, Collection<XMOMODSMapping>> newPair = Pair.of(null, new ArrayList<>());
        indexedMappings.put(key, newPair);
      }
      Pair<XMOMODSMapping, Collection<XMOMODSMapping>> pair = indexedMappings.get(key);
      if (mapping.isTableConfig()) {
        if (pair.getFirst() == null) {
          pair.setFirst(mapping);
        } else {
          logger.debug("Duplicate table definition for '" + key + "' " + mapping.getTablename() + " & " + pair.getFirst().getTableName());
          // keep the one with the higher index
          if (mapping.getId() > pair.getFirst().getId()) {
            toDelete.add(pair.getFirst());
            pair.setFirst(mapping);
          } else {
            toDelete.add(mapping);
          }
        }
      } else {
        pair.getSecond().add(mapping);
      }
    }
    
    for (Pair<XMOMODSMapping, Collection<XMOMODSMapping>> indexedMapping : indexedMappings.values()) {
      XMOMODSMapping correspondingTable = indexedMapping.getFirst();
      Map<String, XMOMODSMapping> columnsByFqPath = new HashMap<>();
      for (XMOMODSMapping columnMapping : indexedMapping.getSecond()) {
        if (columnsByFqPath.containsKey(columnMapping.getFqpath())) {
          XMOMODSMapping alreadyPresentMapping = columnsByFqPath.get(columnMapping.getFqpath());
          if (!alreadyPresentMapping.getTablename().equals(correspondingTable.getTablename())) {
            //replace present
            columnsByFqPath.put(columnMapping.getFqpath(), columnMapping);
            toDelete.add(alreadyPresentMapping);
          } else if (columnMapping.getTablename().equals(correspondingTable.getTablename())) {
            //both have correct, use newer id
            if (alreadyPresentMapping.getId() < columnMapping.getId()) {
              columnsByFqPath.put(columnMapping.getFqpath(), columnMapping);
              toDelete.add(alreadyPresentMapping);
            } else {
              toDelete.add(columnMapping);
            }
          } else {
            // reject current
            toDelete.add(columnMapping);
          }
        } else {
          columnsByFqPath.put(columnMapping.getFqpath(), columnMapping);
        }        
      }
      indexedMapping.setSecond(new ArrayList<>(columnsByFqPath.values()));
    }
    
    // can still contain flattened entries
    //    those have Pair.first==null
    Iterator<Entry<String, Pair<XMOMODSMapping, Collection<XMOMODSMapping>>>> indexedMappingIter = indexedMappings.entrySet().iterator();
    while (indexedMappingIter.hasNext()) {
      Entry<String, Pair<XMOMODSMapping, Collection<XMOMODSMapping>>> indexedMappingEntry = indexedMappingIter.next();
      if (indexedMappingEntry.getValue().getFirst() == null) { // found one
        // shorten the path until we find a parent
        String currentPath = shortenPathInKey(indexedMappingEntry.getKey());
        while (!indexedMappings.containsKey(currentPath)) {
          if (currentPath.contains("@@")) {
            break;
          }
          currentPath = shortenPathInKey(currentPath);
        }
        if (indexedMappings.containsKey(currentPath)) {
          // migrate entries to appropriate parent
          indexedMappings.get(currentPath).getSecond().addAll(indexedMappingEntry.getValue().getSecond());
        } else {
          // can happen for extensions on root level, does do not need any adjustment
        }
        indexedMappingIter.remove();
      }
    }
    
    // compare tableNames if they differ adjust them and add to modified
    for (Entry<String, Pair<XMOMODSMapping, Collection<XMOMODSMapping>>> indexedMappingEntry : indexedMappings.entrySet()) {
      Pair<XMOMODSMapping, Collection<XMOMODSMapping>> indexedMapping = indexedMappingEntry.getValue();
      XMOMODSMapping correspondingTable = indexedMapping.getFirst();
      if (correspondingTable == null) {
        // can happen for extensions on root level, does do not need any adjustment
        continue;
      }
      for (XMOMODSMapping columnMapping : indexedMapping.getSecond()) {
        if (!correspondingTable.getTablename().equals(columnMapping.getTablename())) {
          columnMapping.setTablename(correspondingTable.getTablename());
          modified.add(columnMapping);
        }
      }
    }
     
    con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.delete(toDelete);
      con.persistCollection(modified);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  
  private static String printKey(XMOMODSMapping mapping) {
    StringBuilder sb = new StringBuilder();
    sb.append(mapping.getFqxmlname())
      .append('@');  
    sb.append(mapping.getFqpath());
    sb.append('@')
      .append(mapping.getRevision());
    sb.append(" -> ")
    .append(mapping.getTablename());
    if (!mapping.isTableConfig()) {
      sb.append(".")
      .append(mapping.getColumnname());  
    }
    return sb.toString();
  }

  
  
  
  
  private static String createKey(XMOMODSMapping mapping) {
    StringBuilder sb = new StringBuilder();
    sb.append(mapping.getFqxmlname())
      .append('@');  
    if (mapping.isTableConfig()) {
      sb.append(mapping.getFqpath());
    } else {
      sb.append(shortenPath(mapping.getFqpath()));
    }
    sb.append('@')
      .append(mapping.getRevision());
    return sb.toString();
  }

  
  private static String shortenPathInKey(String key) {
    StringBuilder sb = new StringBuilder();
    String[] parts = key.split("[@]");
    sb.append(parts[0])
      .append("@")
      .append(shortenPath(parts[1]))
      .append("@")
      .append(parts[2]);
    return sb.toString();
  }
  

  private static String shortenPath(String fqPath) {
    String shortenedPath = fqPath;
    if (shortenedPath.endsWith("}")) {
      shortenedPath = shortenedPath.substring(0, shortenedPath.lastIndexOf('{'));
    }
    if (shortenedPath.lastIndexOf('.') < 0) {
      return ""; 
    } else {
      return shortenedPath.substring(0, shortenedPath.lastIndexOf('.'));
    }
  }
  
}
