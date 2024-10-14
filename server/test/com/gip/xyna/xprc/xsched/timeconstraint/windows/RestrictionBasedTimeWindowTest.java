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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.RestrictionBasedTimeWindow.RestrictionBasedTimeWindowDefinition;

import junit.framework.TestCase;


public class RestrictionBasedTimeWindowTest extends TestCase {
  
  private static final String EUROPE_BERLIN = "Europe/Berlin";

  
  public static DateTime toDateTime(long timestamp) {
    DateTimeZone dtz = DateTimeZone.forID(EUROPE_BERLIN);
    return toDateTime(timestamp, dtz);
  }
  
  public static DateTime toDateTime(long timestamp, DateTimeZone dtz) {
    return new DateTime(timestamp, dtz);
  }
  
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TimeControlRuleBuilder.CHECK_DST = false;
  }

    private static final String RESTRICTION_1 = "[Hour=20;Minute=15;DayOfWeek=SAT][Hour=7;Minute=45]"; // Samstag Abend
  private static final String RESTRICTION_2 = "[Second=0;Minute=0;Hour=12:7][Hour=5]"; // Alle sieben Stunden ab 12 Uhr am Tag der Initialisierung
  private static final String RESTRICTION_3 = "[Second=0;Minute=0;Hour=12;Day=10-10L][Hour=8]"; // zwischem dem 10 und dem 10 letzten (in der Regel 10. bis 20.-21.)
  private static final String RESTRICTION_4 = "[Second=0;Minute=0;Hour=12;Day=20W][Hour=3]"; // am Wochentag welcher dem 20. am nähesten is
  private static final String RESTRICTION_5 = "[Second=0;Minute=0;Hour=12;Day=2#MON;Month=JAN][Hour=8]"; // der 2. Montag im Januar
  private static final String RESTRICTION_6 = "[Second=0;Minute=0;Hour=12;Day=31][Hour=8]"; // immer am 31.
  private static final String RESTRICTION_7 = "[Second=0;Minute=0;Hour=12;Day=15;Month=JAN-MAR;Year=2013][Hour=8]"; // am 15. zwischen Januar und Februar 2013
  private static final String RESTRICTION_8 = "[Second=0;Minute=0;Hour=12;Day=3L-3][Hour=8]"; // zwischen den drei letzten und ersten Tagen des Monats
  private static final String RESTRICTION_9 = "[Hour=:7][Hour=1]"; // alle 7 Stunden
  private static final String RESTRICTION_10 = "[Second=0;Minute=0;Hour=14][Hour=1]"; // um 14 Uhr
  private static final String RESTRICTION_11 = "[Minute=30;Hour=2;Day=1L][Hour=1]"; // um 2 Uhr 30 am letzten Tag jedes Monats 

  
  public void testRestriction1() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_1, EUROPE_BERLIN, true));
    
    long now = 1366538400000L; // 2013-04-21T12:00:00.000
    DateTime nowDT = toDateTime(now);
    tw.recalculate(now);
    
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertTrue(nextOpen.getDayOfWeek() == DateTimeConstants.SATURDAY);
    DateTime nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    assertEquals((7 * 60 * 60 * 1000) + (45 * 60 * 1000), nextClose.minus(nextOpen.getMillis()).getMillis());
    DateTime since = toDateTime(tw.getSince());
    // letzte Schließung + 1 Woche - Dauer = nextOpen
    assertEquals(since.plusWeeks(1).minus((7 * 60 * 60 * 1000) + (45 * 60 * 1000)), nextOpen);
    assertFalse(tw.isOpen());
    
    now = 1366491600000L; // 2013-04-20T23:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertTrue(nextOpen.getDayOfWeek() == DateTimeConstants.SATURDAY);
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    since = toDateTime(tw.getSince());
    assertEquals(since.plus((7 * 60 * 60 * 1000) + (45 * 60 * 1000)), nextClose);
    assertTrue(tw.isOpen());
    
    now = 1366481700000L; // 2013-04-20T20:15:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertTrue(nextOpen.getDayOfWeek() == DateTimeConstants.SATURDAY);
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    since = toDateTime(tw.getSince());
    assertEquals(since, nowDT);
    assertEquals(nowDT.plusWeeks(1), nextOpen);
    assertTrue(tw.isOpen());
    
  }
  
  public void testRestriction2() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_2, EUROPE_BERLIN, true));
    
    long now = 1366448400000L; // 2013-04-20T11:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertTrue(nowDT.plusHours(1).equals(nextOpen));
    DateTime nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    DateTime since = toDateTime(tw.getSince());
    assertEquals(since.plusHours(2), nextOpen);
    assertFalse(tw.isOpen());
    
    now = 1366452000000L; // 2013-04-20T12:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertEquals(nextOpen, nowDT.plusHours(7));
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    since = toDateTime(tw.getSince());
    assertEquals(since, nowDT);
    assertTrue(tw.isOpen());
    
    now = 1366459200000L; // 2013-04-20T14:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    assertEquals(nextClose.plusHours(2), nextOpen);
    since = toDateTime(tw.getSince());
    assertEquals(since.plusHours(2), nowDT);
    assertTrue(tw.isOpen());
    
  }
  
  
  public void testRestriction3() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_3, EUROPE_BERLIN, true));
    
    long now = 1366876800000L; // 2013-04-25T10:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusMonths(1).getMonthOfYear(), nextOpen.getMonthOfYear());
    DateTime nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    DateTime since = toDateTime(tw.getSince());
    assertNotSame(nowDT.minusDays(1).getDayOfYear(), since.getDayOfYear());
    assertFalse(tw.isOpen());
    
    now = 1368172800000L; // 2013-05-10T10:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    assertEquals(nowDT.plusHours(2), nextOpen);
    assertFalse(tw.isOpen());
    
    now = 1368180000000L; // 2013-05-10T12:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    since = toDateTime(tw.getSince());
    assertEquals(nowDT, since);
    assertTrue(tw.isOpen());
    
    now = 1369134000000L; // 2013-05-21T13:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusMonths(1).getMonthOfYear(), nextOpen.getMonthOfYear());
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    since = toDateTime(tw.getSince());
    assertEquals(nowDT.minusHours(1), since);
    assertTrue(tw.isOpen());
    
  }
  
  
  public void testRestriction4() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_4, EUROPE_BERLIN, true));
    
    long now = 1366444800000L; // 2013-04-20T10:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusMonths(1).getMonthOfYear(), nextOpen.getMonthOfYear());
    DateTime nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    DateTime since = toDateTime(tw.getSince());
    assertEquals(nowDT.minusDays(1).plusHours(5), since);
    assertFalse(tw.isOpen());
    
    now = 1369036800000L; // 2013-05-20T10:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusHours(2), nextOpen);
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    assertFalse(tw.isOpen());
    
    now = 1366369200000L; // 2013-04-19T13:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusMonths(1).getMonthOfYear(), nextOpen.getMonthOfYear());
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    assertEquals(nowDT.plusHours(2), nextClose);
    since = toDateTime(tw.getSince());
    assertEquals(nowDT.minusHours(1), since);
    assertTrue(tw.isOpen());
    
  }
  
  
  public void testRestriction5() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_5, EUROPE_BERLIN, true));
    
    long now = 1366444800000L; // 2013-04-20T10:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusYears(1).getYear(), nextOpen.getYear());
    assertEquals(13, nextOpen.getDayOfMonth());
    DateTime nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    DateTime since = toDateTime(tw.getSince());
    assertEquals(nowDT.getYear(), since.getYear());
    assertEquals(14, since.getDayOfMonth());
    assertFalse(tw.isOpen());
    
    now = 1358193600000L; // 2013-01-14T21:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusYears(1).getYear(), nextOpen.getYear());
    assertEquals(13, nextOpen.getDayOfMonth());
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isAfter(nextOpen));
    since = toDateTime(tw.getSince());
    assertEquals(nowDT.getYear(), since.getYear());
    assertEquals(14, since.getDayOfMonth());
    assertEquals(nowDT.minusHours(1), since);
    assertFalse(tw.isOpen());
    
    now = 1358164800000L; // 2013-01-14T13:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(nextOpen.isAfter(nowDT));
    assertEquals(nowDT.plusYears(1).getYear(), nextOpen.getYear());
    assertEquals(13, nextOpen.getDayOfMonth());
    nextClose = toDateTime(tw.getNextClose());
    assertTrue(nextClose.isAfter(nowDT));
    assertTrue(nextClose.isBefore(nextOpen));
    since = toDateTime(tw.getSince());
    assertEquals(nowDT.getYear(), since.getYear());
    assertEquals(14, since.getDayOfMonth());
    assertEquals(nowDT.minusHours(1), since);
    assertTrue(tw.isOpen());
  }
  
  
  public void testRestriction6() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_6, EUROPE_BERLIN, true));
    
    long now = 1366012800000L; // 2013-04-15T10:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    DateTime since = toDateTime(tw.getSince());
    assertEquals(nowDT.plusMonths(1).getMonthOfYear(), nextOpen.getMonthOfYear());
    assertEquals(nowDT.minusMonths(1).getMonthOfYear(), since.getMonthOfYear());
    assertFalse(tw.isOpen());
    
    now = 1369904400000L; // 2013-05-30T11:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    assertEquals(nowDT.plusDays(1).plusHours(1), nextOpen);
    assertFalse(tw.isOpen());
    
    now = 1369998000000L; // 2013-05-31T13:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    DateTime nextClose = toDateTime(tw.getNextClose());
    since = toDateTime(tw.getSince());
    assertEquals(nowDT.minusHours(1), since);
    assertEquals(nowDT.plusHours(7), nextClose);
    assertEquals(nowDT.plusMonths(2).getMonthOfYear(), nextOpen.getMonthOfYear());
    assertTrue(tw.isOpen());
    
  }
  
  
  public void testRestriction7() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_7, EUROPE_BERLIN, true));
    
    long now = 1357808400000L; // 2013-01-10T10:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    assertTrue(tw.getSince() < 9223372036000000000L);
    assertEquals(nowDT.plusDays(5).plusHours(2), nextOpen);
    assertFalse(tw.isOpen());
    
    now = 1358251200000L; // 2013-01-15T13:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    DateTime nextClose = toDateTime(tw.getNextClose());
    DateTime since = toDateTime(tw.getSince());
    assertEquals(nowDT.minusHours(1), since);
    assertEquals(nowDT.minusHours(1).plusMonths(1), nextOpen);
    assertTrue(nowDT.isBefore(nextClose));
    assertTrue(nextClose.isBefore(nextOpen));
    assertTrue(tw.isOpen());
    
    now = 1363348800000L; // 2013-03-15T13:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    since = toDateTime(tw.getSince());
    assertEquals(nowDT.minusHours(1), since);
    assertEquals(Long.MAX_VALUE, tw.getNextOpen());
    assertTrue(tw.isOpen());
    
    now = 1363428000000L; // 2013-03-16T11:00:00.000
    
    tw.recalculate(now);
    assertEquals(Long.MAX_VALUE, tw.getNextOpen());
    assertEquals(Long.MAX_VALUE, tw.getNextClose());
    assertFalse(tw.isOpen());
    
  }
  
  
  public void testRestriction8() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_8, EUROPE_BERLIN, true));
    
    long now = 1367136000000L; // 2013-04-28T10:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    DateTime since = toDateTime(tw.getSince());
    assertEquals(nextOpen, nowDT.plusHours(2));
    assertEquals(3, since.getDayOfMonth());
    assertFalse(tw.isOpen());
    
    now = 1367143200000L; // 2013-04-28T12:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    DateTime nextClose = toDateTime(tw.getNextClose());
    since = toDateTime(tw.getSince());
    assertEquals(nowDT, since);
    assertEquals(nextClose, nowDT.plusHours(8));
    assertTrue(tw.isOpen());
    
    now = 1367582400000L; // 2013-05-03T14:00:00.000
    nowDT = toDateTime(now);
    
    tw.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen());
    nextClose = toDateTime(tw.getNextClose());
    since = toDateTime(tw.getSince());
    assertEquals(since, nowDT.minusHours(2));
    assertEquals(29, nextOpen.getDayOfMonth());
    assertEquals(nextClose, nowDT.plusHours(6));
    assertTrue(tw.isOpen());
  }
  
  
  public void testRestriction11() {
    TimeControlRuleBuilder builder = new TimeControlRuleBuilder();
    RestrictionBasedTimeWindow tw = builder.buildTimeWindow(new RestrictionBasedTimeWindowDefinition(RESTRICTION_11, EUROPE_BERLIN, true));
    
    long now = 1356994800000L; // 2013-01-01T00:00:00.000+01:00
    
    tw.recalculate(now);
    DateTime nextOpenDT = toDateTime(tw.getNextOpen());
    
    assertEquals(30, nextOpenDT.getMinuteOfHour());
    assertEquals(2, nextOpenDT.getHourOfDay());
    assertEquals(31, nextOpenDT.getDayOfMonth());
    assertEquals(DateTimeConstants.JANUARY, nextOpenDT.getMonthOfYear());
    assertEquals(1000 * 60 * 60, tw.getNextClose() - tw.getNextOpen());
    
    tw.recalculate(nextOpenDT.plusDays(1).getMillis());
    nextOpenDT = toDateTime(tw.getNextOpen());
    
    assertEquals(30, nextOpenDT.getMinuteOfHour());
    assertEquals(2, nextOpenDT.getHourOfDay());
    assertEquals(28, nextOpenDT.getDayOfMonth());
    assertEquals(DateTimeConstants.FEBRUARY, nextOpenDT.getMonthOfYear());
    assertEquals(1000 * 60 * 60, tw.getNextClose() - tw.getNextOpen());
    
    tw.recalculate(nextOpenDT.plusDays(1).getMillis());
    nextOpenDT = toDateTime(tw.getNextOpen());
    
    assertEquals(0, nextOpenDT.getMinuteOfHour());
    assertEquals(3, nextOpenDT.getHourOfDay()); // execution in saved time
    assertEquals(31, nextOpenDT.getDayOfMonth());
    assertEquals(DateTimeConstants.MARCH, nextOpenDT.getMonthOfYear());
    assertEquals(1000 * 60 * 60, tw.getNextClose() - tw.getNextOpen());
    
    tw.recalculate(nextOpenDT.plusDays(1).getMillis());
    nextOpenDT = toDateTime(tw.getNextOpen());
    
    assertEquals(30, nextOpenDT.getMinuteOfHour());
    assertEquals(2, nextOpenDT.getHourOfDay());
    assertEquals(30, nextOpenDT.getDayOfMonth());
    assertEquals(DateTimeConstants.APRIL, nextOpenDT.getMonthOfYear());
    assertEquals(1000 * 60 * 60, tw.getNextClose() - tw.getNextOpen());
    
  }
  
  
  public void testIncrementPreservationAcrossSerialization() {
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(RESTRICTION_9, EUROPE_BERLIN, true);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    long now = 1367136000000L; // 2013-04-28T10:00:00.000
    DateTime nowDT = toDateTime(now);
    
    tw.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen());
    DateTime nextClose = toDateTime(tw.getNextClose());
    DateTime since = toDateTime(tw.getSince());
    assertTrue(tw.isOpen());
    assertEquals(since, nowDT);
    assertEquals(nextClose, nowDT.plusHours(1));
    assertEquals(nextOpen, nowDT.plusHours(7));

    nowDT = nextOpen;
    
    tw.recalculate(tw.getNextOpen());
    nextOpen = toDateTime(tw.getNextOpen());
    nextClose = toDateTime(tw.getNextClose());
    since = toDateTime(tw.getSince());
    assertTrue(tw.isOpen());
    assertEquals(since, nowDT);
    assertEquals(nextClose, nowDT.plusHours(1));
    assertEquals(nextOpen, nowDT.plusHours(7));
    
    String serialization = tw.getDefinition().serializeToString();
    RestrictionBasedTimeWindowDefinition deserializedDef = (RestrictionBasedTimeWindowDefinition) TimeWindowDefinition.valueOf(serialization);
    RestrictionBasedTimeWindow deserializedTw = (RestrictionBasedTimeWindow) deserializedDef.constructTimeWindow();
    
    now = 1367160600000L; // 2013-04-28T16:50:00.000
    nowDT = toDateTime(now);
    
    deserializedTw.recalculate(now);
    nextOpen = toDateTime(deserializedTw.getNextOpen());
    nextClose = toDateTime(deserializedTw.getNextClose());
    since = toDateTime(deserializedTw.getSince());
    assertEquals(nextOpen, nowDT.plusMinutes(10));
    assertEquals(since, nowDT.plusMinutes(10).minusHours(6));
    assertFalse(deserializedTw.isOpen());
    
    now = 1367136000000L; // 2013-04-28T10:00:00.000
    nowDT = toDateTime(now);
    
    deserializedTw.recalculate(now);
    nextOpen = toDateTime(deserializedTw.getNextOpen());
    nextClose = toDateTime(deserializedTw.getNextClose());
    since = toDateTime(deserializedTw.getSince());
    assertTrue(deserializedTw.isOpen());
    assertEquals(since, nowDT);
    assertEquals(nextClose, nowDT.plusHours(1));
    assertEquals(nextOpen, nowDT.plusHours(7));
    
  }
  
  
  public void testNotConsiderDaylightSaving() {
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(RESTRICTION_10, EUROPE_BERLIN, false);
    RestrictionBasedTimeWindowDefinition defDST = new RestrictionBasedTimeWindowDefinition(RESTRICTION_10, EUROPE_BERLIN, true);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    RestrictionBasedTimeWindow twDST = (RestrictionBasedTimeWindow) defDST.constructTimeWindow();
    
    
    long now = 1364649000000L; // 2013-03-30T14:10:00.000
    
    tw.recalculate(now);
    twDST.recalculate(now);
    DateTime nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
    DateTime nextClose = toDateTime(tw.getNextClose(), DateTimeZone.UTC);
    DateTime since = toDateTime(tw.getSince(), DateTimeZone.UTC);
    DateTime nextOpenDST = toDateTime(twDST.getNextOpen(), DateTimeZone.UTC);
    DateTime nextCloseDST = toDateTime(twDST.getNextClose(), DateTimeZone.UTC);
    DateTime sinceDST = toDateTime(twDST.getSince(), DateTimeZone.UTC);
    assertTrue(tw.isOpen());
    assertTrue(twDST.isOpen());
    assertEquals(since, sinceDST);
    assertEquals(nextClose, nextCloseDST);
    assertEquals(nextOpen, nextOpenDST.plusHours(1));
    
    
    now = 1364731800000L; //2013-03-31T14:10:00.000
    
    tw.recalculate(now);
    twDST.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
    nextClose = toDateTime(tw.getNextClose(), DateTimeZone.UTC);
    since = toDateTime(tw.getSince(), DateTimeZone.UTC);
    nextOpenDST = toDateTime(twDST.getNextOpen(), DateTimeZone.UTC);
    nextCloseDST = toDateTime(twDST.getNextClose(), DateTimeZone.UTC);
    sinceDST = toDateTime(twDST.getSince(), DateTimeZone.UTC);
    assertFalse(tw.isOpen());
    assertTrue(twDST.isOpen());
    
    now = 1364735400000L; //2013-03-31T15:10:00.000
    
    tw.recalculate(now);
    twDST.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
    nextClose = toDateTime(tw.getNextClose(), DateTimeZone.UTC);
    since = toDateTime(tw.getSince(), DateTimeZone.UTC);
    nextOpenDST = toDateTime(twDST.getNextOpen(), DateTimeZone.UTC);
    nextCloseDST = toDateTime(twDST.getNextClose(), DateTimeZone.UTC);
    sinceDST = toDateTime(twDST.getSince(), DateTimeZone.UTC);
    assertTrue(tw.isOpen());
    assertFalse(twDST.isOpen());
    
  }
  
  
  
  public void testExecutionInSavedTime() {
    final String LOCAL_RESTRICTION_1 = "[Second=0;Minute=30;Hour=2][Hour=1]"; // um 2 Uhr 30
    executeRestrictionForSavedTime(LOCAL_RESTRICTION_1);
    
    final String LOCAL_RESTRICTION_2 = "[Minute=30;Hour=2:24][Hour=1]"; // um 2 Uhr 30
    executeRestrictionForSavedTime(LOCAL_RESTRICTION_2);    
  }
  
  
  private static void executeRestrictionForSavedTime(String restriction) {
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(restriction, EUROPE_BERLIN, false);
    RestrictionBasedTimeWindowDefinition defDST = new RestrictionBasedTimeWindowDefinition(restriction, EUROPE_BERLIN, true);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    RestrictionBasedTimeWindow twDST = (RestrictionBasedTimeWindow) defDST.constructTimeWindow();
    
    long now = 1364554800000L; // 2013-03-29T12:00:00.000+01:00
    
    tw.recalculate(now);
    twDST.recalculate(now);
    
    DateTime nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
    DateTime nextClose = toDateTime(tw.getNextClose(), DateTimeZone.UTC);
    DateTime since = toDateTime(tw.getSince(), DateTimeZone.UTC);
    DateTime nextOpenDST = toDateTime(twDST.getNextOpen(), DateTimeZone.UTC);
    DateTime nextCloseDST = toDateTime(twDST.getNextClose(), DateTimeZone.UTC);
    DateTime sinceDST = toDateTime(twDST.getSince(), DateTimeZone.UTC);
    assertEquals(since, sinceDST);
    assertEquals(nextClose, nextCloseDST);
    assertEquals(nextOpen, nextOpenDST);
    
    now = 1364641200000L; // 2013-03-30T12:00:00.000+01:00
    
    tw.recalculate(now);
    twDST.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
    nextClose = toDateTime(tw.getNextClose(), DateTimeZone.UTC);
    since = toDateTime(tw.getSince(), DateTimeZone.UTC);
    nextOpenDST = toDateTime(twDST.getNextOpen(), DateTimeZone.UTC);
    nextCloseDST = toDateTime(twDST.getNextClose(), DateTimeZone.UTC);
    sinceDST = toDateTime(twDST.getSince(), DateTimeZone.UTC);
    assertEquals(since, sinceDST);
    assertNotSame(nextClose, nextCloseDST);
    assertNotSame(nextOpen, nextOpenDST);
    assertEquals(DateTimeZone.forID(EUROPE_BERLIN).nextTransition(now), nextOpenDST.getMillis());
    assertEquals(DateTimeZone.forID(EUROPE_BERLIN).nextTransition(now) + (60 * 60 * 1000), nextCloseDST.getMillis());
    
    now = 1364724000000L; // 2013-03-31T12:00:00.000+02:00
    
    tw.recalculate(now);
    twDST.recalculate(now);
    nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
    nextClose = toDateTime(tw.getNextClose(), DateTimeZone.UTC);
    since = toDateTime(tw.getSince(), DateTimeZone.UTC);
    nextOpenDST = toDateTime(twDST.getNextOpen(), DateTimeZone.UTC);
    nextCloseDST = toDateTime(twDST.getNextClose(), DateTimeZone.UTC);
    sinceDST = toDateTime(twDST.getSince(), DateTimeZone.UTC);
    
    assertNotSame(since, sinceDST);
    assertEquals(DateTimeZone.forID(EUROPE_BERLIN).previousTransition(now)+ 1 + (1000 * 60 * 60), sinceDST.getMillis());
    assertEquals(nextOpen, nextOpenDST.plusHours(1));
    assertEquals(nextClose, nextCloseDST.plusHours(1));
  }
  
  
  public void testBehaviourOfSmallIncrementsInSavedTime() {
    final String LOCAL_RESTRICTION_1 = "[Minute=30;Hour=:3][Hour=1]"; // um halb alle 3 Stunden für eine Stunde
    
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, EUROPE_BERLIN, false);
    RestrictionBasedTimeWindowDefinition defDST = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, EUROPE_BERLIN, true);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    RestrictionBasedTimeWindow twDST = (RestrictionBasedTimeWindow) defDST.constructTimeWindow();
    
    long now = 1364680800000L; // 2013-03-30T23:00:00.000+01:00
    DateTimeZone dtz = DateTimeZone.forID(EUROPE_BERLIN);
    
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    
    now = tw.getNextClose();
    
    tw.recalculate(now);
    twDST.recalculate(now);
    assertNotSame(tw.getNextOpen(), twDST.getNextOpen());
    assertNotSame(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    assertEquals(dtz.nextTransition(now), twDST.getNextOpen());
    assertEquals(dtz.nextTransition(now) + (1000 * 60 * 60), twDST.getNextClose());
    
    now = tw.getNextClose();
    
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen() - (1000 * 60 * 60), twDST.getNextOpen());
    assertEquals(tw.getNextClose() - (1000 * 60 * 60), twDST.getNextClose());
    assertEquals(tw.getSince() - (1000 * 60 * 30), twDST.getSince());
    
    
    final String LOCAL_RESTRICTION_2 = "[Minute=55:30][Minute=5]"; // alle 30min für 5min
    
    now = 1364687400000L; //2013-03-31T00:50:00.000+01:00
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_2, EUROPE_BERLIN, false);
    defDST = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_2, EUROPE_BERLIN, true);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    twDST = (RestrictionBasedTimeWindow) defDST.constructTimeWindow();
    
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    
    now = tw.getNextClose() + 1;
    
    tw.recalculate(now);
    twDST.recalculate(now);
    assertNotSame(tw.getNextOpen(), twDST.getNextOpen());
    assertNotSame(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    assertEquals(dtz.nextTransition(now), twDST.getNextOpen());
    assertEquals(dtz.nextTransition(now) + (1000 * 60 * 5), twDST.getNextClose());
    
    now = tw.getNextClose() + 1;
    
    tw.recalculate(now);
    twDST.recalculate(now);
    assertNotSame(tw.getNextOpen(), twDST.getNextOpen());
    assertNotSame(tw.getNextClose(), twDST.getNextClose());
    assertEquals(dtz.nextTransition(now), twDST.getNextOpen());
    assertEquals(dtz.nextTransition(now) + (1000 * 60 * 5), twDST.getNextClose());
    
    now = tw.getNextClose() + 1;
    
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    
  }
  
  
  public void testExecutionInExtraTime() {
    final String LOCAL_RESTRICTION_1 = "[Second=0;Minute=30;Hour=2][Hour=1]"; // um 2 Uhr 30
    
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, EUROPE_BERLIN, false);
    RestrictionBasedTimeWindowDefinition defDST = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, EUROPE_BERLIN, true);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    RestrictionBasedTimeWindow twDST = (RestrictionBasedTimeWindow) defDST.constructTimeWindow();
    
    DateTimeZone dtz = DateTimeZone.UTC;//DateTimeZone.forID(EUROPE_BERLIN);
    
    long now = 1382742000000L; // 2013-10-26T01:00:00.000+02:00;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen()-(1000*60*60), twDST.getNextOpen());
    assertEquals(tw.getNextClose()-(1000*60*60), twDST.getNextClose());
    assertEquals(tw.getSince()-(1000*60*60), twDST.getSince());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince()-(1000*60*60), twDST.getSince());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    
    final String LOCAL_RESTRICTION_2 = "[Minute=55:30][Minute=5]"; // alle 30min für 5min
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_2, EUROPE_BERLIN, false);
    defDST = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_2, EUROPE_BERLIN, true);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    twDST = (RestrictionBasedTimeWindow) defDST.constructTimeWindow();
    
    now = 1382827800000L; // 2013-10-27T00:50:00.000+02:00
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertNotSame(tw.getNextOpen(), twDST.getNextOpen());
    assertNotSame(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    assertEquals(tw.getNextOpen() + (1000 * 60 * 60), twDST.getNextOpen());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertNotSame(tw.getNextOpen(), twDST.getNextOpen());
    assertNotSame(tw.getNextClose(), twDST.getNextClose());
    assertNotSame(tw.getSince(), twDST.getSince());
    assertEquals(tw.getNextOpen() + (1000 * 60 * 60), twDST.getNextOpen());
    assertNotSame(tw.getNextClose() + (1000 * 60 * 60), twDST.getNextClose());
    assertNotSame(tw.getSince() + (1000 * 60 * 60), twDST.getSince());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    
    now = tw.getNextClose() + 1;
    tw.recalculate(now);
    twDST.recalculate(now);
    assertEquals(tw.getNextOpen(), twDST.getNextOpen());
    assertEquals(tw.getNextClose(), twDST.getNextClose());
    assertEquals(tw.getSince(), twDST.getSince());
    
  }
  
  
  public void testLargeIncrements() {
    final String LOCAL_RESTRICTION_1 = "[Minute=:90][Hour=1]"; // alle 90min für eine Stunde
    final String LOCAL_RESTRICTION_2 = "[Second=:300][Minute=1]"; // alle 5min für eine Minute
    final String LOCAL_RESTRICTION_3 = "[Second=:3660][Minute=1]"; // alle 61 Minute für eine Minute
    
    final int ITERATIONS = 1000;
    
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, EUROPE_BERLIN, false);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    long now = 1381831200000L; // 2013-10-15T10:00:00.000Z
    long lastOpen = -1;
    long restrictionIntervalInMs = 1000 * 60 * 90;
    long restrictionDurationInMs = 1000 * 60 * 60;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose();
    }
    
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_2, EUROPE_BERLIN, false);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    now = 1381831200000L; // 2013-10-15T10:00:00.000Z
    lastOpen = -1;
    restrictionIntervalInMs = 1000 * 300;
    restrictionDurationInMs = 1000 * 60;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose() + 1;
    }
    
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_3, EUROPE_BERLIN, false);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    now = 1381831200000L; // 2013-10-15T10:00:00.000Z
    lastOpen = -1;
    restrictionIntervalInMs = 1000 * 60 * 61;
    restrictionDurationInMs = 1000 * 60;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose() + 1;
    }
    
  }
  
  
  public void testCompositeIncrementsIncludingLarge() {
    final String LOCAL_RESTRICTION_1 = "[Second=:300;Minute=:3][Second=15]"; // alle 300sec=5min und 3min für 15sec ==> alle 15min
    
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, "UTC", false);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    final int ITERATIONS = 1000;
    
    long now = 0L;
    long lastOpen = -1;
    long restrictionIntervalInMs = 1000 * 60 * 15;
    long restrictionDurationInMs = 1000 * 15;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose() + 1;
    }
    
    
    final String LOCAL_RESTRICTION_2 = "[Second=:600;Minute=1-9][Second=15]"; // alle 600sec=10min bei Minute 1 bis 9 ==> stündlich bei Minute 1-9 (je nach StartZeitpunkt)
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_2, "UTC", false);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    now = 0L;
    lastOpen = -1;
    restrictionIntervalInMs = 1000 * 60 * 60;
    restrictionDurationInMs = 1000 * 15;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose() + 1;
    }
    
    //final String LOCAL_RESTRICTION_3 = "[Second=10;Minute=:600][Second=5]"; // kann gelesen werden als: immer zur 10. sekunden, alle 10h
    // durch die 10h Angabe im Minuten-Feld bedeutet es allerdings: zur 10sekunde, zu einer dynamisch gebundenen min alle zehn Stunden ([Second=10;Minute=?;Hour=:10][Second=5])
    final String LOCAL_RESTRICTION_3 = "[Millis=123;Second=:6000;Minute=:10][Second=20]"; // alle 100 min (eigentliche Minuten-Periode ist ein Bruchteil der Sekunden-Einschränkung) für 20 sec
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_3, "UTC", false);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    now = 990L;
    lastOpen = -1;
    restrictionIntervalInMs = 1000 * 6000;
    restrictionDurationInMs = 1000 * 20;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose() + 1;
    }
    
     
    lastOpen = -1;
    final String LOCAL_RESTRICTION_4 = "[Minute=0:240;DayOfWeek=SUN][Second=20]"; // Sonntags alle 4 Stunden
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_4, "UTC", false);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    now = 0L;
    lastOpen = -1;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        DateTime nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
        assertEquals(DateTimeConstants.SUNDAY, nextOpen.getDayOfWeek());
        if (nextOpen.getHourOfDay() < 3) {
          assertEquals(lastOpen, tw.getNextOpen() - ((1000 * 60 * 60 * 24 * 6) /*6 Tage*/  + (1000 * 60 * 60 * 4) /* 4 Stunden*/));
        } else {
          assertEquals(lastOpen, tw.getNextOpen() - (1000 * 60 * 60 * 4));
        }
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose();
    }
    
    final String LOCAL_RESTRICTION_5 = "[Minute=:240;Day=15W][Hour=3]"; // Alle Stunden an dem Wochentag welcher dem 15 am nähsten ist
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_5, "UTC", false);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    now = 0L;
    lastOpen = -1;
    
    tw.recalculate(now); // initialization
    
    now = 1381831200000L; // 2013-10-15T10:00:00.000Z
    
    tw.recalculate(now);
    assertTrue(tw.isOpen());
    assertEquals((1000 * 60 * 60 * 2), now - tw.getSince());
    assertEquals(now + (1000 * 60 * 60), tw.getNextClose());
    assertEquals(now + (1000 * 60 * 60 * 2), tw.getNextOpen());
    
    now = 1384552800000L; // 2013-11-15T22:00:00.000Z
    
    tw.recalculate(now);
    assertTrue(tw.isOpen());
    assertEquals((1000 * 60 * 60 * 2), now - tw.getSince());
    assertEquals(now + (1000 * 60 * 60), tw.getNextClose());
    DateTime nextOpenInDec = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
    assertEquals(DateTimeConstants.DECEMBER, nextOpenInDec.getMonthOfYear());
    assertEquals(0, nextOpenInDec.getHourOfDay());
    assertEquals(16, nextOpenInDec.getDayOfMonth()); // 15. is Sonntag -> 16.
  }
  
  
  
  public void testMillisecondRestrictions() {
    final String LOCAL_RESTRICTION_1 = "[Millis=:100][Millis=1]";
    final String LOCAL_RESTRICTION_2 = "[Millis=123][Millis=25]";
    
    final int ITERATIONS = 1000;
    
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, "UTC", false);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    long now = 0L;
    long lastOpen = -1;
    long restrictionIntervalInMs = 100;
    long restrictionDurationInMs = 1;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose() + 1;
    }
    
    def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_2, "UTC", false);
    tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    now = 0L;
    lastOpen = -1;
    restrictionIntervalInMs = 1000;
    restrictionDurationInMs = 25;
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        assertEquals(lastOpen + restrictionIntervalInMs, tw.getNextOpen());
        assertEquals(restrictionDurationInMs, tw.getNextClose() - tw.getNextOpen());
        assertEquals(123, tw.getNextOpen() % 1000);
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose() + 1;
    }
    
  }
  
  
  public void testPseudoIntervalAndLargeIncrement() {
    final String LOCAL_RESTRICTION_1 = "[Second=10,40;Minute=:600][Second=15]";
    
    final int ITERATIONS = 2000;
    
    RestrictionBasedTimeWindowDefinition def = new RestrictionBasedTimeWindowDefinition(LOCAL_RESTRICTION_1, "UTC", false);
    RestrictionBasedTimeWindow tw = (RestrictionBasedTimeWindow) def.constructTimeWindow();
    
    
    long now = 0L;
    long lastOpen = -1;
    
    tw.recalculate(now); // init
    
    for (int i = 0; i < ITERATIONS; i++) {
      tw.recalculate(now);
      if (lastOpen > 0) {
        DateTime nextOpen = toDateTime(tw.getNextOpen(), DateTimeZone.UTC);
        if (nextOpen.getSecondOfMinute() == 10) {
          assertEquals(lastOpen, tw.getNextOpen() - ((1000 * 60 * 60 * 10) /*10h*/  - (1000 * 30) /* 30sec*/));
        } else {
          assertEquals(lastOpen, tw.getNextOpen() - (1000 * 30));
        }
      }
      if (!tw.isOpen()) {
        lastOpen = tw.getNextOpen();
      }
      now = tw.getNextClose();
    }
    
  }
  
  
  public void testWindowsFromCronConversion() {
    List<CronConversionParameter> params = new ArrayList<RestrictionBasedTimeWindowTest.CronConversionParameter>();
    params.add(new CronConversionParameter(1000 * 60 * 15, EUROPE_BERLIN, false)); // alle 15 min
    params.add(new CronConversionParameter(1000 * 60 * 60, EUROPE_BERLIN, false)); // Stündlich
    params.add(new CronConversionParameter(1000 * 60 * 90, EUROPE_BERLIN, false)); // alle 90 min
    params.add(new CronConversionParameter(1000 * 60 * 60 * 24, EUROPE_BERLIN, false)); // täglich
    params.add(new CronConversionParameter(1000 * 60 * 60 * 24, EUROPE_BERLIN, true)); // täglich
    params.add(new CronConversionParameter(1000 * 60 * 60 * 24 * 7, EUROPE_BERLIN, false)); // wöchentlich
    params.add(new CronConversionParameter(1000 * 60 * 60 * 24 * 7, EUROPE_BERLIN, true)); // wöchentlich
    
    final long INIT_DATE = 1381917600000L; // 2013-10-16T12:00:00.000+02:00
    final int ITERATIONS = 2000;
    
    for (CronConversionParameter param : params) {
      TimeWindow tw = generateTimeWindowDefinition(param, INIT_DATE);
      for (int i = 0; i < ITERATIONS; i++) {
        long lastOpen = tw.getNextOpen();
        tw.recalculate(tw.getNextOpen() + 2);
        if (param.considerdaylightsaving) {
          Period diff = new Period(toDateTime(lastOpen), toDateTime(tw.getNextOpen()));
          boolean inWeeks = (param.interval %  (1000L*60*60*24*7)) == 0;
          if (inWeeks) {
            assertEquals(param.interval /  (1000L*60*60*24*7), diff.getWeeks());
            assertEquals(0, diff.getDays());
          } else {
            assertEquals(0, diff.getWeeks());
            assertEquals(param.interval /  (1000L*60*60*24), diff.getDays());
          }
          assertEquals(0, diff.getMillis());
          assertEquals(0, diff.getSeconds());
          assertEquals(0, diff.getMinutes());
          assertEquals(0, diff.getHours());
          assertEquals(0, diff.getMonths());
          assertEquals(0, diff.getYears());
        } else {
          assertEquals(lastOpen + param.interval, tw.getNextOpen());
        }
      }
    }
  }
  
  
  public void testAcceptDaylightSavingTime() {
    TimeControlRuleBuilder.CHECK_DST = true;
    
    final String LOCAL_RESTRICTION_1 = "[Minute=30;Hour=:48][Second=15]";
    final String LOCAL_RESTRICTION_2 = "[Minute=30:2880][Second=15]";
    final String LOCAL_RESTRICTION_3 = "[Second=:172800;Minute=30][Second=15]";
    final String LOCAL_RESTRICTION_4 = "[Millis=:172800000;Minute=30][Second=15]";
    final String LOCAL_RESTRICTION_5 = "[Minute=30;Hour=:48;Month=:2][Second=15]";
    
    final String LOCAL_RESTRICTION_6 = "[Minute=30;Hour=:49][Second=15]";
    final String LOCAL_RESTRICTION_7 = "[Minute=30:2881][Second=15]";
    final String LOCAL_RESTRICTION_8 = "[Second=:172800;Minute=30;Hour=:48][Second=15]"; //zwei Perioden
    
    String[] allowed = {RESTRICTION_3, RESTRICTION_1, RESTRICTION_4,
        RESTRICTION_5, RESTRICTION_7, RESTRICTION_8, RESTRICTION_6, RESTRICTION_10, LOCAL_RESTRICTION_1, LOCAL_RESTRICTION_2,
                    LOCAL_RESTRICTION_3, LOCAL_RESTRICTION_4, LOCAL_RESTRICTION_5};
    
    String[] notAllowed = {RESTRICTION_2, RESTRICTION_9,
                    LOCAL_RESTRICTION_6, LOCAL_RESTRICTION_7, LOCAL_RESTRICTION_8};
    
    RestrictionBasedTimeWindowDefinition def;
    
    for (String restriction : allowed) {
      def = new RestrictionBasedTimeWindowDefinition(restriction, "UTC", true);
      assertTrue(def.constructTimeWindow() != null);
    }

    for (String restriction : notAllowed) {
      def = new RestrictionBasedTimeWindowDefinition(restriction, "UTC", true);
      try {
        def.constructTimeWindow();
        fail("IllegalArgumentException expected: " + restriction);
      } catch (IllegalArgumentException e) {
        assertEquals("Daylight saving time isn't supported for " + restriction + ".", e.getMessage());
      }
    }
  }
  
  
  
  private static class CronConversionParameter {
    final long interval;
    final String timezoneid;
    final boolean considerdaylightsaving;
    
    CronConversionParameter(long interval, String timezoneid, boolean considerdaylightsaving) {
      this.interval = interval;
      this.timezoneid = timezoneid;
      this.considerdaylightsaving = considerdaylightsaving;
    }
  }
  
  
  private TimeWindow generateTimeWindowDefinition(CronConversionParameter param, long starttime) {
    return generateTimeWindowDefinition(param.interval, param.timezoneid, param.considerdaylightsaving, starttime);
  }
  
  
  private TimeWindow generateTimeWindowDefinition(long interval, String timezoneid, boolean considerdaylightsaving, long starttime) {
    String ruleAndDuration = CronLikeOrderCreationParameter.generateCalendarDefinition(interval) + 
                             CronLikeOrder.DEFAULT_TIME_WINDOW_DURATION;
    TimeWindow tw = new RestrictionBasedTimeWindowDefinition(ruleAndDuration, timezoneid, considerdaylightsaving).constructTimeWindow();
    tw.recalculate(starttime);
    return tw;
  }
  
  
  /*
    DateTimeZone dtz = DateTimeZone.forID(EUROPE_BERLIN);
    //dtz = DateTimeZone.UTC;
    DateTime dt = new DateTime(dtz);
    dt = dt.withMillisOfSecond(0);
    dt = dt.withSecondOfMinute(0);
    dt = dt.withMinuteOfHour(0);
    dt = dt.withHourOfDay(1);
    dt = dt.withMonthOfYear(DateTimeConstants.OCTOBER);
    dt = dt.withDayOfMonth(27);
    dt = dt.withYear(2013);
    System.out.println("now = " + dt.getMillis() + "L; // " + dt);
   */
  
}
