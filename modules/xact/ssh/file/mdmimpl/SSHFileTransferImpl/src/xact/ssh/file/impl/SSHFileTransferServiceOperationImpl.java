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
package xact.ssh.file.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import base.File;
import base.Text;
import xact.ssh.file.FileTransferInfo;
import xact.ssh.file.SSHFileTransferServiceOperation;
import xact.ssh.file.SSHServerParameter;
import xact.templates.Document;
import xact.templates.DocumentType;
import xfmg.xfctrl.filemgmt.ManagedFileId;


public class SSHFileTransferServiceOperationImpl implements ExtendedDeploymentTask, SSHFileTransferServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(SSHFileTransferServiceOperationImpl.class);

  
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

  @Override
  public Document scpDocumentFromRemoteHost(SSHServerParameter server, File remoteFile, DocumentType documentType) {
    Session session = null;
    try {
      session = getSession(server);
      Scp scp = new Scp(session, server);
      
      StringBuilder text = new StringBuilder();
      scp.copyFrom( remoteFile.getPath(), "UTF-8", text );
      
      return new Document(documentType, text.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if( session != null ) {
        session.disconnect();
      }
    }
  }

  @Override
  public void scpDocumentToRemoteHost(SSHServerParameter server, Document document, File remoteFile) {
    Session session = null;
    try {
      session = getSession(server);
      Scp scp = new Scp(session, server);
      scp.copyDocumentTo(document.getText(), "UTF-8", remoteFile.getPath()); //TODO charset
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if( session != null ) {
        session.disconnect();
      }
    }
  }

  @Override
  public Container scpFromRemoteHost(SSHServerParameter server, File remoteFile, Text location) {
    String localName = remoteFile.getPath(); //FIXME lokaler name übergebn?
    int idx = localName.lastIndexOf('/');
    if ( idx > 0 ) {
      localName = localName.substring(idx+1);
    }
    
    Triple<String, OutputStream, String> triple = getFileMgmt().store(location.getText(), remoteFile.getPath() );
    Session session = null;
    try {
      session = getSession(server);
      Scp scp = new Scp(session, server);
      FileTransferInfo info = scp.copyFrom( remoteFile.getPath(), triple.getSecond() );
      
      return new Container(new ManagedFileId(triple.getFirst()), info );
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if( session != null ) {
        session.disconnect();
      }
    }
  }

  @Override
  public FileTransferInfo scpToRemoteHost(SSHServerParameter server, ManagedFileId managedFileId, File remoteFile) {
    Session session = null;
    try {
      session = getSession(server);
      Scp scp = new Scp(session, server);
      TransientFile tf = getFileMgmt().retrieve(managedFileId.getId());
      
      return scp.copyTo(tf, remoteFile.getPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if( session != null ) {
        session.disconnect();
      }
    }
  }
  
  @Override
  public FileTransferInfo scpFileFromRemoteHost(SSHServerParameter server, File remoteFile, File localFile) {
    Session session = null;
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(localFile.getPath());
      session = getSession(server);
      Scp scp = new Scp(session, server);
      return scp.copyFrom( remoteFile.getPath(), fos );
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if( session != null ) {
        session.disconnect();
      }
      if( fos != null ) {
        try {
          fos.close();
        } catch( IOException e ) {
          logger.warn("Could not close "+localFile.getPath(), e);
        }
      }
    }
  }

  @Override
  public FileTransferInfo scpFileToRemoteHost(SSHServerParameter server, File localFile, File remoteFile) {
    Session session = null;
    try {
      session = getSession(server);
      Scp scp = new Scp(session, server);
      return scp.copyTo(localFile.getPath(), remoteFile.getPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if( session != null ) {
        session.disconnect();
      }
    }
  }


  private FileManagement getFileMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
  }
  
  private Session getSession(SSHServerParameter server) throws JSchException {
    JSch jsch = new JSch();

    int port = server.getPort() == null ? 22 : server.getPort().intValue();
    Session s = jsch.getSession(server.getUser(), server.getHost(), port);
    PassphraseRetrievingUserInfo userInfo =
        new PassphraseRetrievingUserInfo(new SecureStorablePassphraseStore(), new LogAdapter(logger) );
    s.setUserInfo(userInfo);
    s.setConfig("StrictHostKeyChecking", "no"); //FIXME sinnvoll?
    if (server.getPassword() != null && server.getPassword().length() > 0) {
      s.setPassword(server.getPassword());
      userInfo.setPassword(server.getPassword());
    }
    s.setConfig("PreferredAuthentications", "password,keyboard-interactive");
    if(server.getSCPTimeouts() != null && server.getSCPTimeouts().getConnectionTimeout() != null && server.getSCPTimeouts().getConnectionTimeout() > 0)
      s.connect(server.getSCPTimeouts().getConnectionTimeout());
    else
      s.connect();
    
    return s;
  }

}
