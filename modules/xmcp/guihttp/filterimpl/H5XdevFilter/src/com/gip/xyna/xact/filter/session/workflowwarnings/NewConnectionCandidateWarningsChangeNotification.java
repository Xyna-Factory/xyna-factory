package com.gip.xyna.xact.filter.session.workflowwarnings;



import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import xmcp.processmodeller.datatypes.Warning;



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

public class NewConnectionCandidateWarningsChangeNotification implements WarningsChangeNotification {

  private static final Logger logger = CentralFactoryLogging.getLogger(NewConnectionCandidateWarningsChangeNotification.class);

  private List<AVariableIdentification> AVarFilteredOldCandidates;
  private List<String> filteredOldCandidates;
  private List<String> filteredNewCandidates;
  private AVariableIdentification toConnect;
  private boolean allowForeachLinks;
  
  private Set<Warning> createdWarnings;


  public NewConnectionCandidateWarningsChangeNotification(List<AVariableIdentification> oldCandidates,
                                                          List<AVariableIdentification> newCandidates, AVariableIdentification toConnect,
                                                          boolean allowForeachLinks) {
    this.toConnect = toConnect;
    this.allowForeachLinks = allowForeachLinks;
    this.createdWarnings = new HashSet<Warning>();
    this.filteredOldCandidates = filterToConnectable(oldCandidates);
    this.filteredNewCandidates = filterToConnectable(newCandidates);
    this.AVarFilteredOldCandidates = oldCandidates;
  }


  @Override
  public void handle(ObjectId objectId, WorkflowWarningsHandler handler) {
    boolean shouldCreateWarning = shouldCreateWarning();
    if (shouldCreateWarning) {
      ObjectId warningId = ObjectId.createWarningId(WorkflowWarningsHandler.warningIdx++);

      if (logger.isDebugEnabled()) {
        logger.debug("Creating warning with id: " + objectId.getObjectId() + ". \nOldCandidates: "
            + String.join(", ", filteredOldCandidates)
            + ". \nNew Candidates: "
            + String.join(", ", filteredNewCandidates));
      }

      Warning warning = new Warning(objectId.getObjectId(), warningId.getObjectId(), WorkflowWarningMessageCode.NEW_CONNECTION_CANDIDATE);
      Warning oldWarning = handler.addWarning(warning);
      
      //warning was not replaced in warningsHandler.
      if(oldWarning != null){
        warning = oldWarning;
      }
      createdWarnings.add(warning);
    }
  }


  public boolean shouldCreateWarning() {
    if (filteredOldCandidates.isEmpty()) {
      return false;
    }
    //return true, if there is a provider in filteredNew, that was not there in filteredOld
    return filteredNewCandidates.stream().anyMatch(x -> !filteredOldCandidates.contains(x));
  }

  
  private boolean filterMyBeLinked(AVariableIdentification x) {
    try {
      return Dataflow.mayBeLinked(toConnect, x);
    } catch (Exception e) {
      return false;
    }
  }

  
  private boolean removeInvalidIds(AVariableIdentification x) {
    try {
      x.idprovider.getId();
      return false;
    } catch (Exception e) {
      return true;
    }
  }
  
  private List<String> filterToConnectable(List<AVariableIdentification> sources) {
    List<AVariableIdentification> result = sources.stream().filter(this::filterMyBeLinked).collect(Collectors.toList());

    if (toConnect.getIdentifiedVariable() != null && !allowForeachLinks && !toConnect.getIdentifiedVariable().isList()) {
      result.removeIf(x -> x.getIdentifiedVariable().isList());
    }
    
    //ignore variable we are connected to
    result.removeIf(x -> x.getIdentifiedVariable().getId().equals(toConnect.connectedness.getConnectedVariableId()));
    result.removeIf(this::removeInvalidIds);
    List<String> resultIds = result.stream().map(x -> x.idprovider.getId()).collect(Collectors.toList());

    return resultIds;
  }


  
  public Set<Warning> getCreatedWarnings() {
    return createdWarnings;
  }


  
  public List<AVariableIdentification> getOldCandidates() {
    return AVarFilteredOldCandidates;
  }
}
