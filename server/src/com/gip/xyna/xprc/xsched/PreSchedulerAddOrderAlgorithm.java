/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xprc.xsched;

import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;


public interface PreSchedulerAddOrderAlgorithm {
  
  /**
   * f�gt einen auftrag zum scheduler hinzu. wird im prescheduler benutzt
   * @throws XPRC_OrderEntryCouldNotBeAcknowledgedException
   * @throws XNWH_RetryTransactionException
   */
  public void addOrderToScheduler(XynaOrderServerExtension xo) throws XPRC_OrderEntryCouldNotBeAcknowledgedException,
      XNWH_RetryTransactionException;
}
