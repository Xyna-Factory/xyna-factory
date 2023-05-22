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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerClassIncompatibleException;

@Persistable(primaryKey=PersistenceLayerInstanceBean.PLI_ID, tableName="persistencelayerinstance")
public class PersistenceLayerInstanceBean extends Storable<PersistenceLayerInstanceBean> {

  private static final long serialVersionUID = 6757745809385843037L;
  
  protected static final String PLI_ID = "persistenceLayerInstanceID";
  private static final String PLI_NAME = "persistenceLayerInstanceName";
  private static final String PL_ID = "persistenceLayerID";
  private static final String CONN_PARAS = "connectionParameter";
  private static final String CONN_TYPE = "connectionType";
  private static final String DEP = "department";
  private static final String DEFAULT = "isDefault";

  private static final String SEPARATOR = "|";
  
  @Column(name=PLI_ID)
  private long persistenceLayerInstanceID;
  @Column(name=PLI_NAME)
  private String persistenceLayerInstanceName;
  @Column(name=PL_ID)
  private long persistenceLayerID;
  @Column(name=CONN_PARAS, size=500)
  private String connectionParameter;
  @Column(name=CONN_TYPE, size=30)
  private String connectionType;
  @Column(name=DEP, size=10)
  private String department;
  @Column(name=DEFAULT)
  private boolean isDefault = false;
  private transient PersistenceLayer persistenceLayerInstance;
  private transient String[] connectionParameterArray;
  private transient ODSConnectionType connectionTypeEnum;
  

  public PersistenceLayerInstanceBean() {

  }


  public PersistenceLayerInstanceBean(long persistenceLayerInstanceID, String persistenceLayerInstanceName, long persistenceLayerID,
                                      String[] connectionParameter, String department, ODSConnectionType connectionType) {

    this.persistenceLayerID = persistenceLayerID;
    this.persistenceLayerInstanceName = persistenceLayerInstanceName;
    this.persistenceLayerInstanceID = persistenceLayerInstanceID;
    this.connectionParameterArray = connectionParameter;
    if (connectionParameter != null && connectionParameter.length > 0) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < connectionParameter.length - 1; i++) {
        sb.append(connectionParameter[i]).append(SEPARATOR);
      }
      sb.append(connectionParameter[connectionParameter.length - 1]);
      this.connectionParameter = sb.toString();
    } else {
      this.connectionParameter = "";
    }
    this.connectionTypeEnum = connectionType;
    this.connectionType = connectionType.toString();
    this.department = department;

  }


  public void setDefault(boolean b) {
    this.isDefault = b;
  }


  public long getPersistenceLayerInstanceID() {
    return persistenceLayerInstanceID;
  }


  public String getPersistenceLayerInstanceName() {
    return persistenceLayerInstanceName;
  }

  public void setPersistenceLayerInstanceName(String persistenceLayerInstanceName) {
    this.persistenceLayerInstanceName = persistenceLayerInstanceName;
  }


  public long getPersistenceLayerID() {
    return persistenceLayerID;
  }


  public String getConnectionParameter() {
    return connectionParameter;
  }


  public String getConnectionType() {
    return connectionType;
  }


  public ODSConnectionType getConnectionTypeEnum() {
    return connectionTypeEnum;
  }


  public String[] getConnectionParameterArray() {
    return connectionParameterArray;
  }


  public String getDepartment() {
    return department;
  }


  public boolean getIsDefault() {
    return isDefault;
  }


  public void createInstance(PersistenceLayerBeanMemoryCache pl) throws PersistenceLayerException,
      XNWH_PersistenceLayerClassIncompatibleException {
    try {
      persistenceLayerInstance = pl.getPersistenceLayerClass().getConstructor().newInstance();
    } catch (Exception e) {
      throw new XNWH_PersistenceLayerClassIncompatibleException(pl.getPersistenceLayerClass().getName(), e);
    }
    persistenceLayerInstance.init(persistenceLayerInstanceID, connectionParameterArray);
  }


  public PersistenceLayer getPersistenceLayerInstance() {
    return persistenceLayerInstance;
  }

  @Override
  public Object getPrimaryKey() {
    return persistenceLayerInstanceID;
  }
  
  private static ResultSetReader<? extends PersistenceLayerInstanceBean> reader = new ResultSetReader<PersistenceLayerInstanceBean>() {

    public PersistenceLayerInstanceBean read(ResultSet rs) throws SQLException {
      long pliID = rs.getLong(PLI_ID);
      String pliName = rs.getString(PLI_NAME);
      long plID = rs.getLong(PL_ID);
      String[] connectionParas = rs.getString(CONN_PARAS).split(Pattern.quote(SEPARATOR));
      String department = rs.getString(DEP);
      ODSConnectionType conType = ODSConnectionType.valueOf(rs.getString(CONN_TYPE));      
      PersistenceLayerInstanceBean b = new PersistenceLayerInstanceBean(pliID, pliName, plID, connectionParas, department, conType);
      b.setDefault(rs.getBoolean(DEFAULT));
      return b;
    }
    
  };

  @Override
  public ResultSetReader<? extends PersistenceLayerInstanceBean> getReader() {
    return reader;
  }

  @Override
  public <U extends PersistenceLayerInstanceBean> void setAllFieldsFromData(U data) {
    persistenceLayerInstanceID = data.getPersistenceLayerInstanceID();
    persistenceLayerID = data.getPersistenceLayerID();
    connectionParameter = data.getConnectionParameter();
    connectionParameterArray = data.getConnectionParameterArray();
    department = data.getDepartment();
    connectionType = data.getConnectionType();
    connectionTypeEnum = data.getConnectionTypeEnum();
    isDefault = data.getIsDefault();
  }
  
}
