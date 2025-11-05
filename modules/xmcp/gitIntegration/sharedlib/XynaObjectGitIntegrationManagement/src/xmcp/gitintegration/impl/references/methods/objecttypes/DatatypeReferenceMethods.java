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
package xmcp.gitintegration.impl.references.methods.objecttypes;



import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import xmcp.gitintegration.impl.references.ReferenceObjectTypeMethods;



public class DatatypeReferenceMethods implements ReferenceObjectTypeMethods {

  private static final Logger logger = CentralFactoryLogging.getLogger(DatatypeReferenceMethods.class);
  
  @Override
  public void trigger(List<File> candidateFiles, String objectName, Long revision) {
    List<String> requiredFiles = getRequiredFiles(objectName, revision);
    File targetDir = new File(RevisionManagement.getPathForRevision(PathType.SERVICE, revision, false), objectName);
    for(String jarName : requiredFiles) {
      Optional<File> candidateFile = candidateFiles.stream().filter(x -> x.getName().equals(jarName)).findFirst();
      if(candidateFile.isPresent()) {
        try {
          if(candidateFile.get().getParentFile().getAbsolutePath().equals(targetDir.getAbsolutePath())) {
            if(logger.isDebugEnabled()) {
              logger.debug("Skipping copy of " + jarName + " for datatype " + objectName + " in revision " + revision + ", because it is already at the destination");
            }
            continue;
          }
          FileUtils.copyFileToDir(candidateFile.get(), targetDir);
        } catch (Ex_FileAccessException e) {
          throw new RuntimeException(e);
        }
      } else {
        if(logger.isWarnEnabled()) {
          logger.warn("Did not find required datatype jar " + jarName + " for datatype " + objectName + " in revision " + revision);
        }
      }
    }
  }
  
  private List<String> getRequiredFiles(String objectName, Long revision) {
    try {
      DOM dom = DOM.getInstance(objectName, revision);
      dom.parseGeneration(false, true);
      return new ArrayList<String>(dom.getAdditionalLibraries());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
