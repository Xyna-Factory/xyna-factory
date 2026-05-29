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
package xact.ssh.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.Cipher;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.mac.Mac;
import org.apache.sshd.common.signature.Signature;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.ServerBuilder;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.apache.sshd.server.subsystem.SubsystemFactory;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPv6ConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_NetworkInterfaceNotFoundException;
import com.gip.xyna.xact.trigger.NetworkInterfaceUtils;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import xact.ssh.server.auth.ClientKeyStorable;
import xact.ssh.server.auth.ClientPasswordStorable;
import xact.ssh.server.auth.XynaAuthenticator;
import xact.ssh.sftp.SFTPSubsystemParameter;
import xact.ssh.sftp.XynaBackedFileProvider;
import xact.ssh.sftp.filesystem.XynaFilterDelegatingFileSystem;

public class XynaSSHServer {

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaSSHServer.class);

  private SshServer sshd;
  private ODS ods;

  private Collection<ClientKeyStorable> clientkeys = null;
  private Collection<ClientPasswordStorable> clientpasswords = null;

  private HostKey hostKey;

  private XynaBackedFileProvider xbfp;
  private XynaAuthenticator auth;

  public XynaSSHServer() {
  }

  @SuppressWarnings("unchecked")
  public void init(SSHServerParameter sp, SFTPSubsystemParameter sftp, XynaBackedFileProvider xbfp) {

    this.xbfp = xbfp;

    initDB();

    sshd = SshServer.setUpDefaultServer();

    sshd.setSignatureFactories(
        (List<NamedFactory<Signature>>) (List<?>) NamedFactory.setUpBuiltinFactories(true, sp.getAuthAlgoFactories()));
    sshd.setKeyExchangeFactories(NamedFactory.setUpTransformedFactories(true,
        sp.getKexFactories(), ServerBuilder.DH2KEX));

    sshd.setMacFactories((List<NamedFactory<Mac>>) (List<?>) NamedFactory.setUpBuiltinFactories(true, sp.getMacFactories()));
    sshd.setCipherFactories(
        (List<NamedFactory<Cipher>>) (List<?>) NamedFactory.setUpBuiltinFactories(true, sp.getCipherFactories()));

    boolean success = false;

    try {
      SimpleGeneratorHostKeyProvider hkp = new SimpleGeneratorHostKeyProvider(Path.of(sp.getHostKeyFilename()));
      hkp.setAlgorithm(sp.getHostkeyAlgorithm());
      hkp.setKeySize(sp.getHostkeySize());

      sshd.setPort(sp.getPort());
      sshd.setHost(sp.getHost());
      sshd.setKeyPairProvider(hkp);

      boolean alwaysAuthenticated = sp.getAlwaysAuth();
      boolean useOTC = sp.getOTCAuth();

      List<UserAuthFactory> userAuthFactories = new ArrayList<>();
      if (sp.getPublicKeyAuth())
        userAuthFactories.add(UserAuthPublicKeyFactory.INSTANCE);
      if (sp.getPasswordAuth())
        userAuthFactories.add(UserAuthPasswordFactory.INSTANCE);

      if (!sp.getPublicKeyAuth() && !sp.getPasswordAuth())
        userAuthFactories.add(UserAuthNoneFactory.INSTANCE);

      sshd.setUserAuthFactories(userAuthFactories);

      initSFTPSubsystem(sftp);

      sshd.setUserAuthFactories(userAuthFactories);

      loadFromDB();

      auth = new XynaAuthenticator(new HashMap<String, String>(), new HashMap<String, String>(),
          alwaysAuthenticated, useOTC, logger);

      if (clientkeys != null) {
        for (ClientKeyStorable s : clientkeys) {
          auth.addUserKey(s.getName(), s.getPublickey());
        }
      }

      if (clientpasswords != null) {
        for (ClientPasswordStorable s : clientpasswords) {
          auth.addUserPassword(s.getUsername(), s.getPassword());
        }
      }

      sshd.setPublickeyAuthenticator(auth);
      sshd.setPasswordAuthenticator(auth);

      sshd.getProperties().put(CoreModuleProperties.IDLE_TIMEOUT.getName(), sp.getIdleTimeout().getDurationInMillis());

      success = true;
    } finally {
      try {
        if (!success) {
          sshd.stop(true);
        }
      } catch (IOException e) {
        logger.warn("Error while stopping trigger", e);
      }
    }

  }

  private void initSFTPSubsystem(SFTPSubsystemParameter sp) {
    if (sp.isEnableSFTP()) {
      sshd.setSubsystemFactories(Arrays.asList(new org.apache.sshd.sftp.server.SftpSubsystemFactory()));
    }
    if (sp.isEnableSCP()) {
      sshd.setCommandFactory(new org.apache.sshd.scp.server.ScpCommandFactory());
    }
    if (sp.isFileAccess()) {
      sshd.setFileSystemFactory(
          new XynaFilterDelegatingFileSystem.Factory(xbfp, sp.getFileRoot(), sp.getFilePrefix()));
    } else {
      sshd.setFileSystemFactory(new XynaFilterDelegatingFileSystem.Factory(xbfp));
    }
  }

  private void loadFromDB() {
    try {
      clientkeys = loadClientKeyEntries();
      clientpasswords = loadUserPasswordEntries();
    } catch (PersistenceLayerException e) {
      logger.warn("SFTPTrigger: Problems loading from Persistencelayer: ", e);
    }
  }

  public Collection<ClientKeyStorable> loadClientKeyEntries() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(ClientKeyStorable.class);
    } finally {
      connection.closeConnection();
    }
  }

  public Collection<ClientPasswordStorable> loadUserPasswordEntries() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(ClientPasswordStorable.class);
    } finally {
      connection.closeConnection();
    }
  }

  private void initDB() {
    ods = ODSImpl.getInstance(true);
    try {
      ods.registerStorable(ClientKeyStorable.class);
      ods.registerStorable(ClientPasswordStorable.class);
    } catch (Exception e) {
      logger.warn("XynaSSHServer: InitDB failed: ", e);
    }
  }

  public InetAddress getIP(String host) throws XACT_InterfaceNoIPv6ConfiguredException,
      XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException, UnknownHostException {

    if (host == null || host.equals("")) {
      return InetAddress.getByName("0.0.0.0");
    }

    InternetAddressBean iab = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
        .getNetworkConfigurationManagement()
        .getInternetAddress(host, null);
    if (iab != null) {
      return iab.getInetAddress();
    }
    if (logger.isInfoEnabled()) {
      logger.info("address " + host + " unknown in network configuration management.");
    }
    // else: abwärtskompatibel:
    boolean ipv6 = false;
    boolean useLocalAddresses = false;
    return NetworkInterfaceUtils.getFirstIpAddressByInterfaceName(host, ipv6, useLocalAddresses);

  }

  public void start() throws IOException {
    sshd.start();
  }

  public void stop(boolean immediately) throws IOException {
    sshd.stop(immediately);
  }

  public void setSubsystemFactories(List<? extends SubsystemFactory> subsystemFactories) {
    sshd.setSubsystemFactories(subsystemFactories);
  }

  public void setShellFactory(ShellFactory shellFactory) {
    sshd.setShellFactory(shellFactory);
  }

  public SshServer getSshServer() {
    return sshd;
  }

  public HostKey getHostKey() {
    if (hostKey == null) {
      try {
        hostKey = new HostKey(sshd.getKeyPairProvider().loadKey(null, KeyPairProvider.SSH_RSA).getPublic());
      } catch (Exception e) {
        logger.warn("Couldn't get SSH-HostKey:" + e.toString());
      }
    }
    return hostKey;
  }

  public boolean addOneTimeCredentials(String user, String password, String expectedIp, String expectedPort) {
    return auth.addOneTimeCredentials(user, password, expectedIp, expectedPort);
  }

}
