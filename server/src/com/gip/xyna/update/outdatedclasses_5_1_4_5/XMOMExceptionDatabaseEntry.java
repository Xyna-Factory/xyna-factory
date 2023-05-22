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
package com.gip.xyna.update.outdatedclasses_5_1_4_5;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;



@Persistable(primaryKey = XMOMExceptionDatabaseEntry.COL_FQNAME, tableName = XMOMExceptionDatabaseEntry.TABLENAME)
public class XMOMExceptionDatabaseEntry extends XMOMDomOrExceptionDatabaseEntry {

  public static final String TABLENAME = "xmomexceptioncache";
  
  public static final String COL_THROWNBY = "thrownBy";

  private static final long serialVersionUID = -7301378884111678454L;

  
  @Column(name = COL_THROWNBY, size = 1000)
  protected String thrownBy;
  
  
  public XMOMExceptionDatabaseEntry() {
  }

  
  public XMOMExceptionDatabaseEntry(String fqname) {
    super(fqname);
  }
    
  
  public XMOMExceptionDatabaseEntry(ExceptionGeneration object) {
    super(object);
  }
  
  
  public String getThrownBy() {
    return thrownBy;
  }

  
  public void setThrownBy(String thrownBy) {
    this.thrownBy = thrownBy;
  }


  private static ResultSetReader<XMOMExceptionDatabaseEntry> reader = new ResultSetReader<XMOMExceptionDatabaseEntry>() {

    public XMOMExceptionDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMExceptionDatabaseEntry x = new XMOMExceptionDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(x, rs);
      x.isExtending = rs.getString(COL_EXTENDS);
      x.extendedBy = rs.getString(COL_EXTENDEDBY);
      x.possesses = rs.getString(COL_POSSESSES);
      x.possessedBy = rs.getString(COL_POSSESSEDBY);
      x.thrownBy = rs.getString(COL_THROWNBY);
      x.neededBy = rs.getString(COL_NEEDEDBY);
      x.producedBy = rs.getString(COL_PRODUCEDBY);
      x.possesses = rs.getString(COL_POSSESSES);
      x.instancesUsedBy = rs.getString(COL_INSTANCESUSEDBY);
      return x;
    }
  };
  
  
  @Override
  public ResultSetReader<? extends XMOMExceptionDatabaseEntry> getReader() {
    return reader;
  }


  @Override
  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    thrownBy = ((XMOMExceptionDatabaseEntry)data).thrownBy;
  }
  
  
  public static class DynamicXMOMCacheReader implements ResultSetReader<XMOMExceptionDatabaseEntry> {

    private Set<XMOMDatabaseEntryColumn> selectedCols;

    public DynamicXMOMCacheReader(Set<XMOMDatabaseEntryColumn> selected) {
      selectedCols = selected;
    }

    public XMOMExceptionDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMExceptionDatabaseEntry entry = new XMOMExceptionDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(entry, selectedCols, rs);
      if (selectedCols.contains(XMOMDatabaseEntryColumn.EXTENDS)) {
        entry.isExtending = rs.getString(COL_EXTENDS);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.EXTENDEDBY)) {
        entry.extendedBy = rs.getString(COL_EXTENDEDBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.POSSESSES)) {
        entry.possesses = rs.getString(COL_POSSESSES);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.POSSESSEDBY)) {
        entry.possessedBy = rs.getString(COL_POSSESSEDBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.NEEDEDBY)) {
        entry.neededBy = rs.getString(COL_NEEDEDBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.PRODUCEDBY)) {
        entry.producedBy = rs.getString(COL_PRODUCEDBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.THROWNBY)) {
        entry.thrownBy = rs.getString(COL_THROWNBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.INSTANCESUSEDBY)) {
        entry.instancesUsedBy = rs.getString(COL_INSTANCESUSEDBY);
      }
      
      return entry;
    }
  }
  
  @Override
  public XMOMExceptionDatabaseEntry clone() throws CloneNotSupportedException {
    XMOMExceptionDatabaseEntry clone = new XMOMExceptionDatabaseEntry();
    clone.setAllFieldsFromData(this);
    return clone;
  }


  @Override
  public XMOMDatabaseType getXMOMDatabaseType() {
    return XMOMDatabaseType.EXCEPTION;
  }

  
  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    switch (column) {
      case THROWNBY :
        return thrownBy;
      default :
        return super.getValueByColumn(column);
    }
  }
  
}
