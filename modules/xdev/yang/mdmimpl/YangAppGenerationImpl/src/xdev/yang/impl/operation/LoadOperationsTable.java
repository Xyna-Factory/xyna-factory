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
package xdev.yang.impl.operation;



import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;
import xdev.yang.impl.XmomDbInteraction;

import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

import xmcp.yang.OperationTableData;
import xmcp.yang.YangOperationImplementation;



public class LoadOperationsTable {


  public List<OperationTableData> loadOperations() {
    try {
      List<OperationTableData> result = new ArrayList<>();
      List<OperationGroupDt> operationGroupDts = determineOperationGroupDatatypes();
      for (OperationGroupDt operationGroupDt : operationGroupDts) {
        List<OperationTableData> data = determineOperationsOfGroup(operationGroupDt);
        result.addAll(data);
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private List<OperationTableData> determineOperationsOfGroup(OperationGroupDt dt) {
    List<OperationTableData> result = new ArrayList<>();
    try {
      DOM datatype = DOM.getOrCreateInstance(dt.getFqn(), new GenerationBaseCache(), dt.getRevision());
      datatype.parseGeneration(false, false);
      List<Operation> operations = datatype.getOperations();
      Document xml;
      for (Operation operation : operations) {
        xml = OperationAssignmentUtils.findYangTypeTag(operation);
        if (!OperationAssignmentUtils.isYangType(xml, Constants.VAL_OPERATION)) {
          continue;
        }
        OperationTableData.Builder data = new OperationTableData.Builder();
        data.operationGroup(dt.getFqn());
        data.rpcName(OperationAssignmentUtils.readRpcName(xml));
        data.rpcNamespace(OperationAssignmentUtils.readRpcNamespace(xml));
        data.tagName(OperationAssignmentUtils.readTagName(xml));
        data.tagNamespace(OperationAssignmentUtils.readTagNamespace(xml));
        data.yangKeyword(OperationAssignmentUtils.readYangKeyword(xml));
        data.operation(operation.getName());
        data.mappingCount(countMappings(xml));
        data.runtimeContext(datatype.getRuntimeContext().getName());
        data.isConfig(OperationAssignmentUtils.readIsConfig(xml));
        result.add(data.instance());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return result;
  }


  private int countMappings(Document xml) {
    Element mappingsNode = XMLUtils.getChildElementByName(xml.getDocumentElement(), Constants.TAG_MAPPINGS);
    return XMLUtils.getChildElementsByName(mappingsNode, Constants.TAG_MAPPING).size();
  }


  private List<OperationGroupDt> determineOperationGroupDatatypes() throws Exception {
    List<OperationGroupDt> result = new ArrayList<>();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<Long> revisions = determineWorkspaceRevisions();
    XmomDbInteraction interaction = new XmomDbInteraction();
    List<XMOMDatabaseSearchResultEntry> xmomDbResults = interaction.searchYangDTs(YangOperationImplementation.class.getCanonicalName(), revisions);
      for (XMOMDatabaseSearchResultEntry entry : xmomDbResults) {
        Long revision = revMgmt.getRevision(entry.getRuntimeContext());
        OperationGroupDt operationGroup = new OperationGroupDt(entry.getFqName(), revision);
        result.add(operationGroup);
      }
    return result;
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


  private static class OperationGroupDt {

    private final String fqn;
    private final Long revision;


    public OperationGroupDt(String fqn, Long revision) {
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
