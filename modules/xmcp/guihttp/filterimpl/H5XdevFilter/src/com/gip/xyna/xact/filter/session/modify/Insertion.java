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
package com.gip.xyna.xact.filter.session.modify;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.ListUtils;
import com.gip.xyna.utils.collections.ListUtils.Position;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.ObjectAdapter;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.session.gb.adapter.ListAdapter;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.BranchJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.CaseJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.DistinctionJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ForeachJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.FormulaJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.FromXmlJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.InsertJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.LibJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MappingJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MemberMethodJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MemberServiceJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MemberVarJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ParallelismJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.PositionJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.PositionJson.RelativePosition;
import com.gip.xyna.xact.filter.xmom.workflows.json.QueryJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.RetryJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.TemplateJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ThrowJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.UpdateResponseJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.Catchable;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;

public class Insertion {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Insertion.class);
  
  public static enum QueryInsertStep {
    mapping, function, mappingOutput;
  }

  private final GBSubObject relativeToObject;
  private final PositionJson position;
  private QueryInsertStep queryInsertStep;
  private StepParallel newCreatedStepParallel = null; 
  
  
  //private final InsertJson insert;
  private GBSubObject insideObject;
  private EnumSet<PossibleContent> possibleContent;
  private PossibleContent actualContent;
  private String insertedObjectId;
      
  public enum PossibleContent {
    service() {
      public JsonVisitor<ServiceJson> getJsonVisitor() {
        return new ServiceJson.ServiceJsonVisitor();
      }
    },
    choice() {
      public JsonVisitor<DistinctionJson> getJsonVisitor() {
        return new DistinctionJson.DistinctionJsonVisitor();
      }
    },
    mapping() {
      public JsonVisitor<MappingJson> getJsonVisitor() {
        return new MappingJson.MappingJsonVisitor();
      }
    },
    expression() {
      public JsonVisitor<MappingJson> getJsonVisitor() {
        return new MappingJson.MappingJsonVisitor();
      }
    },
    formula() {
      public JsonVisitor<FormulaJson> getJsonVisitor() {
        return new FormulaJson.FormulaJsonVisitor();
      }
    },
    variable() {
      public JsonVisitor<VariableJson> getJsonVisitor() {
        return new VariableJson.VariableJsonVisitor();
      }
    },
    distinctionBranch() {
      public JsonVisitor<BranchJson> getJsonVisitor() {
        return new BranchJson.BranchJsonVisitor();
      }
    },
    distinctionCase() {
      public JsonVisitor<CaseJson> getJsonVisitor() {
        return new CaseJson.CaseJsonVisitor();
      }
    },
    throwStep() {
      public JsonVisitor<ThrowJson> getJsonVisitor() {
        return new ThrowJson.ThrowJsonVisitor();
      }
    },
    retryStep() {
      public JsonVisitor<RetryJson> getJsonVisitor() {
        return new RetryJson.RetryJsonVisitor();
      }
    },
    templateStep() {
      public JsonVisitor<TemplateJson> getJsonVisitor() {
        return new TemplateJson.TemplateJsonVisitor();
      }
    },
    query() {
      public JsonVisitor<QueryJson> getJsonVisitor() {
        return new QueryJson.QueryJsonVisitor();
      }
    },
    queryFilterCriterion() {
      public JsonVisitor<FormulaJson> getJsonVisitor() {
        return new FormulaJson.FormulaJsonVisitor();
      }
    },
    querySortCriterion() {
      public JsonVisitor<FormulaJson> getJsonVisitor() {
        return new FormulaJson.FormulaJsonVisitor();
      }
    },
    querySelectionMask() {
      public JsonVisitor<FormulaJson> getJsonVisitor() {
        return new FormulaJson.FormulaJsonVisitor();
      }
    },
    memberVar() {
      public JsonVisitor<MemberVarJson> getJsonVisitor() {
        return new MemberVarJson.MemberVarJsonVisitor();
      }
    },
    memberMethod() {
      public JsonVisitor<MemberMethodJson> getJsonVisitor() {
        return new MemberMethodJson.MemberMethodJsonVisitor();
      }
    },    
    staticMethod() {
      public JsonVisitor<MemberServiceJson> getJsonVisitor() {
        return new MemberServiceJson.MemberServiceJsonVisitor();
      }
    },
    lib() {
      public JsonVisitor<LibJson> getJsonVisitor() {
        return new LibJson.LibJsonVisitor();
      }
    },
    parallelism() {
      public JsonVisitor<ParallelismJson> getJsonVisitor() {
        return new ParallelismJson.ParallelismJsonVisitor();
      }
    },
    foreach(){
      public JsonVisitor<ForeachJson>getJsonVisitor(){
        return new ForeachJson.ForeachJsonVisitor();
      }
    },
    fromXml(){
      public JsonVisitor<FromXmlJson>getJsonVisitor(){
        return new FromXmlJson.FromXmlJsonVisitor();
      }
    }
    ;
    
    public abstract JsonVisitor<? extends XMOMGuiJson> getJsonVisitor();
    
    public static final EnumSet<PossibleContent> workflowStep = EnumSet.of(PossibleContent.service,
                                                                           PossibleContent.choice,
                                                                           PossibleContent.mapping,
                                                                           PossibleContent.throwStep,
                                                                           PossibleContent.templateStep,
                                                                           PossibleContent.retryStep,
                                                                           PossibleContent.query,
                                                                           PossibleContent.fromXml);
    
  }

  public Insertion(GBSubObject relativeToObject, PositionJson position) {
    this.relativeToObject = relativeToObject;
    this.position = position;
  }
  
  
  //returns the stepSerial a step should be added to,
  //creates Parallelism if necessary
  public static StepSerial wrap(PositionJson position, GBSubObject relativeToObject, StepMap stepMap) {
    StepSerial result = null;
    RelativePosition relativePosition = position.getRelativePosition();
    if ( (relativePosition != RelativePosition.left) && (relativePosition != RelativePosition.right) ) {
      return (StepSerial)relativeToObject.getStep();
    }

    // step is to be inserted parallel to relativeToObject

    if (relativeToObject.getBranchInfo() != null) {
      // step is to be inserted as a new branch within an existing parallelism

      StepParallel stepParallel = (StepParallel)relativeToObject.getStep();
      int relativeToLaneIndex = relativeToObject.getBranchInfo().getBranchNr();
      int newStepLaneIndex = (relativePosition == RelativePosition.left) ? relativeToLaneIndex : relativeToLaneIndex+1;
      addParallelismLane(stepParallel, newStepLaneIndex);

      stepMap.updateStep(stepParallel);
      result = (StepSerial) stepParallel.getChildSteps().get(newStepLaneIndex);
    } else {
      // wrapping parallelism does not exist yet and must be created

      Step relativeToStep = relativeToObject.getStep();
      if (relativeToStep instanceof Catchable) {
        // get wrapping StepCatch if existing
        relativeToStep = ((Catchable)relativeToStep).getProxyForCatch();
      }

      Step container = relativeToStep.getParentStep(); // get step the new wrapper-step is to be inserted into
      StepParallel newCreatedStepParallel = new StepParallel(container.getParentScope(), container.getCreator());
      
      newCreatedStepParallel.createEmpty();
      container.replaceChild(relativeToStep, newCreatedStepParallel);

      // make relativeToStep part of the left or right lane (depending on relativePosition) in the new StepParallel and remove it from its current parent
      int relativeToStepIndex = (relativePosition == RelativePosition.left) ? 1 : 0;
      StepSerial relativeToStepLane = (StepSerial)newCreatedStepParallel.getChildSteps().get(relativeToStepIndex);
      relativeToStepLane.addChild(0, relativeToStep);
      result = (StepSerial) newCreatedStepParallel.getChildSteps().get(1 - relativeToStepIndex);
      
      // Query
      if(relativeToStep instanceof StepCatch) {
        Step stepInTry = ((StepCatch)relativeToStep).getStepInTryBlock();
        if(stepInTry instanceof StepFunction) {
          StepMapping mapping = QueryUtils.findQueryHelperMapping(relativeToObject);
          if(mapping != null) { // it's a query
            relativeToStepLane.addChild(0, mapping);
            container.getChildSteps().remove(mapping);
          }
        }
      }

      stepMap.updateStep(container);
    }
    return result;
  }

  
  public void wrapWhenNeeded(GenerationBaseObject gbo) {
    RelativePosition relativePosition = position.getRelativePosition();
    if ( (relativePosition != RelativePosition.left) && (relativePosition != RelativePosition.right) ) {
      return;
    }

    // step is to be inserted parallel to relativeToObject

    if (relativeToObject.getBranchInfo() != null) {
      // step is to be inserted as a new branch within an existing parallelism

      StepParallel stepParallel = (StepParallel)relativeToObject.getStep();
      int relativeToLaneIndex = relativeToObject.getBranchInfo().getBranchNr();
      int newStepLaneIndex = (relativePosition == RelativePosition.left) ? relativeToLaneIndex : relativeToLaneIndex+1;
      addParallelismLane(stepParallel, newStepLaneIndex);

      gbo.getStepMap().updateStep(stepParallel);
    } else {
      // wrapping parallelism does not exist yet and must be created

      Step relativeToStep = relativeToObject.getStep();
      if (relativeToStep instanceof Catchable) {
        // get wrapping StepCatch if existing
        relativeToStep = ((Catchable)relativeToStep).getProxyForCatch();
      }

      Step container = relativeToStep.getParentStep(); // get step the new wrapper-step is to be inserted into
      newCreatedStepParallel = new StepParallel(container.getParentScope(), container.getCreator());
      
      newCreatedStepParallel.createEmpty();
      container.replaceChild(relativeToStep, newCreatedStepParallel);

      // make relativeToStep part of the left or right lane (depending on relativePosition) in the new StepParallel and remove it from its current parent
      int relativeToStepIndex = (relativePosition == RelativePosition.left) ? 1 : 0;
      StepSerial relativeToStepLane = (StepSerial)newCreatedStepParallel.getChildSteps().get(relativeToStepIndex);
      relativeToStepLane.addChild(0, relativeToStep);
      
      // Query
      if(relativeToStep instanceof StepCatch) {
        Step stepInTry = ((StepCatch)relativeToStep).getStepInTryBlock();
        if(stepInTry instanceof StepFunction) {
          StepMapping mapping = QueryUtils.findQueryHelperMapping(relativeToObject);
          if(mapping != null) { // it's a query
            relativeToStepLane.addChild(0, mapping);
            container.getChildSteps().remove(mapping);
          }
        }
      }

      gbo.getStepMap().updateStep(container);
    }
  }

  public void updateParentsWhenNeeded(GBBaseObject object, GenerationBaseObject gbo) {
    if (object.getStep() instanceof StepRetry) {
      StepCatch stepCatch = (StepCatch)relativeToObject.getStep().getParentStep();
      stepCatch.updateRetryHandlers();
    }
  }

  private static void addParallelismLane(StepParallel stepParallel, int index) {
    StepSerial newLane = new StepSerial(stepParallel.getParentScope(), stepParallel.getCreator());
    newLane.createEmpty();
    stepParallel.addChild(index, newLane);
  }

  public void inferWhere(GBSubObject object) {
    RelativePosition relativePosition = position.getRelativePosition();
    if (relativePosition == null || relativePosition == RelativePosition.inside) {
      // bleibt im relativeToObject
      insideObject = relativeToObject;
    } else if ( (relativePosition == RelativePosition.left) || (relativePosition == RelativePosition.right)) {
      // step is to be inserted parallel to relativeToObject
      
      int newStepIndex;
      if (relativeToObject.getBranchInfo() != null) {
        // step is to be inserted as a new branch within an existing parallelism
        StepParallel stepParallel = (StepParallel)relativeToObject.getStep();
        int relativeToStepIndex = relativeToObject.getBranchInfo().getBranchNr();
        newStepIndex = (relativePosition == RelativePosition.left) ? relativeToStepIndex : relativeToStepIndex+1;
        Step container = stepParallel.getChildSteps().get(newStepIndex);
        insideObject = new GBSubObject(relativeToObject.getRoot(), new ObjectId(ObjectType.step, container.getStepId()), container);
      } else {
        GBSubObject containerObject = relativeToObject.getParent();
        if (containerObject.getStep() instanceof StepCatch) {
          // step is wrapped in try-block -> get container of the try-block, instead
          containerObject = containerObject.getParent();
        }

        Step containerParent = containerObject.getParent().getStep();
        if (!(containerParent instanceof StepParallel)) {
          throw new IllegalStateException("Wrapping StepParallel is missing (should have been created by call of wrapWhenNeeded())");
        }

        StepParallel stepParallel = (StepParallel)containerParent;
        int relativeToStepIndex = stepParallel.getChildSteps().indexOf(containerObject.getStep());
        newStepIndex = (relativePosition == RelativePosition.left) ? relativeToStepIndex-1 : relativeToStepIndex+1;
        insideObject = containerObject.getSibling(newStepIndex);
      }
    } else {
      insideObject = relativeToObject.getParent();
      // übergeordnetes Objekt finden
      // TODO reicht das?
    }
    
    if(!insertingAStep(object)) {
      return;
    }
    
    
    // special case condition mapping
    GBSubObject parentStepSerial = insideObject;
    while (parentStepSerial != null && parentStepSerial.getStep() != null && !(parentStepSerial.getStep() instanceof StepSerial)) {
      parentStepSerial = parentStepSerial.getParent();
    }
    if (parentStepSerial != null && parentStepSerial.getStep() != null) {
      StepSerial stepSerial = (StepSerial) parentStepSerial.getStep();
      List<Step> children = stepSerial.getChildSteps();
      List<Step> guiList = new ArrayList<>(children);
      for (Step step : children) {
        if (step instanceof StepMapping && ((StepMapping) step).isConditionMapping()) {
          guiList.remove(step);
        }
      }
      if (queryInsertStep != null) {
        switch (queryInsertStep) {
          case mapping :
            if (position.getInsideIndex() == -1) {
              position.setNextInsideIndex(-1);
            } else if (position.getInsideIndex() == 0) {
              position.setNextInsideIndex(1);
            } else {
              Integer newMappingIndex = adjustPositionForConditionalMappings(guiList, children, object);

              if (object != null) { // move
                Integer oldMappingIndex = children.indexOf(object.getStep());
                if (oldMappingIndex < newMappingIndex && oldMappingIndex != -1) {
                  position.setNextInsideIndex(newMappingIndex);
                } else {
                  position.setNextInsideIndex(newMappingIndex + 1);
                }
              } else {
                position.setNextInsideIndex(newMappingIndex + 1);
              }
            }
            break;
          case function :
            if (position.getNextInsideIndex() != null) {
              position.setInsideIndex(position.getNextInsideIndex());
            } else if (position.getRequestInsideIndex() == 0) {
              position.setInsideIndex(1);
            }
            break;
          default :
            break;
        }
      } else {
        if (position.getInsideIndex() > 0) {
          adjustPositionForConditionalMappings(guiList, children, object);
        }
      }
    }
  }
  

  private Integer adjustPositionForConditionalMappings(List<Step> guiList, List<Step> children, GBSubObject object) {
    Step step = guiList.get(position.getInsideIndex());
    position.setInsideIndex(children.indexOf(step));

    int oldIndexOfStep = object == null ? -1: children.indexOf(object.getStep());
    boolean afterSelf = false;
    if (oldIndexOfStep != -1 && oldIndexOfStep < position.getInsideIndex()) {
      afterSelf = true;
    }

    Step previousStep = children.get(position.getInsideIndex() - 1);
    if (!afterSelf && previousStep instanceof StepMapping && ((StepMapping) previousStep).isConditionMapping()) {
      position.setInsideIndex(position.getInsideIndex() - 1);
    }

    return position.getInsideIndex();
  }


  private boolean insertingAStep(GBSubObject object) {
    if(position.getInsideIndex() == null)
      return false;
    
    if(insideObject.getType() == ObjectType.workflow)
      return false;
    
    if(object != null && object.getType() == ObjectType.variable)
      return false;
    
    if(object != null && object.getId() != null) {
      if(object.getId().getPart() == ObjectPart.input || object.getId().getPart() == ObjectPart.output) {
        return false;       
      }
    }
    
    
    return true;
  }

  public EnumSet<PossibleContent> inferPossibleContent() {
    InferContent ic = new InferContent(relativeToObject, position.getRelativePosition() );
    possibleContent = ic.inferPossibleContent();
    return possibleContent;
  }
  

  public Pair<PossibleContent, XMOMGuiJson> parseContent(InsertJson insert) throws InvalidJSONException, UnexpectedJSONContentException, XynaException {
    List<UnexpectedJSONContentException> ujes = null;
    for( PossibleContent pc : possibleContent ) {
      try {
        insert.parseContent(pc.getJsonVisitor());
        actualContent = pc;
        return Pair.of(actualContent, insert.getContent() );
      } catch( UnexpectedJSONContentException e ) {
        if( ujes == null) {
          ujes = new ArrayList<UnexpectedJSONContentException>();
        }
        ujes.add(e);
      }
    }
    //Wenn das Parsen geklappt hat, ist bereits return ...; aufgerufen worden.
    //Hier liegt also ein Fehler vor.
    if( ujes != null ) {
      UnexpectedJSONContentException thrown = null;
      for( UnexpectedJSONContentException uje : ujes ) {
        if( thrown == null ) {
          thrown = uje;
        } else {
          thrown.addSuppressed(uje);
        }
      }
      throw thrown;
    }
    throw new IllegalStateException("no possibleContent");
  }

  public Pair<PossibleContent, ? extends XMOMGuiJson> copyContent(View view, GBSubObject object) throws XPRC_InvalidServiceIdException {
    switch (object.getType()) {
      case step:
        Step step = object.getStep();
        if (step instanceof StepFunction) {
          return new Pair<>(PossibleContent.service, new ServiceJson(step));
        } else if (step instanceof StepCatch) {
          StepCatch catchStep = (StepCatch)step;
          Step caughtStep = catchStep.getStepInTryBlock();
          return new Pair<>(PossibleContent.service, new ServiceJson(caughtStep));
        } else if (step instanceof StepMapping) {
          StepMapping stepMapping = (StepMapping)step;
          if(stepMapping.isTemplateMapping()) {
            return new Pair<>(PossibleContent.templateStep, new TemplateJson(stepMapping));
          }
          return new Pair<>(PossibleContent.mapping, new MappingJson(view, (StepMapping)step));
        } else if (step instanceof StepThrow) {
          return new Pair<>(PossibleContent.throwStep, new ThrowJson(view, (StepThrow)step));
        } else if (step instanceof StepChoice) {
          StepChoice stepChoice = (StepChoice)step;
          DistinctionJson distinctionJson = new DistinctionJson();
          distinctionJson.setDistinctionType(stepChoice.getDistinctionType());
          distinctionJson.setView(view);
          return new Pair<>(PossibleContent.choice, distinctionJson);
        } else if (step instanceof StepParallel) {
          StepParallel stepParallel= (StepParallel)step;
          ParallelismJson parallelismJson = new ParallelismJson(stepParallel);
          return new Pair<>(PossibleContent.parallelism, parallelismJson);
        } else if (step instanceof StepForeach) {
          StepForeach stepForeach = (StepForeach) step;
          ForeachJson foreachJson = new ForeachJson(view, stepForeach);
          return new Pair<>(PossibleContent.foreach, foreachJson);
        }
        throw new IllegalStateException("Copying of " + step.getClass().getSimpleName() + " is not supported, yet.");
      case variable:
        VariableJson variableJson = new VariableJson(object);
        try {
          // PMOD-376 Copy des Query Outputs kopiert immer Storable
          // Label korrigieren, da es ansonsten immer vom Basistyp ist und nicht vom gecasteten Typ.
          Pair<VarUsageType, Integer> varInfo = ObjectId.parseVariableInfo(object.getId());
          if (VarUsageType.output == varInfo.getFirst() && object.getParent().getStep() instanceof StepFunction) {
            String pathAndName = variableJson.getFQName().getTypePath() + "." + variableJson.getFQName().getTypeName();
            FQName fqName = new FQName(view.getGenerationBaseObject().getRuntimeContext(), pathAndName);
            GenerationBaseObject castGbo = view.getGenerationBaseObject().getXmomLoader().load(fqName, true);
            if(castGbo != null) {
              variableJson.setLabel(castGbo.getDOM().getLabel());
            }
          }
        } catch (Exception ex) {
          Utils.logError(ex);
        }
        
        return new Pair<>(PossibleContent.variable, variableJson);
      case memberVar:
        MemberVarJson memberVarJson = new MemberVarJson(object);
        return new Pair<>(PossibleContent.memberVar, memberVarJson);
      default:
        throw new IllegalStateException("Copying of " + object.getType() + " is not supported, yet.");
    }
  }

  private enum Process {
    insert, move, transfer;
  }


  public UpdateResponseJson insert(GBBaseObject object) {
    return process(Process.insert, object, insideObject);
  }

  
  public UpdateResponseJson move(GBSubObject object) {
    GBSubObject parent = object.getParent();
    if( parent.equals(insideObject) ) {
      return process( Process.move, object, insideObject);
    } else {
      return process( Process.transfer, object, parent);
    }
  }
  
  private UpdateResponseJson process(Process process, GBBaseObject object, GBSubObject parent) {
    ObjectAdapter<?> oa = ObjectAdapter.forType(object.getType());
    if( oa == null ) {
      return null;
    }
    process(process, oa, object, parent);
    switch(parent.getType()) {
      case memberMethodsArea:
        parent.refreshIdentifiedVariables();
        break;
      default:
        break;
    }
    return new UpdateResponseJson(insideObject);//TODO für parent bei transfer ebenso?
  }
  
  private <T> void process(Process process, ObjectAdapter<T> oa, GBBaseObject object, GBSubObject parent) {
    Position<T> pos = null;
    switch( process ) {
    case insert:
      pos = ListUtils.insert( oa.getObject(object) ).
                      into( oa.getListAdapter(insideObject) );
      break;
    case move:
      pos = new ListAdapter.Move<T>(oa.getObject(object)).
                      in( oa.getListAdapter(insideObject) );
      break;
    case transfer:
      pos = ListUtils.transfer( oa.getObject(object) ).
                      from( oa.getListAdapter(parent) ).
                      to( oa.getListAdapter(insideObject) );
      break;
    }

    switch( position.getRelativePosition() ) {
      case inside:
      case right:
      case left:
        pos.at(position.getInsideIndex());
        break;
      case bottom:
        pos.after(oa.getObject(relativeToObject));
        break;
      case top:
        pos.before(oa.getObject(relativeToObject));
        break;
    }
    
  }


  private static class InferContent {

    private ObjectType objectType;
    private ObjectPart part;
    private RelativePosition relativePosition;
    
    public InferContent(GBSubObject relativeToObject, RelativePosition relativePosition) {
      this.objectType = relativeToObject != null ? relativeToObject.getType() : null;
      this.part = relativeToObject.getPart();
      this.relativePosition = relativePosition;
    }

    public EnumSet<PossibleContent> inferPossibleContent() {
      if (objectType == null) {
        // copy to clipboard
        return inferPossibleContent_InsideClipboard();
      }

      if( relativePosition == RelativePosition.inside ) {
        switch( objectType ) {
        case step:
          return inferPossibleContent_InsideStep();
        case variable:
          return inferPossibleContent_InsideVariable();
        case workflow:
          return inferPossibleContent_InsideWorkflow();
        case formulaArea:
          return inferPossibleContent_InsideFormulaArea();
        case expression:
          return inferPossibleContent_InsideFormula();
        case distinctionCase:
          return inferPossibleContent_InsideDistinctionCase();
        case branchArea:
          return inferPossibleContent_InsideBranchArea();
        case caseArea:
          return inferPossibleContent_InsideCaseArea();
        case queryFilterArea:
          return inferPossibleContent_InsideQueryFilterArea();
        case querySortingArea:
          return inferPossibleContent_InsideQuerySortingArea();
        case querySelectionMasksArea:
          return inferPossibleContent_InsideQuerySelectionMaskArea();
        case queryFilterCriterion:
          return inferPossibleContent_InsideQueryFilterCriterion(); // Variable added to Expression
        case memberVarArea:
          return inferPossibleContent_InsideMemberVarArea();
        case memberMethodsArea:
          return inferPossibleContent_InsideMemberMethodArea();
        case methodVarArea:
          return inferPossibleContent_InsideMethodVarArea();
        case libs:
          return inferPossibleContent_InsideLibs();
        default:
          throw new IllegalStateException("Unexpected objectType "+objectType);
        }
      } else {
        switch( objectType ) {
        case step:
          return inferPossibleContent_AlongsideStep();
        case variable:
          return inferPossibleContent_AlongsideVariable();
        case workflow:
          return inferPossibleContent_AlongsideWorkflow();
        case distinctionBranch:
          return inferPossibleContent_AlongsideBranch();
        default:
          throw new IllegalStateException("Unexpected objectType "+objectType);
        }
      }
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideClipboard() {
      EnumSet<PossibleContent> possibleContent = EnumSet.of(PossibleContent.variable);
      possibleContent.addAll(PossibleContent.workflowStep);

      return possibleContent;
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideStep() {
      switch( part ) {
      case all:
        //TODO genauer untersuchen, ob Step Kinder aufnehmen kann?
        return PossibleContent.workflowStep;
      case input:
        return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
      case output:
        return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
      default:
        throw new IllegalStateException("Unexpected part "+part);
      }
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideVariable() {
      throw new IllegalArgumentException(); //FIXME
    }
    
    private EnumSet<PossibleContent> inferPossibleContent_InsideWorkflow() {
      switch( part ) {
      case all:
        //TODO genauer untersuchen, ob Step Kinder aufnehmen kann?
        return PossibleContent.workflowStep;
      case input:
        return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
      case output:
        return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
      default:
        throw new IllegalStateException("Unexpected part "+part);
      }
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideFormulaArea() {
      return EnumSet.of(PossibleContent.formula, PossibleContent.expression);
    }
    
    private EnumSet<PossibleContent> inferPossibleContent_InsideQueryFilterCriterion() {
      return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideMemberVarArea() {
      return EnumSet.of(PossibleContent.memberVar);
//      return EnumSet.of(PossibleContent.memberVar, PossibleContent.variable);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideMemberMethodArea() {
      return EnumSet.of(PossibleContent.memberMethod, PossibleContent.staticMethod);
    }
    
    private EnumSet<PossibleContent> inferPossibleContent_InsideMethodVarArea() {
      return EnumSet.of(PossibleContent.variable);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideLibs() {
      return EnumSet.of(PossibleContent.lib);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideQueryFilterArea() {
      return EnumSet.of(PossibleContent.queryFilterCriterion);
    }
    
    private EnumSet<PossibleContent> inferPossibleContent_InsideQuerySortingArea() {
      return EnumSet.of(PossibleContent.querySortCriterion);
    }
    
    private EnumSet<PossibleContent> inferPossibleContent_InsideQuerySelectionMaskArea() {
      return EnumSet.of(PossibleContent.querySelectionMask);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideFormula() {
      return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideDistinctionCase() {
      return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideBranchArea() {
      return EnumSet.of(PossibleContent.distinctionBranch);
    }

    private EnumSet<PossibleContent> inferPossibleContent_InsideCaseArea() {
      return EnumSet.of(PossibleContent.distinctionCase);
    }

    private EnumSet<PossibleContent> inferPossibleContent_AlongsideStep() {
      return PossibleContent.workflowStep;
    }

    private EnumSet<PossibleContent> inferPossibleContent_AlongsideBranch() {
      return PossibleContent.workflowStep;
    }
    
    private EnumSet<PossibleContent> inferPossibleContent_AlongsideVariable() {
      if( relativePositionIn(RelativePosition.left, RelativePosition.right) ) {
        return EnumSet.of(PossibleContent.variable, PossibleContent.fromXml);
      } else {
        throw new IllegalArgumentException(); //FIXME
      }
    }

    private static EnumSet<PossibleContent> inferPossibleContent_AlongsideWorkflow() {
      throw new IllegalArgumentException(); //FIXME
    }
    
    private boolean relativePositionIn(RelativePosition ... in) {
      for( RelativePosition rp : in ) {
        if( rp == relativePosition ) {
          return true;
        }
      }
      return false;
    }

  }

  public String getInsertedObjectId() {
    return insertedObjectId;
  }

  public GBSubObject getParent() {
    return insideObject;
  }

  public void checkContent(GBSubObject object) throws UnsupportedOperationException {
    switch( object.getType() ) {
      case step:
        if( possibleContent.contains( PossibleContent.service ) ) {
          return;
        }
        break;
      case variable:
        if( possibleContent.contains( PossibleContent.variable ) ) {
          return;
        }
        break;
      case memberVar:
        if( possibleContent.contains( PossibleContent.memberVar ) ) {
          return;
        }
      case distinctionCase:
        if( possibleContent.contains( PossibleContent.distinctionCase ) ) {
          return;
        }
        break;
      case distinctionBranch:
        if( possibleContent.contains( PossibleContent.distinctionBranch ) ) {
          return;
        }
        break;
      case workflow:
        break;
      default:
        break;
    }

    throw new UnsupportedOperationException(UnsupportedOperationException.INSERT_OPERATION, "Inserting " + object.getType() + " into " + relativeToObject.getType() + " is not supported.");
  }

  
  public PossibleContent getActualContent() {
    return actualContent;
  }


  public void setActualContent(PossibleContent actualContent) {
    this.actualContent = actualContent;
  }

  
  public QueryInsertStep getQueryInsertStep() {
    return queryInsertStep;
  }

  
  public void setQueryInsertStep(QueryInsertStep queryInsertStep) {
    this.queryInsertStep = queryInsertStep;
  }

  
  public GBSubObject getInsideObject() {
    return insideObject;
  }

  
  public StepParallel getNewCreatedStepParallel() {
    return newCreatedStepParallel;
  }

}
