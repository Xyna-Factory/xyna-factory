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
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.TriggerInstance;
import xmcp.gitintegration.WorkspaceContentDifference;



public class TriggerInstanceProcessor implements WorkspaceContentProcessor<TriggerInstance> {

  private static final String TAG_TRIGGERINSTANCE = "triggerinstance";
  private static final String TAG_TRIGGERINSTANCENAME = "triggerinstancename";
  private static final String TAG_TRIGGERNAME = "triggername";
  private static final String TAG_STARTPARAMETER = "startparameter";
  private static final String TAG_MAXRECEIVES = "maxreceives";
  private static final String TAG_REJECTAFTERMAXRECEIVES = "rejectaftermaxreceives";

  private static XynaActivationPortal xynaActivationPortal;
  
  private static XynaActivationPortal getXynaActivationPortal() {
    if(xynaActivationPortal == null) {
      xynaActivationPortal = XynaFactory.getInstance().getActivationPortal();
    }
    return xynaActivationPortal;
  }


  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends TriggerInstance> from, Collection<? extends TriggerInstance> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<TriggerInstance> toWorkingList = new ArrayList<TriggerInstance>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, TriggerInstance> toMap = new HashMap<String, TriggerInstance>();
    for (TriggerInstance toEntry : toWorkingList) {
      toMap.put(toEntry.getTriggerInstanceName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (TriggerInstance fromEntry : from) {
        TriggerInstance toEntry = toMap.get(fromEntry.getTriggerInstanceName());

        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_TRIGGERINSTANCE);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          
          boolean sameStartParameter = Objects.equals(fromEntry.getStartParameter(), toEntry.getStartParameter());
          boolean sameRejectPolicy = Objects.equals(fromEntry.getRejectAfterMaxReceives(), toEntry.getRejectAfterMaxReceives());
          boolean sameMaxRecieves = Objects.equals(fromEntry.getMaxReceives(), toEntry.getMaxReceives());

          if (!sameStartParameter || !sameMaxRecieves || !sameRejectPolicy) {
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
    for (TriggerInstance toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_TRIGGERINSTANCE);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public TriggerInstance parseItem(Node node) {
    TriggerInstance ti = new TriggerInstance();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_TRIGGERINSTANCENAME)) {
        ti.setTriggerInstanceName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_TRIGGERNAME)) {
        ti.setTriggerName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_STARTPARAMETER)) {
        ti.setStartParameter(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_MAXRECEIVES)) {
        ti.setMaxReceives(Long.valueOf(childNode.getTextContent()));
      } else if (childNode.getNodeName().equals(TAG_REJECTAFTERMAXRECEIVES)) {
        ti.setRejectAfterMaxReceives(Boolean.parseBoolean(childNode.getTextContent()));
      }
    }
    return ti;
  }


  @Override
  public void writeItem(XmlBuilder builder, TriggerInstance item) {
    builder.startElement(TAG_TRIGGERINSTANCE);
    builder.element(TAG_TRIGGERINSTANCENAME, item.getTriggerInstanceName());
    builder.element(TAG_TRIGGERNAME, item.getTriggerName());
    if (item.getStartParameter() != null) {
      builder.element(TAG_STARTPARAMETER, XmlBuilder.encode(item.getStartParameter()));
    }
    if (item.getMaxReceives() != null) {
      builder.element(TAG_MAXRECEIVES, String.valueOf(item.getMaxReceives()));
    }
    if (item.getRejectAfterMaxReceives() != null) {
      builder.element(TAG_REJECTAFTERMAXRECEIVES, String.valueOf(item.getRejectAfterMaxReceives()));
    }
    builder.endElement(TAG_TRIGGERINSTANCE);
  }


  @Override
  public String getTagName() {
    return TAG_TRIGGERINSTANCE;
  }


  @Override
  public String createItemKeyString(TriggerInstance item) {
    return item.getTriggerInstanceName();
  }


  @Override
  public String createDifferencesString(TriggerInstance from, TriggerInstance to) {
    StringBuffer ds = new StringBuffer();
    if (!from.getTriggerName().equals(to.getTriggerName())) {
      ds.append("\n");
      ds.append("    " + TAG_TRIGGERNAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getTriggerName() + "\"=>\"" + to.getTriggerName() + "\"");
    }
    String fromStartParameter = from.getStartParameter();
    if (fromStartParameter == null) {
      fromStartParameter = "";
    }
    String toStartParameter = to.getStartParameter();
    if (toStartParameter == null) {
      toStartParameter = "";
    }
    if (!fromStartParameter.equals(toStartParameter)) {
      ds.append("\n");
      ds.append("    " + TAG_STARTPARAMETER + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + fromStartParameter + "\"=>\"" + toStartParameter + "\"");
    }
    if (!Objects.equals(from.getMaxReceives(), to.getMaxReceives())) {
      ds.append("\n");
      ds.append("    " + TAG_MAXRECEIVES + " "); 
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getMaxReceives() + "\"=>\"" + to.getMaxReceives() + "\"");
    }
    if (!Objects.equals(from.getRejectAfterMaxReceives(), to.getRejectAfterMaxReceives())) {
      ds.append("\n");
      ds.append("    " + TAG_REJECTAFTERMAXRECEIVES + " "); 
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getRejectAfterMaxReceives() + "\"=>\"" + to.getRejectAfterMaxReceives() + "\"");
    }
    return ds.toString();
  }


  @Override
  public List<TriggerInstance> createItems(Long revision) {
    List<TriggerInstance> tiList = new ArrayList<TriggerInstance>();
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    try {
      List<TriggerInstanceInformation> tiiList = getTriggerInstanceInformationList(revision);
      for (TriggerInstanceInformation tii : tiiList) {
        TriggerInstance ti = new TriggerInstance();
        ti.setTriggerInstanceName(tii.getTriggerInstanceName());
        ti.setTriggerName(tii.getTriggerName());
        StringSerializableList<String> ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?�$%&!", ':');
        ssl.setValues(tii.getStartParameter());
        ti.setStartParameter(ssl.serializeToString());
        Pair<Long, Boolean> tc = xat.getTriggerConfiguration(tii);
        if (tc != null && (tc.getFirst() != null || tc.getSecond() != null)) {
          ti.setMaxReceives(tc.getFirst());
          ti.setRejectAfterMaxReceives(tc.getSecond());
        }
        tiList.add(ti);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Collections.sort(tiList, (x, y) -> x.getTriggerInstanceName().compareTo(y.getTriggerInstanceName()));
    return tiList;
  }


  private List<TriggerInstanceInformation> getTriggerInstanceInformationList(Long revision) throws PersistenceLayerException {
    List<TriggerInstanceInformation> resultList = new ArrayList<TriggerInstanceInformation>();
    List<TriggerInformation> triggerInfoList = getXynaActivationPortal().listTriggerInformation();
    for (TriggerInformation triggerInfo : triggerInfoList) {
      List<TriggerInstanceInformation> triggerInstInfoList = triggerInfo.getTriggerInstances();
      for (TriggerInstanceInformation triggerInstInfo : triggerInstInfoList) {
        if (triggerInstInfo.getRevision().longValue() == revision.longValue()) {
          resultList.add(triggerInstInfo);
        }
      }
    }
    return resultList;
  }


  @Override
  public void create(TriggerInstance item, long revision) {
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    CommandControl.tryLock(CommandControl.Operation.TRIGGER_DEPLOY, revision);
    try {
      StringSerializableList<String> ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?�$%&!", ':');
      String[] startParameters = ssl.deserializeFromString(item.getStartParameter()).toArray(new String[0]);
      xat.deployTrigger(item.getTriggerName(), item.getTriggerInstanceName(), startParameters, "", revision, true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_DEPLOY, revision);
    }
    
    if(item.getMaxReceives() != null || item.getRejectAfterMaxReceives() != null) {
      CommandControl.tryLock(CommandControl.Operation.TRIGGER_MODIFY, revision);
      long maxEvents = item.getMaxReceives() != null ? item.getMaxReceives() : -1l;
      boolean autoReject = item.getRejectAfterMaxReceives() != null ? item.getRejectAfterMaxReceives() : false;
      try {
        xat.configureTriggerMaxEvents(item.getTriggerInstanceName(), maxEvents, autoReject, revision);
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        CommandControl.unlock(CommandControl.Operation.TRIGGER_MODIFY, revision);
      }
    }
  }


  @Override
  public void modify(TriggerInstance from, TriggerInstance to, long revision) {
    this.delete(from, revision);
    this.create(to, revision);
  }


  @Override
  public void delete(TriggerInstance item, long revision) {
    CommandControl.tryLock(CommandControl.Operation.TRIGGER_UNDEPLOY, revision);
    try {
      XynaFactory.getInstance().getActivation().getActivationTrigger().undeployTrigger(
        item.getTriggerName(),
        item.getTriggerInstanceName(),
        revision
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_UNDEPLOY, revision);
    }
  }
}
