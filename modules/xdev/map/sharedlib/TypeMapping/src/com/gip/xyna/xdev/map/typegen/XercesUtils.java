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
package com.gip.xyna.xdev.map.typegen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.xerces.dom.CoreDOMImplementationImpl;
import org.apache.xerces.impl.xs.XSImplementationImpl;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;


/**
 * 
 *
 * Richtiger Wege �ber DOMImplementationRegistry hat Probleme mit ClassLoading:
 * DOMImplementationRegistry kennt xercesImpl nicht, da diese mit anderem ClassLoader geladen 
 * wurde. Daher m�ssen die ben�tigten Objekte direkt instantiiert werden
 */
public class XercesUtils {
  
  private static XynaPropertyBoolean WRITE_XML_WORKAROUND = new XynaPropertyBoolean("xfmg.xfctrl.datamodel.xsd.write_xml_workaround", false ).setHidden(true);  
  
  private static Logger logger = CentralFactoryLogging.getLogger(XercesUtils.class);
  
  /*
  private DOMImplementationRegistry getRegistry() {
    System.setProperty(DOMImplementationRegistry.PROPERTY,
        "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
    DOMImplementationRegistry registry;
   
    try {
      registry = DOMImplementationRegistry.newInstance();
    } catch (ClassCastException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return registry;
  }*/

  
  
  public static XSLoader getXSLoader(DOMErrorHandler errorHandler) {
    //XSImplementation impl = (XSImplementation) getRegistry().getDOMImplementation("XS-Loader");

    XSImplementation impl = (XSImplementation) new XSImplementationImpl();
    XSLoader xsloader = impl.createXSLoader(null);
    xsloader.getConfig().setParameter("error-handler", errorHandler );
    
    //Siehe http://xerces.apache.org/xerces2-j/features.html
    xsloader.getConfig().setParameter( "http://apache.org/xml/features/honour-all-schemaLocations", true);
    
    return xsloader;
  }
  
  public static DOMImplementationLS getDOMImplementationLS() {
    //richtiger Weg w�re �ber 
    //DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
    //return (DOMImplementationLS)registry.getDOMImplementation("LS","3.0");
    //dies klappt wegen ClassLoadingProblemen jedoch nicht
    //daher direkt instantiieren
    return new CoreDOMImplementationImpl();
  }

  
  public static Map<String,Object> domConfigToMap(DOMConfiguration domConfig) {
    Map<String,Object> map = new HashMap<String,Object>();
    DOMStringList keys = domConfig.getParameterNames();
    for( int k=0; k<keys.getLength(); ++k ) {
      String key = keys.item(k);
      map.put( key, domConfig.getParameter(key) );
    }
    return map;
  }

  /**
   * @param document
   * @param filename
   * @return
   * @throws IOException 
   */
  public static File writeDocumentToFile(Document document, String filename) throws IOException, LSException {
    File file = new File(filename);
    
    DOMImplementationLS lsImpl = getDOMImplementationLS();
    LSOutput lsOut = lsImpl.createLSOutput();
    FileOutputStream fos = new FileOutputStream(file);
    lsOut.setByteStream(fos);
    
    getLSSerializer(lsImpl, true).write(document,lsOut);
    fos.close();
    return file;    
  }

  /**
   * @param document
   * @return
   * @throws LSException
   */
  public static String writeDocumentToString(Document document) {
    if( WRITE_XML_WORKAROUND.get() ) {
      //TODO Workaround gegen untere NullPointerException
      return writeDocumentToStringUsingTransformer(document);
    }
    try {
      DOMImplementationLS lsImpl = getDOMImplementationLS();

      LSOutput lsOut = lsImpl.createLSOutput();
      StringWriter sw = new StringWriter();
      lsOut.setCharacterStream(sw);
      lsOut.setEncoding("UTF-8");
      getLSSerializer(lsImpl, true).write(document,lsOut);
      return sw.toString();
    } catch( LSException e ) {
      //Dieses XML-Schreiben ist so empfohlen, hat bei meinen Tests funktioniert.
      //Ist bei Leo aber gescheitert mit
      //  at ----- Caused by java.lang.NullPointerException null. (depth: 3)
      //at org.apache.xerces.dom.DOMNormalizer.isAttrValueWF(Unknown Source)
      //at org.apache.xml.serialize.DOMSerializerImpl.verify(Unknown Source)
      // at org.apache.xml.serialize.DOMSerializerImpl.prepareForSerialization(Unknown Source)
      // at org.apache.xml.serialize.DOMSerializerImpl.write(Unknown Source)
      // at com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XercesUtils.writeDocumentToString(XercesUtils.java:131)
      // Im Netz findet sich dazu keine anderes Beispiel.
      //Um dies zu vermeiden gibt es diesen versteckte XynaProperty
      logger.warn("Could not write xml, try setting hidden xynaproperty \""+WRITE_XML_WORKAROUND.getPropertyName()+"\" to true" );
      throw e;
    }
    //TODO unteres schreibt in String mit UTF-16 im Prolog, dies kann eigenartigerweise nicht von 
    //com.gip.xyna.xprc.xfractwfe.generation.XMLUtils.parseString(data, true); geparst werden
    //siehe Bug 19395
    //return getLSSerializer(lsImpl, true).writeToString(document);
  }
  
  private static String writeDocumentToStringUsingTransformer(Document document) {
    StringWriter sw = new StringWriter();
    try {
      Source source = new DOMSource(document);
      Result result = new StreamResult(sw);
      TransformerFactory factory = TransformerFactory.newInstance();
      try {
        factory.setAttribute("indent-number", 2);
      } catch (IllegalArgumentException e) {
        logger.debug("Unable to set xml indent");
      }
      Transformer xformer = factory.newTransformer();
      xformer.setOutputProperty(OutputKeys.METHOD, "xml");
      xformer.setOutputProperty(OutputKeys.INDENT, "yes");
      xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      xformer.transform(source, result);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  private static LSSerializer getLSSerializer(DOMImplementationLS lsImpl, boolean prettyPrint) {
    LSSerializer lsSerializer = lsImpl.createLSSerializer();
    //https://xerces.apache.org/xerces2-j/javadocs/api/org/w3c/dom/ls/LSSerializer.html
    lsSerializer.getDomConfig().setParameter("format-pretty-print", prettyPrint);
    return lsSerializer;
  }

  public static Document parseXml(String xml) throws XPRC_XmlParsingException {
    try {
      DOMImplementationLS lsImpl = getDOMImplementationLS();
      LSInput liIn = lsImpl.createLSInput();
      liIn.setCharacterStream(new StringReader(xml));
      LSParser lsParser = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS,"http://www.w3.org/2001/XMLSchema");
      return lsParser.parse(liIn);
    } catch( LSException e ) {
      throw new XPRC_XmlParsingException("string", e);
    }
  }
  
  
}
