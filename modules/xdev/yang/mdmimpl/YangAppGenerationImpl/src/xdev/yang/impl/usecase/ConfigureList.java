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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.XynaOrderServerExtension;
import xmcp.yang.LoadYangAssignmentsData;

public class ConfigureList {

  public void configure(XynaOrderServerExtension order, LoadYangAssignmentsData data, xmcp.yang.fman.ListConfiguration config) {
    String fqn = data.getFqn();
    String workspace = data.getWorkspaceName();
    String usecaseName = data.getUsecase();
    
    try(Usecase usecase = Usecase.open(order, fqn, workspace, usecaseName)) {
      Document meta = usecase.getMeta();
      updateMetaXml(meta, data, config);
      usecase.updateMeta();
      usecase.save();
      usecase.deploy();
    } catch(Exception e) {
      
    }
  }
  
  private void updateMetaXml(Document meta, LoadYangAssignmentsData data, xmcp.yang.fman.ListConfiguration config) {
    String yang = data.getTotalYangPath();
    String ns = data.getTotalNamespaces();
    String keywords = data.getTotalKeywords();
    ListConfiguration newConfig = ListConfiguration.fromDatatype(yang, ns, keywords, config);
    Element existingElement = ListConfiguration.loadListConfigurationElement(meta, yang, ns);
    if(existingElement == null) {
      newConfig.createAndAddElement(meta);
      return;
    }
    newConfig.updateNode(existingElement);
  }
}
