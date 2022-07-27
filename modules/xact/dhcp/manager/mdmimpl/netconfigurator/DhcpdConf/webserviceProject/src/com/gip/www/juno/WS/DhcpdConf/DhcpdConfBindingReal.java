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

package com.gip.www.juno.WS.DhcpdConf;



import java.io.File;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Properties;

import org.apache.log4j.Logger;

import dhcpdConf.DCException;
import dhcpdConf.DCProperties;
import dhcpdConf.DependencyException;
import dhcpdConf.DeployStaticHosts;
import dhcpdConf.DeploymentCancelException;
import dhcpdConf.DeploymentWarningException;
import dhcpdConf.DhcpdConf;
import dhcpdConf.db.CmEntry;
import dhcpdConf.db.ConnectData;
import dhcpdConf.db.ConnectDatav4;
import dhcpdConf.db.Hostv4;
import dhcpdConf.db.Target;
import dhcpdConf.db.Targetv4;
import dhcpdConf.ipv6.DhcpdConfv6;
import dhcpdConf.ipv6.host.ConnectDatav6;
import dhcpdConf.ipv6.host.HostAlreadyDeployedException;
import dhcpdConf.ipv6.host.Hostv6;
import dhcpdConf.ipv6.host.Targetv6;

import com.gip.juno.ws.db.tables.dhcp.StaticHostHandler;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.DPPWebserviceSSHException;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.handler.AuthenticationTools;
import com.gip.juno.ws.handler.StaticHostTools;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.tables.DeploymentTools;
import com.gip.juno.ws.tools.*;
import com.gip.juno.ws.tools.QueryTools.DBStringReader;
import com.gip.juno.ws.tools.ssh.SshTools;
import com.gip.www.juno.Gui.WS.Messages.*;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.ssh.Ssh;

// manual Imports:

import com.gip.juno.ws.tools.PropertiesHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileInputStream;


public class DhcpdConfBindingReal {

  private static final String _schema = "dhcp";
  private static final String _propertyFile = "/xyna.dhcpdconf.properties";
  private static Properties _properties = null;

  private static final String KEY_SQL_DUMP_BASE = "dhcpd.conf.v4.shell.sql.dump.base";
  private static final String KEY_SQL_DUMP_DEST_DIR = "dhcpd.conf.v4.shell.sql.dump.destination.dir";
  private static final String KEY_SSL_DIR = "dhcpd.conf.ssl.dir";
  
  private static final TableHandler staticHostHandler = new StaticHostHandler();


  public static class Constants {

    public static class PropertyNames {

      public static final String DB_CONNECT_TIMEOUT_SEC = "db.connect.timeout.seconds";
      public static final String DB_SOCKET_TIMEOUT_SEC = "db.socket.timeout.seconds";
      public static final String SHELL_DEPLOY_COMMAND = "dhcpd.conf.v4.shell.deploy.command";


      public static class Host {

        public static final String TMP_FILES_DIR = "dhcpd.conf.v4.host.tmp.files.dir";
        public static final String START_WORKFLOW_PARTIAL_COMMAND_DEPLOY_HOST = "dhcpd.conf.v4.host.xyna.start.wf.partial.command.deploy";
        public static final String START_WORKFLOW_PARTIAL_COMMAND_UNDEPLOY_HOST = "dhcpd.conf.v4.undeploy.host.xyna.start.wf.partial.command.undeploy";

      }

    }
    public static class DPPEnvironmentPropertyNames {

      public static final String MYSQL_DHCPCONF_USER = "db.dhcp.primary.user";
      public static final String MYSQL_DHCPCONF_PASSWORD = "db.dhcp.primary.password";
    }


    public static final String EMPTY_MAC = "000000000000";
  }


  static Logger logger = Logger.getLogger("DhcpdConf");


  public DhcpdConfResponse_ctype checkDhcpdConf(CheckDhcpdConfRequest_ctype checkDhcpdConfRequest)
                  throws java.rmi.RemoteException {
    // AuthenticationTools.authenticate(checkDhcpdConfRequest.getInputHeader().getUsername(),
    // checkDhcpdConfRequest.getInputHeader().getPassword(), logger);
    // AuthenticationTools.checkPermissions(checkDhcpdConfRequest.getInputHeader().getUsername(), "dhcpdconf", "dhcpd",
    // logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("dhcpd");
    AuthenticationTools.authenticateAndAuthorize(checkDhcpdConfRequest.getInputHeader().getUsername(),
                                                 checkDhcpdConfRequest.getInputHeader().getPassword(), "dhcpdconf",
                                                 wsInvocationId, logger);
    try {
      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);

      SQLUtils utils = getSQLUtils();
      Long standortGruppeID = Long.valueOf(checkDhcpdConfRequest.getCheckDhcpdConfInput().getStandortGruppeID());

      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      deployInfo.setService("DhcpdConf.checkDhcpdConf");
      deployInfo.setUser(checkDhcpdConfRequest.getInputHeader().getUsername());
      deployInfo.setTarget(getStandortgruppe(standortGruppeID));

      // checkDhcpdConf( standortGruppeID ):
      DhcpdConf dhcpdConf = null;
      try {
        dhcpdConf = new DhcpdConf(utils);
        dhcpdConf.initialize(standortGruppeID);
        dhcpdConf.writeConf();
        dhcpdConf.checkConf();
        deployInfo.setLog("Successful.");
      }
      catch (DCException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        throw new DPPWebserviceException(de);
      }
      catch (DependencyException de2) {
        throw new DPPWebserviceException(de2.getMessage(), de2);
      }
      catch (Throwable t) {
        logger.error("Unexpected exception", t);
        deployInfo.setLog(t.toString());
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        releaseSQLUtils(utils);
        if (dhcpdConf != null) {
          dhcpdConf.close();
          closeDhpcdConfVerwaltung(dhcpdConf);
        }
        DeploymentTools.insertRow(deployInfo);
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }


  public DhcpdConfResponse_ctype deployDhcpdConf(DeployDhcpdConfRequest_ctype deployDhcpdConfRequest)
                  throws java.rmi.RemoteException {
    InputHeaderContent_ctype inputHeader = deployDhcpdConfRequest.getInputHeader();
    // AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    // AuthenticationTools.checkPermissions(inputHeader.getUsername(), "dhcpdconf", "dhcpd", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("dhcpd");
    AuthenticationTools.authenticateAndAuthorize(deployDhcpdConfRequest.getInputHeader().getUsername(),
                                                 deployDhcpdConfRequest.getInputHeader().getPassword(), "dhcpdconf",
                                                 wsInvocationId, logger);
    try {
      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);
      SQLUtils utils = getSQLUtils();
      Long standortGruppeID = Long.valueOf(deployDhcpdConfRequest.getDeployDhcpdConfInput().getStandortGruppeID());

      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      deployInfo.setService("DhcpdConf.deployDhcpdConf");
      deployInfo.setUser(deployDhcpdConfRequest.getInputHeader().getUsername());
      deployInfo.setTarget(getStandortgruppe(standortGruppeID));

      DhcpdConf dhcpdConf = null;
      try {
        dhcpdConf = new DhcpdConf(utils);
        dhcpdConf.initialize(standortGruppeID);
        dhcpdConf.writeConf();
        dhcpdConf.checkConf();
        dhcpdConf.deployConf(); // nur 2.
        deployInfo.setLog("Successful.");
      }
      catch (DCException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        throw new DPPWebserviceException(de);
      }
      catch (DependencyException de2) {
        throw new DPPWebserviceException(de2.getMessage(), de2);

      }
      catch (Throwable t) {
        logger.error("Unexpected exception", t);
        deployInfo.setLog(MessageBuilder.stackTraceToString(t));
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        releaseSQLUtils(utils);
        if (dhcpdConf != null) {
          dhcpdConf.close();
          closeDhpcdConfVerwaltung(dhcpdConf);
        }
        DeploymentTools.insertRow(deployInfo);
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }


  public DhcpdConfResponse_ctype deployDhcpdConfNewFormat(DeployDhcpdConfRequest_ctype deployDhcpdConfRequest)
                  throws java.rmi.RemoteException {

    logger.info("#### New Format Deploy DhcpdConf called!");
    InputHeaderContent_ctype inputHeader = deployDhcpdConfRequest.getInputHeader();
    // AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    // AuthenticationTools.checkPermissions(inputHeader.getUsername(), "dhcpdconf", "dhcpd", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("dhcpd");
    AuthenticationTools.authenticateAndAuthorize(deployDhcpdConfRequest.getInputHeader().getUsername(),
                                                 deployDhcpdConfRequest.getInputHeader().getPassword(), "dhcpdconf",
                                                 wsInvocationId, logger);
    try {
      long startTime = System.currentTimeMillis();

      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);
      SQLUtils utils = getSQLUtils();
      Long standortGruppeID = Long.valueOf(deployDhcpdConfRequest.getDeployDhcpdConfInput().getStandortGruppeID());

      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      deployInfo.setService("DhcpdConf.deployDhcpdConf");
      deployInfo.setUser(deployDhcpdConfRequest.getInputHeader().getUsername());
      deployInfo.setTarget(getStandortgruppe(standortGruppeID));

      DhcpdConf dhcpdConf = null;
      try {
        logger.info("Generating networkv4 config file for current pool configuration ...");
        dhcpdConf = new DhcpdConf(utils);
        dhcpdConf.initialize(standortGruppeID);
        dhcpdConf.writeConfNewFormat(false);
        // dhcpdConf.checkConf();
        // dhcpdConf.deployConf(); // nur 2.
        deployInfo.setLog("Successful.");
        logger.info("Finished generating networkv4 config file!");


        Properties wsProperties = PropertiesHandler.getWsProperties();
        Target[] targets = getTargets(dhcpdConf);

        logger.info("Creating backup of current dhcpv4 Tables ...");
        
        backupTargetTables(targets, wsProperties);
        logger.debug("Time after backup dump target tables: " + (System.currentTimeMillis() - startTime));

        logger.info("Creating backup of current dhcpv4 Tables finished!");

        logger.info("Deploying dhcpv4 tables from managment to dpp ...");
        
        copyAllTables(targets, wsProperties, dhcpdConf.getVerwaltung());
        logger.debug("Time after finished: " + (System.currentTimeMillis() - startTime));

        logger.info("Deploying dhcpv4 tables from managment to DPP finished!");

        logger.info("Executing configured deploycommands on DPP ...");
        // execute shell command to deploy new config file
        deployConf(targets, wsProperties);

        logger.info("Executing configured deploycommands on DPP finished!");

        
        deployInfo.setLog("Successful.");


      }
      catch (DCException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        throw new DPPWebserviceException(de);
      }
      catch (DependencyException de2) {
        throw new DPPWebserviceException(de2.getMessage(), de2);
      }
      catch(DeploymentWarningException dwe)
      {
        logger.info("Finished generating networkv4 config file!");
        Properties wsProperties = PropertiesHandler.getWsProperties();
        Target[] targets = getTargets(dhcpdConf);

        logger.info("Creating backup of current dhcpv4 Tables ...");

        backupTargetTables(targets, wsProperties);
        logger.debug("Time after backup dump target tables: " + (System.currentTimeMillis() - startTime));

        logger.info("Creating backup of current dhcpv4 Tables finished!");

        logger.info("Deploying dhcpv4 tables from managment to dpp ...");

        copyAllTables(targets, wsProperties,dhcpdConf.getVerwaltung());

        logger.debug("Time after finished: " + (System.currentTimeMillis() - startTime));
        logger.info("Deploying dhcpv4 tables from managment to DPP finished!");

        logger.info("Executing configured deploycommands on DPP ...");

        // execute shell command to deploy new config file
        deployConf(targets, wsProperties);
        logger.info("Executing configured deploycommands on DPP finished!");

        //deployInfo.setLog("Successful.");

        deployInfo.setLog("Successful with warning: "+dwe.getMessage());
        throw new DPPWebserviceException(new MessageBuilder().setDescription(dwe.getMessage()).setSeverity("2").setErrorNumber("00221").setDomain("F"));
      }
      catch(DeploymentCancelException dce)
      {
        deployInfo.setLog(MessageBuilder.stackTraceToString(dce));
        throw new DPPWebserviceException(new MessageBuilder().setDescription(dce.getMessage()).setErrorNumber("00222").setDomain("F"));
      }
      catch(DPPWebserviceSSHException sshex)
      {
        deployInfo.setLog(MessageBuilder.stackTraceToString(sshex));
        throw sshex;
      }

      catch (Throwable t) {
        logger.error("Unexpected exception", t);
        deployInfo.setLog(MessageBuilder.stackTraceToString(t));
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        releaseSQLUtils(utils);
        if (dhcpdConf != null) {
          dhcpdConf.close();
          closeDhpcdConfVerwaltung(dhcpdConf);
        }
        DeploymentTools.insertRow(deployInfo);
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }


  public DhcpdConfResponse_ctype checkDhcpdConfNewFormat(CheckDhcpdConfRequest_ctype checkDhcpdConfRequest)
                  throws java.rmi.RemoteException {

    logger.info("#### New Format Deploy DhcpdConf called!");
    InputHeaderContent_ctype inputHeader = checkDhcpdConfRequest.getInputHeader();
    // AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    // AuthenticationTools.checkPermissions(inputHeader.getUsername(), "dhcpdconf", "dhcpd", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("dhcpd");
    AuthenticationTools.authenticateAndAuthorize(checkDhcpdConfRequest.getInputHeader().getUsername(),
                                                 checkDhcpdConfRequest.getInputHeader().getPassword(), "dhcpdconf",
                                                 wsInvocationId, logger);
    try {
      long startTime = System.currentTimeMillis();

      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);
      SQLUtils utils = getSQLUtils();
      Long standortGruppeID = Long.valueOf(checkDhcpdConfRequest.getCheckDhcpdConfInput().getStandortGruppeID());

      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      deployInfo.setService("DhcpdConf.deployDhcpdConf");
      deployInfo.setUser(checkDhcpdConfRequest.getInputHeader().getUsername());
      deployInfo.setTarget(getStandortgruppe(standortGruppeID));

      DhcpdConf dhcpdConf = null;
      try {
        dhcpdConf = new DhcpdConf(utils);
        dhcpdConf.initialize(standortGruppeID);
        dhcpdConf.writeConfNewFormat(true);
        // dhcpdConf.checkConf();
        // dhcpdConf.deployConf(); // nur 2.
        deployInfo.setLog("Successful.");

      }
      catch (DCException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        throw new DPPWebserviceException(de);
      }
      catch (DependencyException de2) {
        throw new DPPWebserviceException(de2.getMessage(), de2);

      }
      catch(DeploymentWarningException dwe)
      {
        throw new DPPWebserviceException(new MessageBuilder().setDescription(dwe.getMessage()).setSeverity("2").setErrorNumber("00221").setDomain("F"));
      }
      catch(DeploymentCancelException dce)
      {
        throw new DPPWebserviceException(new MessageBuilder().setDescription(dce.getMessage()).setErrorNumber("00222").setDomain("F"));
      }


      catch (Throwable t) {
        logger.error("Unexpected exception", t);
        deployInfo.setLog(MessageBuilder.stackTraceToString(t));
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        releaseSQLUtils(utils);
        if (dhcpdConf != null) {
          dhcpdConf.close();
          closeDhpcdConfVerwaltung(dhcpdConf);
        }
        DeploymentTools.insertRow(deployInfo);
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }

  
  
  public DhcpdConfResponse_ctype deployStaticHost(DeployStaticHostRequest_ctype deployStaticHostRequest)
                  throws java.rmi.RemoteException {
    InputHeaderContent_ctype inputHeader = deployStaticHostRequest.getInputHeader();
    // AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    // AuthenticationTools.checkPermissions(inputHeader.getUsername(), "dhcpdconf", "statichost", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("statichost");
    AuthenticationTools.authenticateAndAuthorize(deployStaticHostRequest.getInputHeader().getUsername(),
                                                 deployStaticHostRequest.getInputHeader().getPassword(), "dhcpdconf",
                                                 wsInvocationId, logger);
    try {
      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);
      SQLUtils utils = getSQLUtils();

      SQLUtilsContainer sqlUtilsContainerService = getSQLUtilsService();
      SQLUtils sqlUtilsService = sqlUtilsContainerService.getSQLUtils();

      String staticHostID = deployStaticHostRequest.getDeployStaticHostInput().getStaticHostId();
      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      deployInfo.setService("DhcpdConf.deployStaticHost (Ip " + StaticHostTools.queryIp(staticHostID, logger) + ")");
      deployInfo.setUser(deployStaticHostRequest.getInputHeader().getUsername());
      deployInfo.setTarget("StaticHost");
      
      try {
        StaticHostTools.updateCmtsIp(staticHostID, logger);
      } catch (DPPWebserviceIllegalArgumentException e) {
        // don't abort deployment if CmtsIp couldn't be updated
      }
      
      DeployStaticHosts dsh = null;
      try {

        int rowsChanged = updateCmCpeIPs(sqlUtilsService, staticHostID, true);

        dsh = new DeployStaticHosts(utils);

        dsh.deployStaticHost(staticHostID);
        sqlUtilsService.commit();
        deployInfo.setLog("Successful.");

        if (rowsChanged == 0) {
          throw new DPPWebserviceException(new MessageBuilder()
                          .setDescription("Deployment successfull but a RemoteAgent with that mac could not be found.")
                          .setErrorNumber("00219").setDomain("F").setSeverity("3"));
        }
      }
      catch (DCException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        throw new DPPWebserviceException(de);
      }
      catch (DPPWebserviceException e) {
        throw e;
      }
      catch (Throwable t) {
        logger.error("Unexpected exception", t);
        deployInfo.setLog(MessageBuilder.stackTraceToString(t));
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        releaseSQLUtils(utils);
        // releaseSQLUtils(sqlUtilsContainerService);
        DeploymentTools.insertRow(deployInfo);
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }


  public DhcpdConfResponse_ctype deployStaticHostNewFormat(DeployStaticHostRequest_ctype deployStaticHostRequest)
                  throws java.rmi.RemoteException {
    InputHeaderContent_ctype inputHeader = deployStaticHostRequest.getInputHeader();
    // AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    // AuthenticationTools.checkPermissions(inputHeader.getUsername(), "dhcpdconfv6", "host", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("statichost");
    AuthenticationTools.authenticateAndAuthorize(deployStaticHostRequest.getInputHeader().getUsername(),
                                                 deployStaticHostRequest.getInputHeader().getPassword(), "dhcpdconf",
                                                 wsInvocationId, logger);
    try {
      logger.info("Deploying static host ...");
      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);
      String inputHostID = deployStaticHostRequest.getDeployStaticHostInput().getStaticHostId();
      StaticHostTools.updateCmtsIp(inputHostID, logger);
      DeploymentTools.Row deployInfo = new DeploymentTools.Row();

      deployInfo.setUser(deployStaticHostRequest.getInputHeader().getUsername());
      deployInfo.setTarget("Host");

      SQLUtils utils = null;
      SQLUtilsContainer sqlUtilsContainerService = null;
      Hostv4 host = null;
      try {
        Properties wsProperties = PropertiesHandler.getWsProperties();
        utils = getSQLUtils();


        sqlUtilsContainerService = getSQLUtilsService();
        SQLUtils sqlUtilsService = sqlUtilsContainerService.getSQLUtils();
        Integer hostID = null;
        try {
          hostID = Integer.parseInt(inputHostID);
        }
        catch (Exception e) {
          throw new DCException("Could not parse hostID: " + hostID);
        }
        host = Hostv4.query(utils, hostID);
        deployInfo.setService("DhcpdConf.deployHost (Ip " + host.getAssignedIp() + ")");

        host.updateDeploymentState(utils, true);
        copyHostEntryToDPP(host, utils, wsProperties);
        
        boolean cpeIpsUpdateSuccesfull = false; 
        try {
          CmEntry cmEntry = new CmEntry(host.getAgentRemoteID());
  
          // add ip to column cpeIpsv6 in table cm
          cmEntry.addIp(host.getAssignedIp(), sqlUtilsService);
          cpeIpsUpdateSuccesfull = true;
        } catch (Throwable t) {
          logger.warn("Update of CpeIps could not be executed", t);
        }

        utils.commit();
        sqlUtilsService.commit();
        host.commitSQLUtilsForTargets(logger);

        executeDeployHostShellCommands(host, utils, wsProperties);
        
        if (!cpeIpsUpdateSuccesfull) {
          throw new DPPWebserviceException(new MessageBuilder()
                                          .setDescription("Deployment successfull but a CpeIds could not be updated.")
                                          .setErrorNumber("00219").setDomain("F").setSeverity("3"));
        }

        deployInfo.setLog("Successful.");
        logger.info("Deploying static host finished!");
      }
      catch (HostAlreadyDeployedException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Host is already deployed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        MessageBuilder builder = new MessageBuilder();
        builder.setDescription("Host is already deployed.");
        builder.setDomain("F");
        builder.setErrorNumber("00212");
        builder.setCause(de);
        throw new DPPWebserviceException(builder);
      }
      catch (DCException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        throw new DPPWebserviceException("Check failed.", de);
      }
      catch (DPPWebserviceException e) {
        deployInfo.setLog(MessageBuilder.stackTraceToString(e));
        throw e;
      }
      catch (Throwable t) {
        logger.error("Unexpected exception", t);
        deployInfo.setLog(t.toString());
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        try {
          releaseSQLUtils(utils);
          // releaseSQLUtils(sqlUtilsContainerService);
        }
        catch (Exception e) {
          // do nothing
        }
        try {
          host.closeSQLUtilsForTargets(logger);
        }
        catch (Exception e) {
          // do nothing
        }
        try {
          DeploymentTools.insertRow(deployInfo);
        }
        catch (Exception e) {
          // do nothing
        }
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }


  public DhcpdConfResponse_ctype undeployStaticHost(UndeployStaticHostRequest_ctype undeployStaticHostRequest)
                  throws java.rmi.RemoteException {
    InputHeaderContent_ctype inputHeader = undeployStaticHostRequest.getInputHeader();
    // AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    // AuthenticationTools.checkPermissions(inputHeader.getUsername(), "dhcpdconf", "statichost", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("statichost");
    AuthenticationTools.authenticateAndAuthorize(undeployStaticHostRequest.getInputHeader().getUsername(),
                                                 undeployStaticHostRequest.getInputHeader().getPassword(), "dhcpdconf",
                                                 wsInvocationId, logger);
    try {
      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);
      SQLUtils utils = getSQLUtils();

      SQLUtilsContainer sqlUtilsContainerService = getSQLUtilsService();
      SQLUtils sqlUtilsService = sqlUtilsContainerService.getSQLUtils();

      boolean force = (undeployStaticHostRequest.getUndeployStaticHostInput().getForce() == null) ? false : undeployStaticHostRequest
                      .getUndeployStaticHostInput().getForce();
      logger.info("value for force = " + force);

      String staticHostID = undeployStaticHostRequest.getUndeployStaticHostInput().getStaticHostId();
      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      deployInfo.setService("DhcpdConf.undeployStaticHost (Ip " + StaticHostTools.queryIp(staticHostID, logger) + ")");
      deployInfo.setUser(undeployStaticHostRequest.getInputHeader().getUsername());
      deployInfo.setTarget("StaticHost");
      // SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      // undeployStaticHost:
      DeployStaticHosts dsh = null;
      try {
        dsh = new DeployStaticHosts(utils);
        dsh.undeployStaticHost(staticHostID);
        try {
          updateCmCpeIPs(sqlUtilsService, staticHostID, false);
          sqlUtilsService.commit();
        }
        catch (Throwable t) {
          if (!force) {
            throw t;
          }
          else {
            logger.info("Undeploy statichost: Ignored by force", t);
          }
        }

        deployInfo.setLog("Successful.");
      }
      catch (DCException de) {
        logger.error("Expected exception", de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfo.setLog(MessageBuilder.stackTraceToString(de));
        throw new DPPWebserviceException(de);
      }
      catch (Throwable t) {
        logger.error("Unexpected exception", t);
        deployInfo.setLog(MessageBuilder.stackTraceToString(t));
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        releaseSQLUtils(utils);
        // releaseSQLUtils(sqlUtilsContainerService);
        DeploymentTools.insertRow(deployInfo);
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }


  public DhcpdConfResponse_ctype undeployStaticHostNewFormat(UndeployStaticHostRequest_ctype undeployStaticHostRequest)
                  throws java.rmi.RemoteException {
    InputHeaderContent_ctype inputHeader = undeployStaticHostRequest.getInputHeader();
    //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "dhcpdconfv6", "host", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("statichost");
    AuthenticationTools.authenticateAndAuthorize(undeployStaticHostRequest.getInputHeader().getUsername(),
                                                 undeployStaticHostRequest.getInputHeader().getPassword(),
                                                 "dhcpdconf", wsInvocationId, logger);
    try {
      logger.info("Undeploying static host ...");
      init();
      DhcpdConfResponse_ctype ret = new DhcpdConfResponse_ctype();
      ret.setOutputContent("Successful.");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      ret.setOutputHeader(header);

      String inputHostID = undeployStaticHostRequest.getUndeployStaticHostInput().getStaticHostId();
      boolean force = (undeployStaticHostRequest.getUndeployStaticHostInput().getForce() == null) ? false :
                      undeployStaticHostRequest.getUndeployStaticHostInput().getForce();
      logger.info("value for force = " + force);

      DeploymentTools.Row deployInfo = new DeploymentTools.Row();

      deployInfo.setUser(undeployStaticHostRequest.getInputHeader().getUsername());
      deployInfo.setTarget("Host");

      SQLUtils utils  = null;
      SQLUtilsContainer sqlUtilsContainerService = null;
      Hostv4 host = null;
      String deployInfoLog = "";
      try {
        Properties wsProperties = PropertiesHandler.getWsProperties();
        utils = getSQLUtils();

        sqlUtilsContainerService = getSQLUtilsService();
        SQLUtils sqlUtilsService = sqlUtilsContainerService.getSQLUtils();
        Integer hostID = null;
        try {
          hostID = Integer.parseInt(inputHostID);
        }
        catch (Exception e) {
          throw new DCException("Could not parse hostID: " + hostID);
        }
        host = Hostv4.query(utils, hostID);
        String opName = force ? "forceUndeploy" : "undeploy";
        deployInfo.setService("DhcpdConf." + opName + " (Ip " + host.getAssignedIp() + ")");

        boolean cmOpSuccess = false;
        boolean dppOpSuccess = false;
        boolean deployStateSuccess = false;

        try {
          host.readTargets(utils);
        }
        catch (Exception e) {
          if (!force) {
            throw e;
          }
          else {
            logger.info("Undeploy host: Ignored by force", e);
            deployInfoLog += "\n" + MessageBuilder.stackTraceToString(e);
          }
        }

        try {
          deleteHostEntryOnDPP(host, utils, wsProperties);
          dppOpSuccess = true;
        }
        catch (Throwable e) {
          if (!force) {
            throw e;
          }
          else {
            logger.info("Undeploy host: Ignored by force", e);
            deployInfoLog += "\n" + MessageBuilder.stackTraceToString(e);
          }
        }

        try {
          CmEntry cmEntry = new CmEntry(host.getAgentRemoteID());
          // remove ip from column cpeIpsv6 in table cm
          cmEntry.removeIp(host.getAssignedIp(), sqlUtilsService);
          cmOpSuccess = true;
        }
        catch (Throwable e) {
          if (!force) {
            throw e;
          }
          else {
            logger.info("Undeploy host: Ignored by force", e);
            deployInfoLog += "\n" + MessageBuilder.stackTraceToString(e);
          }
        }

        try {
          host.updateDeploymentState(utils, false);
          deployStateSuccess = true;
        }
        catch (Throwable e) {
          if (!force) {
            throw e;
          }
          else {
            logger.info("Undeploy host: Ignored by force", e);
            deployInfoLog += "\n" + MessageBuilder.stackTraceToString(e);
          }
        }

        // commit database changes (before starting workflow)
        if (deployStateSuccess) {
          utils.commit();
        }
        if (dppOpSuccess) {
          host.commitSQLUtilsForTargets(logger);
        }
        if (cmOpSuccess) {
          sqlUtilsService.commit();
        }

        try {
          executeUndeployHostShellCommands(host, utils, wsProperties);
        }
        catch (Throwable e) {
          if (!force) {
            throw e;
          }
          else {
            logger.info("Undeploy host: Ignored by force", e);
            deployInfoLog += "\n" + MessageBuilder.stackTraceToString(e);
          }
        }

        if (deployInfoLog.length() < 1) {
          deployInfoLog = "Successful.";
        }
        logger.info("Undeploying static host finished!");
      }
      catch(DCException de) {
        logger.error("Expected exception",de);
        ret.setOutputContent("Check failed.");
        ret.getOutputHeader().setException(MessageBuilder.stackTraceToString(de));
        ret.getOutputHeader().setStatus("Failed");
        deployInfoLog += "\n" + MessageBuilder.stackTraceToString(de);
        throw new DPPWebserviceException("Check failed.", de);
      }
      catch (DPPWebserviceException e){
        //deployInfo.setLog(MessageBuilder.stackTraceToString(e));
        deployInfoLog += "\n" + MessageBuilder.stackTraceToString(e);
        throw e;
      }
      catch( Throwable t ) {
        logger.error("Unexpected exception",t);
        deployInfoLog += "\n" + MessageBuilder.stackTraceToString(t);
        throw new DPPWebserviceUnexpectedException(t);
      }
      finally {
        try {
          releaseSQLUtils(utils);
          //releaseSQLUtils(sqlUtilsContainerService);
        }
        catch (Exception e) {
          //do nothing
        }
        try {
          host.closeSQLUtilsForTargets(logger);
        }
        catch (Exception e) {
          //do nothing
        }
        try {
          deployInfo.setLog(deployInfoLog);
          DeploymentTools.insertRow(deployInfo);
        }
        catch (Exception e) {
          //do nothing
        }
      }
      return ret;
    }
    catch (RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
    

  
  }

  private void init() throws RemoteException {
    // Einrichten der Properties
    logger.info("Trying to read dhcpdconf property file...");
    DCProperties.readProperties(getProperties());
  }

  private void closeDhpcdConfVerwaltung(DhcpdConf dhcpdConf) {
      if (dhcpdConf == null || dhcpdConf.getVerwaltung() == null)
      return;

      dhcpdConf.getVerwaltung().getSsh().close();

  }


  private Properties getProperties() throws RemoteException {
    if (_properties != null) {
      return _properties;
    }
    else {
      return loadProperties(_propertyFile); // origin file: return loadProperties();
    }
  }


 /* private Properties loadProperties() throws java.rmi.RemoteException {
    try {
      InputStream instr = this.getClass().getResourceAsStream("/xyna.dhcpdconf.properties");  // _propertyFile
      Properties prop = new Properties();
      prop.load(instr);
      instr.close();
      logger.info("Successfully loaded properties from file.");
      return prop;
    }
    catch (Exception e) {
      logger.error("loadProperties : ", e);
      throw new DPPWebserviceUnexpectedException(e);
    }
  }
*/ // Origin file-programm
  
  private Properties loadProperties(String filename) throws java.rmi.RemoteException {
        try {
          String path = "/etc/opt/xyna/environment";
          if(!Files.exists(Paths.get(path + filename))) {
            String homedir = System.getenv("HOME");
            path = homedir + "/environment";
            logger.info("Choose path \"" + path + "\"");
            if(!Files.exists(Paths.get(path + filename))) {
              path = "";
              logger.info("Choose path \"" + path + "\"");
            }
          }
          //InputStream instr = this.getClass().getResourceAsStream(filename);  //former version
          InputStream instr = new FileInputStream(path+filename);
          logger.info("Input Stream read from \"" + path+filename + "\"");
          
          Properties prop = new Properties();
          prop.load(instr);
          instr.close();
          logger.info("Successfully loaded properties from file.");
          return prop;
        } catch (Exception e) {
          logger.error("Error while trying to load properties file" + filename, e);
          throw new DPPWebserviceException("Error while trying to load properties file " + filename, e);
        }
      }

  // private SQLUtilsContainer getSQLUtils() throws RemoteException {
  //
  // FailoverFlag flag = FailoverTools.getCurrentFailover(_schema, logger);
  // DBSchema schema = ManagementData.translateDBSchemaName(_schema, logger);
  // //return SQLUtilsCacheForManagement.createSQLUtilsForUncached(schema, flag, logger);
  // SQLUtilsCacheForManagement.
  // //return SQLUtilsCache.getForManagement(_schema, logger);
  // }
  //
  // private SQLUtilsContainer getSQLUtilsService() throws RemoteException {
  // return SQLUtilsCache.getForManagement("service", logger);
  // }

  private SQLUtils getSQLUtils() throws RemoteException {

    FailoverFlag flag = FailoverTools.getCurrentFailover(_schema, logger);
    DBSchema schema = ManagementData.translateDBSchemaName(_schema, logger);
    return SQLUtilsCacheForManagement.createSQLUtilsForUncached(schema, flag, logger);

  }


  private SQLUtilsContainer getSQLUtilsService() throws RemoteException {
    return SQLUtilsCache.getForManagement("service", logger);
  }


  // private void releaseSQLUtils(SQLUtilsContainer container) throws RemoteException {
  // try {
  // SQLUtilsCache.release(container, logger);
  // } catch (Exception e) {
  // logger.debug(e);
  // }
  // }

  private void releaseSQLUtils(SQLUtils utils) throws RemoteException {
    try {
      utils.rollback();
      utils.closeConnection();
    }
    catch (Exception e) {
      logger.debug(e);
    }
  }


  private static int updateCmCpeIPs(SQLUtils sqlUtilsService, String staticHostID, boolean deploy)
                  throws RemoteException, DPPWebserviceException {
    String remoteId = queryRemoteId(staticHostID, logger);
    if (remoteId == null) {
      throw new DPPWebserviceException("dhcp.statichost not found");
    }
    String cpeIPs = queryCpeIds(remoteId, staticHostID, deploy, logger);
    Parameter params = new Parameter(cpeIPs, remoteId);
    int rowsChanged = sqlUtilsService.executeDML("UPDATE cm SET cpeIps=? WHERE mac=unhex(?)", params);
    if (sqlUtilsService.getLastException() != null) {
      throw new DPPWebserviceException("SQL-Error while updating services.cm.cpeIPs");
    }
    return rowsChanged;
  }


  private String getStandortgruppe(Long id) throws RemoteException {
    String ret = QueryTools.queryNameOfStandortgruppe(id.toString(), logger);
    if ((ret == null) || (ret.trim().equals(""))) {
      logger.error("Standort with id " + id + "does not exist.");
      throw new DPPWebserviceUnexpectedException("Standort with id " + id + "does not exist.");
    }
    return ret;
  }


  private static String queryRemoteId(String staticHostID, Logger logger) throws RemoteException {
    String sql = "SELECT remoteId FROM dhcp.statichost WHERE staticHostID = ? ";
    String schema = _schema;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    builder.addParam(new ColName("staticHostId"), new ColStrValue(staticHostID), ColType.string);
    return new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
  }


  private static String queryCpeIds(String remoteId, String staticHostId, boolean deploy, Logger logger)
                  throws RemoteException {
    String sql = "SELECT  ifnull( group_concat(ip order by ip, ','),'') FROM dhcp.statichost WHERE (deployed1='YES' AND deployed2='YES' AND remoteid = ?)";
    if (deploy) {
      sql = sql + " OR  (statichostid=?)";
    }
    else {
      sql = sql + " AND NOT  (statichostid=?)";
    }

    String schema = _schema;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    builder.addParam(new ColName("remoteId"), new ColStrValue(remoteId), ColType.string);
    builder.addParam(new ColName("staticHostId"), new ColStrValue(staticHostId), ColType.string);
    return new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
  }


  /**
   * copies tables from management instance to a pair of DPP instances; first a mysqldump file (for all tables at once)
   * is generated on the management instance; then the mysql user and password for the DPP instance are read by ssh from
   * a property file on the DPP instance; then a mysql command is executed on the management instance, that logs in to
   * mysql on the DPP instance and executes the sql commands of the dump file there.
   */
  private static void copyAllTables(Target[] targets, Properties wsProperties, ConnectData verwaltung) throws RemoteException {
    logger.info("Copying tables ");
    long startTime = System.currentTimeMillis();

    //Ssh management = getSshForManagement();
    Ssh management = verwaltung.getSsh();  // aus ConnectData lesen statt aus locations
    try {
      dumpTables(management, wsProperties);
      logger.debug("copyAllTables() - time diff after dumping table: " + (System.currentTimeMillis() - startTime));

      String dumpDir = PropertiesHandler.getProperty(wsProperties, KEY_SQL_DUMP_DEST_DIR, logger);
      if (!dumpDir.endsWith("/")) {
        dumpDir += "/";
      }
      String dumpFile = dumpDir + "MultipleTables_dhcp.class.sql";

      for (Target target : targets) {
        Ssh targetSsh = getTargetSsh(target);
        String importUser = getTargetMySqlUser(targetSsh);
        String targetPwd = getTargetMySqlPassword(targetSsh);

        logger.debug("copyAllTables() - time diff after getting password: " + (System.currentTimeMillis() - startTime));

        String ssldir = "/etc/opt/xyna/.ssl/";
        try
        {
          ssldir = PropertiesHandler.getProperty(wsProperties, KEY_SSL_DIR, logger);
          if(!ssldir.endsWith("/"))ssldir = ssldir+"/";
        }
        catch(Exception e)
        {
         logger.info("Path to SSL Certificates not configured. Using default: "+ssldir);
        }

        String hostip = target.getHostIp();
        String command = "mysql -u '" + importUser + "' -p'" + targetPwd + "' -h " + hostip + " -D dhcp < '" + dumpFile + "'";
        
        String clientkeypath = ssldir+"client-key.pem";
        String clientcertpath = ssldir+"client-cert.pem";
        try
        {
          File clientkey = new File(clientkeypath);
          File clientcert = new File(clientcertpath);
          
          if(clientkey.exists()&&clientcert.exists())
          {
            command = "mysql -u '" + importUser + "' -p'" + targetPwd +"' --ssl --ssl-key '"+clientkeypath+"' --ssl-cert '"+ clientcertpath +"' -h " + hostip + " -D dhcp < '" + dumpFile + "'";
          }
          else
          {
            logger.info("No SSL certificates found in "+ssldir);
          }
          
        }
        catch(Exception e)
        {
          logger.warn("Exception trying to use SSL for mysql: ",e);
        }
        
        
        SshTools.exec(management, command, logger);

        logger.debug("copyAllTables() - time diff after copying: " + (System.currentTimeMillis() - startTime));
      }
    }
    finally {
      // close ssh connection to management
      SshTools.closeConnection(management, logger);
    }
  }


  private static void dumpTables(Ssh ssh, Properties wsProperties) throws RemoteException {
    String commandBase = PropertiesHandler.getProperty(wsProperties, KEY_SQL_DUMP_BASE, logger);
    String command = commandBase + " export dhcp " + getTableNames();
    SshTools.exec(ssh, command, logger);
  }


  private static String getTableNames() {
    return "class condition guiparameter guioperator guiattribute guifixedattribute dppfixedattribute sharednetwork" + " pooltype";
  }


  private static String getTargetMySqlPassword(Ssh ssh) throws RemoteException {
//    String command = "cat /etc/xyna/environment/${HOSTNAME}.properties | grep " + Constants.DPPEnvironmentPropertyNames.MYSQL_DHCPCONF_PASSWORD;
//    String out = SshTools.execForOutput(ssh, command, logger);
//    String[] parts = out.split("=");
//    if (parts.length != 2) {
//      throw new DPPWebserviceException(
//                                       "Wrong format of password entry in property file on DPP instance." + "Property key = " + Constants.DPPEnvironmentPropertyNames.MYSQL_DHCPCONF_PASSWORD);
//    }
//    return parts[1];
    Properties wsProperties = PropertiesHandler.getDBProperties();

    String targetUser = PropertiesHandler.getProperty(wsProperties, Constants.DPPEnvironmentPropertyNames.MYSQL_DHCPCONF_PASSWORD, logger);
    return targetUser;
    
  }


  private static String getTargetMySqlUser(Ssh ssh) throws RemoteException {
//    String command = "cat /etc/xyna/environment/${HOSTNAME}.properties | grep " + Constants.DPPEnvironmentPropertyNames.MYSQL_DHCPCONF_USER;
//    String out = SshTools.execForOutput(ssh, command, logger);
//    String[] parts = out.split("=");
//    if (parts.length != 2) {
//      throw new DPPWebserviceException(
//                                       "Wrong format of username entry in property file on DPP instance." + "Property key = " + Constants.DPPEnvironmentPropertyNames.MYSQL_DHCPCONF_USER);
//    }
//    return parts[1];
      Properties wsProperties = PropertiesHandler.getDBProperties();

      String targetUser = PropertiesHandler.getProperty(wsProperties, Constants.DPPEnvironmentPropertyNames.MYSQL_DHCPCONF_USER, logger);
      return targetUser;
    
  }


  private static Ssh getSshForManagement() throws RemoteException {
    FailoverFlag flag = FailoverTools.getCurrentFailover(DBSchema.dhcp, logger);
    LocationTools.LocationsRow row = LocationTools.getManagementRow(flag, logger);
    Ssh ssh = SshTools.openSshConnection(row, logger);
    return ssh;
  }


  private static Ssh getTargetSsh(Target target) {
    return target.getSsh();
  }


  private static void backupTargetTables(Target[] targets, Properties wsProperties) throws RemoteException {
    for (Target target : targets) {
      Ssh targetSsh = getTargetSsh(target);
      dumpTables(targetSsh, wsProperties);
    }
  }


  private static Target[] getTargets(DhcpdConf dhcpdConf) throws RemoteException {
    Target targets[] = {dhcpdConf.getTarget0(), dhcpdConf.getTarget1()};
    return targets;
  }


  private static void deployConf(Target[] targets, Properties wsProperties) throws RemoteException {
    String command = PropertiesHandler.getProperty(wsProperties, Constants.PropertyNames.SHELL_DEPLOY_COMMAND, logger);
    for (Target target : targets) {
      Ssh targetSsh = getTargetSsh(target);
      SshTools.exec(targetSsh, command, logger);
    }
  }


  private void copyHostEntryToDPP(Hostv4 host, SQLUtils sqlUtilsDhcpv6, Properties wsProperties) throws RemoteException {
    host.readTargets(sqlUtilsDhcpv6);
    Targetv4[] targets = host.getTargets();
    if (targets.length == 1) {
      logger.info("Warning: Got only one target for host.");
    }
    int socketTimeout = PropertiesHandler.getIntProperty(wsProperties, Constants.PropertyNames.DB_SOCKET_TIMEOUT_SEC,
                                                         logger);
    int connTimeout = PropertiesHandler.getIntProperty(wsProperties, Constants.PropertyNames.DB_CONNECT_TIMEOUT_SEC,
                                                       logger);
    for (Targetv4 target : targets) {
      target.readConnectData(sqlUtilsDhcpv6);
      ConnectDatav4 connData = target.getConnectData();
      connData.buildSQLUtilsForTarget(logger, connTimeout, socketTimeout);
      host.copyToDPP(connData);
    }
  }


  private void deleteHostEntryOnDPP(Hostv4 host, SQLUtils sqlUtilsDhcpv6, Properties wsProperties)
                  throws RemoteException {
    Targetv4[] targets = host.getTargets();
    if (targets.length == 1) {
      logger.info("Warning: Got only one target for host.");
    }
    int socketTimeout = PropertiesHandler.getIntProperty(wsProperties, Constants.PropertyNames.DB_SOCKET_TIMEOUT_SEC,
                                                         logger);
    int connTimeout = PropertiesHandler.getIntProperty(wsProperties, Constants.PropertyNames.DB_CONNECT_TIMEOUT_SEC,
                                                       logger);
    for (Targetv4 target : targets) {
      target.readConnectData(sqlUtilsDhcpv6);
      ConnectDatav4 connData = target.getConnectData();
      connData.buildSQLUtilsForTarget(logger, connTimeout, socketTimeout);
      host.deleteOnDPP(connData);
    }
  }
  
  private static void executeDeployHostShellCommands(Hostv4 host, SQLUtils sqlUtilsDhcpv6,
                                                     Properties wsProperties)  throws RemoteException {
    String fileContent = buildDeployHostStartOrderParameterFileContent(host.getMac());
    String startOrder = PropertiesHandler.getProperty(wsProperties,
                        Constants.PropertyNames.Host.START_WORKFLOW_PARTIAL_COMMAND_DEPLOY_HOST, logger);
    executeHostShellCommand(host, wsProperties, fileContent, sqlUtilsDhcpv6, startOrder);
  }

  private static void executeUndeployHostShellCommands(Hostv4 host, SQLUtils sqlUtilsDhcpv6,
                                                     Properties wsProperties) throws RemoteException {
    String ip = host.getAssignedIp();
    if ((ip == null) || (ip.trim().length() < 1)) {
      return;
    }
    String fileContent = buildUndeployHostStartOrderParameterFileContent(ip);
    String startOrder = PropertiesHandler.getProperty(wsProperties,
                        Constants.PropertyNames.Host.START_WORKFLOW_PARTIAL_COMMAND_UNDEPLOY_HOST, logger);
    executeHostShellCommand(host, wsProperties, fileContent, sqlUtilsDhcpv6, startOrder);
  }



    private static void executeHostShellCommand(Hostv4 host, Properties wsProperties, String fileContent,
                        SQLUtils sqlUtilsDhcpv6, String startOrder) throws RemoteException {
      String filename = "DPP.dhcpdConfv6.host." + System.currentTimeMillis() + ".TMP." + Math.random();
      String tmpPath = PropertiesHandler.getProperty(wsProperties,
                                                     Constants.PropertyNames.Host.TMP_FILES_DIR, logger);
      if (!tmpPath.endsWith("/")) {
        tmpPath += "/";
      }

      String command = "echo '" + fileContent + "' > " + tmpPath + filename;
      command += " && " + startOrder + " " + tmpPath + filename;
      command += "; " + "rm " + tmpPath + filename;

      logger.info("Going to execute shell command: " + command);

      for (Targetv4 target : host.getTargets()) {
        Ssh targetSsh = target.getOpenSsh(sqlUtilsDhcpv6);
        try {
          SshTools.exec(targetSsh, command, logger);
        }
        finally {
          target.closeSsh(targetSsh);
        }
      }
    }

    private static String buildUndeployHostStartOrderParameterFileContent(String ip) throws RemoteException {
      StringBuilder s = new StringBuilder("");
      s.append("<Input> ");
      s.append("  <Data ReferenceName=\"IPv4\" ReferencePath=\"com.gip.xyna.3.0.XMDM\" VariableName=\"iPv4\" >");
      s.append("    <Data Label=\"IP\" VariableName=\"IP\" >");
      s.append("      <Value>").append(ip).append("</Value>");
      s.append("    </Data>");
      s.append("  </Data>");
      s.append("</Input>");
      return s.toString();
    }


    private static String buildDeployHostStartOrderParameterFileContent(String mac) throws RemoteException {
      StringBuilder s = new StringBuilder("");
      s.append("<Input> ");
      s.append("  <Data ReferenceName=\"MAC\" ReferencePath=\"com.gip.xyna.3.0.XMDM\" VariableName=\"mac\" >");
      s.append("    <Data Label=\"Mac\" VariableName=\"mac\" >");
      s.append("      <Value>").append(mac).append("</Value>");
      s.append("    </Data>");
      s.append("  </Data>");
      s.append("</Input>");
      return s.toString();
    }
      

}
