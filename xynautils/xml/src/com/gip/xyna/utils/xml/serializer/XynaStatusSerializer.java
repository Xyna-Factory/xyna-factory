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

import com.gip.xyna._1_5.xsd.common._1.XynaStatus_ctype;
import com.gip.xyna.utils.xml.XMLUtils;

import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;

public class XynaStatusSerializer implements XMLSerializer {

  private static final String EL_XYNASTATUS = "XynaStatus";
  private static final String ATT_STATUS = "Status";
  private static final String ATT_STATUSREFERENCE = "StatusReferenceNumber";
  private static final String ATT_INFORMATION = "Information";

  private XynaStatus_ctype st;

  public XynaStatusSerializer(String xml) throws Exception {
    throw new Exception("not supported");
  }

  public XynaStatusSerializer(XynaStatus_ctype st) {
    this.st = st;
  }

  public String toXMLString() throws Exception {
    XMLDocument doc = new XMLDocument();
    Element xStatus = doc.createElementNS(XynaOrderSerializer.NS_XCOM, EL_XYNASTATUS);
    doc.appendChild(xStatus);
    XMLUtils.addAttributeIfNotNullOrEmpty(xStatus, ATT_STATUS, st.getStatus());
    XMLUtils.addAttributeIfNotNullOrEmpty(xStatus, ATT_STATUSREFERENCE, st.getStatusReferenceNumber());
    XMLUtils.addAttributeIfNotNullOrEmpty(xStatus, ATT_INFORMATION, st.getInformation());

    // toString
    return XMLUtils.getXMLElementAsString(doc);
  }

  public XynaStatus_ctype toBean() {
    return st;
  }
}
