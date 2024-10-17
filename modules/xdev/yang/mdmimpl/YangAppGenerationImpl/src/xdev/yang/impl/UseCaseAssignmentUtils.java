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
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.Container;
import org.yangcentral.yangkit.model.api.stmt.Grouping;
import org.yangcentral.yangkit.model.api.stmt.Input;
import org.yangcentral.yangkit.model.api.stmt.Leaf;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.Rpc;
import org.yangcentral.yangkit.model.api.stmt.SchemaNode;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.parser.YangYinParser;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.UseCaseAssignementTableData;
import xmcp.yang.YangModuleCollection;

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
  
  public static List<UseCaseAssignementTableData> loadPossibleAssignments(List<Module> modules, String rpcName, LoadYangAssignmentsData data) {
    Rpc rpc = findRpc(modules, rpcName);
    Input input = rpc.getInput();
    List<YangStatement> elements = traverseYang(modules, data.getTotalYangPath(), input);
    return loadAssignments(elements, data);
  }

  private static List<YangStatement> traverseYang(List<Module> modules, String path, YangStatement element) {
    String[] parts = path.split("\\/");
    for(int i=1; i<parts.length; i++) { //ignore initial "/root"
      String part = parts[i];
      element = traverseYangOneLayer(modules, part, element);
    }
    return getCandidates(element);
  }
  

  private static List<YangStatement> getCandidates(YangStatement statement) {
    List<YangElement> candidates = statement.getSubElements();
    List<YangStatement> result = new ArrayList<>();
    for (YangElement candidate : candidates) {
      if (isSupportedElement(candidate)) {
        result.add((YangStatement) candidate);
      }
    }
    return result;
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
    
    throw new RuntimeException("Could not traverse from " + statement.getArgStr() + " to " + pathStep);
  }


  private static List<UseCaseAssignementTableData> loadAssignments(List<YangStatement> subElements, LoadYangAssignmentsData data) {
    List<UseCaseAssignementTableData> result = new ArrayList<>();
    for (YangStatement element : subElements) {
      if (isSupportedElement(element)) {
        String localName = ((SchemaNode) element).getIdentifier().getLocalName();
        UseCaseAssignementTableData.Builder builder = new UseCaseAssignementTableData.Builder();
        LoadYangAssignmentsData updatedData = data.clone();
        updatedData.unversionedSetTotalYangPath(updatedData.getTotalYangPath() + "/" + localName);
        builder.loadYangAssignmentsData(updatedData);
        builder.yangPath(localName);
        builder.type(getYangType(element));
        result.add(builder.instance());
      }
    }
    return result;
  }
  

  private static boolean isSupportedElement(YangElement element) {
    for (Class<?> c : supportedYangStatementsForAssignments) {
      if (c.isAssignableFrom(element.getClass())) {
        return true;
      }
    }
    return false;
  }

  private static String getYangType(YangElement element) {
    for(Entry<Class<?>, String> c : yangStatementIdentifiers.entrySet()) {
      if(c.getKey().isAssignableFrom(element.getClass())) {
        return c.getValue();
      }
    }
    return "Unknown: " + element.getClass().getCanonicalName();
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
    XynaFactoryControl xynaFactoryCtrl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
    XmomDbInteraction interaction = new XmomDbInteraction();
    RevisionManagement revMgmt = xynaFactoryCtrl.getRevisionManagement();
    RuntimeContextDependencyManagement rtcDepMgmt = xynaFactoryCtrl.getRuntimeContextDependencyManagement();
    Long revision;
    try {
      revision = revMgmt.getRevision(null, null, workspaceName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    Set<Long> revisions = new HashSet<Long>();
    revisions.add(revision);
    rtcDepMgmt.getDependenciesRecursivly(revision, revisions);
    List<Long> revisionsList = new ArrayList<>(revisions);
    List<XMOMDatabaseSearchResultEntry> xmomDbResult = interaction.searchYangDTs(YangModuleCollection.class.getCanonicalName(), revisionsList);
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
      for(Element module : modules) {
        addModulesFromTag(module, result);
      }
    }
    return result;
  }


  private static void addModulesFromTag(Element module, List<Module> modules) throws Exception {
    java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(Base64.decode(module.getTextContent()));
    YangSchemaContext context = YangYinParser.parse(is, "module.yang", null);
    context.validate();
    modules.addAll(context.getModules());
  }
}
