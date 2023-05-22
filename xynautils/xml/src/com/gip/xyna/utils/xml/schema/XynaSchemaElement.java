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
package com.gip.xyna.utils.xml.schema;

import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;

public class XynaSchemaElement extends XynaSchemaNode {

   public static final String MAX_UNBOUNDED = "unbounded";

   private String type;
   private String ref;
   private String refNamespace;
   private String minOccurs = "1";
   private String maxOccurs = "1";

   /*
    * protected XynaSchemaElement(XynaSchema root) { super(root); }
    */

   protected XynaSchemaElement(String namespace, XynaSchema root) {
      super(root);
      refNamespace = namespace;
   }

   public XynaSchemaAttribute addAttribute(String name, String type)
         throws Exception {
      XynaSchemaAttribute xsa = new XynaSchemaAttribute(getRoot());
      xsa.setName(name);
      xsa.setType(type);
      getChildren().add(xsa);
      return xsa;
   }

   public XynaSchemaElement addElement(String name, String type)
         throws Exception {

      return addElement(name, null, type);
   }

   /**
    * entweder nur ordnungsgebendes Element oder type sp�ter setzen
    * 
    * @param name
    * @return
    * @throws Exception
    */
   public XynaSchemaElement addElement(String name) throws Exception {
      return addElement(name, null, null);
   }

   /**
    * 
    * @param name
    * @param namespace
    *              NS vom Type. keine Angabe ist �quivalent zum TargetNS.
    * @param typeName
    * @return
    * @throws Exception
    */
   public XynaSchemaElement addElement(String name, String namespace,
         String typeName) throws Exception {
      for (int i = 0; i < getChildren().size(); i++) {
         if (getChildren().get(i) instanceof XynaSchemaElement) {
            XynaSchemaElement brother = (XynaSchemaElement) getChildren()
                  .get(i);
            if (brother.getName() != null && brother.getName().equals(name)
                  && !brother.getMaxOccurs().equals(MAX_UNBOUNDED)) {
               brother
                     .setMaxOccurs(Integer.parseInt(brother.getMaxOccurs()) + 1);
               return brother;
            }
         }
      }
      XynaSchemaElement xe = new XynaSchemaElement(namespace, getRoot());
      xe.setName(name);
      xe.setType(typeName);
      getChildren().add(xe);
      return xe;
   }

   public XynaSchemaElement addReferencedElement(String namespace, String name)
         throws Exception {
      XynaSchemaElement xe = new XynaSchemaElement(namespace, getRoot());
      xe.setRef(name);
      getChildren().add(xe);
      return xe;
   }

   /**
    * Setzt den Typ des Elements im Targetnamespace (falls man
    * Simpletype-Konstanten von XynaSchema benutzt, ben�tigt man keine
    * gesonderte Namespace Angabe).
    * 
    * @param type
    */
   public void setType(String type) {
      this.type = type;
   }

   /**
    * Setzt den Typ des Elements im Gew�hlten Namespace. Falls man
    * Simpletype-Konstanten von XynaSchema benutzt, ben�tigt man keine
    * gesonderte Namespace Angabe.
    * 
    * @param namespace
    * @param type
    */
   public void setType(String namespace, String type) {
      refNamespace = namespace;
      this.type = type;
   }

   public String getType() {
      return type;
   }

   protected void setRef(String ref) {
      this.ref = ref;
   }

   protected String getRef() {
      return ref;
   }

   private XMLElement getNewElement(XMLDocument xmldoc) throws Exception {
      XMLElement el = (XMLElement) xmldoc.createElementNS(XynaSchema.NS_XSD,
            XynaSchema.ELEMENT);
      if (getType() != null && getChildren().size() == 0) {
         el.setAttribute(XynaSchema.ATT_NAME, getName());
         if (refNamespace != null) {
            String shortNS = getRoot().getShortNS(refNamespace);
            if (shortNS != null) {
               // type anh�ngen, falls shortNS = "" => kein doppelpunkt,
               // weil dann ists der targetNS.
               el.setAttribute(XynaSchema.ATT_TYPE,
                     (shortNS.length() > 0 ? shortNS + ":" : "") + getType());
            } else {
               el.setAttribute(XynaSchema.ATT_TYPE, "ns0:" + getType());
               el.setAttribute(XynaSchema.XMLNS + ":ns0", refNamespace);
            }
         } else {
            el.setAttribute(XynaSchema.ATT_TYPE, getType());
         }
      } else if (getRef() != null) {
         String shortNS = getRoot().getShortNS(refNamespace);
         if (shortNS != null) {
            el.setAttribute(XynaSchema.ATT_REFERENCE, shortNS + ":" + getRef());
         } else {
            el.setAttribute(XynaSchema.ATT_REFERENCE, "ns0:" + getRef());
            el.setAttribute(XynaSchema.XMLNS + ":ns0", refNamespace);
         }
      } else {
         el.setAttribute(XynaSchema.ATT_NAME, getName());
      }
      // minmax occurences
      // falls nicht nachrichten-element:
      if (!getRoot().getChildren().contains(this)) {
         if (Integer.parseInt(getMinOccurs()) != 1) {
            el.setAttribute(XynaSchema.ATT_MINOCC, getMinOccurs());
         }
         if (getMaxOccurs().equals(MAX_UNBOUNDED)
               || Integer.parseInt(getMaxOccurs()) != 1) {
            el.setAttribute(XynaSchema.ATT_MAXOCC, getMaxOccurs());
         }
      } else {
         if (Integer.parseInt(getMinOccurs()) != 1
               || getMaxOccurs() == MAX_UNBOUNDED
               || Integer.parseInt(getMaxOccurs()) != 1) {
            throw new Exception(
                  "Nachrichtenelemente k�nnen nur Min- und MaxOccurs=1 haben.");
         }
      }
      return el;
   }

   protected String getRefNamespace() {
      return refNamespace;
   }

   protected void appendXML(XMLDocument xmldoc, XynaSchemaNode parent,
         Node parentNode) throws Exception {
      XMLElement newElement = getNewElement(xmldoc);
      if (parent instanceof XynaSchema) {
         // <xsd:element name="element1">
         // </xsd:element>
         parentNode.appendChild(newElement);
      } else if (parent instanceof XynaSchemaAttribute) {
         throw new Exception("Ein Attribut kann keine Kinder haben."); // TODO
         // XynaException
      } else if (parent instanceof XynaSchemaElement) {
         XynaSchemaElement parentElement = (XynaSchemaElement) parent;
         if (parentElement.getType() != null) {
            // extension (geht nur wenn typ ein complextype ist)
            if (!parentElement.getType().split(":")[0].equals("xsd") && 0 == 1) { // TODO
                                                                                    // 0==1
                                                                                    // mit
                                                                                    // einer
                                                                                    // pr�fung
                                                                                    // ersetzen,
               // ob type ein eigener simpletype ist.
               throw new Exception(
                     "Ein SimpleType kann nicht mit einem Element zu einem ComplexType extended werden.");
            }
            // complextype extension:
            // <xsd:element name="element2">
            // <xsd:complexType>
            // <xsd:complexContent>
            // <xsd:extension base="complexType2">
            // <xsd:sequence/>
            // </xsd:extension>
            // </xsd:complexContent>
            // </xsd:complexType>
            // </xsd:element>

            // pr�fen, ob extension bereits vorhanden (2 f�lle: entweder mit
            // element extended oder mit attribut.
            // sequence ist nur vorhanden, falls mit element extended wurde.
            XMLElement sequence;
            if (((XMLElement) parentNode).getElementsByTagName(
                  XynaSchema.SEQUENCE_NONS).getLength() > 0) {
               // dann muss extension vorhanden sein
               sequence = (XMLElement) ((XMLElement) parentNode)
                     .getElementsByTagName(XynaSchema.SEQUENCE_NONS).item(0);
            } else {
               XMLElement extension;
               if (((XMLElement) parentNode).getElementsByTagName(
                     XynaSchema.EXTENSION_NONS).getLength() > 0) {
                  extension = (XMLElement) ((XMLElement) parentNode)
                        .getElementsByTagName(XynaSchema.EXTENSION_NONS)
                        .item(0);
               } else {
                  // extension einbauen
                  XMLElement complexType = (XMLElement) xmldoc.createElementNS(
                        XynaSchema.NS_XSD, XynaSchema.COMPLEXTYPE);
                  parentNode.appendChild(complexType);
                  XMLElement complexContent = (XMLElement) xmldoc
                        .createElementNS(XynaSchema.NS_XSD,
                              XynaSchema.COMPLEXCONTENT);
                  complexType.appendChild(complexContent);
                  extension = (XMLElement) xmldoc.createElementNS(
                        XynaSchema.NS_XSD, XynaSchema.EXTENSION);
                  complexContent.appendChild(extension);
                  // parenttype aus anderem namespace?
                  String rsn = parentElement.getRefNamespace();
                  if (rsn != null) {
                     String shortNS = parentElement.getRoot().getShortNS(rsn);
                     if (shortNS != null) {
                        extension.setAttribute(XynaSchema.ATT_BASE, shortNS
                              + ":" + parentElement.getType());
                     } else {
                        extension.setAttribute(XynaSchema.ATT_BASE, "ns0:"
                              + parentElement.getType());
                        extension.setAttribute(XynaSchema.XMLNS + ":ns0", rsn);
                     }
                  } else {
                     extension.setAttribute(XynaSchema.ATT_BASE, parentElement
                           .getType());
                  }
               }
               sequence = (XMLElement) xmldoc.createElementNS(
                     XynaSchema.NS_XSD, XynaSchema.SEQUENCE);
               // attribute d�rfen nicht vor der sequence stehen...
               if (extension.getChildNodes().getLength() > 0) {
                  extension.insertBefore(sequence, extension.getChildNodes()
                        .item(0));
               } else {
                  extension.appendChild(sequence);
               }
            }
            sequence.appendChild(newElement);
         } else if (parentElement.getRef() != null) {
            throw new Exception("Eine Referenz kann keine Kinder haben."); // TODO
            // XynaException
         } else {
            // sequence
            // <xsd:sequence>
            // <xsd:element name="element2" type="xsd:string"/>
            // </xsd:sequence>

            // m�glichkeiten: vorhanden complextype => attribute oder
            // complextype => sequence => ... oder
            // nichts
            XMLElement sequence;
            if (((XMLElement) parentNode).getElementsByTagName(
                  XynaSchema.SEQUENCE_NONS).getLength() > 0) {
               sequence = (XMLElement) ((XMLElement) parentNode)
                     .getElementsByTagName(XynaSchema.SEQUENCE_NONS).item(0);
            } else {
               XMLElement complexType;
               if (((XMLElement) parentNode).getElementsByTagName(
                     XynaSchema.COMPLEXTYPE_NONS).getLength() > 0) {
                  complexType = (XMLElement) ((XMLElement) parentNode)
                        .getElementsByTagName(XynaSchema.COMPLEXTYPE_NONS)
                        .item(0);
               } else {
                  complexType = (XMLElement) xmldoc.createElementNS(
                        XynaSchema.NS_XSD, XynaSchema.COMPLEXTYPE);
                  parentNode.appendChild(complexType);
               }

               sequence = (XMLElement) xmldoc.createElementNS(
                     XynaSchema.NS_XSD, XynaSchema.SEQUENCE);
               // attribute d�rfen nicht vor der sequence stehen...
               if (complexType.getChildNodes().getLength() > 0) {
                  complexType.insertBefore(sequence, complexType
                        .getChildNodes().item(0));
               } else {
                  complexType.appendChild(sequence);
               }
            }
            sequence.appendChild(newElement);
         }
      } else if (parent instanceof XynaSchemaComplexType) {
         // <xsd:complexType name="complexType1">
         // <xsd:sequence>
         // <xsd:element name="elementct1" type="xsd:string"/>
         // </xsd:sequence>
         // </xsd:complexType>
         XMLElement sequence;
         if (((XMLElement) parentNode).getElementsByTagName(
               XynaSchema.SEQUENCE_NONS).getLength() > 0) { // hier
            // k�nnen
            // auch
            // attribute
            // dranh�ngen
            sequence = (XMLElement) ((XMLElement) parentNode)
                  .getElementsByTagName(XynaSchema.SEQUENCE_NONS).item(0);
         } else {
            sequence = (XMLElement) xmldoc.createElementNS(XynaSchema.NS_XSD,
                  XynaSchema.SEQUENCE);
            // attribute d�rfen nicht vor der sequence stehen...
            if (parentNode.getChildNodes().getLength() > 0) {
               parentNode.insertBefore(sequence, parentNode.getChildNodes()
                     .item(0));
            } else {
               parentNode.appendChild(sequence);
            }
         }
         sequence.appendChild(newElement);
      }
      // rekursion
      for (int i = 0; i < getChildren().size(); i++) {
         ((XynaSchemaNode) getChildren().get(i)).appendXML(xmldoc, this, newElement);
      }
   }

   public void setMinOccurs(String minOccurs) {
      this.minOccurs = minOccurs;
      if (!maxOccurs.equals(MAX_UNBOUNDED)) {
         maxOccurs = ""
               + Math.max(Integer.parseInt(getMaxOccurs()), Integer
                     .parseInt(getMinOccurs()));
      }
   }

   public void setMinOccurs(int minOccurs) {
      this.minOccurs = "" + minOccurs;
      if (!maxOccurs.equals(MAX_UNBOUNDED)) {
         maxOccurs = ""
               + Math.max(Integer.parseInt(getMaxOccurs()), Integer
                     .parseInt(getMinOccurs()));
      }
   }

   public String getMinOccurs() {
      return minOccurs;
   }

   public void setMaxOccurs(String maxOccurs) {
      this.maxOccurs = maxOccurs;
      if (!this.maxOccurs.equals(MAX_UNBOUNDED)) {
         minOccurs = ""
               + Math.min(Integer.parseInt(getMaxOccurs()), Integer
                     .parseInt(getMinOccurs()));
      }
   }

   public void setMaxOccurs(int maxOccurs) {
      this.maxOccurs = "" + maxOccurs;
      if (!this.maxOccurs.equals(MAX_UNBOUNDED)) {
         minOccurs = ""
               + Math.min(Integer.parseInt(getMaxOccurs()), Integer
                     .parseInt(getMinOccurs()));
      }
   }

   public String getMaxOccurs() {
      return maxOccurs;
   }

   public void importFragmentAsChild(XynaSchemaFragment frag) throws Exception {
      if (frag.getNode() instanceof XynaSchema) {
         // Kind-Elemente anh�ngen ?
         throw new Exception("nicht unterst�tzt."); // TODO
      } else if (frag.getNode() instanceof XynaSchemaAttribute
            || frag.getNode() instanceof XynaSchemaElement) {
         frag.getNode().setRoot(getRoot());
         getChildren().add(frag.getNode());
      } else if (frag.getNode() instanceof XynaSchemaComplexType) {
         // Kind-Elemente anh�ngen?! oder Exception werfen...
         throw new Exception("nicht unterst�tzt."); // TODO
      }
   }

}
