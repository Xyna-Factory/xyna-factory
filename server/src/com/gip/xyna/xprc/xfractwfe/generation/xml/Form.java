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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Updater;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;


public class Form {

  private static String XMOM_VERSION = "unknown";
  static {
    try {
      if (XynaFactory.isFactoryServer()) {
        XMOM_VERSION = Updater.getInstance().getXMOMVersion().getString();
      }
    } catch (Exception e ) { //XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException
      Logger.getLogger(Datatype.class).info("Could not get XMOM_VERSION", e);
    }
  }
  private final static String NAMESPACE = "http://www.gip.com/xyna/xdev/xfractmod";
  
  protected XmomType type;
  protected Variable input;
  protected Map<DocumentationLanguage,String> multilingualLabels;
  protected List<FormItem> formItems;
  

  public Form(XmomType type, Map<DocumentationLanguage,String> multilingualLabels, Variable input, List<FormItem> formItems) {
    this.type = type;
    this.multilingualLabels = multilingualLabels;
    this.input = input;
    this.formItems = formItems;
  }

  public String toXML() {
    XmlBuilder xml = new XmlBuilder();
    xml.startElementWithAttributes(EL.FORMDEFINITION);
    xml.addAttribute(ATT.XMLNS, NAMESPACE);
    xml.addAttribute(ATT.MDM_VERSION, XMOM_VERSION);
    xml.addAttribute(ATT.TYPENAME, type.getName());
    xml.addAttribute(ATT.TYPEPATH, type.getPath());
    xml.addAttribute(ATT.LABEL, type.getLabel());
    xml.endAttributes();
    
    //mehrsprachige Labels
    if( multilingualLabels != null ) {
      for( Map.Entry<DocumentationLanguage,String> entry : multilingualLabels.entrySet() ) {
        if( entry.getValue() != null ) {
          xml.startElementWithAttributes(EL.LABEL);{
            xml.addAttribute(ATT.LANGUAGE,entry.getKey().name());
          }xml.endAttributesAndElement(entry.getValue(), EL.LABEL);
        }
      }
    }
    
    //input
    xml.startElement(EL.INPUT);{
      input.appendXML(xml);
    }xml.endElement(EL.INPUT);
    
    //FormItems
    for( FormItem fi : formItems ) {
      fi.appendXML(xml);
    }
    
    xml.endElement(EL.FORMDEFINITION);
    return xml.toString();
  }

}
