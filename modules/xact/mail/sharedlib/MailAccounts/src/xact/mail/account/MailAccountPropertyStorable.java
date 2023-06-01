/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package xact.mail.account;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

import xact.mail.account.MailAccountData.Builder;
import xact.mail.account.MailAccountData.MailAccountProperty;

@Persistable(primaryKey = MailAccountPropertyStorable.COL_NAME_KEY_INDEX, tableName = MailAccountPropertyStorable.TABLE_NAME)
public class MailAccountPropertyStorable extends Storable<MailAccountPropertyStorable> {
  
  public static final String TABLE_NAME = "mailaccountproperties";
  
  public static final String COL_NAME_KEY_INDEX = "index";
  public static final String COL_NAME = "name";
  public static final String COL_KEY = "key";
  public static final String COL_VALUE = "value";
  public static final String COL_DOCUMENTATION = "documentation";
  
  private static final long serialVersionUID = -1L; 
  private static PreparedQueryCache queryCache = new PreparedQueryCache();
  
  @Column(name = COL_NAME_KEY_INDEX)
  private String namekeyIndex; //name+"_"+key
  @Column(name = COL_NAME, size = 100)
  private String name;
  @Column(name = COL_KEY)
  private String key;
  @Column(name = COL_VALUE)
  private String value;
  @Column(name = COL_DOCUMENTATION, size = 2000)
  private String documentation;
  
  public MailAccountPropertyStorable() {
    super();
  }

  public MailAccountPropertyStorable(String name, String key) {
    this.name = name;
    this.key = key;
    this.namekeyIndex = name+"_"+key;
  }

  @Override
  public String getPrimaryKey() {
    return namekeyIndex;
  }


  private static MailAccountPropertyStorableReader reader = new MailAccountPropertyStorableReader();

  @Override
  public ResultSetReader<? extends MailAccountPropertyStorable> getReader() {
    return reader;
  }
  
  private static class MailAccountPropertyStorableReader implements ResultSetReader<MailAccountPropertyStorable> {

    public MailAccountPropertyStorable read(ResultSet rs) throws SQLException {
      MailAccountPropertyStorable maps = new MailAccountPropertyStorable();
      maps.namekeyIndex = rs.getString(COL_NAME_KEY_INDEX);
      maps.name = rs.getString(COL_NAME);
      maps.key = rs.getString(COL_KEY);
      maps.value = rs.getString(COL_VALUE);
      maps.documentation = rs.getString(COL_DOCUMENTATION);
      return maps;
    }
    
  }

  @Override
  public <U extends MailAccountPropertyStorable> void setAllFieldsFromData(U data) {
    MailAccountPropertyStorable cast = data;
    namekeyIndex = cast.namekeyIndex;
    name = cast.name;
    key = cast.key;
    value = cast.value;
    documentation = cast.documentation;
  }

  public static final Transformation<MailAccountPropertyStorable, String> toNameTransformation = 
    
    new Transformation<MailAccountPropertyStorable, String>() {
     @Override
      public String transform(MailAccountPropertyStorable from) {
        return from.name;
      }
  };

  public void fill(Builder builder) {
    builder.addProperty(new MailAccountProperty(key, value, documentation));
  }

  public static MailAccountPropertyStorable of(String name, MailAccountProperty property) {
    MailAccountPropertyStorable maps = new MailAccountPropertyStorable(name, property.getKey());
    maps.value = property.getValue();
    maps.documentation = property.getDocumentation();
    return maps;
  }

  public static List<MailAccountPropertyStorable> readAllPropertiesForName(ODSConnection con, String name) throws PersistenceLayerException {
    PreparedQuery<MailAccountPropertyStorable> query = 
        queryCache.getQueryFromCache(QUERY_PROPERTIES_FOR_NAME, con, reader);
    return con.query(query, new Parameter(name), -1);
  }
  
  private static final String QUERY_PROPERTIES_FOR_NAME = 
      "select * from "+MailAccountPropertyStorable.TABLE_NAME
      +" where "+MailAccountPropertyStorable.COL_NAME+"=?";

  public String getIndex() {
    return namekeyIndex;
  }
  public String getName() {
    return name;
  }
  
  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
  
  public String getDocumentation() {
    return documentation;
  }

}
