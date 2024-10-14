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
package com.gip.xyna.utils.soap.serializer;

import com.gip.xyna.utils.xml.XMLUtils;
import com.gip.xyna.utils.xml.serializer.XMLSerializer;

import com.gip.xyna.utils.xml.serializer.XynaOrderSerializer;

import java.io.StringReader;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.NodeList;

/**
 * siehe http://www.w3.org/TR/2000/NOTE-SOAP-20000508/#_Toc478383507
 */
public class SoapFaultSerializer implements XMLSerializer {


  private SoapFault soapFault;

  public SoapFaultSerializer(String xml) throws Exception {
    String faultCode = null;
    String faultstring = null;
    String detail = null;
    String actor = null;
  
    //xml parsen
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xml));
    XMLDocument doc = parser.getDocument();
    
    NodeList nl = doc.getChildrenByTagName("Fault", SoapEnvelopeSerializer.NS_SOAP);
    if (nl.getLength() == 0) {
      throw new Exception("SOAP Fault not found in XML.");
    }
    XMLElement fault = (XMLElement)nl.item(0);
    nl = fault.getChildrenByTagName("faultcode");
    if (nl.getLength() == 0) {
      throw new Exception("faultcode is missing from Soap Fault");
    }
    faultCode = XMLUtils.getContentWithTags((XMLElement)nl.item(0));

    nl = fault.getChildrenByTagName("faultstring");
    if (nl.getLength() == 0) {
      throw new Exception("faultstring is missing from Soap Fault");
    }    
    faultstring = XMLUtils.getContentWithTags((XMLElement)nl.item(0));

    nl = fault.getChildrenByTagName("detail");
    if (nl.getLength() == 0) {  //details sind optional
      //throw new Exception("detail is missing from Soap Fault");
    } else {
      detail = XMLUtils.getContentWithTags((XMLElement)nl.item(0));
    }
    
    nl = fault.getChildrenByTagName("faultActor");
    if (nl.getLength() == 0) {  //actor ist optional
      //throw new Exception("detail is missing from Soap Fault");
    } else {
      actor = XMLUtils.getContentWithTags((XMLElement)nl.item(0));
    }
    
    soapFault = new SoapFault(faultCode, faultstring, detail, actor);
  }

  public String toXMLString() throws Exception {
    throw new Exception("not supported");
  }

  public SoapFault toBean() {
    return soapFault;
  }
}
