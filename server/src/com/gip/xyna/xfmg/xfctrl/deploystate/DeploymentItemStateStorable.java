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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ServiceImplInconsistency;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = DeploymentItemStateStorable.COL_UID, tableName = DeploymentItemStateStorable.TABLENAME)
public class DeploymentItemStateStorable extends Storable<DeploymentItemStateStorable> {
  
  private static final long serialVersionUID = 1L;
  
  protected final static String TABLENAME = "deployitemstates";
  protected final static String COL_UID = "uid";
  protected final static String COL_REVISION = "revision";
  protected final static String COL_FQNAME = "fqName";
  protected final static String COL_LAST_DEPLOYMENT_TRANSITION = "lastDeploymentTransition";
  protected final static String COL_LAST_MODIFIED_BY = "lastModifiedBy";
  protected final static String COL_LAST_STATE_CHANGE = "lastStateChange";
  protected final static String COL_LAST_STATE_CHANGE_BY = "lastStateChangeBy";
  protected final static String COL_LAST_OPERATION_INTERFACE_CHANGE = "lastOperationInterfaceChange";
  protected final static String COL_ROLLBACK_CAUSE = "rollbackCause"; //deploymenterror
  protected final static String COL_ROLLBACK_ERROR = "rollbackError"; 
  protected final static String COL_BUILD_ERROR = "buildError";
  protected final static String COL_DEPLOYED_IMP_INCONSISTENCY = "deployedImplInconsistency";
  protected final static String COL_LOCATION_CONTENT_CHANGES = "locationContentChanges";
  
  public final static DeploymentItemStateStorableReader reader = new DeploymentItemStateStorableReader();
  
  @Column(name = COL_UID, size = 500)
  private String uid;
  @Column(name = COL_REVISION, index=IndexType.MULTIPLE)
  private long revision;
  @Column(name = COL_FQNAME, size = 250)
  private String fqName;
  @Column(name = COL_LAST_DEPLOYMENT_TRANSITION)
  private String lastDeploymentTransition;
  @Column(name = COL_LAST_MODIFIED_BY)
  private String lastModifiedBy;
  @Column(name = COL_LAST_STATE_CHANGE)
  private long lastStateChange;
  @Column(name = COL_LAST_STATE_CHANGE_BY)
  private String lastStateChangeBy;
  @Column(name = COL_LAST_OPERATION_INTERFACE_CHANGE)
  private long lastOperationInterfaceChange;
  @Column(name = COL_ROLLBACK_CAUSE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private SerializableExceptionInformation rollbackCause;
  @Column(name = COL_ROLLBACK_ERROR, type = ColumnType.BLOBBED_JAVAOBJECT)
  private SerializableExceptionInformation rollbackError;
  @Column(name = COL_BUILD_ERROR, type = ColumnType.BLOBBED_JAVAOBJECT)
  private SerializableExceptionInformation buildError;
  @Column(name = COL_DEPLOYED_IMP_INCONSISTENCY, type = ColumnType.BLOBBED_JAVAOBJECT)
  private ServiceImplInconsistency deployedImplInconsistency;
  @Column(name = COL_LOCATION_CONTENT_CHANGES)
  private boolean locationContentChanges;
  
  public DeploymentItemStateStorable() {
  }
  
  public DeploymentItemStateStorable(DeploymentItemStateImpl disi, long revision) {
    this.revision = revision;
    fqName = disi.getName();
    uid = revision + " " + fqName;
    if (disi.getLastDeploymentTransition().isPresent()) {
      lastDeploymentTransition = disi.getLastDeploymentTransition().get().getName();
    }
    lastModifiedBy = disi.getLastModifiedBy();
    lastStateChange = disi.getLastStateChange();
    lastStateChangeBy = disi.getLastStateChangeBy();
    lastOperationInterfaceChange = disi.getLastOperationInterfaceChange();
    if (disi.getRollbackCause().isPresent()) {
      rollbackCause = disi.getRollbackCause().get();
    }
    if (disi.getRollbackError().isPresent()) {
      rollbackError = disi.getRollbackError().get();
    }
    if (disi.getBuildError().isPresent()) {
      buildError = disi.getBuildError().get();
    }
    deployedImplInconsistency = disi.getDeployedServiceChangedInconsistency();
    locationContentChanges = disi.deploymentLocationContentChanges();
  }

  @Override
  public ResultSetReader<DeploymentItemStateStorable> getReader() {
    return reader;
  }
  
  
  private static void fillByResultset(DeploymentItemStateStorable diss, ResultSet rs) throws SQLException {
    diss.uid = rs.getString(COL_UID);
    diss.revision = rs.getLong(COL_REVISION);
    diss.fqName = rs.getString(COL_FQNAME);
    diss.lastDeploymentTransition = rs.getString(COL_LAST_DEPLOYMENT_TRANSITION);
    diss.lastModifiedBy = rs.getString(COL_LAST_MODIFIED_BY);
    diss.lastStateChange = rs.getLong(COL_LAST_STATE_CHANGE);
    diss.lastStateChangeBy = rs.getString(COL_LAST_STATE_CHANGE_BY);
    diss.lastOperationInterfaceChange = rs.getLong(COL_LAST_OPERATION_INTERFACE_CHANGE);
    diss.rollbackCause = (SerializableExceptionInformation) diss.readBlobbedJavaObjectFromResultSet(rs, COL_ROLLBACK_CAUSE);
    diss.rollbackError = (SerializableExceptionInformation) diss.readBlobbedJavaObjectFromResultSet(rs, COL_ROLLBACK_ERROR);
    diss.buildError = (SerializableExceptionInformation) diss.readBlobbedJavaObjectFromResultSet(rs, COL_BUILD_ERROR);
    diss.deployedImplInconsistency = (ServiceImplInconsistency) diss.readBlobbedJavaObjectFromResultSet(rs, COL_DEPLOYED_IMP_INCONSISTENCY);
    diss.locationContentChanges = rs.getBoolean(COL_LOCATION_CONTENT_CHANGES);
  }


  @Override
  public Object getPrimaryKey() {
    return uid;
  }


  @Override
  public void setAllFieldsFromData(DeploymentItemStateStorable data) {
    this.uid = data.uid;
    this.revision = data.revision;
    this.fqName = data.fqName;
    this.lastDeploymentTransition = data.lastDeploymentTransition;
    this.lastModifiedBy = data.lastModifiedBy;
    this.lastStateChange = data.lastStateChange;
    this.lastStateChangeBy = data.lastStateChangeBy;
    this.lastOperationInterfaceChange = data.lastOperationInterfaceChange;
    this.rollbackCause = data.rollbackCause;
    this.rollbackError = data.rollbackError;
    this.buildError = data.buildError;
    this.deployedImplInconsistency = data.deployedImplInconsistency;
    this.locationContentChanges = data.locationContentChanges;
  }

  
  public String getUid() {
    return uid;
  }
  
  public long getRevision() {
    return revision;
  }
  
  public String getFqName() {
    return fqName;
  }
  
  public String getLastDeploymentTransition() {
    return lastDeploymentTransition;
  }
  
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }
  
  public long getLastStateChange() {
    return lastStateChange;
  }
  
  public String getLastStateChangeBy() {
    return lastStateChangeBy;
  }
  
  public long getLastOperationInterfaceChange() {
    return lastOperationInterfaceChange;
  }
  
  public SerializableExceptionInformation getRollbackCause() {
    return rollbackCause;
  }
  
  public SerializableExceptionInformation getRollbackError() {
    return rollbackError;
  }

  public SerializableExceptionInformation getBuildError() {
    return buildError;
  }
  
  public ServiceImplInconsistency getDeployedImplInconsistency() {
    return deployedImplInconsistency;
  }


  private static class DeploymentItemStateStorableReader implements ResultSetReader<DeploymentItemStateStorable> {
    public DeploymentItemStateStorable read(ResultSet rs) throws SQLException {
      DeploymentItemStateStorable result = new DeploymentItemStateStorable();
      fillByResultset(result, rs);
      return result;
    }
  }

  public boolean getLocationContentChanges() {
    return locationContentChanges;
  }

}
