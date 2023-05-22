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

import java.net.URL;

import oracle.xml.parser.schema.XMLSchema;
import oracle.xml.parser.schema.XSDElement;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.utils.xml.parser.OracleDOMCreator;
import com.gip.xyna.utils.xml.parser.StringCreator;
import com.gip.xyna.utils.xml.schema.SchemaUtils;

public class XMLUtilsExample {

   /**
    * @param args
    */
   public static void main(String[] args) {

      // Beispiel xml parsen:
      String xmlString = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><bla xmlns=\"myNamespace\" test=\"asd\"><blubb ja=\"nein\">wasch</blubb><hopp xmlns:ns50=\"yourNamespace\"><ns50:hipp>husch</ns50:hipp></hopp></bla>";
      try {
         XMLDocument xmlDoc = OracleDOMCreator.parseXMLString(xmlString);
         // ganzes geparstes XML:
         xmlDoc.print(System.out);
         // Namespace eines Elements...
         NodeList nl = xmlDoc.getElementsByTagName("*");
         XMLElement element = (XMLElement) nl.item(0);
         System.out.println("namespace: " + element.getNamespaceURI());
         System.out.println("qualified name: " + element.getExpandedName());
      } catch (Exception e) {
         e.printStackTrace();
      }

      // XSD Object
      String xsdString = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\" ?>"
            + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.example.org\" "
            + "targetNamespace=\"http://www.example.org\" elementFormDefault=\"qualified\">"
            + "<xsd:element name=\"bla\" type=\"xsd:string\"></xsd:element>"
            + "</xsd:schema>";
      try {
         URL relativeURL = new URL("file:///C:/blablabla"); // f�r imports und
                                                            // includes
         // xsd als xml parsen und damit schema bauen.
         XMLDocument xmlDoc = OracleDOMCreator.parseXMLString(xsdString);
         XMLSchema schema = SchemaUtils.buildSchema(xmlDoc, relativeURL);
         schema.printSchema();
         System.out.println();
         XSDElement xsdEl = schema
               .getElement(schema.getSchemaTargetNS(), "bla");
         System.out.println("Element: " + schema.getSchemaTargetNS() + ":"
               + xsdEl.getName());
         // schema �ndern
         String namespace = "http://www.w3.org/2001/XMLSchema";
         XMLElement xmlElement = (XMLElement) xmlDoc.createElementNS(namespace,
               "xsd:element");
         xmlElement.setAttribute("name", "blaKi");
         xmlElement.setAttribute("type", "xsd:string");
         // h�nge das element auf rootebene an
         xmlDoc.getElementsByTagName("schema").item(0).appendChild(xmlElement);
         // nach �nderung ausgeben:
         schema = SchemaUtils.buildSchema(xmlDoc, relativeURL);
         schema.printSchema();
         xmlDoc.print(System.out);
      } catch (Exception e) {
         e.printStackTrace();
      }

      String xsdString2 = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\" ?>"
            + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.example.org2\" "
            + "targetNamespace=\"http://www.example.org2\" elementFormDefault=\"qualified\">"
            + "<xsd:element name=\"ct\">" + "<xsd:complexType>"
            + " <xsd:sequence>" + "<xsd:element ref=\"e2\"/>"
            + "</xsd:sequence>" + "</xsd:complexType>" + "</xsd:element>"
            + "<xsd:element name=\"e2\" type=\"xsd:string\"/>"
            + "</xsd:schema>";

      String xsdMerge = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\" ?>"
            + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.example.org\" "
            + "targetNamespace=\"http://www.example.org\" elementFormDefault=\"qualified\">"
            + "<xsd:element name=\"Message\">" + "<xsd:complexType>"
            + " <xsd:sequence>" + "</xsd:sequence>" + "</xsd:complexType>"
            + "</xsd:element>" + "</xsd:schema>";

      // 2 xsds mergen, indem die rootelemente der beiden in eine gemeinsame
      // sequenz geh�ngt werden.
      try {
         XMLDocument xmlDoc1 = OracleDOMCreator.parseXMLString(xsdString);
         XMLDocument xmlDoc2 = OracleDOMCreator.parseXMLString(xsdString2);
         XMLDocument xmlDocMerged = OracleDOMCreator.parseXMLString(xsdMerge);
         // rootelemente bestimmen
         NodeList children = xmlDoc1.getElementsByTagName("schema").item(0)
               .getChildNodes();
         Node root = xmlDocMerged.getElementsByTagName("sequence").item(0);

         // rootelemente anh�ngen (der import muss sein, da ansonsten das
         // vater-document nicht passt)
         for (int i = 0; i < children.getLength(); i++) {
            root.appendChild(xmlDocMerged.importNode(children.item(i), true));
         }
         children = xmlDoc2.getElementsByTagName("schema").item(0)
               .getChildNodes();
         for (int i = 0; i < children.getLength(); i++) {
            root.appendChild(xmlDocMerged.importNode(children.item(i), true));
         }
         xmlDocMerged.print(System.out);
      } catch (Exception e) {
         e.printStackTrace();
      }

      // lese mehrere xsds, und f�ge gew�nschte nachrichten in neues xsd ein
      // (als ref innerhalb einer sequence)
      // TODO pfade der imports korrigieren?!
      String f1 = "/afs/gip.local/home/lippert/jdevhome/mywork/Xyna1.4/XSDsForJMSWrapper/f1.xsd";
      String f1EleName = "a1";
      String f2 = "/afs/gip.local/home/lippert/jdevhome/mywork/Xyna1.4/XSDsForJMSWrapper/f2.xsd";
      String f2EleName = "a2";
      try {
         XMLDocument xmlDocMerged = OracleDOMCreator.parseXMLString(xsdMerge);
         xmlDocMerged = OracleDOMCreator.mergeMessage(xmlDocMerged, f1, f1EleName);
         xmlDocMerged = OracleDOMCreator.mergeMessage(xmlDocMerged, f2, f2EleName);
         xmlDocMerged.print(System.out);
      } catch (Exception e) {
         e.printStackTrace();
      }

      String xmlString2 = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><bla xmlns=\"myNamespace\" xmlns:ns5=\"yourNamespace\" test=\"asd\"><blubb ja=\"nein\">wasch2</blubb><hopp><ns5:hipp>husch</ns5:hipp></hopp></bla>";
      String xmlString3 = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><bla xmlns=\"myNamespace\" test=\"asd\"><blubb ja=\"nein2\">wasch3</blubb><hopp><hipp xmlns=\"yourNamespace\">husch</hipp></hopp></bla>";
      // vergleiche mehrere xsd-fragmente
      try {
         XMLDocument xml1 = OracleDOMCreator.parseXMLString(xmlString);
         xml1.print(System.out);
         XMLDocument xml2 = OracleDOMCreator.parseXMLString(xmlString2);
         xml2.print(System.out);
         XMLDocument xml3 = OracleDOMCreator.parseXMLString(xmlString3);
         xml3.print(System.out);
         Node[] nodes = new Node[] { xml1, xml2, xml3 };
         Comparer comparer = new Comparer();
         Node ret = comparer.compareNodes(nodes);
         if (ret.getNodeType() == Node.DOCUMENT_NODE) {
            ((XMLDocument) ret).print(System.out);
         }
         // xml1.print(System.out);
      } catch (Exception e) {
         e.printStackTrace();
      }

      // generierung von xml aus xsds f�r verschiedene m�glichkeiten, wo das xsd
      // herkommt: file, string, mischung.
      // die utility arbeitet nicht mit oracle-xml klassen...
      try {
         // 1. vollst�ndig aus dem filesystem (oder andere url) (imports
         // geschehen automatisch)
         String xsdt1 = "/home/jdevhome/mywork/bpel/_Interfaces/BPELMessages_VPN_Simple_2Standorte.xsd";
         System.out.println(StringCreator.generateXMLFromXSD(OracleDOMCreator
               .createURL(xsdt1), "VPN_Simple_2StandorteRequest"));

         // 2. xsd als string + imports aus filesystem/internet (je nachdem, was
         // die import-schemalocations halt sagen
         String importloc = OracleDOMCreator
               .createURL(
                     "/home/jdevhome/mywork/Xyna1.4/XSDsForJMSWrapper/f1.xsd")
               .toString();
         System.out.println(importloc);
         String xsdt0 = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\" ?>"
               + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.namespace2\""
               + "            targetNamespace=\"http://www.namespace2\" elementFormDefault=\"qualified\" xmlns:ns1=\"http://www.namespace1\">"
               + "  <xsd:import schemaLocation=\""
               + importloc
               + "\" namespace=\"http://www.namespace1\"/>"
               + "  <xsd:element name=\"a2\">"
               + "    <xsd:annotation>"
               + "      <xsd:documentation>A sample element</xsd:documentation>"
               + "    </xsd:annotation>" + "    <xsd:complexType>"
               + "      <xsd:sequence>" + "        <xsd:element ref=\"bb\"/>"
               + "      </xsd:sequence>" + "    </xsd:complexType>"
               + "  </xsd:element>"
               + "  <xsd:element name=\"bb\" type=\"xsd:string\"/>"
               + "  <xsd:element name=\"element3\" type=\"ns1:complexType1\"/>"
               + "</xsd:schema>";
         System.out.println(StringCreator.generateXMLFromXSD(xsdt0, "element3"));

         // 3. vollst�ndig zur laufzeit vorhandene xsds, eigtl mit imports,
         // diese m�ssen aber zur xsd-compilierung gel�scht werden. daf�r dann
         // alle n�tigen xsds �bergeben.
         // gleiches xsd wie in beispiel 1.
         String xsdt0_ohneimport = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\" ?>"
               + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.namespace2\""
               + "            targetNamespace=\"http://www.namespace2\" elementFormDefault=\"qualified\" xmlns:ns1=\"http://www.namespace1\">"
               + "  <xsd:element name=\"a2\">"
               + "    <xsd:annotation>"
               + "      <xsd:documentation>A sample element</xsd:documentation>"
               + "    </xsd:annotation>"
               + "    <xsd:complexType>"
               + "      <xsd:sequence>"
               + "        <xsd:element ref=\"bb\"/>"
               + "      </xsd:sequence>"
               + "    </xsd:complexType>"
               + "  </xsd:element>"
               + "  <xsd:element name=\"bb\" type=\"xsd:string\"/>"
               + "  <xsd:element name=\"element3\" type=\"ns1:complexType1\"/>"
               + "</xsd:schema>";
         String xsdt_1 = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\" ?>"
               + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.namespace1\""
               + "            targetNamespace=\"http://www.namespace1\" elementFormDefault=\"qualified\">"
               + "  <xsd:element name=\"a1\" type=\"xsd:string\">"
               + "    <xsd:annotation>" + "      <xsd:documentation>"
               + "        A sample element" + "      </xsd:documentation>"
               + "    </xsd:annotation>" + "  </xsd:element>"
               + "  <xsd:complexType name=\"complexType1\">"
               + "    <xsd:sequence>"
               + "      <xsd:element name=\"element1\" type=\"xsd:string\"/>"
               + "    </xsd:sequence>" + "  </xsd:complexType>"
               + "</xsd:schema>";
         System.out.println(StringCreator.generateXMLFromXSD(new String[] { xsdt_1,
               xsdt0_ohneimport }, "element3"));

      } catch (Exception e) {
         e.printStackTrace();
      }

   }

}
