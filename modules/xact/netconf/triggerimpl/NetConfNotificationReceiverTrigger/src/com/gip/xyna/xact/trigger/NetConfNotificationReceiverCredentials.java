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


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.hierynomus.sshj.transport.mac.Macs;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.mac.MAC;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;


public class NetConfNotificationReceiverCredentials {

  private static final XynaPropertyBuilds<Set<SupportedHostNameFeature>> supportedFeatures = 
      new XynaPropertyBuilds<Set<SupportedHostNameFeature>>("xact.ssh.hostkeys.supportedfeatures",
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
            .setDefaultDocumentation(DocumentationLanguage.EN, "Supported features for the HostKeyRepository, turning features off can improve performance");

  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverTriggerConnection.class);

  private static String netconf_username;
  private static String netconf_password;
  private static String netconf_HostKeyAuthenticationMode; //Hostkey_Modus: "leasetable", "direct", "none" (default)
  
  private static long netconf_replayinminutes;

  protected XynaIdentityRepository idRepo;


  NetConfNotificationReceiverCredentials() {
  }


  public static void setUserame(String username) {
    netconf_username = username;
  };


  public static void setPassword(String password) {
    netconf_password = password;
  };


  public static String getUserame() {
    return netconf_username;
  };


  public static String getPassword() {
    return netconf_password;
  };


  public static void setHostKeyAuthenticationMode(String authenticationmode) {
    netconf_HostKeyAuthenticationMode = authenticationmode;
  };


  public static String getHostKeyAuthenticationMode() {
    return netconf_HostKeyAuthenticationMode;
  };

  
  public static void setReplayInMinutes(long replayinminutes) {
    netconf_replayinminutes = replayinminutes;
  };


  public static long getReplayInMinutes() {
    return netconf_replayinminutes;
  };


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

    // Reduce valid KeyAlgorithms
    client.getTransport().getConfig()
        .setKeyAlgorithms(java.util.Arrays.<net.schmizz.sshj.common.Factory.Named<com.hierynomus.sshj.key.KeyAlgorithm>> asList(
                          com.hierynomus.sshj.key.KeyAlgorithms.SSHDSA(),
                          com.hierynomus.sshj.key.KeyAlgorithms.SSHRSA(),
                          // com.hierynomus.sshj.key.KeyAlgorithms.EdDSA25519CertV01(),
                          // com.hierynomus.sshj.key.KeyAlgorithms.EdDSA25519(),
                          // com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp521CertV01(),
                             com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp521(), //This KeyAlgorithm is necessary
                          // com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp384CertV01(),
                          // com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp384(),
                          // com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp256CertV01(),
                             com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp256(),
                             com.hierynomus.sshj.key.KeyAlgorithms.RSASHA512(),
                             com.hierynomus.sshj.key.KeyAlgorithms.RSASHA256()
                          // com.hierynomus.sshj.key.KeyAlgorithms.SSHRSACertV01(),
                          // com.hierynomus.sshj.key.KeyAlgorithms.SSHDSSCertV01(),
                          //   com.hierynomus.sshj.key.KeyAlgorithms.SSHRSA(),
                          //   com.hierynomus.sshj.key.KeyAlgorithms.SSHDSA()
                             ));
    
    //Change of order due to the specific FW of an RD.
    client.getTransport().getConfig()
        .setMACFactories(java.util.Arrays.<net.schmizz.sshj.common.Factory.Named<MAC>> asList(
                             Macs.HMACSHA2256(),
                             Macs.HMACSHA2256Etm(),
                             Macs.HMACSHA2512(),
                             Macs.HMACSHA2512Etm(),
                             Macs.HMACSHA1(),
                             Macs.HMACSHA1Etm(),
                             Macs.HMACSHA196(),
                             Macs.HMACSHA196Etm(),
                             Macs.HMACMD5(),
                             Macs.HMACMD5Etm(),
                             Macs.HMACMD596(),
                             Macs.HMACMD596Etm(),
                             Macs.HMACRIPEMD160(),
                             Macs.HMACRIPEMD160Etm(),
                             Macs.HMACRIPEMD16096(),
                             Macs.HMACRIPEMD160OpenSsh()));

    //Repair: protected XynaIdentityRepository idRepo
    idRepo = new IdentityStorableRepository(client.getTransport().getConfig());
    if (logger.isDebugEnabled()) {
      logger.debug("NetConfNotificationReceiver: initSSHClient");
    }
    return client;
  }


  public Collection<AuthMethod> convertAuthMethod(String method, String socket_host, int socket_port) {
    Collection<AuthMethod> aMethodResponse = new ArrayList<AuthMethod>();
    aMethodResponse.add(new net.schmizz.sshj.userauth.method.AuthNone());

    switch (method) {
      case "PASSWORD" :
        if (netconf_password != null) {
          
          if (logger.isDebugEnabled()) {
            logger.debug("NetConfNotificationReceiver: convertAuthMethod - PASSWORD: " + netconf_password);
          }
          Collection<AuthMethod> addMethodPassword = Collections.singleton(new AuthPassword(new PasswordFinder() {

            public boolean shouldRetry(Resource<?> resource) {
              return false;
            }


            public char[] reqPassword(Resource<?> resource) {
              return netconf_password.toCharArray();
            }
          }));

          aMethodResponse.addAll(addMethodPassword);
          return aMethodResponse;

        } else {
          throw new IllegalArgumentException("AuthenticationMethod without necessary credentials '" + method.toString() + "'.");
        }
      case "HOSTBASED" :
        throw new IllegalArgumentException("AuthenticationMethod disabled (security) '" + method.toString() + "'.");
      case "PUBLICKEY" :
        if (logger.isDebugEnabled()) {
          logger.debug("NetConfNotificationReceiver: convertAuthMethod - PUBLICKEY: " + socket_host + " " + socket_port);
        }
        Collection<KeyProvider> keys = generateKeyProvider(socket_host, socket_port);
        Collection<AuthMethod> addMethodKey = keys.stream().map(AuthPublickey::new).collect(Collectors.toList());
        aMethodResponse.addAll(addMethodKey);
        if (logger.isDebugEnabled()) {
          logger.debug( "NetConfNotificationReceiver: convertAuthMethod - PUBLICKEY: "+aMethodResponse.size());
        }
        return aMethodResponse;
      default :
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
