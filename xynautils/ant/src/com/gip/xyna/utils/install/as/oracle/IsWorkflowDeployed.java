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
import org.apache.tools.ant.taskdefs.condition.Condition;

import com.oracle.bpel.client.BPELProcessId;
import com.oracle.bpel.client.IBPELProcessHandle;
import com.oracle.bpel.client.ServerException;



/**
 */
public class IsWorkflowDeployed extends XynaOracleTask implements Condition {

  private String dir;
  private String revision;


  /*
   * (non-Javadoc)
   * @see org.apache.tools.ant.taskdefs.condition.Condition#eval()
   */
  public boolean eval() throws BuildException {
    String name = getWorkflowName();
    try {
      if (isDeployed(name)) {
        log("Workflow " + name + " is already deployed in revision " + getRevision());
        return true;
      }
    }
    catch (NoClassDefFoundError ncdfe) {
      checkException(ncdfe);
    }
    catch (ServerException e) {
      checkException(e);
    }
    return false;
  }


  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.install.XynaOracleTask#executeSub()
   */
  @Override
  protected void executeSub() throws ServerException {
    /*
     * String name = getWorkflowName(); if (isDeployed(name)) { log("Workflow " + name +
     * " is already deployed in revision " + getRevision()); return; }
     */
  }


  private String getWorkflowName() {
    File workflow_dir = new File(getDir());
    if (!workflow_dir.exists()) {
      throw new BuildException("Directory " + workflow_dir.getAbsolutePath() + " not found");
    }
    return workflow_dir.getName();
  }


  private boolean isDeployed(String workflowName) throws ServerException {
    String[] deployedRevisions = getDeployedRevisions(workflowName);
    for (int i = 0; i < deployedRevisions.length; i++) {
      if (deployedRevisions[i].equals(getRevision())) {
        return true;
      }
    }
    return false;
  }


  private String[] getDeployedRevisions(String workflowName) throws ServerException {
    ArrayList<String> revisions = new ArrayList<String>();
    IBPELProcessHandle[] processes = getLocator().listProcesses();
    for (int i = 0; i < processes.length; i++) {
      BPELProcessId id = processes[i].getProcessId();
      if (id.getProcessId().equals(workflowName)) {
        revisions.add(id.getRevisionTag());
      }
    }
    return revisions.toArray(new String[] {});
  }


  /**
   * @param dir the dir to set
   */
  public void setDir(String dir) {
    this.dir = dir;
  }


  /**
   * @return the dir
   */
  private String getDir() {
    if ((dir == null) || (dir.equals(""))) {
      throw new BuildException("Parameter 'dir' not set.");
    }
    return dir;
  }


  /**
   * @param revision the revision to set
   */
  public void setRevision(String revision) {
    this.revision = revision;
  }


  /**
   * @return the revision
   */
  private String getRevision() {
    if ((revision == null) || (revision.equals(""))) {
      throw new BuildException("Parameter 'revision' not set.");
    }
    return revision;
  }

}