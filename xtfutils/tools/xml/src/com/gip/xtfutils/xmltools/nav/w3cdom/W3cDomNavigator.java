/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xtfutils.xmltools.nav.w3cdom;

import java.io.*;
//import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.gip.xtfutils.xmltools.XmlBuildException;
import com.gip.xtfutils.xmltools.XmlParseException;
import com.gip.xtfutils.xmltools.build.EncodingName;
import com.gip.xtfutils.xmltools.nav.XmlNavigator;


public class W3cDomNavigator extends XmlNavigator {

  protected static Logger _logger = Logger.getLogger(W3cDomNavigator.class);

  protected Document _doc = null;
  protected Element _current = null;


  public W3cDomNavigator() {}

  public W3cDomNavigator(Document doc) {
    _doc = doc;
    _current = _doc.getDocumentElement();
  }

  public W3cDomNavigator(Element elem) {
    _current = elem;
    if (elem != null) { _doc = _current.getOwnerDocument(); }
  }

  public W3cDomNavigator(String xml) throws SAXException, IOException, ParserConfigurationException {
    InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    _doc = builder.parse(stream);
    _current = _doc.getDocumentElement();
  }

  @Override
  public boolean isEmpty() {
    return (_current == null);
  }


  @Override
  public void setToEmpty() {
    _current = null;
  }


  @Override
  public XmlNavigator buildEmpty() {
    return new W3cDomNavigator();
  }

  @Override
  public XmlNavigator clone() {
    return new W3cDomNavigator(_current);
  }


  @Override
  public XmlNavigator gotoRoot() {
    if (_doc == null) { return this; }
    _current = _doc.getDocumentElement();
    return this;
  }


  @Override
  public XmlNavigator descendToFirstChild() {
    if (isEmpty()) { return this; }
    if (!_current.hasChildNodes()) {
      setToEmpty();
      return this;
    }
    NodeList kids = _current.getChildNodes();
    //int count = 0;
    for (int i = 0; i < kids.getLength(); i++) {
      Node subnode = kids.item(i);
      if (subnode.getNodeType() == Node.ELEMENT_NODE) {
        _current = (Element) subnode;
        return this;
      }
    }
    setToEmpty();
    return this;
  }


  @Override
  public XmlNavigator descend(String childNameOrig, int index) {
    if (childNameOrig == null) {
      throw new IllegalArgumentException("Child node name is null.");
    }
    if (isEmpty()) { return this; }
    if (!_current.hasChildNodes()) {
      setToEmpty();
      return this;
    }
    NodeList kids = _current.getChildNodes();
    int count = 0;
    for (int i = 0; i < kids.getLength(); i++) {
      Node subnode = kids.item(i);
      if (subnode.getNodeType() != Node.ELEMENT_NODE) { continue; }
      if (subnode.getNodeName() == null) { continue; }
      if (!childNameOrig.equals(adjustNodeName(subnode.getNodeName()))) { continue; }
      if (count == index) {
        _current = (Element) subnode;
        return this;
      }
      count++;
    }
    setToEmpty();
    return this;
  }


  @Override
  public List<XmlNavigator> getChildrenByName(String childNameOrig) {
    if (childNameOrig == null) {
      throw new IllegalArgumentException("Child node name is null.");
    }
    List<XmlNavigator> ret = new ArrayList<XmlNavigator>();
    if (isEmpty()) { return ret; }
    if (!_current.hasChildNodes()) { return ret; }
    NodeList kids = _current.getChildNodes();
    for (int i = 0; i < kids.getLength(); i++) {
      Node subnode = kids.item(i);
      if (subnode.getNodeType() != Node.ELEMENT_NODE) { continue; }
      if (subnode.getNodeName() == null) { continue; }
      if (!childNameOrig.equals(adjustNodeName(subnode.getNodeName()))) { continue; }
      XmlNavigator nav = new  W3cDomNavigator((Element) subnode);
      ret.add(nav);
    }
    return ret;
  }


  @Override
  public List<XmlNavigator> getAllChildren() {
    List<XmlNavigator> ret = new ArrayList<XmlNavigator>();
    if (isEmpty()) { return ret; }
    if (!_current.hasChildNodes()) { return ret; }
    NodeList kids = _current.getChildNodes();
    for (int i = 0; i < kids.getLength(); i++) {
      Node subnode = kids.item(i);
      if (subnode.getNodeType() != Node.ELEMENT_NODE) { continue; }
      XmlNavigator nav = new  W3cDomNavigator((Element) subnode);
      ret.add(nav);
    }
    return ret;
  }


  @Override
  public XmlNavigator ascend() {
    if (isEmpty()) { return this; }
    Node parent = _current.getParentNode();
    if ((parent == null) || !(parent instanceof Element)) {
      setToEmpty();
      return this;
    }
    _current = (Element) parent;
    return this;
  }


  @Override
  public boolean isRoot() {
    if (isEmpty()) { return false; }
    return (_current == _current.getOwnerDocument().getDocumentElement());
  }


  @Override
  public String getTagName() {
    if (isEmpty()) { return null; }
    //return _current.getLocalName();
    return adjustNodeName(_current.getNodeName());
  }


  private String adjustNodeName(String name) {
    if (name == null) { return null; }
    int pos = name.indexOf(":");
    if (pos >= 0) {
      return name.substring(pos + 1);
    }
    return name;
  }

  @Override
  public String getText() {
    if (isEmpty()) { return null; }
    if (!_current.hasChildNodes()) { return null; }
    NodeList kids = _current.getChildNodes();
    for (int i = 0; i < kids.getLength(); i++) {
      Node subnode = kids.item(i);
      if (subnode.getNodeType() == Node.TEXT_NODE) {
        return subnode.getNodeValue();
      }
      if (subnode.getNodeType() == Node.CDATA_SECTION_NODE) {
        return subnode.getNodeValue();
      }
    }
    return null;
  }


  @Override
  public String getAttributeValue(String attributeName) {
    if (attributeName == null) { return null; }
    if (isEmpty()) { return null; }
    /*
    Attr attr = _current.getAttributeNode(attributeName);
    if (attr == null) { return null; }
    return attr.getValue();
    */
    NamedNodeMap map = _current.getAttributes();
    for (int i = 0; i < map.getLength(); i++) {
      Node attr = map.item(i);
      String name = adjustNodeName(attr.getNodeName());
      if (attributeName.equals(name)) {
        return attr.getNodeValue();
      }
    }
    return null;
  }


  /*
  @Override
  public String getAttributeValueStrict(String attributeName) throws XmlParseException {
    if (isEmpty()) {
      throw new XmlParseException("Navigator does not point to xml element. Requested Attribute not found: "
                                  + attributeName);
    }
    String name = getAttributeValue(attributeName);
    if (name == null) {
      throw new XmlParseException("Attribute not found (name = " + attributeName + ")");
    }
    return name;
  }
  */

  @Override
  public String getSelfDescendantString() throws XmlBuildException {
    if (isEmpty()) { return null; }
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
    Node node = doc.importNode(_current, true);
    doc.appendChild(node);

    boolean withPI = true;
    StringWriter sw = new StringWriter();
    try {
      Source source = new DOMSource(doc);
      Result result = new StreamResult(sw);
      TransformerFactory factory = TransformerFactory.newInstance();
      try {
        factory.setAttribute("indent-number", 2);
      } catch (IllegalArgumentException f) {
        //_logger.warn("Unable to set xml indent");
      }
      Transformer xformer = factory.newTransformer();
      xformer.setOutputProperty(OutputKeys.METHOD, "xml");
      xformer.setOutputProperty(OutputKeys.INDENT, "yes");
      xformer.setOutputProperty(OutputKeys.ENCODING, EncodingName.UTF_8.getStringValue());
      xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, withPI ? "no" : "yes");
      xformer.transform(source, result);
    } catch (TransformerException f) {
      throw new RuntimeException(f);
    }
    return sw.toString();
  }

}
