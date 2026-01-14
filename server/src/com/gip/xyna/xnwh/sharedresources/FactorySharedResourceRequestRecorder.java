/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.sharedresources;



import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class FactorySharedResourceRequestRecorder implements SharedResourceRequestRecorder {

  private static final Logger logger = CentralFactoryLogging.getLogger(FactorySharedResourceRequestRecorder.class);
  private static final String requestStartPattern = "Starting Shared Resource Request %d '%s' for resource '%s' against synchronizer '%s'";
  private static final String requestEndPattern = "Finished Shared Resource Request %d in %d ms";
  private static AtomicLong sessionUniqueIdCounter = new AtomicLong();

  private long startTime;
  private long requestId;


  @Override
  public void recordStartRequest(String requestType, String resourceType, String synchronizerName) {
    startTime = System.currentTimeMillis();
    requestId = sessionUniqueIdCounter.getAndIncrement();

    if (logger.isDebugEnabled()) {
      logger.debug(String.format(requestStartPattern, requestId, requestType, resourceType, synchronizerName));
    }
  }


  @Override
  public <T> void recordEndRequest(SharedResourceRequestResult<T> result) {
    if (logger.isDebugEnabled()) {
      long endTime = System.currentTimeMillis();
      long requestTime = endTime - startTime;
      logger.debug(String.format(requestEndPattern, requestId, requestTime));
    }
  }

}
