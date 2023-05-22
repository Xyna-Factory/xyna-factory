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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper.DiscoveryStatisticsValue;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Liststatistics;



public class ListstatisticsImpl extends XynaCommandImplementation<Liststatistics> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ListstatisticsImpl.class);
  private final static FactoryRuntimeStatistics xynaStatistics = XynaFactory.getInstance().getFactoryManagement()
      .getXynaFactoryMonitoring().getFactoryRuntimeStatistics();


  public void execute(OutputStream statusOutputStream, Liststatistics payload) throws XynaException {
    Collection<DiscoveryStatisticsValue> aggregatedValue = xynaStatistics.getAggregatedValue(AggregationStatisticsFactory.getDiscoveryPartialAggregator(payload.getPath()));
    for (DiscoveryStatisticsValue dsv : aggregatedValue) {
      if (payload.getVerbose()) {
        writeLineToCommandLine(statusOutputStream,  dsv.getValue().getFirst(), " = ",  dsv.getValue().getSecond().getValue());
      } else {
        writeLineToCommandLine(statusOutputStream,  dsv.getValue().getFirst());
      }
    }
  }

}
