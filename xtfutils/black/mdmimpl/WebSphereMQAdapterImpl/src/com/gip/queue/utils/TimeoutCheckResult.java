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

package com.gip.queue.utils;

public class TimeoutCheckResult {

  public enum CheckResultSignal {
    STOP, CONTINUE
  }

  private long lastMsgId = -1L;
  private CheckResultSignal _signal = CheckResultSignal.CONTINUE;
  private long timeoutSec = 1L;

  public TimeoutCheckResult() {
    //_lastMsgId = lastMsgId;
    //_signal = signal;
  }

  public long getLastMsgId() {
    return lastMsgId;
  }

  public CheckResultSignal getSignal() {
    return _signal;
  }


  public long getTimeoutSec() {
    return timeoutSec;
  }


  public void setTimeoutSec(long timeoutSec) {
    this.timeoutSec = timeoutSec;
  }


  public void setLastMsgId(long lastMsgId) {
    this.lastMsgId = lastMsgId;
  }


  public void setSignal(CheckResultSignal signal) {
    this._signal = signal;
  }

}