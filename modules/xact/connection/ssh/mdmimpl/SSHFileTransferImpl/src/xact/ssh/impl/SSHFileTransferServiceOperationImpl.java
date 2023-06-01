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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import xact.ssh.SSHFileTransferServiceOperation;
import base.Credentials;
import base.File;
import base.Host;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;



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


  public void sCPToRemoteHost(Host host, Credentials credentials, File localFile, File remoteFile) {

    FileInputStream fis = null;
    try {

      Session session = getSession(credentials, host);

      boolean ptimestamp = false;

      // exec 'scp -t rfile' remotely
      String command = "scp " + (ptimestamp ? "-p" : "") + " -t \"" + remoteFile.getPath() + "\"";
      Channel channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out = channel.getOutputStream();
      InputStream in = channel.getInputStream();

      channel.connect();

      int resultCode = checkAck(in);
      if (resultCode != 0) {
        throw new RuntimeException("Received unexpected result code <" + resultCode + ">");
      }

      java.io.File _lfile = new java.io.File(localFile.getPath());

      if (ptimestamp) {
        command = "T " + (_lfile.lastModified() / 1000) + " 0";
        // The access time should be sent here,
        // but it is not accessible with JavaAPI ;-<
        command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
        out.write(command.getBytes());
        out.flush();
        resultCode = checkAck(in);
        if (resultCode != 0) {
          throw new RuntimeException("Received unexpected result code <" + resultCode + ">");
        }
      }

      // send "C0644 filesize filename", where filename should not include '/'
      long filesize = _lfile.length();
      command = "C0644 " + filesize + " ";
      if (localFile.getPath().lastIndexOf('/') > 0) {
        command += localFile.getPath().substring(localFile.getPath().lastIndexOf('/') + 1);
      } else {
        command += localFile.getPath();
      }
      command += "\n";
      out.write(command.getBytes());
      out.flush();

      resultCode = checkAck(in);
      if (resultCode != 0) {
        throw new RuntimeException("Received unexpected result code <" + resultCode + ">");
      }

      // send a content of lfile
      fis = new FileInputStream(localFile.getPath());
      byte[] buf = new byte[1024];
      while (true) {
        int len = fis.read(buf, 0, buf.length);
        if (len <= 0)
          break;
        out.write(buf, 0, len); //out.flush();
      }
      fis.close();
      fis = null;
      // send '\0'
      buf[0] = 0;
      out.write(buf, 0, 1);
      out.flush();

      resultCode = checkAck(in);
      if (resultCode != 0) {
        throw new RuntimeException("Received unexpected result code <" + resultCode + ">");
      }

      out.close();

      channel.disconnect();
      session.disconnect();

    } catch (Exception e) {
      try {
        if (fis != null)
          fis.close();
      } catch (Exception ee) {
      }
      throw new RuntimeException(e);
    }

  }


  static int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if (b == 0)
      return b;
    if (b == -1)
      return b;

    if (b == 1 || b == 2) {
      StringBuffer sb = new StringBuffer();
      int c;
      do {
        c = in.read();
        sb.append((char) c);
      } while (c != '\n');
      if (b == 1) { // error
        System.out.print(sb.toString());
      }
      if (b == 2) { // fatal error
        System.out.print(sb.toString());
      }
    }
    return b;
  }


  public static class MyUserInfo implements UserInfo {

    String passwd;
    public MyUserInfo(String passwd) {
      this.passwd = passwd;
    }

    public String getPassword() {
      return passwd;
    }


    public boolean promptYesNo(String str) {
      return true;
    }


    public String getPassphrase() {
      return null;
    }


    public boolean promptPassphrase(String message) {
      return true;
    }


    public boolean promptPassword(String message) {
      return true;
    }


    @Override
    public void showMessage(String arg0) {
      if (logger.isDebugEnabled()) { // FIXME is this what we want?
        logger.debug(arg0);
      }
    }

  }


  private Session getSession(Credentials credentials, Host host) throws JSchException {
    JSch jsch = new JSch();

    int port = 22;
    Session s = jsch.getSession(credentials.getUsername(), host.getHostname(), port);
    PassphraseRetrievingUserInfo userInfo =
        new PassphraseRetrievingUserInfo(new SecureStorablePassphraseStore(), new com.jcraft.jsch.Logger() {
          
          @Override
          public void log(int arg0, String arg1) {
            logger.debug(arg1);
          }
          
          
          @Override
          public boolean isEnabled(int arg0) {
            return true;
          }
        });
    s.setUserInfo(userInfo);
    s.setConfig("StrictHostKeyChecking", "no");
    if (credentials.getPassword() != null && credentials.getPassword().length() > 0) {
      s.setPassword(credentials.getPassword());
      userInfo.setPassword(credentials.getPassword());
    }
    s.setConfig("PreferredAuthentications", "password,keyboard-interactive");
    s.connect();
    return s;
  }

}
