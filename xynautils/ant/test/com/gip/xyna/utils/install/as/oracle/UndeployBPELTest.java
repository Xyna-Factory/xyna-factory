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



import org.apache.tools.ant.BuildException;

import com.gip.xyna.utils.install.Environment;
import com.gip.xyna.utils.install.as.oracle.UndeployBPEL;

import junit.framework.TestCase;



/**
 */
public class UndeployBPELTest extends TestCase {

  private UndeployBPEL task = null;


  public void setUp() {
    task = new UndeployBPEL();
    task.setHost(Environment.HOST);
    task.setPassword(Environment.PASSWORD);
    task.setBpelContainer(Environment.BPEL_CONTAINER);
    task.setOpmnPort(Environment.OPMN_PORT);
    task.setUserid(Environment.USERID);
  }


  public void testUndeploy() {

  }


  public void testUndeploy_NoWorkflow() {
    task.setDomain(Environment.DOMAIN);
    task.setRevision("1.0");
    task.setName("DummyWorkflow");
    task.execute();
  }


  public void testUndeploy_NoDomain() {
    task.setDomain("NotExistingDomain");
    task.setRevision("1.0");
    task.setName("DummyWorkflow");
    task.execute();
  }


  public void testUnsetParameter_Workflow() {
    try {
      task.setName(null);
      fail("Exception expected");
    }
    catch (BuildException e) {
      assertEquals("Parameter Workflow not set", e.getMessage());
    }
  }


  public void testUnsetParameter_Revision() {
    try {
      task.setRevision(null);
      fail("Exception expected");
    }
    catch (BuildException e) {
      assertEquals("Parameter Revision not set", e.getMessage());
    }
  }

}
