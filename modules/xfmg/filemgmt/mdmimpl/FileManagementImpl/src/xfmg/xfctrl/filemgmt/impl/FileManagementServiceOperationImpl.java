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
package xfmg.xfctrl.filemgmt.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;

import base.File;
import base.Text;
import xact.templates.Document;
import xact.templates.PlainText;
import xfmg.xfctrl.filemgmt.FileInfo;
import xfmg.xfctrl.filemgmt.FileManagementServiceOperation;
import xfmg.xfctrl.filemgmt.FileSize;
import xfmg.xfctrl.filemgmt.Location;
import xfmg.xfctrl.filemgmt.ManagedFileId;


public class FileManagementServiceOperationImpl implements ExtendedDeploymentTask, FileManagementServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public FileInfo getFileInfo(ManagedFileId managedFileId) {
    com.gip.xyna.xfmg.xfctrl.filemgmt.FileInfo fi = getFileMgmt().getFileInfo(managedFileId.getId());
    java.io.File f = new java.io.File(fi.getOriginalFilename());
    return new FileInfo().buildFileInfo().
        managedFileId(new ManagedFileId(fi.getId())).
        name(f.getName()).
        fileSize(new FileSize(fi.getSize())).
        location(new Location("")).
        instance();
  }

  public void retrieve(ManagedFileId managedFileId, File file) throws XynaException {
    TransientFile tFile = getFileMgmt().retrieve(managedFileId.getId());
    java.io.File realFile = new java.io.File(file.getPath());
    if (realFile.exists() && 
        realFile.isDirectory()) {
      realFile = new java.io.File(realFile, tFile.getOriginalFilename()); 
    }
    
    try {
      if (!realFile.exists()) {
        realFile.createNewFile();
      }
      InputStream in = tFile.openInputStream();
      try {
        OutputStream out = new FileOutputStream(realFile);
        try {
          StreamUtils.copy(in, out);
        } finally {
          out.close();
        }
      } finally {
        in.close();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to retrieve file", e);
    }
  }

  public Document retrieveAsDocument(ManagedFileId managedFileId) throws XynaException {
    TransientFile file = getFileMgmt().retrieve(managedFileId.getId());

    InputStream in = file.openInputStream();
    try {
      InputStreamReader is = new InputStreamReader(in, Constants.DEFAULT_ENCODING);
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(is);
      String read = br.readLine();
      
      while (read != null) {
        sb.append(read);
        sb.append("\n");
        read = br.readLine();
      }

      return new Document(new PlainText(), sb.toString());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file", e);
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        //logger.warn("Failed to close input stream", e);
      }
    }
  }

  public ManagedFileId store(File file, Text location) throws XynaException {
    java.io.File realFile = new java.io.File(file.getPath());
    try {
      FileInputStream fis = new FileInputStream(realFile);
      try {
        return  new ManagedFileId(getFileMgmt().store(location.getText(), realFile.getName(), fis));
      } finally {
        fis.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ManagedFileId storeDocument(Document document, Text filename, Text location) throws XynaException {
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(document.getText() == null ? new byte[0] : document.getText().getBytes(Constants.DEFAULT_ENCODING));
      try {
        return  new ManagedFileId(getFileMgmt().store(location.getText(), filename.getText(), bis));
      } finally {
        bis.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private FileManagement getFileMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
  }

}
