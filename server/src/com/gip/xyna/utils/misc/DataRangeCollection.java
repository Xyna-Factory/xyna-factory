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
package com.gip.xyna.utils.misc;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;


/**
 * diese klasse pflegt eine menge von intervallen von longwerten, wobei eine teilmenge davon ausgezeichnet ist (z.b. zeitpunkte).
 * die bestimmung dieser teilmenge passiert über eine datasource, die im konstruktor übergegeben wird. 
 * es wird sichergestellt, dass die datasource nie zweimal den gleichen wert abfragt.
 * damit funktioniert diese klasse als eine art cache.
 * 
 * den ausgezeichneten werten können auch noch values zugeordnet werden (int). diese haben dann immer gültigkeit bis zum nächsten ausgezeichneten wert. 
 * 
 * 
 * beispiel für verwendung: siehe unittests.
 * 
 * eine möglichkeit für die verwendung ist z.b. eine art versionscache. wenn man wissen möchte, welche versionsnummern es gibt, und die anfrage lazy gemacht wird,
 * kann man sich hiermit merken, welche versions-ranges bereits abgefragt worden sind.
 */
public class DataRangeCollection {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(DataRangeCollection.class);
  
  //TODO performance: binary search statt linearer suche in den ranges verwenden (insbesondere, wenn die anzahl der ranges größer wird)

  public interface DataSource {

    /**
     * fügt alle datapoints zum set dazu, die zwischen start und end liegen
     */
    void addDataPoints(long start, long end, Set<Long> datapoints);

  }

  private static class Interval {

    private long start;
    private long end;
    private long[] dataPoints;
    /*
     * i-ter value ist der value für das i-te intervall zwischen datapoints.
     * 0-ter value ist von start <-> datapoints[0]-1
     * 1-ter value gilt von datapoints[0] <-> datapoints[1]-1
     * ...
     * n-ter value gilt von datapoints[n-1] <-> end
     * 
     * 
     * 0 => value ist noch nicht berechnet!
     */
    private int[] values;
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("{").append(start).append("-").append(end);
      if (dataPoints != null && dataPoints.length > 0) {
        sb.append(",dp=");
        sb.append(Arrays.toString(dataPoints));
      }
      if (values != null && values.length > 0) {
        sb.append(",vals=");
        sb.append(Arrays.toString(values));
      }
      sb.append("}");
      return sb.toString();
    }
  }


  /*
   * aufsteigend
   * [0] -> [start=16, datapoints=18,25, end=60]
   * [1] -> [start=88, datapoints=, end=88]
   * [2] -> [start=90, datapoints=100, end=100]
   */
  private final List<Interval> parts = new ArrayList<Interval>();
  private final DataSource source;


  public DataRangeCollection(DataSource source) {
    this.source = source;
  }


  /**
   * mittels der datasource werden datapoints im gegebenen interval hinzugefügt
   */  
  public void insertDataPoints(final long start, final long end) {
    if (start > end) {
      throw new IllegalArgumentException();
    }
    IndexResult ir = indexOf(start, end);// fixme wenn man von updateinterval kommt, wurde das ggf bereits berechnet
    if (ir.doSomething()) {
      if (ir.isInsertion()) {
        parts.add(ir.insertionIndex, handleNew(start, end));
      } else {
        handleExtensionOrMerge(start, end, ir.touched);
      }  
    }
    if (logger.isTraceEnabled()) {
      logger.trace(System.identityHashCode(this) + ".insertDPs(" + start + "-" + end +") => " + this);
    }
  }
  
  private Interval handleNew(final long start, final long end) {
    Interval pnew = new Interval();
    pnew.start = start;
    pnew.end = end;
    pnew.dataPoints = getDataPointsFromDataSource(start, end);
    pnew.values = new int[pnew.dataPoints.length + 1];
    return pnew;
  }

  private IndexResult indexOf(final long start, final long end) {
    List<Interval> touched = new ArrayList<Interval>();
    if (parts.size() == 0 ||
        parts.get(0).start -1 > end) {
      return IndexResult.newFirst();
    } else if (parts.get(parts.size() - 1).end + 1 < start) {
      return IndexResult.at(parts.size()); // new last
    }
    int insertionIndex = -1;
    for (int i = 0; i < parts.size(); i++) {
      Interval p = parts.get(i);
      if (start >= p.start && end <= p.end) {
        return IndexResult.nothingToDo();
      } else if ((p.start -1 >= start && p.start - 1 <= end) ||
                 (p.end + 1 >= start && p.end + 1 <= end)) {
        touched.add(p);
      } else if (p.start < start) {
        insertionIndex = i + 1;
      }
    }
    if (touched.size() > 0) {
      return IndexResult.touched(touched);
    } else {
      return IndexResult.at(insertionIndex);
    }
  }
  
  private static class IndexResult {
    int insertionIndex = -1;
    List<Interval> touched = null; // TODO List<Pair<Integer, Interval>> to carry the listIdx?
    static IndexResult newFirst() {
      IndexResult ir = new IndexResult();
      ir.insertionIndex = 0;
      return ir;
    }
    static IndexResult at(int position) {
      IndexResult ir = new IndexResult();
      ir.insertionIndex = position;
      return ir;
    }
    static IndexResult touched(List<Interval> touched) {
      IndexResult ir = new IndexResult();
      ir.touched = touched;
      return ir;
    }
    static IndexResult nothingToDo() {
      return new IndexResult();
    }
    boolean isInsertion() {
      return insertionIndex > -1;
    }
    boolean doSomething() {
      return insertionIndex > -1 || (touched != null && touched.size() > 0);
    }
  }
  
  
  private void handleExtensionOrMerge(long start, long end, List<Interval> touched) {
    if (touched.size() == 1) {
      handleExtension(start, end, touched.get(0));
    } else {
      handleMerge(start, end, touched);
    }
  }
  
  private void handleExtension(long start, long end, Interval interval) {
    handleExtension(start, end, interval, true, true);
  }
                               

  private void handleExtension(long start, long end, Interval interval, boolean extendStart, boolean extendEnd) {
    long[] datapoints = interval.dataPoints;
    int[] values = interval.values;
    if (extendStart & interval.start > start) {
      datapoints = merge(getDataPointsFromDataSource(start, interval.start - 1), datapoints);
      values = mergeValues(datapoints.length + 1, null, values);
      interval.start = start;
    }
    if (extendEnd & interval.end < end) {
      datapoints = merge(datapoints, getDataPointsFromDataSource(interval.end + 1, end));
      values = mergeValues(datapoints.length + 1, values, null);
      interval.end = end;
    }
    interval.dataPoints = datapoints;
    interval.values = values;
  }

  /*
   *     [touched.0]           [touched.1]           [touched.2]
   *                [interim.0]           [interim.1]
   *  |                                                           |
   *  v                                                           v
   *  start                                                      end
   *  
   *  start/end kann sich aber auch hier befinden:
   *                |         |
   *                v         v
   *                start     end
   */
  private void handleMerge(long start, long end, List<Interval> touched) {
    handleExtension(start, end, touched.get(0), true, false); //nach links extenden
    handleExtension(start, end, touched.get(touched.size() - 1), false, true); //nach rechts extenden
    List<Interval> interim = createInterim(touched); //zwischenstücke erzeugen
    Interval mergedInterval = merge(start, end, touched, interim);
    int insertionIndex = parts.indexOf(touched.get(0));
    for (int i = 0; i < touched.size(); i++) {
      parts.remove(insertionIndex);
    }
    parts.add(insertionIndex, mergedInterval);
  }


  private Interval merge(long start, long end, List<Interval> touched, List<Interval> interim) {
    assert(interim.size() == touched.size() -1);
    Interval mergeRoot = touched.get(0);
    //merge nach und nach jeweils ein interim mit dem nächsten touched interval. alles zusammen ergibt das große neue gemerg-te interval
    for (int i = 1; i < touched.size(); i++) {
      mergeRoot.dataPoints = merge(mergeRoot.dataPoints, interim.get(i - 1).dataPoints);
      mergeRoot.values = mergeValues(mergeRoot.dataPoints.length + 1, mergeRoot.values, null);
      mergeRoot.dataPoints = merge(mergeRoot.dataPoints, touched.get(i).dataPoints);
      /*
       *      dp1   dp2        dp3
       *       |     |          |
       *   ------------------------------------
       *      mergeroot    |   touched 
       *       |    |      |    |
       *  v[0] |v[1]| v[2] |v[0]|  v[1]
       *  
       *  v[2] muss mit v[0] gemergt werden
       */
      int[] rest = new int[touched.get(i).values.length - 1];
      System.arraycopy(touched.get(i).values, 1, rest, 0, rest.length);
      mergeRoot.values = mergeValues(mergeRoot.values.length + touched.get(i).values.length - 1, mergeRoot.values, rest);
      if (touched.get(i).values[0] > 0) {
        //ersten wert aus touched übernehmen
        mergeRoot.values[mergeRoot.values.length - touched.get(i).values.length] = touched.get(i).values[0];
      }
    }
    mergeRoot.end = touched.get(touched.size() - 1).end;
    return mergeRoot;
  }



  private List<Interval> createInterim(List<Interval> touched) {
    List<Interval> interim = new ArrayList<Interval>();
    Interval previous = null;
    for (Interval interval : touched) {
      if (previous != null) {
        interim.add(handleNew(previous.end + 1, interval.start - 1));
      }
      previous = interval;
    }
    return interim;
  }
  
  
  private int[] mergeValues(int length, int[]... values) {
    int[] ret = new int[length];
    int nullLength = length;
    for (int[] ia : values) {
      if (ia != null) {
        nullLength -= ia.length;
      }
    }
    int destPos = 0;
    for (int[] ia : values) {
      if (ia == null) {
        destPos += nullLength;
      } else if (ia.length > 0) {
        System.arraycopy(ia, 0, ret, destPos, ia.length);
        destPos += ia.length;
      }
    }
    return ret;
  }


  private long[] merge(long[]... arrs) {
    int s = 0;
    long[] only = null;
    int o = 0;
    for (long[] l : arrs) {
      s += l.length;
      if (l.length > 0) {
        only = l;
        o++;
      }
    }
    if (o == 1) {
      return only;
    }
    long[] ret = new long[s];
    int destPos = 0;
    for (long[] l : arrs) {
      int len = l.length;
      if (len > 0) {
        System.arraycopy(l, 0, ret, destPos, len);
        destPos += len;
      }
    }
    return ret;
  }


  private long[] getDataPointsFromDataSource(long start, long end) {
    Set<Long> changes = new TreeSet<Long>();
    source.addDataPoints(start, end, changes);
    long[] changesArr = new long[changes.size()];
    int idx = 0;
    for (Long l : changes) {
      changesArr[idx++] = l;
    }
    return changesArr;
  }


  /**
   * fügt zum set die existierenden datapoints zwischen den beiden übergebenen punkten hinzu
   * es wird nur unterstützt, dass start und end vollständig in einem existierenden intervall liegen.
   * @param start
   * @param end
   * @param existingpoints
   */
  public void collectExistingDataPoints(long start, long end, Set<Long> existingpoints) {
    if (start > end) {
      throw new IllegalArgumentException();
    }
    if (logger.isTraceEnabled()) {
      logger.trace(System.identityHashCode(this) + ".collectingPoints(" + start + "-" + end +")old=" + existingpoints + " " + this);
    }
    try {
    for (int i = 0; i < parts.size(); i++) {
      Interval p = parts.get(i);
      if (start > p.end + 1) {
        /*
         *    p      
         * [ ... ]   
         *          [xxx]
         */
        //next
      } else if (start >= p.start) {
        //ok, in diesem intervall anfangen          
        if (end <= p.end) {
          /*
           *    p    
           * [ ... ]   
           *   [x]
           */
          for (long dp : p.dataPoints) {
            if (dp >= start && dp <= end) {
              existingpoints.add(dp);
            }
          }
          return;
        }
      }
    }
    } finally {
      if (logger.isTraceEnabled()) {
        logger.trace(System.identityHashCode(this) + ".collected:" + existingpoints);
      }
    }
    throw new RuntimeException();
  }


  /**
   * falls noch kein intervall oder nur intervalle mit größeren werten existieren, wird ein intervall angelegt, welches nur diesen punkt enthält.
   * falls bereits ein intervall mit kleineren werten als der übergebene punkt existiert, wird dieses erweitert bis zum übergebenen punkt.
   * TODO motivieren, wieso das sinn macht 
   */
  public void updateInterval(long toDataPoint) {
    IndexResult indexOf = indexOf(toDataPoint, toDataPoint);
    if (!indexOf.doSomething()) {
      return;
    }
    if (indexOf.isInsertion()) {
      if (indexOf.insertionIndex == 0) {
        insertDataPoints(toDataPoint, toDataPoint);
      } else {
        insertDataPoints(parts.get(indexOf.insertionIndex - 1).end, toDataPoint);
      }
    } else {
      //touched -> merging wird von insert erkannt
      insertDataPoints(toDataPoint, toDataPoint);
    }
    if (logger.isTraceEnabled()) {
      logger.trace(System.identityHashCode(this) + ".updatedInterval(" + toDataPoint + "):" + this);
    }
  }


  /**
   * punkt muss in existierendem intervall liegen.
   */
  public void setValue(long dataPoint, int value) {
    for (int i = 0; i < parts.size(); i++) {
      Interval p = parts.get(i);
      if (dataPoint > p.end) {
        /*
         *    p      
         * [ ... ]   
         *          x
         */
        //next
      } else if (dataPoint < p.start) {
        throw new RuntimeException();
      } else {
        //toDataPoint liegt in interval -> ok
        int idx = Arrays.binarySearch(p.dataPoints, dataPoint);
        if (idx >= 0) {
          p.values[idx] = value;
        } else {
          p.values[-idx - 1] = value;
        }
        if (logger.isTraceEnabled()) {
          logger.trace(System.identityHashCode(this) + ".setValue(@" + dataPoint + "=" + value +")" + this);
        }
        return;
      }
    }
    throw new RuntimeException();
  }


  /**
   * punkt muss in existierendem intervall liegen.
   * 
   * nicht gesetzte values haben per default den wert 0.
   */
  public int getValue(long dataPoint) {
    for (int i = 0; i < parts.size(); i++) {
      Interval p = parts.get(i);
      if (dataPoint > p.end) {
        /*
         *    p      
         * [ ... ]   
         *          x
         */
        //next
      } else if (dataPoint < p.start) {
        throw new RuntimeException();
      } else {
        //toDataPoint liegt in interval -> ok
        int idx = Arrays.binarySearch(p.dataPoints, dataPoint);
        if (idx >= 0) {
          return p.values[idx + 1];
        } else {
          return p.values[-idx - 1];
        }
      }
    }
    throw new RuntimeException();
  }


  public boolean hasDataPoints(long start, long end) {
    if (start > end) {
      throw new IllegalArgumentException();
    }
    for (int i = 0; i < parts.size(); i++) {
      Interval p = parts.get(i);
      if (start > p.end + 1) {
        /*
         *    p      
         * [ ... ]   
         *          [xxx]
         */
        //next
      } else if (start >= p.start) {
        //ok, in diesem intervall      
        if (end <= p.end) {
          /*
           *    p      
           * [ ... ]  
           *   [x]
           */
          for (long dp : p.dataPoints) {
            if (dp >= start && dp <= end) {
              return true;
            }
          }
          return false;
        }
      }
    }
    throw new RuntimeException();
  }


  public String toString() {
    StringBuilder sb = new StringBuilder("DR:");
    for (int i = 0; i<parts.size(); i++) {
      if (i > 0) {
        sb.append("\n");
      }
      Interval iv = parts.get(i);
      sb.append(iv.toString());      
    }
    return sb.toString();
  }


  void check() {
    for (int i = 0; i<parts.size()-1; i++) {
      Interval p = parts.get(i);
      Interval next = parts.get(i+1);
      if (p.end>=next.start-1) {
        throw new RuntimeException();
      }
    }
  }

}
