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
package com.gip.xyna.xnwh.pools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.gip.xyna.utils.collections.CSVStringList;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;
import com.gip.xyna.utils.misc.StringParameter.Unparseable;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xnwh.exceptions.XNWH_EncryptionException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.securestorage.SecureStorage;

@Persistable(primaryKey = PoolDefinition.COL_NAME, tableName = PoolDefinition.TABLENAME, tableProperties = {StorableProperty.PROTECTED})
public class PoolDefinition extends Storable<PoolDefinition> {

  private static final long serialVersionUID = 1L;
  
  protected final static String TABLENAME = "pooldefinition";
  protected final static String COL_NAME = "name";
  protected final static String COL_TYPE = "type";
  protected final static String COL_SIZE = "size";
  protected final static String COL_USER = "user";
  protected final static String COL_PASSWORD = "password";
  protected final static String COL_RETRIES = "retries";
  protected final static String COL_ADDITIONAL_PARAMS = "params";
  protected final static String COL_VALIDATION_INTERVAL = "validationinterval";
  protected final static String COL_CONNECTSTRING = "connectstring";
  protected final static String COL_UUID = "uuid";
  
  /**
   * if version is not set, password is stored in clear text
   * if version is "3", password is encrypted using SecureStorage.staticEncrypt with secureStorageIdentifier_uuid
   * password column contains lengthOfPassword passwordSeparator actualPassword padding (encrypted)
   */
  protected final static String COL_VERSION = "version";

  private static final String secureStorageIdentifier = "pooldefinition";
  private static final String currentVersion = "3";
  private static final int paddingSize = 100;
  private static final char passwordSeparator = '|';
  
  @Column(name = COL_NAME, index = IndexType.PRIMARY)
  public String name;
  @Column(name = COL_TYPE)
  public String type;
  @Column(name = COL_SIZE)
  public int size;
  @Column(name = COL_USER)
  public String user;
  @Column(name = COL_PASSWORD)
  public String password;
  @Column(name = COL_RETRIES)
  public int retries;
  @Column(name = COL_ADDITIONAL_PARAMS)
  public CSVStringList params;
  @Column(name = COL_VALIDATION_INTERVAL)
  public long validationinterval;
  @Column(name = COL_CONNECTSTRING)
  public String connectstring;
  @Column(name = COL_VERSION)
  public String version;
  @Column(name = COL_UUID)
  public String uuid;
  
  
  @Override
  public ResultSetReader<PoolDefinition> getReader() {
    return new ResultSetReader<PoolDefinition>() {

      public PoolDefinition read(ResultSet rs) throws SQLException {
        PoolDefinition poolDefinition = new PoolDefinition();
        fillByResultSet(poolDefinition, rs);
        return poolDefinition;
      }
    };
  }


  @Override
  public Object getPrimaryKey() {
    return name;
  }


  @Override
  public void setAllFieldsFromData(PoolDefinition data) {
    this.name=data.name;
    this.type=data.type;
    this.size=data.size;
    this.user=data.user;
    this.password=data.password;
    this.retries=data.retries;
    this.params=data.params;
    this.validationinterval=data.validationinterval;
    this.connectstring=data.connectstring;
    this.version=data.version;
    this.uuid=data.uuid;
  }
  
  
  public void setAllFieldsFromData(TypedConnectionPoolParameter data) {
    this.name=data.getName();
    this.type=data.getType();
    this.size=data.getSize();
    this.user=data.getUser();
    this.uuid = data.getUuid() == null || data.getUuid().length() == 0 ? createUuid() : data.getUuid();
    this.setPassword(data.getPassword()); //input clear; save encrypted
    this.retries=data.getMaxRetries();
    this.params=extractAdditionalParamsAsCSVStringList(data);
    this.validationinterval=data.getValidationInterval();
    this.connectstring = data.getConnectString();
    this.version=currentVersion;
  }
  
  private String createUuid() {
    String uuid = UUID.randomUUID().toString(); 
    logger.debug("uuid: " + uuid);
    return uuid;
  }


  private static CSVStringList extractAdditionalParamsAsCSVStringList(TypedConnectionPoolParameter data) {
    Map<String, String> map = StringParameter.toStringMap(data.getAdditionalDescription().getParameters(ParameterUsage.Create), 
                                                          data.getAdditionalParams(), false);
    List<String> list = CollectionUtils.transform(map.entrySet(), new Transformation<Map.Entry<String, String>, String>() {

      public String transform(Entry<String, String> from) {
        return from.getKey() + "=" + from.getValue();
      }
      
    });
    return new CSVStringList(list);
  }
  
  
  private static void fillByResultSet(PoolDefinition poolDefinition, ResultSet rs) throws SQLException {
    poolDefinition.name=rs.getString(COL_NAME);
    poolDefinition.type=rs.getString(COL_TYPE);
    poolDefinition.size=rs.getInt(COL_SIZE);
    poolDefinition.user=rs.getString(COL_USER);
    poolDefinition.password=rs.getString(COL_PASSWORD);
    poolDefinition.retries=rs.getInt(COL_RETRIES);
    poolDefinition.params=CSVStringList.valueOf(rs.getString(COL_ADDITIONAL_PARAMS));
    poolDefinition.validationinterval=rs.getLong(COL_VALIDATION_INTERVAL);
    poolDefinition.connectstring=rs.getString(COL_CONNECTSTRING);
    poolDefinition.version=rs.getString(COL_VERSION);
    poolDefinition.uuid=rs.getString(COL_UUID);
  }


  public TypedConnectionPoolParameter toCreationParameter() {
    TypedConnectionPoolParameter tcpp = new TypedConnectionPoolParameter(this.type);
    tcpp.name=this.name;
    tcpp.size(this.size);
    tcpp.user=this.user;
    tcpp.setUuid(this.uuid == null || this.uuid.length() == 0 ? createUuid() : this.uuid);
    tcpp.password = getPassword(); //not encrypted
    tcpp.maxRetries(this.retries);
    try {
      tcpp.additionalParams=StringParameter.parse(this.params).
          silent(true).
          unmatchedKey(Unmatched.Ignore).
          unparseableValue(Unparseable.Ignore).
          with(tcpp.getAdditionalDescription().getParameters(ParameterUsage.Create));
    } catch (StringParameterParsingException e) {
      logger.warn(e);
    }
    tcpp.validationInterval(this.validationinterval);
    tcpp.connectString = this.connectstring;
    return tcpp;
  }
  
  
  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }

  
  public String getType() {
    return type;
  }

  
  public void setType(String type) {
    this.type = type;
  }

  
  public int getSize() {
    return size;
  }

  
  public void setSize(int size) {
    this.size = size;
  }

  
  public String getUser() {
    return user;
  }

  
  public void setUser(String user) {
    this.user = user;
  }


  /**
   * Returns clear text password
   * */
  public String getPassword() {
    String result;

    if (currentVersion.equals(version)) {
      try {
        result = SecureStorage.staticDecrypt(createCompleteIdentifier(), password);
        int passwordStartIndex = result.indexOf(passwordSeparator) + 1;
        int length = Integer.parseInt(result.substring(0, passwordStartIndex - 1));
        result = result.substring(passwordStartIndex, length + passwordStartIndex);
      } catch (XNWH_EncryptionException e) {
        throw new RuntimeException(e);
      }
    } else {
      result = password;
    }

    return result;
  }


  /*
   * Expects clear text password
   * */
  public void setPassword(String password) {
    String encrypted;

    try {
      int length = password.length();
      String lengthString = String.valueOf(length);
      String padding =  SecureStorage.createPadding(paddingSize - length - lengthString.length());
      password = lengthString + passwordSeparator + password + padding;
      encrypted = SecureStorage.staticEncrypt(createCompleteIdentifier(), password);
    } catch (XNWH_EncryptionException e) {
      throw new RuntimeException("could not encrypt password.", e);
    }

    this.password = encrypted;
    this.version = currentVersion;
  }

  
  private String createCompleteIdentifier() {
    return secureStorageIdentifier + "_" + uuid;
  }
  
  public int getRetries() {
    return retries;
  }

  
  public void setRetries(int retries) {
    this.retries = retries;
  }

  
  public CSVStringList getParams() {
    return params;
  }

  
  public void setParams(CSVStringList params) {
    this.params = params;
  }

  
  public long getValidationinterval() {
    return validationinterval;
  }

  
  public void setValidationinterval(long validationinterval) {
    this.validationinterval = validationinterval;
  }

  
  public String getConnectstring() {
    return connectstring;
  }

  
  public void setConnectstring(String connectstring) {
    this.connectstring = connectstring;
  }


  public String getVersion() {
    return version;
  }


  public void setVersion(String version) {
    this.version = version;
  }


  public String getUuid() {
    return uuid;
  }


  public void setUuid(String uuid) {
    if (uuid == null || uuid.length() == 0) {
      uuid = createUuid();
    }
    this.uuid = uuid;
  }

}
