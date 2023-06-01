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
package com.gip.xyna.xnwh.statistics.timeseries;



import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;

import com.gip.xyna.xnwh.statistics.timeseries.AggregationType;
import com.gip.xyna.xnwh.statistics.timeseries.FetchedData;
import com.gip.xyna.xnwh.statistics.timeseries.StorageTypeRRD4J;
import com.gip.xyna.xnwh.statistics.timeseries.StoredMetaData;
import com.gip.xyna.xnwh.statistics.timeseries.StorageTypeRRD4J.RRD4JConfiguration;
import com.gip.xyna.xnwh.statistics.timeseries.StorageTypeRRD4J.StorageParameterRRD4J;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceParameter;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceType;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.StorageParameter;

import junit.framework.TestCase;



public class StorageTypeRRD4JTest extends TestCase {


  private RRD4JConfiguration getConf(final String prefix) {
    return new RRD4JConfiguration() {

      private int id = 0;


      @Override
      public String getPathPrefixForRRDDBs() {
        return "rrds";
      }


      @Override
      public int getHeartBeatAsFactorOfStepSize() {
        return 3;
      }


      @Override
      public String createNewUniqueId() {
        return prefix + "_" + id++;
      }


      @Override
      public long getCacheExpirationTime() {
        return 1000;
      }


      @Override
      public long getCacheSyncInterval() {
        return 600;
      }


      @Override
      public int getMaximumNumberOfOpenFiles() {
        return 10;
      }
    };
  }


  public void test1() {
    StorageTypeRRD4J rrd = new StorageTypeRRD4J(getConf("test1"));
    try {
      AggregationType agg = AggregationType.AVERAGE;
      StorageParameter[] parameters = new StorageParameter[] {new StorageParameterRRD4J(agg, 100, 5)};
      DataSourceParameter datasourceParameter = new DataSourceParameter("test1");
      datasourceParameter.setDataSourceType(DataSourceType.GAUGE);
      String id = rrd.create(parameters, datasourceParameter);

      File f = new File("rrds/test1_0.rrd");
      try (RrdDb rrddb = new RrdDb(f.getPath())) {
        assertEquals(1, rrddb.getArchive(0).getSteps());
        assertEquals(20, rrddb.getArchive(0).getRows());
        assertEquals(5, rrddb.getRrdDef().getStep());
        assertEquals(15, rrddb.getDatasource(0).getHeartbeat());
        assertEquals(DsType.GAUGE, rrddb.getDatasource(0).getType());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      /*
       * intervalle sind [6, 10], [11, 15], usw
       */

      try {
        long t = (System.currentTimeMillis() + 20000) / 5000 * 5000; //an 5-sek interval angleichen
        System.out.println(t);
        long s = 1000;
        assertTrue(f.exists());
        rrd.addData(id, t - 5 * s, 1);
        rrd.addData(id, t + 0 * s, 2);
        rrd.addData(id, t + 5 * s, 3);
        rrd.addData(id, t + 10 * s, 4);
        rrd.addData(id, t + 16 * s, 5.2);
        rrd.addData(id, t + 20 * s, 6);
        rrd.addData(id, t + 25 * s, 7);
        long res = 1 * s;
        long start = t - 10 * s;
        long end = t + 30 * s;
        FetchedData fetched = rrd.getData(id, start, end, false, res, agg);
        double[] vals = fetched.values;
        assertEquals(start / 5000 * 5000, fetched.startTimeMS);
        assertEquals(5000, fetched.resolutionMS);
        double[] expectedVals = new double[] {Double.NaN, Double.NaN, 2, 3, 4, 5.2, 5.84, 7, Double.NaN};
        assertEquals(Arrays.toString(vals), Arrays.toString(expectedVals));
      } finally {
        rrd.delete(id);
        assertFalse(f.exists());
      }
    } finally {
      rrd.shutdown();
    }
  }


  public void test2() {
    /* beispiel aus der doku: http://oss.oetiker.ch/rrdtool/tut/rrdtutorial.en.html
     *       in the RRD                 in reality
    
    time+000:   0 delta="U"   time+000:    0 delta="U"
    time+300: 300 delta=300   time+300:  300 delta=300
    time+600: 600 delta=300   time+603:  603 delta=303
    time+900: 900 delta=300   time+900:  900 delta=297
    
    Let's create two identical databases. I've chosen the time range 920805000 to 920805900 as this goes very well with the example numbers.
    
    rrdtool create seconds1.rrd   \
      --start 920804700          \
      DS:seconds:COUNTER:600:U:U \
      RRA:AVERAGE:0.5:1:24
     */
    StorageTypeRRD4J rrd = new StorageTypeRRD4J(getConf("test2"));
    try {
      AggregationType agg = AggregationType.AVERAGE;
      StorageParameter[] parameters = new StorageParameter[] {new StorageParameterRRD4J(agg, 24 * 300, 1 * 300)};
      DataSourceParameter datasourceParameter = new DataSourceParameter("test2");
      datasourceParameter.setDataSourceType(DataSourceType.COUNTER);
      String id = rrd.create(parameters, datasourceParameter);

      File f = new File("rrds/test2_0.rrd");
      try (RrdDb rrddb = new RrdDb(f.getPath())) {
        assertEquals(1, rrddb.getArchive(0).getSteps());
        assertEquals(24, rrddb.getArchive(0).getRows());
        assertEquals(300, rrddb.getRrdDef().getStep());
        assertEquals(3 * 300, rrddb.getDatasource(0).getHeartbeat());
        assertEquals(DsType.COUNTER, rrddb.getDatasource(0).getType());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      try {
        long t = System.currentTimeMillis() + 20000;
        System.out.println(t);
        long s = 1000;
        assertTrue(f.exists());
        rrd.addData(id, t + 0 * s, 0);
        rrd.addData(id, t + 300 * s, 300);
        rrd.addData(id, t + 603 * s, 603);
        rrd.addData(id, t + 900 * s, 900);
        long res = 1 * s;
        long start = t - 3 * s;
        long end = t + 900 * s;
        FetchedData fetched = rrd.getData(id, start, end, false, res, agg);
        double[] vals = fetched.values;
        assertEquals(300 * s, fetched.resolutionMS);
        double[] expectedVals = new double[] {Double.NaN, 1, 1, 1, Double.NaN};
        assertEquals(Arrays.toString(vals), Arrays.toString(expectedVals));
      } finally {
        rrd.delete(id);
        assertFalse(f.exists());
      }
    } finally {
      rrd.shutdown();
    }
  }


  public void testGetMetaData() {
    StorageTypeRRD4J rrd = new StorageTypeRRD4J(getConf("test3"));
    try {
      AggregationType agg = AggregationType.AVERAGE;
      StorageParameter[] parameters = new StorageParameter[] {new StorageParameterRRD4J(agg, 24 * 300, 1 * 300)};
      DataSourceParameter datasourceParameter = new DataSourceParameter("test3");
      datasourceParameter.setDataSourceType(DataSourceType.COUNTER);
      String id = rrd.create(parameters, datasourceParameter);
      try {
        long t = System.currentTimeMillis();
        rrd.addData(id, t, 100);
        StoredMetaData smd = rrd.getMetaData(id);
        assertTrue(smd.starttimeMS % 300000 == 0);
        assertTrue(smd.endtimeMS < System.currentTimeMillis() + 300000);
      } finally {
        rrd.delete(id);
      }
    } finally {
      rrd.shutdown();
    }
  }


  public void testGetParameter() {
    StorageTypeRRD4J rrd = new StorageTypeRRD4J(getConf("test4"));
    try {
      AggregationType agg = AggregationType.AVERAGE;
      StorageParameter[] parameters = new StorageParameter[] {new StorageParameterRRD4J(agg, 24 * 300, 1 * 300)};
      DataSourceParameter datasourceParameter = new DataSourceParameter("test4");
      datasourceParameter.setDataSourceType(DataSourceType.COUNTER);
      String id = rrd.create(parameters, datasourceParameter);
      try {
        StorageParameter[] parameterRead = rrd.getParameter(id);
        assertEquals(1, parameterRead.length);
        assertEquals(StorageParameterRRD4J.class, parameterRead[0].getClass());
        assertEquals(AggregationType.AVERAGE, ((StorageParameterRRD4J) parameterRead[0]).getAggregation());
        assertEquals(24 * 300, ((StorageParameterRRD4J) parameterRead[0]).getLengthInSeconds());
        assertEquals(1 * 300, ((StorageParameterRRD4J) parameterRead[0]).getBucketSizeInSeconds());
      } finally {
        rrd.delete(id);
      }
    } finally {
      rrd.shutdown();
    }
  }


}
