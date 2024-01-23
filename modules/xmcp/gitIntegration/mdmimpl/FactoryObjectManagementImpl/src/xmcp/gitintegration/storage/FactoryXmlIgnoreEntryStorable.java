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
package xmcp.gitintegration.storage;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = FactoryXmlIgnoreEntryStorable.COL_ID, tableName = FactoryXmlIgnoreEntryStorable.TABLE_NAME)
public class FactoryXmlIgnoreEntryStorable extends Storable<FactoryXmlIgnoreEntryStorable> {

  private static final long serialVersionUID = -1L;

  public static final String TABLE_NAME = "factoryxmlignoreentry";

  public static final String COL_ID = "id";
  public static final String COL_CONFIGTYPE = "configtype";
  public static final String COL_VALUE = "value";

  @Column(name = COL_ID)
  private String id;
  @Column(name = COL_CONFIGTYPE)
  private String configtype;
  @Column(name = COL_VALUE)
  private String value;


  public FactoryXmlIgnoreEntryStorable() {
    super();
  }


  public FactoryXmlIgnoreEntryStorable(String configtype, String value) {
    this.id = createId(configtype, value);
    this.configtype = configtype;
    this.value = value;
  }


  @Override
  public String getPrimaryKey() {
    return id;
  }


  private static FactoryXmlIgnoreEntryStorableReader reader = new FactoryXmlIgnoreEntryStorableReader();


  @Override
  public ResultSetReader<? extends FactoryXmlIgnoreEntryStorable> getReader() {
    return reader;
  }


  private static class FactoryXmlIgnoreEntryStorableReader implements ResultSetReader<FactoryXmlIgnoreEntryStorable> {

    public FactoryXmlIgnoreEntryStorable read(ResultSet rs) throws SQLException {
      FactoryXmlIgnoreEntryStorable result = new FactoryXmlIgnoreEntryStorable();
      result.id = rs.getString(COL_ID);
      result.configtype = rs.getString(COL_CONFIGTYPE);
      result.value = rs.getString(COL_VALUE);
      return result;
    }
  }


  @Override
  public <U extends FactoryXmlIgnoreEntryStorable> void setAllFieldsFromData(U data) {
    FactoryXmlIgnoreEntryStorable cast = data;
    id = cast.id;
    configtype = cast.configtype;
    value = cast.value;
  }


  public static String createId(String configtype, String value) {
    return configtype + ":" + value;
  }


  public String getId() {
    return id;
  }


  public String getConfigtype() {
    return configtype;
  }


  public String getValue() {
    return value;
  }


  public void setId(String id) {
    this.id = id;
  }


  public void setConfigtype(String configtype) {
    this.configtype = configtype;
  }


  public void setValue(String value) {
    this.value = value;
  }

}
