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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.xmom.PathBuilder;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils;


public class CleanODSMappings implements CommandExecution {
  
  //private static final Logger logger = CentralFactoryLogging.getLogger(CleanODSMappings.class);
  
  private static enum Mode {
    
    clean("clean"),
    print("print");
    
    private final String name;
    
    private Mode(String name) {
      this.name = name;
    }
    
    private static Mode fromString(String input) {
      if (input != null) {
        for (Mode mode : values()) {
          if (mode.name.equalsIgnoreCase(input)) {
            return mode;
          }
        }
      }
      return Mode.clean;
    }
    
  }

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
    Mode mode = Mode.clean;
    if (allArgs.getArgCount() > 0) {
      mode = Mode.fromString(allArgs.getArg(0));
    }
    
    // get all live revisions
    final RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<Long> liveRevisions = revMgmt.getAllRevisions();
    Set<Long> deadRevisions = new HashSet<>();
    
    
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      FactoryWarehouseCursor<XMOMODSMapping> mappingCursor = XMOMODSMappingUtils.getCursorOverAll(con);
      List<XMOMODSMapping> mappings = mappingCursor.getRemainingCacheOrNextIfEmpty();
      while (mappings != null &&
             mappings.size() > 0) {
        for (XMOMODSMapping mapping : mappings) {
          // collect all not live
          if (!liveRevisions.contains(mapping.getRevision())) {
            deadRevisions.add(mapping.getRevision());
          }
        }
        mappings = mappingCursor.getRemainingCacheOrNextIfEmpty();
      }
    
      if (deadRevisions.size() > 0) {
        clw.writeLineToCommandLine("Found entries for " + deadRevisions.size() + " unmanaged revisions");
      }
      
      switch (mode) {
        default:
        case clean:
          // remove all dead revisions
          for (Long deadRevision : deadRevisions) {
            XMOMODSMappingUtils.removeAllForRevision(con, deadRevision);      
          }  
          break;
        case print:
          for (Long deadRevision : deadRevisions) {
            Collection<XMOMODSMapping> deadEntries = XMOMODSMappingUtils.getAllMappingsForRevision(deadRevision);
            clw.writeLineToCommandLine(deadEntries.size() + " entries for revision " + deadRevision + ":");
            for (XMOMODSMapping deadEntry : deadEntries) {
              clw.writeLineToCommandLine(prettyPrint(deadEntry));
            }
          }
          break;
      }
      
      // TODO check for duplicates? same fqName, fqPath, revisions but different tablename/columnname?
      // TODO there might be entries for flattened paths, those mess up the divergingOrMissingRootEntryBuilder detection but are hard to detect
      
      // get all roots
      // get all tables
      // for each table get all columns via path
      // validate same table name
      int missingTableNameCount = 0;
      StringBuilder missingTableNameEntryBuilder = new StringBuilder();
      int divergingOrMissingRootCount = 0;
      StringBuilder divergingOrMissingRootEntryBuilder = new StringBuilder();
      for (Long liveRevision : liveRevisions) {
        Collection<XMOMODSMapping> allRoots = XMOMODSMappingUtils.getAllRootObjectsForRevision(con, liveRevision);
        for (XMOMODSMapping aRoot : allRoots) {
          Collection<XMOMODSMapping> allForRoot = XMOMODSMappingUtils.getAllMappingsForRootType(aRoot.getFqxmlname(), aRoot.getRevision());
          Map<String, XMOMODSMapping> allTablesForRootByFqPath = new HashMap<>();
          for (XMOMODSMapping aForRoot : allForRoot) {
            if (aForRoot.getColumnname() == null ||
                aForRoot.getColumnname().isEmpty()) {
              if (aForRoot.getTablename() == null ||
                  aForRoot.getTablename().isEmpty()) {
                missingTableNameCount++;
                switch (mode) {
                  default:
                  case clean:
                    con.deleteOneRow(aForRoot);
                    break;
                  case print: 
                    missingTableNameEntryBuilder.append(Constants.LINE_SEPARATOR).append(prettyPrint(aForRoot));
                    break;
                }
              } else {
                allTablesForRootByFqPath.put(aForRoot.getFqpath(), aForRoot);
              }
            }
          }
          
          for (XMOMODSMapping aForRoot : allForRoot) {
            if (aForRoot.getColumnname() != null &&
                !aForRoot.getColumnname().isEmpty()) {
              XMOMODSMapping correspondingRoot = findRootEntry(aForRoot.getFqpath(), allTablesForRootByFqPath);
              if (correspondingRoot == null ||
                  !correspondingRoot.getTablename().equals(aForRoot.getTablename())) { // compare tablename
                // got one
                divergingOrMissingRootCount++;
                switch (mode) {
                  default:
                  case clean:
                    con.deleteOneRow(aForRoot);
                    break;
                  case print: 
                    divergingOrMissingRootEntryBuilder.append(Constants.LINE_SEPARATOR).append(prettyPrint(aForRoot));
                    break;
                }
              }
            }
          }
        }
      }
      if (missingTableNameCount > 0) {
        switch (mode) {
          default:
          case clean:
            clw.writeLineToCommandLine("Found " + missingTableNameCount + " root entries without table name declaration.");
            break;
          case print: 
            clw.writeLineToCommandLine("Found " + missingTableNameCount + " root entries without table name declaration:" + missingTableNameEntryBuilder.toString());
            break;
        }
      }
      if (divergingOrMissingRootCount > 0) {
        switch (mode) {
          default:
          case clean:
            clw.writeLineToCommandLine("Found " + divergingOrMissingRootCount + " column entries with missing root or diverging table names.");
            break;
          case print: 
            clw.writeLineToCommandLine("Found " + divergingOrMissingRootCount + " column entries with missing root or diverging table names:" + divergingOrMissingRootEntryBuilder.toString());
            break;
        }
      }
      if (deadRevisions.size() <= 0 &&
          missingTableNameCount <= 0 &&
          divergingOrMissingRootCount  <= 0) {
        clw.writeLineToCommandLine("All Mappings are valid.");
      }
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  private static XMOMODSMapping findRootEntry(String fqPath, Map<String, XMOMODSMapping> allTablesForRootByFqPath) {
    if (fqPath.isEmpty()) {
      return null;
    }
    String shortenedFqPath = "";
    if (fqPath.indexOf('.') >= 0) {
      if (fqPath.substring(fqPath.lastIndexOf('.')).contains("" + PathBuilder.PATH_CLASS_SUFFIX)) {
        shortenedFqPath = fqPath.substring(0, fqPath.lastIndexOf(PathBuilder.PATH_CLASS_PREFIX));
        if (shortenedFqPath.indexOf('.') >= 0) {
          shortenedFqPath = shortenedFqPath.substring(0, shortenedFqPath.lastIndexOf('.'));
        }
      } else {
        shortenedFqPath = fqPath.substring(0, fqPath.lastIndexOf('.'));
      }
    }
    XMOMODSMapping correspondingRoot = allTablesForRootByFqPath.get(shortenedFqPath);
    if (correspondingRoot == null) {
      // might be flat...recurse
      return findRootEntry(shortenedFqPath, allTablesForRootByFqPath);
    } else {
      return correspondingRoot;
    }
  }
  
  private static String prettyPrint(XMOMODSMapping mapping) {
    StringBuilder lineBuilder = new StringBuilder();
    lineBuilder.append("   - ").append(mapping.getFqxmlname());
    if (mapping.getFqpath() == null || 
        mapping.getFqpath().isEmpty()) {
      // root table name
      lineBuilder.append(" -> ").append(mapping.getTablename());
    } else if (mapping.getColumnname() == null ||
               mapping.getColumnname().isEmpty()) {
      // sub table name
      lineBuilder.append(".").append(mapping.getFqpath()).append(" -> ").append(mapping.getTablename());
    } else {
      // column
      lineBuilder.append(".").append(mapping.getFqpath()).append(" -> ").append(mapping.getTablename()).append('.').append(mapping.getColumnname());
    }
    return lineBuilder.toString();
  }
  
}
