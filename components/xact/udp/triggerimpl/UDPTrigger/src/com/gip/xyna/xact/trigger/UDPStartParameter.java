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
package com.gip.xyna.xact.trigger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;

import org.apache.log4j.Logger;

public class UDPStartParameter implements StartParameter {

    private static Logger logger = CentralFactoryLogging
            .getLogger(UDPStartParameter.class);

    private InetAddress localAddress;

    public InetAddress getLocalAddress() {
        return localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    private int localPort;

    // the empty constructor may not be removed or throw exceptions! additional
    // ones are possible, though.
    public UDPStartParameter() {
    }

    public UDPStartParameter(InetAddress ip, int port) {
        this.localAddress = ip;
        this.localPort = port;
    }

    /**
     * Is called by XynaProcessing with the parameters provided by the deployer
     * 
     * @return StartParameter Instance which is used to instantiate
     *         corresponding Trigger
     */
    public StartParameter build(String... args)
            throws XACT_InvalidStartParameterCountException,
            XACT_InvalidTriggerStartParameterValueException {

        String regexIP = "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+";
        Pattern patternIP = Pattern.compile(regexIP);

        if (args.length == 2) {

            InternetAddressBean iab = XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl()
                    .getNetworkConfigurationManagement()
                    .getInternetAddress(args[0], null);
            if (iab != null) {
                localAddress = iab.getInetAddress();
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("address " + args[0]
                            + " unknown in network configuration management.");
                }

                Matcher matcherIP = patternIP.matcher(args[0]);
                if (matcherIP.matches()) {
                    try {
                        localAddress = InetAddress.getByName(args[0]);
                    } catch (UnknownHostException e) {
                        throw new IllegalArgumentException(
                                "Illegal argument : Unknown Host", e);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Illegal argument : First argument must be the label of a defined IP or an IP address! ");
                }

            }

            localPort = Integer.parseInt(args[1]);

        }

        return new UDPStartParameter(localAddress, localPort);
    }

    /**
     * 
     * @return array of valid lists of descriptions of parameters. example: if
     *         parameters (A,B) and (A,C,D) are valid, then this method should
     *         return new String[]{{"descriptionA", "descriptionB"},
     *         {"descriptionA", "descriptionC", "descriptionD"}}
     */
    public String[][] getParameterDescriptions() {
        String[][] ret={{"Name of ip in NetworkConfigurationManagement or network interface name at which the trigger is listening (required)","Port at which the trigger is listening(required)"}};
        return ret;
    }

}
