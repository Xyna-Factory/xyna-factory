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

package com.gip.xtfutils.xmltools.build.w3cdom;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import com.gip.xtfutils.xmltools.XmlNamespace;
import com.gip.xtfutils.xmltools.build.*;


public class XmlW3cDomBuilder extends XmlDomBuilder {


  @Override
  public String buildXmlString() {
    return buildXmlString(EncodingName.UTF_8);
  }


  @Override
  public String buildXmlString(EncodingName encoding) {
    Document doc = buildDocument();
    return getDocumentString(doc, encoding);
  }


  private String getDocumentString(Document document, EncodingName encoding) {
    boolean withPI = true;
    StringWriter sw = new StringWriter();
    try {
      Source source = new DOMSource(document);
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
      xformer.setOutputProperty(OutputKeys.ENCODING, encoding.getStringValue());
      xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, withPI ? "no" : "yes");
      xformer.transform(source, result);
    } catch (TransformerException f) {
      throw new RuntimeException(f);
    }
    return sw.toString();
  }


  protected Document buildDocument() {
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
    Element docRoot = adaptElementAndChildren(_root, doc);
    doc.appendChild(docRoot);
    return doc;
  }


  protected Element adaptElementAndChildren(ElementNode elem, Document doc) {
    Element ret = null;

    if (elem.getNamespace() != null) {
      ret = doc.createElementNS(elem.getNamespace().getUri(),
                          elem.getNamespace().getPrefix() + ":" + elem.getTagName());
    }
    else {
      ret = doc.createElement(elem.getTagName());
    }

    if (elem.getNamespaceDeclarations().size() > 0) {
      for (XmlNamespace item : elem.getNamespaceDeclarations()) {
        ret.setAttribute("xmlns:" + item.getPrefix(), item.getUri());
      }
    }

    if (elem.getAttributes() != null) {
      for (XmlAttribute attr : elem.getAttributes()) {
        if (attr.hasNamespace()) {
          ret.setAttributeNS(attr.getNamespace().getUri(),
                             attr.getNamespace().getPrefix() + ":" + attr.getName(),
                             attr.getValue());
        }
        else {
          ret.setAttribute(attr.getName(), attr.getValue());
        }
      }
    }

    for (BasicNode child: elem.getChildren()) {
      if (child instanceof TextNode) {
        TextNode textNode = (TextNode) child;
        if (textNode.getText() != null) {
          Text text = doc.createTextNode(textNode.getText());
          ret.appendChild(text);
        }
      }
      else if (child instanceof ElementNode) {
        ElementNode childElem = (ElementNode) child;
        Element w3cChild = adaptElementAndChildren(childElem, doc);
        ret.appendChild(w3cChild);
      }
    }
    return ret;
  }

}
