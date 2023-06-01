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



import java.util.Arrays;

import com.gip.xyna.XynaFactory;
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
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

import xmcp.processmodeller.datatypes.Mapping;
import xmcp.processmodeller.datatypes.ModellingItem;
import xmcp.processmodeller.datatypes.Template;



public class MappingJson extends XMOMGuiJson implements HasXoRepresentation {
  
  private static final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal();
  private static final String XYNA_PROPERTY_KEY_QUERY_DEBUG = "xyna.processmodeller.query.debug";

  private View view;
  private StepMapping stepMapping;
  private ObjectId mappingId;
  private IdentifiedVariables identifiedVariables;
  private Boolean isCondition = false; 
  
  public String label;
  public String expression;

  
  private MappingJson() {
    
  }

  public MappingJson(String label) {
    this.label = label;
  }
  
  
  public MappingJson(View view, StepMapping stepMapping) {
    this.view = view;
    this.stepMapping = stepMapping;
    this.mappingId = ObjectId.createStepId(stepMapping);
    this.identifiedVariables = view.getGenerationBaseObject().identifyVariables(mappingId);
    this.label = stepMapping.getLabel();
  }

  @Override
  public GeneralXynaObject getXoRepresentation() {
    if(stepMapping.isConditionMapping() && hideConditionMapping()) { // suppress helper mappings that are used to build the input of a query
      return null;
    } else if (stepMapping.isTemplateMapping()) {
      Template t = new Template();
      t.setId(mappingId.getObjectId());
      t.setDeletable(true);
      addAreas(t);
      return t;
    } else {
      Mapping m = new Mapping();
      m.setLabel(stepMapping.getLabel());
      m.setId(mappingId.getObjectId());
      m.setDeletable(true);
      addAreas(m);
      return m;
    }
  }
  
  private boolean hideConditionMapping() {
    String showConditionMapping = multiChannelPortal.getProperty(XYNA_PROPERTY_KEY_QUERY_DEBUG);
    if(showConditionMapping != null && showConditionMapping.equalsIgnoreCase("true")) {
      return false;
    }
    return true;
  }

  private void addAreas(ModellingItem item) {
    item.addToAreas(ServiceUtils.createLabelArea(mappingId, stepMapping.getLabel(), stepMapping.getClassName(), false, false));
    item.addToAreas(ServiceUtils.createDocumentationArea(mappingId, stepMapping.getDocumentation()));

    String[] itemTypes;
    boolean readonlyVarAreas;
    if (stepMapping.isTemplateMapping()) {
      itemTypes = new String[0];
      readonlyVarAreas = true;
    } else {
      itemTypes = new String[] { MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN };
      readonlyVarAreas = false;
    }

    item.addToAreas(ServiceUtils.createVariableArea(view.getGenerationBaseObject(), mappingId, VarUsageType.input, identifiedVariables, Tags.MAPPING_INPUT, itemTypes, readonlyVarAreas));
    item.addToAreas(ServiceUtils.createFormulaArea(view.getGenerationBaseObject(), mappingId, stepMapping.getRawExpressions(), Tags.FORMULAS, identifiedVariables, true, false, Arrays.asList(VarUsageType.input, VarUsageType.output)));
    item.addToAreas(ServiceUtils.createVariableArea(view.getGenerationBaseObject(), mappingId, VarUsageType.output, identifiedVariables, Tags.MAPPING_OUTPUT, itemTypes, readonlyVarAreas));
  }

  public String getLabel() {
    return label;
  }


  public String getExpression() {
    return expression;
  }
  

  public Boolean getIsCondition() {
    return isCondition;
  }


  public static MappingJson hiddenQueryMapping() {
    MappingJson m = new MappingJson();
    m.isCondition = true;
    return m;
  }


  public static class MappingJsonVisitor extends EmptyJsonVisitor<MappingJson> {
    MappingJson mj = new MappingJson();

    @Override
    public MappingJson get() {
      return mj;
    }

    @Override
    public MappingJson getAndReset() {
      MappingJson ret = mj;
      mj = new MappingJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals(Tags.TYPE) ) {
        if ( !(value.equals(Tags.MAPPING)) ) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.MAPPING);
        } else {
          return;
        }
      }
      
      if( label.equals(Tags.LABEL) ) {
        mj.label = value;
        return;
      }
      if( label.equals(Tags.EXPRESSION) ) {
        mj.expression = value;
        return;
      }
      
      throw new UnexpectedJSONContentException(label);
    }

  }


}
