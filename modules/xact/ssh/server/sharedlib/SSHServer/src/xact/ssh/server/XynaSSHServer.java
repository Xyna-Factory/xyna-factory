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
package xact.ssh.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

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



public class XynaSSHServer {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaSSHServer.class);
  
  private SshServer sshd;
  private ODS ods;
  
  private Collection<ClientKeyStorable> clientkeys = null;
  private Collection<ClientPasswordStorable> clientpasswords = null;

  private HostKey hostKey;

  public XynaSSHServer() {
  }
  
  
  public void init(SSHServerParameter sp) {
    
    initDB();
    
    sshd = SshServer.setUpDefaultServer();
    boolean success = false;
    
    try {
      SimpleGeneratorHostKeyProvider hkp =  new SimpleGeneratorHostKeyProvider(new File(sp.getHostKeyFilename()) );
      hkp.setAlgorithm(sp.getAlgorithm()); 
      
      sshd.setPort(sp.getPort());
      sshd.setHost(sp.getHost());
      sshd.setKeyPairProvider(hkp);
      
      sshd.setNioWorkers(2); //FIXME
  
      List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
      boolean alwaysAuthenticated = false;
      switch( sp.getAuth() ) {
      case both:
        userAuthFactories.add(new UserAuthPublicKeyFactory());
        userAuthFactories.add(new UserAuthPasswordFactory());
        break;
      case needless:
        userAuthFactories.add(new UserAuthNoneFactory());
        alwaysAuthenticated = true;
        break;
      case password:
        userAuthFactories.add(new UserAuthPasswordFactory());
        break;
      case publickey:
        userAuthFactories.add(new UserAuthPublicKeyFactory());
        break;
      default:
        userAuthFactories.add(new UserAuthNoneFactory());
        break;
      }
  
      sshd.setUserAuthFactories(userAuthFactories);

      loadFromDB();
  
      XynaAuthenticator auth = new XynaAuthenticator(new HashMap<String, String>(), new HashMap<String, String>(), alwaysAuthenticated, logger);
  
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
      
      sshd.getProperties().put(SshServer.IDLE_TIMEOUT, sp.getIdleTimeout().getDurationInMillis() );
      
      
      success = true;
    } finally {
      try {
        if (!success) {
          sshd.stop(true);
        }
      } catch (IOException e) {
        logger.warn("Error while stopping trigger",e);
      }
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

    InternetAddressBean iab =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
        .getInternetAddress(host, null);
    if (iab != null) {
      return iab.getInetAddress();
    }
    if (logger.isInfoEnabled()) {
      logger.info("address " + host + " unknown in network configuration management.");
    }   
    //else: abw�rtskompatibel:
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


  public void setSubsystemFactories(List<NamedFactory<Command>> subsystemFactories) {
    sshd.setSubsystemFactories(subsystemFactories);
  }
  
  public void setShellFactory(Factory<Command> shellFactory) {
    sshd.setShellFactory(shellFactory);
  }
  
  public SshServer getSshServer() {
    return sshd;
  }

  public HostKey getHostKey() {
    if( hostKey == null ) {
      hostKey = new HostKey(sshd.getKeyPairProvider().loadKey(KeyPairProvider.SSH_RSA).getPublic());
    }
    return hostKey;
  }
  
}
