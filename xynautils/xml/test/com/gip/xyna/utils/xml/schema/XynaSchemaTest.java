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
package com.gip.xyna.utils.xml.schema;

import junit.framework.TestCase;

public class XynaSchemaTest extends TestCase {

   public void testNewXSD() {
      try {
         // TODO: attribute stehen in einer hashmap, dadurhc ist reihenfolge
         // nicht eindeutig, dadurch kann dieser test schiefgehen?
         // usecase 1: neues xsd von scratch
         XynaSchema xsd = new XynaSchema("myNamespace");
         XynaSchemaElement el = xsd.addElement("element1",
         XynaSchemaNode.XSD_STRING);
         el.addAttribute("attribute1", XynaSchemaNode.XSD_INTEGER);
         XynaSchemaComplexType ct = xsd.addComplexType("complexType1");
         XynaSchemaElement ct1 = ct.addElement("elementct1",
               XynaSchemaNode.XSD_STRING);
         ct1.setMaxOccurs(XynaSchemaElement.MAX_UNBOUNDED);
         String xsdString = xsd.generateXSD();
         System.out.println(xsdString);
         String x = "<xsd:schema targetNamespace=\"myNamespace\" xmlns=\"myNamespace\" elementFormDefault=\"qualified\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
               + "   <xsd:element name=\"element1\">\n"
               + "      <xsd:complexType>\n"
               + "         <xsd:simpleContent>\n"
               + "            <xsd:extension base=\"xsd:string\">\n"
               + "               <xsd:attribute name=\"attribute1\" type=\"xsd:integer\"/>\n"
               + "            </xsd:extension>\n"
               + "         </xsd:simpleContent>\n"
               + "      </xsd:complexType>\n"
               + "   </xsd:element>\n"
               + "   <xsd:complexType name=\"complexType1\">\n"
               + "      <xsd:sequence>\n"
               + "         <xsd:element name=\"elementct1\" type=\"xsd:string\" maxOccurs=\"unbounded\"/>\n"
               + "      </xsd:sequence>\n"
               + "   </xsd:complexType>\n"
               + "</xsd:schema>";
         assertEquals("xsdString gleich?", xsdString.replaceAll("\\n", ""), x
               .replaceAll("\\n", ""));
         // erwartet:
         // <xsd:schema targetNamespace="myNamespace" xmlns="myNamespace"
         // elementFormDefault="qualified"
         // xmlns:xsd="http://www.w3.org/2001/XMLSchema">
         // <xsd:element name="element1">
         // <xsd:complexType>
         // <xsd:simpleContent>
         // <xsd:extension base="xsd:string">
         // <xsd:attribute name="attribute1" type="xsd:integer"/>
         // </xsd:extension>
         // </xsd:simpleContent>
         // </xsd:complexType>
         // </xsd:element>
         // <xsd:complexType name="complexType1">
         // <xsd:sequence>
         // <xsd:element name="elementct1" type="xsd:string"
         // maxOccurs="unbounded"/>
         // </xsd:sequence>
         // </xsd:complexType>
         // </xsd:schema>
      } catch (Exception e) {
         // e.printStackTrace();
         fail("Fehler aufgetreten in testNewXSD");
      }
   }

   public void testNewXSDWithImport() {
      try {
         String ns = "myNamespace";
         // usecase 2: neues xsd mit import
         XynaSchema xsd = new XynaSchema("myNamespace2");
         xsd.addImport(ns);
         XynaSchemaElement el2 = xsd.addElement("element2",
               XynaSchemaNode.XSD_STRING);
         el2.addReferencedElement(ns, "element1");
         XynaSchemaElement el4 = el2.addElement("element3", ns, "complexType1");
         el4.addElement("element4", ns, "element1");
         el4.setMinOccurs("122");
         el4.setMaxOccurs(123);
         XynaSchemaComplexType ct2 = xsd.addComplexType("ct2");
         ct2.addAttribute("att1", XynaSchemaNode.XSD_STRING);
         XynaSchemaElement el5 = ct2.addElement("baum");
         el5.setMinOccurs(2);
         el5.addAttribute("att3", XynaSchemaNode.XSD_STRING);
         ct2.addAttribute("att2", XynaSchemaNode.XSD_STRING);
         XynaSchemaElement el6 = ct2.addElement("baum2");
         el6.setMinOccurs(0);
         el6.setMaxOccurs(1);
         XynaSchemaAttribute at = el6.addAttribute("att5", XynaSchemaNode.XSD_STRING);
         at.setRequired(true);
         el6.addElement("kind", el6.getNamespace(), ct2.getName());
         el6.addAttribute("att4", XynaSchemaNode.XSD_STRING);
         el6.addElement("kind2");
         el6.addElement("kind"); // gleichbedeutend mit maxOcc hochsetzen.
         el5.setType("myNamespace3", "hellau"); // jetzt wird der typ von el5
                                                // geändert
         ct2.addElement("baum", "bla", "blubb"); // gleichbedeutend mit maxOcc
                                                   // hochsetzen. namespace und
                                                   // typ werden verworfen.
         String xsdString = xsd.generateXSD();
         System.out.println(xsdString);
         String x = "<xsd:schema targetNamespace=\"myNamespace2\" xmlns=\"myNamespace2\" elementFormDefault=\"qualified\" xmlns:myNa=\"myNamespace\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
               + "   <xsd:import schemaLocation=\"locationOfmyNamespace\" namespace=\"myNamespace\"/>\n"
               + "   <xsd:element name=\"element2\">\n"
               + "      <xsd:complexType>\n"
               + "         <xsd:complexContent>\n"
               + "            <xsd:extension base=\"xsd:string\">\n"
               + "               <xsd:sequence>\n"
               + "                  <xsd:element ref=\"myNa:element1\"/>\n"
               + "                  <xsd:element name=\"element3\" minOccurs=\"122\" maxOccurs=\"123\">\n"
               + "                     <xsd:complexType>\n"
               + "                        <xsd:complexContent>\n"
               + "                           <xsd:extension base=\"myNa:complexType1\">\n"
               + "                              <xsd:sequence>\n"
               + "                                 <xsd:element name=\"element4\" type=\"myNa:element1\"/>\n"
               + "                              </xsd:sequence>\n"
               + "                           </xsd:extension>\n"
               + "                        </xsd:complexContent>\n"
               + "                     </xsd:complexType>\n"
               + "                  </xsd:element>\n"
               + "               </xsd:sequence>\n"
               + "            </xsd:extension>\n"
               + "         </xsd:complexContent>\n"
               + "      </xsd:complexType>\n"
               + "   </xsd:element>\n"
               + "   <xsd:complexType name=\"ct2\">\n"
               + "      <xsd:sequence>\n"
               + "         <xsd:element name=\"baum\" minOccurs=\"2\" maxOccurs=\"3\">\n"
               + "            <xsd:complexType>\n"
               + "               <xsd:complexContent>\n"
               + "                  <xsd:extension base=\"ns0:hellau\" xmlns:ns0=\"myNamespace3\">\n"
               + "                     <xsd:attribute name=\"att3\" type=\"xsd:string\"/>\n"
               + "                  </xsd:extension>\n"
               + "               </xsd:complexContent>\n"
               + "            </xsd:complexType>\n"
               + "         </xsd:element>\n"
               + "         <xsd:element name=\"baum2\" minOccurs=\"0\">\n"
               + "            <xsd:complexType>\n"
               + "               <xsd:sequence>\n"
               + "                  <xsd:element name=\"kind\" type=\"ct2\" maxOccurs=\"2\"/>\n"
               + "                  <xsd:element name=\"kind2\"/>\n"
               + "               </xsd:sequence>\n"
               + "               <xsd:attribute name=\"att5\" use=\"required\" type=\"xsd:string\"/>\n"
               + "               <xsd:attribute name=\"att4\" type=\"xsd:string\"/>\n"
               + "            </xsd:complexType>\n"
               + "         </xsd:element>\n"
               + "      </xsd:sequence>\n"
               + "      <xsd:attribute name=\"att1\" type=\"xsd:string\"/>\n"
               + "      <xsd:attribute name=\"att2\" type=\"xsd:string\"/>\n"
               + "   </xsd:complexType>\n" + "</xsd:schema>";
         assertEquals("xsdString gleich?", xsdString.replaceAll("\\n", ""), x
               .replaceAll("\\n", ""));
         // erwartet:
         // <xsd:schema targetNamespace="myNamespace2" xmlns="myNamespace2"
         // elementFormDefault="qualified" xmlns:myNa="myNamespace"
         // xmlns:xsd="http://www.w3.org/2001/XMLSchema">
         // <xsd:import schemaLocation="locationOfmyNamespace"
         // namespace="myNamespace"/>
         // <xsd:element name="element2">
         // <xsd:complexType>
         // <xsd:complexContent>
         // <xsd:extension base="xsd:string">
         // <xsd:sequence>
         // <xsd:element ref="myNa:element1"/>
         // <xsd:element name="element3" minOccurs="122" maxOccurs="123">
         // <xsd:complexType>
         // <xsd:complexContent>
         // <xsd:extension base="myNa:complexType1">
         // <xsd:sequence>
         // <xsd:element name="element4" type="myNa:element1"/>
         // </xsd:sequence>
         // </xsd:extension>
         // </xsd:complexContent>
         // </xsd:complexType>
         // </xsd:element>
         // </xsd:sequence>
         // </xsd:extension>
         // </xsd:complexContent>
         // </xsd:complexType>
         // </xsd:element>
         // <xsd:complexType name="ct2">
         // <xsd:sequence>
         // <xsd:element name="baum" minOccurs="2" maxOccurs="3">
         // <xsd:complexType>
         // <xsd:complexContent>
         // <xsd:extension base="ns0:hellau" xmlns:ns0="myNamespace3">
         // <xsd:attribute name="att3" type="xsd:string"/>
         // </xsd:extension>
         // </xsd:complexContent>
         // </xsd:complexType>
         // </xsd:element>
         // <xsd:element name="baum2" minOccurs="0">
         // <xsd:complexType>
         // <xsd:sequence>
         // <xsd:element name="kind" type="ct2" maxOccurs="2"/>
         // <xsd:element name="kind2"/>
         // </xsd:sequence>
         // <xsd:attribute name="att5" type="xsd:string"/>
         // <xsd:attribute name="att4" type="xsd:string"/>
         // </xsd:complexType>
         // </xsd:element>
         // </xsd:sequence>
         // <xsd:attribute name="att1" type="xsd:string"/>
         // <xsd:attribute name="att2" type="xsd:string"/>
         // </xsd:complexType>
         // </xsd:schema>

      } catch (Exception e) {
         fail("Fehler aufgetreten in testNewXSDWithImport");
      }
   }

   public void testFragments() {
      try {
         XynaSchema xs = new XynaSchema("myNS");
         XynaSchemaComplexType ct1 = xs.addComplexType("ct1");
         ct1.addAttribute("att1", XynaSchema.XSD_LONG);
         XynaSchemaElement el = xs.addElement("e1");
         el.addElement("el2", XynaSchema.XSD_STRING);
         XynaSchema xs2 = new XynaSchema("myNS2");
         xs2.importFragmentAsChild(ct1.getAsFragment());
         xs2.importFragmentAsChild(el.getAsFragment());
         String soll = "<xsd:schema targetNamespace=\"myNS2\" xmlns=\"myNS2\" elementFormDefault=\"qualified\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
               + "   <xsd:complexType name=\"ct1\">\n"
               + "      <xsd:attribute name=\"att1\" type=\"xsd:long\"/>\n"
               + "   </xsd:complexType>\n"
               + "   <xsd:element name=\"e1\">\n"
               + "      <xsd:complexType>\n"
               + "         <xsd:sequence>\n"
               + "            <xsd:element name=\"el2\" type=\"xsd:string\"/>\n"
               + "         </xsd:sequence>\n"
               + "      </xsd:complexType>\n"
               + "   </xsd:element>\n" + "</xsd:schema>";
         assertEquals("xsdString gleich?", xs2.generateXSD().replaceAll("\\n",
               ""), soll.replaceAll("\\n", ""));

      } catch (Exception e) {
         fail("Fehler aufgetreten in testFragments");
      }
   }

}
