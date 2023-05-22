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

package com.gip.xyna.xprc.xpce.planning;



import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;



public class PlanningDispatcher extends XynaDispatcher {

  public static final String DEFAULT_NAME = "PlanningDispatcher";


  public PlanningDispatcher() throws XynaException {
    super(DEFAULT_NAME);
    for( Map.Entry<DestinationKey, DestinationValue[]> entry :  allDestinations.entrySet() ) {
      setDestination( entry.getKey(), entry.getValue()[INDEX_PLANNING], true );
    }
  }


  @Override
  public void dispatch(XynaOrderServerExtension xo) throws XynaException {
    if (xo.getDestinationKey().isCompensate())
      return;
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getPlanningProcessor()
                    .process(getDestination(xo.getDestinationKey()), xo);
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void registerStatisticsIfNecessary(DestinationKey dk) {
    // at the moment there is nothing to be done for planning
  }

}
