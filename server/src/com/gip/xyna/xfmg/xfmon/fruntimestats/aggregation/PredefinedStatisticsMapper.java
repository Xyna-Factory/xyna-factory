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
package com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;

/**
 * Some type unsafe predefined {@link StatisticsMapper}. Offer a typeCast via 
 * {@link PredefinedStatisticsMapper}.typeCast(Class&lt;I&gt; clazzIn, Class&lt;O&gt; clazzOut)
 */
public enum PredefinedStatisticsMapper implements StatisticsMapper<StatisticsValue<?>, StatisticsValue<?>> {

  /**
   * Returns the input as is with no manipulation or cloning
   */
  DIRECT {
    @Override
    public StatisticsValue<?> map(StatisticsValue<?> in, String nodename) {
      return in;
    }
  },
  /**
   * If the input is not an instance of {@link DiscoveryStatisticsValue} it will be mapped into one containing both
   * the name of the current node and it's {@link StatisticsValue}. If it encounters a {@link DiscoveryStatisticsValue}
   * it will append the current node's name as first element to the already aggregated path.
   */
  DISCOVERY {
    @Override
    public StatisticsValue<?> map(StatisticsValue<?> in, String nodename) {
      if (in instanceof DiscoveryStatisticsValue) {
        ((DiscoveryStatisticsValue)in).prefixPath(nodename);
        return in;
      } else {
        if (in == null) {
          return null;
        } else {
          return new DiscoveryStatisticsValue(new String[] {nodename}, in.deepClone());
        }
      }
    }
  }, 
  /**
   * Similar to DISCOVERY but maps into a {@link MapDiscoveryStatisticsValue} that will merge several values into a single Map
   */
  MAP_DISCOVERY {
    @Override
    public StatisticsValue<?> map(StatisticsValue<?> in, String nodename) {
      if (in instanceof MapDiscoveryStatisticsValue) {
        ((MapDiscoveryStatisticsValue)in).prefixPath(nodename);
        return in;
      } else {
        if (in == null) {
          return null;
        } else {
          return new MapDiscoveryStatisticsValue(nodename, in.deepClone());
        }
      }
    }
  };

  
  public abstract StatisticsValue<?> map(StatisticsValue<?> in, String nodename);
  
  
  
  public <I extends StatisticsValue<?>, O extends StatisticsValue<?>> StatisticsMapper<I, O> typeCast(Class<I> clazzIn, Class<O> clazzOut) {
    return (StatisticsMapper<I, O>) this;
  }
  
  
  public static class DiscoveryStatisticsValue implements StatisticsValue<SerializablePair<String, StatisticsValue>> {

    private static final long serialVersionUID = 667542346621458735L;
    private final SerializablePair<String, StatisticsValue> value;
    
    public DiscoveryStatisticsValue(String[] path, StatisticsValue value) {
      this(StatisticsPathImpl.pathToString(path), value);
    }
    
    public DiscoveryStatisticsValue(String path, StatisticsValue value) {
      this.value = new SerializablePair<String, StatisticsValue>(path, value);
    }
    
    public SerializablePair<String, StatisticsValue> getValue() {
      return value;
    }
    
    public void prefixPath(String pathpart) {
      value.setFirst(prefixStringArray(pathpart, value.getFirst()));
    }
    
    public StatisticsValue<SerializablePair<String, StatisticsValue>> deepClone() {
      return new DiscoveryStatisticsValue(value.getFirst(), value.getSecond().deepClone());
    }

    public void merge(StatisticsValue<SerializablePair<String, StatisticsValue>> otherValue) {
      value.getSecond().merge(otherValue.getValue().getSecond());
    }
    
    
    private static String prefixStringArray(String prefix, String path) {
      return StatisticsPathImpl.escapePathPart(prefix) + "." + path;
    }
    
  }
  
  
  public static class MapDiscoveryStatisticsValue implements StatisticsValue<HashMap<StatisticsPath, StatisticsValue>> {

    private static final long serialVersionUID = -4420140246527837636L;
    private HashMap<StatisticsPath, StatisticsValue> discoveryMap;
    
    public HashMap<StatisticsPath, StatisticsValue> getValue() {
      return discoveryMap;
    }
    
    public MapDiscoveryStatisticsValue(HashMap<StatisticsPath, StatisticsValue> discoveryMap) {
      this.discoveryMap = discoveryMap;
    }
    
    public MapDiscoveryStatisticsValue(String firstPathPart, StatisticsValue primitiveValue) {
      discoveryMap = new HashMap<StatisticsPath, StatisticsValue>();
      discoveryMap.put(StatisticsPathImpl.fromString(firstPathPart), primitiveValue);
    }

    public void merge(StatisticsValue<HashMap<StatisticsPath, StatisticsValue>> otherValue) {
      for (Entry<StatisticsPath, StatisticsValue> otherEntry : otherValue.getValue().entrySet()) {
        StatisticsValue previousValue = discoveryMap.get(otherEntry.getKey());
        if (previousValue == null) {
          discoveryMap.put(otherEntry.getKey(), otherEntry.getValue());
        } else {
          previousValue.merge(otherEntry.getValue());
        }
      }
    }

    public StatisticsValue<HashMap<StatisticsPath, StatisticsValue>> deepClone() {
      HashMap<StatisticsPath, StatisticsValue> clonedMap = new HashMap<StatisticsPath, StatisticsValue>();
      for (Entry<StatisticsPath, StatisticsValue>  discoveryEntry : discoveryMap.entrySet()) {
        clonedMap.put(new StatisticsPathImpl(new ArrayList<StatisticsPathPart>(discoveryEntry.getKey().getPath())), discoveryEntry.getValue().deepClone());
      }
      return new MapDiscoveryStatisticsValue(clonedMap);
    }

    
    public void prefixPath(String pathpart) {
      HashMap<StatisticsPath, StatisticsValue> prefixedMap = new HashMap<StatisticsPath, StatisticsValue>();
      for (Entry<StatisticsPath, StatisticsValue>  discoveryEntry : discoveryMap.entrySet()) {
        List<StatisticsPathPart> prefixedPath = new ArrayList<StatisticsPathPart>();
        prefixedPath.add(StatisticsPathImpl.simplePathPart(pathpart));
        prefixedPath.addAll(discoveryEntry.getKey().getPath());
        prefixedMap.put(new StatisticsPathImpl(prefixedPath), discoveryEntry.getValue());
      }
      discoveryMap = prefixedMap;
    }

    
  }
}
