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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;


public enum XMOMDatabaseEntryColumn {
  ID(XMOMDatabaseEntry.COL_ID, null),
  FQNAME(XMOMDatabaseEntry.COL_FQNAME, null),
  FQNAMELOWERCASE(XMOMDatabaseEntry.COL_FQNAME_LOWERCASE, null),
  NAME(XMOMDatabaseEntry.COL_NAME, null),
  PATH(XMOMDatabaseEntry.COL_PATH, null),
  LABEL(XMOMDatabaseEntry.COL_LABEL, null),
  CASE_SENSITIVE_LABEL(XMOMDatabaseEntry.COL_CASE_SENSITIVE_LABEL, null),
  DOCUMENTATION(XMOMDatabaseEntry.COL_DOCUMENTATION, null),
  METADATA(XMOMDatabaseEntry.COL_METADATA, null),
  FACTORYCOMPONENT(XMOMDatabaseEntry.COL_FACTORYCOMPONENT, null),
  REVISION(XMOMDatabaseEntry.COL_REVISION, null),
  TIMESTAMP(XMOMDatabaseEntry.COL_TIMESTAMP, null),
  EXTENDS(XMOMDomOrExceptionDatabaseEntry.COL_EXTENDS, XMOMDomOrExceptionDatabaseEntry.COL_EXTENDEDBY),
  EXTENDEDBY(XMOMDomOrExceptionDatabaseEntry.COL_EXTENDEDBY, XMOMDomOrExceptionDatabaseEntry.COL_EXTENDS),
  NEEDS(XMOMServiceDatabaseEntry.COL_NEEDS, XMOMDomOrExceptionDatabaseEntry.COL_NEEDEDBY),
  PRODUCES(XMOMServiceDatabaseEntry.COL_PRODUCES, XMOMDomOrExceptionDatabaseEntry.COL_PRODUCEDBY),
  POSSESSES(XMOMDomOrExceptionDatabaseEntry.COL_POSSESSES, XMOMDomOrExceptionDatabaseEntry.COL_POSSESSEDBY),
  POSSESSEDBY(XMOMDomOrExceptionDatabaseEntry.COL_POSSESSEDBY, XMOMDomOrExceptionDatabaseEntry.COL_POSSESSES),
  USESINSTANCESOF(XMOMServiceDatabaseEntry.COL_USESINSTANCESOF, XMOMDomOrExceptionDatabaseEntry.COL_INSTANCESUSEDBY),
  EXCEPTIONS(XMOMServiceDatabaseEntry.COL_EXCEPTIONS, XMOMExceptionDatabaseEntry.COL_THROWNBY),
  GROUPS(XMOMServiceGroupDatabaseEntry.COL_GROUPS, XMOMServiceDatabaseEntry.COL_GROUPEDBY),
  CALLS(XMOMWorkflowDatabaseEntry.COL_CALLS, XMOMServiceDatabaseEntry.COL_CALLEDBY),
  NEEDEDBY(XMOMDomOrExceptionDatabaseEntry.COL_NEEDEDBY, XMOMServiceDatabaseEntry.COL_NEEDS),
  PRODUCEDBY(XMOMDomOrExceptionDatabaseEntry.COL_PRODUCEDBY, XMOMServiceDatabaseEntry.COL_PRODUCES),
  THROWNBY(XMOMExceptionDatabaseEntry.COL_THROWNBY, XMOMServiceDatabaseEntry.COL_EXCEPTIONS),
  GROUPEDBY(XMOMServiceDatabaseEntry.COL_GROUPEDBY, XMOMServiceGroupDatabaseEntry.COL_GROUPS),
  WRAPS(XMOMDomDatabaseEntry.COL_WRAPS, XMOMServiceGroupDatabaseEntry.COL_WRAPPEDBY),
  CALLEDBY(XMOMServiceDatabaseEntry.COL_CALLEDBY, XMOMWorkflowDatabaseEntry.COL_CALLS),
  INSTANCESUSEDBY(XMOMDomOrExceptionDatabaseEntry.COL_INSTANCESUSEDBY, XMOMServiceDatabaseEntry.COL_USESINSTANCESOF),
  WRAPPEDBY(XMOMServiceGroupDatabaseEntry.COL_WRAPPEDBY, XMOMDomDatabaseEntry.COL_WRAPS),
  INSTANCESERVICEREFERENCES(XMOMDomDatabaseEntry.COL_INSTANCESERVICEREFERENCES, XMOMWorkflowDatabaseEntry.COL_INSTANCESERVICEREFERENCEOF),
  INSTANCESERVICEREFERENCEOF(XMOMWorkflowDatabaseEntry.COL_INSTANCESERVICEREFERENCEOF, XMOMDomDatabaseEntry.COL_INSTANCESERVICEREFERENCES),
  USEDINIMPLOF(XMOMDomOrExceptionDatabaseEntry.COL_USEDINIMPLOF, XMOMWorkflowDatabaseEntry.COL_IMPLUSES),
  IMPLUSES(XMOMWorkflowDatabaseEntry.COL_IMPLUSES, XMOMDomOrExceptionDatabaseEntry.COL_USEDINIMPLOF);
  
  private final String columnName;
  private final String reversedRelationColumnName;
  
  
  private XMOMDatabaseEntryColumn(String columnName, String reversedRelationColumnName) {
    this.columnName = columnName;
    this.reversedRelationColumnName = reversedRelationColumnName;
  }
  
  
  public String getColumnName() {
    return columnName;
  }
  
  
  public String getReversedColumnName() {
    return reversedRelationColumnName;
  }
  
  
  public XMOMDatabaseEntryColumn getReversedColumn() {
    if (hasReversedColumn()) {
      return getXMOMColumnByName(reversedRelationColumnName);
    } else {
      return this;
    }
  }
  
  
  public boolean hasReversedColumn() {
    return reversedRelationColumnName != null;
  }
  
  
  public static XMOMDatabaseEntryColumn getXMOMColumnByName(String columnName) {
    for (XMOMDatabaseEntryColumn type : values()) {
      if (type.columnName.equalsIgnoreCase(columnName)) {
        return type;
      }
    }
    throw new IllegalArgumentException(columnName);
  }
  

  public final static XMOMDatabaseEntryColumn[] ALL_GENERICS_ARRAY = new XMOMDatabaseEntryColumn[] {
    ID, FQNAME, FQNAMELOWERCASE, NAME, PATH, LABEL, CASE_SENSITIVE_LABEL, DOCUMENTATION, METADATA, FACTORYCOMPONENT, REVISION, TIMESTAMP};
  public final static Collection<XMOMDatabaseEntryColumn> ALL_GENERICS = new HashSet<XMOMDatabaseEntryColumn>(Arrays.asList(ALL_GENERICS_ARRAY));
  public final static XMOMDatabaseEntryColumn[] ALL_FORWARD_RELATIONS_ARRAY = new XMOMDatabaseEntryColumn[] {
    CALLS, POSSESSES, WRAPS, GROUPS, GROUPEDBY, WRAPPEDBY, EXTENDS, USESINSTANCESOF, INSTANCESERVICEREFERENCES, IMPLUSES};
  public final static Collection<XMOMDatabaseEntryColumn> ALL_FORWARD_RELATIONS = new HashSet<XMOMDatabaseEntryColumn>(Arrays.asList(ALL_FORWARD_RELATIONS_ARRAY));
  public final static XMOMDatabaseEntryColumn[] ALL_BACKWARD_RELATIONS_ARRAY = new XMOMDatabaseEntryColumn[] { 
    CALLEDBY, POSSESSEDBY, WRAPS, GROUPS, GROUPEDBY, WRAPPEDBY, EXTENDEDBY, INSTANCESUSEDBY, INSTANCESERVICEREFERENCEOF, USEDINIMPLOF};
  public final static Collection<XMOMDatabaseEntryColumn> ALL_BACKWARD_RELATIONS = new HashSet<XMOMDatabaseEntryColumn>(Arrays.asList(ALL_BACKWARD_RELATIONS_ARRAY));
  
  
}
