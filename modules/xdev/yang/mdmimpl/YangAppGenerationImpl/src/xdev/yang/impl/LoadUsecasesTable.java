/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xdev.yang.impl;



import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

import xmcp.yang.UseCaseTableData;
import xmcp.yang.YangUsecaseImplementation;



public class LoadUsecasesTable {


  public List<UseCaseTableData> loadUsecases() {
    try {
      List<UseCaseTableData> result = new ArrayList<>();
      List<UseCaseGroupDt> usecaseGroupDts = determineUsecaseGroupDatatypes();
      for (UseCaseGroupDt usecaseGroupDt : usecaseGroupDts) {
        List<UseCaseTableData> data = determineUsecasesOfGroup(usecaseGroupDt);
        result.addAll(data);
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private List<UseCaseTableData> determineUsecasesOfGroup(UseCaseGroupDt dt) {
    List<UseCaseTableData> result = new ArrayList<>();
    try {
      DOM datatype = DOM.getOrCreateInstance(dt.getFqn(), new GenerationBaseCache(), dt.getRevision());
      datatype.parseGeneration(false, false);
      List<Operation> operations = datatype.getOperations();
      for (Operation operation : operations) {
        int mappingCount = countMappings(operation);
        UseCaseTableData.Builder data = new UseCaseTableData.Builder();
        data.usecaseGroup(dt.getFqn()).useCase(operation.getName()).mappingCount(mappingCount);
        result.add(data.instance());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return result;
  }


  private int countMappings(Operation operation) {
    if (operation.getUnknownMetaTags() == null) {
      return 0;
    }
    for (String unknownMetaTag : operation.getUnknownMetaTags()) {
      try {
        Document xml = XMLUtils.parseString(unknownMetaTag, false);
        if (!xml.getDocumentElement().getNodeName().equals(Constants.TAG_YANG)) {
          continue;
        }
        Node yangTypeNode = xml.getDocumentElement().getAttributes().getNamedItem(Constants.ATT_YANG_TYPE);
        if (yangTypeNode == null || !Constants.VAL_USECASE.equals(yangTypeNode.getNodeValue())) {
          continue;
        }
        Element mappingsNode = XMLUtils.getChildElementByName(xml.getDocumentElement(), Constants.TAG_MAPPINGS);
        return XMLUtils.getChildElementsByName(mappingsNode, Constants.TAG_MAPPING).size();
      } catch (Exception e) {
        return -1;
      }
    }

    return 0;
  }


  private List<UseCaseGroupDt> determineUsecaseGroupDatatypes() throws Exception {
    List<UseCaseGroupDt> result = new ArrayList<>();
    XMOMDatabase xmomDb = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
    List<Long> revisions = determineWorkspaceRevisions();
    for (Long revision : revisions) {
      List<XMOMDatabaseSelect> selects = buildSelects(); // new selects are required for every XMOM database search
      XMOMDatabaseSearchResult xmomDbSearchResult = xmomDb.searchXMOMDatabase(selects, -1, revision);
      List<XMOMDatabaseSearchResultEntry> xmomDbResults = xmomDbSearchResult.getResult();
      for (XMOMDatabaseSearchResultEntry entry : xmomDbResults) {
        UseCaseGroupDt useCaseGroup = new UseCaseGroupDt(entry.getFqName(), revision);
        result.add(useCaseGroup);
      }
    }
    return result;
  }


  private List<XMOMDatabaseSelect> buildSelects() {
    List<XMOMDatabaseSelect> selects = new ArrayList<>();
    XMOMDatabaseSelect select = new XMOMDatabaseSelect();
    select.addAllDesiredResultTypes(List.of(XMOMDatabaseType.SERVICEGROUP, XMOMDatabaseType.DATATYPE));
    try {
      select.where(XMOMDatabaseEntryColumn.EXTENDS).isEqual(YangUsecaseImplementation.class.getCanonicalName());
    } catch (XNWH_WhereClauseBuildException e) {
      throw new RuntimeException(e);
    }
    selects.add(select);

    return selects;
  }


  private List<Long> determineWorkspaceRevisions() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    List<Long> result = new ArrayList<>();
    WorkspaceManagement workspaceMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<WorkspaceInformation> workspaces = workspaceMgmt.listWorkspaces(false);
    for (WorkspaceInformation workspace : workspaces) {
      Long revision = revMgmt.getRevision(workspace.asRuntimeContext());
      result.add(revision);
    }
    return result;
  }


  private static class UseCaseGroupDt {

    private final String fqn;
    private final Long revision;


    public UseCaseGroupDt(String fqn, Long revision) {
      this.fqn = fqn;
      this.revision = revision;
    }


    public String getFqn() {
      return fqn;
    }


    public Long getRevision() {
      return revision;
    }


  }
}
