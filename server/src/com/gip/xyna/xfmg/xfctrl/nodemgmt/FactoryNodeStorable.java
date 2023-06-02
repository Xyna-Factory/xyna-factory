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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryChannelIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = FactoryNodeStorable.COL_NAME, tableName = FactoryNodeStorable.TABLENAME)
public class FactoryNodeStorable extends Storable<FactoryNodeStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "factorynode";
  public static ResultSetReader<FactoryNodeStorable> reader = new FactoryNodeStorableReader();

  public static final String COL_NAME = "name";
  public static final String COL_DESCRIPTION = "description";
  public static final String COL_INSTANCE_ID = "instanceId";
  public static final String COL_REMOTE_ACCESSS_TYPE = "remoteAccessType";
  public static final String COL_REMOTE_ACCESS_SPECIFIC_PARAMS = "remoteAccessSpecificParams";

  @Column(name = COL_NAME, index = IndexType.PRIMARY)
  private String name;

  @Column(name = COL_DESCRIPTION)
  private String description;
  
  @Column(name = COL_INSTANCE_ID)
  private int instanceId;
  
  @Column(name = COL_REMOTE_ACCESSS_TYPE)
  private String remoteAccessType;
  
  @Column(name = COL_REMOTE_ACCESS_SPECIFIC_PARAMS, size = 4000)
  private final StringSerializableList<String> remoteAccessSpecificParams = StringSerializableList.separator(String.class);
  
  
  public FactoryNodeStorable() {
  }

  public FactoryNodeStorable(String name, String description, int instanceId, String remoteAccessType,
                             String[] remoteAccessSpecificParams) {
    this.name = name;
    this.description = description;
    this.instanceId = instanceId;
    this.remoteAccessType = remoteAccessType;
    this.remoteAccessSpecificParams.addAll(Arrays.asList(remoteAccessSpecificParams));
  }

  
  public FactoryNodeStorable(String name, String description, Integer instanceId,
                             InterFactoryChannelIdentifier channelIdentifier, Map<String, String> params,
                             Set<InterFactoryLinkProfileIdentifier> profiles) {
    this.name = name;
    this.description = description;
    this.instanceId = instanceId;
    this.remoteAccessType = channelIdentifier.toString();
    for (Entry<String, String> entry : params.entrySet()) {
      remoteAccessSpecificParams.add(entry.getKey() + "=" + entry.getValue());
    }
    StringBuilder sb = new StringBuilder();
    for (InterFactoryLinkProfileIdentifier profile : profiles) {
      sb.append(profile.toString()).append(" ");
    } 
    remoteAccessSpecificParams.add("profiles=" + sb.toString().trim());
  }

  @Override
  public ResultSetReader<? extends FactoryNodeStorable> getReader() {
    return reader;
  }

  @Override
  public String getPrimaryKey() {
    return name;
  }

  @Override
  public <U extends FactoryNodeStorable> void setAllFieldsFromData(U data) {
    FactoryNodeStorable cast = data;
    this.name = cast.name;
    this.description = cast.description;
    this.instanceId = cast.instanceId;
    this.remoteAccessType = cast.remoteAccessType;
    this.remoteAccessSpecificParams.setValues(cast.remoteAccessSpecificParams);
  }

  private static class FactoryNodeStorableReader implements ResultSetReader<FactoryNodeStorable> {
    public FactoryNodeStorable read(ResultSet rs) throws SQLException {
      FactoryNodeStorable result = new FactoryNodeStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(FactoryNodeStorable fns, ResultSet rs) throws SQLException {
    fns.name = rs.getString(COL_NAME);
    fns.description = rs.getString(COL_DESCRIPTION);
    fns.instanceId = rs.getInt(COL_INSTANCE_ID);
    fns.remoteAccessType = rs.getString(COL_REMOTE_ACCESSS_TYPE);
    fns.remoteAccessSpecificParams.deserializeFromString(rs.getString(COL_REMOTE_ACCESS_SPECIFIC_PARAMS));
  }

  
  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }

  
  public String getDescription() {
    return description;
  }

  
  public void setDescription(String description) {
    this.description = description;
  }

  
  public int getInstanceId() {
    return instanceId;
  }

  
  public void setInstanceId(int instanceId) {
    this.instanceId = instanceId;
  }

  
  public String getRemoteAccessType() {
    return remoteAccessType;
  }

  
  public void setRemoteAccessType(String remoteAccessType) {
    this.remoteAccessType = remoteAccessType;
  }
  
  
  public List<String> getRemoteAccessSpecificParams() {
    return remoteAccessSpecificParams;
  }

  public String[] getRemoteAccessSpecificParamsArray() {
    return remoteAccessSpecificParams.toArray(new String[remoteAccessSpecificParams.size()]);
  }

  public void setRemoteAccessSpecificParams(List<String> remoteAccessSpecificParams) {
    this.remoteAccessSpecificParams.setValues(remoteAccessSpecificParams);
  }

  
  public static FactoryNodeStorable[] getAll() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(FactoryNodeStorable.class).toArray(new FactoryNodeStorable[0]);
    } finally {
      finallyClose(con);
    }
  }

  public static FactoryNodeStorable get(String name) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      FactoryNodeStorable fns = new FactoryNodeStorable();
      fns.setName(name);
      
      con.queryOneRow(fns);
      return fns;
    } finally {
      finallyClose(con);
    }
  }
  
  public void persist() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(this);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  public void delete() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(this);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  
  private static void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
}
