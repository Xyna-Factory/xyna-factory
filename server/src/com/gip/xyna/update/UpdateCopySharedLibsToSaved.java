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
import java.io.FileFilter;

import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;


public class UpdateCopySharedLibsToSaved extends UpdateJustVersion{

  public UpdateCopySharedLibsToSaved(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }

  @Override
  public void update() throws XynaException {
    VersionDependentPath vdp = VersionDependentPath.getCurrent();
    String deployedSharedLibPath = vdp.getPath(PathType.SHAREDLIB, true);
    File deployedSharedLibDir = new File(deployedSharedLibPath);
    File[] deployedSharedLibFolders = deployedSharedLibDir.listFiles(new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });
    
    String savedSharedLibPath = vdp.getPath(PathType.SHAREDLIB, false);
    if (deployedSharedLibFolders != null) {
      for (File deployedSharedLibFolder : deployedSharedLibFolders) {
        String sharedLibName = deployedSharedLibFolder.getName();
        File savedSharedLibFolder = new File(savedSharedLibPath + sharedLibName);
        if (!savedSharedLibFolder.exists()) {
          logger.info("Copy sharedlib '" + sharedLibName + "' from " + deployedSharedLibPath + " to " + savedSharedLibPath);
          FileUtils.copyRecursively(deployedSharedLibFolder, savedSharedLibFolder);
        }
      }
    }
  }
  
}
