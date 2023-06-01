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
package com.gip.xyna.demon.worker;

import org.apache.log4j.Logger;



/**
 * Der Master weist den Slaves ihre Arbeit zu. 
 *
 * @param <Tool> Werkzeug, mit dem der Slave seine Arbeit verrichten kann
 * @param <Work> Arbeitspaket
 */
public interface Master<Tool,Work> extends Runnable {
  
  /**
   * Übergabe des SlavePools, an den der Master seine Aufträge weiterreicht
   * @param slavePool
   */
  public void setSlavePool(SlavePool<Tool,Work> slavePool);
  
  /**
   * Master wird ab nun nicht mehr benötigt und kann daher Ressourcen freigeben
   */
  public void terminate();

  /**
   * Log des Status
   * @param statusLogger 
   */
  public void logStatus(Logger statusLogger);

}
