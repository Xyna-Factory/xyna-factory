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

package com.gip.xyna.xprc;

import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xpce.ResponseListenerWithOrderDeathSupport;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;


/**
 * 
 * @deprecated nur noch zum Deserialsieren von CountDownLatchResponseListenerWithSuspensionSupport verwenden!
 */
@Deprecated
public abstract class ResponseListenerWithSuspensionSupport extends ResponseListener implements ResponseListenerWithOrderDeathSupport {

  private static final long serialVersionUID = -3318714466081799132L;

  public abstract void onSuspended(ProcessSuspendedException e);
  
  public abstract void onOrderAbortion(ProcessAbortedException e);
  
  // FIXME we might want to move this into the ResponseListener, but all those cases we want to handle (spread the exception upwards) should come from these 
  public abstract void onOrderDeath(OrderDeathException e);

}
