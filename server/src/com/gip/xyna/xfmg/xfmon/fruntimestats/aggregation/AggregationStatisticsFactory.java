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
package com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation;


import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper.DiscoveryStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.AggregatedStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;


/**
 * Offers helpful methods for the creation of {@link AggregatedStatistics} and {@link StatisticsAggregator}
 */
public class AggregationStatisticsFactory {

  
  public static <I extends StatisticsValue<?>, O extends StatisticsValue<?>> AggregatedStatistics<?, O> generateDefaultAggregationStatistics(StatisticsPath ownPath, StatisticsPath pathToAggregate) {
    return new AggregatedStatistics(ownPath, generateDefaultAggregatorForPath(pathToAggregate));
  }
  
  
  public static <I extends StatisticsValue<?>, O extends StatisticsValue<?>> StatisticsAggregator<I, O> 
                generateDefaultAggregatorForPath(StatisticsPath path) {
    return generateDefaultAggregatorForPath(path, false);
  }
  
  
  private static <I extends StatisticsValue<?>, O extends StatisticsValue<?>> StatisticsAggregator<I, O> 
                 generateDefaultAggregatorForPath(StatisticsPath path, boolean forceNoReduction) {
    StatisticsAggregator<I, O> root = createDefaultAggregationPart(path.getPathPart(0), forceNoReduction);
    StatisticsAggregator<I, O> current = root;
    for (int i = 1; i < path.getPath().size(); i++) {
      StatisticsAggregator<I, O> next = createDefaultAggregationPart(path.getPathPart(i), forceNoReduction);
      current.forceSetNextPart(next);
      current = next;
    }
    return root;
  }
  
  
  private static <I extends StatisticsValue<?>, O extends StatisticsValue<?>> StatisticsAggregator<I, O> createDefaultAggregationPart(StatisticsPathPart part, boolean forceNoReduction) {
    StatisticsReducer<StatisticsValue<?>> reducer;
    if (part.getStatisticsNodeTraversal() == StatisticsNodeTraversal.SINGLE || forceNoReduction) {
      reducer = PredefinedStatisticsReducer.NO_REDUCTION;
    } else {
      reducer = PredefinedStatisticsReducer.DEFAULT;
    }
    return new StatisticsAggregator(part, PredefinedStatisticsMapper.DIRECT, reducer);
  }
  
  
  public static <I extends StatisticsValue<?>, O extends StatisticsValue<?>> AggregatedStatistics<?, O> 
                generateAggregationStatistics(StatisticsPath ownPath, StatisticsPath pathToAggregationStack, StatisticsAggregator<I, O> incompleteAggregationStack) {
    // TODO ensure simplePath? ownPath & pathToAggregationStack
    return new AggregatedStatistics(ownPath, completeAggregationStack(pathToAggregationStack, incompleteAggregationStack));
  }
  
  
  /**
   * Appends a default AggregationStack (created from pathToAggregationStack) to the top of the given stack (incompleteAggregationStack)
   */
  public static <I extends StatisticsValue<?>, O extends StatisticsValue<?>> StatisticsAggregator<O, O> 
                completeAggregationStack(StatisticsPath pathToAggregationStack, StatisticsAggregator<I, O> incompleteAggregationStack) {
    return completeAggregationStack(pathToAggregationStack, incompleteAggregationStack, false);
  }
  
  
  public static <I extends StatisticsValue<?>, O extends StatisticsValue<?>> StatisticsAggregator<O, O> 
                completeAggregationStack(StatisticsPath pathToAggregationStack, StatisticsAggregator<I, O> incompleteAggregationStack, boolean forceNoReduction) {
    // TODO ensure simplePath? pathToAggregationStack
    StatisticsAggregator completion = generateDefaultAggregatorForPath(pathToAggregationStack, forceNoReduction);
    StatisticsAggregator lastAggregation = completion;
    while (lastAggregation.getNextAggregationPart() != null) {
      lastAggregation = lastAggregation.getNextAggregationPart();
    }
    lastAggregation.setNextAggregationPart(incompleteAggregationStack);
    return completion;
  }
  
  
  public static StatisticsAggregator<? extends StatisticsValue<?>, DiscoveryStatisticsValue> getDiscoveryAggregator() {
    StatisticsAggregator<? extends StatisticsValue<?>, DiscoveryStatisticsValue> discovery = 
      new StatisticsAggregator<StatisticsValue<?>, DiscoveryStatisticsValue>(StatisticsPathImpl.ALL_AND_SELF,
                      (StatisticsMapper<StatisticsValue<?>, DiscoveryStatisticsValue>) 
                      PredefinedStatisticsMapper.DISCOVERY.typeCast(getGeneralizedStatisticsValueClass(), DiscoveryStatisticsValue.class),
                      PredefinedStatisticsReducer.NO_REDUCTION.typeCast(DiscoveryStatisticsValue.class));
    
    discovery.forceSetNextPart(discovery);
    return (StatisticsAggregator<? extends StatisticsValue<?>, DiscoveryStatisticsValue>) discovery;
  }
  

  public static StatisticsAggregator<? extends StatisticsValue<?>, DiscoveryStatisticsValue> getDiscoveryPartialAggregator(String path) {
    if (path == null) {
      return getDiscoveryAggregator();
    }
    StatisticsPath fromEscapedString = StatisticsPathImpl.fromEscapedString(path);
    StatisticsAggregator previous = null;
    StatisticsAggregator first = null;
    for (int i = 0; i < fromEscapedString.length(); i++) {
      StatisticsPathPart part = fromEscapedString.getPath().get(i);
      StatisticsAggregator sa = new StatisticsAggregator<StatisticsValue<?>, DiscoveryStatisticsValue>(part,
                                                                                    (StatisticsMapper<StatisticsValue<?>, DiscoveryStatisticsValue>) PredefinedStatisticsMapper.DISCOVERY
                                                                                        .typeCast(getGeneralizedStatisticsValueClass(),
                                                                                                  DiscoveryStatisticsValue.class),
                                                                                    PredefinedStatisticsReducer.NO_REDUCTION
                                                                                        .typeCast(DiscoveryStatisticsValue.class));

      if (previous != null) {
        previous.setNextAggregationPart(sa);
      }
      previous = sa;
      if (first == null) {
        first = sa;
      }
    }
    if (previous != null) {
      previous.setNextAggregationPart(getDiscoveryAggregator());
    }
    return first;
  }
  
  
  // ant-compiler would not accept StatisticsAggregator<? extends StatisticsValue, DiscoveryStatisticsValue> as result of getDiscoveryAggregator()
  // Changes to StatisticsValue<?> required PredefinedStatisticsMapper.DISCOVERY.typeCast to receive Class<? extends StatisticsValue<?>> as input
  // this method helps with getting the appropriate Class<? extends StatisticsValue<?>> needed for type cast
  public static Class<? extends StatisticsValue<?>> getGeneralizedStatisticsValueClass() {
    try {
      return (Class<? extends StatisticsValue<?>>)Class.forName(StatisticsValue.class.getName());
    } catch (ClassNotFoundException e) {
      // hardly possible as we just accessed it
      throw new RuntimeException("",e);
    }
  }
  
  
}
