/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.xypilot.impl.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.xypilot.metrics.Metric;
import xmcp.xypilot.metrics.SelectedMetric;


public class MetricSubtypesMap {

  private static Logger _logger = Logger.getLogger(MetricSubtypesMap.class);
  
  private Map<String, Metric> _map = new TreeMap<>();
  
  public MetricSubtypesMap(XynaOrderServerExtension order) {
    List<? extends Metric> list = Metric.getMetricInstances(order);
    for (Metric metric : list) {
      _map.put(metric.getClass().getName(), metric);
    }    
  }
    
  public List<SelectedMetric> adapt(String csv) {
    List<SelectedMetric> ret = new ArrayList<>();    
    _logger.trace("Got metric csv: " + csv);
    Set<String> set = new HashSet<>();
    if (csv != null) {
      String[] parts = csv.split(";");
      for (String val : parts) {
        val = val.trim();        
        if (val.length() > 0) { set.add(val); }
      }
    }
    for (Entry<String, Metric> entry : _map.entrySet()) {    
      SelectedMetric sm = new SelectedMetric();
      sm.setMetric(entry.getValue().clone());
      sm.setSelected(set.contains(entry.getKey()));
      _logger.trace("Set selected metric " + sm.getMetric().getClass().getName() + ": " + sm.getSelected());
      ret.add(sm);          
    }
    return ret;
  }
  
  public String adapt(List<? extends SelectedMetric> list) {
    if (list == null) { return ""; }
    StringBuilder ret = new StringBuilder("");
    for (SelectedMetric smetric : list) {
      if (smetric == null) { continue; }
      if (smetric.getMetric() == null) { continue; }
      _logger.trace("Got selected metric " + smetric.getMetric().getClass().getName() + ": " + smetric.getSelected());
      if (!smetric.getSelected()) { continue; }      
      ret.append(smetric.getMetric().getClass().getName()).append(";");
    }
    _logger.trace("Build csv for metric list: " + ret.toString());
    return ret.toString();
  }
  
}
