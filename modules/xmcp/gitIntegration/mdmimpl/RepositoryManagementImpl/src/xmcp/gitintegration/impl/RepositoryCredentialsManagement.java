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



import java.io.File;
import java.util.Set;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;



public class RepositoryCredentialsManagement {

  private SshTransportConfigCallback sshTransportConfigCallback;

  private Set<String> protocols = Set.of("ssh", "git", "http", "https", "ftp", "ftps", "file");


  public void addCredentialsToCommand(TransportCommand<?, ?> cmd, Repository repository, CredentialsProvider creds) {
    if (creds == null) {
      return;
    }

    String url = getRemoteOriginUrl(repository);
    String protocol = determineProtocol(url);
    if ("https".equals(protocol)) {
      cmd.setCredentialsProvider(creds);
    } else if ("ssh".equals(protocol)) {
      cmd.setTransportConfigCallback(getSshTransportConfigCallback());
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


  private SshTransportConfigCallback getSshTransportConfigCallback() {
    if (sshTransportConfigCallback == null) {
      sshTransportConfigCallback = new SshTransportConfigCallback();
    }
    return sshTransportConfigCallback;
  }


  private static class SshTransportConfigCallback implements TransportConfigCallback {

    private SshSessionFactory f = new SshdSessionFactoryBuilder().setPreferredAuthentications("publickey")
        .setHomeDirectory(FS.DETECTED.userHome()).setSshDirectory(new File(FS.DETECTED.userHome(), "/.ssh")).build(null);


    @Override
    public void configure(Transport transport) {
      SshTransport sshTransport = (SshTransport) transport;
      sshTransport.setSshSessionFactory(f);
    }

  }

}
