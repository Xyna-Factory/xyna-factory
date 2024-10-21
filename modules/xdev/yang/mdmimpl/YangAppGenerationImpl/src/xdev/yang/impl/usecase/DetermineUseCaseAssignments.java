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
package xdev.yang.impl.usecase;



import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;
import xdev.yang.impl.XmomDbInteraction;

import org.yangcentral.yangkit.model.api.stmt.Module;

import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.UseCaseAssignmentTableData;
import xmcp.yang.YangDevice;



public class DetermineUseCaseAssignments {


  public List<UseCaseAssignmentTableData> determineUseCaseAssignments(LoadYangAssignmentsData data) {
    String fqn = data.getFqn();
    String workspaceName = data.getWorkspaceName();
    String usecase = data.getUsecase();
    List<UseCaseAssignmentTableData> result = new ArrayList<UseCaseAssignmentTableData>();
    Pair<Integer, Document> meta = UseCaseAssignmentUtils.loadOperationMeta(fqn, workspaceName, usecase);
    if(meta == null) {
      return result;
    }
    String rpcName = readRpcName(meta.getSecond());
    if(rpcName == null) {
      return result;
    }
    
    List<Module> modules = UseCaseAssignmentUtils.loadModules(workspaceName);
    List<String> moduleCapabilities = loadCapabilities(meta.getSecond(), workspaceName);
    modules.removeIf(x -> !isModuleInCapabilities(moduleCapabilities, x));
    result = UseCaseAssignmentUtils.loadPossibleAssignments(modules, rpcName, data);
    fillValues(meta.getSecond(), modules, result);

    return result;
  }

  private boolean isModuleInCapabilities(List<String> capabilities, Module module) {
    return capabilities.contains(module.getMainModule().getNamespace().getUri().toASCIIString());
  }

  private List<String> loadCapabilities(Document usecaseMeta, String workspaceName) {
    Element deviceFqnEle = XMLUtils.getChildElementByName(usecaseMeta.getDocumentElement(), Constants.TAG_DEVICE_FQN);
    String deviceFqn = deviceFqnEle.getTextContent();
    DOM deviceDatatype = loadDeviceDatatype(deviceFqn, workspaceName);
    List<String> unknownMetaTags = deviceDatatype.getUnknownMetaTags();
    Document deviceMeta = loadDeviceMeta(unknownMetaTags);
    Element  ele = XMLUtils.getChildElementByName(deviceMeta.getDocumentElement(), Constants.TAG_HELLO);
    ele = XMLUtils.getChildElementByName(ele, Constants.TAG_CAPABILITIES);
    List<Element> capabilities = XMLUtils.getChildElementsByName(ele, Constants.TAG_CAPABILITY);
    List<String> result = new ArrayList<String>();
    for(Element capability : capabilities) {
      result.add(capability.getTextContent());
    }
    return result;
  }
  

  private Document loadDeviceMeta(List<String> unknownMetaTags) {
    try {
      for (String unknownMetaTag : unknownMetaTags) {
        Document d = XMLUtils.parseString(unknownMetaTag);
        boolean isYang = d.getDocumentElement().getTagName().equals(Constants.TAG_YANG);
        boolean isDevice = Constants.VAL_DEVICE.equals(d.getDocumentElement().getAttribute(Constants.ATT_YANG_TYPE));
        if (isYang && isDevice) {
          return d;
        }
      }
      throw new RuntimeException("No Device Meta Tag found");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private DOM loadDeviceDatatype(String deviceFqn, String workspace) {
    XynaFactoryControl factoryControl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
    XmomDbInteraction interaction = new XmomDbInteraction();
    RevisionManagement rm = factoryControl.getRevisionManagement();
    Long revision;
    try {
      revision = rm.getRevision(null, null, workspace);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    RuntimeContextDependencyManagement rcdm = factoryControl.getRuntimeContextDependencyManagement();
    List<Long> revisions = new ArrayList<>(rcdm.getDependencies(revision));
    List<XMOMDatabaseSearchResultEntry> candidates = interaction.searchYangDTs(YangDevice.class.getCanonicalName(), revisions);
    for(XMOMDatabaseSearchResultEntry candidate : candidates) {
      if(candidate.getFqName().equals(deviceFqn)) {
        Long deviceRevision;
        try {
          deviceRevision = rm.getRevision(candidate.getRuntimeContext());
          DOM result = DOM.getOrCreateInstance(deviceFqn, new GenerationBaseCache(), deviceRevision);
          result.parseGeneration(true, false);
          return result;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    throw new RuntimeException("Could not find device datatype " + deviceFqn + " in " + workspace + " or dependencies");
  }


  private void fillValues(Document meta, List<Module> modules, List<UseCaseAssignmentTableData> entries) {
    List<UseCaseMapping> mappings = UseCaseMapping.loadMappings(meta);
    for (UseCaseAssignmentTableData entry : entries) {
      for (UseCaseMapping mapping : mappings) {
        if (mapping.getMappingYangPath().equals(entry.getLoadYangAssignmentsData().getTotalYangPath())) {
          entry.unversionedSetValue(mapping.getValue());
        }
      }
    }
  }
  
  private String readRpcName(Document meta) {
    Element rpcElement = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_RPC);
    if(rpcElement == null) {
      return null;
    }
    return rpcElement.getTextContent();
  }

}
