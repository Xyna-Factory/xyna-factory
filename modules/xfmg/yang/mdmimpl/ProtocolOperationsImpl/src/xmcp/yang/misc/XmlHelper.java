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


package xmcp.yang.misc;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class XmlHelper {

  public XmlElemBuilder createElem(Document doc) {
    return new XmlElemBuilder(doc);
  }
  
  
  public Document buildDocument() {
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
    return doc;
  }
  
  
  public String getDocumentString(Document document) {
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
      xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      xformer.transform(source, result);
    } catch (TransformerException f) {
      throw new RuntimeException(f);
    }
    return sw.toString();
  }
  
  
  public void appendXmlSubtree(Document doc, Element elem, String subtree) {
    if (elem == null) { return; }
    try {
      Document doc2 = XMLUtils.parseString(subtree);
      if (doc2.getDocumentElement() == null) { return; }
      // root node "<root>" itself must be ignored
      NodeList list = doc2.getDocumentElement().getChildNodes();
      if (list == null) { return; }
      for (int i = 0; i < list.getLength(); i++) {
        Node node = list.item(i);
        if (!(node instanceof Element)) { continue; }
        elem.appendChild(doc.importNode(node, true));
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
}
