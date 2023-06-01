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
package com.gip.xyna.xprc.xsched;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;



public class PreScheduler extends FunctionGroup {

  public static final String DEFAULT_NAME = "PreScheduler";
  
  private static PreSchedulerAddOrderAlgorithm addOrderAlgorithm = new DefaultPreSchedulerAddOrderAlgorithm();


  PreScheduler() throws XynaException {
    super();
  }


  public void preschedule(XynaOrderServerExtension xo) throws XPRC_OrderEntryCouldNotBeAcknowledgedException, 
                                                              XNWH_RetryTransactionException, 
                                                              XNWH_GeneralPersistenceLayerException, 
                                                              XPRC_DUPLICATE_CORRELATIONID {
    
    //serieshandling
    if ( xo.isInOrderSeries() ) {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderSeriesManagement().preschedule(xo);
    }
    
    //xo.getSchedulerBean() hat nun caps und resourcen
    addOrderAlgorithm.addOrderToScheduler(xo);
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
  }


  public void shutdown() throws XynaException {
  }


  public static synchronized void setAlgorithm(PreSchedulerAddOrderAlgorithm newAlgorithm) {
    addOrderAlgorithm = newAlgorithm;
  }
  
  public static synchronized PreSchedulerAddOrderAlgorithm getAlgorithm() {
    return addOrderAlgorithm;
  }
  
}
