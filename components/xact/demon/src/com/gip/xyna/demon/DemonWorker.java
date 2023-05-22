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

import org.apache.log4j.Logger;




/**
 * Dieses Interface gibt dem Demon die auszuf�hrende Arbeit an.
 * 
 * �ber den DemonStatus wird der Status �ber SNMP zug�nglich.
 */
public interface DemonWorker extends Runnable {

  /**
   * eingentlich Auftragsbearbeitung findet durch Starten eines Thread statt, der dieses run() aufruft.
   */
  public void run();
  
  /**
   * Stop der Auftragsannahme, angefangene Arbeiten d�rfen fortgesetzt werden
   */
  public void terminate();

  
  /**
   * @return Name des DemonWorkers
   */
  public String getName();

  /**
   * Log des Status
   * @param statusLogger 
   */
  public void logStatus(Logger statusLogger);

  /**
   * �bergibt den DemonSnmpConfigurator, mit dem weitere Ausgaben �ber SNMP eingerichtet werden k�nnen.
   * @param demonSnmpConfigurator
   */
  public void configureDemonSnmp(DemonSnmpConfigurator demonSnmpConfigurator);
  
}
