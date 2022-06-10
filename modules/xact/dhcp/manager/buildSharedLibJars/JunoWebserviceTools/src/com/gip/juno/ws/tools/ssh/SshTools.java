/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.juno.ws.tools.ssh;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


import com.gip.juno.ws.enums.*;
import com.gip.juno.ws.exceptions.DPPWebserviceSSHException;
import com.gip.juno.ws.tools.LocationTools;
import com.gip.xyna.utils.ssh.*;

import org.apache.log4j.Logger;

import com.jcraft.jsch.JSchException;


public class SshTools {

  public static class SshParams {
    public String user;
    public String host;
    public String password;
    public String rsaKey;
  }

  public static List<Ssh> getFailoverConnections(String location, Logger logger) throws RemoteException {
    List<Ssh> ret = new ArrayList<Ssh>();
    for (FailoverFlag flag: FailoverFlag.values()) {
      Ssh ssh = openSshConnection(location, flag, logger);
      ret.add(ssh);
    }
    return ret;
  }

  public static Ssh openSshConnection(String location, FailoverFlag flag, Logger logger)
          throws RemoteException {
    try {
      logger.info("Querying data for location " + location + ", failover " + flag.toString()
          + " from database...");
      LocationTools.LocationsRow row = LocationTools.getLocationsRow(location, flag, LocationSchema.service,
          logger);
      return openSshConnection(row, logger);
    } catch (Exception e) {
      throw new DPPWebserviceSSHException("Unable to open SSH-Connection.");
    }
  }

  public static Ssh openSshConnection(LocationTools.LocationsRow row, Logger logger)
          throws RemoteException {
    if (row == null) {
      throw new DPPWebserviceSSHException("openSshConnection: Got no location-data, unable to open connection.");
    }
    if (row.ssh_user == null) {
      throw new DPPWebserviceSSHException("openSshConnection: ssh-username=null");
    }
    if (row.ssh_password == null) {
      throw new DPPWebserviceSSHException("openSshConnection: ssh-password=null");
    }
    if (row.ssh_ip == null) {
      throw new DPPWebserviceSSHException("openSshConnection: ssh-ip=null");
    }
    Ssh ssh = openSshConnection(row.ssh_user, row.ssh_ip, row.ssh_password, row.ssh_rsaKey, logger);
    return ssh;
  }


  public static Ssh openSshConnection(SshParams params, Logger logger) throws RemoteException {
    return openSshConnection(params.user, params.host, params.password, params.rsaKey, logger);
  }


  private static Ssh openSshConnection(String user, String host, String password, String rsaKey, Logger logger)
        throws RemoteException {
    //SSH-Connection oeffnen
    try {
      Ssh ssh = new Ssh( user, host, password);
      if( rsaKey.length() > 0 ) {
        ssh.setHostRsaKey(rsaKey);
      }
      ssh.connect(2000);
      return ssh;
    } catch (JSchException e) {
      logger.error("Could not connect to " + host, e);
      throw new DPPWebserviceSSHException("Could not connect to " + host, e);
    } catch (Exception e) {
      logger.error("Error while creating SSH-Connection to host " + host + " for user " + user);
      throw new DPPWebserviceSSHException("Error while creating SSH-Connection to host " + host + " for user " + user);
    }
  }


  public static void exec(Ssh ssh, String command, Logger logger) throws RemoteException {
    if (ssh == null) {
      logger.error("Error while executing SSH-Command: SSH-connection = null");
      throw new DPPWebserviceSSHException("Error while executing SSH-Command.");
    }
    if ((command == null) || (command.trim().equals(""))) {
      logger.error("Error while executing SSH-Command: command empty.");
      throw new DPPWebserviceSSHException("Error while executing SSH-Command.");
    }
    try {
      SExec sExec = new SExec( ssh );
      logger.info("Executing command " + command + " on " + sExec);
      //sExec.threadExec(command);
      sExec.exec(command);
      String stdout = sExec.getStdOutAsString();
      String error = sExec.getStdErrAsString();
      
      int rc = sExec.getExitCode();
      if (rc != 0) {
        if(error!=null && error.length()==0)error=stdout;
        
        logger.error("Executing command " + command + " on " + sExec + " failed. Output: " + stdout
            + " ; error:" + error);
        throw new DPPWebserviceSSHException("SSH Command " + command + " on " + sExec + " failed. Error output: '" +
                                            error + "', return status = " + rc);
      } else {
        logger.info("Output: " + stdout);
        if ((error != null) && (!error.trim().equals(""))) {
          logger.info("Error: " + error);
        }
      }
    } catch (DPPWebserviceSSHException dc) {
      throw dc;
    } catch (Exception e) {
      logger.error("Error while executing SSH-Command ( " + command + " )", e);
      throw new DPPWebserviceSSHException("Error while executing SSH-Command.");
    }
  }


  public static String execForOutput(Ssh ssh, String command, Logger logger) throws RemoteException {
    if (ssh == null) {
      logger.error("Error while executing SSH-Command: SSH-connection = null");
      throw new DPPWebserviceSSHException("Error while executing SSH-Command.");
    }
    if ((command == null) || (command.trim().equals(""))) {
      logger.error("Error while executing SSH-Command: command empty.");
      throw new DPPWebserviceSSHException("Error while executing SSH-Command.");
    }
    try {
      SExec sExec = new SExec(ssh);
      logger.info("Executing command " + command + " on " + sExec);
      //sExec.threadExec(command);
      sExec.exec(command);
      logger.info("execForOutput: executed command, going to get output...");
      String stdout = sExec.getStdOutAsString();
      String error = sExec.getStdErrAsString();
      int rc = sExec.getExitCode();
      if( rc != 0 ) {
        logger.error("Executing command " + command + " on " + sExec + " failed. Output: "
            + stdout + ";   error: " + error);
        throw new DPPWebserviceSSHException("SSH Command " + command + " on " + sExec + " failed. Error output: '" +
                                            error + "', return status = " + rc);
      } else {
        //logger.info("Output: " + stdout);
        if ((error != null) && (!error.trim().equals(""))) {
          logger.info("Error: " + error);
        }
      }
      return stdout;
    } catch (DPPWebserviceSSHException dc) {
      throw dc;
    } catch (Exception e) {
      logger.error("Error while executing SSH-Command ( " + command + " )", e);
      throw new DPPWebserviceSSHException("Error while executing SSH-Command.");
    }
  }


  public static void closeConnection(Ssh ssh, Logger logger) {
    try {
      ssh.close();
    } catch (Exception e) {
      logger.error("Could not close ssh connection.", e);
    }
  }

  public static void closeConnections(List<Ssh> connections, Logger logger) {
    if (connections == null) {
      return;
    }
    for (Ssh conn : connections) {
      closeConnection(conn, logger);
    }
  }
}
