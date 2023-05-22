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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;




@Persistable(primaryKey = XMOMServiceGroupDatabaseEntry.COL_ID, tableName = XMOMServiceGroupDatabaseEntry.TABLENAME)
public class XMOMServiceGroupDatabaseEntry extends XMOMDatabaseEntry {

  public static final String TABLENAME = "xmomservicegroupcache";
  public static final String COL_GROUPS = "groups";
  public static final String COL_WRAPPEDBY = "wrappedBy";
  
  private static final long serialVersionUID = -7301378884111678454L;

  
  @Column(name = COL_GROUPS, size = 1000)
  private String groups;
  
  @Column(name = COL_WRAPPEDBY, size = 50)
  private String wrappedBy;
  
  
  public XMOMServiceGroupDatabaseEntry() {
  }

  
  public XMOMServiceGroupDatabaseEntry(String fqname, Long revision) {
    super(fqname, revision);
  }
  
  
  // generate cache entry for datatype
  public XMOMServiceGroupDatabaseEntry(DOM dom, String serviceName) {
    super(generateFqNameForServiceGroup(dom, serviceName), dom.getLabelOfService(serviceName), 
                    dom.getOriginalPath(), generateSimpleNameForServiceGroup(dom, serviceName), "", "", 
                    dom.isXynaFactoryComponent(), dom.getRevision());
    groups = concatOperations(getFqname(), dom.getServiceNameToOperationMap().get(serviceName));
    wrappedBy = dom.getOriginalFqName();
    setTimestamp(dom.getParsingTimestamp());
  }
  
    
  public String getGroups() {
    return groups;
  }

  
  public void setGroups(String groups) {
    this.groups = groups;
  }

  
  


  private static ResultSetReader<XMOMServiceGroupDatabaseEntry> reader = new ResultSetReader<XMOMServiceGroupDatabaseEntry>() {

    public XMOMServiceGroupDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMServiceGroupDatabaseEntry x = new XMOMServiceGroupDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(x, rs);
      x.groups = rs.getString(COL_GROUPS);
      x.wrappedBy = rs.getString(COL_WRAPPEDBY);
      return x;
    }

  };
  
  
  @Override
  public ResultSetReader<? extends XMOMServiceGroupDatabaseEntry> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return getId();
  }


  @Override
  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    groups = ((XMOMServiceGroupDatabaseEntry)data).groups;
    wrappedBy = ((XMOMServiceGroupDatabaseEntry)data).wrappedBy;
  }
  
  
  public static class DynamicXMOMCacheReader implements ResultSetReader<XMOMServiceGroupDatabaseEntry> {

    private Set<XMOMDatabaseEntryColumn> selectedCols;

    public DynamicXMOMCacheReader(Set<XMOMDatabaseEntryColumn> selected) {
      selectedCols = selected;
    }

    public XMOMServiceGroupDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMServiceGroupDatabaseEntry entry = new XMOMServiceGroupDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(entry, selectedCols, rs);
      if (selectedCols.contains(XMOMDatabaseEntryColumn.GROUPS)) {
        entry.groups = rs.getString(COL_GROUPS);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.GROUPEDBY)) {
        entry.wrappedBy = rs.getString(COL_WRAPPEDBY);
      }
      return entry;
    }
  }


  @Override
  public XMOMDatabaseType getXMOMDatabaseType() {
    return XMOMDatabaseType.SERVICEGROUP;
  }
  
  
  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    switch (column) {
      case GROUPS :
        return groups;
      case WRAPPEDBY :
        return wrappedBy;
      default :
        return super.getValueByColumn(column);
    }
  }


  
  public String getWrappedBy() {
    return wrappedBy;
  }


  
  public void setWrappedBy(String wrappedBy) {
    this.wrappedBy = wrappedBy;
  }

}
