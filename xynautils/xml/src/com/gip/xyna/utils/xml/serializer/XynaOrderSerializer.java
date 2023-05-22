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

import com.gip.xyna._1_5.xsd.common._1.XynaHeader_ctype;
import com.gip.xyna.utils.xml.XMLUtils;

import java.io.StringReader;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import javax.xml.soap.SOAPElement;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XynaOrderSerializer implements XMLSerializer {

  public static final String NS_XCOM = "http://www.gip.com/xyna/1.5/xsd/common/1.2";
  private static final String CREATIONDATE = "CreationDate";
  private static final String STATUS = "Status";
  private static final String DEPARTMENT = "Department";
  private static final String DEPENDENEC = "Dependence";
  private static final String INFORMATION = "Information";
  private static final String ORDERNUMBER = "OrderNumber";
  private static final String ORDERTYPE = "OrderType";
  private static final String ORDERTYPEVERSION = "OrderTypeVersion";
  private static final String PRIORITY = "Priority";
  private static final String PROCESSID = "ProcessId";
  private static final String REFERENCE = "Reference";
  private static final String SOURCE = "Source";
  private static final String STARTTIMESLOT = "StartTimeSlot";
  private static final String STARTTIME = "StartTime";

  private String payloadAsXMLString;
  private XynaHeader_ctype xh;


  // Konstruktoren nehmen XynaHeader anstelle von XynaOrder, weil die XynaOrder �berschrieben sein kann
  // und sich die damit generierten Klassen evtl unterscheiden k�nnen
  public XynaOrderSerializer(XynaHeader_ctype xh, String payloadAsXMLString) throws Exception {
    this.payloadAsXMLString = payloadAsXMLString;
    this.xh = xh;
  }


  public XynaOrderSerializer(String xynaOrderAsXMLString) throws Exception {
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xynaOrderAsXMLString));
    XMLDocument doc = parser.getDocument();
    NodeList xol = doc.getChildrenByTagName("XynaOrder", NS_XCOM);
    if (xol.getLength() < 1) {
      xol = doc.getChildNodes();
      throw new Exception("XynaOrder hat ung�ltiges Format (XynaOrder Element nicht gefunden): " + xynaOrderAsXMLString);
    }
    // elemente der kinder raussuchen    
    ArrayList<Node> childElements = XMLUtils.getChildrenByType((XMLElement)xol.item(0), Node.ELEMENT_NODE);
    if (childElements.size() < 2) {
      throw new Exception("XynaOrder hat ung�ltiges Format (Fehlende Payload): " + xynaOrderAsXMLString);
    }
    XMLElement payload = (XMLElement)childElements.get(1);
    payloadAsXMLString = XMLUtils.getXMLElementAsString(payload);
    xh = new XynaHeader_ctype();
    XMLElement header = (XMLElement)childElements.get(0); // TODO validation, dass das auch wirklich der header ist
    xh.setCreationDate(header.getAttribute(CREATIONDATE));
    xh.setDepartment(header.getAttribute(DEPARTMENT));
    xh.setDependence(header.getAttribute(DEPENDENEC));
    xh.setInformation(header.getAttribute(INFORMATION));
    xh.setOrderNumber(header.getAttribute(ORDERNUMBER));
    xh.setOrderType(header.getAttribute(ORDERTYPE));
    xh.setOrderTypeVersion(header.getAttribute(ORDERTYPEVERSION));
    xh.setPriority(header.getAttribute(PRIORITY));
    xh.setProcessId(header.getAttribute(PROCESSID));
    xh.setReference(header.getAttribute(REFERENCE));
    xh.setSource(header.getAttribute(SOURCE));
    xh.setStartTime(header.getAttribute(STARTTIME));
    xh.setStartTimeSlot(header.getAttribute(STARTTIMESLOT));
    xh.setStatus(header.getAttribute(STATUS));
  }


  public XynaOrderSerializer(XynaHeader_ctype xh, XMLSerializer payloadSerializer) throws Exception {
    this(xh, payloadSerializer.toXMLString());
  }


  public XynaOrderSerializer(XynaHeader_ctype xh, SOAPElement payload) throws Exception {
    this(xh, (XMLElement) payload);
  }


  public XynaOrderSerializer(XynaHeader_ctype xh, XMLElement payload) throws Exception {
    this(xh, XMLUtils.getXMLElementAsString(payload));
  }


  public String toXMLString() throws Exception {
    XMLDocument doc = new XMLDocument();
    Element xoEl = doc.createElementNS(NS_XCOM, "XynaOrder");
    Element xhEl = doc.createElementNS(NS_XCOM, "XynaHeader");
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, CREATIONDATE, xh.getCreationDate());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, DEPARTMENT, xh.getDepartment());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, DEPENDENEC, xh.getDependence());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, INFORMATION, xh.getInformation());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, ORDERNUMBER, xh.getOrderNumber());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, ORDERTYPE, xh.getOrderType());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, ORDERTYPEVERSION, xh.getOrderTypeVersion());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, PRIORITY, xh.getPriority());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, PROCESSID, xh.getProcessId());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, REFERENCE, xh.getReference());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, SOURCE, xh.getSource());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, STARTTIMESLOT, xh.getStartTimeSlot());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, STARTTIME, xh.getStartTime());
    XMLUtils.addAttributeIfNotNullOrEmpty(xhEl, STATUS, xh.getStatus());
    xoEl.appendChild(xhEl);

    // wichtig, damit namespace-prefixes stimmen. (payload k�nnte ansonsten gleiche prefixes benutzen)
    XMLElement payloadEl = XMLUtils.parseAndImport(payloadAsXMLString, doc);
    if (payloadEl == null) {
      throw new Exception("XML der Payload darf nicht leer sein.");
    }
    xoEl.appendChild(payloadEl);

    doc.appendChild(xoEl);
    // toString
    return XMLUtils.getXMLElementAsString(doc);
  }


  public XynaHeader_ctype toBean() {
    return xh;
  }


  public String getXMLPayload() {
    return payloadAsXMLString;
  }


  public static XynaOrderSerializer buildDefaultFailedXynaOrder(String orderNumber, String information) {
    String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(System.currentTimeMillis()); // FIXME Constant!
    XynaHeader_ctype xh = new XynaHeader_ctype();
    xh.setOrderNumber(orderNumber);
    xh.setStatus("FAILED");
    xh.setInformation(information);
    xh.setPriority("7"); // FIXME Constant!
    xh.setCreationDate(now);
    xh.setOrderType("unknown");
    xh.setOrderTypeVersion("unknown");
    xh.setDepartment("Processing");
    try {
      return new XynaOrderSerializer(xh, "<payload />");
    }
    catch (Exception e) {
      // ntbd kommt nich vor
      return null;
    }
  }
}
