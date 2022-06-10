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
package com.gip.xyna.utils.install.as.oracle;



import org.apache.tools.ant.BuildException;

import com.oracle.bpel.client.BPELProcessId;
import com.oracle.bpel.client.IBPELDomainHandle;
import com.oracle.bpel.client.IBPELProcessHandle;
import com.oracle.bpel.client.Locator;
import com.oracle.bpel.client.ServerException;



/**
 */
public class UndeployBPEL extends XynaOracleTask {

  private String name;
  private String revision;


  @Override
  public void executeSub() throws ServerException {
    IBPELDomainHandle domainHandle = null;
    try {
      Locator locator = getLocator();
      if (locator != null) {
        domainHandle = getLocator().lookupDomain();
      }
    }
    catch (ServerException e) {
      if (e.getMessage().indexOf("The BPEL domain \"" + getDomain() + "\" cannot be found") > 0) {
        log("WARNING: BPEL Domain " + getDomain() + " not available.");
      }
      else {
        throw e;
      }
    }
    if (domainHandle != null) {
      IBPELProcessHandle[] processes = domainHandle.listProcesses();
      boolean found = false;
      for (IBPELProcessHandle proc : processes) {
        if (proc.getProcessId().getProcessId().equals(getName())) {
          found = true;
          if (getRevision().equals("*")) { // undeploy all revisions
            domainHandle.undeployProcess(proc.getProcessId());
            log("All revision of workflow " + getName() + " undeployed.");
          }
          else if (getRevision().equals("")) { // undeploy default revision
            domainHandle.undeployProcess(new BPELProcessId(getDomain(), getName()));
            log("Workflow " + getName() + " (default revision) undeployed.");
          }
          else { // undeploy specific revision 
            domainHandle.undeployProcess(new BPELProcessId(getDomain(), getName(), getRevision()));
            log("Workflow " + getName() + " (revision " + getRevision() + ") undeployed.");
          }
          break;
        }
      }
      if (!found) {
        log("WARNING: workflow " + getName() + " not deployed in domain " + getDomain());
      }
    }

  }


  public void setName(String name) {
    this.name = name;
  }


  /**
   * @return the workflow
   */
  private String getName() {
    if ((name == null) || (name.equals(""))) {
      throw new BuildException("Parameter 'workflow' not set");
    }
    return name;
  }


  public void setRevision(String revision) {
    this.revision = revision;
  }


  /**
   * @return the revision
   */
  private String getRevision() {
    if ((revision == null) || (revision.equals(""))) {
      throw new BuildException("Parameter 'revision' not set");
    }
    return revision;
  }

}