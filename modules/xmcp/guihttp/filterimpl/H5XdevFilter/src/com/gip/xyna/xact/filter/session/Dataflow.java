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
package com.gip.xyna.xact.filter.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.StepMap.RecursiveVisitor;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepChoice;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepForeach;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepFunction;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepThrow;
import com.gip.xyna.xact.filter.session.workflowwarnings.DataflowConnectionWarningsManagement;
import com.gip.xyna.xact.filter.session.workflowwarnings.WorkflowWarningsHandler;
import com.gip.xyna.xact.filter.session.workflowwarnings.DataflowConnectionWarningsManagement.NewProvidersForConnectionData;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.Connectedness;
import com.gip.xyna.xact.filter.util.AVariableIdentification.ThrowExceptionIdProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification.UseAVariable;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.GlobalChoiceVarIdentification;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType.DispatchingParameter;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ForEachScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.SpecialPurposeIdentifier;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch.CatchBranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction.RemoteDespatchingParameter;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xmcp.processmodeller.datatypes.Connection;

//ermittlung von linkstates für alle variablen eines workflows (~autosnapping). die linkstates werden dann gespeichert in dataflowentry-listen
//das anwenden der neuen linkstates auf die xml-repräsentation (generationbase+steps) passiert erst beim speichern des workflows
public class Dataflow {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Dataflow.class);
  
  private static final RemoteDestinationManagement rdMgmt =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
  
  
  private GenerationBaseObject gbo;
  //zwischenspeicher während der berechnung
  private Map<AVariableIdentification, InputConnection> inputConnections;
  private Set<AVariableIdentification> missingVarIds = new HashSet<AVariableIdentification>();
  private Set<AVariableIdentification> consumedVars = new HashSet<AVariableIdentification>();
  
  //ergebnis der berechnung
  private List<DataflowEntry> dataflow = new ArrayList<DataflowEntry>();
  private List<DataflowEntry> rewritten = new ArrayList<DataflowEntry>();
  private List<DataflowEntry> removed = new ArrayList<DataflowEntry>();
  private List<DataflowEntry> problems;
  
  //for every stepFunction, this map contains AVariableIdentifications for remoteDispatchingParameters
  private HashMap<StepFunction, HashMap<Integer, AVariableIdentification>> remoteDispatchingParameterMap = new HashMap<StepFunction, HashMap<Integer,AVariableIdentification>>();
  
  
  private DataflowConnectionWarningsManagement warningsManagement;
  
  private Set<StepForeach> createdForeaches = new HashSet<StepForeach>();
  
  private boolean debug;
  
  
  public static interface InputConnection {
    public SimpleConnection getConnectionForLane(int laneNr);
    public List<SimpleConnection> getConnectionsPerLane();
  }


  public static class SimpleConnection implements InputConnection {
    private Linkstate linkstate;
    private String branchId;
    private String constant;

    /**
     * contains:
     * 0  entries when nothing is connected to the output (linkstate: none)
     * 1  entry   when there is an unambiguous connection to the output (linkstate: auto, user or constant)
     * >1 entries when there are multiple candidates for connections to the output (linkstate: ambigue)
     */
    private List<AVariableIdentification> inputVars;


    public SimpleConnection(List<AVariableIdentification> inputVars, Linkstate linkstate, String branchId, String constant) {
      this.inputVars = inputVars;
      this.linkstate = linkstate;
      this.branchId = branchId;
      this.constant = constant;
    }

    public SimpleConnection(Linkstate linkstate) {
      this(new ArrayList<AVariableIdentification>(), linkstate, null, null);
    }


    public void setLinkState(Linkstate linkstate) {
      this.linkstate = linkstate;
    }

    public Linkstate getLinkState() {
      return linkstate;
    }

    public void setBranchId(String branchId) {
      this.branchId = branchId;
    }

    public String getBranchId() {
      return branchId;
    }
    
    public void setConstant(String constant) {
      this.constant = constant;
    }
    
    public String getConstant() {
      return constant;
    }

    public void addInputVar(AVariableIdentification inputVar) {
      if(!inputVars.contains(inputVar))
        inputVars.add(inputVar);
    }

    public AVariableIdentification getInputVar(int varNr) {
      if ( (inputVars != null) && (inputVars.size() > varNr) ) {
        return inputVars.get(varNr);
      } else {
        return null;
      }
    }

    public void setInputVars(List<AVariableIdentification> inputVars) {
      this.inputVars = inputVars;
    }
    
    public List<AVariableIdentification> getInputVars() {
      return inputVars;
    }

    @Override
    public SimpleConnection getConnectionForLane(int laneNr) {
      return this;
    }

    @Override
    public List<SimpleConnection> getConnectionsPerLane() {
      List<SimpleConnection> connectionsPerLane = new ArrayList<SimpleConnection>();
      connectionsPerLane.add(this);
      
      return connectionsPerLane;
    }

    //ist die verbindung derart, dass ein foreach dafür vorhanden sein muss
    public boolean foreachLink(AVariableIdentification target, int sourceIdx) {
      if (target.getIdentifiedVariable().isList()) {
        return false;
      }
      AVariableIdentification iv = getInputVar(sourceIdx);
      if (iv == null) {
        return false;
      }
      
      if(iv instanceof ReferencedVarIdentification) 
        return ((ReferencedVarIdentification)iv).isForeachOutput() || iv.getIdentifiedVariable().isList();
      
      return iv.getIdentifiedVariable().isList();
    }


    @Override
    public String toString() {
      String ids = String.join(",", inputVars.stream().map(x -> mapToId(x)).collect(Collectors.toList()));
      return "SimpleConnection[" + linkstate.toString() + " - " + ids + "]";
    }
    
    private String mapToId(AVariableIdentification avarIdent) {
      return avarIdent.idprovider != null && !(avarIdent.idprovider instanceof ThrowExceptionIdProvider) ? avarIdent.idprovider.getId() : "null";
    }

  }


  public static class MultiLaneConnection implements InputConnection {
    /**
     * contains one simple connection info per lane bei choices
     * bei catchblöcken gibt es für die outputs auch noch den fall "branchId==null" für den fall, dass kein branch selektiert ist.
     */
    private List<SimpleConnection> connectionsPerLane;
    private List<String> branchIds = new ArrayList<>();
    private int idxBranchIdIsNull = -1; //idx in liste, wo branchId null ist

    public MultiLaneConnection(List<SimpleConnection> connectionsPerLane) {
      this.connectionsPerLane = connectionsPerLane;
    }
    
    public List<String> getBranchIds() {
      return branchIds;
    }
    
    public MultiLaneConnection() {
      this(new ArrayList<SimpleConnection>()); 
    }


    public AVariableIdentification getInputVar(int laneNr, int varNr) {
      SimpleConnection sc = getNthConnectionBranchNotNull(laneNr);
      if (sc == null) {
        return null;
      }
      return sc.getInputVar(varNr);
    }

    private SimpleConnection getNthConnectionBranchNotNull(int n) {
      if (idxBranchIdIsNull != -1 && n >= idxBranchIdIsNull) {
        n++;
      }
      if (connectionsPerLane.size() <= n) {
        return null;
      }
      return connectionsPerLane.get(n);
    }

    public void addLaneConnection(SimpleConnection connection, String branchId) {
      connectionsPerLane.add(connection);
      branchIds.add(branchId);
      if (branchId == null) {
        idxBranchIdIsNull = branchIds.size();
      }
    }

    @Override
    public SimpleConnection getConnectionForLane(int laneNr) {
      if (laneNr == -1) {
        return getSimpleConnectionByBranchId(null);
      }
      SimpleConnection sc = getNthConnectionBranchNotNull(laneNr);
      if (sc == null) {
        return new SimpleConnection(LinkstateIn.NONE);
      }
      return sc;
    }

    @Override
    public List<SimpleConnection> getConnectionsPerLane() {
      return connectionsPerLane;
    }

    public SimpleConnection getSimpleConnectionByBranchId(String branchId) {
      for (int i = 0; i < branchIds.size(); i++) {
        if (Objects.equals(branchIds.get(i), branchId)) {
          return connectionsPerLane.get(i);
        }
      }
      return null;
    }

    public boolean removeBranch(String branchId) {
      int branchIdxToRemove = branchIds.indexOf(branchId);
      if (branchIdxToRemove < 0) {
        return false;
      }

      connectionsPerLane.remove(branchIdxToRemove);
      branchIds.remove(branchIdxToRemove);

      if (idxBranchIdIsNull > branchIdxToRemove) {
        idxBranchIdIsNull--;
      }

      return true;
    }
  }

  public static enum LinkstateIn implements Linkstate {
    AUTO("auto"),
    USER("user"),
    AMBIGUE("ambigue"),
    CONSTANT("constant"),
    INPUTSOURCE("orderInputSource"),
    //variable muss nicht verbunden sein, z.b. ein workflow-output im fall der selektion eines catchblockes, der mit einem throw endet
    NO_NEED("notneeded"),
    NONE("none");
    
    private final String value;
    
    private LinkstateIn(String value) {
      this.value = value;
    }

    public String toValue() {
      return value;
    }
    
  }
  
  public static enum LinkstateOut implements Linkstate {
    CONNECTED("connected"),
    NONE("none");
    
    private final String value;
    
    private LinkstateOut(String value) {
      this.value = value;
    }

    public String toValue() {
      return value;
    }
    
  }
  
  public static interface Linkstate {
    
    String toValue();
    
  }

  public Dataflow(GenerationBaseObject gbo) {
    warningsManagement = new DataflowConnectionWarningsManagement(this);
    warningsManagement.setWorkflowWarningsHandler(new WorkflowWarningsHandler.EmptyWorkflowWarningsHandler());
    this.gbo = gbo;
    resetDataflow();
  }

  private void resetDataflow() {
    this.problems = new ArrayList<DataflowEntry>();
    this.inputConnections = analyzeDataflow(gbo.getWorkflow());
    rewriteDataflow();
  }

  public Map<AVariableIdentification, InputConnection> analyzeDataflow(WF wf) {
    logger.debug("--- Starting analyzeDataflow ---");
    warningsManagement.prepareRun();
    createdForeaches.clear();
    Map<AVariableIdentification, InputConnection> connections = new HashMap<>();
    List<AVariableIdentification> providers = new ArrayList<>();
    consumedVars.clear();
    
    IdentifiedVariables identifiedVariables = gbo.identifyVariables( new ObjectId(ObjectType.workflow, null));
    
    providers.addAll(identifiedVariables.getVariables(VarUsageType.input));
    providers = analyzeDataflow(wf.getWfAsStep(), providers, connections);
    getOrCalculateLinkStatesWFOutput(wf, providers, connections);
    
    if (logger.isDebugEnabled()) {
      logger.debug(" - Returning '" + connections.size() + "'Connections: - ");
      for (Entry<AVariableIdentification, InputConnection> con : connections.entrySet()) {
        logger.debug("Connection for: '" + con.getKey().getIdentifiedVariable().getId() + "': " + con.getKey().getIdentifiedVariable().getLabel());
        logger.debug("  LinkState: " + con.getValue().getConnectionForLane(0).getLinkState().toString());
        for (AVariableIdentification av : con.getValue().getConnectionForLane(0).getInputVars()) {
          logger.debug("  Connected to: " + av.getIdentifiedVariable().getId() + " (" + av.getIdentifiedVariable().getLabel() + ")");
        }
      }
    }
    
    clearUnusedConstants(wf, connections);
    
    warningsManagement.finishRun();
    logger.debug("--- Finished analyzeDataflow ---");
    inputConnections = connections;
    rewriteDataflow();
    return connections;
  }
  
  
  private void clearUnusedConstants(WF wf, Map<AVariableIdentification, InputConnection> connections) {
    List<AVariable> usedConstants = new ArrayList<AVariable>();
    
    //collect all used constants
    for(Entry<AVariableIdentification, InputConnection> s : connections.entrySet()) {
      InputConnection iCon = s.getValue();
      for(SimpleConnection sCon : iCon.getConnectionsPerLane()) {
        if(sCon.getLinkState() == LinkstateIn.CONSTANT) {
          usedConstants.add(sCon.getInputVar(0).getIdentifiedVariable());
        }
      }
    }
    wf.getWfAsStep().getChildStep().removeUnusedConstants(usedConstants);
  }
  
  private void logInputs(Step currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections) {
    logger.debug("analyzing Step: " + currentStep.getClass() + " (" + currentStep.getStepId() + ")");
    logger.debug("  there are '" + providers.size() + "' providers for this step");
    for(AVariableIdentification av : providers) {
      if(av == null) {
        logger.debug("provider null!");
        continue;
      }
      AVariable ava = av.getIdentifiedVariable();
      logger.debug("    " + ava.getId() +
                   " " + ava.getLabel() +
                   " " + ava.getOriginalPath() + "." + ava.getOriginalName() + 
                   " - isList: " + ava.isList() + ", isForeachOutput: " + 
                   ((av instanceof ReferencedVarIdentification)? ((ReferencedVarIdentification) av).isForeachOutput() : "false"));
    }
    logger.debug("  we already calculated '" + connections.size() + "' connections.");
  }
  
  //might change WF structure.
  //returns true, if a ForEach was created
  private boolean resolveAllInputs(Step currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections) {
    IdentifiedVariables identifiedVariables = identifyVariables(currentStep);
    
    if(currentStep instanceof WFStep) {
      identifiedVariables = null;
    }
    
    providers.removeIf(x -> x.getIdentifiedVariable().isPrototype());
    
    if(currentStep instanceof StepForeach) {
      resolveForeachInputVars(currentStep, providers, identifiedVariables, connections);
      return false;
    }
    
    boolean createdForeach = false;
    if(currentStep instanceof StepFunction) {
      StepFunction curStepFunction = (StepFunction)currentStep;
      List<AVariableIdentification> oldProviders = new ArrayList<AVariableIdentification>(providers); // we might consume a provider that we need for checkForeaches (?)
      boolean connectToLast = hasSpecialPurposeConnectToLast(curStepFunction);
      boolean consume = hasSpecialPurposeConsumeUsedProvider(curStepFunction);
      resolveInput(curStepFunction, providers, connections, consume, connectToLast);
      createdForeach = checkForeaches(currentStep, identifiedVariables, connections, oldProviders);
    }
    else {
      resolveInputVars(currentStep, identifiedVariables, providers, connections);
      // do not check for ForEach if this is a StepThrow, StepRetry, WFStep or StepSerial
      if(!(currentStep instanceof StepThrow) && !(currentStep instanceof StepRetry) && !(currentStep instanceof WFStep) && !(currentStep instanceof StepSerial)) {
        createdForeach = checkForeaches(currentStep, identifiedVariables, connections, providers);
      }
    }
    
    if (createdForeach) {
      //add Output of ForEach to providers
      Step parentStep = currentStep.getParentStep();
      Step baseStep = currentStep;
      if (parentStep instanceof StepCatch) {
        baseStep = parentStep;
      }
      StepForeach stepForeach = getOutermostCreatedStepForeach(baseStep); 
      List<AVariableIdentification> foreachOutput = identifyVariables(stepForeach).getVariables(VarUsageType.output);
      providers.addAll(foreachOutput);
    }
    
    return createdForeach;
  }
  
  
  private StepForeach getOutermostCreatedStepForeach(Step baseStep) {
    StepForeach candidate = StepForeach.getParentStepForeachOrNull(baseStep);
    StepForeach result = null;
    while(candidate != null) {
      if(!createdForeaches.contains(candidate)) {
        break; //ForEach is not new
      }
      result = candidate;
      candidate = StepForeach.getParentStepForeachOrNull(candidate);
    }
    return result;
  }
  
  //StepFunction operations may declare a SpecialPurposeIdentifier that signals that inputs of this step 
  //should always connect to the most recently added provider.
  private boolean hasSpecialPurposeConnectToLast(StepFunction step) {
    try {
      Operation op = step.getService().getDom().getOperationByName(step.getOperationName());
      //TODO: add SendDocument. Currently not defined in factory.
      if(op.isSpecialPurpose(SpecialPurposeIdentifier.STOPDOCUMENTCONTEXT,
                             SpecialPurposeIdentifier.STOPGENERICCONTEXT)) {
        return true;
      }
    }
    catch(Exception e) {
      return false;
    }
    return false;
  }
  
  //StepFunction operations may declare a SpecialPurposeIdentifier that signals that inputs of this step
  //should consume their used provider, making that provider unavailable for other steps.
  private boolean hasSpecialPurposeConsumeUsedProvider(StepFunction step) {
    try {
      Operation op = step.getService().getDom().getOperationByName(step.getOperationName());
      if(op.isSpecialPurpose(SpecialPurposeIdentifier.STOPDOCUMENTCONTEXT,
                             SpecialPurposeIdentifier.STOPGENERICCONTEXT)) {
        return true;
      }
    }
    catch(Exception e) {
      return false;
    }
    return false;
  }
  
  //TODO: merge + consider non-domRef operations
  private boolean hasSpecialPurposeCreateContext(StepFunction step) {
    try {
      Operation op = step.getService().getDom().getOperationByName(step.getOperationName());
      if(op.isSpecialPurpose(SpecialPurposeIdentifier.STARTDOCUMENTCONTEXT,
                             SpecialPurposeIdentifier.STARTGENERICCONTEXT)) {
        return true;
      }
    }
    catch(Exception e) {
      return false;
    }
    return false;    
  }
  

  private void resolveInput(StepFunction step, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections, boolean consume, boolean connectToLast) {
    IdentifiedVariables identifiedVariables = identifyVariables(step);
    
    //regular resolve
    if(!consume && !connectToLast) {
      resolveInputVars(step, identifiedVariables, providers, connections);
      return;
    }
    
    List<AVariableIdentification> varsToConnect = identifiedVariables.getListAdapter(VarUsageType.input);
    int count =-1;
    for(AVariableIdentification varToConnect : varsToConnect) {
      count++;
      String varId = step.getInputVarIds()[count]; //source id
      AVariableIdentification existingLink = findExistingLink(varId, providers);
      
      //constant connection
      if (step.getInputConnections().getConstantConnected()[count]) {
        if (existingLink == null) {
          Step wfStep = step.getParentWFObject().getWfAsStep();
          existingLink = createConstConnInputVars(Utils.getGlobalConstVar(varId, wfStep)).get(0);
          createConstantConnection(varToConnect, existingLink, connections);
          continue;
        }
      }
      
      if(existingLink != null && existingLink.connectedness.isConstantConnected()) {
        createConstantConnection(varToConnect, existingLink, connections);
        continue;
      }
      
      SimpleConnection con = null;
      if(connectToLast) {
        AVariableIdentification av = findLastConnectable(providers, varToConnect);
        
        //this only covers the case where an existing ForEach connection has to be replaced by another because now there is a newer provider available
        //if we want to connect to a ForEach variable, we have to check that the ForEach is connected with the last provider.
        //otherwise, we connect us with the last provider (ignoring the ForEach).
        if(av != null && !av.getIdentifiedVariable().isList() && isConnectedToList(av, connections)) {
          List<AVariableIdentification> providersWithOutOurForeach = new ArrayList<AVariableIdentification>(providers);
          providersWithOutOurForeach.remove(av);
          AVariableIdentification lastProviderOutsideForEach = findLastConnectable(providersWithOutOurForeach, varToConnect);
          if(lastProviderOutsideForEach != getSource(av, connections)) {
            AVariableIdentification singleForeachVar = av;
            av = lastProviderOutsideForEach;
            //we are connected to a ForEach, but the ForEach is not connected to the last provider
            //therefore, we connect to the last provider (ignoring the ForEach).
            StepForeach stepForeach = StepForeach.getParentStepForeachOrNull(step.getParentStep());
            if(stepForeach == null) {
              throw new RuntimeException("Foreach not found.");
            }
            
            //remove variable from ForEach
            //if it was the last variable, remove entire ForEach
            //return variables to their scope
            removeObsoleteForeachVariable(singleForeachVar, step, providers, connections);
            step.getInputVarIds()[count] = null;
          }
        }
        
        con = new SimpleConnection(LinkstateIn.NONE);
        if(av != null) {
          List<AVariableIdentification> inputVars = new ArrayList<AVariableIdentification>();
          inputVars.add(av);
          con.inputVars = inputVars;
          con.setLinkState(LinkstateIn.AUTO);
          step.getInputVarIds()[count] = av.getIdentifiedVariable().getId();
        }
        connections.put(varToConnect, con);
      }
      else {
        con = getOrCalculateLinkState(varToConnect, existingLink, providers, connections, null, false, varsToConnect, step);
      }
      
      if(consume) {
        
        //nothing / ambiguous thing to remove
        if(con.getInputVars().size() != 1) {
          continue;
        }
        
        AVariableIdentification source = con.getInputVars().get(0);
        
        //do not consume, if this will be tuned into a ForEach
        if(!varToConnect.getIdentifiedVariable().isList() && source.getIdentifiedVariable().isList()) {
          continue;
        }
        
        //consume
        providers.remove(source);
        consumedVars.add(source);
      }
    }
  }
  
  private AVariableIdentification findLastConnectable(List<AVariableIdentification> providers, AVariableIdentification toConnect) {
    for(int i= providers.size()-1; i>=0; i--) {
      AVariableIdentification provider = providers.get(i);
      if(mayBeLinked(toConnect, provider)) {
        return provider;
      }    
    }
    return null;
  }
  
  private void createConstantConnection( AVariableIdentification varToConnect, AVariableIdentification existingLink, Map<AVariableIdentification, InputConnection> connections) {
    List<AVariableIdentification> inputVars = new ArrayList<AVariableIdentification>();
    inputVars.add(existingLink);
    GeneralXynaObject constValue;
    try {
      constValue = existingLink.getIdentifiedVariable().getXoRepresentation();
      String constant = Utils.xoToJson(constValue, existingLink.getIdentifiedVariable().getCreator().getRevision());
      InputConnection inputConnection = new SimpleConnection(inputVars, LinkstateIn.CONSTANT, null, constant);
      connections.put(varToConnect, inputConnection);
    } catch (InvalidObjectPathException | XDEV_PARAMETER_NAME_NOT_FOUND e) {
      throw new RuntimeException("Constant for EndDocument not found.");
    }
  }
  
  //
  private List<AVariableIdentification> analyzeWFStep(WFStep step, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections){
    /*
     * WF -> stepserial -> childsteps
     *                  -> stepcatch (globaler catchblock)
     * stepserial.getChildSteps berücksichtigt stepcatch nicht
     * wf.getChildSteps gibt stepserial+stepcatch zurück. falls es stepcatch gibt, zeigt der wiederum auf stepserial (das ist sein try-block)
     * wenn man also für alle wf.getChildSteps die rekursion durchführt, kommt man bei den steps doppelt vorbei.
     */
    List<Step> childSteps = step.getChildSteps();
    Step childStep;
    if (childSteps.size() == 1) {
      childStep = childSteps.get(0);
    } else if (childSteps.size() == 2) {
      childStep = childSteps.get(1); //global catch
    } else {
      throw new RuntimeException();
    }
    
    List<AVariableIdentification> newProviders = new ArrayList<AVariableIdentification>(providers);
    newProviders = analyzeDataflow(childStep, newProviders, connections);
    
    return newProviders;
  }
  
  private List<AVariableIdentification> findHiddenFilterConditions(StepSerial baseStep){
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>();
    
    for(Step step : baseStep.getChildSteps()) {
      if(step instanceof StepMapping && ((StepMapping)step).isConditionMapping()) {
        result.addAll((identifyVariables(step).getVariables(VarUsageType.output)));
      }
    }
    
    return result;
  }
  
  private List<AVariableIdentification> findHiddenDocumentParts(StepSerial baseStep, List<AVariableIdentification> knownVariables){
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>();
 
    for(Step step : baseStep.getChildSteps()) {
      if(step instanceof StepMapping && ((StepMapping)step).isTemplateMapping()) {
        result.addAll((identifyVariables(step).getVariables(VarUsageType.output)));
      }
      
      if(step instanceof StepForeach) {
        List<AVariableIdentification> sfeOutputs = identifyVariables(step).getVariables(VarUsageType.output);
        for(AVariableIdentification avar : sfeOutputs) {
          if(!knownVariables.contains(avar)) {
            result.add(avar); //--> we add a previously hidden (list) DocumentPart variable.
          }
        }
      }
    }
    
    return result;
  }
  
  private List<AVariableIdentification> pruneToMostGeneric(List<AVariableIdentification> candidates){
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>();
    
    for(int i=0; i<candidates.size(); i++) {
      AVariableIdentification c = candidates.get(i);
      DomOrExceptionGenerationBase doe = c.getIdentifiedVariable().getDomOrExceptionObject();
      
      boolean containsMoreGenericType = false;
      
      //only attempt to find a more generic type, if we are not anyType
      if (doe != null) {
        for (AVariableIdentification other : result) {
          //only consider variables that match list-wise
          if (c.getIdentifiedVariable().isList() != other.getIdentifiedVariable().isList()) {
            continue;
          }

          DomOrExceptionGenerationBase otherDoe = other.getIdentifiedVariable().getDomOrExceptionObject();
          //if other is anyType, we do not want to match against it
          if (otherDoe == null) {
            continue;
          }

          if (DomOrExceptionGenerationBase.isSuperClass(otherDoe, doe)) {
            containsMoreGenericType = true;
            break;
          }
        }
      }
      
      //if there is no more generic type, we add candidate to result and remove all subTypes of it.
      if(!containsMoreGenericType) {
        
        //remove all subTypes we added not knowing that there is a superType as well
        List<AVariableIdentification> resultCpy = new ArrayList<AVariableIdentification>(result);
        for(AVariableIdentification potentialSubtype : resultCpy) {
          
          // if we are about to add anyType, we do not remove anything from result.
          if(doe == null) {
            break;
          }
          //only consider variables that match list-wise
          if(potentialSubtype.getIdentifiedVariable().isList() != c.getIdentifiedVariable().isList()) {
            continue;
          }
          
          DomOrExceptionGenerationBase otherDoe = potentialSubtype.getIdentifiedVariable().getDomOrExceptionObject();
          //if the other variable is anyType, continue
          if(otherDoe == null) {
            continue;
          }
          
          // if potentialSubtype is indeed a subType of candidate, remove it.
          if(DomOrExceptionGenerationBase.isSuperClass(doe, otherDoe)) {
            result.remove(potentialSubtype);
          }
        }
        
        //add to result
        result.add(c);
      }
    }
    
    return result;
  }
  
  private List<AVariableIdentification> analyzeStepChoice(StepChoice currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections){
    IdentifiedVariablesStepChoice identifiedVariablesStepChoice = (IdentifiedVariablesStepChoice)(identifyVariables(currentStep)); 
    identifiedVariablesStepChoice.clearOutput();
    
    //maybe we just created a ForEach around this step.
    //in that case we do not have the single Version of the loop variable in providers. 
    //(and it should not be there, otherwise it may be used out of scope)
    //however, it should be visible to our child steps!
    List<AVariableIdentification> providersFromForeach = new ArrayList<AVariableIdentification>();
    StepForeach sfe = StepForeach.getParentStepForeachOrNull(currentStep);
    if(sfe != null) {
      IdentifiedVariablesStepForeach iv = (IdentifiedVariablesStepForeach) identifyVariables(sfe);
      List<AVariableIdentification> inputs = iv.getListAdapter(VarUsageType.input);
      for(AVariableIdentification input : inputs) {
        if(!providers.contains(input)) {
          providersFromForeach.add(input);
        }
      }
    }
    
    List<List<AVariableIdentification>> laneOutputs = new ArrayList<>();
    List<Step> childSteps = currentStep.getChildSteps(); //StepSerials
    for (int i = 0; i < childSteps.size(); i++) {
      List<AVariableIdentification> providersForChildren = new ArrayList<AVariableIdentification>(providers);
      providersForChildren.addAll(providersFromForeach);
      Step childStep = childSteps.get(i);
      
      //if this is a TypeChoice, add Variable we perform our type choice on to providers. (=> correct subType)
      AVariableIdentification additionalProvider = identifiedVariablesStepChoice.getAVariableIdentification(i);
      if(currentStep.getDistinctionType() == DistinctionType.TypeChoice) {
        
        //there is no additional provider for merged lanes!
        //does not cover branch with assign Steps => that is the one with 
        //content - and that branch does get an additional provider!
        boolean mergedLanes = childStep.getChildSteps().size() == 0;
         
        if(mergedLanes == false) {
          for(int j=0; j< currentStep.getChildSteps().size(); j++) {
            if(currentStep.getCaseInfo(j).getAlias() != null && currentStep.getCaseInfo(j).getAlias().equals(currentStep.getCaseInfo(i).getComplexName())) {
              mergedLanes = true;
              break;
            }
          }
        }
        
        if(additionalProvider == null && ! mergedLanes) {
          //create an additional Provider and update the first StepAssign (copy)
          //there is no additional provider yet, because it was not necessary
          //but childSteps might have changed
          
          AVariable avar = currentStep.createInputVariableForBranch(i);
          if(avar == null) { //special AnyType case
            laneOutputs.add(new ArrayList<AVariableIdentification>());
            continue;
          }
          additionalProvider = ReferencedVarIdentification.of(avar);
          final String id = avar.getId();
          additionalProvider.idprovider = () -> id;
        }

        if (additionalProvider != null) {
          // set id to show connections
          String guiId = ObjectId.createVariableId(ObjectId.createStepId(currentStep).getBaseId(), VarUsageType.input, i+1);
          additionalProvider.internalGuiId = () -> guiId;

          providersForChildren.add(additionalProvider);
          identifiedVariablesStepChoice.loadCreatedVariables();
        }

      }
      
      List<AVariableIdentification> providersFromChildren = analyzeDataflow(childStep, providersForChildren, connections);
      
      
      // if additionalProvider is not null, check if additionalProvider is necessary (-> there is a connection with it as target)
      //   if it is necessary, check if there is an assign step containing it
      //   there should always be two assign steps in a lane of TypeChoice
      // if the upper assign does not handle additioanlProvider, add it (-> when it was not used before, but is now)
      //   in that case additionalProvider is a new Variable and needs to be persisted!
      // otherwise
      //   additionalProvider was loaded (existed already) and we don't have to do anything
      if(additionalProvider != null) { //additional Provider is null if we are not processing a TypeChoice
        StepAssign usa = ((StepSerial)childStep).findFirstAssign();
        InputConnections newInput = new InputConnections(1);
        String[] newOutput = new String[1];
        
        final AVariableIdentification varToFind = additionalProvider;
        boolean additionalProviderNecessary = connections.values().stream().
            anyMatch(x -> x.getConnectionsPerLane().stream().
                     anyMatch(y -> y.getInputVars().stream().
                              anyMatch(z -> z.getIdentifiedVariable() == varToFind.getIdentifiedVariable())));
        if(additionalProviderNecessary) {
          boolean handlesAdditionalProvider = false;
          for(int j=0; j< usa.getOutputVarIds().length; j++) {
            String id = usa.getOutputVarIds()[j];
            if(varToFind.getIdentifiedVariable().getId().equals(id)) {
              handlesAdditionalProvider = true;
              break;
            }
          }

          //we created a new Variable. set Assign and add Variable to VarMap?
          //add copy to assign -- should be only content
          if(!handlesAdditionalProvider) {
            //update StepAssign
            newInput = new InputConnections(1);
            newOutput = new String[1];
            newInput.getVarIds()[0] = currentStep.getInputVarIds()[0]; //id of TypeChoice input
            newOutput[0] = varToFind.getIdentifiedVariable().getId();//id of new Variable
            usa.replaceVars(newInput, newOutput);
            
            //add variable to global Scope
            gbo.getWFStep().getChildStep().addVar(varToFind.getIdentifiedVariable());
           identifiedVariablesStepChoice.addCreatedVariable(i, varToFind);
          }
            
        }
        else { //additional provider not necessary 
          int indexToRemove = usa.getOutputVars().indexOf(varToFind.getIdentifiedVariable());
          if(indexToRemove != -1) { //additional provider is (was) used in assign step
            //update StepAssign
            newInput = new InputConnections(0);
            newOutput = new String[0];
            usa.replaceVars(newInput, newOutput);
            
            //remove variable
            try {
              ((StepSerial)childStep).removeVar(varToFind.getIdentifiedVariable());
            }catch(RuntimeException e) {
              logger.debug("could not remove additional provider from StepSerial. Variable not found.");
            }
            identifiedVariablesStepChoice.removeCreatedVariable(i);
          }

        }
      }
      
      
      if(providersFromChildren == null) {
        throw new RuntimeException(); //should not happen -> children should at least contain providers
      }
      providersFromChildren.removeAll(providers);
      providersFromChildren.remove(additionalProvider); //prevents type choice input from being added to step output
      providersFromChildren.removeAll(providersFromForeach); //prevents single version of loop variable from showing up in the output
      
      
      //wenn auf der Lane ein verstecktes Mapping ist, müssen wir die FilterCondition daraus jetzt wieder den providern hinzufügen 
      //-> wir haben sie beim darauffolgenden StepFunction (query) entfernt
      List<AVariableIdentification> previouslyHiddenFilterConditions = findHiddenFilterConditions((StepSerial)childStep);
      providersFromChildren.addAll(previouslyHiddenFilterConditions);
      
      //wenn auf der Lane ein DocumentContext entsteht, muss dieser jetzzt wieder den providern hinzugefügt werden
      //auch wenn er eigentlich durch ein End Document verbraucht wurde
      List<AVariableIdentification> previouslyConsumedDocumentContext = findConsumedDocumentContext((StepSerial) childStep);
      providersFromChildren.addAll(previouslyConsumedDocumentContext);
      
      //wenn auf der Lane ein ForEach mit verstecktem Output ist, dann müssen wir diesen versteckten Output jetzt wieder den providern hinzufügen.
      List<AVariableIdentification> previouslyHiddenDocumentParts = findHiddenDocumentParts((StepSerial)childStep, providersFromChildren);
      List<AVariableIdentification> cpy = new ArrayList<AVariableIdentification>(previouslyHiddenDocumentParts);
      
      //remove all non-list variables. We do not want to add the hidden Document parts from a regular Template
      for(AVariableIdentification avar : cpy) {
        if(!avar.getIdentifiedVariable().isList()) {
          previouslyHiddenDocumentParts.remove(avar);
        }
      }
      
      providersFromChildren.addAll(previouslyHiddenDocumentParts);
      
      List<AVariableIdentification> allProvidersFromChildren = new ArrayList<AVariableIdentification>(providersFromChildren);
      
      /*
       * falls lane mit throw endet, nicht als possible output adden
       * falls throw weiter oben ist, ist der workflow eh ungültig, das muss hier nicht behandelt werden 
       * 
       * lane ignorieren, wenn sie mit einer anderen verbunden ist
       */
      if (endsWithThrowOrRetry(childStep) || childStep.getChildSteps().size() == 0) {
        identifiedVariablesStepChoice.addPossibleOutput(null, i);
      } else {
        
        //if this is a TypeChoice or Conditional Choice, we have to remove childProviders, if there is another variable of the same type (+ isList)
        //only the most generic definition of that variable type should remain to calculate output
        if( currentStep.getDistinctionType() == DistinctionType.ConditionalChoice ||
            currentStep.getDistinctionType() == DistinctionType.TypeChoice) {
          providersFromChildren = pruneToMostGeneric(allProvidersFromChildren);
        }
        
        identifiedVariablesStepChoice.addPossibleOutput(providersFromChildren, i);
      }
      
      //if this is a TypeChoice or Conditional Choice, we have to remove childProviders, if there is another variable of the same type (+ isList)
      //only the last definition of that variable type should remain to assign output
      if( currentStep.getDistinctionType() == DistinctionType.ConditionalChoice ||
          currentStep.getDistinctionType() == DistinctionType.TypeChoice) {
        providersFromChildren = identifiedVariablesStepChoice.pruneToLastOutput(allProvidersFromChildren);
      }
      
      laneOutputs.add(providersFromChildren);
    }

    /*
     * 1. erzeuge output von choice (kombination von commonoutputs (berechnete outputs) und useroutputs (bei conditional branching)
     * 2. führe autosnapping für alle outputs durch
     *    für commonoutputs nur die provider aus der lane verwenden
     *    für useroutputs sind auch provider von oberhalb der choice erlaubt
     * 3. update alle zuweisungen in den assign-steps am ende der lanes (applyDataflowToGB)
     */
    identifiedVariablesStepChoice.createOutputsAndAssigns(); //outputs aktualisieren
    
    List<AVariableIdentification> outputVars = identifiedVariablesStepChoice.getVariables(VarUsageType.output);
    for (int i = 0; i<currentStep.getChildSteps().size(); i++) {
      Step childStep = currentStep.getChildSteps().get(i);
      String branchId = ObjectId.createBranchId(ObjectId.createStepId(currentStep).getBaseId(), String.valueOf(i));
      
      //ignore lanes without childSteps -> combined lanes. Besser: com.gip.xyna.xprc.xfractwfe.generation.CaseInfo schauen, ob alias gesetzt ist
      if(childStep.getChildSteps().size() == 0) {
        //add Connection => otherwise lane Numbers are off (=> StepAssign)
        for (AVariableIdentification outputVar : outputVars) {
          getSimpleConnection(outputVar, connections, branchId).setLinkState(LinkstateIn.NO_NEED);
        }
        continue;
      }
      

      if (endsWithThrowOrRetry(childStep)) {
        //old modeller still connects common outputs from lanes ending with throw for Conditional Branching
        if(currentStep.getDistinctionType() == DistinctionType.ConditionalBranch) {
          //we still need to add NO_NEED connections for connections we don't have output for.
          //done after getOrCalculateLinkStatesByAssign
        }
        else {
          for (AVariableIdentification outputVar : outputVars) {
            getSimpleConnection(outputVar, connections, branchId).setLinkState(LinkstateIn.NO_NEED);
          }
          continue;
        }
      }
      
      StepAssign stepAssign =
          (StepAssign) childStep.getChildSteps().get(childStep.getChildSteps().size() - 1);
      List<AVariableIdentification> laneProviders = new ArrayList<AVariableIdentification>(laneOutputs.get(i));
      List<AVariableIdentification> allProviders = new ArrayList<>(providers);
      allProviders.addAll(laneProviders);

      //Conditional Branching does not add hidden filterConditions to assign steps (unlike other TypeChoice Distinction types)
      if (currentStep.getDistinctionType() == DistinctionType.ConditionalBranch) {
        List<AVariableIdentification> previouslyHiddenFilterConditions = findHiddenFilterConditions((StepSerial) childStep);
        laneProviders.removeAll(previouslyHiddenFilterConditions);
        allProviders.removeAll(previouslyHiddenFilterConditions);
        
        List<AVariableIdentification> unconsumedContextProviders = findConsumedDocumentContext((StepSerial) childStep);
        laneProviders.removeAll(unconsumedContextProviders);
        allProviders.removeAll(unconsumedContextProviders);
      }
      
      GetOrCalculateLinkStatesByAssignParameter parameter = new GetOrCalculateLinkStatesByAssignParameter();
      parameter.setAllConnections(connections);
      parameter.setBranchId(branchId);
      parameter.setOriginalStep(currentStep);
      parameter.setOutputVars(identifiedVariablesStepChoice.getUserOutputs());
      parameter.setProviders(allProviders);
      getOrCalculateLinkStatesByAssign(stepAssign, parameter);
      
      parameter = new GetOrCalculateLinkStatesByAssignParameter();
      parameter.setAllConnections(connections);
      parameter.setBranchId(branchId);
      parameter.setOriginalStep(currentStep);
      parameter.setOutputVars(identifiedVariablesStepChoice.getCommonOutputs());
      parameter.setProviders(laneProviders);
      getOrCalculateLinkStatesByAssign(stepAssign, parameter);
      
      // if this is a Conditional Branching ending in a throw
      //   set all connections with linkState NONE to NO_NEED
      //=> this branch never causes problems arising from unconnected Output Variables (-> ambiguous input may prevent deployment though)
      if(endsWithThrowOrRetry(childStep) && currentStep.getDistinctionType() == DistinctionType.ConditionalBranch) {
        List<AVariableIdentification> choiceOutputs = new ArrayList<AVariableIdentification>();
        choiceOutputs.addAll(identifiedVariablesStepChoice.getUserOutputs());
        choiceOutputs.addAll(identifiedVariablesStepChoice.getCommonOutputs());
        for(AVariableIdentification output : choiceOutputs) {
          InputConnection icon = connections.get(output);
          SimpleConnection con = icon.getConnectionForLane(i);
          if(con.getLinkState() == LinkstateIn.NONE)
            con.setLinkState(LinkstateIn.NO_NEED);
        }
      }
    }
    addToConnections(identifiedVariablesStepChoice, connections); //nur für TypeChoice/ConditionalChoice benötigt. ansonsten schadet es nix
    
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>(providers);
    result.addAll(outputVars);
    
    //update outputVars @ step
    currentStep.setCalculatedOutput(outputVars.stream().map(x -> x.getIdentifiedVariable()).collect(Collectors.toList()));
     
    List<AVariableIdentification> choiceOutput = identifiedVariablesStepChoice.getListAdapter(VarUsageType.output); 
    if(sfe != null && createdForeaches.contains(sfe)) {
      //if ForEach was just created, it does not contain our output variables
      //our output variables were just calculated.
      //add our output to stepForeach
      List<AVariableIdentification> foreachOutput = addOutputToStepForeach(sfe, choiceOutput, connections, 0);
      
      //return forEach list output instead of our outputs
      //our outputs are now hidden by the surrounding StepFoeach
      result.removeAll(outputVars);
      result.addAll(new ArrayList<AVariableIdentification>(foreachOutput));
    }
    
    return result;
  }
  
  //add output Variable Identifications to ForEach
  //recursively add to outer ForEaches as well
  private List<AVariableIdentification> addOutputToStepForeach(StepForeach sfe, List<AVariableIdentification> output, Map<AVariableIdentification, InputConnection> connections, int depth) {
    if (depth > 50) {
      Utils.logError("more than 50 nested ForEaches detected. CurrentForEach Id: " + sfe.getStepId(), null);
      return output; //sanity check - do not pass through more than 50 nested ForEaches
    }
    
    for (AVariableIdentification avar : output) {
      addForeachOutput(sfe, avar, connections);
    }

    List<AVariableIdentification> sfeOutputs = identifyVariables(sfe).getListAdapter(VarUsageType.output);
    Optional<StepForeach> parentSfe = getSurroundingStepForeach(sfe);
    if (parentSfe.isEmpty()) {
      return sfeOutputs;
    }

    StepForeach parentSfeStep = parentSfe.get();
    return addOutputToStepForeach(parentSfeStep, sfeOutputs, connections, depth + 1);
  }
  
  
  //do not return contexts that are not consumed
  private List<AVariableIdentification> findConsumedDocumentContext(StepSerial childStep) {
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>();
    
    for (Step step : childStep.getChildSteps()) {
      if (step instanceof StepFunction && hasSpecialPurposeCreateContext((StepFunction)step)) {
        List<AVariableIdentification> avars = (identifyVariables(step).getVariables(VarUsageType.output));
        result.addAll(avars);
      } else if(step instanceof StepCatch && ((StepCatch)step).getStepInTryBlock() instanceof StepFunction) {
        StepFunction sf = (StepFunction)((StepCatch)step).getStepInTryBlock();
        if(hasSpecialPurposeCreateContext(sf)) {
          List<AVariableIdentification> avars = (identifyVariables(sf).getVariables(VarUsageType.output));
          result.addAll(avars);          
        }
      }
    }
    
    result.removeIf(x -> !consumedVars.contains(x));
    
    return result;
  }

  private List<AVariableIdentification> analyzeStepParallel(Step currentStep, List<AVariableIdentification>providers, Map<AVariableIdentification, InputConnection> connections){
    List<AVariableIdentification> newProviders = new ArrayList<AVariableIdentification>();    
    List<Step> childSteps = currentStep.getChildSteps();
    
    for (int i = 0; i<childSteps.size(); i++) {
      Step childStep = childSteps.get(i);
      List<AVariableIdentification> providerCopy = new ArrayList<AVariableIdentification>(providers);
      List<AVariableIdentification> cProviders = new ArrayList<AVariableIdentification>(analyzeDataflow(childStep, providerCopy, connections));
      cProviders.removeAll(providers);
      newProviders.addAll(cProviders);
    }
      
    newProviders.addAll(providers);
    
    return newProviders;
  }
  
  private List<AVariableIdentification> analyzeStepSerial(Step currentStep, List<AVariableIdentification>providers, Map<AVariableIdentification, InputConnection> connections){
    List<AVariableIdentification> newProviders =  providers;
    List<Step> childSteps = new ArrayList<Step>(currentStep.getChildSteps());
    for (int i = 0; i<childSteps.size(); i++) {
      Step childStep = childSteps.get(i);
      newProviders = analyzeDataflow(childStep, newProviders, connections);
    }
    
    return newProviders;
  }
  
  
  private boolean foreachStillHasChildStep(StepForeach step) {
    if(step.getChildScope().getChildStep().getChildSteps().size() == 0)
      return false;
    
    Step childStep = step.getChildScope().getChildStep().getChildSteps().get(0);
    if(childStep instanceof StepForeach)
      return foreachStillHasChildStep((StepForeach)childStep);
    
    
    return true;
  }
  
  private List<AVariableIdentification> analyzeStepForeach(StepForeach stepForeach, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections){
    
    Step oldParent = stepForeach.getParentStep();
    int oldParentIndex = oldParent.getChildSteps().indexOf(stepForeach);
    
    //if there is no childStep, we can remove this ForEach
    if(!foreachStillHasChildStep(stepForeach)) {
      removeStepForeachVariables(stepForeach);
      stepForeach.getParentStep().getChildSteps().remove(stepForeach);
      
      //in this case we might need to remove a lane from a StepParallel
      if(oldParent instanceof StepSerial)
        Utils.removeWrapperWhenObsolete(gbo, (StepSerial)oldParent);
      
      //providers afterwards are the same as when we started.
      return providers;
    }
    
    
    //if there is an unresolved input, we have to remove this ForEach
    //detects if there are no providers - if there is an ambiguity, we see it at the child.
    //detects if the provider is no longer a list
    IdentifiedVariablesStepForeach iv = (IdentifiedVariablesStepForeach)identifyVariables(stepForeach);
    List<AVariableIdentification> inputVars = iv.getVariables(VarUsageType.input);
    for(AVariableIdentification input : inputVars) {
      InputConnection iCon = connections.get(input);
      if (iCon == null || iCon.getConnectionForLane(0).getInputVars().size() < 1
          || !iCon.getConnectionForLane(0).getInputVars().get(0).getIdentifiedVariable().isList()) {
       removeForeachInput(stepForeach, input.getIdentifiedVariable());
       providers.remove(input);
      }
    }
    
    Step childStep = stepForeach.getChildScope().getChildStep().getChildSteps().get(0);
        
    //TODO: see checkForeaches
    // what if we remove an inner ForEach -> we need to update Scope of output Variables?
    //if we removed all inputs, we have to remove the step as well.
    if(stepForeach.getInputListRefs().length == 0) {
      //remove all outputs -> removes List versions of outputs and
      //                      generated input variables (single version of list variable)
      Set<AVariable> v = new HashSet<AVariable>();
      v.addAll(Arrays.asList(stepForeach.getOutputVarsSingle()));
      v.addAll(stepForeach.getChildScope().getPrivateVars());
      v.addAll(((StepSerial)stepForeach.getChildSteps().get(0).getChildSteps().get(0)).getVariablesAndExceptions());
      v.removeAll(stepForeach.getParentWFObject().getWfAsStep().getChildStep().getVariablesAndExceptions());
      removeStepForeachVariables(stepForeach);
      
      //return variables to global StepSerial
      for(AVariable va : v) {
        stepForeach.getParentWFObject().getWfAsStep().getChildStep().addVar(va);
      }
      
      
      //return constant variables to global StepSerial
      //returnConstantVariablesToGlobalStepSerial(stepForeach);
      moveConstantsToParentScope(stepForeach.getChildScope());

      childStep.setParentScope(stepForeach.getParentScope());
      stepForeach.getParentStep().getChildSteps().set(stepForeach.getParentStep().getChildSteps().indexOf(stepForeach), childStep);
      Utils.updateScopeOfSubSteps(childStep);
      
      //still need to analyze Child Step!
      return analyzeDataflow(stepForeach.getChildScope().getChildStep().getChildSteps().get(0), providers, connections);
    }
    
    
    //Listenwertige Variable über die Iteriert wird auch den Kindern anbieten
    List<AVariableIdentification> childProviders = null;
    childProviders = new ArrayList<AVariableIdentification>(providers);
    // einzelwertige Loop-Variable ist nur fuer Schritt innerhalb von Foreach ein Provider
    IdentifiedVariables identifiedVariables = identifyVariables(stepForeach);
    identifiedVariables.identify(); //required for copied stepForeach
    List<AVariableIdentification> foreachInput = identifiedVariables.getVariables(VarUsageType.input);
    if (identifiedVariables != null) {
      childProviders.addAll(foreachInput);
    }

    // output von Schritt innerhalb von Foreach ist Provider fuer nachfolgende Schritte
    //compare providersFromChildren with outputs of ForEach => update ForEach output
    List<AVariableIdentification> providersFromChildren = analyzeDataflow(stepForeach.getChildScope().getChildStep(), childProviders, connections);
    
    //we need to add previously hidden Document Paths.
    List<AVariableIdentification> hiddenDocumentParts = findHiddenDocumentParts(stepForeach.getChildScope().getChildStep(), providersFromChildren);
    providersFromChildren.addAll(hiddenDocumentParts);
    
    List<AVariableIdentification> orgProvidersFromChildren = new ArrayList<AVariableIdentification>(providersFromChildren);
    
    //child may have already removed this StepForeach -> our old parent does not know us anymore
    if(!oldParent.getChildSteps().contains(stepForeach)) {
      //child removed us. return providersFromChildren - without 
      List<AVariableIdentification> result = new ArrayList<AVariableIdentification>(providersFromChildren);
      result.removeAll(foreachInput);
      return result;
    }
    
    //neue outputs sind vorherige outputs+FEoutputs. die outputs innerhalb des foreaches ignorieren. -- but outputs should lists now
    List<AVariableIdentification> newProviders = new ArrayList<>(providers);

    providersFromChildren.removeAll(childProviders);
    List<String> ids = new ArrayList<String>(); //providersFromChildren.stream().map(x -> x.idprovider.getId()).collect(Collectors.toList());
    for(AVariableIdentification avar : providersFromChildren) {
      String idToAdd = avar.idprovider.getId();
      ids.add(idToAdd);
    }
    
    //remove outputs that are no longer created by children
    //and return the variable back to the global StepSerial
    AVariable[] oldOutput = stepForeach.getOutputVarsSingle();
    if(oldOutput != null) {
      for(AVariable oldOutputVar : oldOutput) {
        if(!ids.contains(oldOutputVar.getId())) {
          removeForeachOutput(stepForeach, new AVariable[] {oldOutputVar});
          stepForeach.getParentWFObject().getWfAsStep().getChildStep().addVar(oldOutputVar);
        }
      }   
    }

    //TODO: create function to set it -- maybe changing IdentifiedVariablesStepFunction can help as well -- can be used at createStepForeach as well
    List<AVariableIdentification> actualProvidersFromChildren = null;
    
    //if child is StepFunction (abstract service), we have to use a different set of Variables
    //update childStep - analyzing our old child might have changed it!
    childStep = stepForeach.getChildScope().getChildStep().getChildSteps().get(0);
    if(childStep instanceof StepFunction) {
     actualProvidersFromChildren = getRealOutputVarsOfStepFunction((StepFunction)childStep);
    }
    else if(childStep instanceof StepCatch && ((StepCatch)childStep).getStepInTryBlock() instanceof StepFunction) {
      
      //if detached -> no outputs!
      if( ((StepCatch)childStep).getStepInTryBlock().isExecutionDetached()) {
        actualProvidersFromChildren = new ArrayList<AVariableIdentification>();
      }
      else {
        //actualProviders are output of inner StepFunction (?)
        StepFunction sf = (StepFunction)((StepCatch)childStep).getStepInTryBlock();
        actualProvidersFromChildren = getRealOutputVarsOfStepFunction(sf);       
      }

    }
    else {
      actualProvidersFromChildren = providersFromChildren;
    }
    
    //add new provider from children
    //providersFromChildren contains all outputs that have to be reflected by ForEach
    for(AVariableIdentification providerFromChild : actualProvidersFromChildren) {
      if(!foreachOutputAlreadyAccountedFor(providerFromChild, stepForeach)) {
        addForeachOutput(stepForeach, providerFromChild, connections);
      }
    }
    
    
    identifiedVariables.identify();
    
    
    //add output to providers for later steps - but hide List-Versions of hidden DocumentParts
    List<AVariableIdentification> outputsWithoutHidden = getForeachOutputWithoutHidden(stepForeach, hiddenDocumentParts);
    
    newProviders.addAll(outputsWithoutHidden);
    
    logger.debug("After Foreach, there are '" + newProviders.size() + "' providers");
    for(AVariableIdentification provider : newProviders) {
      AVariable v = provider.getIdentifiedVariable();
      logger.debug("  " + v.getId() + 
                   " - " +  v.getOriginalPath()+ "."+ v.getOriginalName() + 
                   " isList: " + provider.getIdentifiedVariable().isList() +
                   " isForeachOutput: " + ((provider instanceof ReferencedVarIdentification)? ((ReferencedVarIdentification) provider).isForeachOutput() : ""));
    }
    
    
    //happens if child step had an input variable removed
    //check if all ForEach variables are still needed
    //a ForEach variable is no longer needed, if there is no connection pointing to it.
    //since we already processed our child steps, we can check connection
    //child may have already removed this StepForeach -> stepForeach.getParentStep() is null in that case
    inputVars = identifiedVariables.getListAdapter(VarUsageType.input);
    if( stepForeach.getParentStep() != null) {
      for(AVariableIdentification input : inputVars) { //input is supposed to be source of connection (-> single version of loop variable) 
        if(!forEachVariableStillInUse(input, connections.values())) {
          
          //to remove variable, we need to know our (old) parent
          Step childStepInParent = oldParent.getChildSteps().get(oldParentIndex);
          oldParent.getChildSteps().set(oldParentIndex, stepForeach);
          removeForeachInput(stepForeach, input.getIdentifiedVariable());
          oldParent.getChildSteps().set(oldParentIndex, childStepInParent);
          providers.remove(input);
          orgProvidersFromChildren.remove(input); //remove input from providers if we remove this ForEach completely
        }
      }
      
      //remove entire step if there is no input left
      if(stepForeach.getInputListRefs().length == 0) {
        //remove all outputs -> removes List versions of outputs and
        //                      generated input variables (single version of list variable)
        //but single Versions have to be returned to global step serial!
        List<AVariable> varsToReturn = new ArrayList<AVariable>();
        varsToReturn.addAll(Arrays.asList(stepForeach.getOutputVarsSingle()));
        varsToReturn.addAll(stepForeach.getChildScope().getPrivateVars());
        varsToReturn.removeAll(stepForeach.getParentWFObject().getWfAsStep().getChildStep().getVariablesAndExceptions());
        removeStepForeachVariables(stepForeach);
        stepForeach.getParentStep().getChildSteps().set(stepForeach.getParentStep().getChildSteps().indexOf(stepForeach), childStep);
        childStep.setParentScope(stepForeach.getParentScope());
        Utils.updateScopeOfSubSteps(childStep);
        
        //these variables were removed from their StepSerial when they were added to StepForeach output
        for(int i=0; i<varsToReturn.size(); i++) {
          stepForeach.getParentWFObject().getWfAsStep().getChildStep().addVar(varsToReturn.get(i));
        }
        
        
        //remove hidden DocumentParts again
        orgProvidersFromChildren.removeAll(hiddenDocumentParts);

        //return providers from children without ForEach
        return orgProvidersFromChildren;
      }
    }

    //create Connections for outputs
    for(AVariableIdentification outputVar : outputsWithoutHidden) {
        List<AVariableIdentification> sources;
        if ( (childStep instanceof StepCatch) ||
             (childStep instanceof StepFunction) && !(childStep.getParentStep() instanceof StepCatch) ) {
          sources = actualProvidersFromChildren;
        } else { 
          sources = providersFromChildren;
        }

        SimpleConnection con = createSimpleConnectionForEachOutput(stepForeach, outputVar, sources);
        connections.put(outputVar, con);  
    }

    return newProviders;
  }
  

  private void moveConstantsToParentScope(ScopeStep initialScope) {

    RecursiveVisitor v = new RecursiveVisitor() {

      private ScopeStep parent = initialScope.getParentScope();
      
      private void apply(InputConnections inputCons, Step step) {
        Boolean[] constants = inputCons.getConstantConnected();
        for(int i=0; i<constants.length; i++) {
          if(constants[i] == false) {
            continue;
          }
          String id = step.getInputVarIds()[i];
          AVariable avar = initialScope.identifyVariableForThisScope(id);
          if (avar != null) {
            parent.addPrivateVariable(avar);
          }
          logger.debug("move constant vars of" + step + " to global scope.");
          
        }       
      }

      @Override
      public void visitStepChoice(StepChoice step) {
        apply(step.getInputConnections(), step);
        super.visitStepChoice(step);
      }
      
      @Override
      public void visitStepFunction(StepFunction step) {
        apply(step.getInputConnections(), step);
        super.visitStepFunction(step);
      }
      
      @Override
      public void visitStepMapping(StepMapping step) {
        apply(step.getInputConnections(), step);
        super.visitStepMapping(step);
      }
      
      @Override
      public void visit(Step step){
        
      }


      @Override
      public boolean beforeRecursion(Step parent, Collection<Step> children) {
        return true;
      }
    };

    initialScope.visit(v);
  }


  private List<AVariableIdentification> getRealOutputVarsOfStepFunction(StepFunction sf){
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>();
    
    List<AVariable> vars = sf.getOutputVars();
    IdentifiedVariablesStepFunction ivsf = (IdentifiedVariablesStepFunction)identifyVariables(sf);
    for(int i=0; i< vars.size(); i++) {
      AVariable v = vars.get(i);
      //TODO: don't create new
      ReferencedVarIdentification sv = ivsf.createVarIdentification(v);
      
      //idProvider is 'private' StepFunction output
      // -> getOutputVarIds
      String id = sf.getOutputVarIds()[i];
      sv.idprovider = () -> id;
      
      result.add(sv);
    }
    
    return result;
  }
  
  private SimpleConnection createSimpleConnectionForEachOutput(StepForeach sfe, AVariableIdentification outputVar, List<AVariableIdentification> sources) {
    SimpleConnection result = new SimpleConnection(LinkstateIn.AUTO);
    
    //find index of outputVar id in StepForeach outputVars
    int index = -1;
    for(int i=0; i<sfe.getOutputListRefs().length; i++) {
      if(sfe.getOutputListRefs()[i].equals(outputVar.getIdentifiedVariable().getId())) {
        index = i;
        break;
      }
    }
    
    if(index == -1)
      throw new RuntimeException("output not found");
    
    //get id of outputVarsSingle in StepForeach at index
    String idOfSource = sfe.getOutputVarsSingle()[index].getId();
    
    //find source with id in sources
    AVariableIdentification source = null;
    AVariable variableToConnect = null;
    
    //TODO; set elsewhere
    String guiId = null;
    
    //determine correct variable to connect
    Step child = sfe.getChildScope().getChildStep().getChildSteps().get(0);
    for(int i=0; i< child.getOutputVarIds().length; i++) {
      if(child.getOutputVarIds()[i].equals(idOfSource)) {
        if(child instanceof StepFunction) {
          variableToConnect = ((StepFunction)child).getOutputVars().get(i);
          guiId = ObjectId.createVariableId(((StepFunction)child).getStepId(), com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType.output, i);
        } else if(child instanceof StepCatch && ((StepCatch)child).getStepInTryBlock() instanceof StepFunction) {
          variableToConnect = ((StepFunction)((StepCatch)child).getStepInTryBlock()).getOutputVars().get(i);
          guiId = ObjectId.createVariableId(((StepFunction)((StepCatch)child).getStepInTryBlock()).getStepId(), com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType.output, i);
        }
        else
          variableToConnect = child.getOutputVars().get(i);
      }
    }
   
    for(AVariableIdentification c : sources) {
      if(c.getIdentifiedVariable() == variableToConnect) {
        source = c;
        if(guiId != null) {
          final String finalGuiId = guiId;
          source.internalGuiId = () -> finalGuiId;
        }
        break;
      }
    }
    
    
    if(source == null)
      throw new RuntimeException("source variable not found");
    
    // set result
    List<AVariableIdentification> inputVars = new ArrayList<AVariableIdentification>();
    inputVars.add(source);
    result.setInputVars(inputVars);
    
    return result;
  }
  
  private void removeForeachInput(StepForeach stepForeach, AVariable varToRemove) {
    stepForeach.removeInput(varToRemove); //remove single input
    identifyVariables(stepForeach).identify(); //update identified Variables
    logger.debug("Removing input from StepForeach (" + stepForeach + "): " + varToRemove);
  }
  
  private void removeForeachOutput(StepForeach stepForeach, AVariable[] outputsToRemove) {
    if(outputsToRemove == null)
      return;

    for(AVariable avar : outputsToRemove) {
      stepForeach.removeOutputVar(avar); //also removes variable from scope
    }
  }
  


  //when saving, we add the new (list) result to WF output
  private void addForeachOutput(StepForeach stepForeach, AVariableIdentification providerFromChild, Map<AVariableIdentification, InputConnection> connections) {
    logger.debug("Adding " + providerFromChild + " to ForEach " + stepForeach);
    stepForeach.addOutputVar(providerFromChild.getIdentifiedVariable());
    int index = stepForeach.getOutputListRefs().length-1;
    
    //set idprovider
    IdentifiedVariables ivars = identifyVariables(stepForeach);
    ivars.identify();
    AVariableIdentification createdListAVarIdent = ivars.getListAdapter(VarUsageType.output).get(index);
    final String id = createdListAVarIdent.getIdentifiedVariable().getId();
    createdListAVarIdent.idprovider = () -> id;
    
    final String connectedToId = providerFromChild.getIdentifiedVariable().getId();
    createdListAVarIdent.connectedness = new Connectedness() {
      @Override
      public boolean isUserConnected() {
        return false;
      }
      @Override
      public boolean isConstantConnected() {
        return false;
      }
      @Override
      public String getConnectedVariableId() {
        return connectedToId;
      }
    };
    
    createdListAVarIdent.getIdentifiedVariable().setTargetId(stepForeach.getOutputListRefs()[stepForeach.getOutputListRefs().length - 1]);
    
    //add connection between list output of ForEach and output of ForEach child
    List<AVariableIdentification> providerList = new ArrayList<AVariableIdentification>();
    providerList.add(providerFromChild);
    
    connections.put(createdListAVarIdent, new SimpleConnection(providerList, LinkstateIn.AUTO, null, null));
  }
  
  //returns true, if avar is already an output of step
  private boolean foreachOutputAlreadyAccountedFor(AVariableIdentification avar, StepForeach step) {
    if(step.getOutputVarsSingle() == null)
      return false;
    
    for(int i=0; i<step.getOutputVarsSingle().length; i++) {
      AVariable outputVar = step.getOutputVarsSingle()[i];
      if(avar.idprovider.getId().equals(outputVar.getId())) {
        return true;
      }
    }
    return false;
  }
  
  private boolean forEachVariableStillInUse(AVariableIdentification input, Collection<InputConnection> connections) {
    String idToFind = input.getIdentifiedVariable().getId();
    if(idToFind == null)
      return false;
          
    for(InputConnection iCon : connections) {
      for(SimpleConnection con : iCon.getConnectionsPerLane()) {
        if(con.getInputVars().size() != 1)
          continue;
        
        if(idToFind.equals(con.getInputVar(0).getIdentifiedVariable().getId()))
          return true;
      }
    }
    return false;
  }
  
  private List<AVariableIdentification> analyzeStepCatch(Step currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections){
    StepCatch stepCatch = (StepCatch)currentStep;
    List<BranchInfo> branchesForGUI = stepCatch.getBranchesForGUI();
    
    boolean isGlobalCatch = false;
    IdentifiedVariables variables;
    if (stepCatch.getStepInTryBlock() instanceof StepFunction) {
      StepFunction stepInTryBlock = (StepFunction) stepCatch.getStepInTryBlock();
      variables = identifyVariables(stepInTryBlock);
    } else {
      //stepserial in wf
       WFStep wfStep = (WFStep) stepCatch.getStepInTryBlock().getParentWFObject().getWfAsStep();
       variables = identifyVariables(wfStep);
       isGlobalCatch = true;
    }
    
    //handle Step in Try Block. Needs to be analyzed before Branches!
    Step childStep = stepCatch.getStepInTryBlock();
    List<AVariableIdentification> newProviders = providers;
    newProviders = analyzeDataflow(childStep, newProviders, connections);
    
    //analyze Branches
    for (int i = 0; i < branchesForGUI.size(); i++) {
      BranchInfo branch = branchesForGUI.get(i);
      //die darin hinzugefügten provider sind nur innerhalb des catchblocks verfügbar
      
      List<AVariableIdentification> providersForBranch = new ArrayList<AVariableIdentification>(providers);
      
      //id of Exception [-> type this branch covers]
      final String id = new ArrayList<String>(stepCatch.getAllUsedVariableIds()).get(i); //TODO: order .. wir können sonst vllt noch den complexName vergleichen (Pfad+Name)
      VariableIdentification exception = null;
      try {
        exception = currentStep.getParentScope().identifyVariable(id);
      } catch (XPRC_InvalidVariableIdException e) {
        continue;
      }
      DirectVarIdentification ex = DirectVarIdentification.of(exception.getVariable());
      ex.idprovider = () -> id;
      
      String baseId = isGlobalCatch ? "" : ObjectId.createStepId(childStep).getBaseId();
      final String guiId = ObjectId.createVariableId(baseId, VarUsageType.input, i + variables.getVariables(VarUsageType.input).size());
      ex.internalGuiId = () -> guiId;
      
      providersForBranch.add(ex);
      
      List<AVariableIdentification> childProviders =
          analyzeDataflow(((CatchBranchInfo) branch).getMainStep(), providersForBranch, connections);
      /*
       * output-vars von übergeordnetem objekt (wfstep/stepfunction) müssen als input verbunden werden:
       * - branchid setzen
       * - falls catch immer mit throw endet, linkstate auf NOT_NEEDED setzen.  
       * 
       * wie sind die daten im xml gespeichert, wie werden sie da wieder geupdated?
       * => das sind jeweils StepAssign Schritte am Ende der lanes
       */
      String branchId = "";
      if(stepCatch.getStepInTryBlock() instanceof StepFunction) 
        branchId = ObjectId.createBranchId(ObjectId.createStepId(stepCatch.getStepInTryBlock()).getBaseId(), String.valueOf(i));
      else //global catch
        branchId = ObjectId.createBranchId("", String.valueOf(i)); //creates branch-i

      StepAssign stepAssign = (StepAssign) branch.getMainStep().getChildSteps().get(branch.getMainStep().getChildSteps().size() - 1);
      GetOrCalculateLinkStatesByAssignParameter parameter = new GetOrCalculateLinkStatesByAssignParameter();
      parameter.setAllConnections(connections);
      parameter.setBranchId(branchId);
      parameter.setOriginalStep(stepCatch.getStepInTryBlock());
      parameter.setOutputVars(variables.getVariables(VarUsageType.output));
      parameter.setProviders(childProviders);
      getOrCalculateLinkStatesByAssign(stepAssign, parameter);

      if (endsWithThrowOrRetry(branch.getMainStep())) {
        for (AVariableIdentification outputVar : variables.getVariables(VarUsageType.output)) {
          Linkstate oldState = getSimpleConnection(outputVar, connections, branchId).getLinkState();
          if(oldState != LinkstateIn.AUTO && oldState != LinkstateIn.USER) //even though it is not needed, keep AUTO and USER connection.
            getSimpleConnection(outputVar, connections, branchId).setLinkState(LinkstateIn.NO_NEED);
        }
        continue;
      }
    }

    //Handled at workflow output
    //calculate stepAssign for workflow output (global Catch) => not needed for 'default' branch at StepFunctions => output of StepFunction is mapped already
    if (!(stepCatch.getStepInTryBlock() instanceof StepFunction)) {
      if (childStep.getChildSteps().size() > 0) { //if there is no stepAssign in the workflow, we do not need to calculate it.
        Step lastStep = childStep.getChildSteps().get(childStep.getChildSteps().size() - 1);
        if (lastStep instanceof StepAssign) {
          StepAssign stepAssign = (StepAssign) lastStep;
          GetOrCalculateLinkStatesByAssignParameter parameter = new GetOrCalculateLinkStatesByAssignParameter();
          parameter.setAllConnections(connections);
          parameter.setBranchId(null);
          parameter.setOriginalStep((WFStep) stepCatch.getStepInTryBlock().getParentWFObject().getWfAsStep());
          parameter.setOutputVars(variables.getVariables(VarUsageType.output));
          parameter.setProviders(newProviders);
          getOrCalculateLinkStatesByAssign(stepAssign, parameter);
        }
      }
    }
    
    //global catch: if workflow always throws exception than set connection to NO_NEED
    if (!(stepCatch.getStepInTryBlock() instanceof StepFunction)) {
      if (endsWithThrowOrRetry(childStep)) {
        for (AVariableIdentification outputVar : variables.getVariables(VarUsageType.output)) {
          Linkstate oldState = getSimpleConnection(outputVar, connections, null).getLinkState();
          if(oldState != LinkstateIn.AUTO  && oldState != LinkstateIn.USER) //even though it is not needed, keep AUTO und USER connection.
            getSimpleConnection(outputVar, connections, null).setLinkState(LinkstateIn.NO_NEED);
        }
      }
    }
    
    return newProviders;
  }
  
  //entfernt die Filtercondition aus dem versteckten Mapping vor currentStep - wenn vor dem currentStep ein verstecktes Mapping existiert.
  private List<AVariableIdentification> checkQuery(StepFunction currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections){
    Step parent = currentStep.getParentStep();
    if(parent == null)
      return providers;
    
    Step pparent = parent.getParentStep();
    int index = pparent.getChildSteps().indexOf(parent);
    if(index <= 0)
      return providers;
    
    Step previousStep = pparent.getChildSteps().get(index - 1);
    if(!(previousStep instanceof StepMapping))
      return providers;
    
    if(((StepMapping)previousStep).isConditionMapping()) {
      //remove filterCondition defined in previousStep from providers
      List<AVariableIdentification> result = new ArrayList<AVariableIdentification>(providers);
      result.removeAll(identifyVariables(previousStep).getVariables(VarUsageType.output));
      return result;
    }
    
    
    return providers;
  }
  
  private List<AVariableIdentification> analyzeStepFunction(StepFunction currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections) {
    Step compensationStep = currentStep.getCompensateStep();
    
    //if detached, do not add output
    //there can't be compensation or Exceptions
    if(currentStep.isExecutionDetached())
      return providers;
    
    //analyze Compensation
    if(compensationStep != null) {
      // providers for Compensation are all previous providers as well as output of StepFunction
      List<AVariableIdentification> compensationProviders = new ArrayList<AVariableIdentification>(providers);
      compensationProviders.addAll(identifyVariables(currentStep).getVariables(VarUsageType.output));
      analyzeDataflow(compensationStep, compensationProviders, connections);
    }
    
    //analyze RemoteDestination parameters
    if(currentStep.getRemoteDispatchingParameter() != null) {
      analyzeRemoteDestination(currentStep, providers, connections);
    }
    
    IdentifiedVariables identifiedVariables = identifyVariables(currentStep);
    identifiedVariables.identify();
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>(providers);
    result.addAll(identifiedVariables.getVariables(VarUsageType.output));
    
    //check Query
    StepMapping stepMapping = null;
    try {
      stepMapping = QueryUtils.findQueryHelperMapping(GBSubObject.of(gbo, ObjectId.createStepId(currentStep).getObjectId()));
    } catch (MissingObjectException | UnknownObjectIdException | XynaException e) {
      Utils.logError(e);
      throw new RuntimeException("Could not check QueryHelperMapping");
    }
    if(stepMapping != null) {
        return checkQuery(currentStep, result, connections);
    }
    
    //check End Document
    //if this step is a Begin Document, it hides a previous  'Document Context'.
    
    return result;
  }
  
  //assumes remoteDestination of step is valid or null
  //assumes that invokeVarIds have correct length
  private void analyzeRemoteDestination(StepFunction step, List<AVariableIdentification> providers,
                                        Map<AVariableIdentification, InputConnection> connections) {
    RemoteDespatchingParameter parameter = step.getRemoteDispatchingParameter();
    String remoteDestination = step.getRemoteDispatchingParameter().getRemoteDestination();
    if (remoteDestination == null) {
      return;
    }
    List<DispatchingParameter> params =
        rdMgmt.getRemoteDestinationTypeInstance(remoteDestination).getDispatchingParameterDescription().getDispatchingParameters();

    for (int i = 0; i < parameter.getInvokeVarIds().length; i++) {

      DispatchingParameter param = params.get(i);
      String sourceId = parameter.getInvokeVarIds()[i];

      if (!remoteDispatchingParameterMap.containsKey(step)) {
        remoteDispatchingParameterMap.put(step, new HashMap<Integer, AVariableIdentification>());
      }

      HashMap<Integer, AVariableIdentification> parameterMap = remoteDispatchingParameterMap.get(step);
      if (!parameterMap.containsKey(i)) {
        AVariable fakeVar = new DatatypeVariable(gbo.getWorkflow());
        fakeVar.setIsList(param.isList());
        try {
          fakeVar.replaceDomOrException((DomOrExceptionGenerationBase) DomOrExceptionGenerationBase
              .getInstance(XMOMType.DATATYPE, param.getTypepath() + "." + param.getTypename(), gbo.getWorkflow().getRevision()), "");
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException("could not create Dispatching parameter");
        }
        DirectVarIdentification varIdent = DirectVarIdentification.of(fakeVar);
        final int fI = i;
        final String fSourceId = sourceId;
        varIdent.idprovider = () -> fSourceId;
        varIdent.internalGuiId = () -> ObjectId.createRemoteDestinationParameterId(step, fI).getObjectId();
        parameterMap.put(i, varIdent);
      }

      AVariableIdentification ident = parameterMap.get(i);
      Optional<AVariableIdentification> oldVar = sourceId == null ? Optional.empty() : providers.stream()
          .filter(x -> !(x.idprovider instanceof ThrowExceptionIdProvider) && sourceId.equals(x.idprovider.getId())).findAny();

      
      AVariableIdentification existingLink = null;
      if (oldVar.isPresent()) {
        existingLink = oldVar.get();
      }

      List<AVariableIdentification> candidates = getPossibleLinks(ident, providers, connections, existingLink, Collections.emptyList(), step);
      candidates = candidates.stream()
          .filter(x -> x.getIdentifiedVariable() != null && x.getIdentifiedVariable().isList() == ident.getIdentifiedVariable().isList())
          .collect(Collectors.toList());

      int count = candidates.size();
      
      if(existingLink == null && parameter.getIsConstantConnected()[i]) {
        existingLink = createConstConnInputVars(Utils.getGlobalConstVar(sourceId, gbo.getWFStep())).get(0);
      }
      
      //keep old connection
      if (existingLink != null && mayBeLinked(ident, existingLink)
          && ident.getIdentifiedVariable().isList() == existingLink.getIdentifiedVariable().isList()) {
        SimpleConnection sc = new SimpleConnection(LinkstateIn.NONE);
        sc.inputVars = new LinkedList<AVariableIdentification>();
        sc.inputVars.add(existingLink);
        if (parameter.getIsUserConnected()[i]) {
          sc.setLinkState(LinkstateIn.USER);
        } else if (parameter.getIsConstantConnected()[i]) {
          try {
            GeneralXynaObject constValue = existingLink.getIdentifiedVariable().getXoRepresentation();
            sc.setConstant(Utils.xoToJson(constValue, existingLink.getIdentifiedVariable().getCreator().getRevision()));
          } catch (Exception e) {
            sc.setConstant("");
          }
          sc.setLinkState(LinkstateIn.CONSTANT);
        } else {
          sc.inputVars.remove(existingLink);
          sc.inputVars.addAll(candidates);
          sc.setLinkState(count == 1 ? LinkstateIn.AUTO : LinkstateIn.AMBIGUE);
        }
        connections.put(ident, sc);
        return;
      }

      SimpleConnection sc = new SimpleConnection(LinkstateIn.NONE);
      sc.inputVars = new ArrayList<AVariableIdentification>(candidates);
      updateLinkStateofConnection(sc);
      connections.put(ident, sc);

    }

  }
  
  
  private void analyzeStepThrow(StepThrow currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections) {
    
    if(currentStep.getInputVarIds() == null || currentStep.getInputVarIds().length == 0)
      return;
    
    String id = currentStep.getInputVarIds()[0];
    
    if(id == null || id.length() == 0)
      return;
    
    AVariable avar;
    try {
      avar = currentStep.getParentScope().identifyVariable(id).getVariable();
      avar.setTargetId(currentStep.getStepId());
      currentStep.setExceptionID(id);
    } catch (XPRC_InvalidVariableIdException e) {
      currentStep.getInputVarIds()[0] = null;
      currentStep.setExceptionID(null);
    }
  }
  
  private List<AVariableIdentification> analyzeStepMapping(StepMapping step, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections){
    List<AVariableIdentification> result = new ArrayList<AVariableIdentification>();
    

    result = new ArrayList<AVariableIdentification>(providers);
    
    if(!step.isTemplateMapping())
      result.addAll(identifyVariables(step).getVariables(VarUsageType.output));
    //do not add output of Template Mapping. We have to find it again for StepForeach but hide it again there, unless it is inside a StepChoice  
    
    return result;
  }
  
  private List<AVariableIdentification> analyzeStep(Step currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections, boolean ignoreNewProviders) {
    logger.debug("Analyzing Step: " + currentStep + " - " + (ignoreNewProviders? "" : "not") + " ignoring new Providers");
    List<AVariableIdentification> newProviders;
    
    if(currentStep instanceof WFStep) {
      newProviders = analyzeWFStep((WFStep)currentStep, providers, connections);
    }
    else if(currentStep instanceof StepMapping){
      newProviders = analyzeStepMapping((StepMapping)currentStep, providers, connections);
    }
    else if(currentStep instanceof StepFunction) {
      newProviders = analyzeStepFunction((StepFunction)currentStep, providers, connections);
    }
    else if (currentStep instanceof StepChoice) {
      newProviders = analyzeStepChoice((StepChoice)currentStep, providers, connections);
    }
    else if(currentStep instanceof StepAssign) {
      //nothing to be done - providers do not change
      newProviders = new ArrayList<AVariableIdentification>(providers);
    }
    else if(currentStep instanceof StepParallel) {
      newProviders = analyzeStepParallel(currentStep, providers, connections);
    }
    else if(currentStep instanceof StepSerial) {
      newProviders = analyzeStepSerial(currentStep, providers, connections);
    }
    else if(currentStep instanceof StepForeach) {
      newProviders = analyzeStepForeach((StepForeach)currentStep, providers, connections);
    }
    else if(currentStep instanceof StepCatch) {
      newProviders = analyzeStepCatch(currentStep, providers, connections);
    }
    else if(currentStep instanceof StepThrow) {
      //providers = Collections.emptyList(); //no providers after throw -- maybe should not change providers instead?
      //old modeller: you can still use providers after throwing an exception (even though workflow cannot be deployed)
      analyzeStepThrow((StepThrow)currentStep, providers, connections);
      newProviders = new ArrayList<AVariableIdentification>(providers);
    }
    else if(currentStep instanceof StepRetry) {
      //old modeller: you can still use providers after retry (even though workflow cannot be deployed)
      newProviders = new ArrayList<AVariableIdentification>(providers);
    }
    else {
      throw new RuntimeException("unexpected Step class: " + currentStep.getClass()); //unexpected Step class
    }
    
    //result contains all providers for steps further down the WF
    return ignoreNewProviders ? providers : newProviders;
  }
  
  
  
  
  private void logOutput(List<AVariableIdentification> providers) {
    //logger.debug();
  }
  
  private void logInputsAfterResolution(Step currentStep, List<AVariableIdentification>providers, Map<AVariableIdentification, InputConnection> connections) {
    logger.debug("After connecting inputs, there are '" + providers.size() + "' providers and '" + connections.size() + "' connections.");
  }
  
  private List<AVariableIdentification> analyzeDataflow(Step currentStep, List<AVariableIdentification>providers, Map<AVariableIdentification, InputConnection> connections) {
    //log inputs
    logInputs(currentStep, providers, connections);
    
    //inputs -- this might change WF structure
    boolean ignoreNewProviders = resolveAllInputs(currentStep, providers, connections); // adds to connections
    
    //if a ForEach was created around a StepChoice, the outputs are not known until we examine the StepChoice
    if (ignoreNewProviders && currentStep instanceof StepChoice) {
      ignoreNewProviders = false;
    }
    
    //log Inputs after resolution
    logInputsAfterResolution(currentStep, providers, connections);
    
    //switch stepType -- iterate over child steps and return providers for next Step
    providers = analyzeStep(currentStep, providers, connections, ignoreNewProviders);
    
    //log outputs
    logOutput(providers);
    
    return providers;
    
  }
  
  
  

  /*
   * stepforeach inputs sind immer autoconnected. die user-connectedness kommt von dem input des steps innerhalb des foreaches.
   */
  private void resolveForeachInputVars(Step currentStep, List<AVariableIdentification> providers, IdentifiedVariables identifiedVariables,
                                       Map<AVariableIdentification, InputConnection> connections) {
    if (identifiedVariables == null) {
      return;
    }
    List<AVariableIdentification> variables = identifiedVariables.getVariables(VarUsageType.input);
    for (int i = 0; i < variables.size(); i++) {
      AVariableIdentification a = variables.get(i);
      SimpleConnection sc = new SimpleConnection(LinkstateIn.AUTO);
      boolean found = false;
      for (AVariableIdentification p : providers) {
        found = false;
        
        //p.idprovider.getId() => Id of provider (e.g. 27)
        //currentStep.getInputVarIds()[i] => Id of input field (e.g. 86)
        //connects the variable in the forEach line.
        //should connect a list (provider) with single (single version of same variable)
        if (p.idprovider.getId().equals(currentStep.getInputVarIds()[i])) {
          sc.addInputVar(p);
          found = true;
          break;
        }
        
      }
      if(found) {
        connections.put(a, sc); //connection from Input in forEach line to provider connectionC
      }else {
        found = true;   //debug line - if we come here, we did not save the forEach connection properly
      }
    }
  }
  
  
  private boolean belongsToForeach(AVariableIdentification avar, StepForeach sfe) {
    for(int i=0; i<sfe.getInputVarsSingle().length; i++) {
      AVariable ovar = sfe.getInputVarsSingle()[i];
      if(avar.getIdentifiedVariable() == ovar) {
        return true;
      }
    }
    return false;
  }
  
  private AVariableIdentification findExistingLink(String varId, List<AVariableIdentification> providers) {

    if (varId != null && varId.length() > 0) {
      for (AVariableIdentification p : providers) {
        if(p.idprovider instanceof ThrowExceptionIdProvider)
          continue;
        String idCandidate = null;
        try {
          idCandidate = p.idprovider.getId();
        } catch(ArrayIndexOutOfBoundsException e) {
          continue; //not the provider we are looking for
        }
        
        //user-defined sind bereits in allen möglichen Inputs zu finden, die varId passt bereits
        if (varId.equals(idCandidate)) {
          return p;
        }
      }
    }
    return null;
  }

  //nach dem autosnapping dieser variablen überprüfen, ob foreaches erzeugt oder entfernt werden müssen
  //returns true, if a ForEach was added
  private boolean checkForeaches(Step currentStep, IdentifiedVariables identifiedVariables, Map<AVariableIdentification, InputConnection> connections,
                                       List<AVariableIdentification> providers) {
    if(identifiedVariables == null)
      return false;
    
    //no ForEach if we get our input from an OrderInutSource
    if (currentStep instanceof StepFunction && ((StepFunction) currentStep).getOrderInputSourceRef() != null) 
      return false;
    
    boolean reresolveInputVars = false;
    boolean createdForeach = false;
    
    identifiedVariables.identify();
    
    for (AVariableIdentification input : identifiedVariables.getListAdapter(VarUsageType.input)) {
      //if
      //  input is connected to a ForEach variable 
      //  => meaning input is connected to a single variable that is connected to a list
      //    and
      //  connection state is ambiguous
      //then
      //  remove this ForEach (or at least what we iterate over)
      //
      // does this cover steps inside a choice under a ForEach?
      // => yes, it can not be connected to loop variable by AUTOCONNECTION
      // => adding additional providers does not influence a USERCONNECTION
      //
      // This happens if the variable we iterate over was unique, but becomes ambiguous -> adding another provider
      
      int index = -1;
      if(currentStep instanceof StepFunction)
        index = identifiedVariables.getListAdapter(VarUsageType.input).indexOf(input);
      else
        index = currentStep.getInputVars().indexOf(input.getIdentifiedVariable());
      
      if(index == -1)
        throw new RuntimeException("input not found");
      
      String varId = currentStep.getInputVarIds()[index];
      AVariableIdentification existingLink = findExistingLink(varId, providers);

      //check if we might need to modify/remove ForEach
      //isConnectedToList
      InputConnection inputConnection = connections.get(input);
      if(existingLink != null && !existingLink.getIdentifiedVariable().isList() && isConnectedToList(existingLink, connections) &&
          inputConnection.getConnectionForLane(0).getLinkState() == LinkstateIn.AMBIGUE)
      {
        //remove existingLink.getIdentifiedVariable()
        reresolveInputVars = reresolveInputVars | removeObsoleteForeachVariable(existingLink, currentStep, providers, connections);
      } //end of modify/remove ForEach
      
      
      if (mustCreateForeach(currentStep, input, connections)) {
        if(addToExistingForeach(currentStep, input, connections, providers)) {
          logger.debug("added " + input + " to existing foreach");
        } else {
          createStepForeach(currentStep, input, connections, providers);
          createdForeach = true;         
        }

        logger.debug("After generating foreach, there are '" + providers.size() + "' providers.");
        for(AVariableIdentification pro : providers) {
          logger.debug("  " + pro.getIdentifiedVariable().getId() +
                       " - " + pro.getIdentifiedVariable().getOriginalPath() + "." + pro.getIdentifiedVariable().getOriginalName() + 
                       "(" + pro.getIdentifiedVariable().getLabel() + ") isList: " + pro.getIdentifiedVariable().isList());
        }
      }
    } // end of existing input analysis
    
    //we might need to remove/modify a ForEach because of an input that we do not have anymore
    //-> identify all ForEach variables then check if they have a connection and it is of type AUTO.
    List<AVariableIdentification> providerCyp = new ArrayList<AVariableIdentification>(providers);  //removeObsoleteForeachVariable modifies providers.
    for(AVariableIdentification avar : providerCyp) {
      if(!(!avar.getIdentifiedVariable().isList() && isConnectedToList(avar, connections))) {
        continue; //Not a ForEach variable. Continue.
      }
      boolean isStillInUse = false;
      List<SimpleConnection> allCons = new ArrayList<SimpleConnection>();
      for(InputConnection iCon : connections.values()) {
        allCons.addAll(iCon.getConnectionsPerLane());
      }
      for(SimpleConnection sCon : allCons) {
        if(sCon.getInputVars().size() == 1 && sCon.getInputVar(0).getIdentifiedVariable().getId().equals(avar.getIdentifiedVariable().getId())) {
          isStillInUse = true; //do not remove variable from ForEach. It is still in use.
          break;
        }
      }
      
      if(isStillInUse)
        continue;
      
      //remove avar from providers. it is a ForEach variable that is no longer in use.
      reresolveInputVars = reresolveInputVars | removeObsoleteForeachVariable(avar, currentStep, providers, connections);
      
    }
    
    if(reresolveInputVars) {

      //remove old connections
      for(AVariableIdentification avar : identifiedVariables.getListAdapter(VarUsageType.input)) {
        connections.remove(avar);
      }
      
      resolveInputVars(currentStep, identifiedVariables, providers, connections);   // resolve input variables again
    }

    return createdForeach;
  }
  
  
  //returns true if entire ForEach was removed (-> inputs have to be reevaluated
  private boolean removeObsoleteForeachVariable(AVariableIdentification existingLink, Step currentStep, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections) {
    
    //make sure we do not separate StepCatch and StepFunction
    if(currentStep instanceof StepFunction && currentStep.getParentStep() instanceof StepCatch && ((StepCatch)currentStep.getParentStep()).getStepInTryBlock() == currentStep) {
      currentStep = currentStep.getParentStep();
    }
    
    //remove ForEach
    //if ForEach has multiple variables
    //  remove variable from ForEach
    //else
    //  remove entire ForEach

    //existing link is single version of ForEach loop variable - identify StepForeach to modify/delete
    StepForeach sfe = null;
    Collection<Step> steps = gbo.getStepMap().values();
    for(Step step : steps) {
      if(step instanceof StepForeach) {
        if(belongsToForeach(existingLink, (StepForeach)step)) {
          sfe = (StepForeach)step;
          break;
        }
      }
    }
    
    if(sfe == null) 
      throw new RuntimeException("StepForeach not found!");
    
    //TODO: duplicate in analyzeStepForeach
    //remove input
    //if it was the last input left, remove entire ForEach
    removeForeachInput(sfe, existingLink.getIdentifiedVariable());
    providers.remove(existingLink);
    
    //remove connection between ForEach input and single version of Variable
    connections.remove(existingLink);
    

    //remove entire ForEach if there is no input left
    if(sfe.getInputListRefs().length == 0) {
      logger.debug("Removed last Foreach Input. Removing Foreach Step entirely");
      
      //these variables were removed from their StepSerial when they were added to StepForeach Output
      //they are not returned to their StepSerial when removed, because they might be removed completely
      // -> this useCase covers removing StepForeach, not the output variables (of the child step)
      List<AVariable> varsToUpdate = new ArrayList<AVariable>();
      varsToUpdate.addAll(Arrays.asList(sfe.getOutputVarsSingle()));
      
      //remove variables before updating Scope
      removeStepForeachVariables(sfe);
      
      //pass private scopeStep variables to workflow
      for (AVariable var : sfe.getChildScope().getPrivateVars()) {
        sfe.getParentWFObject().getWfAsStep().getChildStep().addVar(var);
      }
      
      //this includes constants defined within the StepForeach
      for(AVariable var : sfe.getChildScope().getChildStep().getVariablesAndExceptions()) {
        sfe.getParentWFObject().getWfAsStep().getChildStep().addVar(var);
      }
      
      //if this step is under a ForEach, but there is another ForEach around that, and we have to remove the outer ForEach, 
      //we do not want to replace it with currentStep, but with our parent-ForEach.
      Step parentStep = sfe.getParentStep();
      Step replacement = getSurroundingStep(currentStep, sfe);
      if(replacement == sfe) 
        replacement = sfe.getChildScope().getChildStep().getChildSteps().get(0);
      
      parentStep.getChildSteps().set(parentStep.getChildSteps().indexOf(sfe), replacement); 
      replacement.setParentScope(parentStep.getParentScope());
      Utils.updateScopeOfSubSteps(replacement);
      
      //these variables were removed from their StepSerial when they were added to StepForeach output
      for(int i=0; i<varsToUpdate.size(); i++) {
        sfe.getParentWFObject().getWfAsStep().getChildStep().addVar(varsToUpdate.get(i));
      }

      return true;
    }
    
    return false;
    
  }
  
  //remove input from StepForeach
  //remove output form StepForeach
  //remove references to StepForeach inputVarsSingle (singleVersion of list we iterate over) in StepForEach childStep
  private void removeStepForeachVariables(StepForeach stepForeach) {
    //remove all outputs -> removes List versions of outputs and
    //remove generated input variables (single version of list variable)
    removeForeachOutput(stepForeach, stepForeach.getOutputVarsSingle());
    for(AVariable var : stepForeach.getInputVarsSingle()) {
      stepForeach.removeInput(var);
 
      
      RecursiveVisitor v = new RecursiveVisitor() {
        AVariableIdentification varToRemoveRef = identifyVariables(stepForeach).getListAdapter(VarUsageType.input).stream().filter(x -> x.getIdentifiedVariable() == var).findFirst().get();
        @Override
        public void visit(Step step) {
          String[] varIds = step.getInputVarIds();
          for (int i = 0; i < varIds.length; i++) {
            String varId = varIds[i]; //source id
            
            if (varId != null && varId.length() > 0) {
              if (varId.equals(varToRemoveRef.idprovider.getId())) {
                step.getInputVarIds()[i] = null;
                logger.debug("removed reference to " + var + " in " + step);
              }
            }
          }
        }
        
        @Override
        public boolean beforeRecursion(Step parent, Collection<Step> children) {
          return true;
        }
      };
      
      stepForeach.getChildScope().visit(v);
    }
  }
  
  
  //returns the outermost ForEach or currentStep, if there is no surrounding ForEach
  //stops at max
  public static Step getSurroundingStep(Step currentStep, Step max) {
    
    if(currentStep == max ||
        (currentStep.getParentStep() != null &&
        currentStep.getParentStep() == max) ||
        (currentStep.getParentStep() != null &&
        currentStep.getParentStep() != null &&
        currentStep.getParentStep().getParentStep() == max) ||
        (currentStep.getParentStep() != null &&
        currentStep.getParentStep().getParentStep() != null &&
        currentStep.getParentStep().getParentStep().getParentStep() != null &&
        currentStep.getParentStep().getParentStep().getParentStep() == max))
      return max;
    
    if(currentStep.getParentStep() == null || 
        !(currentStep.getParentStep() instanceof StepSerial) ||
        currentStep.getParentStep().getParentStep() == null ||
        !(currentStep.getParentStep().getParentStep() instanceof ForEachScopeStep) ||
        currentStep.getParentStep().getParentStep().getParentStep() == null ||
        !(currentStep.getParentStep().getParentStep().getParentStep() instanceof StepForeach))
    {
      return currentStep;
    }
    
    return getSurroundingStep(currentStep.getParentStep().getParentStep().getParentStep(), max);
  }
  

  private Optional<StepForeach> getSurroundingStepForeach(Step step) {
    if (step instanceof StepFunction && step.getParentStep() instanceof StepCatch) {
      step = step.getParentStep();
    }
    try {
      if (step.getParentStep().getParentStep() instanceof ForEachScopeStep) {
        return Optional.of((StepForeach) step.getParentStep().getParentStep().getParentStep());
      }
    } catch (Exception e) {
      //return empty
    }

    return Optional.empty();
  }

  
  
  private void addToStepForeach(Step currentStep, AVariableIdentification input,
                                Map<AVariableIdentification, InputConnection> connections,
                                SimpleConnection c, List<AVariableIdentification> providers) {

    StepForeach sfe = getSurroundingStepForeach(currentStep).get();
    AVariableIdentification listAVar = c.getInputVar(0);
    
    sfe.addInput(listAVar.getIdentifiedVariable());
    String idOfSingleVariableInSFEchildInput = c.getInputVar(0).idprovider.getId();
    sfe.getInputVarIds()[sfe.getInputVarIds().length-1] = idOfSingleVariableInSFEchildInput;
    
    //id of single-version of input (list) variable
    final String id = sfe.getInputVars().get(sfe.getInputVars().size() - 1).getId();
    

    //change currentStep to have a connection to single-version of that variable
    AVariable inputVar = input.getIdentifiedVariable();
    int index = currentStep.getInputVars().indexOf(inputVar);
    if(index != -1)
      currentStep.getInputVarIds()[index] = id;
    else {
      //processing a stepFunction
      //find id in getInputVarIds, that is equal to ..c.getTargetId()..
      for(int i=0; i<currentStep.getInputVarIds().length; i++) {
        String oldId = currentStep.getInputVarIds()[i];
        if(idOfSingleVariableInSFEchildInput.equals(oldId)) {
          currentStep.getInputVarIds()[i] = id;
          break;
        }
      }
    }
    
    
    //update ForEach identified in- and outputs
    IdentifiedVariables identifiedVariables = identifyVariables(sfe);
    identifiedVariables.identify();
    
    //add output of ForEach to providers
    List<AVariableIdentification> hiddenVariables = findHiddenDocumentParts(sfe.getChildScope().getChildStep(), providers);
    List<AVariableIdentification> outputsWithoutHidden = getForeachOutputWithoutHidden(sfe, hiddenVariables);
    providers.addAll(outputsWithoutHidden);
    
    
    
    //handle connections - update old [was: single in currentStep to list, should: single in currentStep to single version of list variable]
    //                     create new [single version of list variable to list we iterate over]
    AVariableIdentification listIteratingOver = c.getInputVars().get(0);
    
    //connections are currently not updated during forEach step creation
    //update connection (input of Step inside ForEach to single version of ForEach variable)
    IdentifiedVariables identVars = gbo.getVariableMap().identifyVariables(ObjectId.createStepId(sfe));
    AVariableIdentification singleVersionOfLoopVar = identVars.getVariable(VarUsageType.input, identVars.getListAdapter(VarUsageType.input).size() - 1);
    c.getInputVars().clear();
    c.getInputVars().add(singleVersionOfLoopVar);
    
    //create connection between List we iterate over and the single version of it -> (expected to) prevent single loop variable to appear unconnected (red)
    //connection is wrong + state of loop variable does not change.
    SimpleConnection newCon = new SimpleConnection(LinkstateIn.AUTO);
    newCon.setBranchId(null);
    newCon.setInputVars(new ArrayList<AVariableIdentification>());
    newCon.getInputVars().add(listIteratingOver);
    connections.put(singleVersionOfLoopVar, newCon);
    
    
  }
  

  //a ForEach may iterate over same variable multiple times
  private boolean shouldAddToExistingForeach(Step currentStep, StepForeach stepForeach, SimpleConnection c) {
    AVariableIdentification listAVar = c.getInputVar(0);
    //if listAVar is input of stepForeach, StepForeach should be extended.
    String[] foreachListInputIds = stepForeach.getInputVarIds();
    for (int i=0; i<foreachListInputIds.length; i++) {
      String foreachListInput = foreachListInputIds[i];
      if (foreachListInput.equals(listAVar.idprovider.getId())) {
        return true;
      }
    }

    return false;
  }
  

  private boolean newStepForeachRequired(Step currentStep, SimpleConnection c) {
    Optional<StepForeach> oldStepForeach = getSurroundingStepForeach(currentStep);
    if (oldStepForeach.isEmpty() || !shouldAddToExistingForeach(currentStep, oldStepForeach.get(), c)) {
      //new ForEach required
      return true;
    } else {
      //should add to existing ForEach
      return false;
    }
  }
  

  private boolean addToExistingForeach(Step currentStep,
                                       AVariableIdentification input, 
                                       Map<AVariableIdentification, InputConnection> connections, 
                                       List<AVariableIdentification> providers) {
    SimpleConnection c = connections.get(input).getConnectionForLane(-1);
    if (!newStepForeachRequired(currentStep, c)) {
      addToStepForeach(currentStep, input, connections, c, providers);
      return true;
    }
    return false;
  }
  
  /*
   * - create StepForeach
   * - create inputVarsSingle - for Input and Output (AVariable)
   * - create IdentifiedVariablesStepForeach with AVariableIdentification objects (4 types -> input/output,single/list)
   * - update connection (key input) to point at freshly created inputVarsSingle entry
   * - create connection between (AvariableIdentification of) inputVarsSingle entry and former target of updated connection
   * - adjust output IDs
   */
  private StepForeach createStepForeach(Step currentStep, AVariableIdentification input,
                                        Map<AVariableIdentification, InputConnection> connections,
                                        List<AVariableIdentification> providers) {
    logger.debug("Creating StepForeach around " + currentStep + " beacause of " + input);
    logger.debug("providers at the start of createStepForeach:");
    logger.debug(String.join("\n  ", providers.stream().map(x -> x.toString()).collect(Collectors.toList())));
    Step stepToReplace = currentStep;
    Step oldParent = currentStep.getParentStep();
    
    // identify connection responsible for creating this StepForeach
    SimpleConnection c = connections.get(input).getConnectionForLane(-1);
    
    //Find id of variable in currentStep responsible for creating this ForEach
    String idOfSingleVariableInSFEchildInput = c.getInputVar(0).idprovider.getId();
    
    //put ForEach around entire StepCatch block if we are at a function step (and not in a catch lane) 
    if(currentStep instanceof StepFunction && oldParent instanceof StepCatch && ((StepCatch)oldParent).getStepInTryBlock() == currentStep) {
      oldParent = oldParent.getParentStep();
      stepToReplace = currentStep.getParentStep();
    }

    //create StepForEach
    StepForeach sfe = new StepForeach(currentStep.getParentScope(), currentStep.getCreator());
    sfe.createEmpty();

    //add new ForEach step to GenerationBaseObject.
    gbo.getStepMap().addStep(sfe);
    
    //Input: creates inputVarSingle (AVariable)
    sfe.addInput(c.getInputVar(0).getIdentifiedVariable());

    //set id of variable responsible for ForEach in StepForeach
    sfe.getInputVarIds()[0] = idOfSingleVariableInSFEchildInput;
    
    //id of single-version of input (list) variable
    final String id = sfe.getInputVars().get(sfe.getInputVarIds().length - 1).getId();
    
    //set childStep of StepForeach to the step we are replacing    
    sfe.getChildScope().getChildStep().addChild(0, stepToReplace);
    
    //replace stepToReplace with StepForeach
    int idx = oldParent.getChildSteps().indexOf(stepToReplace);
    oldParent.getChildSteps().set(idx, sfe);

    //change currentStep to have a connection to single-version of that variable
    if (currentStep instanceof StepFunction) {
      //find id in getInputVarIds, that is equal to ..c.getTargetId()..
      for (int i = 0; i < currentStep.getInputVarIds().length; i++) {
        String oldId = currentStep.getInputVarIds()[i];
        if (idOfSingleVariableInSFEchildInput.equals(oldId)) {
          currentStep.getInputVarIds()[i] = id;
          break;
        }
      }
    } else {
      AVariable inputVar = input.getIdentifiedVariable();
      int index = currentStep.getInputVars().indexOf(inputVar);
      if (index != -1) {
        currentStep.getInputVarIds()[index] = id;
      } else {
        throw new RuntimeException("could not update connection for step inside when creating StepForeach");
      }
    }
    

    //currentStep seinen neuen ParentScope geben (wird für StepFunction benötigt)
    //muss nach umsetzten der InputVarIds passieren, da sonst die alte AVariable nicht im Scope gefunden wird
    stepToReplace.setParentScope(sfe.getChildScope());
    Utils.updateScopeOfSubSteps(stepToReplace);
    
    //Output: add outputs
    List<AVariableIdentification> outputVarsOfForeachChild = null;
    if(currentStep instanceof StepFunction ) {
      if(currentStep.isExecutionDetached()) {
        outputVarsOfForeachChild = new ArrayList<AVariableIdentification>();
      } else {
        outputVarsOfForeachChild = getRealOutputVarsOfStepFunction((StepFunction)currentStep);
      }
    }
    else
      outputVarsOfForeachChild = identifyVariables(currentStep).getListAdapter(VarUsageType.output);

    List<AVariableIdentification> outputAfterForeaches = addOutputToStepForeach(sfe, outputVarsOfForeachChild, connections, 0);
    
    //update ForEach identified in- and outputs
    IdentifiedVariables identifiedVariables = identifyVariables(sfe);
    identifiedVariables.identify();
    
    //add output of ForEach to providers
    providers.addAll(outputAfterForeaches);
    
    
    //handle connections - update old [was: single in currentStep to list, should: single in currentStep to single version of list variable]
    //                     create new [single version of list variable to list we iterate over]
    AVariableIdentification listIteratingOver = c.getInputVars().get(0);
    
    //connections are currently not updated during forEach step creation
    //update connection (input of Step inside ForEach to single version of ForEach variable)
    AVariableIdentification singleVersionOfLoopVar = gbo.getVariableMap().identifyVariables(ObjectId.createStepId(sfe)).getVariable(AVariableIdentification.VarUsageType.input, 0);
    c.getInputVars().clear();
    c.getInputVars().add(singleVersionOfLoopVar);
    
    //create connection between List we iterate over and the single version of it -> (expected to) prevent single loop variable to appear unconnected (red)
    //connection is wrong + state of loop variable does not change.
    SimpleConnection newCon = new SimpleConnection(LinkstateIn.AUTO);
    newCon.setBranchId(null);
    newCon.setInputVars(new ArrayList<AVariableIdentification>());
    newCon.getInputVars().add(listIteratingOver);
    connections.put(singleVersionOfLoopVar, newCon);
    createdForeaches.add(sfe);
    
    logger.debug("providers at the end of createStepForeach:");
    logger.debug(String.join("\n  ", providers.stream().map(x -> x.toString()).collect(Collectors.toList())));
    
    return sfe;
  }
  

  private List<AVariableIdentification> getForeachOutputWithoutHidden(StepForeach stepForeach, List<AVariableIdentification> hiddenDocumentParts){ 
    IdentifiedVariables identifiedVariables = identifyVariables(stepForeach);
    List<AVariableIdentification> outputsWithoutHidden = new ArrayList<AVariableIdentification>(identifiedVariables.getListAdapter(VarUsageType.output));
    List<AVariableIdentification> listVarsToHide = new ArrayList<AVariableIdentification>();
    for(AVariableIdentification documentPartToHide : hiddenDocumentParts) {
      AVariableIdentification varToHide = null;
      String idToFind = null;
      
      try {
        idToFind = documentPartToHide.idprovider.getId();
      }
      catch(Exception e) {  } //TODO: 
      
      if(idToFind == null) {
        continue; //should not happen
      }
      
      List<AVariable> outputs = stepForeach.getOutputVarsSingle(false);
      int index = -1;
      for(AVariable avar : outputs) {
        if(avar.getId() != null && avar.getId().equals(idToFind)) {
          index = outputs.indexOf(avar);
          break;
        }
      }
      
      if(index == -1) {
        //TODO: why does this happen? This should not happen.
        continue;
      }
      
      varToHide = outputsWithoutHidden.get(index);
      listVarsToHide.add(varToHide);
    }
    outputsWithoutHidden.removeAll(listVarsToHide);
    
    return outputsWithoutHidden;
  }
 
  private boolean mustCreateForeach(Step currentStep, AVariableIdentification input, Map<AVariableIdentification, InputConnection> connections) {
    if(!connections.containsKey(input))
      return false;
     
    SimpleConnection connection = connections.get(input).getConnectionForLane(-1);
    switch (((LinkstateIn)connection.linkstate)) {
      case AMBIGUE :
      case CONSTANT :
      case INPUTSOURCE :
      case NO_NEED :
      case NONE :
        return false;
      case AUTO :
      case USER :
        return connection.foreachLink(input, 0);
      default :
        throw new RuntimeException();
    }
  }
  
  //alles außer den angegebenen ids
  protected Set<AVariableIdentification> getFilteredVarIdents(List<AVariableIdentification> identifications, String[] idsToRemove) {
    Set<AVariableIdentification> filteredIdentifications = new HashSet<AVariableIdentification>();
    for (AVariableIdentification identification : identifications) {
      boolean keepIdentification = true;
      for (String idToRemove : idsToRemove) {
        if (identification.idprovider.getId().equals(idToRemove)) {
          keepIdentification = false;
          break;
        }
      }

      if (keepIdentification) {
        filteredIdentifications.add(identification);
      }
    }

    return filteredIdentifications;
  }

  private boolean endsWithThrowOrRetry(Step step) {
    if (step instanceof StepSerial) {
      StepSerial ss = (StepSerial) step;
      List<Step> childSteps = ss.getChildSteps();
      for (int i = childSteps.size() - 1; i >= 0; i--) {
        Step child = ss.getChildSteps().get(i);
        if (child instanceof StepAssign) {//ignore
          continue;
        }
        if (child instanceof StepThrow) {
          return true;
        }
        if (child instanceof StepRetry) {
          return true;
        }
        if (child instanceof StepParallel) { //mindestens eine lane hat ein throw?
          for (Step lane : child.getChildSteps()) {
            if (lane instanceof StepSerial && endsWithThrowOrRetry(lane)) {
              return true;
            }
          }
          return false;
        }
        if (child instanceof StepChoice) { //alle lanes haben ein throw?
          for (Step lane : child.getChildSteps()) {
            if (!endsWithThrowOrRetry(lane)) {
              return false;
            }
          }
          return true;
        }
        return false;
      }
    } else {
      throw new RuntimeException("Unexpected step type " + step.getClassName());
    }
    return false;
  }


  protected List<AVariableIdentification> removePrototypeVars(List<AVariableIdentification> vars) {
    List<AVariableIdentification> varsWithoutPrototypes = new ArrayList<AVariableIdentification>();
    for (AVariableIdentification var : vars) {
      if (!var.getIdentifiedVariable().isPrototype()) {
        varsWithoutPrototypes.add(var);
      }
    }

    return varsWithoutPrototypes;
  }

  public IdentifiedVariables identifyVariables(Step currentStep) {
    try {
      
      IdentifiedVariables identifiedVariables;
      if ( (currentStep instanceof StepSerial) && (currentStep.getParentWFObject().getWfAsStep().getChildStep() == currentStep)) {
        // get wf-variables when variables for main wrapper-step are requested
        identifiedVariables = gbo.getVariableMap().identifyVariables(ObjectId.createStepId(currentStep.getParentWFObject().getWfAsStep()));
      } else {
        identifiedVariables = gbo.getVariableMap().identifyVariables(ObjectId.createStepId(currentStep));
      }

      if( identifiedVariables == null ) {
        problems.add( new DataflowEntry(currentStep.toString(), "Cannot identify variables"));
      }
      return identifiedVariables;
    } catch( Exception e ) {
      problems.add( new DataflowEntry(currentStep.toString(), e.getMessage()) );
      return null;
    }
  }


  private void addToConnections(IdentifiedVariablesStepChoice identifiedVariablesStepChoice, Map<AVariableIdentification, InputConnection> connections) {
    Map<AVariableIdentification, InputConnection> connectionsChoice = identifiedVariablesStepChoice.getConnections();
    if (connectionsChoice == null) {
      return;
    }
    
    for (AVariableIdentification source : connectionsChoice.keySet()) {
      connections.put(source, connectionsChoice.get(source));
    }
  }

  private void resolveInputVars(Step currentStep, IdentifiedVariables identifiedVariables, 
                                List<AVariableIdentification> providers, 
                                Map<AVariableIdentification, InputConnection> connections) {
    if( identifiedVariables == null ) {
      return;
    }
    identifiedVariables.identify(); //update identified variables
    List<AVariableIdentification> inputs = identifiedVariables.getVariables(VarUsageType.input); 
    if( inputs.isEmpty() ) {
      return;
    }
    
    if (currentStep instanceof StepFunction && ((StepFunction) currentStep).getOrderInputSourceRef() != null && ((StepFunction) currentStep).getOrderInputSourceRef().length() > 0) {
      for (AVariableIdentification input : inputs) {
        connections.put(input, new SimpleConnection(LinkstateIn.INPUTSOURCE));
      }
      return;
    }

    //handle conditional mapping -> hidden before query
    List<AVariableIdentification> actualProviders = new ArrayList<AVariableIdentification>();
    boolean isConditionalMapping = false;
    if(currentStep instanceof StepMapping && ((StepMapping)currentStep).isConditionMapping())
      isConditionalMapping = true;
    
    
    String[] varIds = currentStep.getInputVarIds();
    for (int i = 0; i < varIds.length; i++) {
      String varId = varIds[i]; //source id
      AVariableIdentification in = inputs.get(i);
      
      //no not care about prototypes
      if(in.getIdentifiedVariable().isPrototype() || !Utils.variableExists(in.getIdentifiedVariable())) {
        //could set connection to noNeed instead: connections.put(in, new SimpleConnection(LinkstateIn.NO_NEED));
        continue;
      }
      
      actualProviders.clear();
      actualProviders.addAll(providers);
      
      //remove list providers if we process a single input of a conditional mapping - we can not create a ForEach link here
      if(isConditionalMapping && !in.getIdentifiedVariable().isList()) {
        actualProviders.removeIf(x -> x.getIdentifiedVariable().isList());
      }
      
      
      AVariableIdentification existingLink = null;
      if (varId != null && varId.length() > 0) {
        for (AVariableIdentification p : actualProviders) {
          //user-defined sind bereits in allen möglichen Inputs zu finden, die varId passt bereits
          if(p.idprovider instanceof ThrowExceptionIdProvider)
            continue;
          if (varId.equals(p.idprovider.getId())) {
            existingLink = p;
            break;
          }
        }
      }
      SimpleConnection sc = getOrCalculateLinkState(in, existingLink, actualProviders, connections, null, false, inputs, currentStep);

      NewProvidersForConnectionData data = new NewProvidersForConnectionData();
      data.setNewConnection(sc);
      data.setNewProviders(actualProviders);
      data.setStep(currentStep);
      data.setVariableIndex(i);
      data.setVariableToConnect(in);
      data.setUsageType(VarUsageType.input);
      warningsManagement.processNewProvidersForConnection(data);
      
      //if there is a connection -- maybe wrong id -> get id from step defining this variable
      if(sc != null && (sc.getLinkState().equals(LinkstateIn.AUTO) || sc.getLinkState().equals(LinkstateIn.USER))) {
          currentStep.getInputVarIds()[i] = sc.getInputVars().get(0).idprovider.getId();
      }
    }
  }



  private List<AVariableIdentification> identifyVariableIds(String[] varIds, List<AVariableIdentification> providers, ScopeStep lastScope, VarUsageType usage) {
    List<AVariableIdentification> vars = new ArrayList<>();
    int idx =0;
    for (String varId : varIds) {
      vars.add(identifyVariableId(varId, idx, providers, lastScope, usage) );
      ++idx;
    }
    return vars;
  }
  
  private AVariableIdentification identifyVariableId(final String varId, final int idx, List<AVariableIdentification> providers, ScopeStep lastScope, final VarUsageType usage) {
    try {
      VariableIdentification vi = lastScope.identifyVariable(varId);
      if (vi != null && vi.getVariable() != null) {
        if( providers != null ) {
          for( AVariableIdentification var : providers ) {
            if (var.idprovider instanceof ThrowExceptionIdProvider) {
              continue;
            }
            if( var.idprovider.getId() != null && var.idprovider.getId().equals(vi.getVariable().getId()) ) {
              return var;
            }
          }
        }
        AVariableIdentification var = gbo.getVariableMap().scan(varId);
        if (var == null) {
          var = DirectVarIdentification.of(vi.getVariable());
          var.idprovider = () -> varId;
          var.internalGuiId = () -> ObjectId.createVariableId("Missing" + varId, usage, idx);

          missingVarIds.add(var);
        }
        return var;
      } else {
        return null;
      }
    } catch (XPRC_InvalidVariableIdException e) {
      // TODO ???
      return null;
    }
  }
  
  private void getOrCalculateLinkState(Pair<AVariableIdentification, AVariableIdentification> existingConnection, 
                                       List<AVariableIdentification> providers,
                                       Map<AVariableIdentification, InputConnection> connections, 
                                       String branchId, 
                                       boolean treatExistingConnectionAsUserConnected, 
                                       List<AVariableIdentification> stepInputs,
                                       Step currentStep) {
    getOrCalculateLinkState(existingConnection.getFirst(), existingConnection.getSecond(), providers, connections, branchId, treatExistingConnectionAsUserConnected, stepInputs, currentStep);
  }

  
  private SimpleConnection getOrCalculateLinkState(AVariableIdentification toSatisfy, AVariableIdentification existingLink, 
                                                   List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> connections, 
                                                   String branchId, 
                                                   boolean treatExistingConnectionAsUserConnected,
                                                   List<AVariableIdentification> stepInputs,
                                                   Step currentStep) {
    if (existingLink != null && providers.contains(existingLink) && mayBeLinked(toSatisfy, existingLink)) {
      if( existingLink.equals(toSatisfy) ) {
        throw new IllegalStateException(toSatisfy +" # " + existingLink);
      }
      
      SimpleConnection sc = getSimpleConnection(toSatisfy, connections, branchId);
      
      if (treatExistingConnectionAsUserConnected) {
        if (existingLink.connectedness.isConstantConnected()) {
          sc.setLinkState(LinkstateIn.CONSTANT);
        } else if (getPossibleLinks(toSatisfy, providers, connections, existingLink, stepInputs, currentStep).size() > 1) { 
          sc.setLinkState(LinkstateIn.USER);
        } else {
          sc.setLinkState(LinkstateIn.AUTO);
        }
        sc.addInputVar(existingLink);
        return sc;
      } else if (branchId == null && toSatisfy.connectedness.isUserConnected()) {
        sc.setLinkState(LinkstateIn.USER);
        sc.addInputVar(existingLink);
        return sc;
      } else if (existingLink != null && existingLink.connectedness.isUserConnected()) {
        sc.setLinkState(LinkstateIn.USER);
        sc.addInputVar(existingLink);
      } else {
        return calculateLinkState(toSatisfy, providers, connections, branchId, existingLink, stepInputs, currentStep);        
      }
    } else if (existingLink != null && existingLink.connectedness.isConstantConnected()) {
      SimpleConnection constantConnection = getSimpleConnection(toSatisfy, connections, branchId);
      constantConnection.setLinkState(LinkstateIn.CONSTANT);
      constantConnection.addInputVar(existingLink);
      try {
        GeneralXynaObject constValue = existingLink.getIdentifiedVariable().getXoRepresentation();
        constantConnection.setConstant(Utils.xoToJson(constValue, existingLink.getIdentifiedVariable().getCreator().getRevision()));
      } catch (Exception e) {
        constantConnection.setConstant("");
      }
      
      return constantConnection;
    } else if (branchId == null && toSatisfy.connectedness.isConstantConnected()) { //bei branchId!=null steht die constant-connectedness am assign-input (oberer fall)
      SimpleConnection constantConnection = getSimpleConnection(toSatisfy, connections, branchId);
      AVariable globalConstVar = null;
      constantConnection.setLinkState(LinkstateIn.CONSTANT);
      globalConstVar = Utils.getGlobalConstVar(toSatisfy.connectedness.getConnectedVariableId(), gbo.getWFStep());
      constantConnection.setInputVars(createConstConnInputVars(globalConstVar));
      try {
        GeneralXynaObject constValue = globalConstVar.getXoRepresentation();
        constantConnection.setConstant(Utils.xoToJson(constValue, globalConstVar.getCreator().getRevision()));
      } catch (Exception e) {
        constantConnection.setConstant("");
      }
      constantConnection.setInputVars(createConstConnInputVars(globalConstVar));
      return constantConnection;
    } else {
      return calculateLinkState(toSatisfy, providers, connections, branchId, existingLink, stepInputs, currentStep);
    }
    return null;
  }

  private SimpleConnection getSimpleConnection(AVariableIdentification toSatisfy, Map<AVariableIdentification, InputConnection> connections,
                                               String branchId) {
    InputConnection entry = connections.get(toSatisfy);
    if (entry == null) {
      SimpleConnection sc = new SimpleConnection(LinkstateIn.NONE);
      if (branchId == null) {
        connections.put(toSatisfy, sc);
      } else {
        MultiLaneConnection mlc = new MultiLaneConnection();
        mlc.addLaneConnection(sc, branchId);
        connections.put(toSatisfy, mlc);
      }
      return sc;
    } else if (entry instanceof MultiLaneConnection) {
      MultiLaneConnection mlc = (MultiLaneConnection) entry;
      SimpleConnection sc = mlc.getSimpleConnectionByBranchId(branchId);
      if (sc == null) {
        sc = new SimpleConnection(LinkstateIn.NONE);
        mlc.addLaneConnection(sc, branchId);
      }
      return sc;
    } else if (entry instanceof SimpleConnection && branchId == null) {
      return (SimpleConnection) entry;
    } else if (entry instanceof SimpleConnection && branchId != null) {
      //upgrade auf multilaneconnection (passiert, wenn zuerst normaler output von wf gespeichert wurde, und danach pro catchblock)
      MultiLaneConnection mlc = new MultiLaneConnection();
      SimpleConnection sc = new SimpleConnection(LinkstateIn.NONE);
      mlc.addLaneConnection((SimpleConnection) entry, null);
      mlc.addLaneConnection(sc, branchId);
      connections.put(toSatisfy, mlc);
      return sc;
    } else {
      throw new RuntimeException();
    }
  }

  private SimpleConnection calculateLinkState(AVariableIdentification toSatisfy, 
                                              List<AVariableIdentification> providers, 
                                              Map<AVariableIdentification, InputConnection> connections, 
                                              String branchId, 
                                              AVariableIdentification existingLink, 
                                              List<AVariableIdentification> stepInputs,
                                              Step currentStep) {
    SimpleConnection sc = getSimpleConnection(toSatisfy, connections, branchId);
    for (AVariableIdentification link : getPossibleLinks(toSatisfy, providers, connections, existingLink, stepInputs, currentStep)) {
      sc.addInputVar(link);
      
      //prevent multiple entries from changing result //TODO: prevent multiple entries in the first place
      Set<AVariableIdentification> inputsAsSet = new HashSet<AVariableIdentification>(sc.getInputVars());
      sc.setInputVars(new ArrayList<AVariableIdentification>(inputsAsSet));

      updateLinkStateofConnection(sc);
    }
    
    return sc;
  }
  
  private void updateLinkStateofConnection(SimpleConnection sc) {
    switch( sc.getInputVars().size() ) {
      case 0:
        sc.setLinkState(LinkstateIn.NONE);
        break;
      case 1:
        sc.setLinkState(LinkstateIn.AUTO);
        break;
      default:
        sc.setLinkState(LinkstateIn.AMBIGUE);
      }
  }


  private List<AVariableIdentification> getPossibleLinks(AVariableIdentification toSatisfy, 
                                                         List<AVariableIdentification> providers,  
                                                         Map<AVariableIdentification, InputConnection> connections, 
                                                         AVariableIdentification existingLink,
                                                         List<AVariableIdentification> stepInputs,
                                                         Step currentStep) {
    List<AVariableIdentification> result = new ArrayList<>();
    for (AVariableIdentification provider : providers) {
      if (provider == null) {
        continue;
      }
      if (provider.equals(toSatisfy)) {
        continue;
      }
      
      if(foreachSkip(toSatisfy, provider, providers, connections, existingLink, stepInputs, currentStep)) {
        logger.debug("foreachSkip was true for: " + provider.getIdentifiedVariable() + " @ step " + currentStep);
        continue;
      }
      logger.debug("foreachSkip was false for: " + provider.getIdentifiedVariable() + " @ step " + currentStep);

      // Verbindungskombinationen:
      // single -> single: Verbindung moeglich
      // single -> list:   nicht kompatibel
      // list   -> single: Verbindung moeglich (erzeugt Foreach wenn verbunden)
      // list   -> list:   Verbindung moeglich
      if (mayBeLinked(toSatisfy, provider)) {
        result.add(provider);
      }
    }
    return result;
  }
  
  //true => hide provider from toSatisfy
  //  -- because it is the list version of a loop variable (and toSatisfy is connected to the single version of it)
  //  -- because it is the single version of a loop variable (and toSatisfy is not connected to it)
  //
  //wenn wir im ForEach sind, müssen folgende Fälle betrachtet werden:
  //  für die Variable, die mit der Singlewertigen Loopvariable verbunden ist: ignoriere die Listenwertige Variable ( aktueller code )
  //  für andere singlewertige Variablen mit gleichem Typ: ignoriere die Singlewertige Variable
  //    dies gilt nur für das direkte Kind von ForEach, weitere Schritte darunter (im ForEach, aber nicht das direkte Kind) können beide Variablen sehen
  //    das betrifft zum Beispiel StepChoice
  private boolean foreachSkip(AVariableIdentification toSatisfy, 
                              AVariableIdentification provider, 
                              List<AVariableIdentification> providers,  
                              Map<AVariableIdentification, InputConnection> connections, 
                              AVariableIdentification existingLink, 
                              List<AVariableIdentification> stepInputs,
                              Step currentStep) {

    //never hide anything from a list
    if(toSatisfy.getIdentifiedVariable().isList())
      return false;
    
    
    //if we are not directly under a stepForeach, do not hide
    if(!isDirectChildOfForeach(currentStep)) {
      return false;
    }

    
    AVariableIdentification toSatisfySource = existingLink;
    if(existingLink != null && !connections.containsKey(existingLink)) {
      //existingLink might not 'equal' entry in connections => matching ID is sufficient TODO: idProvider instead?
      for(AVariableIdentification avar : connections.keySet()) {
        if( avar.getIdentifiedVariable() != null && 
            avar.getIdentifiedVariable().getId() != null && 
            avar.getIdentifiedVariable().getId().equals(existingLink.getIdentifiedVariable().getId())) {
          toSatisfySource = avar;
          break;
        }
      }     
    }
    
  
    //hide single Versions of loop Variable from other others [same step], so we do not try to iterate over the same (single) variable multiple times
    //if 
    //   provider is single and 
    //   connections contains an entry connecting provider to a list variable and
    //   toSatisfy is not connected to provider (-> if it is [toSatisfy is connected to provider], provider is what we want to be connected with)
    //   stepInputs does contain existingLink (-> we skip only if this step is already linked to that provider)
    //      OR variable should not exist => StepForeach will be removed, but we don't know that yet
    //        => provider is not a list, connected to list but there is no variable that points to it
    //than
    //   return true. hide the single loop variable from everyone, except the variable it is connected to
    
    if(!provider.getIdentifiedVariable().isList() && isConnectedToList(provider, connections) && 
        (connectedToOtherInputOfThisStep(toSatisfy, provider, stepInputs) ||
         isObsoleteForeachVariable(provider, stepInputs, connections)))
      return true;
    

    //hide list Version of loop Variable from toSatisfy, if we are connected to the single version of it
    //if
    //  provider is a list and
    //  there is a connection between toSatisfy and some single Variable and
    //  that variable is connected to provider
    //then
    //  return true. hide list version of variable we are iterating over
    if(provider.getIdentifiedVariable().isList() &&
        toSatisfySource != null && !toSatisfySource.getIdentifiedVariable().isList() &&
        getSource(toSatisfySource, connections) != null && getSource(toSatisfySource, connections) == provider)
      return true;
    
    
    // if there is no reason, do not hide this provider
    return false;
  }
  
  
  private boolean isDirectChildOfForeach(Step currentStep) {
    Step parentStep = currentStep.getParentStep();
    if(parentStep == null) {
      return false;
    }
    
    //we are wrapped inside a StepCatch! - unless we are a prototype service
    if(currentStep instanceof StepFunction) {
      if(parentStep instanceof StepCatch) {
        currentStep = parentStep;
        
        if(currentStep == null || parentStep == null) {
          return false;
        }
      }
    }
    
    //parent might be: stepSerial     .  scopeStep    . stepForeach
    return currentStep.getParentStep().getParentStep().getParentStep() instanceof StepForeach;
  }

  //TODO: maybe this should only be used for 'actual steps' -> not StepForeach, ScopeSteps, StepAssign, ...
  //if there is no variable with connectedness.getConnectedVariableId() == avar
  private boolean isObsoleteForeachVariable(AVariableIdentification avar, List<AVariableIdentification> stepInputs, Map<AVariableIdentification, InputConnection> connections) {
    
    for(AVariableIdentification candidate : stepInputs) {
      if(candidate.connectedness != null && candidate.connectedness.getConnectedVariableId() != null && candidate.connectedness.getConnectedVariableId().equals(avar.getIdentifiedVariable().getId()))
        return false; //there is a connection between avar and an input of this step
    }
    
    // ForEach Variable may still not be obsolete:
    // if there is a step above us creating it. In that case we already processed that step and there is a connection between some variable and avar.
    for(InputConnection iCon : connections.values()) {
      for(SimpleConnection sCon: iCon.getConnectionsPerLane()) {
        for(AVariableIdentification usedVariable : sCon.getInputVars()) {
          if(usedVariable == avar || 
              usedVariable.getIdentifiedVariable().getId() != null && usedVariable.getIdentifiedVariable().getId().equals(avar.getIdentifiedVariable().getId())) 
          {
            return false; //variable is still in used (by a parent step of this)
          }
        }
      }
    }
    
    return true;
  }
  
  //returns true if provider is connected to entry in stepInputs
  //unless that connection is with expcetedTarget
  private boolean connectedToOtherInputOfThisStep(AVariableIdentification expectedTarget, AVariableIdentification provider, List<AVariableIdentification> stepInputs) {
    
    //if exceptedSource is connected to provider => return false
    //=> we are connected to our expected target
    if(expectedTarget.connectedness != null &&
        expectedTarget.connectedness.getConnectedVariableId() != null &&
            expectedTarget.connectedness.getConnectedVariableId().equals(provider.getIdentifiedVariable().getId()))
      return false;
    
    //go through all stepInputs
    //if they are connected to provider => return true
    for(AVariableIdentification stepInput : stepInputs) {
      if(stepInput.connectedness != null &&
          stepInput.connectedness.getConnectedVariableId() != null &&
          stepInput.connectedness.getConnectedVariableId().equals(provider.getIdentifiedVariable().getId()))
        return true;
    }
    
    return false;
  }
  
  //returns either source of target from connected, or null, if there is no or more than one (-> get Source if source is unambiguous)
  private AVariableIdentification getSource(AVariableIdentification target, Map<AVariableIdentification, InputConnection> connections) {
    if(!connections.containsKey(target))
      return null;
    
    InputConnection con = connections.get(target);
    if(con.getConnectionsPerLane().size() != 1)
      return null;
    
    List<AVariableIdentification> candidates = con.getConnectionForLane(0).getInputVars();
    if(candidates.size() != 1)
      return null;
    
    return candidates.get(0);
  }
  
  //returns true, if there is one unambiguous connection between target and some AVariableIdentification in connections and that AVariableIdentification is a list
  private boolean isConnectedToList(AVariableIdentification target, Map<AVariableIdentification, InputConnection> connections) {
    AVariableIdentification source = getSource(target, connections);
    return (source == null) ? false : source.getIdentifiedVariable().isList();
  }

  public static  boolean mayBeLinked(AVariableIdentification target, AVariableIdentification possibleSource) {
    AVariable sourceVar = possibleSource.getIdentifiedVariable();
    AVariable targetVar = target.getIdentifiedVariable();
    DomOrExceptionGenerationBase sourceDOM = possibleSource.getIdentifiedVariable().getDomOrExceptionObject();
    DomOrExceptionGenerationBase targetDOM = target.getIdentifiedVariable().getDomOrExceptionObject();
    boolean sourceIsList = sourceVar.isList()
        || (possibleSource instanceof ReferencedVarIdentification && ((ReferencedVarIdentification) possibleSource).isForeachOutput());
    

    //getDomOrExceptionObject is null for AnyType
    if(targetDOM == null) {
      return (sourceIsList || (!sourceIsList && !targetVar.isList()));
    }
    
    return sourceDOM != null && DomOrExceptionGenerationBase.isSuperClass(targetDOM, sourceDOM)
        && (sourceIsList || (!sourceIsList && !targetVar.isList()));
  }

  
  //if output is ambiguous => empty assign
  private void getOrCalculateLinkStatesWFOutput(WF wf, List<AVariableIdentification> providers, Map<AVariableIdentification, InputConnection> allConnections) {
    List<Step> steps = wf.getWfAsStep().getChildStep().getChildSteps();
    if( steps.size() == 0 ) {
      return; //nichts zu tun
    }

    Step last = steps.get(steps.size() - 1);
    if (last instanceof StepAssign) {
      StepAssign outputAssignment = (StepAssign)last;
      IdentifiedVariables variables = gbo.identifyVariables( new ObjectId(ObjectType.workflow, null));
      
      //only calculate result of child step if it does not end with Throw 
      if (!endsWithThrowOrRetry(wf.getWfAsStep().getChildStep())) {
        GetOrCalculateLinkStatesByAssignParameter parameter = new GetOrCalculateLinkStatesByAssignParameter();
        parameter.setAllConnections(allConnections);
        parameter.setBranchId(null);
        parameter.setOriginalStep(wf.getWfAsStep());
        parameter.setOutputVars(variables.getVariables(VarUsageType.output));
        parameter.setProviders(providers);
        getOrCalculateLinkStatesByAssign(outputAssignment, parameter);
      }
      
      //prevent lists from being mapped to single variables here - we can't create a ForEach here
      for(AVariableIdentification var : variables.getVariables(VarUsageType.output)) {
        if(var.getIdentifiedVariable().isList())
          continue;
        
        InputConnection con = allConnections.get(var);
        if(con == null)
          continue;
        
        List<SimpleConnection> connections = new ArrayList<SimpleConnection>();
        
        if(!(con instanceof SimpleConnection)){
          for(SimpleConnection simpleCon : con.getConnectionsPerLane()) {
            connections.add(simpleCon);
          }
        }
        else {
          connections.add((SimpleConnection)con);
        }
        
        for(SimpleConnection sCon : connections) {
          
          //ignore lanes with StepThrow
          if(sCon.getLinkState() == LinkstateIn.NO_NEED)
            continue;
          
          //do not overwrite CONSTANT connections
          if(sCon.getLinkState() == LinkstateIn.CONSTANT)
            continue;
          
          List<AVariableIdentification> sources = new ArrayList<AVariableIdentification>(sCon.getInputVars());
          for(AVariableIdentification source : sources) {
            if(source.getIdentifiedVariable().isList()) {
              sCon.getInputVars().remove(source);
            }
          }
          
          //if connection used to be user connected, and there is still a connection here (AUTO), we actually want a USER connection, even if there is 
          //there is no ambiguity
          Linkstate oldState = sCon.getLinkState();
          updateLinkStateofConnection(sCon);
          if(oldState == LinkstateIn.USER && sCon.getLinkState() == LinkstateIn.AUTO)
            sCon.setLinkState(LinkstateIn.USER);
        }
        
      }
    }
  }
  
  private void getOrCalculateLinkStatesByAssign(final StepAssign assign, GetOrCalculateLinkStatesByAssignParameter parameters) { 
    Map<AVariableIdentification, InputConnection> allConnections = parameters.getAllConnections();
    String branchId = parameters.getBranchId();
    Step originalStep = parameters.getOriginalStep();
    List<AVariableIdentification> outputVars = parameters.getOutputVars();
    List<AVariableIdentification> providers = parameters.getProviders();
    List<AVariableIdentification> assignInputs = identifyVariableIds(assign.getInputVarIds(), providers, assign.getParentScope(), VarUsageType.input);
    /*
     * nicht connectedness bei den input setzen, weil die assign-inputs nur referenzen auf variablen weiter oben im workflow sind
     * damit würde man fall-abhängig die connectedness überschreiben.
     * => assigninputs clonen.
     * 
     * man benötigt die assigninputs als avariableidentification aus zwei gründen:
     * 1) beim getOrCalculateLinkState wird auf die vorherige connectedness zugegriffen, damit vorherige user-/constant-connectedness wiederhergestellt werden kann
     * 2) beim zurückschreiben der verbindungen (applyDataflowToGB()) wird auf die connectedness zugegriffen
     */
    for (int i = 0; i < assignInputs.size(); i++) {
      if(assignInputs.get(i) == null) { 
        assignInputs.set(i, null);//TODO: ist null, wenn wir versuchen den Workflow Output Konstant zu setzen (?)
        continue;
      }
      final int i_f = i;
      AVariableIdentification cloned = assignInputs.get(i).createClone();
      cloned.connectedness = new Connectedness() {
        
        @Override
        public boolean isUserConnected() {
          return assign.getInputConnections().getUserConnected()[i_f];
        }
        
        
        @Override
        public boolean isConstantConnected() {
          return assign.getInputConnections().getConstantConnected()[i_f];
        }
        
        
        @Override
        public String getConnectedVariableId() {
          return assign.getInputConnections().getVarIds()[i_f];
        }
      };
      assignInputs.set(i, cloned);
    }
    
    /*
     * suche für alle zu belegenden outputVars: gibt es eine assign-zuweisung darauf? dann übernimm möglichst den existierenden input
     */
    List<Pair<AVariableIdentification, AVariableIdentification>> resolutions = new ArrayList<>();
    for (AVariableIdentification varIdentOutput : outputVars) {
      
      //ignore prototypes
      if(varIdentOutput.getIdentifiedVariable().isPrototype()) {
        continue;
      }
      
      boolean match = false;
      for (int i = 0; i < assign.getOutputVarIds().length; i++) {
        if (assign.getOutputVarIds()[i].equals(varIdentOutput.idprovider.getId())) {
          resolutions.add(Pair.of(varIdentOutput, assignInputs.get(i)));
          match = true;
          break;
        }
      }
      if (!match) {
        resolutions.add(Pair.<AVariableIdentification, AVariableIdentification>of(varIdentOutput, null));
      }
    }
    
    //if we try to assign an output of StepChoice (but not a Conditional Branching)
    //then we need to hide all providers that are not of Type AnyType, if we try to
    //connect an AnyType output.
    Step parentStep = assign.getParentStep();
    boolean hideNotAnyTypeProviders = parentStep != null && parentStep.getParentStep() != null &&
        parentStep.getParentStep() instanceof StepChoice &&
        ((StepChoice)parentStep.getParentStep()).getDistinctionType() != DistinctionType.ConditionalBranch;
    
    List<AVariableIdentification> anyTypeProviders = new ArrayList<AVariableIdentification>(providers);
    if(hideNotAnyTypeProviders) {
      anyTypeProviders.removeIf(x -> x.getIdentifiedVariable().getDomOrExceptionObject() != null);
    }
    
    for (int resolutionNr = 0; resolutionNr < resolutions.size(); resolutionNr++) {
      List<AVariableIdentification> providersForVariable = new ArrayList<AVariableIdentification>();
      if(resolutions.get(resolutionNr).getFirst().getIdentifiedVariable().getDomOrExceptionObject() != null)
        providersForVariable.addAll(providers);
      else
        providersForVariable.addAll(anyTypeProviders);
      
      //remove everything that does not match us list-wise - we can't create ForEach here.
      final boolean isList = resolutions.get(resolutionNr).getFirst().getIdentifiedVariable().isList();
      providersForVariable.removeIf(x -> x.getIdentifiedVariable().isList() != isList);
      

      boolean treatExistingConnectionAsUserConnected = false;
      if(assignInputs.size() > resolutionNr && assignInputs.get(resolutionNr) != null) { //there might not be an input here
        treatExistingConnectionAsUserConnected = assignInputs.get(resolutionNr).connectedness.isUserConnected();
      }
      
      getOrCalculateLinkState(resolutions.get(resolutionNr), providersForVariable, allConnections, branchId, treatExistingConnectionAsUserConnected, assignInputs, assign);
      
      //set connection to user, if it used to be of type user
      List<SimpleConnection> con = allConnections.get(resolutions.get(resolutionNr).getFirst()).getConnectionsPerLane();
      if(resolutions.get(resolutionNr).getSecond() != null) {
        for(SimpleConnection sCon : con) {
          if(sCon.getLinkState() == LinkstateIn.AUTO && resolutions.get(resolutionNr).getFirst().connectedness.isUserConnected()) {
            sCon.setLinkState(LinkstateIn.USER);
          }
        }
      }
      
      //Warnings
      for (SimpleConnection sCon : con) {
        NewProvidersForConnectionData data = new NewProvidersForConnectionData();
        data.setNewConnection(sCon);
        data.setNewProviders(providers);
        data.setStep(originalStep);
        data.setVariableIndex(resolutionNr);
        data.setVariableToConnect(resolutions.get(resolutionNr).getFirst());
        data.setUsageType(VarUsageType.output);
        warningsManagement.processNewProvidersForConnection(data);
      }      
      
      
      
      if(logger.isDebugEnabled()) {
        logger.debug("Calced Link for assign (" + assign.getStepId() + "["+resolutionNr+"]): " + resolutions.get(resolutionNr).getFirst().getIdentifiedVariable().getId() + "->");
        for (SimpleConnection sCon : allConnections.get(resolutions.get(resolutionNr).getFirst()).getConnectionsPerLane()) {
          logger.debug("Calced Link for assign (" + assign.getStepId() + "[" + resolutionNr + "]): " + sCon.getLinkState() + " - " + sCon.getInputVar(0));
        }
      }
    }   
  }
  
  private void rewriteDataflow() {
    dataflow.clear();
    Set<AVariableIdentification> toRemove = new HashSet<AVariableIdentification>();
    for (Entry<AVariableIdentification, InputConnection> entry : inputConnections.entrySet()) {
      if( missingVarIds.contains( entry.getKey() ) ) {
        toRemove.add( entry.getKey() );
      }
    }
    for (Entry<AVariableIdentification, InputConnection> entry : inputConnections.entrySet()) {
      List<SimpleConnection> connectionsPerLane = entry.getValue().getConnectionsPerLane();
      List<String> branchIds;
      if (entry.getValue() instanceof MultiLaneConnection) {
        branchIds = ((MultiLaneConnection) entry.getValue()).getBranchIds();
      } else {
        branchIds = null;
      }
      for (int i = 0; i < connectionsPerLane.size(); i++) {
        String branchId = branchIds == null ? null : branchIds.get(i);
        SimpleConnection conCurLane = connectionsPerLane.get(i);
        AVariableIdentification target = entry.getKey();
        Linkstate linkState = conCurLane.getLinkState();
        String constant = conCurLane.getConstant();
        
        if( toRemove.contains(target ) ) {
          addDataflowEntries(removed, target, conCurLane.getInputVars(), linkState, toRemove, branchId, constant);
        } else {
          addDataflowEntries(dataflow, target, conCurLane.getInputVars(), linkState, toRemove, branchId, constant);
        }
      }
    }
  }

  private void addDataflowEntries(List<DataflowEntry> list, AVariableIdentification target,  
      List<AVariableIdentification> sources, Linkstate linkState, Set<AVariableIdentification> toRemove, String branchId, String constant) {
    
    List<SimpleConnection> rewriteConnections;
    SimpleConnection rewriteConnection;
    switch( sources.size() ) {
    case 0:
      list.add( new DataflowEntry( target, null, linkState, branchId, constant) );
      break;
    case 1:
      if( toRemove.contains( sources.get(0) ) ) {
        removed.add( new DataflowEntry( target, sources.get(0), linkState, branchId, constant) );
        InputConnection rewrite = inputConnections.get(sources.get(0));
        List<String> branchIds;
        if (rewrite instanceof MultiLaneConnection) {
          branchIds = ((MultiLaneConnection) rewrite).getBranchIds();
        } else {
          branchIds = null;
        }
        rewriteConnections = rewrite.getConnectionsPerLane();
        for (int connectionNr = 0; connectionNr < rewriteConnections.size(); connectionNr++) {
          branchId = branchIds == null ? null : branchIds.get(connectionNr);
          rewriteConnection = rewriteConnections.get(connectionNr);
          addDataflowEntries(rewritten, target, rewriteConnection.getInputVars(), rewriteConnection.getLinkState(), toRemove, branchId, constant);
        }
      } else {
        list.add( new DataflowEntry( target, sources.get(0), linkState, branchId, constant) );
      }
      break;
    default:
      for (AVariableIdentification source : sources ) {
        if( toRemove.contains( source ) ) {
          removed.add( new DataflowEntry( target, source, linkState, branchId, constant) );
          InputConnection rewrite = inputConnections.get(source);
          List<String> branchIds;
          if (rewrite instanceof MultiLaneConnection) {
            branchIds = ((MultiLaneConnection) rewrite).getBranchIds();
          } else {
            branchIds = null;
          }
          rewriteConnections = rewrite.getConnectionsPerLane();
          for (int connectionNr = 0; connectionNr < rewriteConnections.size(); connectionNr++) {
            branchId = branchIds == null ? null : branchIds.get(connectionNr);
            rewriteConnection = rewriteConnections.get(connectionNr);
            addDataflowEntries(rewritten, target, rewriteConnection.getInputVars(), rewriteConnection.getLinkState(), toRemove, branchId, constant);
          }
        } else {
          list.add( new DataflowEntry( target, source, linkState, branchId, constant) );
        }
      }
    }
  }


  public List<DataflowEntry> asEntryList() {
    if( debug ) {
      List<DataflowEntry> entries = new ArrayList<DataflowEntry>(dataflow);
      
      if(!rewritten.isEmpty()) {
        entries.add(new DataflowEntry("rewritten"));
        entries.addAll(rewritten);
      }
      if(!removed.isEmpty()) {
        entries.add(new DataflowEntry("removed"));
        entries.add(new DataflowEntry("varIds", missingVarIds.toString() ));
        entries.addAll(removed);
      }
      
      if(!problems.isEmpty()) {
        entries.add(new DataflowEntry("problems"));
        entries.addAll(problems);
      }
      
      return entries;
    } else {
      if(!rewritten.isEmpty()) {
        List<DataflowEntry> entries = new ArrayList<DataflowEntry>(dataflow);
        entries.addAll(rewritten);
        return entries;
      } else {
        return dataflow;
      }
    }
  }


  private class DataflowEntry implements HasXoRepresentation {

    private AVariableIdentification target;
    private AVariableIdentification source;
    private Linkstate linkState;
    private String laneId;
    private String debugMessage;
    private String sourceMessage;
    private String constant;

    public DataflowEntry(AVariableIdentification target, AVariableIdentification source, Linkstate linkState, String laneId, String constant) {
      this.target = target;
      this.source = source;
      this.linkState = linkState;
      this.laneId = laneId;
      this.constant = constant;
    }

    public DataflowEntry(String debugMessage) {
      this( "DEBUG", debugMessage);
    }
    
    public DataflowEntry(String source, String debugMessage) {
      this.sourceMessage = source;
      this.debugMessage = debugMessage;
    }
    @Override
    public GeneralXynaObject getXoRepresentation() {
      Connection connection = new Connection();
      if(debugMessage != null) {
        connection.setTargetId(sourceMessage);
        connection.setType(debugMessage);
      } else {
        if (linkState != LinkstateIn.CONSTANT) {
          connection.setSourceId(getVarName(source));
        }
        if (laneId != null) {
          connection.setBranchId(laneId);
        }
        
        if(constant != null)
          connection.setConstant(constant);
        
        connection.setTargetId(getVarName(target));
        connection.setType(linkState.toValue());
      }
      return connection;
    }

    private String getVarName(AVariableIdentification var) {
      if( var == null ) {
        return null;
      }
      if( debug ) {
        return var.internalGuiId.createId() + " - " + var.toString();
      } else {
        return var.internalGuiId.createId();
      }
    }
    
  }


  public Linkstate getLinkState(AVariableIdentification var, boolean input) {
    if (input) {
      for (Entry<AVariableIdentification, InputConnection> entry : inputConnections.entrySet()) {
        if (entry.getKey().equals(var)) {
          if (entry.getValue() instanceof SimpleConnection) {
            return ((SimpleConnection) entry.getValue()).getLinkState();
          } else if (entry.getValue() instanceof MultiLaneConnection) {
            SimpleConnection notSelected = ((MultiLaneConnection) entry.getValue()).getSimpleConnectionByBranchId(null);
            if (notSelected == null) {
              return LinkstateIn.NONE;
            } else {
              return notSelected.getLinkState();
            }
          }
        }
      }
      return LinkstateIn.NONE;
    } else {
      for (Entry<AVariableIdentification, InputConnection> entry : inputConnections.entrySet()) {
        if (!(entry.getValue() instanceof SimpleConnection)) {
          continue; // TODO: What about when it's a MultiLaneConnection?
        }
        
        SimpleConnection connection = (SimpleConnection)entry.getValue();
        for (AVariableIdentification aVar : connection.getInputVars()) {
          if (aVar.equals(var)) {
            if (connection.getInputVars().size() == 1) {
              return LinkstateOut.CONNECTED;
            } else {
              if( entry.getKey() instanceof GlobalChoiceVarIdentification ) {
                return LinkstateOut.CONNECTED;
              } else {
                // we do not count as connected if we are part of an ambiguity
                return LinkstateOut.NONE; //TODO AMBIGUE?
              }
            }
          }
        }
      }
      
      return LinkstateOut.NONE;
    }
  }

  /*
   * synchronisiert input-var-ids aller wf-steps mit dem jetzigen dataflow-stand.
   * 
   * alle wf-steps durchlaufen und dann jeweils checken, ob die inputs des schritts in der dataflow-liste enthalten sind, und falls nicht, anpassung vornehmen.
   */
  public void applyDataflowToGB() {
    removeInvalidConnections();
    applyToStep(gbo.getWorkflow().getWfAsStep());
  }


  public void addUserConnection(GBSubObject source, GBSubObject target, String branchId) {
    List<AVariableIdentification> inputVars = new ArrayList<AVariableIdentification>();
    
    if(source.getType() == ObjectType.distinctionCase) {
      AVariableIdentification var = null; 
      if(!(source.getStep() instanceof StepChoice))
        throw new RuntimeException("unexpected distinctionCase in " + source.getStep().getClassName());
      IdentifiedVariablesStepChoice ovsc = (IdentifiedVariablesStepChoice)identifyVariables(source.getStep());
      int lane = source.getCaseInfo().getIndex();
      var = ovsc.getAVariableIdentification(lane);
      inputVars.add(var);
    }
    else if(source.getType() == ObjectType.distinctionBranch) { //StepCatch -- TODO:
      Step catchStep = source.getStep().getParentStep();
      int lane = findLaneOfCatch((StepCatch)catchStep, target.getStep());
      AVariableIdentification var = null;
      
      String id = (new ArrayList<String>(((StepCatch)catchStep).getAllUsedVariableIds())).get(lane); //TODO: order
      VariableIdentification exception = null;
      try {
        exception = catchStep.getParentScope().identifyVariable(id);
      } catch (XPRC_InvalidVariableIdException e) {
        throw new RuntimeException("could not find Exception Variable");
      }
      var = DirectVarIdentification.of(exception.getVariable());
      var.idprovider = new UseAVariable(var); //TODO:set only once + reuse variable
      inputVars.add(var);
    }
    else
      inputVars.add(source.getVariable().getVariable());

    SimpleConnection newConnection = new SimpleConnection(inputVars, LinkstateIn.USER, branchId, null);
    InputConnection con = inputConnections.get(target.getVariable().getVariable());
    if(con instanceof SimpleConnection) {
      inputConnections.put(target.getVariable().getVariable(), newConnection);
    }
    else {
      if(!(con instanceof MultiLaneConnection))
        throw new RuntimeException("Unsupported Conncetion type: " + con.getClass());
      
      MultiLaneConnection mCon = (MultiLaneConnection)con;
      SimpleConnection sCon = mCon.getSimpleConnectionByBranchId(branchId);
      sCon.setInputVars(inputVars);
      sCon.setLinkState(LinkstateIn.USER);
    }
    
    applyDataflowToGB();
  }
  
  private int findLaneOfCatch(StepCatch stepCatch, Step targetStep) {
    
    List<Step> catchLanes = stepCatch.getBranchesForGUI().stream().map(x -> x.getMainStep()).collect(Collectors.toList()); //TODO: order?
    
    for(int i=0; i<catchLanes.size(); i++) {
      Step stepToExamine = targetStep;
      
      while(stepToExamine != null) {
        if(stepToExamine == catchLanes.get(i))
          return i;
        stepToExamine = stepToExamine.getParentStep();
      }
      
    }
    
    
    return -1;
  }


  public String setConstantValue(AVariableIdentification target, String branchId, GeneralXynaObject value) {
    SimpleConnection connectionToSetConstant = determineConnectionForConstant(target, branchId);

    // add global variable for new value - remove old if present
    AVariable constDataVar = AVariable.createFromXo(value, gbo.getWFStep().getCreator(), target.getIdentifiedVariable().isList());
    constDataVar.setId(gbo.getWFStep().getCreator().getNextXmlId().toString());
    String constant = "";
    try {
      constant = Utils.xoToJson(value, constDataVar.getCreator().getRevision());
    } catch(Exception e) {
      //empty constant
    }
    executeSetConstant(connectionToSetConstant, constDataVar, constant);

    return constDataVar.getId();
  }


  private SimpleConnection determineConnectionForConstant(AVariableIdentification target, String branchId) {
    InputConnection existingConnection = inputConnections.get(target);
    int laneNr = -1; // -1 means null
    if (branchId != null && branchId.length() > 0) {
      try {
        laneNr = ObjectId.parseBranchNumber(ObjectId.parse(branchId));
      } catch (Exception e) {
        Utils.logError("Could not parse branchId '" + branchId + "' while setting constant value for variable " + target.getIdentifiedVariable().getId(), e);
      }
    }
    SimpleConnection connectionToSetConstant = existingConnection.getConnectionForLane(laneNr);
    return connectionToSetConstant;
  }
  

  public String setConstantValue(AVariableIdentification target, String branchId, AVariable constantValue, String constantJson) {
    SimpleConnection connectionToSetConstant = determineConnectionForConstant(target, branchId);
    constantValue.setId(gbo.getWFStep().getCreator().getNextXmlId().toString());
    executeSetConstant(connectionToSetConstant, constantValue, constantJson);
    return constantValue.getId();
  }

  
  private void executeSetConstant(SimpleConnection connectionToSetConstant, AVariable constDataVar, String constantJson) {
    if(connectionToSetConstant.getInputVars().size() == 1 && connectionToSetConstant.getLinkState() == LinkstateIn.CONSTANT) {
      //remove previous constant - if it is not in globalStepSerial, it gets removed by factory code
      StepSerial globalStepSerial = gbo.getWFStep().getChildStep();
      AVariable oldConst = connectionToSetConstant.getInputVars().get(0).getIdentifiedVariable();
      if(globalStepSerial.getVariablesAndExceptions().contains(oldConst)){
        globalStepSerial.removeVar(oldConst);
      }
    
    }
    
    if (constDataVar.getDomOrExceptionObject() != null) {
      DomOrExceptionGenerationBase doe = constDataVar.getDomOrExceptionObject();
      if (doe.getLabel() == null) {
        try {
          doe.parse(false);
        } catch (Exception e) {

        }
      }
      constDataVar.setLabel(constDataVar.getDomOrExceptionObject().getLabel());
    }
    
    constDataVar.setVarName("const_" + constDataVar.getOriginalName() + constDataVar.getId());
    gbo.getWFStep().getChildStep().addVar(constDataVar);
    
    // refer to variable as constant
    connectionToSetConstant.setLinkState(LinkstateIn.CONSTANT);
    connectionToSetConstant.setInputVars(createConstConnInputVars(constDataVar));

    connectionToSetConstant.setConstant(constantJson);

    applyDataflowToGB();
  }
  
  public InputConnection createCopyOfConnection(AVariableIdentification from, AVariableIdentification to, Map<AVariableIdentification, AVariableIdentification> cloneMap,
                              StepSerial targetGlobalStepSerial) {
    InputConnection con = inputConnections.get(from);
    if (con == null) {
      return null;
    }

    InputConnection copy;
    SimpleConnection copyCon;

    if (con instanceof SimpleConnection) {
      copyCon = copySimpleConnection((SimpleConnection) con, cloneMap, targetGlobalStepSerial);
      copy = copyCon;
    } else {
      MultiLaneConnection orgMCon = (MultiLaneConnection)con;
      MultiLaneConnection mCon = new MultiLaneConnection();
      List<SimpleConnection> sCons = con.getConnectionsPerLane();
      for (int i = 0; i < sCons.size(); i++) {
        SimpleConnection sCon = sCons.get(i);
        copyCon = copySimpleConnection(sCon, cloneMap, targetGlobalStepSerial);
        mCon.addLaneConnection(copyCon, copyCon.getBranchId());
        mCon.idxBranchIdIsNull = orgMCon.idxBranchIdIsNull;
      }
      copy = mCon;
    }
    
    return copy;
  }

  

  public void copyConnection(AVariableIdentification from, AVariableIdentification to,
                             Map<AVariableIdentification, AVariableIdentification> cloneMap,
                              GBBaseObject destination) {
    StepSerial globalStepSerial = destination.getStep().getParentWFObject().getWfAsStep().getChildStep();
    InputConnection copy = createCopyOfConnection(from, to, cloneMap, globalStepSerial);
    if (copy != null) {
      inputConnections.put(to, copy);
    }
    
    //apply - since copyConnection is not called from regular getDataflow()
    if (destination != null && destination.getStep() != null) {
      Step destinationStep = destination.getStep();
      identifyVariables(destinationStep).identify(); //refresh identified variables.
      if (destinationStep != null) {
        applyToStep(destinationStep);
      }
    }
  }
  

  private SimpleConnection copySimpleConnection(SimpleConnection sCon, Map<AVariableIdentification, AVariableIdentification> cloneMap, StepSerial globalStepSerial) {
    SimpleConnection copyCon = new SimpleConnection(sCon.getLinkState());
    copyCon.branchId = sCon.branchId;
    copyCon.constant = sCon.constant;
    copyCon.inputVars = new ArrayList<AVariableIdentification>(sCon.inputVars);
    
    if(sCon.getLinkState() != null && sCon.getLinkState() == LinkstateIn.CONSTANT) {
      //copy constant
      AVariable varToCopy = sCon.getInputVar(0).getIdentifiedVariable();
      AVariable copy = null;
      try {
        GeneralXynaObject val = varToCopy.getXoRepresentation();
        copy = AVariable.createFromXo(val, varToCopy.getCreator(), varToCopy.isList());
      } catch (InvalidObjectPathException | XDEV_PARAMETER_NAME_NOT_FOUND e) {
        Utils.logError("Could not copy constant: " + e, e);
        copy = AVariable.createAVariable(globalStepSerial.getParentWFObject().getNextXmlId().toString(), varToCopy.getDomOrExceptionObject(), varToCopy.isList());
      }
      copy.setId("" + globalStepSerial.getCreator().getNextXmlId());
      AVariableIdentification aVarId = DirectVarIdentification.of(copy);
      aVarId.idprovider = copy::getId;
      globalStepSerial.addVar(copy);
      cloneMap.put(copyCon.getInputVar(0), aVarId);
    }
    
    AVariableIdentification orgVar;
    for (int i = 0; i < copyCon.inputVars.size(); i++) {
      orgVar = copyCon.inputVars.get(i);
      //point at clone
      if (cloneMap.containsKey(orgVar)) {
        copyCon.getInputVars().set(i, cloneMap.get(orgVar));
      }
    }
    
    return copyCon;
  }
  


  private List<AVariableIdentification> createConstConnInputVars(AVariable globaleConstVar) {
    List<AVariableIdentification> inputVars = new ArrayList<AVariableIdentification>();
    DirectVarIdentification constVarIdent = DirectVarIdentification.of(globaleConstVar);
    constVarIdent.idprovider = new UseAVariable(constVarIdent); 
    
    if(globaleConstVar == null) {
      throw new RuntimeException("Global variable not found.");
    }
    
    final String id = globaleConstVar.getId();
    constVarIdent.connectedness = new Connectedness() {  
      @Override
      public boolean isUserConnected() {
        return false;
      }
      @Override
      public boolean isConstantConnected() {
        return true;
      }      
      @Override
      public String getConnectedVariableId() {
        return id;
      }
    };
    
    inputVars.add(constVarIdent);

    return inputVars;
  }
  

  public boolean removeUserConnection(GBSubObject target, String branchId) {
    Predicate<SimpleConnection> p = (connectionCurLane) -> connectionCurLane.getLinkState() == LinkstateIn.USER;
    return resetConnection(target, branchId, p);
  }


  public boolean removeConstantValue(GBSubObject target, String branchId) {
    Predicate<SimpleConnection> p = (connectionCurLane) -> connectionCurLane.getLinkState() == LinkstateIn.CONSTANT;
    return resetConnection(target, branchId, p);
  }
  

  public void removeConstantValues(GBSubObject target, Optional<Predicate<SimpleConnection>> additionalPredicate) {
    InputConnection existingConnection = inputConnections.get(target.getVariable().getVariable());
    if (existingConnection == null) {
      // no connection there to be removed
      return;
    }
    
    Predicate<SimpleConnection> p = (connectionCurLane) -> connectionCurLane.getLinkState() == LinkstateIn.CONSTANT;
    if(additionalPredicate.isPresent()) {
      p = p.and(additionalPredicate.get());
    }

    resetConnectionForExisting(target, existingConnection, p);
  }
  
  
  private void resetConnectionForExisting(GBSubObject target, InputConnection con, Predicate<SimpleConnection> p) {
    if (con instanceof SimpleConnection) {
      resetConnection(target, null, p);
    } else if (con instanceof MultiLaneConnection) {
      MultiLaneConnection mlc = (MultiLaneConnection) con;
      List<String> branchIds = mlc.getBranchIds();
      for (String branchId : branchIds) {
        resetConnection(target, branchId, p);
      }
    }
  }
  

  /**
   * Resets connections for the variable, but keeps the entry.
   * Call this if a variable was removed
   */
  public void resetConnection(GBSubObject target) {
    InputConnection existingConnection = inputConnections.get(target.getVariable().getVariable());
    if (existingConnection == null) {
      // no connection there to be reset
      return;
    }

    resetConnectionForExisting(target, existingConnection, (s) -> true);
  }


  private boolean resetConnection(GBSubObject target, String branchId, Predicate<SimpleConnection> condition) {
    InputConnection existingConnection = inputConnections.get(target.getVariable().getVariable());
    if (existingConnection == null) {
      // no connection there to be removed
      return false;
    }

    // remove all links for the given connection
    SimpleConnection connectionCurLane;
    if(existingConnection instanceof SimpleConnection) {
      connectionCurLane = existingConnection.getConnectionForLane(0);
    }
    else {
      if(!(existingConnection instanceof MultiLaneConnection))
        throw new RuntimeException("unsupported Connection Type: " + existingConnection.getClass());
      
      MultiLaneConnection mCon = (MultiLaneConnection)existingConnection;
      connectionCurLane =  mCon.getSimpleConnectionByBranchId(branchId);
    }
    if (connectionCurLane != null && condition.test(connectionCurLane)) {
      connectionCurLane.setLinkState(LinkstateIn.NONE);
      connectionCurLane.setInputVars(new ArrayList<AVariableIdentification>());
    }
    
    applyDataflowToGB();
    resetDataflow(); // recalculate dataflow, because removed connection might cause new ambiguities

    return true;
  }
  
  
  public void prepareStepFunctionVarsForUpdate(IdentifiedVariables ident) {

    //remove and add variables again to update references; TODO: maybe remove all first, add all afterwards?
    List<AVariableIdentification> varsToUpdate = ident.getListAdapter(VarUsageType.input);
    List<InputConnectionEntry> entries = new ArrayList<>();
    for(AVariableIdentification avar: varsToUpdate) {
      InputConnection con = inputConnections.remove(avar);
      if(con != null) {
        InputConnectionEntry entry = new InputConnectionEntry();
        entry.ident = avar;
        entry.con = con;
        entries.add(entry);
      }
    }
    
    for(InputConnectionEntry entry: entries) {
      inputConnections.put(entry.ident, entry.con);
    }
  }
  
  
  private static class InputConnectionEntry{
    private AVariableIdentification ident;
    private InputConnection con;
  }


  /**
   * Deletes the branch from the multi-lane-connection (necessary when deleting a branch).
   * 
   * Not to be confused with resetConnection(...), which only sets the linkstate to none, but keeps the entry for the branch in the multi-lane-connection. 
   * Call this if a branch was removed.
   * */
  public boolean deleteConnectionForBranch(GBSubObject target, String branchId) {
    InputConnection existingConnection = inputConnections.get(target.getVariable().getVariable());
    if (existingConnection == null || !(existingConnection instanceof MultiLaneConnection)) {
      // no connection there to be removed
      return false;
    }

    MultiLaneConnection mCon = (MultiLaneConnection)existingConnection;
    return mCon.removeBranch(branchId);
  }

  /**
   * adds a new connection for the given variable
   */
  public void addConnectionForBranch(GBSubObject target, Step step) {
    InputConnection existingConnection = inputConnections.get(target.getVariable().getVariable());
    if(existingConnection == null) {
      return;// no connection to add to
    }
    
    if (existingConnection instanceof SimpleConnection) {
      //upgrade to MultiLane connection - we are adding the second connection
      MultiLaneConnection mlc = new MultiLaneConnection();
      inputConnections.put(target.getVariable().getVariable(), mlc);
      mlc.addLaneConnection((SimpleConnection)existingConnection, null);
      existingConnection = mlc;
    }
    MultiLaneConnection mCon = (MultiLaneConnection)existingConnection;

    int index = mCon.getConnectionsPerLane().size() - 1;
    String branchId = ObjectId.createBranchId(ObjectId.createStepId(step).getBaseId(), String.valueOf(index));
    
    if (step instanceof StepChoice || step instanceof WFStep) {
      //remove default branch (added later)
      SimpleConnection defaultConnection = mCon.getConnectionsPerLane().get(mCon.getConnectionsPerLane().size() - 1);
      String defaultBranchId = mCon.getBranchIds().get(mCon.getBranchIds().size() - 1);

      mCon.removeBranch(defaultBranchId);

      SimpleConnection connection = new SimpleConnection(LinkstateIn.NONE);
      connection.setBranchId(branchId);
      mCon.addLaneConnection(connection, branchId);

      //add default lane again
      mCon.addLaneConnection(defaultConnection, defaultBranchId);
    } else {
      //StepFunction
      SimpleConnection connection = new SimpleConnection(LinkstateIn.NONE);
      mCon.addLaneConnection(connection, branchId);
    }
  }
  
  

  private void removeInvalidConnections() {
    List<Entry<AVariableIdentification, InputConnection>> oldInputConnections = new ArrayList<>(inputConnections.entrySet());
    inputConnections.clear();
    for (Entry<AVariableIdentification, InputConnection> entry : oldInputConnections) {
      InputConnection inputConnection = entry.getValue();
      
      // check if connection is still valid
      boolean isConnectionValid = inputConnectionStillValid(inputConnection);
     

      if (isConnectionValid) {
        inputConnections.put(entry.getKey(), entry.getValue());
      }
    }
  }

  
  private boolean inputConnectionStillValid(InputConnection inputConnection) {
    if (inputConnection == null) {
      return false;
    }

    if(inputConnection instanceof SimpleConnection) {
      for (SimpleConnection connectionCurLane : inputConnection.getConnectionsPerLane()) {
        for (AVariableIdentification inputVar : connectionCurLane.getInputVars()) {
          if (!isValid(inputVar)) {
            return false;
          }
        }
      }
      return true;
    }
    else {
      List<SimpleConnection> connectionsPerLane = inputConnection.getConnectionsPerLane();
      for (SimpleConnection connectionCurLane : connectionsPerLane) {
        List<AVariableIdentification> inputVars = new ArrayList<AVariableIdentification>(connectionCurLane.getInputVars());
        for (AVariableIdentification inputVar : inputVars) {
          if (!isValid(inputVar)) {
            connectionCurLane.inputVars.clear();
            connectionCurLane.setLinkState(null); // -> set connection to invalid
          }
        }
      }
      return true;
    }
  }

  private boolean isValid(AVariableIdentification var) {
    try {
      var.idprovider.getId();
      return true;
    } catch (Exception e) {
      return false; // if id can't be determined, variable is not valid, anymore
    }
  }


  private void applyToStep(Step step) {
    step.visit(new RecursiveVisitor() {


      @Override
      public void visitStepMapping(StepMapping step) {
        super.visitStepMapping(step);
        apply(step.getInputConnections(), step);
        step.refreshContainers();
      }


      @Override
      public void visitStepFunction(StepFunction step) {
        super.visitStepFunction(step);
        apply(step.getInputConnections(), step);
        applyRemoteDestinationParameter(step);
      }


      @Override
      public void visitStepCatch(StepCatch step) {
        super.visitStepCatch(step);
      }
      
      @Override 
      public void visitStepRetry(StepRetry step) {
        super.visitStepRetry(step);
        apply(step.getInputConnections(), step);
      }


      private void apply(InputConnections inputCon, Step step) {
        applyInputConnections(inputCon, identifyVariables(step), VarUsageType.input);
      }

      private void applyInputConnections(InputConnections inputCon, IdentifiedVariables iv, VarUsageType type) {
        for (int i = 0; i < inputCon.length(); i++) {
          SimpleConnection inputConnection = null;
          
          //otherwise multiple queries do not work (?) Some Steps do not change input variable hashCodes - inputs remain static, no reordering.
          if(iv instanceof IdentifiedVariablesStepFunction || iv instanceof IdentifiedVariablesStepThrow) {
            inputConnection = (SimpleConnection)inputConnections.get(iv.getVariable(type, i));
          }
          else {
            //can't access f_connections directly, because hashCode() provides wrong result for AVariableIdentifications (-> new guiID, but we added using old)
            for(Entry<AVariableIdentification, InputConnection> e : inputConnections.entrySet()) {
              AVariable avar = e.getKey() != null ? e.getKey().getIdentifiedVariable() : null;
              AVariable other = iv.getVariable(type, i).getIdentifiedVariable();
              if(avar != null && other != null && avar == other) {
                inputConnection = (SimpleConnection)e.getValue(); //inputs are always simpleConnections.
                break;
              }
            }           
          }

        
          if (inputConnection == null || inputConnection.getLinkState() == LinkstateIn.NONE || inputConnection.getLinkState() == LinkstateIn.AMBIGUE) {
            inputCon.getVarIds()[i] = null;
            inputCon.getUserConnected()[i] = false;
            inputCon.getConstantConnected()[i] = false;
          } else if (inputConnection.getLinkState() == LinkstateIn.AUTO) {
            inputCon.getVarIds()[i] = inputConnection.getInputVar(0).idprovider.getId();
            inputCon.getUserConnected()[i] = false;
            inputCon.getConstantConnected()[i] = false;
          } else if (inputConnection.getLinkState() == LinkstateIn.CONSTANT) {
            inputCon.getVarIds()[i] = inputConnection.getInputVar(0).idprovider.getId();
            inputCon.getUserConnected()[i] = false;
            inputCon.getConstantConnected()[i] = true;
          } else if (inputConnection.getLinkState() == LinkstateIn.USER) {
            inputCon.getVarIds()[i] = inputConnection.getInputVar(0).idprovider.getId();
            inputCon.getUserConnected()[i] = true;
            inputCon.getConstantConnected()[i] = false;
          }
        }
      }


      @Override
      public void visitStepForeach(StepForeach step) {
        super.visitStepForeach(step);
        // apply(step.getInputConnections(), step);
      }


      @Override
      public void visitStepChoice(StepChoice step) {
        super.visitStepChoice(step);
        apply(step.getInputConnections(), step);
      }


      @Override
      public void visitStepThrow(StepThrow step) {
        super.visitStepThrow(step);
        apply(step.getInputConnections(), step);
      }
      


      @Override
      public void visitStepAssign(StepAssign step) {
        super.visitStepAssign(step);
        Step wrapperParent = step.getParentStep().getParentStep(); // -> StepSerial -> WF/StepChoice/StepCatch
        if ( (wrapperParent instanceof WFStep) || (wrapperParent instanceof StepChoice) ) {
          adaptStepAssign(step, wrapperParent,wrapperParent );
        } else if (wrapperParent instanceof StepCatch) {
          Step stepInTry = ((StepCatch)wrapperParent).getStepInTryBlock();
          if (stepInTry instanceof StepSerial) {
            stepInTry = step.getParentWFObject().getWfAsStep();
          }
          adaptStepAssign(step, stepInTry, wrapperParent);
        } else {
          throw new RuntimeException("did not find supported parent of step assign " + step.getStepId());
        }
      }

      private void adaptStepAssign(StepAssign assign, Step stepProvidingOutputVars, Step stepProvidingLanes) {
        
        int laneNr = getLaneNr(assign, stepProvidingLanes);
        
        //first step assign in TypeChoice needs special treatment: set input to input of choice
        //but only if there are two stepAssigns!
        if (stepProvidingOutputVars instanceof StepChoice
            && ((StepChoice) stepProvidingOutputVars).getDistinctionType() == DistinctionType.TypeChoice) {
          
          StepSerial providingStepSerial = ((StepSerial) stepProvidingLanes.getChildSteps().get(laneNr));
          StepAssign firstStepAssign= providingStepSerial.findFirstAssign();
          StepAssign finalStepAssign = ((StepAssign)providingStepSerial.getChildSteps().get(providingStepSerial.getChildSteps().size()-1));
          String[] assignInputVarIds = assign.getInputVarIds();
          String[] providerInputVarIds = stepProvidingOutputVars.getInputVarIds();
          
          if (firstStepAssign  == assign && finalStepAssign != assign) {
            if (assignInputVarIds != null && assignInputVarIds.length == 1 && providerInputVarIds != null && providerInputVarIds.length == 1) {
              assignInputVarIds[0] = providerInputVarIds[0];
            }
            return;
          }
        }
        
        //bei stepassign kann man sich nicht auf die anzahl der inputconnections verlassen, sondern die ergeben sich durch autosnapping
        IdentifiedVariables vars = identifyVariables(stepProvidingOutputVars);
        
        List<Pair<String, boolean[]>> assignList = new ArrayList<>(); //erstes boolean für userconnected, zweites für constantconnected
        List<String> outputVarIds = new ArrayList<>();
        for (int i = 0; i < vars.getVariables(VarUsageType.output).size(); i++) {
          AVariableIdentification var = vars.getVariable(VarUsageType.output, i);
          
          InputConnection connectionCurOutput = null;
          
          //directly use inputConnections for StepFunction Output
          //otherwise check identified variable => should be unique in inputConnections
          if (stepProvidingOutputVars instanceof StepFunction || stepProvidingOutputVars instanceof StepChoice) {
            //can query HashMap, because hash does not change
            //and there might be multiple entries in inputConnections that have
            //inputConnections.getIdentifiedVariable == var.getIdentifiedVariable
            //-> if same StepFunction is used multiple times
            connectionCurOutput = inputConnections.get(var);
          } else {
            //can't query HashMap directly, because hash might have changed
            for (AVariableIdentification existingLink : inputConnections.keySet()) {
              if (var.getIdentifiedVariable() != null && var.getIdentifiedVariable().equals(existingLink.getIdentifiedVariable())) {
                connectionCurOutput = inputConnections.get(existingLink);
                break;
              }
            }
          }
          
          
          if (connectionCurOutput == null) {
            continue;
          }

          SimpleConnection connectionCurLane = connectionCurOutput.getConnectionForLane(laneNr);

          if ( (connectionCurLane == null) || (connectionCurLane.getLinkState() == LinkstateIn.NONE) || (connectionCurLane.getLinkState() == LinkstateIn.AMBIGUE) ) {
            //im assign nichts speichern
          } else if (connectionCurLane.getLinkState() == LinkstateIn.AUTO) {
            assignList.add(Pair.of(connectionCurLane.getInputVar(0).idprovider.getId(), new boolean[]{false, false}));
            outputVarIds.add(var.idprovider.getId());
          } else if (connectionCurLane.getLinkState() == LinkstateIn.CONSTANT) {
            assignList.add(Pair.of(connectionCurLane.getInputVar(0).getIdentifiedVariable().getId(), new boolean[]{false, true}));
            outputVarIds.add(var.idprovider.getId());
          } else if (connectionCurLane.getLinkState() == LinkstateIn.USER) {
            assignList.add(Pair.of(connectionCurLane.getInputVar(0).idprovider.getId(), new boolean[]{true, false}));
            outputVarIds.add(var.idprovider.getId());
          }
        }

        InputConnections inputCon = new InputConnections(assignList.size());
        for (int i = 0; i < assignList.size(); i++) {
          inputCon.getVarIds()[i] = assignList.get(i).getFirst();
          inputCon.getUserConnected()[i] = assignList.get(i).getSecond()[0];
          inputCon.getConstantConnected()[i] = assignList.get(i).getSecond()[1];
        }
        assign.replaceVars(inputCon, outputVarIds.toArray(new String[0]));
      }


      @Override
      public void visit(Step step) {

      }


      @Override
      public boolean beforeRecursion(Step parent, Collection<Step> children) {
        return true;
      }


      public int getLaneNr(StepAssign assign, Step step) {
        if (step instanceof StepChoice) {
          List<Step> lanes = step.getChildSteps();
          for (int laneNr = 0; laneNr < lanes.size(); laneNr++) {
            List<Step> laneSteps = lanes.get(laneNr).getChildSteps();
            
            //Type Choice lanes have two Assign Steps
            if(((StepChoice)step).getDistinctionType() == DistinctionType.TypeChoice) {
              for(int i =0; i<laneSteps.size(); i++) {
                if(laneSteps.get(i) == assign)
                  return laneNr;
              }
            }
            else {
              if ((laneSteps.size() > 0) && (laneSteps.get(laneSteps.size() - 1) == assign)) {
                return laneNr;
              }            
            }
          }
        } else if (step instanceof StepCatch) {
          StepCatch stepCatch = (StepCatch) step;
          for (int i = 0; i < stepCatch.getBranchesForGUI().size(); i++) {
            Step catchStep = stepCatch.getBranchesForGUI().get(i).getMainStep();
            if (!(catchStep instanceof StepSerial)) {
              throw new RuntimeException("Expecting StepSerial as Catch Step");
            }
            if (catchStep.getChildSteps().get(catchStep.getChildSteps().size() - 1) == assign) {
              return i;
            }
          }
        }
        return -1;
      }
    });
  }
  
  
  private void applyRemoteDestinationParameter(StepFunction step) {
    if (step.getRemoteDispatchingParameter() == null) {
      return;
    }
    HashMap<Integer, AVariableIdentification> map = remoteDispatchingParameterMap.get(step);
    if(map == null) {
      return;
    }
    for (Entry<Integer, AVariableIdentification> entry : map.entrySet()) {
      Integer idx = entry.getKey();
      if (idx >= step.getRemoteDispatchingParameter().getInvokeVarIds().length) {
        continue;
      }
      SimpleConnection connection = (SimpleConnection) inputConnections.get(entry.getValue());
      if (connection != null) {
        List<AVariableIdentification> vars = connection.getConnectionForLane(0).getInputVars();
        if (vars != null && vars.size() == 1) {
          step.getRemoteDispatchingParameter().getInvokeVarIds()[idx] = vars.get(0).idprovider.getId();
          step.getRemoteDispatchingParameter().getIsUserConnected()[idx] = connection.getLinkState() == LinkstateIn.USER;
          step.getRemoteDispatchingParameter().getIsConstantConnected()[idx] = connection.getLinkState() == LinkstateIn.CONSTANT;
          continue;
        }
      }
      step.getRemoteDispatchingParameter().getInvokeVarIds()[idx] = null;
      step.getRemoteDispatchingParameter().getIsUserConnected()[idx] = false;
      step.getRemoteDispatchingParameter().getIsConstantConnected()[idx] = false;
    }
  }
  
  public AVariableIdentification getRemoteDestinationParameterVariableIdentification(ObjectId objectId) {
    String[] splitResult = ObjectId.split(objectId.getBaseId());
    if(splitResult.length != 2) {
      throw new RuntimeException("could not determine step");
    }
    StepFunction step = (StepFunction) gbo.getStep(splitResult[0]);
    Integer index = Integer.parseInt(splitResult[1]);
    return remoteDispatchingParameterMap.get(step).get(index);
  }

  public boolean isDebug() {
    return debug;
  }

  
  public void setDebug(boolean debug) {
    this.debug = debug;
  }
  

  public void replaceVariables(IdentifiedVariables identifiedVariables, Step step, VarUsageType varUsageType) {

    List<AVariableIdentification> newVars = identifyVariables(step).getVariables(varUsageType);
    List<AVariableIdentification> oldVars = identifiedVariables.getVariables(varUsageType);
    for(int i=0; i< oldVars.size(); i++) {
      AVariableIdentification oldVar = oldVars.get(i);
      AVariableIdentification newVar = newVars.get(i);
      
      replaceSource(oldVar, newVar);
      replaceDestination(oldVar, newVar);
    }
  }
  

  private void replaceDestination(AVariableIdentification oldVar, AVariableIdentification newVar) {

    Collection<InputConnection> connections = inputConnections.values();

    for (InputConnection ic : connections) {
      if (ic instanceof SimpleConnection) {
        replaceDestinationInSimpleConnection((SimpleConnection) ic, oldVar, newVar);
      } else {
        List<SimpleConnection> sconnections = ic.getConnectionsPerLane();
        for (SimpleConnection connection : sconnections) {
          replaceDestinationInSimpleConnection(connection, oldVar, newVar);
        }
      }
    }
  }

  private void replaceDestinationInSimpleConnection(SimpleConnection connection, AVariableIdentification oldVar, AVariableIdentification newVar) {
    List<AVariableIdentification> candidates = connection.getInputVars();
    if(candidates.remove(oldVar)){
      candidates.add(newVar);
    }
  }

  private void replaceSource(AVariableIdentification oldVar, AVariableIdentification newVar) {
    if (inputConnections.containsKey(oldVar)) {
      InputConnection ic = inputConnections.get(oldVar);
      inputConnections.remove(oldVar);
      inputConnections.put(newVar, ic);
    }
  }
  
  public Set<Entry<AVariableIdentification, InputConnection>> getConnections(){
    return inputConnections.entrySet();
  }
  
  public Map<AVariableIdentification, InputConnection> getInputConnections() {
    return inputConnections;
  }

  
  public void setWorkflowWarningsHandler(WorkflowWarningsHandler workflowWarningsHandler) {
    warningsManagement.setWorkflowWarningsHandler(workflowWarningsHandler);
  }
  
  
  
  private static class GetOrCalculateLinkStatesByAssignParameter {

    private List<AVariableIdentification> providers;
    private Map<AVariableIdentification, InputConnection> allConnections;
    private List<AVariableIdentification> outputVars;
    private String branchId;
    private Step originalStep;


    public GetOrCalculateLinkStatesByAssignParameter() {}
    
    public List<AVariableIdentification> getProviders() {
      return providers;
    }
    
    public void setProviders(List<AVariableIdentification> providers) {
      this.providers = providers;
    }
    
    public Map<AVariableIdentification, InputConnection> getAllConnections() {
      return allConnections;
    }
    
    public void setAllConnections(Map<AVariableIdentification, InputConnection> allConnections) {
      this.allConnections = allConnections;
    }
    
    public List<AVariableIdentification> getOutputVars() {
      return outputVars;
    }
    
    public void setOutputVars(List<AVariableIdentification> outputVars) {
      this.outputVars = outputVars;
    }
    
    public String getBranchId() {
      return branchId;
    }
    
    public void setBranchId(String branchId) {
      this.branchId = branchId;
    }
    
    public Step getOriginalStep() {
      return originalStep;
    }
    
    public void setOriginalStep(Step originalStep) {
      this.originalStep = originalStep;
    }
  }




}
