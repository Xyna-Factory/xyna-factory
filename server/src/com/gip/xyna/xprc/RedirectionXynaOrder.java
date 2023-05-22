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
package com.gip.xyna.xprc;

import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionResponse;



public class RedirectionXynaOrder extends XynaOrderServerExtension {

  private static final long serialVersionUID = -8766921625317718238L;

  private XynaOrderServerExtension redirectedOrder;


  public RedirectionXynaOrder(DestinationKey dk, GeneralXynaObject payload, XynaOrderServerExtension redirectedOrder,
                              List<ManualInteractionResponse> allowedResponses) {
    super(dk, payload);
  // TODO geht so nicht  setRevision(redirectedOrder.getRevision()); idee: revision in auftrag=runtimecontext im orderarchive. revision im destinationkey=revision f�rs aufl�sen von destinations. wo werden inputs aufgel�st? responselistener?
    this.redirectedOrder = redirectedOrder;
    this.allowedResponses = allowedResponses;
  }


  public XynaOrderServerExtension getRedirectedOrder() {
    return redirectedOrder;
  }


  private List<ManualInteractionResponse> allowedResponses;


  public List<ManualInteractionResponse> getAllowedResponses() {
    return allowedResponses;
  }
}
