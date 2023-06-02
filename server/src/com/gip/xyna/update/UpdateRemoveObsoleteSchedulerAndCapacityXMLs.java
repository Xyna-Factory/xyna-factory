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

import java.io.File;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;


public class UpdateRemoveObsoleteSchedulerAndCapacityXMLs extends Update {

  private final Version afterUpdate;
  private final Version allowedForUpdate;


  public UpdateRemoveObsoleteSchedulerAndCapacityXMLs(final Version allowedForUpdate, final Version afterUpdate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
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
    return false;
  }

  @Override
  protected void update() throws XynaException {

    String path = "com" + Constants.fileSeparator + "gip" + Constants.fileSeparator + "xyna"
                    + Constants.fileSeparator + "3" + Constants.fileSeparator + "0" + Constants.fileSeparator + "XMDM"
                    + Constants.fileSeparator + "xprc" + Constants.fileSeparator;
    
    VersionDependentPath vdp = VersionDependentPath.getCurrent();
    String savedPath = vdp.getPath(PathType.XMOM, false) + Constants.fileSeparator + path;
    String deployedPath = vdp.getPath(PathType.XMOM, true) + Constants.fileSeparator +  path;
    File savedCapacityFile = new File(savedPath + "Capacity.xml");
    File savedSchedulerBeanFile = new File(savedPath + "SchedulerBean.xml");
    File deployedCapacityFile = new File(deployedPath + "Capacity.xml");
    File deployedSchedulerBeanFile = new File(deployedPath + "SchedulerBean.xml");

    if (savedCapacityFile.exists()) {
      logger.debug("Deleting obsolete MDM file: '" + savedCapacityFile.getAbsolutePath() + "'.");
      savedCapacityFile.delete();
    } else {
      logger.debug("Obsolete file '" + savedCapacityFile.getAbsolutePath()
                      + "' does not exist and does not need to be deleted.");
    }

    if (savedSchedulerBeanFile.exists()) {
      logger.debug("Deleting obsolete MDM file: '" + savedSchedulerBeanFile.getAbsolutePath() + "'.");
      savedSchedulerBeanFile.delete();
    } else {
      logger.debug("Obsolete file '" + savedSchedulerBeanFile.getAbsolutePath()
                      + "' does not exist and does not need to be deleted.");
    }

    if (deployedCapacityFile.exists()) {
      logger.warn("Deleting obsolete MDM file: '" + deployedCapacityFile.getAbsolutePath()
                      + "'. Services and workflows that need this file need to be updated.");
      deployedCapacityFile.delete();
    } else {
      logger.debug("Obsolete file '" + deployedCapacityFile.getAbsolutePath()
                      + "' does not exist and does not need to be deleted.");
    }

    if (deployedSchedulerBeanFile.exists()) {
      logger.warn("Deleting obsolete MDM file: '" + deployedSchedulerBeanFile.getAbsolutePath()
                      + "'. Services and workflows that need this file need to be updated.");
      deployedSchedulerBeanFile.delete();
    } else {
      logger.debug("Obsolete file '" + deployedSchedulerBeanFile.getAbsolutePath()
                      + "' does not exist and does not need to be deleted.");
    }

  }

}
