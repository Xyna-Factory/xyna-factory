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
package com.gip.xyna.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.types.BLOB;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;


public class UpdateOrderArchiveNewXynaExceptionInformation extends Update {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateOrderArchiveNewXynaExceptionInformation.class);

  private final Version versionAllowedForUpdate;
  private final Version versionAfterUpdate;


  public UpdateOrderArchiveNewXynaExceptionInformation(Version versionAllowedForUpdate, Version versionAfterUpdate) {
    this.versionAllowedForUpdate = versionAllowedForUpdate;
    this.versionAfterUpdate = versionAfterUpdate;
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return versionAllowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return versionAfterUpdate;
  }

  private static class UpdaterInputStream extends ObjectInputStream {

    public UpdaterInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
      ObjectStreamClass desc = super.readClassDescriptor();
      if (desc.getName().equals("com.gip.xyna.xprc.xprcods.currprocdb.XynaExceptionInformation")) {
        return ObjectStreamClass.lookup(XynaExceptionInformation.class);
      }
      return desc;
    }
  }
  
  private static class UpdaterOutputStream extends ObjectOutputStream {

    public UpdaterOutputStream(OutputStream out) throws IOException {
      super(out);
    }

  }

  private static class HelperClass {

    private List<XynaExceptionInformation> exceptionInfo;
    private long id;


    public HelperClass(long id, List<XynaExceptionInformation> exceptions) {
      this.exceptionInfo = exceptions;
      this.id = id;
    }


    public List<XynaExceptionInformation> getExceptionInformation() {
      return exceptionInfo;
    }


    public long getId() {
      return id;
    }
    
  }

  @Override
  protected void update() throws XynaException {

    // aufträge aus orderarchive lesen, aktualisieren und speichern
    Persistable p = Storable.getPersistable(OrderInstance.class);
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {

      long identifierNumber = 512389; // workaround um das fragezeichen in das statement zu bekommen
      OrderInstanceSelect ois = new OrderInstanceSelect().selectAllForOrderInstance().whereId()
                      .isBiggerThan(identifierNumber).finalizeSelect(OrderInstanceSelect.class);
      String queryString = ois.getSelectString().replaceAll("" + identifierNumber, "?") + " order by "
                      + OrderInstanceColumn.C_ID.getColumnName();

      PreparedQuery<HelperClass> pq = con.prepareQuery(new Query<HelperClass>(queryString, new ResultSetReader<HelperClass>() {

        public HelperClass read(ResultSet rs) throws SQLException {
          Long id = rs.getLong(OrderInstance.COL_ID);
          Blob blob = rs.getBlob(OrderInstance.COL_EXCEPTIONS);
          if (rs.wasNull()) {
            return new HelperClass(id, null);
          }
          InputStream is = blob.getBinaryStream();
          try {
            SerializableClassloadedObject sco;
            try (UpdaterInputStream uis = new UpdaterInputStream(is)) {
              sco = (SerializableClassloadedObject) uis.readObject();
            }
            if (sco == null) {
              return new HelperClass(id, new ArrayList<XynaExceptionInformation>());
            } else {
              List<XynaExceptionInformation> exceptions = (List<XynaExceptionInformation>) sco.getObject();
              return new HelperClass(id, exceptions);
            }
          } catch (IOException e) {
            throw new SQLException("could not create updateInputStream: " + e.getMessage() + " for id " + id);
          } catch (ClassNotFoundException e) {
            throw new SQLException("could not read from blob: " + e.getMessage() + " for id " + id);
          } finally {
            try {
              blob.free();
            } catch (Exception e) {
              try {
                is.close();
              } catch (IOException e1) {
                logger.info("Could not close blob", e);
              }
            }
          }

        }

      }));

      PreparedCommand pc =
          con.prepareCommand(new Command("update " + p.tableName() + " set " + OrderInstance.COL_EXCEPTIONS
              + " = ? where " + p.primaryKey() + " = ?"));

      int numberPerRoundTrip = 50;
      boolean finished = false;
      long bottom = -1;

      while (!finished) {

        List<HelperClass> list = con.query(pq, new Parameter(bottom), numberPerRoundTrip);
        if (list.size() == 0) {
          finished = true;
        }

        for (HelperClass a : list) {
          if (a == null) {
            continue;
          }
          if (a.getExceptionInformation() == null) {
            bottom = a.getId();
            continue;
          }
          ByteArrayOutputStream data = new ByteArrayOutputStream();
          try {
            UpdaterOutputStream uos = new UpdaterOutputStream(data);
            uos.writeObject(a.getExceptionInformation());
            uos.flush();
          } catch (IOException e) {
            throw new XynaException("could not create UpdaterOutputStream for id " + a.getId(), e);
          }
          logger.debug("Updating exception information for order id " + a.getId());
          con.executeDML(pc, new Parameter(new BLOB(data), a.getId()));
          bottom = a.getId();
        }

      }

      con.commit();

    } finally {
      con.closeConnection();
    }
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return false;
  }

}
