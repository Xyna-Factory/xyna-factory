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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = PoolOption.COL_POOLID, tableName = PoolOption.TABLENAME)
public class PoolOption extends Storable<PoolOption> {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(PoolOption.class);

  public static final String TABLENAME = "pooloptions";
  public static final String COL_POOLID = "poolId";
  public static final String COL_OPTIONGUIID = "optionGuiId";
  public static final String COL_VALUE = "value";
  public static final String COL_OPTIONGUIIDCOLLECTION = "optionGuiIdCollection";
  public static final String COL_VALUECOLLECTION = "valueCollection";
  
  @Column(name = COL_POOLID)
  private long poolId;
  
  @Column(name = COL_OPTIONGUIID)
  private String optionGuiId;//menschenlesbar
  
  @Column(name = COL_VALUE)
  private String value;//menschenlesbar
  
  @Column(name = COL_OPTIONGUIIDCOLLECTION, type = ColumnType.BLOBBED_JAVAOBJECT)
  private Collection<String> optionGuiIdCollection;//es können mehrere Optionen zu einem Pool gehören - nicht menschenlesbar beim abspeichern
  
  @Column(name = COL_VALUECOLLECTION, type = ColumnType.BLOBBED_JAVAOBJECT)
  private Collection<String> valueCollection;//es können mehrere Optionen zu einem Pool gehören - nicht menschenlesbar beim abspeichern
  
  @Override
  public Object getPrimaryKey() {
    return poolId;
  }

  
  public PoolOption(){
    
  }
  
  public PoolOption(long poolId, Collection<String> optionGuiId, Collection<String> value){
    this.poolId = poolId;
    this.optionGuiIdCollection = optionGuiId;
    this.valueCollection = value;
    
    StringBuilder guiids = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (String guiid : optionGuiId){
      guiids.append(guiid).append(",");
    }
    for (String val : value){
      values.append(val).append(",");
    }
    // entferne letztes Komma
    logger.debug("option IDs = " +guiids.toString());
    int last = guiids.lastIndexOf(",");
    if (last != -1){//falls keine Optionen angegeben waren, ist last = -1
      guiids.deleteCharAt(last);
      last = values.lastIndexOf(",");
      values.deleteCharAt(last);
    }
    
    
    this.optionGuiId = guiids.toString();
    this.value = values.toString();

  }
  
  private static class PoolOptionReader implements ResultSetReader<PoolOption> {

    public PoolOption read(ResultSet rs) throws SQLException {
      PoolOption po = new PoolOption();
      PoolOption.fillByResultSet(po, rs);
      return po;
    }

  }
  
  private static ResultSetReader<PoolOption> readerForGUIIdCollAndValueColl = new ResultSetReader<PoolOption>() {

    public PoolOption read(ResultSet rs) throws SQLException {
      PoolOption po = new PoolOption();
      po.optionGuiIdCollection = (Collection<String>) po.readBlobbedJavaObjectFromResultSet(rs, COL_OPTIONGUIIDCOLLECTION);
      po.valueCollection = (Collection<String>) po.readBlobbedJavaObjectFromResultSet(rs, COL_VALUECOLLECTION);
      return po;
    }
    
  };
  
  
  public ResultSetReader<PoolOption> getReaderForGUIIdCollAndValueColl() {
    return readerForGUIIdCollAndValueColl;
  }
  
  public static void fillByResultSet(PoolOption po, ResultSet rs) throws SQLException {
    po.poolId = rs.getLong(COL_POOLID);    
    po.optionGuiId = rs.getString(COL_OPTIONGUIID);
    po.value = rs.getString(COL_VALUE);
    po.optionGuiIdCollection = (Collection<String>) po.readBlobbedJavaObjectFromResultSet(rs, COL_OPTIONGUIIDCOLLECTION);
    po.valueCollection = (Collection<String>) po.readBlobbedJavaObjectFromResultSet(rs, COL_VALUECOLLECTION);//rs.getObject(COL_VALUECOLLECTION);
  }
  
  private static final PoolOptionReader reader = new PoolOptionReader();
  
  @Override
  public ResultSetReader<? extends PoolOption> getReader() {
    return reader;
  }

  @Override
  public <U extends PoolOption> void setAllFieldsFromData(U data2) {
    PoolOption data=data2;
    poolId = data.poolId;
    optionGuiId = data.optionGuiId;
    value = data.value;
    optionGuiIdCollection = data.optionGuiIdCollection;
    valueCollection = data.valueCollection;
    
  }
  
  public Collection<String> getOptionGuiIdCollection(){
    return optionGuiIdCollection;
  }
  
  public Collection<String> getValueCollection(){
    return valueCollection;
  }
  
  public long getPoolId(){
    return poolId;
  }
  
  public String getOptionGuiId(){
    return optionGuiId;
  }
  
  public String getValue(){
    return value;
  }

}
