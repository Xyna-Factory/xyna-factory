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
package com.gip.xyna.xact.filter.session.gb.vars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables.InputConnectionProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.Connectedness;
import com.gip.xyna.xact.filter.util.AVariableIdentification.InternalGUIIdGeneration;
import com.gip.xyna.xact.filter.util.AVariableIdentification.UseAVariable;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.GlobalChoiceVarIdentification;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;

public class IdentifiedVariablesStepChoice extends IdentifiedVariablesStep implements InputConnectionProvider {
 
  private static class VarType {
    private final DomOrExceptionGenerationBase gb;
    private final boolean isList;
    
    private VarType(DomOrExceptionGenerationBase gb, boolean isList) {
      this.gb = gb;
      this.isList = isList;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((gb == null) ? 0 : gb.hashCode());
      result = prime * result + (isList ? 1231 : 1237);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      VarType other = (VarType) obj;
      if (gb == null) {
        if (other.gb != null)
          return false;
      } else if (!gb.equals(other.gb))
        return false;
      if (isList != other.isList)
        return false;
      return true;
    }
    
    
  }
  
  private final StepChoice stepChoice;
  private boolean outputCleared;
  
  //Type Choice only
  private Map<Integer, AVariableIdentification> createdVariables;

  //wird vom autosnapping (->dataflow) befüllt
  private Set<VarType> commonOutput;
  
  Map<AVariableIdentification, InputConnection> connections;

  public IdentifiedVariablesStepChoice(ObjectId id, StepChoice stepChoice) {
    super(id);
    this.stepChoice = stepChoice;
    createdVariables = new HashMap<Integer, AVariableIdentification>();
    identify();
  }

  @Override
  public void identify() {
    clearOutput();
    inputVarIdentifications = fillDirectVars(VarUsageType.input, stepChoice.getInputVars(), this);
    outputVarIdentifications = Collections.emptyList(); //outputs werden erst beim autosnapping berechnet, siehe addPossibleOutput+createOutputs

    loadCreatedVariables();
  }
  
  public void loadCreatedVariables() {
    // load created variables
    createdVariables = new HashMap<Integer, AVariableIdentification>();
    if(stepChoice.getDistinctionType() == DistinctionType.TypeChoice) {
      for(int i=0; i<stepChoice.getChildSteps().size(); i++) {
        Step childStep = stepChoice.getChildSteps().get(i);
        AVariableIdentification av = findAdditionalTypeChoiceProvider(childStep);
        if(av != null) {
          createdVariables.put(i, av);
        }
      }
    }
  }
  
  public void createOutputsAndAssigns() {
    createChoiceOutput();
  }
  
  private void createChoiceOutput() {
    List<AVariable> userOutputs = stepChoice.getUserdefinedOutput();
    int numberOfOutputs = userOutputs.size() + commonOutput.size();
    InputConnections[] inputsPerLane = new InputConnections[stepChoice.getChildSteps().size()];
    for (int j = 0; j < inputsPerLane.length; j++) {
      inputsPerLane[j] = new InputConnections(numberOfOutputs);
    }

    /*
     * outputs für die common outputs erstellen oder wiederverwenden.
     * wiederverwenden gibt es in zwei arten:
     * 1) gleiche variable wiederverwenden
     * 2) variable-id wiederverwenden
     * fall 2 gibt es, damit die vorherige zuweisung im assign beibehalten werden kann, wenn der output-typ allgemeiner wird.
     * 
     * => erstmal wird aber nur fall 1 implementiert.
     * 
     * dazu alle ehemaligen outputs berücksichtigen
     */
    final List<AVariableIdentification> list = new ArrayList<AVariableIdentification>();
    for (VarType commonOutputType : commonOutput) {
      AVariable output = getPreviousCommonOutput(commonOutputType);
      if (output == null) {
        output = stepChoice.createOutputVariable(commonOutputType.gb, commonOutputType.isList);
      }
      final GlobalChoiceVarIdentification var = GlobalChoiceVarIdentification.of(output, stepChoice);
      var.idprovider = new UseAVariable(var);
      var.internalGuiId = new InternalGUIIdGeneration() {

        @Override
        public String createId() {
          return ObjectId.createVariableId(id.getBaseId(), VarUsageType.output, indexOfNoEquals(list, var));
        }
      };

      if (stepChoice.getDistinctionType() == DistinctionType.ConditionalBranch) {
        var.setConstPermission(ConstPermission.FOR_BRANCHES);
        var.setReadonly(true);
      }

      list.add(var);
    }
    
    for (AVariable userOutput : userOutputs) {
      final GlobalChoiceVarIdentification var = GlobalChoiceVarIdentification.of(userOutput, stepChoice);
      var.idprovider = new UseAVariable(var);
      var.setConstPermission(ConstPermission.FOR_BRANCHES);
      var.setDeletable(true);
      var.internalGuiId = new InternalGUIIdGeneration() {

        @Override
        public String createId() {
          return ObjectId.createVariableId(id.getBaseId(), VarUsageType.output, indexOfNoEquals(list, var));
        }
      };
      list.add(var);
    }

    sortOutputs(list);

    outputVarIdentifications = list;
  }


  //compare order of IDs in list with assign step (output)
  private void sortOutputs(List<AVariableIdentification> list) {

    //find a lane with implementation (not merged)
    Optional<Step> s = stepChoice.getChildSteps().stream().filter(x -> x.getChildSteps() != null && x.getChildSteps().size() > 0).findAny();
    if (s.isEmpty()) {
      return;
    }

    List<Step> steps = s.get().getChildSteps();
    String[] idsOrdered = steps.get(steps.size() - 1).getOutputVarIds();

    for (int i = 0; i < idsOrdered.length; i++) {
      String id = idsOrdered[i];
      Optional<AVariableIdentification> match = list.stream()
          .filter(x -> x instanceof GlobalChoiceVarIdentification 
              && ((GlobalChoiceVarIdentification) x).idprovider != null
              && ((GlobalChoiceVarIdentification) x).idprovider.getId() != null
              && ((GlobalChoiceVarIdentification) x).idprovider.getId().equals(id))
          .findAny();
      
      if (match.isPresent()) {
        replaceInList(list, match.get(), i);
      }
    }
  }


  private void replaceInList(List<AVariableIdentification> list, AVariableIdentification item, int index) {
    int itemIndex = list.indexOf(item);
    if (itemIndex == index) {
      return;
    } else if (itemIndex < index) {
      if (index > list.size()) {
        list.add(item);
      } else {
        list.add(index, item);
      }
      list.remove(itemIndex);
    } else {
      list.add(index, item);
      list.remove(itemIndex + 1); //toRemove was pushed back by item
    }
    
  }

  private AVariable getPreviousCommonOutput(VarType commonOutputType) {
    //suche die choice outputvariable im workflow, die auf diesen typ passt
    //dazu ein assign einer lane raussuchen. darin stehen die zuweisungen auf die outputvars.
    //alle assigns zeigen auf den gleichen output. aber eventuell ist der workflow noch nicht fertig und manche lanes enthalten kein mapping auf den output
    //deshalb alle lanes ausprobieren
    for (int i = 0; i < stepChoice.getChildSteps().size(); i++) {
      StepSerial stepSerial = (StepSerial) stepChoice.getChildSteps().get(i);
      
      //ignoriere verbindene Lanes (bis auf die letzte)
      if(stepSerial.getChildSteps().size() == 0)
        continue;
      
      StepAssign assign = (StepAssign) stepSerial.getChildSteps().get(stepSerial.getChildSteps().size() - 1);
      for (AVariable avar : assign.getOutputVars()) {
        //achtung, nicht die useroutputs zurückgeben
        if (avar instanceof ServiceVariable && ((ServiceVariable) avar).isUserOutput()) {
          continue;
        }
        if (avar.getDomOrExceptionObject() == commonOutputType.gb && avar.isList() == commonOutputType.isList) {
          return avar;
        }
      }
    }
    return null;
  }


  public Map<AVariableIdentification, InputConnection> getConnections() {
    return connections;
  }
  
  @Override
  protected void add(VarUsageType usage, int index, AVariableIdentification element) {
    if (usage != VarUsageType.input) {
      if (stepChoice.getDistinctionType() != DistinctionType.ConditionalBranch) {
        throw new UnsupportedOperationException("Outputs can only be added to a Conditional Branching.");
      }

      if (element.getIdentifiedVariable().isPrototype()) {
        throw new UnsupportedOperationException("Prototype variables can't be used as user outputs.");
      }

      AVariable userOutputVar = IdentifiedVariablesStepWithWfVars.createServiceOrExceptionVar(element.getIdentifiedVariable(), element.getIdentifiedVariable().getId());
      stepChoice.addUserdefinedOutput(userOutputVar);
      createChoiceOutput();
      return;
    }

    setFunctions(element);
    setFlags(element, usage);
    if (stepChoice.getDistinctionType() == DistinctionType.TypeChoice) {
      // only one input variable for type choice
      inputVarIdentifications.clear();
      inputVarIdentifications.add(element);
      stepChoice.setTypeChoiceVar(element.getIdentifiedVariable());
    } else {
      inputVarIdentifications.add(index, element);
      stepChoice.addInputVar(index, element.getIdentifiedVariable());
      getInputConnections().addInputConnection(index);
    }

    stepChoice.parseFormulas(); // formulas need to be parsed again, because indices of input variables might have changed
  }

  @Override
  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    var.setConstPermission(ConstPermission.FOR_BRANCHES); // TODO: Ist das immer korrekt? Besser prüfen, ob var in getUserOutputs() ist, oder?
    if (stepChoice.getDistinctionType() == DistinctionType.TypeChoice) {
      // input can only be changed via dynamic typing
      var.setDeletable(false);
      var.setReadonly(true);
    } else {
      var.setDeletable(true);
      var.setReadonly(false);
    }
  }

  private void setFunctions(final AVariableIdentification var) {
    var.connectedness = new Connectedness() {

      @Override
      public boolean isUserConnected() {
        return getInputConnections().getUserConnected()[indexOfNoEquals(inputVarIdentifications, var)];
      }

      @Override
      public String getConnectedVariableId() {
        return getInputConnections().getVarIds()[indexOfNoEquals(inputVarIdentifications, var)];
      }

      @Override
      public boolean isConstantConnected() {
        return getInputConnections().getConstantConnected()[indexOfNoEquals(inputVarIdentifications, var)];
      }

    };
  }

  @Override
  protected AVariableIdentification remove(VarUsageType usage, int index) {
    AVariableIdentification varToRemove = outputVarIdentifications.get(index);
    if (stepChoice.getDistinctionType() != DistinctionType.ConditionalBranch ||
        usage != VarUsageType.output ||
        !varToRemove.getIdentifiedVariable().isUserOutput()) {
      throw new UnsupportedOperationException("Only user-outputs of a conditional branching can be deleted.");
    }

    stepChoice.removeUserdefinedOutput(varToRemove.getIdentifiedVariable());
    createChoiceOutput();

    return varToRemove;
  }

  public void clearOutput() {
    commonOutput = new HashSet<>();
    connections = new HashMap<AVariableIdentification, InputConnection>();
    outputCleared = true;
  }
  
  private static final Comparator<VarType> COMPARATOR_HIERARCHY_DEPTH = new Comparator<VarType>() {

    @Override
    public int compare(VarType o1, VarType o2) {
      int depthDiff = -Integer.compare(hierarchyDepth(o1), hierarchyDepth(o2));
      if (depthDiff == 0) {
        
        //AnyType
        if(o1.gb == null && o2.gb == null && o1.isList == o2.isList)
          return 0;
        
        if(o1.gb == null)
          return -1;
        
        if(o2.gb == null)
          return 1;
        
        return o1.gb.getFqClassName().compareTo(o2.gb.getFqClassName());
      }
      return depthDiff;
    }

    private int hierarchyDepth(VarType o) {
      DomOrExceptionGenerationBase doe = o.gb;
      int depth = 0;
      
      //AnyType
      if(doe == null)
        return 0;
      
      while (doe.hasSuperClassGenerationObject()) {
        doe = doe.getSuperClassGenerationObject();
        depth++;
      }
      return depth * 2 + (o.isList ? 1 : 0);
    }
    
  };

  /*
   * Pfeile sind Ableitungen
   * A -> A1 -> A11
   *   -> A2
   * B -> B1
   * 
   * commonoutput = (A,B)
   * laneoutput = (A,C)
   * => commonoutput = (A)
   * kurz: (A,B) + (A,C) => (A)
   *
   * es ergeben sich folgende regeln:
   * (A,B) + (A,C) => (A)
   *   B,C kommen nicht vor 
   * (A,B) + (A1) => (A)
   *   B kommt nicht vor, A ist in beiden vorhanden
   * (A,A1) + (A,A2) => (A)
   *   zweimal A nicht erlaubt (dann muss man manuelle outputs definieren)
   * (B,A1) + (B,A2) => (A,B)
   * (A11) + (A2) => (A)
   * (A11,A2) + (A2) => (A)
   *   (A2) wäre auch denkbar, aber lieber eine ambiguität mehr als einen fall, wo man manuellen output benötigt
   * (A11,A21) + (A12,A3) => (A1,A)
   * 
   */
  //wird von autosnapping aufgerufen
  public void addPossibleOutput(List<AVariableIdentification> singleLaneOutput, int branchIndex) {
    if (singleLaneOutput == null) {
      return; //lane für berechnung von common output ignorieren (lane endet mit throw)
    }
    if( commonOutput.isEmpty() ) {
      if( outputCleared ) {
        commonOutput = fillLaneOutput(singleLaneOutput);
        outputCleared = false;
        return;
      } else {
        //zu uneinheitlicher Output, daher kein globaler Output
        return;
      }
    }
    /*
    * Algorithmus:
    * für jeden typ aus Source1: (sortierung nach hierarchie-tiefe)
    *   1: gibt es typ in Source2? => typ in Result adden und in beiden Sources removen
    *   2: gibt es subtyp von typ in Source2? => typ in Result adden und in beiden Sources removen
    *     else
    *       ersetze typ durch basistyp
    *       goto 1 (sortierung berücksichtigen!)
     */
    SortedSet<VarType> sortedSetCommon = new TreeSet<>(COMPARATOR_HIERARCHY_DEPTH);
    sortedSetCommon.addAll(commonOutput);
    commonOutput.clear();
    SortedSet<VarType> sortedSetLane = fillLaneOutput(singleLaneOutput);
   
    while (!sortedSetCommon.isEmpty() && !sortedSetLane.isEmpty()) {
      VarType v = sortedSetCommon.first();
      if (sortedSetLane.remove(v)) {
        sortedSetCommon.remove(v);
        
        //if there is a subType of us already in commonOutput, remove it.
        Set<VarType> commonOutputCpy = new HashSet<VarType>(commonOutput);
        for(VarType candidate : commonOutputCpy) {
          if(candidate.isList != v.isList) {
            continue;
          }
          
          if(v.gb != null && candidate.gb != null && DomOrExceptionGenerationBase.isSuperClass(v.gb, candidate.gb)) {
            commonOutput.remove(candidate);
          }
        }
        
        //add us after removing subTypes
        commonOutput.add(v);
        
      } else {
        boolean foundSubtype = false;
        for (VarType laneOutput : sortedSetLane) {
          
          if(laneOutput.isList != v.isList) {
            continue;
          }
          
          if (v.gb != null && laneOutput.gb != null && DomOrExceptionGenerationBase.isSuperClass(v.gb, laneOutput.gb)) {
            sortedSetLane.remove(laneOutput);
            sortedSetCommon.remove(v);
            commonOutput.add(v);
            foundSubtype = true;
            break;
          }
        }
        if (!foundSubtype) {
          sortedSetCommon.remove(v);
          
          //AnyType -- we won't find a something more generic
          if(v.gb == null)
            continue;
          
          DomOrExceptionGenerationBase supertype = v.gb.getSuperClassGenerationObject();
          if (supertype != null) {
            //do not add superType, if there is a subType of it already
            boolean subtypeAlreayInResult = false;
            
            for(VarType vt : commonOutput) {
              if(DomOrExceptionGenerationBase.isSuperClass(supertype, vt.gb) && vt.isList == v.isList) {
                subtypeAlreayInResult = true;
                break;
              }
            }
            
            if(!subtypeAlreayInResult) {
              sortedSetCommon.add(new VarType(supertype, v.isList));
            }
          } 
        }
      }
    }
  }


  private SortedSet<VarType> fillLaneOutput(List<AVariableIdentification> singleLaneOutput){
    SortedSet<VarType> result = new TreeSet<VarType>(COMPARATOR_HIERARCHY_DEPTH);
    for (AVariableIdentification laneOutput : singleLaneOutput) {
      VarType varType = new VarType(laneOutput.getIdentifiedVariable().getDomOrExceptionObject(), laneOutput.getIdentifiedVariable().isList());
      result.add(varType);
    }
    
    return result;
  }  
  
  
  
  private AVariableIdentification findAdditionalTypeChoiceProvider(Step childStep) {
    if(childStep.getChildSteps().size() <= 1)
      return null;
    
    
    StepAssign sa = ((StepSerial)childStep).findFirstAssign();
    
    //if there is only one Assign step, there is no additional Type Choice Provider
    if(childStep.getChildSteps().get(childStep.getChildSteps().size()-1) == sa) {
      return null;
    }
    
    VariableIdentification additionalProvider = null;
    ReferencedVarIdentification ex = null;
    if(sa != null)
    {
      if(sa.getOutputVarIds().length != 1)
        return null;
      
      String id = sa.getOutputVarIds()[0];
      try {
         additionalProvider = childStep.getParentScope().identifyVariable(id);
      } catch (XPRC_InvalidVariableIdException e) {
        return null;
      }
      ex = ReferencedVarIdentification.of(additionalProvider.getVariable()); //TODO: get Variable instead of creating it?
      ex.idprovider = () -> id;
    }
      
    return ex;
  }

  @Override
  public InputConnections getInputConnections() {
    return stepChoice.getInputConnections();
  }


  public List<AVariableIdentification> getUserOutputs() {
    List<AVariableIdentification> userOutputs = new ArrayList<>();
    int n = outputVarIdentifications.size();
    int cntUser = stepChoice.getUserdefinedOutput().size();
    for (int i = n - cntUser; i < n; i++) {
      userOutputs.add(outputVarIdentifications.get(i));
    }
    return userOutputs;
  }


  public List<AVariableIdentification> getCommonOutputs() {
    List<AVariableIdentification> commonOutputs = new ArrayList<>();
    int n = outputVarIdentifications.size();
    int cntUser = stepChoice.getUserdefinedOutput().size();
    for (int i = 0; i < n - cntUser; i++) {
      commonOutputs.add(outputVarIdentifications.get(i));
    }
    return commonOutputs;
  }
  
  //removes all variables except the last one - per type (hierarchy)
  //knows where to cut hierarchies by looking into commonOutput
  public List<AVariableIdentification> pruneToLastOutput(List<AVariableIdentification> candidates){
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>();
    
    //we start from the back to add the last occurrence first
    for(int i= candidates.size() -1; i>=0; i--) {
      AVariableIdentification c = candidates.get(i);
      DomOrExceptionGenerationBase doe = c.getIdentifiedVariable().getDomOrExceptionObject();
      
      //if there is a relative in result, do not add it.
      boolean containsRelative = false;
      List<AVariableIdentification> resultCpy = new ArrayList<AVariableIdentification>(result);
      for(AVariableIdentification potentialRelative : resultCpy) {
        //only consider variables that match list-wise
        if(c.getIdentifiedVariable().isList() != potentialRelative.getIdentifiedVariable().isList()) {
          continue;
        }
        
        DomOrExceptionGenerationBase otherDoe = potentialRelative.getIdentifiedVariable().getDomOrExceptionObject();
        //if we already have an AnyType, do not add another
        if(doe == null && otherDoe == null) {
          containsRelative = true;
          break;
        }
        
        //do not compare AnyType with a different type
        if(doe == null || otherDoe == null) {
          continue;
        }
        
        //see if they are in the same type hierarchy (one is superclass of the other)
        if(DomOrExceptionGenerationBase.isSuperClass(doe, otherDoe) || DomOrExceptionGenerationBase.isSuperClass(otherDoe, doe)) {
          containsRelative = true;
          break;
        }
        
        //see if they are in the same type hierarchy (common superclass)
        for(VarType output : commonOutput) {
          if(output.isList != c.getIdentifiedVariable().isList()) {
            continue;
          }
          
          if(DomOrExceptionGenerationBase.isSuperClass(output.gb, doe) && DomOrExceptionGenerationBase.isSuperClass(output.gb, otherDoe)) {
            containsRelative = true;
            break;
          }
        }
        
        if(containsRelative) {
          break;
        }
      }
      
      //if this is a variable of a type we have not seen yet, add it to result.
      if(!containsRelative) {
        result.add(c);
      }
    }
    
    return result;
  }
  
  public void addCreatedVariable(int lane, AVariableIdentification var) {
    createdVariables.put(lane, var);
  }
  
  public Map<Integer, AVariableIdentification> getCreatedVariables() {
    return createdVariables;
  }
  
  public AVariableIdentification getAVariableIdentification(int lane) {
    if(stepChoice.getDistinctionType() != DistinctionType.TypeChoice) {
      return null;
    }
    
    //we have not created a variable for this lane (yet)
    if(!createdVariables.containsKey(lane)) {
      return null;
    }
    
    return createdVariables.get(lane);
  }
  
  public void removeCreatedVariable(int lane) {
    createdVariables.remove(lane);
  }
  
  @Override
  public Step getStep() {
    return stepChoice;
  }


  @Override
  public AVariableIdentification getVariable(VarUsageType usage, int index) {
    boolean requestForFakeVariableTypeChoice =
        stepChoice.getDistinctionType() == DistinctionType.TypeChoice 
        && index != 0 
        && usage == VarUsageType.input;
    boolean requestForFakeVariableCoinditionalChoice =
        stepChoice.getDistinctionType() == DistinctionType.ConditionalChoice 
        && index >= stepChoice.getInputVarIds().length
        && usage == VarUsageType.input;


    if (requestForFakeVariableTypeChoice) {
      // return fake variable for case
      AVariableIdentification varIdent = getAVariableIdentification(index - 1);
      if (varIdent != null) {
        return varIdent;
      }

      // fake variable hasn't been created by dataflow yet, because it hasn't been used in a case -> create new one
      AVariable var = stepChoice.createInputVariableForBranch(index - 1);
      varIdent = DirectVarIdentification.of(var);
      varIdent.idprovider = () -> var.getId();

      return varIdent;
    } else if (requestForFakeVariableCoinditionalChoice) {
      return DirectVarIdentification.of(null);
    }

    //return regular variable
    return super.getVariable(usage, index);
  }


  public void setOutputVarIdentification(List<AVariableIdentification> newOutputVarIdentification) {
    outputVarIdentifications = newOutputVarIdentification;
  }
  
}
