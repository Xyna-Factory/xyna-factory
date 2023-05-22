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

package com.gip.xyna.xdev.xlibdev.supp4eclipse.fileprovider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;



public class EclipseProjectTemplateFileProvider extends FunctionGroup {

  public static final String DEFAULT_NAME = "EclipseProjectTemplateFileProvider";

  private volatile boolean shuttingDown = false;


  public EclipseProjectTemplateFileProvider() throws XynaException {
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
  }


  public void shutdown() {
    shuttingDown = true;
  }


  public boolean isShuttingDown() {
    return shuttingDown;
  }

  public static class DeleteAfterCloseFileInputStream extends FileInputStream {

    private File f;
    
    public DeleteAfterCloseFileInputStream(File f) throws FileNotFoundException {
      super(f);
      this.f = f;
    }

    @Override
    public void close() throws IOException {
      try { 
        super.close();
      } finally {
        if (f != null) {          
          if (!FileUtils.deleteFileWithRetries(f)) {
            throw new IOException("could not delete File " + f.getAbsolutePath() + " on closing stream.");
          } 
          f = null;
        }
      }
    }
    
  }
  

  /**
   * zipped den inhalt des verzeichnisses @tempdir, packt ihn in @targetZipFile und sorgt daf�r, dass
   * das file gel�scht wird, wenn es nicht mehr ben�tigt wird (falls deleteZipAfterStreamClose = true).
   * d.h. falls der zur�ckgegebene inputstream geclosed wird.
   * @return inputstream auf das erstellte zipfile 
   */
  public InputStream getHandledInputStreamFromFile(File targetZipFile, File tempdir,
                                                   boolean deleteZipAfterStreamClose) throws Ex_FileAccessException {
    InputStream result = null;

    String targetPrefix;
    int lastIndexOfDot = targetZipFile.getName().lastIndexOf(".");
    if (lastIndexOfDot > 0) {
      targetPrefix = targetZipFile.getName().substring(0, lastIndexOfDot);
    } else {
      targetPrefix = targetZipFile.getName();
    }
    FileUtils.zipDirectory(targetZipFile, tempdir, targetPrefix);
    boolean exceptionAfterZipCreation = true;
    try {
      if (deleteZipAfterStreamClose) {
        result = new DeleteAfterCloseFileInputStream(targetZipFile);
      } else {
        result = new FileInputStream(targetZipFile);
      }
      exceptionAfterZipCreation = false;
    } catch (FileNotFoundException e) {
      throw new Ex_FileWriteException(targetZipFile.getAbsolutePath(), e);
    } finally {
      if (exceptionAfterZipCreation && targetZipFile.exists()) {
        if (!FileUtils.deleteFileWithRetries(targetZipFile)) {
          logger.warn("file " + targetZipFile.getAbsolutePath() + " could not be deleted");
        }
      }
    }

    return result;
  }

}
