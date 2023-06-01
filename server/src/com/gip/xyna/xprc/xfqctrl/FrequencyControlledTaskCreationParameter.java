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



import java.io.Serializable;

import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FREQUENCY_CONTROLLED_TASK_TYPE;



public abstract class FrequencyControlledTaskCreationParameter implements Serializable {

  private static final long serialVersionUID = 2116774653700438726L;

  private String label;
  private long eventsToLaunch;
  private FrequencyControlledTaskEventAlgorithmParameter algorithmParameters;
  private transient Role role;

  private FrequencyControlledTaskStatisticsParameter statisticParameters;

  private String delay;
  private String timezone;

  public FrequencyControlledTaskCreationParameter(String label, long eventsToLaunch) {
    this.label = label;
    this.eventsToLaunch = eventsToLaunch;
  }
  
  
  public void setLabel(String lable) {
    this.label = lable;
  }
  
  
  public String getLabel() {
    return this.label;
  }
  
  
  public void setEventsToLaunch(long eventsToLaunch) {
    this.eventsToLaunch = eventsToLaunch;
  }


  public long getEventsToLaunch() {
    return eventsToLaunch;
  }


  public void setFrequencyControlledTaskStatisticsParameters(FrequencyControlledTaskStatisticsParameter parameters) {
    this.statisticParameters = parameters;
  }


  public FrequencyControlledTaskStatisticsParameter getFrequencyControlledTaskStatisticsParameters() {
    return this.statisticParameters;
  }
  
  public FrequencyControlledTaskEventAlgorithmParameter getAlgorithmParameters() {
    return algorithmParameters;
  }
  
  public void setAlgorithmParameters(FrequencyControlledTaskEventAlgorithmParameter parameters) {
    algorithmParameters = parameters;
  }

  public void setTransientCreationRole(Role role) {
    this.role = role;
  }
  
  public Role getTransientCreationRole() {
    return role;
  }

  public abstract FREQUENCY_CONTROLLED_TASK_TYPE getTaskType();


  public String getDelay() {
    return delay;
  }


  public void setDelay(String delay) {
    this.delay = delay;
  }


  public String getTimezone() {
    return timezone;
  }


  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

}
