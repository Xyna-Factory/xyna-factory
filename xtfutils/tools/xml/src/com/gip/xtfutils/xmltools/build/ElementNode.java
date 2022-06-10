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

package com.gip.xtfutils.xmltools.build;

import java.util.ArrayList;
import java.util.List;

import com.gip.xtfutils.xmltools.XmlNamespace;
import com.gip.xtfutils.xmltools.build.XmlBuilder.*;

public class ElementNode implements BasicNode {

  protected String _tagName = null;
  protected List<BasicNode> _children = new ArrayList<BasicNode>();
  protected List<XmlAttribute> _attributes = new ArrayList<XmlAttribute>();
  protected XmlNamespace _namespace = null;
  protected List<XmlNamespace> _namespaceDeclarations = new ArrayList<XmlNamespace>();

  //TODO:setter
  protected List<XmlNamespace> _namespaceDefinitions = new ArrayList<XmlNamespace>();

  public ElementNode(String name) {
    _tagName = name;
  }

  public ElementNode(String name, XmlNamespace nsp) {
    _tagName = name;
    _namespace = nsp;
    /*
    if (XmlNamespace.NO_NAMESPACE.equals(nsp)) {
      _namespace = null;
    }
    else {
      _namespace = nsp;
    }
    */
  }

  public String getTagName() { return _tagName; }

  public XmlNamespace getNamespace() { return _namespace; }

  public ElementNode addAttribute(XmlAttribute attr) {
    _attributes.add(attr);
    return this;
  }

  public ElementNode addAttribute(String name, String value) {
    return addAttribute(new XmlAttribute(name, value));
  }

  public ElementNode addAttribute(String name, long value) {
    return addAttribute(new XmlAttribute(name, "" + value));
  }

  public ElementNode addAttribute(String name, String value, XmlNamespace nsp) {
    return addAttribute(new XmlAttribute(name, value, nsp));
  }

  public List<XmlAttribute> getAttributes() { return _attributes; }

  public List<BasicNode> getChildren() { return _children; }

  public List<XmlNamespace> getNamespaceDeclarations() { return _namespaceDeclarations; }

  public void addChild(BasicNode node) {
    if (node == null) {
      throw new IllegalArgumentException("New Child node may not be null.");
    }
    _children.add(node);
  }

  public ElementNode createChildNodeWithNoNamespace(String name) {
    return createChildNode(name, null);
  }

  public ElementNode createChildNode(String name) {
    return createChildNode(name, _namespace);
  }

  public ElementNode createChildNode(String name, XmlNamespace nsp) {
    ElementNode child = new ElementNode(name, nsp);
    this.addChild(child);
    return child;
  }

  public void addInnerText(String text) {
    TextNode t = new TextNode(text);
    this.addChild(t);
  }

  public void setNamespace(XmlNamespace nsp) {
    _namespace = nsp;
  }

  public void addNamespaceDeclaration(XmlNamespace nsp) {
    _namespaceDeclarations.add(nsp);
  }
}
