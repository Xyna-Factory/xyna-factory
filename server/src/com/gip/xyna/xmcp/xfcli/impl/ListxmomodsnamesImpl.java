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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listxmomodsnames;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation.ColumnInfoRecursionMode;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarDefinitionSite;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;



public class ListxmomodsnamesImpl extends XynaCommandImplementation<Listxmomodsnames> {
  

  public void execute(OutputStream statusOutputStream, Listxmomodsnames payload) throws XynaException {
    if (payload.getVersionName() != null && payload.getApplicationName() == null) {
      writeLineToCommandLine(statusOutputStream, "You may not specify a version name without an application name");
      return;
    }
    
    final RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<Long> possibleRevisions = new ArrayList<>();

    for (Application a : rm.getApplications()) {
      if ((payload.getWorkspaceName() == null && payload.getApplicationName() == null)
          || a.getName().equals(payload.getApplicationName())) {
        if (payload.getVersionName() == null || payload.getVersionName().equals(a.getVersionName()))
          possibleRevisions.add(rm.getRevision(a));
      }
    }
    for (Workspace w : rm.getWorkspaces().values()) {
      if ((payload.getWorkspaceName() == null && payload.getApplicationName() == null)
          || w.getName().equals(payload.getWorkspaceName())) {
        if (payload.getVersionName() == null)
          possibleRevisions.add(rm.getRevision(w));
      }
    }

    List<XMOMStorableStructureInformation> foundEntries = new ArrayList<>();
    
    for (Long aRevision : possibleRevisions) {
      XMOMStorableStructureCache cache = XMOMStorableStructureCache.getInstance(aRevision);
      for (XMOMStorableStructureInformation info : cache.getAllStorableStructureInformation()) {
        if (fits(info, payload)) {
          if (info.getSuperEntry() == null) {
            foundEntries.add(info);
          }
        }
      }
    }


    if (foundEntries.size() == 0) {
      StringBuilder sb = new StringBuilder("There are no known XMOM Storables");
      if (payload.getApplicationName() != null || payload.getFqDatatypeName() != null) {
        sb.append(" fitting the requested constraints");
      }
      sb.append(".");
      writeLineToCommandLine(statusOutputStream, sb.toString());
      return;
    }
    writeLineToCommandLine(statusOutputStream, "Mapping of XMOM Storables to ODS Storables");
    
    
    Collections.sort(foundEntries, 
                     Comparator.comparing(XMOMStorableStructureInformation::getFqXmlName)
                               .thenComparing(x -> {try { // we could just compare revisions but that would change the order of comparison
                                                      return rm.getRuntimeContext(x.getDefiningRevision());
                                                    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                                                      return new Application("invalid", "rev=" + x.getDefiningRevision());
                                                    }
                                                   }));

    for (XMOMStorableStructureInformation entry : foundEntries) {
      printStructure(statusOutputStream, entry, payload.getShowcolumns(), payload.getVerbose());
      writeLineToCommandLine(statusOutputStream, Constants.LINE_SEPARATOR);
    }
    
  }
  


  private void printStructure(OutputStream statusOutputStream, XMOMStorableStructureInformation entry, boolean includePrimitives, boolean verbose) {
    StringBuilder sb = new StringBuilder();
    sb.append(" - ").append(entry.getFqXmlName());
    appendRuntimeContext(sb, entry);

    int len = sb.length();
    if (sb.toString().startsWith("\n")) { // Zeilenumbruch ausgleichen
      len--;
    }
    for (int i = 0; i < Math.max(4, 65 - len); i++) { // Minimum 4 Leerzeichen
      sb.append(" ");
    }
    sb.append(" -> ").append(entry.getTableName()).append(Constants.LINE_SEPARATOR);
    String identation = "    ";
    if (entry.getSubEntries() != null &&
        entry.getSubEntries().size() > 0) {
      sb.append(identation).append("tablename inherited by:").append(Constants.LINE_SEPARATOR);
      for (StorableStructureIdentifier ssi : entry.getSubEntries()) {
        sb.append(identation).append("  ").append(ssi.getInfo().getFqXmlName());
        appendRuntimeContext(sb, ssi.getInfo());
        sb.append(Constants.LINE_SEPARATOR);
      }
    }
    if (includePrimitives) {
      printPrimitives(sb, entry, "",  identation);
      
    }
    for (StorableColumnInformation sci : entry.getAllComplexColumns()) {
      printStructureRecursivly(sb, sci, "", identation, true, includePrimitives, true, verbose);
    }
    if (entry.getSubEntries() != null) {
      for (StorableStructureIdentifier ssi : entry.getSubEntries()) {
        printStructureRecursivly(sb, ssi.getInfo(), "", identation, false, includePrimitives, false, verbose);
      }
    }
    writeToCommandLine(statusOutputStream, sb);
  }
  
  private void printStructureRecursivly(StringBuilder sb, StorableStructureInformation entry, String previousPath, String indentation, boolean includingInherited, boolean includePrimitives, boolean visitSuper, boolean verbose) {
    String nextIdentation = indentation + "  ";
    if (entry.getAllComplexColumns().size() > 0 ||
        includePrimitives) {
      sb.append(indentation).append("added columns from ");
      sb.append(entry.getFqXmlName());
      appendRuntimeContext(sb, entry); 
      sb.append(Constants.LINE_SEPARATOR);
      if (includePrimitives) {
        printPrimitives(sb, entry, previousPath,  nextIdentation);
      }
      for (StorableColumnInformation sci : entry.getAllComplexColumns()) {
        printStructureRecursivly(sb, sci, previousPath, nextIdentation, true, includePrimitives, true, verbose);
      }
    }
    if (entry.getSubEntries() != null) {
      for (StorableStructureIdentifier ssi : entry.getSubEntries()) {
        printStructureRecursivly(sb, ssi.getInfo(), previousPath, nextIdentation, false, includePrimitives, false, verbose);
      }
    }
  }
  
  
  private void printStructureRecursivly(StringBuilder sb, StorableColumnInformation sci, String previousPath, String indentation, boolean includingInherited, boolean includePrimitives, boolean visitSuper, boolean verbose) {
    if (!sci.isStorableVariable()) {
      return;
    }
    StorableStructureInformation ssi = sci.getStorableVariableInformation();
    if (ssi instanceof XMOMStorableStructureInformation) {
      return; // don't output ref tables
    }
    String path = previousPath + (previousPath.isEmpty() ? "" : ".");
    if (sci.isFlattened()) {
      path += sci.getPath();
    } else {
      path += sci.getVariableName();
    }
    sb.append(indentation).append(path);
    if (verbose) {
      sb.append(" - ").append(ssi.getFqXmlName());
    }
    appendRuntimeContext(sb, ssi);

    sb.append(" -> ").append(ssi.getTableName());
    sb.append(Constants.LINE_SEPARATOR);
    if (includePrimitives) {
      printPrimitives(sb, ssi, path,  indentation);
    }
    for (StorableColumnInformation column : ssi.getAllComplexColumns()) {
      printStructureRecursivly(sb, column, path, indentation, false, includePrimitives, true, verbose);
    }
    if (visitSuper) {
      StorableStructureIdentifier superEntry = ssi.getSuperEntry();
      while (superEntry != null) {
        printStructureRecursivly(sb, superEntry.getInfo(), previousPath, indentation + "  ", false, includePrimitives, false, verbose);
        superEntry = superEntry.getInfo().getSuperEntry();
      }
    }
    if (ssi.getSubEntries() != null) {
      for (StorableStructureIdentifier sub : ssi.getSubEntries()) {
        printStructureRecursivly(sb, sub.getInfo(), previousPath, indentation + "  ", false, includePrimitives, false, verbose);
      }
    }
    
  }
  
  private void printPrimitives(StringBuilder sb, StorableStructureInformation entry, String previousPath, String indentation) {
    for (StorableColumnInformation sci : entry.getColumnInfo(ColumnInfoRecursionMode.ONLY_LOCAL)) {
      if (sci.getDefinitionSite() != VarDefinitionSite.DATATYPE &&
          !sci.isStorableVariable()) {
        String label;
        if (sci.isFlattened()) {
          label = sci.getPath();
        } else {
          if (sci.getVariableName() == null) {
            if (sci.getType() == VarType.DEFAULT) {
              label = sci.getColumnName();
            } else {
              label = sci.getType().toString();
            }
          } else {
            label = sci.getVariableName();
            if (sci.getType() != VarType.DEFAULT) {
              label += " (" + sci.getType().toString() + ")";
            }
          }
        }
        String path = (previousPath.length() > 0 ? (previousPath + ".") : "") + label;
        sb.append(indentation).append("  [column]  ").append(path).append(" -> ");
        sb.append(sci.getColumnName());
        sb.append(Constants.LINE_SEPARATOR);
      }
    }
  }
  
  final RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  
  private void appendRuntimeContext(StringBuilder sb, StorableStructureInformation entry) {
    if (!RevisionManagement.REVISION_DEFAULT_WORKSPACE.equals(entry.getRevision())) {
      RuntimeContext r;
      try {
        r = rm.getRuntimeContext(entry.getRevision());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        r = new Application("invalid", "rev=" + entry.getRevision());
      }
      if (r instanceof Application) {
        sb.append(" ['").append(r.getName()).append("' / '").append(((Application) r).getVersionName()).append("']");
      } else {
        sb.append(" [").append(r).append("]");
      }
    }
  }
  
  
  private boolean fits(XMOMStorableStructureInformation info, Listxmomodsnames payload) {
    if (payload.getFqDatatypeName() != null) {
      if (!info.getFqXmlName().equals(payload.getFqDatatypeName())) {
        return false;
      }
    }
    return true;
  }

}
