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



import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.idgeneration.GeneratedIDsStorable;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.repository.FileSystemRepository;
import com.gip.xyna.xdev.xlibdev.repository.FileSystemRepository.RevisionNumberProvider;
import com.gip.xyna.xdev.xlibdev.repository.Repository.VersionedObject;
import com.gip.xyna.xdev.xlibdev.repository.RepositoryManagement;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;



public class UpdateCreateXMOMRepository extends UpdateJustVersion {

  public UpdateCreateXMOMRepository(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }


  private static class LocalRevisionNumberProvider implements RevisionNumberProvider {

    private final AtomicLong currentRevision;


    public LocalRevisionNumberProvider(long max) {
      currentRevision = new AtomicLong(max);
    }


    public long getCurrentRevision() {
      return currentRevision.get();
    }


    public long incrementAndGetRevision() {
      return currentRevision.incrementAndGet();
    }
  }


  @Override
  protected void update() throws XynaException {
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      // factory ist noch nicht initialisiert, revisionManagement wird aber gebraucht
      UpdateGeneratedClasses.mockFactory();
      
      WorkflowDatabase wfdb = WorkflowDatabase.getWorkflowDatabasePreInit();
      LocalRevisionNumberProvider lrnp = new LocalRevisionNumberProvider(0);
      save(lrnp, wfdb.getDeployedWfs());
      save(lrnp, wfdb.getDeployedDatatypes());
      save(lrnp, wfdb.getDeployedExceptions());
      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(GeneratedIDsStorable.class);
      try {
        ODSConnection c = ods.openConnection();
        try {
          Collection<GeneratedIDsStorable> list = c.loadCollection(GeneratedIDsStorable.class);
          Set<Integer> bindings = new HashSet<Integer>();
          for (GeneratedIDsStorable s : list) {
            bindings.add(s.getBinding());
          }
          int binding;
          if (bindings.size() == 1) {
            binding = bindings.iterator().next();
          } else if (bindings.size() == 0) {
            binding = 0;
          } else {
            bindings.remove(0);
            binding = bindings.iterator().next();
          }
          GeneratedIDsStorable s =
              new GeneratedIDsStorable(RepositoryManagement.REPOSITORY_IDS_REALM, RepositoryManagement.REPOSITORY_IDS_REALM, binding);
          s.setLastStoredId(lrnp.getCurrentRevision());
          s.setResultingFromShutdown(true);
          c.persistObject(s);
          c.commit();
        } finally {
          c.closeConnection();
        }
      } finally {
        ods.unregisterStorable(GeneratedIDsStorable.class);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }


  private static final Pattern patternDot = Pattern.compile("\\.");


  private void save(LocalRevisionNumberProvider lrnp, HashMap<Long, List<String>> deployed) {
    Set<String> reservedServerObjectXmlNames = GenerationBase.getReservedServerObjectXmlNames();
    
    for (Entry<Long, List<String>> e : deployed.entrySet()) {
      List<VersionedObject> list = new ArrayList<VersionedObject>();
      RuntimeContext rc;
      try {
        RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        rc = rm.getRuntimeContext(e.getKey());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        logger.warn("revision " + e.getKey() + " not found.", e1);
        continue;
      }
      FileSystemRepository fsr =
          new FileSystemRepository(Constants.ROOT_DIR_FOR_REPOSITORY + Constants.FILE_SEPARATOR
              + RepositoryManagement.createRuntimeContextSubDirInRepository(rc), false, lrnp);
      
      Set<String> allNames = new HashSet<String>(e.getValue());
      if (rc instanceof Workspace) {
        allNames.addAll(reservedServerObjectXmlNames); //für die reservierten objekte auch checken, weil die nicht in der wfdatabase stehen (müssen)
      }
      try {
        for (String fqName : allNames) {
          //einzeln speichern um fehler lokal zu halten
          File xmlFile = new File(GenerationBase.getFileLocationForDeploymentStaticHelper(fqName, e.getKey()) + ".xml");
          if (!xmlFile.exists()) {
            boolean reserved = false;
            if ((rc instanceof Workspace) && GenerationBase.isReservedServerObjectByFqOriginalName(fqName)) {
              reserved = true;
              xmlFile = new File(GenerationBase.getFileLocationForSavingStaticHelper(fqName, e.getKey()) + ".xml");
            } 
            if (!xmlFile.exists() && !reserved) {
              logger.info("skipping " + fqName + " in revision " + e.getKey() + " (" + rc + "). XML not found.");
              continue;
            }
          }
          String filePath = patternDot.matcher(fqName).replaceAll(Constants.fileSeparator) + ".xml";
          InputStream is;
          try {
            is = new BufferedInputStream(new FileInputStream(xmlFile)); //wird in saveList geschlossen
            list.add(new VersionedObject(filePath, is));
          } catch (FileNotFoundException e1) {
            logger.warn("Could not access file " + xmlFile + ".");
          }
          if (list.size() >= 100) { //Nicht zuviele Files gleichzeitig geöffnet haben
            saveList(fsr, list);
            list.clear();
          }
        }
        saveList(fsr, list);
      } finally {
        fsr.shutdown();
      }
    }
  }


  private void saveList(FileSystemRepository fsr, List<VersionedObject> list) {
    try {
      try {
        fsr.saveFilesInNewRevision(list.toArray(new VersionedObject[list.size()]), "initial import");
      } finally {
        for (VersionedObject vo : list) {
          vo.getContent().close();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not save file in repository", e);
    }
  }

}
