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

package com.gip.xyna.utils.xml;

import java.io.IOException;
import java.io.Writer;

/**
 * @deprecated use XMLDocument instead. Will be remove in a late release!
 */
public class XML {

   private XMLDocument xmlDoc = null;

   /**
    * @param indentlevel
    */
   public XML(int indentlevel) {
      xmlDoc = new XMLDocument(indentlevel);
   }

   /**
    * Leeren des bisherigen XMLs
    */
   public void clear() {
      xmlDoc.clear();
   }

   /**
    * Store xml header.
    * 
    * @param version
    *              xml version
    * @param encoding
    *              used encoding
    */
   public void header(String version, String encoding) {
      xmlDoc.addDeclaration(version, encoding);
   }

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * Eg. <tag>value</tag> if value != null
    * <p>
    * or <tag/> if value == null
    * 
    * @param tag
    *              element name
    * @param value
    *              CDATA value of the element
    */
   public void enclose(String tag, String value) {
      xmlDoc.addElement(tag, value);
   }

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * Eg. <tag>value</tag> if value != null
    * <p>
    * or <tag/> if value == null
    * 
    * @param tag
    *              element name
    * @param value
    *              CDATA value of the element
    */
   public void enclose(String tag, Object value) {
      xmlDoc.addElement(tag, value);
   }

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              CDATA value of the element
    */
   public void enclose(String tag, int value) {
      xmlDoc.addElement(tag, value);
   }

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * The element will only be added if value != null.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              CDATA value of the element
    */
   public void encloseOptional(String tag, String value) {
      xmlDoc.addOptionalElement(tag, value);
   }

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * The element will only be added if value != null.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              CDATA value of the element
    */
   public void encloseOptional(String tag, Object value) {
      xmlDoc.addOptionalElement(tag, value);
   }

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * The element will only be added if value != Integer.MIN_VALUE.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              CDATA value of the element
    */
   public void encloseOptional(String tag, int value) {
      xmlDoc.addOptionalElement(tag, value);
   }

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * The element will only be added if value != Integer.MIN_VALUE.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              CDATA value of the element
    */
   public void encloseOptional(String tag, long value) {
      xmlDoc.addOptionalElement(tag, value);
   }

   /**
    * Add a element of the given tag name with a single attribute which has the
    * specified value.
    * <p>
    * Eg. <tag attribute='value'/>
    * 
    * @param tag
    *              element name
    * @param attribute
    *              name of the attribute
    * @param value
    *              value of the attribute
    *          
    */
   public void attribute(String tag, String attribute, String value) {
      xmlDoc.addElement(tag, attribute, value, "");
   }

   /**
    * Add a start tag. Also increase the indent.
    * <p>
    * Eg. <tag>
    * 
    * @param tag
    *              element name
    */
   public void begin(String tag) {
      xmlDoc.addStartTag(tag);
   }

   /**
    * Add a start tag with attributes. Also increase the indent.
    * <p>
    * Eg. <tag>
    * 
    * @param tag
    *              element name
    * @param attributes
    */
   public void begin(String tag, String attributes) {
      xmlDoc.addStartTag(tag, attributes);
   }

   /**
    * Add a end tag. Also decrease indent.
    * <p>
    * Eg. <tag/>
    * 
    * @param tag
    *              element name
    */
   public void end(String tag) {
      xmlDoc.addEndTag(tag);
   }

   /**
    * Append sub document.
    * 
    * @param xml
    *              xml document
    */
   public void append(XML xml) {
      xmlDoc.append(xml.toString());
   }

   /**
    * Append sub document.
    * 
    * @param string
    *              xml string
    */
   public void append(String string) {
      xmlDoc.append(string);
   }


   /**
    * Write the cached xml document with the given writer.
    * 
    * @param writer
    *              writer to write cached xml document
    * @throws IOException
    */
   public void write(Writer writer) throws IOException {
      xmlDoc.write(writer);
   }

   /**
    * @see java.lang.Object#toString()
    */
   public String toString() {
      return xmlDoc.toString();
   }
   
   /**
    * @return Namespace
    */
   public String getNamespace() {
      return xmlDoc.getNamespace();
   }

   /**
    * @param ns
    *              Setzt den Namespace
    */
   public void setNamespace(String ns) {
      xmlDoc.setNamespace(ns);
   }

}
