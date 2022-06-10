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

import java.lang.Thread.State;
import java.util.HashSet;
import java.util.Set;

public enum AlgorithmState {

  RUNNING(State.RUNNABLE), 
  NOT_RUNNING(State.NEW, State.TERMINATED),
  WAITING(State.BLOCKED, State.WAITING, State.TIMED_WAITING);
  
  
  private final Set<State> javaStates;
  
  
  private AlgorithmState(State... javaStateArray) {
    this.javaStates = new HashSet<>();
    for (State state : javaStateArray) {
      javaStates.add(state);
    }
  }
  
  public Set<State> getJavaStates() {
    return javaStates;
  }
  
  public static AlgorithmState getByJavaState(Thread thread) {
    State currentState = thread.getState();
    System.out.println("currentState: " + currentState);
    for (AlgorithmState status : values()) {
      for (State state : status.getJavaStates()) {
        if (currentState == state) {
          return status;
        }
      }
    }
    throw new IllegalArgumentException("No representation for current state: " + currentState);
  }
  
}
