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
package com.gip.xyna.xprc.xprcods.orderarchive.audit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.BasicApplicationName;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

/**
 * Klasse zur Verwaltung von Objekten, die in allen Audits als Import gebraucht werden.
 * Dies sind für die Modellierung wichtige Datentypen (z.B. Xyna Exception, Xyna Exception Base,
 * Exception, Storable, ...).
 *
 */
public class BasicAuditImport {
  
  private static Logger logger = CentralFactoryLogging.getLogger(BasicAuditImport.class);
  
  private static final List<String> basicDataTypeNames = new ArrayList<String>();
  
  //TODO auditImports-Cache refreshen, wenn sich eine Basis-Application geändert hat
  private static List<AuditImport> auditImports;          // version of imports where rtc is set to the actual rtc it belongs to
  private static List<AuditImport> auditImportsWithWfRtc; // version of imports where rtc is set to the on from the parent wf
  
  
  static{
    basicDataTypeNames.add(GenerationBase.CORE_EXCEPTION);
    basicDataTypeNames.add(GenerationBase.CORE_XYNAEXCEPTION);
    basicDataTypeNames.add(GenerationBase.CORE_XYNAEXCEPTIONBASE);
    basicDataTypeNames.add("xprc.waitsuspend.TimeConfiguration");
    basicDataTypeNames.add("xprc.waitsuspend.AbsoluteTimeConfiguration");
    basicDataTypeNames.add("xprc.waitsuspend.RelativeTimeConfiguration");
    basicDataTypeNames.add("xprc.waitsuspend.Seconds");
    basicDataTypeNames.add("xprc.waitsuspend.Minutes");
    basicDataTypeNames.add("xprc.waitsuspend.Hours");
    basicDataTypeNames.add("xprc.waitsuspend.Days");
    basicDataTypeNames.add("xprc.waitsuspend.Months");
    basicDataTypeNames.add("xprc.waitsuspend.Years");
    basicDataTypeNames.add("xprc.waitsuspend.TimezoneOffset");
    basicDataTypeNames.add("xprc.synchronization.CorrelationId");
    basicDataTypeNames.add("xprc.synchronization.Timeout");
    basicDataTypeNames.add("xprc.synchronization.SynchronizationAnswer");
    basicDataTypeNames.add("xmcp.manualinteraction.Reason");
    basicDataTypeNames.add("xmcp.manualinteraction.Type");
    basicDataTypeNames.add("xmcp.manualinteraction.UserGroup");
    basicDataTypeNames.add("xmcp.manualinteraction.Todo");
    basicDataTypeNames.add("xmcp.manualinteraction.Result");
    basicDataTypeNames.add("xprc.retry.RetryParameter");
    basicDataTypeNames.add("xact.templates.DocumentType");
    basicDataTypeNames.add("xact.templates.CommandLineInterface");
    basicDataTypeNames.add("xact.templates.HTML");
    basicDataTypeNames.add("xact.templates.NETCONF");
    basicDataTypeNames.add("xact.templates.PlainText");
    basicDataTypeNames.add("xact.templates.XML");
    basicDataTypeNames.add("xact.templates.DocumentContext");
    basicDataTypeNames.add("xact.templates.Document");
    basicDataTypeNames.add(XMOMPersistenceManagement.STORABLE_BASE_CLASS);
    basicDataTypeNames.add("xnwh.persistence.DeleteParameter");
    basicDataTypeNames.add("xnwh.persistence.ReferenceHandling");
    basicDataTypeNames.add("xnwh.persistence.StoreParameter");
  }
  

  
  public static boolean isBasicDataType(String fqXmlName) {
    return basicDataTypeNames.contains(fqXmlName);
  }
  
  /**
   * Gets the basic imports
   * 
   * @param mapIntoWfRtc if true, rtc is to the one from the parent workflow (necessary for old GUI)
   * @return
   */
  public static List<AuditImport> getAuditImports(boolean mapIntoWfRtc) {
    if (auditImports == null) {
      auditImports = new ArrayList<AuditImport>();
      auditImportsWithWfRtc = new ArrayList<AuditImport>();
      for (String fqXmlName : basicDataTypeNames) {
        readXml(fqXmlName);
      }
    }
    
    if (mapIntoWfRtc) {
      return Collections.unmodifiableList(auditImportsWithWfRtc);
    } else {
      return Collections.unmodifiableList(auditImports);
    }
  }
  
  
  private static void readXml(String fqXmlName) {
    //Die Objekte befinden sich alle in der Base-Application.
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    Long revision = applicationManagement.getBasicApplicationRevision(BasicApplicationName.Base);
    
    File file;
    if (revision != null) {
      file = new File((GenerationBase
                      .getFileLocationForDeploymentStaticHelper(fqXmlName, revision)
                      + ".xml"));
    } else {
      //falls die Base-Application nicht existiert, die XMLs im Default-Workspace suchen
      revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      file = new File((GenerationBase
                      .getFileLocationForSavingStaticHelper(fqXmlName, revision)
                      + ".xml"));
    }
    
    AuditImport ai;
    try {
      RuntimeContext rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
      ai = new AuditImport(FileUtils.readFileAsString(file), rtc, fqXmlName);
      auditImports.add(ai);
      
      ai = new AuditImport(FileUtils.readFileAsString(file), null, fqXmlName);
      auditImportsWithWfRtc.add(ai);
    } catch (Ex_FileWriteException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.error("Could not read file '" + file.getPath() + "'", e);
    }
  }
}
