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

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;

public class UseCaseMapping {

  private String mappingYangPath;
  private String namespace;
  private String value;
  
  public UseCaseMapping(String mappingYangPath, String namespace, String value) {
    this.mappingYangPath = mappingYangPath;
    this.namespace = namespace;
    this.value = value;
  }
  
  public static List<UseCaseMapping> loadMappings(Document document) {
    Element mappingsNode = XMLUtils.getChildElementByName(document.getDocumentElement(), Constants.TAG_MAPPINGS);
    List<Element> mappingNodes = XMLUtils.getChildElementsByName(mappingsNode, Constants.TAG_MAPPING);
    List<UseCaseMapping> result = new ArrayList<UseCaseMapping>();
    for(Element mappingNode : mappingNodes) {
      UseCaseMapping mapping = loadUseCaseMapping(mappingNode);
      result.add(mapping);
    }
    return result;
  }

  private static UseCaseMapping loadUseCaseMapping(Element e) {
    String yangPath = e.getAttribute(Constants.ATT_MAPPING_YANGPATH);
    String namespace = e.getAttribute(Constants.ATT_MAPPING_NAMESPACE);
    String value = e.getAttribute(Constants.ATT_MAPPING_VALUE);
    return new UseCaseMapping(yangPath, namespace, value);
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
}
