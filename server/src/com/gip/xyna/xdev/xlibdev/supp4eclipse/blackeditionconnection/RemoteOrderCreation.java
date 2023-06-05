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
package com.gip.xyna.xdev.xlibdev.supp4eclipse.blackeditionconnection;



import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;


// TODO where does this really belong? at the moment we only use it for development purposes
public class RemoteOrderCreation {

  private static final Logger logger = CentralFactoryLogging.getLogger(RemoteOrderCreation.class);


  /**
   * Start an order at a remote factory using the XynaTCPTrigger protocol
   */
  public static void startOrderRemote(RemoteOrderConnectionConfigObject config,
                                     RemoteOrderPayloadConfigObject payloadConfig) throws XynaException {

    BlackEditionConnectionClient client;
    String sessionID;
    if (config.sessionId == null) {
      client = getNewConnection(config);
      client.sendRequest("getSession", new String[] {config.username, config.password});
      while (client.getResponses().size() == 0) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // ignore
        }
        if (client.receivedError()) {
          throw new XynaException("Error while getting session");
        }
      }

      if (client.receivedError()) {
        BlackEditionConnectionResponse response = client.getResponses().get(0);
        String[] errorDescription = response.getResponseAsListOfStrings();
        for (String s : errorDescription) {
          logger.info("Error info: " + s);
        }
        throw new RemoteOrderExecutionException(
                                                "Got error " + errorDescription[0] + " (message: '" + errorDescription[1] + "')");
      }

      Object[] sessionIdObject = client.getResponses().get(0).getResponseAsComplexStringArray();
      sessionID = (String) sessionIdObject[0];
    } else {
      sessionID = config.sessionId;
    }

    // TODO should this support GeneralXynaObject as payload?
    XynaObject payloadObject = payloadConfig.payload;
    String payloadXml = payloadObject != null ? payloadConfig.payload.toXml("n/a") : "";
    if (payloadConfig.payload instanceof Container && ((Container) payloadConfig.payload).size() > 1) {
      payloadXml = "<payload>" + payloadXml + "</payload>";
    }

    client = getNewConnection(config);
    client.sendRequest("startOrder", new String[] {sessionID, payloadConfig.synchroneously ? "true" : "false",
                    payloadConfig.orderType, payloadConfig.priority + "", payloadXml, payloadConfig.timeout + ""});

    while (!client.isFinished()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // ignore
      }
    }

    if (client.receivedError()) {
      BlackEditionConnectionResponse response = client.getResponses().get(0);
      String[] errorDescription = response.getResponseAsListOfStrings();
      for (String s : errorDescription) {
        logger.info("Error info: " + s);
      }
      throw new RemoteOrderExecutionException(
                                              "Got error " + errorDescription[0] + " (message: '" + errorDescription[1] + "')");
    }

    logger.debug("done");

//    if (client.getResponses().size() > 0) {
//      return XynaObject.fromXml(client.getResponses().get(0).getResponseAsString());
//    } else {
//      return null;
//    }

  }


  private static BlackEditionConnectionClient getNewConnection(RemoteOrderConnectionConfigObject config)
                  throws XynaException {

    InetAddress address = null;
    try {
      address = Inet4Address.getByName(config.remoteHost);
    } catch (UnknownHostException e) {
      throw new XynaException("Could not resolve remote address", e);
    }

    BlackEditionConnectionClient client;
    if (config.useEncryption)
      client = new BlackEditionConnectionClient("deafult", false, true, config.keyStorePath, config.keyStoreType,
                                                config.keyStorePassword);
    else
      client = new BlackEditionConnectionClient();
    client.setEndpointTCP(address, config.remotePort);

    return client;

  }

}
