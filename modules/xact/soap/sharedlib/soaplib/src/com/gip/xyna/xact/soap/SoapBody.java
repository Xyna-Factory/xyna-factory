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
package com.gip.xyna.xact.soap;

import org.w3c.dom.Element;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class SoapBody {
  
  private String xmlString;
  private final Element body;
  
  private SoapFault fault;

  public static SoapBody fromXMLStringBody(String xmlStringBody) throws XPRC_XmlParsingException {
    try {
      Element bodyEl = XMLUtils.parseString(xmlStringBody, true).getDocumentElement();
      SoapBody ret = new SoapBody(bodyEl);
      ret.xmlString = xmlStringBody;
      return ret;
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException();
    }
  }
  
  public static SoapBody fromXMLStringPayload(String payloadXML) throws XPRC_XmlParsingException {
    try {
      StringBuilder xml = new StringBuilder();
      xml.append("<").append(SoapEnvelope.ELEMENT_BODY).append(" xmlns=\"").append(SoapEnvelope.NS_SOAP).append("\" >\n");
      xml.append(payloadXML);
      xml.append("</").append(SoapEnvelope.ELEMENT_BODY).append(">");
      
      return new SoapBody(XMLUtils.parseString(xml.toString(), true).getDocumentElement());
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException();
    }
  }

  public SoapBody(Element body) {
    this.body = body;
  }

  public String getXMLString() {
    if (xmlString == null) {
      if (body == null) {
        throw new RuntimeException("body is empty");
      }
      xmlString = XMLUtils.getXMLString(body, false);
    }
    return xmlString;
  }

  /**
   *  gibt das element vom body zurück
   */
  public Element getAsDom() {
    return body;
  }
  
  public SoapFault getFault() {
    checkForFault();
    return fault;
  }


  private void checkForFault() {
    if (fault == null) {
      Element faultElement = XMLUtils.getChildElementByName(body, "Fault", SoapEnvelope.NS_SOAP);
      if (faultElement != null) {
        fault = new SoapFault(faultElement);
      }
    }
  }


  public void setFault(SoapFault fault) {
    this.fault = fault;
  }

}
