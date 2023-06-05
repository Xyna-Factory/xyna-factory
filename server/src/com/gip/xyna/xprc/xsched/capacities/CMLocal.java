/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xsched.capacities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.DatabaseLock;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.Reservations;


/**
 *
 */
public class CMLocal extends CMAbstract {

  public CMLocal(ODS ods, CapacityCache cache, int ownBinding, CapacityStorableQueries capacityStorableQueries, DatabaseLock managementLock ) {
    super(ods, cache, ownBinding, capacityStorableQueries, managementLock);
  }

  @Override
  public boolean isClustered() {
    return false;
  }

  protected int transportReservedCaps(List<Reservations> reservations) {
    //eigentlich darf diese Methode nicht gerufen werden, deswegen wäre 
    //throw new UnsupportedOperationException("transportReservedCaps is unsupported" );
    //korrekt.
    //Leider gibt es das Problem, dass das CapacityManagement die Information, wegen eines DISCONNECTs 
    //auf CMLocal zu wechseln, zu einem anderen Zeitpunkt erhält las der ClusteredScheduler, der die 
    //Demands verwirft.
    //Deswegen wird hier nun nur eine Info ins Log geschrieben und ansonsten normal beendet.
    if (logger.isInfoEnabled()) {
      logger.info("transportReservedCaps(" + reservations + ") called for CMLocal!");
    }
    return 0;
  }
  
  @Override
  protected List<Integer> getAllBindings() {
    List<Integer> allBindings = null;
    //es kann sein, dass CapacityStorable geclustert ist und der CMAlgorithm nur zeitweise auf CMLocal steht 
    CapacityStorable cs = new CapacityStorable();
    if( cs.isClustered(ODSConnectionType.DEFAULT) ) {
      try {
        return cs.getClusterInstance(ODSConnectionType.DEFAULT).getAllBindingsIncludingLocal();
      } catch (XNWH_RetryTransactionException e) {
        //Hier kann nun nichts mehr getan werden, der ClusterProvider kennt das Problem
        //und hat den ClusterState bereits gewechselt. 
        throw new RuntimeException("currently not possible", e);
      }
    } else {
      allBindings = new ArrayList<Integer>();
      allBindings.add( ownBinding );
    }
    return allBindings;
  }

  @Override
  protected void refreshRemoteCapacityCache(String capName) {
    //nichts zu tun
  }

  protected void increaseCaps(ODSConnection defCon, CapacityStorables allCs, String capName, int addCard) throws PersistenceLayerException {
    //addCard Capacities werden hinzugefügt, dies ist immer möglich.
    allCs.getOwn().setCardinality(allCs.getOwn().getCardinality() + addCard );
    
    defCon.persistCollection(allCs);
    defCon.commit();

    //Eintrag im Cache aktualisieren
    cache.refresh(allCs);
  }

  /**
   * Verringern der Gesamt-Cardinality um removeCard
   * @param defCon
   * @param allCs
   * @param capName
   * @param removeCard
   * @throws PersistenceLayerException 
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState 
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain 
   */
  protected void decreaseCaps(ODSConnection defCon, CapacityStorables allCs, String capName, int removeCard) throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    //removeCard Capacities sollen entfernt werden, dies ist unter Umständen nicht möglich
    //hier werden 3 Fälle unterschieden, alle erfordern einen anderen Algorithmus
    //a) Cardinality kann lokal verringert werden
    //b) Capacity ist ACTIVE
    //c) Capacity ist DISABLED
    CapacityInformation ci = getLocalCapacityInformation(capName);
    
    int free = ci.getCardinality() - ci.getInuse();
    if( free >= removeCard ) {
      decreaseCapsLocal(defCon,allCs,capName,removeCard);
    } else {
      if( ci.getState() == State.ACTIVE ) {
        decreaseCapsActive(defCon,allCs,capName,removeCard);
      } else {
        decreaseCapsDisabled(defCon,allCs,capName,removeCard);
      }
    }    
  }

  private void decreaseCapsLocal(ODSConnection defCon, CapacityStorables allCs,
                                 String capName, int removeCard) throws PersistenceLayerException {
    //Verringern der lokalen Cardinality
    allCs.getOwn().setCardinality( allCs.getOwn().getCardinality() - removeCard );
    
    defCon.persistObject(allCs.getOwn());
    defCon.commit();
    
    //Eintrag im Cache aktualisieren
    cache.refresh(allCs);
  }

  private void decreaseCapsActive(ODSConnection defCon, CapacityStorables allCs,
                                  String capName, int removeCard) throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState {
    //Capacities werden laufend neu vergeben, daher ist die genaue Anzahl der verwendeten 
    //Caps nie bekannt -> was nun?
    //TODO bessere Implementierung
    logger.info("Can not change cardinality to the desired value, too many capacities in use");
    throw new XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState();
  }
  
  private void decreaseCapsDisabled(ODSConnection defCon, CapacityStorables allCs,
                                    String capName, int removeCard) throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    logger.info("Can not change cardinality to the desired value, too many capacities in use");
    throw new XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain();
  }
  
  public CapacityInformation getCapacityInformation(String capacityName) {
    return getLocalCapacityInformation(capacityName);
  }
  
  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation() {
    return getLocalExtendedCapacityUsageInformation();
  }

  public List<CapacityInformation> listCapacities() {
    Map<String, CapacityInformation> result = listLocalCapacities();
    return new ArrayList<CapacityInformation>(result.values());
  }

  protected boolean retryReadCap(int retry) {
    if( retry >= 5 ) {
      logger.warn("Giving up to retry after "+retry+" retries");
      return false;
    }
    return true;
  }

}
