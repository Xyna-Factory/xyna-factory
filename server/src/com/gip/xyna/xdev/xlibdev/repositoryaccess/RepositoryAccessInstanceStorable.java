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
import java.util.List;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = RepositoryAccessInstanceStorable.COL_REPOSITORYACCESSINSTANCENAME, tableName = RepositoryAccessInstanceStorable.TABLENAME)
public class RepositoryAccessInstanceStorable extends Storable<RepositoryAccessInstanceStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "repositoryaccessinstance";
  public static final String COL_REPOSITORYACCESSINSTANCENAME = "name";
  public static final String COL_REPOSITORYACCESSNAME = "typename";
  public static final String COL_PARAMETER = "parameters";
  public static final String COL_LOCAL_REPOSITORY_BASE = "localRepositoryBase";


  @Column(name = COL_REPOSITORYACCESSINSTANCENAME)
  private String name;
  
  @Column(name = COL_REPOSITORYACCESSNAME)
  private String typename;

  @Column(name = COL_PARAMETER, size = 4000)
  private final StringSerializableList<String> parameters = StringSerializableList.separator(String.class);
  
  @Column(name = COL_LOCAL_REPOSITORY_BASE)
  private String localRepositoryBase;


  public RepositoryAccessInstanceStorable() {

  }


  private static final ResultSetReader<RepositoryAccessInstanceStorable> reader =
      new ResultSetReader<RepositoryAccessInstanceStorable>() {

        public RepositoryAccessInstanceStorable read(ResultSet rs) throws SQLException {
          RepositoryAccessInstanceStorable cais = new RepositoryAccessInstanceStorable();
          cais.name = rs.getString(COL_REPOSITORYACCESSINSTANCENAME);
          cais.typename = rs.getString(COL_REPOSITORYACCESSNAME);
          cais.parameters.deserializeFromString(rs.getString(COL_PARAMETER));
          cais.localRepositoryBase = rs.getString(COL_LOCAL_REPOSITORY_BASE);
          return cais;
        }

      };


  @Override
  public ResultSetReader<? extends RepositoryAccessInstanceStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return name;
  }


  @Override
  public <U extends RepositoryAccessInstanceStorable> void setAllFieldsFromData(U data) {
    RepositoryAccessInstanceStorable cast = data;
    this.parameters.setValues(cast.parameters);
    this.name = cast.name;
    this.typename = cast.typename;
    this.localRepositoryBase = cast.localRepositoryBase;
  }


  public static void delete(RepositoryAccess removed) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      RepositoryAccessInstanceStorable cais = new RepositoryAccessInstanceStorable();
      cais.name = removed.getName();
      con.deleteOneRow(cais);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public static void create(String repositoryAccessInstanceName, String repositoryAccessName, String localRepositoryBase, List<String> parameter)
      throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      RepositoryAccessInstanceStorable cais = new RepositoryAccessInstanceStorable();
      cais.name = repositoryAccessInstanceName;
      cais.typename = repositoryAccessName;
      cais.parameters.addAll(parameter);
      cais.localRepositoryBase = localRepositoryBase;
      con.persistObject(cais);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public static RepositoryAccessInstanceStorable[] getAll() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(RepositoryAccessInstanceStorable.class).toArray(new RepositoryAccessInstanceStorable[0]);
    } finally {
      con.closeConnection();
    }
  }


  public String getName() {
    return name;
  }
  
  
  public String getTypename() {
    return typename;
  }


  public List<String> getParameters() {
    return parameters;
  }


  public String getLocalRepositoryBase() {
    return localRepositoryBase;
  }
}
