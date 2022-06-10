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
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



public abstract class MDMUpdate {

  private static Logger logger = CentralFactoryLogging.getLogger(Update.class);

  public static FilenameFilter xmlFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      if (name.endsWith(".xml") || new File(dir, name).isDirectory()) {
        return true;
      }
      return false;
    }

  };


  /**
   * updated alle mdm xmls, bei denen die version passt und aktualisiert die Version im xml
   * 
   */
  public int update(Map<String, Document> mapFileNameToDocument) throws XynaException {
    int updatedDuringThisUpdate = 0;
    for (Entry<String, Document> e: mapFileNameToDocument.entrySet()) {
      boolean updatedCurrent = updateInternally(new File(e.getKey()), e.getValue());
      if (updatedCurrent) {
        updatedDuringThisUpdate++;
      }
    }
    return updatedDuringThisUpdate;
  }


  protected static void addRelevantFileNamesAndDocuments(File savedMDMFolder, List<File> deployedMDMFolder, Map<String, Document> alreadyAdded) throws XynaException {
    addFiles(savedMDMFolder, true, alreadyAdded);
    for (File f : deployedMDMFolder) {
      addFiles(f, false, alreadyAdded);
    }
  }

  protected static void addRelevantFileNamesAndDocuments(long revision, Map<String, Document> alreadyAdded) throws XynaException {
    File deployedDir = new File(RevisionManagement.getPathForRevision(PathType.XMOM, revision));
    addFiles(deployedDir, false, alreadyAdded);

    File savedDir = new File(RevisionManagement.getPathForRevision(PathType.XMOM, revision, false));
    addFiles(savedDir, false, alreadyAdded);
  }

  private static void addFiles(File dir, boolean dirMustExist, Map<String, Document> alreadyAdded) throws XynaException {
    if (!dir.isDirectory()) {
      if (dirMustExist) {
        throw new XynaException("directory " + dir.getPath() + " not found");
      } else {
        return;
      }
    }
    File[] files = dir.listFiles(xmlFilter);
    for (File f : files) {
      if (f.isDirectory()) {
        addFiles(f, dirMustExist, alreadyAdded);
      } else {
        String absFilename = f.getAbsolutePath();
        if (!alreadyAdded.containsKey(absFilename)) {
          Document doc = XMLUtils.parse(absFilename);
          alreadyAdded.put(absFilename, doc);
        }
      }
    }
  }


  private boolean updateInternally(File f, Document doc) throws XynaException {
    String mdmVersionFile = doc.getDocumentElement().getAttribute(GenerationBase.ATT.MDM_VERSION);
    if (mdmVersionFile == null || mdmVersionFile.length() == 0) {
      mdmVersionFile = Updater.START_MDM_VERSION;
    }
    if (getAllowedVersionForUpdate().equals(new Version(mdmVersionFile))) {
      update(doc);
      doc.getDocumentElement().setAttribute(GenerationBase.ATT.MDM_VERSION, getVersionAfterUpdate().getString());
      XMLUtils.saveDom(f, doc);
      if (logger.isDebugEnabled()) {
        logger.debug("updated " + f.getAbsolutePath());
      }
      return true;
    } else {
      return false;
    }
  }


  /**
   * updated ein mdm xml und speichert es
   */
  protected abstract void update(Document doc) throws XynaException;


  protected abstract Version getAllowedVersionForUpdate() throws XynaException;


  protected abstract Version getVersionAfterUpdate() throws XynaException;
}
