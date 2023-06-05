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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.timing.TimedTasks;
import com.gip.xyna.utils.timing.TimedTasks.Filter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;

public class Resumer implements TimedTasks.Executor<Resumer.ResumeData> {
  
  static final Logger logger = CentralFactoryLogging.getLogger(Resumer.class);
  
  private TimedTasks<ResumeData> resumeTasks;
  private SuspendResumeManagement srm;
  
  public Resumer() {
    srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
    resumeTasks = new TimedTasks<ResumeData>("FNC-Resumer", this, true);
  }
  
  public void addAwaitApplicationAvailable(long expiration, String nodeName, ResumeTarget resumeTarget, String applicationName) {
    resumeTasks.addTask(expiration, ResumeData.normal(nodeName, resumeTarget, applicationName));
  }
  
  public void addAwaitConnectivity(long expiration, String nodeName, ResumeTarget resumeTarget) {
    resumeTasks.addTask(expiration, ResumeData.normal(nodeName,resumeTarget) );
  }

  public void addAwaitOrder(long expiration, Long remoteOrderId, String nodeName, ResumeTarget resumeTarget) {
    resumeTasks.addTask(expiration, ResumeData.normal(nodeName,remoteOrderId,resumeTarget) );
  }
  
  public void remove(ResumeData resumeData) {
    resumeTasks.removeTask(resumeData);
  }

  public void execute(ResumeData work) {
    switch( work.mode ) {
    case ResumeAll:
      List<ResumeData> all = resumeTasks.removeTasks(new NodeNameFilter(work.nodeName));
      for( ResumeData rd : all ) {
        resume(rd);
      }
      break;
    case Normal:
      resume(work);
      break;
    case ResumeOrder:
      List<ResumeData> order = resumeTasks.removeTasks(new RemoteOrderIdFilter(work.remoteOrderId));
      for( ResumeData rd : order ) {
        resume(rd);
      }
      break;
    case ResumeApplication :
      List<ResumeData> data = resumeTasks.removeTasks(new RemoteApplicationFilter(work.nodeName, work.applicationName));
      for( ResumeData rd : data) {
        resume(rd);
      }
      break;
      default :throw new RuntimeException("Unsupported: " + work.mode);
    }
  }

  private void resume(ResumeData resumeData) {
    if( resumeData.resumeTarget == null ) {
      logger.warn("resumeData.resumeTarget= null " +  resumeData.mode + " "+ resumeData.nodeName);
      return;
    }
    ResumeTarget resumeTarget = resumeData.resumeTarget;
    try {
      Pair<ResumeResult, String> pair = srm.resumeOrder(resumeTarget);
      if( pair.getFirst() != ResumeResult.Resumed ) {
        logger.warn("Tried to resume "+ resumeTarget + " and got " + pair);
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to resume "+ resumeTarget, e);
    }
  }


  public void handleThrowable(Throwable executeFailed) {
    logger.warn("Unexpected exception while resuming", executeFailed);
  }

  public int getWaitingForApplicationAvailability(String nodeName) {
    return resumeTasks.count(new RemoteApplicationFilter(nodeName, null));
  }

  public int getWaiting(String nodeName) {
    return resumeTasks.count( new NodeNameFilter(nodeName) );
  }

  public void resumeWaitingForApplicationAvailability(String nodeName, String applicationName) {
    resumeTasks.addTask(0, ResumeData.resumeWaitingForApplicationAvailability(nodeName, applicationName));
  }

  public void resumeAll(String nodeName) {
    logger.info("resumeAll called");
    resumeTasks.addTask(0, ResumeData.resumeAll(nodeName) );
  }
  
  public void resume(Long remoteOrderId) {
    resumeTasks.addTask(0, ResumeData.resumeOrder(remoteOrderId) );
  }
  
  private enum Mode {
    Normal, ResumeAll, ResumeOrder, ResumeApplication;
  }
  
  public static class ResumeData {
    Mode mode;
    String nodeName;
    ResumeTarget resumeTarget;
    String applicationName;
    private Long remoteOrderId;
    
    private ResumeData(Mode mode) {
      this.mode = mode;
    }

    @Override
    public String toString() {
      return "ResumeData("+mode+","+nodeName+","+applicationName+","+remoteOrderId+","+resumeTarget+")";
    }

    public static ResumeData normal(String nodeName, Long remoteOrderId, ResumeTarget resumeTarget) {
      ResumeData rd = new ResumeData(Mode.Normal);
      rd.remoteOrderId = remoteOrderId;
      rd.nodeName = nodeName;
      rd.resumeTarget = resumeTarget;
      return rd;
    }

    public static ResumeData normal(String nodeName, ResumeTarget resumeTarget) {
      ResumeData rd = new ResumeData(Mode.Normal);
      rd.nodeName = nodeName;
      rd.resumeTarget = resumeTarget;
      return rd;
    }

    public static ResumeData normal(String nodeName, ResumeTarget resumeTarget, String applicationName) {
      ResumeData rd = new ResumeData(Mode.Normal);
      rd.nodeName = nodeName;
      rd.resumeTarget = resumeTarget;
      rd.applicationName = applicationName;
      return rd;
    }

    public static ResumeData resumeAll(String nodeName) {
      ResumeData rd = new ResumeData(Mode.ResumeAll);
      rd.nodeName = nodeName;
      return rd;
    }

    public static ResumeData resumeOrder(Long remoteOrderId) {
      ResumeData rd = new ResumeData(Mode.ResumeOrder);
      rd.remoteOrderId = remoteOrderId;
      return rd;
    }
    
    public static ResumeData resumeWaitingForApplicationAvailability(String nodeName, String applicationName) {
      ResumeData rd = new ResumeData(Mode.ResumeApplication);
      rd.nodeName = nodeName;
      rd.applicationName = applicationName;
      return rd;
    }
  }

  public class NodeNameFilter implements Filter<ResumeData> {

    private String nodeName;

    public NodeNameFilter(String nodeName) {
      this.nodeName = nodeName;
    }

    public boolean isMatching(ResumeData work) {
      return nodeName.equals( work.nodeName );
    }

  }

  public class RemoteOrderIdFilter implements Filter<ResumeData> {

    private Long remoteOrderId;

    public RemoteOrderIdFilter(Long remoteOrderId) {
      this.remoteOrderId = remoteOrderId;
    }

    public boolean isMatching(ResumeData work) {
      return remoteOrderId.equals( work.remoteOrderId );
    }

  }
  
  public class RemoteApplicationFilter implements Filter<ResumeData> {

    private final String nodeName;
    private final String applicationName;
    
    public RemoteApplicationFilter(String nodeName, String applicationName) {
      this.nodeName = nodeName;
      this.applicationName = applicationName;
    }

    public boolean isMatching(ResumeData work) {
      if( ! nodeName.equals(work.nodeName)) {
        return false;
      }
      if( applicationName == null ) {
        //appName == null -> kein spezifischer Filter auf appName, aber muss ungleich null sein!
        return work.applicationName != null;
      } else {
        return applicationName.equals(work.applicationName);
      }
    }

  }


  public boolean isEmpty() {
    return resumeTasks.size() == 0;
  }

}
