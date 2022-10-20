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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.gip.xyna.BijectiveMap;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public class XMLUtils {

  private static final Logger logger = Logger.getLogger(XMLUtils.class);
  
  private static final String CDATA_OPEN_STRING = "<![CDATA[";
  private static final String CDATA_CLOSE_STRING = "]]>";
  private static final char[] CDATA_OPEN = CDATA_OPEN_STRING.toCharArray();
  private static final char[] CDATA_CLOSE = CDATA_CLOSE_STRING.toCharArray();


  /**
   * Upper limit. Filled up lazily, content is never reduced.
   */
  private static final int BUILDER_CACHE_SIZE = 15;

  
  public static enum DocumentBuilderInstance {

    DEFAULT {
      @Override
      public DocumentBuilder constructDocumentBuilder() throws ParserConfigurationException, FactoryConfigurationError {
        return defaultBuilderFactory.newDocumentBuilder();
      }
    },
    NAMESPACE_AWARE {
      @Override
      public DocumentBuilder constructDocumentBuilder() throws ParserConfigurationException, FactoryConfigurationError {
        return namespaceAwareBuilderFactory.newDocumentBuilder();
      }
    },
    NAMESPACE_UNAWARE {
      @Override
      public DocumentBuilder constructDocumentBuilder() throws ParserConfigurationException, FactoryConfigurationError {
        return namespaceUnawareBuilderFactory.newDocumentBuilder();
      }
    };
    
    private static DocumentBuilderFactory defaultBuilderFactory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilderFactory namespaceUnawareBuilderFactory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilderFactory namespaceAwareBuilderFactory = DocumentBuilderFactory.newInstance();
    static {
      namespaceAwareBuilderFactory.setNamespaceAware(true);
      namespaceUnawareBuilderFactory.setNamespaceAware(false);
    }


    private final Set<DocumentBuilder> builders = new HashSet<DocumentBuilder>();

    public DocumentBuilder getDocumentBuilder() {

      synchronized (builders) {
        Iterator<DocumentBuilder> iter = builders.iterator();
        if (!iter.hasNext()) {
          try {
            return constructDocumentBuilder();
          } catch (ParserConfigurationException e1) {
            throw new RuntimeException("Could not create " + this.toString() + " document builder", e1);
          } catch (FactoryConfigurationError e1) {
            throw new RuntimeException("Could not create " + this.toString() + " document builder", e1);
          }
        } else {
          DocumentBuilder result = iter.next();
          iter.remove();
          return result;
        }
      }

    }


    public void returnBuilder(DocumentBuilder builder) {
      boolean resetted = false;
      try {
        builder.reset();
        resetted = true;
      } catch (UnsupportedOperationException e) {
        logger.warn("DocumentBuilder could not be reset", e);
      }
      if (resetted && builders.size() < BUILDER_CACHE_SIZE) {
        synchronized (builders) {
          if (builders.size() < BUILDER_CACHE_SIZE) {
            builders.add(builder);
          }
        }
      }
    }


    protected abstract DocumentBuilder constructDocumentBuilder() throws ParserConfigurationException,
        FactoryConfigurationError;


    public Document parse(File xmlfile) throws XPRC_XmlParsingException, Ex_FileAccessException {
      DocumentBuilder builder = getDocumentBuilder();
      try {
        return builder.parse(xmlfile);
      } catch (SAXException e) {
        throw new XPRC_XmlParsingException(xmlfile.getPath(), e);
      } catch (IOException e) {
        throw new Ex_FileAccessException(xmlfile.getPath(), e);
      } finally {
        returnBuilder(builder);
      }
    }


    public Document parse(String xml) throws XPRC_XmlParsingException, Ex_FileAccessException {
      DocumentBuilder builder = getDocumentBuilder();
      try {
        return builder.parse(new InputSource(new StringReader(xml)));
      } catch (SAXException e) {
        throw new XPRC_XmlParsingException(xml, e);
      } catch (IOException e) {
        throw new Ex_FileAccessException(xml, e);
      } finally {
        returnBuilder(builder);
      }
    }


    public static DocumentBuilderInstance getDocumentBuilder(boolean nameSpaceAware) {
      if (nameSpaceAware) {
        return NAMESPACE_AWARE;
      } else {
        return NAMESPACE_UNAWARE;
      }
    }
    
  }


  private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
  static {
    try {
      transformerFactory.setAttribute("indent-number", 2);
    } catch (IllegalArgumentException f) {
      logger.warn("Unable to set xml indent");
    }
  }

  // setting the following to "true" resulted in slight performance increases
  private static final boolean TRANSFORMER_CACHING = true;
  private static final ConcurrentLinkedQueue<Transformer> transformersWithPi = new ConcurrentLinkedQueue<Transformer>();
  private static final ConcurrentLinkedQueue<Transformer> transformersWithoutPi = new ConcurrentLinkedQueue<Transformer>();
  private static Transformer getTransformer(boolean withPI) throws TransformerConfigurationException {

    Transformer result;
    if (withPI) {
      result = transformersWithPi.poll();
    } else {
      result = transformersWithoutPi.poll();
    }

    if (result == null) {
      result = getTransformerUncached(withPI);
    }
    return result;

  }
  
  private static Transformer getTransformerUncached(boolean withPI) throws TransformerConfigurationException {
    Transformer result;
    synchronized (transformerFactory) {
      result = transformerFactory.newTransformer();
    }
    result.setOutputProperty(OutputKeys.METHOD, "xml");
    result.setOutputProperty(OutputKeys.INDENT, "yes");
    result.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    result.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, withPI ? "no" : "yes");
    return result;
  }

  private static void returnTransformer(Transformer xformer, boolean withPI) {
    if (withPI) {
      if (transformersWithPi.size() < 10) {
        transformersWithPi.add(xformer);
      }
    } else {
      if (transformersWithoutPi.size() < 10) {
        transformersWithoutPi.add(xformer);
      }
    }
  }


  static {
    initArray();
  }

  public static void initialize() {
    // wird nur benötigt, um in XynaFactory die statische Initialisierung zu einem bestimmten Zeitpunkt ausführen zu können
  }
  
  

  public static interface PositionDecider {
    /**
     * ist die position zwischen den beiden übergebenen elementen die gewünschte?
     * predecessor == null =&gt; beginn der liste
     * successor == null =&gt; ende der liste
     * beides null =&gt; liste ist leer
     */
    public boolean decideInsertionBetweenSiblings(Element predecessor, Element successor);
  }
  
  /**
   * fügt newChild als kind von parent an der stelle ein, die von decider als true ermittelt wird (anhand der benachbarten geschwister)
   * @param parent
   * @param newChild
   * @param decider
   * @return
   */
  public static boolean insertChild(Element parent, Element newChild, PositionDecider decider) {
    List<Element> children = getChildElements(parent);
    if (children.size() == 0 && decider.decideInsertionBetweenSiblings(null, null)) {
      parent.appendChild(newChild);
      return true;
    }
    Element previousChild = null;
    for (Element child : children) {
      if (decider.decideInsertionBetweenSiblings(previousChild, child)) {
        parent.insertBefore(newChild, child);
        return true;
      }
      previousChild = child;
    }
    if (decider.decideInsertionBetweenSiblings(previousChild, null)) {
      parent.appendChild(newChild);
      return true;
    }
    return false;
  }


  public static List<Element> getChildElements(Node n) {
    NodeList nl = n.getChildNodes();
    ArrayList<Element> ret = new ArrayList<Element>();
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i);
      if (next.getNodeType() == Node.ELEMENT_NODE) {
        ret.add((Element) next);
      }
    }
    return ret;
  }


  /**
   * liefert direkten text-inhalt der node. falls mehrere textnodes direkte kinder sind, werden die inhalte aneinander
   * gehängt. falls das übergebene element null ist, wird ein leerer string zurückgegeben.
   * @param el
   * @return
   */
  public static String getTextContent(Element el) {
    if (el == null) {
      return "";
    }
    NodeList nl = el.getChildNodes();
    StringBuilder ret = new StringBuilder("");
    boolean containsCdata = containsCdata(nl);
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i);
      if (next.getNodeType() == Node.TEXT_NODE) {
        if (containsCdata) {
          ret.append(next.getNodeValue().trim());  
        } else {
          ret.append(next.getNodeValue());
        }
      } else if (next.getNodeType() == Node.CDATA_SECTION_NODE) {
        ret.append(next.getNodeValue());
      }
    }
    return ret.toString();
  }

  private static boolean containsCdata(NodeList nl) {
    for (int i = 0; i < nl.getLength(); i++) {
      Node next = nl.item(i);
      if (next.getNodeType() == Node.CDATA_SECTION_NODE) {
        return true;
      }
    }
    return false;
  }

  /**
   * wie {@link #getTextContent(Element)}, nur wird null zurückgegeben, wenn element null ist.
   *  &lt;ELEMENT&gt;&lt;/ELEMENT&gt; ist äquivalent zu &lt;ELEMENT/&gt;. beide geben leerstring ("") zurück
   * wie {@link #getTextContent(Element)}, nur wird für den String {@link #escapeXMLValueAndInvalidChars(String)} aufgerufen, um alle in XML nicht akzeptierten Zeichen zu ersetzen.
   */
  public static String getTextContentEscaped(Element el) {
    return escapeXMLValueAndInvalidChars(getTextContent(el));
  }

  /**
   * wie {@link #getTextContent(Element)}, nur wird null zurückgegeben, wenn element null ist.
   *  &lt;ELEMENT&gt;&lt;/ELEMENT&gt; ist äquivalent zu &lt;ELEMENT/&gt;. beide geben leerstring ("") zurück
   */
  public static String getTextContentOrNull(Element el) {
    if (el == null) {
      return null;
    }
    NodeList nl = el.getChildNodes();
    StringBuilder ret = new StringBuilder("");
    boolean containsCdata = containsCdata(nl);
    boolean foundTextNode = false;
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i);
      if (next.getNodeType() == Node.TEXT_NODE) {
        if (containsCdata) {
          ret.append(next.getNodeValue().trim());  
        } else {
          ret.append(next.getNodeValue());
        }
        foundTextNode = true;
      } else if (next.getNodeType() == Node.CDATA_SECTION_NODE) {
        ret.append(next.getNodeValue());
        foundTextNode = true;
      }
    }
    if (!foundTextNode) {
      return "";
    }
    return ret.toString();
  }


  /**
   * setzt den textcontent des ersten gefundenen kind-knoten des elements, oder legt einen neuen an, falls keiner
   * existiert.
   * @param el
   * @param content
   */
  public static void setTextContent(Element el, String content) {
    if (content == null) {
      content = "";
    }
    NodeList nl = el.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i);
      if (next.getNodeType() == Node.TEXT_NODE) {
        next.setNodeValue(content);
        return;
      }
    }
    el.appendChild(el.getOwnerDocument().createTextNode(content));
  }


  public static List<Element> getChildElementsByName(Element el, String name) {
    String trimmedName = name.trim();
    NodeList nl = el.getChildNodes();
    ArrayList<Element> ret = new ArrayList<Element>();
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i);
      if (next.getNodeType() == Node.ELEMENT_NODE && next.getNodeName().equals(trimmedName)) {
        ret.add((Element) next);
      }
    }
    return ret;
  }
  

  public static List<Element> getChildElementsByName(Element el, String name, String namespace) {
    String trimmedName = name.trim();
    NodeList nl = el.getChildNodes();
    ArrayList<Element> ret = new ArrayList<Element>();
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i);
      if (next.getNodeType() == Node.ELEMENT_NODE && next.getNamespaceURI().equals(namespace)
          && next.getLocalName().equals(trimmedName)) {
        ret.add((Element) next);
      }
    }
    return ret;
  }

  /**
   * ermittelt alle kind und kindeskind elemente rekursiv, die den angegebenen tagnamen haben.
   * @param el
   * @param name
   * @return
   */
  public static List<Element> getChildElementsRecursively(Element el, String name) {
    String trimmedName = name.trim();
    NodeList nl = el.getChildNodes();
    ArrayList<Element> ret = new ArrayList<Element>();
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i);
      if (next.getNodeType() == Node.ELEMENT_NODE) {
        if (next.getNodeName().equals(trimmedName)) {
          ret.add((Element) next);
        }
        ret.addAll(getChildElementsRecursively((Element) next, name));
      }
    }
    return ret;
  }


  public static Element getChildElementByName(Element el, String name) {
    String trimmedName = name.trim();
    NodeList nl = el.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node next =nl.item(i); 
      if (next.getNodeType() == Node.ELEMENT_NODE && next.getNodeName().equals(trimmedName)) {
        return (Element) next;
      }
    }
    return null;
  }


  public static Element getChildElementByName(Element el, String name, String namespace) {
    String trimmedName = name.trim();
    NodeList nl = el.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node next = nl.item(i);
      if (next.getNodeType() == Node.ELEMENT_NODE && next.getNamespaceURI().equals(namespace)
          && next.getLocalName().equals(trimmedName)) {
        return (Element) next;
      }
    }
    return null;
  }
  
  /**
   * gibt zurück, ob das Attribut mit dem Namen attributeName existiert und den Wert "true" hat.
   */
  public static boolean isTrue(Element e, String attributeName) {
    if (e == null) {
      return false;
    } else {
      return e.getAttribute(attributeName.trim()).equals("true");
    }
  }

  /**
   * nicht namespaceaware
   */
  public static Document parse(String xmlfileLocation) throws Ex_FileAccessException, XPRC_XmlParsingException {
    return parse(xmlfileLocation, false);
  }


  public static Document parse(String xmlfileLocation, boolean nameSpaceAware) throws Ex_FileAccessException, XPRC_XmlParsingException {
    return parse(new File(xmlfileLocation), nameSpaceAware);
  }
  
  
  /**
   * nicht namespaceaware
   */
  public static Document parse(File xmlfile) throws Ex_FileAccessException, XPRC_XmlParsingException {
    return parse(xmlfile, false);
  }


  public static Document parse(File xmlfile, boolean nameSpaceAware) throws Ex_FileAccessException, XPRC_XmlParsingException {
    return DocumentBuilderInstance.getDocumentBuilder(nameSpaceAware).parse(xmlfile);
  }
  
  
  //FIXME wieso wirft diese methode eine fielaccessexception?? -> ACHTUNG, erst ändern, wenn klar ist, dass diese schnittstellenänderung okay ist.
  //kann bei aufrufern zu compilefehlern führen, wenn man das ändert.
  public static Document parseString(String xml, boolean namespaceAware) throws XPRC_XmlParsingException, Ex_FileAccessException {

    DocumentBuilderInstance builder = DocumentBuilderInstance.getDocumentBuilder(namespaceAware);
    String xmlTrim = xml.trim();
    String xmlClean = replaceControlAndInvalidChars(xmlTrim);
    return builder.parse(xmlClean);
  }


  public static Document parseString(String xml) throws XPRC_XmlParsingException {
    try {
      return parseString(xml, false);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static void saveDomToWriter(Writer w, Document doc) {
    saveDomToWriter(w, doc, true);
  }


  public static void saveDomToWriter(Writer w, Node n, boolean withPI) {
    try {
      Source source = new DOMSource(n);
      Result result = new StreamResult(w);

      removeWhiteSpaces(n);
      
      Transformer xformer;
      if (TRANSFORMER_CACHING) {
        xformer = getTransformer(withPI);
      } else {
        xformer = getTransformerUncached(withPI);
      }
      try {
        xformer.transform(source, result);
      } finally {
        if (TRANSFORMER_CACHING) {
          returnTransformer(xformer, withPI);
        }
      }
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
  }


  private static void removeWhiteSpaces(Node n) {
    for (int i = n.getChildNodes().getLength() - 1; i >= 0; i--) {
      Node c = n.getChildNodes().item(i);
      if (c.getNodeType() == Node.ELEMENT_NODE) {
        removeWhiteSpaces(c);
      } else if (c.getNodeType() == Node.TEXT_NODE) {
        String text = c.getNodeValue();
        if (text != null) {
          text = text.trim();
          if (text.isEmpty()) {
            n.removeChild(c);
          } else {
            n.setNodeValue(text);
          }
        }
      }
    }
  }

  public static void saveDom(File f, Document d) throws  Ex_FileAccessException {
    if (!f.exists()) {
      if (f.getParentFile() != null)
        f.getParentFile().mkdirs();
      try {
        f.createNewFile();
      } catch (IOException e) {
        throw new Ex_FileWriteException(f.getAbsolutePath(), e);
      }
    }
    
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
    } catch (FileNotFoundException e) {
      throw new Ex_FileAccessException(f.getName(), e);
    }
    try {
      saveDomToOutputStream(fos, d);
      try {
        fos.flush();
      } catch (IOException e) {
        throw new Ex_FileAccessException(f.getName(), e);
      }
    } finally {
      try {
        fos.close();
      } catch (IOException e) {
        throw new Ex_FileAccessException(f.getName(), e);
      }
    }
  }

  /**
   * This Method prints the xml representation of the Document d into the OutputStream os
   * @param os
   * @param d
   */
  public static void saveDomToOutputStream(OutputStream os, Document d) {
    try {
      saveDomToWriter(new OutputStreamWriter(os, Constants.DEFAULT_ENCODING), d);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getXMLString(Element e, boolean withPI) {
    StringWriter sw = new StringWriter();
    saveDomToWriter(sw, e, withPI);
    return sw.toString();

  }


  public static void validateXMLvsXSD(String pathToXmlFile) throws XPRC_XmlParsingException, Ex_FileAccessException {
    validateXMLvsXSDAndReturnDoc(pathToXmlFile);
  }
  
  
  public static Document validateXMLvsXSDAndReturnDoc(String pathToXmlFile) throws XPRC_XmlParsingException, Ex_FileAccessException {
    if (logger.isDebugEnabled()) {
      logger.debug("Validating " + pathToXmlFile + " using XSD " + Constants.PATH_TO_XSD_FILE);
    }

    File f = new File(pathToXmlFile);
    if (!f.exists()) {
      throw new Ex_FileWriteException(f.getAbsolutePath());
    }

    File xsd = new File(Constants.PATH_TO_XSD_FILE);
    Document doc = XMLUtils.parse(f.getAbsolutePath(), true);

    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    //logger.debug("using " + factory.getClass().getName());
    Source schemaFile = new StreamSource(xsd);
    Schema schema = null;
    try {
      schema = factory.newSchema(schemaFile);
    } catch (SAXException e) {
      throw new XPRC_XmlParsingException(xsd.getAbsolutePath(), e);
    }

    Validator validator = schema.newValidator();
    try {
      validator.validate(new DOMSource(doc));
    } catch (SAXParseException e) {
      throw new XPRC_XmlParsingException(f.getAbsolutePath(), e);
    } catch (SAXException e) {
      throw new XPRC_XmlParsingException(f.getAbsolutePath(), e);
    } catch (IOException e) {
      throw new Ex_FileAccessException(f.getAbsolutePath(), e);
    }
    return doc;
  }


  /**
   * ermittelt nächstes geschwister element. falls keines existiert, wird null zurückgegeben
   */
  public static Element getNextElementSibling(Element el) {
    Node next = el.getNextSibling();
    while (next != null && next.getNodeType() != Node.ELEMENT_NODE) {
      next = next.getNextSibling();
    }
    return (Element)next;
  }


  private static final int CONTROL_CHAR_MAPPING = 256;
  private static boolean[] isControlChar;
  
  static void initArray() {
    isControlChar = new boolean[CONTROL_CHAR_MAPPING];
    for (int ch = 0; ch <CONTROL_CHAR_MAPPING; ch++) {
      // ignore all except TAB, LF and CR that are < than SPACE
      // ignore DEL and all characters < NO-BREAK-SPACE except NEXT-LINE
      if((ch < 0x20 && ch != 0x09 && ch != 0x0A && ch != 0x0D) ||
                      (ch >= 0x7F && ch <= 0x9F && ch != 0x85)) {
        isControlChar[ch] = true;
      }
    }
  }
  
  /**
   * wird vom generierten code verwendet! achtung bei umbenennung
   */
  public static String REPLACE_CONTROL_CHARS_METHOD_NAME = "replaceControlAndInvalidChars";

  
  public static String replaceControlAndInvalidChars(String string) {
    return replaceControlAndInvalidChars(string, false);
  }

  /**
   * ersetzt alle chars &lt; 0x20 ausser \n, \r, \t durch [&lt;char als int&gt;]
   * 
   * alle anderen nicht erlaubten zeichen müssen separat escaped werden.
   */
  public static String replaceIllegalXMLChars(String string) {
    if (string == null) {
      return null;
    }
    final int length = string.length();
    char[] m_charsBuff = new char[length];
    string.getChars(0, length, m_charsBuff, 0);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      char c = m_charsBuff[i];
      if (c < 0x20) {
        switch (c) {
          case '\n' : //fall through
          case '\r' : //fall through
          case '\t' :
            sb.append(c);
            break;
          default :
            //escape
            appendEscapedCharacter(sb, c);
            break;
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
  
  private static final XynaPropertyBoolean formalXMLEscaping = new XynaPropertyBoolean("xprc.xfractwfe.generation.FormalEscaping", false)
      .setDefaultDocumentation(DocumentationLanguage.EN, "If this is set, '&#x' will be used to escape in xml files instead if '[ ]'");
  
  /**
   * ersetzt zeichen, die in xml nicht erlaubt sind, durch # (0x23) oder falls verbose durch [&lt;hex&gt;]
   * falls string mit whitespace beginnt oder endet, wird er in CDATA gepackt.
   */
  public static String replaceControlAndInvalidChars(String string, boolean verboseSubstitution) {
    /*
     * nicht erlaubte zeichen:
     * http://www.w3.org/TR/REC-xml/#NT-Char
     * 
     * Char    ::=    #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
     * 
     * discouraged:
     * [#x7F-#x84], [#x86-#x9F], [#xFDD0-#xFDEF],
       [#x1FFFE-#x1FFFF], [#x2FFFE-#x2FFFF], [#x3FFFE-#x3FFFF],
       [#x4FFFE-#x4FFFF], [#x5FFFE-#x5FFFF], [#x6FFFE-#x6FFFF],
       [#x7FFFE-#x7FFFF], [#x8FFFE-#x8FFFF], [#x9FFFE-#x9FFFF],
       [#xAFFFE-#xAFFFF], [#xBFFFE-#xBFFFF], [#xCFFFE-#xCFFFF],
       [#xDFFFE-#xDFFFF], [#xEFFFE-#xEFFFF], [#xFFFFE-#xFFFFF],
       [#x10FFFE-#x10FFFF].
     * 
     * 
     */
    
    final int length = string.length();
    char[] m_charsBuff = new char[length];
    string.getChars(0, length, m_charsBuff, 0);
    boolean startsOrEndsWithWhiteSpace = m_charsBuff.length >= 1 && (Character.isWhitespace(m_charsBuff[0]) || Character.isWhitespace(m_charsBuff[m_charsBuff.length - 1]));

    if (verboseSubstitution) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < length; i++) {
        char c = m_charsBuff[i];
        if (c < CONTROL_CHAR_MAPPING && isControlChar[c]) {
          appendEscapedCharacter(sb, c);
          continue;
        } else if (c >= 0xD800) {
          if (c < 0xE000) {
            appendEscapedCharacter(sb, c);
            continue;
          } else if (c >= 0xFFFE) {
            if (c < 0x10000) {
              appendEscapedCharacter(sb, c);
              continue;
            } else if (c >= 0x110000) {
              appendEscapedCharacter(sb, c);
              continue;
            }
          }
        }
        sb.append(c);
      }
      m_charsBuff = new char[sb.length()];
      sb.getChars(0, sb.length(), m_charsBuff, 0);
    } else {
      for (int i = 0; i < length; i++) {
        char c = m_charsBuff[i];
        if (c < CONTROL_CHAR_MAPPING && isControlChar[c]) {
          m_charsBuff[i] = 0x23;
        } else if (c >= 0xD800) {
          if (c < 0xE000) {
            m_charsBuff[i] = 0x23;
          } else if (c >= 0xFFFE) {
            if (c < 0x10000) {
              m_charsBuff[i] = 0x23;
            } else if (c >= 0x110000) {
              m_charsBuff[i] = 0x23;
            }
          }
        }
      }
    }
    if (m_charsBuff.length >= 1) {
      if (startsOrEndsWithWhiteSpace) {
        char[] m_cdatatescaped_charsBuff = new char[m_charsBuff.length + CDATA_OPEN.length + CDATA_CLOSE.length];
        System.arraycopy(CDATA_OPEN, 0, m_cdatatescaped_charsBuff, 0, CDATA_OPEN.length);
        System.arraycopy(m_charsBuff, 0, m_cdatatescaped_charsBuff, CDATA_OPEN.length, m_charsBuff.length);
        System.arraycopy(CDATA_CLOSE, 0, m_cdatatescaped_charsBuff, CDATA_OPEN.length + m_charsBuff.length, CDATA_CLOSE.length);
        String escapedSequence = new String(m_cdatatescaped_charsBuff);
        if (escapedSequence.indexOf(CDATA_CLOSE_STRING) < m_cdatatescaped_charsBuff.length - CDATA_CLOSE.length) {
          String escapedStringWithCDATAClose = new String(m_charsBuff);
          StringBuilder sb = new StringBuilder();
          int fromIndex = 0;
          while (escapedStringWithCDATAClose.indexOf(CDATA_CLOSE_STRING, fromIndex) >= 0) {
            int newIndex = escapedStringWithCDATAClose.indexOf(CDATA_CLOSE_STRING, fromIndex);
            sb.append(CDATA_OPEN_STRING)
              .append(escapedStringWithCDATAClose.substring(fromIndex, newIndex))
              .append("]]")
              .append(CDATA_CLOSE_STRING);
            fromIndex = newIndex + CDATA_CLOSE_STRING.length() - 1;
          }
          if (fromIndex != escapedStringWithCDATAClose.length()) {
            sb.append(CDATA_OPEN_STRING)
              .append(escapedStringWithCDATAClose.substring(fromIndex))
              .append(CDATA_CLOSE_STRING);
          }
          return sb.toString();
        } else {
          return escapedSequence;
        }
      } else {
        return new String(m_charsBuff);
      }
    } else {
      return new String(m_charsBuff);
    }
  }

  private static void appendEscapedCharacter(StringBuilder sb, char c) {
    if (formalXMLEscaping.get()) {
      sb.append("&#x").append(toHexString(c)).append(";");
    } else {
      sb.append("[").append(toHexString(c)).append("]");
    }
  }
  
  private static String toHexString(char c) {
    return Integer.toHexString((int) c).toUpperCase();
  }

  public static NamespaceContext getNamespaceContextForDocument(Document doc) {
    return new DocumentNameSpaceContext(doc);
  }
  
  
  // does not handle overloaded prefixes, last prefix found will win
  private static class DocumentNameSpaceContext implements NamespaceContext {

    private static final String DEFAULT_NS = "DEFAULT";
    private BijectiveMap<String, String> prefix2Uri = new BijectiveMap<String, String>();


    private DocumentNameSpaceContext(Document doc) {
      crawlNode(doc.getFirstChild());
    }


    private void crawlNode(Node node) {
      NamedNodeMap attributes = node.getAttributes();
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attribute = attributes.item(i);
        storeAttribute((Attr) attribute);
      }
      NodeList childs = node.getChildNodes();
      for (int i = 0; i < childs.getLength(); i++) {
        Node chield = childs.item(i);
        if (chield.getNodeType() == Node.ELEMENT_NODE) {
          crawlNode(chield);
        }
      }
    }


    private void storeAttribute(Attr attribute) {
      if (attribute.getNamespaceURI() != null &&
          attribute.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
        if (attribute.getNodeName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
          prefix2Uri.put(DEFAULT_NS, attribute.getNodeValue());
        } else {
          prefix2Uri.put(attribute.getLocalName(), attribute.getNodeValue());
        }
      }
    }


    public String getNamespaceURI(String prefix) {
      if (prefix == null || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX) || prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
        return prefix2Uri.get(DEFAULT_NS);
      } else {
        return prefix2Uri.get(prefix);
      }
    }


    public String getPrefix(String namespaceURI) {
      return prefix2Uri.getInverse(namespaceURI);
    }


    public Iterator<String> getPrefixes(String namespaceURI) {
      return prefix2Uri.keySet().iterator();
    }

  }
  
  
  private final static Pattern escapableSignsRegex = Pattern.compile("([" +
                                                                     EscapableXMLEntity.SMALLER_SIGN.unescapedRepresentation +
                                                                     EscapableXMLEntity.GREATER_SIGN.unescapedRepresentation +
                                                                     EscapableXMLEntity.AMPERSAND.unescapedRepresentation +
                                                                     EscapableXMLEntity.APOSTROPHE.unescapedRepresentation +
                                                                     EscapableXMLEntity.QUOTE.unescapedRepresentation + "])");

  private final static Pattern escapableSignsWithoutQuoteRegex = Pattern.compile("([" +
                                                                     EscapableXMLEntity.SMALLER_SIGN.unescapedRepresentation +
                                                                     EscapableXMLEntity.GREATER_SIGN.unescapedRepresentation +
                                                                     EscapableXMLEntity.AMPERSAND.unescapedRepresentation +
                                                                     EscapableXMLEntity.APOSTROPHE.unescapedRepresentation + "])");

  /**
   * wird vom generierten code verwendet! achtung bei umbenennung
   */
  public static String ESCAPE_XML_VALUE_METHOD_NAME = "escapeXMLValue";
  
  public static String escapeXMLValue(final String xml) {
    return escapeXMLValue(xml, true, true);
  }

  // TODO we might benefit from merging both (ESCAPE_XML_VALUE_METHOD_NAME & REPLACE_CONTROL_CHARS_METHOD_NAME) methods
  public static String escapeXMLValue(final String xml, boolean replaceQuotes, boolean assumeWrappingCDATA) {
    if ( (xml != null && xml.length() > 0) &&
         ( (!assumeWrappingCDATA) || // do not escape single signs if the whole block will be CDATA escaped
           (!Character.isWhitespace(xml.charAt(0)) && !Character.isWhitespace(xml.charAt(xml.length() - 1))) ) ) {
      StringBuffer escapedOutput = new StringBuffer(xml.length());
      Matcher m;
      if (replaceQuotes) {
        m = escapableSignsRegex.matcher(xml);
      } else {
        m = escapableSignsWithoutQuoteRegex.matcher(xml);
      }

      while (m.find()) {
        EscapableXMLEntity entity = EscapableXMLEntity.getEntityByUnescapedRepresentation(m.group(1).charAt(0));
        if (entity != null) {
          m.appendReplacement(escapedOutput, entity.getFullEscapedRepresentation());
        }
      }
      m.appendTail(escapedOutput);
      return escapedOutput.toString();
    } else {
      return xml;
    }
  }

  public static String escapeXMLValueAndInvalidChars(String value) {
    return escapeXMLValueAndInvalidChars(value, true, true);
  }

  public static String escapeXMLValueAndInvalidChars(String value, boolean replaceQuotes, boolean assumeWrappingCDATA) {
    return (value != null) ? replaceControlAndInvalidChars(escapeXMLValue(value, replaceQuotes, assumeWrappingCDATA)) : null;
  }

  public enum EscapableXMLEntity {
    SMALLER_SIGN("lt","<"),
    GREATER_SIGN("gt",">"),
    AMPERSAND("amp","&"),
    APOSTROPHE("apos","'"),
    QUOTE("quot","\"");
    
    public final static String XML_ESCAPE_PREFIX = "&";
    public final static String XML_ESCAPE_SUFFFIX = ";";
    
    private final String escapedRepresentation;
    private final String unescapedRepresentation;
    
    private EscapableXMLEntity(String escapedRepresentation, String unescapedRepresentation) {
      this.escapedRepresentation = escapedRepresentation;
      this.unescapedRepresentation = unescapedRepresentation;
    }
    
    public String getEscapedRepresentation() {
      return escapedRepresentation;
    }
    
    public String getFullEscapedRepresentation() {
      return XML_ESCAPE_PREFIX + escapedRepresentation + XML_ESCAPE_SUFFFIX;
    }
    
    public String getUnescapedRepresentation() {
      return unescapedRepresentation;
    }
    
    
    public static EscapableXMLEntity getEntityByEscapedRepresentation(String escapedRepresentation) {
      for (EscapableXMLEntity entity : values()) {
        if (entity.getEscapedRepresentation().equals(escapedRepresentation)) {
          return entity;
        }
      }
      return null;
    }
    
    
    public static EscapableXMLEntity getEntityByUnescapedRepresentation(char unescapedRepresentation) {
      switch (unescapedRepresentation) {
        case '<' :
          return SMALLER_SIGN;
        case '>' :
          return GREATER_SIGN;
        case '&' :
          return AMPERSAND;
        case '\'' :
          return APOSTROPHE;
        case '"' :
          return QUOTE;
        default :
          return null;
      }
    }
  }
  
  
  public static boolean containsReferencesOf(String xml, String fqReference) throws XPRC_XmlParsingException, Ex_FileAccessException {
    int fqSplitIndex = fqReference.lastIndexOf('.');
    // how else could references look like?
    String xpath = "//*[@ReferenceName=\"" + fqReference.substring(fqSplitIndex + 1) + "\"][@ReferencePath=\"" + fqReference.substring(0, fqSplitIndex) + "\"]";
    
    Document doc = XMLUtils.parseString(xml, false);
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpathObj = factory.newXPath();
    XPathExpression expr;
    try {
      expr = xpathObj.compile(xpath);
    } catch (XPathExpressionException e) {
      throw new XPRC_XmlParsingException(fqReference, e);
    }
    
    Object result;
    try {
      result = expr.evaluate(doc, XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      throw new Ex_FileAccessException(fqReference);
    }
    NodeList nodes = (NodeList) result;
    return nodes.getLength() > 0;
  }


  private static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

  public static XMLInputFactory defaultXmlInputFactory() {
    return xmlInputFactory;
  }


  public static String getRootElementName(InputStream is) throws XMLStreamException {
    XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);
    while (reader.hasNext()) {
      int event = reader.next();
      switch (event) {
        case XMLStreamConstants.START_ELEMENT :
          String fastResult = reader.getLocalName();
          return fastResult;
      }
    }
    return ""; // no root element present
  }


  public static void appendServiceReference(XmlBuilder xml, Pair<Service, StepFunction> serviceWithFunction, boolean includeLabel) {
    Service service = serviceWithFunction.getFirst();
    StepFunction stepFunction = serviceWithFunction.getSecond();

    xml.startElementWithAttributes(EL.SERVICEREFERENCE); {
      xml.addAttribute(ATT.ID, service.getId());
      if (includeLabel) {
        xml.addAttribute(ATT.LABEL, escapeXMLValue(service.getLabel(), true, false));
      }
      
      String referenceName = escapeXMLValue( service.isDOMRef() ? service.getFullServiceName() : service.getServiceName() );
      xml.addAttribute(ATT.REFERENCENAME, referenceName);
      String servicePath = escapeXMLValue(service.getServicePath());
      if ( (servicePath != null) && (servicePath.length() > 0) ) {
        xml.addAttribute(ATT.REFERENCEPATH, servicePath);
      }

      xml.endAttributes();

      // <Source>
      xml.startElementWithAttributes(EL.SOURCE); {
        xml.addAttribute(ATT.REFID, stepFunction.getXmlIdCatchFallback().toString());
      } xml.endAttributesAndElement();

      // <Target>
      xml.startElementWithAttributes(EL.TARGET); {
        xml.addAttribute(ATT.REFID, stepFunction.getXmlIdCatchFallback().toString());
      } xml.endAttributesAndElement();
    } xml.endElement(EL.SERVICEREFERENCE);
  }


  public static List<Element> getFilteredSubElements(Element element, List<String> filter) {
    List<Element> filteredSubElements = new ArrayList<Element>();
    if (element == null) {
      return filteredSubElements;
    }

    NodeList subNodes = element.getChildNodes();
    for (int nodeNo = 0; nodeNo < subNodes.getLength(); nodeNo++) {
      Node subNode = subNodes.item(nodeNo);
      if ( (subNode.getNodeType() == Node.ELEMENT_NODE) && (!filter.contains(subNode.getNodeName())) ) {
        filteredSubElements.add((Element)subNode);
      }
    }

    return filteredSubElements;
  }

  public static void removeChildNodes(Element element) {
    NodeList nl = element.getChildNodes();
    while (nl.getLength() > 0) {
      element.removeChild(nl.item(0));
    }
  }


}
