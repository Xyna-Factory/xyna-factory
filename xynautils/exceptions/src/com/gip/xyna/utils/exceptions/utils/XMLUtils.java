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
package com.gip.xyna.utils.exceptions.utils;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;




/**
 * abgespeckte xml utils, um keine abhängigkeit von dem xml utils package zu erzeugen
 */
public class XMLUtils {

  private static DocumentBuilderFactory defaultBuilderFactory = DocumentBuilderFactory.newInstance();
  private static DocumentBuilderFactory namespaceUnawareBuilderFactory = DocumentBuilderFactory.newInstance();
  private static DocumentBuilderFactory namespaceAwareBuilderFactory = DocumentBuilderFactory.newInstance();
  private static DocumentBuilder defaultDocumentBuilder;
  private static DocumentBuilder namespaceAwareDocumentBuilder;
  private static DocumentBuilder namespaceUnawareDocumentBuilder;

  static {
    namespaceUnawareBuilderFactory.setNamespaceAware(false);
    namespaceAwareBuilderFactory.setNamespaceAware(true);
    try {
      namespaceAwareDocumentBuilder = namespaceAwareBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Could not create namespaceaware document builder", e);
    } catch (FactoryConfigurationError e) {
      throw new RuntimeException("Could not create namespaceaware document builder", e);
    }
    try {
      namespaceUnawareDocumentBuilder = namespaceUnawareBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Could not create namespaceunaware document builder");
    } catch (FactoryConfigurationError e) {
      throw new RuntimeException("Could not create namespaceunaware document builder", e);
    }
    try {
      defaultDocumentBuilder = defaultBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Could not create default document builder");
    } catch (FactoryConfigurationError e) {
      throw new RuntimeException("Could not create default document builder", e);
    }
  }


  public static String getTextContent(Element el) {
    if (el == null) {
      return "";
    }
    NodeList nl = el.getChildNodes();
    String ret = "";
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == Node.TEXT_NODE) {
        ret += nl.item(i).getNodeValue();
      }
    }
    return ret;
  }
  
  public static List<Element> getChildElements(Element el) {
    NodeList nl = el.getChildNodes();
    ArrayList<Element> ret = new ArrayList<Element>();
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
        ret.add((Element) nl.item(i));
      }
    }
    return ret;
  }


  public static List<Element> getChildElementsByName(Element el, String name) {
    NodeList nl = el.getChildNodes();
    ArrayList<Element> ret = new ArrayList<Element>();
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == Node.ELEMENT_NODE && nl.item(i).getNodeName().equals(name.trim())) {
        ret.add((Element) nl.item(i));
      }
    }
    return ret;
  }


  public static Element getChildElementByName(Element el, String name) {
    NodeList nl = el.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == Node.ELEMENT_NODE && nl.item(i).getNodeName().equals(name.trim())) {
        return (Element) nl.item(i);
      }
    }
    return null;
  }

  /**
   * @param is
   * @param fileName nur für logging/exceptions
   * @return
   * @throws InvalidXMLException
   */
  public static Document getDocumentFromStream(InputStream is, String fileName) throws InvalidXMLException {
    DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
    fac.setNamespaceAware(true);
    DocumentBuilder builder = null;
    try {
      builder = fac.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    Document doc = null;
    try {
      doc = builder.parse(is);
    } catch (SAXException e) {
      throw new InvalidXMLException(fileName, e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    doc.setDocumentURI(fileName);
    return doc;
  }


  public static Document getDocumentFromFile(String fileName) throws FileNotFoundException, InvalidXMLException {
    Document doc = getDocumentFromStream(new FileInputStream(fileName), fileName);
    doc.setDocumentURI(fileName);
    return doc;
  }


  public static void validateAgainstXSD(Document doc, InputStream xsd, String xsdFileName) throws InvalidXMLException {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Source schemaFile;
    try {
      schemaFile = new StreamSource(xsd);
    } catch (Exception e) {
      throw new RuntimeException("XSD " + xsdFileName +  " not found", e);
    }
    Schema schema = null;
    try {
      schema = factory.newSchema(schemaFile);
    } catch (SAXException e) {
      throw new RuntimeException("schema " + xsdFileName +  " invalid", e);
    }

    Validator validator = schema.newValidator();

    try {
      validator.validate(new DOMSource(doc));
    } catch (SAXException e) {
      throw new InvalidXMLException(doc.getDocumentURI(), e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
   
  }


  public static boolean isTrue(Element e, String attributeName) {
    return e.getAttribute(attributeName.trim()).equals("true");
  }


  /**
   * nicht namespaceaware
   */
  public static Document parse(File xmlfile) throws InvalidXMLException {
    return parse(xmlfile, false);
  }


  public static Document parse(File xmlfile, boolean nameSpaceAware) throws InvalidXMLException {

    DocumentBuilder builder = getDocumentBuilderNeedsToBeSynchronized(nameSpaceAware);
    synchronized (builder) {
      try {
        return builder.parse(xmlfile);
      } catch (SAXException e) {
        throw new InvalidXMLException(xmlfile.getPath(), e);
      } catch (IOException e) {
        throw new RuntimeException(xmlfile.getPath(), e);
      }
    }

  }


  private static DocumentBuilder getDocumentBuilderNeedsToBeSynchronized(boolean nameSpaceAware) {
    DocumentBuilder builder;
    if (nameSpaceAware) {
      builder = namespaceAwareDocumentBuilder;
    } else {
      builder = namespaceUnawareDocumentBuilder;
    }
    if (builder == null) {
      throw new IllegalStateException(XMLUtils.class.getSimpleName() + " have not been initialized properly");
    }
    return builder;
  }


  /**
   * nicht namespaceaware
   */
  public static Document parse(String xmlfileLocation) throws InvalidXMLException {
    return parse(xmlfileLocation, false);
  }


  public static Document parse(String xmlfileLocation, boolean nameSpaceAware) throws InvalidXMLException {

    DocumentBuilder builder = getDocumentBuilderNeedsToBeSynchronized(nameSpaceAware);
    synchronized (builder) {
      try {
        return builder.parse(new File(xmlfileLocation));
      } catch (SAXException e) {
        throw new InvalidXMLException(xmlfileLocation, e);
      } catch (IOException e) {
        throw new RuntimeException(xmlfileLocation, e);
      }
    }

  }

}