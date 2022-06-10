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

package com.gip.xyna.xact.filter.monitor.auditpreprocessing;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xact.filter.monitor.MonitorSession.MonitorSessionInstance;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.AuditImportsFilterComponent;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.AuditOrderItemMetaDataFilterComponent;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.ComponentBasedAuditFilter;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.UploadedAuditImportsFilterComponent;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.OrderItemWithoutAudit.OrderItemMetaData;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;

public class OrderItemWithoutAuditLoader {
  private static FileManagement fileManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();

  public static OrderItemWithoutAudit loadFromUpload(MonitorSessionInstance session, Long orderId) throws Ex_FileAccessException {
    String fileId = session.getOrderIdToFileIdMap().get(orderId);
    if(fileId == null) {
      throw new Ex_FileAccessException("Upload for orderId " + orderId);
    }

    session.getOrderIdToFileIdMap().put(orderId, fileId);
    String path = fileManagement.getAbsolutePath(fileId);
    InputSource source = null;
    
    try {
      source = new InputSource(new FileReader(path));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    
    UploadedAuditImportsFilterComponent component = new UploadedAuditImportsFilterComponent();
    OrderItemWithoutAudit result = getOrderItemWithoutAuditAsXML(source, component);
    return result;
  }

  //read file at path, remove Audit tag and write result into String
  private static OrderItemWithoutAudit getOrderItemWithoutAuditAsXML(InputSource inputSource, AuditImportsFilterComponent importsComponent) {
    ComponentBasedAuditFilter filter = new ComponentBasedAuditFilter();
    AuditOrderItemMetaDataFilterComponent metaComponent = new AuditOrderItemMetaDataFilterComponent();
    filter.addAuditFilterComponent(importsComponent);
    filter.addAuditFilterComponent(metaComponent);
    
    try {
      filter.parse(inputSource);
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    
    OrderItemWithoutAudit result = new OrderItemWithoutAudit();
    OrderItemMetaData meta = metaComponent.getResult();
    result.setImports(importsComponent.getImports());
    result.setMeta(meta);
    
    return result;
  }
}
