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
package com.gip.xyna.xsor.interconnect;



import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import com.gip.xyna.xsor.common.Interconnectable;
import com.gip.xyna.xsor.common.ReplyCode;
import com.gip.xyna.xsor.common.Replyable;
import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.protocol.LinkedBlockingDequeWithLockAccess;



public class InterconnectTest extends TestCase {

  public void test1() throws InterruptedException {
    final InterconnectServer server = new InterconnectServer(1414, 100);
    final InterconnectSender sender = new InterconnectSender("localhost", 1414, 100);
    Thread interconnectServerThread = new Thread(server, server.getClass().getSimpleName() + "-Thread");
    interconnectServerThread.setDaemon(true);
    interconnectServerThread.start();
    Thread interconnectSenderThread = new Thread(sender, sender.getClass().getSimpleName() + "-Thread");
    interconnectSenderThread.setDaemon(true);
    interconnectSenderThread.start();
    server.continueWorking();
    sender.continueWorking();

    final ConcurrentHashMap<Integer, ReplyCode> waitMgmt = new ConcurrentHashMap<Integer, ReplyCode>();
    final LinkedBlockingDequeWithLockAccess senderQueue = new LinkedBlockingDequeWithLockAccess();
    sender.register(new Interconnectable() {

      public void storeReplyCodeAndNotifyWaiting(int corrId, ReplyCode replyCode) {
        waitMgmt.put(corrId, replyCode);
      }


      public void sendSpecialStartedMessage() {
        throw new RuntimeException();
      }


      public void processIncommingRequest(byte[] received, int corrId, Replyable replyable) {
        throw new RuntimeException();
      }


      public LinkedBlockingDequeWithLockAccess getOutgoingQueue() {
        return senderQueue;
      }


      public int getObjectType() {
        return 1;
      }
    });
    server.register(new Interconnectable() {

      public void storeReplyCodeAndNotifyWaiting(int corrId, ReplyCode replyCode) {
        throw new RuntimeException();
      }


      public void sendSpecialStartedMessage() {

      }

      Random r = new Random();
      public void processIncommingRequest(byte[] received, int corrId, Replyable replyable) {
        if (r.nextBoolean()) {
      //    throw new RuntimeException();
        }
        
        int s = 0;
        for (int i = 0; i<10; i++) {
          s += i + 1;
        }
        if (s == 0) {
          System.out.println("a");
        }
        byte[] ret = new byte[4 + 4];
        int ordinal = ReplyCode.OK.ordinal();
        XSORUtil.setInt(ordinal, ret, 0);
        XSORUtil.setInt(corrId, ret, 4);
        replyable.offer(ret, corrId);
      }


      public LinkedBlockingDequeWithLockAccess getOutgoingQueue() {
        throw new RuntimeException();
      }


      public int getObjectType() {
        return 1;
      }
    });

    Thread socketCloserThread = new Thread(new Runnable() {

      public void run() {
        Random r = new Random();
        while (true) {
          try {
            Thread.sleep(r.nextInt(200));
            Socket s;
            String source;
            if (r.nextBoolean()) {
              s = server.getSocket();
              source = "server";
            } else {
              s = sender.getSocket();
              source = "sender";
            }
            if (s != null) {
              System.out.println("------------------------closing socket of " + source + " now!");
              s.close();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

    }, "SocketCloserThread");
    socketCloserThread.setDaemon(true);
    socketCloserThread.start();


    int cnt = 1000;
    long t = System.currentTimeMillis();
    for (int i = 0; i < cnt; i++) {
      byte[] ret = new byte[1 + 4 + 4];
      int transactionCorrId = getNextCorrId();
      ret[0] = (byte) 'T';
      XSORUtil.setInt(1, ret, 1);
      XSORUtil.setInt(transactionCorrId, ret, 5);
      senderQueue.offer(ret);
    }
    System.out.println("queue füllen brauchte " + (System.currentTimeMillis() - t) + "ms");
    t = System.currentTimeMillis();
    //&& System.currentTimeMillis() - t < 40 * 1000
    while (waitMgmt.size() < cnt) {
      System.out.println("received " + waitMgmt.size() + " replies");
      Thread.sleep(200);
    }
    System.out.println("verarbeitung brauchte " + (System.currentTimeMillis() - t) + "ms");
  }


  private AtomicInteger ai = new AtomicInteger(0);


  private int getNextCorrId() {
    return ai.incrementAndGet();
  }

}
