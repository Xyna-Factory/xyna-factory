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
package com.gip.xyna.xprc.xfractwfe;



import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;



/*
 * This class does not really need to extend OrderInstanceBackup but it has to be a Storable,
 * provide a reader for the OrderInstanceBackup and override getObjectInputStreamForStorable
 */
public class SerialVersionIgnoringOrderInstanceBackup extends OrderInstanceBackup {


  private static final long serialVersionUID = 2671022750766805608L;
  private static final Logger logger = CentralFactoryLogging.getLogger(SerialVersionIgnoringOrderInstanceBackup.class);


  public SerialVersionIgnoringOrderInstanceBackup() {
  }


  public SerialVersionIgnoringOrderInstanceBackup(Long orderId) {
    this.id = orderId;
  }


  @Override
  public ObjectInputStream getObjectInputStreamForStorable(InputStream in) throws IOException {
    logger.trace("getObjectInputStreamForStorable: returning SerialVersionIgnoringObjectInputStream");
    return new SerialVersionIgnoringObjectInputStream(in, getRevision());
  }


  private static ResultSetReader<SerialVersionIgnoringOrderInstanceBackup> reader = new SerialVersionIgnoringReader();


  public static ResultSetReader<SerialVersionIgnoringOrderInstanceBackup> getSerialVersionIgnoringReader() {
    return reader;
  }
  
  private static class SerialVersionIgnoringReader implements ResultSetReader<SerialVersionIgnoringOrderInstanceBackup> {

    public SerialVersionIgnoringOrderInstanceBackup read(ResultSet rs) throws SQLException {

      // FIXME die Felder werden hier teilweise leer gelassen, obwohl man das Entry dann später benutzt, um wieder
      //       in die DB zu schreiben. Dadurch verliert man ggf. Daten!
      SerialVersionIgnoringOrderInstanceBackup svioib = new SerialVersionIgnoringOrderInstanceBackup();

      svioib.id = rs.getLong(COL_ID);
      svioib.setRevision(rs.getLong(COL_REVISION));
      try {
        svioib.xynaorder = (XynaOrderServerExtension) svioib.readBlobbedJavaObjectFromResultSet(rs, COL_XYNAORDER, String.valueOf(svioib.id));
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("Failed to read order", t);
        // do as much as possible and don't abort (it might be safer to delete that entry)
      }
      try {
        svioib.details = (OrderInstanceDetails) svioib.readBlobbedJavaObjectFromResultSet(rs, COL_DETAILS, String.valueOf(svioib.id));
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("Failed to read audit", t);
        // do as much as possible and don't abort (it might be safer to delete that entry)
      }

      // read some fields that should always work
      svioib.backupCause = rs.getString(COL_BACKUP_CAUSE);
      svioib.rootId = rs.getLong(COL_ROOT_ID);
      svioib.bootCntId = rs.getLong(COL_BOOTCNTID);
      svioib.setBinding(rs.getInt(COL_BINDING));

      return svioib;

    }
 
  }

}
