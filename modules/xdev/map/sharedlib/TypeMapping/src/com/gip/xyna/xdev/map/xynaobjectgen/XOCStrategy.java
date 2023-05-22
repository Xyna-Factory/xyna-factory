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
package com.gip.xyna.xdev.map.xynaobjectgen;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


/**
 *
 */
public class XOCStrategy {

  protected String targetId = null; 
  
  public String getTargetId(Node node, String defaultTargetId) {
    return targetId == null ? defaultTargetId : targetId;
  }

  public String getNamespace(Node node, String defaultNamespace) {
    String namespace = node.getNamespaceURI();
    return namespace == null ? defaultNamespace : namespace;
  }

  public String getTypeName(Node node) {
    return null;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public boolean canLookupFailureBeIgnored(String childKey) {
    return false;
  }

  protected String getAttributeValue(Node node, String name) {
    NamedNodeMap nnm = node.getAttributes();
    Node attribute = nnm.getNamedItem(name);
    if( attribute != null ) {
      return attribute.getNodeValue();
    }
    return null;
  }
  
  
}
