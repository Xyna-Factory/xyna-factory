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
package xmcp.gitintegration.impl;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.Set;

import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import xmcp.gitintegration.storage.UserManagementStorage;



public class RepositoryCredentialsManagement {

  private static final Set<String> protocols = Set.of("ssh", "git", "http", "https", "ftp", "ftps", "file");


  public XynaRepoCredentials createCreds(String user, String path, String repositoryUsername) throws Exception {
    UserManagementStorage storage = new UserManagementStorage();
    String password = storage.loadPassword(user, path);
    String privateKey = storage.loadPrivateKey(user, path);
    String passphrase = storage.loadPassphrase(user, path);
    UsernamePasswordCredentialsProvider userNamePw = password == null ? null : new UsernamePasswordCredentialsProvider(repositoryUsername, password);
    SshTransportConfigCallback sshCallback = new SshTransportConfigCallback(privateKey, passphrase);
    return new XynaRepoCredentials(userNamePw, sshCallback);
  }


  public void addCredentialsToCommand(TransportCommand<?, ?> cmd, Repository repository, XynaRepoCredentials creds) {
    if (creds == null) {
      return;
    }

    String url = getRemoteOriginUrl(repository);
    String protocol = determineProtocol(url);
    if ("https".equals(protocol)) {
      cmd.setCredentialsProvider(creds.userNamePwProvider);
    } else if ("ssh".equals(protocol)) {
      cmd.setTransportConfigCallback(creds.sshCallback);
    }
  }


  private String determineProtocol(String url) {
    String protocol = url.substring(0, url.indexOf(":"));
    if (protocols.contains(protocol)) {
      return protocol;
    }

    return url.contains(":") ? "ssh" : "file";
  }


  private String getRemoteOriginUrl(Repository repository) {
    return repository.getConfig().getString("remote", "origin", "url");
  }


  private static class SshTransportConfigCallback implements TransportConfigCallback {

    private Iterable<KeyPair> loadKeyPairs(String privateKeyContent, String passphrase) {
      Iterable<KeyPair> keyPairs;
      try {
        FilePasswordProvider provider = (session, resourceKey, retryIndex) -> passphrase;
        InputStream stream = new ByteArrayInputStream(privateKeyContent.getBytes());
        keyPairs = SecurityUtils.loadKeyPairIdentities(null, null, stream, provider);
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to load ssh key pair", e);
      }
      return keyPairs;
    }


    private Iterable<KeyPair> keyPairs;
    private SshSessionFactory factory;


    public SshTransportConfigCallback(String privateKeyContent, String passphrase) {
      if (privateKeyContent == null) { return; }
      keyPairs = loadKeyPairs(privateKeyContent, passphrase);
      factory = new SshdSessionFactoryBuilder()
          .setPreferredAuthentications("publickey")
          .setDefaultKeysProvider(x -> keyPairs)
          .setHomeDirectory(FS.DETECTED.userHome())
          .setSshDirectory(new File(FS.DETECTED.userHome(), "/.ssh"))
          .build(null);
    }


    @Override
    public void configure(Transport transport) {
      SshTransport sshTransport = (SshTransport) transport;
      sshTransport.setSshSessionFactory(factory);
    }
  }
  
  public static class XynaRepoCredentials {

    private UsernamePasswordCredentialsProvider userNamePwProvider;
    private SshTransportConfigCallback sshCallback;


    private XynaRepoCredentials(UsernamePasswordCredentialsProvider userNamePwProvider, SshTransportConfigCallback sshCallback) {
      this.userNamePwProvider = userNamePwProvider;
      this.sshCallback = sshCallback;
    }
  }
}
