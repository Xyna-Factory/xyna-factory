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
package com.gip.xyna.utils.xynatree;

import com.gip.xyna.utils.xml.schema.XynaSchema;
import com.gip.xyna.utils.xml.schema.XynaSchemaElement;
import com.gip.xyna.utils.xml.schema.XynaSchemaNode;

import java.util.Hashtable;
import java.util.Iterator;

public class XynaNodeUtils {

   private static final String STANDARD_XSD_TYPE = XynaSchemaNode.XSD_STRING;

   public XynaNodeUtils() {
   }

   /**
    * siehe buildSchema(XynaNode, String, boolean), mit boolean = false.
    * 
    * @param tree
    * @param namespace
    * @return
    * @throws Exception
    */
   public static XynaSchema buildXynaSchema(XynaNode tree, String namespace)
         throws Exception {
      return buildXynaSchema(tree, namespace, false);
   }

   /**
    * Erzeugt ein XynaSchema aus dem Baum tree. Die Typen der XSD-Elemente
    * werden aus dem Typ des Wertes eines Knotens ermittelt. Falls der Wert
    * eines Knotens ein XMLObject ist, wird XMLObject.getType() benutzt. Falls
    * nicht, wird versucht, den Typ möglichst genau zu ermitteln. Falls der Wert
    * null ist, wird der Typ auf den STANDARD_XSD_TYPE gesetzt.
    * 
    * @param tree
    * @param namespace
    *              gewünschter namespace
    * @param minmaxOccursUnbounded
    *              true => setzt für jedes Element minOcc = 0 und maxOcc =
    *              unbounded.
    * @return
    * @throws Exception
    */
   public static XynaSchema buildXynaSchema(XynaNode tree, String namespace,
         boolean minmaxOccursUnbounded) throws Exception {
      XynaSchema schema = new XynaSchema(namespace);
      addTreeRecursive(schema, tree, minmaxOccursUnbounded);
      return schema;
   }

   /**
    * siehe buildXynaSchema. Das dort erzeugte Schema wird in einen String
    * serialisiert.
    * 
    * @param tree
    * @param namespace
    * @return
    * @throws Exception
    */
   public static String buildXSD(XynaNode tree, String namespace)
         throws Exception {
      return buildXynaSchema(tree, namespace, false).generateXSD();
   }

   private static void addTreeRecursive(XynaSchemaNode schemaNode,
         XynaNode node, boolean minmaxOccursUnbounded) throws Exception {
      XynaSchemaElement el = null;
      if (schemaNode instanceof XynaSchema) {
         el = ((XynaSchema) schemaNode).addElement(node.getName());
      } else if (schemaNode instanceof XynaSchemaElement) {
         el = ((XynaSchemaElement) schemaNode).addElement(node.getName());
      }
      if (minmaxOccursUnbounded) {
         el.setMaxOccurs(XynaSchemaElement.MAX_UNBOUNDED);
         el.setMinOccurs(0);
      }
      if (node.getChildren().length == 0) {
         if (node.getValue() != null) {
            // Typ ermitteln:
            if (node.getValue() instanceof XMLObject) {
               el.setType(((XMLObject) node.getValue()).getType());
            } else {
               String classname = node.getValue().getClass().getName();
               el.setType(XynaSchemaNode.getXSDTypeOfClassname(classname));
               // TODO ggfs hier erweitern für beliebige objekte mittels
               // reflection?!
               // und importierten Types/References?
            }
         } else {
            el.setType(STANDARD_XSD_TYPE);
         }
      }
      Hashtable<String, String> attributes = node.getAttributes();
      for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
         String name = it.next();
         el.addAttribute(name, STANDARD_XSD_TYPE);
      }
      // rekursion
      XynaNode[] children = node.getChildren();
      for (int i = 0; i < children.length; i++) {
         addTreeRecursive(el, children[i], minmaxOccursUnbounded);
      }
   }

}
