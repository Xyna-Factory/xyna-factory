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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import java.sql.ResultSet;
import java.sql.SQLException;



@Persistable(tableName = XSORClusterInstanceStorable.TABLE_NAME, primaryKey = XSORClusterInstanceStorable.COL_ID)
public class XSORClusterInstanceStorable extends Storable<XSORClusterInstanceStorable> {

  private static final long serialVersionUID = 1L;
  public static final String TABLE_NAME = "xsorclusterinstance";
  public static final String COL_ID = "id";
  public static final String COL_HOSTNAME = "hostname";
  public static final String COL_IC_SERVER_PORT = "icserverport";
  public static final String COL_IC_REMOTE_PORT = "icremoteport";
  public static final String COL_IC_CORR_QUEUE_LENGTH = "iccorrqueuelength";
  public static final String COL_CLUSTERSTATE = "clusterstate";
  public static final String COL_ONLINE = "online";
  private static final String COL_WASMASTER = "wasmaster";
  private static final String COL_CM_HOST_REMOTE = "cmhostnameremote";
  private static final String COL_CM_PORT_REMOTE = "cmportregistryremote";
  private static final String COL_CM_HOST_LOCAL = "cmhostnamelocal";
  private static final String COL_CM_PORT_REGISTRY_LOCAL = "cmportregistrylocal";
  private static final String COL_CM_PORT_COM_LOCAL = "cmportcommunicationlocal";
  private static final String COL_PERSIST_MAX_BATCH_SIZE = "persistmaxbatchsize";
  private static final String COL_PERSIST_INTERVAL_MS = "persistintervalms";
  private static final String COL_PERSIST_SYNCHRONOUS = "persistsynchronous";
  private static final String COL_NODE_ID = "nodeid";
  private static final String COL_NODE_PREFERENCE = "nodepreference";
  private static final String COL_CM_AVAILABILITYDELAY_MS = "availabilitydelayms";

  private static ResultSetReader<XSORClusterInstanceStorable> reader =
      new ResultSetReader<XSORClusterInstanceStorable>() {

        public XSORClusterInstanceStorable read(ResultSet rs) throws SQLException {
          XSORClusterInstanceStorable rmici = new XSORClusterInstanceStorable();
          rmici.id = rs.getLong(COL_ID);
          rmici.hostname = rs.getString(COL_HOSTNAME);
          rmici.icRemotePort = rs.getInt(COL_IC_REMOTE_PORT);
          rmici.icServerPort = rs.getInt(COL_IC_SERVER_PORT);
          rmici.icCorrQueueLength=rs.getInt(COL_IC_CORR_QUEUE_LENGTH);
          rmici.clusterstate = rs.getString(COL_CLUSTERSTATE);
          rmici.online = rs.getBoolean(COL_ONLINE);
          rmici.wasMaster = rs.getBoolean(COL_WASMASTER);
          rmici.cmHostNameRemote = rs.getString(COL_CM_HOST_REMOTE);
          rmici.cmHostNameLocal = rs.getString(COL_CM_HOST_LOCAL);
          rmici.cmPortRegistryRemote = rs.getInt(COL_CM_PORT_REMOTE);
          rmici.cmPortRegistryLocal = rs.getInt(COL_CM_PORT_REGISTRY_LOCAL);
          rmici.cmPortCommunicationLocal = rs.getInt(COL_CM_PORT_COM_LOCAL);
          rmici.persistIntervalMs = rs.getLong(COL_PERSIST_INTERVAL_MS);
          rmici.persistMaxBatchSize = rs.getInt(COL_PERSIST_MAX_BATCH_SIZE);
          rmici.persistSynchronous = rs.getBoolean(COL_PERSIST_SYNCHRONOUS);
          rmici.nodeId = rs.getString(COL_NODE_ID);
          rmici.nodePreference = rs.getBoolean(COL_NODE_PREFERENCE);
          rmici.availabilityDelayMs = rs.getInt(COL_CM_AVAILABILITYDELAY_MS);
          return rmici;
        }

      };

  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_HOSTNAME)
  private String hostname;

  @Column(name = COL_IC_SERVER_PORT)
  private int icServerPort;

  @Column(name = COL_IC_REMOTE_PORT)
  private int icRemotePort;
  
  @Column(name = COL_IC_CORR_QUEUE_LENGTH)
  private int icCorrQueueLength;
  
  @Column(name = COL_CLUSTERSTATE)
  private String clusterstate;

  @Column(name = COL_ONLINE)
  private boolean online;

  @Column(name = COL_WASMASTER)
  private boolean wasMaster;

  @Column(name = COL_CM_HOST_REMOTE)
  private String cmHostNameRemote;

  @Column(name = COL_CM_PORT_REMOTE)
  private int cmPortRegistryRemote;

  @Column(name = COL_CM_HOST_LOCAL)
  private String cmHostNameLocal;

  @Column(name = COL_CM_PORT_REGISTRY_LOCAL)
  private int cmPortRegistryLocal;

  @Column(name = COL_CM_PORT_COM_LOCAL)
  private int cmPortCommunicationLocal;

  @Column(name = COL_PERSIST_MAX_BATCH_SIZE)
  private int persistMaxBatchSize;

  @Column(name = COL_PERSIST_INTERVAL_MS)
  private long persistIntervalMs;

  @Column(name = COL_PERSIST_SYNCHRONOUS)
  private boolean persistSynchronous;

  @Column(name = COL_NODE_ID)
  private String nodeId;

  @Column(name = COL_NODE_PREFERENCE)
  private boolean nodePreference;

  @Column(name = COL_CM_AVAILABILITYDELAY_MS)
  private int availabilityDelayMs;

  public XSORClusterInstanceStorable() {
  }


  public XSORClusterInstanceStorable(long id, String hostname, int icServerPort, int icRemotePort, 
                                              int icCorrQueueLength, 
                                              String cmHostNameLocal,
                                              String cmHostNameRemote, int cmPortCommunicationLocal,
                                              int cmPortRegistryLocal, int cmPortRegistryRemote,
                                              int persistMaxBatchSize, long persistIntervalMs,
                                              boolean persistSynchonous, String nodeId, boolean nodePreference, int availabilityDelayMs) {
    this.id = id;
    this.hostname = hostname;
    this.icRemotePort = icRemotePort;
    this.icServerPort = icServerPort;
    this.icCorrQueueLength = icCorrQueueLength;
    online = true;
    clusterstate = ClusterState.NO_CLUSTER.toString();
    wasMaster = false;
    this.cmHostNameLocal = cmHostNameLocal;
    this.cmHostNameRemote = cmHostNameRemote;
    this.cmPortCommunicationLocal = cmPortCommunicationLocal;
    this.cmPortRegistryLocal = cmPortRegistryLocal;
    this.cmPortRegistryRemote = cmPortRegistryRemote;
    this.persistIntervalMs = persistIntervalMs;
    this.persistMaxBatchSize = persistMaxBatchSize;
    this.persistSynchronous = persistSynchonous;
    this.nodeId = nodeId;
    this.nodePreference = nodePreference;
    this.availabilityDelayMs = availabilityDelayMs;
  }


  public int getServerPort() {
    return icServerPort;
  }


  public int getRemotePort() {
    return icRemotePort;
  }
  
  
  protected XSORClusterInstanceStorable(long id) {
    this.id = id;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public long getId() {
    return id;
  }


  public String getHostname() {
    return hostname;
  }


  public String getClusterstate() {
    return clusterstate;
  }


  public boolean isOnline() {
    return online;
  }


  @Override
  public ResultSetReader<? extends XSORClusterInstanceStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends XSORClusterInstanceStorable> void setAllFieldsFromData(U data2) {
    XSORClusterInstanceStorable data = data2;
    id = data.id;
    hostname = data.hostname;
    icServerPort = data.icServerPort;
    icRemotePort = data.icRemotePort;
    icCorrQueueLength =data.icCorrQueueLength;
    clusterstate = data.clusterstate;
    online = data.online;
    wasMaster = data.wasMaster;
    cmHostNameLocal = data.cmHostNameLocal;
    cmHostNameRemote = data.cmHostNameRemote;
    cmPortCommunicationLocal = data.cmPortCommunicationLocal;
    cmPortRegistryLocal = data.cmPortRegistryLocal;
    cmPortRegistryRemote = data.cmPortRegistryRemote;
    persistIntervalMs = data.persistIntervalMs;
    persistMaxBatchSize = data.persistMaxBatchSize;
    persistSynchronous = data.persistSynchronous;
    nodeId = data.nodeId;
    nodePreference = data.nodePreference;
    availabilityDelayMs = data.availabilityDelayMs;
  }


  public void setClusterState(String clusterState) {
    this.clusterstate = clusterState;
  }


  public void setWasMaster(boolean wasMaster) {
    this.wasMaster = wasMaster;
  }


  public String getCMHostNameRemote() {
    return cmHostNameRemote;
  }


  public int getCMPortRegistryRemote() {
    return cmPortRegistryRemote;
  }


  public String getCMHostNameLocal() {
    return cmHostNameLocal;
  }


  public int getCMPortRegistryLocal() {
    return cmPortRegistryLocal;
  }


  public int getCMPortCommunicationLocal() {
    return cmPortCommunicationLocal;
  }


  public boolean getWasMaster() {
    return wasMaster;
  }


  public int getPersistMaxBatchSize() {
    return persistMaxBatchSize;
  }


  public long getPersistIntervalMs() {
    return persistIntervalMs;
  }


  public boolean getPersistSynchronous() {
    return persistSynchronous;
  }


  public String getNodeId() {
    return nodeId;
  }


  public boolean getNodePreference() {
    return nodePreference;
  }


  public int getAvailabilityDelayMs() {
    return availabilityDelayMs;
  }

  public int getIcCorrQueueLength() {
    return icCorrQueueLength;
  }


}