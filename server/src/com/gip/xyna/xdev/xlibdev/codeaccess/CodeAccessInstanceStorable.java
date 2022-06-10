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

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = CodeAccessInstanceStorable.COL_ID, tableName = CodeAccessInstanceStorable.TABLENAME)
public class CodeAccessInstanceStorable extends Storable<CodeAccessInstanceStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "codeaccessinstance";
  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_REPOSITORYNAME = "repositoryname";
  public static final String COL_REVISION = "revision";

  
  @Column(name = COL_ID)
  private String id;

  @Column(name = COL_NAME)
  private String name;

  @Column(name = COL_REPOSITORYNAME)
  private String repositoryname;

  @Column(name = COL_REVISION)
  private Long revision;


  public CodeAccessInstanceStorable() {

  }


  private static final ResultSetReader<CodeAccessInstanceStorable> reader =
      new ResultSetReader<CodeAccessInstanceStorable>() {

        public CodeAccessInstanceStorable read(ResultSet rs) throws SQLException {
          CodeAccessInstanceStorable cais = new CodeAccessInstanceStorable();
          cais.id = rs.getString(COL_ID);
          cais.name = rs.getString(COL_NAME);
          cais.repositoryname = rs.getString(COL_REPOSITORYNAME);
          cais.revision = rs.getLong(COL_REVISION);
          return cais;
        }

      };


  @Override
  public ResultSetReader<? extends CodeAccessInstanceStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends CodeAccessInstanceStorable> void setAllFieldsFromData(U data) {
    CodeAccessInstanceStorable cast = data;
    this.id = cast.id;
    this.name = cast.name;
    this.revision = cast.revision;
    this.repositoryname = cast.repositoryname;
  }


  public static void delete(CodeAccess removed) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      CodeAccessInstanceStorable cais = new CodeAccessInstanceStorable();
      cais.id = removed.getName() + "#" + removed.getRevision();
      con.deleteOneRow(cais);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public static void create(Long revision, String repositoryname, String name)
      throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      CodeAccessInstanceStorable cais = new CodeAccessInstanceStorable();
      cais.id = name + "#" + revision;
      cais.name = name;
      cais.repositoryname = repositoryname;
      cais.revision = revision;
      con.persistObject(cais);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public static CodeAccessInstanceStorable[] getAll() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(CodeAccessInstanceStorable.class).toArray(new CodeAccessInstanceStorable[0]);
    } finally {
      con.closeConnection();
    }
  }


  public Long getRevision() {
    return revision;
  }


  public String getRepositoryAccessName() {
    return repositoryname;
  }
  
  
  public String getRepositoryName() {
    return getRepositoryAccessName();
  }


  public String getName() {
    return name;
  }

  
  public String getId() {
    return id;
  }
}
