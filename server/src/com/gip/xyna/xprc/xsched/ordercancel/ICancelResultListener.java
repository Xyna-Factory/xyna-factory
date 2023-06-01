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

package com.gip.xyna.xprc.xsched.ordercancel;


public abstract class ICancelResultListener {
  
  private boolean success = false;
  public void callCancelSucceededAndSetSuccessFlag() {
    success = true;
    cancelSucceeded();
  }

  //protected, damit man weiss, dass callCancelSucceededAndSetSuccessFlag aufgerufen werden soll
  protected abstract void cancelSucceeded();

  public abstract void cancelFailed();

  private Long absoluteCancelTimeout = (long) 0;
  final public Long getAbsoluteCancelTimeout() {
    return absoluteCancelTimeout;
  }
  final public void setAbsoluteCancelTimeout(Long absoluteCancelTimeout) {
    this.absoluteCancelTimeout = absoluteCancelTimeout;
  }

  private volatile boolean isObsolete = false;
  final public void makeObsolete() {
    isObsolete = true;
  }

  final public boolean isObsolete() {
    return isObsolete;
  }

  //können compensations und resumes gecancelt werden?
  private boolean cancelCompensationAndResumes;
  public boolean cancelCompensationAndResumes() {
    return cancelCompensationAndResumes;
  }
  
  public void setCancelCompensationAndResumes(boolean b) {
    this.cancelCompensationAndResumes = b;
  }
  
  //wenn resumes gecancelt werden, sollen sie ihre capacities und vetos ignorieren und immer sofort gescheduled werden?
  private boolean ignoreResourcesWhenResuming;
  public boolean ignoreResourcesWhenResuming() {
    return ignoreResourcesWhenResuming;
  }
  
  public void setIgnoreResourcesWhenResuming(boolean b) {
    this.ignoreResourcesWhenResuming = b;
  }

  /**
   * wurde bereits cancelSucceeded aufgerufen?
   */
  public boolean cancelSuccess() {
    return success;
  }
}
