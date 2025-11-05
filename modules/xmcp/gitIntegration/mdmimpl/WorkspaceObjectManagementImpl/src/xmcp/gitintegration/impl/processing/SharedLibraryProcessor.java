/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.FileUtils;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibDeploymentAlgorithm;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.Reference;
import xmcp.gitintegration.ReferenceData;
import xmcp.gitintegration.ReferenceManagement;
import xmcp.gitintegration.SharedLibrary;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.impl.ItemComparator;
import xmcp.gitintegration.impl.ItemDifference;
import xmcp.gitintegration.impl.ReferenceComparator;
import xmcp.gitintegration.impl.ReferenceConverter;
import xmcp.gitintegration.impl.ReferenceUpdater;
import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.impl.xml.ReferenceXmlConverter;
import xmcp.gitintegration.storage.ReferenceStorable;
import xmcp.gitintegration.storage.ReferenceStorage;
import xmcp.gitintegration.tools.ItemDifferenceConverter;

public class SharedLibraryProcessor implements WorkspaceContentProcessor<SharedLibrary> {

  private static final String TAG_SHARED_LIBRARY = "sharedlibrary";
  private static final String TAG_NAME = "name";
  
  
  private final ItemComparator<SharedLibrary> comparator;
  private final ItemDifferenceConverter converter;
  
  public SharedLibraryProcessor() {
    comparator = new ItemComparator<>(SharedLibrary::getName, List.of(SharedLibraryProcessor::checkReferencesEquals));
    converter = new ItemDifferenceConverter();
  }
  
  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends SharedLibrary> from, Collection<? extends SharedLibrary> to) {
    List<ItemDifference<SharedLibrary>> diffs = comparator.compare(from, to);
    return converter.convert(diffs, TAG_SHARED_LIBRARY);
  }

  
  private static boolean checkReferencesEquals(SharedLibrary from, SharedLibrary to) {
    ReferenceComparator referenceComparator = new ReferenceComparator();
    return referenceComparator.compare(from.getReferences(), to.getReferences()).isEmpty();
  }

  @Override
  public SharedLibrary parseItem(Node node) {
    SharedLibrary dt = new SharedLibrary();
    ReferenceXmlConverter converter = new ReferenceXmlConverter();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_NAME)) {
        dt.unversionedSetName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(converter.getTagName())) {
        dt.setReferences(converter.parseTags(childNode));
      }
    }
    return dt;
  }

  @Override
  public void writeItem(XmlBuilder builder, SharedLibrary item) {
    builder.startElement(TAG_SHARED_LIBRARY);
    builder.element(TAG_NAME, item.getName());
    ReferenceXmlConverter converter = new ReferenceXmlConverter();
    converter.appendReferences(item.getReferences(), builder);
    builder.endElement(TAG_SHARED_LIBRARY);
  }

  @Override
  public String getTagName() {
    return TAG_SHARED_LIBRARY;
  }

  @Override
  public String createItemKeyString(SharedLibrary item) {
    return item.getName();
  }

  @Override
  public String createDifferencesString(SharedLibrary from, SharedLibrary to) {
    StringBuilder sb = new StringBuilder();
    ReferenceComparator rc = new ReferenceComparator();
    ReferenceXmlConverter converter = new ReferenceXmlConverter();
    
    List<ItemDifference<Reference>> differences = rc.compare(from.getReferences(), to.getReferences());
    converter.createDifferencesString(sb, differences);
    return sb.toString();
  }

  @Override
  public List<SharedLibrary> createItems(Long revision) {
    List<SharedLibrary> result = new ArrayList<>();
    ReferenceStorage storage = new ReferenceStorage();
    Map<String, List<ReferenceStorable>> rsMap = storage.getReferenceStorableListGroupByName(revision, ReferenceObjectType.SHAREDLIB);
    if (rsMap.isEmpty()) {
      return result;
    }
    for (Map.Entry<String, List<ReferenceStorable>> entry : rsMap.entrySet()) {
      SharedLibrary sharedLib = new SharedLibrary();
      sharedLib.unversionedSetName(entry.getKey());
      List<Reference> refList = new ArrayList<Reference>();
      sharedLib.setReferences(refList);
      List<ReferenceStorable> refStorables = entry.getValue();
      Collections.sort(refStorables, (x, y) -> x.getIndex().compareTo(y.getIndex()));
      for (ReferenceStorable refStorable : refStorables) {
        refList.add(new Reference(refStorable.getPath(), refStorable.getReftype()));
      }
      result.add(sharedLib);
    }
    return result;
  }


  @Override
  public void create(SharedLibrary item, long revision) {
    String workspaceName = ReferenceUpdater.getWorkspaceName(revision);
    ReferenceConverter converter = new ReferenceConverter();
    List<InternalReference> internalReferences = new ArrayList<>();
    for (Reference ref : item.getReferences()) {
      ReferenceData.Builder builder = new ReferenceData.Builder();
      builder.objectName(item.getName()).objectType(ReferenceObjectType.SHAREDLIB.toString()).path(ref.getPath())
          .referenceType(ref.getType()).workspaceName(workspaceName);
      ReferenceManagement.addReference(builder.instance());
      internalReferences.add(converter.convert(ref));
    }
    
    ReferenceSupport refSupport = new ReferenceSupport();
    refSupport.triggerReferences(internalReferences, revision);
    
    try {
      SharedLibDeploymentAlgorithm.deploySharedLib(item.getName(), revision);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void modify(SharedLibrary from, SharedLibrary to, long revision) {
    ReferenceComparator rc = new ReferenceComparator();
    ReferenceUpdater updater = new ReferenceUpdater();
    List<ItemDifference<Reference>> idrList = rc.compare(from.getReferences(), to.getReferences());
    updater.update(idrList, revision, ReferenceObjectType.SHAREDLIB, from.getName(), to.getName());
  }

  @Override
  public void delete(SharedLibrary item, long revision) {
    ReferenceStorage storage = new ReferenceStorage();
    for (Reference reference : item.getReferences() != null ? item.getReferences() : new ArrayList<Reference>()) {
      storage.deleteReference(reference.getPath(), revision, item.getName());
    }
    
    File sharedLibDeployDir = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision, true), item.getName());
    File sharedLibSavedDir = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision, false), item.getName());
    
    FileUtils.deleteDirectoryRecursively(sharedLibDeployDir);
    FileUtils.deleteDirectoryRecursively(sharedLibSavedDir);
  }
}
