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
package com.gip.xyna.xfmg.xfctrl.threadmgmt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.utils.misc.StringParameter;

public class ManagedAlgorithmInfo {
  
  private final String name;
  private final AlgorithmState status;
  private final List<StringParameter<?>> parameterInformation;
  private final Map<String, Object> parameter;
  private final Optional<Throwable> terminationException;
  private final long startTime;
  private final long stopTime;
  private final long lastExecution;
  
  
  public ManagedAlgorithmInfo(String name, AlgorithmState status, List<StringParameter<?>> additionalParameters, Map<String, Object> parameter,
                              Optional<Throwable> terminationException, long startTime, long stopTime, long lastExecution) {
    this.name = name;
    this.status = status;
    this.parameterInformation = additionalParameters;
    this.parameter = parameter;
    this.terminationException = terminationException;
    this.startTime = startTime;
    this.stopTime = stopTime;
    this.lastExecution = lastExecution;
  }

  
  public String getName() {
    return name;
  }

  
  public AlgorithmState getStatus() {
    return status;
  }

  
  public List<StringParameter<?>> getAdditionalParameters() {
    return parameterInformation;
  }
  
  
  public Map<String, Object> getParameter() {
    return parameter;
  }
  
  
  public Optional<Throwable> getTerminationException() {
    return terminationException;
  }
  
    
  public long getStartTime() {
    return startTime;
  }
  
  public long getStopTime() {
    return stopTime;
  }
  
  public long getLastExecution() {
    return lastExecution;
  }

}
