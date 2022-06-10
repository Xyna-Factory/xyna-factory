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
package com.gip.xyna.update.specialstorables.serialdatapreserving;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.utils.streams.TeeInputStream;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xprcods.orderarchive.AuditData;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;

public class OrderInstanceDetailsPreservingSerializedData extends OrderInstanceDetails {
    private static final long serialVersionUID = 1L;
    private byte[] auditDataBytes;
    
    @Override
    public void serializeByColName(String colName, Object val, OutputStream os) throws IOException {
      if( colName.equals(COL_AUDIT_DATA_AS_JAVA_OBJECT) ) {
        StreamUtils.copy( new ByteArrayInputStream(auditDataBytes), os );
      } else {
        super.serializeByColName(colName, val, os);
      }
    }
    
    @Override
    public Object deserializeByColName(String colName, InputStream is) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      TeeInputStream tis = new TeeInputStream(is, baos);
      Object o = super.deserializeByColName(colName, tis);
      
      if( colName.equals(COL_AUDIT_DATA_AS_JAVA_OBJECT) ) {
        auditDataBytes = baos.toByteArray();
      }
      return o;
    }

    @Override
    public ResultSetReader<OrderInstanceDetailsPreservingSerializedData> getReader() {
      return new OrderInstanceDetailsPreservingSerializedDataReader();
    }
    
    private static class OrderInstanceDetailsPreservingSerializedDataReader implements ResultSetReader<OrderInstanceDetailsPreservingSerializedData> {

      public OrderInstanceDetailsPreservingSerializedData read(ResultSet rs) throws SQLException {
        OrderInstanceDetailsPreservingSerializedData oi = new OrderInstanceDetailsPreservingSerializedData();
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


    
  }
