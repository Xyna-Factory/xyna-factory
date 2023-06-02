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
package gip.base.callback;


import java.rmi.server.RMISocketFactory;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.io.Serializable;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class FixedPortSocketFactory extends RMISocketFactory implements Serializable {

  private transient static Logger logger = Logger.getLogger(FixedPortSocketFactory.class);
  
  protected int basePort;
  protected int range;
  protected int current;
  
  public FixedPortSocketFactory(int basePort, int range) {
    this.basePort = basePort;
    this.range = range;
    this.current = 0;
  }
  
  public ServerSocket createServerSocket(int port) throws IOException {
    //logger.debug("### FixedPortSocketFactory: request: createServerSocket(" + port + ")"); 
    if (0 != port) {
      return new ServerSocket(port);
    }
    ServerSocket ss = null;
    while (true) {
      //logger.debug("### FixedPortSocketFactory: trying: new ServerSocket(" + (basePort + current) + ")");
      try {
        ss = new ServerSocket(basePort + current);
        break;
      }
      catch (IOException iox) {
        logger.debug("### FixedPortSocketFactory: failure: " , iox);//$NON-NLS-1$
        if (current < range-1) {
          current++;
        }
        else {
          throw iox;
        }
      }
    }
    return ss;
  }

  public Socket createSocket(String host, int port) throws IOException {
    //logger.debug("### FixedPortSocketFactory: createSocket(" + host + ", " + port + ")");
    if (0 != port) {
      return new Socket(host, port);
    }
    Socket s = null;
    while (true) {
      //logger.debug("### FixedPortSocketFactory: trying: new Socket(" + host + ", " + (basePort + current) + ")");
      try {
        s = new Socket(host, basePort + current);
        break;
      }
      catch (IOException iox) {
        //logger.debug("### FixedPortSocketFactory: failure: " , iox);
        if (current < range-1) {
          current++;
        }
        else {
          throw iox;
        }
      }
    }
    return s;
  }
}
