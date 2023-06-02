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
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDomDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMExceptionDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMOperationDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMServiceGroupDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMWorkflowDatabaseEntry;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;


/**
 * XMOMDatabaseEntry hat neue Spalten: id und revision, insbesondere hat sich der primarykey geändert.
 */
public class UpdateXMOMDatabaseToRevision extends Update{

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerate;
  
  UpdateXMOMDatabaseToRevision(Version allowedForUpdate, Version afterUpdate, boolean mustRegenerate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerate = mustRegenerate;
  }
  
  @Override
  protected void update() throws XynaException {
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMDomDatabaseEntry.class,
                           XMOMDomDatabaseEntry.class,
                           new TransformXMOMDomDatabaseEntry(),
                           ODSConnectionType.HISTORY);
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMExceptionDatabaseEntry.class,
                           XMOMExceptionDatabaseEntry.class,
                           new TransformXMOMExceptionDatabaseEntry(),
                           ODSConnectionType.HISTORY);
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMOperationDatabaseEntry.class,
                           XMOMOperationDatabaseEntry.class,
                           new TransformXMOMOperationDatabaseEntry(),
                           ODSConnectionType.HISTORY);
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMWorkflowDatabaseEntry.class,
                           XMOMWorkflowDatabaseEntry.class,
                           new TransformXMOMWorkflowDatabaseEntry(),
                           ODSConnectionType.HISTORY);
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMServiceGroupDatabaseEntry.class,
                           XMOMServiceGroupDatabaseEntry.class,
                           new TransformXMOMServiceGroupDatabaseEntry(),
                           ODSConnectionType.HISTORY);
  }

  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedForUpdate;
  }

  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return afterUpdate;
  }

  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustRegenerate;
  }

  
  private static class TransformXMOMDomDatabaseEntry implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMDomDatabaseEntry, XMOMDomDatabaseEntry> {

    public XMOMDomDatabaseEntry transform(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMDomDatabaseEntry from) {
      XMOMDomDatabaseEntry to = new XMOMDomDatabaseEntry(from.getFqname(), RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      to.setLabel(from.getLabel());
      to.setPath(from.getPath());
      to.setName(from.getName());
      to.setDocumentation(from.getDocumentation());
      to.setMetadata(from.getMetadata());
      to.setFactorycomponent(from.getFactorycomponent());
      to.setExtends(from.getExtends());
      to.setExtendedBy(from.getExtendedBy());
      to.setPossesses(from.getPossesses());
      to.setPossessedBy(from.getPossessedBy());
      to.setNeededBy(from.getNeededBy());
      to.setProducedBy(from.getProducedBy());
      to.setInstancesUsedBy(from.getInstancesUsedBy());
      to.setWraps(from.getWraps());
      
      return to;
    }
  }

  private static class TransformXMOMExceptionDatabaseEntry implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMExceptionDatabaseEntry, XMOMExceptionDatabaseEntry> {
    
    public XMOMExceptionDatabaseEntry transform(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMExceptionDatabaseEntry from) {
      XMOMExceptionDatabaseEntry to = new XMOMExceptionDatabaseEntry(from.getFqname(), RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      to.setLabel(from.getLabel());
      to.setPath(from.getPath());
      to.setName(from.getName());
      to.setDocumentation(from.getDocumentation());
      to.setMetadata(from.getMetadata());
      to.setFactorycomponent(from.getFactorycomponent());
      to.setExtends(from.getExtends());
      to.setExtendedBy(from.getExtendedBy());
      to.setPossesses(from.getPossesses());
      to.setPossessedBy(from.getPossessedBy());
      to.setNeededBy(from.getNeededBy());
      to.setProducedBy(from.getProducedBy());
      to.setInstancesUsedBy(from.getInstancesUsedBy());
      to.setThrownBy(from.getThrownBy());
      
      return to;
    }
  }

  private static class TransformXMOMOperationDatabaseEntry implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMOperationDatabaseEntry, XMOMOperationDatabaseEntry> {
    
    public XMOMOperationDatabaseEntry transform(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMOperationDatabaseEntry from) {
      XMOMOperationDatabaseEntry to = new XMOMOperationDatabaseEntry(from.getFqname(), RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      to.setLabel(from.getLabel());
      to.setPath(from.getPath());
      to.setName(from.getName());
      to.setDocumentation(from.getDocumentation());
      to.setMetadata(from.getMetadata());
      to.setFactorycomponent(from.getFactorycomponent());
      to.setGroupedBy(from.getGroupedBy());
      to.setNeeds(from.getNeeds());
      to.setProduces(from.getProduces());
      to.setExceptions(from.getExceptions());
      to.setCalledBy(from.getCalledBy());
      to.setUsesInstancesOf(from.getUsesInstancesOf());
      
      return to;
    }
  }

  private static class TransformXMOMWorkflowDatabaseEntry implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMWorkflowDatabaseEntry, XMOMWorkflowDatabaseEntry> {
    
    public XMOMWorkflowDatabaseEntry transform(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMWorkflowDatabaseEntry from) {
      XMOMWorkflowDatabaseEntry to = new XMOMWorkflowDatabaseEntry(from.getFqname(), RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      to.setLabel(from.getLabel());
      to.setPath(from.getPath());
      to.setName(from.getName());
      to.setDocumentation(from.getDocumentation());
      to.setMetadata(from.getMetadata());
      to.setFactorycomponent(from.getFactorycomponent());
      to.setGroupedBy(from.getGroupedBy());
      to.setNeeds(from.getNeeds());
      to.setProduces(from.getProduces());
      to.setExceptions(from.getExceptions());
      to.setCalledBy(from.getCalledBy());
      to.setUsesInstancesOf(from.getUsesInstancesOf());
      to.setCalls(from.getCalls());
      
      return to;
    }
  }

  private static class TransformXMOMServiceGroupDatabaseEntry implements Transformation<com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMServiceGroupDatabaseEntry, XMOMServiceGroupDatabaseEntry> {
    
    public XMOMServiceGroupDatabaseEntry transform(com.gip.xyna.update.outdatedclasses_5_1_4_5.XMOMServiceGroupDatabaseEntry from) {
      XMOMServiceGroupDatabaseEntry to = new XMOMServiceGroupDatabaseEntry(from.getFqname(), RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      to.setLabel(from.getLabel());
      to.setPath(from.getPath());
      to.setName(from.getName());
      to.setDocumentation(from.getDocumentation());
      to.setMetadata(from.getMetadata());
      to.setFactorycomponent(from.getFactorycomponent());
      to.setGroups(from.getGroups());
      to.setWrappedBy(from.getWrappedBy());
      
      return to;
    }
  }
}
