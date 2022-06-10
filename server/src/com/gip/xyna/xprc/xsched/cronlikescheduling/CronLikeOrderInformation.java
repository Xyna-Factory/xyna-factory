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

package com.gip.xyna.xprc.xsched.cronlikescheduling;

import java.io.Serializable;

import com.gip.xyna.xmcp.RemoteCronLikeOrderCreationParameter;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xprc.CustomStringContainer;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;



public class CronLikeOrderInformation implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;
  private String targetOrderType;

  private String label;

  private Long interval;
  private String calendarDefinition;
  private Long starttime;
  private Long nextExecution;
  private Boolean singleExecution;

  private Boolean enabled;
  private String status;
  private String errorMessage;
  private OnErrorAction onError;

  private String payload;
  
  private RuntimeContext runtimeContext;
  
  private String timeZoneID;
  private Boolean useDST;
  
  private CustomStringContainer cronLikeOrderCustoms;


  public CronLikeOrderInformation(CronLikeOrder clo) {
    this(clo.getId(), clo.getOrdertype(), getInputPayloadAsXML(clo), clo.getLabel(), clo
        .getInterval(), clo.getCalendarDefinition(), clo.getStartTime(), clo.getTimeZoneID(), clo.getNextExecution(), clo
        .isSingleExecution(), clo.getConsiderDaylightSaving(), clo.isEnabled(), clo.getOnErrorAsEnum(), clo.getStatus(), clo.getErrorMessage(), clo
        .getRuntimeContext(), new CustomStringContainer(clo.getCronLikeOrderCustom0(),
                                                        clo.getCronLikeOrderCustom1(),
                                                        clo.getCronLikeOrderCustom2(),
                                                        clo.getCronLikeOrderCustom3()));

  }
  
  
  private static String getInputPayloadAsXML(CronLikeOrder clo) {
    if (clo.getCreationParameters() != null) {
      if (clo.getCreationParameters().getInputPayload() != null) {
        return clo.getCreationParameters().getInputPayload().toXml();
      } else if (clo.getCreationParameters() instanceof RemoteCronLikeOrderCreationParameter) {
        return ((RemoteCronLikeOrderCreationParameter) clo.getCreationParameters()).getInputPayloadAsString();
      }
    }
    return null;
  }


  private CronLikeOrderInformation(Long id, String targetOrderType, String payload, String label, Long interval,
                                   String calendarDefinition, Long startTime, String timeZoneID, Long nextExecution,
                                   Boolean singleExecution, Boolean useDST,
                                   Boolean enabled, OnErrorAction onError, String status, String errorMsg,
                                   RuntimeContext runtimeContext,
                                   CustomStringContainer cronLikeOrderCustoms) {
    this.id = id;
    this.targetOrderType = targetOrderType;
    this.payload = payload;
    this.label = label;
    this.interval = interval;
    this.calendarDefinition = calendarDefinition;
    this.starttime = startTime;
    this.nextExecution = nextExecution;
    this.singleExecution = singleExecution;
    this.enabled = enabled;
    this.onError = onError;
    this.status = status;
    this.errorMessage = errorMsg;
    this.runtimeContext = runtimeContext;
    this.timeZoneID = timeZoneID;
    this.useDST = useDST;
    this.cronLikeOrderCustoms = cronLikeOrderCustoms;
  }


  public Long getId() {
    return id;
  }


  public String getLabel() {
    return this.label;
  }


  public String getTargetOrdertype() {
    return targetOrderType;
  }


  public Long getInterval() {
    return interval;
  }


  public String getCalendarDefinition() {
    return calendarDefinition;
  }


  public Long getStartTime() {
    return starttime;
  }


  public void setStartTime(long starttime) {
    this.starttime = starttime;
  }


  @Deprecated
  public Long getNextExecutionTime() {
    return nextExecution;
  }
  
  
  public Long getNextExecution() {
    return nextExecution;
  }
  
  public Boolean isSingleExecution() {
    return singleExecution;
  }
  
  public Boolean isEnabled() {
    return enabled;
  }
  
  public OnErrorAction getOnError() {
    return onError;
  }


  public String getStatus() {
    return status;
  }


  public String getErrorMessage() {
    return errorMessage;
  }


  public void setStatus(String s) {
    this.status = s;
  }


  public void setErrorMessage(String s) {
    this.errorMessage = s;
  }
  
  
  public String getPayload() {
    return payload;
  }
  
  
  public String getTimeZoneID() {
    return timeZoneID;
  }
  
  
  public Boolean getConsiderDaylightSaving() {
    return useDST;
  }


  @Override
  public CronLikeOrderInformation clone() {
    return new CronLikeOrderInformation(id, targetOrderType, payload, label, interval, calendarDefinition, starttime, timeZoneID,
                                        nextExecution, singleExecution, useDST, enabled, onError, status, errorMessage,
                                        runtimeContext, cronLikeOrderCustoms);
  }


  @Override // TODO is there a reason to not check 'if (x == null && other.x == null)' instead of possible returning true?
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (o == this)
      return true;
    if (!(o instanceof CronLikeOrderInformation))
      return false;
    CronLikeOrderInformation other = (CronLikeOrderInformation) o;

    if (id != null)
      if (!id.equals(other.id))
        return false;
    if (targetOrderType != null)
      if (!getTargetOrdertype().equals(other.getTargetOrdertype()))
        return false;
    if (payload != null)
      if (!getPayload().equals(other.getPayload()))
        return false;
    if (label != null)
      if (!getLabel().equals(other.getLabel()))
        return false;
    if (interval != null)
      if (!getInterval().equals(other.getInterval()))
        return false;
    if (starttime != null)
      if (!getStartTime().equals(other.getStartTime()))
        return false;
    if (nextExecution != null) {
      if (!getNextExecution().equals(other.getNextExecution()))
        return false;
    }
    if (enabled != null)
      if (!isEnabled().equals(other.isEnabled()))
        return false;
    if (onError != null)
      if (!getOnError().equals(other.getOnError()))
        return false;
    if (status != null)
      if (!getStatus().equals(other.getStatus()))
        return false;
    if (errorMessage != null)
      if (!getErrorMessage().equals(other.getErrorMessage()))
        return false;
    if (timeZoneID != null)
      if (!getTimeZoneID().equals(other.getTimeZoneID()))
        return false;
    if (useDST != null)
      if (!getConsiderDaylightSaving().equals(other.getConsiderDaylightSaving()))
        return false;
    if (cronLikeOrderCustoms != null)
      if (!cronLikeOrderCustoms.equals(other.cronLikeOrderCustoms)) 
        return false;
    
    return true;
  }


  @Override
  public int hashCode() {
    int hash = 0;
    if (id != null) {
      hash += id.hashCode();
    }
    if (targetOrderType != null) {
      hash += targetOrderType.hashCode();
    }
    if (payload != null) {
      hash += payload.hashCode();
    }
    if (label != null) {
      hash += label.hashCode();
    }
    if (interval != null) {
      hash += interval.hashCode();
    }
    if (starttime != null) {
      hash += starttime.hashCode();
    }
    if (nextExecution != null) {
      hash += nextExecution.hashCode();
    }
    if (enabled != null) {
      hash += enabled.hashCode();
    }
    if (onError != null) {
      hash += onError.hashCode();
    }
    if (status != null) {
      hash += status.hashCode();
    }
    if (errorMessage != null) {
      hash += errorMessage.hashCode();
    }
    if (timeZoneID != null) {
      hash += timeZoneID.hashCode();
    }
    if (useDST != null) {
      hash += useDST.hashCode();
    }
    if (cronLikeOrderCustoms != null) {
      hash += cronLikeOrderCustoms.hashCode();
    }

    return hash;
  }

  
  public String getApplicationName() {
    if (runtimeContext instanceof Application) {
      return runtimeContext.getName();
    }
    
    return null;
  }


  public String getVersionName() {
    if (runtimeContext instanceof Application) {
      return ((Application) runtimeContext).getVersionName();
    }
    
    return null;
  }

  
  public String getWorkspaceName() {
    if (runtimeContext instanceof Workspace) {
      return runtimeContext.getName();
    }
    
    return null;
  }
  
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
  
  
  public CustomStringContainer getCronLikeOrderCustoms() {
    return cronLikeOrderCustoms;
  }

}
