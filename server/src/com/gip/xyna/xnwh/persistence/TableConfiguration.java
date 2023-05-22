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
package com.gip.xyna.xnwh.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;

@Persistable(primaryKey=TableConfiguration.TABLE_ID, tableName="tableconfiguration")
public class TableConfiguration extends Storable<TableConfiguration> {

  private static final long serialVersionUID = -8161391662445726119L;
  private static final Logger logger = CentralFactoryLogging.getLogger(TableConfiguration.class);
  protected static final String TABLE_ID = "tableConfigurationID";
  private static final String TABLE = "table";
  private static final String PLI_ID = "persistenceLayerInstanceID";
  private static final String COL_PROPERTIES = "properties";
  
  @Column(name=TABLE_ID)
  private long tableConfigurationID;
  @Column(name=TABLE, size=50)
  private String table;
  @Column(name=PLI_ID)
  private long persistenceLayerInstanceID;
  
  @Column(name=COL_PROPERTIES, size=5000)
  private String properties;
  
  public TableConfiguration() {
    
  }
  
  public TableConfiguration(long tableConfigurationID, String tableName, long persistenceLayerInstanceID, String properties) {
    this.tableConfigurationID = tableConfigurationID;
    this.table = tableName;
    this.persistenceLayerInstanceID = persistenceLayerInstanceID;
    this.properties = properties;
  }

  @Override
  public Object getPrimaryKey() {
    return tableConfigurationID;
  }
  
  
  public long getTableConfigurationID() {
    return tableConfigurationID;
  }

  
  public String getTable() {
    return table;
  }
  

  public String getProperties() {
    return properties;
  }


  public Properties getPropertiesMap() {
    if (properties != null) {
      Properties props = new Properties();
      try {
        props.load(new ByteArrayInputStream(properties.getBytes(Constants.DEFAULT_ENCODING)));
      } catch (IOException e) {
        logger.warn("could not load properties from string: " + properties);
      }
      return props;
    }
    return null;
  }

  
  public long getPersistenceLayerInstanceID() {
    return persistenceLayerInstanceID;
  }

  private static ResultSetReader<? extends TableConfiguration> reader = new ResultSetReader<TableConfiguration>() {

    public TableConfiguration read(ResultSet rs) throws SQLException {
      
      long persistenceLayerInstanceID = rs.getLong(PLI_ID);
      long tableConfigurationID = rs.getLong(TABLE_ID);
      String tableName = rs.getString(TABLE);
      String props = rs.getString(COL_PROPERTIES);
      TableConfiguration tc = new TableConfiguration(tableConfigurationID, tableName, persistenceLayerInstanceID, props);
      return tc;
    }
    
  };

  @Override
  public ResultSetReader<? extends TableConfiguration> getReader() {
    return reader;
  }

  @Override
  public <U extends TableConfiguration> void setAllFieldsFromData(U data) {
    TableConfiguration cast = data;
    tableConfigurationID = cast.tableConfigurationID;
    table = cast.table;
    persistenceLayerInstanceID = cast.persistenceLayerInstanceID;
  }
  
  public String toString() {
    return table;
  }

}
