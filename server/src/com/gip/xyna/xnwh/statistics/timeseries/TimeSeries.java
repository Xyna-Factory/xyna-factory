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
package com.gip.xyna.xnwh.statistics.timeseries;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceParameter;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceType;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.StorageParameter;



public class TimeSeries {

  private final TimeSeriesStorable ts;
  private final TimeSeriesStorageStorable[] tss;
  private final TimeSeriesManagement tsm;


  public TimeSeries(TimeSeriesManagement tsm, TimeSeriesStorable ts, TimeSeriesStorageStorable[] tss) {
    this.ts = ts;
    this.tss = tss;
    this.tsm = tsm;
  }


  public void addData(long timestampMS, Number value) {
    for (TimeSeriesStorageStorable t : tss) {
      tsm.types.get(tsm.typesByName.get(t.getStorageType())).addData(t.getStorageId(), timestampMS, value);
    }
  }


  /**
   * liefert daten zwischen start- und endzeit bzgl der übergebenen resolution und aggregation.
   * 
   * falls konvertierung nicht unterstützt, kann es sein, dass sowohl start- und endzeit, als auch resolution im result anders sind.
   * 
   * @param resolutionMS gewünschte größe der buckets in millisekunden, bzgl der die daten zurückgegeben werden. bsp: 900000 =&gt; jeder zurückgegebene wert entspricht einer aggregation über 15 minuten.
   * @param aggregationType falls werte mit unterschiedlichen aggregationtypes gespeichert sind, ist dies ein filter auf die vorhandenen aggregationstypen.
   */
  public FetchedData getData(long starttimeMS, long endtimeMS, boolean calculateDiffs, long resolutionMS,
                             AggregationType aggregationType) {
    for (TimeSeriesStorageStorable t : tss) {
      //TODO wie merged man daten verschiedener typen?
      return tsm.types.get(tsm.typesByName.get(t.getStorageType())).getData(t.getStorageId(), starttimeMS, endtimeMS, calculateDiffs,
                                                                            resolutionMS, aggregationType);
    }
    return new FetchedData(new double[0], -1, -1);
  }


  public StoredMetaData getMetaData() {
    for (TimeSeriesStorageStorable t : tss) {
      //TODO wie merged man daten verschiedener typen?
      return tsm.types.get(tsm.typesByName.get(t.getStorageType())).getMetaData(t.getStorageId());
    }
    return new StoredMetaData(-1, -1);
  }


  public TimeSeriesCreationParameter getDefinition() {
    DataSourceParameter datasourceParameter = new DataSourceParameter(ts.getDatasourceName());
    datasourceParameter.setDataSourceType(DataSourceType.valueOf(ts.getDatasourceType()));
    datasourceParameter.setMinValue(ts.getMinvalue());
    datasourceParameter.setMaxValue(ts.getMaxvalue());
    List<StorageParameter> storageParameter = new ArrayList<>();
    for (TimeSeriesStorageStorable t : tss) {
      for (StorageParameter sp : tsm.types.get(tsm.typesByName.get(t.getStorageType())).getParameter(t.getStorageId())) {
        storageParameter.add(sp);
      }
    }
    return new TimeSeriesCreationParameter(storageParameter.toArray(new StorageParameter[0]), datasourceParameter);
  }


  public long getId() {
    return ts.getId();
  }


  public String getStorageId(int storageIdx) {
    return tss[storageIdx].getStorageId();
  }

}
