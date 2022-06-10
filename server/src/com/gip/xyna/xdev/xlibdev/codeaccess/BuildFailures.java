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
package com.gip.xyna.xdev.xlibdev.codeaccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.BuildFailure;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = BuildFailures.COL_ID, tableName = BuildFailures.TABLENAME)
public class BuildFailures extends Storable<BuildFailures>{

  private static final long serialVersionUID = 1L;
  
  public static final String TABLENAME = "codeaccessbuildfailures";
  public static final String COL_ID = "id";
  public static final String COL_CODEACCESSNAME = "codeaccessname";
  public static final String COL_REVISION = "revision";
  public static final String COL_FAILURES = "failures";


  @Column(name = COL_ID)
  private String id;
  
  @Column(name = COL_CODEACCESSNAME)
  private String codeaccessname;
  
  @Column(name = COL_REVISION)
  private Long revision;
  
  @Column(name = COL_FAILURES, type=ColumnType.BLOBBED_JAVAOBJECT)
  private Collection<BuildFailure> failures;
  
  
  public BuildFailures() {
  }
  
  public BuildFailures(String codeaccessname, Collection<BuildFailure> failures, Long revision) {
    this.id = codeaccessname + "#" + revision;
    this.codeaccessname = codeaccessname;
    this.revision = revision;
    this.failures = new ArrayList<BuildFailure>(failures);
    for (BuildFailure buildFailure : this.failures) {
      buildFailure.prepareForSerialization();
    }
  }
  
  @Override
  public String getPrimaryKey() {
    return id;
  }

  
  public String getId() {
    return id;
  }

  
  public void setId(String id) {
    this.id = id;
  }

  
  public String getCodeaccessname() {
    return codeaccessname;
  }

  
  public void setCodeaccessname(String codeaccessname) {
    this.codeaccessname = codeaccessname;
  }

  
  public Long getRevision() {
    return revision;
  }

  
  public void setRevision(Long revision) {
    this.revision = revision;
  }

  
  public Collection<BuildFailure> getFailures() {
    return failures;
  }

  
  public void setFailures(Collection<BuildFailure> failures) {
    this.failures = failures;
  }


  private static final ResultSetReader<BuildFailures> reader =
    new ResultSetReader<BuildFailures>() {

      public BuildFailures read(ResultSet rs) throws SQLException {
        BuildFailures bf = new BuildFailures();
        bf.id = rs.getString(COL_ID);
        bf.codeaccessname = rs.getString(COL_CODEACCESSNAME);
        bf.revision = rs.getLong(COL_REVISION);
        bf.failures = (List<BuildFailure>) bf.readBlobbedJavaObjectFromResultSet(rs, COL_FAILURES);
        return bf;
      }

    };

  @Override
  public ResultSetReader<? extends BuildFailures> getReader() {
    return reader;
  }
  
  
  @Override
  public <U extends BuildFailures> void setAllFieldsFromData(U data) {
    BuildFailures cast = data;
    this.id = cast.id;
    this.codeaccessname = cast.codeaccessname;
    this.revision = cast.revision;
    this.failures = cast.failures;
  }
  
  
  public static void store(String codeaccessname, Collection<BuildFailure> failures, Long revision) throws PersistenceLayerException {
    BuildFailures bf = new BuildFailures(codeaccessname, failures, revision);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(bf);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  
  public static Collection<BuildFailure> restore(String codeaccessname, Long revision) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<BuildFailures> failures = con.loadCollection(BuildFailures.class);
      for (BuildFailures buildFailures : failures) {
        if (buildFailures.getRevision().equals(revision)
              && buildFailures.getCodeaccessname().equals(codeaccessname)) {
          return buildFailures.getFailures();
        }
      }
    } finally {
      con.closeConnection();
    }
    return Collections.emptyList();
  }

  public static void delete(String codeaccessname, Long revision) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      BuildFailures bf = new BuildFailures();
      bf.id = codeaccessname + "#" + revision;
      con.deleteOneRow(bf);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
}
