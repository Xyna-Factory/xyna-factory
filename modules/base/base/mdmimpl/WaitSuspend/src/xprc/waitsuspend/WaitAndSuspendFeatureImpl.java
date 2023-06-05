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
package xprc.waitsuspend;


import java.util.Calendar;
import java.util.EnumSet;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderAlreadyExistsException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler.CronLikeOrderPersistenceOption;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderColumn;
import com.gip.xyna.xprc.xsched.ordersuspension.ResumeOrderBean;


public class WaitAndSuspendFeatureImpl implements DeploymentTask {

  private static Logger logger = CentralFactoryLogging.getLogger(WaitAndSuspendFeatureImpl.class);
  private static WaitAndSuspendHandler waitAndSuspendHandler = new WaitAndSuspendHandler();
  private static EnumSet<CronLikeOrderColumn> CLO_UNIQUENESS = EnumSet.of(CronLikeOrderColumn.ROOT_ORDER_ID,CronLikeOrderColumn.CUSTOM0);
  
  protected WaitAndSuspendFeatureImpl() {
  }


  public void onDeployment() {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
    addListener(waitAndSuspendHandler);
  }

  public void onUndeployment() {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
    removeListener(waitAndSuspendHandler);
  }


  public static void suspend(XynaOrderServerExtension xo, TimeConfiguration timeConfigurationParameter,
                             Long suspensionTime, AtomicLong resumeTime, String laneId) throws SuspensionError {
    try {
      waitOrSuspend(xo,timeConfigurationParameter,suspensionTime,resumeTime,laneId,false);
    } catch (IllegalArgumentException e) {
      throw new SuspensionError(e);
    }
  }


  public static void wait(XynaOrderServerExtension xo, TimeConfiguration timeConfigurationParameter,
                          Long suspensionTime, AtomicLong resumeTime, String laneId) throws WaitingError {
    try {
      waitOrSuspend(xo,timeConfigurationParameter,suspensionTime,resumeTime,laneId,true);
    } catch (IllegalArgumentException e) {
      throw new WaitingError(e);
    }
  }


  private static final XynaPropertyDuration minTimeForSuspensionWithFreeCapacities =
      new XynaPropertyDuration("xyna.xprc.waitsuspend.suspension.threshold", "2 s")
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Minimum wait duration that results in suspension (with freed capacities) of the order. Otherwise the thread will just sleep and not free capacities.");
  private static final XynaPropertyDuration minTimeForSuspension = new XynaPropertyDuration("xyna.xprc.waitsuspend.wait.threshold", "15 s")
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Minimum wait duration that results in suspension (without freed capacities) of the order. Otherwise the thread will just sleep.");


  /**
   * @param timeConfigurationParameter
   * @param xo
   * @param suspensionTime
   * @param resumeTime
   * @param laneId
   * @param waitOrSuspend
   * @throws WaitingError 
   */
  private static void waitOrSuspend(XynaOrderServerExtension xo, TimeConfiguration timeConfigurationParameter, 
                                    Long suspensionTime, AtomicLong resumeTimeAL, String laneId, boolean waitOrSuspend)  {
    long resumeTime = resumeTimeAL.get();
    if (suspensionTime == null) {
      //erster Aufruf
      resumeTime = calculateAbsoluteMilliSecondsResumeTimeOutOfTimeConfig(timeConfigurationParameter);
      XynaPropertyDuration dur = waitOrSuspend ? minTimeForSuspension : minTimeForSuspensionWithFreeCapacities;
      long waitTime = resumeTime - System.currentTimeMillis();
      if (waitTime < dur.getMillis()) {
        if (waitTime > 0) {
          try {
            Thread.sleep(waitTime);
          } catch (InterruptedException e) {
            throw new RuntimeException("interrupted");
          }
        }
        return;
      }
      resumeTimeAL.set(resumeTime); //resumeTime in AtomicLong eintragen, damit in StepFunction/FractalProcessStep auslesbar
      prepareResume(xo, laneId, resumeTime, waitOrSuspend, false);
      suspend(xo, laneId, resumeTime, waitOrSuspend);
    } else {
      //erneuter Aufruf (beim Resume oder evtl. nach Neustart der Factory)
      if( resumeTime == 0L) {
        throw new RuntimeException("Suspension time is set but resume time has not been evaluated.");
      } else {
        if (resumeTime <= System.currentTimeMillis() ) {
          return; //regulärer Ausgang: Wartezeit ist abgelaufen
        } else {
          prepareResume(xo, laneId, resumeTime, waitOrSuspend, true);
          suspend(xo, laneId, resumeTime, waitOrSuspend);
        }
      }
    }
  }


  /**
   * Vorbereitungen, damit ein Resume möglich wird
   * @param xo
   * @param laneId
   * @param resumeTime
   * @param waitOrSuspend
   * @param additionalCall true, wenn waitOrSuspend ein zweites Mal aufgerufen wird, bevor die Wartezeit abgelaufen ist
   */
  private static void prepareResume(XynaOrderServerExtension xo, String laneId, long resumeTime, boolean waitOrSuspend, boolean additionalCall) {
    ResumeTarget resumeTarget = new ResumeTarget(xo,laneId);
    ResumeOrderBean payload = new ResumeOrderBean(resumeTarget);
    
    CronLikeOrderCreationParameter cronParameters =
        new CronLikeOrderCreationParameter(XynaDispatcher.DESTINATION_KEY_RESUME, resumeTime, null, payload);
    cronParameters.setRootOrderId(xo.getRootOrder().getId());
    cronParameters.setCronLikeOrderCustom0(resumeTarget.serializeToString());
     
    try {
      CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
      cls.createCronLikeOrder(cronParameters, 
                                      CronLikeOrderPersistenceOption.REMOVE_ON_SHUTDOWN_IF_NO_CLUSTER,
                                      null,
                                      Thread.NORM_PRIORITY,
                                      additionalCall ? CLO_UNIQUENESS : null);
    } catch (XPRC_CronLikeOrderAlreadyExistsException e) {
      if( additionalCall ) {
        //dies ist hier kein Fehler, daher ignorieren
        if( logger.isDebugEnabled() ) {
          logger.debug( e.getMessage() +" expected resume is in "+(resumeTime-System.currentTimeMillis())+" ms" );
        }
      } else {
        //unerwartet!
        throw new RuntimeException("Unexpected error while trying to schedule resume order for order "+xo, e);
      }
    } catch (XPRC_CronLikeSchedulerException e) {
      throw new RuntimeException("Unexpected error while trying to schedule resume order for order "+xo, e);
    } catch (XNWH_RetryTransactionException e) {
      throw new RuntimeException("Unexpected error while trying to schedule resume order for order "+xo, e);
    }
  }

  /**
   * Suspendierung durch Werfen der SuspendedException
   * @param xo
   * @param laneId
   * @param resumeTime
   * @param waitOrSuspend
   */
  private static void suspend(XynaOrderServerExtension xo, String laneId, long resumeTime, boolean waitOrSuspend ) {
    SuspensionCause suspensionCause = null;
    if( waitOrSuspend ) {
      suspensionCause = new SuspensionCause_Wait(laneId);
    } else {
      suspensionCause = new SuspensionCause_Suspend(laneId);
    }
    throw new ProcessSuspendedException(suspensionCause);
  }

  
  private static Long calculateAbsoluteMilliSecondsResumeTimeOutOfTimeConfig(TimeConfiguration timeConfig) {

    if (timeConfig instanceof RelativeTimeConfiguration) {

      Calendar tempCalendar = Calendar.getInstance();
      RelativeTimeConfiguration relTimeConfig = (RelativeTimeConfiguration) timeConfig;

      if (relTimeConfig.getTimeInSeconds() != null && relTimeConfig.getTimeInSeconds().getTimeUnitInSeconds() != null) {
        tempCalendar.add(Calendar.SECOND, relTimeConfig.getTimeInSeconds().getTimeUnitInSeconds());
      }
      if (relTimeConfig.getTimeInMinutes() != null && relTimeConfig.getTimeInMinutes().getTimeUnitInMinutes() != null) {
        tempCalendar.add(Calendar.MINUTE, relTimeConfig.getTimeInMinutes().getTimeUnitInMinutes());
      }
      if (relTimeConfig.getTimeInHours() != null && relTimeConfig.getTimeInHours().getTimeUnitInHours() != null) {
        tempCalendar.add(Calendar.HOUR, relTimeConfig.getTimeInHours().getTimeUnitInHours());
      }
      if (relTimeConfig.getTimeInDays() != null && relTimeConfig.getTimeInDays().getTimeUnitInDays() != null) {
        tempCalendar.add(Calendar.DAY_OF_MONTH, relTimeConfig.getTimeInDays().getTimeUnitInDays());
      }
      if (relTimeConfig.getTimeInMonths() != null && relTimeConfig.getTimeInMonths().getTimeUnitInMonths() != null) {
        tempCalendar.add(Calendar.MONTH, relTimeConfig.getTimeInMonths().getTimeUnitInMonths());
      }
      if (relTimeConfig.getTimeInYears() != null && relTimeConfig.getTimeInYears().getTimeUnitInYears() != null) {
        tempCalendar.add(Calendar.YEAR, relTimeConfig.getTimeInYears().getTimeUnitInYears());
      }

      return tempCalendar.getTime().getTime();

    } else if (timeConfig instanceof AbsoluteTimeConfiguration) {

      AbsoluteTimeConfiguration absTimeConfig = (AbsoluteTimeConfiguration) timeConfig;

      Integer timezoneOffsetInHours = absTimeConfig.getTimezoneOffset() != null ? absTimeConfig.getTimezoneOffset()
                      .getTimezoneOffsetInHours() : 0;
      if (timezoneOffsetInHours == null) {
        timezoneOffsetInHours = 0;
      }

      TimeZone tz = TimeZone.getTimeZone("GMT"); // ignore DST
      int timeOffsetInMilliSeconds = timezoneOffsetInHours * 60 * 60 * 1000;
      tz.setRawOffset(timeOffsetInMilliSeconds);
      Calendar tempCalendar = Calendar.getInstance(tz);

      if (absTimeConfig.getTimeInYears() != null && absTimeConfig.getTimeInYears().getTimeUnitInYears() != null) {
        tempCalendar.set(Calendar.YEAR, absTimeConfig.getTimeInYears().getTimeUnitInYears());
      }

      if (absTimeConfig.getTimeInMonths() != null && absTimeConfig.getTimeInMonths().getTimeUnitInMonths() != null) {
        tempCalendar.set(Calendar.MONTH, absTimeConfig.getTimeInMonths().getTimeUnitInMonths() - 1);
      } else {
        tempCalendar.set(Calendar.MONTH, Calendar.JANUARY);
      }

      if (absTimeConfig.getTimeInDays() != null && absTimeConfig.getTimeInDays().getTimeUnitInDays() != null) {
        tempCalendar.set(Calendar.DAY_OF_MONTH, absTimeConfig.getTimeInDays().getTimeUnitInDays());
      } else {
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
      }

      if (absTimeConfig.getTimeInHours() != null && absTimeConfig.getTimeInHours().getTimeUnitInHours() != null) {
        tempCalendar.set(Calendar.HOUR_OF_DAY, absTimeConfig.getTimeInHours().getTimeUnitInHours());
      } else {
        tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
      }

      if (absTimeConfig.getTimeInMinutes() != null && absTimeConfig.getTimeInMinutes().getTimeUnitInMinutes() != null) {
        tempCalendar.set(Calendar.MINUTE, absTimeConfig.getTimeInMinutes().getTimeUnitInMinutes());
      } else {
        tempCalendar.set(Calendar.MINUTE, 0);
      }

      if (absTimeConfig.getTimeInSeconds() != null && absTimeConfig.getTimeInSeconds().getTimeUnitInSeconds() != null) {
        tempCalendar.set(Calendar.SECOND, absTimeConfig.getTimeInSeconds().getTimeUnitInSeconds());
      } else {
        tempCalendar.set(Calendar.SECOND, 0);
      }

      long l = tempCalendar.getTime().getTime();
      if (l > 0 && l < System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
        //absolute zeit wurde angegeben, aber vermutlich nicht wie intendiert, nämlich mehr als ein tag in der vergangenheit
        logger.info("Absolute time provided is more than one day in the past.");
      }
      return l;

    } else {
      throw new IllegalArgumentException("Unsupported time configuration type: " + (timeConfig != null ? timeConfig
                      .getClass().getName() : "null"));
    }

  }
  
}
