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

package com.gip.xyna.xact.filter.session.workflowwarnings;



import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;

import xmcp.processmodeller.datatypes.Warning;
import xmcp.processmodeller.datatypes.response.GetWarningsResponse;



public abstract class WorkflowWarningsHandler {

  public static int warningIdx = 0;


  public void handleChange(ObjectId objectId, WarningsChangeNotification change) {
    change.handle(objectId, this);
  }


  public abstract GetWarningsResponse createWarningsResponse(GenerationBaseObject obj);
  public abstract Warning addWarning(Warning warning);
  public abstract Warning deleteWarning(String warningId);
  public abstract Warning getWarning(String warningId);
  public abstract void deleteAllWarnings();
  /**
   * Deletes all warnings matching objectId <br>
   * - if objectId is a step, all warnings associated with that step are deleted <br>
   * - otherwise only warnings exactly matching the objectId are deleted
   */
  public abstract void deleteAllWarnings(String objectId);
  

  /**
   * Empty implementations of WorkflowWarningsHandler methods.
   * 
   * Can be used instead of a regular WorkflowWarningsHandler to ignore warnings
   *
   */
  public static class EmptyWorkflowWarningsHandler extends WorkflowWarningsHandler{

    @Override
    public GetWarningsResponse createWarningsResponse(GenerationBaseObject obj) {
      return null;
    }

    @Override
    public Warning addWarning(Warning warning) {
      return null;
    }

    @Override
    public Warning deleteWarning(String warningId) {
      return null;
    }

    @Override
    public Warning getWarning(String warningId) {
      return null;
    }

    @Override
    public void deleteAllWarnings() {
      
    }

    @Override
    public void deleteAllWarnings(String objectId) {
    }

  }


}
