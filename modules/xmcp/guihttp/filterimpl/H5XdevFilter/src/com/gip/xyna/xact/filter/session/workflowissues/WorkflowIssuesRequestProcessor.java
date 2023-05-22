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

package com.gip.xyna.xact.filter.session.workflowissues;



import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.Dataflow.Linkstate;
import com.gip.xyna.xact.filter.session.Dataflow.LinkstateIn;
import com.gip.xyna.xact.filter.session.Dataflow.MultiLaneConnection;
import com.gip.xyna.xact.filter.session.Dataflow.SimpleConnection;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.workflows.json.Workflow;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xmcp.processmodeller.datatypes.Issue;



public class WorkflowIssuesRequestProcessor {

  public static List<Issue> getIssuesOfWorkflow(GenerationBaseObject obj, XMOMLoader loader) {

    List<Issue> result = new ArrayList<Issue>();
    result.addAll(getDataflowIssues(obj.getDataflow()));
    result.addAll(getWorkflowStepRelatedIssues(obj.getWFStep(), loader));
    
    String workflowJson = Utils.xoToJson(new Workflow(obj).getXoRepresentation());

    result.sort((a, b) -> workflowJson.indexOf("\"id\": \""+ a.getId()+"\"") - (workflowJson.indexOf("\"id\": \""+ b.getId()+"\"")));
    
    return result;
  }


  /**
   * AMBUIGUE_VARIABLE
   * UNASSIGNED_VARIABLE
   * UNASSIGNED_VARIABLE_BRANCH
   */
  private static List<Issue> getDataflowIssues(Dataflow dataflow) {
    Set<Entry<AVariableIdentification, InputConnection>> entries = dataflow.getConnections();
    List<Issue> issues = new ArrayList<Issue>();

    for (Entry<AVariableIdentification, InputConnection> entry : entries) {
      AVariableIdentification target = entry.getKey();
      InputConnection con = entry.getValue();
      issues.addAll(getConnectionIssue(target, con));
    }

    return issues;
  }


  private static List<Issue> getConnectionIssue(AVariableIdentification target, InputConnection con) {
    List<Issue> result = new ArrayList<Issue>();
    for (SimpleConnection sCon : con.getConnectionsPerLane()) {
      Linkstate state = sCon.getLinkState();
      if (state.equals(LinkstateIn.AMBIGUE)) {
        Issue issue = createAmbiguousVariableIssue(target, sCon);
        result.add(issue);
      } else if (state.equals(LinkstateIn.NONE)) {
        Issue issue = createUnassignedVariableIssue(target, sCon, getBranchIdOfConnection(sCon, con));
        result.add(issue);
      }
    }

    return result;
  }


  private static String getBranchIdOfConnection(SimpleConnection sCon, InputConnection con) {
    if (con instanceof SimpleConnection) {
      return null;
    }

    if (!(con instanceof MultiLaneConnection)) {
      return null;
    }

    MultiLaneConnection mCon = (MultiLaneConnection) con;
    return mCon.getBranchIds().get(mCon.getConnectionsPerLane().indexOf(sCon));
  }


  private static Issue createUnassignedVariableIssue(AVariableIdentification target, SimpleConnection sCon, String branchId) {
    String id = null;
    String messageCode = null;

    if (branchId == null) {
      id = target.internalGuiId.createId();
      messageCode = WorkflowIssueMessageCode.UNASSIGNED_VARIABLE;
    } else {
      id = branchId;
      messageCode = WorkflowIssueMessageCode.UNASSIGNED_VARIABLE_BRANCH;
    }


    Issue issue = new Issue.Builder().id(id).messageCode(messageCode).instance();
    return issue;
  }


  private static Issue createAmbiguousVariableIssue(AVariableIdentification target, SimpleConnection sCon) {
    String id = target.internalGuiId.createId();
    String messageCode = WorkflowIssueMessageCode.AMBIGUE_VARIABLE;
    Issue issue = new Issue.Builder().id(id).messageCode(messageCode).instance();
    return issue;
  }


  /**
   * ABSTRACT_CONSTANT
   * PROTOTYPE_VARIABEL
   * PROTOTYPE_STEP
   * OBJECTS_AFTER_BLOCKER
   * RETRY_AT_INVALID_POSITION
   * INVALID_FORMULA
   * INVALID_ORDER_INPUT_SOURCE
   */
  private static List<Issue> getWorkflowStepRelatedIssues(WFStep wfStep, XMOMLoader loader) {
    Set<Step> steps = wfStep.getAllStepsRecursively();
    WorkflowStepIssueVisitor visitor = new WorkflowStepIssueVisitor(loader);

    for (Step step : steps) {
      step.visit(visitor);
    }

    return visitor.getCollectedIssues();
  }
}
