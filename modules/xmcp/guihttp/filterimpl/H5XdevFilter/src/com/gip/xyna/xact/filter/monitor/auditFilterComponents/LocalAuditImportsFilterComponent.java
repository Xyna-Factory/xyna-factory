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
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;

public class LocalAuditImportsFilterComponent extends AuditImportsFilterComponent {

  
  private RuntimeContext workflowRtc;
  
  public LocalAuditImportsFilterComponent(RuntimeContext workflowRtc) {
    this.workflowRtc = workflowRtc;
  }
  

  
  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if(!qName.equals(EL.IMPORT)) {
      return;
    }

    String fqName = atts.getValue(ATT.AUDIT_FQNAME);
    RuntimeContext rc = AuditFilterFunctions.getRtcFromAttributes(atts);
    if(rc == null) {
      rc = workflowRtc;
    }
    
    
    AuditImport im = new AuditImport(null, rc, fqName);
    imports.add(im);
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    //nothing to be done
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    //nothing to be done
  }

}
