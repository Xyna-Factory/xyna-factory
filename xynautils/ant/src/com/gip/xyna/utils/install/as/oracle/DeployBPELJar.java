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



import java.io.File;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;

import com.oracle.bpel.client.BPELProcessId;
import com.oracle.bpel.client.IBPELDomainHandle;
import com.oracle.bpel.client.IBPELProcessHandle;
import com.oracle.bpel.client.ServerException;



/**
 */
public class DeployBPELJar extends XynaOracleTask {

  private String jarfile;
  private boolean checkRevision = true;


  @Override
  public void executeSub() throws ServerException {
    File workflowFile = new File(getJarfile());
    String name = getWorkflowNameFromFile(workflowFile);
    String revision = getRevisionFromFile(workflowFile);
    if (isCheckRevision() && isDeployed(revision)) {
      log("Workflow " + name + " with revision " + revision + " is already deployed in domain " + getDomain());
      return;
    }
    IBPELDomainHandle domainHandle = getLocator().lookupDomain();
    domainHandle.deploySuitcase(workflowFile);
  }


  private String getRevisionFromFile(File workflow) {
    int pos_revision = workflow.getName().lastIndexOf('_');
    return workflow.getName().substring(pos_revision + 1, workflow.getName().length() - ".jar".length());
  }


  private String getWorkflowNameFromFile(File workflow) {
    int pos_revision = workflow.getName().lastIndexOf('_');
    int pos_name = workflow.getName().indexOf('_');
    return workflow.getName().substring(pos_name + 1, pos_revision);
  }


  private boolean isDeployed(String revision) throws ServerException {
    String[] deployedRevisions = getDeployedRevisions();
    for (int i = 0; i < deployedRevisions.length; i++) {
      if (deployedRevisions[i].equals(revision)) {
        return true;
      }
    }
    return false;
  }


  private String[] getDeployedRevisions() throws ServerException {
    ArrayList<String> revisions = new ArrayList<String>();
    IBPELProcessHandle[] processes = getLocator().listProcesses();
    for (int i = 0; i < processes.length; i++) {
      BPELProcessId id = processes[i].getProcessId();
      if (id.getProcessId().equals(getJarfile())) {
        revisions.add(id.getRevisionTag());
      }
    }
    return revisions.toArray(new String[] {});
  }


  public void setJarfile(String jarfile) {
    this.jarfile = jarfile;
  }


  /**
   * @return the jarfile
   */
  private String getJarfile() {
    if ((jarfile == null) || jarfile.equals("")) {
      throw new BuildException("Parameter 'jarfile' not set.");
    }
    return jarfile;
  }


  public void setCheckRevision(boolean checkRevision) {
    this.checkRevision = checkRevision;
  }


  /**
   * @return the checkRevision
   */
  private boolean isCheckRevision() {
    return checkRevision;
  }


}