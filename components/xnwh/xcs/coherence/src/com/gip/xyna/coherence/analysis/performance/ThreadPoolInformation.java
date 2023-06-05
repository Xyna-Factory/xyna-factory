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

package com.gip.xyna.coherence.analysis.performance;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ThreadPoolExecutor;

import com.gip.xyna.coherence.Constants;
import com.gip.xyna.coherence.utils.threadpool.ThreadPool;



public abstract class ThreadPoolInformation {

  public void printInformation(OutputStream target) throws IOException {
    try {
      target.write(new String("No thread pool information available for custom thread pool")
          .getBytes(Constants.DEFAULT_ENCODING));
      printInformationInternally(target, Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Could not write thread pool information, encoding <" + Constants.DEFAULT_ENCODING
          + "> not supported", e);
    }
  }


  abstract void printInformationInternally(OutputStream target, String encoding) throws IOException;


  public static ThreadPoolInformation getThreadPoolInformation(String id, ThreadPoolExecutor tpe, int maxQueueSize) {
    return new JavaThreadPoolInformation(id, tpe, maxQueueSize);
  }


  public static ThreadPoolInformation getThreadPoolInformation(ThreadPool tp) {
    return new CustomThreadPoolInformation(tp);
  }

}
