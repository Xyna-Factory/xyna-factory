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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listthreadpoolinfo;
import com.gip.xyna.xprc.ThreadPoolStatistics;
import com.gip.xyna.xprc.XynaExecutor;



public class ListthreadpoolinfoImpl extends XynaCommandImplementation<Listthreadpoolinfo> {

  private static final String headline =
      "ThreadPools: name (queuetype): active threads/current poolsize/min poolsize/largest poolsize/max poolsize," +
      " waiting in queue/max queuesize, keepalive seconds, completed tasks, rejected tasks";


  public void execute(OutputStream statusOutputStream, Listthreadpoolinfo payload) throws XynaException {
    ThreadPoolStatistics[] statistics = XynaExecutor.getInstance().getThreadPoolStatistics();
    writeLineToCommandLine(statusOutputStream, headline);
    for (ThreadPoolStatistics tp : statistics) {
      writeLineToCommandLine(statusOutputStream,
                             "  o " + tp.getId() + "(" + tp.getQueueType() + "): " + tp.getActiveCount() + "/" + tp.getCurrentPoolSize() + "/"
                                 + tp.getCorePoolSize() + "/" + tp.getLargestPoolSize() + "/" + tp.getMaxPoolSize()
                                 + " " + tp.getCurrentQueueSize() + "/" + tp.getMaxQueueSize() + " "
                                 + tp.getKeepAliveSeconds() + " " + tp.getCompletedTasks() + " " + tp.getRejectedTasks());
    }
  }

}
