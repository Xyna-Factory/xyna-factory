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
package com.gip.xyna.utils.install.as.oracle;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.oracle.bpel.client.BPELDomainStatus;
import com.oracle.bpel.client.Server;
import com.oracle.bpel.client.ServerException;



/**
 */
public class DeleteDomain extends XynaOracleTask {

  private boolean ask = true;


  @Override
  public void executeSub() throws ServerException {
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
      if (getDomain().equals("default")) {
        log("WARNING: Its not allowed to delete default domain. Skip deletion.");
      }
      else {
        boolean execute = false;
        if (ask) {
          log("Delete domain " + getDomain() + "? (y, n) (default is no)");
          BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
          String input = "";
          try {
            input = reader.readLine();
          }
          catch (IOException e) {
            log(e.getMessage());
          }
          if (input.trim().equals("y")) {
            execute = true;
          }
        }
        if (execute) {
          getServer().deleteDomain(getDomain(), true);
          log("Domain '" + getDomain() + "' deleted.");
        } else {
          log("Skip deletion of domain '" + getDomain() + "' because of user request.");
        }
      }
    }
    else {
      log("Skip deletion of domain '" + getDomain() + "' because domain not exists.");
    }
  }


  /**
   * @param ask the ask to set
   */
  public void setAsk(boolean ask) {
    this.ask = ask;
  }

}
