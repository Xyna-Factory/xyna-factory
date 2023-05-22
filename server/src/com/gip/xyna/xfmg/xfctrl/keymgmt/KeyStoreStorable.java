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
package com.gip.xyna.xfmg.xfctrl.keymgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Version;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStoreType;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = KeyStoreStorable.COL_NAME, tableName = KeyStoreStorable.TABLENAME)
public class KeyStoreStorable extends Storable<KeyStoreStorable> implements KeyStore {

  private static final long serialVersionUID = 1L;
  private static final String LIST_SERIALIZATION_SEPERATOR = "<;_;>"; 

  public static final String TABLENAME = "keystoreparameter";
  public static ResultSetReader<KeyStoreStorable> reader = new KeyStoreStorableReader();

  public static final String COL_NAME = "name";
  public static final String COL_TYPE_NAME = "type";
  public static final String COL_TYPE_VERSION = "version";
  public static final String COL_IMPORT_FILENAME = "filename"; 
  public static final String COL_PARAMETER = "parameter";

  @Column(name = COL_NAME, index = IndexType.PRIMARY)
  private String name;

  @Column(name = COL_TYPE_NAME)
  private String type;
  
  private transient KeyStoreType<?> ksType;
  
  @Column(name = COL_TYPE_VERSION)
  private String version;
  
  private transient Version versionObj;
  
  @Column(name = COL_IMPORT_FILENAME)
  private String filename;
  
  @Column(name = COL_PARAMETER)
  private StringSerializableList<String> parameter = StringSerializableList.separator(String.class, LIST_SERIALIZATION_SEPERATOR);
  
  private transient Map<String, Object> parsedParams = new HashMap<>();
  
  
  public KeyStoreStorable() {
  }

  public KeyStoreStorable(String name) {
    this.name = name;
  }
  
  public KeyStoreStorable(String name, KeyStoreType<?> ksType, String filename, Map<String, Object> parsedParams) {
    this(name);
    this.ksType = ksType;
    KeyStoreTypeIdentifier ksti = ksType.getTypeIdentifier();
    this.type = ksti.getName();
    this.versionObj = ksti.getVersion();
    this.version = ksti.getVersion().getString();
    this.filename = filename;
    this.parsedParams = parsedParams;
    rebuildSerializedParameter();
  }



  @Override
  public ResultSetReader<? extends KeyStoreStorable> getReader() {
    return reader;
  }

  @Override
  public String getPrimaryKey() {
    return name;
  }

  @Override
  public <U extends KeyStoreStorable> void setAllFieldsFromData(U data) {
    KeyStoreStorable cast = data;
    this.name = cast.name;
    this.type = cast.type;
    this.ksType = cast.ksType;
    this.version = cast.version;
    this.filename = cast.filename;
    this.parameter = cast.parameter;
    this.parsedParams = cast.parsedParams;
  }

  private static class KeyStoreStorableReader implements ResultSetReader<KeyStoreStorable> {
    public KeyStoreStorable read(ResultSet rs) throws SQLException {
      KeyStoreStorable result = new KeyStoreStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(KeyStoreStorable cns, ResultSet rs) throws SQLException {
    cns.name = rs.getString(COL_NAME);
    cns.type = rs.getString(COL_TYPE_NAME);
    cns.version = rs.getString(COL_TYPE_VERSION);
    cns.filename = rs.getString(COL_IMPORT_FILENAME);
    cns.parameter.deserializeFromString(rs.getString(COL_PARAMETER));
    cns.rebuildParsedParams();
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

  
  private KeyStoreType<?> getKeyStoreType() {
    if (ksType == null) {
      KeyManagement keyMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
      try {
        ksType = keyMgmt.getRegisteredKeyStoreType(new KeyStoreTypeIdentifier(this.type, this.version));
      } catch (XFMG_UnknownKeyStoreType e) {
        // TODO handle more gracefully? this could result from a removed type on startUp
        try {
          ksType = keyMgmt.getRegisteredKeyStoreType(new KeyStoreTypeIdentifier(this.type, this.version));
        } catch (XFMG_UnknownKeyStoreType e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        throw new IllegalArgumentException("KeyStoreType could not be resolved!");
      }
    }
    return ksType;
  }
  
  public String getVersion() {
    return version;
  }

  
  public void setVersion(String version) {
    this.version = version;
  }

  
  public Version getVersionObj() {
    return versionObj;
  }
  
  
  public String getFilename() {
    return filename;
  }

  
  public void setFilename(String filename) {
    this.filename = filename;
  }
  
  
  public Map<String, Object> getParameterMap() {
    return parsedParams;
  }
  
  
  public void setParameterMap(Map<String, Object> parsedParams) {
    this.parsedParams = parsedParams;
    rebuildSerializedParameter();
  }
  
  
  public StringSerializableList<String> getParameter() {
    return parameter;
  }
  
  
  public void setParameter(StringSerializableList<String> parameter) {
    this.parameter = parameter;
    rebuildParsedParams();
  }

  
  private void rebuildParsedParams() {
    try {
      this.parsedParams = StringParameter.paramListToMap(getKeyStoreType().getTypeDescription().getParameters(ParameterUsage.Create), parameter);
    } catch (StringParameterParsingException e) {
      logger.warn("Failed to rebuild parameters", e);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void rebuildSerializedParameter() {
    List<StringParameter<?>> allParameter = getKeyStoreType().getTypeDescription().getParameters(ParameterUsage.Create);
    for (StringParameter param : allParameter) {
      Object value = param.getFromMap(parsedParams);
      if (value != null) {
        parameter.add(param.toNamedParameterObject(value));
      }
    }
  }
  
}
