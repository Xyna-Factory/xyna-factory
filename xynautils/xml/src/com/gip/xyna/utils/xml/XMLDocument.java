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
package com.gip.xyna.utils.xml;

import java.io.IOException;
import java.io.Writer;

/**
 * Container for xml documents.
 * <p>
 * Allow the creation of optional xml documents.
 * 
 */
public class XMLDocument {

   private static final String DEFAULT_INDENT = "  ";

   private int indentlevel;
   private String indent;
   private StringBuffer buffer;
   private String namespace = "";

   /**
    * Create a new empty xml document.
    */
   public XMLDocument() {
      this.indentlevel = 0;
      indent = "";
      buffer = new StringBuffer();
   }
   
   /**
    * Create a new empty xml document.
    * <p>
    * New content will be placed by the given indent position. 0 means no
    * indent.
    * 
    * @param indentlevel
    *              the start level of the indent
    */
   public XMLDocument(int indentlevel) {
     setIndentLevel( indentlevel );
     buffer = new StringBuffer();
   }

   /**
    * Clear current xml document and set indentlevel to 0.
    */
   public void clear() {
      buffer = new StringBuffer();
      setIndentLevel( 0 );
   }
   
   /**
    * Sets a new level of the indent.
    * @param il 
    *              the new level of the indent
    */
   public void setIndentLevel(int indentlevel) {
     if (indentlevel < 0) {
       throw new IllegalArgumentException("indentLevel must be 0 or greater.");
     }
     this.indentlevel = indentlevel;
     indent = "";
     for (int i = 0; i < indentlevel; ++i) {
       indent += "  ";
     }
   }

   /**
    * Add xml declaration.
    * 
    * @param version
    *              xml version
    * @param encoding
    *              used encoding
    */
   public void addDeclaration(String version, String encoding) {
      buffer.append(indent);
      buffer.append("<?xml");
      buffer.append(" version=\"").append(version).append("\"");
      buffer.append(" encoding=\"").append(encoding).append("\"");
      buffer.append("?>\n");
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
    *              value of the element
    */
   public void addElement(String tag, Object value) {
      buffer.append(indent);
      if (value == null) {
        buffer.append("<").append(namespace).append(tag).append(" />\n");
      }
      else {
        buffer.append("<").append(namespace).append(tag).append('>');
        buffer.append( XMLUtils.encodeString( value.toString() ) );
        buffer.append("</").append(namespace).append(tag).append(">\n");
      }
   }
   
   /**
    * Add a element of the given tag name, the given Attribute and with the given value.
    * <p>
    * Eg. <tag>value</tag> if value != null
    * <p>
    * or <tag/> if value == null
    * 
    * @param tag
    *              element name
    * @param attribute
    *              attribute
    * @param value
    *              value of the element
    */
   public void addElement(String tag, Attribute attribute, Object value) {
      buffer.append(indent);
      buffer.append("<").append(namespace).append(tag);
      buffer.append( " "+attribute);
      if (value == null) {
        buffer.append(" />\n");
      }
      else {
        buffer.append('>');
        buffer.append( XMLUtils.encodeString( value.toString() ) );
        buffer.append("</").append(namespace).append(tag).append(">\n");
      }
   }
   
   
   

   /**
    * Add a element of the given tag name and with the given value.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              value of the element
    */
   public void addElement(String tag, int value) {
      addElement( tag, ""+value );
   }

   /**
    * Add a element of the given tag name only if the given value is not null.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              value of the element
    */
   public void addOptionalElement(String tag, Object value) {
      if (value == null) {
         return;
      }
      addElement( tag, value );
   }

   /**
    * Add a element of the given tag name only if the given value is not
    * Integer.MIN_VALUE.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              value of the element
    */
   public void addOptionalElement(String tag, int value) {
      if (value == Integer.MIN_VALUE) {
         return;
       }
       addElement( tag, ""+value );
   }

   /**
    * Add a element of the given tag name only if the given value is not
    * Integer.MIN_VALUE.
    * <p>
    * Eg. <tag>value</tag>
    * 
    * @param tag
    *              element name
    * @param value
    *              value of the element
    */
   public void addOptionalElement(String tag, long value) {
      if (value == Integer.MIN_VALUE) {
         return;
       }
       addElement( tag, ""+value );
   }

   /**
    * Add a element of the given tag name with a single attribute which has the
    * specified value.
    * <p>
    * Eg. <tag attribute='value'/>
    * 
    * @param tag
    *              element name
    * @param attributeName
    *              name of the attribute
    * @param attributeValue
    *              value of the attribute
    * @param value
    *              value of the element
    */
   public void addElement(String tag, String attributeName, Object attributeValue, Object value) {
     addElement( tag, new Attribute(attributeName,attributeValue), value );
   }

   /**
    * Add a element of the given tag name with the given attributes and the
    * specified values.
    * <p>
    * Eg. <tag attribute1='value1' attribute2='value2'/>
    * 
    * @param tag
    *              element name
    * @param attributes
    *              names of the attributes
    * @param values
    *              values of the attributes
    * @deprecated keine richten Tag-Daten möglich
    */
   public void addElement(String tag, String[] attributes, String[] values) {
      buffer.append(indent);
      buffer.append("<").append(namespace).append(tag);
      for (int i = 0; i < attributes.length; i++) {
         buffer.append(' ');
         if (values[i] == null) {
            values[i] = "";
         }
         buffer.append(attributes[i]).append("='").append(values[i])
               .append("'");
      }
      buffer.append(" />\n");
   }
   
   /**
    * Add a element of the given tag name with the given attributes and the
    * specified values.
    * <p>
    * Eg. <tag attribute1='value1' attribute2='value2'/>
    * 
    * @param tag
    *              element name
    * @param attributes
    *              attributes
    * @param value
    *              value of the element
    */
   public void addElement(String tag, Attribute[] attributes, Object value ) {
      buffer.append(indent);
      buffer.append("<").append(namespace).append(tag);
      for (int i = 0; i < attributes.length; i++) {
         if (attributes[i] != null) {
           buffer.append(' ').append( attributes[i] );
         }
      }
      if (value == null) {
        buffer.append(" />\n");
      }
      else {
        buffer.append('>');
        buffer.append( XMLUtils.encodeString( value.toString() ) );
        buffer.append("</").append(namespace).append(tag).append(">\n");
      }
   }

   
   /**
    * Add a element of the given tag name and with the given value.
    * The value will be set in an CDATA-block.
    * @param tag
    *              element name
    * @param value
    *              value of the element
    */
   public void addElementCDATA(String tag, Object value) {
     buffer.append(indent);
     if (value == null) {
       buffer.append("<").append(namespace).append(tag).append(" />\n");
     }
     else {
       buffer.append("<").append(namespace).append(tag).append('>');
       buffer.append("<![CDATA[").append( value.toString() ).append("]]>");
       buffer.append("</").append(namespace).append(tag).append(">\n");
     }
   }
 
   
   /**
    * Add a start tag. Also increase the indent.
    * <p>
    * Eg. <tag>
    * 
    * @param tag
    *              element name
    */
   public void addStartTag(String tag) {
      buffer.append(indent).append("<").append(namespace).append(tag).append(">\n");
      increaseIndent();
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
   public void addStartTag(String tag, String attributes) {
      buffer.append(indent).append("<").append(namespace).append(tag);
      buffer.append(' ').append(attributes);
      buffer.append(">\n");
      increaseIndent();
   }

   /**
    * Add a end tag. Also decrease indent.
    * <p>
    * Eg. <tag/>
    * 
    * @param tag
    *              element name
    */
   public void addEndTag(String tag) {
      decreaseIndent();
      buffer.append(indent).append("</").append(namespace).append(tag).append(">\n");
   }

   /**
    * Append sub document.
    * 
    * @param xmlDoc
    *              xml document
    */
   public void append(XMLDocument xmlDoc) {
      if (xmlDoc != null) {
         buffer.append(xmlDoc.toString());
      }
   }

   /**
    * Append sub document.
    * 
    * @param string
    *              xml string
    */
   public void append(String string) {
      if (string != null) {
         buffer.append(string);
      }
   }

   /**
    * Increase indent
    */
   private void increaseIndent() {
      ++indentlevel;
      indent += DEFAULT_INDENT;
   }

   /**
    * Decrease indent
    */
   private void decreaseIndent() {
      ++indentlevel;
      indent = indent.substring(DEFAULT_INDENT.length());
   }


   /**
    * Write the cached xml document with the given writer.
    * 
    * @param writer
    *              writer to write cached xml document
    * @throws IOException
    */
   public void write(Writer writer) throws IOException {
      writer.write(buffer.toString());
      buffer.setLength(0);
   }

   /**
    * @see java.lang.Object#toString()
    */
   public String toString() {
      return buffer.toString();
   }

   /**
    * @return Namespace
    */
   public String getNamespace() {
      if (namespace.length() == 0) {
         return namespace;
      }
      return namespace.substring(namespace.length() - 1);
   }

   /**
    * @param ns
    *              Sets the Namespace
    */
   public void setNamespace(String ns) {
      if (ns == null || ns.length() == 0) {
         namespace = "";
      } else {
         namespace = ns + ":";
      }
   }

}