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

package com.gip.xyna.xact.filter.session;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.session.modify.Insertion;
import com.gip.xyna.xact.filter.session.modify.operations.copy.CopyData;
import com.gip.xyna.xact.filter.session.modify.operations.copy.StepCopier;
import com.gip.xyna.xact.filter.session.repair.WorkflowRepair;
import com.gip.xyna.xact.filter.xmom.workflows.json.PositionJson;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Workflow;
import com.gip.xyna.xprc.xfractwfe.generation.xml.WorkflowOperation;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;

import xmcp.xact.modeller.Hint;
import xmcp.processmodeller.datatypes.RepairEntry;



public class Clipboard {

  public enum ClipboardCopyDirection { TO_CLIPBOARD, FROM_CLIPBOARD, RECURSIVE_TO_CLIPBOARD }


  private Map<Integer, ClipboardEntry> entries = new HashMap<>();
  private List<Integer> order = new ArrayList<>();
  private int nextId = 0;
  
  private List<Hint> hints = new ArrayList<>();


  public int addEntry(int position, ClipboardEntry element) {
    int usedId = nextId++;
    entries.put(usedId, element);
    order.add(position, usedId);
    return usedId;
  }

  public ClipboardEntry getEntry(int id, boolean remove) {
    
    ClipboardEntry entry = entries.get(id);
    if (remove) {
      entries.remove(id);
    }

    return entry;
  }

  public List<Pair<Integer, ClipboardEntry>> getEntries() {
    List<Pair<Integer, ClipboardEntry>> result = new ArrayList<Pair<Integer, ClipboardEntry>>();
    
    for(Integer id : order) {
      Pair<Integer, ClipboardEntry> pair = new Pair<Integer, ClipboardEntry>(id, entries.get(id));
      result.add(pair);
    }
    
    return result;
  }

  public void clear() {
    entries.clear();
    order.clear();
    nextId = 0;
  }

  //TODO:
  public void removeEntry(GBSubObject object) {
    int id = -1;
    for (Entry<Integer, ClipboardEntry> entry : entries.entrySet()) {
      if (entry.getValue().getObject().equals(object)) {
        id = entry.getKey();
      }
    }

    if (id != -1) {
      entries.remove(id);
    }
    
    final int fId = id;
    order.removeIf(x -> x == fId);
  }

  
  
  public static List<RepairEntry> copyStepFromClipboard(GBSubObject objInClipboard, GBSubObject targetStep, Modification modification, Integer index, PositionJson position) {
    StepMap targetStepMap = modification.getObject().getStepMap();
    GenerationBaseObject sourceGbo = objInClipboard.getRoot();
    StepSerial targetStepSerial = Insertion.wrap(position, targetStep, targetStepMap);
    WF targetWF = targetStep.getStep().getParentWFObject();
    FQName targetFQName = null;
    try {
      targetFQName = new FQName(targetWF.getRevision(), sourceGbo.getWorkflow().getOriginalFqName());
    } catch (XFMG_NoSuchRevision e) {
      throw new RuntimeException(e);
    }
    GenerationBaseObject cloneWF = cloneWF(sourceGbo.getXmomLoader(), sourceGbo, targetFQName);
    List<Step> sourceStepsToInsert = cloneWF.getWFStep().getChildStep().getChildSteps();

    if (index == null || index == -1) {
      index = targetStepSerial.getChildSteps().size();
    }

    Set<Step> stepsToRepair = new HashSet<Step>();
    for (Step s : sourceStepsToInsert) {
      WF.addChildStepsRecursively(stepsToRepair, s);
    }

    WorkflowRepair repair = new WorkflowRepair();
    List<RepairEntry> repairEntries = repair.repairSteps(new ArrayList<Step>(stepsToRepair), cloneWF);

    cloneWF.createDataflow();
    CopyData data = new CopyData(targetStepMap, modification.getObject().getDataflow(), cloneWF.getDataflow());

    for (Step sourceStep : sourceStepsToInsert) {
      StepCopier.copyStepInto(sourceStep, targetStepSerial, index++, data);
    }

    return repairEntries;
  }

  
  private static GenerationBaseObject cloneWF(XMOMLoader loader, GenerationBaseObject object, FQName targetFQName) {
    String xml = turnWFIntoXml(object);
    GenerationBaseObject cloneWF = null;

    try {
      cloneWF = loader.load(targetFQName, xml);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
    return cloneWF;
  }
  
  
  private static String turnWFIntoXml(GenerationBaseObject gbo) {
    WF wf = gbo.getWorkflow();
    WorkflowOperation workflowOperation = new WorkflowOperation(wf, wf.getOriginalSimpleName(), wf.getLabel());
    XmomType persistType = new XmomType(wf.getOriginalPath(), wf.getOriginalSimpleName(), wf.getLabel());
    Workflow workflow = new Workflow(persistType, workflowOperation);
    wf.clearUnusedVariables();

    String xml = workflow.toXML();
    return xml;
  }
  
  
  public List<Hint> getHints(){
    return hints;
  }
  
  public void resetHints() {
    hints.clear();
  }
  
  
  public class ClipboardEntry {
    private GBSubObject object;
    private String fqn;
    private Long revision;
    
    public GBSubObject getObject() {
      return object;
    }
    
    public void setObject(GBSubObject object) {
      this.object = object;
    }
    
    public String getFqn() {
      return fqn;
    }
    
    public void setFqn(String fqn) {
      this.fqn = fqn;
    }
    
    public Long getRevision() {
      return revision;
    }
    
    public void setRevision(Long revision) {
      this.revision = revision;
    }
  }

  
  public void moveEntry(int targetPosition, GBSubObject objToMove) {
    Integer identifierOfObjToMove = findIdentifierOfObj(objToMove);
    if (identifierOfObjToMove == null) {
      return;
    }

    if (targetPosition == -1) {
      order.remove(identifierOfObjToMove);
      order.add(identifierOfObjToMove);
    } else {
      int oldPosition = order.indexOf(identifierOfObjToMove);
      if (oldPosition > targetPosition) {
        //move to the front
        order.remove(oldPosition);
        order.add(targetPosition, identifierOfObjToMove);
      } else {
        //move to the back
        order.add(targetPosition, identifierOfObjToMove);
        order.remove(oldPosition);
      }
    }
  }

  public Integer findIdentifierOfObj(GBSubObject objToMove) {
    for (Entry<Integer, ClipboardEntry> entry : entries.entrySet()) {
      if (entry.getValue().getObject().equals(objToMove)) {
        return entry.getKey();
      }
    }
    return null;
  }
}
