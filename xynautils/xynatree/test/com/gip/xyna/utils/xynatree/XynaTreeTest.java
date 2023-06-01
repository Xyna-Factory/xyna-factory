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

import com.gip.xyna.utils.xml.parser.StringCreator;

import junit.framework.TestCase;

public class XynaTreeTest extends TestCase {

   public void testMain() {
      try {
         XynaNode root = new XynaNode("root");
         root.setValue("rootVal");
         root.setAttribute("rootAtt", "rootAttVal");
         XynaNode k1 = new XynaNode("k1");
         k1.setValue("k1Val");
         root.appendChild(k1);
         XynaNode k3 = new XynaNode("k3");
         k3.setValue("k3Val");
         root.appendChild(k3);
         root.appendChild(k3);
         XynaNode k2 = new XynaNode("k2");
         k2.setAttribute("testatt", "123");
         k2.setValue(k3);
         root.insertChild(k2, k3);
         XynaNode k4 = new XynaNode("k4");
         k4.setValue(new Integer(235));
         k3.appendChild(k4);
         String xsd = XynaNodeUtils.buildXSD(root, "myNamespace");
         System.out.println(xsd);
         String xsd1 = "<xsd:schema targetNamespace=\"myNamespace\" xmlns=\"myNamespace\" elementFormDefault=\"qualified\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
               + "   <xsd:element name=\"root\">\n"
               + "      <xsd:complexType>\n"
               + "         <xsd:sequence>\n"
               + "            <xsd:element name=\"k1\" type=\"xsd:string\"/>\n"
               + "            <xsd:element name=\"k2\">\n"
               + "               <xsd:complexType>\n"
               + "                  <xsd:simpleContent>\n"
               + "                     <xsd:extension base=\"xsd:string\">\n"
               + "                        <xsd:attribute name=\"testatt\" type=\"xsd:string\"/>\n"
               + "                     </xsd:extension>\n"
               + "                  </xsd:simpleContent>\n"
               + "               </xsd:complexType>\n"
               + "            </xsd:element>\n"
               + "            <xsd:element name=\"k3\" maxOccurs=\"2\">\n"
               + "               <xsd:complexType>\n"
               + "                  <xsd:sequence>\n"
               + "                     <xsd:element name=\"k4\" type=\"xsd:integer\" maxOccurs=\"2\"/>\n"
               + "                  </xsd:sequence>\n"
               + "               </xsd:complexType>\n"
               + "            </xsd:element>\n"
               + "         </xsd:sequence>\n"
               + "         <xsd:attribute name=\"rootAtt\" type=\"xsd:string\"/>\n"
               + "      </xsd:complexType>\n"
               + "   </xsd:element>\n"
               + "</xsd:schema>";
         assertEquals("generiertes XSD", xsd1.replaceAll("\\n", ""), xsd
               .replaceAll("\\n", ""));
         String xml = StringCreator.generateXMLFromXSD(xsd, root.getName());
         // gehört nicht zum testcase. XML kann nicht generiert werden => ok,
         // weil basetype nicht bekannt.
         System.out
               .println("----------------------------------------------------------");
         System.out.println(xml);
         System.out
               .println("----------------------------------------------------------");
         String jsonSimple = root.toJSON();
         System.out.println(jsonSimple);
         String jsonSimple1 = "{ root : rootVal, \n"
               + "rootAtt : rootAttVal, \n" + "children : [ \n"
               + "{ k1 : k1Val\n" + "}, \n" + "{ k2 : { k3 : k3Val, \n"
               + "children : [ \n" + "{ k4 : 235\n" + "}\n" + "]\n" + "}, \n"
               + "testatt : 123\n" + "}, \n" + "{ k3 : k3Val, \n"
               + "children : [ \n" + "{ k4 : 235\n" + "}\n" + "]\n" + "}, \n"
               + "{ k3 : k3Val, \n" + "children : [ \n" + "{ k4 : 235\n"
               + "}\n" + "]\n" + "}\n" + "]\n" + "}";
         assertEquals("generiertes JSON (simple)", jsonSimple1.replaceAll(
               "\\n", ""), jsonSimple.replaceAll("\\n", ""));
         System.out
               .println("----------------------------------------------------------");
         String jsonExtended = root.toJSONExtended();
         System.out.println(jsonExtended);
         String jsonExtended1 = "{ name : root, \n" + "value : rootVal, \n"
               + " attributes : { rootAtt : rootAttVal\n" + "}, \n"
               + "children : [ \n" + "{ name : k1, \n" + "value : k1Val, \n"
               + " attributes : { \n" + "}\n" + "}, \n" + "{ name : k2, \n"
               + "value : { k3 : k3Val, \n" + "children : [ \n"
               + "{ k4 : 235\n" + "}\n" + "]\n" + "}, \n"
               + " attributes : { testatt : 123\n" + "}\n" + "}, \n"
               + "{ name : k3, \n" + "value : k3Val, \n" + " attributes : { \n"
               + "}, \n" + "children : [ \n" + "{ name : k4, \n"
               + "value : 235, \n" + " attributes : { \n" + "}\n" + "}\n"
               + "]\n" + "}, \n" + "{ name : k3, \n" + "value : k3Val, \n"
               + " attributes : { \n" + "}, \n" + "children : [ \n"
               + "{ name : k4, \n" + "value : 235, \n" + " attributes : { \n"
               + "}\n" + "}\n" + "]\n" + "}\n" + "]\n" + "}";
         // assertEquals("generiertes JSON (extended)",
         // jsonExtended1.replaceAll("\\n", ""), jsonExtended.replaceAll("\\n",
         // ""));
         System.out.println(root.toXML());
      } catch (Exception e) {
         fail(e.getMessage());
      }
   }
}
