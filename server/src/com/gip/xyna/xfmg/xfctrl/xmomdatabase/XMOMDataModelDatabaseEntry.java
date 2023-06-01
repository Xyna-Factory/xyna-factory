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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;


@Persistable(primaryKey = XMOMDataModelDatabaseEntry.COL_ID, tableName = XMOMDataModelDatabaseEntry.TABLENAME)
public class XMOMDataModelDatabaseEntry extends XMOMDatabaseEntry{

  public static final String TABLENAME = "xmomdatamodelcache";
  
  public static final String COL_USEDBY = "usedBy";
  
  private static final long serialVersionUID = 1L;

  @Column(name = COL_USEDBY, size = 1000)
  protected String usedBy; //Workflows (fqName#revision), die das Datenmodell verwenden
  
  
  public XMOMDataModelDatabaseEntry() {
  }
  
  public XMOMDataModelDatabaseEntry(String fqName) {
    super(fqName, RevisionManagement.REVISION_DATAMODEL);
  }

  
  public String getUsedBy() {
    return usedBy;
  }
  
  
  public void setUsedBy(String usedBy) {
    this.usedBy = usedBy;
  }
  
  
  @Override
  public XMOMDatabaseType getXMOMDatabaseType() {
    return XMOMDatabaseType.DATAMODEL;
  }


  private static ResultSetReader<XMOMDataModelDatabaseEntry> reader = new ResultSetReader<XMOMDataModelDatabaseEntry>() {

    public XMOMDataModelDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMDataModelDatabaseEntry x = new XMOMDataModelDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(x, rs);
      x.usedBy = rs.getString(COL_USEDBY);
      return x;
    }

  };
  
  @Override
  public ResultSetReader<? extends XMOMDataModelDatabaseEntry> getReader() {
    return reader;
  }

  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    usedBy = ((XMOMDataModelDatabaseEntry)data).usedBy;
  }
}
