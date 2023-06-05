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
package com.gip.xyna.update;

import com.gip.xyna.update.utils.StorableUpdater;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;


public class UpdateTriggerAndFilterInstanceState extends UpdateJustVersion{
  
  public UpdateTriggerAndFilterInstanceState(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }
  
  @Override
  public void update() throws XynaException {
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_5_1.TriggerInstanceStorable.class,
                           TriggerInstanceStorable.class,
                           new TransformTriggerInstance(),
                           ODSConnectionType.DEFAULT);

    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_5_1.FilterInstanceStorable.class,
                           FilterInstanceStorable.class,
                           new TransformFilterInstance(),
                           ODSConnectionType.DEFAULT);
  }
  

  private static class TransformTriggerInstance implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_5_1.TriggerInstanceStorable, TriggerInstanceStorable> {
    
    public TriggerInstanceStorable transform(com.gip.xyna.update.outdatedclasses_5_1_5_1.TriggerInstanceStorable from) {
      TriggerInstanceStorable to = new TriggerInstanceStorable(from.getTriggerInstanceName(), from.getRevision());
      to.setDescription(from.getDescription());
      to.setStartParameter(from.getStartParameter());
      to.setTriggerName(from.getTriggerName());
      
      TriggerInstanceState state;
      if (from.isEnabled()) {
        state = TriggerInstanceState.ENABLED;
      } else if (from.isDisabledautomatically()){
        state = TriggerInstanceState.ERROR;
      } else {
        state = TriggerInstanceState.DISABLED;
      }
      
      to.setState(state);
      return to;
    }
  }

  private static class TransformFilterInstance implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_5_1.FilterInstanceStorable, FilterInstanceStorable> {
    
    public FilterInstanceStorable transform(com.gip.xyna.update.outdatedclasses_5_1_5_1.FilterInstanceStorable from) {
      FilterInstanceStorable to = new FilterInstanceStorable(from.getFilterInstanceName(), from.getRevision());
      to.setFilterName(from.getFilterName());
      to.setTriggerInstanceName(from.getTriggerInstanceName());
      to.setDescription(from.getDescription());
      
      FilterInstanceState state;
      if (from.isEnabled()) {
        state = FilterInstanceState.ENABLED;
      } else if (from.isDisabledautomatically()){
        state = FilterInstanceState.ERROR;
      } else {
        state = FilterInstanceState.DISABLED;
      }
      
      to.setState(state);
      to.setOptional(false);
      
      return to;
    }
  }

}
