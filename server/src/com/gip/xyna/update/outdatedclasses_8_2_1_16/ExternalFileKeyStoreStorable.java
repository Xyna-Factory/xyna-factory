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
package com.gip.xyna.update.outdatedclasses_8_2_1_16;

import java.io.FileInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Version;
import com.gip.xyna.xfmg.xfctrl.keymgmt.JavaSecurityStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStore;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreTypeIdentifier;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = ExternalFileKeyStoreStorable.COL_NAME, tableName = ExternalFileKeyStoreStorable.TABLENAME)
public class ExternalFileKeyStoreStorable extends Storable<ExternalFileKeyStoreStorable> implements KeyStore {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "extfilekeystore";
  public static ResultSetReader<ExternalFileKeyStoreStorable> reader = new ExternalFileKeyStoreStorableReader();

  public static final String COL_NAME = "name";
  public static final String COL_TYPE_NAME = "type";
  public static final String COL_TYPE_VERSION = "version";
  public static final String COL_EXTERNAL_FILENAME = "extfilename"; 
  public static final String COL_FILE_TYPE = "filetype";

  @Column(name = COL_NAME, index = IndexType.PRIMARY)
  private String name;

  @Column(name = COL_TYPE_NAME)
  private String type;
  
  @Column(name = COL_TYPE_VERSION)
  private String version;
  
  @Column(name = COL_EXTERNAL_FILENAME)
  private String extfilename;
  
  @Column(name = COL_FILE_TYPE)
  private String filetype;
  
  private transient Version versionObj;
  
  
  public ExternalFileKeyStoreStorable() {
  }

  public ExternalFileKeyStoreStorable(String name) {
    this.name = name;
  }
  
  public ExternalFileKeyStoreStorable(String name, KeyStoreType<?> ksType, String filename, String filetype) {
    this(name);
    KeyStoreTypeIdentifier ksti = ksType.getTypeIdentifier();
    this.type = ksti.getName();
    this.versionObj = ksti.getVersion();
    this.version = ksti.getVersion().getString();
    this.extfilename = filename;
    this.filetype = filetype;
  }



  @Override
  public ResultSetReader<? extends ExternalFileKeyStoreStorable> getReader() {
    return reader;
  }

  @Override
  public String getPrimaryKey() {
    return name;
  }

  @Override
  public <U extends ExternalFileKeyStoreStorable> void setAllFieldsFromData(U data) {
    ExternalFileKeyStoreStorable cast = data;
    this.name = cast.name;
    this.type = cast.type;
    this.version = cast.version;
    this.extfilename = cast.extfilename;
    this.filetype = cast.filetype;
  }

  private static class ExternalFileKeyStoreStorableReader implements ResultSetReader<ExternalFileKeyStoreStorable> {
    public ExternalFileKeyStoreStorable read(ResultSet rs) throws SQLException {
      ExternalFileKeyStoreStorable result = new ExternalFileKeyStoreStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(ExternalFileKeyStoreStorable cns, ResultSet rs) throws SQLException {
    cns.name = rs.getString(COL_NAME);
    cns.type = rs.getString(COL_TYPE_NAME);
    cns.version = rs.getString(COL_TYPE_VERSION);
    cns.extfilename = rs.getString(COL_EXTERNAL_FILENAME);
    cns.filetype = rs.getString(COL_FILE_TYPE);
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

  
  public String getVersion() {
    return version;
  }

  
  public void setVersion(String version) {
    this.version = version;
  }

  
  public String getExtfilename() {
    return extfilename;
  }

  
  public void setExtfilename(String extfilename) {
    this.extfilename = extfilename;
  }
  
  
  public String getPassphrase() {
    return (String) XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().retrieve(JavaSecurityStoreType.NAME, name);
  }

  
  public String getFiletype() {
    return filetype;
  }

  
  public void setFiletype(String filetype) {
    this.filetype = filetype;
  }

  
  public Version getVersionObj() {
    return versionObj;
  }
  


  public Class<java.security.KeyStore> getTargetClass() {
    return java.security.KeyStore.class;
  }

  public java.security.KeyStore convert(Map<String, Object> parsedParams) throws Exception {
    java.security.KeyStore ks = java.security.KeyStore.getInstance(filetype);
    String passphrase = getPassphrase();
    char[] passphraseChars = null;
    if (passphrase != null && passphrase.length() > 0) {
      passphraseChars = passphrase.toCharArray();
    }
    try (FileInputStream fis = new FileInputStream(extfilename)) {
      ks.load(fis, passphraseChars);
    }
    return ks;
  }


  
  
}
