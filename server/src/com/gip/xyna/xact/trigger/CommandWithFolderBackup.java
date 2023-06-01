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
package com.gip.xyna.xact.trigger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xact.exceptions.XACT_ErrorClosingStream;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xfmg.Constants;


public abstract class CommandWithFolderBackup {
  
  public enum ExecutionPhase {  
    BACKUP, EXECUTION, ROLLBACK, ERROR_DURING_ROLLBACK
  };
  
  private static Logger logger = CentralFactoryLogging.getLogger(CommandWithFolderBackup.class);

  private static final String PREFIX = "temp_";
  private static final String SUFFIX = ".zip";
  
  private ExecutionPhase currentPhase;
  
  protected abstract void executeInternally() throws InternalException;
  protected abstract void rollbackFailureTreatment(Throwable t) throws InternalException;
 
  /**
   * ExceptionWrapper für alle Exceptions, die in implementierten methoden passieren können
   */
  public static class InternalException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public InternalException(Throwable t) {
      super(t);
    }
  }
  
  private File folderToBackUp;
  private File backUpZip;
  
  public void execute(File folderToBackUp) throws Ex_FileAccessException, XACT_JarFileUnzipProblem, InternalException {
    this.currentPhase = ExecutionPhase.BACKUP;
    logger.debug("Execution of command with folder rollback started");
    this.folderToBackUp = folderToBackUp;
    backUpZip = backup();
    try {
      this.currentPhase = ExecutionPhase.EXECUTION;
      executeInternally();
      removeBackup();
    } catch (InternalException e) {
      rollback(e);
    } catch (RuntimeException e) {
      rollback(e);
    } catch (Error e) {
      Department.handleThrowable(e);
      rollback(e);
    }
  }

  protected void rollbackAndExecute() throws XACT_JarFileUnzipProblem, Ex_FileAccessException, InternalException {
    rollback();
    executeInternally();
  }

  private <E extends Throwable> void rollback(E e) throws E, InternalException {
    this.currentPhase = ExecutionPhase.ROLLBACK;
    logger.error("Error while trying to execute command, rollback initiated", e);
    boolean success = false;
    try {
      rollbackAndExecute();
      success = true;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      this.currentPhase = ExecutionPhase.ERROR_DURING_ROLLBACK;
      logger.error("Error during rollback of command", t);
      rollbackFailureTreatment(t);
    }
    if (success) {
      //ansonsten hat das rollback entschieden, keinen fehler zu werfen, ok.
      throw e;
    }
  }
  
  protected File backup() throws Ex_FileAccessException, XACT_JarFileUnzipProblem, InternalException {
    File file;
    do {
      file = new File(generateRandomZipPath());
    } while (file.exists());
    ZipOutputStream zos;
    try {
      zos = new ZipOutputStream(new FileOutputStream(file));
    } catch (FileNotFoundException e1) {
      throw new XACT_JarFileUnzipProblem(folderToBackUp.getPath(), "Could not open ZipOutputStream");
    }
    try {
      FileUtils.zipDir(folderToBackUp, zos, folderToBackUp);
    } finally {
      try {
        zos.close();
      } catch (IOException e) {
        throw new Ex_FileAccessException(folderToBackUp.getPath(), e);
      }
    }
    return file;
  }
  
  protected void removeBackup() {
    if (backUpZip.exists()) {
      backUpZip.delete();
    }
  }
  
  
  protected void rollback() throws XACT_JarFileUnzipProblem, Ex_FileAccessException {
    System.gc(); //classloader referenzen gc-en, die evtl noch auf jars zeigen
    File[] files = folderToBackUp.listFiles();
    for (File file : files) {
      file.delete();
    }
    try {
      ZipInputStream zis = new ZipInputStream(new FileInputStream(backUpZip));
      try {
        FileUtils.saveZipToDir(zis, folderToBackUp);
      } finally {
        zis.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(folderToBackUp.getPath(), e);
    }
    removeBackup();
  }
  
  
  private String generateRandomZipPath() {
    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append(Constants.BASEDIR)
               .append(Constants.fileSeparator)
               .append(PREFIX)
               .append(System.currentTimeMillis())
               .append("_")
               .append(Math.round(Math.random()*Long.MAX_VALUE))
               .append(SUFFIX);
    return nameBuilder.toString();
  }
  
  
  public ExecutionPhase getCurrentPhase() {
    return currentPhase;
  }
  
  
}
