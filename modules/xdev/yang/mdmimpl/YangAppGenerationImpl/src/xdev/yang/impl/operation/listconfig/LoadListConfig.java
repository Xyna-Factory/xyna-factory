/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xdev.yang.impl.operation.listconfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xdev.yang.impl.operation.Operation;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.fman.ListConfiguration;

public class LoadListConfig {

  public ListConfiguration load(XynaOrderServerExtension order, LoadYangAssignmentsData data) {
    String fqn = data.getFqn();
    String workspace = data.getWorkspaceName();
    String operationName = data.getOperation();
    
    try(Operation operation = Operation.open(order, fqn, workspace, operationName)) {
      Document meta = operation.getMeta();
      String yang = data.getTotalYangPath();
      String ns = data.getTotalNamespaces();
      Element ele = xdev.yang.impl.operation.ListConfiguration.loadListConfigurationElement(meta, yang, ns);
      if(ele != null) {
        xdev.yang.impl.operation.ListConfiguration existing = xdev.yang.impl.operation.ListConfiguration.loadFromElement(ele);
        return new ListConfiguration.Builder().config(existing.getConfig().toConfigString()).instance();
      }
    } catch(Exception e) {
      
    }
    
    return new ListConfiguration();
  }
}
