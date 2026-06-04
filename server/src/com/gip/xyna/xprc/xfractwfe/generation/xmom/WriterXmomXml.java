/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation.xmom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomNodeInfo.XmomNodeInfoList;


public class WriterXmomXml {

  public String toXmlString(XmomTree tree) {
    XmlBuilder xml = new XmlBuilder();
    writeXml(xml, tree.getRoot());
    return xml.toString();
  }
  
  
  private void writeXml(XmlBuilder xml, XmomNodeInfo xmom) {
    if (xmom.isIgnoreOrEmpty()) {
      return;
    }
    if (!xmom.hasChildren()) {
      if (!xmom.hasValue()) {
        return;
      }
      writeValueElement(xml, xmom);
      return;
    }
    Map<String, XmomNodeInfoList> map = xmom.getChildMap();
    List<XmomNodeInfo> valueList = new ArrayList<>();
    List<XmomNodeInfo> childElemList = new ArrayList<>();
    
    for (Entry<String, XmomNodeInfoList> entry : map.entrySet()) {
      XmomNodeInfoList list = entry.getValue();
      for (XmomNodeInfo info : list.getList()) {
        if (info.hasValue()) {
          valueList.add(info);
        } else if (info.hasChildren()) {
          childElemList.add(info);
        }
      }
    }
    if (valueList.size() == 0) {
      xml.startElement(xmom.getName());
    } else {
      xml.startElementWithAttributes(xmom.getName());
      for (XmomNodeInfo info : valueList) {
        if (isAttributename(info.getName())) {
          writeAttribute(xml, info);
        } else {
          childElemList.add(info);
        }
      }
      if (childElemList.size() < 1) {
        xml.endAttributesAndElement();
        return;
      }
      xml.endAttributes();
    }
    for (XmomNodeInfo info : childElemList) {
      writeXml(xml, info);
    }
    xml.endElement(xmom.getName());
  }
  
  
  private void writeValueElement(XmlBuilder xml, XmomNodeInfo info) {
    xml.element(info.getName(), XMLUtils.escapeXMLValueAndInvalidChars(info.getValue().get(), false, false));
  }
  
  
  private void writeAttribute(XmlBuilder xml, XmomNodeInfo info) {
    xml.addAttribute(info.getName(), info.getValue().get());
  }
  
  
  private boolean isAttributename(String name) {
    if (EL.MAPPING.equals(name)) {
      return false;
    } else if (EL.LINKTYPE.equals(name)) {
      return false;
    }
    /*
    else if (EL.SERVICE.equals(name)) {
      return false;
    }
    */
    return true;
  }
  
}
