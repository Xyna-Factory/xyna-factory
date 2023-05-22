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

import com.gip.xyna._1_5.xsd.common._1.CancelAttributes;
import com.gip.xyna._1_5.xsd.common._1.CancelOrder;

import com.gip.xyna.utils.xml.XMLUtils;

import java.io.StringReader;
import java.io.StringWriter;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CancelOrderSerializer implements XMLSerializer {
  
  private static final String CANCELORDER = "CancelOrder";
  private static final String THIS = "This";
  private static final String ALL = "All";
  private static final String CANCELATTRIBUTES = "CancelAttributes";
  private static final String EXTERNALORDERNUMBER = "ExternalOrderNumber";

  private CancelOrder co;
  //private static Logger logger = Logger.getLogger(CancelOrderSerializer.class.getName());

  public CancelOrderSerializer(String xml) throws Exception {
    co = new CancelOrder();
    
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xml));
    XMLDocument doc = parser.getDocument();
    NodeList coNl = doc.getElementsByTagNameNS(XynaOrderSerializer.NS_XCOM, CANCELORDER);
    if (coNl.getLength() > 0) {
      XMLElement el = (XMLElement)coNl.item(0);
      NodeList ca = el.getChildrenByTagName(CANCELATTRIBUTES, XynaOrderSerializer.NS_XCOM);
      if (ca.getLength() > 0) {
        el = (XMLElement)ca.item(0);
        CancelAttributes catt = new CancelAttributes();      
        co.setCancelAttributes(catt);        
        //keine nullpointerexception laut api muss getattribute leeren string zur�ckgeben, falls es nicht vorhanden ist
        catt.set_this(el.getAttribute(THIS).equalsIgnoreCase("true")); 
        //logger.debug("this = " + catt.is_this());
        catt.setAll(el.getAttribute(ALL).equalsIgnoreCase("true"));
        //logger.debug("all = " + catt.isAll());
        catt.setExternalOrderNumber(el.getAttribute(EXTERNALORDERNUMBER));
        //logger.debug("extordernumb = " + catt.getExternalOrderNumber());
      } else {
        throw new Exception("Deserialisierung fehlgeschlagen. Element " + CANCELATTRIBUTES + " nicht gefunden.");
      }
    } else {
      throw new Exception("Deserialisierung fehlgeschlagen. Element " + CANCELORDER + " nicht gefunden."); //TODO xynastatus ist hier laut xsd auch erlaubt
    }
  }

  public CancelOrderSerializer(CancelOrder co) {
    this.co = co;
  }

  public String toXMLString() throws Exception {
    XMLDocument doc = new XMLDocument();
    Element coEl = doc.createElementNS(XynaOrderSerializer.NS_XCOM, CANCELORDER);
    doc.appendChild(coEl);

    if (co.getCancelAttributes() != null) {
      Element cancelAttEl = doc.createElementNS(XynaOrderSerializer.NS_XCOM, CANCELATTRIBUTES);
      coEl.appendChild(cancelAttEl);
      XMLUtils.addAttributeIfNotNullOrEmpty(cancelAttEl, EXTERNALORDERNUMBER, co.getCancelAttributes().getExternalOrderNumber());
      XMLUtils.addAttributeIfNotNullOrEmpty(cancelAttEl, THIS, "" + co.getCancelAttributes().is_this());
      XMLUtils.addAttributeIfNotNullOrEmpty(cancelAttEl, ALL, "" + co.getCancelAttributes().isAll());
    } else if (co.getXynaStatus() != null) {
      Element xsEl = doc.createElementNS(XynaOrderSerializer.NS_XCOM, "XynaStatus");
      coEl.appendChild(xsEl);
      XMLUtils.addAttributeIfNotNullOrEmpty(xsEl, "Status", co.getXynaStatus().getStatus());
      XMLUtils.addAttributeIfNotNullOrEmpty(xsEl, "StatusReferenceNumber", co.getXynaStatus().getStatusReferenceNumber());
      XMLUtils.addAttributeIfNotNullOrEmpty(xsEl, "Information", co.getXynaStatus().getInformation());
    } else {
      throw new Exception("CancelOrder kann nicht in XML umgewandelt werden, da CancelAttributes und XynaStatus nicht beide null sein d�rfen.");
    }
    //toString
    StringWriter sw = new StringWriter();
    doc.print(sw);
    return sw.toString();
  }

  public CancelOrder toBean() throws Exception {
    return co;
  }
}
