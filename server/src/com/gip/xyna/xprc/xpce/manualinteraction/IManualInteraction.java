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

package com.gip.xyna.xprc.xpce.manualinteraction;

import java.util.Map;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.MiProcessingRejected;
import com.gip.xyna.xprc.exceptions.XPRC_FACTORY_IS_SHUTTING_DOWN;
import com.gip.xyna.xprc.exceptions.XPRC_IllegalManualInteractionResponse;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;



public interface IManualInteraction {


  public enum ProcessManualInteractionResult {
    SUCCESS, NOT_FOUND, FOREIGN_BINDING
  }


  public Map<Long, ManualInteractionEntry> listManualInteractionEntries() throws PersistenceLayerException;


  public ProcessManualInteractionResult processManualInteractionEntry(Long id, GeneralXynaObject response)
      throws XPRC_FACTORY_IS_SHUTTING_DOWN, PersistenceLayerException, XPRC_ResumeFailedException,
      XPRC_IllegalManualInteractionResponse, MiProcessingRejected;

}
