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
package com.gip.xyna.utils.scheduler;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.gip.xyna.utils.scheduler.UrgencyOrderList.Urgency;


/**
 * 
 * 
 * TODO noch Testfälle aus com.gip.xyna.xprc.xsched.SchedulerAlgorithmUsingUrgencyAndDemandTest
 *                     und com.gip.xyna.xprc.xsched.SchedulerAlgorithmTestBase
 *      übernehmen?
 * TODO com.gip.xyna.xprc.xsched.ParallelSchedulingTest für CapacityDemand-Tests 
 *      sind hier nicht möglich
 * alle drei Tests sind gelöscht, da sie nach Scheduler-Umstellung schwierig zu reparieren sind.
 *
 *
 * TODO weitere Testfälle?
 */
public class SchedulerTest extends TestCase {

  public void testScheduleOne() {
    
    Scheduler<TestOrder, TestSchedulerInformation> scheduler = createTestScheduler(
        new TryScheduleTest() {

          @Override
          public ScheduleResult trySchedule(Urgency<TestOrder> uo) {
            if( uo.getOrderId() == 4 ) {
              execute(uo);
              return ScheduleResult.Scheduled;
            } else {
              return ScheduleResult.Continue;
            }
          }

        }
        ); 
    
    for( int i=0; i<10; ++i ) {
      scheduler.offerOrder( new TestOrder(i) );
    }
    scheduler.exec();
    
    TestSchedulerInformation tsi = scheduler.getSchedulerInformation();
    assertEquals(10, tsi.getIterated() );
    assertEquals(1, tsi.getScheduled() );
    assertEquals("1_10_1", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("0_0_9", tsi.getWaiting_Tags_Null_Unknown() );
    assertEquals("[4]", tsi.getScheduledOrdersAsString() );
     
    
  }

 

  public void testScheduleCapacity() {
    
    Scheduler<TestOrder, TestSchedulerInformation> scheduler = createTestScheduler(
        new TryScheduleTest() {
          @Override
          public ScheduleResult trySchedule(Urgency<TestOrder> uo) {
            switch( (int)(uo.getOrderId() %3) ) {
              case 0: //Cap A ist dauerhaft nicht verfügbar
                return new ScheduleResult.TagScheduleResult("A", true);
              case 1: //Cap B ist erst nach dem 2 ScheulerLauf wieder verfügbar
                if( getCurrentSchedulerInformation().schedulerRunNumber <= 2 ) {
                  return new ScheduleResult.TagScheduleResult("B", true);
                } else {
                  execute(uo);
                  return ScheduleResult.Scheduled;
                }
              case 2: //Cap C ist verfügbar
                execute(uo);
                return ScheduleResult.Scheduled;
              default:
                return ScheduleResult.BreakLoop;
            }
          }
          
        }
        ); 
    
    for( int i=0; i<10; ++i ) {
      scheduler.offerOrder( new TestOrder(i) );
    }
    scheduler.exec();
    TestSchedulerInformation tsi = scheduler.getSchedulerInformation();
    assertEquals("1_10_3", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("7_0_0", tsi.getWaiting_Tags_Null_Unknown() ); //A+B
    assertEquals("[2, 5, 8]", tsi.getScheduledOrdersAsString() );
    
    scheduler.exec();
    tsi = scheduler.getSchedulerInformation();
    assertEquals("2_2_0", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("7_0_0", tsi.getWaiting_Tags_Null_Unknown() ); //A+B
    assertEquals("[]", tsi.getScheduledOrdersAsString() );
    
    scheduler.exec();
    tsi = scheduler.getSchedulerInformation();
    assertEquals("3_4_3", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("4_0_0", tsi.getWaiting_Tags_Null_Unknown() ); //A
    assertEquals("[1, 4, 7]", tsi.getScheduledOrdersAsString() );
    
  }

  public void testBreakLoop() {
    
    Scheduler<TestOrder, TestSchedulerInformation> scheduler = createTestScheduler(
        new TryScheduleTest() {
          @Override
          public ScheduleResult trySchedule(Urgency<TestOrder> uo) {
            if( uo.getOrderId() == 4 ) {
              return ScheduleResult.BreakLoop;
            } else {
              return ScheduleResult.Scheduled;
            }
          }
        }
        ); 
    
    for( int i=0; i<10; ++i ) {
      scheduler.offerOrder( new TestOrder(i) );
    }
    
    scheduler.exec();
    TestSchedulerInformation tsi = scheduler.getSchedulerInformation();
    assertEquals("1_5_4", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("0_0_6", tsi.getWaiting_Tags_Null_Unknown() ); //6 Aufträge sind nicht untersucht worden
    
    scheduler.exec();
    tsi = scheduler.getSchedulerInformation();
    assertEquals("2_1_0", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("0_0_6", tsi.getWaiting_Tags_Null_Unknown() );
   
    assertFalse(tsi.isLoopEndedRegularily() );
  }

  public void testException() {
    
    Scheduler<TestOrder, TestSchedulerInformation> scheduler = createTestScheduler(
        new TryScheduleTest() {
          @Override
          public ScheduleResult trySchedule(Urgency<TestOrder> uo) {
            if( uo.getOrderId() == 4 ) {
              throw new RuntimeException("Testing");
            } else {
              return ScheduleResult.Scheduled;
            }
          }
        }
        ); 
    
    for( int i=0; i<10; ++i ) {
      scheduler.offerOrder( new TestOrder(i) );
    }
    
    scheduler.exec();
    TestSchedulerInformation tsi = scheduler.getSchedulerInformation();
    assertEquals("1_5_4", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("0_0_6", tsi.getWaiting_Tags_Null_Unknown() ); //6 Aufträge sind nicht untersucht worden
    
    scheduler.exec();
    tsi = scheduler.getSchedulerInformation();
    assertEquals("2_1_0", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("0_0_6", tsi.getWaiting_Tags_Null_Unknown() );
   
    assertFalse(tsi.isLoopEndedRegularily() );
  }

  public void testSchedulingOrderUsingUrgency() {
   
    TryScheduleTest c = new TryScheduleTest() {
      @Override
      public ScheduleResult trySchedule(Urgency<TestOrder> uo) {
        execute(uo);
        return ScheduleResult.Scheduled;
      }
    };

    Scheduler<TestOrder, TestSchedulerInformation> scheduler = createTestScheduler(c);
    
    scheduler.offerOrder( new TestOrder(0, 1000) );
    scheduler.offerOrder( new TestOrder(2, 2000) );
    scheduler.offerOrder( new TestOrder(1, 2000) );
    scheduler.offerOrder( new TestOrder(4, 100) );
    scheduler.offerOrder( new TestOrder(5, 10000) );
    scheduler.offerOrder( new TestOrder(3, 300) );
    
    scheduler.exec();
    TestSchedulerInformation tsi = scheduler.getSchedulerInformation();
    assertEquals("1_6_6", tsi.getSchedulerRunNumber_Iterated_Scheduled() );
    assertEquals("0_0_0", tsi.getWaiting_Tags_Null_Unknown() ); //6 Aufträge sind nicht untersucht worden
    assertEquals("[5, 1, 2, 0, 3, 4]", tsi.getScheduledOrdersAsString() );
    
  }

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  public static Scheduler<TestOrder, TestSchedulerInformation> createTestScheduler(
    SchedulerCustomisation<TestOrder, TestSchedulerInformation> customisation ) {
    return new Scheduler<TestOrder, TestSchedulerInformation>(customisation);
  }
  
  private static class TestOrder {
    long id;
    long urgency;
    
    
    public TestOrder(long id) {
      this.id = id;
      this.urgency = 0;
    }
    public TestOrder(long id, long urgency) {
      this.id = id;
      this.urgency = urgency;
    }

    public long getId() {
      return id;
    }
    
    @Override
    public String toString() {
      return "TestOrder("+id+")";
    }
  }
 
  
  private abstract static class TryScheduleTest extends SimpleSchedulerCustomisation<TestOrder, TestSchedulerInformation> {

    private TestSchedulerInformationBuilder tsib = new TestSchedulerInformationBuilder();
    
    @Override
    public long calculateUrgency(TestOrder order) {
      return order.urgency;
    }
    
    protected void execute(Urgency<TestOrder> uo) {
      tsib.addOrderId(uo.getOrderId());
    }
   


    @Override
    public long getOrderId(TestOrder order) {
      return order.getId();
    }

    @Override
    public SchedulerInformationBuilder<TestSchedulerInformation> getInformationBuilder() {
      return tsib;
    }

    public TestSchedulerInformation getCurrentSchedulerInformation() {
      return tsib.tsi;
    }
    
  }
  
  
  
  
  
  public static class TestSchedulerInformation {

    int scheduled;
    int iterated;
    public int waitingForTags;
    public int waitingForTagNull;
    public int waitingForUnknown;
    public long schedulerRunNumber;
    public boolean loopEndedRegularily;
    public List<Long> scheduledOrders = new ArrayList<Long>();

    public int getScheduled() {
      return scheduled;
    }

    public int getIterated() {
      return iterated;
    }

    public int getWaitingForTags() {
      return waitingForTags;
    }


    public long getSchedulerRunNumber() {
      return schedulerRunNumber;
    }

    public String getSchedulerRunNumber_Iterated_Scheduled() {
      return schedulerRunNumber+"_"+iterated+"_"+scheduled;
    }

    public String getWaiting_Tags_Null_Unknown() {
      return waitingForTags+"_"+waitingForTagNull+"_"+waitingForUnknown;
    }
    
    public boolean isLoopEndedRegularily() {
      return loopEndedRegularily;
    }
    
    
    public String getScheduledOrdersAsString() {
      return scheduledOrders.toString();
    }

  }
  
  public static class TestSchedulerInformationBuilder implements SchedulerInformationBuilder<TestSchedulerInformation> {
    
    private TestSchedulerInformation tsi = new TestSchedulerInformation();
    
    
    public TestSchedulerInformation build() {
      TestSchedulerInformation ret = tsi;
      tsi = new TestSchedulerInformation();
      return ret;
    }

    public void addOrderId(Long orderId) {
      // TODO Auto-generated method stub
      tsi.scheduledOrders .add(orderId);
    }

    public long setTimestamp(Timestamp timestamp) {
      // TODO Auto-generated method stub
      return 0;
    }

    public void loopEndedRegularily(boolean loopEndedRegularily) {
      tsi.loopEndedRegularily = loopEndedRegularily;
    }

    public void scheduledOrders(int scheduled) {
      tsi.scheduled = scheduled;
    }

    public void iteratedOrders(int iterated) {
      tsi.iterated = iterated;
    }

    public long timestamp(Timestamp timestamp) {
      // TODO Auto-generated method stub
      return 0;
    }

    public void waitingForTags(int waitingForTags) {
      tsi.waitingForTags = waitingForTags;
    }

    public void waitingForTagNull(int waitingForTagNull) {
      tsi.waitingForTagNull = waitingForTagNull;
    }

    public void waitingForUnknown(int waitingForUnknown) {
      tsi.waitingForUnknown = waitingForUnknown;
    }

    public void schedulerRunNumber(long schedulerRunNumber) {
      tsi.schedulerRunNumber = schedulerRunNumber;
    }
    

    
  }
  
}
