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
package com.gip.xyna.xprc.xpce.cleanup;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

public class XynaCleanup extends FunctionGroup {

  public static final String DEFAULT_NAME = "Xyna Cleanup";
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaCleanup.class);

  private CleanupDispatcher cleanupEngineDispatcher;


  public XynaCleanup() throws XynaException {
    super();
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
    cleanupEngineDispatcher = new CleanupDispatcher();
  }


  public void shutdown() throws XynaException {
  }


  public void dispatch(XynaOrderServerExtension xo) throws XynaException {
    cleanupEngineDispatcher.dispatch(xo);
  }
  

  private static final AtomicLong cntThreadsPostCleanup = new AtomicLong(0);
  private static final XynaPropertyInt maximumNumberOfUnsecuredThreadsPostCleanup =
      new XynaPropertyInt("xprc.xpce.cleanup.capacities.free.late.thread.maximum", 100)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Maximum number of order execution threads executing cleanup and finish phases concurrently without the" +
                                   " limitation of capacities. Orders exceeding this number will free their capacities and vetoes only after" +
                                   " they have been archived and have notified their response listener.");


  public static void cleanup(XynaOrderServerExtension xo) throws PersistenceLayerException {
    if (numberOfThreadsInPostCleanupIsSmall()) {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().freeCapacitiesAndVetos(xo, true, true);
      if (xo.hasParentOrder()) {
        xo.setNeedsToBeBackupedOnSuspensionOfParent(true);
      }
      if (logger.isTraceEnabled()) {
        logger.trace("#threadspostcleanup=" + cntThreadsPostCleanup.get());
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Too many threads (" + cntThreadsPostCleanup.get() + ") waiting to be archived. Order " + xo.getId()
            + " will free capacities and vetoes after archiving is finished.");
      }
      //zuviele threads sind mit dem postcleanup besch�ftigt und die kapazit�tsfreigabe kann zu einer threadanzahlsexplosion f�hren. -> ab sofort kapazit�t erst ganz am ende freigeben
      xo.setFreeCapacityLater(true);
    }
  }
  
  public static void cleanupFinally(XynaOrderServerExtension xo) {
    if (xo.getFreeCapacityLater()) {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().freeCapacitiesAndVetos(xo, true, true);
      if (xo.hasParentOrder()) {
        xo.setNeedsToBeBackupedOnSuspensionOfParent(true);
      }
      xo.setFreeCapacityLater(false);
    } else {
      cntThreadsPostCleanup.decrementAndGet();
    }
  }


  private static boolean numberOfThreadsInPostCleanupIsSmall() {
    boolean isSmall = cntThreadsPostCleanup.incrementAndGet() <= maximumNumberOfUnsecuredThreadsPostCleanup.get();
    if (isSmall) {
      //gibt gleich capacity zur�ck und soll deshalb den threadz�hler hochgez�hlt belassen
      return true;
    }
    cntThreadsPostCleanup.decrementAndGet();
    return false;
  }


  public CleanupDispatcher getCleanupEngineDispatcher() {
    return cleanupEngineDispatcher;
  }

}
