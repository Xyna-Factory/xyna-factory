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
package com.gip.xyna.xprc.xprcods.orderarchive;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.xpce.ProcessStep;
import com.gip.xyna.xprc.xpce.dispatcher.ServiceDestination;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;



public class OrderInstanceDetails extends OrderInstance {

  private static final long serialVersionUID = 2671022750766805608L;

  public static final String COL_AUDIT_DATA_AS_XML = "auditDataAsXML";
  public static final String COL_AUDIT_DATA_AS_XML_B = "auditDataAsXMLb";
  public static final String COL_AUDIT_DATA_AS_JAVA_OBJECT = "auditDataAsJavaObject";

  @Column(name = COL_AUDIT_DATA_AS_XML, size = Integer.MAX_VALUE)
  protected String auditDataAsXML;
  @Column(name = COL_AUDIT_DATA_AS_XML_B, type = ColumnType.BLOBBED_JAVAOBJECT)
  protected String auditDataAsXMLb;
  @Column(name = COL_AUDIT_DATA_AS_JAVA_OBJECT, type = ColumnType.BLOBBED_JAVAOBJECT)
  protected AuditData auditData;


  public OrderInstanceDetails(long id) {
    super(id);
  }


  public OrderInstanceDetails() {
  }


  public OrderInstanceDetails(XynaOrder order) {
    super(order);
    auditData = new AuditData(order);
    if (order.hasError()) {
      for (XynaException xe : order.getErrors()) {
        addException(xe);
      }
    }
  }


  public OrderInstanceDetails(XynaOrderServerExtension order) {
    super(order); //achtung, der superkonstruktor ist hier anders als oben!!
    auditData = new AuditData(order);
    if (order.hasError()) {
      for (XynaException xe : order.getErrors()) {
        addException(xe);
      }
    }
  }


  public AuditData getAuditDataAsJavaObject() {
    return auditData;
  }


  public String getAuditDataAsXML() {
    if (auditDataAsXMLb != null) {
      return auditDataAsXMLb;
    } else {
      return auditDataAsXML;
    }
  }


  public String getAuditDataAsXMLb() {
    if (auditDataAsXML != null) {
      return auditDataAsXML;
    } else {
      return auditDataAsXMLb;
    }
  }
  
  
  public void setAuditDataXML(String auditDataAsXML) {
    this.auditDataAsXML = auditDataAsXML;
  }
  
  public void setAuditDataXMLb(String auditDataAsXMLb) {
    this.auditDataAsXMLb = auditDataAsXMLb;
  }


  private void checkAuditDataNotNull() {
    if (auditData == null) {
      throw new RuntimeException(OrderInstance.class.getSimpleName()
          + " has already been cleared of java auditdata. This should not happen until the order has finished.");
    }
  }

  public void convertAuditDataToXML() throws XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRevision(getApplicationName(), getVersionName(), getWorkspaceName());
    } catch(XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // just use working set
    }
    
    convertAuditDataToXML(revision, false);
  }
  

  public boolean convertAuditDataToXML(Long revision, boolean removeAuditDataAfterwards) throws XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    if (auditData != null) {
      ExecutionType type;
      try {
        type = getExecutionType() != null ? ExecutionType.valueOf(getExecutionType()) : null;
      } catch (IllegalArgumentException e) {
        type = null;
      }

      String audit = auditData.toXML(getId(), getParentId(), getStartTime(), getStopTime(), getExceptions(), type, revision, removeAuditDataAfterwards);
      if (XynaProperty.ORDER_INSTANCE_BACKUP_STORE_AUDITXML_BINARY.get()) {
        auditDataAsXML = null;
        auditDataAsXMLb = audit;
      } else {
        auditDataAsXML = audit;
        auditDataAsXMLb = null;
      }
      return true;
    }
    //else dieser fall kann zb vorkommen, wenn default und history persistence layer auf die 
    //gleiche lokation zeigen. dann ist aber das xml schon generiert worden.
    return false;
  }


  public void setAuditDataError(ExecutionType executionType, ProcessStep pstep, Long revision) {
    checkAuditDataNotNull();
    if (pstep != null) {
      auditData.setProcessIfUnset(pstep, revision);
      auditData.addErrorStepValues(executionType, pstep);
    }
  }


  private Pair<GeneralXynaObject[], Long> cloneVars(GeneralXynaObject[] vars) {
    long version = -1;
    GeneralXynaObject[] varsClone = null;
    if (vars != null) {
      varsClone = new GeneralXynaObject[vars.length];
      for (int i = 0; i < varsClone.length; i++) {
        if (vars[i] != null) {
          if (vars[i].supportsObjectVersioning()) {
            varsClone[i] = vars[i];
            if (version == -1) {
              version = XOUtils.nextVersion();
            }
          } else {
            varsClone[i] = vars[i].clone();
          }
        } else {
          varsClone[i] = null;
        }
      }
    }
    return Pair.of(varsClone, version);
  }


  public void setAuditDataPostStep(ExecutionType executionType, ProcessStep pstep, Long revision) {
    checkAuditDataNotNull();
    if (pstep != null) {
      GeneralXynaObject[] vars = pstep.getCurrentOutgoingValues();
      Pair<GeneralXynaObject[], Long> clone = cloneVars(vars);
      auditData.addParameterPostStepValues(executionType, pstep, clone.getFirst(), clone.getSecond());
      auditData.setProcessIfUnset(pstep, revision);
    }
  }


  public void setAuditDataPreStep(ExecutionType executionType, ProcessStep pstep, Long revision) {
    checkAuditDataNotNull();
    if (pstep != null) {
      GeneralXynaObject[] vars = pstep.getCurrentIncomingValues();
      Pair<GeneralXynaObject[], Long> clone = cloneVars(vars);
      auditData.setProcessIfUnset(pstep, revision);
      auditData.addParameterPreStepValues(executionType, pstep, clone.getFirst(), clone.getSecond());
    }
  }


  public void setAuditDataPreComp(ExecutionType executionType, ProcessStep pstep, Long revision) {
    checkAuditDataNotNull();
    if (pstep != null) {
      auditData.setProcessIfUnset(pstep, revision);
      auditData.addPreCompensationEntry(executionType, pstep);
    }
  }


  public void setAuditDataPostComp(ExecutionType executionType, ProcessStep pstep, Long revision) {
    checkAuditDataNotNull();
    if (pstep != null) {
      auditData.setProcessIfUnset(pstep, revision);
      auditData.addPostCompensationEntry(executionType, pstep);
    }
  }


  public void clearAuditDataJavaObjects() {
    auditData = null;
  }
  
  public void clearAuditData() {
    clearAuditDataJavaObjects();
    auditDataAsXML = null;
    auditDataAsXMLb = null;
  }


  public OrderInstanceDetails clone() {
    OrderInstanceDetails oid = new OrderInstanceDetails();
    oid.setAllFieldsFromData(this);
    return oid;
  }


  public void setAuditDataFinished(XynaOrderServerExtension order) {
    checkAuditDataNotNull();
    GeneralXynaObject ob = order.getOutputPayload();
    if (ob != null) {
      long version;
      if (ob.supportsObjectVersioning()) {
        version = XOUtils.nextVersion();
      } else {
        ob = ob.clone();
        version = -1;
      }
      auditData.setOrderOutputData(new GeneralXynaObject[] {ob}, version);
      if (order.getMonitoringCode() >= MonitoringCodes.MASTER_WORKFLOW_MONITORING) {
        auditData.addParameterPostStepValues(order.getExecutionType(), null, new GeneralXynaObject[] {ob}, version);
      }
    }
    if (order.getExecutionDestination() instanceof ServiceDestination) {
      updateProcessInformationForServiceDestination(order);
    }
  }


  private static class OrderInstanceDetailsReader implements ResultSetReader<OrderInstanceDetails> {

    public OrderInstanceDetails read(ResultSet rs) throws SQLException {
      OrderInstanceDetails oi = new OrderInstanceDetails();
      OrderInstance.fillByResultSet(oi, rs);
      oi.auditData =
          (AuditData) oi.readBlobbedJavaObjectFromResultSet(rs, OrderInstanceColumn.C_AUDIT_DATA_AS_JAVA_OBJECT
              .getColumnName());
      oi.auditDataAsXML = rs.getString(OrderInstanceColumn.C_AUDIT_DATA_XML.getColumnName());
      oi.auditDataAsXMLb =
          (String) oi.readBlobbedJavaObjectFromResultSet(rs, OrderInstanceColumn.C_AUDIT_DATA_XML_B.getColumnName());
      
      oi.exceptions =
          (List<XynaExceptionInformation>) oi.readBlobbedJavaObjectFromResultSet(rs, OrderInstanceColumn.C_EXCEPTIONS
              .getColumnName());
      return oi;
    }

  }


  private static OrderInstanceDetailsReader reader = new OrderInstanceDetailsReader();


  @Override
  public ResultSetReader<? extends OrderInstanceDetails> getReader() {
    return reader;
  }


  @Override
  public <U extends OrderInstance> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    if (data instanceof OrderInstanceDetails) {
      OrderInstanceDetails dataDetails = (OrderInstanceDetails) data;
      auditData = dataDetails.auditData;
      auditDataAsXML = dataDetails.auditDataAsXML;
      auditDataAsXMLb = dataDetails.auditDataAsXMLb;
      exceptions = dataDetails.exceptions;
    }
  }


  public void updateProcessInformationForServiceDestination(XynaOrderServerExtension order) {
    if (order.getExecutionDestination() instanceof ServiceDestination) {
      // TODO this does not actually set the process and is thus quite misleading...
      ServiceDestination sd = (ServiceDestination) order.getExecutionDestination();
      auditData.setProcessIfUnset(sd.getOriginalFqDataTypeName() + "." + sd.getServiceName(), sd.resolveRevision(order.getRevision()));
    }
  }

  public void updateExecutionInputParametersAfterScheduling(ExecutionType executionType, GeneralXynaObject inputPayload) {
    setExecutionInputParameters(executionType, inputPayload);
  }


  private void setExecutionInputParameters(ExecutionType executionType, GeneralXynaObject inputPayload) {
    if (inputPayload == null) {
      auditData.addParameterPreStepValues(executionType, null, XynaObject.EMPTY_XYNA_OBJECT_ARRAY, -1);
    } else {
      long version;
      if (inputPayload.supportsObjectVersioning()) {
        version = XOUtils.nextVersion();
      } else {
        inputPayload = inputPayload.clone();
        version = -1;
      }
      auditData.addParameterPreStepValues(executionType, null, new GeneralXynaObject[] {inputPayload}, version);
    }
  }


  public String toStringDetails() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString()).append(" lastupdate=").append(
                                                              Constants.defaultUTCSimpleDateFormat()
                                                                  .format(new Date(getLastUpdate())))
        .append(", status=").append(getStatusAsString()).append(", compensateStatus=").append(getStatusCompensate())
        .append(", auditData=").append(getAuditDataAsXML());
    return sb.toString();
  }


  public void updateAuditDataAddSubworkflowId(ProcessStep pstep, long subworkflowId) {
    checkAuditDataNotNull();
    if (pstep != null) {
      auditData.addSubworkflowId(pstep, subworkflowId);
    }
  }
  

}
