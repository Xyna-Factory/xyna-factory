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

package com.gip.xyna.xact.filter.monitor.auditpreprocessing;



import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;



public class RemoveOperationTagFilter extends XMLFilterImpl {

  private boolean insideAuditTag;


  public RemoveOperationTagFilter(XMLReader xmlReader) {
    super(xmlReader);
    insideAuditTag = false;
  }


  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

    if (qName.equals(EL.OPERATION)) {
      insideAuditTag = true;
    }
    if (!insideAuditTag) {
      super.startElement(uri, localName, qName, atts);
    }
  }


  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {

    if (!insideAuditTag) {
      super.endElement(uri, localName, qName);
    }

    if (qName.equals(EL.OPERATION)) {
      insideAuditTag = false;
    }

  }


  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (!insideAuditTag) {
      super.characters(ch, start, length);
    }
  }
  
  
  public static String filter(String xml) throws ParserConfigurationException, SAXException, TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    SAXParser parser = parserFactory.newSAXParser();
    XMLReader xr = new RemoveOperationTagFilter(parser.getXMLReader());
    Source src = new SAXSource(xr, new InputSource(new StringReader(xml)));

    ByteArrayOutputStream filteredXmlStream = new ByteArrayOutputStream();
    javax.xml.transform.Result res = new StreamResult(filteredXmlStream);
    TransformerFactory.newInstance().newTransformer().transform(src, res);

    return filteredXmlStream.toString();
  }
}
