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
package com.gip.xyna.xdev.xlibdev.repository;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.ExpiringMap;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.LineBufferedInputStream;
import com.gip.xyna.utils.streams.LineBufferedInputStream.LineMarker;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xdev.xlibdev.repository.Repository.Revision;
import com.gip.xyna.xdev.xlibdev.repository.Repository.VersionedObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



public class RepositoryManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "RepositoryManagement";


  public RepositoryManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  
  @Override
  protected void init() throws XynaException {
    XynaFactory.getInstance().getFutureExecution().addTask(RepositoryManagement.class, DEFAULT_NAME).after(DeploymentHandling.class)
        .execAsync(new Runnable() {

          public void run() {
            initXMLRepositories();
          }

        });
  }


  @Override
  protected void shutdown() throws XynaException {
    for (Repository r : xmlrepositories.values()) {
      r.shutdown();
    }
    xmlrepositories.clear();
  }


  private final ExpiringMap<RuntimeContext, Repository> xmlrepositories = new ExpiringMap<RuntimeContext, Repository>(1, TimeUnit.HOURS, true) {

    private static final long serialVersionUID = 1L;


    @Override
    public Repository remove(Object key, boolean check) {
      Repository r = super.remove(key, check);
      if (r != null) {
        r.shutdown();
      }
      return r;
    }

  };


  private void initXMLRepositories() {
    //priority so wählen, damit von generationbase auch für serverreservierte objekte richtig berücksichtigt
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addDeploymentHandler(DeploymentHandling.PRIORITY_EXCEPTION_DATABASE, new DeploymentHandler() {

          RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
          private AtomicBoolean changedRevision = new AtomicBoolean(false);

          public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
            if (mode.updateXMOMRepository()) {
              try {
                if (RepositoryManagement.this.saveXMLInRepository(rm.getRuntimeContext(object.getRevision()), object.getOriginalFqName(),
                                                                  object.getDeploymentComment())) {
                  changedRevision.set(true);
                }
              } catch (XynaException e) {
                throw new XPRC_DeploymentHandlerException(object.getOriginalFqName(), "XML Repository");
              }
            }
          }


          public void finish(boolean success) throws XPRC_DeploymentHandlerException {
            if (changedRevision.compareAndSet(true, false)) {              
              XynaFactory.getInstance().getIDGenerator().storeLastUsed(REPOSITORY_IDS_REALM);
            }
          }


          @Override
          public void begin() throws XPRC_DeploymentHandlerException {
          }

        });
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addUndeploymentHandler(DeploymentHandling.PRIORITY_EXCEPTION_DATABASE, new UndeploymentHandler() {

          RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();


          public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {
            try {
              RepositoryManagement.this.removeXmlFromRepository(rm.getRuntimeContext(object.getRevision()), object.getOriginalFqName());
            } catch (XynaException e) {
              throw new XPRC_UnDeploymentHandlerException(object.getOriginalFqName(), "XML Repository");
            }
          }


          public void exec(FilterInstanceStorable object) {
          }


          public void exec(TriggerInstanceStorable object) {
          }


          public void exec(Capacity object) {
          }


          public void exec(DestinationKey object) {
          }


          public void finish() throws XPRC_UnDeploymentHandlerException {
            XynaFactory.getInstance().getIDGenerator().storeLastUsed(REPOSITORY_IDS_REALM);
          }


          public boolean executeForReservedServerObjects() {
            return true;
          }


          public void exec(FilterStorable object) {
          }


          public void exec(TriggerStorable object) {
          }

        });
  }


  protected void removeXmlFromRepository(RuntimeContext rc, String fqXmlName) throws XynaException {
    Repository repository = getRepository(rc);
    String fileName = getFileNameInRepository(fqXmlName);
    repository.deleteFilesInNewRevision(new String[] {fileName}, "");
  }


  private String getFileNameInRepository(String fqXmlName) {
    return dotPattern.matcher(fqXmlName).replaceAll(Constants.fileSeparator) + ".xml";
  }


  public static final String REPOSITORY_IDS_REALM = "xmomrepository";
  private static final Pattern dotPattern = Pattern.compile("\\.");


  //globale id und nicht eindeutig pro revision verwenden, damit man im audit einfach nur die id angeben kann
  private static class RepositoryRevisionProviderImpl implements FileSystemRepository.RevisionNumberProvider {

    private long currentRevision;

    private final IDGenerator idgen = XynaFactory.getInstance().getIDGenerator();

    public RepositoryRevisionProviderImpl() {
      incrementAndGetRevision();
    }
    
    public synchronized long getCurrentRevision() {
      return currentRevision;
    }


    public synchronized long incrementAndGetRevision() {
      currentRevision = idgen.getUniqueId(REPOSITORY_IDS_REALM);
      return currentRevision;
    }

  }


  private Repository getRepository(RuntimeContext rc) {
    Repository repository = xmlrepositories.get(rc);
    if (repository == null) {
      repository = createRepository(rc);
    }
    return repository;
  }


  public String getXMLFromRepository(RuntimeContext rc, long repositoryRevision, String fqXmlName) throws XynaException {
    Repository repository = getRepository(rc);
    String fileName = getFileNameInRepository(fqXmlName);
    String xml;
    try {
      xml = getContentOrNull(repository.getContentOfFileInRevision(fileName, repositoryRevision));
    } catch (IOException e) {
      throw new Ex_FileAccessException(fileName);
    }
    if (xml == null) {
      throw new RuntimeException("XML for " + fqXmlName + " not found in repository (rev=" + repositoryRevision + ", RuntimeContext=" + rc
          + ")");
    }
    return xml;
  }


  public boolean saveXMLInRepository(RuntimeContext rc, String fqXmlName, String comment) throws XynaException {
    Repository repository = getRepository(rc);
    String fileName = getFileNameInRepository(fqXmlName);
    long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rc);
    String xmlLocation = GenerationBase.getFileLocationForDeploymentStaticHelper(fqXmlName, revision) + ".xml";
    //TODO schnittstelle in abgeleiteter repository klasse um solche utilmethoden erweitern
    try {      
      String xmlRepository = getContentOrNull(repository.getContentOfFileInRevision(fileName, repository.getCurrentRevision()));
      String xmlDeployed = getContentOrNull(new FileInputStream(xmlLocation));
      if (xmlDeployed.equals(xmlRepository)) {
        return false;
      }
      repository.saveFilesInNewRevision(new VersionedObject[] {new VersionedObject(fileName, new ByteArrayInputStream(xmlDeployed
                                            .getBytes(Constants.DEFAULT_ENCODING)))}, comment);
      return true;
    } catch (IOException e) {
      throw new Ex_FileAccessException(fqXmlName);
    }
  }


  private static final XynaPropertyBoolean closeRepositoryToFlush =
      new XynaPropertyBoolean("xprc.xprcods.orderarchive.repository.closefiletoflush", false);
  //zeichen, die nicht in verzeichnisname enthalten sein sollen, der sich aus applicationname/versionname ergibt.
  public static final Pattern PATTERN_FOR_REPOSITORY_DIR_NAMES_REPLACEMENT = Pattern.compile("[^\\w\\.\\-]");
  private volatile RepositoryRevisionProviderImpl rrpi;

  private static final Map<String, RuntimeContext> existingRepositoryRuntimeContexts = new HashMap<String, RuntimeContext>();


  /**
   * erzeuge eindeutigen namen für das repository unterverzeichnis und speichert das mapping einem file
   */
  public static synchronized String createRuntimeContextSubDirInRepository(RuntimeContext rc) {
    String s;
    if (rc instanceof Workspace) {
      s = "WS_" + PATTERN_FOR_REPOSITORY_DIR_NAMES_REPLACEMENT.matcher(rc.getName()).replaceAll("_");
    } else if (rc instanceof Application) {
      s =
          "APP_" + PATTERN_FOR_REPOSITORY_DIR_NAMES_REPLACEMENT.matcher(rc.getName()).replaceAll("_") + "="
              + PATTERN_FOR_REPOSITORY_DIR_NAMES_REPLACEMENT.matcher(((Application) rc).getVersionName()).replaceAll("_");
    } else {
      throw new RuntimeException("Unsupported RuntimeContext Type " + rc);
    }
    if (existingRepositoryRuntimeContexts.size() > 10000) {
      //alte einträge entfernen
      existingRepositoryRuntimeContexts.clear();
    }
    lazyInitRepositoryRuntimeContexts();

    //eindeutigen verzeichnisnamen sicherstellen
    while (true) {
      RuntimeContext existingRC = existingRepositoryRuntimeContexts.get(s);
      if (existingRC == null) {
        break;
      } else if (existingRC.equals(rc)) {
        return s;
      }
      s += "X"; //TODO hochlaufende nummer? sollte aber nicht häufig passieren...
    }

    //namen persistieren
    File f = getRuntimeContextsFile();
    try {
      OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f, true)), Constants.DEFAULT_ENCODING);
      try {
        osw.write(s + " " + rc.serializeToString() + "\n");
      } finally {
        osw.close();
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not write RuntimeContext Mapping for Repository to " + f.getAbsolutePath(), e);
    }
    existingRepositoryRuntimeContexts.put(s, rc);

    return s;
  }


  private static synchronized void lazyInitRepositoryRuntimeContexts() {
    if (existingRepositoryRuntimeContexts.isEmpty()) {
      //von file nachladen
      File f = new File(Constants.ROOT_DIR_FOR_REPOSITORY + Constants.FILE_SEPARATOR + "runtimecontexts");
      try {
        if (!f.exists()) {
          if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
          }
          f.createNewFile();
        }
        LineBufferedInputStream lbis =
            new LineBufferedInputStream(new FileInputStream(f), LineMarker.LF.getBytes(), Constants.DEFAULT_ENCODING, true);
        try {
          String line;
          while (null != (line = lbis.readLine())) {
            String[] parts = line.split(" ", 2);
            existingRepositoryRuntimeContexts.put(parts[0], RuntimeContext.valueOf(parts[1]));
          }
        } finally {
          lbis.close();
        }
      } catch (IOException e) {
        throw new RuntimeException("Could not access RuntimeContext Mapping for Repository (" + f.getAbsolutePath(), e);
      }
    }
  }


  private static File getRuntimeContextsFile() {
    return new File(Constants.ROOT_DIR_FOR_REPOSITORY + Constants.FILE_SEPARATOR + "runtimecontexts");
  }


  private Repository createRepository(RuntimeContext rc) {
    String rootDir = Constants.ROOT_DIR_FOR_REPOSITORY + Constants.FILE_SEPARATOR + createRuntimeContextSubDirInRepository(rc);
    if (rrpi == null) {
      rrpi = new RepositoryRevisionProviderImpl();
    }
    FileSystemRepository r = new FileSystemRepository(rootDir, closeRepositoryToFlush.get(), rrpi);
    Repository previous = xmlrepositories.putIfAbsent(rc, r);
    if (previous != null) {
      r.shutdown();
      return previous;
    }
    return r;
  }


  public long getCurrentRepositoryRevision() {
    if (rrpi == null) {
      rrpi = new RepositoryRevisionProviderImpl();
    }
    return rrpi.getCurrentRevision();
  }


  private String getContentOrNull(InputStream is) throws IOException {
    if (is == null) {
      return null;
    } else {
      try {
        if (!(is instanceof BufferedInputStream)) {
          is = new BufferedInputStream(is);
        }
        return new Scanner(is, Constants.DEFAULT_ENCODING).useDelimiter("\\A").next();
      } finally {
        is.close();
      }
    }
  }
  
  public void cleanupRepositories(long timestampMillis) throws XynaException {
    /*
     * für alle repositories die es gibt (in xmomrepository):
     *   aufräumen
     *   ist history leer -> verzeichnis entfernen und eintrag aus repositories textfile
     */
    lazyInitRepositoryRuntimeContexts();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<Entry<String, RuntimeContext>> rcs = new ArrayList<Entry<String, RuntimeContext>>(existingRepositoryRuntimeContexts.entrySet());
    List<Entry<String, RuntimeContext>> toDelete = new ArrayList<Entry<String,RuntimeContext>>();
    for (Entry<String, RuntimeContext> e : rcs) {
      boolean found;
      try {
        rm.getRevision(e.getValue());
        found = true;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY ex) {
        found = false;
      }
      boolean delete = cleanupRepository(e.getValue(), timestampMillis, found);
      if (delete) {
        toDelete.add(e);
      }
    }

    if (toDelete.size() > 0) {
      //runtimecontexts-file anpassen
      synchronized (RepositoryManagement.class) {
        for (Entry<String, RuntimeContext> e : toDelete) {
          existingRepositoryRuntimeContexts.remove(e.getKey());
        }

        File f = getRuntimeContextsFile();
        File mvd = new File(f.getAbsolutePath() + "." + Constants.defaultUTCSimpleDateFormat().format(new Date()));

        FileUtils.moveFile(f, mvd);

        //runtimecontexts-file neu schreiben
        try {
          f.createNewFile();
          OutputStreamWriter osw =
              new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f, true)), Constants.DEFAULT_ENCODING);
          try {
            rcs = new ArrayList<Entry<String, RuntimeContext>>(existingRepositoryRuntimeContexts.entrySet());
            for (Entry<String, RuntimeContext> e : rcs) {
              osw.write(e.getKey() + " " + e.getValue().serializeToString() + "\n");
            }
          } finally {
            osw.close();
          }
          mvd.delete();
        } catch (IOException e) {
          logger.warn("Could not create file " + f.getAbsolutePath() + ". Backup is " + mvd.getAbsolutePath(), e);
        }
      }

      // verzeichnisse löschen
      for (Entry<String, RuntimeContext> e : toDelete) {
        File f = new File(Constants.ROOT_DIR_FOR_REPOSITORY, e.getKey());
        logger.info("deleting directory " + f.getAbsolutePath());
        FileUtils.deleteDirectoryRecursively(f);
      }
    }
  }


  /**
   * @param rc
   * @param timestampMillis Fileversionen die nach diesem Zeitpunkt nicht mehr gültig sind, können entfernt werden.
   * @throws XynaException 
   */
  private boolean cleanupRepository(RuntimeContext rc, long timestampMillis, boolean activeRuntimeContext) throws XynaException {
    Repository repository = getRepository(rc);
    repository.cleanupEarlierThan(timestampMillis);
    if (!activeRuntimeContext) { //ansonsten könnte noch aufträge erstellt werden
      //nun noch überprüfen, dass nach timestampmillis keine aufträge mehr gelaufen sein können -> currentRevision == damalige revision && alles gelöscht.
      long r = repository.getCurrentRevision();
      if (repository.listFiles(r).length == 0) {
        Revision rev = repository.getRevision(r);
        if (rev == null || rev.getTimestamp() <= timestampMillis) {
          return true;
        }
      }
    }
    return false;
  }


  public void backup(File backupdir) {
    File backup = new File(backupdir, Constants.defaultUTCSimpleDateFormat().format(new Date())+ ".zip");
    try {
      FileUtils.zipDirectory(backup, new File(Constants.ROOT_DIR_FOR_REPOSITORY));
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("Could not create backup: " + backup.getAbsolutePath(), e);
    }
  }

}
