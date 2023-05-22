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

package com.gip.xyna.xact.filter.monitor.auditFilterComponents;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;

public class AuditRuntimeContextFilterComponent implements AuditFilterComponent {

  private RuntimeContext result;
  
  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if(!qName.equals(EL.ENHANCED_AUDIT)) {
      return;
    }
    
    result = AuditFilterFunctions.getRtcFromAttributes(atts);
    
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    //nothing to be done
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    //nothing to be done
  }

  
  public RuntimeContext getResult() {
    return result;
  }

}
