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
package com.gip.xyna.xact.soap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

/*
 * http://www.w3.org/TR/2000/NOTE-SOAP-20000508/
 */
public class SoapEnvelope {
  protected static final String NS_SOAP =
      "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String ELEMENT_ENVELOPE = "Envelope";
    protected static final String ELEMENT_HEADER = "Header";
    protected static final String ELEMENT_BODY = "Body";
    
  private SoapBody body;
  private SoapHeader header;
  
  public SoapBody getBody() {
    return body;
  }
  
  public void setBody(SoapBody body) {
    this.body = body;
  }
  
  public SoapHeader getHeader() {
    return header;
  }
  
  public void setHeader(SoapHeader header) {
    this.header = header;
  }
  
  public static SoapEnvelope fromXMLString(String soapEnvXML) throws XPRC_XmlParsingException {
    try {
      Document doc = XMLUtils.parseString(soapEnvXML, true);
      Element el = doc.getDocumentElement();
      if (!(el.getLocalName().equals(ELEMENT_ENVELOPE) && el.getNamespaceURI().equals(NS_SOAP))) {
        throw new XPRC_XmlParsingException(soapEnvXML); //FIXME bessere exception
      }
      
      SoapEnvelope ret = new SoapEnvelope();

      //body
      Element body = XMLUtils.getChildElementByName(el, ELEMENT_BODY, NS_SOAP);      
      if (body == null) {
        throw new XPRC_XmlParsingException(soapEnvXML); //FIXME bessere exception
      }
      
      //header optional
      Element header = XMLUtils.getChildElementByName(el, ELEMENT_HEADER, NS_SOAP);
            
      ret.setBody(new SoapBody(body));
      
      if (header != null) {
        ret.setHeader(new SoapHeader(header));
      }
      
      return ret;
      
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String getXMLString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<").append(ELEMENT_ENVELOPE).append(" xmlns=\"").append(NS_SOAP).append("\" >\n");
    sb.append(body.getXMLString());
    if (header != null) {
      sb.append(header.getXMLString());
    }
    sb.append("</").append(ELEMENT_ENVELOPE).append(">");
    return sb.toString();
  }
  
}
