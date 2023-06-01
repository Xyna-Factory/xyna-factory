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
package com.gip.xyna.update.outdatedclasses_5_1_4_5;



import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;



public abstract class XMOMServiceDatabaseEntry extends XMOMDatabaseEntry {

  public static final String COL_NEEDS = "needs";
  public static final String COL_PRODUCES = "produces";
  public static final String COL_EXCEPTIONS = "exceptions";
  public static final String COL_CALLEDBY = "calledBy";
  public static final String COL_GROUPEDBY = "groupedBy";
  public static final String COL_USESINSTANCESOF = "usesInstancesOf";

  private static final long serialVersionUID = -7301378884111678454L;

  @Column(name = COL_NEEDS, size = 250)
  protected String needs;

  @Column(name = COL_PRODUCES, size = 250)
  protected String produces;

  @Column(name = COL_EXCEPTIONS, size = 250)
  protected String exceptions;

  @Column(name = COL_CALLEDBY, size = 1000)
  protected String calledBy;
  
  @Column(name = COL_GROUPEDBY, size = 50)
  protected String groupedBy;
  
  @Column(name = COL_USESINSTANCESOF, size = 2000)
  protected String usesInstancesOf;
  
  
  public XMOMServiceDatabaseEntry() {
  }
  
  
  public XMOMServiceDatabaseEntry(String fqName) {
    super(fqName);
  }
  
  
  public XMOMServiceDatabaseEntry(String fqName, String label, String path, String name, String documentation, String metadata, String serviceGroupName,
                               List<? extends AVariable> inputVars, List<? extends AVariable> outputVars, List<? extends AVariable> thrown, Boolean factoryComponent) {
    
    super(fqName, label, path, name, documentation, metadata, factoryComponent);
    needs = concatVariables(inputVars);
    produces = concatVariables(outputVars);
    exceptions = concatVariables(thrown);
    groupedBy = serviceGroupName;
    // retrieveBaseInstantiations will append needs,produces & exceptions for OperationEntries making it important to fill those first
    usesInstancesOf = retrieveBaseInstantiations(fqName);
  }
  
  
  public String getNeeds() {
    return needs;
  }

  
  public void setNeeds(String needs) {
    this.needs = needs;
  }

  
  public String getProduces() {
    return produces;
  }

  
  public void setProduces(String produces) {
    this.produces = produces;
  }

  
  public String getExceptions() {
    return exceptions;
  }

  
  public void setExceptions(String exceptions) {
    this.exceptions = exceptions;
  }

  
  public String getCalledBy() {
    return calledBy;
  }

  
  public void setCalledBy(String calledBy) {
    this.calledBy = calledBy;
  }
  
  
  public String getGroupedBy() {
    return groupedBy;
  }

  
  public void setGroupedBy(String groupedBy) {
    this.groupedBy = groupedBy;
  }
  
  
  public String getUsesInstancesOf() {
    return usesInstancesOf;
  }

  
  public void setUsesInstancesOf(String usesInstancesOf) {
    this.usesInstancesOf = usesInstancesOf;
  }

  
  @Override
  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    needs = ((XMOMServiceDatabaseEntry)data).needs;
    produces = ((XMOMServiceDatabaseEntry)data).produces;
    exceptions = ((XMOMServiceDatabaseEntry)data).exceptions;
    calledBy = ((XMOMServiceDatabaseEntry)data).calledBy;
    groupedBy = ((XMOMServiceDatabaseEntry)data).groupedBy;
    usesInstancesOf  = ((XMOMServiceDatabaseEntry)data).usesInstancesOf;
  }
  
  
  @Override
  public abstract XMOMServiceDatabaseEntry clone() throws CloneNotSupportedException;
  
  
  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    switch (column) {
      case PRODUCES :
        return produces;
      case NEEDS :
        return needs;
      case EXCEPTIONS :
        return exceptions;
      case CALLEDBY :
        return calledBy;
      case GROUPEDBY :
        return groupedBy;
      case USESINSTANCESOF :
        return usesInstancesOf;
      default :
        return super.getValueByColumn(column);
    }
  }
  
  
  protected abstract String retrieveBaseInstantiations(String originalFqName);
  
  
  void extendInstantiations(ODSConnection con) throws PersistenceLayerException {
    Set<String> baseInstatiationSet = new HashSet<String>();
    for (String instantiation : usesInstancesOf.split(SEPERATION_MARKER)) {
      baseInstatiationSet.add(instantiation);
    }
    
    Set<String> instatiationSet = new HashSet<String>(baseInstatiationSet);
    for (String instantiation : baseInstatiationSet) {
      if (instantiation == null ||
          instantiation.equals("")) {
        continue;
      }
      XMOMDatabaseEntry entry = new XMOMDomDatabaseEntry(instantiation);
      try {
        con.queryOneRow(entry);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        entry = new XMOMExceptionDatabaseEntry(instantiation);
        try {
          con.queryOneRow(entry);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
          continue;
        }
      }
      String extenders = entry.getValueByColumn(XMOMDatabaseEntryColumn.EXTENDS);
      if (extenders != null &&
          !extenders.equals("") &&
          !extenders.equals("null")) {
        for (String extender : extenders.split(SEPERATION_MARKER)) {
          instatiationSet.add(extender);
        }
      }
    }
    StringBuilder instantiationBuilder = new StringBuilder();
    Iterator<String> instantiationIter = instatiationSet.iterator();
    while (instantiationIter.hasNext()) {
      String instantiation = instantiationIter.next();
      if (instantiation != null &&
          !instantiation.equals("") &&
          !instantiation.equals("null")) {
        instantiationBuilder.append(instantiation);
        if (instantiationIter.hasNext()) {
          instantiationBuilder.append(SEPERATION_MARKER);
        }
      }
    }
    usesInstancesOf = instantiationBuilder.toString();
  }
  
  
}
