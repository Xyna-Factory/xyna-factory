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
package com.gip.xyna.xact.filter.xmom.datatypes.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.SharedLib;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;

import xmcp.processmodeller.datatypes.DataType;
import xmcp.processmodeller.datatypes.DataTypeTypeLabelArea;
import xmcp.processmodeller.datatypes.TextArea;
import xmcp.processmodeller.datatypes.datatypemodeller.DynamicMethod;
import xmcp.processmodeller.datatypes.datatypemodeller.GlobalStorablePropertyArea;
import xmcp.processmodeller.datatypes.datatypemodeller.MemberMethodArea;
import xmcp.processmodeller.datatypes.datatypemodeller.Method;
import xmcp.processmodeller.datatypes.servicegroupmodeller.JavaLibrariesArea;
import xmcp.processmodeller.datatypes.servicegroupmodeller.JavaLibrary;
import xmcp.processmodeller.datatypes.servicegroupmodeller.JavaSharedLibrariesArea;
import xmcp.processmodeller.datatypes.servicegroupmodeller.JavaSharedLibrary;

public class DatatypeXo extends DomOrExceptionXo implements HasXoRepresentation {

  private List<DatatypeMethodXo> methods;
  private final DOM dom;
  private boolean readonly = false;

  private static final Logger logger = CentralFactoryLogging.getLogger(DatatypeXo.class);


  public DatatypeXo(GenerationBaseObject gbo) {
    super(gbo);
    this.dom = gbo.getDOM();
    this.methods = Utils.createDtMethods(dom, gbo);
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    DataType dataType = new DataType();
    dataType.setReadonly(readonly);
    try {
      dataType.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(dom.getRevision()));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // nothing
    }
    dataType.setFqn(dom.getOriginalFqName());
    dataType.setDeletable(false);
    dataType.setLabel(dom.getLabel());
    dataType.setId("dt");
    dataType.setIsAbstract(dom.isAbstract());
    dataType.addToAreas(createDataTypeTypeLabelArea());

//    if (dom.isInheritedFromStorable()) {
//      dataType.addToAreas(createStorablePropertyArea());
//    }
    dataType.addToAreas(createGlobalStorablePropertyArea());

    TextArea documentationArea = createDocumentationArea();
    documentationArea.setText(dom.getDocumentation());
    dataType.addToAreas(documentationArea);
    
    dataType.addToAreas(createJavaLibrariesArea());
    dataType.addToAreas(createJavaSharedLibrariesArea());

    dataType.addToAreas(createInheritedVariablesArea());
    dataType.addToAreas(createMemberVariableArea());
    dataType.addToAreas(createInheritedMethodArea());
    dataType.addToAreas(createOverrideMethodArea());
    dataType.addToAreas(createMemberMethodArea());
    return dataType;
  }
  
  private static JavaSharedLibrariesArea createEmptyJavaSharedLibrariesArea() {
    JavaSharedLibrariesArea area = new JavaSharedLibrariesArea();
    area.setReadonly(false);
    area.setName(Tags.SERVICE_GROUP_JAVA_SHARED_LIBRARIES_AREA_ID);
    area.setId(Tags.SERVICE_GROUP_JAVA_SHARED_LIBRARIES_AREA_ID);
    area.setItemTypes(Collections.emptyList());
    return area;
  }
  
  private JavaSharedLibrariesArea createJavaSharedLibrariesArea() {
    JavaSharedLibrariesArea area = createEmptyJavaSharedLibrariesArea();
    String[] usedSharedLibNames = dom.getSharedLibs();
    List<SharedLib> availableSharedLibs = com.gip.xyna.xact.filter.util.Utils.getSharedLibs(dom.getRevision());
    for (int libIdx = 0; libIdx < availableSharedLibs.size(); libIdx++) {
      SharedLib lib = availableSharedLibs.get(libIdx);
      boolean used = false;
      for (String usedLibName : usedSharedLibNames) {
        if (lib.getName().equals(usedLibName)) {
          used = true;
          break;
        }
      }

      area.addToItems(new JavaSharedLibrary(ObjectId.createServiceGroupSharedLibId(libIdx), false, lib.getName(), used));
    }

    return area;
  }
  
  
  private static JavaLibrariesArea createEmptyJavaLibrariesArea() {
    JavaLibrariesArea area = new JavaLibrariesArea();
    area.setReadonly(false);
    area.setName(Tags.SERVICE_GROUP_JAVA_LIBRARIES_AREA_ID);
    area.setId(Tags.SERVICE_GROUP_JAVA_LIBRARIES_AREA_ID);
    area.setItemTypes(Collections.emptyList());
    return area;
  }
  

  private JavaLibrariesArea createJavaLibrariesArea() {
    JavaLibrariesArea area = createEmptyJavaLibrariesArea();
    Set<String> libs = dom.getAdditionalLibraries();
    int i = 0;
    for (String lib : libs) {
      area.addToItems(new JavaLibrary(ObjectId.createServiceGroupLibId(i), false, lib));
      i++;
    }
    return area;
  }
  
  
  private static MemberMethodArea createEmptyMemberMethodArea() {
    MemberMethodArea area = new MemberMethodArea();
    area.setItemTypes(Collections.emptyList());
    area.setId(ObjectId.createId(ObjectType.memberMethodsArea, null));
    area.setName(Tags.DATA_TYPE_MEMBER_METHODS_AREA);
    area.setReadonly(false);
    return area;
  }
  
  private MemberMethodArea createMemberMethodArea() {
    MemberMethodArea area = createEmptyMemberMethodArea();
    methods.stream().filter(vm -> vm.isMemberMethod() && !vm.isStatic())
    .forEach(m -> {
      DynamicMethod method = (DynamicMethod) m.getXoRepresentation();
      method.setReadonly(false);
      method.setDeletable(!m.isInherited());
      area.addToItems(method);
    });
    return area;
  }
  
  private static MemberMethodArea createEmptyOverrideMethodArea() {
    MemberMethodArea area = new MemberMethodArea();
    area.setItemTypes(new ArrayList<String>(Arrays.asList(MetaXmomContainers.DT_DYNAMIC_METHOD)));
    area.setId(ObjectId.createId(ObjectType.overriddenMethodsArea, null));
    area.setName(Tags.DATA_TYPE_OVERRIDDEN_METHODS_AREA);
    area.setReadonly(false);
    return area;
  }
  
  private MemberMethodArea createOverrideMethodArea() {
    MemberMethodArea area = createEmptyOverrideMethodArea();
    methods.stream().filter(vm -> { 
      return vm.overrides() && !vm.isStatic();
    })
    .forEach(m -> {
      DynamicMethod method = (DynamicMethod) m.getXoRepresentation();
      method.setDeletable(true);
      area.addToItems(method);
    });
    return area;
  }
  
  
  private static MemberMethodArea createEmptyInheritedMethodArea() {
    MemberMethodArea area = new MemberMethodArea();
    area.setItemTypes(Collections.emptyList());
    area.setReadonly(true);
    area.setName(Tags.DATA_TYPE_INHERITED_METHODS_AREA);  
    return area; 
  }
  
  private MemberMethodArea createInheritedMethodArea() {
    MemberMethodArea area = createEmptyInheritedMethodArea();
    methods.stream().filter(vm -> { 
      return vm.isInherited() && !vm.isStatic();
    })
      .forEach(m -> {
        Method method = (Method) m.getXoRepresentation();
        method.setReadonly(true);
        area.addToItems(method);
      });
    return area;
  }

  
//  private StorablePropertyArea createStorablePropertyArea() {
//    PersistenceInformation pi = dom.getPersistenceInformation();
//    StorablePropertyArea area = new StorablePropertyArea();
//    area.setItemTypes(Collections.emptyList());
//    area.setReadonly(true);
//    area.setName(Tags.DATA_TYPE_STORABLE_PROPERTIES_AREA);
//    
//    boolean hasHistorizationTimestamp = false;
//    boolean hasCurrentVersionFlag = false;
//    
//    for(DatatypeMemberXo v : getVariables()) {
//      if(v.getVariable().getPersistenceTypes() != null) {
//        if(v.getVariable().getPersistenceTypes().contains(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP)) {
//          hasHistorizationTimestamp = true;
//        }
//        if(v.getVariable().getPersistenceTypes().contains(PersistenceTypeInformation.CURRENTVERSION_FLAG)) {
//          hasCurrentVersionFlag = true;
//        }
//      }
//    }
//    area.setUseHistorization(hasHistorizationTimestamp && hasCurrentVersionFlag);
//    
//    int i = 0;
//    for(DatatypeMemberXo v : getVariables() ){
//      StorableProperty property = new StorableProperty();
//      property.setId(ObjectId.createId(ObjectType.storableProperty, String.valueOf(i)));
//      property.setVariableName(v.getVarName());
//      if(pi != null) {
//        if(pi.getReferences() != null) {
//          property.setReference(pi.getReferences().contains(v.getVarName()));
//        }
//        if(pi.getIndices() != null) {
//          property.setIndex(pi.getIndices().contains(v.getVarName()));
//        }
//        if(pi.getConstraints() != null) {
//          property.setUnique(pi.getConstraints().contains(v.getVarName()));
//        }
//        if(pi.getCustomField0() != null && pi.getCustomField0().contains(v.getVarName())) {
//          property.setCustomField(Tags.DATA_TYPE_TYPE_CUSTOM_FIELD0);
//        }
//        if(pi.getCustomField1() != null && pi.getCustomField1().contains(v.getVarName())) {
//          property.setCustomField(Tags.DATA_TYPE_TYPE_CUSTOM_FIELD1);
//        }
//        if(pi.getCustomField2() != null && pi.getCustomField2().contains(v.getVarName())) {
//          property.setCustomField(Tags.DATA_TYPE_TYPE_CUSTOM_FIELD2);
//        }
//        if(pi.getCustomField3() != null && pi.getCustomField3().contains(v.getVarName())) {
//          property.setCustomField(Tags.DATA_TYPE_TYPE_CUSTOM_FIELD3);
//        }
//      }
//      area.addToItems(property);      
//      i++;
//    }
//    
//    return area;
//  }


  private GlobalStorablePropertyArea createGlobalStorablePropertyArea() {
    GlobalStorablePropertyArea area = new GlobalStorablePropertyArea();
    area.setItemTypes(Collections.emptyList());
    area.setReadonly(true);
    area.setName(Tags.DATA_TYPE_GLOBAL_STORABLE_PROPERTIES_AREA);

    area.setIsStorable(dom.isInheritedFromStorable());
    if (dom.isInheritedFromStorable()) {
      boolean hasHistorizationTimestamp = false;
      boolean hasCurrentVersionFlag = false;
  
      for (DatatypeMemberXo v : getVariables()) {
        if (v.getVariable().getPersistenceTypes() != null) {
          if (v.getVariable().getPersistenceTypes().contains(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP)) {
            hasHistorizationTimestamp = true;
          }

          if (v.getVariable().getPersistenceTypes().contains(PersistenceTypeInformation.CURRENTVERSION_FLAG)) {
            hasCurrentVersionFlag = true;
          }
        }
      }
      area.setUseHistorization(hasHistorizationTimestamp && hasCurrentVersionFlag);

      try {
        XMOMStorableStructureInformation structuralInformation = XMOMStorableStructureCache.getInstance(dom.getRevision()).getStructuralInformation(dom.getFqClassName());
        area.setODSName(structuralInformation.getTableName());
      } catch (Exception e) {
        // storable is not deployed
      }
    }

    return area;
  }


  public static GeneralXynaObject getAnyTypeXoRepresentation(RuntimeContextJson rtc) {
    DataType dataType = new DataType();

    dataType.setRtc(convertRTC(rtc));
    dataType.setFqn(GenerationBase.ANYTYPE_REFERENCE_PATH + "." + GenerationBase.ANYTYPE_REFERENCE_NAME);
    dataType.setDeletable(false);
    dataType.setLabel("AnyType");
    dataType.setId("dt");
    dataType.setIsAbstract(false);

    DataTypeTypeLabelArea area = new DataTypeTypeLabelArea();
    area.setFqn(GenerationBase.ANYTYPE_REFERENCE_PATH + "." + GenerationBase.ANYTYPE_REFERENCE_NAME);
    area.setIsAbstract(false);
    area.setName(Tags.DATA_TYPE_TYPE_INFO_AREA);
    area.setText("AnyType");
    area.setId(ObjectId.createId(ObjectType.typeInfoArea, null));
    dataType.addToAreas(area);

    TextArea documentationArea = createDocumentationArea();
    documentationArea.setText("Conceptual parent of all modelled datatypes and exceptions.");
    dataType.addToAreas(documentationArea);

    dataType.addToAreas(createEmptyJavaLibrariesArea());
    dataType.addToAreas(createEmptyJavaSharedLibrariesArea());
    dataType.addToAreas(createEmptyInheritedVariablesArea());
    dataType.addToAreas(createEmptyMemberVariableArea());
    dataType.addToAreas(createEmptyInheritedMethodArea());
    dataType.addToAreas(createEmptyOverrideMethodArea());
    dataType.addToAreas(createEmptyMemberMethodArea());

    return dataType;
  }
  

  private static xmcp.processmodeller.datatypes.RuntimeContext convertRTC(RuntimeContextJson rtc) {
    RuntimeContext r = rtc.toRuntimeContext();

    if (r instanceof Application) {
      Application app = (Application) r;
      return new xmcp.processmodeller.datatypes.Application(app.getName(), app.getVersionName());
    } else if (r instanceof Workspace) {
      return new xmcp.processmodeller.datatypes.Workspace(r.getName());
    } else {
      throw new RuntimeException("unknown runtimeContext: " + r);
    }
  }

  public boolean isReadonly() {
    return readonly;
  }

  
  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }
  
}
