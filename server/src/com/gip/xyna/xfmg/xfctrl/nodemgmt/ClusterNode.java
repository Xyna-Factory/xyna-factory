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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.util.Collections;
import java.util.List;


public class ClusterNode {
  
  private ClusterNodeStorable nodeInformation; //Informationen zum Cluster Knoten

  
  public ClusterNode() {
    this.nodeInformation = new ClusterNodeStorable();
  }
  public ClusterNode(ClusterNodeStorable nodeInformation) {
    this.nodeInformation = nodeInformation;
  }
  
  
  public void setName(String name) {
    nodeInformation.setName(name);
  }

  public void setFactoryNodes(List<String> factoryNodes) {
    nodeInformation.setFactoryNodeNames(factoryNodes);
  }

  public void setDescription(String description) {
    nodeInformation.setDescription(description);
  }

  public String getName() {
    return nodeInformation.getName();
  }

  public String getDescription() {
    return nodeInformation.getDescription();
  }

  public List<String> getFactoryNodes() {
    return Collections.unmodifiableList(nodeInformation.getSerializableFactoryNodes());
  }
  public ClusterNodeStorable getNodeInformation() {
    return nodeInformation;
  }

}
