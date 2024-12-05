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

package com.gip.xyna.xsor.interconnect;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.common.Interconnectable;
import com.gip.xyna.xsor.common.Replyable;
import com.gip.xyna.xsor.common.XSORUtil;


public class InterconnectServer extends InterconnectableStore implements Runnable, Replyable {

  private static final Logger logger = Logger.getLogger(InterconnectServer.class.getName());
  private final static int SIZE_ENCODED_MSG_LENGTH = 8;
  private static final int DEFAULT_BUFFER_SIZE = 65536;

  InetSocketAddress serverAddress = null;
  private ServerSocket serverSocket = null;
  private Socket socket = null;
  private boolean socketClosedForWriting=true;
  
  private volatile boolean running = true;
  private volatile boolean paused = true;
  private final Object notifyObject = new Object();
  
  private final int[] lastReceivedCorrId;
  private final byte[][] lastReplied;
  private int lastIndex=0;
  private final int corrQueueLength;

  public InterconnectServer(int port, int correctQueueLength) {
    corrQueueLength=correctQueueLength;
    lastReceivedCorrId=new int[corrQueueLength];
    lastReplied=new byte[corrQueueLength][];
    lastIndex=0;

    serverAddress = new InetSocketAddress(port);

    serverSocket = getOrCreateServerSocket();
  }

  private ServerSocket getOrCreateServerSocket() {
    if (serverSocket != null && serverSocket.isBound() && !serverSocket.isClosed()) {
      if (logger.isDebugEnabled()) {
        logger.debug("returned serversocket: " + serverSocket.toString() + " bound: " + serverSocket.isBound()
            + " closed: " + serverSocket.isClosed());
      }

      return serverSocket;
    }

    if (serverSocket != null && !serverSocket.isClosed()) {
      try {
        serverSocket.close();
      } catch (IOException e) {
        logger.warn("error closing serversocket.", e);
      }
    }

    try {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
    } catch (IOException e) {
      logger.error("error creating serversocket.", e);
    }

    while (!serverSocket.isBound()) {
      try {
        serverSocket.bind(serverAddress);
      } catch (IOException e) {
        logger.error("IO-Exception while opening serversocket port " + serverAddress.getPort(), e);
        XSORUtil.sleep(1000);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("created serversocket: " + serverSocket.toString());
    }

    return serverSocket;
  }

  private byte[] temp;

  public void run() {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];//wird bei Bedarf unten vergroessert
    while (running) {
      try {
        while (running && !paused) {
          ServerSocket s = getOrCreateServerSocket();
          if (s != null && !s.isClosed()) {
            Socket localSocket = s.accept();
            if (logger.isInfoEnabled()) {
              logger.info("got new socket from " + localSocket.getInetAddress() + ":" + localSocket.getPort());
            }
            socket = localSocket;
            socketClosedForWriting=false;
            InputStream is = localSocket.getInputStream();
            while (running && !paused) {
              if (!readInBuffer(is, buffer, SIZE_ENCODED_MSG_LENGTH)) {
                if (logger.isInfoEnabled()) {
                  logger.info("eof reached");
                }
                break;
              }
              int msgLength;
              if (0 > (msgLength = getLength(buffer))) {
                throw new IOException("Incoming bytes were gibberish.");
              }
              if (logger.isTraceEnabled()) {
                logger.trace("got valid message length = " + msgLength);
              }
              if (buffer.length < msgLength) {
                if (logger.isDebugEnabled()) {
                  logger.debug("increasing bufferlength to accommodate new msgLength");
                }
                try {
                  buffer = new byte[msgLength];
                } catch (OutOfMemoryError e) {
                  //speicher ist nun wieder freigegeben.
                  buffer = new byte[DEFAULT_BUFFER_SIZE];
                  throw new IOException("message length larger than expected (" + msgLength + ").", e);
                }
              }
              if (!readInBuffer(is, buffer, msgLength)) {
                if (logger.isDebugEnabled()) {
                  logger.debug("eof reached");
                }
                break;
              }
              if (logger.isTraceEnabled()) {
                logger.trace("msg has been read from stream successfully.");
              }
              if(buffer[0]=='c'){
                for(int i=0;i<corrQueueLength;i++){
                  lastReceivedCorrId[i]=0;
                }
                logger.info("CORRECT corridbuffer cleared");
              }
              int objectType = XSORUtil.getInt(1, buffer);
              Interconnectable interconnectable = registeredXSORMemory.get(objectType);
              if (interconnectable != null) {
                byte[] request;
                if (temp != null && temp.length == msgLength) {
                  System.arraycopy(buffer, 0, temp, 0, msgLength);
                  request = temp;
                } else {
                  request = Arrays.copyOf(buffer, msgLength); //erstellt neues byte[]
                  temp = request;
                }
                int corrId=XSORUtil.getInt(5, request);
                boolean alreadyProcessed=false;
                for(int i=0;i<corrQueueLength;i++){
                  if (lastReceivedCorrId[i] == corrId) {//Doppelt empfangene Nachricht
                    alreadyProcessed = true;
                    if (lastReplied[i] != null) {
                      this.offerInternally(lastReplied[i]);//Antwort wiederholen
                      if (logger.isDebugEnabled()) {
                        logger.debug("CORRECT request corrId=" + corrId + "received another time. Repeating reply.");
                      }
                    } else {
                      if (logger.isDebugEnabled()) {
                        logger.debug("CORRECT request corrId=" + corrId + "received another time. Ignoring.");
                      }
                      //sollte noch auf dem Flug sein, also einfach ignorieren
                    }
                    break; //kann nicht mehrfach im ringbuffer stehen.
                  }         
                }
                if(!alreadyProcessed){
                  int lastIndexTemp = lastIndex;
                  lastReplied[lastIndexTemp]=null;
                  
                  lastReceivedCorrId[lastIndexTemp]=corrId;
                  lastIndex=(lastIndex+1)%corrQueueLength;
                  boolean success = false;
                  try {
                    interconnectable.processIncommingRequest(request, corrId, this);
                    success = true;
                  } catch (Exception e) {
                    logger.error("Exception processing request.", e);
                  } finally {
                    if (!success) {
                      if (lastReplied[lastIndexTemp] == null) {
                        
                      }
                    }
                  }
                }
                
              } else {
                logger.warn("Dispatching failed. objectType=" + objectType);
              }
            }
          }
        }
        waitForNotification();
      } catch (IOException e) {
        closeSocket();
        logger.error("IOException reading data ", e);
      } catch (Exception e) {
        closeSocket();
        logger.error("Exception reading data ", e);
      }
    }
    logger.info(Thread.currentThread().getName() + " shut down");
  }
  
  private void closeSocket() {
    Socket s = socket;
    if (s != null) {
      socket = null;
      socketClosedForWriting=true;
      if (logger.isDebugEnabled()) {
        logger.debug("closing open connection.");
      }
      try {
        s.close();
      } catch (IOException e) {
        logger.warn("could not close connection", e);
      }
    }
  }


  /**
   * bestimmt die länge aus dem 8 byte langen buffer, mit gleichzeitigem check, ob die länge korrekt kodiert wurde.
   * damit man sicher sein kann, dass man nicht als länge das ende einer alten nachricht ausliest.
   * 
   * L(i) gleich das i-te byte des ints, der die länge bestimmt.
   * 
   * 0. 42
   * 1. L(0)
   * 2. L(1)
   * 3. L(2)
   * 4. L(3)
   * 5. L(0)^L(1)
   * 6. L(2)^L(3)
   * 7. -117
   */
  private static int getLength(byte[] buffer) {
    byte l0, l1, l2, l3;
    if (buffer[0] == 42) {
      if (buffer[7] == -117) {
        l0 = buffer[1];
        l1 = buffer[2];
        l2 = buffer[3];
        l3 = buffer[4];
        if ((l0 ^ l1) == buffer[5]) {
          if ((l2 ^ l3) == buffer[6]) {
            return XSORUtil.getInt(1, buffer);
          }
        }
      }
    }
    return -1;
  }


  public static byte[] appendLengthAsHead(byte[] input) throws IOException {
    int length = input.length;
    byte[] completePackage = new byte[8 + length];
    completePackage[0] = (byte) 42;
    byte l0 = (byte) (length >>> 24);
    byte l1 = (byte) (length >>> 16);
    byte l2 = (byte) (length >>> 8);
    byte l3 = (byte) (length >>> 0);
    completePackage[1] = l0;
    completePackage[2] = l1;
    completePackage[3] = l2;
    completePackage[4] = l3;
    completePackage[5] = (byte) (l0 ^ l1);
    completePackage[6] = (byte) (l2 ^ l3);
    completePackage[7] = (byte) -117;   
    System.arraycopy(input, 0, completePackage, 8, length);
    return completePackage;
  }

  private void waitForNotification() {
    synchronized (notifyObject) {
      if (running && paused) {
        try {
          notifyObject.wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }


  private boolean readInBuffer(InputStream is, byte[] buffer, int size) throws IOException {
    int read = 0;
    while (read < size) {
      int length = is.read(buffer, read, size - read);
      if (length == -1) {
        //eof
        return false;
      }
      read = read + length;
    }
    return true;
  }
  
  private void offerInternally(byte[] reply) {
    Socket s = socket;
    if (s != null) {
      if (!socketClosedForWriting){
        try {
          s.getOutputStream().write(reply);
          s.getOutputStream().flush();
        } catch (IOException e) {
          logger.error("IOException offering reply:" + XSORUtil.prettyPrint(reply), e);
          closeSocket();
        }
      } else {
        logger.debug("Socket is still closed. Not sending reply. Reply will be sent due to repeated request. ");
      }
    } else {
      logger.error("could not send reply. socket was closed");
    }
  }


  public void offer(byte[] reply, int corrId) {
    //dieser aufruf kommt von processIncomingRequest, normalerweise weiss man hier noch
    //den index, der die aktuelle corrId enthält.
    int guessedIndex = (lastIndex - 1 + corrQueueLength) % corrQueueLength;
    for (int i = guessedIndex; i < corrQueueLength; i++) {
      if (corrId == lastReceivedCorrId[i]) {
        lastReplied[i] = reply;
        offerInternally(reply);
        return;
      }
    }
    for (int i = 0; i < guessedIndex; i++) {
      if (corrId == lastReceivedCorrId[i]) {
        lastReplied[i] = reply;
        offerInternally(reply);
        return;
      }
    }
    throw new RuntimeException("corrId " + corrId + " not found in lastReceivedCorrId array");
  }


  public void shutdown() {
    synchronized (notifyObject) {
      running = false;
      notifyObject.notifyAll();
    }
    try {
      ServerSocket s = serverSocket;
      serverSocket = null;
      s.close();
    } catch (IOException e) {
    }
  }


  public void pauseWorking() {
    synchronized (notifyObject) {
      paused = true;
      notifyObject.notifyAll();
    }
    // sicherstellen, dass nicht weiter vom socket-inputstream gelesen wird und keine neuen Verbindungen geöffnet werden.
    if (serverSocket != null && !serverSocket.isClosed()) {
      try {
        serverSocket.close();
        logger.debug("closed serversocket: " + serverSocket.toString());
      } catch (IOException e) {
        logger.warn("error closing serversocket.", e);
      }
    }
    closeSocket();
  }


  public void continueWorking() {
    closeSocket(); //sollte kein offenes socket vorfinden!
    synchronized (notifyObject) {
      paused = false;
      notifyObject.notifyAll();
    }
  }


  public void clearQueue() {
    for (int i = 0; i < corrQueueLength; i++) {
      lastReplied[i] = null;
      lastReceivedCorrId[i] = 0;
    }
  }

  //für Tests
  Socket getSocket() {
    return socket;
  }


}
