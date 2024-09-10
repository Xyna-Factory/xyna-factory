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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import org.yangcentral.yangkit.model.api.stmt.Module;

import xmcp.yang.UseCaseAssignementTableData;



public class DetermineUseCaseAssignments {


  public List<UseCaseAssignementTableData> determineUseCaseAssignments(String yangPath, String fqn, String usecase, String workspaceName) {
    List<UseCaseAssignementTableData> result = new ArrayList<UseCaseAssignementTableData>();
    Document meta = loadOperationMeta(fqn, workspaceName, usecase);
    if(meta == null) {
      return result;
    }
    String rpcName = readRpcName(meta);
    if(rpcName == null) {
      return null;
    }
    
    List<Module> modules = UseCaseAssignmentUtils.loadModules(workspaceName);
    result = UseCaseAssignmentUtils.loadPossibleAssignments(modules, yangPath, rpcName);
    fillValues(modules, result);

    return result;
  }


  private void fillValues(List<Module> modules, List<UseCaseAssignementTableData> result) {
    // TODO set values  
  }


  private String readRpcName(Document meta) {
    Element rpcElement = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_RPC);
    if(rpcElement == null) {
      return null;
    }
    return rpcElement.getNodeValue();
  }


  private Document loadOperationMeta(String fqn, String workspaceName, String usecase) {
    try {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(null, null, workspaceName);
      DOM dom = DOM.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      dom.parseGeneration(false, false);
      Operation operation = dom.getOperationByName(usecase);
      List<String> unknownMetaTags = operation.getUnknownMetaTags();
      if(unknownMetaTags == null) {
        return null;
      }
      for(String unknownMetaTag : unknownMetaTags) {
        Document d = XMLUtils.parseString(unknownMetaTag);
        boolean isYang = d.getDocumentElement().getTagName().equals(Constants.TAG_YANG);
        boolean isUseCase = Constants.VAL_USECASE.equals(d.getDocumentElement().getAttribute(Constants.ATT_YANG_TYPE));
        if(isYang && isUseCase) {
          return d;
        }
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
