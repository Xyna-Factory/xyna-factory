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
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.common.Interconnectable;
import com.gip.xyna.xsor.common.ReplyCode;
import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.protocol.LinkedBlockingDequeWithLockAccess;



public class InterconnectSender extends InterconnectableStore implements Runnable {

  private static final Logger logger = Logger.getLogger(InterconnectSender.class.getName());

  private volatile Socket socket;
  private final String address;
  private final int port;
  private int additionalWait = 0;//Verzoegerung fuer Tests
  private volatile boolean running = true;
  private volatile boolean paused = true; //zu beginn pausiert, bis continueWorking gesagt wird
  private final LinkedBlockingDequeWithLockAccess lastSent;
  private final int correctQueueLength;

  private volatile boolean specialStartMessageSent=false;

  static Object notifyObject = new Object();//zur Notifizierung nach dem Einstellen einer Nachricht durch eines der CCMemories
  
  

  /*
   * Constructor
   */
  public InterconnectSender(String targetAddress, int targetPort, int correctLength) {
    port = targetPort;
    address = targetAddress;
    correctQueueLength=correctLength;
    lastSent=new LinkedBlockingDequeWithLockAccess(correctQueueLength);
  }


  /*
   * Notfiziert den InterconnectSender, dass ein Eintrag in die Queue eines CCMemory eingestellt wurde. 
   */
  public static void notifyQueue() {
    synchronized (notifyObject) {
      notifyObject.notifyAll();
    }
  }


  public void run() {
    Thread t = new Thread(new ReplyReader(), ReplyReader.class.getSimpleName() + "-Thread");
    t.start();
    while (running) {
      while (running && !paused) {
        XSORUtil.sleep(additionalWait);//Fuer Testen
        additionalWait = 0;//Fuer Testen
        if (socket == null) {
          try {
            if (logger.isInfoEnabled()) {
              logger.info("opening socket to " + address + ":" + port);
            }
            Socket localSocket = new Socket(address, port);
            localSocket.setSoTimeout(10000);//BUGBUG konfigurierbar
            socket = localSocket;
            Iterator<byte[]> it = lastSent.iterator();
            while (it.hasNext()) {
              byte[] nextItem = it.next();
              int cid = XSORUtil.getInt(5, nextItem);
              if (logger.isDebugEnabled()) {
                logger.debug("CORRECT repeating " + cid + (char) (nextItem[0]));
              }
              byte[] completePackage = InterconnectServer.appendLengthAsHead(nextItem);
              OutputStream os = localSocket.getOutputStream();
              os.write(completePackage);
              os.flush(); // won't do a thing
            }
          } catch (UnknownHostException e) {
            logger.error("Unknown Host " + address, e);
            XSORUtil.sleep(1000);
          } catch (IOException e) {
            logger.error("IO-Exception while opening socket to " + address + ":" + port, e);
            XSORUtil.sleep(1000);
          }
          if (socket != null) {
            if (logger.isDebugEnabled()) {
              logger.debug("Interconnectsender connected to " + address + ":" + port);
            }
          }
        }
        boolean foundMessage = false;
        for (Interconnectable i : registeredXSORMemory.values()) {
          LinkedBlockingDequeWithLockAccess queue = i.getOutgoingQueue();

          byte[] nextItem = queue.pollAndMoveToLastSent(lastSent); //ohne warten, weil man an mehreren queues lauscht!
          if (nextItem != null) {
            if (logger.isDebugEnabled()) {
              int cid = XSORUtil.getInt(5, nextItem);
              logger.debug("CORRECT sending message " + cid + (char) (nextItem[0]) + " " + nextItem.length);
            }
            foundMessage = true;
            try {
              Socket localSocket=socket;
              if (localSocket!=null){
                synchronized(localSocket){
                  OutputStream os = localSocket.getOutputStream();
                  byte[] completePackage = InterconnectServer.appendLengthAsHead(nextItem);
                  os.write(completePackage);
                  os.flush(); // won't do a thing
                }
              } else {
                throw new Exception("Could not sent Message");
              }
            } catch (Exception e) {
              logger.error("could not send last message from queue. retrying after new socket could be opened.", e);
              queue.addFirstAndRemoveFromLastSent(nextItem, lastSent);
              closeSocket();
              break;
            }
            //FIXME wenn jetzt ein fehler im netzwerk passiert, kann es passieren, dass man diese nachricht verliert, 
            //weil nicht auf eine antwort des anderen knoten gewartet wird.
            //und hat evtl zusätzlich einen synchron wartenden client (waitmanagement). der läuft dann in ein timeout...
          }

        }
        if (!foundMessage) {//In keiner der Queues eine Nachricht gefunden.


          //Potentiell könnte aber inzwischen eine weitere
          //Nachricht eingetroffen sein.
          //Warte deshalb nur eine kurze Zeit
          /*
           * TODO man könnte hier effizienter sein, indem man länger wartet und innerhalb des synchronized
           * blocks einen modcounter der queues abfragt.
           * 
           * lastModCounter = modCounter;
           * ...
           * synchronized (notifyObject) {
           *   while (modCounter == lastModCounter && running) {
           *     notifyObject.wait();
           *   }
           * }
           */
          try {
            synchronized (notifyObject) {
              notifyObject.wait(100);
            }
          } catch (InterruptedException e) {
          }
        }
      }
      //auf continueworking warten
      waitForNotification();

    }

    closeSocket();
    logger.info(Thread.currentThread().getName() + " shut down");
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


  /*
   * Für Testzwecke Einfuegen einer Verzoegerung
   */
  public void setAdditionalWait(int i) {
    additionalWait = i;
  }
  
  
  private static byte[] searchForCorrId(int corrId, BlockingQueue<byte[]> lastSent) {
    Iterator<byte[]>it=lastSent.iterator();
    while (it.hasNext()){
      byte[] candidate=it.next();
      if (XSORUtil.getInt(5, candidate)==corrId){//gefunden
        return candidate;
      }
    }
    return null;
  }

  //Runnable zum Auslesen der Replies
  class ReplyReader implements Runnable {

    public void run() {//Besser als Callback=> Abhängigkeiten
      while (running) {
        while (running && !paused) {
          Socket localSocket = socket;
          if (localSocket != null) {
            byte[] answer = new byte[8];
            boolean lastSentWasNotEmpty=lastSent.size()>0;
            try {
              if (-1 == localSocket.getInputStream().read(answer, 0, 8)) {
                //eof
                synchronized(localSocket){
                  if(localSocket==socket){//d.h. socket ist kaputt
                    closeSocket();//potentiell wird neu geoeffneter Socket geschlossen, aber das ist ok
                  }
                }
                continue;
              }
            } catch(SocketTimeoutException ste){
              answer = null;
              if(lastSentWasNotEmpty){//Timeout abgelaufen, obwohl noch auf Antwort gewartet wird
                logger.info("Timeout reading reply.", ste);
                closeSocket();
              } else {
                continue;
              }
              
            } catch (IOException e) {
              answer = null;
              logger.error("Error reading reply.", e);
              closeSocket();
            }
            if (answer != null && !paused) {
              try {
                int corrId = XSORUtil.getInt(4, answer);
                byte[] sentBytes=lastSent.poll();
                if(sentBytes==null){
                  //sollte eigtl nicht vorkommen. wenn, dann haben wir zumindest keinen request zu 
                  //diesem reply verschickt und deshalb kann da keiner drauf warten. -> ignorieren
                  logger.warn("Discarding unexpected Reply, corrID=" + corrId + ". ");
                  continue;
                } else {
                  int sentCorrId = XSORUtil.getInt(5, sentBytes);
                  if (corrId != sentCorrId) {
                    logger.error("CORRECT Receiving Reply for wrong request, corrID=" + corrId + "!=" + sentCorrId
                        + "=sentCorrId");//Should not happen!!
                    byte[] elementWithCorrId=searchForCorrId(corrId, lastSent);
                    if (elementWithCorrId==null){
                      lastSent.addFirst(sentBytes);
                      logger.error("CORRECT got multiple reply or reply for message not sent");
                      
                    } else {
                      //Runtime Exception on the other side. oder wurde remotely verworfen.
                      
                      //repair lastSent-Queue: entferne alles bis zu der empfangenen correlationid                      
                      do {//empty queue until element with corrid found
                        sentBytes=lastSent.poll();
                        sentCorrId = XSORUtil.getInt(5, sentBytes);
                        logger.error("CORRECT REMOVING "+sentCorrId);
                      } while (sentBytes!=elementWithCorrId && lastSent.size()>0/*zur Sicherheit gegen Endlosschleife*/);
                    }
                    
                  } else {
                    if (logger.isDebugEnabled()) {
                      logger.debug("CORRECT out, corrID=" + corrId);
                    }
                  }
                }
                
                ReplyCode replyCode = ReplyCode.values()[XSORUtil.getInt(0, answer)];
                int objectType = XSORUtil.getInt(1, sentBytes);
                Interconnectable interconnectable = registeredXSORMemory.get(objectType);
                if (interconnectable != null) {
                  interconnectable.storeReplyCodeAndNotifyWaiting(corrId, replyCode);
                } else {
                  throw new RuntimeException("objectType " + objectType + " unknown. request="
                      + XSORUtil.prettyPrint(sentBytes) + " reply=" + XSORUtil.prettyPrint(answer));
                }
              } catch (Exception e) {
                logger.error("Error processing reply.", e);
              }
            }
          } else {//Socket==null
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
            }
          }
        }
        //auf continueworking warten
        waitForNotification();
      }
      logger.info(Thread.currentThread().getName() + " shut down");
    }
  }
    


  public void shutdown() {
    synchronized (notifyObject) {
      running = false;
      notifyObject.notifyAll();
    }
  }


  public void pauseWorking() {
    synchronized (notifyObject) {
      paused = true;
      notifyObject.notifyAll();
    }
  }


  public int continueWorking() {
    int n = 0;
    closeSocket();
    for (Interconnectable interconnectable : registeredXSORMemory.values()) {
      n += interconnectable.getOutgoingQueue().size(); //immer zählen
      if (!specialStartMessageSent) {
        interconnectable.sendSpecialStartedMessage();
      }
    }
    specialStartMessageSent = true;

    synchronized (notifyObject) {
      paused = false;
      notifyObject.notifyAll();
    }
    return n;
  }


  private void closeSocket() {
    Socket oldSocket=socket;
    socket=null;
    try {
      if (oldSocket!=null){
        oldSocket.close();
      }
    }
    catch (IOException e) {
      logger.error("error closing socket",e);
    }
  }


  public void clearQueue() {
    lastSent.clear();
  }

  //für Tests
  Socket getSocket() {
    return socket;
  }


}
