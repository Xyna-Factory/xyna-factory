/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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



import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;
import com.gip.xyna.xmcp.xfcli.CLIRegistry;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import xact.connection.ManagedConnection;
import xact.ssh.EncryptionAlgorithmType;
import xact.ssh.EncryptionType;
import xact.ssh.KeyAttributes;
import xact.ssh.KeyFileName;
import xact.ssh.KeyPair;
import xact.ssh.KeyPairGenerationParameter;
import xact.ssh.KnownHost;
import xact.ssh.PassPhrase;
import xact.ssh.SSHConnectionManagementServiceOperation;
import xact.ssh.SSHConnectionParameter;
import xact.ssh.SSHNETCONFConnection;
import xact.ssh.SSHShellConnection;
import xact.ssh.cli.generated.OverallInformationProvider;
import xact.templates.CommandLineInterface;
import xact.templates.DocumentType;
import xact.templates.NETCONF;



public class SSHConnectionManagementServiceOperationImpl implements ExtendedDeploymentTask, SSHConnectionManagementServiceOperation {

  private final static Logger logger = CentralFactoryLogging.getLogger(SSHConnectionManagementServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    SSHConnectionManagementRepositoryAccess.init();
    List<Class<? extends AXynaCommand>> commands;
    try {
      commands = OverallInformationProvider.getCommands();
      for (Class<? extends AXynaCommand> command : commands) {
        CLIRegistry.getInstance().registerCLICommand(command);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not register cli commands.", e);
    }
  }


  public void onUndeployment() throws XynaException {
    SSHConnectionManagementRepositoryAccess.shutdown();
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.;
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public void addKnownHost(KnownHost knownHost) {
    EncryptionType type = null;
    if (knownHost.getType() != null) {
      type = EncryptionType.getByXynaFqClassNamen(knownHost.getType().getClass().getName());
    }
    SSHConnectionManagementRepositoryAccess.addKnownHost(knownHost.getName(), type, knownHost.getKey(), knownHost.getComment());
  }


  public void exportKnownHost(KnownHost knownHost, KeyFileName keyFileName) {
    SSHConnectionManagementRepositoryAccess.exportKnownHost(knownHost.getName(),
                                                            EncryptionType.getByXynaFqClassNamen(knownHost.getType().getClass().getName()),
                                                            keyFileName.getName());
  }


  public void generateKeyPair(KeyPairGenerationParameter kpgp) {
    EncryptionType type = EncryptionType.getByXynaFqClassNamen(kpgp.getType().getClass().getName());
    KeyAttributes keyAttributes = kpgp.getKeyAttributes();
    String identity = "";
    String typeclass = "";
    long priority = 0;
    if (keyAttributes != null) {
      if ((keyAttributes.getIdentity() != null) && (! keyAttributes.getIdentity().isBlank())) {
        identity = keyAttributes.getIdentity();
      }
      if (keyAttributes.getPriority() != 0) {
        priority = keyAttributes.getPriority();
      }
      if ((keyAttributes.getTypeclass() != null) && (! keyAttributes.getTypeclass().isBlank())) {
        typeclass = keyAttributes.getTypeclass();
      }
    }
    SSHConnectionManagementRepositoryAccess.generateKeyPair(type, kpgp.getKeySize(), kpgp.getPassPhrase().getContent(),
                                                            kpgp.getOverwriteExisting(), identity, priority, typeclass);
  }


  @Deprecated
  public ManagedConnection getConnection(SSHConnectionParameter sSHConnectionParameter, DocumentType type) {
      //Preservation of the Connection-App
      //throws GenericConnectionException {
    //wird genauso auch in SSHConnectionParameter.connect(..) aufgerufen
    ManagedConnection connection;
    if (type instanceof CommandLineInterface) {
      connection = new SSHShellConnection(sSHConnectionParameter, null);
    } else if (type instanceof NETCONF) {
      connection = new SSHNETCONFConnection(sSHConnectionParameter, null);
    } else {
      throw new RuntimeException("Unexpected DocumentType: " + type);
    }
    
    try {
      connection.connect();
    } catch (Exception sshE) {
      logger.warn("Error (SSHException) in getConnection",sshE);
      throw new RuntimeException(sshE);
    }
    
    return connection;
  }


  public List<? extends KeyPair> getPublicKey(EncryptionAlgorithmType encryptionAlgorithmType) {
    EncryptionType type = null;
    if (encryptionAlgorithmType != null) {
      type = EncryptionType.getByXynaFqClassNamen(encryptionAlgorithmType.getClass().getName());
    }
    List<KeyPair> keys = new ArrayList<KeyPair>();
    keys = SSHConnectionManagementRepositoryAccess.getPublicKey(type);
    return keys;
  }


  public void importKnownHosts(KeyFileName keyFileName) {
    SSHConnectionManagementRepositoryAccess.importKnownHosts(keyFileName.getName());
  }


  public void removeKnownHost(KnownHost knownHost) {
    EncryptionType type = null;
    if (knownHost.getType() != null) {
      EncryptionType.getByXynaFqClassNamen(knownHost.getType().getClass().getName());
    }
    SSHConnectionManagementRepositoryAccess.removeKnownHost(knownHost.getName(), knownHost.getKey(), type);
  }


  public void returnConnection(ManagedConnection connection) { //throws GenericConnectionException {
    connection.disconnect();
  }


  public void addKeyFiles(KeyFileName publicKeyFileName, KeyFileName privateKeyFileName, PassPhrase passPhrase) {
    SSHConnectionManagementRepositoryAccess.addKeyFiles(publicKeyFileName.getName(), privateKeyFileName.getName(), passPhrase.getContent(),"",0,"");
  }


  public void addKeyPair(KeyPair keyPair) {
    KeyAttributes keyAttributes = keyPair.getKeyAttributes();
    String identity = "";
    String typeclass = "";
    long priority = 0;
    if (keyAttributes != null) {
      if ((keyAttributes.getIdentity() != null) && (! keyAttributes.getIdentity().isBlank())) {
        identity = keyAttributes.getIdentity();
      }
      if (keyAttributes.getPriority() != 0) {
        priority = keyAttributes.getPriority();
      }
      if ((keyAttributes.getTypeclass() != null) && (! keyAttributes.getTypeclass().isBlank())) {
        typeclass = keyAttributes.getTypeclass();
      }
    }
    SSHConnectionManagementRepositoryAccess.addKeyPair(keyPair.getPrivateKey(), keyPair.getPublicKey(),
                                                       keyPair.getPassPhrase().getContent(), identity, priority, typeclass);
  }


  public void modifyKeypair0(KeyAttributes oldkeyAttributes, KeyAttributes newkeyAttributes) {
    String identity = "";
    String typeclass = null;
    long priority = 0;
    if ((newkeyAttributes.getIdentity() != null) && (! newkeyAttributes.getIdentity().isBlank())) {
      identity = newkeyAttributes.getIdentity();
    }
    if (newkeyAttributes.getPriority() != 0) {
      priority = newkeyAttributes.getPriority();
    }
    if ((newkeyAttributes.getTypeclass() != null)) { //&& (! newkeyAttributes.getTypeclass().isBlank())) {
      typeclass = newkeyAttributes.getTypeclass();
    }
    SSHConnectionManagementRepositoryAccess.modifyKeyPair(oldkeyAttributes.getIdentity(), identity, priority, typeclass);
  }


  public void addKeyFilesWithKeyAttributes(KeyFileName publicKeyFileName, KeyFileName privateKeyFileName, PassPhrase passPhrase, KeyAttributes keyAttributes) {
    String identity = "";
    String typeclass = "";
    long priority = 0;
    if ((keyAttributes.getIdentity() != null) && (! keyAttributes.getIdentity().isBlank())) {
      identity = keyAttributes.getIdentity();
    }
    if (keyAttributes.getPriority() != 0) {
      priority = keyAttributes.getPriority();
    }
    if ((keyAttributes.getTypeclass() != null) && (! keyAttributes.getTypeclass().isBlank())) {
      typeclass = keyAttributes.getTypeclass();
    }
    SSHConnectionManagementRepositoryAccess.addKeyFiles(publicKeyFileName.getName(), privateKeyFileName.getName(), passPhrase.getContent(), identity, priority, typeclass);
  }

}
