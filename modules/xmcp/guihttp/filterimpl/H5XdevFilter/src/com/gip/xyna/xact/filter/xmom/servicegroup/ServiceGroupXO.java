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

package com.gip.xyna.xact.filter.xmom.servicegroup;

import java.util.Collections;
import java.util.List;

import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.xmom.datatypes.json.DatatypeMethodXo;
import com.gip.xyna.xact.filter.xmom.datatypes.json.Utils;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xmcp.SharedLib;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import xmcp.processmodeller.datatypes.ServiceGroup;
import xmcp.processmodeller.datatypes.datatypemodeller.MemberMethodArea;
import xmcp.processmodeller.datatypes.datatypemodeller.Method;
import xmcp.processmodeller.datatypes.servicegroupmodeller.JavaSharedLibrariesArea;
import xmcp.processmodeller.datatypes.servicegroupmodeller.JavaSharedLibrary;
import xmcp.processmodeller.datatypes.servicegroupmodeller.LibrariesArea;
import xmcp.processmodeller.datatypes.servicegroupmodeller.Library;
import xmcp.processmodeller.datatypes.servicegroupmodeller.ServiceGroupTypeLabelArea;


public class ServiceGroupXO implements HasXoRepresentation {
  
  private final GenerationBaseObject gbo;
  private final DOM dom; 
  private List<DatatypeMethodXo> methods;
  private boolean readonly = false;
  
  public ServiceGroupXO(GenerationBaseObject gbo) {
    this.gbo = gbo;
    this.dom = gbo.getDOM();
    this.methods = Utils.createDtMethods(dom, gbo);
  }

  @Override
  public GeneralXynaObject getXoRepresentation() {
    ServiceGroup serviceGroup = new ServiceGroup();
    serviceGroup.setReadonly(readonly);
    try {
      serviceGroup.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(dom.getRevision()));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // nothing
    }
    serviceGroup.setFqn(gbo.getOriginalFqName());
    serviceGroup.setDeletable(false);
    serviceGroup.setLabel(dom.getLabel());
    serviceGroup.setId("sg");

    serviceGroup.addToAreas(createTypeLabelArea());
    serviceGroup.addToAreas(createLibrariesArea());
    serviceGroup.addToAreas(createJavaSharedLibrariesArea());
    serviceGroup.addToAreas(createMemberMethodArea());

    return serviceGroup;
  }
  
  protected MemberMethodArea createMemberMethodArea() {
    MemberMethodArea area = new MemberMethodArea();
    area.setItemTypes(Collections.emptyList());
    area.setId(ObjectId.createId(ObjectType.memberMethodsArea, null));
    area.setName(Tags.SERVICE_GROUP_MEMBER_METHODS_AREA_ID);
    area.setReadonly(false);
    methods.stream().filter(DatatypeMethodXo::isStatic)
    .forEach(m -> {
      Method method = (Method) m.getXoRepresentation();
      method.setReadonly(false);
      method.setDeletable(true);
      area.addToItems(method);
    });
    return area;
  }
  
  private JavaSharedLibrariesArea createJavaSharedLibrariesArea() {
    JavaSharedLibrariesArea area = new JavaSharedLibrariesArea();
    area.setReadonly(false);
    area.setName(Tags.SERVICE_GROUP_JAVA_SHARED_LIBRARIES_AREA_ID);
    area.setId(Tags.SERVICE_GROUP_JAVA_SHARED_LIBRARIES_AREA_ID);
    area.setItemTypes(Collections.emptyList());

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
  
  private static LibrariesArea createEmptyJavaLibrariesArea() {
    LibrariesArea area = new LibrariesArea();
    area.setReadonly(false);
    area.setName(Tags.SERVICE_GROUP_JAVA_LIBRARIES_AREA_ID);
    area.setId(Tags.SERVICE_GROUP_JAVA_LIBRARIES_AREA_ID);
    area.setItemTypes(Collections.emptyList());
    return area;
  }

  private LibrariesArea createLibrariesArea() {
    LibrariesArea area = createEmptyJavaLibrariesArea();
    int i = 0;
    for (String lib : dom.getAdditionalLibraries()) {
      Library javaLib = new Library(ObjectId.createServiceGroupLibId(i), false, lib);
      area.addToItems(javaLib);
      area.addToJavaLibraries(javaLib);
      i++;
    }
    i = 0;
    for (String lib : dom.getPythonLibraries()) {
      Library pythonLib = new Library(ObjectId.createServiceGroupLibId(i), false, lib);
      area.addToItems(pythonLib);
      area.addToPythonLibraries(pythonLib);
      i++;
    }
    return area;
  }
  
  private ServiceGroupTypeLabelArea createTypeLabelArea() {
    ServiceGroupTypeLabelArea area = new ServiceGroupTypeLabelArea();
    area.setFqn(dom.getOriginalFqName());
    area.setReadonly(gbo.getSaveState()); //if it is saved, it is read only
    area.setName(Tags.SERVICE_GROUP_TYPE_LABEL_AREA_NAME);
    area.setText(dom.getLabel());
    area.setId(Tags.SERVICE_GROUP_TYPE_LABEL_AREA_ID);
    return area;
  }

  
  public boolean isReadonly() {
    return readonly;
  }

  
  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

}
