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

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;


/*
 * Import - insideImport
 *   Workspace | Application | Version - writing
 *   Document - writingDocument
 *     DataType | ExceptionType | service - insideDocumentMember
 *       additional tags that may include service
 */
public class UploadedAuditImportsFilterComponent extends AuditImportsFilterComponent {
  private boolean writing;
  private boolean writingDocument;
  private boolean insideImport;
  private boolean insideDocumentMember;
  private StringBuilder sb;

  private String applicationName;
  private String workspaceName;
  private String applicationVersion;
  private String documentElement;
  private String fqn;


  public UploadedAuditImportsFilterComponent() {
    writing = false;
    sb = new StringBuilder();
    applicationName = null;
    workspaceName = null;
    applicationVersion = null;
    documentElement = null;
    fqn = null;
    writingDocument = false;
    insideImport = false;
    insideDocumentMember = false;
  }


  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

    if (qName.equals(EL.IMPORT)) {
      insideImport = true;
    }

    if (!insideImport) {
      return;
    }

    if (writingDocument) {
      writeStartElement(qName, atts);
    }

    if (qName.equals(EL.DOCUMENT)) {
      writingDocument = true;
    }

    if (writingDocument && !insideDocumentMember && (qName.equals(EL.DATATYPE) || qName.equals(EL.EXCEPTIONTYPE) || qName.equals(EL.SERVICE))) {
      fqn = atts.getValue(ATT.TYPEPATH) + "." + atts.getValue(ATT.TYPENAME);
      insideDocumentMember = true;
    }
    
    if(!writingDocument && !insideDocumentMember && (qName.equals(EL.WORKSPACE) || qName.equals(EL.APPLICATION) || qName.equals(EL.APPLICATION_VERSION))) {
      writing = true;
    }
  }


  private void writeStartElement(String qName, Attributes atts) {
    sb.append("<").append(qName).append(" ");
    for (int i = 0; i < atts.getLength(); i++) {
      sb.append(atts.getQName(i));
      sb.append("=\"");
      sb.append(atts.getValue(i));
      sb.append("\" ");
    }
    sb.append(">");
  }


  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {

    if (!insideImport) {
      return;
    }
    
    if(insideDocumentMember && (qName.equals(EL.DATATYPE) || qName.equals(EL.EXCEPTIONTYPE) || qName.equals(EL.SERVICE))){
      insideDocumentMember = false;
    }

    if (writingDocument && qName.equals(EL.DOCUMENT)) {
      documentElement = sb.toString();
      sb.setLength(0);
      writingDocument = false;
      writing = false;
    }

    if (writingDocument) {
      sb.append("</");
      sb.append(qName);
      sb.append(">");
    }

    if (writing && qName.equals(EL.WORKSPACE)) {
      workspaceName = sb.toString();
      sb.setLength(0);
      writing = false;
    }

    if (writing && qName.equals(EL.APPLICATION)) {
      applicationName = sb.toString();
      sb.setLength(0);
      writing = false;
    }


    if (writing && qName.equals(EL.APPLICATION_VERSION)) {
      applicationVersion = sb.toString();
      sb.setLength(0);
      writing = false;
    }

    if (qName.equals(EL.IMPORT)) {
      addImport();
      reset();
    }
  }


  private void addImport() {
    RuntimeContext runtimeContext = createRuntimeContext();
    AuditImport im = new AuditImport(documentElement, runtimeContext, fqn);
    imports.add(im);
  }


  private RuntimeContext createRuntimeContext() {
    if (workspaceName != null) {
      return new Workspace(workspaceName);
    } else if(applicationName != null && applicationVersion != null){
      return new Application(applicationName, applicationVersion);
    } else {
      return null; //basic imports may not have a runtimeContext
    }
  }


  private void reset() {
    writing = false;
    writingDocument = false;
    insideImport = false;
    workspaceName = null;
    applicationName = null;
    applicationVersion = null;
    documentElement = null;
    fqn = null;
  }


  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (writing || writingDocument) {
      sb.append(ch, start, length);
    }
  }

}
