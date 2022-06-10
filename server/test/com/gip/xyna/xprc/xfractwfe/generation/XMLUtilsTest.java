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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.xml.XMLConstants;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;


public class XMLUtilsTest extends TestCase {
  
  private static final String XML_ONLY_ROOT = "<myxml />";
  private static final String XML_ONE_CHILD = "<myxml><child /></myxml>";
  private static final String XML_ONE_CHILD_AND_COMMENT = "<myxml><child /><!-- comment --></myxml>";
  private static final String XML_ONE_GRANDCHILD = "<myxml><child><grandchild /></child></myxml>";
  private static final String XML_NS = "<myxml xmlns=\"bla\"><child><ns1:grandchild xmlns:ns1=\"blubb\"/></child></myxml>";
  
  public void testGetChildElements() throws XynaException {
    //methode soll keine probleme mit nicht-elementen haben
    Document doc = XMLUtils.parseString(XML_ONE_CHILD_AND_COMMENT);
    List<Element> children = XMLUtils.getChildElements(doc.getDocumentElement());
    assertEquals("1 child expected", 1, children.size());
    //methode soll keine kindeskinder liefern
    doc = XMLUtils.parseString(XML_ONE_GRANDCHILD);
    children = XMLUtils.getChildElements(doc.getDocumentElement());
    assertEquals("1 child expected", 1, children.size());
    //methode soll mit root und nicht-root funktionieren
    children = XMLUtils.getChildElements(children.get(0));
    assertEquals("1 grandchild expected", 1, children.size());
    //methode soll kein problem mit 0 subelementen haben
    children = XMLUtils.getChildElements(children.get(0));
    assertEquals("0 greatgrandchildren expected", 0, children.size());
    doc = XMLUtils.parseString(XML_ONLY_ROOT);
    children = XMLUtils.getChildElements(doc.getDocumentElement());
    assertEquals("no children expected", 0, children.size());
    //namespace-safety
    doc = XMLUtils.parseString(XML_NS);
    children = XMLUtils.getChildElements(doc.getDocumentElement());
    assertEquals("1 child expected", 1, children.size());    
    children = XMLUtils.getChildElements(children.get(0));
    assertEquals("1 grandchild expected", 1, children.size());    
  }
  
  private static final String XML_NORMAL_TEXT = "<myxml>test Text</myxml>";
  private static final String XML_DOUBLE_TEXT = "<myxml>test Text<child>test2 Text</child></myxml>";
  private static final String XML_PARALLEL_TEXTNODES = "<myxml>test Text<child>test2 Text></child>test3 Text</myxml>";
  private static final String XML_LINEBREAK_TEXT = "<myxml>test\nText</myxml>";
  private static final String XML_UNTRIMMED_TEXT = "<myxml> test\n</myxml>";
  private static final String XML_SPECIAL_CHARS = "<myxml>>>&lt;&gt;&amp;</myxml>";
  
  
  public void testGetTextContent() throws XynaException {
    Document doc = XMLUtils.parseString(XML_NORMAL_TEXT);
    String text = XMLUtils.getTextContent(doc.getDocumentElement());
    assertEquals("expected different TextContent", "test Text", text);
    
    doc = XMLUtils.parseString(XML_DOUBLE_TEXT);
    text = XMLUtils.getTextContent(doc.getDocumentElement());
    assertEquals("expected different TextContent", "test Text", text);
    Element el = XMLUtils.getChildElementByName(doc.getDocumentElement(), "child");
    text = XMLUtils.getTextContent(el);
    assertEquals("expected different TextContent", "test2 Text", text);
    
    doc = XMLUtils.parseString(XML_PARALLEL_TEXTNODES);
    text = XMLUtils.getTextContent(doc.getDocumentElement());
    assertEquals("expected different TextContent", "test Texttest3 Text", text);
    
    doc = XMLUtils.parseString(XML_LINEBREAK_TEXT);
    text = XMLUtils.getTextContent(doc.getDocumentElement());
    assertEquals("expected different TextContent", "test\nText", text);
    
    doc = XMLUtils.parseString(XML_UNTRIMMED_TEXT);
    text = XMLUtils.getTextContent(doc.getDocumentElement());
    assertEquals("expected different TextContent", " test\n", text);
    
    doc = XMLUtils.parseString(XML_SPECIAL_CHARS);
    text = XMLUtils.getTextContent(doc.getDocumentElement());
    assertEquals("expected different TextContent", ">><>&", text);
  }
  
  public void testSetTextContent() throws XynaException {
    Document doc = XMLUtils.parseString(XML_ONLY_ROOT);
    XMLUtils.setTextContent(doc.getDocumentElement(), "test Text");
    assertEquals("expected different TextContent", "test Text", XMLUtils.getTextContent(doc.getDocumentElement()));
    
    doc = XMLUtils.parseString(XML_NORMAL_TEXT);
    XMLUtils.setTextContent(doc.getDocumentElement(), "test Text");
    assertEquals("expected different TextContent", "test Text", XMLUtils.getTextContent(doc.getDocumentElement()));
    
    doc = XMLUtils.parseString(XML_PARALLEL_TEXTNODES);
  }


  public void testParseMultipleThreads() throws InterruptedException {

    int NUMBER_OF_THREADS = 10;
    final CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);

    Runnable r = new Runnable() {

      public void run() {
        try {
          Thread t = Thread.currentThread();
          for (int i = 0; i < 5000; i++) {

            Document doc;
            String text; 

            switch (i % 4) {
              case 0 :
                try {
                  doc = XMLUtils.parseString(XML_NORMAL_TEXT);
                } catch (XPRC_XmlParsingException e) {
                  fail("parsing failed");
                  return;
                }
                text = XMLUtils.getTextContent(doc.getDocumentElement());
                assertEquals("expected different TextContent", "test Text", text);
                break;

              case 1 :
                try {
                  doc = XMLUtils.parseString(XML_DOUBLE_TEXT);
                } catch (XPRC_XmlParsingException e) {
                  fail("parsing failed");
                  return;
                }
                text = XMLUtils.getTextContent(doc.getDocumentElement());
                assertEquals("expected different TextContent", "test Text", text);
                Element el = XMLUtils.getChildElementByName(doc.getDocumentElement(), "child");
                text = XMLUtils.getTextContent(el);
                assertEquals("expected different TextContent", "test2 Text", text);
                break;

              case 2 :
                try {
                  doc = XMLUtils.parseString(XML_PARALLEL_TEXTNODES);
                } catch (XPRC_XmlParsingException e) {
                  fail("parsing failed");
                  return;
                }
                text = XMLUtils.getTextContent(doc.getDocumentElement());
                assertEquals("expected different TextContent", "test Texttest3 Text", text);
                break;

              case 3 :
                try {
                  doc = XMLUtils.parseString(XML_LINEBREAK_TEXT);
                } catch (XPRC_XmlParsingException e) {
                  fail("parsing failed");
                  return;
                }
                text = XMLUtils.getTextContent(doc.getDocumentElement());
                assertEquals("expected different TextContent", "test\nText", text);
                break;
              default :
                throw new RuntimeException();
            }
 
          }
        } catch (Exception e) {
          e.printStackTrace();
          fail(e.getMessage());
        } finally {
          latch.countDown();
        }
      }
    };

    for (int i=0; i<NUMBER_OF_THREADS; i++) {
      Thread t = new Thread(r);
      t.setName("Thread-" + i);
      t.start();
    }

    latch.await();

  }


  public void testXMLRenderer() throws XPRC_XmlParsingException {
    String xmlString = "<A>\n" + "  <B/>\n" + "</A>\n";
    Document d = XMLUtils.parseString(xmlString);
    printTree(d.getDocumentElement(), "");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStreamWriter w = new OutputStreamWriter(baos);
    XMLUtils.saveDomToWriter(w, d, false);
    assertEquals(xmlString, baos.toString());
  }

  private void printTree(Element element, String indent) {
    String s = indent + "<" + element.getTagName() + ">";
    NodeList childNodes = element.getChildNodes();
    for (int i = 0; i<childNodes.getLength(); i++) {
      Node c = childNodes.item(i);
      s += "[" + c.getClass().getSimpleName() + c.getNodeValue() + "]";
    }
    System.out.println(s);
    for (Element e : XMLUtils.getChildElements(element)) {
      printTree(e, indent + "  ");
    }
  }

}
