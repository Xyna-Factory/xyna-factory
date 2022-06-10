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

package com.gip.xyna.xact.filter.session.workflowwarnings;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;

import xmcp.processmodeller.datatypes.Warning;
import xmcp.processmodeller.datatypes.response.GetWarningsResponse;



public class DefaultWorkflowWarningsHandler extends WorkflowWarningsHandler {

  private static final Logger logger = CentralFactoryLogging.getLogger(DefaultWorkflowWarningsHandler.class);
  private Map<String, Warning> pendingWarnings = new TreeMap<>();


  @Override
  public GetWarningsResponse createWarningsResponse(GenerationBaseObject obj) {
    GetWarningsResponse result = new GetWarningsResponse();
    result.setWarnings(new ArrayList<>(pendingWarnings.values()));

    return result;
  }

  @Override
  public Warning addWarning(Warning warning) {
    if(warning == null) {
      return null;
    }
    
    for (Warning pendingWarning : pendingWarnings.values()) {
      if (Objects.equals(pendingWarning.getObjectId(), warning.getObjectId()) &&
          Objects.equals(pendingWarning.getMessageCode(), warning.getMessageCode())) {
        // a warning of this type for this object already exists
        return pendingWarning;
      }
    }

    return pendingWarnings.put(warning.getWarningId(), warning);
  }

  @Override
  public Warning deleteWarning(String warningId) {
    return pendingWarnings.remove(warningId);
  }

  @Override
  public void deleteAllWarnings() {
    pendingWarnings = new TreeMap<>();
  }


  private boolean shouldDeleteWarnings(ObjectId objectIdIn, ObjectId pendingWarningObjectId) {

    //delete all warnings for this step
    if (ObjectType.step == objectIdIn.getType()) {
      return Objects.equals(objectIdIn.getBaseId(), pendingWarningObjectId.getBaseId());
    }

    //delete by object id
    return Objects.equals(objectIdIn.getObjectId(), pendingWarningObjectId.getObjectId());
  }
  
  
  private void deleteAllWarningsInternal(String objectIdStr) throws Exception{
    ObjectId objectId = ObjectId.parse(objectIdStr);

    synchronized (pendingWarnings) {
      Set<Entry<String, Warning>> entries = new HashSet<>(pendingWarnings.entrySet());
      for (Entry<String, Warning> entry : entries) {
        ObjectId pendingWarningObjectId = ObjectId.parse(entry.getValue().getObjectId());
        if (shouldDeleteWarnings(objectId, pendingWarningObjectId)) {
          Warning removedWarning = pendingWarnings.remove(entry.getKey());
          if(logger.isDebugEnabled()) {
            logger.debug("Removed " + removedWarning + " from pending warnings.");
          }
        }
      }
    }

    
    if (objectId.getType() != ObjectType.formula && objectId.getType() != ObjectType.expression) {
      return;
    }

    // adapt indices of succeeding formulas
    int deletedFormulaIdx = ObjectId.parseFormulaNumber(objectId);
    for (Entry<String, Warning> warningEntry: pendingWarnings.entrySet()) {
      Warning curWarning = warningEntry.getValue();
      ObjectId curWarningObjectId = ObjectId.parse(curWarning.getObjectId());
      int curWarningFormulaIdx = ObjectId.parseFormulaNumber(curWarningObjectId);
      if (Objects.equals(curWarningObjectId.getBaseId(), objectId.getBaseId()) &&
          curWarningObjectId.getType() == objectId.getType() &&
          curWarningFormulaIdx > deletedFormulaIdx) {
        curWarning.setObjectId(ObjectId.createFormulaId(curWarningObjectId.getBaseId(), VarUsageType.input, curWarningFormulaIdx-1));
      }
    }
  }


  @Override
  public void deleteAllWarnings(String objectIdStr) {
    try {
      deleteAllWarningsInternal(objectIdStr);
    } catch (Exception e) {
      logger.debug("Exception during warning deletion: ", e);
    }
  }
  
  @Override
  public Warning getWarning(String warningId) {
    return pendingWarnings.get(warningId);
  }

}
