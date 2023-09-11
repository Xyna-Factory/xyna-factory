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

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import xmcp.gitintegration.impl.processing.ReferenceSupport;
import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceObjectTypeMethods;



public class DatatypeReferenceMethods implements ReferenceObjectTypeMethods {

  @Override
  public void trigger(List<InternalReference> references, String objectName, Long revision) {
    //identify required libraries
    List<String> requiredFiles = getRequiredFiles(objectName, revision);
    ReferenceSupport impl = new ReferenceSupport();
    //findJar using references
    File targetDir = new File(RevisionManagement.getPathForRevision(PathType.SERVICE, revision, false));
    for (String jarName : requiredFiles) {
      File fromFile = impl.findJar(references, jarName, revision);
      //copy fromFile to expected location
      try {
        FileUtils.copyFileToDir(fromFile, targetDir);
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  //TODO: check if parseGeneration is required or something
  //TODO: only additionalLibraries, not shared Libraries
  private List<String> getRequiredFiles(String objectName, Long revision) {
    try {
      DOM dom = DOM.getInstance(objectName, revision);
      return new ArrayList<String>(dom.getAdditionalLibraries());
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }

}
