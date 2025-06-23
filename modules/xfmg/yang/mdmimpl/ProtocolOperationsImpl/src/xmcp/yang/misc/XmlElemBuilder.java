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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class XmlElemBuilder {

  private final Document _doc;
  private String _elementName = null;
  private String _namespace = null;
  private String _text = null;
  
  
  public XmlElemBuilder(Document doc) {
    _doc = doc;
  }
  
  
  public XmlElemBuilder elementName(String val) {
    _elementName = val;
    return this;
  }
  
  
  public XmlElemBuilder namespace(String val) {
    _namespace = val;
    return this;
  }
  
  
  public XmlElemBuilder text(String val) {
    _text = val;
    return this;
  }
  
  
  private Element build() {
    if (_elementName == null) {
      throw new IllegalArgumentException("Xml element name not set");
    }
    Element ret = null;
    if (_namespace != null) {
      ret = _doc.createElementNS(_namespace, _elementName);
      ret.setAttribute("xmlns", _namespace);
    } else {
      ret = _doc.createElement(_elementName);
    }
    if (_text != null) {
      Text text = _doc.createTextNode(_text);
      ret.appendChild(text);
    }
    return ret;
  }
  
  
  public Element buildAndAppendAsDocumentRoot() {
    Element ret = build();
    _doc.appendChild(ret);
    return ret;
  }
  
  
  public Element buildAndAppendAsChild(Element parent) {
    Element ret = build();
    parent.appendChild(ret);
    return ret;
  }
  
}
