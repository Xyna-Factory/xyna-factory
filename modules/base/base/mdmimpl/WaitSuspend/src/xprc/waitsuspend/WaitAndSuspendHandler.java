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

package xprc.waitsuspend;



import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.orderabortion.SuspendedOrderAbortionSupportListenerInterface;
import com.gip.xyna.xprc.xsched.ordersuspension.ResumeOrderBean;


public class WaitAndSuspendHandler implements SuspendedOrderAbortionSupportListenerInterface {

  private static Logger logger = CentralFactoryLogging.getLogger(WaitAndSuspendHandler.class);
  
  public boolean cleanupOrderFamily(Long rootOrderId, Set<Long> suspendedOrderIds, ODSConnection con)
                  throws PersistenceLayerException {

    CronLikeScheduler cronLikeScheduler =
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
    Collection<CronLikeOrder> crons = cronLikeScheduler.getAllCronLikeOrdersForRootOrderIdFlat(rootOrderId, con);
    boolean deletedAtLeastOneCron = false;
    for (CronLikeOrder cron : crons) {
      boolean delete = false;
      if (suspendedOrderIds == null) {
        delete = true;
      } else if (cron.getCreationParameters().getOrderType()
          .equals(XynaDispatcher.DESTINATION_KEY_RESUME.getOrderType())) {
        if (cron.getCreationParameters().getInputPayload() instanceof ResumeOrderBean) {
          ResumeOrderBean rob = (ResumeOrderBean) cron.getCreationParameters().getInputPayload();
          if (suspendedOrderIds.contains(rob.getTarget().getOrderId())) {
            delete = true;
          }
        }
      }

      if (delete) {
        try {
          cronLikeScheduler.removeCronLikeOrder(con, cron.getId());
          deletedAtLeastOneCron = true;
        } catch (XPRC_CronRemovalException e) {
          logger.error("Failed to remove cron like order with id " + cron.getId(), e);
        }
      }
    }
    return deletedAtLeastOneCron;
  }

}
