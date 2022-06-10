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

package com.gip.xtfutils.xmltools.nav.axiom;


import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.jaxp.OMSource;

import com.gip.xtfutils.xmltools.XmlBuildException;
import com.gip.xtfutils.xmltools.nav.XmlNavigator;


public class AxiomNavigator extends XmlNavigator {

  protected OMElement _root = null;
  protected OMElement _current = null;


  public AxiomNavigator() {}


  public AxiomNavigator(OMElement root) {
    init(root);
  }


  public AxiomNavigator(OMElement root, OMElement current) {
    _root = root;
    _current = current;
  }


  public AxiomNavigator(String xml) throws XMLStreamException {
    try {
      StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(xml.getBytes("UTF-8")));
      OMElement root = builder.getDocumentElement();
      init(root);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public void init(OMElement root) {
    _root = root;
    _current = root;
  }


  public boolean isEmpty() {
    return (_current == null);
  }


  public XmlNavigator descend(String childName, int index) {
    if (isEmpty()) { return this; }
    int count = 0;
    Iterator<?> it = _current.getChildrenWithLocalName(childName);
    while (it.hasNext() && (count <= index)) {
      Object obj = it.next();
      if (obj == null) { continue; }
      if (!(obj instanceof OMElement)) { continue; }
      OMElement tmp = (OMElement) obj;
      if (childName.equals(tmp.getLocalName()) && (count == index)) {
        _current = tmp;
        return this;
      }
      count++;
    }
    setToEmpty();
    return this;
  }


  public XmlNavigator descendToFirstChild() {
    if (isEmpty()) { return this; }
    Iterator<?> it = _current.getChildren();
    while (it.hasNext()) {
      Object child = it.next();
      if (child instanceof OMElement) {
        _current = (OMElement) child;
        return this;
      }
    }
    return this;
  }


  public String getText() {
    if (isEmpty()) { return null; }
    return _current.getText();
  }


  public String getAttributeValue(String name) {
    if (name == null) { return null; }
    if (isEmpty()) { return null; }
    /*
    OMAttribute attr = _current.getAttribute(new QName(name));
    if (attr == null) { return null; }
    return attr.getAttributeValue();
    */
    Iterator<?> it = _current.getAllAttributes();
    if (it.hasNext()) {
      OMAttribute attr = (OMAttribute) it.next();
      if (name.equals(attr.getLocalName())) {
        return attr.getAttributeValue();
      }
    }
    return null;
  }


  public XmlNavigator ascend() {
    if (isEmpty()) { return this; }
    OMContainer cont = _current.getParent();
    if ((cont == null) || !(cont instanceof OMElement)) {
      setToEmpty();
      return this;
    }
    _current = (OMElement) cont;
    return this;
  }


  public OMElement getCurrent() {
    return _current;
  }

  public OMElement getRoot() {
    return _root;
  }

  @Override
  public void setToEmpty() {
    _current = null;
  }

  @Override
  public XmlNavigator buildEmpty() {
    return new AxiomNavigator();
  }

  @Override
  public XmlNavigator clone() {
    return new AxiomNavigator(_root, _current);
  }

  @Override
  public XmlNavigator gotoRoot() {
    _current = _root;
    return this;
  }


  @Override
  public List<XmlNavigator> getAllChildren() {
    List<XmlNavigator> ret = new ArrayList<XmlNavigator>();
    if (isEmpty()) { return ret; }
    Iterator<?> it = _current.getChildren();
    while (it.hasNext()) {
      Object obj = it.next();
      if (obj == null) { continue; }
      if (!(obj instanceof OMElement)) { continue; }
      OMElement tmp = (OMElement) obj;
      ret.add(new AxiomNavigator(this.getRoot(), tmp));
    }
    return ret;
  }


  @Override
  public List<XmlNavigator> getChildrenByName(String childNameOrig) {
    if (childNameOrig == null) {
      throw new IllegalArgumentException("Child node name is null.");
    }
    List<XmlNavigator> ret = new ArrayList<XmlNavigator>();
    if (isEmpty()) { return ret; }
    Iterator<?> it = _current.getChildrenWithLocalName(childNameOrig);
    while (it.hasNext()) {
      Object obj = it.next();
      if (obj == null) { continue; }
      if (!(obj instanceof OMElement)) { continue; }
      OMElement tmp = (OMElement) obj;
      if (childNameOrig.equals(tmp.getLocalName())) {
        ret.add(new AxiomNavigator(this.getRoot(), tmp));
      }
    }
    return ret;
  }


  @Override
  public boolean isRoot() {
    return (_root == _current);
  }

  @Override
  public String getTagName() {
    if (isEmpty()) { return null; }
    return _current.getLocalName();
  }

  @Override
  public String getSelfDescendantString() throws XmlBuildException {
    OMElement element = _current;
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setAttribute("indent-number", Integer.valueOf(2));
      Transformer transformer = transformerFactory.newTransformer();

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(6) );
      StringWriter sw = new StringWriter();
      StreamResult result = new StreamResult(sw);
      OMSource source = new OMSource(element);
      transformer.transform( source, result );
      return sw.toString();
    }
    catch( TransformerException e) {
      throw new RuntimeException(e);
    }
  }

}

