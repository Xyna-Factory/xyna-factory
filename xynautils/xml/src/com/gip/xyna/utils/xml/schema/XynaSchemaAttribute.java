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
package com.gip.xyna.utils.xml.schema;

import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;

/**
 * Represents a attribute node in xml schema.
 */
public class XynaSchemaAttribute extends XynaSchemaNode {

   private String type;
   private boolean required = false;

   protected XynaSchemaAttribute(XynaSchema root) {
      super(root);
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getType() {
      return type;
   }

   private XMLElement getNewElement(XMLDocument xmldoc) {
      XMLElement el = (XMLElement) xmldoc.createElementNS(XynaSchema.NS_XSD,
            XynaSchema.ATTRIBUTE);
      el.setAttribute(XynaSchema.ATT_NAME, getName());
      if (required) {
        el.setAttribute(XynaSchema.ATT_USE, "required");
      }
      if (getType() != null && getChildren().size() == 0) {
         el.setAttribute(XynaSchema.ATT_TYPE, getType());
      }      
      return el;
   }

   protected void appendXML(XMLDocument xmldoc, XynaSchemaNode parent,
         Node parentNode) throws Exception {
      XMLElement newElement = getNewElement(xmldoc);
      if (parent instanceof XynaSchema) {
         throw new Exception("Schema kann nicht direkt Attribute haben."); // TODO
                                                                           // XynaException
      } else if (parent instanceof XynaSchemaElement) {

         XynaSchemaElement parentElement = (XynaSchemaElement) parent;
         if (parentElement.getType() != null) {
            // extension
            String content = "";
            if (parentElement.getType().split(":")[0].equals("xsd") || 0 == 1) { // TODO
                                                                                 // 0==1
                                                                                 // mit
                                                                                 // einer
                                                                                 // prüfung
                                                                                 // ersetzen,
                                                                                 // ob
                                                                                 // type
                                                                                 // ein
                                                                                 // eigener
                                                                                 // simpletype
                                                                                 // ist.
               // simpletype extension:
               // <xsd:element name="element1">
               // <xsd:complexType>
               // <xsd:simpleContent>
               // <xsd:extension base="xsd:string">
               // <xsd:attribute name="attribute1" type="xsd:int"/>
               // <xsd:attribute name="attribute1"/>
               // </xsd:extension>
               // </xsd:simpleContent>
               // </xsd:complexType>
               // </xsd:element>
               content = XynaSchema.SIMPLECONTENT;

               // erstmal immer complexcontent
               // content = XynaSchema.COMPLEXCONTENT;
            } else {
               // <xsd:element name="element4">
               // <xsd:complexType>
               // <xsd:complexContent>
               // <xsd:extension base="complexType1">
               // <xsd:attribute name="attribute2"/>
               // </xsd:extension>
               // </xsd:complexContent>
               // </xsd:complexType>
               // </xsd:element>
               content = XynaSchema.COMPLEXCONTENT;
            }
            // prüfen, ob extension bereits vorhanden
            XMLElement extension;
            if (parentNode.getChildNodes().getLength() > 0) {
               // dann muss extension vorhanden sein!
               extension = (XMLElement) ((XMLElement) parentNode)
                     .getElementsByTagName(XynaSchema.EXTENSION_NONS).item(0);
            } else {
               // extension einbauen
               XMLElement complexType = (XMLElement) xmldoc.createElementNS(
                     XynaSchema.NS_XSD, XynaSchema.COMPLEXTYPE);
               parentNode.appendChild(complexType);
               XMLElement simpleContent = (XMLElement) xmldoc.createElementNS(
                     XynaSchema.NS_XSD, content);
               complexType.appendChild(simpleContent);
               extension = (XMLElement) xmldoc.createElementNS(
                     XynaSchema.NS_XSD, XynaSchema.EXTENSION);

               // parenttype aus anderem namespace?
               String rsn = parentElement.getRefNamespace();
               if (rsn != null) {
                  String shortNS = parentElement.getRoot().getShortNS(rsn);
                  if (shortNS != null) {
                     extension.setAttribute(XynaSchema.ATT_BASE, shortNS + ":"
                           + parentElement.getType());
                  } else {
                     extension.setAttribute(XynaSchema.ATT_BASE, "ns0:"
                           + parentElement.getType());
                     extension.setAttribute(XynaSchema.XMLNS + ":ns0", rsn);
                  }
               } else {
                  extension.setAttribute(XynaSchema.ATT_BASE, parentElement
                        .getType());
               }

               simpleContent.appendChild(extension);
            }
            extension.appendChild(newElement);            
         } else if (parentElement.getRef() != null) {
            throw new Exception("Eine Referenz kann keine Kinder haben."); // TODO
                                                                           // XynaException
         } else {

            // <xsd:element name="element2">
            // <xsd:complexType>
            // <xsd:attribute name="attribute1"/>
            // </xsd:complexType>
            // </xsd:element>

            // gibt es complextype schon?
            XMLElement complextype;
            if (parentNode.getChildNodes().getLength() > 0) {
               complextype = (XMLElement) ((XMLElement) parentNode)
                     .getElementsByTagName(XynaSchema.COMPLEXTYPE_NONS).item(0);
            } else {
               complextype = (XMLElement) xmldoc.createElementNS(
                     XynaSchema.NS_XSD, XynaSchema.COMPLEXTYPE);
               parentNode.appendChild(complextype);
            }
            complextype.appendChild(newElement);
         }

      } else if (parent instanceof XynaSchemaAttribute) {
         throw new Exception("Attribut kann keine Attribute als Kind haben."); // TODO
                                                                                 // XynaException;
      } else if (parent instanceof XynaSchemaComplexType) {
         parentNode.appendChild(newElement);
      }
      // rekursion
      for (int i = 0; i < getChildren().size(); i++) {
         ((XynaSchemaNode) getChildren().get(i)).appendXML(xmldoc, this, newElement);
      }

   }

  /**
   * standard ist optional!
   * @param required
   */
  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isRequired() {
    return required;
  }
}
