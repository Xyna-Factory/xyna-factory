/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPassword;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import xact.ssh.EncryptionType;
import xact.ssh.HostKeyStorableRepository;
import xact.ssh.IdentityStorableRepository;
import xact.ssh.SupportedHostNameFeature;
import xact.ssh.XynaHostKeyRepository;
import xact.ssh.XynaIdentityRepository;


public class NetConfNotificationReceiverCredentials {

  public static enum AuthMethodName {
    DEFAULT, PUBLICKEY, PASSWORD, HOSTBASED
  }

  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverTriggerConnection.class);
  
  private BasicCredentials basicCred;
  protected XynaIdentityRepository idRepo;
    
  private static final XynaPropertyBuilds<Set<SupportedHostNameFeature>> supportedFeatures =
    new XynaPropertyBuilds<Set<SupportedHostNameFeature>>(
      "xact.ssh.hostkeys.supportedfeatures",
      new XynaPropertyBuilds.Builder<Set<SupportedHostNameFeature>>() {

       public Set<SupportedHostNameFeature> fromString(String arg0)
                       throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
         return SupportedHostNameFeature.fromStringList(arg0);
       }

       public String toString(Set<SupportedHostNameFeature> arg0) {
         StringBuilder sb = new StringBuilder();
         Iterator<SupportedHostNameFeature> iter = arg0.iterator();
         while (iter.hasNext()) {
           sb.append(iter.next().toString());
           if (iter.hasNext()) {
             sb.append(", ");
           }
         }
         return sb.toString();
       }

      },
      SupportedHostNameFeature.all())
    .setDefaultDocumentation(DocumentationLanguage.EN,
                             "Supported features for the HostKeyRepository, turning features off can improve performance");

  
  public NetConfNotificationReceiverCredentials(BasicCredentials basicCred) {
    this.basicCred = basicCred;
  }


  private Optional<String> getAlgoType(String socket_host, int socket_port) {
    Optional<String> algoTypeOpt = Optional.empty();
    int port = socket_port;
    String hostname = socket_host;
    if (logger.isDebugEnabled()) {
      logger.debug("NetConfNotificationReceiver: getAlgoType: " + socket_host + " " + socket_port);
    }
    XynaHostKeyRepository hostRepo = new HostKeyStorableRepository(supportedFeatures.get());
    List<String> algoList = hostRepo.findExistingAlgorithms(hostname, port);

    if (algoList.size() > 0) {
      boolean univariate = true;
      String firstElement = algoList.get(0).trim();
      EncryptionType encryFirstElement = EncryptionType.getBySshStringRepresentation(firstElement);
      for (Iterator<String> iter = algoList.iterator(); iter.hasNext();) {
        String element = iter.next().trim();
        if (!element.equalsIgnoreCase(firstElement)) {
          univariate = false;
        }
      }
      if (univariate) {
        algoTypeOpt = Optional.ofNullable(encryFirstElement.getStringRepresentation());
      }
    }
    return algoTypeOpt;
  }


  private Collection<KeyProvider> generateKeyProvider(String socket_host, int socket_port) {
    List<KeyProvider> kpl = new ArrayList<KeyProvider>();
    kpl = idRepo.getKey(null, getAlgoType(socket_host, socket_port));
    if (logger.isDebugEnabled()) {
      logger.debug("NetConfNotificationReceiver: generateKeyProvider - keys: " + kpl.size());
    }
    return kpl;
  }


  protected SSHClient initSSHClient() {
    SSHClient client = new SSHClient();
    XynaHostKeyRepository hostRepo = new HostKeyStorableRepository(supportedFeatures.get());
    client.addHostKeyVerifier(hostRepo);

    client.getTransport().getConfig().setKeyAlgorithms(SshjKeyAlgorithm.extractFactories(basicCred.getKeyAlgorithms()));
    client.getTransport().getConfig().setMACFactories(SshjMacFactory.extractFactories(basicCred.getMacFactories()));
    
    //Repair: protected XynaIdentityRepository idRepo
    idRepo = new IdentityStorableRepository(client.getTransport().getConfig());
    if (logger.isDebugEnabled()) {
      logger.debug("NetConfNotificationReceiver: initSSHClient");
    }
    return client;
  }


  public Collection<AuthMethod> convertAuthMethod(AuthMethodName method, String socket_host, int socket_port) {
    String netconf_password = basicCred.getPassword();
    Collection<AuthMethod> aMethodResponse = new ArrayList<AuthMethod>();
    aMethodResponse.add(new net.schmizz.sshj.userauth.method.AuthNone());

    if (method == AuthMethodName.PASSWORD) {
      if (netconf_password != null) {
        Collection<AuthMethod> addMethodPassword = Collections.singleton(new AuthPassword(
          new PasswordFinder() {
            public boolean shouldRetry(Resource<?> resource) {
              return false;
            }

            public char[] reqPassword(Resource<?> resource) {
              return netconf_password.toCharArray();
            }
          }
        ));

        aMethodResponse.addAll(addMethodPassword);
        return aMethodResponse;

      } else {
        throw new IllegalArgumentException("AuthenticationMethod without necessary credentials '" + method.toString() + "'.");
      }
    } else if (method == AuthMethodName.HOSTBASED) {
      throw new IllegalArgumentException("AuthenticationMethod disabled (security) '" + method.toString() + "'.");
    } else if (method == AuthMethodName.PUBLICKEY) {
      if (logger.isDebugEnabled()) {
        logger.debug("NetConfNotificationReceiver: convertAuthMethod - PUBLICKEY: " + socket_host + " " + socket_port);
      }
      Collection<KeyProvider> keys = generateKeyProvider(socket_host, socket_port);
      Collection<AuthMethod> addMethodKey = keys.stream().map(AuthPublickey::new).collect(Collectors.toList());
      aMethodResponse.addAll(addMethodKey);
      if (logger.isDebugEnabled()) {
        logger.debug( "NetConfNotificationReceiver: convertAuthMethod - PUBLICKEY: " + aMethodResponse.size());
      }
      return aMethodResponse;
    } else {
      throw new IllegalArgumentException("Unknown AuthenticationMethod '" + method.toString() + "'.");
    }
  }


  public void injectHostKeyHash(String socket_host, String hostkeyAlias) {
    if ((hostkeyAlias != null) && (!hostkeyAlias.isEmpty())) {
      HostKeyStorableRepository tmpHostRepo = new HostKeyStorableRepository(supportedFeatures.get());
      tmpHostRepo.injectHostKey(hostkeyAlias);
      if (logger.isDebugEnabled()) {
        logger.debug("SSHConnectionInstanceOperationImpl prepAuthentification - conParams.getHostKeyAlias():" +
                     xact.ssh.HostKeyHashMap.getNumberOfKeys(hostkeyAlias));
      }
    } else {
      HostKeyStorableRepository tmpHostRepo = new HostKeyStorableRepository(supportedFeatures.get());
      tmpHostRepo.injectHostKey(socket_host);
    }
  }

}
