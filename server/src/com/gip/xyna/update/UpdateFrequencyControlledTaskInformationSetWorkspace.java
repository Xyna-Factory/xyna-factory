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
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;


public class UpdateFrequencyControlledTaskInformationSetWorkspace extends UpdateJustVersion{

  public UpdateFrequencyControlledTaskInformationSetWorkspace(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }
  
  @Override
  protected void update() throws XynaException {
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_6_0_1_6.FrequencyControlledTaskInformation.class,
                           FrequencyControlledTaskInformation.class,
                           new TransformFqctrlTask(),
                           ODSConnectionType.HISTORY);
  }
  
  
  private static class TransformFqctrlTask implements Transformation<com.gip.xyna.update.outdatedclasses_6_0_1_6.FrequencyControlledTaskInformation, FrequencyControlledTaskInformation> {
    
    public FrequencyControlledTaskInformation transform(com.gip.xyna.update.outdatedclasses_6_0_1_6.FrequencyControlledTaskInformation from) {
      FrequencyControlledTaskInformation to = new FrequencyControlledTaskInformation();
      to.setTaskId(from.getTaskId());
      to.setTaskLabel(from.getTasklabel());
      to.setEventCount(from.getEventcount());
      to.setFinishedEvents(from.getFinishedevents());
      to.setFailedEvents(from.getFailedevents());
      to.setEventCreationInfo(from.getEventcreationinfo());
      to.setStatus(from.getTaskstatus());
      to.setMaxEvents(from.getMaxevents());
      to.setStatistics(from.getStatistics());
      to.setStarttime(from.getStarttime());
      to.setStoptime(from.getStoptime());
      to.setWorkspacename(RevisionManagement.DEFAULT_WORKSPACE.getName());
      
      return to;
    }
  }
  
  
}
