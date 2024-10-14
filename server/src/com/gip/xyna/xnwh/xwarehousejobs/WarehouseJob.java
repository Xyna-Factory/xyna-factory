/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.xnwh.xwarehousejobs;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_WarehouseJobRunnableParameterInvalidException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJobScheduleFactory.WarehouseJobScheduleType;




@Persistable(tableName = WarehouseJob.TABLE_NAME, primaryKey = WarehouseJob.COLUMN_ID)
public class WarehouseJob extends Storable<WarehouseJob> {

  private static final long serialVersionUID = 1L;

  private static WarehouseJobReader reader = new WarehouseJobReader();
  private static final Logger logger = CentralFactoryLogging.getLogger(WarehouseJob.class);


  public static final String TABLE_NAME = "warehousejobs";
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_DESCRIPTION = "description";
  public static final String COLUMN_SCHEDULE_TYPE = "scheduletype";
  public static final String COLUMN_ACTION_PARAMETERS = "actionparameters";
  public static final String COLUMN_ACTION_TYPE = "actiontype";
  public static final String COLUMN_SCHEDULE_PARAMETERS = "scheduleparameters";

  private transient WarehouseJobRunnable jobRunnable;
  private transient WarehouseJobSchedule jobSchedule;

  @Column(name = COLUMN_ID)
  private Long id = -1L;
  @Column(name = COLUMN_NAME)
  private String name;
  @Column(name = COLUMN_DESCRIPTION)
  private String description;
  @Column(name = COLUMN_ACTION_PARAMETERS)
  private String actionparameters;
  @Column(name = COLUMN_ACTION_TYPE)
  private String actionType;
  @Column(name = COLUMN_SCHEDULE_TYPE)
  private String scheduleType;
  @Column(name = COLUMN_SCHEDULE_PARAMETERS)
  private String scheduleParameters;

  private volatile boolean isRunning = false;

  private transient String[] actionparameterAsArray;
  private transient String[] scheduleparameterAsArray;


  public WarehouseJob() {
  }

  WarehouseJob(Long id, String name) {
    this.name = name;
    this.id = id;
  }


  public WarehouseJobScheduleType getScheduleType() {
    if (jobSchedule != null) {
      return jobSchedule.getType();
    } else {
      logger.warn("Tried to get job type but no underlying job schedule was defined");
      return null;
    }
  }


  public final WarehouseJobSchedule getJobSchedule() {
    return jobSchedule;
  }


  final void setJobSchedule(WarehouseJobSchedule jobSchedule) {
    this.jobSchedule = jobSchedule;
    this.scheduleType = jobSchedule.getType().toString();
    this.scheduleparameterAsArray = jobSchedule.getScheduleParameters();
    if (this.scheduleparameterAsArray != null) {
      StringBuffer sb = new StringBuffer();
      for (String s : scheduleparameterAsArray) {
        sb.append(s).append(":");
      }
      this.scheduleParameters = sb.toString();
    }
  }


  public long getId() {
    return id;
  }


  public String getActionparameters() {
    return actionparameters;
  }


  public String getActiontype() {
    return actionType;
  }


  public String getScheduletype() {
    return scheduleType;
  }


  public String getScheduleparameters() {
    return scheduleParameters;
  }


  public final boolean isRunning() {
    return isRunning;
  }


  final void setRunning(boolean b) {
    isRunning = b;
  }


  public final WarehouseJobRunnable getRunnable() {
    return jobRunnable;
  }


  final void setRunnable(WarehouseJobRunnable jobRunnable) {
    this.jobRunnable = jobRunnable;
    this.actionType = jobRunnable.getActionType().toString();
    this.actionparameterAsArray = jobRunnable.getActionParameters();
    if (this.actionparameterAsArray != null) {
      StringBuffer sb = new StringBuffer();
      for (String s : actionparameterAsArray) {
        sb.append(s).append(":");
      }
      this.actionparameters = sb.toString();
    }
  }


  public final String getDescription() {
    return description;
  }


  final void setDescription(String description) {
    this.description = description;
  }


  public String getName() {
    return name;
  }


  public boolean isRegistered() {
    return jobSchedule.isRegistered();
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public ResultSetReader<? extends WarehouseJob> getReader() {
    return reader;
  }


  @Override
  public <U extends WarehouseJob> void setAllFieldsFromData(U data) {
    WarehouseJob cast = data;
    this.id = cast.id;
    this.description = cast.description;
    this.name = cast.name;
    this.actionparameters = cast.actionparameters;
    this.actionparameterAsArray = this.actionparameters.split(":");
    this.actionType = cast.actionType;
    try {
      this.setRunnable(instantiateWarehouseJobRunnable());
    } catch (XynaException e) {
      throw new RuntimeException("Error while creating warehouse job runnable", e);
    }
    this.scheduleType = cast.scheduleType;
    this.scheduleParameters = cast.scheduleParameters;
    this.scheduleparameterAsArray = this.scheduleParameters.split(":");
    try {
      this.setJobSchedule(WarehouseJobScheduleFactory.getScheduleByType(this.jobRunnable, WarehouseJobScheduleType
          .fromString(this.scheduleType), this.scheduleparameterAsArray));
    } catch (XynaException e) {
      throw new RuntimeException("Error while creating warehouse job schedule", e);
    }
  }


  protected static void setAllFieldsFromData(WarehouseJob job, ResultSet rs) throws SQLException, XynaException {
    job.id = rs.getLong(COLUMN_ID);
    job.description = rs.getString(COLUMN_DESCRIPTION);
    job.name = rs.getString(COLUMN_NAME);
    job.actionparameters = rs.getString(COLUMN_ACTION_PARAMETERS);
    job.actionparameterAsArray = job.actionparameters.split(":");
    job.actionType = rs.getString(COLUMN_ACTION_TYPE);
    job.setRunnable(job.instantiateWarehouseJobRunnable());
    job.scheduleType = rs.getString(COLUMN_SCHEDULE_TYPE);
    job.scheduleParameters = rs.getString(COLUMN_SCHEDULE_PARAMETERS);
    job.scheduleparameterAsArray = job.scheduleParameters.split(":");
    job.setJobSchedule(WarehouseJobScheduleFactory.getScheduleByType(job.jobRunnable, WarehouseJobScheduleType
        .fromString(job.scheduleType), job.scheduleparameterAsArray));
  }
  
  
  protected WarehouseJobRunnable instantiateWarehouseJobRunnable() throws XNWH_WarehouseJobRunnableParameterInvalidException {
    return new WarehouseJobRunnable(this, this.name, WarehouseJobActionType.valueOf(this.actionType), this.actionparameterAsArray);
  }


  private static class WarehouseJobReader implements ResultSetReader<WarehouseJob> {

    public WarehouseJob read(ResultSet rs) throws SQLException {
      WarehouseJob whj = new WarehouseJob();
      try {
        setAllFieldsFromData(whj, rs);
      } catch (XynaException e) {
        logger.error(null,e);
        throw new SQLException("An unknown error occurred: " + e.getMessage());
      }
      return whj;
    }

  }

}
