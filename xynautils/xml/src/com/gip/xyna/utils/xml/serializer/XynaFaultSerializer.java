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
package com.gip.xyna.utils.xml.serializer;



import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;

import com.gip.xyna.utils.xml.XMLUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;



public class XynaFaultSerializer implements XMLSerializer {

  private static final String NS_FAULTS = "http://www.gip.com/xyna/1.5/xsd/faults/1.0";
  private static final String CODE = "Code";
  private static final String SUMMARY = "Summary";
  private static final String DETAILS = "Details";
  private static final String XYNAFAULT = "XynaFault";
  private XynaFault_ctype xf;
  private String xml;


  public XynaFaultSerializer(String xml) throws Exception {
    this.xml = xml;
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xml));
    XMLDocument doc = parser.getDocument();
    NodeList xol = doc.getChildrenByTagName(XYNAFAULT, NS_FAULTS);
    if (xol.getLength() != 1) {
      throw new Exception("XynaFault hat ungültiges Format. Element XynaFault nicht gefunden.");
    }
    xf = new XynaFault_ctype();
    XMLElement xfel = (XMLElement) xol.item(0);
    xf.setCode(getChildValueByTagNameNS(xfel, CODE, NS_FAULTS, false));
    xf.setDetails(getChildValueByTagNameNS(xfel, DETAILS, NS_FAULTS, false));
    xf.setSummary(getChildValueByTagNameNS(xfel, SUMMARY, NS_FAULTS, false));
  }


  private String getChildValueByTagNameNS(XMLElement el, String name, String ns, boolean childIsOptional)
                  throws Exception {
    NodeList xol = el.getChildrenByTagName(name, ns);
    if (xol.getLength() == 0) {
      if (childIsOptional) {
        return null;
      }
      throw new Exception("Benötigtes Kindelement {" + ns + "}" + name + " nicht gefunden.");
    }
    return ((XMLElement) xol.item(0)).getTextContent();
  }


  public XynaFaultSerializer(XynaFault_ctype xf) {
    this.xf = xf;
  }


  public String toXMLString() throws IOException {
    if (xml == null) {
      XMLDocument doc = new XMLDocument();
      Element xfel = doc.createElementNS(NS_FAULTS, XYNAFAULT);
      doc.appendChild(xfel);
      XMLElement code = (XMLElement) doc.createElementNS(NS_FAULTS, CODE);
      XMLElement details = (XMLElement) doc.createElementNS(NS_FAULTS, DETAILS);
      XMLElement summary = (XMLElement) doc.createElementNS(NS_FAULTS, SUMMARY);
      xfel.appendChild(code);
      xfel.appendChild(summary);
      xfel.appendChild(details);

      code.setTextContent(xf.getCode());
      details.setTextContent(xf.getDetails());
      summary.setTextContent(xf.getSummary());
      // toString
      return XMLUtils.getXMLElementAsString(doc);
    }
    return xml;
  }


  public XynaFault_ctype toBean() {
    return xf;
  }
}
