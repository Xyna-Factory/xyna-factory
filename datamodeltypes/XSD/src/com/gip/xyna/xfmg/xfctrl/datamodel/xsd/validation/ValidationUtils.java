/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Graph;
import com.gip.xyna.utils.collections.Graph.Node;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class ValidationUtils {

  
  public static void validate(String xml, Map<String, String> fileNameContentMap) throws Exception {

    Document document = XMLUtils.parseString(xml, true);

    Map<String, byte[]> xsdStorage = prepareXsdStorage(fileNameContentMap);
    XSDStorageResolver schemaResourceResolver = new XSDStorageResolver(xsdStorage);
    
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(schemaResourceResolver);
    Schema schema = factory.newSchema(prepareSources(xsdStorage));
    
    Validator validator = schema.newValidator();
    ErrorHandlerImpl err = prepareErrorHandler();
    validator.setErrorHandler(err);
    validator.validate(new DOMSource(document));    
    if (err.accepted()) {
      return;
    }
    err.nextRun();
      
    CentralFactoryLogging.getLogger(ValidationUtils.class).debug(err.toString());
    err.throwMostSevereError();
  }
  
  
  private static ErrorHandlerImpl prepareErrorHandler() {
    return new ErrorHandlerImpl();
  }

  
  private static Source[] prepareSources(Map<String, byte[]> xsdStorage) {
    Map<String, Graph.Node<XsdWithDependencies>> nodes = new HashMap<>();
    
    for (Entry<String, byte[]> entry : xsdStorage.entrySet()) {
      // is key in graph data
      Graph.Node<XsdWithDependencies> node = getOrCreateNode(entry.getKey(), entry.getValue(), nodes);
      // parse xsd
      Collection<String> dependencies = node.getContent().getImportsAndIncludes();
      // add all imports and includes as dependencies
      for (String dependency : dependencies) {
        Graph.Node<XsdWithDependencies> depNode = getOrCreateNode(dependency, null, nodes);;
        node.addDependency(depNode);
      }
    }
    
    // starting from roots, create ordered List
    List<Graph.Node<XsdWithDependencies>> orderedNodes = orderNodes(nodes.values());
    return (Source[]) orderedNodes.stream().map(n -> new StreamSource(new ByteArrayInputStream(n.getContent().getXSD()))).toArray(i -> new Source[i]);
  }
  
  
  private static Graph.Node<XsdWithDependencies> getOrCreateNode(String potentialPathname, byte[] content, Map<String, Graph.Node<XsdWithDependencies>> nodeCache) {
    String filename = pathToFilename(potentialPathname);
    Graph.Node<XsdWithDependencies> node = nodeCache.get(filename);
    if (node == null) {
      XsdWithDependencies xsd = content == null ? new XsdWithDependencies(filename) : new XsdWithDependencies(filename, content);
      node = new Graph.Node<XsdWithDependencies>(xsd);
      nodeCache.put(filename, node);
    } else if (content != null){
      node.getContent().setContent(content);
    }
    return node;
  }
  
  
  private static List<Node<XsdWithDependencies>> orderNodes(Collection<Node<XsdWithDependencies>> toBeOrdered) {
    List<Graph.Node<XsdWithDependencies>> orderedNodes = new ArrayList<>();
    Graph<XsdWithDependencies> graph = new Graph<>(toBeOrdered);
    // add roots
    orderedNodes.addAll(graph.getRoots());
    Collection<Node<XsdWithDependencies>> restToBeOrdered = new ArrayList<>(toBeOrdered);
    // remove roots from rest
    restToBeOrdered.removeAll(orderedNodes);
    if (restToBeOrdered.size() > 0) {
      // order the rest
      List<Graph.Node<XsdWithDependencies>> rest = orderNodes(recreate(restToBeOrdered));
      outer: for (Node<XsdWithDependencies> node : rest) {
        for (Node<XsdWithDependencies> orderedNode : orderedNodes) {
          if (node.getContent().getId().equals(orderedNode.getContent().getId())) {
            // don't add if already contained
            continue outer;
          }
        }
        orderedNodes.add(node);        
      }
    }
    return orderedNodes; 
  }



  private static Collection<Node<XsdWithDependencies>> recreate(Collection<Node<XsdWithDependencies>> nodesWithParents) {
    Map<String, Graph.Node<XsdWithDependencies>> recreation = new HashMap<>();
    for (Node<XsdWithDependencies> nodeWithParents : nodesWithParents) {
      Graph.Node<XsdWithDependencies> node = getOrCreateNode(nodeWithParents.getContent().getId(), nodeWithParents.getContent().content, recreation);
      Collection<String> dependencies = node.getContent().getImportsAndIncludes();
      for (String dependency : dependencies) {
        Graph.Node<XsdWithDependencies> depNode = getOrCreateNode(dependency, null, recreation);
        node.addDependency(depNode);
      }
    }
    return recreation.values();
  }



  public static String pathToFilename(String path) {
    return path.substring(path.lastIndexOf(Constants.FILE_SEPARATOR) + 1);
  }


  private static Map<String, byte[]> prepareXsdStorage(Map<String, String> fileNameContentMap) throws IOException {
    Map<String, byte[]> xsdStorage = new HashMap<String, byte[]>();
    for (String fileName : fileNameContentMap.keySet()) {
      xsdStorage.put(fileName, Base64.decode(fileNameContentMap.get(fileName)));
    }
    return xsdStorage;
  }
  
  
}
