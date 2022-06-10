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

package com.gip.xyna.xact.filter.monitor.auditFilterComponents;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

public class ComponentBasedAuditFilter extends XMLFilterImpl{

  
  private List<AuditFilterComponent> components;
  
  public ComponentBasedAuditFilter() {
    components = new ArrayList<AuditFilterComponent>();
    
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    try {
      SAXParser parser = parserFactory.newSAXParser();
      setParent(parser.getXMLReader());
    } catch (ParserConfigurationException | SAXException e) {
    }
    
  }
  
  public void addAuditFilterComponent(AuditFilterComponent afc) {
    components.add(afc);
  }
  
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    for(AuditFilterComponent afc : components) {
      afc.startElement(uri, localName, qName, atts);
    }
  }
  
  public void endElement(String uri, String localName, String qName) throws SAXException {
    for(AuditFilterComponent afc : components) {
      afc.endElement(uri, localName, qName);
    }
  }
  
  public void characters(char[] ch, int start, int length) throws SAXException {
    for(AuditFilterComponent afc : components) {
      afc.characters(ch, start, length);
    }
  }
}
