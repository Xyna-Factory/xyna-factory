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
package com.gip.xyna.xprc.xpce.ordersuspension.interfaces;

/**
 * Interface mit den Eigenschaften, die der SuspendResumeAlgorithm verwendet
 */
public interface ResumableParallelExecutor {

  
  public static enum ResumeState {
    /**
     * Task wurde nicht gefunden in der List aller suspendierten Tasks
     */
    NotFound, 
    /**
     * FractalWorkflowParallelExecutor ist in einem Zustand, in dem er nicht läuft und daher keine Resumes durchführen kann
     */
    NotRunning, 
    /**
     * Resume hat geklappt
     */
    Resumed, 
    /**
     * ParallelExecutor wurde nicht gefunden
     */
    NoParallelExecutorFound,
    /**
     * ParallelExecutors Threadpool kann keine Tasks annehmen
     */
    ParallelExecutorOverloaded; 
  }
  
  
  /**
   * Resume eines einzelnen Tasks
   * @param laneId
   * @return
   */
  ResumeState resumeTask(String laneId);
 

  /**
   * Ausgabe der speziellen Id des ParallelExecutors, innerhalb des Workflows unique
   * @return
   */
  String getParallelExecutorId();


  /**
   * Wartet, bis alle Threads der parallelen Ausführung beendet sind
   * @return
   * @throws InterruptedException 
   */
  boolean await() throws InterruptedException;

}
