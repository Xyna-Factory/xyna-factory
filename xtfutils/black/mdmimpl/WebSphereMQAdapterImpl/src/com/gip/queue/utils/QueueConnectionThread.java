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

package com.gip.queue.utils;

import org.apache.log4j.Logger;

import com.gip.queue.utils.TimeoutCheckResult.CheckResultSignal;


public class QueueConnectionThread implements Runnable {

  private static Logger _logger = Logger.getLogger(QueueConnectionThread.class);

  private TimeoutCheckTarget _connToCheck;
  private final long _internalConnectionId;
  private long _lastMsgId = -1L;
  private long _timeoutMillis = 1000L;


  public QueueConnectionThread(TimeoutCheckTarget connToCheck, long internalConnectionId) {
    _connToCheck = connToCheck;
    _internalConnectionId = internalConnectionId;
  }


  public void run() {
    _logger.warn("Starting thread, internal connection id = " + _internalConnectionId);
    doLoop();
  }


  private void doLoop() {
    while (true) {
      try {
        Thread.sleep(_timeoutMillis);
      }
      catch (InterruptedException e) {
        break;
      }
      if (Thread.interrupted()) {
        _logger.info("Thread was interrupted. Internal connection id = " + _internalConnectionId +
                 ", last msg id = " + _lastMsgId);
        //finish thread
        break;
      }
      _logger.info("Calling checkTimeout. Internal connection id = " + _internalConnectionId +
                 ", last msg id = " + _lastMsgId);
      TimeoutCheckInput input = new TimeoutCheckInput();
      input.setIdAtLastCheck(_lastMsgId);

      TimeoutCheckResult result = _connToCheck.checkTimeout(input);
      _lastMsgId = result.getLastMsgId();
      if (result.getTimeoutSec() > 1) {
        _timeoutMillis = result.getTimeoutSec() * 1000;
      }
      if (result.getSignal() == CheckResultSignal.STOP) {
        //finish thread
        _logger.info("No new messages were sent, leaving thread. Internal connection id = " + _internalConnectionId +
                 ", last msg id = " + _lastMsgId);
        break;
      }
    }
    _logger.warn("Thread will stop now. Internal connection id = " + _internalConnectionId +
                 ", last msg id = " + _lastMsgId);
  }


}
