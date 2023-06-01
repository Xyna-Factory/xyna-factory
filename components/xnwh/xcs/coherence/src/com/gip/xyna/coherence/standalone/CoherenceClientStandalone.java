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

package com.gip.xyna.coherence.standalone;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Random;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionClientParameters;
import com.gip.xyna.coherence.remote.CacheControllerRemoteInterface;
import com.gip.xyna.coherence.remote.CacheControllerRemoteInterfaceImpl;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;



public class CoherenceClientStandalone {


  private static final Logger logger = LoggerFactory.getLogger(CoherenceClientStandalone.class);


  public static void main(String[] args) throws UnknownHostException, RMIConnectionFailureException, RemoteException {

    RMIConnectionClientParameters parameters =
        new RMIConnectionClientParameters(CacheControllerRemoteInterface.DEFAULT_REMOTE_INTERFACE_PORT,
                                          InetAddress.getLocalHost(), CacheControllerRemoteInterfaceImpl.RMI_NAME);
    GenericRMIAdapter<CacheControllerRemoteInterface> rmiAdapter =
        new GenericRMIAdapter<CacheControllerRemoteInterface>(parameters.getHostName(), parameters.getPort(), parameters.getRMIBindingName());

    if (args.length == 0) {
      System.out.println("Usage: <action> <object id>");
      return;
    }

    if ("read".equals(args[0])) {
      long objectId = parseObjectID(args);
      CoherencePayload result;
      try {
        result = rmiAdapter.getRmiInterface().read(objectId);
      } catch (RemoteException e) {
        if (e.getCause() != null) {
          String msg = e.getCause().getMessage();
          msg = msg.substring(0, msg.indexOf("; nested"));
          System.out.println("Error: " + msg);
        } else {
          System.out.println("Unknown error, see log file for more information");
        }
        logger.debug(null, e);
        return;
      }
      System.out.println("Read object <" + objectId + ">: " + result);
    } else if ("lock".equals(args[0])) {
      long objectId = parseObjectID(args);
      System.out.println("Obtaining lock for object <" + objectId + ">...");
      long sessionId = new Random().nextLong();
      try {
        rmiAdapter.getRmiInterface().lock(objectId, sessionId);
      } catch (RemoteException e) {
        if (e.getCause() != null) {
          String msg = e.getCause().getMessage();
          msg = msg.substring(0, msg.indexOf("; nested"));
          System.out.println("Error: " + msg);
        } else {
          System.out.println("Unknown error, see log file for more information");
        }
        logger.debug(null, e);
        return;
      }
      System.out.println("Object <" + objectId + ">. Session ID for unlock: <" + sessionId + ">");
    } else if ("unlock".equals(args[0])) {
      long objectId = parseObjectID(args);
      if (args.length < 3) {
        throw new IllegalArgumentException("Three parameters required: <unlock> <object id> <session id>");
      }
      long sessionId = Long.valueOf(args[2]);
      try {
        rmiAdapter.getRmiInterface().unlock(objectId, sessionId);
      } catch (RemoteException e) {
        if (e.getCause() != null) {
          String msg = e.getCause().getMessage();
          msg = msg.substring(0, msg.indexOf("; nested"));
          System.out.println("Error: " + msg);
        } else {
          System.out.println("Unknown error, see log file for more information");
        }
        logger.debug(null, e);
        return;
      }
      System.out.println("Object <" + objectId + "> unlocked");
    } else if ("create".equals(args[0])) {

      if (args.length < 2) {
        System.out.println("Error: No content specified");
        return;
      }
      String text = args[1];

      long objectId;
      try {
        objectId = rmiAdapter.getRmiInterface().create(new TestCoherencePayload(text));
      } catch (RemoteException e) {
        if (e.getCause() != null) {
          String msg = e.getCause().getMessage();
          msg = msg.substring(0, msg.indexOf("; nested"));
          System.out.println("Error: " + msg);
        } else {
          System.out.println("Unknown error, see log file for more information");
        }
        logger.debug(null, e);
        return;
      }
      System.out.println("Created new object <" + objectId + ">");
    }

  }


  private static long parseObjectID(String[] args) {
    if (args.length < 2) {
      throw new IllegalArgumentException("Two parameters required: <action> <object id>");
    }
    return Long.valueOf(args[1]);
  }

}
