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
package xmcp.gitintegration.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

import xmcp.gitintegration.Reference;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.impl.references.ReferenceType;
import xmcp.gitintegration.storage.ReferenceStorable;
import xmcp.gitintegration.storage.ReferenceStorage;

/**
 * 
 * should not be a Processor, because ReferenceStorables
 * are not supposed to be shared between factories.
 * Instead, relevant WorkspaceContentItems contains reference
 * tags. Reference tags contain a subset of the data stored
 * in ReferenceStorable objects.
 * 
 * ReferenceStorable objects are specific to a factory (contain
 * workspace revision), while the date provided by reference
 * tags is factory-independent.
 */
public class ReferenceManagementImpl {

  
  public Reference convert(ReferenceStorable storable) {
    Reference.Builder builder = new Reference.Builder();
    builder.path(storable.getPath()).type(storable.getReftype());
    return builder.instance();
  }
  
  public List<Reference> convertList(List<? extends ReferenceStorable> references) {
    return references.stream().map(x -> convert(x)).collect(Collectors.toList());
  }
  
  public void create(Reference tag, long revision, String objectName, String objectType) {
    //when merging workspace.xml
    create(tag.getPath(), objectType, tag.getType().toString(), revision, objectName);
  }
  
  public void create(String path, String objectType, String refType, Long wsRev, String objectName) {
    //for command line/GUI
    ReferenceStorage storage = new ReferenceStorage();
    ReferenceStorable storable = new ReferenceStorable();
    storable.setObjectName(objectName);
    storable.setWorkspace(wsRev);
    storable.setPath(path);
    storable.setReftype(refType);
    storable.setObjecttype(objectType);

    List<String> issues = validateReference(storable);
    if (!issues.isEmpty()) {
      String issueString = String.join("\n", issues);
      throw new RuntimeException("There are " + issues.size() + " problems with the reference: \n " + issueString);
    }

    storage.persist(storable);
  }
  
  private List<String> validateReference(ReferenceStorable storable) {
    List<String> result = new ArrayList<String>();
    RevisionManagement refMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      ReferenceType.valueOf(storable.getReftype());
    } catch (Exception e) {
      result.add("Invalid ReferenceType. Available ReferenceTypes: " + Arrays.asList(ReferenceType.values()));
    }

    try {
      ReferenceObjectType.valueOf(storable.getObjecttype());
    } catch (Exception e) {
      result.add("Invalid ObjectType. Available ObjectTypes: " + Arrays.asList(ReferenceObjectType.values()));
    }

    try {
      refMgmt.getWorkspace(storable.getWorkspace());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      result.add("Workspace not found.");
    }

    // storable.getPath(); does not get validated
    // storable.getObjectName(); does not get validated

    return result;
  }
  
  public void delete(String path, Long revision, String objectName) {
    ReferenceStorage storage = new ReferenceStorage();
    storage.deleteReference(path, revision, objectName);
  }
  
 
}
