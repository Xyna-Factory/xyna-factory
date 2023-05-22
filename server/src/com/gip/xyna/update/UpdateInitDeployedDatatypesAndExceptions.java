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
package com.gip.xyna.update;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeployedDatatypesAndExceptionsStorable;


public class UpdateInitDeployedDatatypesAndExceptions extends UpdateJustVersion {

  
  private final Update wrappedUpdate;
  
  public UpdateInitDeployedDatatypesAndExceptions(Update update) throws XynaException {
    super(update.getAllowedVersionForUpdate(), update.getVersionAfterUpdate(), update.mustUpdateGeneratedClasses(),
          update.mustRewriteWorkflows(), update.mustRewriteDatatypes(), update.mustRewriteExceptions());
    wrappedUpdate = update;
  }
  
  
  @Override
  protected void update() throws XynaException {
    try {
      ownUpdate();
    } finally {
      wrappedUpdate.update();
    }
  }
  
  
  private void ownUpdate() throws XynaException {
    Map<Long, Collection<Pair<XMOMType, String>>> all = getAllDeployedDatatypesAndExceptions();
    Collection<DeployedDatatypesAndExceptionsStorable> storables = new ArrayList<DeployedDatatypesAndExceptionsStorable>();
    for (Long revision : all.keySet()) {
      Collection<Pair<XMOMType, String>> forRevision = all.get(revision);
      for (Pair<XMOMType, String> pair : forRevision) {
        if (pair.getFirst() == XMOMType.DATATYPE) {
          storables.add(DeployedDatatypesAndExceptionsStorable.datatype(pair.getSecond(), revision));
        } else {
          storables.add(DeployedDatatypesAndExceptionsStorable.exception(pair.getSecond(), revision));
        }
      }
    }
    ODSImpl.getInstance().registerStorable(DeployedDatatypesAndExceptionsStorable.class);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistCollection(storables);
      con.commit();
    } finally {
      con.closeConnection();
      ODSImpl.getInstance().unregisterStorable(DeployedDatatypesAndExceptionsStorable.class);
    }
    // v99 already inits the wf-db our values have to be injected in the currently initialized instance
    WorkflowDatabase wfdb = WorkflowDatabase.getWorkflowDatabasePreInit();
    try {
      Method refreshMethod = WorkflowDatabase.class.getDeclaredMethod("refreshDeployedDatatypesAndExceptions", Collection.class);
      refreshMethod.setAccessible(true);
      refreshMethod.invoke(wfdb, storables);
    } catch (Throwable e) {
      logger.warn("Failed to initialize WorkflowDatabase with datatypes and exceptions.",e);
    }
  }

  
  
  private static Map<Long, Collection<Pair<XMOMType, String>>> getAllDeployedDatatypesAndExceptions() {
    Map<Long, Collection<Pair<XMOMType, String>>> all = new HashMap<Long, Collection<Pair<XMOMType,String>>>();
    Collection<Long> revisions = discoverRevisions();
    for (Long revision : revisions) {
      File basePath = new File(VersionManagement.getPathForRevision(PathType.XMOM, revision));
      List<File> foundFiles = new ArrayList<File>();
      FileUtils.findFilesRecursively(basePath, foundFiles, new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return dir.isDirectory() || name.endsWith(".xml");
        }
      });
      List<Pair<XMOMType, String>> thisRevision = new ArrayList<Pair<XMOMType,String>>();
      for (File file : foundFiles) {
        try {
          Document d = XMLUtils.parse(file, true);
          Element rootElement = d.getDocumentElement();
          XMOMType type = XMOMType.getXMOMTypeByRootTag(rootElement.getTagName());
          if (type == XMOMType.DATATYPE || type == XMOMType.EXCEPTION) {
            thisRevision.add(Pair.of(type, GenerationBase.getFqXMLName(d))); 
          }
        } catch (Throwable t) {
          Department.handleThrowable(t);
        }
      }
      all.put(revision, thisRevision);
    }
    return all;
  }
  
  
  private static Collection<Long> discoverRevisions() {
    File baseFolder = new File(Constants.BASEDIR + Constants.FILE_SEPARATOR + Constants.REVISION_PATH);
    File[] revisionFolders = baseFolder.listFiles(new FileFilter() {
      public boolean accept(File file) {
        return file.isDirectory() && file.getName().startsWith(Constants.PREFIX_REVISION);
      }
    });
    List<Long> revisions = new ArrayList<Long>();
    for (File file : revisionFolders) {
      String revision = file.getName().substring(Constants.PREFIX_REVISION.length());
      if (revision.equals("workingset")) {
        revisions.add(-1L);
      } else {
        try {
          revisions.add(Long.parseLong(revision));
        } catch (NumberFormatException e) {
          // in case of special revisions like datamodel, just skip them
        }
      }
    }
    return revisions;
  }
  
  
}
