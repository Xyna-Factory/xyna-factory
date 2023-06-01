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
package com.gip.xyna.demon;

/**
 * Über dieses Interface kann der DemonWorker weitergehend konfiguriert werden. 
 * Es kann festgelegt werden, welche Objekte zu intialisieren sind und ob diese 
 * bei den Statusübergängen des Demons gestoppt oder gestartet werden müssen.  
 *
 */
public interface DemonWorkConfigurator {

  /**
   * notwendige Initialisierungen
   */
  public void initialize();
  
  /**
   * Statusübergang des Demons: Stopped->Running
   * 
   */
  public void start();
  
  /**
   * Statusübergang des Demons: Running->Stopped
   */
  public void terminate();
  
}
