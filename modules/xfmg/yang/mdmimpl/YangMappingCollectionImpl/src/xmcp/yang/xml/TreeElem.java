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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;


public class TreeElem implements Comparable<TreeElem> {

  public static enum XmlNodeStringOptions {
    SINGLE_LINE_WITH_NSP,
    SINGLE_LINE_WITHOUT_NSP,
  }
  
  private final YangXmlPathElem _element;
  private List<TreeElem> _children = new ArrayList<>();
  
  
  public TreeElem(YangXmlPathElem element) {
    _element = element;
  }
  
  
  public YangXmlPathElem getYangXmlPathElement() {
    return _element;
  }

  
  public List<TreeElem> getChildren() {
    return _children;
  }

  
  public Optional<TreeElem> getOptionalMatchingChild(YangXmlPathElem input) {
    return getOptionalMatchingListElement(input, _children);
  }
  
  
  public static Optional<TreeElem> getOptionalMatchingListElement(YangXmlPathElem input, List<TreeElem> list) {
    for (TreeElem child : list) {
      int val = input.compareTo(child._element); 
      if (val == 0) {
        return Optional.ofNullable(child);
      }
      else if (val < 0) {
        // since child list is sorted, iteration can be aborted
        return Optional.empty();
      }
    }
    return Optional.empty();
  }
  
  
  public void addChildUnlessPresent(YangXmlPathElem input) {
    if (getOptionalMatchingChild(input).isPresent()) { return; }
    TreeElem child = new TreeElem(input);
    addChild(child);
  }
  
  
  public TreeElem addChild(YangXmlPathElem input) {
    TreeElem child = new TreeElem(input);
    addChild(child);
    return child;
  }
  
  public void addChild(TreeElem child) {
    _children.add(child);
    Collections.sort(_children);
  }
  
  
  @Override
  public int compareTo(TreeElem input) {
    return _element.compareTo(input._element);
  }
  
  
  @Override 
  public boolean equals(Object obj) {
    if (obj instanceof TreeElem) {
      return compareTo((TreeElem) obj) == 0;
    }
    return false;
  }
  
  
  @Override
  public int hashCode() {
    return _element.hashCode();
  }
  
  
  public Optional<Element> toW3cElement(Document doc) {
    if (_element.hasListIndex()) {
      if (_children.size() == 1) {
        return _children.get(0).toW3cElement(doc);
      }
      return Optional.empty();
    }
    Element ret = null;
    if (_element.hasNamespace()) {
      ret = doc.createElementNS(_element.getNamespace().get(), _element.getElemName());
      ret.setAttribute("xmlns", _element.getNamespace().get());
    } else {
      ret = doc.createElement(_element.getElemName());
    }
    if (_element.hasTextValue()) {
      Text text = doc.createTextNode(_element.getTextValue().get());
      ret.appendChild(text);
    }
    for (TreeElem item : _children) {
      Optional<Element> child = item.toW3cElement(doc);
      if (child.isPresent()) {
        ret.appendChild(child.get());
      }
    }
    return Optional.ofNullable(ret);
  }
  
}
