/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
 * Über die Methoden dieses Interfaces werden die Slave-Threads mit ihrem Werkzeug <Tool> ausgestattet.
 *
 * @param <Tool>
 */
public interface SlaveInitializer<Tool> {

  /**
   * Liefert ein neues Werkzeug für den Slave "number"
   * @param number
   * @return
   */
  public Tool create(int number);

  /**
   * Sammelt das Werkzeug wieder ein
   * @param tool
   * @param number
   */
  public void destroy( Tool tool, int number );

  /**
   * Liefert den Prefix, mit dem die Slave-Thread-Namen beginnen
   * @return 
   */
  public String getThreadNamePrefix();

  /**
   * SlaveInitializer wird ab nun benötigt und sollte daher fertig initialisiert sein
   */
  public void initialize();

  /**
   * SlaveInitializer wird ab nun nicht mehr benötigt und kann daher Ressourcen freigeben
   */
  public void terminate();

  /**
   * Log des Status
   * @param statusLogger 
   */
  public void logStatus(Logger statusLogger);
  
}
