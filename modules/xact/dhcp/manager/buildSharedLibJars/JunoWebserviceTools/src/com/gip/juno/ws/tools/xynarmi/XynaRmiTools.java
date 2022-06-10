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
package com.gip.juno.ws.tools.xynarmi;

import java.rmi.RemoteException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.GenericRMIAdapter.URLChooser;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xmcp.XynaRMIChannel;


public class XynaRmiTools {
  
  private static Logger logger = Logger.getLogger(XynaRmiTools.class);
  
  private static final String PRIMARY_RMI_URL_PROPERTY_KEY = "primary.xyna.rmichannel.url";
  private static final String SECONDARY_RMI_URL_PROPERTY_KEY = "secondary.xyna.rmichannel.url";
    
  public static XynaRMIChannel getXynaFactoryRmiChannel() throws RemoteException, RMIConnectionFailureException {
    URLChooser chooser = GenericRMIAdapter.getMultipleURLChooser(getFailOverUrls(), false, 2);
    GenericRMIAdapter<XynaRMIChannel> xynaRmiChannel = new GenericRMIAdapter<XynaRMIChannel>(chooser);
    return xynaRmiChannel.getRmiInterface();
  }
  
  private static String[] getFailOverUrls() throws RemoteException {
    Properties webServiceProperties = PropertiesHandler.getWsProperties();
    return new String[] {PropertiesHandler.getProperty(webServiceProperties, PRIMARY_RMI_URL_PROPERTY_KEY, logger),
                         PropertiesHandler.getProperty(webServiceProperties, SECONDARY_RMI_URL_PROPERTY_KEY, logger)};
    
  }
}
