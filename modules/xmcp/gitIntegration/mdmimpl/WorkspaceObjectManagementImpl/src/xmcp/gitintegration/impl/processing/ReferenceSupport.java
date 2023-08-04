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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.Reference;
import xmcp.gitintegration.WorkspaceContentDifferenceType;
import xmcp.gitintegration.impl.ItemDifference;
import xmcp.gitintegration.impl.OutputCreator;
import xmcp.gitintegration.impl.references.ReferenceMethods;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.impl.references.ReferenceType;
import xmcp.gitintegration.impl.references.methods.LibFolderMethods;
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

  public static final String TAG_REFERENCES = "references";
  public static final String TAG_REFERENCE = "reference";
  public static final String TAG_PATH = "path";
  public static final String TAG_TYPE = "type";

  private static final HashMap<ReferenceType, ReferenceMethods> implementations = setReferenceMethods();
  
  
  private static HashMap<ReferenceType, ReferenceMethods> setReferenceMethods() {
    HashMap<ReferenceType, ReferenceMethods> result = new HashMap<ReferenceType, ReferenceMethods>();
    
    //register implementations here
    result.put(ReferenceType.lib_folder, new LibFolderMethods());
    
    return result;
  }

  private ReferenceMethods dispatch(ReferenceType type) {
    return implementations.get(type);
  }
  

  public String getTagName() {
    return TAG_REFERENCES;
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


  public void create(Reference tag, long revision, String objectName, String objectType) {
    //when merging workspace.xml
    create(tag.getPath(), objectType, tag.getType().toString(), revision, objectName);
  }


  public void appendReferences(List<? extends Reference> tags, XmlBuilder builder) {
    builder.startElement(TAG_REFERENCES);
    for (Reference tag : tags) {
      appendReference(tag, builder);
    }
    builder.endElement(TAG_REFERENCES);
  }


  public void appendReference(Reference tag, XmlBuilder builder) {
    builder.startElement(TAG_REFERENCE);
    builder.element(TAG_TYPE, tag.getType().toString());
    builder.element(TAG_PATH, tag.getPath());
    builder.endElement(TAG_REFERENCE);
  }


  /**
   * Input should point to a node with getNodeName() equals TAG_REFERENCES
   */
  public List<Reference> parseTags(Node n) {
    List<Reference> result = new ArrayList<Reference>();
    if (n.getNodeName().equals(TAG_REFERENCES)) {
      NodeList list = n.getChildNodes();
      for (int i = 0; i < list.getLength(); i++) {
        Node child = list.item(i);
        if (child.getNodeName().equals(TAG_REFERENCE)) {
          Reference tag = parse(child);
          result.add(tag);
        }
      }
    }
    return result;
  }


  /**
   * Input should point to a node with getNodeName() equals TAG_REFERENCE
   */
  public Reference parse(Node n) {
    Reference result = new Reference();
    NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      String nodeName = child.getNodeName();
      if (nodeName.equals(TAG_PATH)) {
        result.setPath(child.getTextContent());
      } else if (nodeName.equals(TAG_TYPE)) {
        result.setType(child.getTextContent());
      }
    }

    return result;
  }


  public Reference convertToTag(ReferenceStorable storable) {
    Reference result = new Reference();
    result.setPath(storable.getPath());
    result.setType(storable.getReftype());
    return result;
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


  private static String keyMap(Reference t) {
    return t.getPath();
  }


  //to may be null
  private boolean compareReferenceTags(Reference from, Reference to) {
    return to != null && Objects.equals(from.getPath(), to.getPath()) && Objects.equals(from.getType(), to.getType());
  }


  public List<ItemDifference<Reference>> compare(Collection<? extends Reference> from, Collection<? extends Reference> to) {
    List<ItemDifference<Reference>> result = new ArrayList<ItemDifference<Reference>>();
    from = from == null ? new ArrayList<Reference>() : from;
    List<Reference> toList = to == null ? new ArrayList<Reference>() : new ArrayList<Reference>(to);
    Map<String, Reference> toMap = toList.stream().collect(Collectors.toMap(ReferenceSupport::keyMap, Function.identity()));
    String key;

    // iterate over from-list
    // create MODIFY and DELETE entries
    for (Reference fromEntry : from) {
      key = keyMap(fromEntry);
      Reference toEntry = toMap.get(key);
      toList.remove(toEntry);
      if (!compareReferenceTags(fromEntry, toEntry)) {
        Class<? extends WorkspaceContentDifferenceType> type = toEntry == null ? DELETE.class : MODIFY.class;
        result.add(new ItemDifference<Reference>(type, fromEntry, toEntry));
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for (Reference tag : toList) {
      result.add(new ItemDifference<Reference>(CREATE.class, null, tag));
    }

    return result;
  }


  private void appendDifference(StringBuilder ds, ItemDifference<Reference> difference) {
    appendFormatted(ds, difference.getFrom());
    if (difference.getType() == MODIFY.class) {
      ds.append(" => ");
    }
    appendFormatted(ds, difference.getTo());
    ds.append("\n");
  }


  /**
   * reference may be null
   */
  private void appendFormatted(StringBuilder sb, Reference reference) {
    if (reference == null) {
      return;
    }
    sb.append(reference.getType()).append(": \"").append(reference.getPath()).append("\"");
  }


  public void createDifferencesString(StringBuilder ds, List<ItemDifference<Reference>> differences) {
    OutputCreator creator = new OutputCreator();
    creator.appendDiffs(ds, differences, TAG_REFERENCES, this::appendDifference);

  }


  public void modify(Reference from, Reference to, Long revision, String objectName, ReferenceObjectType objectType) {
    ReferenceStorage storage = new ReferenceStorage();
    storage.deleteReference(from.getPath(), revision, objectName);
    ReferenceStorable storable = new ReferenceStorable();
    storable.setObjectName(objectName);
    storable.setWorkspace(revision);
    storable.setPath(to.getPath());
    storable.setReftype(to.getType().toString());
    storable.setObjecttype(objectType.toString());
    storage.persist(storable);
  }


  public void delete(String path, Long revision, String objectName) {
    ReferenceStorage storage = new ReferenceStorage();
    storage.deleteReference(path, revision, objectName);
  }


  public List<ReferenceStorable> getReferencetorableList(Long revision, String objectName, ReferenceObjectType objectType) {
    List<ReferenceStorable> resultList = new ArrayList<ReferenceStorable>();
    ReferenceStorage storage = new ReferenceStorage();
    List<ReferenceStorable> refStorablList = storage.getAllReferencesForType(revision, objectType);
    if (refStorablList != null) {
      for (ReferenceStorable refStorable : refStorablList) {
        if (refStorable.getObjectName().equals(objectName)) {
          resultList.add(refStorable);
        }
      }
    }
    return resultList;
  }
  
  public File findJar(List<Reference> references, String jarName, Long revision) {
    for (Reference reference : references) {
      ReferenceType referenceType = ReferenceType.valueOf(reference.getType());
      ReferenceMethods methods = dispatch(referenceType);
      Optional<File> result = methods.findJar(reference, jarName, revision);
      if (result.isPresent()) {
        return result.get();
      }
    }
    return null;
  }
}
