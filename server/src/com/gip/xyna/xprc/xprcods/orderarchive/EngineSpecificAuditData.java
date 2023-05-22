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

package com.gip.xyna.xprc.xprcods.orderarchive;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.xpce.ProcessStep;


public interface EngineSpecificAuditData extends Serializable {

  void addParameterPreStepValues(ProcessStep step, GeneralXynaObject[] generalXynaObjects, long version);


  void addParameterPostStepValues(ProcessStep step, GeneralXynaObject[] generalXynaObjects, long version);


  public String toXML(long id, long parentId, long startTime, long endTime, List<XynaExceptionInformation> exceptions, Long revision, boolean removeAuditDataAfterwards)
      throws XPRC_CREATE_MONITOR_STEP_XML_ERROR;


  void addPreCompensationEntry(ProcessStep pstep);


  void addPostCompensationEntry(ProcessStep pstep);


  void addErrorStepValues(ProcessStep pstep);
  
  
  void clearStepData();

}
