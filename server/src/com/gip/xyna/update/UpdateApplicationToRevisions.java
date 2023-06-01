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

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.update.utils.StorableUpdater;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationStorable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;


public class UpdateApplicationToRevisions extends UpdateJustVersion {

  private static List<Application> updated = new ArrayList<Application>();
  
  public UpdateApplicationToRevisions(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }

  @Override
  protected void update() throws XynaException {
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationStorable.class,
                           ApplicationStorable.class,
                           new TransformApplication(),
                           ODSConnectionType.HISTORY);
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationEntryStorable.class,
                           ApplicationEntryStorable.class,
                           new TransformApplicationEntry(),
                           ODSConnectionType.HISTORY);
  }
  
  private static class TransformApplication implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationStorable, ApplicationStorable> {
    
    public ApplicationStorable transform(com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationStorable from) {
      ApplicationStorable to = new ApplicationStorable();
      to.setId(from.getId());
      to.setName(from.getName());
      to.setVersion(from.getVersion());
      to.setState(from.getState());
      to.setComment(from.getComment());
      if(from.getStateAsEnum() == ApplicationState.WORKINGCOPY) {
        to.setParentRevision(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        updated.add(new Application(to.getName(), to.getVersion()));
      }
      
      return to;
    }
  }

  private static class TransformApplicationEntry implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationEntryStorable, ApplicationEntryStorable> {
    
    public ApplicationEntryStorable transform(com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationEntryStorable from) {
      ApplicationEntryStorable to = new ApplicationEntryStorable();
      to.setId(from.getId());
      to.setApplication(from.getApplication());
      to.setVersion(from.getVersion());
      to.setType(from.getType());
      to.setName(from.getName());
      if(updated.contains(new Application(from.getApplication(), from.getVersion()))) {
        to.setParentRevision(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      }
      
      return to;
    }
  }
}
