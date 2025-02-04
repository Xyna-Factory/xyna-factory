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
import java.util.Arrays;
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
import org.yangcentral.yangkit.model.api.stmt.Deviate;
import org.yangcentral.yangkit.model.api.stmt.DeviateType;
import org.yangcentral.yangkit.model.api.stmt.Deviation;
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
import xdev.yang.impl.usecase.ListConfiguration.ListLengthConfig;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.UseCaseAssignmentTableData;
import xmcp.yang.YangModuleCollection;



public class UseCaseAssignmentUtils {
  
  public static class DeviationList {
    private List<Deviation> deviations = new ArrayList<>();
    public DeviationList(List<Deviation> deviations) {
      if (deviations != null) {
        this.deviations.addAll(deviations);
      }
    }
    public List<Deviation> getList() {
      return deviations;
    }
    public void replace(List<Deviation> deviations) {
      this.deviations.clear();
      this.deviations.addAll(deviations);
    }
  }
  
  public static class RpcAndDeviations {
    private Rpc rpc;
    private DeviationList deviationList;
    public RpcAndDeviations(Rpc rpc, List<Deviation> deviations) {
      this.rpc = rpc;
      deviationList = new DeviationList(deviations);      
    }
    public Rpc getRpc() {
      return rpc;
    }
    public DeviationList getDeviationList() {
      return deviationList;
    }
  }

  private static Logger _logger = Logger.getLogger(UseCaseAssignmentUtils.class);
  

  public static List<UseCaseAssignmentTableData> loadPossibleAssignments(List<Module> modules, String rpcName, String rpcNs,
                                                                         LoadYangAssignmentsData data, Document meta,
                                                                         List<String> supportedFeatures) {
    RpcAndDeviations rpc = findRpc(modules, rpcName, rpcNs);
    Input input = rpc.getRpc().getInput();
    List<ListConfiguration> listConfigs = ListConfiguration.loadListConfigurations(meta);
    List<YangStatement> elements =
        traverseYang(data.getTotalYangPath(), data.getTotalNamespaces(), data.getTotalKeywords(), input, listConfigs, 
                     supportedFeatures, rpc.getDeviationList());
    List<UseCaseAssignmentTableData> result = loadAssignments(elements, data, supportedFeatures, rpc.getDeviationList());
    return result;
  }

  
  private static boolean identifiersAreEqual(String id1, String id2) {
    return removeOptionalPrefix(id1).equals(removeOptionalPrefix(id2));
  }
  
  private static String removeOptionalPrefix(String id) {
    if (id == null) { return ""; }
    if (!id.contains(":")) { return id; }
    return id.substring(id.indexOf(":") + 1);
  }

  private static void filterDeviations(DeviationList deviations, String[] partsIn, String[] namespacePartsIn,
                                       String[] keywordParts) {
    String[] parts = partsIn;
    String[] namespaceParts = namespacePartsIn;
    int indexOfUses = -1;
    for (int i = 0; i < keywordParts.length; i++) {
      if (Constants.TYPE_USES.equals(keywordParts[i])) {
        indexOfUses = i;
      }
    }
    if (indexOfUses + 1 >= parts.length) { return; }
    if (indexOfUses > 0) {
      parts = Arrays.copyOfRange(parts, indexOfUses, parts.length);
      namespaceParts = Arrays.copyOfRange(namespaceParts, indexOfUses, namespaceParts.length);
    }
    List<Deviation> filtered = new ArrayList<>();
    for (Deviation dev : deviations.getList()) {
      if (keepDeviation(dev, parts, namespaceParts)) {
        filtered.add(dev);
      }
    }
    deviations.replace(filtered);
  }
  
  
  private static boolean hasDeviationTypeNotSupported(Deviation dev) {
    if (dev.getDeviates() == null) { return false; }
    for (Deviate deviate : dev.getDeviates()) { 
      if (deviate.getDeviateType() == DeviateType.NOT_SUPPORTED) {
        return true;
      }
    }
    return false;
  }
  
  
  private static boolean keepDeviation(Deviation dev, String[] parts, String[] namespaceParts) {
    if (dev.getDeviates() == null) { return false; }
    if (dev.getTargetPath() == null) { return false; }
    if (dev.getTargetPath().getPath() == null) { return false; }
    if (dev.getTargetPath().getPath().size() != parts.length + 1) { return false; }
    if (dev.getTargetPath().getPath().size() != namespaceParts.length + 1) { return false; }
    
    for (int i = 0; i < parts.length; i++) {
      _logger.warn("### Checking deviation path elem local name [ " + i + " ]: " + 
                   dev.getTargetPath().getPath().get(i).getLocalName() + " <-> " + parts[i]);
      _logger.warn("### Checking deviation path elem namespace [ " + i + " ]: " + 
                   dev.getTargetPath().getPath().get(i).getNamespace().toString() + " <-> " + namespaceParts[i]);
      if (!namespaceParts[i].equals(dev.getTargetPath().getPath().get(i).getNamespace().toString())) { return false; }
      if (!identifiersAreEqual(parts[i], dev.getTargetPath().getPath().get(i).getLocalName())) { return false; }
    }
    return true;
  }
  
  
  private static List<YangStatement> traverseYang(String path, String namespaces, String keywords, YangStatement element,
                                                  List<ListConfiguration> listConfigs, List<String> supportedFeatures,
                                                  DeviationList deviations) {
    String[] parts = path.split("\\/");
    String[] namespaceParts = namespaces.split(Constants.NS_SEPARATOR);
    String[] keywordParts = keywords.split(" ");
    filterDeviations(deviations, parts, namespaceParts, keywordParts);
    
    for (int i = 1; i < parts.length; i++) { //ignore initial "/<rpcName>"
      String part = parts[i];
      String namespace = namespaceParts[i];
      String keyword = keywordParts[i];
      element = traverseYangOneLayer(part, namespace, element, supportedFeatures);
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
      default :
        return getCandidates(element, supportedFeatures);
    }
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
  
  
  private static void handleDeviationsForElement(YangStatement node, DeviationList deviations,
                                                 LoadYangAssignmentsData data) {
    for (Deviation dev : deviations.getList()) {
      String devLocalname = dev.getTargetPath().getLast().getLocalName();
      String devNamespace = dev.getTargetPath().getLast().getNamespace().toString();
      String localname = YangStatementTranslation.getLocalName(node);
      String namespace = YangStatementTranslation.getNamespace(node);
      _logger.warn("### Checking path end / local name: " + localname + " <-> " + devLocalname);
      _logger.warn("### Checking path end / namespace: " + namespace + " <-> " + devNamespace);
      if (identifiersAreEqual(localname, devLocalname) && namespace.equals(devNamespace)) {
        if (hasDeviationTypeNotSupported(dev)) {
          data.unversionedSetIsNotSupportedDeviation(true);
        }
        //else 
        if ((dev.getDeviates() != null) && (dev.getDeviates().size() > 0)) {
          data.unversionedSetDeviationInfo(dev.getDeviates().get(0).getArgStr());  
        }
        return;
      }
    }
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


  private static List<UseCaseAssignmentTableData> loadAssignments(List<YangStatement> subElements, LoadYangAssignmentsData data, 
                                                                  List<String> supportedFeatures, DeviationList deviations) {
    YangSubelementContentHelper helper = new YangSubelementContentHelper();
    List<UseCaseAssignmentTableData> result = new ArrayList<>();
    for (YangStatement element : subElements) {
      if (isSupportedElement(element, supportedFeatures) && helper.getConfigSubelementValueBoolean(element)) {
        String localName = YangStatementTranslation.getLocalName(element);
        String namespace = YangStatementTranslation.getNamespace(element);
        String keyword = element.getYangKeyword().getLocalName();
        UseCaseAssignmentTableData.Builder builder = new UseCaseAssignmentTableData.Builder();
        LoadYangAssignmentsData updatedData = data.clone();
        updatedData.unversionedSetTotalYangPath(updatedData.getTotalYangPath() + "/" + localName);
        updatedData.unversionedSetTotalNamespaces(updatedData.getTotalNamespaces() + Constants.NS_SEPARATOR + namespace);
        updatedData.unversionedSetTotalKeywords(updatedData.getTotalKeywords() + " " + keyword);
        updatedData.unversionedSetWarning(null);
        helper.copyRelevantSubelementValues(element, updatedData);
        handleDeviationsForElement(element, deviations, updatedData);
        builder.loadYangAssignmentsData(updatedData);
        builder.yangPath(localName);
        builder.type(getYangType(element));
        result.add(builder.instance());
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

  private static RpcAndDeviations findRpc(List<Module> modules, String rpcName, String rpcNs) {
    for (Module module : modules) {
      Rpc result = module.getRpc(rpcName);
      if (result != null && Objects.equals(YangStatementTranslation.getNamespace(result), rpcNs)) {
        return new RpcAndDeviations(result, module.getDeviations());
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

  public static List<Module> loadModules(String workspaceName) {
    List<Module> result = new ArrayList<>();
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
        result.addAll(loadModulesFromDt(entry.getFqName(), entryRevision));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return result;
  }

  private static List<Module> loadModulesFromDt(String fqName, Long entryRevision) throws Exception {
    DOM dom = DOM.getOrCreateInstance(fqName, new GenerationBaseCache(), entryRevision);
    dom.parseGeneration(true, false);
    List<String> metaTags = dom.getUnknownMetaTags();
    List<Module> result = new ArrayList<Module>();
    for(String meta : metaTags) {
      Document xml = XMLUtils.parseString(meta, true);
      if (!xml.getDocumentElement().getNodeName().equals(Constants.TAG_YANG)) {
        continue;
      }
      Node yangTypeNode = xml.getDocumentElement().getAttributes().getNamedItem(Constants.ATT_YANG_TYPE);
      if (yangTypeNode == null || !Constants.VAL_MODULECOLLECTION.equals(yangTypeNode.getNodeValue())) {
        continue;
      }
      List<Element> modules = XMLUtils.getChildElementsByName(xml.getDocumentElement(), "module");
      YangSchemaContext context = null;
      for(Element module : modules) {
        context = addModulesFromTag(module, result, context);
      }
    }
    return result;
  }


  private static YangSchemaContext addModulesFromTag(Element module, List<Module> modules, YangSchemaContext context) throws Exception {
    java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(Base64.decode(module.getTextContent()));
    context = YangYinParser.parse(is, "module.yang", context);
    context.validate();
    modules.addAll(context.getModules());
    return context;
  }

  public static Pair<Integer, Document> loadOperationMeta(String fqn, String workspaceName, String usecase) {
    try {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(null, null, workspaceName);
      DOM dom = DOM.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      dom.parseGeneration(false, false);
      Operation operation = dom.getOperationByName(usecase);
      List<String> unknownMetaTags = operation.getUnknownMetaTags();
      if(unknownMetaTags == null) {
        return null;
      }
      for(int i=0; i< unknownMetaTags.size(); i++) {
        String unknownMetaTag = unknownMetaTags.get(i);
        Document d = XMLUtils.parseString(unknownMetaTag);
        boolean isYang = d.getDocumentElement().getTagName().equals(Constants.TAG_YANG);
        boolean isUseCase = Constants.VAL_USECASE.equals(d.getDocumentElement().getAttribute(Constants.ATT_YANG_TYPE));
        if(isYang && isUseCase) {
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


  public static String readDeviceFqn(Document usecaseMeta) {
    Element deviceFqnEle = XMLUtils.getChildElementByName(usecaseMeta.getDocumentElement(), Constants.TAG_DEVICE_FQN);
    return deviceFqnEle.getTextContent();
  }

  public static String loadRpcNs(String rpc, String deviceFqn, String workspaceName) {
    List<Module> modules = UseCaseAssignmentUtils.loadModules(workspaceName);
    //filter modules to supported by device
    List<YangDeviceCapability> capabilities = YangCapabilityUtils.loadCapabilities(deviceFqn, workspaceName);
    modules = YangCapabilityUtils.filterModules(modules, capabilities);
    List<Rpc> candidates = UseCaseAssignmentUtils.findRpcs(modules, rpc);
    if (candidates.size() != 1) {
      throw new RuntimeException("Could not determine rpc namespace. There are " + candidates.size() + " candidates.");
    }
    return YangStatementTranslation.getNamespace(candidates.get(0));
  }
}
