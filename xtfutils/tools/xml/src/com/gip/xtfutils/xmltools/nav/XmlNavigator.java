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

package com.gip.xtfutils.xmltools.nav;

import java.util.List;

import com.gip.xtfutils.xmltools.XmlBuildException;
import com.gip.xtfutils.xmltools.XmlParseException;



public abstract class XmlNavigator {


  public abstract boolean isEmpty();

  public abstract void setToEmpty();

  public abstract XmlNavigator buildEmpty();

  @Override
  public abstract XmlNavigator clone();

  public abstract XmlNavigator gotoRoot();


  public XmlNavigator descend(String child, String... list) {
    descend(child);
    for (String s : list) {
      descend(s);
    }
    return this;
  }

  public XmlNavigator descendStrict(String child) throws XmlParseException {
    return descendStrict(child, 0);
  }

  public XmlNavigator descend(String child) {
    descend(child, 0);
    return this;
  }

  public XmlNavigator descendStrict(String childNameOrig, int index) throws XmlParseException {
    descend(childNameOrig, index);
    if (this.isEmpty()) {
      throw new XmlParseException("Child Element not found: " + childNameOrig + " (index: " + index + ")");
    }
    return this;
  }

  public abstract XmlNavigator descendToFirstChild();

  public abstract XmlNavigator descend(String childNameOrig, int index);

  public int getNumChildrenWithName(String childNameOrig) {
    List<XmlNavigator> list = getChildrenByName(childNameOrig);
    return list.size();
  }

  public XmlNavigator getFirstChildWithName(String childNameOrig) {
    XmlNavigator clone = this.clone();
    clone.descend(childNameOrig);
    return clone;
  }

  public XmlNavigator getFirstChildWithNameStrict(String childNameOrig) throws XmlParseException {
    XmlNavigator ret = getFirstChildWithName(childNameOrig);
    if (ret.isEmpty()) {
      throw new XmlParseException("No xml element '" +  childNameOrig + "' found.");
    }
    return ret;
  }

  public abstract List<XmlNavigator> getChildrenByName(String childNameOrig);

  public XmlNavigator getFirstChild() {
    XmlNavigator clone = this.clone();
    return clone.descendToFirstChild();
  }

  public abstract List<XmlNavigator> getAllChildren();


  public XmlNavigator ascend(int steps) {
    if (isEmpty()) { return this; }
    for (int i = 0; i < steps; i++) { ascend(); }
    return this;
  }


  public abstract XmlNavigator ascend();

  public abstract boolean isRoot();

  public abstract String getTagName();

  public abstract String getText();

  public String getChildText(String childName) {
    XmlNavigator cloned = this.clone();
    cloned.descend(childName);
    return cloned.getText();
  }

  public String getChildTextStrict(String childName) throws XmlParseException {
    XmlNavigator cloned = this.clone();
    cloned.descend(childName);
    if (cloned.isEmpty()) {
      throw new XmlParseException("Child element not found: " + childName);
    }
    return cloned.getText();
  }

  public long getTextAsLong() throws XmlParseException {
    if (isEmpty()) { throw new XmlParseException("XmlNavigator does not point to xml element."); }
    String text = getText();
    //TODO: return 0 ?
    if (text == null) { throw new XmlParseException("Node text is empty."); }
    return Long.parseLong(getText());
  }

  public Long getTextAsLongOrNull() {
    if (isEmpty()) { return null; }
    String text = getText();
    if (text == null) { return null; }
    return new Long(getText());
  }

  public abstract String getAttributeValue(String attributeName);


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

  public abstract String getSelfDescendantString() throws XmlBuildException;

  public List<XmlNavigator> getChildElementsRecursively(String childName) {
    List<XmlNavigator> ret = getChildrenByName(childName);
    List<XmlNavigator> kids = getAllChildren();
    for (XmlNavigator child : kids) {
      ret.addAll(child.getChildElementsRecursively(childName));
    }
    return ret;
  }


  public XmlNavigator getFirstDescendantWithName(String childName) {
    List<XmlNavigator> list = getChildrenByName(childName);
    if (list.size() > 0) {
      return list.get(0);
    }
    List<XmlNavigator> kids = getAllChildren();
    for (XmlNavigator child : kids) {
      XmlNavigator tmp = child.getFirstDescendantWithName(childName);
      if (!tmp.isEmpty()) {
        return tmp;
      }
    }
    XmlNavigator ret = this.clone();
    ret.setToEmpty();
    return ret;
  }

  public XmlNavigator getFirstDescendantWithNameStrict(String childName) throws XmlParseException {
    XmlNavigator ret = getFirstDescendantWithName(childName);
    if (ret.isEmpty()) {
      throw new XmlParseException("No xml element '" +  childName + "' found.");
    }
    return ret;
  }

}
