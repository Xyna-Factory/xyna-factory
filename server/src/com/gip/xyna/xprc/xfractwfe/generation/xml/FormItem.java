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
package com.gip.xyna.xprc.xfractwfe.generation.xml;

import java.util.Map;

import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;


public class FormItem implements XmlAppendable {

  protected String label;
  protected Map<DocumentationLanguage,String> multilingualLabels;
  protected Map<DocumentationLanguage,String> documentations;
  protected String dataLink; //dataLink ohne "%0%." am Anfang
  
  
  
  public FormItem(String label, Map<DocumentationLanguage, String> multilingualLabels,
                   Map<DocumentationLanguage, String> documentations, String dataLink) {
    this.label = label;
    this.multilingualLabels = multilingualLabels;
    this.documentations = documentations;
    this.dataLink = dataLink;
  }



  /**
   * @param xml
   */
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.FORM_ITEM);
    xml.addAttribute(ATT.LABEL, label);

    xml.endAttributes();
    
    //mehrsprachige Labels
    if( multilingualLabels != null ) {
      for( Map.Entry<DocumentationLanguage,String> entry : multilingualLabels.entrySet() ) {
        if( entry.getValue() != null ) {
          xml.startElementWithAttributes(EL.LABEL);{
            xml.addAttribute(ATT.LANGUAGE, entry.getKey().name());
          }xml.endAttributesAndElement(entry.getValue(), EL.LABEL );
        }
      }
    }
    
    //mehrsprachige Dokumentationen
    if( documentations != null ) {
      for( Map.Entry<DocumentationLanguage,String> entry : documentations.entrySet() ) {
        if( entry.getValue() != null ) {
          xml.startElementWithAttributes(EL.DOCUMENTATION);{
            xml.addAttribute(ATT.LANGUAGE,entry.getKey().name());
          }xml.endAttributesAndElement(XMLUtils.escapeXMLValue(entry.getValue()), EL.DOCUMENTATION );
        }
      }
    }
    
    //DataLink
    xml.element(EL.DATALINK,"%0%." + dataLink);
    
    xml.endElement(EL.FORM_ITEM);
  }

}
