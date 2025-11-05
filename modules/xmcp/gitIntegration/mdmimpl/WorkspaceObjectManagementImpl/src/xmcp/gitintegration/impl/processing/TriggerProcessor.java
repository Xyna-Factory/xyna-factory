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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xact.XynaActivationBase;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.Trigger;
import xmcp.gitintegration.Reference;
import xmcp.gitintegration.ReferenceData;
import xmcp.gitintegration.ReferenceManagement;
import xmcp.gitintegration.RepositoryManagement;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.impl.ItemDifference;
import xmcp.gitintegration.impl.ReferenceComparator;
import xmcp.gitintegration.impl.ReferenceConverter;
import xmcp.gitintegration.impl.ReferenceUpdater;
import xmcp.gitintegration.impl.XynaContentDifferenceType;
import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.impl.xml.ReferenceXmlConverter;
import xmcp.gitintegration.storage.ReferenceStorable;
import xmcp.gitintegration.storage.ReferenceStorage;
import xprc.xpce.Workspace;



public class TriggerProcessor implements WorkspaceContentProcessor<Trigger> {

  private static final String TAG_TRIGGER = "trigger";
  private static final String TAG_TRIGGERNAME = "triggername";
  private static final String TAG_FQTRIGGERCLASSNAME = "fqtriggerclassname";
  private static final String TAG_JARFILES = "jarfiles";
  private static final String TAG_SHAREDLIBS = "sharedlibs";

  private static final ReferenceXmlConverter converter = new ReferenceXmlConverter();
  private static XynaActivationBase xynaActivation;
  private static RevisionManagement revisionManagement;

  private static XynaActivationBase getXynaActivation() {
    if(xynaActivation == null) {
      xynaActivation = XynaFactory.getInstance().getActivation();
    }
    return xynaActivation;
  }

  private static RevisionManagement getRevisionManagement() {
    if(revisionManagement == null) {
      revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    }
    return revisionManagement;
  }


  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends Trigger> from, Collection<? extends Trigger> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<Trigger> toWorkingList = new ArrayList<Trigger>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, Trigger> toMap = new HashMap<String, Trigger>();
    for (Trigger toEntry : toWorkingList) {
      toMap.put(toEntry.getTriggerName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (Trigger fromEntry : from) {
        Trigger toEntry = toMap.get(fromEntry.getTriggerName());

        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_TRIGGER);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (!Objects.equals(fromEntry.getFQTriggerClassName(), toEntry.getFQTriggerClassName())
              || !Objects.equals(fromEntry.getSharedlibs(), toEntry.getSharedlibs())
              || !Objects.equals(fromEntry.getJarfiles(), toEntry.getJarfiles())
              || getReferenceDifferenceList(fromEntry, toEntry).size() > 0) {
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
    for (Trigger toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_TRIGGER);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public Trigger parseItem(Node node) {
    Trigger trig = new Trigger();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_TRIGGERNAME)) {
        trig.setTriggerName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_FQTRIGGERCLASSNAME)) {
        trig.setFQTriggerClassName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_JARFILES)) {
        trig.setJarfiles(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_SHAREDLIBS)) {
        trig.setSharedlibs(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(converter.getTagName())) {
        trig.setReferences(converter.parseTags(childNode));
      }
    }
    return trig;
  }


  @Override
  public void writeItem(XmlBuilder builder, Trigger item) {
    builder.startElement(TAG_TRIGGER);
    builder.element(TAG_TRIGGERNAME, item.getTriggerName());
    if (item.getFQTriggerClassName() != null) {
      builder.element(TAG_FQTRIGGERCLASSNAME, item.getFQTriggerClassName());
    }
    if (item.getJarfiles() != null) {
      builder.element(TAG_JARFILES, item.getJarfiles());
    }
    if (item.getSharedlibs() != null) {
      builder.element(TAG_SHAREDLIBS, item.getSharedlibs());
    }
    if ((item.getReferences() != null) && (!item.getReferences().isEmpty())) {
      converter.appendReferences(item.getReferences(), builder);
    }
    builder.endElement(TAG_TRIGGER);
  }


  @Override
  public String getTagName() {
    return TAG_TRIGGER;
  }


  @Override
  public String createItemKeyString(Trigger item) {
    return item.getTriggerName();
  }


  @Override
  public String createDifferencesString(Trigger from, Trigger to) {
    StringBuffer ds = new StringBuffer();
    if (!Objects.equals(from.getFQTriggerClassName(), to.getFQTriggerClassName())) {
      ds.append("\n");
      ds.append("    " + TAG_FQTRIGGERCLASSNAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getFQTriggerClassName() + "\"=>\"" + to.getFQTriggerClassName() + "\"");
    }
    if (!Objects.equals(from.getJarfiles(), to.getJarfiles())) {
      ds.append("\n");
      ds.append("    " + TAG_JARFILES + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getJarfiles() + "\"=>\"" + to.getJarfiles() + "\"");
    }
    if (!Objects.equals(from.getSharedlibs(), to.getSharedlibs())) {
      ds.append("\n");
      ds.append("    " + TAG_SHAREDLIBS + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getSharedlibs() + "\"=>\"" + to.getSharedlibs() + "\"");
    }

    List<ItemDifference<Reference>> idrList = getReferenceDifferenceList(from, to);
    if (!idrList.isEmpty()) {
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


  private List<ItemDifference<Reference>> getReferenceDifferenceList(Trigger from, Trigger to) {
    ReferenceComparator comparator = new ReferenceComparator();
    return comparator.compare(from.getReferences(), to.getReferences());
  }


  @Override
  public List<Trigger> createItems(Long revision) {
    List<Trigger> tiList = new ArrayList<Trigger>();
    try {
      List<TriggerInformation> trigInfoList = getTriggerInformationList(revision);
      for (TriggerInformation trigInfo : trigInfoList) {
        Trigger trig = new Trigger();
        trig.setTriggerName(trigInfo.getTriggerName());
        trig.setFQTriggerClassName(trigInfo.getFqTriggerClassName());

        StringSerializableList<String> ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
        ssl.setValues(getJarfileList(trig.getTriggerName(), revision));
        trig.setJarfiles(ssl.serializeToString());

        List<Reference> refList = new ArrayList<Reference>();
        ReferenceStorage storage = new ReferenceStorage();
        for (ReferenceStorable storable : storage.getReferencetorableList(revision, trig.getTriggerName(), ReferenceObjectType.TRIGGER)) {
          refList.add(new Reference(storable.getPath(), storable.getReftype()));
        }
        Collections.sort(refList, (x, y) -> x.getPath().compareTo(y.getPath()));
        trig.setReferences(refList);

        ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
        ssl.setValues(Arrays.asList(trigInfo.getSharedLibs()));
        trig.setSharedlibs(ssl.serializeToString());

        tiList.add(trig);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Collections.sort(tiList, (x, y) -> x.getTriggerName().compareTo(y.getTriggerName()));
    return tiList;
  }


  private List<TriggerInformation> getTriggerInformationList(Long revision)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    List<TriggerInformation> resultList = new ArrayList<TriggerInformation>();
    List<TriggerInformation> triggerInfoList = getXynaActivation().listTriggerInformation();

    for (TriggerInformation triggerInfo : triggerInfoList) {
      if (revision == getRevisionManagement().getRevision(triggerInfo.getRuntimeContext())) {
        resultList.add(triggerInfo);
      }
    }
    return resultList;
  }


  private List<String> getJarfileList(String triggerName, Long revision) throws Exception {
    List<String> resultList = new ArrayList<String>();
    com.gip.xyna.xact.trigger.Trigger trigger =
        XynaFactory.getInstance().getActivation().getActivationTrigger().getTrigger(revision, triggerName, false);
    File[] files = trigger.getJarFiles();
    if (files != null) {
      for (File file : files) {
        Path path = Paths.get(file.getParent());
        if (path.getNameCount() > 3) {
          // remove prefix "../revision/revision_REV>/"
          Path resultPath = path.subpath(3, path.getNameCount() - 1);
          resultList.add((new File(resultPath.toString(), file.getName())).getPath());
        } else {
          resultList.add(file.getPath());
        }
      }
    }

    return resultList;
  }


  private List<File> copyToSavedIfNecessary(File[] jarFiles, String fqClassName, Long revision) throws Ex_FileAccessException {
    String targetDirPath = XynaActivationTrigger.getTriggerSavedFolderByTriggerFqClassName(fqClassName, revision);
    return XynaActivationTrigger.copyFilesToTargetFolder(targetDirPath, jarFiles);
  }


  private void createTrigger(Trigger item, long revision) {
    ReferenceSupport refSupport = new ReferenceSupport();
    ReferenceConverter refConverter = new ReferenceConverter();
    Workspace workspaceName = new Workspace(ReferenceUpdater.getWorkspaceName(revision));
    String pathToRepo = RepositoryManagement.getRepositoryConnection(workspaceName).getPath();

    StringSerializableList<String> ssl;
    ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
    String[] jarFiles = ssl.deserializeFromString(item.getJarfiles()).toArray(new String[]{});
    ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
    String[] sharedLibs = ssl.deserializeFromString(item.getSharedlibs()).toArray(new String[]{});
    if (jarFiles.length > 0 && item.getReferences() == null) {
      throw new RuntimeException("No references found (trigger: " + item.getTriggerName() + ")");
    }
    File[] jarFilesArray = new File[jarFiles.length];
    int idx = 0;
    List<Reference> references = new ArrayList<>(item.getReferences());
    List<InternalReference> internalReferences = new ArrayList<>();
    for(Reference reference : references) {
      InternalReference internalRef = refConverter.convert(reference);
      internalRef.setPathToRepo(pathToRepo);
      internalReferences.add(internalRef);
    }
    List<File> candidateFiles = refSupport.executeReferences(internalReferences);
    for (String jarFile : jarFiles) {
      base.File file = candidateFiles.stream().filter(x -> x.getName().equals(jarFile)).map(x -> new base.File(x.getAbsolutePath())).findFirst().get();
      jarFilesArray[idx++] = new File(file.getPath());
    }
    try {
      List<File> jarFilesList = copyToSavedIfNecessary(jarFilesArray, item.getFQTriggerClassName(), revision);
      getXynaActivation().addTrigger(item.getTriggerName(), jarFilesList.toArray(new File[jarFilesList.size()]), item.getFQTriggerClassName(), sharedLibs, revision);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }


  @Override
  public void create(Trigger item, long revision) {
    String workspaceName = ReferenceUpdater.getWorkspaceName(revision);
    for (Reference reference : item.getReferences() != null ? item.getReferences() : new ArrayList<Reference>()) {
      ReferenceData.Builder builder = new ReferenceData.Builder();
      builder.objectName(item.getTriggerName()).objectType(ReferenceObjectType.TRIGGER.toString()).path(reference.getPath())
      .referenceType(reference.getType()).workspaceName(workspaceName);
      ReferenceManagement.addReference(builder.instance());
    }

    createTrigger(item, revision);
  }


  @Override
  public void modify(Trigger from, Trigger to, long revision) {
    createTrigger(to, revision);
    ReferenceComparator comparator = new ReferenceComparator();
    ReferenceUpdater updater = new ReferenceUpdater();
    List<ItemDifference<Reference>> idrList = comparator.compare(from.getReferences(), to.getReferences());
    updater.update(idrList,revision, ReferenceObjectType.TRIGGER, from.getFQTriggerClassName(), to.getFQTriggerClassName());
  }


  @Override
  public void delete(Trigger item, long revision) {
    try {
      getXynaActivation().removeTrigger(item.getTriggerName(), revision);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ReferenceStorage storage = new ReferenceStorage();
    for (Reference reference : item.getReferences() != null ? item.getReferences() : new ArrayList<Reference>()) {
      storage.deleteReference(reference.getPath(), revision, item.getTriggerName());
    }
  }
}
