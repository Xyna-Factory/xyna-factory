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

package com.gip.xyna.update.specialstorablesignoringserialversionuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xfractwfe.SerialVersionIgnoringObjectInputStream;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;


public class OrderInstanceBackupIgnoringSerialVersionUID extends OrderInstanceBackup {

  private static final long serialVersionUID = 1L;
  

  @Override
  public ObjectInputStream getObjectInputStreamForStorable(InputStream in) throws IOException {
    // extra nasty hack: this only works because the revision has been read before
    return new SerialVersionIgnoringObjectInputStream(in, getRevision());
  }


  public static ResultSetReader<OrderInstanceBackupIgnoringSerialVersionUID> reader =
      new ResultSetReader<OrderInstanceBackupIgnoringSerialVersionUID>() {

        public OrderInstanceBackupIgnoringSerialVersionUID read(ResultSet rs) throws SQLException {
          OrderInstanceBackupIgnoringSerialVersionUID oi = new OrderInstanceBackupIgnoringSerialVersionUID();
          ClusteredStorable.fillByResultSet(oi, rs);
          OrderInstanceBackup.fillByResultSet(oi, rs);
          return oi;
        }

  };

  public ResultSetReader<? extends OrderInstanceBackupIgnoringSerialVersionUID> getReader() {
    return reader;
  }
  
}
