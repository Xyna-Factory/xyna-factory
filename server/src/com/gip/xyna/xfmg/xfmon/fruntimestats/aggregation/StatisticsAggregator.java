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
package com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation;

import java.util.Collection;

import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;

/**
 * An AggregationStack is built from the TopDown with the first {@link StatisticsAggregator} being the one that will
 * be executed last. The generic parameter {@link I} specifies the aggregation input while {@link O} specifies the
 * aggregation output. When setting the next aggregation step via {@link StatisticsAggregator}.setNextAggregationPart the 
 * output has to match the input.
 * During AggregationStack traversal the {@link StatisticsMapper} will be called once for every encountered value
 * and afterwards the {@link StatisticsReducer} will be invoked for all mapped results. 
 * It is possible to define type unsafe recursive aggregations by setting the next part to the current aggregator,
 * so defined aggregations can not be registered as they would lead to endless recursions.
 * There are some type unsafe default Mappers ({@link PredefinedStatisticsMapper}) and Reducers
 * ({@link PredefinedStatisticsReducer}) that can be type cast (with {@link PredefinedStatisticsReducer}.typeCast).
 * In most cases only the lowest levels of an AggregationStack will have to be defined, such stacks can be completed
 * with {@link AggregationStatisticsFactory}.completeAggregationStack.
 */
public class StatisticsAggregator<I extends StatisticsValue<?>, O extends StatisticsValue<?>> {

  private final StatisticsPathPart pathpart;
  private StatisticsAggregator<?, I> nextPart;
  private final StatisticsMapper<I, O> mapper;
  private final StatisticsReducer<O> reducer;
  
  
  public StatisticsAggregator(StatisticsPathPart pathpart, StatisticsMapper<I, O> mapper, StatisticsReducer<O> reducer) {
    this.pathpart = pathpart;
    if (mapper == null) {
      this.mapper = (StatisticsMapper<I, O>) PredefinedStatisticsMapper.DIRECT;
    } else {
      this.mapper = mapper;
    }
    if (reducer == null) {
      this.reducer = (StatisticsReducer<O>) PredefinedStatisticsReducer.NO_REDUCTION;
    } else {
      this.reducer = reducer;
    }
  }
  
  
  public O map (I in, String nodename) {
    return mapper.map(in, nodename);
  }
  
  public Collection<O> reduce(Collection<O> in) {
    return reducer.reduce(in);
  }
  
  public void setNextAggregationPart(StatisticsAggregator<? extends StatisticsValue<?>, I> nextPart) {
    this.nextPart = nextPart;
  }
  
  void forceSetNextPart(StatisticsAggregator nextPart) {
    this.nextPart = nextPart;
  }
  
  
  public StatisticsAggregator<?, I> getNextAggregationPart() {
    return nextPart;
  }
  
  
  public StatisticsPathPart getPathpart() {
    return pathpart;
  }

  
}
