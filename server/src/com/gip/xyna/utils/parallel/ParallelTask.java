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
package com.gip.xyna.utils.parallel;


/**
 * Task, der im {@link ParallelExecutor} ausgeführt werden kann.
 * Dieser Task hat eine Priorität, die über {@link #getPriority()} vom ParallelExecutor ausgewertet wird.
 * 
 */
public interface ParallelTask {
  
  /**
   * Priority, mit der der Task ausgeführt wird. Hohe Werte werden bevorzugt.
   * @return
   */
  int getPriority();

  /**
   * Ausführung des Tasks. Darf keine Exceptions werfen, da diese den Thread beenden!
   */
  void execute(); 
  
}
