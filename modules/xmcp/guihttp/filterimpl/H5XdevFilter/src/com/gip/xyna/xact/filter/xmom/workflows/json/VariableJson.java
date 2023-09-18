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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.vars.ConstPermission;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepFunction;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesWorkflow;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;

import xmcp.processmodeller.datatypes.Data;
import xmcp.processmodeller.datatypes.Variable;

public class VariableJson extends XMOMGuiJson implements HasXoRepresentation {
  
  private static final String SERVER_EXCEPTION_LABEL = "Server Exception";
  private static final String THIS_VAR_NAME = "this";

  private static final Logger logger = CentralFactoryLogging.getLogger(VariableJson.class);
  
  private boolean request;
  //FIXME ugly threadlocals for now until we find a accessor 
  public static ThreadLocal<Dataflow> dataflowInjector = new ThreadLocal<>();
  
  private String varId;
  private AVariableIdentification var;
  private String type;
  private String label;
  private FQNameJson fqName;
  private boolean isList;
  private boolean isPrototype;
  private boolean isThisVar;
  private boolean readonly;
  private boolean deletable;
  
  private String castToFqn = null;
  private String castLabel = null;

  private VariableJson() {
    this.request = true;
  }
  

  public VariableJson(String type, String label, FQNameJson fqName) {
    this.type = type;
    this.label = label;
    this.fqName = fqName;
  }
  
  public VariableJson(GBSubObject object) {
    this.request = false;
    this.varId = object.getObjectId();
    this.var = object.getVariable().getVariable();
    this.type = var.getIdentifiedVariable() instanceof ExceptionVariable ? Tags.EXCEPTION : Tags.VARIABLE;
    this.label = object.getVariable().getVariable().getIdentifiedVariable().getLabel();
    this.fqName = new FQNameJson(var.getIdentifiedVariable().getOriginalPath(), var.getIdentifiedVariable().getOriginalName());
    this.isList = var.getIdentifiedVariable().isList();
    this.isPrototype = var.getIdentifiedVariable().isPrototype();
    this.readonly = var.isReadonly();
    this.deletable = var.isDeletable();
   // this.usage = object.getVariable().getUsage();
  }

  private VariableJson(AVariableIdentification var, Pair<String, String> signaturePathAndName, String castToFqn, GenerationBaseObject gbo, boolean isThisVar) {
    this.request = false;
    this.varId = var.internalGuiId.createId();
    this.var = var;
   /* try {
      this.usage = ObjectId.parse(varId).getPart().asUsage();
    } catch (UnknownObjectIdException e) {
      logger.warn("unexpected object id: " + varId);
      this.usage = null;
    }*/
    this.isPrototype = var.getIdentifiedVariable().isPrototype();
    if (!isPrototype) {
      this.fqName = new FQNameJson(signaturePathAndName.getFirst(), signaturePathAndName.getSecond());
    }
    this.castToFqn = castToFqn;
    this.isThisVar = isThisVar;
    this.readonly = var.isReadonly();
    this.deletable = var.isDeletable();
    if(castToFqn != null && !castToFqn.isEmpty() && !DatatypeVariable.ANY_TYPE.equals(castToFqn)) {
      try {
        FQNameJson fqNameJson = FQNameJson.ofPathAndName(castToFqn);
        GenerationBaseObject castGbo = gbo.getXmomLoader().load(new FQName(gbo.getRuntimeContext(), fqNameJson.getTypePath() + "." + fqNameJson.getTypeName()), true);
        this.castLabel = castGbo.getGenerationBase().getLabel();
      } catch (XynaException ex) {
        Utils.logError(ex);
      }
    }
  }


  public String getType() {
    return type;
  }

  public void setLabel(String label) {
    this.label = label;
  }
  
  public String getLabel() {
    return label;
  }
  public boolean isList() {
    return isList;
  }
  public boolean isPrototype() {
    return isPrototype;
  }
  public FQNameJson getFQName() {
    return fqName;
  }


  public void setList(boolean isList) {
    this.isList = isList;
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    Variable variable;
    if (var.getIdentifiedVariable() instanceof ExceptionVariable) {
      variable = new xmcp.processmodeller.datatypes.exception.Exception();
    } else {
      variable = new Data();
    }
    if(request) {
      variable.setLabel(label);
      variable.setIsList(isList);
    } else {
      variable.setId(varId);
      variable.setIsList(var.getIdentifiedVariable().isList());
      variable.setCastToFqn(castToFqn);
      variable.setDeletable(deletable);
      variable.setReadonly(readonly);
      variable.setAllowCast(var.getAllowCast());

      if (!Utils.variableExists(var.getIdentifiedVariable())) {
        variable.setAllowConst(ConstPermission.NEVER.name());
      } else {
        variable.setAllowConst(var.getConstPermission().name());
      }

      try {
        variable.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(var.getIdentifiedVariable().getRevision()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // nothing
      }

      String label = var.getIdentifiedVariable().getLabel();
      if (label != null && !label.isEmpty()) {
        variable.setLabel(label);
      } else if (var.getIdentifiedVariable().getFQClassName().equals("com.gip.xyna.utils.exceptions.XynaException")) {
        variable.setLabel(SERVER_EXCEPTION_LABEL);
      } else {
        variable.setLabel(var.getIdentifiedVariable().getOriginalName());
      }
      if(castToFqn != null && !castToFqn.isEmpty()
          && castLabel != null && !castLabel.isEmpty()) {
        variable.setLabel(castLabel);
      }

      if(var.getIdentifiedVariable().getCreator() instanceof DOM) { 
        variable.setName(isThisVar ? THIS_VAR_NAME : var.getIdentifiedVariable().getVarName());
      }
    }
    if(fqName != null && Utils.variableExists(var.getIdentifiedVariable())) {
      variable.setFqn(fqName.toString());
    }
    
    variable.setIsAbstract(isPrototype || !Utils.variableExists(var.getIdentifiedVariable()));
    return variable;
  }

  public static class VariableJsonVisitor extends EmptyJsonVisitor<VariableJson> {
    VariableJson vj = new VariableJson();

    @Override
    public VariableJson get() {
      return vj;
    }
    @Override
    public VariableJson getAndReset() {
      VariableJson ret = vj;
      vj = new VariableJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals("type") ) {
        if ( !Tags.VARIABLE.equals(value) && !Tags.EXCEPTION.equals(value) ) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.VARIABLE + " or " + Tags.EXCEPTION);
        }
        vj.type = value;
        return;
      }
      if( label.equals(Tags.LABEL) ) {
        vj.label = value;
        return;
      }
      if( label.equals(Tags.IS_LIST) ) {
        vj.isList = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.IS_PROTOTYPE) ) {
        vj.isPrototype = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.CAST_TO_FQN) ) {
        vj.castToFqn = value;
        return;
      }
      if( FQNameJson.useLabel(label) ) {
        vj.fqName = FQNameJson.parseAttribute(vj.fqName, Tags.FQN, value);
        return;
      }
      throw new UnexpectedJSONContentException(label);
    }

  }
  
  public static List<VariableJson> toList(IdentifiedVariables identifiedVariables, VarUsageType usage, GenerationBaseObject gbo, Boolean overrideVariablesDeleteableState) {
    List<VariableJson> list = new ArrayList<VariableJson>();
    boolean input = false;
    if( usage == VarUsageType.input || usage == VarUsageType.output ) {
      input = usage == VarUsageType.input;
      if( identifiedVariables instanceof IdentifiedVariablesWorkflow ) {
        input = ! input;
      }
    }
    
    List<AVariableIdentification> variableIdentifications = identifiedVariables.getVariables(usage);
    if(variableIdentifications != null) {
      for (int i = 0; i < variableIdentifications.size(); i++) {
        AVariableIdentification var = variableIdentifications.get(i);

        Pair<String, String> signaturePathAndName = null;
        if (!var.getIdentifiedVariable().isPrototype()) {
          signaturePathAndName = identifiedVariables.getSignaturePathAndName(usage, i);
        }
  
        String castToFqn = null;
        if (identifiedVariables instanceof IdentifiedVariablesStepFunction) {
          IdentifiedVariablesStepFunction identifiedVariablesStepFunction = (IdentifiedVariablesStepFunction)identifiedVariables;
          castToFqn = identifiedVariablesStepFunction.getVarCastToFqn(usage, i);
        }

        boolean isThisVar = ( (var.getIdentifiedVariable().getCreator() instanceof DOM) &&
                              (gbo.getViewType() == com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type.dataType) &&
                              (identifiedVariables.getVariables(VarUsageType.input)).indexOf(var) == 0 ); 
        VariableJson variableJson = new VariableJson(var, signaturePathAndName, castToFqn, gbo, isThisVar);
        if(overrideVariablesDeleteableState != null) {
          variableJson.deletable = overrideVariablesDeleteableState;
        }
        list.add(variableJson);
      }
    }
    return list;
  }
}
