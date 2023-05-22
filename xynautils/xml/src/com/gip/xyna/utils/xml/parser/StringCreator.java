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
package com.gip.xyna.utils.xml.parser;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.xsd2inst.SampleXmlUtil;

public class StringCreator {

   /**
    * imports von xsds die bereits in dem array xsdStrings enthalten sind,
    * f�hren zu problemen. die xsdStrings w�ren also als xsd-dateien eigtl
    * invalide wegen fehlender imports.
    * 
    * @param xsdStrings
    *              sammlung von xsds, die ben�tigt werden.
    * @param elementName
    * @return
    * @throws Exception
    */
   public static String generateXMLFromXSD(String[] xsdStrings,
         String elementName) throws Exception {
      // Process Schema files
      List<XmlObject> sdocs = new ArrayList<XmlObject>();
      for (int i = 0; i < xsdStrings.length; i++) {
         try {
            sdocs.add(XmlObject.Factory.parse(xsdStrings[i], (new XmlOptions())
                  .setLoadLineNumbers().setLoadMessageDigest()));
         } catch (Exception e) {
            System.err.println("Can not load schema file: " + xsdStrings[i]
                  + ": ");
            e.printStackTrace();
         }
      }
      XmlObject[] schemas = (XmlObject[]) sdocs.toArray(new XmlObject[sdocs
            .size()]);
      return generateXMLInternal(schemas, elementName);
   }

   /**
    * imports/includes m�ssen global erreichbar sein
    * 
    * @param xsdString
    *              xsd inhalt (darf nicht filename sein (CDATA-fehler))
    * @param elementName
    * @return
    * @throws Exception
    */
   public static String generateXMLFromXSD(String xsdString, String elementName)
         throws Exception {
      // Process Schema files
      List<XmlObject> sdocs = new ArrayList<XmlObject>();
      try {
         sdocs.add(XmlObject.Factory.parse(xsdString, (new XmlOptions())
               .setLoadLineNumbers().setLoadMessageDigest()));
      } catch (Exception e) {
         System.err.println("Can not load schema file: " + xsdString + ": ");
         e.printStackTrace();
      }

      XmlObject[] schemas = (XmlObject[]) sdocs.toArray(new XmlObject[sdocs
            .size()]);
      return generateXMLInternal(schemas, elementName);
   }

   // code von
   // http://svn.apache.org/viewvc/xmlbeans/trunk/src/tools/org/apache/xmlbeans/impl/xsd2inst/SchemaInstanceGenerator.java?revision=149478
   // Helper method to create a URL from a file name
   // aus oracle demo copy&paste

   /**
    * imports/includes m�ssen relativ zur url oder global erreichbar sein!
    * 
    * @param url
    * @param elementName
    * @return
    * @throws Exception
    */
   public static String generateXMLFromXSD(URL url, String elementName)
         throws Exception {
      // Process Schema files
      List<XmlObject> sdocs = new ArrayList<XmlObject>();
      try {
         sdocs.add(XmlObject.Factory.parse(url, (new XmlOptions())
               .setLoadLineNumbers().setLoadMessageDigest()));
      } catch (Exception e) {
         System.err
               .println("Can not load schema file: " + url.getPath() + ": ");
         e.printStackTrace();
      }

      XmlObject[] schemas = (XmlObject[]) sdocs.toArray(new XmlObject[sdocs
            .size()]);
      return generateXMLInternal(schemas, elementName);
   }

   private static String generateXMLInternal(XmlObject[] schemas,
         String elementName) throws Exception {
      boolean downloadImports = true;
      SchemaTypeSystem sts = null;
      if (schemas.length > 0) {
         Collection errors = new ArrayList();
         XmlOptions compileOptions = new XmlOptions();
         if (downloadImports) {
            compileOptions.setCompileDownloadUrls();
         }
         try {
            sts = XmlBeans.compileXsd(schemas, XmlBeans.getBuiltinTypeSystem(),
                  compileOptions);
            // sts = XmlBeans.loadXsd(schemas, compileOptions);
         } catch (Exception e) {
            if (errors.isEmpty() || !(e instanceof XmlException))
               e.printStackTrace();

            System.out.println("Schema compilation errors: ");
            for (Iterator i = errors.iterator(); i.hasNext();)
               System.out.println(i.next());
         }
      }

      if (sts == null) {
         System.out.println("No Schemas to process.");
         return "";
      }
      SchemaType[] globalElems = sts.documentTypes();
      SchemaType elem = null;
      for (int i = 0; i < globalElems.length; i++) {
         if (elementName.equals(globalElems[i].getDocumentElementName()
               .getLocalPart())) {
            elem = globalElems[i];
            break;
         }
      }

      if (elem == null) {
         System.out.println("Could not find a global element with name \""
               + elementName + "\"");
         return "";
      }

      // Now generate it
      return SampleXmlUtil.createSampleForType(elem);
   }

}
