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
package com.gip.xyna.xprc;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.gip.xyna.xprc.xfractwfe.base.ProcessStepTest;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBufferTest;
import com.gip.xyna.xprc.xfractwfe.generation.DOM1Test;
import com.gip.xyna.xprc.xfractwfe.generation.DOMEmptyPathTest;
import com.gip.xyna.xprc.xfractwfe.generation.WF1Test;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtilsTest;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcherTest;
import com.gip.xyna.xprc.xprcods.manualinteractiondb.ManualInteractionManagementTest;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetailsTest;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceTest;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstancesDatabaseTest;
import com.gip.xyna.xprc.xsched.CancelTest;
import com.gip.xyna.xprc.xsched.CapacityManagementTest;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderTest;


public class AllTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for Xyna Processing");
    //$JUnit-BEGIN$
    //Fractal Workflow Engine
    suite.addTestSuite(ProcessStepTest.class);
    suite.addTestSuite(DOMEmptyPathTest.class);
    suite.addTestSuite(DOM1Test.class);
    suite.addTestSuite(WF1Test.class);
    suite.addTestSuite(XMLUtilsTest.class);
    suite.addTestSuite(CodeBufferTest.class);
    //Master Workflow
    suite.addTestSuite(MonitoringDispatcherTest.class);
    //Processing ODS
    suite.addTestSuite(OrderInstanceDetailsTest.class);
    suite.addTestSuite(OrderInstancesDatabaseTest.class);
    suite.addTestSuite(OrderInstanceTest.class);
    suite.addTestSuite(ManualInteractionManagementTest.class);
    //Scheduler
    //suite.addTestSuite(OrderSeriesManagementTest.class);
    suite.addTestSuite(CapacityManagementTest.class);
    suite.addTestSuite(CancelTest.class);
    //misc
    //suite.addTestSuite(SeriesInformationTest.class);
    suite.addTestSuite(CronLikeOrderTest.class);
    //$JUnit-END$
    return suite;
  }

}
