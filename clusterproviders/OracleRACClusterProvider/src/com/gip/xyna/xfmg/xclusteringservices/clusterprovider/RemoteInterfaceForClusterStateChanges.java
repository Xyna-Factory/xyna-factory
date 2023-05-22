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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import com.gip.xyna.utils.db.SQLUtils;



public interface RemoteInterfaceForClusterStateChanges {

  /**
   * der joinende legt vorher in der db eine zeile f�r sich an
   * knoten A signalisiert an knoten B, dass er mit ins cluster will.
   */
  public void join(SQLUtils sqlUtils, int joinedBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException;

  /**
   * knoten A signalisiert an knoten B, dass er wieder da ist (war bereits im cluster)
   */
  public void startup(SQLUtils sqlUtils, int startingBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException;

  /**
   * W�hrend des Connect (nach startup oder join): B signalisiert an Knoten A, dass dieser noch nicht CONNECTED sein kann.
   * Diese Nachricht wird h�ufig verschickt, damit A wei�, dass B noch versucht, den CONNECT herzustellen 
   * @param connectingBinding
   */
  public void waiting(SQLUtils sqlUtils, int connectingBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException;

  /**
   * Antwort auf startup und join: B signalisiert an Knoten A, dass dieser nun CONNECTED ist.
   * @param connectingBinding
   */
  public void connect(SQLUtils sqlUtils, int connectingBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException;
  
  /**
   * knoten A signalisiert an knoten B, dass eine trennung der knoten passieren soll. 
   * Ziel-Zust�nde von A und B schreibt A vorher in die Tabelle XynaClusterSetup
   * 
   * 2 Usecases:
   * - A f�hrt runter
   *   
   * - A hat kein RMI Interconnect und will deshalb Master werden.
   *   - B ist down
   *     
   *   - (rmi-)kabel kaputt
   * 
   * keine fehlermeldungen, weil man nicht sinnvoll darauf reagieren kann.
   */
  public void disconnect(SQLUtils sqlUtils, int quittingBinding);
  
  /**
   * hat nichts mit remote zu tun, ist nur f�r das shutdown der implementierung dieser klasse (aufr�umen).
   * queue listener beenden und so.
   */
  public void shutdown(String cause);
  
  public boolean isShutdown();
  
}
