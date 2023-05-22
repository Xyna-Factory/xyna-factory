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
package com.gip.xyna.xprc.xfractwfe.generation.xml;

import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.gip.xyna.utils.misc.StringReplacer;



/**
 *
 */
public class XmlBuilder {

  public final static String LINE_SEPARATOR = System.getProperty("line.separator");
  
  public final static StringReplacer ENCODER = StringReplacer.
      replace('&', "&amp;").
      replace('\'', "&apos;").
      replace('>', "&gt;").
      replace('<', "&lt;").
      replace('\"', "&quot;").
      build();
  
  private StringBuilder xml = new StringBuilder();
  private int indent = 0;
  
  public String toString() {
    return xml.toString();
  }
  
  private void indent() {
    for( int i=0; i<indent; ++i ) {
      xml.append("  ");
    }
  }

  /**
   * startet Element "&lt;elementName&gt;"
   * @param elementName
   */
  public void startElement(String elementName) {
    indent();
    xml.append("<").append(elementName).append(">").append(LINE_SEPARATOR);
    ++indent;
  }

  
  /**
   * startet Element "&lt;elementName"
   * @param elementName
   */
  public void startElementWithAttributes(String elementName) {
    indent();
    xml.append("<").append(elementName);
    ++indent;
  }
  
  /**
   * erg�nzt Attribute " name=\"value\""
   */
  public void addAttribute(String name, String value) {
    if( value == null ) {
      //keine Ausgabe von nicht gesetzten Attributen 
    } else {
      xml.append(" ").append(name).append("=\"").append(value).append("\"");
    }
  }
    
  /**
   * beendet die Attribute "&gt;\n"
   */
  public void endAttributes() {
    xml.append('>').append(LINE_SEPARATOR);
  }

  /**
   * beendet die Attribute "&gt;\n"
   */
  public void endAttributesNoLineBreak() {
    xml.append('>');
  }

  /**
   * kurzes Ende "/&gt;"
   */
  public void endAttributesAndElement() {
    xml.append("/>").append(LINE_SEPARATOR);
    --indent;
  }
  
  /**
   * vollst�ndiges Ende "&gt;value&lt;/elementName&gt;" 
   * @param value
   * @param elementName
   */
  public void endAttributesAndElement(String value, String elementName) {
    xml.append(">").append(value);
    xml.append("</").append(elementName).append('>').append(LINE_SEPARATOR);
    --indent;
  }

  /**
   * kurzes Ende "/&gt;"
   */
  public void endElement() {
    xml.append("/>").append(LINE_SEPARATOR);
    --indent;
  }

  /**
   * vollst�ndiges Ende "&lt;/elementName&gt;"
   */
  public void endElement(String elementName) {
    --indent;
    indent();
    xml.append("</").append(elementName).append('>').append(LINE_SEPARATOR);
  }

  /**
   * vollst�ndiges Ende "&lt;/elementName&gt;"
   */
  public void endElementNoIdent(String elementName) {
    --indent;
    xml.append("</").append(elementName).append('>').append(LINE_SEPARATOR);
  }

  /**
   * "&lt;elementName&gt;value&lt;/elementName&gt;"
   * @param elementName
   * @param value
   */
  public void element(String elementName, String value) {
    indent();
    xml.append("<").append(elementName).append('>').append(value);
    xml.append("</").append(elementName).append('>').append(LINE_SEPARATOR);
  }

  /**
   * "&lt;elementName/&gt;"
   * @param elementName
   */
  public void element(String elementName) {
    indent();
    xml.append("<").append(elementName).append("/>").append(LINE_SEPARATOR);
  }

  /**
   * "&lt;elementName&gt;value&lt;/elementName&gt;"
   * @param elementName
   * @param value
   */
  public void optionalElement(String elementName, String value) {
    if( value != null) {
      element(elementName, value);
    }
  }

  /**
   * @param string
   */
  public void append(String string) {
    xml.append(string);
  }

  public void append(Element element) {
    DOMImplementationLS lsImpl = (DOMImplementationLS)element.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
    LSSerializer serializer = lsImpl.createLSSerializer();
    serializer.getDomConfig().setParameter("xml-declaration", false);
    serializer.getDomConfig().setParameter("namespace-declarations", false);

    indent();
    xml.append(serializer.writeToString(element)).append(LINE_SEPARATOR);
  }

  public static String encode(String string) {
    return ENCODER.replace(string);
  }
  

}
