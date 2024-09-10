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
package xdev.yang.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.Container;
import org.yangcentral.yangkit.model.api.stmt.Grouping;
import org.yangcentral.yangkit.model.api.stmt.Input;
import org.yangcentral.yangkit.model.api.stmt.Leaf;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.Rpc;
import org.yangcentral.yangkit.model.api.stmt.SchemaNode;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.parser.YinParser;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xmcp.yang.UseCaseAssignementTableData;
import xmcp.yang.YangUsecaseImplementation;

public class UseCaseAssignmentUtils {

  private static final Set<Class<?>> supportedYangStatementsForAssignments = setupSupportedAssignmentClasses();
  private static final Map<Class<?>, String> yangStatementIdentifiers = setupStatementIdentifiers();

  private static Set<Class<?>> setupSupportedAssignmentClasses() {
    Set<Class<?>> result = new HashSet<Class<?>>();
    result.add(Container.class);
    result.add(Leaf.class);
    result.add(Grouping.class);
    return result;
  }
  
  private static Map<Class<?>, String> setupStatementIdentifiers() {
    Map<Class<?>, String> result = new HashMap<Class<?>, String>();
    result.put(Container.class, Constants.TYPE_CONTAINER);
    result.put(Leaf.class, Constants.TYPE_LEAF);
    result.put(Grouping.class, Constants.TYPE_GROUPING);
    return result;
  }
  
  public static List<UseCaseAssignementTableData> loadPossibleAssignments(List<Module> modules, String path, String rpcName) {
    Rpc rpc = findRpc(modules, rpcName);
    Input input = rpc.getInput();
    List<YangElement> elements = traverseYang(modules, path, input);
    return loadAssignments(elements, path);
  }

  private static List<YangElement> traverseYang(List<Module> modules, String path, YangStatement element) {
    String[] parts = path.split("\\/");
    for(int i=0; i<parts.length; i++) {
      String part = parts[i];
      element = traverseYangOneLayer(modules, part, element);
    }
    return element.getSubElements();
  }
  
  private static YangStatement traverseYangOneLayer(List<Module> modules, String pathStep, YangStatement statement) {
    List<YangElement> candidates = statement.getSubElements();
    for(YangElement candidate : candidates) {
      if(isSupportedElement(candidate)) {
        if(((SchemaNode)candidate).getIdentifier().getLocalName().equals(pathStep)) {
          return (YangStatement) candidate;
        }
      }
    }
    
    throw new RuntimeException("Could not traverse from " + statement.getElementPosition().toString() + " to " + pathStep);
  }

  private static List<UseCaseAssignementTableData> loadAssignments(List<YangElement> subElements, String totalPath) {
    List<UseCaseAssignementTableData> result = new ArrayList<>();
    for(YangElement element : subElements) {
      if(isSupportedElement(element)) {
        UseCaseAssignementTableData.Builder builder = new UseCaseAssignementTableData.Builder();
        builder.totalYangPath(totalPath);
        builder.yangPath(((SchemaNode)element).getIdentifier().getLocalName());
        builder.type(yangStatementIdentifiers.get(element.getClass()));
        result.add(builder.instance());
      }
    }
    return result;
  }
  
  private static boolean isSupportedElement(YangElement element) {
    return supportedYangStatementsForAssignments.contains(element.getClass());
  }
  

  private static Rpc findRpc(List<Module> modules, String rpcName) {
    for(Module module : modules) {
      Rpc result = module.getRpc(rpcName);
      if(result != null) {
        return result;
      }
    }
    throw new RuntimeException("rpc " + rpcName + " not found.");
  }

  public static List<Module> loadModules(String workspaceName) {
    List<Module> result = new ArrayList<>();
    XmomDbInteraction interaction = new XmomDbInteraction();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision;
    try {
      revision = revMgmt.getRevision(null, null, workspaceName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    List<XMOMDatabaseSearchResultEntry> xmomDbResult = interaction.searchYangDTs(YangUsecaseImplementation.class.getCanonicalName(), List.of(revision));
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
      Document xml = XMLUtils.parseString(meta, false);
      if (!xml.getDocumentElement().getNodeName().equals(Constants.TAG_YANG)) {
        continue;
      }
      Node yangTypeNode = xml.getDocumentElement().getAttributes().getNamedItem(Constants.ATT_YANG_TYPE);
      if (yangTypeNode == null || !Constants.VAL_MODULECOLLECTION.equals(yangTypeNode.getNodeValue())) {
        continue;
      }
      List<Element> modules = XMLUtils.getChildElementsByName((Element)yangTypeNode, "module");
      for(Element module : modules) {
        addModulesFromTag(module, result);
      }
    }
    return result;
  }
  
  private static void addModulesFromTag(Element module, List<Module> modules) throws Exception {
    YinParser parser = new YinParser("tmp");
    org.dom4j.Document document = convertMetaTagToDocument(module);
    List<YangElement> elements = parser.parse(document);
    for(YangElement element : elements) {
      if(element instanceof Module) {
        modules.add((Module)element);
      }
    }
  }
  
  private static org.dom4j.Document convertMetaTagToDocument(Element node) {
    Document document = node.getOwnerDocument();
    DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
    LSSerializer serializer = domImplLS.createLSSerializer();
    String str = serializer.writeToString(node);
    org.dom4j.Document result;
    try {
      result = DocumentHelper.parseText(str);
      return result;
    } catch (DocumentException e) {
      throw new RuntimeException(e);
    }
  }
}
