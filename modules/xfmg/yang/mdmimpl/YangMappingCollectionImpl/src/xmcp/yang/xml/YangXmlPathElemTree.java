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

package xmcp.yang.xml;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class YangXmlPathElemTree {

  private List<TreeElem> _rootList = new ArrayList<>();
  
  
  public YangXmlPathElemTree(YangXmlPathList list) {
    //list.sort();
    for (YangXmlPath path : list.getPathList()) {
      addPath(path);
    }
  }
  
  
  private void addPath(YangXmlPath path) {
    /*
    for (TreeElem elem : _rootList) {
      if (
    }
    */
    
    if (path.getPath().size() == 0) { return; }
    if (_rootList.size() == 0) {
      insertInNewBranch(path, _rootList, 0);
      return;
    }
    YangXmlPathElem current = path.getPath().get(0);
    Optional<TreeElem> matched = TreeElem.getOptionalMatchingListElement(current, _rootList);
    if (matched.isPresent()) {
      insertInExistingBranch(path, matched.get().getChildren(), 1);
      return;
    }
    insertInNewBranch(path, _rootList, 0);
  }
  
  
  private void insertInExistingBranch(YangXmlPath path, List<TreeElem> list, int index) {
    if (index >= path.getPath().size()) { return; }
    YangXmlPathElem current = path.getPath().get(index);
    Optional<TreeElem> matched = TreeElem.getOptionalMatchingListElement(current, list);
    if (matched.isPresent()) {
      insertInExistingBranch(path, matched.get().getChildren(), index + 1);
      return;
    }
    insertInNewBranch(path, list, index);
  }
  
  
  private void insertInNewBranch(YangXmlPath path, List<TreeElem> listIn, int index) {
    /*
    if (index >= path.getPath().size()) { return; }
    YangXmlPathElem current = path.getPath().get(index);
    TreeElem next = new TreeElem(current);
    list.add(next);
    insertInNewBranch(path, next.getChildren(), index + 1);
    */
    List<TreeElem> list = listIn;
    for (int i = index; i < path.getPath().size(); i++) {
      YangXmlPathElem current = path.getPath().get(i);
      TreeElem next = new TreeElem(current);
      list.add(next);
      Collections.sort(list);
      list = next.getChildren();
    }
  }
  
  /*
  private void insertInNewBranch(YangXmlPath path, TreeElem parent, int index) {
    insertInNewBranch(path, parent.getChildren(), index);
  }
  */
  
  public String toXml() {
    if (_rootList.size() < 1) { return ""; }
    /*
    XmlBuilder xml = new XmlBuilder();
    
    return xml.toString();
    */
    Document doc = buildDocument();
    Element root = null;
    if (_rootList.size() == 1) {
      root = _rootList.get(0).toW3cElement(doc);
    } else {
      root = doc.createElement(Constants.DEFAULT_ROOT_TAG_NAME);
      for (TreeElem elem : _rootList) {
        Element child = elem.toW3cElement(doc);
        root.appendChild(child);
      }
    }
    doc.appendChild(root);
    return getDocumentString(doc);
  }
  
  
  private Document buildDocument() {
    Document doc = null;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.newDocument();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    //Element docRoot = adaptElementAndChildren(_root, doc);
    //doc.appendChild(docRoot);
    return doc;
  }
  
  
  private String getDocumentString(Document document) {
    boolean withPI = false;
    StringWriter sw = new StringWriter();
    try {
      Source source = new DOMSource(document);
      Result result = new StreamResult(sw);
      TransformerFactory factory = TransformerFactory.newInstance();
      try {
        factory.setAttribute("indent-number", 2);
      } catch (IllegalArgumentException f) {
        // do nothing
      }
      Transformer xformer = factory.newTransformer();
      xformer.setOutputProperty(OutputKeys.METHOD, "xml");
      xformer.setOutputProperty(OutputKeys.INDENT, "yes");
      xformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
      xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, withPI ? "no" : "yes");
      xformer.transform(source, result);
    } catch (TransformerException f) {
      throw new RuntimeException(f);
    }
    return sw.toString();
  }

}
