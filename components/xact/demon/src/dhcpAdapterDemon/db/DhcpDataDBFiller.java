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
package dhcpAdapterDemon.db;

import org.apache.log4j.Logger;

import dhcpAdapterDemon.DhcpData;

/**
 * 
 *
 */
public interface DhcpDataDBFiller  {


  /**
   * Start des DBFillers: evtl. gesicherte Daten lesen, danach DBFillers in eigenem Thread starten
   */
  public void start();

  /**
   * Stop des DBFillers: evtl. nicht verarbeitete Daten sichern
   */
  public void terminate();

  /**
   * Logt den Status
   * @param statusLogger 
   */
  public void logStatus(Logger statusLogger);


  /**
   * Eintragen eines weiteren Dhcp-Datensatzes
   * @param dhcpData
   */
  public void add(DhcpData dhcpData);

  /**
   * Ausgabe der Statistik-Daten
   * @return
   */
  public DBFillerStatistics getDBFillerStatistics();
  
}