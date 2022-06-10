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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Collection of useful methods for handling namespaces.
 */
public class NamespaceUtils {

   private static final String NAMESPACE_MARKER = "xmlns";
   private static final String TARGET_NAMESPACE = "targetNamespace";

   /**
    * Get the namespace prefix for the given namespace uri.
    * 
    * @param namespaceURI
    *              a namespace uri
    * @return the namespace prefix for the given uri. Null if non could be
    *         found.
    * @see Node
    */
   public static String getPrefixForNS(Node node, String namespaceURI) {
      NamedNodeMap attributes = node.getAttributes();
      for (int i = 0; i < attributes.getLength(); i++) {
         Node attr = attributes.item(i);
         if (isNamespaceDeclaration(attr)) {
            System.out.println(attr.getLocalName());
            System.out.println(attr.getNodeName());
            System.out.println(attr.getNodeValue());
            if (attr.getNodeValue().equals(namespaceURI)) {

            }
         }
      }
      return null;
   }

   /**
    * Check if the given node is a namespace declaration.
    * 
    * @param node
    *              node to check
    * @return true if node is a namespace declaration, else false
    * @see Node
    */
   public static boolean isNamespaceDeclaration(Node node) {
      if (node.getLocalName().equals(NAMESPACE_MARKER))
         return true;
      if (node.getLocalName().equals(TARGET_NAMESPACE))
         return true;
      if ((node.getPrefix() != null)
            && node.getPrefix().equals(NAMESPACE_MARKER))
         return true;
      return false;
   }

}