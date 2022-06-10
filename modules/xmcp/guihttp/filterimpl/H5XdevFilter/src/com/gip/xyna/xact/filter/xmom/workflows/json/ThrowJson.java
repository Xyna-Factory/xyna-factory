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



import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;

import xmcp.processmodeller.datatypes.exception.Throw;



public class ThrowJson extends XMOMGuiJson implements HasXoRepresentation {

  private View view;
  private String label;
  private boolean isList;
  private String documentation;
  private ObjectId throwId;
  private IdentifiedVariables identifiedVariables;
  private FQNameJson fqName;

  public ThrowJson() {

  }

  public ThrowJson(View view, StepThrow stepThrow) {
    this.view = view;
    this.label = stepThrow.getLabel();
    this.documentation = stepThrow.getDocumentation();
    this.throwId = ObjectId.createStepId(stepThrow);
    this.identifiedVariables = view.getGenerationBaseObject().identifyVariables(throwId);
    List<AVariableIdentification> inputVariables = this.identifiedVariables.getVariables(VarUsageType.input);
    if(inputVariables != null && inputVariables.size() == 1) {
      this.fqName = FQNameJson.ofPathAndName(inputVariables.get(0).getIdentifiedVariable().getFQClassName());
    }
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    Throw t = new Throw();
    t.setId(throwId.getObjectId());
    t.setLabel(label);
    t.setDeletable(true);
    t.addToAreas(ServiceUtils.createLabelArea(label));
    t.addToAreas(ServiceUtils.createDocumentationArea(throwId, documentation));
    t.addToAreas(ServiceUtils.createVariableArea(view.getGenerationBaseObject(), throwId, VarUsageType.input, 
                             identifiedVariables, Tags.THROW_INPUT, new String[] { MetaXmomContainers.EXCEPTION_FQN }, true));
    return t;
  }

  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isList() {
    return isList;
  }
  
  
  public void setList(boolean isList) {
    this.isList = isList;
  }

  public FQNameJson getFqName() {
    return fqName;
  }
  
  
  public void setFqName(FQNameJson fqName) {
    this.fqName = fqName;
  }


  public static class ThrowJsonVisitor extends EmptyJsonVisitor<ThrowJson> {
    ThrowJson tj = new ThrowJson();

    @Override
    public ThrowJson get() {
      return tj;
    }

    @Override
    public ThrowJson getAndReset() {
      ThrowJson ret = tj;
      tj = new ThrowJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if ( !(value.equals(Tags.THROW)) && !(value.equals(Tags.EXCEPTION))) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.THROW + " or " + Tags.EXPRESSION);
        } else {
          return;
        }
      }

      if (label.equals(Tags.LABEL)) {
        tj.label = value;
        return;
      }
      if( label.equals(Tags.IS_LIST) ) {
        tj.isList = Boolean.parseBoolean(value);
        return;
      }
      if (FQNameJson.useLabel(label)) {
        tj.fqName = FQNameJson.parseAttribute(tj.fqName, label, value);
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

}
