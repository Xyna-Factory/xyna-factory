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
package com.gip.xyna.xdev.map.typegen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSException;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xdev.map.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xdev.map.typegen.exceptions.WSDLParsingException;
import com.gip.xyna.xdev.map.typegen.exceptions.WSDLParsingException.WSDLParsingFailure;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/**
 *
 */
public class WsdlToXsd {
  
  
  private static final String NAMESPACE_SCHEMA = "http://www.w3.org/2001/XMLSchema";
  private static final String NAMESPACE_SOAP = "http://schemas.xmlsoap.org/wsdl";
  
  
  
  private String wsdlName;
  private Document xsdDocument;
  
  /**
   * @param wsdl
   * @return
   * @throws TypeMapperCreationException 
   */
  public boolean extract(File wsdl) throws WSDLParsingException {
    wsdlName = wsdl.getName();
    
    Element element = null;
    try {
      Document doc = XMLUtils.parse(wsdl, true);
      element = doc.getDocumentElement();
    } catch (XPRC_XmlParsingException e) {
      throw new WSDLParsingException(WSDLParsingFailure.Parse, wsdlName, e );
    } catch (Ex_FileAccessException e) {
      throw new WSDLParsingException(WSDLParsingFailure.Parse, wsdlName, e );
    }
        
    NodeList bind = element.getElementsByTagNameNS( NAMESPACE_SCHEMA,  "schema");
    if( bind.getLength() == 0 ) {
      return false;
    } else {
      //Bei korrektem Aufbau des WSDL sollte es nur ein "types" und damit nur ein "schema" geben
      Node schema = bind.item(0);
      
      xsdDocument = createXSDDocument( extractNamespaces(element), schema ) ;
      
      return true;
    }
  }
  
  private Map<String, String> extractNamespaces(Element element) {
    Map<String, String> namespaces = new HashMap<String, String>();
    NamedNodeMap attr = element.getAttributes();
    for( int a=0; a< attr.getLength(); ++a ) {
      Node n = attr.item(a);
      if( ! n.getNodeName().startsWith("xmlns") ) {
        continue;
      }
      String namespace = n.getNodeValue();
      if( namespace.startsWith(NAMESPACE_SOAP)) {
        continue; //SOAP-Namespace wird nicht mehr gebraucht
      }
      String prefix;
      if( n.getNodeName().equals("xmlns") ) {
        prefix = "";
      } else {
        prefix = n.getNodeName().substring(6);
      }
      namespaces.put(prefix, namespace);
    }
    return namespaces;
  }

  private Document createXSDDocument(Map<String, String> namespaces, Node schema) throws WSDLParsingException {
    Document doc;
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      doc = db.newDocument();
      
      //Node "schema" importieren
      Element root = (Element)doc.importNode(schema, true);
      doc.appendChild( root );
      
      //SchemaLocations anpassen, da diese beim Import Fehler verursachen
      flattenSchemaLocations(root);
      
      
      //Namespaces definieren und ans Root-Element anhängen
      for( Map.Entry<String,String> entry : namespaces.entrySet() ) {
        if( entry.getKey().length() <= 0 ) {
          root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", entry.getValue() );          
        } else {
          root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:"+entry.getKey(), entry.getValue() );
        }
      }
      
      return doc;
    } catch( ParserConfigurationException e) {
      throw new WSDLParsingException(WSDLParsingFailure.Parse, wsdlName, e );
    }
  }
  
  private void flattenSchemaLocations(Element root) {
    NodeList imports = root.getElementsByTagNameNS(NAMESPACE_SCHEMA, "import");
    for( int i=0; i< imports.getLength(); ++i ) {
      Element imp = (Element)imports.item(i);
      Attr attr = imp.getAttributeNode( "schemaLocation");
      if( attr != null ) {
        File schema = new File(attr.getNodeValue());
        attr.setNodeValue( schema.getName() );
      }
    }  
  }

  public File saveXsdAs(String filename) throws WSDLParsingException {
    try {
      return XercesUtils.writeDocumentToFile(xsdDocument, filename);
    } catch( LSException e ) {
      throw new WSDLParsingException(WSDLParsingFailure.WriteXsd, wsdlName, e );
    } catch( IOException e ) {
      throw new WSDLParsingException(WSDLParsingFailure.WriteXsd, wsdlName, e );
    }
  }

  
}
