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

package com.gip.xtfutils.xmltools.build.jdom;

import java.io.StringWriter;

import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.gip.xtfutils.xmltools.XmlNamespace;
import com.gip.xtfutils.xmltools.build.*;



public class XmlJdomBuilder extends XmlDomBuilder {

  public static final String UTF8 = "UTF-8";
  public static final String ISO15 = "iso-8859-15";


  //protected Document _doc = null;

  @Override
  public String buildXmlString() {
    return buildXmlString(EncodingName.UTF_8);
  }

  @Override
  public String buildXmlString(EncodingName encoding) {
    Document doc = buildDocument();
    return getDocumentString(doc, encoding);
  }

  public Document buildDocument() {
    Document doc = new Document();
    Element docRoot = adaptElementAndChildren(_root);
    doc.setRootElement(docRoot);
    return doc;
  }


  protected Element adaptElementAndChildren(ElementNode elem) {
    Namespace nsp = null;
    Element ret = null;

    if (elem.getNamespace() != null) {
      nsp = Namespace.getNamespace(elem.getNamespace().getPrefix(), elem.getNamespace().getUri());
      ret = new Element(elem.getTagName(), nsp);
    }
    else {
      ret = new Element(elem.getTagName());
    }

    if (elem.getNamespaceDeclarations().size() > 0) {
      for (XmlNamespace item : elem.getNamespaceDeclarations()) {
        Namespace nsp2 = Namespace.getNamespace(item.getPrefix(), item.getUri());
        ret.addNamespaceDeclaration(nsp2);
      }
    }

    if (elem.getAttributes() != null) {
      for (XmlAttribute attr : elem.getAttributes()) {
        if (attr.hasNamespace()) {
          XmlNamespace attrNsp = attr.getNamespace();
          Namespace nsp2 = Namespace.getNamespace(attrNsp.getPrefix(), attrNsp.getUri());
          ret.setAttribute(attr.getName(), attr.getValue(), nsp2);
        }
        else {
          ret.setAttribute(attr.getName(), attr.getValue());
        }
      }
    }

    for (BasicNode child: elem.getChildren()) {
      if (child instanceof TextNode) {
        TextNode text = (TextNode) child;
        ret.addContent(new Text(text.getText()));
      }
      else if (child instanceof ElementNode) {
        ElementNode childElem = (ElementNode) child;
        ret.addContent(adaptElementAndChildren(childElem));
      }
    }
    return ret;
  }


  public static String getDocumentString(Document doc) {
    return getDocumentString(doc, EncodingName.UTF_8);
  }


  public static String getDocumentString(Document doc, EncodingName encoding)  {
    if (doc == null) { return ""; }

    String indent,lineSep;
    indent = " ";
    lineSep = "\n";

    XMLOutputter outp = new XMLOutputter();
    Format format = Format.getPrettyFormat();
    format.setIndent(indent);
    format.setLineSeparator(lineSep);

    if (encoding == EncodingName.UTF_8) {
      format.setEncoding(UTF8);
    }
    else if (encoding == EncodingName.ISO_8859_15) {
      format.setEncoding(ISO15);
    }
    outp.setFormat(format);
    try {
      StringWriter sw = new StringWriter();
      outp.output(doc, sw);
      return sw.toString();
    }
    catch (Exception e) {
      //TODO
      //do nothing
    }
    return "";
  }

}
