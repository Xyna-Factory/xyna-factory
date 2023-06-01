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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.ApplicationInfoEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.OrdertypeXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class ApplicationXMLTest {


  public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

    File file = new File("./test/application.xml");
    //System.out.println( file.getAbsolutePath() );
    
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    ApplicationXmlHandler handler = new ApplicationXmlHandler();
    saxParser.parse(file, handler);
    ApplicationXmlEntry applicationXml = handler.getApplicationXmlEntry();
    
    System.out.println( "Application " + applicationXml.getApplicationName() + " " + applicationXml.getVersionName() );
    
    ApplicationInfoEntry ai = applicationXml.getApplicationInfo();
    if( ai != null ) {
      System.out.println( "ApplicationInfo");
      System.out.println( "  DE: " + ai.getDescription().get(DocumentationLanguage.DE) );
      System.out.println( "  EN: " + ai.getDescription().get(DocumentationLanguage.EN) );
      System.out.println( "  BuildDate " + ai.getBuildDate() );
      List<RuntimeContextRequirementXmlEntry> rcrs = ai.getRuntimeContextRequirements();
      if( rcrs != null ) {
        System.out.println( "  RuntimeContextRequirements ");
        for( RuntimeContextRequirementXmlEntry rcrxe : rcrs ) {
          System.out.println( "    "+rcrxe.getApplication()+" "+rcrxe.getVersion() +" " + rcrxe.getWorkspace() );
        }
      }
      
    } else {
      System.out.println( "keine ApplicationInfo");
    }
    
    System.out.println( "OrderTypes");
    for( OrdertypeXmlEntry oxe : applicationXml.getOrdertypes() ) {
      System.out.println( "  " + oxe.getDestinationKey() );
    }
    
    /*
    System.out.println( "XMOMs");
    for( XMOMXmlEntry xxe : applicationXml.getXmomEntries() ) {
      System.out.println( "  " + xxe.getFqName() );
    }*/
    
    Document doc = applicationXml.buildXmlDocument();
    StringWriter sw = new StringWriter();
    XMLUtils.saveDomToWriter(sw,doc);
    System.err.println( sw.toString() );
  }

}
