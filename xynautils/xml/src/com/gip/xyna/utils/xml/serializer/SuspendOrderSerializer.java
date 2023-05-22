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
package com.gip.xyna.utils.xml.serializer;

import com.gip.xyna._1_5.xsd.common._1.SuspendOrder;

import java.io.StringReader;
import java.io.StringWriter;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SuspendOrderSerializer implements XMLSerializer {
  private static final String SUSPENDORDER = "SuspendOrder";
  private static final String EXTORDERID = "ExternalOrderNumber";
  private static final String CAUSE = "Cause";
  private static final String RELEASECAPS = "releaseCapsImmediately";

  private SuspendOrder so;

  public SuspendOrderSerializer(String xml) throws Exception {
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xml));
    XMLDocument doc = parser.getDocument();
    NodeList rootEl = doc.getChildrenByTagName(SUSPENDORDER, XynaOrderSerializer.NS_XCOM);
    if (rootEl.getLength() == 0) {
      throw new Exception("Element {" + XynaOrderSerializer.NS_XCOM + "}" + SUSPENDORDER +
          " nicht gefunden.");
    }

    so = new SuspendOrder();
    so.setExternalOrderNumber(((XMLElement)rootEl.item(0)).getAttribute(EXTORDERID));
    so.setCause(((XMLElement)rootEl.item(0)).getAttribute(CAUSE));
    String relCaps = ((XMLElement)rootEl.item(0)).getAttribute(RELEASECAPS);
    if (relCaps == null || relCaps.length() == 0) {
      so.setReleaseCapsImmediately(false);
    } else {
      so.setReleaseCapsImmediately(Boolean.valueOf(relCaps));
    }
  }

  public SuspendOrderSerializer(SuspendOrder ro) {
    this.so = ro;
  }

  public String toXMLString() throws Exception {
    XMLDocument doc = new XMLDocument();
    Element resumeOrder = doc.createElementNS(XynaOrderSerializer.NS_XCOM, SUSPENDORDER);
    doc.appendChild(resumeOrder);
    if (so.getExternalOrderNumber() == null) {
      throw new Exception("ExternalOrderId is a required attribute");
    }
    resumeOrder.setAttribute(EXTORDERID, so.getExternalOrderNumber());
    if (so.getCause() == null) {
      throw new Exception("Cause is a required attribute");
    }
    resumeOrder.setAttribute(CAUSE, so.getCause());
    resumeOrder.setAttribute(RELEASECAPS, "" + so.isReleaseCapsImmediately());

    //toString
    StringWriter sw = new StringWriter();
    doc.print(sw);
    return sw.toString();
  }

  public SuspendOrder toBean() {
    return so;
  }
}
