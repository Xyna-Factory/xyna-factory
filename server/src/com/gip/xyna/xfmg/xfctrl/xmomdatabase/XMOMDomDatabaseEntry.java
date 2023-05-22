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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCall;



@Persistable(primaryKey = XMOMDomDatabaseEntry.COL_ID, tableName = XMOMDomDatabaseEntry.TABLENAME)
public class XMOMDomDatabaseEntry extends XMOMDomOrExceptionDatabaseEntry {

  public static final String TABLENAME = "xmomdomcache";
  public static final String COL_WRAPS = "wraps";
  public static final String COL_INSTANCESERVICEREFERENCES = "instanceServiceReferences";
  
  private static final long serialVersionUID = -7301378884111678454L;
  

  @Column(name = COL_WRAPS, size = 250)
  protected String wraps;

  @Column(name = COL_INSTANCESERVICEREFERENCES, size = 250)
  protected String instanceServiceReferences;
  
  public XMOMDomDatabaseEntry() {
  }

  
  public XMOMDomDatabaseEntry(String fqname, Long revision) {
    super(fqname, revision);
  }
    
  
  public XMOMDomDatabaseEntry(DOM object) {
    super(object);
    wraps = concatServiceNames(object.getServiceNameToOperationMap().keySet());
    instanceServiceReferences = retrieveInstanceServiceImplReference(object);
  }

  
  public void setWraps(String wraps) {
    this.wraps = wraps;
  }
  
  
  public String getWraps() {
    return wraps;
  }

  
  public void setInstanceServiceReferences(String instanceServiceReferences) {
    this.instanceServiceReferences = instanceServiceReferences;
  }
  
  
  public String getInstanceServiceReferences() {
    return instanceServiceReferences;
  }
  
  private static ResultSetReader<XMOMDomDatabaseEntry> reader = new ResultSetReader<XMOMDomDatabaseEntry>() {

    public XMOMDomDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMDomDatabaseEntry x = new XMOMDomDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(x, rs);
      x.isExtending = rs.getString(COL_EXTENDS);
      x.extendedBy = rs.getString(COL_EXTENDEDBY);
      x.possesses = rs.getString(COL_POSSESSES);
      x.possessedBy = rs.getString(COL_POSSESSEDBY);
      x.neededBy = rs.getString(COL_NEEDEDBY);
      x.producedBy = rs.getString(COL_PRODUCEDBY);
      x.wraps = rs.getString(COL_WRAPS);
      x.instancesUsedBy = rs.getString(COL_INSTANCESUSEDBY);
      x.usedInImplOf = rs.getString(COL_USEDINIMPLOF);
      x.instanceServiceReferences = rs.getString(COL_INSTANCESERVICEREFERENCES);
      return x;
    }

  };
  
  
  @Override
  public ResultSetReader<? extends XMOMDomDatabaseEntry> getReader() {
    return reader;
  }


  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    wraps = ((XMOMDomDatabaseEntry)data).wraps;
    instanceServiceReferences = ((XMOMDomDatabaseEntry)data).instanceServiceReferences;
  }
  
  
  public static class DynamicXMOMCacheReader implements ResultSetReader<XMOMDomDatabaseEntry> {

    private Set<XMOMDatabaseEntryColumn> selectedCols;

    public DynamicXMOMCacheReader(Set<XMOMDatabaseEntryColumn> selected) {
      selectedCols = selected;
    }

    public XMOMDomDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMDomDatabaseEntry entry = new XMOMDomDatabaseEntry();
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
      if (selectedCols.contains(XMOMDatabaseEntryColumn.WRAPS)) {
        entry.wraps = rs.getString(COL_WRAPS);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.INSTANCESUSEDBY)) {
        entry.instancesUsedBy = rs.getString(COL_INSTANCESUSEDBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.USEDINIMPLOF)) {
        entry.usedInImplOf = rs.getString(COL_USEDINIMPLOF);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.INSTANCESERVICEREFERENCES)) {
        entry.instanceServiceReferences = rs.getString(COL_INSTANCESERVICEREFERENCES);
      }
      return entry;
    }
  }
  

  @Override
  public XMOMDatabaseType getXMOMDatabaseType() {
    return XMOMDatabaseType.DATATYPE;
  }
  
  
  private String concatServiceNames(Collection<String> serviceNames) {
    if (serviceNames != null && serviceNames.size() > 0) {
      StringBuilder serviceBuilder = new StringBuilder();
      Iterator<String> serviceIter = serviceNames.iterator();
      String serviceName;
      while (serviceIter.hasNext()) {
        serviceName = serviceIter.next();
        serviceBuilder.append(getFqname()).append(".");
        serviceBuilder.append(serviceName);
        if (serviceIter.hasNext()) {
          serviceBuilder.append(SEPERATION_MARKER);
        }
      }
      return serviceBuilder.toString();
    } else {
      return "";
    }
  }
  
  private String retrieveInstanceServiceImplReference(DOM dom) {
    StringBuilder sb = new StringBuilder();
    List<Operation> ops = dom.getOperations();
    for (Operation op : ops) {
      if (op instanceof WorkflowCall) {
        WorkflowCall wfCall = (WorkflowCall)op;
        sb.append(wfCall.getWfFQClassName());
      }
    }
    
    return sb.toString();
  }
  
  
  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    switch (column) {
      case WRAPS :
        return wraps;
      case INSTANCESERVICEREFERENCES :
        return instanceServiceReferences;
      default :
        return super.getValueByColumn(column);
    }
  }

}
