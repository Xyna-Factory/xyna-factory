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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.AutosaveFilter;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.PreparedXMOMDatabaseSelect;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;


// TODO cache Document and GenerationBase of fqXmlName ?
public class RefactoringContext {
  
  private final Collection<? extends RefactoringElement> refactorings;
  private final AutosaveFilter autosaveFilter;
  private final String sessionId;
  private final String creator;
  private final Long revision;
  private boolean refactorOperation = false;
  
  private Set<DependencyNode> deployedDependencies;
  private Set<XMOMDatabaseSearchResultEntry> savedDependencies;
  
  
  
  public RefactoringContext(RefactoringElement refactoring, AutosaveFilter autosaveFilter, String sessionId, String creator, Long revision) {
    this(Collections.singleton(refactoring), autosaveFilter, sessionId, creator, revision);
  }
  
  public RefactoringContext(Collection<? extends RefactoringElement> refactorings, AutosaveFilter autosaveFilter, String sessionId, String creator, Long revision) {
    this.refactorings = refactorings;
    this.autosaveFilter = autosaveFilter;
    this.sessionId = sessionId;
    this.creator = creator;
    this.revision = revision;
  }

  
  public Collection<? extends RefactoringElement> getRefactoringElements() {
    return refactorings;
  }

  
  public String getSessionId() {
    return sessionId;
  }

  
  public String getCreator() {
    return creator;
  }
  
  
  public AutosaveFilter getAutosaveFilter() {
    return autosaveFilter;
  }

  
  public Long getRevision() {
    return revision;
  }
  
  public Set<DependencyNode> getDeployedDependencies() {
    if (deployedDependencies == null) {
      DependencyRegister dependencyRegister =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
      Set<DependencyNode> dependencies = new HashSet<DependencyNode>();
      for (RefactoringElement refactoring : refactorings) {
        if (refactoring.type != RefactoringTargetType.FORM) {
          dependencies.addAll(dependencyRegister.getDependencies(refactoring.fqXmlNameOld, refactoring.type.getDependencySourceType(), revision, true));
          // new does not exist in most cases, only relevant for moveOperation
          dependencies.addAll(dependencyRegister.getDependencies(refactoring.fqXmlNameNew, refactoring.type.getDependencySourceType(), revision, true));
          //unsch�n: dependencynode konstruktor ist protected. => workaround um den eigenen knoten ins set mit aufzunehmen (falls deployed). 
          DependencyNode ownNode =
              dependencyRegister.getDependencyNode(refactoring.fqXmlNameOld, DependencySourceType.WORKFLOW, revision);
          if (ownNode != null) {
            dependencies.add(ownNode);
          }
          ownNode = dependencyRegister.getDependencyNode(refactoring.fqXmlNameNew, DependencySourceType.WORKFLOW, revision);
          if (ownNode != null) {
            dependencies.add(ownNode);
          }
        }
      }
      deployedDependencies = dependencies;
    }
    return deployedDependencies;
  }

  
  public Set<XMOMDatabaseSearchResultEntry> getSavedDependencies() {
    if (savedDependencies == null) {
      List<XMOMDatabaseSelect> selects = new ArrayList<XMOMDatabaseSelect>();
      for (RefactoringElement refactoring : refactorings) {
        XMOMDatabaseSelect select = new XMOMDatabaseSelect();
        XMOMDatabaseEntryColumn[] columnsContainingDependencies;
        switch (refactoring.type) {
          case DATATYPE :
            columnsContainingDependencies = new XMOMDatabaseEntryColumn[] {
                            XMOMDatabaseEntryColumn.EXTENDEDBY,
                            XMOMDatabaseEntryColumn.POSSESSEDBY,
                            XMOMDatabaseEntryColumn.NEEDEDBY, 
                            XMOMDatabaseEntryColumn.PRODUCEDBY,
                            XMOMDatabaseEntryColumn.INSTANCESUSEDBY};
            break;
          case EXCEPTION :
            columnsContainingDependencies = new XMOMDatabaseEntryColumn[] {
                            XMOMDatabaseEntryColumn.EXTENDEDBY,
                            XMOMDatabaseEntryColumn.POSSESSEDBY, 
                            XMOMDatabaseEntryColumn.NEEDEDBY,
                            XMOMDatabaseEntryColumn.PRODUCEDBY, 
                            XMOMDatabaseEntryColumn.THROWNBY,
                            XMOMDatabaseEntryColumn.INSTANCESUSEDBY};
            break;
          case WORKFLOW :
            columnsContainingDependencies = new XMOMDatabaseEntryColumn[] {
                            XMOMDatabaseEntryColumn.CALLEDBY};
            break;
          case FORM :
            savedDependencies = Collections.emptySet();
            return savedDependencies;
          default :
            throw new IllegalArgumentException("Invalid XMOMType: " + refactoring.type);
        }
        
        try {
          select.addDesiredResultTypes(XMOMDatabaseType.GENERIC);
          for (XMOMDatabaseEntryColumn col : columnsContainingDependencies) {
            select.select(col);
          }
          select.where(XMOMDatabaseEntryColumn.FQNAME).isEqual(refactoring.fqXmlNameOld);
          select.and(select.newWhereClause().whereNumber(XMOMDatabaseEntryColumn.REVISION).isEqual(revision));
          selects.add(select);
          select = new XMOMDatabaseSelect();
          select.addDesiredResultTypes(XMOMDatabaseType.GENERIC);
          select.select(XMOMDatabaseEntryColumn.FQNAME);
          select.where(XMOMDatabaseEntryColumn.FQNAME).isEqual(refactoring.fqXmlNameOld);
          select.and(select.newWhereClause().whereNumber(XMOMDatabaseEntryColumn.REVISION).isEqual(revision));
          selects.add(select);
          if (refactoring.type == RefactoringTargetType.DATATYPE) {
            //servicegroups: alle operations der servicegroup suchen und schauen, wer sie verwendet. im entsprechenden domcache-entry steht leider nichts verwertbares drin.
            select = new XMOMDatabaseSelect();
            select.addDesiredResultTypes(XMOMDatabaseType.GENERIC);
            select.select(XMOMDatabaseEntryColumn.CALLEDBY);
            select.where(XMOMDatabaseEntryColumn.FQNAME).isLike(refactoring.fqXmlNameOld + "." + refactoring.nameOld + ".%");
            select.and(select.newWhereClause().whereNumber(XMOMDatabaseEntryColumn.REVISION).isEqual(revision));
            selects.add(select);
          }
        } catch (XNWH_WhereClauseBuildException e) {
          throw new RuntimeException(e);
        }
      }
      
    
      XMOMDatabase xmomDatabase = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      PreparedXMOMDatabaseSelect p = xmomDatabase.prepareSearch(selects);
      XMOMDatabaseSearchResult dependencies = xmomDatabase.executePreparedSelect(p, selects, -1, revision);
      
      List<XMOMDatabaseSearchResultEntry> dependencyEntries = dependencies.getResult();
      SortedSet<XMOMDatabaseSearchResultEntry> sorted = new TreeSet<XMOMDatabaseSearchResultEntry>(new Comparator<XMOMDatabaseSearchResultEntry>() {
  
        public int compare(XMOMDatabaseSearchResultEntry o1, XMOMDatabaseSearchResultEntry o2) {
          return o1.getFqName().compareTo(o2.getFqName());
        }
      });
      sorted.addAll(dependencyEntries);
      savedDependencies = sorted;
    }
    return savedDependencies;
  }

  public boolean isRefactorOperation() {
    return refactorOperation;
  }

  public void setRefactorOperation(boolean refactorOperation) {
    this.refactorOperation = refactorOperation;
  }
  
  

}
