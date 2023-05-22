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

import com.gip.xyna._1_5.scheduler._1.Capacities;
import com.gip.xyna._1_5.scheduler._1.Capacity;
import com.gip.xyna._1_5.scheduler._1.PreSchedulerInformation;
import com.gip.xyna._1_5.scheduler._1.Resource;
import com.gip.xyna._1_5.scheduler._1.Resources;

import java.io.StringReader;
import java.io.StringWriter;

import java.math.BigInteger;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PreSchedulerInformationSerializer implements XMLSerializer {

  private static final String NS_SCHED = "http://www.gip.com/xyna/1.5/scheduler/1.0";
  private static final String CAPACITY = "Capacity";
  private static final String RESOURCE = "Resource";
  private static final String CAPACITIES = "Capacities";
  private static final String RESOURCES = "Resources";
  private static final String CAPTYPE = "type";
  private static final String RESTYPE = "type";
  private static final String TXID = "txid";
  private PreSchedulerInformation inf;

  public PreSchedulerInformationSerializer(PreSchedulerInformation inf) {
    this.inf = inf;
  }
  
  public PreSchedulerInformationSerializer(String xml) throws Exception {
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    parser.parse(new StringReader(xml));
    XMLDocument doc = parser.getDocument();
    NodeList nlCap = doc.getElementsByTagNameNS(NS_SCHED, CAPACITY);
    NodeList nlRes = doc.getElementsByTagNameNS(NS_SCHED, RESOURCE);
    
    inf = new PreSchedulerInformation();
    Capacities caps = new Capacities();
    inf.setCapacities(caps);
    Resources ress = new Resources();
    inf.setResources(ress);
    
    Capacity[] cap = new Capacity[nlCap.getLength()];
    caps.setCapacity(cap);
    try {
      for (int i = 0; i<nlCap.getLength(); i++) {
        XMLElement el = (XMLElement)nlCap.item(i);
        cap[i] = new Capacity();
        cap[i].set_value(new BigInteger(el.getTextContent()));
        cap[i].setType(new BigInteger(el.getAttribute(CAPTYPE)));
        if (el.hasAttribute(TXID)) {
          cap[i].setTxid(new BigInteger(el.getAttribute(TXID)));
        }
      }
    } catch (NumberFormatException e) {
      throw new Exception("XML PreSchedulerInformation hat ung�ltigen Wert f�r ein Capacity Feld. Es sind nur Zahlen erlaubt. " + xml, e);
    }
    
    Resource[] res = new Resource[nlRes.getLength()];
    ress.setResource(res);
    for (int i = 0; i<nlRes.getLength(); i++) {
      XMLElement el = (XMLElement)nlRes.item(i);
      res[i] = new Resource();
      res[i].set_value(el.getTextContent());
      res[i].setType(el.getAttribute(RESTYPE));
    }
  }

  public String toXMLString() throws Exception {
    XMLDocument doc = new XMLDocument();
    Element infEl = doc.createElementNS(NS_SCHED, "PreSchedulerInformation");
    doc.appendChild(infEl);

    if (inf.getCapacities() != null) {
      Element capsEl = doc.createElementNS(NS_SCHED, CAPACITIES);
      infEl.appendChild(capsEl);
      if (inf.getCapacities().getCapacity() != null) {
        for (int i = 0; i<inf.getCapacities().getCapacity().length; i++) {
          XMLElement capEl = (XMLElement)doc.createElementNS(NS_SCHED, CAPACITY);      
          capsEl.appendChild(capEl);
          capEl.setTextContent(inf.getCapacities().getCapacity()[i].get_value().toString());
          capEl.setAttribute(CAPTYPE, inf.getCapacities().getCapacity()[i].getType().toString());
          if (inf.getCapacities().getCapacity()[i].getTxid() != null) {
            capEl.setAttribute(TXID, inf.getCapacities().getCapacity()[i].getTxid().toString());
          }
        }
      }
    }

    if (inf.getResources() != null) {
      Element ressEl = doc.createElementNS(NS_SCHED, RESOURCES);
      infEl.appendChild(ressEl);
      if (inf.getResources().getResource() != null) {
        for (int i = 0; i<inf.getResources().getResource().length; i++) {
          XMLElement resEl = (XMLElement)doc.createElementNS(NS_SCHED, RESOURCE);      
          ressEl.appendChild(resEl);
          resEl.setTextContent(inf.getResources().getResource()[i].get_value());
          resEl.setAttribute(RESTYPE, inf.getResources().getResource()[i].getType());
        }
      }
    }

    //toString
    StringWriter sw = new StringWriter();
    doc.print(sw);
    return sw.toString();
  }

  public PreSchedulerInformation toBean() {
    return inf;
  }
}
