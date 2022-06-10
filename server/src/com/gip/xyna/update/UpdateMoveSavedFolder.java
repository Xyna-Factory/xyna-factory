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
import java.io.IOException;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;


public class UpdateMoveSavedFolder extends UpdateJustVersion {
  
  public UpdateMoveSavedFolder(Version oldVersion, Version newVersion, boolean needsRegenerate) {
    super(oldVersion, newVersion, needsRegenerate);
  }
  
  
  @Override
  public void update() throws XynaException {
    VersionDependentPath oldPath = VersionDependentPath.savedAndRevisions;
    String oldSavedPath = oldPath.getPath(PathType.ROOT, false);
    String oldServicesPath = oldPath.getPath(PathType.SERVICE, false);
    
    VersionDependentPath newPath = VersionDependentPath.onlyRevisions;
    String newSavedPath = newPath.getPath(PathType.ROOT, false);
    String newServicesPath = newPath.getPath(PathType.SERVICE, false);
    
    // saved umziehen
    checkAndMoveFolder(oldSavedPath, newSavedPath, false);

    // services umziehen
    checkAndMoveFolder(oldServicesPath, newServicesPath, false);
  }
  
  public static void checkAndMoveFolder(String from, String to, boolean flag) throws Ex_FileAccessException {
    File fromFile = new File(from);
    File toFile = new File(to);
    
    if(!fromFile.exists()) {
      if(toFile.exists()) {
        // schon verschoben ... nichts zu tun!
        logger.info("Rename " + from + " to " + to + " was already done.");
        return;
      }
      // instead create the required folder
      if (!toFile.mkdirs()) {
        logger.warn("Could not create directory " + toFile);
      }
      return;
    }
    if(!fromFile.renameTo(toFile)) {
      logger.info("Copy from " + from + " to " + to);
      if(flag) {
       File []list = fromFile.listFiles(); 
       if(list == null) {
         return;
       }
       for(File file : list) {
         File newFile = new File(toFile, file.getName());
         if(!newFile.exists()) {
           logger.info( "Copy " + file + " to " + newFile );
           copyRecursivelyWithFolderStructure(file, newFile, false);
         }
       }
      } else {
        copyRecursivelyWithFolderStructure(fromFile, toFile, false);
      }
      logger.info("Delete " + from);
      FileUtils.deleteDirectory(fromFile);
    } else {
      logger.info("Rename " + from + " to " + to);
    }
  }
  
  
  private static void copyRecursivelyWithFolderStructure(File in, File outDir, boolean replaceFiles) throws Ex_FileAccessException {
    if (in.isDirectory()) {
      if (in.getName().equals(".svn")) {
        return;
      }
      File[] files = in.listFiles();
      if (files.length == 0) {
        if (!outDir.exists()) {
          outDir.mkdirs();
        }
      } else {
        for (File f : files) {
          if(f.isDirectory()) {
            copyRecursivelyWithFolderStructure(f, new File(outDir, f.getName()), replaceFiles);
          } else {
            copyRecursivelyWithFolderStructure(f, outDir, replaceFiles);
          }
        }
      }
    } else {
      File outFile = new File(outDir, in.getName());
      if(replaceFiles || !outFile.exists()) {
        if (!outFile.exists()) {
          outDir.mkdirs();
          try {
            outFile.createNewFile();
          } catch (IOException e) {
            throw new Ex_FileAccessException(outFile.getAbsolutePath(), e);
          }
        }
        FileUtils.copyFile(in, outFile);
      }
    }
  }
}
