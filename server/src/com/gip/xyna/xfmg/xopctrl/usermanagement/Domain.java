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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;




@Persistable(primaryKey = Domain.COL_NAME, tableName = Domain.TABLENAME)
public class Domain extends Storable<Domain> {

  private static final long serialVersionUID = -310896826280564946L;
  
  public final static String TABLENAME = "domainarchive";
  
  public static final String COL_NAME = "name";
  public static final String COL_DOMAINTYPE = "domainType";
  public static final String COL_MAXRETRIES = "maxRetries";
  public static final String COL_CONNECTION_TIMEOUT = "connectionTimeout";
  public static final String COL_DESCRIPTION = "description";
  public static final String COL_DOMAINTYPE_SPECIFIC_DATA = "domainSpecificData";
  
  
  @Column(name = COL_NAME, size = 50)
  private String name;
  @Column(name = COL_DOMAINTYPE, size = 50)
  private String domainType;
  @Column(name = COL_MAXRETRIES)
  private int maxRetries;
  @Column(name = COL_CONNECTION_TIMEOUT) // in seconds
  private int connectionTimeout;
  @Column(name = COL_DESCRIPTION, size = 200)
  private String description;
  @Column(name = COL_DOMAINTYPE_SPECIFIC_DATA, type = ColumnType.BLOBBED_JAVAOBJECT)
  private DomainTypeSpecificData domainTypeSpecificData=null;

  
  public Domain() {
    //für Storable
  }
  
  Domain(String name) { //used from the usermanagement
    this.name = name; 
  }
  
  public Domain(String name, DomainType type, int maxRetries, int connectionTimeout) {
    this(name);
    this.domainType = type.toString();
    this.maxRetries = maxRetries;
    this.connectionTimeout = connectionTimeout;
  }
  
  public String getName() {
    return name;
  }

  
  public String getDomainType() {
    return domainType;
  }

  public DomainType getDomainTypeAsEnum() {
    return DomainType.valueOfNiceString(domainType);
  }
  
  public void setDomainType(DomainType domainType) {
    this.domainType = domainType.toString();
  }

  
  public int getMaxRetries() {
    return maxRetries;
  }

  
  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  
  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }
  
  
  public String getDescription() {
    return description;
  }

  
  public void setDescription(String description) {
    this.description = description;
  }

  
  public DomainTypeSpecificData getDomainSpecificData() {
    return domainTypeSpecificData;
  }

  
  public void setDomainSpecificData(DomainTypeSpecificData domainSpecificData) {
    this.domainTypeSpecificData = domainSpecificData;
  }

  @Override
  public Object getPrimaryKey() {
    return name;
  }

  
  private static ResultSetReader<Domain> reader = new ResultSetReader<Domain>() {

    public Domain read(ResultSet rs) throws SQLException {
      Domain d = new Domain();
      d.connectionTimeout = rs.getInt(COL_CONNECTION_TIMEOUT);
      d.maxRetries = rs.getInt(COL_MAXRETRIES);
      d.description = rs.getString(COL_DESCRIPTION);
      d.domainType = rs.getString(COL_DOMAINTYPE);
      d.domainTypeSpecificData = (DomainTypeSpecificData) d.readBlobbedJavaObjectFromResultSet(rs, COL_DOMAINTYPE_SPECIFIC_DATA);
      d.name = rs.getString(COL_NAME);      
      return d;
    }

  };


  @Override
  public ResultSetReader<? extends Domain> getReader() {
    return reader;
  }

  @Override
  public <U extends Domain> void setAllFieldsFromData(U data) {
    Domain cast = data;
    this.connectionTimeout = cast.connectionTimeout;
    this.maxRetries = cast.maxRetries;
    this.description = cast.description;
    this.domainType = cast.domainType;
    this.domainTypeSpecificData = cast.domainTypeSpecificData; 
    this.name = cast.name;
  }

  public UserAuthentificationMethod generateAuthenticationMethod(Domain domain) {
    return getDomainTypeAsEnum().generateAuthenticationMethod(this);
  }
  

}
