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

package com.gip.xyna.coherence.coherencemachine;



/**
 * Class representing an create action. Used for creating an single object.
 */
public class CreateObjectAction extends CoherenceAction {

  private static final long serialVersionUID = -5637261540819109669L;


  public CreateObjectAction(long objectId, int requestingClusterID) {
    super(requestingClusterID, objectId);
  }


  @Override
  public final CoherenceActionType getActionType() {
    return CoherenceActionType.CREATE;
  }

}
