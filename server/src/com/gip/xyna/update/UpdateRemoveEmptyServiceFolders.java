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
package com.gip.xyna.update;

import java.io.File;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;



/**
 * entfernt die durch bugz 14362 entstandenen leeren service-verzeichnisse im workingset. 
 * in den application-verzeichnissen ist das nicht notwendig
 */
public class UpdateRemoveEmptyServiceFolders extends Update {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateRemoveEmptyServiceFolders.class);

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerate;


  UpdateRemoveEmptyServiceFolders(Version allowedForUpdate, Version afterUpdate, boolean mustRegenerate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerate = mustRegenerate;
  }


  @Override
  protected void update() throws XynaException {
    VersionDependentPath vdp = VersionDependentPath.getCurrent();
    File dir = new File(vdp.getPath(PathType.SERVICE, true));
    File[] files = dir.listFiles();
    for (File f : files) {
      if (f.isDirectory() && f.listFiles().length == 0) {
        FileUtils.deleteDirectory(f);
      }
    }
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
}
