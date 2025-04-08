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
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.DataDefinition;
import org.yangcentral.yangkit.model.api.stmt.Input;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.Rpc;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.model.impl.stmt.ContainerImpl;
import org.yangcentral.yangkit.model.impl.stmt.LeafImpl;
import org.yangcentral.yangkit.parser.YangYinParser;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;
import xdev.yang.impl.XmomDbInteraction;
import xdev.yang.impl.YangCapabilityUtils;
import xdev.yang.impl.YangCapabilityUtils.YangDeviceCapability;
import xdev.yang.impl.YangStatementTranslator;
import xdev.yang.impl.YangStatementTranslator.YangStatementTranslation;
import xdev.yang.impl.operation.ListConfiguration.ListLengthConfig;
import xdev.yang.impl.operation.anyxml.AnyXmlSubstatementConfigManager;
import xdev.yang.impl.operation.anyxml.AnyXmlSubstatementFinder;
import xdev.yang.impl.operation.anyxml.AnyXmlSubstatementConfigManager.AnyXmlConfigTag;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.OperationAssignmentTableData;
import xmcp.yang.YangModuleCollection;


public class OperationAssignmentUtils {

  private static Logger _logger = Logger.getLogger(OperationAssignmentUtils.class);
  
  public static List<OperationAssignmentTableData> loadPossibleAssignments(List<Module> modules, String rpcName, String rpcNs,
                                                                         LoadYangAssignmentsData data, Document meta,
                                                                         List<String> supportedFeatures) {
    Rpc rpc = findRpc(modules, rpcName, rpcNs);
    DeviationList deviations = DeviationList.build(modules);
    Input input = rpc.getInput();
    List<ListConfiguration> listConfigs = ListConfiguration.loadListConfigurations(meta);
    AnyXmlSubstatementFinder finder = new AnyXmlSubstatementFinder(modules);
    List<AnyXmlConfigTag> anyXmlConfig = new AnyXmlSubstatementConfigManager().loadAnyXmlConfigs(meta);
    List<YangStatement> elements = traverseYang(data.getTotalYangPath(), data.getTotalNamespaces(), data.getTotalKeywords(), 
                                                input, listConfigs, supportedFeatures, finder, anyXmlConfig);
    List<OperationAssignmentTableData> result = loadAssignments(elements, data, supportedFeatures, deviations);
    return result;
  }


  private static List<YangStatement> traverseYang(String path, String namespaces, String keywords, YangStatement element,
                                                  List<ListConfiguration> listConfigs, List<String> supportedFeatures, AnyXmlSubstatementFinder finder, List<AnyXmlConfigTag> anyXmlConfig) {
    LoadYangAssignmentsDataContent content = new LoadYangAssignmentsDataContent(path, namespaces, keywords);
    String[] parts = content.getParts();
    String[] namespaceParts = content.getNamespaceParts();
    String[] keywordParts = content.getKeywordParts();
    for (int i = 1; i < parts.length; i++) { //ignore initial "/<rpcName>"
      String part = parts[i];
      String namespace = namespaceParts[i];
      String keyword = keywordParts[i];

      if (Constants.TYPE_ANYXML.equals(keyword)) {
        if (i == parts.length - 1) {
          break;
        }
        i++;
        part = parts[i];
        namespace = namespaceParts[i];
        keyword = keywordParts[i];
        element = tarverseYangAnyXml(element, part, namespace, content, i, finder, anyXmlConfig);
        continue;
      }
      
      if (!Constants.TYPE_ANYXML.equals(keyword)) {
        element = traverseYangOneLayer(part, namespace, element, supportedFeatures);
      }
      if (Constants.TYPE_LIST.equals(keyword)) {
        //do not traverse this layer because it is synthetic
        i++;
        continue;
      }
    }

    String keyword = keywordParts[keywordParts.length - 1];
    switch (keyword) {
      case Constants.TYPE_LEAFLIST :
        ListConfiguration leaflistConfig = getListConfig(listConfigs, path, namespaces);
        return getLeafListCandidates(element, leaflistConfig);
      case Constants.TYPE_LIST :
        ListConfiguration listConfig = getListConfig(listConfigs, path, namespaces);
        return getListCandidates(element, listConfig);
      case Constants.TYPE_ANYXML:
        return getAnyXmlCandidates(element, path, namespaces, finder, anyXmlConfig);
      default :
        return getCandidates(element, supportedFeatures);
    }
  }
  
  private static YangStatement tarverseYangAnyXml(YangStatement element, String pathStep, String namespace, LoadYangAssignmentsDataContent content, int index, AnyXmlSubstatementFinder finder, List<AnyXmlConfigTag> anyXmlConfig) {
    String modPath = String.join("/", List.of(content.getParts()).subList(0, index));
    String modNamespaces = String.join(Constants.NS_SEPARATOR, List.of(content.getNamespaceParts()).subList(0, index));
    List<YangStatement> candidates = getAnyXmlCandidates(element, modPath, modNamespaces, finder, anyXmlConfig);
    for(YangStatement candidate : candidates) {
      if (YangStatementTranslation.getLocalName(candidate).equals(pathStep)
          && Objects.equals(YangStatementTranslation.getNamespace(candidate), namespace)){
        return candidate;
      }
    }
    return null;
  }


  private static List<YangStatement> getAnyXmlCandidates(YangStatement element, String path, String namespaces,
                                                         AnyXmlSubstatementFinder finder, List<AnyXmlConfigTag> anyXmlConfig) {
    List<YangStatement> result = new ArrayList<>();
    for (AnyXmlConfigTag tag : anyXmlConfig) {
      if (!tag.getNamespaces().equals(namespaces) || !tag.getYang().equals(path)) {
        continue;
      }
      String name = tag.getTag();
      String namespace = tag.getNamespace();
      YangStatement statement = finder.find(name, namespace);
      if (statement != null) {
        result.add(statement);
      }

    }
    return result;
  }


  private static ListConfiguration getListConfig(List<ListConfiguration> listConfigs, String path, String namespaces) {
    for(ListConfiguration candidate : listConfigs) {
      if(Objects.equals(candidate.getYang(), path) && Objects.equals(candidate.getNamespaces(), namespaces)) {
        return candidate;
      }
    }
    return null;
  }

  private static List<YangStatement> getListCandidates(YangStatement statement, ListConfiguration listConfig) {
    List<YangStatement> result = new ArrayList<YangStatement>();
    if(listConfig == null) {
      return result;
    }

    ListLengthConfig config = listConfig.getConfig();
    for (int i = 0; i < config.getNumberOfCandidateEntries(); i++) {
      ContainerImpl impl = new ContainerImpl(config.createCandidateName(i) + Constants.LIST_INDEX_SEPARATOR + statement.getArgStr());
      impl.setContext(statement.getContext());
      impl.setChildren(statement.getSubElements());
      result.add(impl);
    }

    return result;
  }

  private static List<YangStatement> getLeafListCandidates(YangStatement statement, ListConfiguration listConfig) {
    List<YangStatement> result = new ArrayList<YangStatement>();
    if(listConfig == null) {
      return result;
    }

    ListLengthConfig config = listConfig.getConfig();
    for (int i = 0; i < config.getNumberOfCandidateEntries(); i++) {
      LeafImpl impl = new LeafImpl(config.createCandidateName(i) + Constants.LIST_INDEX_SEPARATOR + statement.getArgStr());
      impl.setContext(statement.getContext());
      result.add(impl);
    }

    return result;
  }


  private static List<YangStatement> getCandidates(YangStatement statement, List<String> supportedFeatures) {
    List<YangElement> candidates = YangStatementTranslation.getSubStatements(statement);
    List<YangStatement> result = new ArrayList<>();
    for (YangElement candidate : candidates) {
      if (isSupportedElement(candidate, supportedFeatures)) {
        result.add((YangStatement) candidate);
      }
    }
    return result;
  }


  private static YangStatement traverseYangOneLayer(String pathStep, String namespace, YangStatement statement,
                                                    List<String> supportedFeatures) {
    List<YangElement> candidates = YangStatementTranslation.getSubStatements(statement);
    for (YangElement candidate : candidates) {
      if (isSupportedElement(candidate, supportedFeatures)) {
        YangStatement node = (YangStatement) candidate;
        if (YangStatementTranslation.getLocalName(node).equals(pathStep)
            && Objects.equals(YangStatementTranslation.getNamespace(node), namespace)) {
          return node;
        }
      }
    }
    throw new RuntimeException("Could not traverse from " + statement.getArgStr() + " to " + pathStep);
  }


  private static List<OperationAssignmentTableData> loadAssignments(List<YangStatement> subElements, LoadYangAssignmentsData data, 
                                                                  List<String> supportedFeatures, DeviationList moduleDeviations) {
    YangSubelementContentHelper helper = new YangSubelementContentHelper();
    DeviationTools deviationTools = new DeviationTools();
    List<OperationAssignmentTableData> result = new ArrayList<>();
    
    for (YangStatement element : subElements) {
      if (isSupportedElement(element, supportedFeatures) && helper.getConfigSubelementValueBoolean(element)) {
        String localName = YangStatementTranslation.getLocalName(element);
        String namespace = YangStatementTranslation.getNamespace(element);
        String keyword = element.getYangKeyword().getLocalName();
        OperationAssignmentTableData.Builder builder = new OperationAssignmentTableData.Builder();
        LoadYangAssignmentsData updatedData = data.clone();
        updatedData.unversionedSetTotalYangPath(updatedData.getTotalYangPath() + "/" + localName);
        updatedData.unversionedSetTotalNamespaces(updatedData.getTotalNamespaces() + Constants.NS_SEPARATOR + namespace);
        updatedData.unversionedSetTotalKeywords(updatedData.getTotalKeywords() + " " + keyword);
        updatedData.unversionedSetWarning(null);
        updatedData.unversionedSetDeviationInfo(null);
        updatedData.unversionedSetSubelementDeviationInfo(null);
        updatedData.unversionedSetIsNotSupportedDeviation(false);
        helper.copyRelevantSubelementValues(element, updatedData);
        deviationTools.handleDeviationsForElement(moduleDeviations, element, data, updatedData);
        if (!updatedData.getIsNotSupportedDeviation()) {
          builder.loadYangAssignmentsData(updatedData);
          builder.yangPath(localName);
          builder.type(getYangType(element));
          result.add(builder.instance());
        }
      }
    }
    return result;
  }


  private static boolean isSupportedElement(YangElement element, List<String> supportedFeatures) {
    Boolean supportedStatement = false;
    for (Entry<Class<?>, YangStatementTranslation> c : YangStatementTranslator.translations.entrySet()) {
      if (c.getKey().isAssignableFrom(element.getClass())) {
        supportedStatement = true;
      }
    }
    return supportedStatement && ifFeatureSupport(element, supportedFeatures);
  }


  private static boolean ifFeatureSupport(YangElement element, List<String> supportedFeatures) {
    if (element instanceof DataDefinition) {
      List<String> ifFeatures = new ArrayList<String>();
      ((DataDefinition) element).getIfFeatures().forEach(e -> {
        ifFeatures.add(e.getArgStr());
      });
      // retainAll returns false when the list does not change, i.e. when all if-features are supported
      return !ifFeatures.retainAll(supportedFeatures);
    }
    return true;
  }


  private static String getYangType(YangElement element) {
    for(Entry<Class<?>, YangStatementTranslation> c : YangStatementTranslator.translations.entrySet()) {
      if(c.getKey().isAssignableFrom(element.getClass())) {
        return c.getValue().getYangStatementName();
      }
    }
    return "Unknown: " + element.getClass().getCanonicalName();
  }

  
  private static Rpc findRpc(List<Module> modules, String rpcName, String rpcNs) {
    for (Module module : modules) {
      Rpc result = module.getRpc(rpcName);
      if (result != null && Objects.equals(YangStatementTranslation.getNamespace(result), rpcNs)) {
        return result;
      }
    }
    throw new RuntimeException("rpc " + rpcName + " in namespace " + rpcNs + " not found.");
  }
  
  public static List<Rpc> findRpcs(List<Module> modules, String rpcName) {
    List<Rpc> result = new ArrayList<>();
    for (Module module : modules) {
      Rpc rpc = module.getRpc(rpcName);
      if (rpc != null) {
        result.add(rpc);
      }
    }
    return result;
  }

  public static List<ModuleGroup> loadModules(String workspaceName) {
    List<ModuleGroup> result = new ArrayList<>();
    XynaFactoryControl xynaFactoryCtrl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
    XmomDbInteraction interaction = new XmomDbInteraction();
    RevisionManagement revMgmt = xynaFactoryCtrl.getRevisionManagement();
    Long revision;
    try {
      revision = revMgmt.getRevision(null, null, workspaceName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    List<XMOMDatabaseSearchResultEntry> xmomDbResult = interaction.searchYangDTs(YangModuleCollection.class.getCanonicalName(), List.of(revision));
    for(XMOMDatabaseSearchResultEntry entry : xmomDbResult) {
      Long entryRevision;
      try {
        entryRevision = revMgmt.getRevision(entry.getRuntimeContext());
        ModuleGroup group = loadModulesFromDt(entry.getFqName(), entryRevision);
        result.add(group);
      } catch (Exception e) {
        _logger.error(e.getMessage(), e);
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return result;
  }

  
  public static ModuleGroup loadModulesFromDt(String fqName, Long entryRevision) throws Exception {
    ModuleGroup result = new ModuleGroup();
    DOM dom = DOM.getOrCreateInstance(fqName, new GenerationBaseCache(), entryRevision);
    dom.parseGeneration(true, false);
    List<String> metaTags = dom.getUnknownMetaTags();
    for(String meta : metaTags) {
      Document xml = XMLUtils.parseString(meta, true);
      if (!xml.getDocumentElement().getNodeName().equals(Constants.TAG_YANG)) {
        continue;
      }
      Node yangTypeNode = xml.getDocumentElement().getAttributes().getNamedItem(Constants.ATT_YANG_TYPE);
      if (yangTypeNode == null || !Constants.VAL_MODULECOLLECTION.equals(yangTypeNode.getNodeValue())) {
        continue;
      }
      List<Element> xmlModules = XMLUtils.getChildElementsByName(xml.getDocumentElement(), "module");
      for(Element module : xmlModules) {
        ModuleParseData parsed = parseModulesFromTag(module);
        result.add(parsed);
      }
    }    
    return result;
  }


  private static ModuleParseData parseModulesFromTag(Element module) throws Exception {
    byte[] decoded = Base64.decode(module.getTextContent());
    java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(decoded);
    YangSchemaContext context = YangYinParser.parse(is, "module.yang", null);
    context.validate();    
    ModuleParseData parsed = new ModuleParseData(decoded, context.getModules());
    return parsed;
  }

  public static Pair<Integer, Document> loadOperationMeta(String fqn, String workspaceName, String yangOperation) {
    try {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(null, null, workspaceName);
      DOM dom = DOM.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      dom.parseGeneration(false, false);
      Operation operation = dom.getOperationByName(yangOperation);
      List<String> unknownMetaTags = operation.getUnknownMetaTags();
      if(unknownMetaTags == null) {
        return null;
      }
      for(int i=0; i< unknownMetaTags.size(); i++) {
        String unknownMetaTag = unknownMetaTags.get(i);
        Document d = XMLUtils.parseString(unknownMetaTag);
        boolean isYang = d.getDocumentElement().getTagName().equals(Constants.TAG_YANG);
        boolean isOperation = Constants.VAL_OPERATION.equals(d.getDocumentElement().getAttribute(Constants.ATT_YANG_TYPE));
        if(isYang && isOperation) {
          return new Pair<Integer, Document>(i, d);
        }
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static Document findYangTypeTag(Operation operation) {
    if (operation.getUnknownMetaTags() == null) {
      return null;
    }
    for (String unknownMetaTag : operation.getUnknownMetaTags()) {
      try {
        Document xml = XMLUtils.parseString(unknownMetaTag, false);
        if (!xml.getDocumentElement().getNodeName().equals(Constants.TAG_YANG)) {
          continue;
        }
        return xml;
      } catch(Exception e) {
        continue;
      }
    }
    return null;
  }


  public static boolean isYangType(Document xml, String expectedYangType) {
    if (xml == null) {
      return false;
    }
    Node yangTypeNode = xml.getDocumentElement().getAttributes().getNamedItem(Constants.ATT_YANG_TYPE);
    return yangTypeNode != null && expectedYangType.equals(yangTypeNode.getNodeValue());
  }

  public static String readRpcName(Document meta) {
    Element rpcElement = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_RPC);
    if(rpcElement == null) {
      return null;
    }
    return rpcElement.getTextContent();
  }

  public static String readRpcNamespace(Document meta) {
    Element rpcElement = XMLUtils.getChildElementByName(meta.getDocumentElement(), Constants.TAG_RPC_NS);
    if(rpcElement == null) {
      return null;
    }
    return rpcElement.getTextContent();
  }


  public static String readDeviceFqn(Document operationMeta) {
    Element deviceFqnEle = XMLUtils.getChildElementByName(operationMeta.getDocumentElement(), Constants.TAG_DEVICE_FQN);
    return deviceFqnEle.getTextContent();
  }

  public static String loadRpcNs(String rpc, String deviceFqn, String workspaceName) {
    List<ModuleGroup> modules = OperationAssignmentUtils.loadModules(workspaceName);
    //filter modules to supported by device
    List<YangDeviceCapability> capabilities = YangCapabilityUtils.loadCapabilities(deviceFqn, workspaceName);
    List<Module> filtered = YangCapabilityUtils.filterModules(modules, capabilities);
    List<Rpc> candidates = OperationAssignmentUtils.findRpcs(filtered, rpc);
    if (candidates.size() != 1) {
      throw new RuntimeException("Could not determine rpc namespace. There are " + candidates.size() + " candidates.");
    }
    return YangStatementTranslation.getNamespace(candidates.get(0));
  }
}
