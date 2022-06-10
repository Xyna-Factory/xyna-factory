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
package com.gip.xyna.xprc.xfractwfe.generation.xml;


import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Updater;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class Workflow {
  
  private static String XMOM_VERSION = "unknown";
  private final static String NAMESPACE = "http://www.gip.com/xyna/xdev/xfractmod";

  static {
    try {
      if (XynaFactory.isFactoryServer()) {
        XMOM_VERSION = Updater.getInstance().getXMOMVersion().getString();
      }
    } catch (Exception e ) { //XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException
      Logger.getLogger(Workflow.class).info("Could not get XMOM_VERSION", e);
    }
  }

  protected XmomType type;
  protected Operation operation;


  private Workflow() {
  }

  public Workflow(XmomType type, Operation operation) {
    this.type = type;
    this.operation = operation; 
  }


  public String getFQTypeName() {
    return type.getFQTypeName();
  }

  public String toXML() {
    XmlBuilder xml = new XmlBuilder();
    xml.append(GenerationBase.COPYRIGHT_HEADER);
    xml.startElementWithAttributes(EL.SERVICE); {
      xml.addAttribute(ATT.XMLNS, NAMESPACE);
      xml.addAttribute(ATT.MDM_VERSION, XMOM_VERSION);
      xml.addAttribute(ATT.TYPENAME, XMLUtils.escapeXMLValue(type.getName(), true, false));
      xml.addAttribute(ATT.TYPEPATH, XMLUtils.escapeXMLValue(type.getPath(), true, false));
      xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(type.getLabel(), true, false));
      xml.endAttributes();

      // <Meta>
      if (operation.hasUnknownMetaTags() || (operation.getDocumentation() != null && operation.getDocumentation().length() > 0)) {
        xml.startElement(EL.META); {
          if(operation.getDocumentation() != null && operation.getDocumentation().length() > 0) {
            String doc = XMLUtils.escapeXMLValueAndInvalidChars(operation.getDocumentation(), false, false);
            xml.element(EL.DOCUMENTATION, doc);
          }
          if(operation.hasUnknownMetaTags()) {
            operation.appendUnknownMetaTags(xml);
          }
        } xml.endElement(EL.META);
      }

      operation.appendXML(xml);
    } xml.endElement(EL.SERVICE);

    return xml.toString();
  }


}
