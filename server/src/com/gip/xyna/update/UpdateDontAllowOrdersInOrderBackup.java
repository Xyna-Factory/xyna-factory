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
package com.gip.xyna.update;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TableConfiguration;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;


public class UpdateDontAllowOrdersInOrderBackup extends UpdateJustVersion {

  private static Logger logger = CentralFactoryLogging.getLogger(UpdateDontAllowOrdersInOrderBackup.class);

  /**
   * dieses update muss nicht mehrfach bei einem serverstart ausgef�hrt werden.
   */
  private static boolean hasBeenExecuted = false;
  
  public UpdateDontAllowOrdersInOrderBackup(Version vStart, Version vEnd) {
    super(vStart, vEnd);
  }

  public UpdateDontAllowOrdersInOrderBackup(Version vStart, Version vEnd, boolean mustUpdateGeneratedClasses) {
    super(vStart, vEnd, mustUpdateGeneratedClasses);
  }

  @Override
  protected void update() throws XynaException {
    if (hasBeenExecuted) {
      logger.debug("skipping update because it has already been executed.");
      return;
    }
    Integer cntBackup;
    try {
      Persistable persistableOrderBackup = Storable.getPersistable(OrderInstanceBackup.class);
      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(OrderInstanceBackup.class);
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        PreparedQuery<Integer> pq = con.prepareQuery(new Query<Integer>("select count(*) from "
                        + persistableOrderBackup.tableName(), new ResultSetReader<Integer>() {

          public Integer read(ResultSet rs) throws SQLException {
            return rs.getInt(1);        
          }

        }));
        cntBackup = con.queryOneRow(pq, null);
      } finally {
        con.closeConnection();
      }
      
      if (cntBackup > 0) {
        StringBuilder errMsgBuilder = new StringBuilder();
        errMsgBuilder.append("Critical update ")
                     .append(getAllowedVersionForUpdate().getString())
                     .append("->")
                     .append(getVersionAfterUpdate().getString())
                     .append(" can only be applied if there were no orders running at the last shut down of the server. ")
                     .append("Please remove all entries from table ")
                     .append(persistableOrderBackup.tableName())
                     .append(" or start up the server in the previous version and finish all running orders.");
        try {
          String lineseperator = System.getProperty("line.separator");
          errMsgBuilder.append(lineseperator)
                       .append("Running orders are stored in:")
                       .append(lineseperator);
          TableConfiguration[] configurations = ods.getTableConfigurations();
          Set<Long> persLayerIdsForOrderBackup = new HashSet<Long>(); 
          for (TableConfiguration config : configurations) {
            if (config.getTable().equals(OrderInstanceBackup.TABLE_NAME) ) {
              persLayerIdsForOrderBackup.add(config.getPersistenceLayerInstanceID());
            }
          }
          
          for (PersistenceLayerInstanceBean persLayer : ods.getPersistenceLayerInstances()) {
            if (persLayerIdsForOrderBackup.contains(persLayer.getPersistenceLayerInstanceID())) {
              errMsgBuilder.append(persLayer.getConnectionType())
                           .append(": ")
                           .append(persLayer.getPersistenceLayerInstance().getClass().getSimpleName())
                           .append(" [")
                           .append(persLayer.getConnectionParameter())
                           .append("]")
                           .append(lineseperator);
            }
          }
        } catch (Throwable t) {
          ; //errors during generation of a nicer exception message don't really concern us
        }
        throw new RuntimeException(errMsgBuilder.toString());
      }
    } catch (XynaException xe) {
      throw new RuntimeException("Error occurred during critical update", xe);
    }
    hasBeenExecuted = true;
  }

}
