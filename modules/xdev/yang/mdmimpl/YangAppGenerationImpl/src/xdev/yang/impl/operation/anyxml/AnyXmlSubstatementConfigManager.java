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
package xdev.yang.impl.operation.anyxml;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;
import xdev.yang.impl.operation.Operation;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.fman.AnyXmlSubstatementConfiguration;



public class AnyXmlSubstatementConfigManager {

  public void configure(XynaOrderServerExtension order, LoadYangAssignmentsData data, AnyXmlSubstatementConfiguration config) {
    String fqn = data.getFqn();
    String workspace = data.getWorkspaceName();
    String operationName = data.getOperation();
    
    try(Operation operation = Operation.open(order, fqn, workspace, operationName)) {
      Document meta = operation.getMeta();
      updateMetaXml(meta, data, config);
      operation.updateMeta();
      operation.save();
      operation.deploy();
    } catch(Exception e) {
      
    }
  }
  
  public List<AnyXmlConfigTag> loadAnyXmlConfigs(Document meta) {
    List<AnyXmlConfigTag> result = new ArrayList<>();
    Element configListNode = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_ANYXMLCONFIGS);
    if(configListNode == null) {
      return result;
    }
    
    List<Element> xmlTags = XMLUtils.getChildElementsByName(configListNode, Constants.TAG_ANYXMLCONFIG);
    for(Element xmlTag : xmlTags) {
      result.add(AnyXmlConfigTag.fromElement(xmlTag));
    }
    
    return result;
  }
  
  private void updateMetaXml(Document meta, LoadYangAssignmentsData data, AnyXmlSubstatementConfiguration config) {
    String yang = data.getTotalYangPath();
    String ns = data.getTotalNamespaces();
    String tag = config.getConfig().substring(0, config.getConfig().indexOf(":"));
    String substatementNamespace = config.getConfig().substring(config.getConfig().indexOf(":") + 1);
    AnyXmlConfigTag tagToConfigure = new AnyXmlConfigTag(yang, ns, tag, substatementNamespace);
    if(XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_ANYXMLCONFIGS) == null) {
      Element ele = meta.createElement(Constants.TAG_ANYXMLCONFIGS);
      meta.getDocumentElement().appendChild(ele);
    }
    Element existingElement = findAnyXmlConfigNode(meta, tagToConfigure);
    if(existingElement != null && !config.getAdd()) {
      existingElement.getParentNode().removeChild(existingElement);
    } else if(existingElement == null && config.getAdd()) {
      tagToConfigure.addToDocument(meta);
    }
  }
  
  
  private Element findAnyXmlConfigNode(Document meta, AnyXmlConfigTag toFind) {
    Element el = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_ANYXMLCONFIGS);
    List<Element> candidates = XMLUtils.getChildElementsByName(el, Constants.TAG_ANYXMLCONFIG);
    for(Element candidate : candidates) {
      AnyXmlConfigTag candidateObject = AnyXmlConfigTag.fromElement(candidate);
      if(toFind.equals(candidateObject)) {
        return candidate;
      }
    }
    return null;
  }
  
  
  public static class AnyXmlConfigTag {

    private String yang;
    private String namespaces;
    private String tag;
    private String namespace;


    public AnyXmlConfigTag(String yang, String namespaces, String tag, String namespace) {
      this.yang = yang;
      this.namespaces = namespaces;
      this.tag = tag;
      this.namespace = namespace;
    }


    public void addToDocument(Document meta) {
      Element el = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_ANYXMLCONFIGS);
      Element newChild = meta.createElement(Constants.TAG_ANYXMLCONFIG);
      newChild.setAttribute(Constants.ATT_ANYXMLCONFIG_YANG, yang);
      newChild.setAttribute(Constants.ATT_ANYXMLCONFIG_NAMESPACES, namespaces);
      newChild.setAttribute(Constants.ATT_ANYXMLCONFIG_TAG, tag);
      newChild.setAttribute(Constants.ATT_ANYXMLCONFIG_NAMESPACE, namespace);
      el.appendChild(newChild);
    }

    public static AnyXmlConfigTag fromElement(Element el) {
      String keywords = el.getAttribute(Constants.ATT_ANYXMLCONFIG_YANG);
      String namespaces = el.getAttribute(Constants.ATT_ANYXMLCONFIG_NAMESPACES);
      String tag = el.getAttribute(Constants.ATT_ANYXMLCONFIG_TAG);
      String namespace = el.getAttribute(Constants.ATT_ANYXMLCONFIG_NAMESPACE);
      return new AnyXmlConfigTag(keywords, namespaces, tag, namespace);
    }
    
    @Override
    public boolean equals(Object obj) {
      if(obj == null) {
        return false;
      }
      if(!(obj instanceof AnyXmlConfigTag)) {
        return false;
      }
      AnyXmlConfigTag other  = (AnyXmlConfigTag)obj;
      return
          Objects.equals(yang, other.yang) &&
          Objects.equals(namespaces, other.namespaces) &&
          Objects.equals(tag, other.tag) &&
          Objects.equals(namespace, other.namespace);
    }


    
    public String getYang() {
      return yang;
    }


    
    public String getNamespaces() {
      return namespaces;
    }


    
    public String getTag() {
      return tag;
    }


    
    public String getNamespace() {
      return namespace;
    }
  }
}
