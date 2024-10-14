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



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.Datatype;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.Reference;
import xmcp.gitintegration.ReferenceData;
import xmcp.gitintegration.ReferenceManagement;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.impl.ItemDifference;
import xmcp.gitintegration.impl.ReferenceComparator;
import xmcp.gitintegration.impl.ReferenceUpdater;
import xmcp.gitintegration.impl.XynaContentDifferenceType;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.impl.xml.ReferenceXmlConverter;
import xmcp.gitintegration.storage.ReferenceStorable;
import xmcp.gitintegration.storage.ReferenceStorage;



public class DatatypeProcessor implements WorkspaceContentProcessor<Datatype> {

  private static final String TAG_DATATYPE = "datatype";
  private static final String TAG_FQNAME = "fqname";


  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends Datatype> from, Collection<? extends Datatype> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<Datatype> toWorkingList = new ArrayList<Datatype>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, Datatype> toMap = new HashMap<String, Datatype>();
    for (Datatype toEntry : toWorkingList) {
      toMap.put(toEntry.getFQName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (Datatype fromEntry : from) {
        Datatype toEntry = toMap.get(fromEntry.getFQName());
        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_DATATYPE);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (!getReferenceDifferenceList(fromEntry, toEntry).isEmpty()) {
            wcd.setDifferenceType(new MODIFY());
            wcd.setNewItem(toEntry);
            toWorkingList.remove(toEntry); // remove entry from to-list
          } else {
            toWorkingList.remove(toEntry); // remove entry from to-list
            continue; // EQUAL -> ignore entry
          }
        } else {
          wcd.setDifferenceType(new DELETE());
        }
        wcdList.add(wcd);
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for (Datatype toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_DATATYPE);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public Datatype parseItem(Node node) {
    Datatype dt = new Datatype();
    ReferenceXmlConverter converter = new ReferenceXmlConverter();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_FQNAME)) {
        dt.setFQName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(converter.getTagName())) {
        dt.setReferences(converter.parseTags(childNode));
      }
    }
    return dt;
  }


  @Override
  public void writeItem(XmlBuilder builder, Datatype item) {
    builder.startElement(TAG_DATATYPE);
    builder.element(TAG_FQNAME, item.getFQName());
    ReferenceXmlConverter converter = new ReferenceXmlConverter();
    converter.appendReferences(item.getReferences(), builder);
    builder.endElement(TAG_DATATYPE);
  }


  @Override
  public String getTagName() {
    return TAG_DATATYPE;
  }


  @Override
  public String createItemKeyString(Datatype item) {
    return item.getFQName();
  }


  @Override
  public String createDifferencesString(Datatype from, Datatype to) {
    StringBuffer ds = new StringBuffer();
    ReferenceXmlConverter converter = new ReferenceXmlConverter();

    // Block TAG_REFERENCES
    List<ItemDifference<Reference>> idrList = getReferenceDifferenceList(from, to);
    if (idrList.size() > 0) {
      ds.append("\n");
      ds.append("    " + converter.getTagName());
      for (ItemDifference<Reference> idr : idrList) {
        StringBuffer refEntry = new StringBuffer();
        refEntry.append("\n");
        refEntry.append("      " + idr.getType() + " ");
        if (idr.getType() == XynaContentDifferenceType.CREATE) {
          refEntry.append(idr.getTo().getPath() + ":" + idr.getTo().getType());
        } else if (idr.getType() == XynaContentDifferenceType.MODIFY) {
          refEntry
              .append(idr.getFrom().getPath() + ":" + idr.getFrom().getType() + "=>" + idr.getTo().getPath() + ":" + idr.getTo().getType());
        } else if (idr.getType() == XynaContentDifferenceType.DELETE) {
          refEntry.append(idr.getFrom().getPath() + ":" + idr.getFrom().getType());
        }
        ds.append(refEntry.toString());
      }
    }
    return ds.toString();
  }


  private List<ItemDifference<Reference>> getReferenceDifferenceList(Datatype from, Datatype to) {
    ReferenceComparator rc = new ReferenceComparator();
    return rc.compare(from.getReferences(), to.getReferences());
  }


  @Override
  public List<Datatype> createItems(Long revision) {
    List<Datatype> dtList = new ArrayList<Datatype>();
    Map<String, List<ReferenceStorable>> rsMap = getReferenceStorableListGroupMyFQName(revision);
    if (!rsMap.isEmpty()) {
      for (Map.Entry<String, List<ReferenceStorable>> entry : rsMap.entrySet()) {
        Datatype dd = new Datatype();
        dd.setFQName(entry.getKey());
        List<Reference> refList = new ArrayList<Reference>();
        dd.setReferences(refList);
        for (ReferenceStorable refStorable : entry.getValue()) {
          refList.add(new Reference(refStorable.getPath(), refStorable.getReftype()));
        }
        dtList.add(dd);
      }
    }
    return dtList;
  }


  private static Map<String, List<ReferenceStorable>> getReferenceStorableListGroupMyFQName(Long revision) {
    Map<String, List<ReferenceStorable>> resultMap = new HashMap<String, List<ReferenceStorable>>();
    ReferenceStorage storage = new ReferenceStorage();
    List<ReferenceStorable> refStorablList = storage.getAllReferencesForType(revision, ReferenceObjectType.DATATYPE);
    if (refStorablList != null) {
      for (ReferenceStorable refStorable : refStorablList) {
        if (resultMap.get(refStorable.getObjectName()) == null) {
          resultMap.put(refStorable.getObjectName(), new ArrayList<ReferenceStorable>());
        }
        resultMap.get(refStorable.getObjectName()).add(refStorable);
      }
    }
    return resultMap;
  }


  @Override
  public void create(Datatype item, long revision) {
    String workspaceName = ReferenceUpdater.getWorkspaceName(revision);
    for (Reference ref : item.getReferences()) {
      ReferenceData.Builder builder = new ReferenceData.Builder();
      builder.objectName(item.getFQName()).objectType(ReferenceObjectType.DATATYPE.toString()).path(ref.getPath())
          .referenceType(ref.getType()).workspaceName(workspaceName);
      ReferenceManagement.addReference(builder.instance());
    }
  }


  @Override
  public void modify(Datatype from, Datatype to, long revision) {
    ReferenceComparator rc = new ReferenceComparator();
    ReferenceUpdater updater = new ReferenceUpdater();
    List<ItemDifference<Reference>> idrList = rc.compare(from.getReferences(), to.getReferences());
    updater.update(idrList, revision, ReferenceObjectType.DATATYPE, from.getFQName(), to.getFQName());
  }


  @Override
  public void delete(Datatype item, long revision) {
    ReferenceStorage storage = new ReferenceStorage();
    for (Reference reference : item.getReferences() != null ? item.getReferences() : new ArrayList<Reference>()) {
      storage.deleteReference(reference.getPath(), revision, item.getFQName());
    }
  }

}
