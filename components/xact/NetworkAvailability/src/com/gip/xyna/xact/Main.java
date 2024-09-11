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
package com.gip.xyna.xact;



import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.gip.xyna.demon.Demon;
import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.demon.DemonSnmpConfigurator;
import com.gip.xyna.demon.DemonWorker;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.ChainedOidSingleHandler;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.xact.NetworkAvailability.FactoryChecker;
import com.gip.xyna.xact.NetworkAvailability.NetworkState;
import com.gip.xyna.xact.NetworkAvailability.StateChangeHandler;



public class Main implements DemonWorker {
  
  public static final OID OID_STATE = new OID(".1.3.6.1.4.1.28747.1.11.4.1.2.1");
  
  private static final String PROPERTY_FILENAME = "networkAvailability.properties";
  private static final String LOGFILE_DEFAULT = PROPERTY_FILENAME;
  private static final String DEMON_PREFIX = "demon";
  static Logger logger = LogManager.getLogger(NetworkAvailability.class.getName());


  private static final String PROP_LOCALPORT_RECEIVE = "port.local.receive";
  private static final String PROP_LOCALPORT_SEND = "port.local.send";
  private static final String PROP_HOSTNAME_REMOTE = "hostname.remote";
  private static final String PROP_REMOTE_PORT = "port.remote";
  private static final String PROP_ISMASTER = "master";
  private static final String PROP_PERCENTAGE_LOW = "statechange.percentage.low";
  private static final String PROP_PERCENTAGE_HIGH = "statechange.percentage.high";
  private static final String PROP_TIMEOUT_MS = "timeout.ms";
  private static final String PROP_INTERVALLENGTH = "intervallength.ms";
  private static final String PROP_TIMEOUT_SHORT_MS = "timeout.short.ms";
  private static final String PROP_RATE = "rate.hz";
  private static final String PROP_COMMAND_STATECHANGE = "statechange.cli.command";
  private static final String PROP_FACTORYCHECK_RATE = "factory.check.rate.hz";
  private static final String PROP_COMMAND_FACTORY_CHECK = "factory.check.cli.command";
  private static final String PROP_FACTORYCHECK_EXPECTED_EXITVAL = "factory.check.cli.expectedexitcode";
  private static final String PROP_FACTORYCHECK_TIMEOUT = "factory.check.cli.timeout";

  private static String propertiesPath;


  public static void main(String[] args) throws IOException {
    String pathToProperties = (args.length > 0 ? args[0] : "./config") + "/";

    PropertyConfigurator.configure(pathToProperties + LOGFILE_DEFAULT);

    propertiesPath = pathToProperties + PROPERTY_FILENAME;
    DemonProperties.readProperties(propertiesPath);
    
    logger = LogManager.getRootLogger();
    logger.debug("Initializing Demon");
    Demon demon = Demon.createDemon(DEMON_PREFIX);
    demon.startDemon();
    logger.info("Demon started");


    Main main = new Main();
    try {
      logger.debug("Starting demonWorker");
      demon.setDemonWorker(main);
      demon.startDemonWorker();

    } catch (Exception e) {
      logger.error("Exception while initializing DhcpAdapterDemon", e);
      logger.error("Demon will be stopped now");
      main.terminate();
    }

  }


  public void configureDemonSnmp(DemonSnmpConfigurator snmpConfig) {
    snmpConfig.addOidSingleHandler(new ChainedOidSingleHandler() {
      public boolean matches(SnmpCommand cmd, OID oid) {
        if (cmd == SnmpCommand.GET && oid.startsWith(OID_STATE)) {
          return true;
        }
        return false;
      }
      
      
      public VarBind getNext(OID arg0, VarBind varBind, int i) {        
        return ChainedOidSingleHandler.WALK_END.getNext( ChainedOidSingleHandler.WALK_END.startOID(), varBind, i);
      }
      
      
      public VarBind get(OID oid, int arg1) {
        return new StringVarBind(oid.getOid(), na.getState().toString());
      }
    });
  }


  public String getName() {
    return "NetworkAvailability";
  }


  public void logStatus(Logger logger2) {
    logger.info(na.getState());
  }


  private NetworkAvailability na;


  public void run() {
    final ProcessUtil pu = new ProcessUtil();
    String log4jprop = System.getProperty("log4j.configuration");
    if (log4jprop != null) {
      if (log4jprop != null && log4jprop.startsWith("file:")) {
        log4jprop = log4jprop.substring("file:".length());
      } else {
        throw new RuntimeException();
      }
      LogManager.resetConfiguration();
      PropertyConfigurator.configure(log4jprop);
    }
    DemonProperties.readProperties(propertiesPath);

    int localportReceive = DemonProperties.getIntProperty(PROP_LOCALPORT_RECEIVE, 1414);
    int localportSend = DemonProperties.getIntProperty(PROP_LOCALPORT_SEND, 0);
    String hostname = DemonProperties.getProperty(PROP_HOSTNAME_REMOTE);
    if (hostname == null || hostname.length() == 0) {
      throw new RuntimeException("property " + PROP_HOSTNAME_REMOTE + " not set properly");
    }
    int remotePort = DemonProperties.getIntProperty(PROP_REMOTE_PORT, 1414);
    boolean isMaster = DemonProperties.getBooleanProperty(PROP_ISMASTER, true);

    double percentageLow = getDoubleProperty(PROP_PERCENTAGE_LOW, 0.6);
    double percentageHigh = getDoubleProperty(PROP_PERCENTAGE_HIGH, 0.9);
    int intervalLength = DemonProperties.getIntProperty(PROP_INTERVALLENGTH, 30000);
    int timeoutMs = DemonProperties.getIntProperty(PROP_TIMEOUT_MS, 5000);
    int shortTimeoutMs = DemonProperties.getIntProperty(PROP_TIMEOUT_SHORT_MS, 200);
    double rate = getDoubleProperty(PROP_RATE, 10);
    final String commandForStateChange = DemonProperties.getProperty(PROP_COMMAND_STATECHANGE);
    double factoryCheckRate = getDoubleProperty(PROP_FACTORYCHECK_RATE, 0.3);
    final String commandForFactoryCheck = DemonProperties.getProperty(PROP_COMMAND_FACTORY_CHECK);
    final int expectedFactoryCheckExitValue = DemonProperties.getIntProperty(PROP_FACTORYCHECK_EXPECTED_EXITVAL, 0);
    final int timeoutFactoryCheck = DemonProperties.getIntProperty(PROP_FACTORYCHECK_TIMEOUT, 3000);
    
    final ExecutorService executor = Executors.newFixedThreadPool(1);
    na = new NetworkAvailability(new FactoryChecker() {

      public boolean check() throws InterruptedException {
        if (commandForFactoryCheck == null || commandForFactoryCheck.length() == 0) {
          logger.error("configuration does not provide a way to check factory state. please restart daemon after fixing the problem.");
          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
          }
          return false;
        }
        Process p = null;
        try {
          if (logger.isTraceEnabled()) {
            logger.trace("calling " + commandForFactoryCheck);
          }
          p = Runtime.getRuntime().exec(commandForFactoryCheck);
          final Process pc = p;
          Future<Integer> f = executor.submit(new Callable<Integer>() {

            public Integer call() throws Exception {
              try {
              return pc.waitFor();
              } finally {
                if (logger.isTraceEnabled()) {
                  logger.trace("finished waiting for process");
                }
              }
            }
            
          });
          int v;
          try {
            v = f.get(timeoutFactoryCheck, TimeUnit.MILLISECONDS);
          } catch (ExecutionException e) {
            pu.killProcess(p);     
            throw (IOException) new IOException("exception waiting for process").initCause(e);
          } catch (TimeoutException e) {
            pu.killProcess(p);            
            return false;
          }
          return v == expectedFactoryCheckExitValue;
        } catch (IOException e) {
          logger.warn("command to check for factory state could not be executed successfully.", e);
          return false;
        } finally {
          if (p != null) {
            safelyCloseStream(p.getErrorStream());
            safelyCloseStream(p.getInputStream());
            safelyCloseStream(p.getOutputStream());
          }
        }
      }


    }, factoryCheckRate, new StateChangeHandler() {

      public boolean stateChange(NetworkState newState) {
        if (commandForStateChange != null && commandForStateChange.length() != 0) {
          Process p = null;
          try {
            p = Runtime.getRuntime().exec(commandForStateChange + " " + newState);
            int v = p.waitFor();
            if (v != 0) {
              logger.warn("stateChange could not be executed successfully (result=" + v + ")");
              return false;
            }
          } catch (IOException e) {
            logger.warn("command to be execute after state change could not be executed successfully.", e);
            return false;
          } catch (InterruptedException e) {
            return false;
          } finally {
            if (p != null) {
              safelyCloseStream(p.getErrorStream());
              safelyCloseStream(p.getInputStream());
              safelyCloseStream(p.getOutputStream());
            }
          }
          return true;
        } else {
          logger.error("configuration does not provide a way to signal state change. please restart daemon after fixing the problem.");
          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
          }
          return false;
        }
      }
    }, percentageLow, percentageHigh, intervalLength, timeoutMs, shortTimeoutMs, rate);
    try {
      na.start(localportReceive, localportSend, hostname, remotePort, isMaster);
      while (na.isRunning()) {
        //ansosnten wird sofort terminiert.
        Thread.sleep(10000);
      }
    } catch (SocketException e) {
      logger.error(null, e);
    } catch (UnknownHostException e) {
      logger.error(null, e);
    } catch (InterruptedException e) {
    } finally {
      executor.shutdown();
    }
  }


  private double getDoubleProperty(String propName, double defaultVal) {
    String s = DemonProperties.getProperty(propName, String.valueOf(defaultVal));
    double d;
    try {
      d = Double.valueOf(s);
    } catch (NumberFormatException e) {
      logger.warn("invalid value for property " + propName + ". must be double. using default value (" + defaultVal
          + ") instead.", e);
      d = defaultVal;
    }
    return d;
  }


  public void terminate() {
    if (na != null) {
      na.stop();
    }
  }


  private static void safelyCloseStream(Closeable toBeClosed) {
    try {
      toBeClosed.close();
    } catch (Throwable t) {
      logger.info("Failed to close stream", t);
    }
  }


}
