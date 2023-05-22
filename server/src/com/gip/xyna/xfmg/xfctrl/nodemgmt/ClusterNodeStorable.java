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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
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

@Persistable(primaryKey = ClusterNodeStorable.COL_NAME, tableName = ClusterNodeStorable.TABLENAME)
public class ClusterNodeStorable extends Storable<ClusterNodeStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "clusternode";
  public static ResultSetReader<ClusterNodeStorable> reader = new ClusterNodeStorableReader();

  public static final String COL_NAME = "name";
  public static final String COL_DESCRIPTION = "description";
  public static final String COL_FACTORY_NODES = "factorynodes";

  @Column(name = COL_NAME, index = IndexType.PRIMARY)
  private String name;

  @Column(name = COL_DESCRIPTION)
  private String description;
  
  @Column(name = COL_FACTORY_NODES)
  private final StringSerializableList<String> serializableFactoryNodes = StringSerializableList.separator(String.class);
  private transient List<FactoryNode> factoryNodes;
  
  
  public ClusterNodeStorable() {
  }

  public ClusterNodeStorable(String name, String description, int instanceId, List<FactoryNode> factoryNodes) {
    this.name = name;
    this.description = description;
    this.serializableFactoryNodes.addAll(extractNames(factoryNodes));
    this.factoryNodes = factoryNodes;
  }

  private List<String> extractNames(List<FactoryNode> factoryNodes) {
    List<String> names = new ArrayList<>();
    for (FactoryNode factoryNode : factoryNodes) {
      names.add(factoryNode.getNodeInformation().getName());
    }
    return names;
  }

  public ClusterNodeStorable(ClusterNodeStorable nodeInformation) {
    this.name = nodeInformation.name;
    this.description = nodeInformation.description;
    this.factoryNodes = nodeInformation.factoryNodes;
    this.serializableFactoryNodes.setValues(nodeInformation.serializableFactoryNodes);
  }

  @Override
  public ResultSetReader<? extends ClusterNodeStorable> getReader() {
    return reader;
  }

  @Override
  public String getPrimaryKey() {
    return name;
  }

  @Override
  public <U extends ClusterNodeStorable> void setAllFieldsFromData(U data) {
    ClusterNodeStorable cast = data;
    this.name = cast.name;
    this.description = cast.description;
    this.serializableFactoryNodes.setValues(cast.serializableFactoryNodes);
  }

  private static class ClusterNodeStorableReader implements ResultSetReader<ClusterNodeStorable> {
    public ClusterNodeStorable read(ResultSet rs) throws SQLException {
      ClusterNodeStorable result = new ClusterNodeStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(ClusterNodeStorable cns, ResultSet rs) throws SQLException {
    cns.name = rs.getString(COL_NAME);
    cns.description = rs.getString(COL_DESCRIPTION);
    cns.serializableFactoryNodes.deserializeFromString(rs.getString(COL_FACTORY_NODES));
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


  public StringSerializableList<String> getSerializableFactoryNodes() {
    return serializableFactoryNodes;
  }

  
  public void setFactoryNodeNames(List<String> factoryNodeNames) {
    this.serializableFactoryNodes.setValues(factoryNodeNames);
  }
  
  
  public List<FactoryNode> getFactoryNodes() {
    if (factoryNodes == null) {
      factoryNodes = restoreFactoryNodes();
    }
    return factoryNodes;
  }

  
  private List<FactoryNode> restoreFactoryNodes() {
    List<FactoryNode> fns = new ArrayList<>();
    NodeManagement nodeMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    for (String factoryNodeName : serializableFactoryNodes) {
      fns.add(nodeMgmt.getNodeByName(factoryNodeName));
    }
    return fns;
  }

  public void setFactoryNodes(List<FactoryNode> factoryNodes) {
    this.factoryNodes = factoryNodes;
    this.serializableFactoryNodes.setValues( extractNames(factoryNodes));
  }

  

  
  public static ClusterNodeStorable[] getAll() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(ClusterNodeStorable.class).toArray(new ClusterNodeStorable[0]);
    } finally {
      finallyClose(con);
    }
  }

  public static ClusterNodeStorable get(String name) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      ClusterNodeStorable fns = new ClusterNodeStorable();
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
      logger.warn("1234567890 : persist " + getSerializableFactoryNodes().serializeToString() );
      
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
