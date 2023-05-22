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
package com.gip.xyna.xprc.xfractwfe;



import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;



/*
 * Differs only in:
 * if (svioib.getRevision().equals(from)) {
     svioib.setRevision(to);
   }
 * there should be a better way to solve this  
 */
public class MigrationSerialVersionIgnoringOrderInstanceBackup extends OrderInstanceBackup {


  private static final long serialVersionUID = 2671022750766805608L;
  private static final Logger logger = CentralFactoryLogging.getLogger(MigrationSerialVersionIgnoringOrderInstanceBackup.class);

  public transient Long rootRevision;

  public MigrationSerialVersionIgnoringOrderInstanceBackup(Long rootRevision) {
    this.rootRevision = rootRevision;
  }


  @Override
  public ObjectInputStream getObjectInputStreamForStorable(InputStream in) throws IOException {
    logger.trace("getObjectInputStreamForStorable: returning SerialVersionIgnoringObjectInputStream");
    if (rootRevision == null) {
      return new SerialVersionIgnoringObjectInputStream(in, getRevision()); //das geht, weil unten im reader zuerst die revision ausgelesen wird
    } else {
      return new SerialVersionIgnoringObjectInputStream(in, rootRevision);      
    }
  }


  /**
   * liest das backup derart, dass:<br>
   * - in dem zur�ckgegebenen backup die revision auf "to" steht, falls vorher "from" drin stand<p>
   * - beim deserialisieren die bestehende serialversionuid ignoriert wird (sondern die von rootRevision angenommen wird). falls rootRevision null ist, wird die von oib.getRevision verwendet<p>
   * @param from
   * @param to
   * @param rootRevision
   * @return
   */
  public static ResultSetReader<MigrationSerialVersionIgnoringOrderInstanceBackup> getSerialVersionIgnoringReader(Long from, Long to, Long rootRevision) {
    return new SerialVersionIgnoringReader(from, to, rootRevision);
  }
  
  private static class SerialVersionIgnoringReader implements ResultSetReader<MigrationSerialVersionIgnoringOrderInstanceBackup> {
    
    private Long from;
    private Long to;
    private Long rootRevision;
    
    SerialVersionIgnoringReader(Long from, Long to, Long rootRevision) {
      this.from = from;
      this.to = to;
      this.rootRevision = rootRevision;
    }


    public MigrationSerialVersionIgnoringOrderInstanceBackup read(ResultSet rs) throws SQLException {
      MigrationSerialVersionIgnoringOrderInstanceBackup svioib = new MigrationSerialVersionIgnoringOrderInstanceBackup(rootRevision);

      svioib.id = rs.getLong(COL_ID);
      svioib.setRevision(rs.getLong(COL_REVISION));
      if (svioib.getRevision().equals(from)) {
        svioib.setRevision(to);
      }
      try {
        svioib.xynaorder = (XynaOrderServerExtension) svioib.readBlobbedJavaObjectFromResultSet(rs, COL_XYNAORDER, String.valueOf(svioib.id));
      } catch (SQLException t) {
        throw t;
      }
      try {
        svioib.details = (OrderInstanceDetails) svioib.readBlobbedJavaObjectFromResultSet(rs, COL_DETAILS, String.valueOf(svioib.id));
      } catch (SQLException t) {
        throw t;
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
