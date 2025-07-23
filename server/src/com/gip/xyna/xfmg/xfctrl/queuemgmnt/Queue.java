/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = Queue.Constant.ColName.UNIQUE_NAME, tableName = Queue.Constant.TABLE_NAME)
public class Queue extends Storable<Queue> implements IQueue {

  private static final long serialVersionUID = 1L;
  private static final int currentVersion = 1;

  private boolean isInitialVersion() {
    return this.version == null || this.version == 0;
  }

  public static class Constant {
    public static final String TABLE_NAME = "queue";

    public static class ColName {
      public static final String UNIQUE_NAME = "uniqueName";
      public static final String EXTERNAL_NAME = "externalName";
      public static final String CONNECT_DATA = "connectData";
      public static final String QUEUE_TYPE = "queueType";
      public static final String CONFIG_VERSION = "version";
      public static final String CONNECT_DATA_STR = "connectDataStr";
      public static final String QUEUE_TYPE_STR = "queueTypeStr";
    }
  }

  public static class Reader implements ResultSetReader<Queue> {
    public Queue read(ResultSet rs) throws SQLException {
      Queue queue = new Queue();
      queue.setExternalName(rs.getString(Constant.ColName.EXTERNAL_NAME));
      queue.setUniqueName(rs.getString(Constant.ColName.UNIQUE_NAME));

      Integer savedVersion = rs.getInt(Constant.ColName.CONFIG_VERSION);

      if (savedVersion == null || savedVersion == 0) {
        Object connData = queue.readBlobbedJavaObjectFromResultSet(rs, Constant.ColName.CONNECT_DATA);
        queue.setConnectData((QueueConnectData) connData);
        Object qType = queue.readBlobbedJavaObjectFromResultSet(rs, Constant.ColName.QUEUE_TYPE);
        queue.setQueueType((QueueType) qType);
      } /* else if (savedVersion == x) {} */ else {
        queue.setConnectDataStr(rs.getString(Constant.ColName.CONNECT_DATA_STR));
        queue.setQueueTypeStr(rs.getString(Constant.ColName.QUEUE_TYPE_STR));
      }

      queue.setVersion(currentVersion);
      return queue;
    }
  }

  @Column(name = Constant.ColName.UNIQUE_NAME)
  private String uniqueName;

  @Column(name = Constant.ColName.EXTERNAL_NAME)
  private String externalName;

  @Deprecated
  @Column(name = Constant.ColName.CONNECT_DATA, type = ColumnType.BLOBBED_JAVAOBJECT)
  private QueueConnectData connectData;

  @Deprecated
  @Column(name = Constant.ColName.QUEUE_TYPE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private QueueType queueType;

  @Column(name = Constant.ColName.CONFIG_VERSION)
  private Integer version;

  @Column(name = Constant.ColName.CONNECT_DATA_STR)
  private String connectDataStr;

  @Column(name = Constant.ColName.QUEUE_TYPE_STR)
  private String queueTypeStr;

  @Override
  public ResultSetReader<? extends Queue> getReader() {
    return new Reader();
  }

  @Override
  public Object getPrimaryKey() {
    return uniqueName;
  }

  @Override
  public <U extends Queue> void setAllFieldsFromData(U data) {
    this.setConnectData(data.getConnectData());
    this.setConnectDataStr(data.getConnectDataStr());
    this.setExternalName(data.getExternalName());
    this.setUniqueName(data.getUniqueName());
    this.setQueueType(data.getQueueType());
    this.setQueueTypeStr(data.getQueueTypeStr());
    this.setVersion(data.getVersion());
  }


  @Override
  public String getUniqueName() {
    return uniqueName;
  }


  @Override
  public void setUniqueName(String uniqueName) {
    this.uniqueName = uniqueName;
  }

  @Override
  public String getExternalName() {
    return externalName;
  }


  @Override
  public void setExternalName(String externalName) {
    this.externalName = externalName;
  }

 @Override
  public QueueConnectData getConnectData() {
    return connectData;
  }

  public QueueConnectData getConnectDataForCurrentVersion() {
    if (isInitialVersion()) {
      return connectData;
    } else {
      var strConv = new QueueConnectStringData();
      return strConv.fromStringParameters(connectDataStr);
    }
  }

  @Override
  public Integer getVersion() {
      return version;
  }

  @Override
  public void setConnectData(QueueConnectData connectData) {
    if (isInitialVersion() || connectData == null) {
      this.connectData = connectData;
    } else {
      this.connectData = null;
      var strConv = new QueueConnectStringData();
      setConnectDataStr(strConv.fromConnectData(connectData));
    }
  }

  public void setConnectDataStr(String cd) {
    connectDataStr = cd;
  }

  public String getConnectDataStr() {
    return connectDataStr;
  }

  public void setQueueTypeStr(String qt) {
    queueTypeStr = qt;
  }

  public String getQueueTypeStr() {
    return queueTypeStr;
  }

  public QueueType getQueueType() {
    return queueType;
  }

  public QueueType getQueueTypeForCurrentVersion() {
    if (isInitialVersion()) {
      return queueType;
    } else {
      return Enum.<QueueType>valueOf(QueueType.class, queueTypeStr);
    }
  }

  @Override
  public void setQueueType(QueueType queueType) {
    if (isInitialVersion() || queueType == null) {
      this.queueType = queueType;
    } else {
      this.queueType = null;
      setQueueTypeStr(queueType.name());
    }
  }

  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  @Override
  public void setVersion(Integer version) {
      this.version = version;
  }


  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("Queue {  ");
    s.append("UniqueName : ").append(this.getUniqueName()).append(", ");
    s.append("ExternalName : ").append(this.getExternalName()).append(", ");
    s.append("QueueType: ").append(this.getQueueTypeForCurrentVersion().toString()).append(", ");
    s.append("ConnectData : ").append(this.getConnectDataForCurrentVersion().toString());
    if (this.getVersion() != null) {
      s.append("ConfigVersion : ").append(this.getVersion().toString());
    }
    s.append(" } \n");
    return s.toString();
  }

}
