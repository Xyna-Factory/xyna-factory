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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;




@Persistable(primaryKey = XMOMDomDatabaseEntry.COL_ID, tableName = XMOMFormDefinitionDatabaseEntry.TABLENAME)
public class XMOMFormDefinitionDatabaseEntry extends XMOMDatabaseEntry {

  private static final long serialVersionUID = 1;

  public static final String TABLENAME = "xmomformcache";


  public XMOMFormDefinitionDatabaseEntry() {
  }


  public XMOMFormDefinitionDatabaseEntry(String fqname, Long revision) {
    super(fqname, revision);
  }


  @Override
  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
  }


  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    return super.getValueByColumn(column);
  }


  @Override
  public XMOMDatabaseType getXMOMDatabaseType() {
    return XMOMDatabaseType.FORMDEFINITION;
  }


  static ResultSetReader<XMOMFormDefinitionDatabaseEntry> reader = new ResultSetReader<XMOMFormDefinitionDatabaseEntry>() {

    public XMOMFormDefinitionDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMFormDefinitionDatabaseEntry x = new XMOMFormDefinitionDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(x, rs);
      return x;
    }

  };


  @Override
  public ResultSetReader<? extends XMOMFormDefinitionDatabaseEntry> getReader() {
    return reader;
  }
}
