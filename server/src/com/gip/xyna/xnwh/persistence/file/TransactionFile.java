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
package com.gip.xyna.xnwh.persistence.file;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.exceptions.Ex_FileAccessException;

/**
 * Klasse kappselt das File-Objekt transaktionssicher.
 * Dazu wird eine temporäre Datei angelegt, auf welcher die Dateioperationen durchgeführt werden.
 * Beim commit-Aufruf wird die temporäre Datei umbenannt. Dieses rename wird mittels eines OS-Aufrufs
 * durchgeführt und sollte atomar sein (in jedem Fall unter *nix).
 * 
 * In dieser Klasse findet kein Locking zur Vermeidung von konkurrierenden Dateioperationen statt. Dies
 * muss, wenn gewünscht, in bei der Verwendung selbst gemacht werden.
 * 
 * NB: Klasse ist nicht thread-safe.
 *
 */
public class TransactionFile extends File {

  private static final long serialVersionUID = 799167492966291043L;
  private static final Logger logger = CentralFactoryLogging.getLogger(TransactionFile.class);
  
  public static final String FILE_SUFFIX = ".transaction.tmp";

  // File-Objekt, für welches ein transaktionsgesicherter Zugriff gewährleistet wird
  private File originalFile;
  
  private boolean isCommited = false;
  private boolean localChanges = false;
  private final long expectLastModified;
  private boolean deleted = false;
  
  /**
   * Konstruktor erzeugt neues File-Objekt, auf dem Dateioperationen durchgeführt werden können,
   * die bis zu einem commit die Originaldatei nicht modifizieren.
   * 
   * @param fileObject  Datei, für welche eine temporäre Datei angelegt werden soll 
   */
  public TransactionFile(File fileObject) {
    this(fileObject, fileObject.getAbsoluteFile().getParentFile());
  }
  
  public TransactionFile(File fileObject, long lastModified) {
    this(fileObject, fileObject.getAbsoluteFile().getParentFile(), lastModified);
  }
  
  /**
   * Konstruktor erzeugt neues File-Objekt, auf dem Dateioperationen durchgeführt werden können,
   * die bis zu einem commit die Originaldatei nicht modifizieren.
   * 
   * @param fileObject  Datei, für welche eine temporäre Datei angelegt werden soll
   * @param directoryPath   Ordner, in dem temporäre Datei gespeichert wird 
   */
  public TransactionFile(File fileObject, File directoryPath) {
    this(fileObject, directoryPath, fileObject.lastModified());
  }
  
  
  public TransactionFile(File fileObject, File directoryPath, long lastModified) {
    super(createTemporaryName(fileObject, directoryPath));
    originalFile = fileObject;
    expectLastModified = lastModified;
  }
  
  public File getOriginalFile() {
    return originalFile;
  }
  
  
  public boolean hasLocalChanges() {
    return localChanges;
  }

  /**
   * setzt das originale file auf readonly
   * @return entspricht der zeitstempel lastmodified dem, den das file anfangs hatte.
   */
  public boolean checkNotModified() {
    //beim commit wird das file überschrieben, und damit wieder writeable gemacht
    originalFile.setReadOnly();
    if (expectLastModified != originalFile.lastModified()) {
      return false;
    }
    return true;
  }

  /**
   * Führt alle Änderungen auf der Datei durch. 
   * Commit darf nur einmal ausgeführt werden. Für
   * weiteren schreibend Zugriff muss neues Objekt erzeugt werden. 
   */
  public void commit() {
    if(isCommited) {
      throw new XynaRuntimeException("Commit is not allowed to be called twice.", null, null);
    }
    if(originalFile.getParentFile() != null) {
      originalFile.getParentFile().mkdirs();
    }
    
    if (!exists() && deleted) {
      if (!originalFile.delete() && originalFile.exists()) {        
        logger.warn("Could not delete " + originalFile.getAbsolutePath());
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Deleted " + originalFile.getName());
        }
      }
    } else {
      // Unter Windows muss Datei ggf. vorher gelöscht sein, bevor sie umbeannt werden kann --> Verlust der Atomarität
      if (!renameTo(originalFile)) {
        throw new RuntimeException("Could not rename file " + getAbsolutePath() + " to " + originalFile.getAbsolutePath());
      }
    }

    if (originalFile instanceof TransactionFile) {
      ((TransactionFile) originalFile).localChanges = true;
    }
    isCommited = true;
    if (logger.isTraceEnabled()) {
      logger.trace("Committed changes to " + originalFile.getName());
    }
  }

  

  public void rollback() {
    originalFile.setWritable(true);
    originalFile = null;
    delete();
    if (logger.isTraceEnabled()) {
      logger.trace("Rollbacked changes to " + originalFile.getName());
    }
  }
  
  @Override
  public boolean delete() {
    localChanges = true;
    deleted = true;
    return super.delete();
  }


  /**
   * Kopiert Dateiinhalt von Originaldatei zur temporären Datei, um an dieser Daten anhängen zu können.
   */
  public void copyFileContent() {
    if(originalFile.exists()) {
      try {
        FileUtils.copyFile(originalFile, this);
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException("Copy contents failed.", e);
      }
    }
  }
  
  
  /**
   * 
   * @param pathname    Pfad zur Orginaldatei. Wird zur Erzeugung des TempFileName verwendet.
   * @return            Pfad zur erzeugten temp. Datei.
   */
  private static String createTemporaryName(File pathname, File directory) {
    try {
      directory.mkdirs();
      File tmpFile = File.createTempFile(pathname.getName(), FILE_SUFFIX, directory);
      return tmpFile.getAbsolutePath();
    } catch(IOException e) {
      throw new RuntimeException("Temporary file for " + pathname + " could not be created.", e);
    }
  }

}
