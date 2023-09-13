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
package xmcp.gitintegration.impl.processing;



import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceMethods;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.impl.references.ReferenceObjectTypeMethods;
import xmcp.gitintegration.impl.references.ReferenceType;
import xmcp.gitintegration.impl.references.methods.LibFolderMethods;
import xmcp.gitintegration.impl.references.methods.objecttypes.DatatypeReferenceMethods;
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
public class ReferenceSupport {

  public File findJar(List<InternalReference> references, String jarName, Long revision) {
    for (InternalReference reference : references) {
      ReferenceType referenceType = ReferenceType.valueOf(reference.getType());
      ReferenceMethods methods = dispatch(referenceType);
      Optional<File> result = methods.findJar(reference, jarName, revision);
      if (result.isPresent()) {
        return result.get();
      }
    }
    return null;
  }


  private static final HashMap<ReferenceType, ReferenceMethods> implementations = setReferenceMethods();
  private static final HashMap<ReferenceObjectType, ReferenceObjectTypeMethods> objectTypeImplementations = setRefTypeMethods();


  private static HashMap<ReferenceType, ReferenceMethods> setReferenceMethods() {
    HashMap<ReferenceType, ReferenceMethods> result = new HashMap<ReferenceType, ReferenceMethods>();

    //register implementations here
    result.put(ReferenceType.lib_folder, new LibFolderMethods());

    return result;
  }


  private static HashMap<ReferenceObjectType, ReferenceObjectTypeMethods> setRefTypeMethods() {
    HashMap<ReferenceObjectType, ReferenceObjectTypeMethods> result = new HashMap<>();

    //register implementations here
    result.put(ReferenceObjectType.DATATYPE, new DatatypeReferenceMethods());

    return result;
  }


  private ReferenceMethods dispatch(ReferenceType type) {
    return implementations.get(type);
  }


  public void triggerReferences(String objectName, Long revision, String repoPath) {
    ReferenceStorage storage = new ReferenceStorage();
    List<ReferenceStorable> references = storage.getAllReferencesForObject(revision, objectName);
    if (references.isEmpty()) {
      return;
    }
    List<InternalReference> refs = convertList(references, repoPath);
    ReferenceObjectType objectType = ReferenceObjectType.valueOf(references.get(0).getObjecttype());
    objectTypeImplementations.get(objectType).trigger(refs, objectName, revision);
  }


  public void triggerReferences(List<InternalReference> references, Long revision) {
    ReferenceStorage storage = new ReferenceStorage();
    List<ReferenceStorable> allrefs = storage.getAllReferencesForWorkspace(revision);
    Map<String, ObjectReferenceInformation> grouped = new HashMap<>();
    for (InternalReference reference : references) {
      Optional<ReferenceStorable> opt = allrefs.stream().filter(x -> matchRevisionAndPath(x, reference.getPath(), revision)).findAny();
      if (opt.isEmpty()) {
        continue;
      }
      ReferenceStorable storable = opt.get();
      grouped.putIfAbsent(storable.getObjectName(), new ObjectReferenceInformation());
      ObjectReferenceInformation info = grouped.get(storable.getObjectName());
      info.objectType = ReferenceObjectType.valueOf(storable.getObjecttype());
      info.references.add(reference);
    }

    //call objectTypeImplementations.get(objectType).trigger8refs, objectName, revision)
    for (Entry<String, ObjectReferenceInformation> kvp : grouped.entrySet()) {
      List<InternalReference> refs = kvp.getValue().references;
      String objectName = kvp.getKey();
      objectTypeImplementations.get(kvp.getValue().objectType).trigger(refs, objectName, revision);
    }

  }


  private boolean matchRevisionAndPath(ReferenceStorable storabe, String path, Long revision) {
    return storabe.getWorkspace().equals(revision) && storabe.getPath().equals(path);
  }


  public List<InternalReference> convertList(List<ReferenceStorable> in, String repoPath) {
    List<InternalReference> result = new ArrayList<InternalReference>();
    for (ReferenceStorable s : in) {
      result.add(convert(s, repoPath));
    }
    return result;
  }


  public InternalReference convert(ReferenceStorable in, String repoPath) {
    InternalReference result = new InternalReference();
    result.setPath(in.getPath());
    result.setType(in.getReftype());
    result.setPathToRepo(repoPath);
    return result;
  }


  private static class ObjectReferenceInformation {

    private ReferenceObjectType objectType;
    private List<InternalReference> references = new ArrayList<InternalReference>();
  }
}
