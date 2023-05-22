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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = Queue.Constant.ColName.UNIQUE_NAME, tableName = Queue.Constant.TABLE_NAME)
public class Queue extends Storable<Queue> {

  private static final long serialVersionUID = 1L;

  public static class Constant {
    public static final String TABLE_NAME = "queue";

    public static class ColName {
      public static final String UNIQUE_NAME = "uniqueName";
      public static final String EXTERNAL_NAME = "externalName";
      public static final String CONNECT_DATA = "connectData";
      public static final String QUEUE_TYPE = "queueType";
    }
  }

  public static class Reader implements ResultSetReader<Queue> {
    public Queue read(ResultSet rs) throws SQLException {
      Queue queue = new Queue();
      queue.setExternalName(rs.getString(Constant.ColName.EXTERNAL_NAME));
      queue.setUniqueName(rs.getString(Constant.ColName.UNIQUE_NAME));

      Object connData = queue.readBlobbedJavaObjectFromResultSet(rs, Constant.ColName.CONNECT_DATA);
      queue.setConnectData((QueueConnectData) connData);
      Object qType = queue.readBlobbedJavaObjectFromResultSet(rs, Constant.ColName.QUEUE_TYPE);
      queue.setQueueType((QueueType) qType);
      return queue;
    }
  }

  @Column(name = Constant.ColName.UNIQUE_NAME)
  private String uniqueName;

  @Column(name = Constant.ColName.EXTERNAL_NAME)
  private String externalName;

  @Column(name = Constant.ColName.CONNECT_DATA, type = ColumnType.BLOBBED_JAVAOBJECT)
  private QueueConnectData connectData;

  @Column(name = Constant.ColName.QUEUE_TYPE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private QueueType queueType;


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
    this.setExternalName(data.getExternalName());
    this.setUniqueName(data.getUniqueName());
    this.setQueueType(data.getQueueType());
  }


  public String getUniqueName() {
    return uniqueName;
  }


  public void setUniqueName(String uniqueName) {
    this.uniqueName = uniqueName;
  }


  public String getExternalName() {
    return externalName;
  }


  public void setExternalName(String externalName) {
    this.externalName = externalName;
  }


  public QueueConnectData getConnectData() {
    return connectData;
  }


  public void setConnectData(QueueConnectData connectData) {
    this.connectData = connectData;
  }



  public QueueType getQueueType() {
    return queueType;
  }


  public void setQueueType(QueueType queueType) {
    this.queueType = queueType;
  }


  public static long getSerialversionuid() {
    return serialVersionUID;
  }


  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("Queue {  ");
    s.append("UniqueName : ").append(this.getUniqueName()).append(", ");
    s.append("ExternalName : ").append(this.getExternalName()).append(", ");
    s.append("QueueType: ").append(this.getQueueType().toString()).append(", ");
    s.append("ConnectData : ").append(this.getConnectData().toString());
    s.append(" } \n");
    return s.toString();
  }

}
