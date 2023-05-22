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

package com.gip.xyna.xact.filter.xmom.workflows.json;



import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.Step;

import xmcp.processmodeller.datatypes.Compensation;



public class CompensationJson implements HasXoRepresentation {

  private final View view;
  private final Step compensation;


  public CompensationJson(View view, Step compensation) {
    this.view = view;
    this.compensation = compensation;
  }


  @Override
  public GeneralXynaObject getXoRepresentation() {
    Compensation c = new Compensation();
    c.setDeletable(false);
    c.setOverriddenDefaultCompensation(compensation != null);

    if (compensation != null) {
      WorkflowStepVisitor step = new WorkflowStepVisitor(view, compensation);
      c.addToAreas(ServiceUtils.createContentArea(step.createWorkflowSteps(), ObjectId.createStepId(compensation).getObjectId(), Tags.CONTENT, false));
    }

    return c;
  }
}
