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
/**
 * Optionsv4BindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */
  
package com.gip.www.juno.DHCP.WS.Optionsv4;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.xynadhcp.Optionsv4Handler;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.handler.AuthenticationTools;
import com.gip.juno.ws.handler.ChangeMonitor;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.WebserviceHandler;
import com.gip.juno.ws.handler.tables.DeploymentTools;
import com.gip.juno.ws.tools.ConnectionInfo;
import com.gip.juno.ws.tools.LocationData;
import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.juno.ws.tools.ssh.MySqlDumpTableCopy;
import com.gip.juno.ws.tools.ssh.SshTools;
import com.gip.juno.ws.tools.ssh.TargetSshConnection;
import com.gip.juno.ws.tools.ssh.MySqlDumpTableCopy.MySqlUser;

import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype;
import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.MetaInfoRow_ctype;
import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype;
import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype;
import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype;
import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype;
import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype;
import com.gip.www.juno.DHCP.WS.Optionsv4.Messages.StatusReport_ctype;
import com.gip.www.juno.DHCP.tlvdatabase.ProcessAdmList;

import com.gip.xyna.utils.ssh.Ssh;

public class Optionsv4BindingImpl implements com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4_PortType{
    
    static Logger logger = Logger.getLogger(Optionsv4BindingImpl.class);
    public static final String WS_PROPERTY_RELOAD = "optionsv4.shell.command.reload";
    
    private static TableHandler _handler = new Optionsv4Handler();
  
  public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype getMetaInfo(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest) throws java.rmi.RemoteException {
    try {
            Payload_ctype payload = new Payload_ctype();
            MetaInfoRow_ctype ref = new MetaInfoRow_ctype();
            InputHeaderContent_ctype header = getMetaInfoRequest.getInputHeader();
            String username = header.getUsername();
            String password = header.getPassword();
            List<MetaInfoRow_ctype> list = new WebserviceHandler<MetaInfoRow_ctype>().getMetaInfo(ref, _handler, username,
                                                                                                  password);
            MetaInfoRow_ctype[] ret = list.toArray(new MetaInfoRow_ctype[list.size()]);
            payload.setMetaInfoOutput(ret);
            return createResponse(payload);
          }
          catch (Exception e) {
            _handler.getLogger().error("", e);
            return createResponse("Error", e);
          }
    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype getAllRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
      try {
            Payload_ctype payload = new Payload_ctype();
            Row_ctype ref = new Row_ctype();
            InputHeaderContent_ctype header = getAllRowsRequest.getInputHeader();
            String username = header.getUsername();
            String password = header.getPassword();
            List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().getAllRows(ref, _handler, username, password);
            payload.setGetAllRowsOutput(ret.toArray(new Row_ctype[ret.size()]));
            return createResponse(payload);
          }
          catch (Exception e) {
            _handler.getLogger().error("", e);
            return createResponse("Error", e);
          }
    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype searchRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
      try {
            Payload_ctype payload = new Payload_ctype();
            Row_ctype row = searchRowsRequest.getSearchRowsInput();
            InputHeaderContent_ctype header = searchRowsRequest.getInputHeader();
            String username = header.getUsername();
            String password = header.getPassword();
            List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().searchRows(row, _handler, username, password);
            payload.setSearchRowsOutput(ret.toArray(new Row_ctype[ret.size()]));
            return createResponse(payload);
          }
          catch (Exception e) {
            _handler.getLogger().error("", e);
            return createResponse("Error", e);
          }
    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype updateRow(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      try {
          Payload_ctype payload = new Payload_ctype();
          InputHeaderContent_ctype header = updateRowRequest.getInputHeader();
          String username = header.getUsername();
          String password = header.getPassword();
          Row_ctype input = updateRowRequest.getUpdateRowInput();
          
          ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
          deployInfo.setService(_handler.getTablename() + ".Update");
          deployInfo.setUser(username);
          deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
          Row_ctype before = monitor.queryRowToUpdate(input);

          Row_ctype ret = new WebserviceHandler<Row_ctype>().updateRow(input, _handler, username, password);
          
          String changelog = monitor.buildUpdateString(before, ret);
          deployInfo.setLog(changelog);

          payload.setUpdateRowOutput(ret);
          return createResponse(payload);
      }
      catch (Exception e) {
          deployInfo.setLog(e);
          _handler.getLogger().error("", e);
          return createResponse("Error", e);
      }
      finally {
        DeploymentTools.insertRow(deployInfo);
      }

    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype insertRow(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      try {
            Payload_ctype payload = new Payload_ctype();
            InputHeaderContent_ctype header = insertRowRequest.getInputHeader();
            String username = header.getUsername();
            String password = header.getPassword();
            Row_ctype input = insertRowRequest.getInsertRowInput();

            ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
            String changelog = monitor.buildInsertString(input);
            deployInfo.setService(_handler.getTablename() + ".Insert");
            deployInfo.setUser(username);
            deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
            
            //String newId = getNewId(username, password);
            //input.setId(newId);
            Row_ctype ret = new WebserviceHandler<Row_ctype>().insertRow(input, _handler, username, password);
            payload.setInsertRowOutput(ret);
            
            deployInfo.setLog(changelog);

            return createResponse(payload);
      }
      catch (Exception e) {
            deployInfo.setLog(e);
            _handler.getLogger().error("", e);
            return createResponse("Error", e);
      }
      finally {
        DeploymentTools.insertRow(deployInfo);
      }

    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype deleteRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      try {
            Payload_ctype payload = new Payload_ctype();
            InputHeaderContent_ctype header = deleteRowsRequest.getInputHeader();
            String username = header.getUsername();
            String password = header.getPassword();
            Row_ctype input = deleteRowsRequest.getDeleteRowsInput();
            
            ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
            String changelog = monitor.buildDeleteString(monitor.queryOneRow(input));
            deployInfo.setService(_handler.getTablename() + ".Delete");
            deployInfo.setUser(username);
            deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);

            String ret = new WebserviceHandler<Row_ctype>().deleteRows(input, _handler, username, password);
            payload.setDeleteRowsOutput(ret);
            
            deployInfo.setLog(changelog);

            return createResponse(payload);
          }
          catch (Exception e) {
            deployInfo.setLog(e);
            _handler.getLogger().error("", e);
            return createResponse("Error", e);
          }
      finally {
        DeploymentTools.insertRow(deployInfo);
      }

    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype countRowsWithCondition(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) throws java.rmi.RemoteException {
      try {
            Payload_ctype payload = new Payload_ctype();
            InputHeaderContent_ctype header = countRowsWithConditionRequest.getInputHeader();
            String username = header.getUsername();
            String password = header.getPassword();
            Row_ctype input = countRowsWithConditionRequest.getCountRowsWithConditionInput();
            String ret = new WebserviceHandler<Row_ctype>().deleteRows(input, _handler, username, password);
            payload.setCountRowsWithConditionOutput(ret);
            return createResponse(payload);
          }
          catch (Exception e) {
            _handler.getLogger().error("", e);
            return createResponse("Error", e);
          }
    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype countAllRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException {
      try {
            Payload_ctype payload = new Payload_ctype();
            InputHeaderContent_ctype header = countAllRowsRequest.getInputHeader();
            String username = header.getUsername();
            String password = header.getPassword();
            String ret = new WebserviceHandler<Row_ctype>().countAllRows(_handler, username, password);
            payload.setCountAllRowsOutput(ret);
            return createResponse(payload);
          }
          catch (Exception e) {
            _handler.getLogger().error("", e);
            return createResponse("Error", e);
          }
    }

    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype deployOnDPP(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeployOnDPPRequest_ctype deployOnDPPRequest) throws java.rmi.RemoteException {
      try {
            Payload_ctype payload = new Payload_ctype();
            InputHeaderContent_ctype inputHeader = deployOnDPPRequest.getInputHeader();
//            AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
//            AuthenticationTools.checkPermissions(inputHeader.getUsername(), "optionsv6adm", "deploy", logger);
            AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new AuthenticationTools.WebServiceInvocationIdentifier("deploy");
            AuthenticationTools.authenticateAndAuthorize(inputHeader.getUsername(), inputHeader.getPassword(),
                                                         "optionsv4", wsInvocationId, logger);
            // these are the names of the DPPs the options should be deployed to
            // ConnectionData could by retrieved for those by calling LocationTools.getLocationsRow(location, flag, schema,
            // logger)
            String[] deploymentLocations = deployOnDPPRequest.getDeployOnDPPInput().split(",");// .getLocations();
            logger.info("deploymentLocations: " + deploymentLocations + " #" + deploymentLocations.length);

            StatusReport_ctype[] statusReports = new StatusReport_ctype[deploymentLocations.length];
            for (int i = 0; i < deploymentLocations.length; i++) {
              logger.info("deploymentLocations[i]: " + deploymentLocations[i]);
              statusReports[i] = new StatusReport_ctype(deploymentLocations[i], "Success");
            }
            
            ProcessAdmList pal = new ProcessAdmList();
            pal.process();


            int count = 0;
            for (String deploc : deploymentLocations) {
              try {
                if ((deploc == null) || (deploc.trim().equals(""))) {
                  logger.error("Missing Input value: Location.");
                  throw new DPPWebserviceIllegalArgumentException("Missing Input value: Location.");
                }
                DeploymentTools.Row deployInfo = new DeploymentTools.Row();
                deployInfo.setService("Optionsv4.Deploy");
                deployInfo.setUser(deployOnDPPRequest.getInputHeader().getUsername());
                //deployInfo.setTarget(getTargetString(deployOnDPPRequest.getDeployOnDPPInput()));
                deployInfo.setTarget(getTargetString(deploc));

                List<TargetSshConnection> connections = null;
                try {
//                  connections = TargetSshConnection.getFailoverPairConnections(deployOnDPPRequest.getDeployOnDPPInput(),
//                                                                               logger);
                  connections = TargetSshConnection.getFailoverPairConnections(deploc,
                                                                               logger);
                  initLocation(connections);
                  deployInfo.setLog("Successful.");
                }
                catch (Exception e) {
                  logger.error("Docsis_Encoding: ", e);
                  statusReports[count] = new StatusReport_ctype(deploymentLocations[count], "Failed");
                  deployInfo.setLog("Failed: " + MessageBuilder.stackTraceToString(e));
                  throw e;
                }
                finally {
                  TargetSshConnection.closeConnections(connections);
                  DeploymentTools.insertRow(deployInfo);
                }
              }
              catch (java.rmi.RemoteException e) {
                throw e;
              }
              catch (Exception e) {
                logger.error("", e);
                throw new DPPWebserviceException("Error in Docsis_Encoding: ", e);
              }
              count++;
            }


            payload.setDeployOnDPPResponse(statusReports);
            return createResponse(payload);
          }
          catch (Throwable t) {
            _handler.getLogger().error("", t);
            return createResponse("Error", t);
          }
    }
    
    private Response_ctype createResponse(Payload_ctype payload) {
        Response_ctype ret = new Response_ctype();
        ret.setResponseHeader(new ResponseHeader_ctype());
        ret.getResponseHeader().setDescription("Ok");
        ret.setPayload(payload);
        return ret;
      }
    
    private static Response_ctype createResponse(String classification, Throwable t) {
        ResponseHeader_ctype header = new ResponseHeader_ctype();
        String errorDescription = "";
        if (t instanceof DPPWebserviceModificationCollisionException) {
          header.setParameterList(convertDPPCollisionToErrorParameter_ctype((DPPWebserviceModificationCollisionException)t));
          errorDescription = t.getMessage();
        } else if (t instanceof DPPWebserviceException) {
          errorDescription = t.getMessage();
        } else {
          errorDescription = new MessageBuilder().setDescription(classification).setCause(t).build();
        }
        Response_ctype ret = new Response_ctype();
        ret.setResponseHeader(header);
        ret.getResponseHeader().setDescription(errorDescription);
        return ret;
      }
    
    private static ErrorParameter_ctype[] convertDPPCollisionToErrorParameter_ctype(DPPWebserviceModificationCollisionException e) {
        Set<Entry<String, String>> errorParams = e.getErrorParameter();
        ErrorParameter_ctype[] params = new ErrorParameter_ctype[errorParams.size()];
        Iterator<Entry<String, String>> iterator = errorParams.iterator();
        for (int i = 0; i < errorParams.size(); i++) {
          Entry<String, String> entry = iterator.next();
          params[i] = new ErrorParameter_ctype(entry.getKey(), entry.getValue());
        }
        return params;
      }
    
    //private static Comparator<String> idComperator = new IdComperator();


//    private static class IdComperator implements Comparator<String> {
//
//      public int compare(String o1, String o2) {
//        Integer i1 = Integer.parseInt(o1);
//        Integer i2 = Integer.parseInt(o2);
//        return i1.compareTo(i2);
//      }
//    }
//    
//    private final static String END_OF_DATA_MARKER_ID = "255";
//
//
//    private String getNewId(String username, String password) throws RemoteException {
//      List<String> ids = WebserviceHandler.getColValuesDistinct("id", _handler, username, password);
//      Collections.sort(ids, idComperator);
//      if (ids.get(ids.size() - 1).equals(END_OF_DATA_MARKER_ID)) {
//        int newId = Integer.parseInt(ids.get(ids.size() - 2));
//        return Integer.toString(++newId);
//      } else {
//      int newId = Integer.parseInt(ids.get(ids.size() - 1));
//      return Integer.toString(++newId);
//    }
//  }
    
    private String getTargetString(String location) throws RemoteException {
      String ret = location + " (";
        LocationData instance = LocationData.getInstance(LocationSchema.service, logger);
        ConnectionInfo info = instance.get(location, FailoverFlag.primary, logger);
        ret += info.ssh_ip;
        info = instance.get(location, FailoverFlag.secondary, logger);
        ret += " / " + info.ssh_ip;
        ret += " )";
        return ret;
      }
    
    private void initLocation(List<TargetSshConnection> connections) throws java.rmi.RemoteException {
        Properties properties = PropertiesHandler.getWsProperties();

        MySqlDumpTableCopy dump = new MySqlDumpTableCopy();
        dump.setDatabaseName("xynadhcp");
        dump.addTable("optionsv4");
        dump.setLogger(logger);
        dump.setWebserviceProperties(properties);
        dump.setTargetConnections(connections);
        dump.setMySqlUser(MySqlUser.dhcptrigger);

        dump.execute();

        reloadConfig(connections, properties);

        // getStatus(connections, properties);

      }
    
    private void reloadConfig(List<TargetSshConnection> connections, Properties properties) throws RemoteException {
        String command = PropertiesHandler.getProperty(properties, WS_PROPERTY_RELOAD, logger);
        for (TargetSshConnection conn : connections) {
          Ssh ssh = conn.getSsh();
          logger.info("Going to send command " + command + " over ssh-connection.");
          SshTools.exec(ssh, command, logger);
        }
      }

}
