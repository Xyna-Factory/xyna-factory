/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.yang.misc;

import xmcp.yang.netconf.enums.ConfigurationDatastoreCandidate;
import xmcp.yang.netconf.enums.ConfigurationDatastoreRunning;
import xmcp.yang.netconf.enums.ConfigurationDatastoreStartup;
import xmcp.yang.netconf.enums.NetConfConfigurationDatastoreType;


public class DataStoreHelper {

  public enum NetConfOperation {
    EDIT_CONFIG, DELETE_CONFIG, OTHER
  };
  
  public String getDataStoreTagName(NetConfConfigurationDatastoreType datastore, NetConfOperation op) {
    if (datastore == null) {
      throw new IllegalArgumentException("Cannot determine datastore name, received null");
    }
    if (datastore instanceof ConfigurationDatastoreCandidate) {
      if (op == NetConfOperation.DELETE_CONFIG) {
        throw new IllegalArgumentException("Datastore name " + Constants.NetConf.XmlTag.CANDIDATE + " is not supported for operation " +
                                           Constants.NetConf.OperationNameTag.DELETE_CONFIG);
      }
      return Constants.NetConf.XmlTag.CANDIDATE;
    }
    if (datastore instanceof ConfigurationDatastoreStartup) {
      if (op == NetConfOperation.EDIT_CONFIG) {
        throw new IllegalArgumentException("Datastore name " + Constants.NetConf.XmlTag.STARTUP + " is not supported for operation " +
                                           Constants.NetConf.OperationNameTag.EDIT_CONFIG);
      }
      return Constants.NetConf.XmlTag.STARTUP;
    }
    if (datastore instanceof ConfigurationDatastoreRunning) {
      if (op == NetConfOperation.DELETE_CONFIG) {
        throw new IllegalArgumentException("Datastore name " + Constants.NetConf.XmlTag.RUNNING + " is not supported for operation " +
                                           Constants.NetConf.OperationNameTag.DELETE_CONFIG);
      }
      return Constants.NetConf.XmlTag.RUNNING;
    }
    throw new IllegalArgumentException("Cannot determine datastore name, unknown datatype: " + datastore.getClass().getName());
  }
  
}
