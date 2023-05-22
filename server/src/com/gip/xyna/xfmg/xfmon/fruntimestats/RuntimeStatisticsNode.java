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
package com.gip.xyna.xfmg.xfmon.fruntimestats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownStatistic;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathHandlingHolder;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl.FilterPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;


public class RuntimeStatisticsNode {

  protected final Map<String, RuntimeStatisticsNode> children = new HashMap<String, RuntimeStatisticsNode>(1);
  @SuppressWarnings("rawtypes")
  private Statistics statistic;
  protected final String nodename;
  
  public RuntimeStatisticsNode(String nodename) {
    this.nodename = nodename;
  }
  
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void insertStatistic(int index, Statistics value) throws XFMG_StatisticAlreadyRegistered {
    if (isLeaf(index, value)) {
      if (statistic != null) {
        throw new XFMG_StatisticAlreadyRegistered(value.getPath().toString());
      }
      this.statistic = value;
    } else {
      String childName = value.getPath().getPathPart(index+1).getPartName();
      RuntimeStatisticsNode child = children.get(childName);
      if (child == null) {
        child = new RuntimeStatisticsNode(childName);
        children.put(childName, child);
      }
      child.insertStatistic(index+1, value);
    }
  }
  
  
  @SuppressWarnings("rawtypes")
  public Collection<Statistics> removeStatistic(int index, StatisticsPath path) throws XFMG_InvalidStatisticsPath {
    Collection<Statistics> removed = new ArrayList<Statistics>();
    switch (path.getPathPart(index + 1).getStatisticsNodeTraversal()) {
      case ALL :
      case ALL_OR_SELF :
      case ALL_AND_SELF :
        if (isLeaf(index + 1, path)) {
          for (RuntimeStatisticsNode child : children.values()) {
            if (child.statistic != null) {
              removed.add(child.statistic);
            }
          }
          children.clear();
        } else {
          for (RuntimeStatisticsNode child : children.values()) {
            removed.addAll(child.removeStatistic(index + 1, path));
          }
        }
        break;
      case MULTI :
      case MULTI_OR_SELF :
      case MULTI_AND_SELF :
        if (!(path.getPathPart(index) instanceof FilterPathPart)) {
          throw new XFMG_InvalidStatisticsPath(path.toString(), "Unknown filter instance");
        }
        FilterPathPart part = (FilterPathPart) path.getPathPart(index);
        if (isLeaf(index + 1, path)) {
          for (RuntimeStatisticsNode child : children.values()) {
            if (child.statistic != null) {
              removed.add(child.statistic);
            }
          }
          children.clear();
        } else {
          for (RuntimeStatisticsNode child : children.values()) {
            if (part.accept(child.nodename)) {
              removed.addAll(child.removeStatistic(index + 1, path));
            }
          }
        }
        break;
      case SINGLE :
        String childname = path.getPathPart(index + 1).getPartName();
        RuntimeStatisticsNode child = children.get(childname);
        if (child == null) {
          return Collections.emptyList();
        } else {
          if (isLeaf(index + 1, path)) {
            removed.add(child.statistic);
            children.remove(childname);
          } else {
            removed.addAll(child.removeStatistic(index + 1, path));
          }
        }
        break;
      default :
        throw new XFMG_InvalidStatisticsPath(path.toString(), "Invalid traversal: " + path.getPathPart(index + 1).toString());
    }
    List<String> nodesToRemove = new ArrayList<String>();
    for (RuntimeStatisticsNode child : children.values()) {
      if (child.children.size() == 0 && child.statistic == null) {
        nodesToRemove.add(child.nodename); // remove empty branches
      }
    }
    for (String childname : nodesToRemove) {
      children.remove(childname);
    }
    return removed;
  }
  
  
  public Statistics<?, ?> getStatistic(int index, StatisticsPath path) {
    if (isLeaf(index, path)) {
      return statistic;
    } else {
      String childname = path.getPathPart(index+1).getPartName();
      RuntimeStatisticsNode child = children.get(childname);
      if (child == null) {
        return null;
      } else {
        return child.getStatistic(index+1, path);  
      }
    }
  }
  
  
  public <I extends StatisticsValue<?>, O extends StatisticsValue<?>> Collection<O> aggregate(StatisticsAggregator<I, O> aggregation) throws XFMG_InvalidStatisticsPath, XFMG_UnknownStatistic {
    Collection<I> inputs;
    StatisticsAggregator<?, I> nextAggregator = aggregation.getNextAggregationPart();
    boolean atLeastOneValid = false;
    XFMG_UnknownStatistic lastException = null;
    if (nextAggregator != null && !isRecursiveCallOnLeaf(aggregation)) {
      switch (nextAggregator.getPathpart().getStatisticsNodeTraversal()) {
        case SINGLE :
          RuntimeStatisticsNode nextNode = getChildOrThrow(nextAggregator.getPathpart().getPartName());
          inputs = nextNode.aggregate(nextAggregator);
          break;
        case ALL_OR_SELF :
          if (children.values().size() == 0) {
            return mapLocalStatistic(aggregation);
          } // else fall through
        case ALL :
        case ALL_AND_SELF :
          inputs = new ArrayList<I>();
          for (RuntimeStatisticsNode child : children.values()) {
            try {
              inputs.addAll(child.aggregate(nextAggregator));
              atLeastOneValid = true;
            } catch (XFMG_UnknownStatistic e) {
              if (!(nextAggregator.getPathpart() instanceof UnknownPathHandlingHolder) ||
                  ((UnknownPathHandlingHolder)nextAggregator.getPathpart()).getUnknownPathHandling() == UnknownPathOnTraversalHandling.THROW_IF_ANY) {
                throw e;
              } else {
                lastException = e;
              }
            }
          }
          if (!atLeastOneValid && lastException != null &&
              !(((UnknownPathHandlingHolder)nextAggregator.getPathpart()).getUnknownPathHandling() == UnknownPathOnTraversalHandling.IGNORE)) {
            throw lastException;
          }
          break;
        case MULTI_OR_SELF :
          if (children.values().size() == 0) {
            return mapLocalStatistic(aggregation);
          } // else fall through
        case MULTI :
        case MULTI_AND_SELF :
          if (!(nextAggregator.getPathpart() instanceof FilterPathPart)) {
            throw new XFMG_InvalidStatisticsPath(nextAggregator.getPathpart().toString(), "Unknown filter instance");
          }
          FilterPathPart filter = (FilterPathPart) nextAggregator.getPathpart();
          inputs = new ArrayList<I>();
          for (RuntimeStatisticsNode child : children.values()) {
            if (filter.accept(child.nodename)) {
              try {
                inputs.addAll(child.aggregate(nextAggregator));
                atLeastOneValid = true;
              } catch (XFMG_UnknownStatistic e) {
                if (!(nextAggregator.getPathpart() instanceof UnknownPathHandlingHolder) ||
                    ((UnknownPathHandlingHolder)nextAggregator.getPathpart()).getUnknownPathHandling() == UnknownPathOnTraversalHandling.THROW_IF_ANY) {
                  throw e;
                } else {
                  lastException = e;
                }
              }
            }
          }
          if (!atLeastOneValid && lastException != null &&
              !(((UnknownPathHandlingHolder)nextAggregator.getPathpart()).getUnknownPathHandling() == UnknownPathOnTraversalHandling.IGNORE)) {
            throw lastException;
          }
          break;
        default :
          throw new XFMG_InvalidStatisticsPath(nextAggregator.getPathpart().toString(), "Invalid traversal: " + nextAggregator.getPathpart().getStatisticsNodeTraversal().toString());
      }
      inputs = nextAggregator.reduce(inputs);
      Collection<O> outs = new ArrayList<O>(inputs.size());
      for (I input : inputs) {
        outs.add(aggregation.map(input, nodename));
      }
      switch (nextAggregator.getPathpart().getStatisticsNodeTraversal()) {
        case MULTI_AND_SELF:
        case ALL_AND_SELF:
          outs.addAll(mapLocalStatistic(aggregation));
      }
      return outs;
    } else {
      return mapLocalStatistic(aggregation);
    }
  }
 
  
  @SuppressWarnings("unchecked")
  private <I extends StatisticsValue<?>, O extends StatisticsValue<?>> Collection<O> mapLocalStatistic(StatisticsAggregator<I, O> aggregation) {
    if (statistic == null) {
      return Collections.emptyList();
    } else {
      O output = aggregation.map((I) statistic.getValueObject(), nodename);
      if (output == null) {
        return Collections.emptyList();
      } else {
        Collection<O> outs = new ArrayList<O>();
        outs.add(output);
        return outs;
      }
    }
  }
  
  
  @SuppressWarnings("rawtypes")
  private boolean isRecursiveCallOnLeaf(StatisticsAggregator aggregator) {
    return aggregator.getNextAggregationPart() == aggregator && children.size() == 0;
  }
  
  
  protected RuntimeStatisticsNode getChildOrThrow(String childname) throws XFMG_UnknownStatistic { 
    RuntimeStatisticsNode child = children.get(childname);
    if (child == null) {
      throw new XFMG_UnknownStatistic(childname) {

        private static final long serialVersionUID = 1L;

        @Override
        public synchronized Throwable fillInStackTrace() { 
          return this; //performance. der stacktrace hilft eh nicht
        }
        
      };
    } else {
      return child;
    }
  }
  
  
  private static boolean isLeaf(int currentIndex, Statistics<?, StatisticsValue<?>> value) {
    return isLeaf(currentIndex, value.getPath());
  }
  
  private static boolean isLeaf(int currentIndex, StatisticsPath path) {
    return (currentIndex + 1) >= path.getPath().size();
  }
  
  public static RuntimeStatisticsNode createRootNode() {
    return new RootNode();
  }
  
  private static class RootNode extends RuntimeStatisticsNode {
    
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public RootNode() {
      super("ROOT");
    }
    
    public <I extends StatisticsValue<?>, O extends StatisticsValue<?>> java.util.Collection<O> aggregate(StatisticsAggregator<I,O> aggregation) throws XFMG_InvalidStatisticsPath, XFMG_UnknownStatistic {
      lock.readLock().lock();
      try {
        Collection<O> inputs;
        switch (aggregation.getPathpart().getStatisticsNodeTraversal()) {
          case SINGLE :
            RuntimeStatisticsNode nextNode = getChildOrThrow(aggregation.getPathpart().getPartName());
            inputs = nextNode.aggregate(aggregation);
            break;
          case ALL :
          case ALL_OR_SELF :
          case ALL_AND_SELF :
            inputs = new ArrayList<O>();
            for (RuntimeStatisticsNode child : children.values()) {
              inputs.addAll(child.aggregate(aggregation));
            }
            break;
          case MULTI :
            if (!(aggregation.getPathpart() instanceof FilterPathPart)) {
              throw new XFMG_InvalidStatisticsPath(aggregation.getPathpart().toString(), "Unknown filter instance");
            }
            FilterPathPart filter = (FilterPathPart) aggregation.getPathpart();
            inputs = new ArrayList<O>();
            for (RuntimeStatisticsNode child : children.values()) {
              if (filter.accept(child.nodename)) {
                inputs.addAll(child.aggregate(aggregation));
              }
            }
            break;
          default :
            throw new XFMG_InvalidStatisticsPath(aggregation.getPathpart().toString(), "Invalid traversal: " + aggregation.getPathpart().getStatisticsNodeTraversal().toString());
        }
        return aggregation.reduce(inputs);
      } finally {
        lock.readLock().unlock();
      }
    };
    
    @SuppressWarnings("rawtypes")
    public void insertStatistic(int index, Statistics value) throws XFMG_StatisticAlreadyRegistered {
      lock.writeLock().lock();
      try {
        String childName = value.getPath().getPathPart(index).getPartName();
        if (!children.containsKey(childName)) {
          children.put(childName, new RuntimeStatisticsNode(childName));
        }
        children.get(childName).insertStatistic(index, value);
      } finally {
        lock.writeLock().unlock();
      }
    };
    
    @SuppressWarnings("rawtypes")
    public Collection<Statistics> removeStatistic(int index, StatisticsPath path) throws XFMG_InvalidStatisticsPath {
      lock.writeLock().lock();
      try {
        String childname = path.getPathPart(index).getPartName();
        RuntimeStatisticsNode child = children.get(childname);
        if (child != null) {
          return child.removeStatistic(index, path);
        } else {
          return Collections.emptyList();
        }
      } finally {
        lock.writeLock().unlock();
      }
    };
    
    public Statistics<?, ?> getStatistic(int index, StatisticsPath path) {
      lock.readLock().lock();
      try {
        RuntimeStatisticsNode child = children.get(path.getPathPart(index).getPartName());
        if (child == null) {
          return null;
        } else {
          return child.getStatistic(index, path);
        }
      } finally {
        lock.readLock().unlock();
      }
    };
  }
  
}
