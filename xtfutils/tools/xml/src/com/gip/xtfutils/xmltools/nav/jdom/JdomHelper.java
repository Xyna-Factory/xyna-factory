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

package com.gip.xtfutils.xmltools.nav.jdom;

import java.io.StringReader;
import java.io.StringWriter;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.gip.xtfutils.xmltools.XmlBuildException;
import com.gip.xtfutils.xmltools.XmlParseException;


public class JdomHelper {

  public static final String UTF8 = "UTF-8";


  public static Document createDocument(String xml) throws XmlParseException {
    org.jdom2.input.SAXBuilder parser = new  org.jdom2.input.SAXBuilder();
    StringReader sr = new StringReader(xml);
    try {
      return parser.build(sr);
    }
    catch (Exception e) {
      throw new XmlParseException("Parsing of xml String failed.", e);
    }
  }



  /**
  * Liefert ein XML Dokument als String mit dem uebergebenen Element als Root.
   * @throws XmlBuildException
  */
 public static String toXmlString(Element xmlElement) throws XmlBuildException {
   if (xmlElement == null) { return ""; }
   Element dummy = xmlElement.clone();
   Document doc = new Document(dummy);
   String ret = documentToString(doc);
   return ret;
 }


 /**
  * Umwandeln eines XMl Documents in einen String
 * @throws XmlBuildException
  */
 public static String documentToString(Document doc) throws XmlBuildException {
   if (doc == null) { return ""; }
   String indent, lineSep;
   indent = " ";
   lineSep = "\n";

   // Umwandeln des JDom Trees in einen String
   try {
     XMLOutputter outp = new XMLOutputter();
     Format format = Format.getPrettyFormat();
     format.setIndent(indent);
     format.setLineSeparator(lineSep);
     format.setEncoding(UTF8);
     outp.setFormat(format);
     StringWriter sw = new StringWriter();

     outp.output(doc, sw);
     return sw.toString();
   }
   catch (Exception e) {
     throw new XmlBuildException(e);
   }
 }

}
