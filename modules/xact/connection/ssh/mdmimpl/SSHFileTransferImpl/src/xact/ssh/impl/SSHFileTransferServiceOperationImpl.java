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
package xact.ssh.impl;



import org.apache.log4j.Logger;

import xact.ssh.SSHFileTransferServiceOperation;
import base.Credentials;
import base.File;
import base.Host;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;



/*
Remarks:
The implementation of the SSHFileTransfer reproduces the features of the JSCH implementation. Was this method "sCPToRemoteHost" (in this form) used in a project?
Is there a desire to extend this method (e.g. XynaHostKeyRepository, XynaIdentityRepository, ...)?
*/

public class SSHFileTransferServiceOperationImpl implements ExtendedDeploymentTask, SSHFileTransferServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(SSHFileTransferServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input
    // parameter.
  }


  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input
    // parameter.
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty
    // xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if
    // this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted,
    // while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued
    // in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be
    // continued after calling Thread.stop on the thread.;
    // executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public void sCPToRemoteHost(Host host, Credentials credentials, File localFile, File remoteFile) {

    try {
      final SSHClient ssh = new SSHClient();

      String remotePath = remoteFile.getPath();
      String localPath = localFile.getPath();

      // Might be adjusted
      String identity = credentials.getUsername() + "@" + host.getHostname();
      Credentials usedCredentials = getCredentials(identity, credentials);

      // Optional: "useCompression"
      ssh.useCompression();

      ssh.addHostKeyVerifier(new PromiscuousVerifier());

      ssh.connect(host.getHostname());
      try {
        ssh.authPassword(usedCredentials.getUsername(), usedCredentials.getPassword());

        // Test: Workaround with Key
        // String testKey = "/tmp/keygen/key0";
        // File privateKey = new File(testKey);
        // KeyProvider keys = ssh.loadKeys(privateKey.getPath());
        // ssh.authPublickey(usedCredentials.getUsername(),keys);

        ssh.newSCPFileTransfer().upload(new FileSystemFile(localPath), remotePath);
        // ssh.newSCPFileTransfer().download(remotePath, new FileSystemFile(localPath));

      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        ssh.disconnect();
        ssh.close();
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  private Credentials getCredentials(String identity, Credentials credentials) {
    Credentials responseCredentials = credentials;
    SecureStorablePassphraseStore passphraseStore = new SecureStorablePassphraseStore();

    if (credentials.getPassword() != null && credentials.getPassword().length() > 0) {
      passphraseStore.store(identity, credentials.getPassword());
    } else {
      String retrievedPassword = passphraseStore.retrieve(identity);
      responseCredentials.setPassword(retrievedPassword);
    }
    return responseCredentials;
  }

}