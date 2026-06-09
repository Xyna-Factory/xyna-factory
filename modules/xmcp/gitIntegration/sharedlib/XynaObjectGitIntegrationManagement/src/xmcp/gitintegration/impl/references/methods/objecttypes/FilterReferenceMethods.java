/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xmcp.gitintegration.impl.references.methods.objecttypes;



import java.io.File;
import java.util.List;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;

import xmcp.gitintegration.impl.references.ReferenceObjectTypeMethods;



public class FilterReferenceMethods implements ReferenceObjectTypeMethods {

  private static final Logger logger = CentralFactoryLogging.getLogger(FilterReferenceMethods.class);


  @Override
  public void trigger(List<File> candidateFiles, String objectName, Long revision) {
    File targetDir = new File(RevisionManagement.getPathForRevision(PathType.FILTER, revision, false), objectName);

    if (!targetDir.exists()) {
      targetDir.mkdirs();
    }

    for (File candidateFile : candidateFiles) {
      try {
        if (candidateFile.getParentFile().getAbsolutePath().equals(targetDir.getAbsolutePath())) {
          if (logger.isDebugEnabled()) {
            logger.debug("Skipping copy of " + candidateFile.getName() + " for filter " + objectName + " in revision " + revision
                + ", because it is already at the destination");
          }
          continue;
        }
        FileUtils.copyFileToDir(candidateFile, targetDir);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
