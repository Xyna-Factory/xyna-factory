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
package com.gip.xyna.xdev.xlibdev.xmomaccess;

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

@Persistable(primaryKey = XMOMAccessInstanceStorable.COL_ID, tableName = XMOMAccessInstanceStorable.TABLENAME)
public class XMOMAccessInstanceStorable extends Storable<XMOMAccessInstanceStorable>{

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "xmomaccessinstance";
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

  
  private static final ResultSetReader<XMOMAccessInstanceStorable> reader = new ResultSetReader<XMOMAccessInstanceStorable>() {

    public XMOMAccessInstanceStorable read(ResultSet rs) throws SQLException {
      XMOMAccessInstanceStorable xais = new XMOMAccessInstanceStorable();
      xais.name = rs.getString(COL_NAME);
      xais.repositoryname = rs.getString(COL_REPOSITORYNAME);
      xais.revision = rs.getLong(COL_REVISION);
      xais.id = rs.getString(COL_ID);
      return xais;
    }

  };


  @Override
  public ResultSetReader<? extends XMOMAccessInstanceStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends XMOMAccessInstanceStorable> void setAllFieldsFromData(U data) {
    XMOMAccessInstanceStorable cast = data;
    this.id = cast.id;
    this.name = cast.name;
    this.revision = cast.revision;
    this.repositoryname = cast.repositoryname;
  }


  public static XMOMAccessInstanceStorable[] getAll() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(XMOMAccessInstanceStorable.class).toArray(new XMOMAccessInstanceStorable[0]);
    } finally {
      con.closeConnection();
    }
  }


  public String getId() {
    return id;
  }


  public Long getRevision() {
    return revision;
  }


  public String getRepositoryAccessName() {
    return repositoryname;
  }


  public String getName() {
    return name;
  }


  public static void create(Long revision, String name, String repositoryname) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      XMOMAccessInstanceStorable xais = new XMOMAccessInstanceStorable();
      xais.id = name + "#" + revision;
      xais.name = name;
      xais.repositoryname = repositoryname;
      xais.revision = revision;
      con.persistObject(xais);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  public static void delete(XMOMAccess removed) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      XMOMAccessInstanceStorable xais = new XMOMAccessInstanceStorable();
      xais.id = removed.getName() + "#" + removed.getRevision();
      con.deleteOneRow(xais);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
}
