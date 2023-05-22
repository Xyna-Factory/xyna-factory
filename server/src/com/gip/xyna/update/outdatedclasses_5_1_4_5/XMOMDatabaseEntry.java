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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCall;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCallServiceReference;



public abstract class XMOMDatabaseEntry extends Storable<XMOMDatabaseEntry> implements Cloneable {

  public static final String COL_FQNAME = "fqname";
  public static final String COL_LABEL = "label";
  public static final String COL_PATH = "path";
  public static final String COL_NAME = "name";
  public static final String COL_DOCUMENTATION = "documentation";
  public static final String COL_METADATA = "metadata";
  public static final String COL_FACTORYCOMPONENT = "factorycomponent";
  
  static final String SEPERATION_MARKER = ",";
  
  protected final static Logger logger = CentralFactoryLogging.getLogger(XMOMDatabaseEntry.class);
  

  private static final long serialVersionUID = -7301378884111678454L;

  @Column(name = COL_FQNAME, size = 250)
  private String fqname;

  @Column(name = COL_LABEL, size = 250)
  private String label;
  
  @Column(name = COL_PATH, size = 250)
  private String path;

  @Column(name = COL_NAME, size = 50)
  private String name;

  @Column(name = COL_DOCUMENTATION, size = 250)
  private String documentation;

  @Column(name = COL_METADATA, size = 250)
  private String metadata;
  
  @Column(name = COL_FACTORYCOMPONENT)
  private Boolean factorycomponent;
  
  
  public XMOMDatabaseEntry() {
  }

  
  public XMOMDatabaseEntry(String fqname) {
    if (!isValidFQName(fqname)) {
      throw new RuntimeException("Creation of XMOMDatabaseEntry without fqName detected.");
    }
    this.fqname = fqname;
  }
  
  
  public XMOMDatabaseEntry(String fqname, String label, String path, String name, String documentation, String metadata, Boolean factoryComponent) {
    this(fqname);
    if (label != null) {
      //currently the label is only used in user driven XMOM-Searches, to make those case insensitive we only save (&search) lower cases
      this.label = label.toLowerCase();
    } else {
      this.label = name.toLowerCase();
    }
    this.path = path;
    this.name = name;
    this.documentation = documentation;
    this.metadata = metadata;
    this.factorycomponent = factoryComponent;
  }
  
  
  static boolean isValidFQName(String fqName) {
    if (fqName == null ||
        fqName.equals("") ||
        fqName.equals(".") ||
        fqName.equals("null")) {
      return false;
    } else {
      return true;
    }
  }
  
  
  protected static String concatVariables(List<? extends AVariable> vars) {
    if (vars != null && vars.size() > 0) {
      StringBuilder varBuilder = new StringBuilder("");
      Iterator<? extends AVariable> aIter = vars.iterator();
      AVariable aVariable;
      while (aIter.hasNext()) {
        aVariable = aIter.next();
        if (!aVariable.isJavaBaseType()) {
          varBuilder.append(aVariable.getOriginalPath());
          varBuilder.append(".");
          varBuilder.append(aVariable.getOriginalName());
          if (aIter.hasNext()) {
            varBuilder.append(SEPERATION_MARKER);
          }
        }
      }
      String varString = varBuilder.toString();
      if (varString != null &&
          !varString.equals(".") &&
          !varString.equals("null")) {
        return varString;
      } else {
        return "";
      }
    } else {
      return "";
    }
  }
  

  protected static String concatOperations(String serviceFqName, List<? extends Operation> ops) {
    StringBuilder opBuilder = new StringBuilder();
    Iterator<? extends Operation> aIter = ops.iterator();
    Operation operation;
    while (aIter.hasNext()) {
      operation = aIter.next();
      if (operation instanceof WorkflowCallServiceReference) {
        WorkflowCall wf = (WorkflowCall) operation;
        opBuilder.append(wf.getWfFQClassName());
      } else {
        opBuilder.append(serviceFqName);
        opBuilder.append(".");
        opBuilder.append(operation.getName());
      }
      if (aIter.hasNext()) {
        opBuilder.append(SEPERATION_MARKER);
      }
    }
    return opBuilder.toString();
  }
  
  
  
  public String getFqname() {
    return fqname;
  }
  
  public void setFqname(String fqname) {
    this.fqname = fqname;
  }

  
  public String getLabel() {
    return label;
  }

  
  public void setLabel(String label) {
    this.label = label;
  }

  
  public String getDocumentation() {
    return documentation;
  }

  
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  
  public String getMetadata() {
    return metadata;
  }

  
  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  
  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }
  
  
  public String getPath() {
    return path;
  }

  
  public void setPath(String path) {
    this.path = path;
  }
  
  
  public abstract XMOMDatabaseType getXMOMDatabaseType();


  @Override
  public Object getPrimaryKey() {
    return getFqname();
  }

  public static void readFromResultSetReader(XMOMDatabaseEntry entry, ResultSet rs) throws SQLException {
    entry.fqname = rs.getString(COL_FQNAME);
    entry.label = rs.getString(COL_LABEL);
    entry.name = rs.getString(COL_NAME);
    entry.path = rs.getString(COL_PATH);
    entry.documentation = rs.getString(COL_DOCUMENTATION);
    entry.metadata = rs.getString(COL_METADATA);
    entry.factorycomponent = rs.getBoolean(COL_FACTORYCOMPONENT);
  }
  

  public static void readFromResultSetReader(XMOMDatabaseEntry entry, Set<XMOMDatabaseEntryColumn> selectedCols,
                                             ResultSet rs) throws SQLException {
    if (selectedCols.contains(XMOMDatabaseEntryColumn.FQNAME)) {
      entry.fqname = rs.getString(COL_FQNAME);
    }
    if (selectedCols.contains(XMOMDatabaseEntryColumn.LABEL)) {
      entry.label = rs.getString(COL_LABEL);
    }
    if (selectedCols.contains(XMOMDatabaseEntryColumn.PATH)) {
      entry.path = rs.getString(COL_PATH);
    }
    if (selectedCols.contains(XMOMDatabaseEntryColumn.NAME)) {
      entry.name = rs.getString(COL_NAME);
    }
    if (selectedCols.contains(XMOMDatabaseEntryColumn.DOCUMENTATION)) {
      entry.documentation = rs.getString(COL_DOCUMENTATION);
    }
    if (selectedCols.contains(XMOMDatabaseEntryColumn.METADATA)) {
      entry.metadata = rs.getString(COL_METADATA);
    }
    if (selectedCols.contains(XMOMDatabaseEntryColumn.FACTORYCOMPONENT)) {
      entry.factorycomponent = rs.getBoolean(COL_FACTORYCOMPONENT);
    }
  }

  @Override
  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    XMOMDatabaseEntry cast = data;
    fqname = cast.fqname;
    label = cast.label;
    path = cast.path;
    name = cast.name;
    documentation = cast.documentation;
    metadata = cast.metadata;
    factorycomponent = cast.factorycomponent;
  }
  
  
  protected static String generateFqNameForOperation(DOM enclosingDOM, String serviceName, JavaOperation operation) {
    return generateFqNameForOperation(enclosingDOM, serviceName, operation.getName());
  }
  
  
  protected static String generateFqNameForOperation(DOM enclosingDOM, String serviceName, String operationName) {
    StringBuilder nameBuilder = new StringBuilder(enclosingDOM.getOriginalFqName());
    nameBuilder.append(".").append(serviceName).append(".").append(operationName);
    return nameBuilder.toString();
  }
  
  
  protected static String getDomOriginalFQNameFromOperationFqName(String operationName) {
    StringBuilder nameBuilder = new StringBuilder();
    String[] operationNameParts = operationName.split("\\.");
    int relevantParts = operationNameParts.length - 2;
    for (int i=0; i<relevantParts; i++) {
      nameBuilder.append(operationNameParts[i]);
      if (i<relevantParts-1) {
        nameBuilder.append(".");
      }
    }
    return nameBuilder.toString();
  }
  
  
  protected static String generateSimpleNameForOperation(DOM enclosingDOM, String serviceName, JavaOperation operation) {
    return generateSimpleNameForOperation(enclosingDOM, serviceName, operation.getName());
  }
  
  
  protected static String generateSimpleNameForOperation(DOM enclosingDOM, String serviceName, String operationName) {
    StringBuilder nameBuilder = new StringBuilder(enclosingDOM.getOriginalSimpleName());
    nameBuilder.append(".").append(serviceName).append(".").append(operationName);
    return nameBuilder.toString();
  }
  
  
  protected static String generateFqNameForServiceGroup(DOM enclosingDOM, String serviceName) {
    StringBuilder nameBuilder = new StringBuilder(enclosingDOM.getOriginalFqName());
    nameBuilder.append(".").append(serviceName);
    return nameBuilder.toString();
  }
  
  
  protected static String generateSimpleNameForServiceGroup(DOM enclosingDOM, String serviceName) {
    StringBuilder nameBuilder = new StringBuilder(enclosingDOM.getOriginalSimpleName());
    nameBuilder.append(".").append(serviceName);
    return nameBuilder.toString();
  }
  
  @Override
  public abstract XMOMDatabaseEntry clone() throws CloneNotSupportedException;
  
  
  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    switch (column) {
      case FQNAME :
        return fqname;
      case LABEL :
        return label;
      case PATH :
        return path;
      case NAME :
        return name;
      case DOCUMENTATION :
        return documentation;
      case METADATA :
        return metadata;
      case FACTORYCOMPONENT:
        return Boolean.toString(factorycomponent);
      default :
        throw new IllegalArgumentException(column.toString());
    }
  }


  public Boolean getFactorycomponent() {
    return factorycomponent;
  }


  
  public void setFactorycomponent(Boolean factorycomponent) {
    this.factorycomponent = factorycomponent;
  }
  
}
