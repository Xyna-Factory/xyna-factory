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
package com.gip.xyna.xfmg.xfctrl.threadmgmt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;


public class InfrastructureAlgorithmExecutionManagement extends FunctionGroup {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(InfrastructureAlgorithmExecutionManagement.class);
  
  private final ConcurrentMap<String, ManagedAlgorithm> managedAlgorithms;

  
  public InfrastructureAlgorithmExecutionManagement() throws XynaException {
    super();
    managedAlgorithms = new ConcurrentHashMap<>();
  }


  public String getDefaultName() {
    return "Infrastructure Algorithm Execution Management";
  }


  protected void init() throws XynaException {

  }


  protected void shutdown() throws XynaException {

  }
  
  
  public AlgorithmStateChangeResult startAlgorithm(String name) {
    try {
      return startAlgorithm(name, new AlgorithmStartParameter(false));
    } catch (StringParameterParsingException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public AlgorithmStateChangeResult startAlgorithm(String name, AlgorithmStartParameter startParams) throws StringParameterParsingException {
    return startAlgorithm(name, startParams, new OutputStream() { @Override public void write(int b) throws IOException {} });
  }
  
  
  public AlgorithmStateChangeResult startAlgorithm(String name, AlgorithmStartParameter startParams, OutputStream statusOutputStream) throws StringParameterParsingException {
    ManagedAlgorithm thread = managedAlgorithms.get(name);
    if (thread == null) {
      return AlgorithmStateChangeResult.NOT_REGISTERED;
    }
    if (thread.getStatus() != AlgorithmState.NOT_RUNNING) {
      if (!startParams.isRestartAllowed()) {
        return AlgorithmStateChangeResult.ALREADY_IN_STATE;
      } else {
        AlgorithmStateChangeResult stopResult = stopAlgorithm(name);
        switch (stopResult) {
          case FAILED :
          case NOT_REGISTERED :
            return stopResult;
          case SUCCESS :
            // fallthrough
          case ALREADY_IN_STATE :
            // ntbd
            break;
          default :
            throw new IllegalStateException("AlgorithmStateChangeResult " + stopResult + " could not be interpreted.");
        }
        // continue below with thread.start()
      }
    }
    Map<String, Object> params = StringParameter.parse(startParams.getStartParameter()).with(thread.getStartParameterInformation());
    if (thread.start(params, statusOutputStream)) {
      logger.debug("Started infrastructure thread " + name);
      return AlgorithmStateChangeResult.SUCCESS;
    } else {
      return AlgorithmStateChangeResult.FAILED;
    }
  }
  
  
  public AlgorithmStateChangeResult stopAlgorithm(String name) {
    ManagedAlgorithm thread = managedAlgorithms.get(name);
    if (thread == null) {
      return AlgorithmStateChangeResult.NOT_REGISTERED;
    }
    if (thread.getStatus() == AlgorithmState.NOT_RUNNING) {
      return AlgorithmStateChangeResult.ALREADY_IN_STATE;
    }
    if (thread.stop()) {
      logger.debug("Stopped infrastructure thread " + name);
      return AlgorithmStateChangeResult.SUCCESS;
    } else {
      return AlgorithmStateChangeResult.FAILED;
    }
  }
  
  
  public boolean registerAlgorithm(ManagedAlgorithm algo) {
    return managedAlgorithms.putIfAbsent(algo.getName(), algo) == null;
  }

  
  public Collection<ManagedAlgorithmInfo> listManagedAlgorithms() {
    return managedAlgorithms.values().stream().map(t -> t.getInfo()).collect(Collectors.toList());
  }

}
