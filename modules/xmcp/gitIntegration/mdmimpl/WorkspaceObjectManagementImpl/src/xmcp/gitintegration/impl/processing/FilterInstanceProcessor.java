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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.XynaActivationBase;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xact.trigger.DeployFilterParameter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FilterInstance;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContentDifference;



public class FilterInstanceProcessor implements WorkspaceContentProcessor<FilterInstance> {

  private static final String TAG_FILTERINSTANCE = "filterinstance";
  private static final String TAG_FILTERINSTANCENAME = "filterinstancename";
  private static final String TAG_FILTERNAME = "filtername";
  private static final String TAG_TRIGGERINSTANCENAME = "triggerinstancename";

  private static final XynaActivationPortal xynaActivationPortal = XynaFactory.getInstance().getActivationPortal();
  private static final XynaActivationBase xynaActivation = XynaFactory.getInstance().getActivation();


  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends FilterInstance> from, Collection<? extends FilterInstance> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<FilterInstance> toWorkingList = new ArrayList<FilterInstance>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, FilterInstance> toMap = new HashMap<String, FilterInstance>();
    for (FilterInstance toEntry : toWorkingList) {
      toMap.put(toEntry.getFilterInstanceName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (FilterInstance fromEntry : from) {
        FilterInstance toEntry = toMap.get(fromEntry.getFilterInstanceName());
        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_FILTERINSTANCE);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (!fromEntry.getFilterName().equals(toEntry.getFilterName())
              || !fromEntry.getTriggerInstanceName().equals(toEntry.getTriggerInstanceName())) {
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
    for (FilterInstance toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_FILTERINSTANCE);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public FilterInstance parseItem(Node node) {
    FilterInstance fi = new FilterInstance();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_FILTERINSTANCENAME)) {
        fi.setFilterInstanceName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_FILTERNAME)) {
        fi.setFilterName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_TRIGGERINSTANCENAME)) {
        fi.setTriggerInstanceName(childNode.getTextContent());
      }
    }
    return fi;
  }


  @Override
  public void writeItem(XmlBuilder builder, FilterInstance item) {
    builder.startElement(TAG_FILTERINSTANCE);
    builder.element(TAG_FILTERINSTANCENAME, item.getFilterInstanceName());
    builder.element(TAG_FILTERNAME, item.getFilterName());
    builder.element(TAG_TRIGGERINSTANCENAME, item.getTriggerInstanceName());
    builder.endElement(TAG_FILTERINSTANCE);
  }


  @Override
  public String getTagName() {
    return TAG_FILTERINSTANCE;
  }


  @Override
  public String createItemKeyString(FilterInstance item) {
    return item.getFilterInstanceName();
  }


  @Override
  public String createDifferencesString(FilterInstance from, FilterInstance to) {
    StringBuffer ds = new StringBuffer();
    if (!from.getFilterName().equals(to.getFilterName())) {
      ds.append("\n");
      ds.append("    " + TAG_FILTERNAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getFilterName() + "\"=>\"" + to.getFilterName() + "\"");
    }
    if (!from.getTriggerInstanceName().equals(to.getTriggerInstanceName())) {
      ds.append("\n");
      ds.append("    " + TAG_TRIGGERINSTANCENAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getTriggerInstanceName() + "\"=>\"" + to.getTriggerInstanceName() + "\"");
    }
    return ds.toString();
  }


  @Override
  public List<FilterInstance> createItems(Long revision) {
    List<FilterInstance> fiList = new ArrayList<FilterInstance>();
    try {
      List<FilterInstanceInformation> fiiList = getFilterInstanceInformationList(revision);
      for (FilterInstanceInformation fii : fiiList) {
        FilterInstance fi = new FilterInstance();
        fi.setFilterInstanceName(fii.getFilterInstanceName());
        fi.setFilterName(fii.getFilterName());
        fi.setTriggerInstanceName(fii.getTriggerInstanceName());
        fiList.add(fi);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Collections.sort(fiList, (x, y) -> x.getFilterInstanceName().compareTo(y.getFilterInstanceName()));
    return fiList;
  }


  private List<FilterInstanceInformation> getFilterInstanceInformationList(Long revision) throws PersistenceLayerException {
    List<FilterInstanceInformation> resultList = new ArrayList<FilterInstanceInformation>();
    List<FilterInformation> filterInfoList = xynaActivationPortal.listFilterInformation();
    for (FilterInformation filterInfo : filterInfoList) {
      List<FilterInstanceInformation> filterInstInfoList = filterInfo.getFilterInstances();
      for (FilterInstanceInformation filterInstInfo : filterInstInfoList) {
        if (filterInstInfo.getRevision().longValue() == revision.longValue()) {
          resultList.add(filterInstInfo);
        }
      }
    }
    return resultList;
  }


  @Override
  public void create(FilterInstance item, long revision) {
    CommandControl.tryLock(CommandControl.Operation.FILTER_DEPLOY, revision);
    try {
      DeployFilterParameter deployFilterParameter =
          new DeployFilterParameter.Builder().filterName(item.getFilterName()).instanceName(item.getFilterInstanceName())
              .triggerInstanceName(item.getTriggerInstanceName()).revision(revision).optional(false).build();
      xynaActivation.getActivationTrigger().deployFilter(deployFilterParameter);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_DEPLOY, revision);
    }
  }


  @Override
  public void modify(FilterInstance from, FilterInstance to, long revision) {
    this.delete(from, revision);
    this.create(to, revision);
  }


  @Override
  public void delete(FilterInstance item, long revision) {
    CommandControl.tryLock(CommandControl.Operation.FILTER_UNDEPLOY, revision);
    try {
      xynaActivation.getActivationTrigger().undeployFilter(item.getFilterInstanceName(), revision);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_UNDEPLOY, revision);
    }
  }


}
