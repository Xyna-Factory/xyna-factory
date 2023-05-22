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
package com.gip.xyna.xnwh.persistence;



final public class TransactionProperty {

  public static enum TransactionPropertyType {
    /**
     * falls gesetzt, k�nnen alle zugriffe aufs netzwerk bei einem clusterbetrieb asynchron ausgef�hrt werden. ausnahme:
     * beim lesenden zugriff werden daten bei bedarf �bers netzwerk aktualisiert, wenn lokal die daten nicht aktuell
     * sind.
     * <p>
     * d.h. locking passiert nur lokal und ist damit nur passiv gegen�ber konkurrierenden locks aus dem cluster
     * synchronisiert (der andere knoten muss aktiv synchronisieren). nicht synchronisierte updates auf verschiedenen
     * knoten k�nnen sich gegenseitig �berschreiben.
     * <p>
     * folglich sind daten bei schreibenden operationen (update, delete, insert) ggfs. nicht ausfall-sicher, obwohl von
     * der ausgef�hrten persistencelayer operation erfolg zur�ckgemeldet wurde. ausserdem wird ggfs. f�lschlicherweise
     * erfolg gemeldet, obwohl das cluster bei der nachtr�glichen synchronisierung einen fehler meldet (z.b. weil das
     * objekt bereits gel�scht wurde)
     */
    noSynchronousActiveClusterSynchronizationNeeded,

    /**
     * beim commit soll der cluster-zustand atomar mit dem hier angegebenen
     * {@link TransactionProperty#getCommitIfClusterNodeStateEqualHash()} verglichen werden. falls unterschiedlich, wird
     * ein fehler geworfen. damit kann sichergestellt werden, dass applikationslogik threadsicher von einem
     * clusternodestate ausgehen und verschieden darauf reagieren kann.
     */
    commitIfClusterNodeStateEqual,
    
    /**
     * wenn man eine einzelne zeile selektiert, wird nicht immer die gleiche zeile zur�ckgegeben (sofern es mehrere passende gibt),
     * sondern eine zuf�llige passende.
     */
    getRandomElement;
  }


  final private TransactionPropertyType type;
  private long clusterNodeStateHash;

  private static final TransactionProperty noClusterSyncTP;
  private static final TransactionProperty selectRandomElementTP;

  static {
    noClusterSyncTP = new TransactionProperty(TransactionPropertyType.noSynchronousActiveClusterSynchronizationNeeded);
    selectRandomElementTP = new TransactionProperty(TransactionPropertyType.getRandomElement);
  }


  /*
   * statische konstruktoren:
   */

  public static TransactionProperty noSynchronousActiveClusterSynchronizationNeeded() {
    return noClusterSyncTP;
  }


  public static TransactionProperty selectRandomElement() {
    return selectRandomElementTP;
  }

  
  public static TransactionProperty commitIfClusterNodeStateEqual(long clusterNodeStateHash) {
    TransactionProperty tp = new TransactionProperty(TransactionPropertyType.commitIfClusterNodeStateEqual);
    tp.clusterNodeStateHash = clusterNodeStateHash;
    return tp;
  }
  

  /*
   * normaler privater konstruktor
   */
  private TransactionProperty(TransactionPropertyType type) {
    this.type = type;
  }


  /*
   * membervars zu verschiedenen typen 
   */

  public long getCommitIfClusterNodeStateEqualHash() {
    return clusterNodeStateHash;
  }


  public TransactionPropertyType getPropertyType() {
    return type;
  }
  

}
