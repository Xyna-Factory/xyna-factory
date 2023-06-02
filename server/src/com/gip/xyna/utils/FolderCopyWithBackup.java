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
package com.gip.xyna.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.ZipInputStream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xfmg.Constants;


public class FolderCopyWithBackup {
  
  private final File sourceFolder;
  private final File destinationFolder;
  private File backupZip;
  private boolean doNothing;
  private boolean doBackup = true;
  private boolean copyDone = false;
  
  public FolderCopyWithBackup(String sourceFolder, String destinationFolder) {
    this(new File(sourceFolder), new File(destinationFolder));
  }
  
  public FolderCopyWithBackup(File sourceFolder, File destinationFolder) {
    if (!sourceFolder.exists()) {
      doNothing = true;
    }
    if (!destinationFolder.exists()) {
      if (!doNothing) {
        destinationFolder.mkdirs();
      }
      doBackup = false;
    } else if (destinationFolder.listFiles().length <= 0) {
      doBackup = false;
    }
    this.sourceFolder = sourceFolder;
    this.destinationFolder = destinationFolder;
  }
  
  /**
   * kopiert neue files und erstellt backup
   */
  public void copy(boolean purgeDestination) throws Ex_FileAccessException {
    if (!doNothing) {
      if (doBackup) {
        backupZip = generateRandomZipDestination();
        FileUtils.zipDirectory(backupZip, destinationFolder);
        if (purgeDestination) {
          FileUtils.deleteDirectory(destinationFolder);
          destinationFolder.mkdir();
        }
      }
      FileUtils.copyRecursivelyWithFolderStructure(sourceFolder, destinationFolder);
    }
    copyDone = true;
  }
  
  /**
   * backup zurückspielen
   */
  public void rollback() throws Ex_FileAccessException {
    if (!doNothing && copyDone) {
      FileUtils.deleteDirectory(destinationFolder);
      if (doBackup) {
        destinationFolder.mkdir();
        try {
          FileInputStream backupStream = new FileInputStream(backupZip);
          try {
            ZipInputStream unzippingStream = new ZipInputStream(backupStream);
            FileUtils.saveZipToDir(unzippingStream, destinationFolder);
          } finally {
            backupStream.close();
          }
          backupZip.delete();
        } catch (IOException e) {
          throw new Ex_FileAccessException(backupZip.getPath());
        } catch (XACT_JarFileUnzipProblem e) {
          throw new Ex_FileAccessException(backupZip.getPath());
        }
      }
    }
  }
  
  /**
   * backup löschen
   */
  public void remove() {
    if (!doNothing) {
      if (doBackup && backupZip != null && backupZip.exists()) {
        backupZip.delete();
      }
    }
  }
  
  
  private File generateRandomZipDestination() throws Ex_FileAccessException {
    File file = new File(generateRandomZipFilename());
    while (file.exists()) {
      file = new File(generateRandomZipFilename());
    }
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw new Ex_FileAccessException(file.getPath());
    }
    return file;
  }
  
  
  private String generateRandomZipFilename() {
    return FileUtils.generateRandomFilename(Constants.BASEDIR, "temp_" + System.currentTimeMillis() + "_", ".zip");
  }
  

}
