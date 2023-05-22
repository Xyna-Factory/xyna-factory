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

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Diese Klasse kann benutzt werden um auf Kommandozeile XPATHs auszuwerten.
 */
public class XPathUtil {
  public XPathUtil() {
  }

  public static void main(String[] args) {
    try {
      if (args.length != 2) {
        System.out.println("Usage: XPathUtil (<FileName of XML> | <XML String>) <XPath Expression>");
        return;
      }
      String xpath = args[1];
      String xml = args[0];
      DocumentBuilder db =
        DocumentBuilderFactory.newInstance().newDocumentBuilder();
      db.setErrorHandler(new ErrorHandler() { //dont show errors in system.out yet
            public void error(SAXParseException exception) {
            }

            public void fatalError(SAXParseException exception) {
            }

            public void warning(SAXParseException exception) {
            }
          });
      Document d;
      try {
        //try to parse as file
        d = db.parse(xml);
      }
      catch (Exception e) {
        //try to parse as xml directly
        try {
          d = db.parse(new InputSource(new StringReader(xml)));
        }
        catch (Exception f) {
          throw new Exception(args[0] +
              " is neither a valid XMLFile (" +
              e.getClass().getSimpleName() + " - " +
              e.getMessage() + ") nor a valid XML (" +
              f.getClass().getSimpleName() + " - " +
              f.getMessage() + ").");
        }
      }
      XPath xp = XPathFactory.newInstance().newXPath();
      Node node = (Node)xp.evaluate(xpath, d, XPathConstants.NODE);
      if (node == null) {
        throw new Exception("Node not found");
      }
      if (node.getNodeType() == Node.ATTRIBUTE_NODE) {        
        System.out.println(">" + node.getNodeValue().trim() + "<");
      } else if (node.getNodeType() == Node.ELEMENT_NODE) {
        System.out.println(">" + node.getChildNodes().item(0).getNodeValue().trim() + "<");
      } else {
        throw new Exception("unimplemented object found: " +
            node.getClass().getSimpleName());
      }
    }
    catch (Exception e) {
      System.out.println("XPATH-FAILURE: " + e.getClass().getSimpleName() + " " + e.getMessage());
      e.printStackTrace();
    }
  }
}
