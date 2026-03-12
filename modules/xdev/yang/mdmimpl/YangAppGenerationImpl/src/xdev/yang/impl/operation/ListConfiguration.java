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
package xdev.yang.impl.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;
import xmcp.yang.ListKeyName;


public class ListConfiguration {

  private String yang;
  private String namespaces;
  private String keywords;
  private ListLengthConfig config;
  private List<String> listKeyNames = new ArrayList<>();
  

  public ListConfiguration(String yang, String namespaces, String keywords, ListLengthConfig config,
                           List<String> listKeyNames) {
    this.yang = yang;
    this.namespaces = namespaces;
    this.keywords = keywords;
    this.config = config;
    if (listKeyNames != null) {
      this.listKeyNames.addAll(listKeyNames);
    }
  }
  
  public static ListConfiguration loadFromElement(Element element) {
    String yang = element.getAttribute(Constants.ATT_LIST_CONFIG_YANG);
    String namespace = element.getAttribute(Constants.ATT_LIST_CONFIG_NS);
    String keywords = element.getAttribute(Constants.ATT_LIST_CONFIG_KEYWORDS);
    ListLengthConfig config = ListLengthConfig.loadFromElement(element);
    List<String> listKeys = loadListKeyNamesFromXml(element);
    return new ListConfiguration(yang, namespace, keywords, config, listKeys);
  }
  
  private static List<String> loadListKeyNamesFromXml(Element element) {
    List<String> ret = new ArrayList<>();
    Element child = XMLUtils.getChildElementByName(element, Constants.TAG_LISTKEYS);
    if (child == null) { return ret; }
    List<Element> list = XMLUtils.getChildElementsByName(child, Constants.TAG_LISTKEY);
    if (list == null) { return ret; }
    for (Element name : list) {
      if (name == null) { continue; }
      String val = XMLUtils.getTextContentOrNull(name);
      if (val != null) {
        ret.add(val);
      }
    }
    return ret;
  }
  
  public void updateNode(Document document, Element element) {
    element.setAttribute(Constants.ATT_LIST_CONFIG_YANG, yang);
    element.setAttribute(Constants.ATT_LIST_CONFIG_NS, namespaces);
    element.setAttribute(Constants.ATT_LIST_CONFIG_KEYWORDS, keywords);
    addListKeyNodes(document, element);
    config.updateNode(element);
  }
  
  private void addListKeyNodes(Document document, Element element) {
    Element child = document.createElement(Constants.TAG_LISTKEYS);
    element.appendChild(child);
    for (String val : listKeyNames) {
      Element name = document.createElement(Constants.TAG_LISTKEY);
      child.appendChild(name);
      Text text = document.createTextNode(val);
      name.appendChild(text);
    }
  }

  public void createAndAddElement(Document document) {
    Element listConfigurationsElement = loadListConfigurationsElement(document);
    Element newEntryNode = document.createElement(Constants.TAG_LISTCONFIG);
    updateNode(document, newEntryNode);
    listConfigurationsElement.appendChild(newEntryNode);
  }

  public static Element loadListConfigurationsElement(Document document) {
    return XMLUtils.getChildElementByName(document.getDocumentElement(), Constants.TAG_LISTCONFIGS);
  }
  
  public static List<Element> loadListConfigurationElements(Document document) {
    Element listConfigsElement = loadListConfigurationsElement(document);
    return XMLUtils.getChildElementsByName(listConfigsElement, Constants.TAG_LISTCONFIG);
  }
  
  public static Element loadListConfigurationElement(Document document, String yangPath, String namespaces) {
    List<Element> listConfigElements = loadListConfigurationElements(document);
    for(Element element : listConfigElements) {
      if(yangPath.equals(element.getAttribute(Constants.ATT_LIST_CONFIG_YANG)) &&
          namespaces.equals(element.getAttribute(Constants.ATT_LIST_CONFIG_NS))) {
        return element;
      }
    }
    return null;
  }

  public static List<ListConfiguration> loadListConfigurations(Document document) {
    List<ListConfiguration> result = new ArrayList<ListConfiguration>();
    List<Element> listConfigElements = loadListConfigurationElements(document);
    for(Element listConfigElement: listConfigElements) {
      ListConfiguration listConfig = ListConfiguration.loadFromElement(listConfigElement);
      result.add(listConfig);
    }
    
    return result;
  }
  
  public static ListConfiguration fromDatatype(String yang, String ns, String keywords, xmcp.yang.fman.ListConfiguration dtConfig,
                                               List<? extends ListKeyName> listKeyNames) {
    ListLengthConfig config;
    String configString = dtConfig.getConfig();
    if(configString.isBlank()) {
      return new ListConfiguration(yang, ns, keywords, new ConstantListLengthConfig(0), null);
    }
    if(configString.contains(":")) {
      String variable = configString.substring(0, configString.indexOf(":"));
      String path = configString.substring(configString.indexOf(":") + 1);
      config = new DynamicListLengthConfig(variable, path);
    } else {
      config = new ConstantListLengthConfig(Integer.valueOf(configString));
    }
    return new ListConfiguration(yang, ns, keywords, config, adaptListKeys(listKeyNames));
  }
  
  
  private static List<String> adaptListKeys(List<? extends ListKeyName> listKeyNames) {
    List<String> ret = new ArrayList<>();
    if (listKeyNames == null) { return ret; }
    for (ListKeyName name : listKeyNames) {
      if (name == null) { continue; }
      if (name.getListKeyName() == null) { continue; }
      ret.add(name.getListKeyName());
    }
    return ret;
  }
  
  
  public String getYang() {
    return yang;
  }

  
  public String getNamespaces() {
    return namespaces;
  }

  
  public String getKeywords() {
    return keywords;
  }

  public ListLengthConfig getConfig() {
    return config;
  }

  public List<String> getListKeyNames() {
    return listKeyNames;
  }
  
  public void setConfig(ListLengthConfig config) {
    this.config = config;
  }

  public abstract static class ListLengthConfig {
    
    private static final Map<String, Function<Element, ListLengthConfig>> loadFromElementFunctions = setupLoadFunctions();
    
    private static Map<String, Function<Element, ListLengthConfig>> setupLoadFunctions() {
      Map<String, Function<Element, ListLengthConfig>> result = new HashMap<>();
      result.put(Constants.VAL_LIST_CONFIG_CONSTANT, ConstantListLengthConfig::loadFromElement);
      result.put(Constants.VAL_LIST_CONFIG_DYNAMIC, DynamicListLengthConfig::loadFromElement);
      return result;
    }
    
    public abstract void updateNode(Element element);
    public abstract int getNumberOfCandidateEntries();
    public abstract String createCandidateName(int i);
    public abstract String toConfigString();
    
    public static ListLengthConfig loadFromElement(Element element) {
      String type = element.getAttribute(Constants.ATT_LIST_CONFIG_TYPE);
      return loadFromElementFunctions.get(type).apply(element);
    }
  }
  
  public static class ConstantListLengthConfig extends ListLengthConfig {
    private int length;
    
    public static ConstantListLengthConfig loadFromElement(Element element) {
      int length = Integer.valueOf(element.getAttribute(Constants.ATT_LIST_CONFIG_CONSTANT_LENGTH));
      return new ConstantListLengthConfig(length);
    }
    
    @Override
    public void updateNode(Element element) {
      element.setAttribute(Constants.ATT_LIST_CONFIG_TYPE, Constants.VAL_LIST_CONFIG_CONSTANT);
      element.setAttribute(Constants.ATT_LIST_CONFIG_CONSTANT_LENGTH, String.valueOf(length));
    }

    @Override
    public int getNumberOfCandidateEntries() {
      return length;
    }
    
    @Override
    public String createCandidateName(int i) {
      return String.valueOf(i);
    }
    
    @Override
    public String toConfigString() {
      return String.valueOf(length);
    }
    
    public ConstantListLengthConfig(int length) {
      this.length = length;
    }

    
    public int getLength() {
      return length;
    }

    
    public void setLength(int length) {
      this.length = length;
    }
    
  }
  
  public static class DynamicListLengthConfig extends ListLengthConfig {
    private String variable;
    private String path;
    
    public DynamicListLengthConfig(String variable, String path) {
      this.variable = variable;
      this.path = path;
    }
    
    public static DynamicListLengthConfig loadFromElement(Element element) {   
      String variable = element.getAttribute(Constants.ATT_LIST_CONFIG_DYNAMIC_VARIABLE);
      String path = element.getAttribute(Constants.ATT_LIST_CONFIG_DYNAMIC_PATH);
      return new DynamicListLengthConfig(variable, path);
    }
    
    public void updateNode(Element element) {
      element.setAttribute(Constants.ATT_LIST_CONFIG_TYPE, Constants.VAL_LIST_CONFIG_DYNAMIC);
      element.setAttribute(Constants.ATT_LIST_CONFIG_DYNAMIC_VARIABLE, variable);
      element.setAttribute(Constants.ATT_LIST_CONFIG_DYNAMIC_PATH, path);
    }
    
    @Override
    public int getNumberOfCandidateEntries() {
      return 1;
    }
    
    @Override
    public String createCandidateName(int i) {
      return variable;
    }
    
    @Override
    public String toConfigString() {
      return String.format("%s:%s", variable, path);
    }
    
    public String getVariable() {
      return variable;
    }
    
    public String getPath() {
      return path;
    }
  }
}
