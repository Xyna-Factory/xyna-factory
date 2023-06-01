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
package com.gip.xyna.xdev.xlibdev.repositoryaccess;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = RepositoryAccessStorable.COL_NAME, tableName = RepositoryAccessStorable.TABLENAME)
public class RepositoryAccessStorable extends Storable<RepositoryAccessStorable> {

  private static final long serialVersionUID = 1L;
  public static final String COL_NAME = "name";
  public static final String COL_FQCLASSNAME = "fqclassname";
  public static final String TABLENAME = "repositoryaccess";

  @Column(name = COL_NAME)
  private String name;

  @Column(name = COL_FQCLASSNAME)
  private String fqClassName;

  public RepositoryAccessStorable() {
    
  }

  public static RepositoryAccessStorable[] getAll() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(RepositoryAccessStorable.class).toArray(new RepositoryAccessStorable[0]);
    } finally {
      con.closeConnection();
    }
  }


  public String getName() {
    return name;
  }


  public String getFqClassName() {
    return fqClassName;
  }


  public static void persist(String name, String fqClassName) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      RepositoryAccessStorable cas = new RepositoryAccessStorable();
      cas.fqClassName = fqClassName;
      cas.name = name;
      con.persistObject(cas);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private static final ResultSetReader<RepositoryAccessStorable> reader = new ResultSetReader<RepositoryAccessStorable>() {

    public RepositoryAccessStorable read(ResultSet rs) throws SQLException {
      RepositoryAccessStorable cas = new RepositoryAccessStorable();
      cas.name = rs.getString(COL_NAME);
      cas.fqClassName = rs.getString(COL_FQCLASSNAME);
      return cas;
    }
  };


  @Override
  public ResultSetReader<? extends RepositoryAccessStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return name;
  }


  @Override
  public <U extends RepositoryAccessStorable> void setAllFieldsFromData(U data) {
    RepositoryAccessStorable cast = data;
    name = cast.name;
    fqClassName = cast.fqClassName;
  }

}
