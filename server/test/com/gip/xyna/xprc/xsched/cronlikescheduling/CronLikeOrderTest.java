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

package com.gip.xyna.xprc.xsched.cronlikescheduling;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;


public class CronLikeOrderTest extends TestCase {

  /**
   * @param args
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY 
   */
  public static void main(String[] args) {
    testCalculateNextExecutionTime();
  }
  
  public static void createXynaFactoryMock(final AtomicLong idCounter) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    
    FutureExecution fexec = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(fexec.nextId()).andReturn(1).anyTimes();

    EasyMock.replay(fexec);
    
    XynaFactoryBase xynaFactory = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xynaFactory.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.replay(xynaFactory);
    XynaFactory.setInstance(xynaFactory);
    
    IDGenerator idGenerator = EasyMock.createMock(IDGenerator.class);
    EasyMock.expect(idGenerator.getUniqueId()).andAnswer(new IAnswer<Long>() {

      public Long answer() throws Throwable {
        return idCounter.getAndIncrement();
      }
    }).anyTimes();
    EasyMock.replay(idGenerator);
    
    
    
    RevisionManagement rm = EasyMock.createMock(RevisionManagement.class);
    EasyMock.expect(rm.getRevision(RevisionManagement.DEFAULT_WORKSPACE)).andReturn(-1L).anyTimes();
    EasyMock.replay(rm);
    
    XynaFactoryControl xfc = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfc.getRevisionManagement()).andReturn(rm).anyTimes();;
    EasyMock.replay(xfc);
    
    XynaFactoryManagementBase xfmb = EasyMock.createMock(XynaFactoryManagementBase.class); 
    EasyMock.expect(xfmb.getXynaFactoryControl()).andReturn(xfc).anyTimes();
    EasyMock.replay(xfmb);
    
    xynaFactory = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xynaFactory.getIDGenerator()).andReturn(idGenerator).anyTimes();
    EasyMock.expect(xynaFactory.getFactoryManagement()).andReturn(xfmb).anyTimes();
    EasyMock.replay(xynaFactory);

    XynaFactory.setInstance(xynaFactory);
  }
  
  private static class CronLikeOrderDerived extends CronLikeOrder {

    /**
     * 
     */
    private static final long serialVersionUID = 6975456327644770794L;

    public CronLikeOrderDerived(CronLikeOrderCreationParameter clocp) {
      super( clocp );
    }
    
    public long calculateNextExecutionTime( long now ) {
      return super.calculateNextExecutionTime(now);
    }
  }
  
  public static void testCalculateNextExecutionTime() {
    try {
      createXynaFactoryMock(new AtomicLong(0));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    TimeZone tz = TimeZone.getTimeZone("Europe/Berlin");
    Calendar cal = Calendar.getInstance(tz);
    cal.setTimeInMillis(System.currentTimeMillis());
    cal.set(Calendar.HOUR_OF_DAY, 2);
    cal.set(Calendar.MINUTE, 30);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    long interval = 24 * 60 * 60 * 1000L;
    CronLikeOrderCreationParameter clocp = new CronLikeOrderCreationParameter( "Test", "QS.infrastructure.EmptyWF", cal, interval, true, true, OnErrorAction.DISABLE, null, null, null, null, (GeneralXynaObject[])null);
    CronLikeOrder clo = new CronLikeOrderDerived(clocp);
    clo.setNextExecutionTime(cal.getTimeInMillis());
    
    cal.setTimeInMillis(clo.getNextExecution());
    System.out.println(String.format("%d : %4d-%02d-%02dT%02d:%02d:%02d,%03d", clo.getNextExecution(),
                                     cal.get(Calendar.YEAR),
                                     cal.get(Calendar.MONTH) + 1,
                                     cal.get(Calendar.DAY_OF_MONTH),
                                     cal.get(Calendar.HOUR_OF_DAY),
                                     cal.get(Calendar.MINUTE),
                                     cal.get(Calendar.SECOND),
                                     cal.get(Calendar.MILLISECOND)));
    
    Calendar calPrev = Calendar.getInstance(tz);
    calPrev.setTimeInMillis(cal.getTimeInMillis());
    Calendar calNext = Calendar.getInstance(tz);
    calNext.setTimeInMillis(cal.getTimeInMillis());
    
    for (int d = 0; d < 366; d++) {
      long now = clo.getNextExecution();
      long next = clo.calculateNextExecutionTime(now);
      clo.setNextExecutionTime(next);
      calNext.setTimeInMillis(next);

      if (tz.inDaylightTime(calPrev.getTime()) != tz.inDaylightTime(calNext.getTime())) {
        System.out.println( "Time Shift" );
        
        if (tz.inDaylightTime(calNext.getTime())) {
          // when setting to DST, there is a gap - the time difference is shorter than 24h
          assertEquals(3, calNext.get(Calendar.HOUR_OF_DAY));
          assertEquals(0, calNext.get(Calendar.MINUTE));
          assertTrue( ( calNext.getTimeInMillis() - calPrev.getTimeInMillis() ) < 24 * 60 * 60 * 1000 );
        } else {
          // otherwise there is an ambiguous time where we take the latter one - the time difference is longer than 24h
          assertEquals(cal.get(Calendar.HOUR_OF_DAY), calNext.get(Calendar.HOUR_OF_DAY));
          assertEquals(cal.get(Calendar.MINUTE), calNext.get(Calendar.MINUTE));
          assertTrue( ( calNext.getTimeInMillis() - calPrev.getTimeInMillis() ) > 24 * 60 * 60 * 1000 );
        }
      } else {
        assertEquals(cal.get(Calendar.HOUR_OF_DAY), calNext.get(Calendar.HOUR_OF_DAY));
        assertEquals(cal.get(Calendar.MINUTE), calNext.get(Calendar.MINUTE));
      }

      printTime(calNext);

      calPrev.setTimeInMillis(calNext.getTimeInMillis());
    }
  }

  public static void testStaticCalendarDefinition() {
    try {
      createXynaFactoryMock(new AtomicLong(0));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    cal.set(Calendar.HOUR_OF_DAY, 2);
    cal.set(Calendar.MINUTE, 30);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    CronLikeOrderCreationParameter clocp = new CronLikeOrderCreationParameter( "Test", "QS.infrastructure.EmptyWF", cal, null, true, true, OnErrorAction.DISABLE, null, null, null, null, (GeneralXynaObject[])null);
    clocp.setCalendarDefinition("[Day=1]"); //immer am 1. eines Monats
    CronLikeOrder clo = new CronLikeOrderDerived(clocp);
    
    Calendar calNext = Calendar.getInstance();
    
    for (int d = 0; d < 12; d++) {
      long next = clo.getNextExecution();
      calNext.setTimeInMillis(next);
      printTime(calNext);
      assertEquals((cal.get(Calendar.MONTH) + d + 1) % 12, calNext.get(Calendar.MONTH));
      assertEquals(1, calNext.get(Calendar.DAY_OF_MONTH));
      assertEquals(cal.get(Calendar.HOUR_OF_DAY), calNext.get(Calendar.HOUR_OF_DAY));
      assertEquals(cal.get(Calendar.MINUTE), calNext.get(Calendar.MINUTE));
      
      next = clo.calculateNextExecutionTime(next);
      clo.setNextExecutionTime(next);
    }
  }

  public static void testExecuteImmediately() {
    try {
      createXynaFactoryMock(new AtomicLong(0));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    TimeZone tz = TimeZone.getTimeZone("Europe/Berlin");
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    
    CronLikeOrderCreationParameter clocp = new CronLikeOrderCreationParameter( "Test", "QS.infrastructure.EmptyWF", null, tz.getID(), null, false, true, OnErrorAction.DISABLE, null, null, null, null, (GeneralXynaObject[])null);
    clocp.setCalendarDefinition("[Hour=:1]"); //jede Stunde
    CronLikeOrder clo = new CronLikeOrderDerived(clocp);
    
    Calendar calNext = Calendar.getInstance();
    
    //keine Startzeit angegeben -> startet sofort und dann jede Stunde
    for (int d = 0; d < 24; d++) {
      long next = clo.getNextExecution();
      calNext.setTimeInMillis(next);
      printTime(calNext);
      assertEquals((cal.get(Calendar.HOUR_OF_DAY) + d) % 24, calNext.get(Calendar.HOUR_OF_DAY));
      assertEquals(cal.get(Calendar.MINUTE), calNext.get(Calendar.MINUTE));
      
      next = clo.calculateNextExecutionTime(next);
      clo.setNextExecutionTime(next);
    }
  }
  
  
  
  private static void printTime(Calendar cal) {
    System.out.println(String.format("%d : %4d-%02d-%02dT%02d:%02d:%02d,%03d (%b)", cal.getTimeInMillis(),
                                     cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                                     cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
                                     cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
                                     cal.get(Calendar.MILLISECOND), cal.getTimeZone().inDaylightTime(cal.getTime())));
  }
}
