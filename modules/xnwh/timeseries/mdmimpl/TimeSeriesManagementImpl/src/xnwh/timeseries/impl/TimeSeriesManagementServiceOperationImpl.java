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
package xnwh.timeseries.impl;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.statistics.timeseries.AggregationType;
import com.gip.xyna.xnwh.statistics.timeseries.StorageTypeRRD4J.StorageParameterRRD4J;
import com.gip.xyna.xnwh.statistics.timeseries.StoredMetaData;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceParameter;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceType;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.StorageParameter;
import com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate;
import com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate;

import base.date.format.YyyyMMDdTHHMmSs;
import xnwh.timeseries.DataAggregationType;
import xnwh.timeseries.Datasource;
import xnwh.timeseries.FetchedData;
import xnwh.timeseries.Interval;
import xnwh.timeseries.StorageDefinition;
import xnwh.timeseries.TimeSeriesDataPoint;
import xnwh.timeseries.TimeSeriesDataType;
import xnwh.timeseries.TimeSeriesDefinition;
import xnwh.timeseries.TimeSeriesId;
import xnwh.timeseries.TimeSeriesManagementServiceOperation;
import xnwh.timeseries.TimeSeriesMetaData;
import xnwh.timeseries.aggregation.Average;
import xnwh.timeseries.aggregation.First;
import xnwh.timeseries.aggregation.Last;
import xnwh.timeseries.aggregation.Maximum;
import xnwh.timeseries.aggregation.Minimum;
import xnwh.timeseries.datatype.Counter;
import xnwh.timeseries.datatype.Gauge;
import xnwh.timeseries.interval.IntervalStartDuration;
import xnwh.timeseries.interval.IntervalStartEnd;
import xnwh.timeseries.storage.StorageDefinitionRRD;



public class TimeSeriesManagementServiceOperationImpl implements ExtendedDeploymentTask, TimeSeriesManagementServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public void addDataToTimeSeries(TimeSeriesId timeSeriesId, TimeSeriesDataPoint timeSeriesDataPoint) {
    try {
      XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore().getTimeSeriesManagement().getTimeSeries(timeSeriesId.getId())
          .addData(timeSeriesDataPoint.getDate().toMillis(), timeSeriesDataPoint.getValue());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Time series with id " + timeSeriesId.getId() + " unknown.", e);
    }
  }


  public TimeSeriesId createTimeSeries(TimeSeriesDefinition timeSeriesDefinition) {

    int l = timeSeriesDefinition.getStorageDefinitions().size();
    StorageParameter[] storageParameter = new StorageParameter[l];
    for (int i = 0; i < l; i++) {
      if (timeSeriesDefinition.getStorageDefinitions().get(i) instanceof StorageDefinitionRRD) {
        StorageDefinitionRRD sd = (StorageDefinitionRRD) timeSeriesDefinition.getStorageDefinitions().get(i);
        storageParameter[i] = new StorageParameterRRD4J(getAggregationType(sd.getAggregation()), sd.getRingBufferLength().toMillis() / 1000,
                                                        sd.getPrecision().toMillis() / 1000);
      } else {
        throw new RuntimeException("unsupported type " + timeSeriesDefinition.getStorageDefinitions().get(i).getClass().getSimpleName());
      }
    }
    DataSourceParameter datasourceParameter = new DataSourceParameter(timeSeriesDefinition.getName());
    datasourceParameter.setDataSourceType(getDataSourceType(timeSeriesDefinition.getDatasource().getTimeSeriesDataType()));
    Double max = timeSeriesDefinition.getDatasource().getMaxValue();
    if (max != null) {
      datasourceParameter.setMaxValue(max);
    }
    Double min = timeSeriesDefinition.getDatasource().getMinValue();
    if (min != null) {
      datasourceParameter.setMinValue(min);
    }
    TimeSeriesCreationParameter parameter = new TimeSeriesCreationParameter(storageParameter, datasourceParameter);
    long id;
    try {
      id = XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore().getTimeSeriesManagement().createTimeSeries(parameter);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return new TimeSeriesId(id);
  }


  private DataSourceType getDataSourceType(TimeSeriesDataType t) {
    if (t instanceof Counter) {
      return DataSourceType.COUNTER;
    } else if (t instanceof Gauge) {
      return DataSourceType.GAUGE;
    } else {
      throw new RuntimeException("unsupported datasource type: " + t.getClass().getName());
    }
  }


  private AggregationType getAggregationType(DataAggregationType t) {
    if (t instanceof Average) {
      return AggregationType.AVERAGE;
    } else if (t instanceof Maximum) {
      return AggregationType.MAX;
    } else if (t instanceof Minimum) {
      return AggregationType.MIN;
    } else if (t instanceof Last) {
      return AggregationType.LAST;
    } else if (t instanceof First) {
      return AggregationType.FIRST;
    } else {
      throw new RuntimeException("unsupported aggregation type: " + t.getClass().getName());
    }
  }


  public void deleteTimeSeries(TimeSeriesId timeSeriesId) {
    try {
      XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore().getTimeSeriesManagement()
          .removeTimeSeries(timeSeriesId.getId());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public FetchedData fetchDataOfTimeSeries(TimeSeriesId timeSeriesId, Interval interval, RelativeDate resolution,
                                           DataAggregationType aggregation) {
    FetchedData.Builder fdb = new FetchedData.Builder();
    long[] intervalAsMillis = getInterval(interval);
    try {
      com.gip.xyna.xnwh.statistics.timeseries.FetchedData data = XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore()
          .getTimeSeriesManagement().getTimeSeries(timeSeriesId.getId())
          .getData(intervalAsMillis[0], intervalAsMillis[1], false, resolution.toMillis(), getAggregationType(aggregation));
      //new AbsoluteDate(new SimpleDateFormat(new YyyyMMDdTHHMmSs().getFormat()).format(new Date(data.startTimeMS)), new YyyyMMDdTHHMmSs())
      AbsoluteDate ad = new AbsoluteDate(null, new YyyyMMDdTHHMmSs());
      ad.fromMillis(data.startTimeMS);
      fdb.startTime(ad);
      fdb.resolution(new RelativeDate(data.resolutionMS + " ms"));
      List<Double> list = new ArrayList<>(data.values.length);
      for (double d : data.values) {
        list.add(d);
      }
      fdb.values(list);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Time series with id " + timeSeriesId.getId() + " unknown.", e);
    }

    return fdb.instance();
  }


  private long[] getInterval(Interval interval) {
    if (interval instanceof IntervalStartEnd) {
      IntervalStartEnd i = (IntervalStartEnd) interval;
      return new long[] {i.getStart().toMillis(), i.getEnd().toMillis()};
    } else if (interval instanceof IntervalStartDuration) {
      IntervalStartDuration i = (IntervalStartDuration) interval;
      return new long[] {i.getStart().toMillis(), i.getDuration().toMillis() + i.getStart().toMillis()};
    } else {
      throw new RuntimeException("unsupported interval type: " + interval.getClass().getName());
    }
  }


  public TimeSeriesDefinition getTimeSeriesDefinition(TimeSeriesId timeSeriesId) {
    try {
      TimeSeriesCreationParameter definition = XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore()
          .getTimeSeriesManagement().getTimeSeries(timeSeriesId.getId()).getDefinition();
      TimeSeriesDefinition.Builder tdb = new TimeSeriesDefinition.Builder();
      tdb.name(definition.datasourceParameter.getDataSourceName());

      Datasource.Builder dsb = new Datasource.Builder();
      dsb.maxValue(definition.datasourceParameter.getMaxValue());
      dsb.minValue(definition.datasourceParameter.getMinValue());
      dsb.timeSeriesDataType(getTimeSeriesDataType(definition.datasourceParameter.getDataSourceType()));
      tdb.datasource(dsb.instance());

      List<StorageDefinition> storageDefinitions = new ArrayList<>(definition.storageParameter.length);
      for (StorageParameter sp : definition.storageParameter) {
        if (sp instanceof StorageParameterRRD4J) {
          StorageParameterRRD4J spr = (StorageParameterRRD4J) sp;
          storageDefinitions.add(new StorageDefinitionRRD(new RelativeDate(spr.getLengthInSeconds() + " s"),
                                                          new RelativeDate(spr.getBucketSizeInSeconds() + " s"),
                                                          getAggregationType(spr.getAggregation())));
        } else {
          throw new RuntimeException("unknown storage type: " + sp.getClass().getSimpleName());
        }
      }
      tdb.storageDefinitions(storageDefinitions);
      return tdb.instance();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Time series with id " + timeSeriesId.getId() + " unknown.", e);
    }
  }


  private DataAggregationType getAggregationType(AggregationType aggregation) {
    switch (aggregation) {
      case AVERAGE :
        return new Average();
      case FIRST :
        return new First();
      case LAST :
        return new Last();
      case MAX :
        return new Maximum();
      case MIN :
        return new Minimum();
      default :
        throw new RuntimeException();
    }
  }


  private TimeSeriesDataType getTimeSeriesDataType(DataSourceType dataSourceType) {
    switch (dataSourceType) {
      case COUNTER :
        return new Counter();
      case GAUGE :
        return new Gauge();
      default :
        throw new RuntimeException();
    }
  }


  public TimeSeriesMetaData getTimeSeriesMetaData(TimeSeriesId timeSeriesId) {
    try {
      StoredMetaData metaData = XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore().getTimeSeriesManagement()
          .getTimeSeries(timeSeriesId.getId()).getMetaData();
      AbsoluteDate startTime = new AbsoluteDate(null, new YyyyMMDdTHHMmSs());
      startTime.fromMillis(metaData.starttimeMS);
      AbsoluteDate endTime = new AbsoluteDate(null, new YyyyMMDdTHHMmSs());;
      endTime.fromMillis(metaData.endtimeMS);
      return new TimeSeriesMetaData(startTime, endTime);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Time series with id " + timeSeriesId.getId() + " unknown.", e);
    }
  }


}
