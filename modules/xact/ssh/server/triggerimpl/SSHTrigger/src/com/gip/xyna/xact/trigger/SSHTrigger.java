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
package com.gip.xyna.xact.trigger;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.sshd.server.shell.ShellFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.concurrent.FakedFuture;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.trigger.SFTPTriggerConnection;
import com.gip.xyna.xact.trigger.SSHStartParameter;
import com.gip.xyna.xact.trigger.SSHDTriggerConnection;
import com.gip.xyna.xact.trigger.SSHShellTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;

import xact.ssh.sftp.filesystem.RequestContext;
import xact.ssh.sftp.filesystem.XynaBackedFile;
import xact.ssh.sftp.filesystem.XynaFilterDelegatingFileSystem;
import xact.ssh.sftp.filesystem.cache.FileCache;
import xact.ssh.sftp.XynaBackedFileProvider;
import xact.ssh.server.XynaSSHServer;

public class SSHTrigger extends EventListener<SSHDTriggerConnection, SSHStartParameter>
    implements ShellFactory, XynaBackedFileProvider {

  private static Logger logger = CentralFactoryLogging.getLogger(SSHTrigger.class);

  private XynaSSHServer sshd;

  private BlockingQueue<SSHDTriggerConnection> requests;

  private SSHStartParameter startParameter;

  public SSHTrigger() {
  }

  public void start(SSHStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {
    this.startParameter = sp;

    requests = new LinkedBlockingQueue<SSHDTriggerConnection>(10);

    sshd = new XynaSSHServer();
    try {
      sshd.init(sp, sp, this);
    } catch (Exception e) { // XACT_InterfaceNoIPv6ConfiguredException,
                            // XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException
      throw new XACT_TriggerCouldNotBeStartedException(e) {
        private static final long serialVersionUID = 1L;
      };
    }

    if (startParameter.isEnableShell()) {
      sshd.setShellFactory(this);
    }

    if (startParameter.isEnableSCP() || startParameter.isEnableSFTP()) {
        FileCache.startCleanUp();
    }

    try {
      sshd.start();
    } catch (Exception e) {
      logger.error("Problems starting SSH Server: " + e);
      throw new XACT_TriggerCouldNotBeStartedException(e) {
        private static final long serialVersionUID = 1L;
      };
    }
  }

  public Command createShell(ChannelSession cs) {
    return new ShellCommand(sshd, requests, startParameter);
  }

  public SSHDTriggerConnection receive() {
    SSHDTriggerConnection sshCon = null;
    while (!sshd.getSshServer().isClosed() && !sshd.getSshServer().isClosing()) {
      try {
        sshCon = requests.poll(1000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        if (sshd.getSshServer().isClosed() || sshd.getSshServer().isClosing()) {
          continue;
        } else {
          logger.warn("Error polling from requests: ", e);
        }
      }
      if (sshCon != null) {
        if (logger.isDebugEnabled())
          logger.debug("New SSHDTriggerConnection " + sshCon.getClass().getSimpleName());
        if (sshCon instanceof SSHShellTriggerConnection
            && ((SSHShellTriggerConnection)sshCon).getRequestType() == null) {
          return null; // ungültiger Eintrag nach Aufruf stop()
        }
        return sshCon;
      }
    }
    logger.info("Internal server shutdown, SSH-Server-Thread terminating.");
    return null;
  }

  /**
   * called by Xyna Processing to stop the Trigger.
   * should make sure, that start() may be called again directly afterwards.
   * connection instances
   * returned by the method receive() should not be expected to work after stop()
   * has been called.
   */
  public void stop() throws XACT_TriggerCouldNotBeStoppedException {
    try {
      logger.info("SSHTrigger stop");
      sshd.stop(true);
      requests.clear();

      // laufendes Receive darf keinen gültigen Eintrag finden, daher Dummy-Eintrag
      // zum Wecken
      requests.offer(new SSHShellTriggerConnection(null, null));

    } catch (IOException e) {
      throw new RuntimeException("Problems stopping SSH Server: " + e);
    } finally {
      FileCache.stopCleanUp();
    }
  }

  /**
   * Called by Xyna Processing if there are not enough system capacities to
   * process the request.
   */
  protected void onProcessingRejected(String cause, SSHDTriggerConnection con) {
    con.handleProcessingRejected(cause);
  }

  /**
   * called when a triggerconnection generated by this trigger was not accepted by
   * any filter
   * registered to this trigger
   * 
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(SSHDTriggerConnection con) {
    con.handleNoFilterFound();
  }

  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    return "SSH Trigger. Receives SSH Requests. Supports Shell, SFTP and SCP";
  }

  public boolean addOneTimeCredentials(String user, String password, String expectedIp, String expectedPort) {
    return sshd.addOneTimeCredentials(user, password, expectedIp, expectedPort);
  }

  public TableFormatter getCacheAccessHistoryAsTable() {
    return FileCache.getCacheAccessHistoryAsTable();
  }

  public TableFormatter listCacheKeysAsTable() {
    return FileCache.listCacheKeysAsTable();
  }

  public Future<XynaBackedFile> requestFile(RequestContext info) {
    FakedFuture<XynaBackedFile> response = new FakedFuture<XynaBackedFile>();
    logger.debug("Requesting file:" + info.getPath());
    try {
      SFTPTriggerConnection newConnection = new SFTPTriggerConnection(response, info);
      if (!requests.offer(newConnection, startParameter.getSftpTimeout().getDurationInMillis(), TimeUnit.MILLISECONDS)) {
        logger.debug("Request queue did not accept request in time, aborting");
        response.cancel(false);
      }
    } catch (InterruptedException e) {
      logger.debug("Interrupted while waiting on request queue", e);
      response.injectException(e);
    }
    return response;
  }

  public Duration getRequestTimeout() {
    return startParameter.getSftpTimeout();
  }
}
