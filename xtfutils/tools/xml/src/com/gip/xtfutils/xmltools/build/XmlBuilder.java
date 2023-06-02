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

package com.gip.xtfutils.xmltools.build;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xtfutils.xmltools.XmlNamespace;



public abstract class XmlBuilder {

  private static Logger _logger = Logger.getLogger(XmlBuilder.class);


  public static class TextNode implements BasicNode {
    protected String _text = null;

    public TextNode(String text) { _text = text; }
    public String getText() { return _text; }
  }


  // ### start of main class

  protected ElementNode _root = null;
  protected List<ElementNode> _ancestors = new ArrayList<ElementNode>();

  /*
  public ElementNode getRootNode() {
    return _root;
  }
  */

  public ElementNode getCurrentNode() {
    if (_ancestors.size() < 1) { return null; }
    return _ancestors.get(_ancestors.size() - 1);
  }

  public XmlNamespace getCurrentNamespace() {
    if (getCurrentNode() == null) { return null; }
    return getCurrentNode().getNamespace();
  }

  public ElementNode openTagWithNoNamespace(String name) {
    return openTag(name, null);
  }

  public ElementNode openTag(String name) {
    if (getCurrentNode() == null) {
      return openTag(name, null);
    }
    return openTag(name, getCurrentNamespace());
  }

  public abstract ElementNode createElementNode(String name, XmlNamespace nsp);
  protected abstract TextNode createTextNode(String text);


  public ElementNode createElementNode(String name) {
    return createElementNode(name, null);
  }

  public ElementNode openTag(String name, XmlNamespace nsp) {
    //_logger.debug("Open Tag: " + name + ", " + nsp);

    ElementNode node = createElementNode(name, nsp);
    if (_ancestors.size() > 0) {
      getCurrentNode().addChild(node);
    }
    else {
      _root = node;
    }
    _ancestors.add(node);
    return node;
  }

  public void closeTag() {
    if (_ancestors.size() < 1) { return; }
    _ancestors.remove(_ancestors.size() - 1);
  }

  public void closeAllTags() {
    if (_ancestors.size() < 1) { return; }
    _ancestors.clear();
  }

  public void closeTag(String name) {
    if (name == null) { throw new IllegalArgumentException("XmlBuilder: Node name may not be null."); }
    if (name.trim().equals(getCurrentNode().getTagName())) {
      closeTag();
      return;
    }
    throw new IllegalArgumentException("XmlBuilder: Opening and closing tag names do not match.");
  }

  public void addInnerText(String text) {
    if (_ancestors.size() < 1) { return; }
    if (text == null) { return; }
    getCurrentNode().addChild(createTextNode(text));
  }


  public ElementNode addChildElementWithInnerText(String name, String text) {
    return addChildElementWithInnerText(name, text, getCurrentNamespace());
  }

  public ElementNode addChildElementWithInnerText(String name, String text, XmlNamespace nsp) {
    ElementNode node = openTag(name, nsp);
    if (text != null) {
      getCurrentNode().addChild(createTextNode(text));
    }
    closeTag(name);
    return node;
  }

  public ElementNode addEmptyChildElement(String name) {
    return addEmptyChildElement(name, getCurrentNamespace());
  }

  public ElementNode addEmptyChildElement(String name, XmlNamespace nsp) {
    ElementNode node = openTag(name, nsp);
    closeTag(name);
    return node;
  }

  public void append(XmlBuilder html) {
    if (html._ancestors.size() < 1) { return; }
    getCurrentNode().addChild(html._ancestors.get(0));
  }

  public abstract String buildXmlString();

  public abstract String buildXmlString(EncodingName encoding);
}
