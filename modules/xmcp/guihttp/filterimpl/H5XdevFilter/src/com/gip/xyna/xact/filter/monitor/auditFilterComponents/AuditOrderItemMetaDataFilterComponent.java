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



import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.gip.xyna.xact.filter.monitor.auditpreprocessing.OrderItemWithoutAudit.OrderItemMetaData;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;




public class AuditOrderItemMetaDataFilterComponent implements AuditFilterComponent {

  private OrderItemMetaData result;

  private boolean setting;
  private String currentValue;
  
  private String applicationName;
  private String applicationVersion;
  
  private boolean inOtherTag;

  private Set<String> tagsToRead;

  public AuditOrderItemMetaDataFilterComponent() {
    result = new OrderItemMetaData();
    setting = false;
    currentValue = "";
    inOtherTag = false;
    tagsToRead = new HashSet<String>();
    tagsToRead.add("OrderID");
    tagsToRead.add("Destination");
    tagsToRead.add(EL.WORKSPACE);
    tagsToRead.add(EL.APPLICATION);
    tagsToRead.add(EL.APPLICATION_VERSION);
  }


  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

    if(qName.equals(EL.AUDIT) || qName.equals(EL.IMPORT)) {
      inOtherTag = true;
    }
    
    if(inOtherTag) {
      return;
    }
    
    if (tagsToRead.contains(qName)) {
      setting = true;
    }
  }


  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    setting = false;
    
    if(qName.equals(EL.AUDIT) || qName.equals(EL.IMPORT)) {
      inOtherTag = false;
    }
    
    switch(qName) {
      case "OrderID": 
        result.setOrderID(Long.valueOf(currentValue));
        currentValue = "";
        break;
      case "Destination":
        result.setDestination(currentValue);
        currentValue = "";
        break;
      case EL.WORKSPACE:
        result.setRtc(new Workspace(currentValue));
        currentValue = "";
        break;
      case EL.APPLICATION:
        applicationName = currentValue;
        currentValue = "";
        setApplicationIfFinished();
        break;
      case EL.APPLICATION_VERSION:
        applicationVersion = currentValue;
        setApplicationIfFinished();
        break;
    }
    
    if(tagsToRead.contains(qName)) {
      currentValue = "";
    }

  }
  
  
  private void setApplicationIfFinished() {
    if(applicationName != null && applicationVersion != null) {
      result.setRtc(new Application(applicationName, applicationVersion));
      applicationName = null;
      applicationVersion = null;
    }
  }


  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (setting) {
      currentValue += new String(ch, start, length);
    }
  }


  public OrderItemMetaData getResult() {
    return result;
  }

}
