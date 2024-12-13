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
import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;

public class UseCaseMapping implements Comparable<UseCaseMapping> {

  private String mappingYangPath;
  private String namespace;
  private String value;
  private String keyword;
  
  public UseCaseMapping(String mappingYangPath, String namespace, String value, String keyword) {
    this.mappingYangPath = mappingYangPath;
    this.namespace = namespace;
    this.value = value;
    this.keyword = keyword;
  }
  
  public static Element loadMappingsElement(Document document) {
    return XMLUtils.getChildElementByName(document.getDocumentElement(), Constants.TAG_MAPPINGS);
  }
  
  public static List<Element> loadMappingElements(Document document) {
    Element mappingsNode = XMLUtils.getChildElementByName(document.getDocumentElement(), Constants.TAG_MAPPINGS);
    return XMLUtils.getChildElementsByName(mappingsNode, Constants.TAG_MAPPING);
  }
  
  public static List<UseCaseMapping> loadMappings(Document document) {
    List<Element> mappingNodes = loadMappingElements(document);
    List<UseCaseMapping> result = new ArrayList<UseCaseMapping>();
    for(Element mappingNode : mappingNodes) {
      UseCaseMapping mapping = loadUseCaseMapping(mappingNode);
      result.add(mapping);
    }
    return result;
  }

  public static UseCaseMapping loadUseCaseMapping(Element e) {
    String yangPath = e.getAttribute(Constants.ATT_MAPPING_YANGPATH);
    String namespace = e.getAttribute(Constants.ATT_MAPPING_NAMESPACE);
    String value = e.getAttribute(Constants.ATT_MAPPING_VALUE);
    String keyword = e.getAttribute(Constants.ATT_MAPPING_KEYWORD);
    return new UseCaseMapping(yangPath, namespace, value, keyword);
  }
  
  public void updateNode(Element e) {
    e.setAttribute(Constants.ATT_MAPPING_YANGPATH, mappingYangPath);
    e.setAttribute(Constants.ATT_MAPPING_NAMESPACE, namespace);
    e.setAttribute(Constants.ATT_MAPPING_VALUE, value);
    e.setAttribute(Constants.ATT_MAPPING_KEYWORD, keyword);
  }
  

  public void createAndAddElement(Document meta) {
    Element newMappingEle = meta.createElement(Constants.TAG_MAPPING);
    updateNode(newMappingEle);
    Element mappingsEle = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_MAPPINGS);
    mappingsEle.appendChild(newMappingEle);
  }


  public List<MappingPathElement> createPathList() {
    return createPathList(mappingYangPath, namespace, keyword);
  }
  
  
  public static List<MappingPathElement> createPathList(String totalYangPath, String totalNamespaces, String totalKeywords) {
    List<MappingPathElement> result = new ArrayList<>();
    String[] yangPathElements = totalYangPath.split("\\/");
    String[] namespaceElements = totalNamespaces.split(Constants.NS_SEPARATOR);
    String[] totalKeywordElements = totalKeywords.split(" ");
    if (yangPathElements.length != namespaceElements.length) {
      throw new RuntimeException("yangPathElement count does not match namespace: " + yangPathElements.length + ": "
          + namespaceElements.length);
    }

    for (int i = 0; i < yangPathElements.length; i++) {
      MappingPathElement element = new MappingPathElement(yangPathElements[i], namespaceElements[i], totalKeywordElements[i]);
      result.add(element);
    }
    
    return result;
  }
  

  public boolean match(List<MappingPathElement> pathList) {
    List<MappingPathElement> myPathList = createPathList();
    if (myPathList.size() != pathList.size()) {
      return false;
    }

    for (int i = 0; i < myPathList.size(); i++) {
      if (!Objects.equals(myPathList.get(i), pathList.get(i))) {
        return false;
      }
    }

    return true;
  }

  

  public String getMappingYangPath() {
    return mappingYangPath;
  }

  
  public void setMappingYangPath(String mappingYangPath) {
    this.mappingYangPath = mappingYangPath;
  }

  
  public String getNamespace() {
    return namespace;
  }

  
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  
  public String getValue() {
    return value;
  }


  public void setValue(String value) {
    this.value = value;
  }


  @Override
  public int compareTo(UseCaseMapping o) {
    List<MappingPathElement> pathList = createPathList();
    List<MappingPathElement> otherPathList = o.createPathList();
    return MappingPathElement.compareLists(pathList, otherPathList);
  }


}
