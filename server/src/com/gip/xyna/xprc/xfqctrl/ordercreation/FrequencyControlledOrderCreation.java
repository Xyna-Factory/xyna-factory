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
package com.gip.xyna.xprc.xfqctrl.ordercreation;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfqctrl.FrequenceControlledTaskEventAlgorithm;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTask.FrequencyControlledOrderInputSourceUsingTask;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTask.FrequencyControlledOrderInputSourceUsingTaskCreationParameter;


public class FrequencyControlledOrderCreation extends FunctionGroup {

  public static final String DEFAULT_NAME = "Frequency Controlled Order Creation";
  
  public static final Logger logger = CentralFactoryLogging.getLogger(FrequencyControlledOrderCreation.class);
  
  
  public FrequencyControlledOrderCreation() throws XynaException {
    super();
  }
  
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
  }


  @Override
  protected void shutdown() throws XynaException {

  }


  public FrequencyControlledOrderCreationTask generateFrequencyControlledOrderCreationTask(final FrequencyControlledOrderCreationTaskCreationParameter creationParameter,
                                                                                           final FrequenceControlledTaskEventAlgorithm eventAlgorithm)
      throws XynaException {
    if (creationParameter instanceof FrequencyControlledOrderInputSourceUsingTaskCreationParameter) {
      return new FrequencyControlledOrderInputSourceUsingTask(
                                                              (FrequencyControlledOrderInputSourceUsingTaskCreationParameter) creationParameter,
                                                              eventAlgorithm);
    }
    return new FrequencyControlledOrderCreationTask(creationParameter, eventAlgorithm);
  }

}
