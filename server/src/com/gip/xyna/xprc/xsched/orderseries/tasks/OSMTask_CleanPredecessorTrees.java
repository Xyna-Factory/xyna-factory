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

package com.gip.xyna.xprc.xsched.orderseries.tasks;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.xsched.orderseries.OSMCacheImpl;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;




/**
 * OSMTask_CleanPredecessorTrees:
 *
 *
 */
public class OSMTask_CleanPredecessorTrees extends OSMTask {

  private CountDownLatch finishLatch;
  private String comment;

  public OSMTask_CleanPredecessorTrees() {
    finishLatch = new CountDownLatch(1);
  }
  
  @Override
  public String getCorrelationId() {
    return null;
  }

  @Override
  protected void executeInternal() {
    try {
      int sizeBefore = predecessorTrees.getNumberOfTrees();
      predecessorTrees.prune(ownBinding);
      int sizeAfter = predecessorTrees.getNumberOfTrees();
      //ab und zu bleiben noch Leichen im OSMCache zur�ck
      if( osmCache instanceof OSMCacheImpl ) {
        OSMCacheImpl osmCacheImpl = (OSMCacheImpl)osmCache;
        osmCacheImpl.lockAll();
        try {
          Collection<SeriesInformationStorable> chachedSis = osmCacheImpl.getCacheCopy();
          for( SeriesInformationStorable sis : chachedSis ) {
            if( sis.getOrderStatus() == OrderStatus.SUCCEEDED ) {
              if( sis.getSuccessorOrderIds().isEmpty() ) {
                //SeriesInformationStorable wird nicht mehr gebraucht
                osmCacheImpl.remove(sis.getCorrelationId());
              }
            }
          }
          
        } finally {
          osmCacheImpl.unlockAll();
        }
      }
      
      int maxSize = XynaProperty.ORDER_SERIES_MAX_PRE_TREES_IN_CACHE.get();
      if( sizeAfter > maxSize ) {
        logger.info( sizeAfter +" predecessorTrees but allowed are only "+ maxSize );
        predecessorTrees.shrinkToSize(maxSize*60/100); //60% der maximal erlaubten Menge aufbewahren
        sizeAfter = predecessorTrees.getNumberOfTrees();
      }
      
      comment = "Size of predecessorTrees pruned from "+sizeBefore+" to "+sizeAfter+".";
    } finally {
      if( finishLatch != null ) { 
        finishLatch.countDown();
      }
    }
  }

  /**
   * @throws InterruptedException 
   * 
   */
  public void await() throws InterruptedException {
    finishLatch.await();
  }

  /**
   * @return
   */
  public RescheduleSeriesOrderInformation getInfo() {
    RescheduleSeriesOrderInformation rsoi = new RescheduleSeriesOrderInformation();
    rsoi.setComment(comment);
    return rsoi;
  }

}
