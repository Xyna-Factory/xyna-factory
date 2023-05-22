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
package com.gip.xyna.update.specialstorables.serialdatapreserving;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.utils.streams.TeeInputStream;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;


public class OrderInstanceBackupPreservingSerializedData extends OrderInstanceBackup {
    private static final long serialVersionUID = 1L;
    private byte[] xynaOrderBytes;
    private byte[] detailsBytes;
    
    @Override
    public void serializeByColName(String colName, Object val, OutputStream os) throws IOException {
      if( colName.equals(COL_XYNAORDER) ) {
        StreamUtils.copy( new ByteArrayInputStream(xynaOrderBytes), os );
      } else if( colName.equals(COL_DETAILS) ) {
        StreamUtils.copy( new ByteArrayInputStream(detailsBytes), os );
      } else {
        super.serializeByColName(colName, val, os);
      }
    }
    
    @Override
    public Object deserializeByColName(String colName, InputStream is) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      TeeInputStream tis = new TeeInputStream(is, baos);
      Object o = super.deserializeByColName(colName, tis);
      
      if( colName.equals(COL_XYNAORDER) ) {
        xynaOrderBytes = baos.toByteArray();
      } else if( colName.equals(COL_DETAILS) ) {
        detailsBytes = baos.toByteArray();
      }
      return o;
    }
    
    public static ResultSetReader<OrderInstanceBackupPreservingSerializedData> reader = new ResultSetReader<OrderInstanceBackupPreservingSerializedData>() {

      public OrderInstanceBackupPreservingSerializedData read(ResultSet rs) throws SQLException {
        OrderInstanceBackupPreservingSerializedData oi = new OrderInstanceBackupPreservingSerializedData();
        ClusteredStorable.fillByResultSet(oi, rs);
        oi.id = rs.getLong(COL_ID);
        //logger.info("ResultSetReader read id "+ oi.id);
        try {
          //logger.info( "reading xynaOrder ");
          readOrderFromBackup(rs, oi);
          //logger.info( "reading details ");
          oi.details = (OrderInstanceDetails) oi.readBlobbedJavaObjectFromResultSet(rs, COL_DETAILS);
          oi.backupCause = rs.getString(COL_BACKUP_CAUSE);
          oi.rootId = rs.getLong(COL_ROOT_ID);
          oi.bootCntId = rs.getLong(COL_BOOTCNTID);
          if (oi.bootCntId == 0 && rs.wasNull()) {
            oi.bootCntId = null;
          }
          //logger.info("ResultSetReader reading finished");
          return oi;
        } catch ( Throwable t ) {
          //logger.info("ResultSetReader failed ", t);
          throw new RuntimeException(t);
        }
      }

    };
    

    protected static void readOrderFromBackup(ResultSet rs, OrderInstanceBackupPreservingSerializedData oi) throws SQLException {
      oi.xynaorder = (XynaOrderServerExtension) oi.readBlobbedJavaObjectFromResultSet(rs, COL_XYNAORDER);
      if (oi.xynaorder != null) {
        for (XynaOrderServerExtension xo : oi.xynaorder.getOrderAndChildrenRecursively()) {
          xo.setHasBeenBackuppedAfterChange(true);
          xo.setHasBeenBackuppedAtLeastOnce();
        }
      }
    }

    
  }
