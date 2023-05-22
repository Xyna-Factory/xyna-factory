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
package com.gip.xyna.utils.install.as.oracle;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.oracle.bpel.client.BPELDomainStatus;
import com.oracle.bpel.client.Server;
import com.oracle.bpel.client.ServerException;



/**
 */
public class CreateDomain extends XynaOracleTask {

  private boolean ask = true;


  @Override
  public void executeSub() throws ServerException {
    Map<String, String> domainProperties = new HashMap<String, String>();
    domainProperties.put("datasourceJndi", "jdbc/BPELServerDataSourceWorkflow");
    domainProperties.put("txDatasourceJndi", "jdbc/BPELServerDataSource");
    Server server = getServer();
    BPELDomainStatus[] domainStatus = server.getAllDomainStatus();
    boolean domainFound = false;
    for (int i = 0; i < domainStatus.length; i++) {
      if (domainStatus[i].getDomainId().equals(getDomain())) {
        domainFound = true;
        break;
      }
    }
    if (domainFound) {
      log("Skip creation of domain '" + getDomain() + "' because domain already exists.");
    }
    else {
      boolean execute = true;
      if (ask) {
        log("Create domain " + getDomain() + "? (y, n) (default is yes)");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        try {
          input = reader.readLine();
        }
        catch (IOException e) {
          log(e.getMessage());
        }
        if (input.trim().equals("n")) {
          execute = false;
        }
      }
      if (execute) {
        server.createDomain(getDomain(), null, domainProperties);
        log("Domain '" + getDomain() + "' created.");
      }
      else {
        log("Skip creation of domain '" + getDomain() + "' because of user request.");
      }
    }
  }


  /**
   * @param ask the ask to set
   */
  public void setAsk(boolean ask) {
    this.ask = ask;
  }
}