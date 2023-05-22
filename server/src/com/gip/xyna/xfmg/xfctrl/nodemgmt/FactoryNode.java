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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

public class FactoryNode {

  private FactoryNodeStorable nodeInformation; //Informationen zum Factory Knoten
  private InterFactoryLink interFactoryLink; //Informationen zum Zugriff auf den Knoten

  public FactoryNode(FactoryNodeStorable nodeInformation) {
    super();
    this.nodeInformation = nodeInformation;
  }
  
  public FactoryNode(FactoryNodeStorable nodeInformation, InterFactoryLink interFactoryLink) {
    super();
    this.nodeInformation = nodeInformation;
    this.interFactoryLink = interFactoryLink;
  }

  public FactoryNodeStorable getNodeInformation() {
    return nodeInformation;
  }
  
  public InterFactoryLink getInterFactoryLink() {
    return interFactoryLink;
  }

}
