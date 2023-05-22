/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xtfutils.xmltools.nav.jdom;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

import com.gip.xtfutils.xmltools.XmlBuildException;
import com.gip.xtfutils.xmltools.XmlParseException;
import com.gip.xtfutils.xmltools.nav.XmlNavigator;



public class JdomNavigator extends XmlNavigator {

  protected static Logger _logger = Logger.getLogger(JdomNavigator.class);

  /*
  public enum ThrowExceptionIfContentNotFound {
    TRUE, FALSE
  }
  */

  protected Document _doc = null;
  protected Element _current = null;

  //protected ThrowExceptionIfContentNotFound _doThrowException = ThrowExceptionIfContentNotFound.FALSE;

  public JdomNavigator() {}

  public JdomNavigator(Document doc) {
    _doc = doc;
    _current = _doc.getRootElement();
  }

  public JdomNavigator(Element elem) {
    _current = elem;
    if (elem != null) { _doc = _current.getDocument(); }
  }

  public JdomNavigator(String xml) throws XmlParseException {
    init(xml);
  }

  protected void init(String xml) throws XmlParseException {
    if (xml == null) {
      throw new XmlParseException("JdomNavigator: xml string is null.");
    }
    _doc = JdomHelper.createDocument(xml);
    _current = _doc.getRootElement();
  }


  public Element getCurrentElement() {
    return _current;
  }

  public boolean isEmpty() {
    return (_current == null);
  }

  public void setToEmpty() {
    _current = null;
  }

  @Override
  public XmlNavigator clone() {
    XmlNavigator cloned = new JdomNavigator(_current);
    return cloned;
  }

  public XmlNavigator gotoRoot() {
    if (_doc == null) { return this; }
    _current = _doc.getRootElement();
    return this;
  }


  /*
  public XmlNavigator descend(String... list) {
    for (String s : list) {
      descend(s);
    }
    return this;
  }
  */

  /*
  public XmlNavigator descendStrict(String child) throws XmlParseException {
    return descendStrict(child, 0);
  }
  */

  /*
  public XmlNavigator descend(String child) {
    descend(child, 0);
    return this;
  }
  */

  /*
  public XmlNavigator descendStrict(String childNameOrig, int index) throws XmlParseException {
    descend(childNameOrig, index);
    if (this.isEmpty()) {
      throw new XmlParseException("Child Element not found: " + childNameOrig + " (index: " + index + ")");
    }
    return this;
  }
  */

  public XmlNavigator descendToFirstChild() {
    if (_current == null) { return this; }
    List<?> kids = _current.getChildren();
    if (kids.size() < 1) {
      _current = null;
      return this;
    }
    Object obj = kids.get(0);
    if (!(obj instanceof Element)) {
      _current = null;
      return this;
    }
    _current = (Element) obj;
    return this;
  }


  public XmlNavigator descend(String childNameOrig, int index) {
    if (childNameOrig == null) { return this; }
    if (_current == null) { return this; }
    List<?> kids = _current.getChildren();

    //TODO: geht nicht wegen zu niedriger jdom-version:
    //List<?> named = new ElementFilter(child).filter();

    String childName = childNameOrig.trim();
    int count = 0;
    for (Object obj : kids) {
      if (!(obj instanceof Element)) {
        continue;
      }
      Element child = (Element) obj;
      //_logger.trace("Checking child with name " + child.getName());

      if (childName.equals(child.getName())) {
        if (count == index) {
          _current = child;
          return this;
        }
        else {
          count++;
        }
      }
    }
    //not found:
    _current = null;
    return this;
  }


  /*
  public int getNumChildrenWithName(String childNameOrig) {
    List<XmlNavigator> list = getChildrenByName(childNameOrig);
    return list.size();
  }
  */

  /*
  public XmlNavigator getFirstChildWithName(String childNameOrig) {
    XmlNavigator clone = this.clone();
    clone.descend(childNameOrig);
    return clone;
  }
  */

  public List<XmlNavigator> getChildrenByName(String childNameOrig) {
    List<XmlNavigator> ret = new ArrayList<XmlNavigator>();
    if (_current == null) { return ret; }
    List<?> kids = _current.getChildren();

    //TODO: geht nicht wegen zu niedriger jdom-version:
    //List<?> named = new ElementFilter(child).filter();

    String childName = childNameOrig.trim();
    for (Object obj : kids) {
      if (!(obj instanceof Element)) {
        continue;
      }
      Element child = (Element) obj;
      //_logger.debug("Checking for list child with name " + child.getName());
      if (childName.equals(child.getName())) {
        ret.add(new JdomNavigator(child));
      }
    }
    return ret;
  }


  /*
  public XmlNavigator getFirstChild() {
    if (_current == null) { return this; }
    List<?> kids = _current.getChildren();
    if (kids.size() < 1) {
      return this;
    }
    Element child = (Element) kids.get(0);
    return new JdomNavigator(child);
  }
  */

  public List<XmlNavigator> getAllChildren() {
    List<XmlNavigator> ret = new ArrayList<XmlNavigator>();
    if (_current == null) { return ret; }
    List<?> kids = _current.getChildren();

    //TODO: geht nicht wegen zu niedriger jdom-version:
    //List<?> named = new ElementFilter(child).filter();

    for (Object obj : kids) {
      if (!(obj instanceof Element)) {
        continue;
      }
      Element child = (Element) obj;
      ret.add(new JdomNavigator(child));
    }
    return ret;
  }


  /*
  public XmlNavigator ascend(int steps) {
    if (_current == null) { return this; }
    for (int i = 0; i < steps; i++) { ascend(); }
    return this;
  }
  */

  public XmlNavigator ascend() {
    if (_current == null) { return this; }
    _current = _current.getParentElement();
    return this;
  }

  public boolean isRoot() {
    if (_current == null) { return false; }
    return _current.isRootElement();
  }

  public String getTagName() {
    if (_current == null) { return null; }
    return _current.getName();
  }

  public String getText() {
    if (_current == null) { return null; }
    return _current.getText();
  }

  /*
  public String getChildText(String childName) {
    if (_current == null) { return null; }
    return getFirstChildWithName(childName).getText();
  }
  */

  /*
  public long getTextAsLong() throws XmlParseException {
    if (_current == null) { throw new XmlParseException("JdomNavigator does not point to xml element."); }
    return Long.parseLong(_current.getText());
  }
  */

  /*
  public Long getTextAsLongOrNull() {
    if (_current == null) { return null; }
    try {
      return Long.parseLong(_current.getText());
    }
    catch (Exception e) {
      //do nothing
    }
    return null;
  }
  */

  public String getAttributeValue(String attributeName) {
    if (_current == null) { return null; }
    //return _current.getAttributeValue(attributeName);
    try {
      Attribute attr = getFirstAttribute(attributeName);
      if (attr != null) {
        return attr.getValue();
      }
    }
    catch (Exception e) {
      //do nothing
    }
    return null;
  }


  protected Attribute getFirstAttribute(String attributeName) throws XmlParseException {
    if (attributeName == null) {
      throw new XmlParseException("Attribute name may not be null.");
    }
    List<?> list = _current.getAttributes();
    for (Object obj : list) {
      if (!(obj instanceof Attribute)) { continue; }
      Attribute attr = (Attribute) obj;
      if (attributeName.equals(attr.getName())) {
        return attr;
      }
    }
    throw new XmlParseException("Attribute not found (name = " + attributeName + ")");
  }


  /*
  public String getAttributeValueStrict(String attributeName) throws XmlParseException {
    if (_current == null) {
      throw new XmlParseException("JdomNavigator does not point to xml element. Requested Attribute not found: "
                                  + attributeName);
    }
    Attribute attr = getFirstAttribute(attributeName);
    if (attr == null) {
      throw new XmlParseException("Attribute not found (name = " + attributeName + ")");
    }
    return attr.getValue();
  }
  */

  public XmlNavigator buildEmpty() {
    return new JdomNavigator();
  }

  public String getSelfDescendantString() throws XmlBuildException {
    return JdomHelper.toXmlString(this.getCurrentElement());
  }

}
