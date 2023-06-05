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

package com.gip.xyna.xfmg.xfctrl.classloading;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;


public class DeploymentHandlerManagerTest extends TestCase {

  final List<String> list = new ArrayList<String>();

  public void testAddUndeploymentHandler() throws XynaException {

    UndeploymentHandler handler1 = new UndeploymentHandler() {

      public void onUndeployment() {
        list.add(":-)");
      }
      
    };

    UndeploymentHandler handler2 = new UndeploymentHandler() {

      public void onUndeployment() {
        list.add(":-(");
      }
      
    };

    ClassProvider c = ClassProvider.getClassProviderForXynaProcess(TestClass123.class);

    AutomaticUnDeploymentHandlerManager handlerManager = new AutomaticUnDeploymentHandlerManager();

    handlerManager.addUnDeploymentHandler(c, handler1);
    handlerManager.addUnDeploymentHandler(c, handler2);

    handlerManager.notifyUndeployment(TestClass123.class.getName(), VersionManagement.REVISION_WORKINGSET);

    if (list.size() < 2)
      fail("Not all handlers were called");

    if (!list.contains(":-)"))
      fail("First listener was not called correctly");

    if (!list.contains(":-("))
      fail("Second listener was not called correctly");

  }


  private class TestClass123 extends XynaProcess {

    private static final long serialVersionUID = 1L;


    public FractalProcessStep<?>[] getAllSteps() {
      return null;
    }

    public XynaObject getOutput() {
      return null;
    }

    public FractalProcessStep<?>[] getStartSteps() {
      return null;
    }

    public FractalProcessStep<?>[] getAllLocalSteps() {
      return null;
    }

    @Override
    protected void initializeMemberVars() {
    }

    @Override
    protected void onDeployment() throws XynaException {
    }

    @Override
    protected void onUndeployment() throws XynaException {
    }

    public void setInputVars(GeneralXynaObject o) throws XynaException {
    }

    @Override
    public String getOriginalName() {
      return null;
    }
    
  }

}
