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

import java.util.Vector;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;

/**
 * Represents a node in a xml schema.
 */
public abstract class XynaSchemaNode {
   public static final String XSD_STRING = "xsd:string";
   public static final String XSD_INTEGER = "xsd:integer";
   public static final String XSD_LONG = "xsd:long";
   public static final String XSD_DOUBLE = "xsd:double";

   private String name;
   private Vector<XynaSchemaNode> children = new Vector<XynaSchemaNode>();
   private XynaSchema root;

   protected XynaSchemaNode(XynaSchema root) {
      this.root = root;
   }

   public XynaSchema getRoot() {
      return root;
   }

   /**
    * setzt rekursiv den root neu (inkl aller kinder etc)
    * 
    * @param root
    */
   protected void setRoot(XynaSchema root) {
      this.root = root;
      for (int i = 0; i < getChildren().size(); i++) {
         ((XynaSchemaNode) getChildren().get(i)).setRoot(root);
      }
   }

   public String getNamespace() throws Exception {
      return root.getNamespace();
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setChildren(Vector<XynaSchemaNode> children) {
      this.children = children;
   }

   public Vector<XynaSchemaNode> getChildren() {
      return children;
   }

   public static String getXSDTypeOfClassname(String classname) {
      if (classname.equals(String.class.getName())) {
         return XSD_STRING;
      } else if (classname.equals(Integer.class.getName())) {
         return XSD_INTEGER;
      } else if (classname.equals(Long.class.getName())) {
         return XSD_LONG;
      } else if (classname.equals(Double.class.getName())) {
         return XSD_DOUBLE;
      }
      return classname;
   }

   protected abstract void appendXML(XMLDocument xmldoc, XynaSchemaNode parent,
         Node parentNode) throws Exception;

   public XynaSchemaFragment getAsFragment() throws Exception {
      XynaSchemaFragment frag = new XynaSchemaFragment(this);
      return frag;
   }

}
