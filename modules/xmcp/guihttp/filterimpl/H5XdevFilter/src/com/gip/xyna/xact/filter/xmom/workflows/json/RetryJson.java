/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;

import xmcp.processmodeller.datatypes.Retry;



public class RetryJson extends XMOMGuiJson implements HasXoRepresentation {

  private View view;
  private String label;
  private String documentation;
  private ObjectId retryId;
  private IdentifiedVariables identifiedVariables;


  private RetryJson() {

  }
  
  
  public RetryJson(View view, StepRetry stepRetry) {
    this.view = view;
    this.label = stepRetry.getLabel();
    this.documentation = stepRetry.getDocumentation();
    this.retryId = ObjectId.createStepId(stepRetry);
    this.identifiedVariables = view.getGenerationBaseObject().identifyVariables(retryId);
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    Retry retry = new Retry();
    retry.setId(retryId.getObjectId());
    retry.setLabel(label);
    retry.setDeletable(true);
    retry.addToAreas(ServiceUtils.createLabelArea(label));
    retry.addToAreas(ServiceUtils.createDocumentationArea(retryId, documentation));
    retry.addToAreas(ServiceUtils.createVariableArea(view.getGenerationBaseObject(), retryId, VarUsageType.input, identifiedVariables, Tags.RETRY_INPUT, new String[] {MetaXmomContainers.DATA_FQN}, true));
    return retry;
  }

  public String getLabel() {
    return label;
  }


  public String getDocumentation() {
    return documentation;
  }

  public static class RetryJsonVisitor extends EmptyJsonVisitor<RetryJson> {
    RetryJson tj = new RetryJson();

    @Override
    public RetryJson get() {
      return tj;
    }

    @Override
    public RetryJson getAndReset() {
      RetryJson ret = tj;
      tj = new RetryJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!value.equals(Tags.RETRY)) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.RETRY);
        } else {
          return;
        }
      }

      if (label.equals(Tags.LABEL)) {
        tj.label = value;
        return;
      }
      if (label.equals(Tags.RETRY_DOCUMENTATION)) {
        tj.documentation = value;
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

}
