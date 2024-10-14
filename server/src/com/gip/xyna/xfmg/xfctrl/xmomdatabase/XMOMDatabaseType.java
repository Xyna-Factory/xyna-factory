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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;


public enum XMOMDatabaseType {
  GENERIC("Generic", null, Arrays.asList(new XMOMDatabaseEntryColumn[] {
                                             XMOMDatabaseEntryColumn.FQNAME,
                                             XMOMDatabaseEntryColumn.PATH,
                                             XMOMDatabaseEntryColumn.NAME,
                                             XMOMDatabaseEntryColumn.LABEL,
                                             XMOMDatabaseEntryColumn.METADATA,
                                             XMOMDatabaseEntryColumn.DOCUMENTATION,
                                             XMOMDatabaseEntryColumn.FACTORYCOMPONENT,
                                             XMOMDatabaseEntryColumn.ID})) {

                    @Override
                    public XMOMDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
                      throw new RuntimeException("generateInstanceOfArchiveStorableWithPrimaryKey called for illegal ArchiveIdentifier: " + this.toString());
                    }
                  },
  EXCEPTION("Exception", XMOMExceptionDatabaseEntry.TABLENAME, Arrays.asList(new XMOMDatabaseEntryColumn[] {
                         XMOMDatabaseEntryColumn.EXTENDS, 
                         XMOMDatabaseEntryColumn.EXTENDEDBY,
                         XMOMDatabaseEntryColumn.POSSESSES,
                         XMOMDatabaseEntryColumn.POSSESSEDBY, 
                         XMOMDatabaseEntryColumn.NEEDEDBY,
                         XMOMDatabaseEntryColumn.PRODUCEDBY, 
                         XMOMDatabaseEntryColumn.THROWNBY,
                         XMOMDatabaseEntryColumn.INSTANCESUSEDBY,
                         XMOMDatabaseEntryColumn.USEDINIMPLOF})) {

                    @Override
                    public XMOMExceptionDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
                      return new XMOMExceptionDatabaseEntry(fqName, revision);
                    }
                  },
  DATATYPE("Datatype", XMOMDomDatabaseEntry.TABLENAME, Arrays.asList(new XMOMDatabaseEntryColumn[] {
                       XMOMDatabaseEntryColumn.EXTENDS,
                       XMOMDatabaseEntryColumn.EXTENDEDBY,
                       XMOMDatabaseEntryColumn.POSSESSES,
                       XMOMDatabaseEntryColumn.POSSESSEDBY,
                       XMOMDatabaseEntryColumn.NEEDEDBY, 
                       XMOMDatabaseEntryColumn.PRODUCEDBY,
                       XMOMDatabaseEntryColumn.WRAPS,
                       XMOMDatabaseEntryColumn.INSTANCESUSEDBY,
                       XMOMDatabaseEntryColumn.USEDINIMPLOF,
                       XMOMDatabaseEntryColumn.INSTANCESERVICEREFERENCES})) {

                    @Override
                    public XMOMDomDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
                      return new XMOMDomDatabaseEntry(fqName, revision);
                    }
                  }, 
  SERVICE("Service", null, Arrays.asList(new XMOMDatabaseEntryColumn[] {
                  XMOMDatabaseEntryColumn.PRODUCES,
                  XMOMDatabaseEntryColumn.NEEDS,
                  XMOMDatabaseEntryColumn.CALLEDBY,
                  XMOMDatabaseEntryColumn.EXCEPTIONS,
                  XMOMDatabaseEntryColumn.USESINSTANCESOF})) {

                    @Override
                    public XMOMDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
                      throw new RuntimeException("generateInstanceOfArchiveStorableWithPrimaryKey called for illegal ArchiveIdentifier: " + this.toString());
                    }
                  }, 
  WORKFLOW("Workflow", XMOMWorkflowDatabaseEntry.TABLENAME, Arrays.asList(new XMOMDatabaseEntryColumn[] {
                       XMOMDatabaseEntryColumn.CALLS,
                       XMOMDatabaseEntryColumn.INSTANCESERVICEREFERENCEOF,
                       XMOMDatabaseEntryColumn.IMPLUSES})) {

    @Override
    public XMOMWorkflowDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
      return new XMOMWorkflowDatabaseEntry(fqName, revision);
    }
  }, 
  OPERATION("Operation", XMOMOperationDatabaseEntry.TABLENAME, Arrays.asList(new XMOMDatabaseEntryColumn[] {XMOMDatabaseEntryColumn.GROUPEDBY})) {

    @Override
    public XMOMOperationDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
      return new XMOMOperationDatabaseEntry(fqName, revision);
    }
  }, 
  SERVICEGROUP("ServiceGroup", XMOMServiceGroupDatabaseEntry.TABLENAME, Arrays.asList(new XMOMDatabaseEntryColumn[] {XMOMDatabaseEntryColumn.GROUPS})) {

    @Override
    public XMOMServiceGroupDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
      return new XMOMServiceGroupDatabaseEntry(fqName, revision);
    }
  },
  DATAMODEL("DataModel", XMOMDataModelDatabaseEntry.TABLENAME, new ArrayList<XMOMDatabaseEntryColumn>()) {
    
    @Override
    public XMOMDataModelDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
      return new XMOMDataModelDatabaseEntry(fqName); //Datenmodelle haben immer revision -2
    }
  },
  FORMDEFINITION("Form", XMOMFormDefinitionDatabaseEntry.TABLENAME, new ArrayList<XMOMDatabaseEntryColumn>()) {
    
    @Override
    public XMOMFormDefinitionDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision) {
      return new XMOMFormDefinitionDatabaseEntry(fqName, revision);
    }
  }
  ;


  private List<XMOMDatabaseEntryColumn> allowedColumns;
  private String archiveName;
  private String name;

  private XMOMDatabaseType(String name, String archiveName,
                      List<XMOMDatabaseEntryColumn> allowedColumns) {
    this.name = name;
    this.archiveName = archiveName;
    this.allowedColumns = allowedColumns;
  }


  public List<XMOMDatabaseEntryColumn> getAllowedColumns() {
    return allowedColumns;
  }


  public String getArchiveIdentifier() {
    return archiveName;
  }
  
  
  public String getName() {
    return name;
  }

  
  public static XMOMDatabaseType getXMOMDatabaseTypeByName(String name) {
    for (XMOMDatabaseType type : values()) {
      if (type.name.equals(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Illegal name for XMOMDatabaseType: "+name);
  }


  public static XMOMDatabaseType getCorrespondingXMOMDatabaseTypeByXMOMType(XMOMType type) {
    switch (type) {
      case DATATYPE :
        return XMOMDatabaseType.DATATYPE;
      case EXCEPTION :
        return XMOMDatabaseType.EXCEPTION;
      case WORKFLOW :
        return XMOMDatabaseType.WORKFLOW;
      case FORM :
        return XMOMDatabaseType.FORMDEFINITION;
      default :
        throw new RuntimeException();
    }
  }


  public static List<XMOMDatabaseType> getXMOMDatabaseTypeByXMOMType(XMOMType type) {
    switch (type) {
      case DATATYPE :
        List<XMOMDatabaseType> result = new ArrayList<XMOMDatabaseType>();
        result.add(XMOMDatabaseType.DATATYPE);
        result.add(XMOMDatabaseType.OPERATION);
        result.add(XMOMDatabaseType.SERVICEGROUP);
        return result;
      case EXCEPTION :
        result = new ArrayList<XMOMDatabaseType>();
        result.add(XMOMDatabaseType.EXCEPTION);
        return result;
      case WORKFLOW :
        result = new ArrayList<XMOMDatabaseType>();
        result.add(XMOMDatabaseType.WORKFLOW);
        return result;
      case FORM :
        result = new ArrayList<XMOMDatabaseType>();
        result.add(XMOMDatabaseType.FORMDEFINITION);
        return result;
      default :
        throw new RuntimeException();
    }
  }
  
  public static XMOMDatabaseType getXMOMDatabaseTypeByDependencySourceType(DependencySourceType type) {
    switch (type) {
      case DATATYPE :
        return XMOMDatabaseType.DATATYPE;
      case WORKFLOW :
        return XMOMDatabaseType.WORKFLOW;
      case XYNAEXCEPTION :
        return XMOMDatabaseType.EXCEPTION;
      case FILTER :
      case ORDERTYPE :
      case SHAREDLIB :
      case TRIGGER :
      case XYNAFACTORY :
      case XYNAPROPERTY :
        return null;
      default :
        return null;
    }
  }

  
  public static List<XMOMDatabaseType> getXMOMDatabaseTypesForColumn(XMOMDatabaseEntryColumn column) {
    List<XMOMDatabaseType> types = new ArrayList<XMOMDatabaseType>();
    for (XMOMDatabaseType archiveType : values()) {
      if (archiveType.allowedColumns.contains(column)) {
        if (archiveType == XMOMDatabaseType.GENERIC) {
          types.add(XMOMDatabaseType.DATATYPE);
          types.add(XMOMDatabaseType.EXCEPTION);
          types.add(XMOMDatabaseType.WORKFLOW);
          types.add(XMOMDatabaseType.OPERATION);
          types.add(XMOMDatabaseType.SERVICEGROUP);
          return types;
        } else if (archiveType == XMOMDatabaseType.SERVICE) {
          types.add(XMOMDatabaseType.WORKFLOW);
          types.add(XMOMDatabaseType.OPERATION);
          return types;
        } else {
          types.add(archiveType);
        }  
      }
    }
    return types;
  }

  
  public abstract XMOMDatabaseEntry generateInstanceOfArchiveStorableWithPrimaryKey(String fqName, Long revision);
  
  
  public static Collection<XMOMDatabaseType> resolveXMOMDatabaseTypes(Collection<XMOMDatabaseType> unresolvedTypes) {
    Collection<XMOMDatabaseType> resolved = new HashSet<XMOMDatabaseType>();
    for (XMOMDatabaseType type : unresolvedTypes) {
      resolved.addAll(resolveXMOMDatabaseType(type)); 
    }
    return resolved;
  }
  
  
  private static Collection<XMOMDatabaseType> resolveXMOMDatabaseType(XMOMDatabaseType unresolvedType) {
    Collection<XMOMDatabaseType> resolved = new HashSet<XMOMDatabaseType>();
    switch (unresolvedType) {
      case GENERIC :
        resolved.add(XMOMDatabaseType.DATATYPE);
        resolved.add(XMOMDatabaseType.EXCEPTION);
        resolved.add(XMOMDatabaseType.SERVICEGROUP);
        resolved.add(XMOMDatabaseType.WORKFLOW);
        resolved.add(XMOMDatabaseType.OPERATION);
        break;
      case SERVICE :
        resolved.add(XMOMDatabaseType.WORKFLOW);
        resolved.add(XMOMDatabaseType.OPERATION);
        break;
      default :
        resolved.add(unresolvedType);
        break;
    }
    return resolved;
  }
  
  //transform Archives; change abstract ArchiveTypes to specific ones if necessary
  // Generic => ALL | Service => WORKFLOW, OPERATION | Generic + X => X | SERVICE + WORKFLOW or OPERATION => WORKFLOW or OPERATION
  public static Set<XMOMDatabaseType> transformArchives(Collection<XMOMDatabaseType> archiveForSelect) {
    Set<XMOMDatabaseType> transformed = new HashSet<XMOMDatabaseType>();
    if (archiveForSelect.size() == 1) {
      XMOMDatabaseType type = archiveForSelect.iterator().next();
      if (type == XMOMDatabaseType.GENERIC) {
        transformed.add(XMOMDatabaseType.DATATYPE);
        transformed.add(XMOMDatabaseType.EXCEPTION);
        transformed.add(XMOMDatabaseType.WORKFLOW);
        transformed.add(XMOMDatabaseType.OPERATION);
        transformed.add(XMOMDatabaseType.SERVICEGROUP);
        transformed.add(XMOMDatabaseType.DATAMODEL);
        transformed.add(XMOMDatabaseType.FORMDEFINITION);
      } else if (type == XMOMDatabaseType.SERVICE) {
        transformed.add(XMOMDatabaseType.WORKFLOW);
        transformed.add(XMOMDatabaseType.OPERATION);
      } else {
        transformed.addAll(archiveForSelect);
      }
    } else  if (archiveForSelect.size() == 2 && archiveForSelect.contains(XMOMDatabaseType.GENERIC) && archiveForSelect.contains(XMOMDatabaseType.SERVICE)) {
      transformed.add(XMOMDatabaseType.WORKFLOW);
      transformed.add(XMOMDatabaseType.OPERATION);
    } else {
      Iterator<XMOMDatabaseType> typeIter = archiveForSelect.iterator();
      while (typeIter.hasNext()) {
        XMOMDatabaseType actType = typeIter.next();
        if (actType != XMOMDatabaseType.GENERIC && actType != XMOMDatabaseType.SERVICE) {
          transformed.add(actType);
        }
      }
    }
    return transformed;
  }
  
  
  //A select is invalid (will always return an empty set) if it does checks on relations that can never manifest in a single object
  public static boolean areArchiveTypesInvalid(Collection<XMOMDatabaseType> types) {
    outer : for (XMOMDatabaseType outerArchive : types) {
      inner : for (XMOMDatabaseType innerArchive : types) {
        switch (outerArchive) {
          case GENERIC :
            continue outer;
          case EXCEPTION :
          case DATATYPE :
            switch (innerArchive) {
              case SERVICE :
              case WORKFLOW :
              case OPERATION :
              case SERVICEGROUP :
                return true;
              default :
                continue inner;
            }
          case SERVICE :
            switch (innerArchive) {
              case EXCEPTION :
              case DATATYPE :
              case SERVICEGROUP :
                return true;
              default :
                continue inner;
            }
          case WORKFLOW :
            switch (innerArchive) {
              case EXCEPTION :
              case DATATYPE :
              case OPERATION :
              case SERVICEGROUP :
                return true;
              default :
                continue inner;
            }
          case OPERATION :
            switch (innerArchive) {
              case EXCEPTION :
              case DATATYPE :
              case WORKFLOW :
              case SERVICEGROUP :
                return true;
              default :
                continue inner;
            }
          case SERVICEGROUP :
            switch (innerArchive) {
              case EXCEPTION :
              case DATATYPE :
              case WORKFLOW :
              case OPERATION :
              case SERVICE :
                return true;
              default :
                continue inner;
            }
        }
      }
    }
    return false;
  }
  
  
  public static List<XMOMDatabaseType> getArchiveTypesForColumnLookup(XMOMDatabaseEntryColumn column, boolean onlyInstantiableTypes) {
    if (column.hasReversedColumn()) {
      XMOMDatabaseEntryColumn reversedColumn = column.getReversedColumn();
      List<XMOMDatabaseType> typesForLookup = new ArrayList<XMOMDatabaseType>();
      for (XMOMDatabaseType type : values()) {
        if (type.getAllowedColumns().contains(reversedColumn)) {
          if (type == SERVICE && onlyInstantiableTypes) {
            typesForLookup.add(WORKFLOW);
            typesForLookup.add(OPERATION);
          } else {
            typesForLookup.add(type);
          }
        }
      }
      return typesForLookup;
    } else {
      // unreversable columns are generic
      return Arrays.asList(DATATYPE, EXCEPTION, WORKFLOW, OPERATION, SERVICEGROUP);
    }
  }

}
