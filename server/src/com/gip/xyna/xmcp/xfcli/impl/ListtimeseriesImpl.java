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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listtimeseries;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeries;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.StorageParameter;



public class ListtimeseriesImpl extends XynaCommandImplementation<Listtimeseries> {

  public void execute(OutputStream statusOutputStream, Listtimeseries payload) throws XynaException {
    StringBuilder sb = new StringBuilder();
    List<TimeSeries> timeseries =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore().getTimeSeriesManagement().listTimeSeries();
    Collections.sort(timeseries, new Comparator<TimeSeries>() {

      @Override
      public int compare(TimeSeries o1, TimeSeries o2) {
        return Long.compare(o1.getId(), o2.getId());
      }

    });
    TimeSeriesTableFormatter formatter = new TimeSeriesTableFormatter(timeseries);
    formatter.writeTableHeader(sb);
    formatter.writeTableRows(sb);
    writeLineToCommandLine(statusOutputStream, sb.toString());
  }


  private static class TimeSeriesTableFormatter extends TableFormatter {

    private final List<List<String>> rows;

    private static final List<String> header;
    static {
      header = new ArrayList<>();
      header.add("id");
      header.add("name");
      header.add("dstype");
      header.add("start");
      header.add("end");
      header.add("storagetype");
      header.add("id");
      header.add("details");
    }


    public TimeSeriesTableFormatter(List<TimeSeries> timeseries) {
      SimpleDateFormat sdf = Constants.defaultUTCSimpleDateFormat();
      rows = new ArrayList<>();
      for (TimeSeries t : timeseries) {
        List<String> line = new ArrayList<>();
        line.add(String.valueOf(t.getId()));
        TimeSeriesCreationParameter definition = t.getDefinition();
        line.add(definition.datasourceParameter.getDataSourceName());
        line.add(definition.datasourceParameter.getDataSourceType().name());
        line.add(sdf.format(new Date(t.getMetaData().starttimeMS)));
        line.add(sdf.format(new Date(t.getMetaData().endtimeMS)));
        for (int i = 0; i<definition.storageParameter.length; i++) {
          StorageParameter sp = definition.storageParameter[i];
          List<String> llocal = new ArrayList<>(line);
          llocal.add(sp.getClass().getSimpleName());
          llocal.add(t.getStorageId(i));
          llocal.add(sp.toString());
          rows.add(llocal);
        }
      }
    }


    @Override
    public List<List<String>> getRows() {
      return rows;
    }


    @Override
    public List<String> getHeader() {
      return header;
    }

  }

}
