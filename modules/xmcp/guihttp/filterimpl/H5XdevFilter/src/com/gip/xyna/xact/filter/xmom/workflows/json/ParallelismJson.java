/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.xact.filter.xmom.workflows.json;



import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.StepVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;

import xmcp.processmodeller.datatypes.ContentArea;
import xmcp.processmodeller.datatypes.Parallelism;
import xmcp.processmodeller.datatypes.distinction.Branch;



public class ParallelismJson extends XMOMGuiJson implements HasXoRepresentation {

  private View view;
  private StepParallel stepParallel;
  private ObjectId parallelismId;
  
  
  public ParallelismJson() {
    
  }
  
  public ParallelismJson(StepParallel stepParallel) {
    this.stepParallel = stepParallel;
  }


  public ParallelismJson(View view, StepParallel stepParallel, StepVisitor stepVisitor) { // TODO: Parameter stepVisitor entfernen?
    this.view = view;
    this.stepParallel = stepParallel;
    this.parallelismId = ObjectId.createStepId(stepParallel);
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    Parallelism p = new Parallelism();
    p.setId(parallelismId.getObjectId());
    p.setDeletable(true);
    ContentArea contentArea = new ContentArea();
    contentArea.setItemTypes(ServiceUtils.getItemTypesContentArea(MetaXmomContainers.BRANCH_FQN));
    contentArea.setName(Tags.CONTENT);
    contentArea.setId(ObjectId.createId(ObjectType.branchArea, parallelismId.getBaseId()));

    List<Step> parallelLanes = stepParallel.getChildSteps();
    for (int branchNo = 0; branchNo < parallelLanes.size(); branchNo++) {
      Step child = parallelLanes.get(branchNo);
      Branch branch = new Branch();
      branch.setId(ObjectId.createBranchId(parallelismId.getBaseId(), String.valueOf(branchNo)));
      WorkflowStepVisitor parallelismLane = new WorkflowStepVisitor(view, child);
      branch.addToAreas(ServiceUtils.createContentArea(parallelismLane.createWorkflowSteps(), ObjectId.createStepId(child).getObjectId(), Tags.CONTENT, false));
      contentArea.addToItems(branch);
    }
    if(contentArea.getItems() == null) {
      contentArea.setItems(Collections.emptyList());
    }
    p.addToAreas(contentArea);
    
    return p;
  }
  
  public static class ParallelismJsonVisitor extends EmptyJsonVisitor<ParallelismJson> {

    private ParallelismJson vj = new ParallelismJson();


    @Override
    public ParallelismJson get() {
      return vj;
    }

    @Override
    public ParallelismJson getAndReset() {
      ParallelismJson ret = vj;
      vj = new ParallelismJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {

      throw new UnexpectedJSONContentException(label);
    }

  }

}
