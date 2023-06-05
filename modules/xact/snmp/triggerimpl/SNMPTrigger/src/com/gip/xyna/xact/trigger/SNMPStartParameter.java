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
package com.gip.xyna.xact.trigger;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;

public class SNMPStartParameter implements StartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(SNMPStartParameter.class);

  private static final int DEFAULT_LISTENERTHREADS = 10;
  private static final int DEFAULT_QUEUECAPACITY = 30;
  private static final int DEFAULT_TIMEOUTCAPFULL = 200;
  private static final int DEFAULT_RECEIVERTIMEOUT = 600;

  private static final String DESCR_PORT = "Port (required)";
  private static final String DESCR_ADDRESS = "Name of ip in NetworkConfigurationManagement or network interface name (required)";
  private static final String DESCR_LISTENERTHREADS = "Number of Threads which will be opened to listen for incoming requests (Default=" + DEFAULT_LISTENERTHREADS+ ")";
  private static final String DESCR_QUEUECAPACITY = "Max size of internally used queue to save requests before they are dispatched (Default=" + DEFAULT_QUEUECAPACITY + ")";
  private static final String DESCR_TIMEOUTCAPFULL = "Time in milliseconds which requests will wait for at most until they can be put in the internally used queue (Default=" + DEFAULT_TIMEOUTCAPFULL + ")";
  private static final String DESCR_RECEIVERTIMEOUT = "Timeout in seconds until the dispatcher will loop again trying to get requests from the internally used queue, if the queue remains empty. (Default=" + DEFAULT_RECEIVERTIMEOUT + ")";
  private static final String DESCR_VERSION = "Valid values for SNMP Version are \"2c\" and \"3\".";

  public static final String VERSION_2C = "2c";
  public static final String VERSION_3 = "3";
  
  private int port;
  private InetAddress address;
  private int listenerThreads = DEFAULT_LISTENERTHREADS;
  private int queueCapacity = DEFAULT_QUEUECAPACITY;
  //milliseconds
  private int timeoutCapFull = DEFAULT_TIMEOUTCAPFULL;
  //seconds
  private int receiverTimeout = DEFAULT_RECEIVERTIMEOUT;
  private String version = VERSION_2C;


  // the empty constructor may not be removed! additional ones are possible, though.
  public SNMPStartParameter() {
  }


  public SNMPStartParameter(String address, int port) throws XACT_InvalidTriggerStartParameterValueException {
    this.port = port;

    InternetAddressBean iab =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
            .getInternetAddress(address, null);
    if (iab == null) {
      if (logger.isInfoEnabled()) {
        logger.info("Getting IPv4 address for interface " + address);
      }

      try {
        this.address = NetworkInterfaceUtils.getFirstIPv4AddressByInterfaceName(address);
      } catch (XynaException e) {
        throw new XACT_InvalidTriggerStartParameterValueException(address);
      }
    } else {
      this.address = iab.getInetAddress();
    }
  }


  public SNMPStartParameter(String address, int port, int listenerThreads, int queueCapacity,
                            int timeoutCapFull, int receiverTimeout) throws XACT_InvalidTriggerStartParameterValueException {
    this(address, port);
    this.listenerThreads = listenerThreads;
    this.queueCapacity = queueCapacity;
    this.timeoutCapFull = timeoutCapFull;
    this.receiverTimeout = receiverTimeout;
  }


  public SNMPStartParameter(String address, int port, int listenerThreads, int queueCapacity,
                            int timeoutCapFull, int receiverTimeout, String version)
                  throws XACT_InvalidTriggerStartParameterValueException {
    this(address, port, listenerThreads, queueCapacity, timeoutCapFull, receiverTimeout);
    if (version.equals(VERSION_2C) || version.equals(VERSION_3)) {
      this.version = version;
    } else {
      throw new XACT_InvalidTriggerStartParameterValueException("version");
    }
  }


  public StartParameter build(String... args) throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {
    if (args.length == 2) {
      return new SNMPStartParameter(args[0], parseInt(args[1], "port"));
    } else if (args.length == 6) {
      return new SNMPStartParameter(args[0], parseInt(args[1], "port"), parseInt(args[2], "listenerThreads"),
                                    parseInt(args[3], "queueCapacity"), parseInt(args[4], "timeoutCapFull"),
                                    parseInt(args[5], "receiverTimeout"));
    } else if (args.length == 7) {
      return new SNMPStartParameter(args[0], parseInt(args[1], "port"), parseInt(args[2], "listenerThreads"),
                                    parseInt(args[3], "queueCapacity"), parseInt(args[4], "timeoutCapFull"),
                                    parseInt(args[5], "receiverTimeout"), args[6]);
    } else {
      throw new XACT_InvalidStartParameterCountException();
    }
  }


  public int parseInt(String value, String parameterName) throws XACT_InvalidTriggerStartParameterValueException {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new XACT_InvalidTriggerStartParameterValueException(parameterName, e);
    }
  }


  public InetAddress getIP() {
    return address;
  }


  public int getPort() {
    return port;
  }


  public int getNumListenerThreads() {
    return listenerThreads;
  }


  /**
   * milliseconds
   */
  public int getTimeoutCapacityFull() {
    return timeoutCapFull;
  }


  public int getQueueCapacity() {
    return queueCapacity;
  }


  /**
   * seconds
   */
  public int getReceiverTimeout() {
    return receiverTimeout;
  }


  public String getVersion() {
    return version;
  }


  public String[][] getParameterDescriptions() {
    return new String[][] {
                    {DESCR_ADDRESS, DESCR_PORT},
                    {DESCR_ADDRESS, DESCR_PORT, DESCR_LISTENERTHREADS, DESCR_QUEUECAPACITY,
                                    DESCR_TIMEOUTCAPFULL, DESCR_RECEIVERTIMEOUT},
                    {DESCR_ADDRESS, DESCR_PORT, DESCR_LISTENERTHREADS, DESCR_QUEUECAPACITY,
                                    DESCR_TIMEOUTCAPFULL, DESCR_RECEIVERTIMEOUT, DESCR_VERSION}};
  }

}
