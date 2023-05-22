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

package com.gip.xyna.xact.filter.session.workflowwarnings;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.Dataflow.Linkstate;
import com.gip.xyna.xact.filter.session.Dataflow.LinkstateIn;
import com.gip.xyna.xact.filter.session.Dataflow.SimpleConnection;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;

import xmcp.processmodeller.datatypes.Warning;



/**
 *
 * Responsible for managing Connection-related workflow warning.
 * Called into from Dataflow during analysis
 *
 */
public class DataflowConnectionWarningsManagement {

  private static final Logger logger = CentralFactoryLogging.getLogger(DataflowConnectionWarningsManagement.class);


  private WorkflowWarningsHandler workflowWarningsHandler;

  /**
   * required for inputConnections => To determine old providers for steps
   * inputConections object changed (-> is replaced) during analyzeDataflow
   */
  private Dataflow dataflow;

  /**
   * for every (relevant) AVariableIdentification, this map contains the list of providers 
   * that were available during the last calculation<br>
   * lifeCyle:<br>
   *  -read and updated when processing variable <br>
   *  -value objects are not replaced, just cleared
   * 
   */
  private Map<AVariableIdentification, List<AVariableIdentification>> oldProviders;

  /**
   * warnings we keep track of -> which providers were present when warning was created <br>
   * may contain Warnings that have been removed from the WarningsHandler (cleaned by outdatedWarnings) <br>
   * String in key is ObjectId of Warning (not warningId)
   */
  private Map<String, StoredWarningsEntry> storedWarnings;

  /**
   * warnings where key (ObjectId) does not match variable position<br>
   * lifeCycle:<br>
   *   -cleared at beginning of run<br>
   *   -adding entries of warnings where the current variable at objectId
   *    does not match the variable mentioned by the warning<br>
   *   -removing entries if they find their variable
  */
  private Set<StoredWarningsEntry> displacedWarnings;

  /**
   * ObjectId of warnings that were not mentioned during a run
   * meaning that these warnings should be removed, since their
   * variable does not exist anymore <br>
   * lifeCycle: <br>
   *   -set to keys of storedWarnings at beginning of run <br>
   *   -removing entries when processing providers for variable <br>
   *   -cleared at the end of a run, removing all outdated warnings
   */
  private Set<String> outdatedWarnings;


  public DataflowConnectionWarningsManagement(Dataflow dataflow) {
    this.dataflow = dataflow;
    oldProviders = new HashMap<>();
    storedWarnings = new HashMap<>();
    displacedWarnings = new HashSet<>();
    outdatedWarnings = new HashSet<>();
  }


  public void processNewProvidersForConnection(NewProvidersForConnectionData data) {
    try {
      processNewPrvidersForConnectionInternal(data);
    } catch (Exception e) {
      //exceptions with warnings should not stop us
      logger.debug("Exception during warning creation.", e);
    }
  }


  private void processNewPrvidersForConnectionInternal(NewProvidersForConnectionData data) {
    Step currentStep = data.getStep();
    AVariableIdentification in = data.getVariableToConnect();
    List<AVariableIdentification> actualProviders = data.getNewProviders();
    SimpleConnection sc = data.getNewConnection();
    List<AVariableIdentification> oldCandidates = getOldCandidates(currentStep, in);
    VarUsageType usage = data.getUsageType();
    String objectId = createObjectIdForWarning(currentStep, data.getVariableIndex(), usage).getObjectId();

    outdatedWarnings.remove(objectId);
    updateStaleEntries(data);

    if (sc != null && sc.getLinkState() == LinkstateIn.USER) {
      createUserConWarning(data, oldCandidates);
    }

    validateExistingWarning(data);

    //update last seen providers (after calling Workflow Warnings Handler and even if there is no userConnection at the moment)
    oldCandidates.clear();
    oldCandidates.addAll(actualProviders);
  }


  private void updateStaleEntries(NewProvidersForConnectionData data) {
    updateStaleObjectId(data);
    updateStaleAVariableReference(data);
  }


  //situation: in storedWarnings is an entry matching our objectId, but for a different AVariableIdentification
  //if we detect that, move warning to displacedWarnings
  private void updateStaleAVariableReference(NewProvidersForConnectionData data) {
    Step step = data.getStep();
    int index = data.getVariableIndex();
    VarUsageType usage = data.getUsageType();
    String objectId = createObjectIdForWarning(step, index, usage).getObjectId();
    StoredWarningsEntry entry = storedWarnings.get(objectId);
    if (entry == null) {
      return; // no entry
    }

    if (entry.getVariableToConnect().getIdentifiedVariable().equals(data.getVariableToConnect().getIdentifiedVariable())) {
      return; //variable correct
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Found stale Warning: " + entry.getWarningId() + ". ObjectId (" + objectId + ") does not match Variable ("
          + data.getVariableToConnect() + "). Warning is now displaced.");
    }

    storedWarnings.remove(objectId);
    displacedWarnings.add(entry);
  }


  //situation: in storedWarnings is an entry where objectId is stale
  //variable in matches the AVariableIdentification in that entry
  //if we detect a stale objectId, we should remove that entry and create a new Warning
  private void updateStaleObjectId(NewProvidersForConnectionData data) {
    AVariableIdentification in = data.getVariableToConnect();
    Step step = data.getStep();
    int index = data.getVariableIndex();
    VarUsageType usage = data.getUsageType();

    Optional<LoadedWarningEntry> entry = findEntryForVariable(in);
    if (entry.isEmpty()) {
      return;
    }
    LoadedWarningEntry loadedWarning = entry.get();
    String objectIdInUse = loadedWarning.getObjectId();
    ObjectId correctObjectId = createObjectIdForWarning(step, index, usage);
    if (objectIdInUse != null && objectIdInUse.equals(correctObjectId.getObjectId())) {
      return; //id is not stale
    }

    updateWarning(loadedWarning, correctObjectId);
  }


  private void updateWarning(LoadedWarningEntry loadedWarning, ObjectId correctObjectId) {
    String objectIdInUse = loadedWarning.getObjectId();
    String warningIdInUse = loadedWarning.getWarningsEntry().getWarningId();
    Warning oldWarning = workflowWarningsHandler.getWarning(warningIdInUse);
    if (oldWarning == null) {
      //warning was removed already
      if (logger.isDebugEnabled()) {
        logger.debug("warning " + warningIdInUse + " was deleted by user. Updating storedWarnings.");
      }
      storedWarnings.remove(objectIdInUse);
    } else {
      if (logger.isDebugEnabled() && objectIdInUse != null) {
        logger.debug("Detected stale warning: " + oldWarning.getWarningId() + ". It uses " + objectIdInUse + " instead of "
            + correctObjectId.getObjectId());
      }
      workflowWarningsHandler.deleteWarning(oldWarning.getWarningId());
      oldWarning.setObjectId(correctObjectId.getObjectId());
      workflowWarningsHandler.addWarning(oldWarning);
      if (objectIdInUse != null) {
        storedWarnings.remove(objectIdInUse);
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Previously displaced warning " + loadedWarning.getWarningsEntry().getWarningId() + " back in use for "
              + correctObjectId.getObjectId());
        }
        displacedWarnings.remove(loadedWarning.getWarningsEntry());
      }
      storedWarnings.put(correctObjectId.getObjectId(), loadedWarning.getWarningsEntry());
    }
  }


  private Optional<LoadedWarningEntry> findEntryForVariable(AVariableIdentification av) {
    for (Entry<String, StoredWarningsEntry> entry : storedWarnings.entrySet()) {
      if (entry.getValue().getVariableToConnect().getIdentifiedVariable().equals(av.getIdentifiedVariable())) {
        LoadedWarningEntry result = new LoadedWarningEntry();
        result.setObjectId(entry.getKey());
        result.setWarningsEntry(entry.getValue());
        return Optional.of(result);
      }
    }

    for (StoredWarningsEntry entry : displacedWarnings) {
      if (entry.getVariableToConnect().getIdentifiedVariable().equals(av.getIdentifiedVariable())) {
        LoadedWarningEntry result = new LoadedWarningEntry();
        result.setObjectId(null);
        result.setWarningsEntry(entry);
        return Optional.of(result);
      }
    }

    return Optional.empty();
  }


  /**
   * Remove warnings from WarningsHandler, if they are no longer valid <br>
   * - provider list does not contain new providers anymore
   * - userConnection no longer exists
   */
  private void validateExistingWarning(NewProvidersForConnectionData data) {
    Step currentStep = data.getStep();
    int varIndex = data.getVariableIndex();
    VarUsageType usage = data.getUsageType();
    ObjectId existingWarningObjectId = createObjectIdForWarning(currentStep, varIndex, usage);
    Linkstate linkstate = data.getNewConnection().getLinkState();

    StoredWarningsEntry entry = storedWarnings.get(existingWarningObjectId.getObjectId());
    if (entry == null) {
      return;
    }

    if (!linkstate.equals(LinkstateIn.USER) || !isEntryValid(entry, data)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Deleting warning, because it is no longer valid: " + entry.getWarningId() + " (linkstate: " + linkstate + ")");
        List<String> oldProviders = entry.getProvidersDuringCreation().stream().map(x -> x.toString()).collect(Collectors.toList());
        List<String> newProviders = data.getNewProviders().stream().map(x -> x.toString()).collect(Collectors.toList());
        logger.debug("oldProviders: " + String.join(", ", oldProviders));
        logger.debug("newProviders: " + String.join(", ", newProviders));
      }
      workflowWarningsHandler.deleteWarning(entry.getWarningId());
      storedWarnings.remove(existingWarningObjectId.getObjectId());
    }
  }


  /**
   * An Entry is still valid, if a new Warning would be created, given the current providers and those
   * present during the original warning
   */
  private boolean isEntryValid(StoredWarningsEntry entry, NewProvidersForConnectionData data) {
    List<AVariableIdentification> newProviders = data.getNewProviders();
    List<AVariableIdentification> oldProviders = entry.getProvidersDuringCreation();
    Step currentStep = data.getStep();
    boolean isConditionalMapping = isConditionalMapping(currentStep);
    boolean allowForeachLinks = !(isConditionalMapping || currentStep instanceof StepRetry);
    AVariableIdentification in = data.getVariableToConnect();

    NewConnectionCandidateWarningsChangeNotification notification;
    notification = new NewConnectionCandidateWarningsChangeNotification(oldProviders, newProviders, in, allowForeachLinks);

    return notification.shouldCreateWarning();
  }


  private void createUserConWarning(NewProvidersForConnectionData data, List<AVariableIdentification> oldCandidates) {
    Step currentStep = data.getStep();
    AVariableIdentification in = data.getVariableToConnect();
    List<AVariableIdentification> actualProviders = data.getNewProviders();
    int varIndex = data.getVariableIndex();
    VarUsageType usage = data.getUsageType();

    boolean isConditionalMapping = isConditionalMapping(currentStep);
    ObjectId objectId = createObjectIdForWarning(currentStep, varIndex, usage);
    //ForEach links are not allowed for conditional Mappings (Query), Retry and outputs
    boolean allowForeachLinks = !(isConditionalMapping || currentStep instanceof StepRetry || usage.equals(VarUsageType.output));

    //call Workflow Warnings Handler
    NewConnectionCandidateWarningsChangeNotification notification;
    notification = new NewConnectionCandidateWarningsChangeNotification(oldCandidates, actualProviders, in, allowForeachLinks);
    workflowWarningsHandler.handleChange(objectId, notification);
    Set<Warning> createdWarnings = notification.getCreatedWarnings();
    List<AVariableIdentification> filteredOldCandidates = notification.getOldCandidates();
    storeCreatedUserConWarnings(createdWarnings, filteredOldCandidates, in);
  }


  private ObjectId createObjectIdForWarning(Step currentStep, int varIndex, VarUsageType usage) {
    ObjectId result = null;
    try {
      result = ObjectId.parse(ObjectId.createVariableId(ObjectId.createStepId(currentStep).getBaseId(), usage, varIndex));
    } catch (Exception e) {
      logger.debug("Could not create objectId for Step " + currentStep + " input #" + varIndex, e);
    }
    return result;
  }


  private List<AVariableIdentification> determineOldestProviders(List<AVariableIdentification> oldProviders, String objectId) {
    StoredWarningsEntry oldWarning = storedWarnings.get(objectId);

    if (oldWarning != null && workflowWarningsHandler.getWarning(oldWarning.getWarningId()) == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("warning " + oldWarning.getWarningId() + " was deleted by user. Updating storedWarnings.");
      }
      oldWarning = null;
      storedWarnings.remove(objectId);
    }
    List<AVariableIdentification> oldestProviders = oldProviders;
    if (oldWarning != null) {
      oldestProviders = oldWarning.getProvidersDuringCreation();
    }

    return oldestProviders;
  }


  private void storeCreatedUserConWarnings(Set<Warning> newWarn, List<AVariableIdentification> oproviders, AVariableIdentification in) {
    for (Warning warning : newWarn) {
      StoredWarningsEntry entry = new StoredWarningsEntry();

      List<AVariableIdentification> oldestProviders = determineOldestProviders(oproviders, warning.getObjectId());
      entry.setWarningId(warning.getWarningId());
      entry.setProvidersDuringCreation(oldestProviders);
      entry.setVariableToConnect(in);

      if (logger.isDebugEnabled()) {
        logger.debug("adding warning to store: " + warning.getWarningId() + " for " + warning.getObjectId());
      }

      storedWarnings.put(warning.getObjectId(), entry);
    }
  }


  private boolean isConditionalMapping(Step currentStep) {
    return currentStep instanceof StepMapping && ((StepMapping) currentStep).isConditionMapping();
  }


  private List<AVariableIdentification> getOldCandidates(Step step, AVariableIdentification var) {
    List<AVariableIdentification> result = null;
    //otherwise check identified variable => should be unique in inputConnections
    if (step instanceof StepFunction || step instanceof StepChoice) {
      //can query HashMap, because hash does not change
      //and there might be multiple entries in inputConnections that have
      //inputConnections.getIdentifiedVariable == var.getIdentifiedVariable
      //-> if same StepFunction is used multiple times
      result = oldProviders.get(var);
    } else {
      //can't query HashMap directly, because hash might have changed
      Map<AVariableIdentification, InputConnection> connections = dataflow.getInputConnections();
      if (connections != null) { // connections is null after UNDO
        for (AVariableIdentification existingLink : connections.keySet()) {
          if (var.getIdentifiedVariable() != null && var.getIdentifiedVariable().equals(existingLink.getIdentifiedVariable())) {
            result = oldProviders.get(existingLink);
            break;
          }
        }
      }
    }

    if (result == null) {
      result = new ArrayList<AVariableIdentification>();
      oldProviders.put(var, result);
    }

    return result;
  }


  public void setWorkflowWarningsHandler(WorkflowWarningsHandler workflowWarningsHandler) {
    this.workflowWarningsHandler = workflowWarningsHandler;
  }


  public void prepareRun() {
    try {
      prepareRunInternal();
    } catch (Exception e) {
      logger.debug("Exception during prepareRun.", e);
    }
  }


  private void prepareRunInternal() {
    displacedWarnings.clear();
    outdatedWarnings.clear();

    for (String objectId : storedWarnings.keySet()) {
      outdatedWarnings.add(objectId);
    }
  }


  public void finishRun() {
    try {
      finishRunInternal();
    } catch (Exception e) {
      logger.debug("Exception during finishRun.", e);
    }
  }


  private void finishRunInternal() {
    clearOutdatedWarnings();
  }


  private void clearOutdatedWarnings() {
    for (String objectId : outdatedWarnings) {

      StoredWarningsEntry entry = storedWarnings.remove(objectId);
      if (entry == null) {
        continue;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Removing outdated warning: " + entry.getWarningId() + " (" + objectId + ")");
      }

      workflowWarningsHandler.deleteWarning(entry.getWarningId());
    }
  }


  public static class NewProvidersForConnectionData {

    private Step step;
    private AVariableIdentification variableToConnect;
    private List<AVariableIdentification> newProviders;
    private int variableIndex; //e.g. 0 for first input/output of step
    private VarUsageType usageType;
    private SimpleConnection newConnection;


    public Step getStep() {
      return step;
    }


    public void setStep(Step step) {
      this.step = step;
    }


    public AVariableIdentification getVariableToConnect() {
      return variableToConnect;
    }


    public void setVariableToConnect(AVariableIdentification variableToConnect) {
      this.variableToConnect = variableToConnect;
    }


    public List<AVariableIdentification> getNewProviders() {
      return newProviders;
    }


    public void setNewProviders(List<AVariableIdentification> newProviders) {
      this.newProviders = new ArrayList<>(newProviders);
    }


    public int getVariableIndex() {
      return variableIndex;
    }


    public void setVariableIndex(int variableIndex) {
      this.variableIndex = variableIndex;
    }


    public SimpleConnection getNewConnection() {
      return newConnection;
    }


    public void setNewConnection(SimpleConnection newConnection) {
      this.newConnection = newConnection;
    }


    public VarUsageType getUsageType() {
      return usageType;
    }


    public void setUsageType(VarUsageType usageType) {
      this.usageType = usageType;
    }
  }


  private static class StoredWarningsEntry {

    private String warningId; //id of the warning that was added to warningsHandler
    private List<AVariableIdentification> providersDuringCreation;
    private AVariableIdentification variableToConnect;


    public String getWarningId() {
      return warningId;
    }


    public void setWarningId(String warningId) {
      this.warningId = warningId;
    }


    public List<AVariableIdentification> getProvidersDuringCreation() {
      return providersDuringCreation;
    }


    public void setProvidersDuringCreation(List<AVariableIdentification> providersDuringCreation) {
      this.providersDuringCreation = new ArrayList<AVariableIdentification>(providersDuringCreation);
    }


    public AVariableIdentification getVariableToConnect() {
      return variableToConnect;
    }


    public void setVariableToConnect(AVariableIdentification variableToConnect) {
      this.variableToConnect = variableToConnect;
    }
  }

  private static class LoadedWarningEntry {

    private StoredWarningsEntry warningsEntry;
    private String objectId; // null if warningsEntry is displaced


    public StoredWarningsEntry getWarningsEntry() {
      return warningsEntry;
    }


    public void setWarningsEntry(StoredWarningsEntry warningsEntry) {
      this.warningsEntry = warningsEntry;
    }


    public String getObjectId() {
      return objectId;
    }


    public void setObjectId(String objectId) {
      this.objectId = objectId;
    }
  }
}
