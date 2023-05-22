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

package com.gip.xyna.xfmg.xclusteringservices;



import java.util.List;

import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterInitializationException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidConnectionParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStartParametersForClusterProviderException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;



/**
 * eine instanz eines clusters.<br>
 * - hat verbindung zu anderen knoten.<br> 
 * - kennt seinen status<br>
 * lebenszyklus eines knotens:<br>
 * 1. createcluster oder joincluster<br>
 * 2. disconnect<br>
 * 3. restore<br>
 *   (bestehend aus 2 schritten: restorePrepare und restoreConnect)
 * 4. ggfs gehe zu 2.<br>
 * optional 5. crash<br>
 * optional 6. restore<br>
 * optional 7. ggfs gehe zu 2.<br>
 * 8. leave<br>
 * optional 9. gehe zu 1.
 * 
 */
public interface ClusterProvider {

  public boolean isConnected();

  /**
   * informationen f�r die CLI oder die GUI
   */
  public ClusterInformation getInformation();

  /**
   * vor�bergehend aus cluster raus, mit der motivation, sp�ter wieder mit restoreCluster dazusto�en zu k�nnen 
   */
  public void disconnect();

  /**
   * dieser knoten verl�sst das cluster endg�ltig.
   */
  public void leaveCluster();
  
  
  public void setClusterStateChangeHandler(ClusterStateChangeHandler cscHandler);


  public void changeClusterState(ClusterState newState);


  public String getTypeName();


  /**
   * parameter info for a new {@link ClusterProvider} instance format: (&lt;parameter name&gt; : &lt;description&gt;\n )*
   */
  public String getStartParameterInformation();


  /**
   * parameter info how to add a new node to this cluster format: (&lt;parameter name&gt; : &lt;description&gt;\n )*
   */
  public String getNodeConnectionParameterInformation();


  /**
   * erstellt leeres cluster
   * @return interne id, die beim {@link #restoreClusterPrepare(long)} wieder angegeben wird
   */
  public long createCluster(String[] startParameters) throws XFMG_InvalidStartParametersForClusterProviderException,
      XFMG_ClusterInitializationException;


  /**
   * stellt gespeicherte clusterinstanz wieder her
   */
  public void restoreClusterPrepare(long internalClusterId) throws XFMG_ClusterInitializationException;

  /**
   * nach dem aufruf von {@link #restoreClusterPrepare(long)}: stellt connection zu anderen knoten (falls vorhanden) her.
   */
  public void restoreClusterConnect();

  /**
   * f�gt verbindung zu weiterem cluster knoten dazu
   * @return interne id, die beim {@link #restoreClusterPrepare(long)} wieder angegeben wird
   */
  public long joinCluster(String[] connectionParameters)
      throws XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException;


  public int getLocalBinding();


  public ClusterState getState();

  /**
   * Gibt eine Liste aller Bindings zur�ck oder wirft XNWH_RetryTransactionException, wenn dies nicht
   * m�glich ist. Ein m�glicher Retry wird intern gemacht, au�erhalb ist kein Retry n�tig.
   * @return Liste aller Bindings
   * @throws XNWH_RetryTransactionException falls Bindings nicht ermittelt werden k�nnen (beispielsweise weil DB nicht verf�gbar ist)
   */
  public List<Integer> getAllBindingsIncludingLocal() throws XNWH_RetryTransactionException;

  /**
   * clusterprovider checkt seinen interconnect und macht status-change, wenn notwendig.
   */
  public void checkInterconnect();

  /**
   * ClusterProvider wird informiert, dass ein erneuter Aufruf seines ClusterStateChangeHandler.isReadyForChange(...)
   * nun true ergeben k�nnte.
   */
  public void readyForStateChange();
  
  
  /**
   * Currently only called from a clustered OraclePersistenceLayer to failFast if the OracleRAC-DB is not reachable
   * Might be better communicated via getState() = DISCONNECTED_CRASH  
   */
  public boolean fastCheckIsMediumReachable();
  
}
