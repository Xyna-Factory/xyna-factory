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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.Reference;
import xmcp.gitintegration.Filter;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.impl.ItemDifference;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.storage.ReferenceStorable;



public class FilterProcessor implements WorkspaceContentProcessor<Filter> {

  private static final String TAG_FILTER = "filter";
  private static final String TAG_FILTERNAME = "filtername";
  private static final String TAG_FQFILTERCLASSNAME = "fqfilterclassname";
  private static final String TAG_JARFILES = "jarfiles";
  private static final String TAG_TRIGGERNAME = "triggername";
  private static final String TAG_SHAREDLIBS = "sharedlibs";

  private static final XynaActivationPortal xynaActivationPortal = XynaFactory.getInstance().getActivationPortal();
  private static RevisionManagement revisionManagement;
  private RevisionManagement getRevisionManagement() {
    if(revisionManagement == null) {
      revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    }
    return revisionManagement;
  }

  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends Filter> from, Collection<? extends Filter> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<Filter> toWorkingList = new ArrayList<Filter>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, Filter> toMap = new HashMap<String, Filter>();
    for (Filter toEntry : toWorkingList) {
      toMap.put(toEntry.getFilterName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (Filter fromEntry : from) {
        Filter toEntry = toMap.get(fromEntry.getFilterName());

        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_FILTER);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (!Objects.equals(fromEntry.getFQFilterClassName(), toEntry.getFQFilterClassName())
              || !Objects.equals(fromEntry.getSharedlibs(), toEntry.getSharedlibs())
              || !Objects.equals(fromEntry.getJarfiles(), toEntry.getJarfiles())
              || !Objects.equals(fromEntry.getTriggerName(), toEntry.getTriggerName())
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
    for (Filter toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_FILTER);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public Filter parseItem(Node node) {
    Filter filter = new Filter();
    NodeList childNodes = node.getChildNodes();
    ReferenceSupport rs = new ReferenceSupport();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_FILTERNAME)) {
        filter.setFilterName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_FQFILTERCLASSNAME)) {
        filter.setFQFilterClassName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_JARFILES)) {
        filter.setJarfiles(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_SHAREDLIBS)) {
        filter.setSharedlibs(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_TRIGGERNAME)) {
        filter.setTriggerName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(rs.getTagName())) {
        filter.setReferences(rs.parseTags(childNode));
      }
    }
    return filter;
  }


  @Override
  public void writeItem(XmlBuilder builder, Filter item) {
    builder.startElement(TAG_FILTER);
    builder.element(TAG_FILTERNAME, item.getFilterName());
    if (item.getFQFilterClassName() != null) {
      builder.element(TAG_FQFILTERCLASSNAME, item.getFQFilterClassName());
    }
    if (item.getJarfiles() != null) {
      builder.element(TAG_JARFILES, item.getJarfiles());
    }
    if (item.getSharedlibs() != null) {
      builder.element(TAG_SHAREDLIBS, item.getSharedlibs());
    }
    builder.element(TAG_TRIGGERNAME, item.getTriggerName());
    if ((item.getReferences() != null) && (!item.getReferences().isEmpty())) {
      ReferenceSupport rs = new ReferenceSupport();
      rs.appendReferences(item.getReferences(), builder);
    }
    builder.endElement(TAG_FILTER);
  }


  @Override
  public String getTagName() {
    return TAG_FILTER;
  }


  @Override
  public String createItemKeyString(Filter item) {
    return item.getFilterName();
  }


  @Override
  public String createDifferencesString(Filter from, Filter to) {
    StringBuffer ds = new StringBuffer();
    
    if (!Objects.equals(from.getFQFilterClassName(), to.getFQFilterClassName())) {
      ds.append("\n");
      ds.append("    " + TAG_FQFILTERCLASSNAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getFQFilterClassName() + "\"=>\"" + to.getFQFilterClassName() + "\"");
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
    if (!Objects.equals(from.getTriggerName(), to.getTriggerName())) {
      ds.append("\n");
      ds.append("    " + TAG_TRIGGERNAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getTriggerName() + "\"=>\"" + to.getTriggerName() + "\"");
    }

    List<ItemDifference<Reference>> idrList = getReferenceDifferenceList(from, to);
    if (idrList.size() > 0) {
      ReferenceSupport rs = new ReferenceSupport();
      ds.append("\n");
      ds.append("    " + rs.getTagName());
      for (ItemDifference<Reference> idr : idrList) {
        StringBuffer refEntry = new StringBuffer();
        refEntry.append("\n");
        refEntry.append("      " + idr.getType().getSimpleName() + " ");
        if (idr.getType().getSimpleName().equals((CREATE.class.getSimpleName()))) {
          refEntry.append(idr.getTo().getPath() + ":" + idr.getTo().getType());
        } else if (idr.getType().getSimpleName().equals((MODIFY.class.getSimpleName()))) {
          refEntry
              .append(idr.getFrom().getPath() + ":" + idr.getFrom().getType() + "=>" + idr.getTo().getPath() + ":" + idr.getTo().getType());
        } else if (idr.getType().getSimpleName().equals((DELETE.class.getSimpleName()))) {
          refEntry.append(idr.getFrom().getPath() + ":" + idr.getFrom().getType());
        }
        ds.append(refEntry.toString());
      }
    }
    return ds.toString();
  }


  private List<ItemDifference<Reference>> getReferenceDifferenceList(Filter from, Filter to) {
    ReferenceSupport rs = new ReferenceSupport();
    return rs.compare(from.getReferences(), to.getReferences());
  }


  @Override
  public List<Filter> createItems(Long revision) {
    List<Filter> tiList = new ArrayList<Filter>();
    try {
      List<FilterInformation> filterInfoList = getFilterInformationList(revision);
      for (FilterInformation filterInfo : filterInfoList) {
        Filter filter = new Filter();
        filter.setFilterName(filterInfo.getFilterName());
        filter.setFQFilterClassName(filterInfo.getFqFilterClassName());

        StringSerializableList<String> ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
        ssl.setValues(getJarfileList(filter.getFilterName(), revision));
        filter.setJarfiles(ssl.serializeToString());

        ReferenceSupport rs = new ReferenceSupport();
        List<Reference> refList = new ArrayList<Reference>();
        for (ReferenceStorable storable : rs.getReferencetorableList(revision, filter.getFilterName(), ReferenceObjectType.FILTER)) {
          refList.add(rs.convertToTag(storable));
        }
        filter.setReferences(refList);

        ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
        ssl.setValues(Arrays.asList(filterInfo.getSharedLibs()));
        filter.setSharedlibs(ssl.serializeToString());

        filter.setTriggerName(filterInfo.getTriggerName());

        tiList.add(filter);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return tiList;
  }


  private List<FilterInformation> getFilterInformationList(Long revision)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    List<FilterInformation> resultList = new ArrayList<FilterInformation>();
    List<FilterInformation> filterInfoList = xynaActivationPortal.listFilterInformation();

    for (FilterInformation filterInfo : filterInfoList) {
      if (revision == getRevisionManagement().getRevision(filterInfo.getRuntimeContext())) {
        resultList.add(filterInfo);
      }
    }
    return resultList;
  }


  private List<String> getJarfileList(String filterName, Long revision)
      throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException, XACT_FilterNotFound {
    List<String> resultList = new ArrayList<String>();
    com.gip.xyna.xact.trigger.Filter filter =
        XynaFactory.getInstance().getActivation().getActivationTrigger().getFilter(revision, filterName, false);
    File[] files = filter.getJarFiles();
    if (files != null) {
      for (File file : files) {
        Path path = Paths.get(file.getParent());
        if (path.getNameCount() > 3) {
          // remove prefix "../revision/revision_REV/"
          Path resultPath = path.subpath(3, path.getNameCount() - 1);
          resultList.add((new File(resultPath.toString(), file.getName())).getPath());
        } else {
          resultList.add(file.getPath());
        }
      }
    }
    return resultList;
  }


  @Override
  public void create(Filter item, long revision) {
    // TODO Auto-generated method stub

  }


  @Override
  public void modify(Filter from, Filter to, long revision) {
    // TODO Auto-generated method stub

  }


  @Override
  public void delete(Filter item, long revision) {
    // TODO Auto-generated method stub

  }


}
