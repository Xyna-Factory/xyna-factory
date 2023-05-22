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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications;



import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.NotificationProcessor.RemoteCallNotificationStatus;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;



public class AwaitApplicationAvailableNotification extends RemoteCallNotification {

  private final long absoluteTimeout;
  private final ResumeTarget resumeTarget;
  private final String applicationName;


  public AwaitApplicationAvailableNotification(long absoluteTimeout, ResumeTarget resumeTarget, String applicationName) {
    this.absoluteTimeout = absoluteTimeout;
    this.resumeTarget = resumeTarget;
    this.applicationName = applicationName;
  }


  @Override
  public RemoteCallNotificationStatus execute(FactoryNodeCaller factoryNodeCaller) {
    factoryNodeCaller.getResumer().addAwaitApplicationAvailable(absoluteTimeout, factoryNodeCaller.getNodeName(), resumeTarget,
                                                                applicationName);
    return RemoteCallNotificationStatus.Succeeded;
  }

}
