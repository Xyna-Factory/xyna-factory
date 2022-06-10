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
package com.gip.xyna.xprc.xfractwfe.servicestepeventhandling;


//if this grows it could become a FunctionGroup, for now it's just a central repository for the ThreadLocal<ServiceStepEventSource>
public class ServiceStepEventHandling {

  public static ThreadLocal<ServiceStepEventSource> serviceStepEventSource = new ThreadLocal<ServiceStepEventSource>();
  
  public static ServiceStepEventSource getEventSource() {
    return ServiceStepEventHandling.serviceStepEventSource.get();
  }
  
}
