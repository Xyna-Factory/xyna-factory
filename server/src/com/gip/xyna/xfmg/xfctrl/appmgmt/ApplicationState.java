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
package com.gip.xyna.xfmg.xfctrl.appmgmt;


public enum ApplicationState {

  RUNNING(true),
  STOPPED(false),
  OK(false), //f�r Application Definitions
  @Deprecated
  //wird noch f�r alte in diesem zustand befindliche applications verwendet.
  AUDIT_MODE(false),
  
  @Deprecated
  //wird nur noch im ApplicationStorable verwendet, aber eigentlich nicht mehr ben�tigt;
  //ob es sich um eine Application Definition handelt kann daran erkannt werden dass es eine parentRevision gibt
  WORKINGCOPY(false),

  
  //folgende werden nicht persistiert, sondern nur bei listApplications zur�ckgegeben, falls ein Objekt fehlerhaft ist
  WARNING(true), //TODO isRunning?
  ERROR(true),  //TODO isRunning?
  
  //Sonderfall: nicht importierte Application
  FILE(false);
  
  private boolean isRunning;
  
  private ApplicationState(boolean isRunning) {
    this.isRunning = isRunning;
  }
  
  public boolean isRunning() {
    return isRunning;
  }

  public boolean hasWarning() {
    return this == WARNING;
  }
  
  public boolean hasError() {
    return this == ERROR;
  }

}
