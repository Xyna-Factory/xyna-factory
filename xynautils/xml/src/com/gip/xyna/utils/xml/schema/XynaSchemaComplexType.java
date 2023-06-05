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
 * Represents a complex type in xml schema.
 */
public class XynaSchemaComplexType extends XynaSchemaNode {

   protected XynaSchemaComplexType(XynaSchema root) {
      super(root);
   }

   public XynaSchemaElement addElement(String name, String type)
         throws Exception {
      return addElement(name, null, type);
   }

   /**
    * entweder nur ordnungsgebendes Element oder type später setzen
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
    *              namespace des Types. keine Angabe ist äquivalent zum
    *              TargetNS.
    * @param typeName
    * @return
    * @throws Exception
    */
   public XynaSchemaElement addElement(String name, String namespace,
         String typeName) throws Exception {
      // FIXME doppelter code in XynaSchemaElement...
      for (int i = 0; i < getChildren().size(); i++) {
         if (getChildren().get(i) instanceof XynaSchemaElement) {
            XynaSchemaElement brother = (XynaSchemaElement) getChildren()
                  .get(i);
            if (brother.getName() != null && brother.getName().equals(name)) {
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

   public XynaSchemaAttribute addAttribute(String name, String type)
         throws Exception {
      XynaSchemaAttribute xsa = new XynaSchemaAttribute(getRoot());
      xsa.setName(name);
      xsa.setType(type);
      getChildren().add(xsa);
      return xsa;
   }

   public XynaSchemaElement addReferencedElement(String namespace, String name)
         throws Exception {
      XynaSchemaElement xe = new XynaSchemaElement(namespace, getRoot());
      xe.setRef(name);
      getChildren().add(xe);
      return xe;
   }

   protected void appendXML(XMLDocument xmldoc, XynaSchemaNode parent,
         Node parentNode) throws Exception {
      XMLElement newElement = (XMLElement) xmldoc.createElementNS(
            XynaSchema.NS_XSD, XynaSchema.COMPLEXTYPE);
      newElement.setAttribute(XynaSchema.ATT_NAME, getName());
      if (parent instanceof XynaSchema) {
         parentNode.appendChild(newElement);
      } else {
         throw new Exception(
               "ComplexTypes dürfen nur direkt im Schema hinzugefügt werden.");
      }
      // rekursion
      for (int i = 0; i < getChildren().size(); i++) {
         ((XynaSchemaNode) getChildren().get(i)).appendXML(xmldoc, this, newElement);
      }

   }

   public void importFragmentAsChild(XynaSchemaFragment frag) throws Exception {
      if (frag.getNode() instanceof XynaSchema) {
         // Kind-Elemente anhängen ?
         throw new Exception("nicht unterstützt."); // TODO
      } else if (frag.getNode() instanceof XynaSchemaAttribute
            || frag.getNode() instanceof XynaSchemaElement) {
         frag.getNode().setRoot(getRoot());
         getChildren().add(frag.getNode());
      } else if (frag.getNode() instanceof XynaSchemaComplexType) {
         // Kind-Elemente anhängen?! oder Exception werfen...
         throw new Exception("nicht unterstützt."); // TODO
      }
   }

}
