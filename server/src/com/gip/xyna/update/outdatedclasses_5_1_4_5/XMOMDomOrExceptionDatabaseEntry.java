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



import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;




public abstract class XMOMDomOrExceptionDatabaseEntry extends XMOMDatabaseEntry {

  public static final String COL_EXTENDS = "extends";
  public static final String COL_EXTENDEDBY = "extendedBy";
  public static final String COL_POSSESSES = "possesses";
  public static final String COL_POSSESSEDBY = "possessedBy";
  public static final String COL_NEEDEDBY = "neededBy";
  public static final String COL_PRODUCEDBY = "producedBy";
  public static final String COL_INSTANCESUSEDBY = "instancesUsedBy";
   

  private static final long serialVersionUID = -7301378884111678454L;

  
  @Column(name = COL_EXTENDS, size = 100)
  protected String isExtending;

  @Column(name = COL_EXTENDEDBY, size = 100)
  protected String extendedBy;
  
  @Column(name = COL_POSSESSES, size = 250)
  protected String possesses;
  
  @Column(name = COL_POSSESSEDBY, size = 1000)
  protected String possessedBy;
  
  @Column(name = COL_NEEDEDBY, size = 1000)
  protected String neededBy;
  
  @Column(name = COL_PRODUCEDBY, size = 1000)
  protected String producedBy;
  
  @Column(name = COL_INSTANCESUSEDBY, size = 1000)
  protected String instancesUsedBy;

  
  public XMOMDomOrExceptionDatabaseEntry() {
  }

  
  public XMOMDomOrExceptionDatabaseEntry(String fqname) {
    super(fqname);
  }
    
  
  //generate cache entry for dom or exception
  public XMOMDomOrExceptionDatabaseEntry(DomOrExceptionGenerationBase object) {
    super(object.getOriginalFqName(), object.getLabel(), object.getOriginalPath(), object.getOriginalSimpleName(), "", object.getXmlRootTagMetdata(), object.isXynaFactoryComponent());
    isExtending = retrieveSuperClasses(object);
    possesses = concatVariables(object.getAllMemberVarsIncludingInherited());
  }
  
  
  public String getExtends() {
    return isExtending;
  }


  public void setExtends(String isExtending) {
    this.isExtending = isExtending;
  }
  
  
  public String getExtendedBy() {
    return extendedBy;
  }


  public void setExtendedBy(String extendedBy) {
    this.extendedBy = extendedBy;
  }


  public String getPossesses() {
    return possesses;
  }

  
  public void setPossesses(String possesses) {
    this.possesses = possesses;
  }
  
  
  public String getPossessedBy() {
    return possessedBy;
  }

  
  public void setPossessedBy(String possessedBy) {
    this.possessedBy = possessedBy;
  }

  
  public String getNeededBy() {
    return neededBy;
  }

  
  public void setNeededBy(String neededBy) {
    this.neededBy = neededBy;
  }
  
  
  public String getProducedBy() {
    return producedBy;
  }

  
  public void setProducedBy(String producedBy) {
    this.producedBy = producedBy;
  }
  
  
  public String getInstancesUsedBy() {
    return instancesUsedBy;
  }

  
  public void setInstancesUsedBy(String instancesUsedBy) {
    this.instancesUsedBy = instancesUsedBy;
  }
  
  
  private String retrieveSuperClasses(DomOrExceptionGenerationBase object) {
    StringBuilder baseTypeBuilder = new StringBuilder();
    DomOrExceptionGenerationBase superclass = object.getSuperClassGenerationObject();
    while (superclass != null) {
      String superclassName = superclass.getOriginalFqName();
      if (XMOMDatabaseEntry.isValidFQName(superclassName)) {
        baseTypeBuilder.append(superclassName);
      }
      superclass = superclass.getSuperClassGenerationObject();
      if (superclass != null) {
        baseTypeBuilder.append(SEPERATION_MARKER);  
      }
    }
    return baseTypeBuilder.toString();
  }


  @Override
  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    isExtending = ((XMOMDomOrExceptionDatabaseEntry)data).isExtending;
    extendedBy = ((XMOMDomOrExceptionDatabaseEntry)data).extendedBy;
    possesses = ((XMOMDomOrExceptionDatabaseEntry)data).possesses;
    possessedBy = ((XMOMDomOrExceptionDatabaseEntry)data).possessedBy;
    neededBy = ((XMOMDomOrExceptionDatabaseEntry)data).neededBy;
    producedBy = ((XMOMDomOrExceptionDatabaseEntry)data).producedBy;
    instancesUsedBy = ((XMOMDomOrExceptionDatabaseEntry)data).instancesUsedBy;
  }

  @Override
  public abstract XMOMDomOrExceptionDatabaseEntry clone() throws CloneNotSupportedException;


  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    switch (column) {
      case EXTENDS :
        return isExtending;
      case EXTENDEDBY :
        return extendedBy;
      case POSSESSES :
        return possesses;
      case POSSESSEDBY :
        return possessedBy;
      case NEEDEDBY :
        return neededBy;
      case PRODUCEDBY :
        return producedBy;
      case INSTANCESUSEDBY :
        return instancesUsedBy;
      default :
        return super.getValueByColumn(column);
    }
  }
}
