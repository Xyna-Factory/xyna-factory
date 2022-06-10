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
package com.gip.xyna.xprc;



import java.util.List;

import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionResponse;



public class ManualInteractionXynaOrder extends XynaOrderServerExtension {

  private static final long serialVersionUID = -3385838056411569243L;

  private List<ManualInteractionResponse> allowedResponses;
 
  
  public ManualInteractionXynaOrder(XynaOrderCreationParameter xocp, List<ManualInteractionResponse> allowedResponses) {
    super(xocp);
    this.allowedResponses = allowedResponses;
  }
  
  public ManualInteractionXynaOrder(DestinationKey key, List<ManualInteractionResponse> allowedResponses) {
    super(key);
    this.allowedResponses = allowedResponses;
  }

  
  public ManualInteractionXynaOrder(List<ManualInteractionResponse> allowedResponses) {
    this(new DestinationKey(ManualInteractionManagement.MANUALINTERACTION_WORKFLOW_FQNAME), allowedResponses);
  }
  

  public List<ManualInteractionResponse> getAllowedResponses() {
    return allowedResponses;
  }

}
