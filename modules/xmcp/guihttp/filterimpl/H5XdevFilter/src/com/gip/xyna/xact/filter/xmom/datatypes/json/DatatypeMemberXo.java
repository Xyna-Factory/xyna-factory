/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.xmom.datatypes.json;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.xmom.PluginPaths;
import com.gip.xyna.xact.filter.xmom.datatypes.json.Utils.ExtendedContextBuilder;
import com.gip.xyna.xact.filter.xmom.workflows.enums.GuiLabels;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation.ColumnInfoRecursionMode;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarDefinitionSite;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceInformation;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;

import xmcp.processmodeller.datatypes.Area;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.TextArea;
import xmcp.processmodeller.datatypes.datatypemodeller.DataMemberVariable;
import xmcp.processmodeller.datatypes.datatypemodeller.StorablePropertyArea;

public class DatatypeMemberXo implements HasXoRepresentation {
  
  private AVariable var;
  private String id;
  private PersistenceInformation pi;
  private XMOMStorableStructureInformation si;
  private boolean isStorable;
  private ObjectId memberId;
  private FQNameJson fqName;
  private ObjectIdentifierJson inheritedFrom;

  protected final GuiHttpPluginManagement pluginMgmt;
  protected final ExtendedContextBuilder contextBuilder;

  private static final Logger logger = CentralFactoryLogging.getLogger(DatatypeMemberXo.class);

  public DatatypeMemberXo(AVariable var, String id, PersistenceInformation pi, XMOMStorableStructureInformation si, boolean isStorable, ExtendedContextBuilder contextBuilder) {
    this.var = var;
    this.id = id;
    this.pi = pi;
    this.si = si;
    this.isStorable = isStorable;
    pluginMgmt = GuiHttpPluginManagement.getInstance();
    this.contextBuilder = contextBuilder;

    try {
      this.memberId = ObjectId.parse(id);
    } catch (UnknownObjectIdException e) {
      throw new RuntimeException("Could generate id for member function", e);
    }

    if (!var.isJavaBaseType()) {
      this.fqName = new FQNameJson( var.getOriginalPath(), var.getOriginalName() );
    }
  }
  
  public ObjectIdentifierJson getInheritedFrom() {
    return inheritedFrom;
  }
  
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fqName == null) ? 0 : fqName.hashCode());
    result = prime * result + ((var == null) ? 0 : var.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DatatypeMemberXo other = (DatatypeMemberXo) obj;
    if (fqName == null) {
      if (other.fqName != null)
        return false;
    } else if (!fqName.equals(other.fqName))
      return false;
    if (var == null) {
      if (other.var != null)
        return false;
    }
    if( var.isList() != other.var.isList() ) {
      return false;
    }
    if (var.getVarName() == null) {
      if (other.var.getVarName() != null)
        return false;
    } else if (!var.getVarName().equals(other.var.getVarName())) {
      return false;
    }
    if( var.isJavaBaseType() && var.getJavaTypeEnum() != other.var.getJavaTypeEnum() ) {
      return false;
    }
    
    return true;
  }

  public void setInheritedFrom(ObjectIdentifierJson inheritedFrom) {
    this.inheritedFrom = inheritedFrom;
  }
  
  public AVariable getVariable() {
    return var;
  }
  
  public String getVarName() {
    return var.getVarName();
  }

  public boolean isList() {
    return var.isList();
  }
  
  public FQNameJson getFQName() {
    return fqName;
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    DataMemberVariable v = new DataMemberVariable();
    try {
      DomOrExceptionGenerationBase doe = var.getDomOrExceptionObject();
      if (doe != null) {
        v.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(doe.getRevision()));
      } else {
        v.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(var.getRevision()));
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // nothing
    }
    v.setLabel(var.getLabel());
    v.setName(var.getVarName());
    v.setId(id);
    v.setIsList(var.isList());

    boolean isUniqueIdentifier = false;
    if(var.getPersistenceTypes() != null && var.getPersistenceTypes().contains(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP)) {
      v.setStorableRole(GuiLabels.DT_LABEL_HISTORIZATION_TIMESTAMP);
    } else if (var.getPersistenceTypes() != null && var.getPersistenceTypes().contains(PersistenceTypeInformation.UNIQUE_IDENTIFIER)) {
      v.setStorableRole(GuiLabels.DT_LABEL_UNIQUE_IDENTIFIER);
      isUniqueIdentifier = true;
    } else if (var.getPersistenceTypes() != null && var.getPersistenceTypes().contains(PersistenceTypeInformation.CURRENTVERSION_FLAG)) {
      v.setStorableRole(GuiLabels.DT_LABEL_CURRENTVERSION_FLAG);
    }

    if( var.isJavaBaseType() ) {
      v.setPrimitiveType(var.getJavaTypeEnum().getJavaTypeName());
    } else {
      if(fqName != null) {
        v.setFqn(fqName.toString());
      }
    }

    v.addToAreas(getDocumentationArea());
    if (isStorable && pi != null && !isUniqueIdentifier) {
      v.addToAreas(getStorablePropertyArea());
    }
    
    v.addToAreas(DomOrExceptionXo.createMetaTagArea(var.getUnknownMetaTags()));

    return v;
  }

  private TextArea getDocumentationArea() {
    TextArea area = new TextArea();
    area.setName(Tags.DATA_TYPE_DOCUMENTATION_AREA);
    area.setId(ObjectId.createMemberDocumentationAreaId(String.valueOf(ObjectId.parseMemberVarNumber(memberId))));
    area.setText(var.getDocumentation());
    area.setReadonly(inheritedFrom != null);
    String location = XMOMType.DATATYPE.equals(contextBuilder.getType()) ? PluginPaths.location_datatype_member_documenation : PluginPaths.location_exception_member_documentation;
    area.unversionedSetPlugin(pluginMgmt.createPlugin(contextBuilder.instantiateContext(location, area.getId())));

    return area;
  }

  private StorablePropertyArea getStorablePropertyArea() {
    String odsName = null;
    StorableColumnInformation complexSubMember = null;
    Set<StorableColumnInformation> flatSubMembers = null;
    boolean isPrimitive = true;

    if (si != null) {
      StorableColumnInformation sci = si.getColumnInfo(var.getVarName(), true);
      if (sci != null) {
        if (sci.isStorableVariable()) {
          StorableStructureInformation ssi = sci.getStorableVariableInformation();
          odsName = ssi.getTableName();
        } else {
          odsName = sci.getColumnName();
        }
      }

      for (StorableColumnInformation curComplexMember : si.getAllComplexColumns(true)) {
        if (curComplexMember.getVariableName() != null && curComplexMember.getVariableName().equals(var.getVarName())) {
          complexSubMember = curComplexMember;
          break;
        }
      }

      if (complexSubMember == null) {
        flatSubMembers = si.getColumnInfosByPathPrefix(var.getVarName() + ".");
      }

      isPrimitive = complexSubMember == null && flatSubMembers.isEmpty();
    }

    StorablePropertyArea area = createStorablePropertyArea(var.getVarName(), odsName, isPrimitive);

    if (si != null) {
      // add complex members recursively
      if (complexSubMember != null) {
        createStructureRecursively(area, complexSubMember, true);
        return sortStorablePropertyArea(area);
      }

      // add flat members recursively
      if (flatSubMembers != null && !flatSubMembers.isEmpty()) {
        unwrapFlatStrucure(area, flatSubMembers, var.getVarName());
      }
    }

    return sortStorablePropertyArea(area);
  }

  private StorablePropertyArea createStorablePropertyArea(String varName, String odsName, boolean isPrimitive) {
    StorablePropertyArea area = new StorablePropertyArea();
    area.setItemTypes(Collections.emptyList());
    area.setReadonly(true);
    area.setName(Tags.DATA_TYPE_STORABLE_PROPERTIES_AREA);
    area.setFieldName(odsName);
    area.setIsReference(pi.getReferences().contains(varName));

    if (isPrimitive && pi.getReferences() != null) {
      area.setIsIndex(pi.getIndices().contains(varName));
      area.setIsUnique(pi.getConstraints().contains(varName));
    }

    return area;
  }

  private StorablePropertyArea sortStorablePropertyArea(StorablePropertyArea area) {
    if (area.getItems() == null) {
      return area;
    }

    Collections.sort(area.getItems(), new StorablePropertyAreaComparator());

    for (Item item : area.getItems()) {
      DataMemberVariable subVariable = (DataMemberVariable)item;
      for (Area subArea : subVariable.getAreas()) {
        if (subArea instanceof StorablePropertyArea) {
          sortStorablePropertyArea((StorablePropertyArea)subArea);
        }
      }
    }

    return area;
  }

  private static class StorablePropertyAreaComparator implements Comparator<Item> {
    public int compare(Item o1, Item o2)
    {
      if (!(o1 instanceof DataMemberVariable) || !(o2 instanceof DataMemberVariable)) {
        return 0;
      }
  
      DataMemberVariable var1 = (DataMemberVariable)o1;
      DataMemberVariable var2 = (DataMemberVariable)o2;
  
      if (var1.getName() != null) {
        return var1.getName().compareTo(var2.getName());
      } else {
        return -1;
      }
    }
  }

  private void createStructureRecursively(StorablePropertyArea area, StorableColumnInformation sci, boolean isTopLevel) {
    if (!sci.isStorableVariable()) {
      return;
    }

    StorableStructureInformation ssi = sci.getStorableVariableInformation();
    if (ssi instanceof XMOMStorableStructureInformation) {
      return; // skip ref tables
    }

    StorablePropertyArea curArea = area;
    if (!isTopLevel) { // area for top level is already created in getStorablePropertyArea()
      String name = (sci.getPath() != null && !sci.getPath().isEmpty()) ? sci.getPath() : sci.getVariableName();
      while (name != null && name.contains(".")) {
        String subName = name.substring(0, name.indexOf('.'));
        curArea = getOrAddStorablePropertySubEntry(curArea, subName, subName, null, false);
        name = name.substring(name.indexOf('.') + 1);
      }

      curArea = getOrAddStorablePropertySubEntry(curArea, name, name, ssi.getTableName(), false);
    }

    addPrimitives(curArea, ssi);

    for (StorableColumnInformation column : ssi.getAllComplexColumns(true)) {
      createStructureRecursively(curArea, column, false);
    }
  }

  private void unwrapFlatStrucure(StorablePropertyArea area, Set<StorableColumnInformation> flatSubMembers, String pathPrefix) {
    for (StorableColumnInformation flatSubMember : flatSubMembers) {
      try {
        String subPath = flatSubMember.getPath().substring(pathPrefix.length() + 1);
        String subVarName = subPath.substring(0, subPath.indexOf('.') >= 0 ? subPath.indexOf('.') : subPath.length());

        boolean isPrimitive;
        String odsName;
        StorableStructureInformation ssi = null;
        if (flatSubMember.isStorableVariable()) {
          isPrimitive = false;
          ssi = flatSubMember.getStorableVariableInformation();
          odsName = subPath.equals(subVarName) ? ssi.getTableName() : null;
        } else {
          isPrimitive = subPath.equals(subVarName);
          odsName = isPrimitive ? flatSubMember.getColumnName() : null;
        }

        StorablePropertyArea subArea = getOrAddStorablePropertySubEntry(area, subVarName, subVarName, odsName, isPrimitive);

        if (!isPrimitive) {
          String subPathPrefix = pathPrefix + "." + subVarName;
          unwrapFlatStrucure(subArea, si.getColumnInfosByPathPrefix(subPathPrefix + "."), subPathPrefix);
        }

        if (subPath.equals(subVarName) && flatSubMember.isStorableVariable() && !(ssi instanceof XMOMStorableStructureInformation)) { // skip ref tables
          addPrimitives(subArea, ssi);
          for (StorableColumnInformation column : ssi.getAllComplexColumns(true)) {
            createStructureRecursively(subArea, column, false);
          }
        }
      } catch (Exception e) {
        logger.warn("Could not determine storable info for flat member " + pathPrefix, e);
      }
    }
  }

  private void addPrimitives(StorablePropertyArea area, StorableStructureInformation entry) {
    for (StorableColumnInformation sci : entry.getColumnInfo(ColumnInfoRecursionMode.ONLY_LOCAL)) {
      StorablePropertyArea curArea = area;
      if (!VarType.DEFAULT.equals(sci.getType())) {
        continue;
      }

      String name = (sci.getPath() != null && !sci.getPath().isEmpty()) ? sci.getPath() : sci.getVariableName();
      while (name != null && name.contains(".")) {
        String subName = name.substring(0, name.indexOf('.'));
        curArea = getOrAddStorablePropertySubEntry(curArea, subName, subName, null, false);
        name = name.substring(name.indexOf('.') + 1);
      }

      if (sci.getDefinitionSite() != VarDefinitionSite.DATATYPE && !sci.isStorableVariable()) {
        getOrAddStorablePropertySubEntry(curArea, name, name, sci.getColumnName(), true);
      }
    }
  }

  private StorablePropertyArea addStorablePropertySubEntry(StorablePropertyArea area, String varName, String varLabel, String odsName, boolean isPrimitive) {
    DataMemberVariable subVariable = new DataMemberVariable();
    subVariable.setName(varName);
    subVariable.setLabel(varLabel);
    subVariable.setReadonly(true);
    subVariable.setDeletable(false);
    area.addToItems(subVariable);

    StorablePropertyArea subArea = createStorablePropertyArea(varName, odsName, isPrimitive);
    subVariable.addToAreas(subArea);

    return subArea;
  }

  private StorablePropertyArea getOrAddStorablePropertySubEntry(StorablePropertyArea area, String varName, String varLabel, String odsName, boolean isPrimitive) {
    if (area.getItems() == null) {
      return addStorablePropertySubEntry(area, varName, varLabel, odsName, isPrimitive);
    }

    for (Item item : area.getItems()) {
      if (item instanceof DataMemberVariable) {
        DataMemberVariable subVariable = (DataMemberVariable)item;
        if (Objects.equals(subVariable.getName(), varName)) {
          for (Area subArea : subVariable.getAreas()) {
            if (subArea instanceof StorablePropertyArea) {
              return (StorablePropertyArea)subArea;
            }
          }
        }
      }
    }

    return addStorablePropertySubEntry(area, varName, varLabel, odsName, isPrimitive);
  }

}
