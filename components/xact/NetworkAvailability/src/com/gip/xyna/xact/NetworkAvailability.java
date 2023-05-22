/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xact;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;



public class NetworkAvailability {

  static enum NetworkState {
    OK, ERROR;
  }

  private abstract class InterruptableDatagramSocketUser implements Runnable {

    private Thread t;
    private CountDownLatch stopLatch;
    protected DatagramSocket socket = null;
    private final String name;


    public InterruptableDatagramSocketUser(String name) {
      this.name = name;
    }


    public void start() {
      stopLatch = new CountDownLatch(1);
      t = new Thread(this, name + "-Thread");
      t.setDaemon(true);
      t.start();
    }


    public abstract void run2() throws Exception;


    public void run() {
      try {
        while (running.get()) {
          try {
            run2();
          } catch (InterruptedException e) {
          } catch (Exception e) {
            logger.error(null, e);
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }
          } finally {
            if (socket != null) {
              socket.close();
            }
          }
        }
        stopLatch.countDown();
      } catch (Throwable t) {
        System.exit(1); //irgendetwas ist schief gegangen -> kann man nicht mehr retten, also neu starten.
      }
    }


    public void interrupt() {
      t.interrupt();
      try {
        stopLatch.await();
      } catch (InterruptedException e) {
      }
    }
  }

  private class Receiver extends InterruptableDatagramSocketUser {

    public Receiver() {
      super(Receiver.class.getSimpleName());
    }


    public void run2() throws IOException {
      boolean stateOfOtherNode = false;
      socket = new DatagramSocket(localPortReceive);
      socket.setSoTimeout(timeoutMs);
      byte[] buf = new byte[8];
      while (running.get()) {
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        try {
          socket.receive(p);
          long n = getNumber(p.getData());
          int m = (int) (n % 2);
          n = n / 2;
          if (logger.isTraceEnabled()) {
            logger.trace("received " + n + ", " + m);
          }
          if (n > lastSent.get()) {
            lastReceived.set(n);
            boolean newStateOfOtherNode = m == 1;
            if (stateOfOtherNode != newStateOfOtherNode) {
              NetworkState newState = newStateOfOtherNode ? NetworkState.OK : NetworkState.ERROR;
              if (logger.isInfoEnabled()) {
                logger.info("adapting to state change of other node: " + newState + ".");
              }
              changeState(newState);
            }
            stateOfOtherNode = newStateOfOtherNode;
          }
          //else nicht erwartete antwort

          synchronized (sender) {
            sender.notify();
          }
        } catch (SocketTimeoutException e) {
          //timeout

        }
      }
    }

  }

  private class Sender extends InterruptableDatagramSocketUser {

    public Sender() {
      super(Sender.class.getSimpleName());
    }


    public void run2() throws Exception {

      socket = new DatagramSocket(localPortSend);
      while (running.get()) {
        while (running.get() && factoryAvailable.get()) {
          long lastr = lastReceived.get();
          if (lastr > lastSent.get()) {
            //send now, seit dem letzten senden hat offenbar der andere korrekt geantwortet

            long n = lastr + 1;
            long currentTime = System.currentTimeMillis();

            send(socket, n);

            long diff = currentTime - lastSentTime.get();
            if (diff < shortTimeoutMs) {
              successfulResponses.put(currentTime, diff);
              sumDurationSuccessfulResponses += diff;
            } else if (logger.isTraceEnabled()) {
              logger.trace("unsuccessful: " + n + " was sent after " + diff + "ms (>= " + shortTimeoutMs + "ms).");
            }

            lastSent.set(n);
            lastSentTime.set(currentTime);

            waitForRate();
          } else if (lastSentTime.get() + timeoutMs < System.currentTimeMillis()) {
            //timeout

            long n = lastSent.get() + 2;
            long currentTime = System.currentTimeMillis();

            send(socket, n);

            lastSent.set(n);
            lastSentTime.set(currentTime);

            waitUntilReceiveOrTimeout(lastr);
          } else {
            //erneut checken

            waitUntilReceiveOrTimeout(lastr);
          }
        }
        if (!factoryAvailable.get()) {
          changeState(NetworkState.ERROR);
          Thread.sleep(1000);
        }
      }
    }


    private void waitUntilReceiveOrTimeout(long lastReceive) throws InterruptedException {
      checkState();
      boolean waitForReceive = false;
      do {
        synchronized (this) {
          waitForReceive = lastReceived.get() == lastReceive;
          if (waitForReceive) {
            if (logger.isTraceEnabled()) {
              logger.trace("waiting for receive or timeout for max "
                  + Math.max(0, (lastSentTime.get() + timeoutMs - System.currentTimeMillis())) + " ms");
            }
            wait(Math.max(1, lastSentTime.get() + timeoutMs - System.currentTimeMillis()));
          }
        }
        //solange bis eine bedingung davon nicht mehr erf�llt ist: d.h. entweder timeout ist abgelaufen oder receive wurde empfangen
      } while (waitForReceive && System.currentTimeMillis() < lastSentTime.get() + timeoutMs);
    }


    private void waitForRate() throws InterruptedException {
      checkState();
      do {
        Thread.sleep((long) Math.max(0, lastSentTime.get() + (long) sendIntervalMs - System.currentTimeMillis()));
      } while (System.currentTimeMillis() < lastSentTime.get() + sendIntervalMs);
    }

  }

  public interface FactoryChecker {

    /**
     * gibt true zur�ck, falls factory verf�gbar. ansonsten false.
     */
    public boolean check() throws InterruptedException;
  }

  private class FactoryCheckerThread implements Runnable {

    public void start() {
      Thread t = new Thread(this, FactoryCheckerThread.class.getSimpleName());
      t.setDaemon(true);
      t.start();
    }


    public void run() {
      while (running.get()) {
        try {
          factoryAvailable.set(factoryChecker.check());
          if (logger.isTraceEnabled()) {
            logger.trace("factoryAvailable = " + factoryAvailable.get() + ", state = " + state);
          }
          try {
            Thread.sleep((long) (1000 / factoryCheckRate));
          } catch (InterruptedException e) {
          }
        } catch (Exception e) {
          logger.warn(null, e);
          factoryAvailable.set(false);
        } catch (Error e) {
          System.exit(1); //irgendwas ist schief gelaufen -> nun kann der andere knoten merken, dass man nicht mehr erreichbar ist...
        }
      }
    }

  }

  public interface StateChangeHandler {

    public boolean stateChange(NetworkState newState);
  }


  private static final Logger logger = Logger.getLogger(NetworkAvailability.class.getName());
  private volatile NetworkState state = NetworkState.ERROR;
  private final AtomicBoolean running = new AtomicBoolean(false);

  private final AtomicBoolean factoryAvailable = new AtomicBoolean(true);
  private final AtomicLong lastReceived = new AtomicLong(-1);
  private final AtomicLong lastSent = new AtomicLong(-2);
  private final AtomicLong lastSentTime = new AtomicLong(0);


  private int remotePort;
  private int localPortReceive;
  private int localPortSend;
  private InetAddress remoteHost;

  private final SortedMap<Long, Long> successfulResponses = new TreeMap<Long, Long>(); //<zeitstempel, zeitspanne zwischen 2 sends 
  private long sumDurationSuccessfulResponses;
  private StateChangeHandler stateChangeHandler;
  private FactoryChecker factoryChecker;

  private double factoryCheckRate = 0.1; //hz
  private double percentageLow = 0.6;
  private double percentageHigh = 0.9;
  private long analysisIntervalLength = 30000;
  private int timeoutMs = 3000; //maximale wartezeit auf antwort und gleichzeitig maximale zeit, bis der sender erneut sendet
  private int shortTimeoutMs = 200; //maximales zeitfenster, in dem eine antwort als rechtzeitig erhalten gilt
  private double sendIntervalMs = 200; //ms

  private Sender sender;
  private Receiver receiver;
  private FactoryCheckerThread checker;

  private boolean master;


  public NetworkAvailability(FactoryChecker factoryChecker, double factoryCheckRate,
                             StateChangeHandler stateChangeHandler, double percentageLow, double percentageHigh,
                             long intervalLength, int timeoutMs, int shortTimeoutMs, double rate) {
    this.factoryChecker = factoryChecker;
    this.factoryCheckRate = factoryCheckRate;
    this.stateChangeHandler = stateChangeHandler;
    this.percentageHigh = percentageHigh;
    this.percentageLow = percentageLow;
    this.analysisIntervalLength = intervalLength;
    this.timeoutMs = timeoutMs;
    this.shortTimeoutMs = shortTimeoutMs;
    this.sendIntervalMs = 1000 / rate;
  }


  private long getNumber(byte[] data) throws IOException {
    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
    return dataInputStream.readLong();
  }


  private byte[] createBytes(long n, NetworkState s) throws IOException {
    n = n * 2;
    if (s == NetworkState.OK) {
      n += 1;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(baos);
    dataOutputStream.writeLong(n);
    dataOutputStream.flush();
    return baos.toByteArray();
  }


  private void send(DatagramSocket socket, long n) throws IOException {
    NetworkState s = state;
    byte[] buf = createBytes(n, s);
    DatagramPacket p = new DatagramPacket(buf, buf.length);
    p.setAddress(remoteHost);
    p.setPort(remotePort);
    socket.send(p);
    if (logger.isTraceEnabled()) {
      logger.trace("sent " + n + (s == NetworkState.OK ? ", 1" : ", 0"));
    }
  }


  private ExecutorService executorThread = Executors.newSingleThreadExecutor();


  private void changeState(final NetworkState newState) {
    if (state != newState) {
      //asynchron ausf�hren, damit nicht das versenden weiterer pakete verz�gert wird.
      executorThread.execute(new Runnable() {

        public void run() {
          if (state != newState) {
            if (stateChangeHandler.stateChange(newState)) {
              state = newState;
              if (logger.isInfoEnabled()) {
                logger.info("newState = " + newState + " was communicated successfully via script.");
              }
            }
          }
        }
      });
    }
  }


  private void checkState() {
    //alle alten eintr�ge entfernen
    long min = System.currentTimeMillis() - analysisIntervalLength;
    removeOldEntries(min);

    double percentage = Math.min(1, 1.0 * sumDurationSuccessfulResponses / analysisIntervalLength);

    if (logger.isDebugEnabled()) {
      if (logger.isTraceEnabled()) {
        logger.trace("current percentage = " + percentage + " [" + sumDurationSuccessfulResponses + " / "
            + analysisIntervalLength + "]" + successfulResponses.toString());
      } else {
        logger.debug("current percentage = " + percentage + " [" + sumDurationSuccessfulResponses + " / "
            + analysisIntervalLength + "]");
      }
    }
    if (percentage < percentageLow) {
      changeState(NetworkState.ERROR);
    } else if (percentage > percentageHigh) {
      changeState(NetworkState.OK);
    }
  }


  private void removeOldEntries(long minValue) {
    Iterator<Entry<Long, Long>> it = successfulResponses.entrySet().iterator();
    while (it.hasNext()) {
      Entry<Long, Long> entry = it.next();
      long l = entry.getKey();
      if (l < minValue) {
        it.remove();
        sumDurationSuccessfulResponses -= entry.getValue();
      } else {
        break;
      }
    }
  }


  public void start(int localReceiverPort, int localSenderPort, String remoteHostname, int remotePort, boolean master)
      throws SocketException, UnknownHostException {
    remoteHost = InetAddress.getByName(remoteHostname);
    if (!running.compareAndSet(false, true)) {
      throw new RuntimeException();
    }
    successfulResponses.clear();
    sumDurationSuccessfulResponses = 0;
    localPortReceive = localReceiverPort;
    localPortSend = localSenderPort;
    this.remotePort = remotePort;
    this.master = master;

    if (master) {
      lastSent.set(0);
      lastReceived.set(1);
    } else {
      lastSent.set(-1);
      lastReceived.set(0);
    }
    stateChangeHandler.stateChange(state);

    checker = new FactoryCheckerThread();
    checker.start();

    receiver = new Receiver();
    receiver.start();

    sender = new Sender();
    sender.start();
  }


  public void stop() {
    if (!running.compareAndSet(true, false)) {
      throw new RuntimeException();
    }
    logger.info("stopping threads...");
    receiver.interrupt();
    sender.interrupt();
    logger.info("threads stopped.");
  }


  public NetworkState getState() {
    return state;
  }


  public boolean isRunning() {
    return running.get();
  }

}
