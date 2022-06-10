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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.CredentialsCache;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InfrastructureLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteOrderExecutionInterface.TransactionMode;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RemoteCallXynaOrderCreationParameter;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;

public class RemoteOrderExecution {
  
  static final Logger logger = CentralFactoryLogging.getLogger(RemoteOrderExecution.class);
  
  private static CredentialsCache credentials = CredentialsCache.getInstance();

  private final String nodeName;
  private final RemoteOrderExcecutionLinkProfile orderExecution;
  private final InfrastructureLinkProfile infrastructure;
  private final AtomicInteger startOrderCounter;
  private volatile boolean connected;
  private final String identifier;
  private XFMG_NodeConnectException lastConnectException;
  
  public RemoteOrderExecution(FactoryNode node, String identifier) {
    this.nodeName = node.getNodeInformation().getName();
    this.orderExecution = node.getInterFactoryLink().<RemoteOrderExcecutionLinkProfile>getProfile(InterFactoryLinkProfileIdentifier.OrderExecution);
    this.infrastructure = node.getInterFactoryLink().<InfrastructureLinkProfile>getProfile(InterFactoryLinkProfileIdentifier.Infrastructure);
    startOrderCounter = new AtomicInteger(0);
    this.connected = true; //geraten, aber richtiger Test wäre zu Aufwändig
    this.identifier = identifier;
  }
  
  public String getNodeName() {
    return nodeName;
  }

  public void reconnect() {
    try {
      orderExecution.getAdapter().reconnect();
    } catch (RMIConnectionFailureException e) {
      logger.debug("could not reconnect: " + e.toString());
    }
  }
  
  public OrderExecutionResponse createOrder(final RemoteCallXynaOrderCreationParameter xocp) throws XFMG_NodeConnectException {
    return executeRemoteCommand(new RemoteCommand<OrderExecutionResponse>() {

      public OrderExecutionResponse execute() throws XFMG_NodeConnectException {
        try {
          return orderExecution.createOrder(credentials.getCredentials(nodeName, infrastructure), identifier, xocp, TransactionMode.START);
        } catch(XFMG_NodeConnectException e){
          try {
            logger.debug("createOrder failed. Trying to reconnect.");
            orderExecution.getAdapter().reconnect();
          } catch (RMIConnectionFailureException e1) {
            logger.debug("Reconnecting to remote node failed: " + e1.toString());
            throw e;
          }
          logger.debug("Reconnected successfully");
          return orderExecution.createOrder(credentials.getCredentials(nodeName, infrastructure), identifier, xocp, TransactionMode.START);
        }
        finally {
          startOrderCounter.getAndIncrement();
        }
      }
    });
  }

  public List<RemoteData> awaitData(final long timeoutMillis) throws XFMG_NodeConnectException {
    return executeRemoteCommand(new RemoteCommand<List<RemoteData>>() {

      public List<RemoteData> execute() throws XFMG_NodeConnectException {
        return orderExecution.awaitData(credentials.getCredentials(nodeName, infrastructure), identifier, timeoutMillis);
      }
    });
  }

  public void abortCommunication() throws XFMG_NodeConnectException{
    executeRemoteCommand(new RemoteCommand<Void>() {
      public Void execute() throws XFMG_NodeConnectException {
        orderExecution.abortCommunication(credentials.getCredentials(nodeName, infrastructure), identifier);
        return null;
      }
    });
  }
  
  public List<Long> checkRunningOrders(final List<Long> orderIds) {
    try {
      return executeRemoteCommand(new RemoteCommand<List<Long>>() {

        public List<Long> execute() throws XFMG_NodeConnectException {
          return orderExecution.checkRunningOrders(credentials.getCredentials(nodeName, infrastructure), identifier, orderIds);
        }
      });
    } catch (XFMG_NodeConnectException e) {
      return Collections.emptyList();
    }
    
  }


  public boolean isConnected() {
    return connected;
  }


  public XFMG_NodeConnectException getLastNodeConnectException() {
    return lastConnectException;
  }
  
  
  public boolean checkConnectivity() {
    try {
      orderExecution.getRunningCount(credentials.getCredentials(nodeName, infrastructure), identifier);
      connected = true;
      logger.debug("connected to " + nodeName);
      lastConnectException = null;
    } catch( XFMG_NodeConnectException e ) {
      credentials.clearSession(nodeName);
      try {
        //retry with new session
        orderExecution.getRunningCount(credentials.getCredentials(nodeName, infrastructure), identifier);
        connected = true;
        logger.debug("connected to " + nodeName);
        lastConnectException = null;
      } catch (XFMG_NodeConnectException ee) {
        connected = false;
        logger.debug("disconnected from " + nodeName);
        lastConnectException = ee;
        credentials.clearSession(nodeName);
      }
    }
    return connected;
  }
  
  
  private <O> O executeRemoteCommand(RemoteCommand<O> command) throws XFMG_NodeConnectException {
    try {
      O output = command.execute();
      connected = true;
      logger.debug("connected to " + nodeName);
      lastConnectException = null;
      return output;
    } catch (XFMG_NodeConnectException e) {
      if (checkConnectivity()) {
        try {
          O output = command.execute();
          connected = true;
          logger.debug("connected to " + nodeName);
          lastConnectException = null;
          return output;
        } catch (XFMG_NodeConnectException ee) {
          connected = false;
          logger.debug("disconnected from " + nodeName);
          lastConnectException = ee;
          credentials.clearSession(nodeName);
          throw ee;
        }
      } else {
        throw e;
      }
    }
  }
  
  
  private interface RemoteCommand<O> {
    
    O execute() throws XFMG_NodeConnectException;
    
  }
  
}
