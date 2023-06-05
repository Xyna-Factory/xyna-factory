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
package com.gip.xyna.update;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_WarehouseJobRunnableParameterInvalidException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJob;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJobActionType;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJobRunnable;


public class UpdateWarehouseCopyJobParameter extends UpdateJustVersion {

  public UpdateWarehouseCopyJobParameter(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }
  
  
  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(ConversionWarehouseJob.class);
    ODSConnection con = ods.openConnection();
    try {
      //load all
      Collection<? extends ConversionWarehouseJob> jobs = con.loadCollection(ConversionWarehouseJob.class);
      con.persistCollection(jobs);
      con.commit();
    } finally {
      con.closeConnection();
      ods.unregisterStorable(ConversionWarehouseJob.class);
    }
    
  }
  
  
  public static class ConversionWarehouseJob extends WarehouseJob {

    private static final long serialVersionUID = 2697037412535455655L;
    
    public ConversionWarehouseJob() {
    }
    @Override
    protected WarehouseJobRunnable instantiateWarehouseJobRunnable()
                    throws XNWH_WarehouseJobRunnableParameterInvalidException {
      if (WarehouseJobActionType.valueOf(getActiontype()) == WarehouseJobActionType.Copy) {
        String[] actionparameterAsArray = getActionparameters().split(":");
        long sourceId;
        try {
          sourceId = Long.parseLong(actionparameterAsArray[0]);
        } catch (NumberFormatException e) {
          logger.error(null,e);
          throw new XNWH_WarehouseJobRunnableParameterInvalidException("sourceId", actionparameterAsArray[0]);
        }
        
        long targetId;
        try {
          targetId = Long.parseLong(actionparameterAsArray[1]);
        } catch (NumberFormatException e) {
          logger.error(null,e);
          throw new XNWH_WarehouseJobRunnableParameterInvalidException("sourceId", actionparameterAsArray[1]);
        }
        String tableName = actionparameterAsArray[2];
        
        // convert persLayer-IDs to conTypes
        ODSConnectionType sourceConType = null;
        ODSConnectionType targetConType = null;
        logger.debug("going to look for sourceId: " + sourceId + " and targetid: " + targetId);
        PersistenceLayerInstanceBean[] plInstances = ODSImpl.getInstance().getPersistenceLayerInstances(); // if need be we'll read ourself?
        for (PersistenceLayerInstanceBean pliBean : plInstances) {
          if (pliBean.getPersistenceLayerInstanceID() == sourceId) {
            logger.debug("found source: " + pliBean.getConnectionTypeEnum());
            sourceConType = pliBean.getConnectionTypeEnum();
          }
          if (pliBean.getPersistenceLayerInstanceID() == targetId) {
            logger.debug("found target: " + pliBean.getConnectionTypeEnum());
            targetConType = pliBean.getConnectionTypeEnum();
          }
        }
        return new WarehouseJobRunnable(this, getName(), WarehouseJobActionType.Copy, new String[] {sourceConType.toString(), targetConType.toString(), tableName});
      } else {
        return super.instantiateWarehouseJobRunnable();
      }
    }
    @Override
    public ResultSetReader<? extends ConversionWarehouseJob> getReader() {
      return new ResultSetReader<ConversionWarehouseJob>() {

        public ConversionWarehouseJob read(ResultSet rs) throws SQLException {
          ConversionWarehouseJob whj = new ConversionWarehouseJob();
          try {
            setAllFieldsFromData(whj, rs);
          } catch (XynaException e) {
            throw new SQLException("An unknown error occurred: " + e.getMessage());
          }
          return whj;
        }
      };
    }
  }

}
