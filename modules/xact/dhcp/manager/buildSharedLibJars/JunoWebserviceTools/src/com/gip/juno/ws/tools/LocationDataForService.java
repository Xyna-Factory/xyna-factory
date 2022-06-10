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


package com.gip.juno.ws.tools;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.LocationSchema;

/**
 * class used to store connection data about service databases in DPP locations 
 */
public class LocationDataForService extends LocationData {

  protected static LocationData _instance = null;
  
  public LocationDataForService(Logger logger) throws RemoteException {
    super(logger);
  }
  protected static LocationData getInstance(Logger logger) throws RemoteException {
    if (_instance == null) {
      _instance = new LocationDataForService(logger);
    }
    return _instance;
  }
  protected LocationSchema getLocationSchema() { return LocationSchema.service; }
}
