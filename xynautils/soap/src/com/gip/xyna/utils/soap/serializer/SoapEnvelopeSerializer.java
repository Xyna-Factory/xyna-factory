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
package com.gip.xyna.utils.soap.serializer;

import com.gip.xyna.utils.xml.XMLUtils;
import com.gip.xyna.utils.xml.serializer.XMLSerializer;

import java.io.StringReader;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class SoapEnvelopeSerializer implements XMLSerializer { //TODO unterst�tzt nur soap1.1

  private SoapEnvelope soapEnv;

  protected static final String NS_SOAP =
    "http://schemas.xmlsoap.org/soap/envelope/";

  private static final String ELEMENT_ENVELOPE = "Envelope";
  private static final String ELEMENT_HEADER = "Header";
  private static final String ELEMENT_BODY = "Body";
  
  public SoapEnvelopeSerializer(String xml) throws Exception {
    soapEnv = new SoapEnvelope();
    SoapBody body = new SoapBody();
    soapEnv.setBody(body);

    //xml parsen
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xml));
    XMLDocument doc = parser.getDocument();
    NodeList nl = doc.getChildrenByTagName(ELEMENT_ENVELOPE, NS_SOAP);
    if (nl.getLength() == 0) {
      throw new Exception("SOAP Envelope not found in XML. XML starts with: " + xml.substring(0, Math.min(200, xml.length())));
    }
    XMLElement env = (XMLElement)nl.item(0);
    nl = env.getChildrenByTagName(ELEMENT_HEADER, NS_SOAP);
    //header ist optional und wird erstmal nicht weiter verwendet...

    nl = env.getChildrenByTagName(ELEMENT_BODY, NS_SOAP);
    if (nl.getLength() == 0) {
      throw new Exception("SOAP Envelope is missing its Body Element");
    }
    XMLElement bodyEl = (XMLElement)nl.item(0);
    //FIXME nur ein payload element ausreichend??
    if (bodyEl.getChildNodes().getLength() == 0) {
      body.setXMLPayload("");
    } else {
      body.setXMLPayload(XMLUtils.getContentWithTags(bodyEl));
    }
  }

  public SoapEnvelopeSerializer(SoapEnvelope se) {
    soapEnv = se;
  }

  public String toXMLString() throws Exception {
    XMLDocument doc = new XMLDocument();
    Element env = doc.createElementNS(NS_SOAP, ELEMENT_ENVELOPE);
    Element bodyEl = doc.createElementNS(NS_SOAP, ELEMENT_BODY);
    doc.appendChild(env);
    env.appendChild(bodyEl);
    XMLElement payloadEl = XMLUtils.parseAndImport(soapEnv.getBody().getXMLPayload(), doc);
    if (payloadEl != null) {
      bodyEl.appendChild(payloadEl);
    }

    // toString
    return XMLUtils.getXMLElementAsString(doc);
  }

  public SoapEnvelope toBean() {
    return soapEnv;
  }
}
