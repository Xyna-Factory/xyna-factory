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
package com.gip.xyna.xprc.xfqctrl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryComponent;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfint.xnumdav.StorableAggregatableDataEntry;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_IllegalStateForTaskArchiving;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCreationParameters;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidFrequencyControlledTaskId;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FREQUENCY_CONTROLLED_TASK_TYPE;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;


public class XynaFrequencyControlTest extends TestCase {
  
  public static final Logger logger = CentralFactoryLogging.getLogger(XynaFrequencyControlTest.class);

  public XynaFrequencyControl myXfqctrl;
  
  protected void setUp() throws Exception {    
    super.setUp();

    IDGenerator id = EasyMock.createMock(IDGenerator.class); 
    IDGenerator.setInstance(id);
    EasyMock.expect(id.getUniqueId()).andReturn(1l).anyTimes();
    
    
    ODS ods = EasyMock.createMock(ODS.class);
    ods.registerStorable(EasyMock.isA(Class.class));
    EasyMock.expectLastCall().anyTimes();
    
    XynaProcessingODS xprocods = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xprocods.getODS()).andReturn(ods).anyTimes();
    
    XynaProcessing xproc = EasyMock.createMock(XynaProcessing.class);
    EasyMock.expect(xproc.getXynaProcessingODS()).andReturn(xprocods).anyTimes();
    
    XynaFactory xf = EasyMock.createMock(XynaFactory.class);
    xf.addComponentToBeInitializedLater((XynaFactoryComponent) EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(xf.getIDGenerator()).andReturn(id).anyTimes();
    EasyMock.expect(xf.isStartingUp()).andReturn(true).anyTimes();
    EasyMock.expect(xf.getProcessing()).andReturn(xproc).anyTimes();
    XynaFactory.setInstance(xf);
    
    EasyMock.replay(id, ods, xprocods, xproc, xf);
    
    myXfqctrl = new TestFrequencyControl();
    myXfqctrl.init();
  }


  protected void tearDown() throws Exception {
    super.tearDown();
  }
 
  
  public List<Long> triggers = new ArrayList<Long>();
  
  
  public void testLoadEvent() throws XynaException, InterruptedException {
    // 20 Events, maxLoad 3
    long eventsToLaunch = 20;
    long maxLoad = 3;
    long taskId = myXfqctrl.startFrequencyControlledTask(new TestLoadTaskCP("Test",eventsToLaunch,maxLoad));
    
    FrequencyControlledTaskInformation info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    while (info.getTaskStatus() != "Finished") {
      Thread.sleep(600);
      info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
      // there shoudln't be more running events than maxLoad
      assertTrue(maxLoad >= ((info.getEventCount() - info.getFinishedEvents()) - info.getFailedEvents()));
    }
    logger.debug(info);
    assertEquals(eventsToLaunch, info.getEventCount());
    assertEquals(eventsToLaunch, info.getEventsToLaunch());
    assertEquals(eventsToLaunch, triggers.size());
    triggers.clear();
    
    
    // 20 events maxLoad 20
    eventsToLaunch = 100;
    maxLoad = 20;
    taskId = myXfqctrl.startFrequencyControlledTask(new TestLoadTaskCP("Test",eventsToLaunch,maxLoad));
    
    info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    while (info.getTaskStatus() != "Finished") {
      Thread.sleep(120);
      info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
      // there shoudln't be more running events than maxLoad
      assertTrue(maxLoad >= ((info.getEventCount() - info.getFinishedEvents()) - info.getFailedEvents()));
    }
    logger.debug(info);
    assertEquals(eventsToLaunch, info.getEventCount());
    assertEquals(eventsToLaunch, info.getEventsToLaunch());
    assertEquals(eventsToLaunch, triggers.size());
    triggers.clear();
    
    
    // 20 events maxLoad 20
    eventsToLaunch = 10;
    maxLoad = 10;
    taskId = myXfqctrl.startFrequencyControlledTask(new TestLoadTaskCP("Test",eventsToLaunch,maxLoad));
    
    info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    while (info.getTaskStatus() != "Finished") {
      Thread.sleep(120);
      info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
      // there shoudln't be more running events than maxLoad
      assertTrue(maxLoad >= ((info.getEventCount() - info.getFinishedEvents()) - info.getFailedEvents()));
    }
    logger.debug(info);
    assertEquals(eventsToLaunch, info.getEventCount());
    assertEquals(eventsToLaunch, info.getEventsToLaunch());
    assertEquals(eventsToLaunch, triggers.size());
    triggers.clear();
  }
  

  
  public void testRateEvent() throws XynaException, InterruptedException {
    final int eventsOffset = 2;
    final double rateOffset = 0.02;
    
    //many events at high rate
    long eventsToLaunch = 2000;
    double rate = 20;
    long taskId = myXfqctrl.startFrequencyControlledTask(new TestRateTaskCP("Test",eventsToLaunch,rate));

    FrequencyControlledTaskInformation info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    while (!info.getTaskStatus().equals("Finished")) {
      Thread.sleep(1000);
      info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    }
    
    long startTime = triggers.get(0);
    long stopTime = triggers.get((int)eventsToLaunch-1);
    double estimatedEvents = (stopTime - startTime) * (rate/1000.0);
    double achievedRate = eventsToLaunch / ((stopTime - startTime) / 1000.0d);
    
    
    
    // not more then 2 orders difference between how many should have been launched during that periode
    assertTrue(estimatedEvents > eventsToLaunch-eventsOffset && estimatedEvents < eventsToLaunch+eventsOffset);
    // not more then 2% difference in rate
    logger.debug(achievedRate + " < " + rate*(1+rateOffset));
    logger.debug(achievedRate + " > " + rate*(1-rateOffset));
    assertTrue(achievedRate < rate*(1+rateOffset) && achievedRate > rate*(1-rateOffset));
    
    triggers.clear();
    
    // few events at low rate
    // it has shown that we have higher diffs on low rates
    final double rateOffsetForLowRate = 0.3;
    
    eventsToLaunch = 5;
    rate = 0.1;
    taskId = myXfqctrl.startFrequencyControlledTask(new TestRateTaskCP("Test",eventsToLaunch,rate));

    info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    while (info.getTaskStatus() != "Finished") {
      Thread.sleep(1000);
      info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    }
    
    startTime = triggers.get(0);
    stopTime = triggers.get((int)eventsToLaunch-1);
    estimatedEvents = (stopTime - startTime) * (rate/1000.0);
    achievedRate = eventsToLaunch / ((stopTime - startTime) / 1000.0d);
    
    // not more then 2 orders difference between how many should have been launched during that periode
    assertTrue(estimatedEvents > eventsToLaunch-eventsOffset && estimatedEvents < eventsToLaunch+eventsOffset);
    // not more then 2% difference in rate
    logger.debug(achievedRate + " < " + rate*(1+rateOffsetForLowRate));
    logger.debug(achievedRate + " > " + rate*(1-rateOffsetForLowRate));
    assertTrue(achievedRate < rate*(1+rateOffsetForLowRate) && achievedRate > rate*(1-rateOffsetForLowRate));
    
    triggers.clear();
    
    
    // mid events & rate
    // neither do we achive a low failer on mid rates
    final double rateOffsetForMidRate = 0.05;
    eventsToLaunch = 50;
    rate = 5;
    taskId = myXfqctrl.startFrequencyControlledTask(new TestRateTaskCP("Test",eventsToLaunch,rate));

    info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    while (info.getTaskStatus() != "Finished") {
      Thread.sleep(1000);
      info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    }
    
    startTime = triggers.get(0);
    stopTime = triggers.get((int)eventsToLaunch-1);
    estimatedEvents = (stopTime - startTime) * (rate/1000.0);
    achievedRate = eventsToLaunch / ((stopTime - startTime) / 1000.0d);
    
    // not more then 2 orders difference between how many should have been launched during that periode
    assertTrue(estimatedEvents > eventsToLaunch-eventsOffset && estimatedEvents < eventsToLaunch+eventsOffset);
    // not more then 2% difference in rate
    logger.debug(achievedRate + " < " + rate*(1+rateOffsetForMidRate));
    logger.debug(achievedRate + " > " + rate*(1-rateOffsetForMidRate));
    assertTrue(achievedRate < rate*(1+rateOffsetForMidRate) && achievedRate > rate*(1-rateOffsetForMidRate));
    
    triggers.clear();
  }
  
  
  public void testCancelTask() throws InterruptedException, XynaException {
    //cancel loadEvent
    long eventsToLaunch = 20;
    long maxLoad = 3;
    long taskId = myXfqctrl.startFrequencyControlledTask(new TestLoadTaskCP("Test",eventsToLaunch,maxLoad));
    //Sleep a bit
    Thread.sleep(1500);
    
    assertTrue(myXfqctrl.cancelFrequencyControlledTask(taskId));
    Thread.sleep(300);
    FrequencyControlledTaskInformation info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    assertEquals("Canceled", info.getTaskStatus());
    assertTrue(info.getEventCount() != info.getEventsToLaunch());
    
  //Sleep again, check again and compare to evaluate if nothing changed (task's canceled, there should be no changes)
    Thread.sleep(1500);
    
    FrequencyControlledTaskInformation laterInfo = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    assertEquals(info.getEventCount(), laterInfo.getEventCount());
    //assertEquals(info.getFinishedEvents(), laterInfo.getFinishedEvents()); There could be some running events sleeping
    assertEquals(info.getTaskStatus(), laterInfo.getTaskStatus());
    
    triggers.clear();
    
    //cancel rate event
    eventsToLaunch = 30;
    double rate = 3;
    taskId = myXfqctrl.startFrequencyControlledTask(new TestRateTaskCP("Test",eventsToLaunch,rate));

  //Sleep a bit
    Thread.sleep(1500);
    
    assertTrue(myXfqctrl.cancelFrequencyControlledTask(taskId));
    Thread.sleep(300);
    info = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    assertEquals("Canceled", info.getTaskStatus());
    assertTrue(info.getEventCount() != info.getEventsToLaunch());
    
  //Sleep again, check again and compare to evaluate if nothing changed (task's canceled, there should be no changes)
    Thread.sleep(1500);
    
    laterInfo = myXfqctrl.getFrequencyControlledTaskInformation(taskId, null);
    assertEquals(info.getEventCount(), laterInfo.getEventCount());
    //assertEquals(info.getFinishedEvents(), laterInfo.getFinishedEvents()); There could be some running events sleeping
    assertEquals(info.getTaskStatus(), laterInfo.getTaskStatus());
    
    triggers.clear();
    
    //cancel a canceled task
    assertFalse(myXfqctrl.cancelFrequencyControlledTask(taskId));

  }
  
  
  public void testInvalidParams() throws InterruptedException, XynaException {

    // rate = 0 should not be allowed
    long eventsToLaunch = 1000;
    double rate = 0;

    
    try {
      myXfqctrl.startFrequencyControlledTask(new TestRateTaskCP("Test",eventsToLaunch,rate));
      fail("TaskCreation should have thrown a XPRC_InvalidCreationParameters");
    } catch (XPRC_InvalidCreationParameters e) {
      ;
    }

    
    // load controlled with zero maxEvents parallel
    eventsToLaunch = 3;
    long maxLoad = 0;
    
    try {
      myXfqctrl.startFrequencyControlledTask(new TestLoadTaskCP("Test",eventsToLaunch,maxLoad));
      fail("TaskCreation should have thrown a XPRC_InvalidCreationParameters");
    } catch (XPRC_InvalidCreationParameters e) {
      ;
    }

  }
  
  
  private class TestFrequencyControl extends XynaFrequencyControl {

    public TestFrequencyControl() throws XynaException {
      super();
    }
    
    @Override
    public void archiveFinishedTask(long taskId) throws XPRC_IllegalStateForTaskArchiving,
                    XPRC_InvalidFrequencyControlledTaskId, PersistenceLayerException {
      // Don't archive, keep em in memory for the TestDuration
    }
    
    
    @Override //Overridden to accept TestTask
    public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParams)
                    throws XynaException {
      FrequenceControlledTaskEventAlgorithm eventAlgorithm = FrequenceControlledTaskEventAlgorithm.createEventCreationAlgorithm(creationParams);
      FrequencyControlledTask task;
      FREQUENCY_CONTROLLED_TASK_TYPE type = creationParams.getTaskType();
      if (type == null) { //it's a TestTask
        task = new TestTask(creationParams, eventAlgorithm);
        tasks.put(task.getID(), task);
        task.start();
        return task.getID();  
      }     
      
      return super.startFrequencyControlledTask(creationParams);      
    }
    
  }
  
  
  private class TestLoadTaskCP extends FrequencyControlledTaskCreationParameter implements LoadControlledCreationParameter {
    private long maxLoad;
    public TestLoadTaskCP(String label, long eventsToLaunch, long maxLoad) {
      super(label, eventsToLaunch);
      this.maxLoad = maxLoad;
    }
    
    @Override
    public FREQUENCY_CONTROLLED_TASK_TYPE getTaskType() {
      return null;
    }
    
    public long getMaxLoad() {
      return maxLoad;
    }

    public long getSmoothingWait() {
      return 0;
    }   
  }
  
  private class TestRateTaskCP extends FrequencyControlledTaskCreationParameter implements RateControlledCreationParameter {
    private double rate;
    public TestRateTaskCP(String label, long eventsToLaunch, double rate) {
      super(label, eventsToLaunch);
      this.rate = rate;
    }

    @Override
    public FREQUENCY_CONTROLLED_TASK_TYPE getTaskType() {
      return null;
    }

    public double getRate() {
      return rate;
    }   
  }
  
  private class TestTask extends FrequencyControlledTask {
    
    public TestTask(FrequencyControlledTaskCreationParameter creationParameter,
                    FrequenceControlledTaskEventAlgorithm eventAlgorithm) {
      super(creationParameter, eventAlgorithm);
    }
    @Override
    protected FREQUENCY_CONTROLLED_TASK_TYPE getTaskType() {
      return null;
    }
    @Override
    public void eventTriggered(long eventId) {  
      super.eventTriggered(eventId);
      Thread t = new Thread(new TestEventExecution());
      t.start();
    }
    public void eventFinishedTest(long eventId) {
      super.eventFinished(eventId);
      logger.debug("Telling algorithm of finished event");
      getEventAlgorithm().eventFinished();
      logger.debug("Are we finished: " + getFinishedEventCount() + " == " + eventsToLaunch);
      if (getFinishedEventCount() == eventsToLaunch) {
        logger.debug("We're finished");
        this.status = FREQUENCY_CONTROLLED_TASK_STATUS.Finished;
      }
    }
    
    
    private class TestEventExecution implements Runnable {

      public void run() {
        triggers.add(System.currentTimeMillis());
        try {
          long sleepTime = (long) (Math.random()*100);
          logger.debug("now sleeping: "+sleepTime);
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        eventFinishedTest(0);
      }
    }


    @Override
    protected void customPostTaskProcessing() {
    }


    @Override
    protected Set<String> getAdditionalStatisticNames() {
      return null;
    }


    @Override
    protected Collection<StorableAggregatableDataEntry> getAdditionalStatistics(String statisticsName) {
      return null;
    }


    @Override
    protected String getAdditionalStatisticsUnit(String statisticsName) {
      return null;
    }

  }

}
