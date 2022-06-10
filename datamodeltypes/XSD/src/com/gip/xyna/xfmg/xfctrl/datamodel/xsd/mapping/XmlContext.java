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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;

public class XmlContext {

  private Document doc;
  private NamespacePrefixCache namespacePrefixCache;
  private CreateXmlOptions createXmlOptions;

  public XmlContext(Document doc, CreateXmlOptions createXmlOptions) {
    this.doc = doc;
    this.createXmlOptions = createXmlOptions;
    this.namespacePrefixCache = createXmlOptions.getNamespacePrefixCache();
  }

  public Element createElement(boolean qualified, FQName name) {
    if( qualified ) {
      Element el = doc.createElementNS(name.getNamespace(), name.getName());
      el.setPrefix(namespacePrefixCache.getNamespacePrefix(name.getNamespace()));
      return el;
    } else {
      Element el = doc.createElement(name.getName());
      return el;
    }
  }
  
  public Attr createAttribute(boolean qualified, String namespace, String name) {
    if( qualified ) {
      Attr attr = doc.createAttributeNS(namespace, name);
      attr.setPrefix(namespacePrefixCache.getNamespacePrefix(namespace));
      return attr;
    } else {
      Attr attr = doc.createAttribute(name);
      return attr;
    }
  }
  public Attr createAttribute(boolean qualified, FQName name) {
    return createAttribute(qualified, name.getNamespace(), name.getName());
  }

  public CreateXmlOptions getCreateXmlOptions() {
    return createXmlOptions;
  }

  public String getNamespacePrefix(String namespace) {
    return namespacePrefixCache.getNamespacePrefix(namespace);
  }

  public void appendNamespaces(Element root) {
    for( Map.Entry<String,String> entry : namespacePrefixCache.getUsedNamespacePrefixes().entrySet() ) {
      String name;
      if( entry.getValue() == null || entry.getValue().equals("") ) {
        name = "xmlns";
      } else {
        name = "xmlns:"+entry.getValue();
      }
      root.setAttributeNS(NamespacePrefixCache.NAMESPACE_XMLNS, name, entry.getKey() ); //"xmlns:" ist hier Pflicht, sonst "org.w3c.dom.DOMException: NAMESPACE_ERR"
    }
  }

  public Attr createTypeAttribute(String namespace, String typeName) {
    Attr attrType = createAttribute( true, NamespacePrefixCache.NAMESPACE_XSI, "type" );
    String prefix = namespacePrefixCache.getNamespacePrefix(namespace);
    if( prefix == null || prefix.equals("") ) {
      attrType.setValue(typeName);
    } else {
      attrType.setValue(prefix+":"+typeName);
    }
    return attrType;
  }

  public Node importNode(Element el) {
    return doc.importNode(el, true);
  }

}
