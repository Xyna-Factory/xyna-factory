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
package xfmg.xfmon.protocolmsg.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

@Persistable(primaryKey = ProtocolMessageStorable.COL_MESSAGE_ID, tableName = ProtocolMessageStorable.TABLENAME)
public class ProtocolMessageStorable extends Storable<ProtocolMessageStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "protocolmsg";

  public static final String COL_MESSAGE_ID = "messageId";
  public static final String COL_ROOT_ORDER_ID = "rootOrderId";
  public static final String COL_ORIGIN_ID = "originId";
  public static final String COL_CONNECTION_ID = "connectionId";
  public static final String COL_PARTNER_ADDRESS = "partnerAddress";
  public static final String COL_LOCAL_ADDRESS = "localAddress";
  public static final String COL_PROTOCOL_NAME = "protocolName";
  public static final String COL_PAYLOAD = "payload";
  public static final String COL_TIME = "time";
  public static final String COL_COMMUNICATION_DIRECTION = "communicationDirection";
  public static final String COL_PROTOCOL_ADAPTER_NAME = "protocolAdapterName";
  public static final String COL_MESSAGE_TYPE = "messageType";
  public static final String COL_REVISION = "revision";
  
  public static final String[] COLUMNS;
  public static final String[] FILTERABLE_COLUMNS = new String[] {COL_ROOT_ORDER_ID, COL_ORIGIN_ID, COL_CONNECTION_ID, COL_PARTNER_ADDRESS,
    COL_LOCAL_ADDRESS, COL_PROTOCOL_NAME, COL_TIME, COL_COMMUNICATION_DIRECTION, COL_PROTOCOL_ADAPTER_NAME};
  
  static {
    final String[] NOT_FILTERABLE_COLUMNS = new String[] {COL_MESSAGE_ID, COL_PAYLOAD, COL_REVISION};
    COLUMNS = new String[NOT_FILTERABLE_COLUMNS.length + FILTERABLE_COLUMNS.length];
    System.arraycopy(NOT_FILTERABLE_COLUMNS, 0, COLUMNS, 0, NOT_FILTERABLE_COLUMNS.length);
    System.arraycopy(FILTERABLE_COLUMNS, 0, COLUMNS, NOT_FILTERABLE_COLUMNS.length, FILTERABLE_COLUMNS.length);
  }
  
  
  @Column(name = COL_MESSAGE_ID)
  private long messageId;
  
  @Column(name = COL_ROOT_ORDER_ID)
  private Long rootOrderId;
  
  @Column(name = COL_ORIGIN_ID)
  private String originId;
  
  @Column(name = COL_CONNECTION_ID)
  private String connectionId;
  
  @Column(name = COL_PARTNER_ADDRESS)
  private String partnerAddress;
  
  @Column(name = COL_LOCAL_ADDRESS)
  private String localAddress;
  
  @Column(name = COL_PROTOCOL_NAME)
  private String protocolName;
  
  @Column(name = COL_PAYLOAD, type=ColumnType.BLOBBED_JAVAOBJECT)
  private GeneralXynaObject payload;
  
  @Column(name = COL_TIME, index=IndexType.MULTIPLE)
  private long time;
  
  @Column(name = COL_COMMUNICATION_DIRECTION)
  private String communicationDirection;
  
  @Column(name = COL_PROTOCOL_ADAPTER_NAME)
  private String protocolAdapterName;
  
  @Column(name = COL_MESSAGE_TYPE)
  private String messageType;
  
  @Column(name = COL_REVISION)
  private long revision;
  
  
  public ProtocolMessageStorable() {
  }

  public ProtocolMessageStorable(long messageId) {
    this.messageId = messageId;
  }
  
  
  public ProtocolMessageStorable(XynaObject msg, long revision) {
    try {
      this.messageId = getUniqueId();
      this.rootOrderId = (Long) msg.get(COL_ROOT_ORDER_ID);
      this.originId = (String) msg.get(COL_ORIGIN_ID);
      this.connectionId = (String) msg.get(COL_CONNECTION_ID);
      this.partnerAddress = (String) msg.get(COL_PARTNER_ADDRESS);
      this.localAddress = (String) msg.get(COL_LOCAL_ADDRESS);
      this.protocolName = (String) msg.get(COL_PROTOCOL_NAME);
      this.payload = (GeneralXynaObject) msg.get(COL_PAYLOAD);
      this.time = (Long) msg.get(COL_TIME);
      this.communicationDirection = (String) msg.get(COL_COMMUNICATION_DIRECTION);
      this.protocolAdapterName = (String) msg.get(COL_PROTOCOL_ADAPTER_NAME);
      this.messageType = (String) msg.get(COL_MESSAGE_TYPE);
      this.revision = revision;
    } catch (InvalidObjectPathException e) {
      throw new RuntimeException(e);
    }
  }
  
  public ProtocolMessageStorable(ProtocolMessageStorable storable) {
    this();
    this.setAllFieldsFromData(storable);
  }


  // for default
  private static AtomicLong transientIdGenerator = new AtomicLong(0);
  

  private static long getUniqueId() {
    return transientIdGenerator.incrementAndGet();
  }


  public static final ResultSetReader<ProtocolMessageStorable> reader = new ResultSetReader<ProtocolMessageStorable>() {

    public ProtocolMessageStorable read(ResultSet rs) throws SQLException {
      ProtocolMessageStorable msg = new ProtocolMessageStorable();
      
      msg.messageId = rs.getLong(COL_MESSAGE_ID);
      msg.rootOrderId = rs.getLong(COL_ROOT_ORDER_ID);
      msg.originId = rs.getString(COL_ORIGIN_ID);
      msg.connectionId = rs.getString(COL_CONNECTION_ID);
      msg.partnerAddress = rs.getString(COL_PARTNER_ADDRESS);
      msg.localAddress = rs.getString(COL_LOCAL_ADDRESS);
      msg.protocolName = rs.getString(COL_PROTOCOL_NAME);
      msg.payload = (GeneralXynaObject) msg.readBlobbedJavaObjectFromResultSet(rs, COL_PAYLOAD);
      msg.time = rs.getLong(COL_TIME);
      msg.communicationDirection = rs.getString(COL_COMMUNICATION_DIRECTION);
      msg.protocolAdapterName = rs.getString(COL_PROTOCOL_ADAPTER_NAME);
      msg.messageType = rs.getString(COL_MESSAGE_TYPE);
      msg.revision = rs.getLong(COL_REVISION);
      
      return msg;
    }

  };
  
  
  public static void registerWithOdsIfNecessary(ODS ods) throws PersistenceLayerException {
    ods.registerStorable(ProtocolMessageStorable.class);
  }

  @Override
  public ResultSetReader<? extends ProtocolMessageStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return messageId;
  }

  @Override
  public <U extends ProtocolMessageStorable> void setAllFieldsFromData(U data) {
    ProtocolMessageStorable cast = data;
    this.messageId = cast.messageId;
    this.rootOrderId = cast.rootOrderId;
    this.originId = cast.originId;
    this.connectionId = cast.connectionId;
    this.partnerAddress = cast.partnerAddress;
    this.localAddress = cast.localAddress;
    this.protocolName = cast.protocolName;
    this.payload = cast.payload;
    this.time = cast.time;
    this.communicationDirection = cast.communicationDirection;
    this.protocolAdapterName = cast.protocolAdapterName;
    this.messageType = cast.messageType;
    this.revision = cast.revision;
  }

  
  public long getMessageId() {
    return messageId;
  }

  
  public void setMessageId(long messageId) {
    this.messageId = messageId;
  }

  
  public Long getRootOrderId() {
    return rootOrderId;
  }

  
  public void setRootOrderId(Long rootOrderId) {
    this.rootOrderId = rootOrderId;
  }

  
  public String getOriginId() {
    return originId;
  }

  
  public void setOriginId(String originId) {
    this.originId = originId;
  }

  
  public String getConnectionId() {
    return connectionId;
  }

  
  public void setConnectionId(String connectionId) {
    this.connectionId = connectionId;
  }

  
  public String getPartnerAddress() {
    return partnerAddress;
  }

  
  public void setPartnerAddress(String partnerAddress) {
    this.partnerAddress = partnerAddress;
  }

  
  public String getLocalAddress() {
    return localAddress;
  }

  
  public void setLocalAddress(String localAddress) {
    this.localAddress = localAddress;
  }

  
  public String getProtocolName() {
    return protocolName;
  }

  
  public void setProtocolName(String protocolName) {
    this.protocolName = protocolName;
  }

  
  public GeneralXynaObject getPayload() {
    return payload;
  }

  
  public void setPayload(GeneralXynaObject payload) {
    this.payload = payload;
  }

  
  public long getTime() {
    return time;
  }

  
  public void setTime(long time) {
    this.time = time;
  }

  
  public String getCommunicationDirection() {
    return communicationDirection;
  }

  
  public void setCommunicationDirection(String communicationDirection) {
    this.communicationDirection = communicationDirection;
  }

  
  public String getProtocolAdapterName() {
    return protocolAdapterName;
  }

  
  public void setProtocolAdapterName(String protocolAdapterName) {
    this.protocolAdapterName = protocolAdapterName;
  }

  
  public long getRevision() {
    return revision;
  }

  
  public void setRevision(long revision) {
    this.revision = revision;
  }
  
  
  public String getMessageType() {
    return messageType;
  }

  
  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  

}
