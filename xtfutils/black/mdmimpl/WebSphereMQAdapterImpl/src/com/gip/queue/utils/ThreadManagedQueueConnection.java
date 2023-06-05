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

package com.gip.queue.utils;

import org.apache.log4j.Logger;

import com.gip.queue.utils.TimeoutCheckResult.CheckResultSignal;
import com.gip.queue.utils.exception.QueueException;


/**
 * connection is opened by constructor
 *
 */
public class ThreadManagedQueueConnection implements TimeoutCheckTarget {

  private static Logger _logger = Logger.getLogger(ThreadManagedQueueConnection.class);

  private final Thread _thread;
  private final QueueConnection _conn;
  private final long _connectionId;

  private boolean _isOpen = false;

  private int _timeoutSec = 0;

  private long _lastInternalMsgId = 0L;


  public ThreadManagedQueueConnection(QueueConnectionBuilder builder) throws QueueException {
    _conn = builder.build();
    _connectionId = ConnectionIdCounter.getNextId();
    _thread = new Thread(new QueueConnectionThread(this, _connectionId));
    open();
  }


  public synchronized void send(MsgToSend msg, int timeoutSec) throws QueueException {
    setTimeout(timeoutSec);
    _conn.send(msg);
    _lastInternalMsgId++;
  }

  public synchronized boolean IsOpen() {
    return _isOpen;
  }

  public synchronized void close() {
    try {
      closeImpl();
      _thread.interrupt();
    }
    catch (Exception e) {
      _logger.error("Error closing queue, connection id = " + _connectionId, e);
    }
  }

  public synchronized TimeoutCheckResult checkTimeout(TimeoutCheckInput input) {
    TimeoutCheckResult ret = new TimeoutCheckResult();
    ret.setTimeoutSec(_timeoutSec);
    ret.setLastMsgId(_lastInternalMsgId);

    //check if queue closed without thread
    if (!_isOpen) {
      ret.setSignal(CheckResultSignal.CONTINUE);
      return ret;
    }

    //check for activity since last check
    if (_lastInternalMsgId == input.getIdAtLastCheck()) {
      closeImpl();
      ret.setSignal(CheckResultSignal.STOP);
      return ret;
    }
    ret.setSignal(CheckResultSignal.CONTINUE);
    return ret;
  }


  private void setTimeout(int timeoutSec) {
    _timeoutSec = timeoutSec;
  }


  private void open() throws QueueException {
    _conn.open();
    _isOpen = true;
    _lastInternalMsgId = 0;
    _thread.start();
  }


  private void closeImpl() {
    try {
      _conn.close();
    }
    catch (Exception e) {
      _logger.error("Error closing queue, connection id = " + _connectionId, e);
    }
    _isOpen = false;
  }


}
