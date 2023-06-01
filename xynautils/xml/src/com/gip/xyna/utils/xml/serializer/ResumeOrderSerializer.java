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

import com.gip.xyna._1_5.xsd.common._1.ResumeOrder;

import java.io.StringReader;
import java.io.StringWriter;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ResumeOrderSerializer implements XMLSerializer {

  private static final String RESUMEORDER = "ResumeOrder";
  private static final String EXTORDERID = "ExternalOrderNumber";
  private static final String SUSP_CAUSE = "SuspendCause";

  private ResumeOrder ro;

  public ResumeOrderSerializer(String xml) throws Exception {
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xml));
    XMLDocument doc = parser.getDocument();
    NodeList rootEl = doc.getChildrenByTagName(RESUMEORDER, XynaOrderSerializer.NS_XCOM);
    if (rootEl.getLength() == 0) {
      throw new Exception("Element {" + XynaOrderSerializer.NS_XCOM + "}" + RESUMEORDER +
          " nicht gefunden.");
    }

    ro = new ResumeOrder();
    ro.setExternalOrderNumber(((XMLElement)rootEl.item(0)).getAttribute(EXTORDERID));
    ro.setSuspendCause(((XMLElement)rootEl.item(0)).getAttribute(SUSP_CAUSE));
  }

  public ResumeOrderSerializer(ResumeOrder ro) {
    this.ro = ro;
  }

  public String toXMLString() throws Exception {
    XMLDocument doc = new XMLDocument();
    Element resumeOrder = doc.createElementNS(XynaOrderSerializer.NS_XCOM, RESUMEORDER);
    doc.appendChild(resumeOrder);
    if (ro.getExternalOrderNumber() != null) {
      resumeOrder.setAttribute(EXTORDERID, ro.getExternalOrderNumber());
    }
    if (ro.getSuspendCause() != null) {
      resumeOrder.setAttribute(SUSP_CAUSE, ro.getSuspendCause());
    }

    //toString
    StringWriter sw = new StringWriter();
    doc.print(sw);
    return sw.toString();
  }

  public ResumeOrder toBean() {
    return ro;
  }
}
